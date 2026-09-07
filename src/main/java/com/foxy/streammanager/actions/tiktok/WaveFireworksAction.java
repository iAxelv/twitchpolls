package com.foxy.streammanager.actions.tiktok;

import com.foxy.streammanager.actions.ActionContext;
import com.foxy.streammanager.actions.ActionStrategy;
import org.bukkit.scheduler.BukkitRunnable;

public class WaveFireworksAction implements ActionStrategy {
    @Override
    public void execute(ActionContext context) {
        int amount = Math.max(1, context.config().getInt("amount", 10));
        long intervalTicks = Math.max(1L, context.config().getLong("interval-ticks", 10L));
        new BukkitRunnable() {
            private int strikes;

            @Override
            public void run() {
                if (!context.player().isOnline() || strikes >= amount) {
                    cancel();
                    return;
                }
                context.player().getWorld().strikeLightning(context.player().getLocation());
                strikes++;
            }
        }.runTaskTimer(context.plugin(), 0L, intervalTicks);
    }
}
