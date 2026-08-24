package com.foxy.twitchPolls.actions;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Skeleton;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class SpawnVenecoAction implements ActionStrategy {
    @Override public void execute(ActionContext context) {
        Skeleton veneco = (Skeleton) context.world().spawnEntity(context.randomLocation(3, 0), EntityType.SKELETON);
        veneco.customName(Component.text("VENECO PRIME"));
        veneco.setCustomNameVisible(true);
        ItemStack bow = new ItemStack(Material.BOW);
        bow.addUnsafeEnchantment(Enchantment.POWER, 10);
        if (veneco.getEquipment() != null) veneco.getEquipment().setItemInMainHand(bow);
        veneco.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 99999, 9));
        if (context.config().getBoolean("radius", true)) Bukkit.broadcast(Component.text("Un veneco apareció en el mundo"));
    }
}