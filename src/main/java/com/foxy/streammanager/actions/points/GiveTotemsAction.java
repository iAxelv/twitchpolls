package com.foxy.streammanager.actions.points;

import com.foxy.streammanager.actions.ActionContext;
import com.foxy.streammanager.actions.ActionStrategy;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

public class GiveTotemsAction implements ActionStrategy {

    @Override
    public void execute(ActionContext context) {
        List<Integer> amounts = context.config().getIntegerList("amounts");

        int amount;
        if (!amounts.isEmpty()) {
            amount = amounts.get(ThreadLocalRandom.current().nextInt(amounts.size()));
        } else {
            int minimum = Math.max(1, context.config().getInt("min-totems", 1));
            int maximum = Math.max(minimum, context.config().getInt("max-totems", 2));
            amount = ThreadLocalRandom.current().nextInt(minimum, maximum + 1);
        }

        for (int index = 0; index < amount; index++) {
            Map<Integer, ItemStack> leftovers = context.player().getInventory().addItem(
                new ItemStack(Material.TOTEM_OF_UNDYING, 1)
            );

            leftovers.values().forEach(item ->
                context.world().dropItemNaturally(context.player().getLocation(), item)
            );
        }
    }
}