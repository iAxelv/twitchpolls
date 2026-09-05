package com.foxy.twitchPolls.actions.tiktok;

import com.foxy.twitchPolls.actions.ActionContext;
import com.foxy.twitchPolls.actions.ActionStrategy;
import org.bukkit.FireworkEffect;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Firework;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.scheduler.BukkitRunnable;

public class BalloonsAction implements ActionStrategy {
    @Override
    public void execute(ActionContext context) {
        long durationTicks = Math.max(1L, context.config().getLong("duration-seconds", 10L) * 20L);
        double riseSpeed = Math.max(0.0, context.config().getDouble("rise-speed", 0.08));
        Location feet = context.player().getLocation().clone().subtract(0.0, 0.25, 0.0);
        Firework firework = context.world().spawn(feet, Firework.class);
        FireworkMeta fireworkMeta = firework.getFireworkMeta();
        fireworkMeta.setPower(1);
        fireworkMeta.addEffect(FireworkEffect.builder()
                .with(FireworkEffect.Type.BALL_LARGE)
                .withColor(org.bukkit.Color.ORANGE, org.bukkit.Color.YELLOW)
                .flicker(true)
                .trail(true)
                .build());
        firework.setFireworkMeta(fireworkMeta);
        context.world().playSound(feet, Sound.ENTITY_FIREWORK_ROCKET_LAUNCH, 1.0f, 1.0f);

        new BukkitRunnable() {
            private long ticks;

            @Override
            public void run() {
                if (!context.player().isOnline() || ticks++ >= durationTicks) {
                    cancel();
                    return;
                }

                Location current = context.player().getLocation();
                Location next = current.clone();
                next.setY(Math.min(current.getY() + riseSpeed, current.getWorld().getMaxHeight() - 1.0));
                context.player().teleport(next);
                context.player().setFallDistance(0.0f);

                Location particleLocation = next.clone().subtract(0.0, 0.25, 0.0);
                context.world().spawnParticle(Particle.FLAME, particleLocation, 5, 0.12, 0.05, 0.12, 0.01);
                context.world().spawnParticle(Particle.SMOKE, particleLocation, 3, 0.1, 0.05, 0.1, 0.01);
                context.world().spawnParticle(Particle.FIREWORK, particleLocation, 2, 0.08, 0.05, 0.08, 0.02);
            }
        }.runTaskTimer(context.plugin(), 0L, 1L);
    }
}