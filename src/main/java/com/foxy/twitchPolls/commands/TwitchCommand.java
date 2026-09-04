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

    public TwitchCommand(TwitchPolls plugin, TwitchManager twitchManager, TikTokManager tikTokManager,
                         TestCommand testCommand) {
        this.plugin = plugin;
        this.twitchManager = twitchManager;
        this.tikTokManager = tikTokManager;
        this.testCommand = testCommand;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length > 0 && args[0].equalsIgnoreCase("reload")) {
            plugin.reloadConfig();
            plugin.reloadEventConfigs();
            testCommand.reload();
            plugin.reloadEventProviderConnections();
            sendMessage(sender, "messages.command-reload", "&aConfiguración recargada correctamente.");
            return true;
        }

        if (args.length > 0 && args[0].equalsIgnoreCase("reconnect")) {
            if ("tiktok".equalsIgnoreCase(plugin.getConfig().getString("settings.event-provider", "twitch"))) {
                tikTokManager.reconnect();
                sendMessage(sender, "messages.command-reconnect", "&aReintentando la conexión con TikTok...");
            } else {
                twitchManager.reload();
                sendMessage(sender, "messages.command-reconnect", "&aReintentando la conexión con Twitch...");
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

        if (args.length > 0 && !args[0].equalsIgnoreCase("poll")) {
            return false;
        }

        if ("tiktok".equalsIgnoreCase(plugin.getConfig().getString("settings.event-provider", "twitch"))) {
            sendMessage(sender, "messages.command-error", "&cLas encuestas requieren Twitch como proveedor de eventos.");
            return true;
        }

        sendMessage(sender, "messages.command-starting", "&aIniciando encuesta...");
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            twitchManager.createPoll();
            sendMessage(sender, "messages.command-success", "&aEncuesta enviada a Twitch exitosamente.");
        });
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return List.of("reload", "reconnect", "poll", "test").stream()
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