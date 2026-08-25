package com.foxy.twitchPolls.actions.points;

import com.foxy.twitchPolls.actions.ActionContext;
import com.foxy.twitchPolls.actions.ActionStrategy;
import org.bukkit.attribute.Attribute;
import org.bukkit.scheduler.BukkitRunnable;

public class OneHeartChallengeAction implements ActionStrategy {
    @Override public void execute(ActionContext context) {
        double original = context.player().getHealth();
        var maxHealth = context.player().getAttribute(Attribute.MAX_HEALTH);
        if (maxHealth == null) return;
        context.player().setHealth(Math.min(2.0, maxHealth.getValue()));
        new BukkitRunnable() { @Override public void run() { if (context.player().isOnline() && !context.player().isDead()) context.player().setHealth(Math.min(original, maxHealth.getValue())); } }
                .runTaskLater(context.plugin(), Math.max(1, context.config().getInt("duration-seconds", 10)) * 20L);
    }
}