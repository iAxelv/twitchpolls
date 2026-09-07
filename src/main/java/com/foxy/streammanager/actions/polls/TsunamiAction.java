package com.foxy.streammanager.actions.polls;

import com.foxy.streammanager.actions.ActionContext;
import com.foxy.streammanager.actions.ActionStrategy;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class TsunamiAction implements ActionStrategy {
    @Override
    public void execute(ActionContext context) {
        Player player = context.player();
        Location origin = player.getLocation().clone();
        Vector travel = origin.getDirection().setY(0).normalize();
        if (travel.lengthSquared() < 0.01) {
            travel = new Vector(0, 0, 1);
        }
        Vector side = new Vector(-travel.getZ(), 0, travel.getX());

        int durationSeconds = Math.max(3, context.config().getInt("duration-seconds", 10));
        int waveHeight = Math.max(2, Math.min(8, context.config().getInt("wave-height", 6)));
        int waveWidth = Math.max(4, Math.min(24, context.config().getInt("wave-width", 16)));
        double waveSpeed = Math.max(0.2, Math.min(4.0, context.config().getDouble("wave-speed", 1.2)));
        double pushStrength = Math.max(0.4, Math.min(3.5, context.config().getDouble("push-strength", 1.8)));
        double damage = Math.max(0.0, Math.min(10.0, context.config().getDouble("damage", 1.5)));
        int damageIntervalTicks = Math.max(10, context.config().getInt("damage-interval-ticks", 20));
        boolean clearWater = context.config().getBoolean("clear-water-on-end", true);
        Map<Block, Material> placedWater = new HashMap<>();
        Map<UUID, Integer> lastDamageTicks = new HashMap<>();

        player.showTitle(net.kyori.adventure.title.Title.title(
                net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer.legacyAmpersand()
                        .deserialize("&9&lTSUNAMI"),
                net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer.legacyAmpersand()
                        .deserialize("&b&lBusca altura de inmediato!"),
                net.kyori.adventure.title.Title.Times.times(
                        java.time.Duration.ofMillis(250), java.time.Duration.ofSeconds(3), java.time.Duration.ofMillis(500))));
        player.playSound(player.getLocation(), Sound.ENTITY_ENDER_DRAGON_GROWL, 1.0f, 0.55f);

        final Vector direction = travel;
        final Vector widthDirection = side;
        new BukkitRunnable() {
            int elapsedTicks;
            final int totalTicks = durationSeconds * 20;

            @Override
            public void run() {
                if (!player.isOnline() || elapsedTicks >= totalTicks) {
                    clearPlacedWater(placedWater, clearWater);
                    cancel();
                    return;
                }

                if (elapsedTicks % 2 == 0) {
                    renderWave(context.world(), origin, direction, widthDirection,
                            elapsedTicks / 20.0 * waveSpeed, waveHeight, waveWidth,
                            pushStrength, damage, damageIntervalTicks, elapsedTicks,
                            placedWater, lastDamageTicks);
                }
                elapsedTicks += 1;
            }
        }.runTaskTimer(context.plugin(), 0L, 1L);
    }

    private void renderWave(World world, Location origin, Vector direction, Vector side,
                            double travelled, int height, int width, double pushStrength,
                            double damage, int damageIntervalTicks, int elapsedTicks,
                            Map<Block, Material> placedWater, Map<UUID, Integer> lastDamageTicks) {
        double frontDistance = -12.0 + travelled;
        Location front = origin.clone().add(direction.clone().multiply(frontDistance));
        int halfWidth = width / 2;

        for (int horizontal = -halfWidth; horizontal <= halfWidth; horizontal++) {
            Location column = front.clone().add(side.clone().multiply(horizontal));
            int groundY = findGroundY(world, column.getBlockX(), column.getBlockZ(), origin.getBlockY());
            for (int vertical = 0; vertical < height; vertical++) {
                Block block = world.getBlockAt(column.getBlockX(), groundY + 1 + vertical, column.getBlockZ());
                if (block.getType().isAir()) {
                    placedWater.putIfAbsent(block, block.getType());
                    block.setType(Material.WATER, false);
                }
            }
        }

        world.spawnParticle(Particle.SPLASH, front.clone().add(0, height * 0.55, 0),
                Math.min(80, width * 3), width * 0.35, height * 0.35, 0.35, 0.08);
        world.spawnParticle(Particle.BUBBLE, front.clone().add(0, height * 0.4, 0),
                Math.min(50, width * 2), width * 0.35, height * 0.45, 0.35, 0.04);
        world.spawnParticle(Particle.BUBBLE_COLUMN_UP, front.clone().add(0, 0.6, 0),
                Math.min(30, width), width * 0.35, 0.35, 0.35, 0.02);

        for (Entity entity : world.getNearbyEntities(front, halfWidth + 2.0, height + 1.5, 2.5)) {
            if (entity.getType() == EntityType.ARMOR_STAND || entity.isDead()) {
                continue;
            }
            Location entityLocation = entity.getLocation();
            int groundY = findGroundY(world, entityLocation.getBlockX(), entityLocation.getBlockZ(), origin.getBlockY());
            if (entityLocation.getY() > groundY + height + 0.5 || isSheltered(entityLocation, direction)) {
                continue;
            }

            entity.setVelocity(entity.getVelocity().add(direction.clone().multiply(pushStrength)));
            if (entity instanceof Player target) {
                target.addPotionEffect(new PotionEffect(PotionEffectType.MINING_FATIGUE, 70, 1, false, false, true));
                target.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 70, 1, false, false, true));
                target.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 35, 0, false, false, true));
                if (target.isInWater()) {
                    target.setRemainingAir(Math.max(0, target.getRemainingAir() - 3));
                }
                if (damage > 0.0 && elapsedTicks - lastDamageTicks.getOrDefault(target.getUniqueId(), -damageIntervalTicks)
                        >= damageIntervalTicks) {
                    target.damage(damage);
                    lastDamageTicks.put(target.getUniqueId(), elapsedTicks);
                }
            }
        }
    }

    private int findGroundY(World world, int x, int z, int referenceY) {
        int startY = Math.min(world.getMaxHeight() - 1, referenceY + 3);
        for (int y = startY; y >= world.getMinHeight(); y--) {
            if (world.getBlockAt(x, y, z).getType().isSolid()) {
                return y;
            }
        }
        return world.getMinHeight();
    }

    private void clearPlacedWater(Map<Block, Material> placedWater, boolean clearWater) {
        if (!clearWater) {
            return;
        }
        placedWater.forEach((block, material) -> {
            if (block.getType() == Material.WATER) {
                block.setType(material, false);
            }
        });
        placedWater.clear();
    }

    private boolean isSheltered(Location entityLocation, Vector direction) {
        World world = entityLocation.getWorld();
        if (world == null) {
            return true;
        }
        for (int step = 1; step <= 3; step++) {
            Location check = entityLocation.clone().subtract(direction.clone().multiply(step));
            Material material = world.getBlockAt(check.getBlockX(), entityLocation.getBlockY() + 1, check.getBlockZ()).getType();
            if (material.isSolid()) {
                return true;
            }
        }
        return false;
    }
}