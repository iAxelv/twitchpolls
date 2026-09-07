package com.foxy.streammanager.actions.polls;

import com.foxy.streammanager.actions.ActionContext;
import com.foxy.streammanager.actions.ActionStrategy;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.FallingBlock;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.concurrent.ThreadLocalRandom;

public class TornadoAction implements ActionStrategy {
    @Override
    public void execute(ActionContext context) {
        int durationSeconds = Math.max(1, context.config().getInt("duration-seconds", 30));
        double radius = Math.max(3.0, context.config().getDouble("radius", 12.0));
        double pullStrength = Math.max(0.12, context.config().getDouble("pull-strength", 0.7));
        int blocksPerTick = Math.max(8, context.config().getInt("blocks-per-tick", 36));
        boolean particles = context.config().getBoolean("particles", true);

        Location streamerLocation = context.player().getLocation().clone();
        double angle = ThreadLocalRandom.current().nextDouble(Math.PI * 2);
        double distance = ThreadLocalRandom.current().nextDouble(4.0, radius);
        Location center = streamerLocation.clone().add(
                Math.cos(angle) * distance,
                0.0,
                Math.sin(angle) * distance
        );
        center.setY(Math.max(context.world().getMinHeight() + 2, streamerLocation.getY() + 2.0));

        new BukkitRunnable() {
            int elapsedTicks = 0;
            int totalTicks = durationSeconds * 20;
            double driftX = ThreadLocalRandom.current().nextDouble(-1.6, 1.6);
            double driftZ = ThreadLocalRandom.current().nextDouble(-1.6, 1.6);
            int driftCooldown = 0;

            @Override
            public void run() {
                if (!context.player().isOnline() || elapsedTicks >= totalTicks) {
                    cancel();
                    return;
                }

                World world = context.world();
                if (driftCooldown-- <= 0) {
                    driftCooldown = ThreadLocalRandom.current().nextInt(18, 42);
                    driftX = ThreadLocalRandom.current().nextDouble(-1.8, 1.8);
                    driftZ = ThreadLocalRandom.current().nextDouble(-1.8, 1.8);
                }

                center.add(driftX * 0.18, 0, driftZ * 0.18);
                double maxDistance = radius * 1.4;
                double dx = center.getX() - streamerLocation.getX();
                double dz = center.getZ() - streamerLocation.getZ();
                double distance = Math.hypot(dx, dz);
                if (distance > maxDistance) {
                    double scale = maxDistance / distance;
                    center.setX(streamerLocation.getX() + dx * scale);
                    center.setZ(streamerLocation.getZ() + dz * scale);
                }

                int groundY = world.getHighestBlockYAt(center.getBlockX(), center.getBlockZ());
                int baseY = Math.max(world.getMinHeight() + 2, groundY + 1);
                double groundAnchor = baseY + 2.2;
                center.setY(Math.max(baseY + 0.8, Math.min(center.getY(), groundAnchor + 1.8)));
                if (particles) {
                    for (int i = 0; i < 20; i++) {
                        double spiralAngle = (elapsedTicks * 0.6) + (i * 0.8);
                        double ringRadius = 2.2 + i * 0.24;
                        double x = center.getX() + Math.cos(spiralAngle) * ringRadius;
                        double z = center.getZ() + Math.sin(spiralAngle) * ringRadius;
                        double heightSpread = Math.max(2.0, 7.0 - Math.abs((i - 10) * 0.55));
                        double y = Math.max(baseY, center.getY() + ThreadLocalRandom.current().nextDouble(-0.6, heightSpread));
                        world.spawnParticle(Particle.CLOUD, x, y, z, 2, 0.12, 0.12, 0.12, 0.01);
                        world.spawnParticle(Particle.SMOKE, x, y, z, 2, 0.12, 0.12, 0.12, 0.01);
                    }
                }

                world.playSound(center, Sound.ENTITY_WIND_CHARGE_WIND_BURST, 0.9f, 1.0f);
                applyTornadoPull(world, center, streamerLocation, radius, pullStrength, blocksPerTick, baseY);
                elapsedTicks += 1;
            }
        }.runTaskTimer(context.plugin(), 0L, 1L);
    }

