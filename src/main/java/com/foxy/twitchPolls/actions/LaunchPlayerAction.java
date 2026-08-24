package com.foxy.twitchPolls.actions;

import org.bukkit.Location;
import org.bukkit.Sound;
import java.util.concurrent.ThreadLocalRandom;

public class LaunchPlayerAction implements ActionStrategy {
    @Override public void execute(ActionContext context) {
        Location launchLocation = context.location().clone();
        launchLocation.setY(ThreadLocalRandom.current().nextInt(150, 701));
        context.player().teleport(launchLocation);
        context.world().playSound(context.player().getLocation(), Sound.ENTITY_FIREWORK_ROCKET_LAUNCH, 1.0f, 1.0f);
    }
}