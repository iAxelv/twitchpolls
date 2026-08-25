package com.foxy.twitchPolls.actions.donations;

import com.foxy.twitchPolls.actions.ActionContext;
import com.foxy.twitchPolls.actions.ActionStrategy;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.FallingBlock;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.concurrent.ThreadLocalRandom;

public class EarthquakeAction implements ActionStrategy {
    @Override public void execute(ActionContext context) {
        int duration = Math.max(1, context.config().getInt("duration-seconds", 12));
        int amount = Math.max(1, context.config().getInt("debris-per-wave", 2));
        int blocksPerWave = Math.max(1, context.config().getInt("blocks-per-wave", 2));
        double radius = Math.max(2.0, context.config().getDouble("radius", 8.0));
        new BukkitRunnable() {
            int waves = duration * 2;
            @Override public void run() {
                if (!context.player().isOnline() || waves-- <= 0) { cancel(); return; }
                context.player().setVelocity(context.player().getVelocity().setY(0.25));
                context.world().spawnParticle(Particle.BLOCK, context.location(), 30, radius / 2, 0.1, radius / 2,
                        Material.DIRT.createBlockData());
                context.world().playSound(context.location(), Sound.ENTITY_GENERIC_EXPLODE, 0.35f, 0.6f);
                for (int index = 0; index < amount; index++) {
                    Location location = context.randomLocation(radius, 5);
                    FallingBlock block = (FallingBlock) context.world().spawnEntity(location, EntityType.FALLING_BLOCK);
                    block.setBlockData(Material.COBBLESTONE.createBlockData());
                    block.setDropItem(false);
                    block.setHurtEntities(false);
                }
                for (int index = 0; index < blocksPerWave; index++) {
                    int x = context.location().getBlockX() + ThreadLocalRandom.current().nextInt((int) -radius, (int) radius + 1);
                    int z = context.location().getBlockZ() + ThreadLocalRandom.current().nextInt((int) -radius, (int) radius + 1);
                    var ground = context.world().getHighestBlockAt(x, z);
                    if (ground.getType().isSolid() && ground.getType() != Material.BEDROCK) ground.breakNaturally();
                }
            }
        }.runTaskTimer(context.plugin(), 0L, 10L);
    }
}