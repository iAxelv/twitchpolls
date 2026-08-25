package com.foxy.twitchPolls.actions.points;

import com.foxy.twitchPolls.actions.ActionContext;
import com.foxy.twitchPolls.actions.ActionStrategy;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.concurrent.ThreadLocalRandom;

public class RandomSlotSwitchAction implements ActionStrategy {
    @Override public void execute(ActionContext context) {
        long interval = Math.max(1L, context.config().getLong("interval-seconds", 2)) * 20L;
        int duration = Math.max(1, context.config().getInt("duration-seconds", 12));
        new BukkitRunnable() {
            int remaining = duration;
            @Override public void run() {
                if (!context.player().isOnline() || remaining-- <= 0) { cancel(); return; }
                context.player().getInventory().setHeldItemSlot(ThreadLocalRandom.current().nextInt(9));
            }
        }.runTaskTimer(context.plugin(), 0L, interval);
    }
}