package com.foxy.twitchPolls.commands;

import com.foxy.twitchPolls.TwitchManager;
import com.foxy.twitchPolls.TwitchPolls;
import com.foxy.twitchPolls.TikTokManager;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.Collections;
import java.util.List;

public class TwitchCommand implements CommandExecutor, TabCompleter {
    private final TwitchPolls plugin;
    private final TwitchManager twitchManager;
    private final TikTokManager tikTokManager;
    private final TestCommand testCommand;
    private final ConfigCommand configCommand;

    public TwitchCommand(TwitchPolls plugin, TwitchManager twitchManager, TikTokManager tikTokManager,
                         TestCommand testCommand, ConfigCommand configCommand) {
        this.plugin = plugin;
        this.twitchManager = twitchManager;
        this.tikTokManager = tikTokManager;
        this.testCommand = testCommand;
        this.configCommand = configCommand;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length > 0 && args[0].equalsIgnoreCase("reload")) {
            plugin.reloadConfig();
            plugin.getCredentialsManager().reload();
            plugin.getLanguageManager().reload();
            plugin.reloadEventConfigs();
            testCommand.reload();
            configCommand.reload();
            plugin.reloadEventProviderConnections();
            sendMessage(sender, "messages.command-reload", "&aConfiguration reloaded successfully.");
            return true;
        }

        if (args.length > 0 && args[0].equalsIgnoreCase("reconnect")) {
            if ("tiktok".equalsIgnoreCase(plugin.getConfig().getString("settings.event-provider", "twitch"))) {
                tikTokManager.reconnect();
                sendMessage(sender, "messages.command-reconnect", "&aRetrying the TikTok connection...");
            } else {
                twitchManager.reload();
                sendMessage(sender, "messages.command-reconnect", "&aRetrying the Twitch connection...");
            }
            return true;
        }

        if (args.length > 0 && args[0].equalsIgnoreCase("test")) {
            if (!(sender instanceof org.bukkit.entity.Player player)) {
                sender.sendMessage(LegacyComponentSerializer.legacyAmpersand().deserialize(
                        plugin.getLanguageManager().getString("messages.command-player-only",
                                "&cThis subcommand can only be used in-game.")));
                return true;
            }
            testCommand.open(player);
            return true;
        }

        if (args.length > 0 && args[0].equalsIgnoreCase("config")) {
            if (!(sender instanceof org.bukkit.entity.Player player)) {
                sendMessage(sender, "messages.command-player-only", "&cThis subcommand can only be used in-game.");
                return true;
            }
            configCommand.open(player);
            return true;
        }

        return false;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return List.of("reload", "reconnect", "test", "config").stream()
                    .filter(option -> option.startsWith(args[0].toLowerCase()))
                    .toList();
        }
        return Collections.emptyList();
    }

    private void sendMessage(CommandSender sender, String path, String fallback) {
        String message = plugin.getLanguageManager().getString(path, fallback);
        sender.sendMessage(LegacyComponentSerializer.legacyAmpersand().deserialize(message));
    }
}