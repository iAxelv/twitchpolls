package com.foxy.streammanager.actions.common;

import com.foxy.streammanager.TwitchPolls;
import com.foxy.streammanager.actions.ActionContext;
import com.foxy.streammanager.actions.ActionStrategy;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class SurpriseRouletteAction implements ActionStrategy {

    @Override
    public void execute(ActionContext context) {
        Player player = context.player();
        List<ConfigurationSection> events = activeDonationEvents(
                ((TwitchPolls) context.plugin()).getEventConfig("donations"));
        if (events.isEmpty()) {
            ((TwitchPolls) context.plugin()).getLogger()
                    .warning("SURPRISE was triggered, but no active donation events are configured.");
            return;
        }

        long durationTicks = Math.max(1L,
                context.config().getLong("roulette-duration-seconds", 5L) * 20L);
        long intervalTicks = Math.max(1L,
                context.config().getLong("roulette-interval-ticks", 5L));

        new BukkitRunnable() {
            private long elapsedTicks;

            @Override
            public void run() {
                if (!player.isOnline()) {
                    cancel();
                    return;
                }

                if (elapsedTicks >= durationTicks) {
                    cancel();
                    ConfigurationSection winner = randomEvent(events);
                    showRouletteTitle(context, player,
                            winner.getString("title", winner.getName()), intervalTicks);
                    player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);
                    announceWinner(context, winner);
                    TwitchPolls plugin = (TwitchPolls) context.plugin();
                        Bukkit.getScheduler().runTask(plugin, () -> plugin.getActionManager()
                            .executeDonationAction(player, winner, context.redeemerUsername()));
                    return;
                }

                ConfigurationSection preview = randomEvent(events);
                showRouletteTitle(context, player,
                        preview.getString("title", preview.getName()), intervalTicks);
                player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.7f, 1.2f);
                elapsedTicks += intervalTicks;
            }
        }.runTaskTimer(context.plugin(), 0L, intervalTicks);
    }

    private List<ConfigurationSection> activeDonationEvents(ConfigurationSection donations) {
        List<ConfigurationSection> events = new ArrayList<>();
        if (donations == null) {
            return events;
        }
        for (String key : donations.getKeys(false)) {
            ConfigurationSection event = donations.getConfigurationSection(key);
            if (event != null && event.getBoolean("active", true)) {
                events.add(event);
            }
        }
        return events;
    }

    private ConfigurationSection randomEvent(List<ConfigurationSection> events) {
        return events.get(ThreadLocalRandom.current().nextInt(events.size()));
    }

    private void announceWinner(ActionContext context, ConfigurationSection winner) {
        TwitchPolls plugin = (TwitchPolls) context.plugin();
        String message = plugin.getLanguageManager().getString(
                "messages.surprise-roulette-winner", "&d[Surprise] &fSelected: &e%event%")
                .replace("%event%", winner.getString("title", winner.getName()));
        Bukkit.broadcast(LegacyComponentSerializer.legacyAmpersand().deserialize(message));
    }

    private void showRouletteTitle(ActionContext context, Player player,
                                   String eventName, long intervalTicks) {
        TwitchPolls plugin = (TwitchPolls) context.plugin();
        String title = plugin.getLanguageManager().getString(
                "messages.surprise-roulette-title", "&dSurprise");
        String subtitle = plugin.getLanguageManager().getString(
                "messages.surprise-roulette-subtitle", "&f%event%")
                .replace("%event%", eventName);
        player.showTitle(Title.title(component(title), component(subtitle),
                Title.Times.times(Duration.ZERO,
                        Duration.ofMillis(Math.max(100L, intervalTicks * 50L)), Duration.ZERO)));
    }

    private Component component(String text) {
        return LegacyComponentSerializer.legacyAmpersand().deserialize(text);
    }
}