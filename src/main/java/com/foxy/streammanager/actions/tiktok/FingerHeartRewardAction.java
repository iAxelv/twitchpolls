package com.foxy.streammanager.actions.tiktok;

import com.foxy.streammanager.actions.ActionContext;
import com.foxy.streammanager.actions.ActionStrategy;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

public class FingerHeartRewardAction implements ActionStrategy {
    @Override public void execute(ActionContext context) {
        int multiplier = Math.max(1, context.eventMultiplier());
        int totems = Math.max(1, context.config().getInt("totems", 2)) * multiplier;
        int apples = Math.max(1, context.config().getInt("notch-apples", 2)) * multiplier;
        giveItems(context, Material.TOTEM_OF_UNDYING, totems);
        giveItems(context, Material.ENCHANTED_GOLDEN_APPLE, apples);
    }

    private void giveItems(ActionContext context, Material material, int amount) {
        for (int index = 0; index < amount; index++) {
            context.player().getInventory().addItem(new ItemStack(material)).values().forEach(item ->
                    context.world().dropItemNaturally(context.player().getLocation(), item));
        }
    }
}