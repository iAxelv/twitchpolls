package com.foxy.twitchPolls.actions.donations;

import com.foxy.twitchPolls.actions.ActionContext;
import com.foxy.twitchPolls.actions.ActionStrategy;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.FallingBlock;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.concurrent.ThreadLocalRandom;

public class MeteorShowerAction implements ActionStrategy {
    @Override
    public void execute(ActionContext context) {
        int duration = Math.max(1, context.config().getInt("duration-seconds", 20));
        int amount = Math.max(1, context.config().getInt("amount-per-wave", 3));
        double radius = Math.max(1.0, context.config().getDouble("radius", 12.0));
        int height = Math.max(1, context.config().getInt("height", 16));
        float explosionPower = (float) Math.max(0.0, context.config().getDouble("explosion-power", 1.5));
        boolean setFire = context.config().getBoolean("set-fire", true);
        boolean breakBlocks = context.config().getBoolean("break-blocks", false);
        Material[] meteorMaterials = {Material.MAGMA_BLOCK, Material.OBSIDIAN, Material.IRON_BLOCK,
            Material.CRYING_OBSIDIAN, Material.TNT};

        new BukkitRunnable() {
            private int waves = duration * 2;

            @Override
            public void run() {
                if (waves-- <= 0 || !context.player().isOnline()) {
                    cancel();
                    return;
                }
                for (int index = 0; index < amount; index++) {
                        Location location = context.randomLocation(radius, height
                            + ThreadLocalRandom.current().nextInt(0, 8));
                        Material material = meteorMaterials[ThreadLocalRandom.current().nextInt(meteorMaterials.length)];
                    FallingBlock meteor = (FallingBlock) context.world().spawnEntity(location, EntityType.FALLING_BLOCK);
                        BlockData data = material.createBlockData();
                        meteor.setBlockData(data);
                    meteor.setDropItem(false);
                    meteor.setHurtEntities(true);
                    meteor.setVelocity(meteor.getVelocity().setY(-0.6));
                    context.world().spawnParticle(Particle.FLAME, location, 12, 0.2, 0.2, 0.2, 0.03);
                    new BukkitRunnable() {
                        @Override public void run() {
                            if (meteor.isValid()) {
                                Location impact = meteor.getLocation();
                                meteor.remove();
                                context.world().createExplosion(impact, explosionPower, setFire, breakBlocks);
                                context.world().spawnParticle(Particle.EXPLOSION, impact, 1);
                            }
                        }
                    }.runTaskLater(context.plugin(), 35L);
                }
            }
        }.runTaskTimer(context.plugin(), 0L, 10L);
    }
}