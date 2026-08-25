package com.foxy.twitchPolls.actions.donations;

import com.foxy.twitchPolls.actions.ActionContext;
import com.foxy.twitchPolls.actions.ActionStrategy;
import org.bukkit.Particle;
import org.bukkit.entity.Entity;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

public class BlackHoleAction implements ActionStrategy {
    @Override public void execute(ActionContext context) {
        double radius = Math.max(2.0, context.config().getDouble("radius", 12.0));
        double strength = Math.max(0.01, context.config().getDouble("strength", 0.15));
        int duration = Math.max(1, context.config().getInt("duration-seconds", 10));
        new BukkitRunnable() {
            int ticks = duration * 20;
            @Override public void run() {
                if (!context.player().isOnline() || ticks-- <= 0) { cancel(); return; }
                var center = context.player().getLocation().clone().add(0, 1, 0);
                context.world().spawnParticle(Particle.PORTAL, center, 20, 0.8, 0.8, 0.8, 0.1);
                for (Entity entity : context.world().getNearbyEntities(center, radius, radius, radius)) {
                    if (entity.equals(context.player()) || entity instanceof org.bukkit.entity.Item) continue;
                    Vector pull = center.toVector().subtract(entity.getLocation().toVector()).normalize().multiply(strength);
                    entity.setVelocity(entity.getVelocity().add(pull));
                }
            }
        }.runTaskTimer(context.plugin(), 0L, 1L);
    }
}