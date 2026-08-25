package com.foxy.twitchPolls.actions.donations;

import com.foxy.twitchPolls.actions.ActionContext;
import com.foxy.twitchPolls.actions.ActionStrategy;
import org.bukkit.Location;
import org.bukkit.entity.Creeper;
import org.bukkit.entity.EntityType;

public class ChargedCreeperArmyAction implements ActionStrategy {
    @Override
    public void execute(ActionContext context) {
        int amount = Math.max(1, context.config().getInt("amount", 10));
        double radius = Math.max(0.5, context.config().getDouble("radius", 4.0));
        Location center = context.player().getLocation();
        for (int index = 0; index < amount; index++) {
            double angle = Math.PI * 2 * index / amount;
            Location location = center.clone().add(Math.cos(angle) * radius, 0, Math.sin(angle) * radius);
            Creeper creeper = (Creeper) context.world().spawnEntity(location, EntityType.CREEPER);
            creeper.setPowered(true);
            creeper.setTarget(context.player());
        }
    }
}