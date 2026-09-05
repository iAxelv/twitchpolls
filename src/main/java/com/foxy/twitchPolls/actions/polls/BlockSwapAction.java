package com.foxy.twitchPolls.actions.polls;

import com.foxy.twitchPolls.TwitchPolls;
import com.foxy.twitchPolls.actions.ActionContext;
import com.foxy.twitchPolls.actions.ActionStrategy;
import org.bukkit.Material;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class BlockSwapAction implements ActionStrategy {
    private final TwitchPolls plugin;
    public BlockSwapAction(TwitchPolls plugin) {
        this.plugin = plugin;
    }
    @Override public void execute(ActionContext context) {
        if (context.config().getBoolean("single-chunk", false)) {
            replaceChunk(context);
            return;
        }
        long duration = Math.max(1L, context.config().getLong("duration-seconds", 45)) * 20L;
        long interval = Math.max(1L, context.config().getLong("interval-ticks", 10L));
        List<Material> replacements = new ArrayList<>();
        for (String materialName : context.config().getStringList("replacements")) {
            Material material = Material.matchMaterial(materialName);
            if (material != null && material.isBlock() && material != Material.BEDROCK) {
                replacements.add(material);
            }
        }
        if (replacements.isEmpty()) return;
        new BukkitRunnable() {
            long remaining = duration;
            @Override public void run() {
                if (!context.player().isOnline() || remaining <= 0) { cancel(); return; }
                int y = context.player().getLocation().getBlockY() - 1;
                int centerX = context.player().getLocation().getBlockX();
                int centerZ = context.player().getLocation().getBlockZ();
                for (int x = centerX - 1; x <= centerX + 1; x++) {
                    for (int z = centerZ - 1; z <= centerZ + 1; z++) {
                        var block = context.world().getBlockAt(x, y, z);
                        if (block.getType() != Material.BEDROCK && block.getType().isSolid()) {
                            block.setType(replacements.get(ThreadLocalRandom.current().nextInt(replacements.size())), false);
                        }
                    }
                }
                remaining -= interval;
            }
        }.runTaskTimer(plugin, 0L, interval);
    }

    private void replaceChunk(ActionContext context) {
        List<Material> replacements = new ArrayList<>();
        for (String materialName : context.config().getStringList("replacements")) {
            Material material = Material.matchMaterial(materialName);
            if (material != null && material.isBlock() && material != Material.BEDROCK
                    && !material.isAir()) {
                replacements.add(material);
            }
        }
        if (replacements.isEmpty()) {
            return;
        }

        var chunk = context.player().getLocation().getChunk();
        int minHeight = context.world().getMinHeight();
        int maxHeight = context.world().getMaxHeight();
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                for (int y = minHeight; y < maxHeight; y++) {
                    var block = chunk.getBlock(x, y, z);
                    if (!block.getType().isAir() && block.getType() != Material.BEDROCK) {
                        block.setType(replacements.get(ThreadLocalRandom.current().nextInt(replacements.size())), false);
                    }
                }
            }
        }
    }
}