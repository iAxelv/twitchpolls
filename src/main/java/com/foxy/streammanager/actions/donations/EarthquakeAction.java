package com.foxy.streammanager.actions.donations;

import com.foxy.streammanager.actions.ActionContext;
import com.foxy.streammanager.actions.ActionStrategy;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.FallingBlock;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.concurrent.ThreadLocalRandom;

public class EarthquakeAction implements ActionStrategy {
    @Override public void execute(ActionContext context) {
        int duration = Math.max(1, context.config().getInt("duration-seconds", 12));
        int amount = Math.max(3, context.config().getInt("debris-per-wave", 6));
        int blocksPerWave = Math.max(4, context.config().getInt("blocks-per-wave", 8));
        double radius = Math.max(2.0, context.config().getDouble("radius", 9.0));
        float explosionPower = (float) Math.max(0.5, context.config().getDouble("explosion-power", 1.5));

        new BukkitRunnable() {
            int waves = duration * 2;
            @Override public void run() {
                if (!context.player().isOnline() || waves-- <= 0) { cancel(); return; }

                Location center = context.location();
                context.world().spawnParticle(Particle.BLOCK, center, 35, radius / 2, 0.1, radius / 2,
                        Material.DIRT.createBlockData());
                context.world().playSound(center, Sound.ENTITY_ZOMBIE_VILLAGER_CONVERTED, 0.6f, 0.5f);
                Location blast = context.randomLocation(radius, 0);
                context.world().createExplosion(blast, explosionPower, false, true);

                Location playerLoc = context.player().getLocation().clone();
                double shakeX = (ThreadLocalRandom.current().nextDouble() - 0.5) * 0.18;
                double shakeY = (ThreadLocalRandom.current().nextDouble() - 0.5) * 0.08;
                double shakeZ = (ThreadLocalRandom.current().nextDouble() - 0.5) * 0.18;
                playerLoc.add(shakeX, shakeY, shakeZ);
                float yawShake = (float) ((ThreadLocalRandom.current().nextDouble() - 0.5) * 6.0);
                float pitchShake = (float) ((ThreadLocalRandom.current().nextDouble() - 0.5) * 4.0);
                playerLoc.setYaw(playerLoc.getYaw() + yawShake);
                playerLoc.setPitch(playerLoc.getPitch() + pitchShake);
                context.player().teleport(playerLoc);

                for (int index = 0; index < amount; index++) {
                    Location location = context.randomLocation(radius, 4 + ThreadLocalRandom.current().nextInt(0, 3));
                    FallingBlock block = (FallingBlock) context.world().spawnEntity(location, EntityType.FALLING_BLOCK);
                    block.setBlockData(Material.COBBLESTONE.createBlockData());
                    block.setDropItem(false);
                    block.setHurtEntities(false);
                    block.setVelocity(new Vector(0, -0.45, 0));
                }

                for (int index = 0; index < blocksPerWave; index++) {
                    int x = center.getBlockX() + ThreadLocalRandom.current().nextInt((int) -radius, (int) radius + 1);
                    int z = center.getBlockZ() + ThreadLocalRandom.current().nextInt((int) -radius, (int) radius + 1);

                    for (int offsetY = -2; offsetY <= 1; offsetY++) {
                        int targetX = x + ThreadLocalRandom.current().nextInt(-1, 2);
                        int targetZ = z + ThreadLocalRandom.current().nextInt(-1, 2);
                        int targetY = center.getBlockY() + offsetY;
                        Location blockLocation = new Location(context.world(), targetX, targetY, targetZ);
                        Material blockType = blockLocation.getBlock().getType();
                        if (blockType.isSolid() && blockType != Material.BEDROCK && blockType != Material.BARRIER) {
                            blockLocation.getBlock().breakNaturally();
                        }
                    }

                    var ground = context.world().getHighestBlockAt(x, z);
                    if (ground.getType().isSolid() && ground.getType() != Material.BEDROCK) ground.breakNaturally();
                }
            }
        }.runTaskTimer(context.plugin(), 0L, 10L);
    }
}