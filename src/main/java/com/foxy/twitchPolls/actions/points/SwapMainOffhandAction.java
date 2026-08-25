package com.foxy.twitchPolls.actions.points;

import com.foxy.twitchPolls.actions.ActionContext;
import com.foxy.twitchPolls.actions.ActionStrategy;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.inventory.PlayerInventory;

public class SwapMainOffhandAction implements ActionStrategy {
    @Override
    public void execute(ActionContext context) {
        long interval = Math.max(1L, context.config().getLong("interval-seconds", 1L)) * 20L;
        long duration = Math.max(1L, context.config().getLong("duration-seconds", 8L)) * 20L;
        new BukkitRunnable() {
            long elapsed;

            @Override
            public void run() {
                if (!context.player().isOnline() || elapsed >= duration) {
                    cancel();
                    return;
                }
                PlayerInventory inventory = context.player().getInventory();
                var main = inventory.getItemInMainHand();
                inventory.setItemInMainHand(inventory.getItemInOffHand());
                inventory.setItemInOffHand(main);
                elapsed += interval;
            }
        }.runTaskTimer(context.plugin(), interval, interval);
    }
}