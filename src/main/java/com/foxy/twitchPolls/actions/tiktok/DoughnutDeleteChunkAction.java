package com.foxy.twitchPolls.actions.tiktok;

import com.foxy.twitchPolls.actions.ActionContext;
import com.foxy.twitchPolls.actions.ActionStrategy;
import org.bukkit.Chunk;
import org.bukkit.Material;
import org.bukkit.scheduler.BukkitRunnable;

public class DoughnutDeleteChunkAction implements ActionStrategy {
    @Override public void execute(ActionContext context) {
        long duration = Math.max(1L, context.config().getLong("duration-seconds", 1L)) * 20L;
        long interval = Math.max(1L, context.config().getLong("interval-seconds", 5L)) * 20L;
        new BukkitRunnable() {
            long remaining = duration;
            @Override public void run() {
                if (!context.player().isOnline() || remaining <= 0) {
                    cancel();
                    return;
                }
                Chunk chunk = context.player().getLocation().getChunk();
                clearChunk(chunk);
                remaining -= interval;
            }
        }.runTaskTimer(context.plugin(), 0L, interval);
    }

    private void clearChunk(Chunk chunk) {
        int minHeight = chunk.getWorld().getMinHeight();
        int maxHeight = chunk.getWorld().getMaxHeight();
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                for (int y = minHeight; y < maxHeight; y++) {
                    var block = chunk.getBlock(x, y, z);
                    if (y == minHeight) {
                        block.setType(Material.BEDROCK, false);
                    } else if (block.getType() != Material.BEDROCK) {
                        block.setType(Material.AIR, false);
                    }
                }
            }
        }
    }
}