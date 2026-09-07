package com.foxy.streammanager.actions.polls;

import com.foxy.streammanager.actions.ActionContext;
import com.foxy.streammanager.actions.ActionStrategy;

import org.bukkit.Sound;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class InventoryRandomAction implements ActionStrategy {
    @Override public void execute(ActionContext context) {
        PlayerInventory inventory = context.player().getInventory();
        List<ItemStack> items = new ArrayList<>();
        for (int i = 0; i < 36; i++) items.add(inventory.getItem(i));
        Collections.shuffle(items);
        for (int i = 0; i < 36; i++) inventory.setItem(i, items.get(i));
        context.world().playSound(context.player().getLocation(), Sound.ENTITY_ITEM_PICKUP, 1.0f, 1.0f);
    }
}