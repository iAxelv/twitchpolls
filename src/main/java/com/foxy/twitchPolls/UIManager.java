package com.foxy.twitchPolls;

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
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@SuppressWarnings("deprecation")
public class UIManager {
    private final TwitchPolls plugin;
    private final Map<UUID, BukkitTask> countdowns = new HashMap<>();
    private BukkitTask pollBossBarTask;
    private BukkitTask testPollTask;
    private BossBar pollBossBar;
    private BossBar nextPollBossBar;

    public UIManager(TwitchPolls plugin) { this.plugin = plugin; }

    public void showPollStart(Player player, int duration) {
        showTitle(player, "messages.poll-start-title", "messages.poll-start-subtitle");
        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, 1.0f);
        startPollBossBar(player, duration);
    }

    public void showPollEnd(Player player, String winner, boolean playSound) {
        showTitle(player, "messages.poll-end-title", "messages.poll-end-subtitle", "%winner%", winner);
        if (playSound) player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.0f);
        stopPollBossBar();
    }

    public void showPointEvent(Player player, ConfigurationSection config) {
        showPointEvent(player, config, config.getInt("value", 0));
    }

    public void showPointEvent(Player player, ConfigurationSection config, int rewardCost) {
        showTitle(player, "messages.points-event-title", "messages.points-event-subtitle", "%event%", config.getString("title", config.getName()), "%value%", String.valueOf(rewardCost));
        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, 1.6f);
    }

    public void showDonationEvent(Player player, ConfigurationSection config) {
        String type = config.getString("type", "donación");
        showTitle(player, "messages.donation-event-title", "messages.donation-event-subtitle", "%event%", config.getString("title", config.getName()), "%value%", String.valueOf(config.getInt("value", 0)), "%type%", type);
        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, 1.6f);
    }

    public void showTikTokFollowEvent(Player player, ConfigurationSection config) {
        showTitle(player, "messages.tiktok-follow-event-title", "messages.tiktok-follow-event-subtitle",
                "%event%", config.getString("title", config.getName()));
        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, 1.6f);
    }

    public void testPoll(Player player, ConfigurationSection config, ActionManager actionManager) {
        int duration = Math.max(1, plugin.getConfig().getInt("settings.poll-duration-seconds", 45));
        showPollStart(player, duration);
        if (testPollTask != null) testPollTask.cancel();
        testPollTask = Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (!player.isOnline()) return;
            showPollEnd(player, config.getString("title", config.getName()), true);
            actionManager.executeAction(player, config);
            startEventCountdown(player, config);
            testPollTask = null;
        }, duration * 20L);
    }

    public void startEventCountdown(Player player, ConfigurationSection config) {
        if (!config.contains("duration-seconds")) return;
        UUID playerId = player.getUniqueId();
        BukkitTask previous = countdowns.remove(playerId);
        if (previous != null) previous.cancel();
        String format = plugin.getLanguageManager().getString("messages.points-event-duration", "&eEnds in: &f%time%s");
        int duration = Math.max(1, config.getInt("duration-seconds"));
        sendCountdown(player, format, duration);
        BukkitTask task = new BukkitRunnable() {
            private int remaining = duration;
            public void run() {
                remaining--;
                if (remaining <= 0) {
                    player.sendActionBar(LegacyComponentSerializer.legacyAmpersand().deserialize(""));
                    countdowns.remove(playerId);
                    cancel();
                } else sendCountdown(player, format, remaining);
            }
        }.runTaskTimer(plugin, 20L, 20L);
        countdowns.put(playerId, task);
    }

    public void cancelAll() {
        for (BukkitTask task : countdowns.values()) {
            task.cancel();
        }
        countdowns.clear();
        if (testPollTask != null) testPollTask.cancel();
        stopPollBossBar();
        stopNextPollBossBar();
    }

    public void startNextPoll(Player player, int seconds) {
        if (nextPollBossBar != null) return;
        String format = plugin.getLanguageManager().getString("messages.next-poll-bossbar-title", "&dNext poll: &f%time%s");
        nextPollBossBar = Bukkit.createBossBar(color(format.replace("%time%", String.valueOf(seconds))), BarColor.BLUE, BarStyle.SOLID);
        nextPollBossBar.addPlayer(player);
        nextPollBossBar.setProgress(1.0);
    }

    public void updateNextPoll(int remaining, int total) {
        if (nextPollBossBar == null) return;
        String format = plugin.getLanguageManager().getString("messages.next-poll-bossbar-title", "&dNext poll: &f%time%s");
        nextPollBossBar.setTitle(color(format.replace("%time%", String.valueOf(Math.max(0, remaining)))));
        nextPollBossBar.setProgress(Math.max(0.0, (double) remaining / total));
    }

    public void stopNextPoll() { stopNextPollBossBar(); }
    public void stopPoll() { stopPollBossBar(); }

    private void startPollBossBar(Player player, int total) {
        stopPollBossBar();
        String format = plugin.getLanguageManager().getString("messages.bossbar-title", "&dPoll: &f%time%s remaining");
        pollBossBar = Bukkit.createBossBar(color(format.replace("%time%", String.valueOf(total))), BarColor.PURPLE, BarStyle.SOLID);
        pollBossBar.addPlayer(player);
        final int[] remaining = {total};
        pollBossBarTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            remaining[0]--;
            if (remaining[0] <= 0) stopPollBossBar();
            else {
                pollBossBar.setTitle(color(format.replace("%time%", String.valueOf(remaining[0]))));
                pollBossBar.setProgress((double) remaining[0] / total);
            }
        }, 20L, 20L);
    }

    private void stopPollBossBar() {
        if (pollBossBar != null) { pollBossBar.removeAll(); pollBossBar = null; }
        if (pollBossBarTask != null) { pollBossBarTask.cancel(); pollBossBarTask = null; }
    }

    private void stopNextPollBossBar() { if (nextPollBossBar != null) { nextPollBossBar.removeAll(); nextPollBossBar = null; } }
    private void sendCountdown(Player player, String format, int seconds) { player.sendActionBar(LegacyComponentSerializer.legacyAmpersand().deserialize(format.replace("%time%", String.valueOf(seconds)))); }

    private void showTitle(Player player, String titlePath, String subtitlePath, String... replacements) {
        String title = plugin.getLanguageManager().getString(titlePath, "");
        String subtitle = plugin.getLanguageManager().getString(subtitlePath, "");
        for (int i = 0; i + 1 < replacements.length; i += 2) { title = title.replace(replacements[i], replacements[i + 1]); subtitle = subtitle.replace(replacements[i], replacements[i + 1]); }
        if (!title.isBlank() || !subtitle.isBlank()) player.showTitle(Title.title(component(title), component(subtitle), Title.Times.times(Duration.ofMillis(500), Duration.ofSeconds(3), Duration.ofMillis(1000))));
    }

    private Component component(String text) { return LegacyComponentSerializer.legacyAmpersand().deserialize(text == null ? "" : text); }
    private String color(String text) { return ChatColor.translateAlternateColorCodes('&', text == null ? "" : text); }
}