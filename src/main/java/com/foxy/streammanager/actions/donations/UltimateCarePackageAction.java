package com.foxy.streammanager.actions.donations;

import com.foxy.streammanager.actions.ActionContext;
import com.foxy.streammanager.actions.ActionStrategy;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;

public class UltimateCarePackageAction implements ActionStrategy {
    @Override
    public void execute(ActionContext context) {
        var inventory = context.player().getInventory();
        inventory.setHelmet(enchant(new ItemStack(Material.NETHERITE_HELMET), Enchantment.PROTECTION, 4,
                Enchantment.RESPIRATION, 3, Enchantment.AQUA_AFFINITY, 1));
        inventory.setChestplate(enchant(new ItemStack(Material.NETHERITE_CHESTPLATE), Enchantment.PROTECTION, 4));
        inventory.setLeggings(enchant(new ItemStack(Material.NETHERITE_LEGGINGS), Enchantment.PROTECTION, 4));
        inventory.setBoots(enchant(new ItemStack(Material.NETHERITE_BOOTS), Enchantment.PROTECTION, 4,
                Enchantment.FEATHER_FALLING, 4, Enchantment.DEPTH_STRIDER, 3, Enchantment.SOUL_SPEED, 3));
        ItemStack sword = enchant(new ItemStack(Material.NETHERITE_SWORD), Enchantment.SHARPNESS, 5);
        inventory.addItem(sword);
        int totems = Math.max(1, context.config().getInt("totems", 5));
        inventory.addItem(new ItemStack(Material.TOTEM_OF_UNDYING, totems));
    }

    private ItemStack enchant(ItemStack item, Object... enchantments) {
        for (int index = 0; index < enchantments.length; index += 2) {
            item.addEnchantment((Enchantment) enchantments[index], (Integer) enchantments[index + 1]);
        }
        return item;
    }
}