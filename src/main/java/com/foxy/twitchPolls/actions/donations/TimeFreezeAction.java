package com.foxy.twitchPolls.actions.donations;

import com.foxy.twitchPolls.actions.ActionContext;
import com.foxy.twitchPolls.actions.ActionStrategy;
import org.bukkit.entity.LivingEntity;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class TimeFreezeAction implements ActionStrategy {
    @Override
    public void execute(ActionContext context) {
        int duration = Math.max(1, context.config().getInt("duration-seconds", 30));
        int amplifier = Math.max(0, Math.min(254, context.config().getInt("amplifier", 254)));
        new BukkitRunnable() {
            private int ticks = duration * 20;

            @Override
            public void run() {
                if (ticks-- <= 0) {
                    cancel();
                    return;
                }
                for (var entity : context.world().getEntities()) {
                    if (entity != context.player()) {
                        entity.setVelocity(entity.getVelocity().zero());
                        if (entity instanceof LivingEntity living) {
                            living.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 30, amplifier,
                                    false, false, false));
                        }
                    }
                }
            }
        }.runTaskTimer(context.plugin(), 0L, 1L);
    }
}