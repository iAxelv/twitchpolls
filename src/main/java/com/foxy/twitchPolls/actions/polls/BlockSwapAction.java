package com.foxy.twitchPolls.actions.polls;

import com.foxy.twitchPolls.TwitchPolls;
import com.foxy.twitchPolls.actions.ActionContext;
import com.foxy.twitchPolls.actions.ActionStrategy;
import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class BlockSwapAction implements ActionStrategy, Listener {
    private final TwitchPolls plugin;
    private final Map<UUID, ActiveEffect> active = new HashMap<>();
    public BlockSwapAction(TwitchPolls plugin) { this.plugin = plugin; plugin.getServer().getPluginManager().registerEvents(this, plugin); }
    @Override public void execute(ActionContext context) {
        Material replacement = Material.matchMaterial(context.config().getString("replacement", "SPONGE"));
        if (replacement == null || !replacement.isBlock()) replacement = Material.SPONGE;
        UUID playerId = context.player().getUniqueId();
        long duration = Math.max(1L, context.config().getLong("duration-seconds", 45)) * 1000L;
        active.put(playerId, new ActiveEffect(System.currentTimeMillis() + duration, replacement));
        new BukkitRunnable() {
            @Override public void run() { active.remove(playerId); }
        }.runTaskLater(plugin, duration / 50L);
    }
    @EventHandler public void onBreak(BlockBreakEvent event) {
        ActiveEffect effect = active.get(event.getPlayer().getUniqueId());
        if (effect == null) return;
        if (effect.expiresAt < System.currentTimeMillis()) { active.remove(event.getPlayer().getUniqueId()); return; }
        event.setDropItems(false);
        plugin.getServer().getScheduler().runTask(plugin, () -> event.getBlock().setType(effect.replacement, true));
    }
    private record ActiveEffect(long expiresAt, Material replacement) { }
}