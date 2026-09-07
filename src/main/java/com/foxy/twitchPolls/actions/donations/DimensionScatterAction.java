package com.foxy.twitchPolls.actions.donations;

import com.foxy.twitchPolls.actions.ActionContext;
import com.foxy.twitchPolls.actions.ActionStrategy;
import org.bukkit.Location;
import org.bukkit.scheduler.BukkitRunnable;
import java.util.concurrent.ThreadLocalRandom;

public class DimensionScatterAction implements ActionStrategy {
    @Override public void execute(ActionContext context) {
        int teleports = Math.max(1, context.config().getInt("teleports", 4));
        double minDistance = Math.max(20.0, context.config().getDouble("min-distance", 100.0));
        double maxDistance = Math.max(minDistance, context.config().getDouble("max-distance", 300.0));
        new BukkitRunnable() {
            int count;
            @Override public void run() {
                if (!context.player().isOnline() || count++ >= teleports) { cancel(); return; }
                double angle = ThreadLocalRandom.current().nextDouble(Math.PI * 2);
                double distance = ThreadLocalRandom.current().nextDouble(minDistance, maxDistance);
                Location destination = context.player().getLocation().clone().add(
                        Math.cos(angle) * distance, 0, Math.sin(angle) * distance);
                destination.setY(context.world().getHighestBlockYAt(destination) + 1.0);
                context.player().teleport(destination);
            }
        }.runTaskTimer(context.plugin(), 0L,
                Math.max(1L, context.config().getLong("interval-ticks", 50L)));
    }
}
