package com.foxy.twitchPolls.actions.donations;

import com.foxy.twitchPolls.actions.ActionContext;
import com.foxy.twitchPolls.actions.ActionStrategy;
import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.plugin.Plugin;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class MidasTouchAction implements ActionStrategy, Listener {
    private final Map<UUID, Long> activePlayers = new HashMap<>();

    public MidasTouchAction(Plugin plugin) {
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @Override
    public void execute(ActionContext context) {
        long duration = Math.max(1L, context.config().getLong("duration-seconds", 60L)) * 1000L;
        activePlayers.put(context.player().getUniqueId(), System.currentTimeMillis() + duration);
    }

    @EventHandler
    public void onMove(PlayerMoveEvent event) {
        Long expires = activePlayers.get(event.getPlayer().getUniqueId());
        if (expires == null) return;
        if (expires <= System.currentTimeMillis()) {
            activePlayers.remove(event.getPlayer().getUniqueId());
            return;
        }
        if (event.getTo() == null || event.getFrom().getBlockX() == event.getTo().getBlockX()
                && event.getFrom().getBlockY() == event.getTo().getBlockY()
                && event.getFrom().getBlockZ() == event.getTo().getBlockZ()) return;
        var block = event.getTo().clone().subtract(0, 1, 0).getBlock();
        if (!block.getType().isAir() && block.getType() != Material.BEDROCK) {
            block.setType(Material.GRAVEL, true);
        }
    }
}