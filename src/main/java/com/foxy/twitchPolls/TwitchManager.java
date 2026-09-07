package com.foxy.twitchPolls;

import com.github.philippheuer.credentialmanager.domain.OAuth2Credential;
import com.github.twitch4j.TwitchClient;
import com.github.twitch4j.TwitchClientBuilder;
import com.google.common.io.CharStreams;
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

import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicLong;
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
    private BukkitTask eventSubscriptionTask;
    private final AtomicLong connectionGeneration = new AtomicLong();
    private volatile boolean automaticPollActive;
    private final Map<String, ConfigurationSection> activePollChoices = new HashMap<>();
    private final Map<String, Integer> pollOptionCooldowns = new HashMap<>();

    public TwitchManager(TwitchPolls plugin, ActionManager actionManager, UIManager uiManager, SessionManager sessionManager) {
        this.plugin = plugin;
        this.actionManager = actionManager;
        this.uiManager = uiManager;
        this.sessionManager = sessionManager;
    }

    public void connect() {
        long generation = connectionGeneration.incrementAndGet();
        configureTwitchRequestTimeout();
        String clientId = plugin.getCredentialsManager().getCredential("twitch.client-id");
        String clientSecret = plugin.getCredentialsManager().getCredential("twitch.client-secret");
        String oauthToken = cleanToken(plugin.getCredentialsManager().getCredential("twitch.oauth-token"));
        String refreshToken = plugin.getCredentialsManager().getCredential("twitch.refresh-token", "");
        String broadcasterId = plugin.getCredentialsManager().getCredential("twitch.broadcaster-id");

        if (oauthToken == null || oauthToken.isBlank()) {
            plugin.getLogger().warning("Twitch OAuth token is empty. Please configure it in secrets.yml.");
            return;
        }

        if (!refreshToken.isBlank()) {
            String refreshed = refreshAccessToken(clientId, clientSecret, refreshToken);
            if (refreshed != null) {
                oauthToken = refreshed;
            }
        }

        plugin.getCredentialsManager().setCredential("twitch.oauth-token", oauthToken);

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
        scheduleEventSubscriptions(broadcasterId, generation);

        startPollCycle();
    }

    private void configureTwitchRequestTimeout() {
        String property = "hystrix.command.default.execution.isolation.thread.timeoutInMilliseconds";
        if (System.getProperty(property) == null) {
            System.setProperty(property, "30000");
        }
    }

    public void disconnect() {
        connectionGeneration.incrementAndGet();
        if (eventSubscriptionTask != null) {
            eventSubscriptionTask.cancel();
            eventSubscriptionTask = null;
        }
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
            twitchClient = null;
        }
    }

    private void scheduleEventSubscriptions(String broadcasterId, long generation) {
        eventSubscriptionTask = Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, new Runnable() {
            private int index;

            @Override
            public void run() {
                if (generation != connectionGeneration.get() || twitchClient == null || index >= 6) {
                    if (index >= 6 && eventSubscriptionTask != null) {
                        eventSubscriptionTask.cancel();
                        eventSubscriptionTask = null;
                    }
                    return;
                }

                switch (index++) {
                    case 0 -> twitchClient.getEventSocket().register(
                            SubscriptionTypes.POLL_END,
                            ChannelPollEndCondition.builder().broadcasterUserId(broadcasterId).build());
                    case 1 -> twitchClient.getEventSocket().register(
                            SubscriptionTypes.CHANNEL_POINTS_CUSTOM_REWARD_REDEMPTION_ADD,
                            ChannelPointsCustomRewardRedemptionAddCondition.builder()
                                    .broadcasterUserId(broadcasterId).build());
                    case 2 -> twitchClient.getEventSocket().register(
                            SubscriptionTypes.CHANNEL_CHEER,
                            ChannelCheerCondition.builder().broadcasterUserId(broadcasterId).build());
                    case 3 -> twitchClient.getEventSocket().register(
                            SubscriptionTypes.CHANNEL_SUBSCRIBE,
                            ChannelSubscribeCondition.builder().broadcasterUserId(broadcasterId).build());
                    case 4 -> twitchClient.getEventSocket().register(
                            SubscriptionTypes.CHANNEL_SUBSCRIPTION_GIFT,
                            ChannelSubscriptionGiftCondition.builder().broadcasterUserId(broadcasterId).build());
                    case 5 -> twitchClient.getEventSocket().register(
                            SubscriptionTypes.CHANNEL_SUBSCRIPTION_MESSAGE,
                            ChannelSubscriptionMessageCondition.builder().broadcasterUserId(broadcasterId).build());
                    default -> { }
                }
            }
        }, 0L, 20L);
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

        String broadcasterId = plugin.getCredentialsManager().getCredential("twitch.broadcaster-id");
        String clientId = plugin.getCredentialsManager().getCredential("twitch.client-id");
        String clientSecret = plugin.getCredentialsManager().getCredential("twitch.client-secret");
        String oauthToken = cleanToken(plugin.getCredentialsManager().getCredential("twitch.oauth-token"));
        String refreshToken = plugin.getCredentialsManager().getCredential("twitch.refresh-token", "");
        int duration = plugin.getConfig().getInt("settings.poll-duration-seconds");
        String pollTitle = plugin.getLanguageManager().getString("messages.poll-title", "Poll");

        if (!refreshToken.isBlank()) {
            String refreshed = refreshAccessToken(clientId, clientSecret, refreshToken);
            if (refreshed != null) {
                oauthToken = refreshed;
                plugin.getCredentialsManager().setCredential("twitch.oauth-token", oauthToken);
            }
        }
        
        if (pollTitle == null || pollTitle.isEmpty()) {
            plugin.getLogger().log(Level.WARNING, "Poll title not configured in language file");
            pollTitle = "Poll";
        }

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

        List<String> availableKeys = eventKeys.stream()
            .filter(key -> pollOptionCooldowns.getOrDefault(key, 0) <= 0)
            .collect(Collectors.toCollection(ArrayList::new));

        if (availableKeys.isEmpty()) {
            int lowestCooldown = eventKeys.stream()
                .mapToInt(key -> pollOptionCooldowns.getOrDefault(key, 0))
                .min()
                .orElse(0);
            availableKeys = eventKeys.stream()
                .filter(key -> pollOptionCooldowns.getOrDefault(key, 0) == lowestCooldown)
                .collect(Collectors.toCollection(ArrayList::new));
            plugin.getLogger().warning("All poll options are on cooldown; using the options with the shortest remaining cooldown.");
        }

        Collections.shuffle(availableKeys);
        List<String> selectedKeys = new ArrayList<>(availableKeys.subList(0, Math.min(3, availableKeys.size())));

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
            advancePollOptionCooldowns();
            int cooldown = Math.max(0, plugin.getConfig().getInt("settings.poll-option-cooldown-polls", 0));
            for (String key : selectedKeys) {
                if (cooldown == 0) {
                    pollOptionCooldowns.remove(key);
                } else {
                    pollOptionCooldowns.put(key, cooldown);
                }
            }
            notifyStart(duration, choices);
        } catch (Exception exception) {
            plugin.getLogger().log(Level.SEVERE,
                    "Failed to create Twitch poll for broadcaster " + broadcasterId,
                    exception);
            scheduleAutomaticPollRestart();
        }
    }

    public void executePointAction(org.bukkit.entity.Player player, ConfigurationSection actionConfig, String username) {
        executePointAction(player, actionConfig, username, actionConfig == null ? 0 : actionConfig.getInt("value", 0));
    }

    private void executePointAction(org.bukkit.entity.Player player, ConfigurationSection actionConfig,
                                    String username, int rewardCost) {
        if (actionConfig == null || !player.isOnline()) return;
        uiManager.showPointEvent(player, actionConfig, rewardCost);
        actionManager.executePointAction(player, actionConfig, username);
        String broadcast = plugin.getLanguageManager().getString("messages.points-event-broadcast", "")
                .replace("%event%", actionConfig.getString("title", actionConfig.getName()))
            .replace("%value%", String.valueOf(rewardCost))
            .replace("%username%", username == null || username.isBlank() ? "unknown" : username);
        if (!broadcast.isBlank()) {
            Bukkit.broadcast(formatColor(broadcast));
        }
    }

    public void executeDonationAction(org.bukkit.entity.Player player, ConfigurationSection actionConfig, String username) {
        executeDonationAction(player, actionConfig, username, "Twitch");
    }

    public void executeDonationAction(org.bukkit.entity.Player player, ConfigurationSection actionConfig,
                                      String username, String provider) {
        if (actionConfig == null || !player.isOnline()) return;
        uiManager.showDonationEvent(player, actionConfig);
        actionManager.executeDonationAction(player, actionConfig, username);
        String eventTitle = actionConfig.getString("title", actionConfig.getName());
        int value = actionConfig.getInt("value", 0);
        String type = donationTypeLabel(actionConfig.getString("type", "donation"));
        String broadcast = plugin.getLanguageManager().getString(
                "messages.donation-event-broadcast",
            "&d[" + provider + "] &f%event% &7was activated by &e%value% %type% &7(%username%)")
            .replace("%provider%", provider)
                .replace("%event%", eventTitle)
                .replace("%value%", String.valueOf(value))
                .replace("%type%", type)
                .replace("%username%", username == null || username.isBlank() ? "unknown" : username);
        if (!broadcast.isBlank()) {
            Bukkit.broadcast(formatColor(broadcast));
        }
    }

    private String donationTypeLabel(String type) {
        return switch (type.toLowerCase()) {
            case "bits" -> "bits";
            case "gift_sub" -> "gift subs";
            case "prime_sub" -> "prime sub";
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
                List<String> broadcastLines = plugin.getLanguageManager().getStringList("messages.broadcast-start");
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
                uiManager.showPollEnd(player, winnerTitle, true);

                if (actionConfig != null) {
                    if ("SURPRISE".equalsIgnoreCase(actionConfig.getString("action", ""))) {
                        actionManager.executeAction(player, actionConfig);
                    } else {
                        actionManager.executeAction(player, actionConfig);
                        uiManager.startEventCountdown(player, actionConfig);
                    }
                }
            }

            if (plugin.getConfig().getBoolean("settings.broadcast-results")) {
                String broadcast = plugin.getLanguageManager().getString("messages.broadcast-end", "").replace("%winner%", winnerTitle);
                Bukkit.broadcast(formatColor(broadcast));
            }

            if (automaticPollActive) {
                scheduleAutomaticPollRestart();
            }
        });
    }

    private void advancePollOptionCooldowns() {
        pollOptionCooldowns.replaceAll((key, remaining) -> Math.max(0, remaining - 1));
        pollOptionCooldowns.entrySet().removeIf(entry -> entry.getValue() <= 0);
    }

    private void onPointRedemption(CustomRewardRedemptionAddEvent event) {
        if (event.getReward() == null || event.getReward().getTitle() == null) {
            return;
        }

        ConfigurationSection points = plugin.getEventConfig("points");
        if (points == null) {
            return;
        }

        ConfigurationSection matched = null;
        for (String key : points.getKeys(false)) {
            ConfigurationSection candidate = points.getConfigurationSection(key);
            if (candidate == null || !candidate.getBoolean("active", true)) {
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
        Integer rewardCost = event.getReward().getCost();
        String username = event.getUserName();
        Bukkit.getScheduler().runTask(plugin, () -> {
            org.bukkit.entity.Player player = sessionManager.getStreamer();
            if (player != null && player.isOnline()) {
                int currentCost = rewardCost == null ? selected.getInt("value", 0) : rewardCost;
                executePointAction(player, selected, username, currentCost);
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

    private String refreshAccessToken(String clientId, String clientSecret, String refreshToken) {
        if (clientId == null || clientId.isBlank() || clientSecret == null || clientSecret.isBlank() || refreshToken == null || refreshToken.isBlank()) {
            return null;
        }

        try {
            String encodedClientId = URLEncoder.encode(clientId, StandardCharsets.UTF_8);
            String encodedClientSecret = URLEncoder.encode(clientSecret, StandardCharsets.UTF_8);
            String encodedRefreshToken = URLEncoder.encode(refreshToken, StandardCharsets.UTF_8);

            String url = "https://id.twitch.tv/oauth2/token"
                    + "?client_id=" + encodedClientId
                    + "&client_secret=" + encodedClientSecret
                    + "&grant_type=refresh_token"
                    + "&refresh_token=" + encodedRefreshToken;

            HttpURLConnection connection = (HttpURLConnection) URI.create(url).toURL().openConnection();
            connection.setRequestMethod("POST");
            connection.setConnectTimeout(10000);
            connection.setReadTimeout(10000);
            connection.setDoOutput(false);
            connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");

            int status = connection.getResponseCode();
            String responseBody = status >= 200 && status < 300
                    ? CharStreams.toString(new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))
                    : CharStreams.toString(new InputStreamReader(connection.getErrorStream(), StandardCharsets.UTF_8));

            if (status < 200 || status >= 300) {
                plugin.getLogger().log(Level.WARNING, "Twitch token refresh failed: {0}", responseBody);
                return null;
            }

            String newAccessToken = CredentialsManager.extractJsonString(responseBody, "access_token");
            String newRefreshToken = CredentialsManager.extractJsonString(responseBody, "refresh_token");

            if (newAccessToken == null || newAccessToken.isBlank()) {
                plugin.getLogger().warning("Twitch token refresh response did not include access_token");
                return null;
            }

            plugin.getCredentialsManager().setCredential("twitch.oauth-token", newAccessToken);
            if (newRefreshToken != null && !newRefreshToken.isBlank()) {
                plugin.getCredentialsManager().setCredential("twitch.refresh-token", newRefreshToken);
            }

            plugin.getLogger().info("Twitch OAuth token refreshed successfully.");
            return newAccessToken;
        } catch (IOException exception) {
            plugin.getLogger().log(Level.SEVERE, "Error refreshing Twitch OAuth token", exception);
            return null;
        }
    }

    private net.kyori.adventure.text.Component formatColor(String text) {
        return LegacyComponentSerializer.legacyAmpersand().deserialize(text == null ? "" : text);
    }

}