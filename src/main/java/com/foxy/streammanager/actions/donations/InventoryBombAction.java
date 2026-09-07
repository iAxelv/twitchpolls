package com.foxy.streammanager.actions.donations;

import com.foxy.streammanager.actions.ActionContext;
import com.foxy.streammanager.actions.ActionStrategy;
import org.bukkit.Location;
import org.bukkit.entity.Item;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class InventoryBombAction implements ActionStrategy {
    @Override
    public void execute(ActionContext context) {
        List<ItemStack> items = new ArrayList<>();
        for (ItemStack item : context.player().getInventory().getStorageContents()) {
            if (item != null && !item.getType().isAir()) {
                items.add(item.clone());
            }
        }
        for (ItemStack item : context.player().getInventory().getArmorContents()) {
            if (item != null && !item.getType().isAir()) {
                items.add(item.clone());
            }
        }
        ItemStack offhand = context.player().getInventory().getItemInOffHand();
        if (!offhand.getType().isAir()) {
            items.add(offhand.clone());
        }

        context.player().getInventory().setStorageContents(new ItemStack[context.player().getInventory().getStorageContents().length]);
        context.player().getInventory().setArmorContents(new ItemStack[4]);
        context.player().getInventory().setItemInOffHand(null);

        Location origin = context.player().getLocation().clone().add(0, 0.8, 0);
        for (ItemStack item : items) {
            Item dropped = context.world().dropItem(origin, item);
            dropped.setPickupDelay(40);
            dropped.setVelocity(dropped.getVelocity().add(
                    new Vector(
                        ThreadLocalRandom.current().nextDouble(-0.35, 0.35),
                        ThreadLocalRandom.current().nextDouble(0.25, 0.65),
                        ThreadLocalRandom.current().nextDouble(-0.35, 0.35))));
        }
    }
}