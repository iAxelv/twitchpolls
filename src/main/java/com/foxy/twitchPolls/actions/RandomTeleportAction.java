package com.foxy.twitchPolls.actions;

import org.bukkit.Sound;
import java.util.concurrent.ThreadLocalRandom;

public class RandomTeleportAction implements ActionStrategy {
    @Override public void execute(ActionContext context) {
        double x = (ThreadLocalRandom.current().nextBoolean() ? 1 : -1) * ThreadLocalRandom.current().nextDouble(5, 11);
        double y = (ThreadLocalRandom.current().nextBoolean() ? 1 : -1) * ThreadLocalRandom.current().nextDouble(5, 11);
        double z = (ThreadLocalRandom.current().nextBoolean() ? 1 : -1) * ThreadLocalRandom.current().nextDouble(5, 11);
        context.player().teleport(context.location().clone().add(x, y, z));
        context.world().playSound(context.player().getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 1.0f);
    }
}