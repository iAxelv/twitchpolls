package com.foxy.twitchPolls.actions.points;

import com.foxy.twitchPolls.actions.ActionContext;
import com.foxy.twitchPolls.actions.ActionStrategy;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Pig;
import org.bukkit.scheduler.BukkitRunnable;

public class PigStackAttackAction implements ActionStrategy {
    @Override
    public void execute(ActionContext context) {
        Pig bottom = null;
        Pig attacker = null;
        int count = Math.max(1, context.config().getInt("pigs", 4));
        double height = context.config().getDouble("height", 1.0);
        for (int index = 0; index < count; index++) {
            Pig pig = (Pig) context.world().spawnEntity(context.location().clone().add(0, 1.0 + index * height, 0), EntityType.PIG);
            if (attacker == null) {
                attacker = pig;
            }
            if (bottom != null) {
                bottom.addPassenger(pig);
            }
            bottom = pig;
        }
        final Pig attackingPig = attacker;
        attackingPig.setTarget(context.player());
        double damage = Math.max(0.0, context.config().getDouble("attack-damage", 2.0));
        long attackTicks = Math.max(1L, context.config().getLong("attack-duration-seconds", 10L)) * 20L;
        new BukkitRunnable() {
            private long elapsed;

            @Override
            public void run() {
                if (!attackingPig.isValid() || !context.player().isValid() || elapsed >= attackTicks) {
                    cancel();
                    return;
                }
                attackingPig.setTarget(context.player());
                attackingPig.getPathfinder().moveTo(context.player());
                if (attackingPig.getLocation().distanceSquared(context.player().getLocation()) <= 4.0) {
                    context.player().damage(damage, attackingPig);
                    context.player().setVelocity(context.player().getVelocity().add(
                            context.player().getLocation().toVector().subtract(attackingPig.getLocation().toVector())
                                    .normalize().multiply(0.15)));
                }
                elapsed += 10L;
            }
        }.runTaskTimer(context.plugin(), 0L, 10L);
    }
}