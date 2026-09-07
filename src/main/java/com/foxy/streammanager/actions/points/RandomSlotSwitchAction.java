package com.foxy.streammanager.actions.points;

import com.foxy.streammanager.actions.ActionContext;
import com.foxy.streammanager.actions.ActionStrategy;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.concurrent.ThreadLocalRandom;

public class RandomSlotSwitchAction implements ActionStrategy {
    @Override public void execute(ActionContext context) {
        long interval = Math.max(1L, context.config().getLong("interval-seconds", 2)) * 20L;
        long durationTicks = Math.max(1L, context.config().getLong("duration-seconds", 12)) * 20L;
        long deadline = System.currentTimeMillis() + durationTicks * 50L;
        new BukkitRunnable() {
            @Override public void run() {
                if (!context.player().isOnline() || System.currentTimeMillis() >= deadline) { cancel(); return; }
                context.player().getInventory().setHeldItemSlot(ThreadLocalRandom.current().nextInt(9));
            }
        }.runTaskTimer(context.plugin(), 0L, interval);
    }
}