package com.foxy.twitchPolls.actions.points;

import com.foxy.twitchPolls.actions.ActionContext;
import com.foxy.twitchPolls.actions.ActionStrategy;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.scheduler.BukkitRunnable;

public class PumpkinHeadAction implements ActionStrategy {
    @Override
    public void execute(ActionContext context) {
        PlayerInventory inventory = context.player().getInventory();
        ItemStack originalHelmet = inventory.getHelmet();
        boolean storedInInventory = originalHelmet != null && !originalHelmet.isEmpty();

        if (storedInInventory) {
            inventory.addItem(originalHelmet.clone());
        }

        ItemStack pumpkin = new ItemStack(Material.CARVED_PUMPKIN);
        pumpkin.addUnsafeEnchantment(Enchantment.BINDING_CURSE, 1);
        inventory.setHelmet(pumpkin);

        long duration = Math.max(1L, context.config().getLong("duration-seconds", 10L)) * 20L;
        new BukkitRunnable() {
            @Override
            public void run() {
                if (!context.player().isOnline()) {
                    cancel();
                    return;
                }
                if (storedInInventory) {
                    inventory.removeItem(originalHelmet);
                }
                inventory.setHelmet(originalHelmet);
                cancel();
            }
        }.runTaskLater(context.plugin(), duration);
    }
}