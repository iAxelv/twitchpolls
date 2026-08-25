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
        BlockData data = Material.MAGMA_BLOCK.createBlockData();

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
                    FallingBlock meteor = (FallingBlock) context.world().spawnEntity(location, EntityType.FALLING_BLOCK);
                    meteor.setBlockData(data);
                    meteor.setDropItem(false);
                    meteor.setHurtEntities(true);
                    meteor.setVelocity(meteor.getVelocity().setY(-0.6));
                    context.world().spawnParticle(Particle.FLAME, location, 12, 0.2, 0.2, 0.2, 0.03);
                }
            }
        }.runTaskTimer(context.plugin(), 0L, 10L);
    }
}