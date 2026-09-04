package com.foxy.twitchPolls;

import io.github.jwdeveloper.tiktok.TikTokLive;
import io.github.jwdeveloper.tiktok.data.events.gift.TikTokGiftEvent;
import io.github.jwdeveloper.tiktok.data.events.social.TikTokFollowEvent;
import io.github.jwdeveloper.tiktok.data.events.social.TikTokLikeEvent;
import io.github.jwdeveloper.tiktok.live.LiveClient;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.util.concurrent.atomic.AtomicLong;
import java.util.Locale;
import java.util.logging.Level;

public final class TikTokManager {
    private final TwitchPolls plugin;
    private final ActionManager actionManager;
    private final UIManager uiManager;
    private final SessionManager sessionManager;
    private final AtomicLong connectionGeneration = new AtomicLong();
    private BukkitTask reconnectTask;
    private LiveClient client;
    private long likesSinceLastTrigger;

    public TikTokManager(TwitchPolls plugin, ActionManager actionManager, UIManager uiManager,
                         SessionManager sessionManager) {
        this.plugin = plugin;
        this.actionManager = actionManager;
        this.uiManager = uiManager;
        this.sessionManager = sessionManager;
    }

    public void connect() {
        likesSinceLastTrigger = 0;
        if (!plugin.getConfig().getBoolean("tiktok.enabled", false)) {
            plugin.getLogger().info("TikTok is disabled in the configuration.");
            return;
        }

        String username = plugin.getConfig().getString("tiktok.streamer-username", "").trim()
            .toLowerCase(Locale.ROOT);
        if (username.isBlank() || "usuario_tiktok".equalsIgnoreCase(username)) {
            plugin.getLogger().warning("TikTok is selected, but tiktok.streamer-username is not configured.");
            return;
        }

        long generation = connectionGeneration.incrementAndGet();
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                LiveClient newClient = TikTokLive.newClient(username)
                        .onGift((liveClient, event) -> onGift(event))
                        .onFollow((liveClient, event) -> onFollow(event))
                        .onLike((liveClient, event) -> onLike(event))
                        .onConnected((liveClient, event) -> plugin.getLogger().info(
                            "Connected to TikTok Live for @" + username))
                        .onDisconnected((liveClient, event) -> onDisconnected(liveClient, generation))
                        .onError((liveClient, event) -> plugin.getLogger().log(
                            Level.WARNING, "Error in TikTok Live", event.getException()))
                        .buildAndConnect();
                synchronized (this) {
                    if (generation != connectionGeneration.get()
                            || !plugin.isEnabled()
                            || !"tiktok".equalsIgnoreCase(plugin.getConfig().getString(
                            "settings.event-provider", "twitch"))) {
                        newClient.disconnect();
                        return;
                    }
                    client = newClient;
                }
            } catch (Exception exception) {
                if (generation == connectionGeneration.get()) {
                    plugin.getLogger().log(Level.WARNING,
                        "Could not connect to TikTok Live for @" + username
                            + "; retrying automatically.", exception);
                    scheduleReconnect();
                }
            }
        });
    }

    public synchronized void disconnect() {
        connectionGeneration.incrementAndGet();
        if (reconnectTask != null) {
            reconnectTask.cancel();
            reconnectTask = null;
        }
        LiveClient activeClient = client;
        client = null;
        if (activeClient != null) {
            try {
                activeClient.disconnect();
            } catch (Exception exception) {
                plugin.getLogger().log(Level.FINE, "Error closing TikTok Live", exception);
            }
        }
    }

    public void reconnect() {
        disconnect();
        connect();
    }

    private synchronized void onDisconnected(LiveClient disconnectedClient, long generation) {
        if (generation != connectionGeneration.get() || disconnectedClient != client) {
            return;
        }
        client = null;
        scheduleReconnect();
        plugin.getLogger().warning("TikTok Live disconnected; retrying connection.");
    }

    private synchronized void scheduleReconnect() {
        if (reconnectTask != null || !plugin.isEnabled()
                || !"tiktok".equalsIgnoreCase(plugin.getConfig().getString(
                "settings.event-provider", "twitch"))) {
            return;
        }

        reconnectTask = Bukkit.getScheduler().runTaskLaterAsynchronously(plugin, () -> {
            synchronized (this) {
                reconnectTask = null;
            }
            connect();
        }, 100L);
    }

    private void onGift(TikTokGiftEvent event) {
        if (event.getGift() == null) {
            return;
        }

        String giftName = event.getGift().getName();
        int diamondCost = event.getGift().getDiamondCost();
        ConfigurationSection tiktokEvents = plugin.getEventConfig("tiktok");
        if (tiktokEvents == null) {
            return;
        }

        ConfigurationSection matched = null;
        for (String key : tiktokEvents.getKeys(false)) {
            ConfigurationSection candidate = tiktokEvents.getConfigurationSection(key);
            if (candidate == null || !candidate.getBoolean("active", true)
                    || !"tiktok_gift".equalsIgnoreCase(candidate.getString("type", ""))) {
                continue;
            }

                String configuredName = candidate.getString("gift-name",
                    candidate.getString("gift_name", "")).trim();
                int configuredId = candidate.getInt("gift-id", candidate.getInt("gift_id", -1));
                boolean nameMatches = !configuredName.isBlank() && configuredName.equalsIgnoreCase(giftName);
                boolean idMatches = configuredId >= 0 && configuredId == event.getGift().getId();
                    boolean valueMatches = !candidate.getBoolean("match-diamond-cost", false)
                        || candidate.getInt("value", -1) == diamondCost;
                if ((nameMatches || idMatches) && valueMatches) {
                matched = candidate;
                break;
            }
        }

        if (matched == null) {
            return;
        }

        ConfigurationSection actionConfig = matched;
        String sender = event.getUser() == null ? "unknown" : event.getUser().getProfileName();
        int combo = Math.max(1, event.getCombo());
        Bukkit.getScheduler().runTask(plugin, () -> {
            Player player = sessionManager.getStreamer();
            if (player != null && player.isOnline()) {
                if (!plugin.getConfig().getBoolean("tiktok.show-title", false)) {
                    uiManager.showDonationEvent(player, actionConfig);
                }
                actionManager.executeDonationAction(player, actionConfig, sender, combo);
                    int totalValue = actionConfig.getInt("value", 1) * combo;
                    String broadcast = plugin.getLanguageManager().getString(
                        "messages.tiktok-gift-event-broadcast",
                        "&d[TikTok] &f%event% &7was activated by &e%value% %gift% &7(%username%)")
                        .replace("%provider%", "TikTok")
                        .replace("%event%", actionConfig.getString("title", actionConfig.getName()))
                        .replace("%value%", String.valueOf(totalValue))
                        .replace("%gift%", actionConfig.getString("display-name", giftName))
                        .replace("%username%", sender);
                    Bukkit.broadcast(LegacyComponentSerializer.legacyAmpersand().deserialize(broadcast));
            }
        });
    }

    private void onFollow(TikTokFollowEvent event) {
        ConfigurationSection tiktokEvents = plugin.getEventConfig("tiktok");
        if (tiktokEvents == null) {
            return;
        }

        ConfigurationSection matched = null;
        for (String key : tiktokEvents.getKeys(false)) {
            ConfigurationSection candidate = tiktokEvents.getConfigurationSection(key);
            if (candidate != null && candidate.getBoolean("active", true)
                    && "tiktok_follow".equalsIgnoreCase(candidate.getString("type", ""))) {
                matched = candidate;
                break;
            }
        }
        if (matched == null) {
            return;
        }

        ConfigurationSection actionConfig = matched;
        String follower = event.getUser() == null ? "unknown" : event.getUser().getProfileName();
        Bukkit.getScheduler().runTask(plugin, () -> {
            Player player = sessionManager.getStreamer();
            if (player != null && player.isOnline()) {
                if (!plugin.getConfig().getBoolean("tiktok.show-title", false)) {
                    uiManager.showTikTokFollowEvent(player, actionConfig);
                }
                actionManager.executeDonationAction(player, actionConfig, follower);
                String broadcast = plugin.getLanguageManager().getString(
                        "messages.tiktok-follow-event-broadcast",
                        "&d[TikTok] &f%event% &7was activated by a follow &7(%username%)")
                        .replace("%event%", actionConfig.getString("title", actionConfig.getName()))
                        .replace("%username%", follower);
                Bukkit.broadcast(LegacyComponentSerializer.legacyAmpersand().deserialize(broadcast));
            }
        });
    }

    private synchronized void onLike(TikTokLikeEvent event) {
        ConfigurationSection tiktokEvents = plugin.getEventConfig("tiktok");
        if (tiktokEvents == null) {
            return;
        }

        ConfigurationSection matched = null;
        for (String key : tiktokEvents.getKeys(false)) {
            ConfigurationSection candidate = tiktokEvents.getConfigurationSection(key);
            if (candidate != null && candidate.getBoolean("active", true)
                    && "tiktok_likes".equalsIgnoreCase(candidate.getString("type", ""))) {
                matched = candidate;
                break;
            }
        }
        if (matched == null) {
            return;
        }

        long threshold = Math.max(1, matched.getLong("likes-threshold", 500));
        likesSinceLastTrigger += Math.max(1, event.getLikes());
        long triggerCount = likesSinceLastTrigger / threshold;
        likesSinceLastTrigger %= threshold;
        if (triggerCount == 0) {
            return;
        }

        ConfigurationSection actionConfig = matched;
        Bukkit.getScheduler().runTask(plugin, () -> {
            String broadcast = plugin.getLanguageManager().getString(
                    "messages.tiktok-likes-broadcast",
                    "&d[TikTok] &f%event% &7was activated after &e%likes% likes")
                    .replace("%event%", actionConfig.getString("title", actionConfig.getName()))
                    .replace("%likes%", String.valueOf(threshold));
            for (long trigger = 0; trigger < triggerCount; trigger++) {
                Bukkit.broadcast(LegacyComponentSerializer.legacyAmpersand().deserialize(broadcast));
            }

            Player player = sessionManager.getStreamer();
            if (player == null || !player.isOnline()) {
                return;
            }
            if (!plugin.getConfig().getBoolean("tiktok.show-title", false)) {
                uiManager.showTikTokLikesEvent(player, actionConfig);
            }
            for (long trigger = 0; trigger < triggerCount; trigger++) {
                actionManager.executeAction(player, actionConfig);
            }
        });
    }
}