    private void applyTornadoPull(World world, Location center, Location streamerLocation, double radius, double pullStrength, int blocksPerTick, int baseY) {
        for (Entity entity : world.getNearbyEntities(center, radius * 1.6, radius * 2.6, radius * 1.6)) {
            double distanceToCenter = entity.getLocation().distance(center);
            if (distanceToCenter > radius * 1.7) continue;

            Vector toCenter = center.clone().subtract(entity.getLocation()).toVector();
            double distance = Math.max(0.1, toCenter.length());
            double attractionMultiplier = Math.min(2.8, 1.3 + (radius / 9.0));
            toCenter.normalize().multiply(Math.max(0.22, pullStrength * attractionMultiplier) / Math.max(1.0, distance * 0.18));

            double verticalPull = Math.max(0.28, (center.getY() - entity.getLocation().getY()) * 0.08);
            if (entity.getLocation().getY() < center.getY() + 2.8) {
                verticalPull += 0.9;
            }

            double lowerSpread = Math.max(0.0, center.getY() - entity.getLocation().getY()) / 8.0;
            double baseRadiusBoost = 1.0 + Math.min(1.6, lowerSpread);
            Vector widePull = new Vector(
                    ThreadLocalRandom.current().nextDouble(-0.6, 0.6) * baseRadiusBoost,
                    0.0,
                    ThreadLocalRandom.current().nextDouble(-0.6, 0.6) * baseRadiusBoost
            );

            if (entity instanceof Player player) {
                double playerDistance = player.getLocation().distance(center);
                double liftLimit = Math.max(0.1, 0.95 - (playerDistance / radius));
                Vector lift = new Vector(0, Math.max(0.2, 0.7 - liftLimit) + verticalPull, 0);
                player.setVelocity(player.getVelocity().add(toCenter).add(widePull).add(lift));
                if (playerDistance < 2.5 || ThreadLocalRandom.current().nextInt(0, 7) == 0) {
                    player.setVelocity(player.getVelocity().add(new Vector(
                            ThreadLocalRandom.current().nextDouble(-0.9, 0.9),
                            ThreadLocalRandom.current().nextDouble(0.12, 0.5),
                            ThreadLocalRandom.current().nextDouble(-0.9, 0.9)
                    )));
                }
                continue;
            }

            if (entity.isDead() || entity.getType() == org.bukkit.entity.EntityType.ARMOR_STAND) continue;
            entity.setVelocity(entity.getVelocity().add(toCenter).add(widePull).add(new Vector(0, 0.4 + verticalPull, 0)));
        }

        for (int index = 0; index < blocksPerTick + 8; index++) {
            double angle = ThreadLocalRandom.current().nextDouble(Math.PI * 2);
            double distance = ThreadLocalRandom.current().nextDouble(radius * 1.3, radius * 2.0);
            double offsetX = Math.cos(angle) * distance;
            double offsetZ = Math.sin(angle) * distance;
            int x = center.getBlockX() + (int) Math.round(offsetX);
            int y = Math.max(baseY - 8, center.getBlockY() + ThreadLocalRandom.current().nextInt(-10, 12));
            int z = center.getBlockZ() + (int) Math.round(offsetZ);
            Location blockLocation = new Location(world, x, y, z);
            Material material = blockLocation.getBlock().getType();
            if (material == Material.AIR || material == Material.BEDROCK || material == Material.BARRIER) {
                continue;
            }

            if (blockLocation.distanceSquared(streamerLocation) < (radius * radius) * 2.0) {
                FallingBlock fallingBlock = (FallingBlock) world.spawnEntity(blockLocation, org.bukkit.entity.EntityType.FALLING_BLOCK);
                fallingBlock.setBlockData(material.createBlockData());
                fallingBlock.setDropItem(false);
                Vector launch = new Vector(
                        ThreadLocalRandom.current().nextDouble(-1.8, 1.8),
                        ThreadLocalRandom.current().nextDouble(0.8, 1.7),
                        ThreadLocalRandom.current().nextDouble(-1.8, 1.8)
                );
                if (ThreadLocalRandom.current().nextInt(0, 3) == 0) {
                    launch.multiply(1.7);
                }
                fallingBlock.setVelocity(launch);
                blockLocation.getBlock().setType(Material.AIR);
            }
        }
    }
}
