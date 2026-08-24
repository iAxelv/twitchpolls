package com.foxy.twitchPolls.commands;

import com.foxy.twitchPolls.TwitchManager;
import com.foxy.twitchPolls.TwitchPolls;
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
    private final TestCommand testCommand;

    public TwitchCommand(TwitchPolls plugin, TwitchManager twitchManager, TestCommand testCommand) {
        this.plugin = plugin;
        this.twitchManager = twitchManager;
        this.testCommand = testCommand;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length > 0 && args[0].equalsIgnoreCase("reload")) {
            plugin.reloadConfig();
            testCommand.reload();
            twitchManager.reload();
            sendMessage(sender, "messages.command-reload", "&aConfiguración recargada correctamente.");
            return true;
        }

        if (args.length > 0 && args[0].equalsIgnoreCase("test")) {
            if (!(sender instanceof org.bukkit.entity.Player player)) {
                sender.sendMessage("Este subcomando solo puede ejecutarse dentro del juego.");
                return true;
            }
            testCommand.open(player);
            return true;
        }

        if (args.length > 0 && !args[0].equalsIgnoreCase("poll")) {
            return false;
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
            return List.of("reload", "poll", "test").stream()
                    .filter(option -> option.startsWith(args[0].toLowerCase()))
                    .toList();
        }
        return Collections.emptyList();
    }

    private void sendMessage(CommandSender sender, String path, String fallback) {
        String message = plugin.getConfig().getString(path, fallback);
        sender.sendMessage(LegacyComponentSerializer.legacyAmpersand().deserialize(message));
    }
}