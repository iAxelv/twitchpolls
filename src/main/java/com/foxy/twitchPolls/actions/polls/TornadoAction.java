package com.foxy.twitchPolls.actions.polls;

import com.foxy.twitchPolls.actions.ActionContext;
import com.foxy.twitchPolls.actions.ActionStrategy;
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

                center.setY(Math.max(world.getMinHeight() + 2, Math.min(center.getY() + 0.35, streamerLocation.getY() + 9.0)));
                if (particles) {
                    for (int i = 0; i < 12; i++) {
                        double spiralAngle = (elapsedTicks * 0.45) + (i * 0.6);
                        double x = center.getX() + Math.cos(spiralAngle) * (1.8 + i * 0.22);
                        double z = center.getZ() + Math.sin(spiralAngle) * (1.8 + i * 0.22);
                        double y = center.getY() + ThreadLocalRandom.current().nextDouble(0.6, 4.2);
                        world.spawnParticle(Particle.CLOUD, x, y, z, 2, 0.12, 0.12, 0.12, 0.01);
                        world.spawnParticle(Particle.SMOKE, x, y, z, 2, 0.12, 0.12, 0.12, 0.01);
                    }
                }

                world.playSound(center, Sound.ENTITY_WIND_CHARGE_WIND_BURST, 0.9f, 1.0f);
                applyTornadoPull(world, center, streamerLocation, radius, pullStrength, blocksPerTick);
                elapsedTicks += 1;
            }
        }.runTaskTimer(context.plugin(), 0L, 1L);
    }

    private void applyTornadoPull(World world, Location center, Location streamerLocation, double radius, double pullStrength, int blocksPerTick) {
        for (Entity entity : world.getNearbyEntities(center, radius, radius * 2.2, radius)) {
            if (entity.getLocation().distanceSquared(center) > radius * radius) continue;

            Vector toCenter = center.clone().subtract(entity.getLocation()).toVector();
            double distance = Math.max(0.1, toCenter.length());
            double attractionMultiplier = Math.min(2.2, 1.1 + (radius / 12.0));
            toCenter.normalize().multiply(Math.max(0.16, pullStrength * attractionMultiplier) / Math.max(1.0, distance * 0.2));

            if (entity instanceof Player player) {
                double playerDistance = player.getLocation().distance(center);
                double liftLimit = Math.max(0.08, 0.9 - (playerDistance / radius));
                Vector lift = new Vector(0, Math.max(0.12, 0.72 - liftLimit), 0);
                player.setVelocity(player.getVelocity().add(toCenter).add(lift));
                if (playerDistance < 2.2 || ThreadLocalRandom.current().nextInt(0, 8) == 0) {
                    player.setVelocity(player.getVelocity().add(new Vector(
                            ThreadLocalRandom.current().nextDouble(-0.5, 0.5),
                            ThreadLocalRandom.current().nextDouble(0.08, 0.32),
                            ThreadLocalRandom.current().nextDouble(-0.5, 0.5)
                    )));
                }
                continue;
            }

            if (entity.isDead() || entity.getType() == org.bukkit.entity.EntityType.ARMOR_STAND) continue;
            entity.setVelocity(entity.getVelocity().add(toCenter).add(new Vector(0, 0.24, 0)));
        }

        for (int index = 0; index < blocksPerTick; index++) {
            double angle = ThreadLocalRandom.current().nextDouble(Math.PI * 2);
            double distance = ThreadLocalRandom.current().nextDouble(radius * 1.5);
            double offsetX = Math.cos(angle) * distance;
            double offsetZ = Math.sin(angle) * distance;
            int x = center.getBlockX() + (int) Math.round(offsetX);
            int y = center.getBlockY() + ThreadLocalRandom.current().nextInt(-4, 7);
            int z = center.getBlockZ() + (int) Math.round(offsetZ);
            Location blockLocation = new Location(world, x, y, z);
            Material material = blockLocation.getBlock().getType();
            if (material == Material.AIR || material == Material.BEDROCK || material == Material.BARRIER) {
                continue;
            }

            if (blockLocation.distanceSquared(streamerLocation) < (radius * radius) * 1.4) {
                FallingBlock fallingBlock = (FallingBlock) world.spawnEntity(blockLocation, org.bukkit.entity.EntityType.FALLING_BLOCK);
                fallingBlock.setBlockData(material.createBlockData());
                fallingBlock.setDropItem(false);
                Vector launch = new Vector(
                        ThreadLocalRandom.current().nextDouble(-1.2, 1.2),
                        ThreadLocalRandom.current().nextDouble(0.65, 1.35),
                        ThreadLocalRandom.current().nextDouble(-1.2, 1.2)
                );
                if (ThreadLocalRandom.current().nextInt(0, 3) == 0) {
                    launch.multiply(1.5);
                }
                fallingBlock.setVelocity(launch);
                blockLocation.getBlock().setType(Material.AIR);
            }
        }
    }
}
