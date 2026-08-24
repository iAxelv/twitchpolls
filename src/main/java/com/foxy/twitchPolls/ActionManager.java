package com.foxy.twitchPolls;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Creeper;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.Warden;
import org.bukkit.entity.Zombie;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.Sound;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.plugin.Plugin;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class ActionManager {

    private final Plugin plugin;

    public ActionManager(Plugin plugin) {
        this.plugin = plugin;
    }

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
            case "DROP_ORES":
                int oreMin = config.getInt("spawn_min", 1);
                int oreMax = config.getInt("spawn_max", 5);
                double oreRadius = config.getDouble("radius", 1.0);
                int oreAmount = ThreadLocalRandom.current().nextInt(oreMin, oreMax + 1);
                Material[] ores = {Material.IRON_INGOT, Material.LAPIS_LAZULI, Material.REDSTONE, Material.COAL, Material.EMERALD, Material.DIAMOND};
                for (int i = 0; i < oreAmount; i++) {
                    double angle = Math.random() * Math.PI * 2;
                    double radius = Math.random() * oreRadius;
                    double x = Math.cos(angle) * radius;
                    double z = Math.sin(angle) * radius;
                    Material randomOre = ores[ThreadLocalRandom.current().nextInt(ores.length)];
                    world.dropItem(loc.clone().add(x, 1, z), new ItemStack(randomOre));
                }
                world.playSound(loc, Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 2.0f);
                break;
            case "LAUNCH_PLAYER":
                Location launchLoc = loc.clone();
                launchLoc.setY(ThreadLocalRandom.current().nextInt(150, 701));
                player.teleport(launchLoc);
                world.playSound(player.getLocation(), Sound.ENTITY_FIREWORK_ROCKET_LAUNCH, 1.0f, 1.0f);
                break;
            case "RANDOM_TELEPORT":
                double xOffset = (ThreadLocalRandom.current().nextBoolean() ? 1 : -1) * ThreadLocalRandom.current().nextDouble(5, 11);
                double yOffset = (ThreadLocalRandom.current().nextBoolean() ? 1 : -1) * ThreadLocalRandom.current().nextDouble(5, 11);
                double zOffset = (ThreadLocalRandom.current().nextBoolean() ? 1 : -1) * ThreadLocalRandom.current().nextDouble(5, 11);
                player.teleport(loc.clone().add(xOffset, yOffset, zOffset));
                world.playSound(player.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 1.0f);
                break;
            case "POTATO_PREMIUM":
                PlayerInventory inv = player.getInventory();
                List<Integer> filledSlots = new ArrayList<>();
                for (int i = 0; i < 36; i++) {
                    ItemStack item = inv.getItem(i);
                    if (item != null && item.getType() != Material.AIR) {
                        filledSlots.add(i);
                    }
                }
                if (filledSlots.isEmpty()) {
                    inv.addItem(new ItemStack(Material.POTATO));
                } else {
                    int randomSlot = filledSlots.get(ThreadLocalRandom.current().nextInt(filledSlots.size()));
                    inv.setItem(randomSlot, new ItemStack(Material.POTATO));
                }
                world.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_YES, 1.0f, 1.0f);
                break;
            case "INVENTORY_RANDOM":
                PlayerInventory invRand = player.getInventory();
                List<ItemStack> items = new ArrayList<>();
                for (int i = 0; i < 36; i++) {
                    items.add(invRand.getItem(i));
                }
                Collections.shuffle(items);
                for (int i = 0; i < 36; i++) {
                    invRand.setItem(i, items.get(i));
                }
                world.playSound(player.getLocation(), Sound.ENTITY_ITEM_PICKUP, 1.0f, 1.0f);
                break;
            case "FLOOR_IS_LAVA":
                new BukkitRunnable() {
                    int ticks = 0;
                    @Override
                    public void run() {
                        if (ticks >= 200 || !player.isOnline()) {
                            this.cancel();
                            return;
                        }
                        if (player.getLocation().getBlock().getType() != Material.WATER) {
                            player.setFireTicks(40);
                        }
                        ticks += 10;
                    }
                }.runTaskTimer(plugin, 0L, 10L);
                break;
            case "WARDEN_JUMPSCARE":
                Vector dir = loc.getDirection().normalize().multiply(-10);
                Location wardenLoc = loc.clone().add(dir);
                wardenLoc.setY(world.getHighestBlockYAt(wardenLoc));
                Warden warden = (Warden) world.spawnEntity(wardenLoc, EntityType.WARDEN);
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        if (warden.isValid()) {
                            warden.remove();
                        }
                    }
                }.runTaskLater(plugin, 100L);
                break;
            case "MAX_FOOD":
                player.setFoodLevel(20);
                player.setSaturation(20.0f);
                world.playSound(player.getLocation(), Sound.ENTITY_PLAYER_BURP, 1.0f, 1.0f);
                break;
            case "MINI_ZOMBIE":
                Zombie zombie = (Zombie) world.spawnEntity(loc, EntityType.ZOMBIE);
                zombie.setBaby();
                zombie.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, PotionEffect.INFINITE_DURATION, 1));
                break;
            case "NOTHING":
                break;
            case "RANDOM_EFFECT":
                PotionEffectType[] effects = PotionEffectType.values();
                PotionEffectType randomEffect = null;
                while (randomEffect == null) {
                    randomEffect = effects[ThreadLocalRandom.current().nextInt(effects.length)];
                }
                player.addPotionEffect(new PotionEffect(randomEffect, 100, 4));
                break;
            default:
                break;
        }
    }
}