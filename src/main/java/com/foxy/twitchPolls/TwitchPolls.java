package com.foxy.twitchPolls;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

import java.util.logging.Level;

public final class TwitchPolls extends JavaPlugin {

    private TwitchManager twitchManager;
    private ActionManager actionManager;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        actionManager = new ActionManager();
        twitchManager = new TwitchManager(this, actionManager);
        twitchManager.connect();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (command.getName().equalsIgnoreCase("poll")) {
            if (args.length > 0 && args[0].equalsIgnoreCase("reload")) {
                reloadConfig();
                twitchManager.reload();
                sender.sendMessage(LegacyComponentSerializer.legacyAmpersand().deserialize(getConfig().getString("messages.command-reload")));
                return true;
            }

            sender.sendMessage(LegacyComponentSerializer.legacyAmpersand().deserialize(getConfig().getString("messages.command-starting")));
            getServer().getScheduler().runTaskAsynchronously(this, () -> {
                try {
                    twitchManager.createPoll();
                    sender.sendMessage(LegacyComponentSerializer.legacyAmpersand().deserialize(getConfig().getString("messages.command-success")));
                } catch (Exception exception) {
                    getLogger().log(Level.WARNING, "Hubo un problema de lectura al procesar la encuesta. Es posible que sí se haya creado en Twitch.", exception);
                    sender.sendMessage(LegacyComponentSerializer.legacyAmpersand().deserialize(getConfig().getString("messages.command-error")));
                }
            });
            return true;
        }
        return false;
    }

    @Override
    public void onDisable() {
        if (twitchManager != null) {
            twitchManager.disconnect();
        }
    }
}