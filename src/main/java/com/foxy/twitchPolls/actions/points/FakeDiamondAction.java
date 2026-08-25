package com.foxy.twitchPolls.actions.points;

import com.foxy.twitchPolls.TwitchPolls;
import com.foxy.twitchPolls.actions.ActionContext;
import com.foxy.twitchPolls.actions.ActionStrategy;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashMap;
import java.util.Map;

public class FakeDiamondAction implements ActionStrategy, Listener {
    private final TwitchPolls plugin;
    private final Map<Location, Material> fakeBlocks = new HashMap<>();

    public FakeDiamondAction(TwitchPolls plugin) {
        this.plugin = plugin;
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @Override
    public void execute(ActionContext context) {
        Location location = context.location().clone().add(context.location().getDirection().normalize()
                .multiply(context.config().getDouble("distance", 2.0)));
        Block block = location.getBlock();
        if (!block.getType().isAir()) {
            return;
        }
        Location key = block.getLocation();
        Material replacement = Material.matchMaterial(context.config().getString("replacement", "COBWEB"));
        fakeBlocks.put(key, replacement == null ? Material.COBWEB : replacement);
        block.setType(Material.DIAMOND_BLOCK, false);
        long duration = Math.max(1L, context.config().getLong("duration-seconds", 2L)) * 20L;
        new BukkitRunnable() {
            @Override
            public void run() {
                if (fakeBlocks.remove(key) != null && block.getType() == Material.DIAMOND_BLOCK) {
                    block.setType(Material.AIR);
                }
            }
        }.runTaskLater(plugin, duration);
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Location key = event.getBlock().getLocation();
        Material replacement = fakeBlocks.get(key);
        if (replacement == null) {
            return;
        }
        fakeBlocks.remove(key);
        event.setDropItems(false);
        event.getBlock().setType(replacement);
        event.getBlock().getWorld().spawnParticle(Particle.SMOKE, key.clone().add(0.5, 0.5, 0.5), 30,
                0.3, 0.3, 0.3, 0.02);
    }
}