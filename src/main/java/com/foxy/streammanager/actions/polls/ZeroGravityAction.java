package com.foxy.streammanager.actions.polls;

import com.foxy.streammanager.actions.ActionContext;
import com.foxy.streammanager.actions.ActionStrategy;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

public class ZeroGravityAction implements ActionStrategy {
    @Override public void execute(ActionContext context) {
        long duration = Math.max(1L, context.config().getLong("duration-seconds", 20L)) * 20L;
        boolean originalGravity = context.player().hasGravity();
        new BukkitRunnable() {
            long remaining = duration;
            @Override public void run() {
                if (!context.player().isOnline() || remaining <= 0) {
                    context.player().setGravity(originalGravity);
                    context.player().removePotionEffect(PotionEffectType.SLOW_FALLING);
                    context.player().removePotionEffect(PotionEffectType.LEVITATION);
                    cancel();
                    return;
                }
                context.player().setGravity(false);
                context.player().addPotionEffect(new PotionEffect(PotionEffectType.SLOW_FALLING, 40, 0, false, false, false));
                context.player().addPotionEffect(new PotionEffect(PotionEffectType.LEVITATION, 10, 0, false, false, false));
                remaining -= 10L;
            }
        }.runTaskTimer(context.plugin(), 0L, 10L);
    }
}
