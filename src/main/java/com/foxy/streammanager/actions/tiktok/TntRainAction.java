package com.foxy.streammanager.actions.tiktok;

import com.foxy.streammanager.actions.ActionContext;
import com.foxy.streammanager.actions.ActionStrategy;
import org.bukkit.Location;
import org.bukkit.entity.TNTPrimed;
import org.bukkit.scheduler.BukkitRunnable;

public class TntRainAction implements ActionStrategy {
    @Override
    public void execute(ActionContext context) {
        long scaledAmount = (long) Math.max(1, context.config().getInt("amount", 10))
                * (context.config().getBoolean("scale-with-combo", false) ? context.eventMultiplier() : 1);
        int amount = (int) Math.min(Integer.MAX_VALUE, scaledAmount);
        double height = Math.max(1.0, context.config().getDouble("height", 10.0));
        long spawnIntervalTicks = Math.max(1L, context.config().getLong("spawn-interval-ticks", 1L));
        int fuseTicks = Math.max(1, context.config().getInt("fuse-ticks", 60));

        new BukkitRunnable() {
            private int spawned;

            @Override
            public void run() {
                if (!context.player().isOnline() || spawned >= amount) {
                    cancel();
                    return;
                }

                Location location = context.player().getLocation().add(0, height, 0);
                TNTPrimed tnt = context.world().spawn(location, TNTPrimed.class);
                tnt.setFuseTicks(fuseTicks);
                spawned++;
            }
        }.runTaskTimer(context.plugin(), 0L, spawnIntervalTicks);
    }
}