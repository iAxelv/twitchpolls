package com.foxy.streammanager.actions.tiktok;

import com.foxy.streammanager.TwitchPolls;
import com.foxy.streammanager.actions.ActionContext;
import com.foxy.streammanager.actions.ActionStrategy;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.Sound;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

public class HeartMeRouletteAction implements ActionStrategy {
    private static final Map<UUID, Long> COOLDOWNS = new ConcurrentHashMap<>();

    @Override public void execute(ActionContext context) {
        Player player = context.player();
        long now = System.currentTimeMillis();
        long cooldownMillis = Math.max(0L, context.config().getLong("cooldown-seconds", 10L)) * 1000L;
        Long availableAt = COOLDOWNS.get(player.getUniqueId());
        if (availableAt != null && availableAt > now) {
            return;
        }
        ConfigurationSection donationEvents = ((TwitchPolls) context.plugin()).getEventConfig("donations");
        List<ConfigurationSection> events = activeDonationEvents(donationEvents);
        if (events.isEmpty()) {
            return;
        }
        COOLDOWNS.put(player.getUniqueId(), now + cooldownMillis);

        long durationTicks = Math.max(1L, context.config().getLong("roulette-duration-seconds", 5L) * 20L);
        long intervalTicks = Math.max(1L, context.config().getLong("roulette-interval-ticks", 5L));
        new org.bukkit.scheduler.BukkitRunnable() {
            private long elapsedTicks;

            @Override public void run() {
                if (!player.isOnline()) {
                    cancel();
                    return;
                }
                if (elapsedTicks >= durationTicks) {
                    cancel();
                    ConfigurationSection winner = events.get(ThreadLocalRandom.current().nextInt(events.size()));
                    showRouletteTitle(context, player, winner.getString("title", winner.getName()), intervalTicks);
                    player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);
                    announceWinner(context, winner);
                    Bukkit.getScheduler().runTask(context.plugin(), () ->
                            ((TwitchPolls) context.plugin()).getActionManager()
                                    .executeDonationAction(player, winner, context.redeemerUsername()));
                    return;
                }
                ConfigurationSection preview = events.get(ThreadLocalRandom.current().nextInt(events.size()));
                showRouletteTitle(context, player, preview.getString("title", preview.getName()), intervalTicks);
                player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.7f, 1.2f);
                elapsedTicks += intervalTicks;
            }
        }.runTaskTimer(context.plugin(), 0L, intervalTicks);
    }

    private List<ConfigurationSection> activeDonationEvents(ConfigurationSection donationEvents) {
        List<ConfigurationSection> events = new ArrayList<>();
        if (donationEvents == null) {
            return events;
        }
        for (String key : donationEvents.getKeys(false)) {
            ConfigurationSection event = donationEvents.getConfigurationSection(key);
            if (event != null && event.getBoolean("active", true)
                    && !"HEART_ME_ROULETTE".equalsIgnoreCase(event.getString("action", ""))) {
                events.add(event);
            }
        }
        return events;
    }

    private void announceWinner(ActionContext context, ConfigurationSection winner) {
        TwitchPolls plugin = (TwitchPolls) context.plugin();
        String message = plugin.getLanguageManager().getString(
                "messages.tiktok-heart-me-winner", "&d[TikTok] &fWinner: &e%event%")
                .replace("%event%", winner.getString("title", winner.getName()));
        Bukkit.broadcast(LegacyComponentSerializer.legacyAmpersand().deserialize(message));
    }

    private void showRouletteTitle(ActionContext context, Player player, String eventName, long intervalTicks) {
        TwitchPolls plugin = (TwitchPolls) context.plugin();
        String title = plugin.getLanguageManager().getString(
                "messages.tiktok-heart-me-title", "&dRuleta");
        String subtitle = plugin.getLanguageManager().getString(
                "messages.tiktok-heart-me-subtitle", "&f%event%").replace("%event%", eventName);
        player.showTitle(Title.title(component(title), component(subtitle),
                Title.Times.times(Duration.ZERO, Duration.ofMillis(Math.max(100L, intervalTicks * 50L)), Duration.ZERO)));
    }

    private Component component(String text) {
        return LegacyComponentSerializer.legacyAmpersand().deserialize(text);
    }

}