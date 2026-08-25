package com.foxy.twitchPolls.actions.polls;

import com.foxy.twitchPolls.actions.ActionContext;
import com.foxy.twitchPolls.actions.ActionStrategy;

import org.bukkit.Sound;
import org.bukkit.entity.Creeper;
import org.bukkit.entity.EntityType;
import java.util.concurrent.ThreadLocalRandom;

public class SpawnCreeperAction implements ActionStrategy {
    @Override public void execute(ActionContext context) {
        int amount = ThreadLocalRandom.current().nextInt(context.config().getInt("spawn_min", 1), context.config().getInt("spawn_max", 3) + 1);
        double radius = context.config().getDouble("radius", 2.0);
        for (int i = 0; i < amount; i++) {
            Creeper creeper = (Creeper) context.world().spawnEntity(context.randomLocation(radius, 0), EntityType.CREEPER);
            creeper.setPowered(true);
        }
        context.world().playSound(context.location(), Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 1.0f, 1.0f);
    }
}