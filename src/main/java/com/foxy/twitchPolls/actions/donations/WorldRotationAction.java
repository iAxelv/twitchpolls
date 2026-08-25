package com.foxy.twitchPolls.actions.donations;

import com.foxy.twitchPolls.actions.ActionContext;
import com.foxy.twitchPolls.actions.ActionStrategy;
import org.bukkit.Sound;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.concurrent.ThreadLocalRandom;

public class WorldRotationAction implements ActionStrategy {
    @Override public void execute(ActionContext context) {
        int rotations = Math.max(1, context.config().getInt("rotations", 4));
        long interval = Math.max(1, context.config().getLong("interval-seconds", 3)) * 20L;
        double radius = Math.max(2.0, context.config().getDouble("radius", 12.0));
        long durationTicks = Math.max(1L, context.config().getLong("duration-seconds",
            Math.max(1, rotations - 1) * Math.max(1L, context.config().getLong("interval-seconds", 3)))) * 20L;
        long deadline = System.currentTimeMillis() + durationTicks * 50L;
        double minimumYOffset = context.config().getDouble("y-offset-min", -10.0);
        double maximumYOffset = context.config().getDouble("y-offset-max", 10.0);
        if (maximumYOffset < minimumYOffset) {
            double swap = minimumYOffset;
            minimumYOffset = maximumYOffset;
            maximumYOffset = swap;
        }
        final double minimumY = minimumYOffset;
        final double maximumY = maximumYOffset;
        new BukkitRunnable() {
            int remaining = rotations;
            @Override public void run() {
            if (!context.player().isOnline() || remaining-- <= 0 || System.currentTimeMillis() >= deadline) { cancel(); return; }
                double yOffset = minimumY == maximumY ? minimumY
                    : ThreadLocalRandom.current().nextDouble(minimumY, maximumY);
                var target = context.randomLocation(radius, yOffset);
            target.setY(Math.max(context.world().getMinHeight() + 1.0,
                Math.min(context.world().getMaxHeight() - 2.0, target.getY())));
                target.setDirection(context.player().getLocation().getDirection());
                context.player().teleport(target);
                context.world().playSound(target, Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 0.8f);
            }
        }.runTaskTimer(context.plugin(), 0L, interval);
    }
}