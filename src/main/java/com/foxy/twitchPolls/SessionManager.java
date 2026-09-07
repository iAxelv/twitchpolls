package com.foxy.twitchPolls;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.UUID;

public class SessionManager implements Listener {
    private final TwitchPolls plugin;
    private UUID streamerId;

    public SessionManager(TwitchPolls plugin) {
        this.plugin = plugin;
        refreshFromOnlinePlayers();
    }

    public Player getStreamer() {
        if (streamerId == null) return null;
        Player player = Bukkit.getPlayer(streamerId);
        return player != null && player.isOnline() ? player : null;
    }

    public String getStreamerUsername() {
        Player player = getStreamer();
        return player == null ? plugin.getConfig().getString("settings.streamer-username", "Streamer") : player.getName();
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        if (isStreamer(event.getPlayer())) streamerId = event.getPlayer().getUniqueId();
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        if (event.getPlayer().getUniqueId().equals(streamerId)) streamerId = null;
    }

    private void refreshFromOnlinePlayers() {
        streamerId = null;
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (isStreamer(player)) {
                streamerId = player.getUniqueId();
                return;
            }
        }
    }

    public void refresh() {
        refreshFromOnlinePlayers();
    }

    private boolean isStreamer(Player player) {
        return plugin.getConfig().getString("settings.streamer-username", "").equalsIgnoreCase(player.getName());
    }
}