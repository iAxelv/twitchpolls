package com.foxy.twitchPolls;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

public final class TwitchPolls extends JavaPlugin {

    private TwitchManager twitchManager;
    private ActionManager actionManager;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        actionManager = new ActionManager(this);
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
                twitchManager.createPoll();
                sender.sendMessage(LegacyComponentSerializer.legacyAmpersand().deserialize(getConfig().getString("messages.command-success")));
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