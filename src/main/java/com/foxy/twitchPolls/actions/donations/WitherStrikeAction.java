package com.foxy.twitchPolls.actions.donations;

import com.foxy.twitchPolls.actions.ActionContext;
import com.foxy.twitchPolls.actions.ActionStrategy;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.EntityType;

public class WitherStrikeAction implements ActionStrategy {
    @Override
    public void execute(ActionContext context) {
        double height = Math.max(1.0, context.config().getDouble("height", 10.0));
        Location location = context.player().getLocation().clone().add(0, height, 0);
        context.world().spawnEntity(location, EntityType.WITHER);
        context.world().playSound(location, Sound.ENTITY_WITHER_SPAWN, 1.0f, 1.0f);
    }
}