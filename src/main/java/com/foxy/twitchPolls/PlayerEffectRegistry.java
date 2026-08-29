package com.foxy.twitchPolls;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.Plugin;

import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.List;

/**
 * Manages player effect cleanup when players disconnect.
 * Prevents memory leaks from long-running effects that store player UUIDs.
 * 
 * Actions that store player data should register their cleanup handlers here.
 * This listener is called when a player quits to ensure all active effects are cleaned up.
 */
public class PlayerEffectRegistry implements Listener {
    private final List<PlayerCleanupHandler> handlers = new CopyOnWriteArrayList<>();

    public PlayerEffectRegistry(Plugin plugin) {
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    /**
     * Register a cleanup handler for a player effect map.
     * The handler will be called whenever a player quits to clean up their data.
     *
     * @param handler A PlayerCleanupHandler that removes data for a specific player
     */
    public void registerCleanupHandler(PlayerCleanupHandler handler) {
        handlers.add(handler);
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        UUID playerId = event.getPlayer().getUniqueId();
        for (PlayerCleanupHandler handler : handlers) {
            handler.cleanup(playerId);
        }
    }

    /**
     * Functional interface for player cleanup handlers.
     * Implementations should remove any data associated with the given player UUID.
     */
    @FunctionalInterface
    public interface PlayerCleanupHandler {
        /**
         * Clean up any data associated with this player UUID.
         * Safe to call even if the player has no active effects.
         *
         * @param playerId The UUID of the player who quit
         */
        void cleanup(UUID playerId);
    }
}
