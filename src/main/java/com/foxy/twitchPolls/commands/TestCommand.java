package com.foxy.twitchPolls.commands;

import com.foxy.twitchPolls.ActionManager;
import com.foxy.twitchPolls.TwitchPolls;
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
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

public class TestCommand implements Listener {
    private final TwitchPolls plugin;
    private final ActionManager actionManager;
    private YamlConfiguration guiConfig;

    public TestCommand(TwitchPolls plugin, ActionManager actionManager) {
        this.plugin = plugin;
        this.actionManager = actionManager;
        reload();
    }

    public void reload() {
        File file = new File(plugin.getDataFolder(), "gui.yml");
        guiConfig = YamlConfiguration.loadConfiguration(file);
    }

    public void open(Player player) {
        ConfigurationSection items = guiConfig.getConfigurationSection("items");
        if (items == null) {
            player.sendMessage(ChatColor.RED + "No hay acciones configuradas en gui.yml.");
            return;
        }

        int rows = Math.max(1, Math.min(6, guiConfig.getInt("rows", 3)));
        TestHolder holder = new TestHolder();
        Inventory inventory = Bukkit.createInventory(holder, rows * 9,
                color(guiConfig.getString("title", "&5Probar acciones")));
        holder.inventory = inventory;

        for (String itemKey : items.getKeys(false)) {
            ConfigurationSection itemConfig = items.getConfigurationSection(itemKey);
            if (itemConfig == null || !itemConfig.getBoolean("enabled", true)) {
                continue;
            }
            int slot = itemConfig.getInt("slot", -1);
            if (slot < 0 || slot >= inventory.getSize()) {
                continue;
            }
            String action = itemConfig.getString("event", itemKey);
            if (!actionManager.getRegisteredActions().contains(action.toUpperCase())) {
                continue;
            }
            holder.actions.put(slot, action);
            inventory.setItem(slot, createItem(itemConfig));
        }

        player.openInventory(inventory);
    }

    private ItemStack createItem(ConfigurationSection config) {
        Material material;
        try {
            material = Material.valueOf(config.getString("item", "PAPER").toUpperCase());
        } catch (IllegalArgumentException exception) {
            material = Material.PAPER;
        }

        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(color(config.getString("display-name", "&fAcción")));
        meta.setLore(config.getStringList("lore").stream().map(this::color).toList());
        item.setItemMeta(meta);
        return item;
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
        String action = holder.actions.get(event.getRawSlot());
        if (action == null || !(event.getWhoClicked() instanceof Player player)) {
            return;
        }

        player.closeInventory();
        player.sendMessage(ChatColor.YELLOW + "Acción " + action + " en 3 segundos...");
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (!player.isOnline()) {
                return;
            }
            ConfigurationSection actionConfig = actionManager.findActionConfig(action);
            if (actionConfig != null) {
                actionManager.executeAction(player, actionConfig);
            } else {
                player.sendMessage(ChatColor.RED + "No se encontró la configuración de " + action + ".");
            }
        }, 60L);
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

    private static class TestHolder implements InventoryHolder {
        private final Map<Integer, String> actions = new HashMap<>();
        private Inventory inventory;

        @Override
        public Inventory getInventory() {
            return inventory;
        }
    }
}