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
        ItemStack savedHelmet = originalHelmet == null ? new ItemStack(Material.AIR) : originalHelmet.clone();
        boolean storedInInventory = !savedHelmet.isEmpty();

        if (storedInInventory) {
            inventory.addItem(savedHelmet.clone());
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
                    inventory.removeItem(savedHelmet);
                }
                inventory.setHelmet(savedHelmet);
                cancel();
            }
        }.runTaskLater(context.plugin(), duration);
    }
}