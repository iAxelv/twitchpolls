package com.foxy.twitchPolls.actions.polls;

import com.foxy.twitchPolls.actions.ActionContext;
import com.foxy.twitchPolls.actions.ActionStrategy;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.concurrent.ThreadLocalRandom;

public class RandomWeatherAction implements ActionStrategy {
    @Override public void execute(ActionContext context) {
        int changes = Math.max(1, context.config().getInt("changes", 4));
        long interval = Math.max(1, context.config().getLong("interval-seconds", 5)) * 20L;
        long duration = Math.max(1, context.config().getLong("duration-seconds", changes * Math.max(1L, context.config().getLong("interval-seconds", 5))));
        boolean originalStorm = context.world().hasStorm();
        boolean originalThunder = context.world().isThundering();
        long originalTime = context.world().getTime();
        long deadline = System.currentTimeMillis() + duration * 1000L;
        new BukkitRunnable() {
            int remaining = changes;
            @Override public void run() {
                if (!context.player().isOnline() || remaining-- <= 0 || System.currentTimeMillis() >= deadline) {
                    context.world().setStorm(originalStorm);
                    context.world().setThundering(originalThunder);
                    context.world().setTime(originalTime);
                    cancel();
                    return;
                }
                boolean storm = ThreadLocalRandom.current().nextBoolean();
                context.world().setStorm(storm);
                context.world().setThundering(storm && ThreadLocalRandom.current().nextBoolean());
                context.world().setTime(ThreadLocalRandom.current().nextLong(24000));
            }
        }.runTaskTimer(context.plugin(), 0L, interval);
    }
}