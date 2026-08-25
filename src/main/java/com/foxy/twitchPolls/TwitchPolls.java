package com.foxy.twitchPolls;

import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.configuration.file.YamlConfiguration;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import com.foxy.twitchPolls.commands.TestCommand;
import com.foxy.twitchPolls.commands.TwitchCommand;

public final class TwitchPolls extends JavaPlugin {

    private TwitchManager twitchManager;
    private ActionManager actionManager;
    private TestCommand testCommand;
    private final Map<String, YamlConfiguration> eventConfigs = new HashMap<>();

    @Override
    public void onEnable() {
        saveDefaultConfig();
        saveResource("gui.yml", false);
        migrateLegacyEventConfigs();
        saveResource("events/polls.yml", false);
        saveResource("events/donations.yml", false);
        saveResource("events/points.yml", false);
        reloadEventConfigs();
        actionManager = new ActionManager(this);
        twitchManager = new TwitchManager(this, actionManager);
        testCommand = new TestCommand(this, actionManager, twitchManager);
        getServer().getPluginManager().registerEvents(testCommand, this);
        TwitchCommand twitchCommand = new TwitchCommand(this, twitchManager, testCommand);
        getCommand("twitch").setExecutor(twitchCommand);
        getCommand("twitch").setTabCompleter(twitchCommand);
        twitchManager.connect();
    }

    @Override
    public void onDisable() {
        if (actionManager != null) {
            actionManager.cancelPointCountdowns();
        }
        if (twitchManager != null) {
            twitchManager.disconnect();
        }
    }

    public void reloadEventConfigs() {
        eventConfigs.clear();
        for (String eventType : new String[]{"polls", "donations", "points"}) {
            File file = new File(getDataFolder(), "events/" + eventType + ".yml");
            eventConfigs.put(eventType, YamlConfiguration.loadConfiguration(file));
        }
    }

    public YamlConfiguration getEventConfig(String eventType) {
        return eventConfigs.get(eventType);
    }

    private void migrateLegacyEventConfigs() {
        if (!getConfig().isConfigurationSection("events")) {
            return;
        }

        boolean migrated = false;
        boolean migrationFailed = false;
        for (String eventType : new String[]{"polls", "donations", "points"}) {
            File eventFile = new File(getDataFolder(), "events/" + eventType + ".yml");
            if (eventFile.exists()) {
                continue;
            }

            org.bukkit.configuration.ConfigurationSection legacySection =
                    getConfig().getConfigurationSection("events." + eventType);
            if (legacySection == null) {
                continue;
            }

            YamlConfiguration eventConfig = new YamlConfiguration();
            for (Map.Entry<String, Object> entry : legacySection.getValues(true).entrySet()) {
                eventConfig.set(entry.getKey(), entry.getValue());
            }
            try {
                eventFile.getParentFile().mkdirs();
                eventConfig.save(eventFile);
                migrated = true;
            } catch (IOException exception) {
                migrationFailed = true;
                getLogger().log(java.util.logging.Level.WARNING,
                        "No se pudo migrar events." + eventType + " a " + eventFile.getPath(), exception);
            }
        }
        if (migrated && !migrationFailed) {
            getConfig().set("events", null);
            saveConfig();
        }
    }
}