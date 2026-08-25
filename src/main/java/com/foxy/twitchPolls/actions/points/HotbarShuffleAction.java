package com.foxy.twitchPolls.actions.points;

import com.foxy.twitchPolls.actions.ActionContext;
import com.foxy.twitchPolls.actions.ActionStrategy;
import org.bukkit.Sound;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class HotbarShuffleAction implements ActionStrategy {
    @Override
    public void execute(ActionContext context) {
        PlayerInventory inventory = context.player().getInventory();
        List<ItemStack> hotbar = new ArrayList<>();
        for (int slot = 0; slot < 9; slot++) {
            hotbar.add(inventory.getItem(slot));
        }
        Collections.shuffle(hotbar);
        for (int slot = 0; slot < 9; slot++) {
            inventory.setItem(slot, hotbar.get(slot));
        }
        context.player().playSound(context.player().getLocation(), Sound.ENTITY_ITEM_PICKUP, 1.0f, 0.7f);
    }
}