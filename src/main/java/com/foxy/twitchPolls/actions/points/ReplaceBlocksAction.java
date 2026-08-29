package com.foxy.twitchPolls.actions.points;

import com.foxy.twitchPolls.PlayerEffectRegistry;
import com.foxy.twitchPolls.TwitchPolls;
import com.foxy.twitchPolls.actions.ActionContext;
import com.foxy.twitchPolls.actions.ActionStrategy;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ReplaceBlocksAction implements ActionStrategy, Listener {
    private final TwitchPolls plugin;
    private final Map<UUID, ActiveEffect> activePlayers = new HashMap<>();

    public ReplaceBlocksAction(TwitchPolls plugin, PlayerEffectRegistry effectRegistry) {
        this.plugin = plugin;
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
        // Register cleanup handler for when players quit
        effectRegistry.registerCleanupHandler(activePlayers::remove);
    }

    @Override
    public void execute(ActionContext context) {
        long durationMillis = Math.max(1L, context.config().getLong("duration-seconds", 60L)) * 1000L;
        Material replacement = Material.matchMaterial(context.config().getString("replacement", "GRAVEL"));
        if (replacement != Material.GRAVEL && replacement != Material.SAND) {
            replacement = Material.GRAVEL;
        }
        activePlayers.put(context.player().getUniqueId(),
                new ActiveEffect(System.currentTimeMillis() + durationMillis, replacement));
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        UUID playerId = event.getPlayer().getUniqueId();
        ActiveEffect effect = activePlayers.get(playerId);
        if (effect == null) {
            return;
        }
        if (effect.expiresAt() <= System.currentTimeMillis()) {
            activePlayers.remove(playerId);
            return;
        }

        Block block = event.getBlock();
        Material original = block.getType();
        if (original == Material.GRAVEL || original == Material.SAND) {
            return;
        }

        event.setDropItems(false);
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            if (block.getType().isAir()) {
                block.setType(effect.replacement(), true);
            }
        }, 1L);
    }

    private record ActiveEffect(long expiresAt, Material replacement) {
    }
}