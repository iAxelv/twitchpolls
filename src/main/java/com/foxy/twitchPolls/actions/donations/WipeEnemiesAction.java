package com.foxy.twitchPolls.actions.donations;

import com.foxy.twitchPolls.actions.ActionContext;
import com.foxy.twitchPolls.actions.ActionStrategy;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Monster;

public class WipeEnemiesAction implements ActionStrategy {
    @Override
    public void execute(ActionContext context) {
        double radius = Math.max(0.0, context.config().getDouble("radius", 100.0));
        double radiusSquared = radius * radius;
        for (Entity entity : context.world().getEntities()) {
            if (entity instanceof Monster && entity.getLocation().distanceSquared(context.location()) <= radiusSquared) {
                entity.remove();
            }
        }
    }
}