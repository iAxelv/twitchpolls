package com.foxy.twitchPolls;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Creeper;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.Sound;

import java.util.concurrent.ThreadLocalRandom;

public class ActionManager {

    public void executeAction(Player player, ConfigurationSection config) {
        if (config == null || !config.contains("action")) {
            return;
        }

        String action = config.getString("action");
        World world = player.getWorld();
        Location loc = player.getLocation();

        switch (action) {
            case "SPAWN_CREEPER":
                int creeperMin = config.getInt("spawn_min", 1);
                int creeperMax = config.getInt("spawn_max", 3);
                double creeperRadius = config.getDouble("radius", 2.0);
                int creeperAmount = ThreadLocalRandom.current().nextInt(creeperMin, creeperMax + 1);
                for (int i = 0; i < creeperAmount; i++) {
                    double angle = Math.random() * Math.PI * 2;
                    double radius = Math.random() * creeperRadius;
                    double x = Math.cos(angle) * radius;
                    double z = Math.sin(angle) * radius;
                    Location spawnLoc = loc.clone().add(x, 0, z);
                    Creeper creeper = (Creeper) world.spawnEntity(spawnLoc, EntityType.CREEPER);
                    creeper.setPowered(true);
                }
                world.playSound(loc, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 1.0f, 1.0f);
                break;
            case "EFFECT_LEVITATION":
                int timeMin = config.getInt("time_min", 100);
                int timeMax = config.getInt("time_max", 300);
                int amplifier = config.getInt("amplifier", 5);
                int durationTicks = ThreadLocalRandom.current().nextInt(timeMin, timeMax + 1);
                player.addPotionEffect(new PotionEffect(PotionEffectType.LEVITATION, durationTicks, amplifier));
                world.playSound(loc, Sound.ENTITY_ENDER_DRAGON_FLAP, 1.0f, 1.0f);
                break;
            case "DROP_DIAMONDS":
                int diamondMin = config.getInt("spawn_min", 1);
                int diamondMax = config.getInt("spawn_max", 5);
                double diamondRadius = config.getDouble("radius", 1.0);
                int diamondAmount = ThreadLocalRandom.current().nextInt(diamondMin, diamondMax + 1);
                for (int i = 0; i < diamondAmount; i++) {
                    double angle = Math.random() * Math.PI * 2;
                    double radius = Math.random() * diamondRadius;
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