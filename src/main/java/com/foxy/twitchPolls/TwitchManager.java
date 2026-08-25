package com.foxy.twitchPolls;

import com.github.philippheuer.credentialmanager.domain.OAuth2Credential;
import com.github.twitch4j.TwitchClient;
import com.github.twitch4j.TwitchClientBuilder;
import com.github.twitch4j.eventsub.condition.ChannelPollEndCondition;
import com.github.twitch4j.eventsub.condition.ChannelCheerCondition;
import com.github.twitch4j.eventsub.condition.ChannelSubscribeCondition;
import com.github.twitch4j.eventsub.condition.ChannelSubscriptionGiftCondition;
import com.github.twitch4j.eventsub.condition.ChannelSubscriptionMessageCondition;
import com.github.twitch4j.eventsub.domain.PollChoice;
import com.github.twitch4j.eventsub.events.ChannelPollEndEvent;
import com.github.twitch4j.eventsub.events.ChannelCheerEvent;
import com.github.twitch4j.eventsub.events.ChannelSubscribeEvent;
import com.github.twitch4j.eventsub.events.ChannelSubscriptionGiftEvent;
import com.github.twitch4j.eventsub.events.ChannelSubscriptionMessageEvent;
import com.github.twitch4j.eventsub.events.CustomRewardRedemptionAddEvent;
import com.github.twitch4j.eventsub.condition.ChannelPointsCustomRewardRedemptionAddCondition;
import com.github.twitch4j.eventsub.subscriptions.SubscriptionTypes;
import com.github.twitch4j.common.enums.SubscriptionPlan;
import com.github.twitch4j.helix.domain.Poll;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;
import java.util.logging.Level;

public class TwitchManager {

    private final TwitchPolls plugin;
    private final ActionManager actionManager;
    private final UIManager uiManager;
    private final SessionManager sessionManager;
    private TwitchClient twitchClient;
    private BukkitTask pollTask;
    private BukkitTask pollResumeTask;
    private volatile boolean automaticPollActive;
    private final Map<String, ConfigurationSection> activePollChoices = new HashMap<>();

    public TwitchManager(TwitchPolls plugin, ActionManager actionManager, UIManager uiManager, SessionManager sessionManager) {
        this.plugin = plugin;
        this.actionManager = actionManager;
        this.uiManager = uiManager;
        this.sessionManager = sessionManager;
    }

    public void connect() {
        String clientId = plugin.getConfig().getString("twitch.client-id");
        String clientSecret = plugin.getConfig().getString("twitch.client-secret");
        String oauthToken = cleanToken(plugin.getConfig().getString("twitch.oauth-token"));
        String refreshToken = plugin.getConfig().getString("twitch.refresh-token", "");
        String broadcasterId = plugin.getConfig().getString("twitch.broadcaster-id");

        OAuth2Credential credential = new OAuth2Credential("twitch", oauthToken, refreshToken, null, null, null, null);

        twitchClient = TwitchClientBuilder.builder()
                .withClientId(clientId)
                .withClientSecret(clientSecret)
                .withEnableHelix(true)
                .withEnableEventSocket(true)
                .withDefaultAuthToken(credential)
                .build();

        twitchClient.getEventManager().onEvent(ChannelPollEndEvent.class, this::onPollEnd);
        twitchClient.getEventManager().onEvent(CustomRewardRedemptionAddEvent.class, this::onPointRedemption);
        twitchClient.getEventManager().onEvent(ChannelCheerEvent.class, this::onCheer);
        twitchClient.getEventManager().onEvent(ChannelSubscribeEvent.class, this::onSubscribe);
        twitchClient.getEventManager().onEvent(ChannelSubscriptionGiftEvent.class, this::onGiftSubscription);
        twitchClient.getEventManager().onEvent(ChannelSubscriptionMessageEvent.class, this::onSubscriptionMessage);
        twitchClient.getEventSocket().register(
                SubscriptionTypes.POLL_END,
                ChannelPollEndCondition.builder().broadcasterUserId(broadcasterId).build()
        );
        twitchClient.getEventSocket().register(
                SubscriptionTypes.CHANNEL_POINTS_CUSTOM_REWARD_REDEMPTION_ADD,
                ChannelPointsCustomRewardRedemptionAddCondition.builder().broadcasterUserId(broadcasterId).build()
        );
        twitchClient.getEventSocket().register(
                SubscriptionTypes.CHANNEL_CHEER,
                ChannelCheerCondition.builder().broadcasterUserId(broadcasterId).build()
        );
        twitchClient.getEventSocket().register(
                SubscriptionTypes.CHANNEL_SUBSCRIBE,
                ChannelSubscribeCondition.builder().broadcasterUserId(broadcasterId).build()
        );
        twitchClient.getEventSocket().register(
                SubscriptionTypes.CHANNEL_SUBSCRIPTION_GIFT,
                ChannelSubscriptionGiftCondition.builder().broadcasterUserId(broadcasterId).build()
        );
        twitchClient.getEventSocket().register(
                SubscriptionTypes.CHANNEL_SUBSCRIPTION_MESSAGE,
                ChannelSubscriptionMessageCondition.builder().broadcasterUserId(broadcasterId).build()
        );

        startPollCycle();
    }

