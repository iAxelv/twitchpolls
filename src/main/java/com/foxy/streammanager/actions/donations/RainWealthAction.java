package com.foxy.streammanager.actions.donations;

import com.foxy.streammanager.actions.ActionContext;
import com.foxy.streammanager.actions.ActionStrategy;
import org.bukkit.Material;
import org.bukkit.entity.Item;
import org.bukkit.inventory.ItemStack;

public class RainWealthAction implements ActionStrategy {
    private static final Material[] WEALTH = {
            Material.DIAMOND, Material.NETHERITE_INGOT,
            Material.EMERALD, Material.ENCHANTED_GOLDEN_APPLE
    };

    @Override
    public void execute(ActionContext context) {
        int amount = Math.max(1, context.config().getInt("amount", 4));
        double radius = Math.max(0.0, context.config().getDouble("radius", 5.0));
        double height = context.config().getDouble("height", 8.0);
        for (Material material : WEALTH) {
            for (int index = 0; index < amount; index++) {
                Item item = context.world().dropItem(
                        context.randomLocation(radius, height), new ItemStack(material));
                item.setPickupDelay(10);
            }
        }
    }
}