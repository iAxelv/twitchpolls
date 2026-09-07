package com.foxy.streammanager.actions.tiktok;

import com.foxy.streammanager.actions.ActionContext;
import com.foxy.streammanager.actions.ActionStrategy;
import org.bukkit.Location;
import org.bukkit.entity.Creeper;
import org.bukkit.entity.EntityType;

public class ChargedCreeperLikeAction implements ActionStrategy {
    @Override
    public void execute(ActionContext context) {
        int amount = Math.max(1, context.config().getInt("amount", 10));
        for (int index = 0; index < amount; index++) {
            Location location = context.player().getLocation();
            Creeper creeper = (Creeper) context.world().spawnEntity(location, EntityType.CREEPER);
            creeper.setPowered(true);
            creeper.setTarget(context.player());
        }
    }
}