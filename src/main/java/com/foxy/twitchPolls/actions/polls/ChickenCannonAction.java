package com.foxy.twitchPolls.actions.polls;

import com.foxy.twitchPolls.actions.ActionContext;
import com.foxy.twitchPolls.actions.ActionStrategy;
import org.bukkit.Location;
import org.bukkit.entity.Chicken;
import org.bukkit.entity.EntityType;
import org.bukkit.util.Vector;

import java.util.concurrent.ThreadLocalRandom;

public class ChickenCannonAction implements ActionStrategy {
    @Override public void execute(ActionContext context) {
        int amount = Math.max(1, context.config().getInt("amount", 5));
        double speed = Math.max(0.1, context.config().getDouble("speed", 1.2));
        for (int index = 0; index < amount; index++) {
            Location location = context.location().clone().add(0, 1.0, 0);
            Chicken chicken = (Chicken) context.world().spawnEntity(location, EntityType.CHICKEN);
            Vector direction = new Vector(ThreadLocalRandom.current().nextDouble(-0.8, 0.8),
                    ThreadLocalRandom.current().nextDouble(0.4, 1.0),
                    ThreadLocalRandom.current().nextDouble(-0.8, 0.8)).normalize();
            chicken.setVelocity(direction.multiply(speed));
        }
    }
}