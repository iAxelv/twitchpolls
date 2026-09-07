package com.foxy.twitchPolls.actions.donations;

import com.foxy.twitchPolls.actions.ActionContext;
import com.foxy.twitchPolls.actions.ActionStrategy;
import org.bukkit.Location;
import org.bukkit.entity.Fireball;
import org.bukkit.scheduler.BukkitRunnable;
import java.util.concurrent.ThreadLocalRandom;

public class HellfireRainAction implements ActionStrategy {
    @Override public void execute(ActionContext context) {
        int amount = Math.max(1, context.config().getInt("amount", 30));
        double radius = Math.max(2.0, context.config().getDouble("radius", 18.0));
        double height = Math.max(5.0, context.config().getDouble("height", 25.0));
        new BukkitRunnable() {
            int spawned;
            @Override public void run() {
                if (!context.player().isOnline() || spawned >= amount) { cancel(); return; }
                Location target = context.randomLocation(radius, 0);
                Location spawn = target.clone().add(ThreadLocalRandom.current().nextDouble(-8, 9), height, ThreadLocalRandom.current().nextDouble(-8, 9));
                Fireball fireball = context.world().spawn(spawn, Fireball.class);
                fireball.setDirection(target.toVector().subtract(spawn.toVector()).normalize());
                fireball.setYield((float) context.config().getDouble("yield", 3.0));
                fireball.setIsIncendiary(context.config().getBoolean("incendiary", true));
                fireball.setVelocity(target.toVector().subtract(spawn.toVector()).normalize().multiply(0.7));
                spawned++;
            }
        }.runTaskTimer(context.plugin(), 0L,
                Math.max(1L, context.config().getLong("spawn-interval-ticks", 2L)));
    }
}
