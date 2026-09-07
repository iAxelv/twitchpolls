package com.foxy.twitchPolls.actions.points;

import com.foxy.twitchPolls.actions.ActionContext;
import com.foxy.twitchPolls.actions.ActionStrategy;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Creeper;
import org.bukkit.entity.FallingBlock;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

public class CreeperMeteorsAction implements ActionStrategy {
    @Override public void execute(ActionContext context) {
        int amount = Math.max(1, context.config().getInt("amount", 6));
        double radius = Math.max(1.0, context.config().getDouble("radius", 8.0));
        int height = Math.max(5, context.config().getInt("height", 20));
        BlockData data = Material.OBSIDIAN.createBlockData();
        for (int i = 0; i < amount; i++) {
            Location location = context.randomLocation(radius, height + i * 2);
            FallingBlock meteor = context.world().spawn(location, FallingBlock.class, falling -> {
                falling.setBlockData(data);
                falling.setDropItem(false);
                falling.setHurtEntities(true);
                falling.setVelocity(new Vector(0.0, -0.35, 0.0));
            });
            new BukkitRunnable() {
                @Override public void run() {
                    if (!meteor.isValid()) { cancel(); return; }
                    Location meteorLocation = meteor.getLocation();
                    boolean landed = meteor.isOnGround()
                            || meteor.getVelocity().getY() <= 0.0
                            && meteorLocation.clone().subtract(0, 0.6, 0).getBlock().getType().isSolid();
                    if (landed) {
                        Location spawn = meteorLocation.clone().add(0, 0.5, 0);
                        meteor.remove();
                        int fuseTicks = Math.max(1, context.config().getInt("fuse-ticks", 10));
                        context.world().spawn(spawn, Creeper.class, spawned -> {
                            spawned.setIgnited(true);
                            spawned.setFuseTicks(fuseTicks);
                            spawned.setMaxFuseTicks(fuseTicks);
                        });
                        cancel();
                    }
                }
            }.runTaskTimer(context.plugin(), 1L, 1L);
        }
    }
}
