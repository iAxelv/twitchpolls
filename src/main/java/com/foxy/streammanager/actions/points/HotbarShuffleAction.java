package com.foxy.streammanager.actions.points;

import com.foxy.streammanager.actions.ActionContext;
import com.foxy.streammanager.actions.ActionStrategy;
import org.bukkit.Sound;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class HotbarShuffleAction implements ActionStrategy {
    @Override
    public void execute(ActionContext context) {
        PlayerInventory inventory = context.player().getInventory();
        long duration = Math.max(1L, context.config().getLong("duration-seconds", 15)) * 20L;
        long interval = Math.max(1L, context.config().getLong("interval-seconds", 1)) * 20L;
        new BukkitRunnable() {
            long remaining = duration;
            @Override public void run() {
                if (!context.player().isOnline() || remaining <= 0) { cancel(); return; }
                List<ItemStack> hotbar = new ArrayList<>();
                for (int slot = 0; slot < 9; slot++) hotbar.add(inventory.getItem(slot));
                Collections.shuffle(hotbar);
                for (int slot = 0; slot < 9; slot++) inventory.setItem(slot, hotbar.get(slot));
                context.player().playSound(context.player().getLocation(), Sound.ENTITY_ITEM_PICKUP, 1.0f, 0.7f);
                remaining -= interval;
            }
        }.runTaskTimer(context.plugin(), 0L, interval);
    }
}