    public void disconnect() {
        if (pollTask != null) {
            pollTask.cancel();
            pollTask = null;
        }
        if (pollResumeTask != null) {
            pollResumeTask.cancel();
            pollResumeTask = null;
        }
        automaticPollActive = false;
        uiManager.cancelAll();
        if (twitchClient != null) {
            twitchClient.close();
        }
    }

    public void reload() {
        disconnect();
        connect();
    }

    private void startPollCycle() {
        if (!plugin.getConfig().getBoolean("settings.automatic-polls", false)
                || automaticPollActive || pollTask != null) {
            return;
        }
        int interval = plugin.getConfig().getInt("settings.poll-interval-seconds") * 20;
        int intervalSeconds = Math.max(1, interval / 20);
        final int[] timeRemaining = {intervalSeconds};

        pollTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            org.bukkit.entity.Player player = sessionManager.getStreamer();
            if (player == null || !player.isOnline()) {
                timeRemaining[0] = intervalSeconds;
                uiManager.stopNextPoll();
                return;
            }

            uiManager.startNextPoll(player, intervalSeconds);
            timeRemaining[0]--;
            uiManager.updateNextPoll(timeRemaining[0], intervalSeconds);
            if (timeRemaining[0] <= 0) {
                timeRemaining[0] = intervalSeconds;
                automaticPollActive = true;
                pauseAutomaticPolls();
                Bukkit.getScheduler().runTaskAsynchronously(plugin, this::createPoll);
            }
        }, 20L, 20L);
    }

    private void pauseAutomaticPolls() {
        if (pollTask != null) {
            pollTask.cancel();
            pollTask = null;
        }
        uiManager.stopNextPoll();
    }

    private void scheduleAutomaticPollRestart() {
        if (pollResumeTask != null) {
            pollResumeTask.cancel();
        }
        pollResumeTask = Bukkit.getScheduler().runTaskLater(plugin, () -> {
            pollResumeTask = null;
            automaticPollActive = false;
            startPollCycle();
        }, 5L * 20L);
    }

    public void createPoll() {
        automaticPollActive = true;
        Bukkit.getScheduler().runTask(plugin, this::pauseAutomaticPolls);

        String broadcasterId = plugin.getConfig().getString("twitch.broadcaster-id");
        String oauthToken = cleanToken(plugin.getConfig().getString("twitch.oauth-token"));
        int duration = plugin.getConfig().getInt("settings.poll-duration-seconds");
        String pollTitle = plugin.getConfig().getString("messages.poll-title");

        ConfigurationSection eventsSection = plugin.getEventConfig("polls");

        if (eventsSection == null) {
            scheduleAutomaticPollRestart();
            return;
        }

        List<String> eventKeys = new ArrayList<>();
        for (String key : eventsSection.getKeys(false)) {
            if (eventsSection.getBoolean(key + ".active", true)) {
                eventKeys.add(key);
            }
        }

        if (eventKeys.isEmpty()) {
            scheduleAutomaticPollRestart();
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
            scheduleAutomaticPollRestart();
        }
    }

    public void executePointAction(org.bukkit.entity.Player player, ConfigurationSection actionConfig, String username) {
        if (actionConfig == null || !player.isOnline()) return;
        uiManager.showPointEvent(player, actionConfig);
        actionManager.executePointAction(player, actionConfig, username);
        String broadcast = plugin.getConfig().getString("messages.points-event-broadcast", "")
                .replace("%event%", actionConfig.getString("title", actionConfig.getName()))
            .replace("%value%", String.valueOf(actionConfig.getInt("value", 0)))
            .replace("%username%", username == null || username.isBlank() ? "desconocido" : username);
        if (!broadcast.isBlank()) {
            Bukkit.broadcast(formatColor(broadcast));
        }
    }

    public void executeDonationAction(org.bukkit.entity.Player player, ConfigurationSection actionConfig, String username) {
        if (actionConfig == null || !player.isOnline()) return;
        uiManager.showDonationEvent(player, actionConfig);
        actionManager.executeDonationAction(player, actionConfig, username);
        String eventTitle = actionConfig.getString("title", actionConfig.getName());
        int value = actionConfig.getInt("value", 0);
        String type = donationTypeLabel(actionConfig.getString("type", "donación"));
        String broadcast = plugin.getConfig().getString(
                "messages.donation-event-broadcast",
                "&d[Twitch] &f%event% &7se activó por &e%value% %type% &7(%username%)")
                .replace("%event%", eventTitle)
                .replace("%value%", String.valueOf(value))
                .replace("%type%", type)
                .replace("%username%", username == null || username.isBlank() ? "desconocido" : username);
        if (!broadcast.isBlank()) {
            Bukkit.broadcast(formatColor(broadcast));
        }
    }

    private String donationTypeLabel(String type) {
        return switch (type.toLowerCase()) {
            case "bits" -> "bits";
            case "gift_sub" -> "subs de regalo";
            case "prime_sub" -> "sub prime";
            case "sub" -> "subs";
            default -> type;
        };
    }

    private void notifyStart(int durationSeconds, List<PollChoice> choices) {
        Bukkit.getScheduler().runTask(plugin, () -> {
            org.bukkit.entity.Player player = sessionManager.getStreamer();

            if (player != null && player.isOnline()) {
                uiManager.showPollStart(player, durationSeconds);
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
            uiManager.stopPoll();
            org.bukkit.entity.Player player = sessionManager.getStreamer();

            if (player != null && player.isOnline()) {
                uiManager.showPollEnd(player, winnerTitle, actionConfig == null || !"RANDOM_SOUND".equals(actionConfig.getString("action")));

                if (actionConfig != null) {
                    actionManager.executeAction(player, actionConfig);
                }
            }

            if (plugin.getConfig().getBoolean("settings.broadcast-results")) {
                String broadcast = plugin.getConfig().getString("messages.broadcast-end").replace("%winner%", winnerTitle);
                Bukkit.broadcast(formatColor(broadcast));
            }

            if (automaticPollActive) {
                scheduleAutomaticPollRestart();
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
        String username = event.getUserName();
        Bukkit.getScheduler().runTask(plugin, () -> {
            org.bukkit.entity.Player player = sessionManager.getStreamer();
            if (player != null && player.isOnline()) {
                executePointAction(player, selected, username);
            }
        });
    }

    private void onCheer(ChannelCheerEvent event) {
        if (event.getBits() != null) {
            handleDonation("bits", event.getBits(), event.getUserName());
        }
    }

    private void onSubscribe(ChannelSubscribeEvent event) {
        String type = event.getTier() == SubscriptionPlan.TWITCH_PRIME
                ? "prime_sub" : "sub";
        handleDonation(type, 1, event.getUserName());
    }

    private void onGiftSubscription(ChannelSubscriptionGiftEvent event) {
        if (event.getTotal() != null) {
            handleDonation("gift_sub", event.getTotal(), event.getUserName());
        }
    }

    private void onSubscriptionMessage(ChannelSubscriptionMessageEvent event) {
        String type = event.getTier() == SubscriptionPlan.TWITCH_PRIME
                ? "prime_sub" : "sub";
        handleDonation(type, 1, event.getUserName());
    }

    private void handleDonation(String type, int value, String username) {
        ConfigurationSection donations = plugin.getEventConfig("donations");
        if (donations == null) {
            return;
        }

        ConfigurationSection selected = null;
        for (String key : donations.getKeys(false)) {
            ConfigurationSection candidate = donations.getConfigurationSection(key);
            if (candidate != null && candidate.getBoolean("active", true)
                    && type.equalsIgnoreCase(candidate.getString("type", ""))
                    && candidate.getInt("value", -1) == value) {
                selected = candidate;
                break;
            }
        }
        if (selected == null) {
            return;
        }

        ConfigurationSection actionConfig = selected;
        Bukkit.getScheduler().runTask(plugin, () -> {
            org.bukkit.entity.Player player = sessionManager.getStreamer();
            if (player != null && player.isOnline()) {
                executeDonationAction(player, actionConfig, username);
            }
        });
    }

    private String cleanToken(String token) {
        if (token == null) return "";
        return token.startsWith("oauth:") ? token.substring(6) : token;
    }

    private net.kyori.adventure.text.Component formatColor(String text) {
        return LegacyComponentSerializer.legacyAmpersand().deserialize(text == null ? "" : text);
    }

}