package com.foxy.streammanager.actions.points;

import com.foxy.streammanager.actions.ActionContext;
import com.foxy.streammanager.actions.ActionStrategy;
import org.bukkit.attribute.Attribute;
import org.bukkit.scheduler.BukkitRunnable;

public class OneHeartChallengeAction implements ActionStrategy {
    @Override public void execute(ActionContext context) {
        double original = context.player().getHealth();
        float originalSaturation = context.player().getSaturation();
        float originalExhaustion = context.player().getExhaustion();
        var maxHealth = context.player().getAttribute(Attribute.MAX_HEALTH);
        if (maxHealth == null) return;
        context.player().setHealth(Math.min(2.0, maxHealth.getValue()));
        int duration = Math.max(1, context.config().getInt("duration-seconds", 10));
        new BukkitRunnable() {
            int ticks = duration * 20;
            @Override public void run() {
                if (!context.player().isOnline() || context.player().isDead() || ticks-- <= 0) {
                    if (context.player().isOnline() && !context.player().isDead()) {
                        context.player().setHealth(Math.min(original, maxHealth.getValue()));
                        context.player().setSaturation(originalSaturation);
                        context.player().setExhaustion(originalExhaustion);
                    }
                    cancel();
                    return;
                }
                context.player().setSaturation(0.0f);
                context.player().setHealth(Math.min(2.0, maxHealth.getValue()));
            }
        }.runTaskTimer(context.plugin(), 0L, 1L);
    }
}