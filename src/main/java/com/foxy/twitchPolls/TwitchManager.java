package com.foxy.twitchPolls;

import com.github.philippheuer.credentialmanager.domain.OAuth2Credential;
import com.github.twitch4j.TwitchClient;
import com.github.twitch4j.TwitchClientBuilder;
import com.github.twitch4j.eventsub.condition.ChannelPollEndCondition;
import com.github.twitch4j.eventsub.domain.PollChoice;
import com.github.twitch4j.eventsub.events.ChannelPollEndEvent;
import com.github.twitch4j.eventsub.events.CustomRewardRedemptionAddEvent;
import com.github.twitch4j.eventsub.condition.ChannelPointsCustomRewardRedemptionAddCondition;
import com.github.twitch4j.eventsub.subscriptions.SubscriptionTypes;
import com.github.twitch4j.helix.domain.Poll;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;
import java.util.logging.Level;

@SuppressWarnings("deprecation")
public class TwitchManager {

    private final TwitchPolls plugin;
    private final ActionManager actionManager;
    private TwitchClient twitchClient;
    private BukkitTask pollTask;
    private BukkitTask bossBarTask;
    private BukkitTask testPollTask;
    private BossBar activeBossBar;
    private final Map<String, ConfigurationSection> activePollChoices = new HashMap<>();

    public TwitchManager(TwitchPolls plugin, ActionManager actionManager) {
        this.plugin = plugin;
        this.actionManager = actionManager;
    }

    public void connect() {
        String clientId = plugin.getConfig().getString("twitch.client-id");
        String clientSecret = plugin.getConfig().getString("twitch.client-secret");
        String oauthToken = cleanToken(plugin.getConfig().getString("twitch.oauth-token"));
        String broadcasterId = plugin.getConfig().getString("twitch.broadcaster-id");

        OAuth2Credential credential = new OAuth2Credential("twitch", oauthToken);

        twitchClient = TwitchClientBuilder.builder()
                .withClientId(clientId)
                .withClientSecret(clientSecret)
                .withEnableHelix(true)
                .withEnableEventSocket(true)
                .withDefaultAuthToken(credential)
                .build();

        twitchClient.getEventManager().onEvent(ChannelPollEndEvent.class, this::onPollEnd);
        twitchClient.getEventManager().onEvent(CustomRewardRedemptionAddEvent.class, this::onPointRedemption);
        twitchClient.getEventSocket().register(
                SubscriptionTypes.POLL_END,
                ChannelPollEndCondition.builder().broadcasterUserId(broadcasterId).build()
        );
        twitchClient.getEventSocket().register(
            SubscriptionTypes.CHANNEL_POINTS_CUSTOM_REWARD_REDEMPTION_ADD,
            ChannelPointsCustomRewardRedemptionAddCondition.builder().broadcasterUserId(broadcasterId).build()
        );

        startPollCycle();
    }

    public void disconnect() {
        if (pollTask != null) {
            pollTask.cancel();
        }
        if (bossBarTask != null) {
            bossBarTask.cancel();
        }
        if (testPollTask != null) {
            testPollTask.cancel();
        }
        if (activeBossBar != null) {
            activeBossBar.removeAll();
        }
        if (twitchClient != null) {
            twitchClient.close();
        }
    }

    public void reload() {
        disconnect();
        connect();
    }

