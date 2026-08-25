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
        new BukkitRunnable() {
            int remaining = rotations;
            @Override public void run() {
                if (!context.player().isOnline() || remaining-- <= 0) { cancel(); return; }
                var target = context.randomLocation(radius, 0);
                target.setY(context.world().getHighestBlockYAt(target) + 1.0);
                target.setDirection(context.player().getLocation().getDirection());
                context.player().teleport(target);
                context.world().playSound(target, Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 0.8f);
            }
        }.runTaskTimer(context.plugin(), 0L, interval);
    }
}