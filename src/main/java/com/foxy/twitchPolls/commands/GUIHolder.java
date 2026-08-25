package com.foxy.twitchPolls.commands;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

import java.util.HashMap;
import java.util.Map;

public abstract class GUIHolder implements InventoryHolder {
    protected final String category;
    protected final Map<Integer, String> actions = new HashMap<>();
    protected final Map<Integer, String> categories = new HashMap<>();
    protected final Map<Integer, Boolean> backSlots = new HashMap<>();
    private Inventory inventory;

    protected GUIHolder(String category) { this.category = category; }

    public void setInventory(Inventory inventory) { this.inventory = inventory; }

    @Override
    public Inventory getInventory() { return inventory; }
}