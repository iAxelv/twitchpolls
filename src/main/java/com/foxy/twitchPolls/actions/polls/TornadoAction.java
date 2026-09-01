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
        double radius = Math.max(2.0, context.config().getDouble("radius", 8.0));
        double pullStrength = Math.max(0.08, context.config().getDouble("pull-strength", 0.42));
        int blocksPerTick = Math.max(3, context.config().getInt("blocks-per-tick", 22));
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

            @Override
            public void run() {
                if (!context.player().isOnline() || elapsedTicks >= totalTicks) {
                    cancel();
                    return;
                }

                World world = context.world();
                if (center.distanceSquared(streamerLocation) > radius * radius) {
                    double dx = streamerLocation.getX() - center.getX();
                    double dz = streamerLocation.getZ() - center.getZ();
                    double len = Math.sqrt(dx * dx + dz * dz);
                    if (len > 0.01) {
                        center.add((dx / len) * 0.8, 0, (dz / len) * 0.8);
                    }
                }

                center.setY(Math.max(world.getMinHeight() + 2, Math.min(center.getY() + 0.25, streamerLocation.getY() + 8.0)));
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
        for (Entity entity : world.getNearbyEntities(center, radius, radius * 1.8, radius)) {
            if (entity.getLocation().distanceSquared(center) > radius * radius) continue;

            Vector toCenter = center.clone().subtract(entity.getLocation()).toVector();
            double distance = Math.max(0.1, toCenter.length());
            toCenter.normalize().multiply(Math.max(0.12, pullStrength) / Math.max(1.0, distance * 0.25));

            if (entity instanceof Player player) {
                double playerDistance = player.getLocation().distance(center);
                double liftLimit = Math.max(0.1, 0.75 - (playerDistance / radius));
                Vector lift = new Vector(0, Math.max(0.08, 0.48 - liftLimit), 0);
                player.setVelocity(player.getVelocity().add(toCenter).add(lift));
                if (playerDistance < 2.2) {
                    player.setVelocity(player.getVelocity().add(new Vector(
                            ThreadLocalRandom.current().nextDouble(-0.35, 0.35),
                            0.12,
                            ThreadLocalRandom.current().nextDouble(-0.35, 0.35)
                    )));
                }
                continue;
            }

            if (entity.isDead() || entity.getType() == org.bukkit.entity.EntityType.ARMOR_STAND) continue;
            entity.setVelocity(entity.getVelocity().add(toCenter).add(new Vector(0, 0.2, 0)));
        }

        for (int index = 0; index < blocksPerTick; index++) {
            double angle = ThreadLocalRandom.current().nextDouble(Math.PI * 2);
            double distance = ThreadLocalRandom.current().nextDouble(radius * 1.2);
            double offsetX = Math.cos(angle) * distance;
            double offsetZ = Math.sin(angle) * distance;
            int x = center.getBlockX() + (int) Math.round(offsetX);
            int y = center.getBlockY() + ThreadLocalRandom.current().nextInt(-3, 6);
            int z = center.getBlockZ() + (int) Math.round(offsetZ);
            Location blockLocation = new Location(world, x, y, z);
            Material material = blockLocation.getBlock().getType();
            if (material == Material.AIR || material == Material.BEDROCK || material == Material.BARRIER) {
                continue;
            }

            if (blockLocation.distanceSquared(streamerLocation) < (radius * radius) * 1.1) {
                FallingBlock fallingBlock = (FallingBlock) world.spawnEntity(blockLocation, org.bukkit.entity.EntityType.FALLING_BLOCK);
                fallingBlock.setBlockData(material.createBlockData());
                fallingBlock.setDropItem(false);
                fallingBlock.setVelocity(new Vector(
                        ThreadLocalRandom.current().nextDouble(-0.8, 0.8),
                        0.7,
                        ThreadLocalRandom.current().nextDouble(-0.8, 0.8)
                ));
                blockLocation.getBlock().setType(Material.AIR);
            }
        }
    }
}
