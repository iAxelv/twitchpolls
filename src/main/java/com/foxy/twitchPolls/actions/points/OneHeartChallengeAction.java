package com.foxy.twitchPolls.actions.points;

import com.foxy.twitchPolls.actions.ActionContext;
import com.foxy.twitchPolls.actions.ActionStrategy;
import org.bukkit.scheduler.BukkitRunnable;

public class OneHeartChallengeAction implements ActionStrategy {
    @Override public void execute(ActionContext context) {
        double original = context.player().getHealth();
        context.player().setHealth(Math.min(2.0, context.player().getMaxHealth()));
        new BukkitRunnable() { @Override public void run() { if (context.player().isOnline() && !context.player().isDead()) context.player().setHealth(Math.min(original, context.player().getMaxHealth())); } }
                .runTaskLater(context.plugin(), Math.max(1, context.config().getInt("duration-seconds", 10)) * 20L);
    }
}