    private void startPollCycle() {
        if (!plugin.getConfig().getBoolean("settings.automatic-polls", false)) {
            return;
        }
        int interval = plugin.getConfig().getInt("settings.poll-interval-seconds") * 20;
        pollTask = Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, this::createPoll, interval, interval);
    }

    public void createPoll() {
        String broadcasterId = plugin.getConfig().getString("twitch.broadcaster-id");
        String oauthToken = cleanToken(plugin.getConfig().getString("twitch.oauth-token"));
        int duration = plugin.getConfig().getInt("settings.poll-duration-seconds");
        String pollTitle = plugin.getConfig().getString("messages.poll-title");

        ConfigurationSection eventsSection = plugin.getEventConfig("polls");

        if (eventsSection == null) {
            return;
        }

        List<String> eventKeys = new ArrayList<>();
        for (String key : eventsSection.getKeys(false)) {
            if (eventsSection.getBoolean(key + ".active", true)) {
                eventKeys.add(key);
            }
        }

        if (eventKeys.isEmpty()) {
            return;
        }

        Collections.shuffle(eventKeys);
        List<String> selectedKeys = eventKeys.subList(0, Math.min(3, eventKeys.size()));

        List<PollChoice> choices = new ArrayList<>();
        activePollChoices.clear();

        for (String key : selectedKeys) {
            String title = eventsSection.getString(key + ".title");
            choices.add(new PollChoice().withTitle(title));
            activePollChoices.put(title, eventsSection.getConfigurationSection(key));
        }

        Poll poll = new Poll()
                .withBroadcasterId(broadcasterId)
                .withTitle(pollTitle)
                .withChoices(choices)
                .withDurationSeconds(duration);

        try {
            twitchClient.getHelix().createPoll(oauthToken, poll).execute();
            notifyStart(duration, choices);
        } catch (Exception exception) {
            plugin.getLogger().log(Level.SEVERE,
                    "Failed to create Twitch poll for broadcaster " + broadcasterId,
                    exception);
        }
    }

    public void testPoll(Player player, ConfigurationSection actionConfig) {
        int duration = plugin.getConfig().getInt("settings.poll-duration-seconds", 45);
        String eventTitle = actionConfig.getString("title", actionConfig.getName());
        String startTitle = plugin.getConfig().getString("messages.poll-start-title", "&d¡Nueva Encuesta!");
        String startSubtitle = plugin.getConfig().getString("messages.poll-start-subtitle", "&fLos viewers están votando..");

        player.showTitle(Title.title(formatColor(startTitle), formatColor(startSubtitle), titleTimes()));
        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, 1.0f);
        startBossBar(player, duration);

        if (testPollTask != null) {
            testPollTask.cancel();
        }
        testPollTask = Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (!player.isOnline()) {
                return;
            }

            stopBossBar();
            String endTitle = plugin.getConfig().getString("messages.poll-end-title", "&a¡Votación Terminada!");
            String endSubtitle = plugin.getConfig()
                    .getString("messages.poll-end-subtitle", "&fGanó: &e%winner%")
                    .replace("%winner%", eventTitle);
            player.showTitle(Title.title(formatColor(endTitle), formatColor(endSubtitle), titleTimes()));
            if (!"RANDOM_SOUND".equals(actionConfig.getString("action"))) {
                player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.0f);
            }
            actionManager.executeAction(player, actionConfig);
            testPollTask = null;
        }, Math.max(1, duration) * 20L);
    }

    public void executePointAction(Player player, ConfigurationSection actionConfig) {
        if (actionConfig == null || !player.isOnline()) {
            return;
        }

        String eventTitle = actionConfig.getString("title", actionConfig.getName());
        String title = plugin.getConfig().getString("messages.points-event-title", "");
        String subtitle = plugin.getConfig().getString("messages.points-event-subtitle", "")
                .replace("%event%", eventTitle)
                .replace("%value%", String.valueOf(actionConfig.getInt("value", 0)));
        if (!title.isBlank() || !subtitle.isBlank()) {
            player.showTitle(Title.title(formatColor(title), formatColor(subtitle), titleTimes()));
        }

        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, 1.6f);
        actionManager.executeAction(player, actionConfig);

        String broadcast = plugin.getConfig().getString("messages.points-event-broadcast", "")
                .replace("%event%", eventTitle)
                .replace("%value%", String.valueOf(actionConfig.getInt("value", 0)));
        if (!broadcast.isBlank()) {
            Bukkit.broadcast(formatColor(broadcast));
        }
    }

    private void notifyStart(int durationSeconds, List<PollChoice> choices) {
        Bukkit.getScheduler().runTask(plugin, () -> {
            String streamerName = plugin.getConfig().getString("settings.streamer-username");
            Player player = Bukkit.getPlayer(streamerName);

            if (player != null && player.isOnline()) {
                String title = plugin.getConfig().getString("messages.poll-start-title");
                String sub = plugin.getConfig().getString("messages.poll-start-subtitle");
                player.showTitle(Title.title(formatColor(title), formatColor(sub), titleTimes()));
                player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, 1.0f);

                startBossBar(player, durationSeconds);
            }

            if (plugin.getConfig().getBoolean("settings.broadcast-results")) {
                List<String> broadcastLines = plugin.getConfig().getStringList("messages.broadcast-start");
                for (String line : broadcastLines) {
                    if (line.contains("%options%")) {
                        for (PollChoice choice : choices) {
                            Bukkit.broadcast(formatColor("&e- " + choice.getTitle()));
                        }
                    } else {
                        Bukkit.broadcast(formatColor(line));
                    }
                }
            }
        });
    }

    private void startBossBar(Player player, int totalSeconds) {
        stopBossBar();

        String titleFormat = plugin.getConfig().getString("messages.bossbar-title", "&dEncuesta: &f%time%s restantes");
        String initialTitle = titleFormat.replace("%time%", String.valueOf(totalSeconds));

        activeBossBar = Bukkit.createBossBar(
                ChatColor.translateAlternateColorCodes('&', initialTitle),
                BarColor.PURPLE,
                BarStyle.SOLID
        );

        activeBossBar.addPlayer(player);

        final int[] timeRemaining = {totalSeconds};

        bossBarTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            timeRemaining[0]--;
            if (timeRemaining[0] <= 0) {
                activeBossBar.removeAll();
                bossBarTask.cancel();
                return;
            }

            String newTitle = titleFormat.replace("%time%", String.valueOf(timeRemaining[0]));
            activeBossBar.setTitle(ChatColor.translateAlternateColorCodes('&', newTitle));
            activeBossBar.setProgress((double) timeRemaining[0] / totalSeconds);

        }, 20L, 20L);
    }

    private void stopBossBar() {
        if (activeBossBar != null) {
            activeBossBar.removeAll();
            activeBossBar = null;
        }
        if (bossBarTask != null) {
            bossBarTask.cancel();
            bossBarTask = null;
        }
    }

    private void onPollEnd(ChannelPollEndEvent event) {
        if (event.getStatus() != null && !event.getStatus().toString().equalsIgnoreCase("completed")) {
            return;
        }

        int maxVotes = event.getChoices().stream()
                .mapToInt(choice -> choice.getVotes())
                .max()
                .orElse(0);

        List<PollChoice> topChoices = event.getChoices().stream()
                .filter(c -> c.getVotes() == maxVotes)
                .collect(Collectors.toList());

        String winnerTitle = topChoices.isEmpty() ? "" :
                topChoices.get(ThreadLocalRandom.current().nextInt(topChoices.size())).getTitle();

        ConfigurationSection actionConfig = activePollChoices.get(winnerTitle);

        Bukkit.getScheduler().runTask(plugin, () -> {
            if (activeBossBar != null) {
                activeBossBar.removeAll();
            }
            if (bossBarTask != null) {
                bossBarTask.cancel();
            }

            String streamerName = plugin.getConfig().getString("settings.streamer-username");
            Player player = Bukkit.getPlayer(streamerName);

            if (player != null && player.isOnline()) {
                String title = plugin.getConfig().getString("messages.poll-end-title");
                String sub = plugin.getConfig().getString("messages.poll-end-subtitle").replace("%winner%", winnerTitle);
                player.showTitle(Title.title(formatColor(title), formatColor(sub), titleTimes()));
                
                if (actionConfig == null || !"RANDOM_SOUND".equals(actionConfig.getString("action"))) {
                    player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.0f);
                }

                if (actionConfig != null) {
                    actionManager.executeAction(player, actionConfig);
                }
            }

            if (plugin.getConfig().getBoolean("settings.broadcast-results")) {
                String broadcast = plugin.getConfig().getString("messages.broadcast-end").replace("%winner%", winnerTitle);
                Bukkit.broadcast(formatColor(broadcast));
            }
        });
    }

    private void onPointRedemption(CustomRewardRedemptionAddEvent event) {
        if (event.getReward() == null || event.getReward().getCost() == null) {
            return;
        }

        ConfigurationSection points = plugin.getEventConfig("points");
        if (points == null) {
            return;
        }

        ConfigurationSection matched = null;
        for (String key : points.getKeys(false)) {
            ConfigurationSection candidate = points.getConfigurationSection(key);
            if (candidate == null || !candidate.getBoolean("active", true)
                    || candidate.getInt("value", -1) != event.getReward().getCost()) {
                continue;
            }
            String rewardTitle = candidate.getString("reward-title", candidate.getString("title", key));
            if (rewardTitle.equalsIgnoreCase(event.getReward().getTitle())) {
                matched = candidate;
                break;
            }
        }

        if (matched == null) {
            return;
        }

        ConfigurationSection selected = matched;
        String streamerName = plugin.getConfig().getString("settings.streamer-username");
        Bukkit.getScheduler().runTask(plugin, () -> {
            Player player = Bukkit.getPlayer(streamerName);
            if (player != null && player.isOnline()) {
                executePointAction(player, selected);
            }
        });
    }

    private String cleanToken(String token) {
        if (token == null) return "";
        return token.startsWith("oauth:") ? token.substring(6) : token;
    }

    private Component formatColor(String text) {
        return LegacyComponentSerializer.legacyAmpersand().deserialize(text == null ? "" : text);
    }

    private Title.Times titleTimes() {
        return Title.Times.times(Duration.ofMillis(500), Duration.ofSeconds(3), Duration.ofMillis(1000));
    }
}