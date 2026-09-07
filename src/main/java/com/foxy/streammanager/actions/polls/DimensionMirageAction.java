package com.foxy.streammanager.actions.polls;

import com.foxy.streammanager.actions.ActionContext;
import com.foxy.streammanager.actions.ActionStrategy;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.BlockState;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Mob;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class DimensionMirageAction implements ActionStrategy {
    @Override public void execute(ActionContext context) {
        int chunkRadius = Math.max(1, context.config().getInt("chunk-radius", 1));
        int duration = Math.max(1, context.config().getInt("duration-seconds", 30));
        List<BlockState> originals = new ArrayList<>();
        List<BlockState> illusions = new ArrayList<>();
        List<Location> surfaces = new ArrayList<>();
        List<Entity> spawnedMobs = new ArrayList<>();
        Location center = context.player().getLocation().clone();
        int centerChunkX = center.getBlockX() >> 4;
        int centerChunkZ = center.getBlockZ() >> 4;
        double lavaChance = Math.max(0.0, Math.min(1.0, context.config().getDouble("lava-chance", 0.12)));
        for (int chunkX = centerChunkX - chunkRadius; chunkX <= centerChunkX + chunkRadius; chunkX++) {
            for (int chunkZ = centerChunkZ - chunkRadius; chunkZ <= centerChunkZ + chunkRadius; chunkZ++) {
                for (int localX = 0; localX < 16; localX++) {
                    for (int localZ = 0; localZ < 16; localZ++) {
                        int blockX = (chunkX << 4) + localX;
                        int blockZ = (chunkZ << 4) + localZ;
                        Location location = new Location(context.world(), blockX, 0, blockZ);
                        location.setY(context.world().getHighestBlockYAt(blockX, blockZ));
                        surfaces.add(location.clone());
                        BlockState original = location.getBlock().getState();
                        BlockState illusion = location.getBlock().getState();
                        Material material = ThreadLocalRandom.current().nextDouble() < lavaChance
                            ? Material.LAVA
                            : (ThreadLocalRandom.current().nextInt(6) == 0
                            ? Material.MAGMA_BLOCK
                            : (ThreadLocalRandom.current().nextBoolean() ? Material.SOUL_SAND : Material.NETHERRACK));
                        illusion.setType(material);
                        originals.add(original);
                        illusions.add(illusion);
                    }
                }
            }
        }
        sendBlockChanges(context, illusions);
        spawnNetherMobs(context, surfaces, spawnedMobs);
        context.world().playSound(center, Sound.BLOCK_PORTAL_AMBIENT, 1.2f, 0.6f);
        new BukkitRunnable() {
            @Override public void run() {
                if (context.player().isOnline()) sendBlockChanges(context, originals);
                for (Entity mob : spawnedMobs) {
                    if (!mob.isDead()) mob.remove();
                }
            }
        }.runTaskLater(context.plugin(), duration * 20L);
    }

    private void sendBlockChanges(ActionContext context, List<BlockState> states) {
        for (BlockState state : states) {
            context.player().sendBlockChange(state.getLocation(), state.getBlockData());
        }
    }

    private void spawnNetherMobs(ActionContext context, List<Location> surfaces, List<Entity> spawnedMobs) {
        spawnMobs(context, surfaces, EntityType.WITHER_SKELETON,
                context.config().getInt("wither-skeletons", 4), 1.0, spawnedMobs);
        spawnMobs(context, surfaces, EntityType.BLAZE,
                context.config().getInt("blazes", 4), 1.0, spawnedMobs);
        spawnMobs(context, surfaces, EntityType.GHAST,
                context.config().getInt("ghasts", 1), 8.0, spawnedMobs);
    }

    private void spawnMobs(ActionContext context, List<Location> surfaces, EntityType type,
                           int amount, double height, List<Entity> spawnedMobs) {
        for (int index = 0; index < Math.max(0, amount); index++) {
            Location spawn = surfaces.get(ThreadLocalRandom.current().nextInt(surfaces.size())).clone().add(0, height, 0);
            Mob mob = (Mob) context.world().spawnEntity(spawn, type);
            mob.setTarget(context.player());
            spawnedMobs.add(mob);
        }
    }
}
