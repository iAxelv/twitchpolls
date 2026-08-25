package com.foxy.twitchPolls.actions.points;

import com.foxy.twitchPolls.actions.ActionContext;
import com.foxy.twitchPolls.actions.ActionStrategy;
import org.bukkit.Location;
import org.bukkit.scheduler.BukkitRunnable;

public class SpinHeadAction implements ActionStrategy {
    @Override
    public void execute(ActionContext context) {
        long duration = Math.max(1L, Math.round(context.config().getDouble("duration-seconds", 3.0) * 20.0));
        long interval = Math.max(1L, context.config().getLong("interval-ticks", 2L));
        new BukkitRunnable() {
            private long elapsed;

            @Override
            public void run() {
                if (!context.player().isValid() || elapsed >= duration) {
                    cancel();
                    return;
                }
                Location turned = context.player().getLocation();
                turned.setYaw(turned.getYaw() + (float) context.config().getDouble("degrees-per-turn", 90.0));
                context.player().teleport(turned);
                elapsed += interval;
            }
        }.runTaskTimer(context.plugin(), 0L, interval);
    }
}