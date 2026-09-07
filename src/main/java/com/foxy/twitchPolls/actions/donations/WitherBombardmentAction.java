package com.foxy.twitchPolls.actions.donations;

import com.foxy.twitchPolls.actions.ActionContext;
import com.foxy.twitchPolls.actions.ActionStrategy;
import org.bukkit.Location;
import org.bukkit.entity.WitherSkull;
import org.bukkit.scheduler.BukkitRunnable;
import java.util.concurrent.ThreadLocalRandom;

public class WitherBombardmentAction implements ActionStrategy {
    @Override public void execute(ActionContext context) {
        int amount = Math.max(1, context.config().getInt("amount", 20));
        double height = Math.max(8.0, context.config().getDouble("height", 25.0));
        new BukkitRunnable() {
            int spawned;
            @Override public void run() {
                if (!context.player().isOnline() || spawned >= amount) { cancel(); return; }
                Location target = context.player().getLocation();
                Location spawn = target.clone().add(ThreadLocalRandom.current().nextDouble(-12, 13),
                        height + ThreadLocalRandom.current().nextDouble(0, 10),
                        ThreadLocalRandom.current().nextDouble(-12, 13));
                WitherSkull skull = context.world().spawn(spawn, WitherSkull.class);
                skull.setDirection(target.toVector().subtract(spawn.toVector()).normalize());
                skull.setYield((float) context.config().getDouble("yield", 2.0));
                skull.setIsIncendiary(false);
                spawned++;
            }
        }.runTaskTimer(context.plugin(), 0L,
                Math.max(1L, context.config().getLong("spawn-interval-ticks", 2L)));
    }
}
