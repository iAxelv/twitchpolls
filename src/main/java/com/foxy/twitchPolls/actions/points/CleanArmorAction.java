package com.foxy.twitchPolls.actions.points;

import com.foxy.twitchPolls.actions.ActionContext;
import com.foxy.twitchPolls.actions.ActionStrategy;
import org.bukkit.Location;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.util.Vector;

import java.util.concurrent.ThreadLocalRandom;

public class CleanArmorAction implements ActionStrategy {
    @Override
    public void execute(ActionContext context) {
        PlayerInventory inventory = context.player().getInventory();
        ItemStack[] armor = inventory.getArmorContents();
        inventory.setArmorContents(new ItemStack[4]);
        Vector[] directions = {new Vector(1, 0.2, 0), new Vector(-1, 0.2, 0),
                new Vector(0, 0.2, 1), new Vector(0, 0.2, -1)};
        double minDistance = Math.max(0.0, context.config().getDouble("min-distance", 1.0));
        double maxDistance = Math.max(minDistance, context.config().getDouble("max-distance", 3.0));
        Location origin = context.location().clone().add(0, 0.2, 0);
        for (int index = 0; index < armor.length; index++) {
            if (armor[index] != null && !armor[index].isEmpty()) {
                var item = context.world().dropItem(origin, armor[index]);
                double distance = minDistance == maxDistance
                    ? minDistance
                    : ThreadLocalRandom.current().nextDouble(minDistance, maxDistance);
                double launchSpeed = Math.min(0.8, 0.25 + distance * 0.15);
                item.setVelocity(directions[index].clone().multiply(launchSpeed));
            }
        }
    }
}