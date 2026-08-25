package com.foxy.twitchPolls.actions.points;

import com.foxy.twitchPolls.actions.ActionContext;
import com.foxy.twitchPolls.actions.ActionStrategy;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Pig;

public class PigStackAttackAction implements ActionStrategy {
    @Override
    public void execute(ActionContext context) {
        Pig bottom = null;
        int count = Math.max(1, context.config().getInt("pigs", 4));
        double height = context.config().getDouble("height", 1.0);
        for (int index = 0; index < count; index++) {
            Pig pig = (Pig) context.world().spawnEntity(context.location().clone().add(0, 1.0 + index * height, 0), EntityType.PIG);
            if (bottom != null) {
                bottom.addPassenger(pig);
            }
            bottom = pig;
        }
    }
}