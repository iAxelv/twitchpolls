package com.foxy.streammanager.actions.points;

import com.foxy.streammanager.actions.ActionContext;
import com.foxy.streammanager.actions.ActionStrategy;
import org.bukkit.Sound;
import org.bukkit.entity.Item;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

public class ClumsyOffhandAction implements ActionStrategy {
    @Override public void execute(ActionContext context) {
        ItemStack item = context.player().getInventory().getItemInOffHand();
        if (item.getType().isAir()) return;
        Item dropped = context.world().dropItem(context.player().getEyeLocation(), item.clone());
        context.player().getInventory().setItemInOffHand(null);
        Vector velocity = context.player().getLocation().getDirection().normalize().multiply(
                Math.max(0.4, context.config().getDouble("forward-speed", 0.8)));
        velocity.setY(Math.max(0.2, context.config().getDouble("upward-speed", 0.65)));
        dropped.setVelocity(velocity);
        context.world().playSound(context.player().getLocation(), Sound.ENTITY_ITEM_PICKUP, 1.0f, 0.6f);
    }
}
