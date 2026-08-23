package com.foxy.twitchPolls;

import com.github.philippheuer.credentialmanager.domain.OAuth2Credential;
import com.github.twitch4j.TwitchClient;
import com.github.twitch4j.TwitchClientBuilder;
import com.github.twitch4j.eventsub.condition.ChannelPollEndCondition;
import com.github.twitch4j.eventsub.domain.PollChoice;
import com.github.twitch4j.eventsub.events.ChannelPollEndEvent;
import com.github.twitch4j.eventsub.subscriptions.SubscriptionTypes;
import com.github.twitch4j.helix.domain.Poll;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TwitchManager {

    private final TwitchPolls plugin;
    private final ActionManager actionManager;
    private TwitchClient twitchClient;
    private BukkitTask pollTask;
    private final Map<String, String> activePollChoices = new HashMap<>();

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

        twitchClient.getEventSocket().register(
                SubscriptionTypes.POLL_END,
                ChannelPollEndCondition.builder().broadcasterUserId(broadcasterId).build()
        );

        startPollCycle();
    }

    public void disconnect() {
        if (pollTask != null) {
            pollTask.cancel();
        }
        if (twitchClient != null) {
            twitchClient.close();
        }
    }

    public void reload() {
        if (pollTask != null) {
            pollTask.cancel();
        }
        startPollCycle();
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

        ConfigurationSection eventsSection = plugin.getConfig().getConfigurationSection("events");
        List<String> eventKeys = new ArrayList<>(eventsSection.getKeys(false));
        Collections.shuffle(eventKeys);

        List<String> selectedKeys = eventKeys.subList(0, Math.min(3, eventKeys.size()));
        List<PollChoice> choices = new ArrayList<>();

        activePollChoices.clear();
        for (String key : selectedKeys) {
            String title = eventsSection.getString(key + ".title");
            choices.add(new PollChoice().withTitle(title));
            activePollChoices.put(title, eventsSection.getString(key + ".action"));
        }

        Poll poll = new Poll()
                .withBroadcasterId(broadcasterId)
                .withTitle(pollTitle)
                .withChoices(choices)
                .withDurationSeconds(duration);

        twitchClient.getHelix().createPoll(oauthToken, poll).execute();
        notifyStart();
    }

    private void notifyStart() {
        Bukkit.getScheduler().runTask(plugin, () -> {
            String streamerName = plugin.getConfig().getString("settings.streamer-username");
            Player player = Bukkit.getPlayer(streamerName);

            if (player != null && player.isOnline()) {
                String title = plugin.getConfig().getString("messages.poll-start-title");
                String sub = plugin.getConfig().getString("messages.poll-start-subtitle");
                player.showTitle(Title.title(formatColor(title), formatColor(sub), titleTimes()));
                player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, 1.0f);
            }

            if (plugin.getConfig().getBoolean("settings.broadcast-results")) {
                Bukkit.broadcast(formatColor(plugin.getConfig().getString("messages.broadcast-start")));
            }
        });
    }

    private void onPollEnd(ChannelPollEndEvent event) {
        String winnerTitle = event.getChoices().stream()
                .max((c1, c2) -> Integer.compare(c1.getVotes(), c2.getVotes()))
                .map(PollChoice::getTitle)
                .orElse("");

        String action = activePollChoices.getOrDefault(winnerTitle, "NONE");

        Bukkit.getScheduler().runTask(plugin, () -> {
            String streamerName = plugin.getConfig().getString("settings.streamer-username");
            Player player = Bukkit.getPlayer(streamerName);

            if (player != null && player.isOnline()) {
                String title = plugin.getConfig().getString("messages.poll-end-title");
                String sub = plugin.getConfig().getString("messages.poll-end-subtitle").replace("%winner%", winnerTitle);
                player.showTitle(Title.title(formatColor(title), formatColor(sub), titleTimes()));
                player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.0f);
                actionManager.executeAction(player, action);
            }

            if (plugin.getConfig().getBoolean("settings.broadcast-results")) {
                String broadcast = plugin.getConfig().getString("messages.broadcast-end").replace("%winner%", winnerTitle);
                Bukkit.broadcast(formatColor(broadcast));
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