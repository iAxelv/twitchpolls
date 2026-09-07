package com.foxy.twitchPolls.commands;

import com.foxy.twitchPolls.TwitchManager;
import com.foxy.twitchPolls.TwitchPolls;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

@SuppressWarnings("deprecation")
public class ConfigCommand implements Listener {
    private final TwitchPolls plugin;
    private final TwitchManager twitchManager;
    private final Map<UUID, InputRequest> pendingInputs = new HashMap<>();
    private YamlConfiguration menuConfig;

    public ConfigCommand(TwitchPolls plugin, TwitchManager twitchManager) {
        this.plugin = plugin;
        this.twitchManager = twitchManager;
        reload();
    }

    public void reload() {
        menuConfig = YamlConfiguration.loadConfiguration(new File(plugin.getDataFolder(), "gui.yml"));
    }

    public void open(Player player) {
        ConfigurationSection configMenu = menuConfig.getConfigurationSection("config-menu");
        if (configMenu == null) {
            send(player, "messages.gui.config-missing", "&cNo config menu found in gui.yml.");
            return;
        }
        int rows = Math.max(1, Math.min(6, configMenu.getInt("rows", 3)));
        ConfigHolder holder = new ConfigHolder();
        Inventory inventory = Bukkit.createInventory(holder, rows * 9,
                color(configMenu.getString("title", "&8StreamManager Config")));
        holder.setInventory(inventory);
        ConfigurationSection items = configMenu.getConfigurationSection("items");
        if (items == null) {
            send(player, "messages.gui.config-items-missing", "&cNo config items found in gui.yml.");
            return;
        }
        for (String key : items.getKeys(false)) {
            ConfigurationSection itemConfig = items.getConfigurationSection(key);
            if (itemConfig == null || !itemConfig.getBoolean("enabled", true)) {
                continue;
            }
            int slot = itemConfig.getInt("slot", -1);
            if (slot < 0 || slot >= inventory.getSize()) {
                continue;
            }
            String path = itemConfig.getString("path", key);
            holder.items.put(slot, path);
            inventory.setItem(slot, createItem(itemConfig, path));
        }
        player.openInventory(inventory);
    }

    private ItemStack createItem(ConfigurationSection itemConfig, String path) {
        String type = itemConfig.getString("type", "input").toLowerCase(Locale.ROOT);
        boolean enabled = "boolean".equals(type) && plugin.getConfig().getBoolean(path, false);
        boolean alternateProvider = "provider".equals(type)
            && "tiktok".equalsIgnoreCase(plugin.getConfig().getString(path, "twitch"));
        String materialKey = enabled || alternateProvider ? "enabled-material" : "material";
        Material material = material(itemConfig.getString(materialKey, "PAPER"));
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(color(itemConfig.getString("display-name", path)));
            String value = String.valueOf(plugin.getConfig().get(path, ""));
            List<String> lore = itemConfig.getStringList("lore");
            meta.setLore(lore.stream()
                    .map(line -> color(line.replace("%value%", value).replace("%state%", value)))
                    .toList());
            item.setItemMeta(meta);
        }
        return item;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getView().getTopInventory().getHolder() instanceof ConfigHolder holder)) {
            return;
        }
        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player player)
                || event.getRawSlot() < 0 || event.getRawSlot() >= event.getView().getTopInventory().getSize()) {
            return;
        }
        String path = holder.items.get(event.getRawSlot());
        if (path == null) {
            return;
        }
        ConfigurationSection itemConfig = findItemByPath(path);
        if (itemConfig == null) {
            return;
        }
        String type = itemConfig.getString("type", "input").toLowerCase(Locale.ROOT);
        if ("boolean".equals(type)) {
            saveValue(path, !plugin.getConfig().getBoolean(path, false));
            open(player);
            return;
        }
        if ("provider".equals(type)) {
            String currentProvider = plugin.getConfig().getString(path, "twitch");
            saveValue(path, "twitch".equalsIgnoreCase(currentProvider) ? "tiktok" : "twitch");
            open(player);
            return;
        }
        player.closeInventory();
        pendingInputs.put(player.getUniqueId(), new InputRequest(path, type,
                itemConfig.getStringList("allowed-values")));
        send(player, "messages.gui.config-prompt", "&eType the new value in chat, or type CANCEL to return.");
    }

    private ConfigurationSection findItemByPath(String path) {
        ConfigurationSection items = menuConfig.getConfigurationSection("config-menu.items");
        if (items == null) {
            return null;
        }
        for (String key : items.getKeys(false)) {
            ConfigurationSection item = items.getConfigurationSection(key);
            if (item != null && path.equals(item.getString("path", key))) {
                return item;
            }
        }
        return null;
    }

    @EventHandler
    public void onChat(AsyncPlayerChatEvent event) {
        InputRequest request = pendingInputs.remove(event.getPlayer().getUniqueId());
        if (request == null) {
            return;
        }
        event.setCancelled(true);
        String input = event.getMessage().trim();
        Bukkit.getScheduler().runTask(plugin, () -> handleInput(event.getPlayer(), request, input));
    }

    private void handleInput(Player player, InputRequest request, String input) {
        if (!player.isOnline()) {
            return;
        }
        if (input.equalsIgnoreCase("CANCEL")) {
            open(player);
            return;
        }
        if (!request.allowedValues().isEmpty() && request.allowedValues().stream()
                .noneMatch(value -> value.equalsIgnoreCase(input))) {
            send(player, "messages.gui.config-invalid", "&cInvalid value. Try again or type CANCEL.");
            pendingInputs.put(player.getUniqueId(), request);
            return;
        }
        Object value = input;
        if ("integer".equals(request.type())) {
            try {
                value = Integer.parseInt(input);
            } catch (NumberFormatException exception) {
                send(player, "messages.gui.config-invalid", "&cPlease enter a whole number, or type CANCEL.");
                pendingInputs.put(player.getUniqueId(), request);
                return;
            }
        }
        if (value instanceof Integer integer && integer < 0) {
            send(player, "messages.gui.config-invalid", "&cThat number is outside the allowed range.");
            pendingInputs.put(player.getUniqueId(), request);
            return;
        }
        saveValue(request.path(), value);
        send(player, "messages.gui.config-saved", "&aConfiguration updated.");
        open(player);
    }

    private void saveValue(String path, Object value) {
        plugin.getConfig().set(path, value);
        plugin.saveConfig();
        if (path.equals("settings.event-provider") || path.startsWith("tiktok.")) {
            plugin.reloadEventProviderConnections();
        } else if (path.startsWith("settings.")) {
            twitchManager.reloadAutomaticPolls();
            plugin.getSessionManager().refresh();
        }
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        if (event.getView().getTopInventory().getHolder() instanceof ConfigHolder) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        pendingInputs.remove(event.getPlayer().getUniqueId());
    }

    private Material material(String name) {
        try {
            return Material.valueOf(name.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            return Material.PAPER;
        }
    }

    private void send(Player player, String path, String fallback) {
        player.sendMessage(LegacyComponentSerializer.legacyAmpersand().deserialize(
                plugin.getLanguageManager().getString(path, fallback)));
    }

    private String color(String text) {
        return ChatColor.translateAlternateColorCodes('&', text == null ? "" : text);
    }

    private record InputRequest(String path, String type, List<String> allowedValues) { }

    private static class ConfigHolder extends GUIHolder {
        private final Map<Integer, String> items = new HashMap<>();

        private ConfigHolder() {
            super("config");
        }
    }
}