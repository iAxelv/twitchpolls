package com.foxy.twitchPolls.actions.donations;

import com.foxy.twitchPolls.actions.ActionContext;
import com.foxy.twitchPolls.actions.ActionStrategy;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.EnderDragon;
import org.bukkit.entity.EntityType;

public class DragonAttackAction implements ActionStrategy {
    @Override
    public void execute(ActionContext context) {
        World overworld = Bukkit.getWorlds().stream()
                .filter(world -> world.getEnvironment() == World.Environment.NORMAL)
                .findFirst().orElse(context.world());
        Location origin = context.player().getLocation().clone();
        origin.setWorld(overworld);
        origin.add(0, Math.max(1.0, context.config().getDouble("height", 20.0)), 0);
        EnderDragon dragon = (EnderDragon) overworld.spawnEntity(origin, EntityType.ENDER_DRAGON);
        long duration = Math.max(1L, context.config().getLong("duration-seconds", 300L)) * 20L;
        Bukkit.getScheduler().runTaskLater(context.plugin(), () -> {
            if (dragon.isValid()) {
                dragon.remove();
            }
        }, duration);
    }
}