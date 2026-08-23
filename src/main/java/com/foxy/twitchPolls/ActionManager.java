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

import java.util.concurrent.ThreadLocalRandom;

public class ActionManager {

    public void executeAction(Player player, String action) {
        World world = player.getWorld();
        Location loc = player.getLocation();

        switch (action) {
            case "SPAWN_CREEPER":
                int creeperAmount = ThreadLocalRandom.current().nextInt(1, 4);
                for (int i = 0; i < creeperAmount; i++) {
                    double angle = Math.random() * Math.PI * 2;
                    double radius = Math.random() * 2;
                    double x = Math.cos(angle) * radius;
                    double z = Math.sin(angle) * radius;
                    Location spawnLoc = loc.clone().add(x, 0, z);
                    Creeper creeper = (Creeper) world.spawnEntity(spawnLoc, EntityType.CREEPER);
                    creeper.setPowered(true);
                }
                world.playSound(loc, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 1.0f, 1.0f);
                break;
            case "EFFECT_LEVITATION":
                int durationTicks = ThreadLocalRandom.current().nextInt(100, 301);
                int amplifier = ThreadLocalRandom.current().nextInt(0, 6);
                player.addPotionEffect(new PotionEffect(PotionEffectType.LEVITATION, durationTicks, amplifier));
                world.playSound(loc, Sound.ENTITY_ENDER_DRAGON_FLAP, 1.0f, 1.0f);
                break;
            case "DROP_DIAMONDS":
                int diamondAmount = ThreadLocalRandom.current().nextInt(1, 5);
                for (int i = 0; i < diamondAmount; i++) {
                    double angle = Math.random() * Math.PI * 2;
                    double radius = Math.random() * 1;
                    double x = Math.cos(angle) * radius;
                    double z = Math.sin(angle) * radius;
                    world.dropItem(loc.clone().add(x, 1, z), new ItemStack(Material.DIAMOND));
                }
                world.playSound(loc, Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 2.0f);
                break;
            case "LAUNCH_PLAYER":
                Location launchLoc = loc.clone();
                launchLoc.setY(150);
                player.teleport(launchLoc);
                world.playSound(player.getLocation(), Sound.ENTITY_FIREWORK_ROCKET_LAUNCH, 1.0f, 1.0f);
                break;
            default:
                break;
        }
    }
}