package com.foxy.streammanager.actions.points;

import com.foxy.streammanager.actions.ActionContext;
import com.foxy.streammanager.actions.ActionStrategy;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Pig;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.List;

public class PigStackAttackAction implements ActionStrategy {
    @Override
    public void execute(ActionContext context) {
        Pig bottom = null;
        List<Pig> attackers = new ArrayList<>();
        int count = Math.max(1, context.config().getInt("pigs", 4));
        double height = context.config().getDouble("height", 1.0);
        for (int index = 0; index < count; index++) {
            Pig pig = (Pig) context.world().spawnEntity(context.location().clone().add(0, 1.0 + index * height, 0), EntityType.PIG);
            attackers.add(pig);
            if (bottom != null) {
                bottom.addPassenger(pig);
            }
            bottom = pig;
        }
        final List<Pig> attackingPigs = List.copyOf(attackers);
        double damage = Math.max(0.0, context.config().getDouble("attack-damage", 2.0));
        long attackTicks = Math.max(1L, context.config().getLong("attack-duration-seconds", 10L)) * 20L;
        new BukkitRunnable() {
            private long elapsed;

            @Override
            public void run() {
                if (!context.player().isValid() || elapsed >= attackTicks
                        || attackingPigs.stream().noneMatch(pig -> pig.isValid())) {
                    cancel();
                    return;
                }
                for (Pig attackingPig : attackingPigs) {
                    if (!attackingPig.isValid()) {
                        continue;
                    }
                    attackingPig.setTarget(context.player());
                    attackingPig.getPathfinder().moveTo(context.player());
                    if (attackingPig.getLocation().distanceSquared(context.player().getLocation()) <= 4.0) {
                        context.player().damage(damage, attackingPig);
                    }
                }
                elapsed += 10L;
            }
        }.runTaskTimer(context.plugin(), 0L, 10L);
    }
}