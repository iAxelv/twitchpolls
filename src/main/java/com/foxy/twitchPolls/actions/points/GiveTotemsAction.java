package com.foxy.twitchPolls.actions.points;

import com.foxy.twitchPolls.actions.ActionContext;
import com.foxy.twitchPolls.actions.ActionStrategy;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import java.util.concurrent.ThreadLocalRandom;

public class GiveTotemsAction implements ActionStrategy {
    @Override
    public void execute(ActionContext context) {
        int minimum = Math.max(1, context.config().getInt("min-totems", 1));
        int maximum = Math.max(minimum, context.config().getInt("max-totems", 4));
        int amount = ThreadLocalRandom.current().nextInt(minimum, maximum + 1);

        for (int index = 0; index < amount; index++) {
            context.player().getInventory().addItem(new ItemStack(Material.TOTEM_OF_UNDYING, 1));
        }
    }
}