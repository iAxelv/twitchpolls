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
    private final Map<UUID, ActiveEffect> activePlayers = new HashMap<>();

    public MidasTouchAction(Plugin plugin) {
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @Override
    public void execute(ActionContext context) {
        long duration = Math.max(1L, context.config().getLong("duration-seconds", 60L)) * 1000L;
        boolean preserveBedrock = context.config().getBoolean("preserve-bedrock", true);
        activePlayers.put(context.player().getUniqueId(),
            new ActiveEffect(System.currentTimeMillis() + duration, preserveBedrock));
    }

    @EventHandler
    public void onMove(PlayerMoveEvent event) {
        ActiveEffect effect = activePlayers.get(event.getPlayer().getUniqueId());
        if (effect == null) return;
        if (effect.expiresAt() <= System.currentTimeMillis()) {
            activePlayers.remove(event.getPlayer().getUniqueId());
            return;
        }
        if (event.getTo() == null || event.getFrom().getBlockX() == event.getTo().getBlockX()
                && event.getFrom().getBlockY() == event.getTo().getBlockY()
                && event.getFrom().getBlockZ() == event.getTo().getBlockZ()) return;
        var feet = event.getTo().getBlock();
        for (int y = feet.getY() - 1; y >= event.getPlayer().getWorld().getMinHeight(); y--) {
            var block = feet.getWorld().getBlockAt(feet.getX(), y, feet.getZ());
            if (!effect.preserveBedrock() || block.getType() != Material.BEDROCK) {
                block.setType(Material.AIR, false);
            }
        }
    }

    private record ActiveEffect(long expiresAt, boolean preserveBedrock) {
    }
}