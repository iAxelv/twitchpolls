package com.foxy.twitchPolls.actions.polls;

import com.foxy.twitchPolls.TwitchPolls;
import com.foxy.twitchPolls.actions.ActionContext;
import com.foxy.twitchPolls.actions.ActionStrategy;

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
        TwitchPolls plugin = (TwitchPolls) context.plugin();
        Skeleton veneco = (Skeleton) context.world().spawnEntity(context.randomLocation(3, 0), EntityType.SKELETON);
        String venecoName = plugin.getLanguageManager().getString("messages.spawn-veneco-name", "VENECO PRIME");
        veneco.customName(Component.text(venecoName));
        veneco.setCustomNameVisible(true);
        ItemStack bow = new ItemStack(Material.BOW);
        bow.addUnsafeEnchantment(Enchantment.POWER, 10);
        if (veneco.getEquipment() != null) veneco.getEquipment().setItemInMainHand(bow);
        veneco.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 99999, 9));
        if (context.config().getBoolean("radius", true)) {
            String message = plugin.getLanguageManager().getString("messages.spawn-veneco-broadcast", "A veneco appeared in the world");
            Bukkit.broadcast(Component.text(message));
        }
    }
}