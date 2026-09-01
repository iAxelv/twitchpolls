package com.foxy.twitchPolls.commands;

import com.foxy.twitchPolls.ActionManager;
import com.foxy.twitchPolls.TwitchPolls;
import com.foxy.twitchPolls.UIManager;
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
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("deprecation")
public class TestCommand implements Listener {
    private static final String[] EVENT_TYPES = {"polls", "donations", "points"};
    private final TwitchPolls plugin;
    private final ActionManager actionManager;
    private final UIManager uiManager;
    private YamlConfiguration guiConfig;

    public TestCommand(TwitchPolls plugin, ActionManager actionManager, UIManager uiManager) {
        this.plugin = plugin;
        this.actionManager = actionManager;
        this.uiManager = uiManager;
        reload();
    }

    public void reload() {
        File file = new File(plugin.getDataFolder(), "gui.yml");
        guiConfig = YamlConfiguration.loadConfiguration(file);
    }

    public void open(Player player) {
        ConfigurationSection categories = guiConfig.getConfigurationSection("categories");
        if (categories == null) {
            player.sendMessage(color(plugin.getLanguageManager().getString(
                    "messages.gui.categories-missing", "&cNo categories configured in gui.yml.")));
            return;
        }

        int rows = Math.max(1, Math.min(6, guiConfig.getInt("category-rows", 1)));
        TestHolder holder = new TestHolder(null);
        Inventory inventory = Bukkit.createInventory(holder, rows * 9,
                color(guiConfig.getString("title", "&5Acciones")));
        holder.setInventory(inventory);

        for (String category : EVENT_TYPES) {
            ConfigurationSection itemConfig = categories.getConfigurationSection(category);
            if (itemConfig == null || !itemConfig.getBoolean("enabled", true)) {
                continue;
            }
            int slot = itemConfig.getInt("slot", -1);
            if (slot < 0 || slot >= inventory.getSize()) {
                continue;
            }
            holder.categories.put(slot, category);
            inventory.setItem(slot, createItem(itemConfig, category));
        }

        player.openInventory(inventory);
    }

    private void openCategory(Player player, String category) {
        ConfigurationSection items = getCategoryItems(category);
        if (items == null) {
            player.sendMessage(color(plugin.getLanguageManager().getString(
                    "messages.gui.actions-missing", "&cNo actions configured for %category%.")
                    .replace("%category%", category)));
            return;
        }

        int rows = Math.max(1, Math.min(6, guiConfig.getInt("rows", 4)));
        TestHolder holder = new TestHolder(category);
        Inventory inventory = Bukkit.createInventory(holder, rows * 9,
                color(guiConfig.getString("titles." + category, "&5Acciones " + category)));
        holder.setInventory(inventory);

        for (String itemKey : items.getKeys(false)) {
            ConfigurationSection itemConfig = items.getConfigurationSection(itemKey);
            if (itemConfig == null || !itemConfig.getBoolean("enabled", true)) {
                continue;
            }
            int slot = itemConfig.getInt("slot", -1);
            if (slot < 0 || slot >= inventory.getSize()) {
                continue;
            }
            String action = itemConfig.getString("event", itemKey).toUpperCase();
            if (!actionManager.getRegisteredActions().contains(action)
                    || actionManager.findActionConfig(action, category) == null) {
                continue;
            }
            holder.actions.put(slot, action);
            inventory.setItem(slot, createItem(itemConfig, category, action));
        }

        ConfigurationSection backConfig = guiConfig.getConfigurationSection("back-item");
        if (backConfig != null) {
            int backSlot = backConfig.getInt("slot", inventory.getSize() - 1);
            if (backSlot >= 0 && backSlot < inventory.getSize()) {
                holder.backSlots.put(backSlot, true);
                inventory.setItem(backSlot, createItem(backConfig));
            }
        }
        player.openInventory(inventory);
    }

    private ConfigurationSection getCategoryItems(String category) {
        if ("polls".equals(category)) {
            return guiConfig.getConfigurationSection("items");
        }
        return guiConfig.getConfigurationSection("category-items." + category);
    }

    private ItemStack createItem(ConfigurationSection config) {
        return createItem(config, null, null);
    }

    private ItemStack createItem(ConfigurationSection config, String category) {
        return createItem(config, category, null);
    }

    private ItemStack createItem(ConfigurationSection config, String category, String actionName) {
        Material material;
        try {
            material = Material.valueOf(config.getString("item", "PAPER").toUpperCase());
        } catch (IllegalArgumentException exception) {
            material = Material.PAPER;
        }

        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();

        if (meta != null) {
            meta.setDisplayName(color(config.getString("display-name", "&fAction")));
            meta.setLore(buildLore(config, category, actionName).stream().map(this::color).toList());
            item.setItemMeta(meta);
        }

        return item;
    }

    private List<String> buildLore(ConfigurationSection config, String category, String actionName) {
        List<String> lore = new ArrayList<>();
        if (config == null) {
            return lore;
        }

        List<String> configLore = config.getStringList("lore");

        if (actionName == null || category == null || "polls".equalsIgnoreCase(category)) {
            return configLore;
        }

        ConfigurationSection actionConfig = actionManager.findActionConfig(actionName, category);
        String type = "points".equalsIgnoreCase(category) ? "points" : "bits";
        String value = "0";

        if (actionConfig != null) {
            type = actionConfig.getString("type", type);
            Object rawValue = actionConfig.get("value");
            value = rawValue == null ? "0" : String.valueOf(rawValue);
        }

        for (String line : configLore) {
            lore.add(line.replace("%type%", type).replace("%value%", value));
        }

        return lore;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getView().getTopInventory().getHolder() instanceof TestHolder holder)) {
            return;
        }
        event.setCancelled(true);
        if (event.getRawSlot() < 0 || event.getRawSlot() >= event.getView().getTopInventory().getSize()) {
            return;
        }
        int slot = event.getRawSlot();
        if (holder.category == null) {
            String category = holder.categories.get(slot);
            if (category != null && event.getWhoClicked() instanceof Player player) {
                openCategory(player, category);
            }
            return;
        }
        if (holder.backSlots.containsKey(slot) && event.getWhoClicked() instanceof Player player) {
            open(player);
            return;
        }

        String action = holder.actions.get(slot);
        if (action == null || !(event.getWhoClicked() instanceof Player player)) {
            return;
        }

        player.closeInventory();
        if (!player.isOnline()) {
            return;
        }
        ConfigurationSection actionConfig = actionManager.findActionConfig(action, holder.category);
        if (actionConfig != null) {
            if ("points".equals(holder.category)) {
                uiManager.showPointEvent(player, actionConfig);
                actionManager.executePointAction(player, actionConfig);
            } else if ("donations".equals(holder.category)) {
                uiManager.showDonationEvent(player, actionConfig);
                actionManager.executeDonationAction(player, actionConfig, "unknown");
            } else {
                uiManager.testPoll(player, actionConfig, actionManager);
            }
        } else {
            player.sendMessage(color(plugin.getLanguageManager().getString(
                    "messages.gui.action-missing", "&cNo configuration found for %action%.")
                    .replace("%action%", action)));
        }
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        if (event.getView().getTopInventory().getHolder() instanceof TestHolder) {
            event.setCancelled(true);
        }
    }

    private String color(String text) {
        return ChatColor.translateAlternateColorCodes('&', text == null ? "" : text);
    }

    private static class TestHolder extends GUIHolder {
        private TestHolder(String category) { super(category); }
    }
}