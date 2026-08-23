package com.foxy.twitchPolls;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Creeper;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.Sound;

public class ActionManager {

    public void executeAction(Player player, String action) {
        World world = player.getWorld();
        Location loc = player.getLocation();

        switch (action) {
            case "SPAWN_CREEPER":
                for (int i = 0; i < 3; i++) {
                    Creeper creeper = (Creeper) world.spawnEntity(loc.clone().add(Math.random() * 4 - 2, 0, Math.random() * 4 - 2), EntityType.CREEPER);
                    creeper.setPowered(true);
                }
                world.playSound(loc, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 1.0f, 1.0f);
                break;
            case "EFFECT_LEVITATION":
                player.addPotionEffect(new PotionEffect(PotionEffectType.LEVITATION, 200, 1));
                world.playSound(loc, Sound.ENTITY_ENDER_DRAGON_FLAP, 1.0f, 1.0f);
                break;
            case "DROP_DIAMONDS":
                for (int i = 0; i < 5; i++) {
                    world.dropItem(loc.clone().add(0, 2, 0), new ItemStack(Material.DIAMOND));
                }
                world.playSound(loc, Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 2.0f);
                break;
            case "LAUNCH_PLAYER":
                player.setVelocity(player.getVelocity().setY(2.5));
                world.playSound(loc, Sound.ENTITY_FIREWORK_ROCKET_LAUNCH, 1.0f, 1.0f);
                break;
            case "SPAWN_CHICKENS":
                for (int i = 0; i < 15; i++) {
                    world.spawnEntity(loc.clone().add(Math.random() * 6 - 3, 2, Math.random() * 6 - 3), EntityType.CHICKEN);
                }
                world.playSound(loc, Sound.ENTITY_CHICKEN_AMBIENT, 1.0f, 1.0f);
                break;
            case "EFFECT_BLINDNESS":
                player.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 300, 0));
                world.playSound(loc, Sound.AMBIENT_CAVE, 1.0f, 0.5f);
                break;
            default:
                break;
        }
    }
}