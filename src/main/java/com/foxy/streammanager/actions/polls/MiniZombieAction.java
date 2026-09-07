package com.foxy.streammanager.actions.polls;

import com.foxy.streammanager.actions.ActionContext;
import com.foxy.streammanager.actions.ActionStrategy;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Zombie;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class MiniZombieAction implements ActionStrategy {
    @Override public void execute(ActionContext context) {
        Zombie zombie = (Zombie) context.world().spawnEntity(context.location(), EntityType.ZOMBIE);
        zombie.setBaby();
        zombie.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 999999, 1));
        String streamerName = context.streamerUsername();
        zombie.customName(Component.text(streamerName));
        zombie.setCustomNameVisible(true);
        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) head.getItemMeta();
        if (meta != null) { meta.setOwningPlayer(Bukkit.getOfflinePlayer(streamerName)); head.setItemMeta(meta); }
        if (zombie.getEquipment() != null) zombie.getEquipment().setHelmet(head);
    }
}