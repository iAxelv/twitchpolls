package com.foxy.twitchPolls;

import org.bukkit.plugin.java.JavaPlugin;
import com.foxy.twitchPolls.commands.TestCommand;
import com.foxy.twitchPolls.commands.TwitchCommand;

public final class TwitchPolls extends JavaPlugin {

    private TwitchManager twitchManager;
    private ActionManager actionManager;
    private TestCommand testCommand;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        saveResource("gui.yml", false);
        actionManager = new ActionManager(this);
        twitchManager = new TwitchManager(this, actionManager);
        testCommand = new TestCommand(this, actionManager);
        getServer().getPluginManager().registerEvents(testCommand, this);
        TwitchCommand twitchCommand = new TwitchCommand(this, twitchManager, testCommand);
        getCommand("twitch").setExecutor(twitchCommand);
        getCommand("twitch").setTabCompleter(twitchCommand);
        twitchManager.connect();
    }

    @Override
    public void onDisable() {
        if (twitchManager != null) {
            twitchManager.disconnect();
        }
    }
}