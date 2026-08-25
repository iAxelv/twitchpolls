package com.foxy.twitchPolls.actions.donations;

import com.foxy.twitchPolls.actions.ActionContext;
import com.foxy.twitchPolls.actions.ActionStrategy;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.FallingBlock;
import org.bukkit.entity.TNTPrimed;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.concurrent.ThreadLocalRandom;

public class ApocalypseRainAction implements ActionStrategy {
    private static final Material[] FALLING_BLOCKS = {Material.ANVIL, Material.MAGMA_BLOCK};

    @Override
    public void execute(ActionContext context) {
        int duration = Math.max(1, context.config().getInt("duration-seconds", 30));
        int amount = Math.max(1, context.config().getInt("amount-per-wave", 3));
        double radius = Math.max(1.0, context.config().getDouble("radius", 15.0));
        int height = Math.max(1, context.config().getInt("height", 12));
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
                            + ThreadLocalRandom.current().nextInt(0, 7));
                    if (ThreadLocalRandom.current().nextBoolean()) {
                        BlockData data = FALLING_BLOCKS[ThreadLocalRandom.current().nextInt(FALLING_BLOCKS.length)]
                                .createBlockData();
                        FallingBlock falling = (FallingBlock) context.world().spawnEntity(location, EntityType.FALLING_BLOCK);
                        falling.setBlockData(data);
                        falling.setDropItem(false);
                        falling.setHurtEntities(true);
                    } else {
                        TNTPrimed tnt = context.world().spawn(location, TNTPrimed.class);
                        tnt.setFuseTicks(Math.max(1, context.config().getInt("tnt-fuse-ticks", 40)));
                    }
                }
            }
        }.runTaskTimer(context.plugin(), 0L, 10L);
    }
}