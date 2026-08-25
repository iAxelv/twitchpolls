package com.foxy.twitchPolls.actions.points;

import com.foxy.twitchPolls.actions.ActionContext;
import com.foxy.twitchPolls.actions.ActionStrategy;
import org.bukkit.Location;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.util.Vector;

public class CleanArmorAction implements ActionStrategy {
    @Override
    public void execute(ActionContext context) {
        PlayerInventory inventory = context.player().getInventory();
        ItemStack[] armor = inventory.getArmorContents();
        inventory.setArmorContents(new ItemStack[4]);
        Vector[] directions = {new Vector(1, 0.2, 0), new Vector(-1, 0.2, 0),
                new Vector(0, 0.2, 1), new Vector(0, 0.2, -1)};
        Location origin = context.location().clone().add(0, 0.2, 0);
        for (int index = 0; index < armor.length; index++) {
            if (armor[index] != null && !armor[index].isEmpty()) {
                var item = context.world().dropItem(origin, armor[index]);
                item.setVelocity(directions[index].clone().multiply(0.35));
            }
        }
    }
}