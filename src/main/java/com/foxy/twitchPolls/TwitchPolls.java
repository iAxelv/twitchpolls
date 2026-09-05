package com.foxy.twitchPolls;

import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.configuration.file.YamlConfiguration;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.Map;
import com.foxy.twitchPolls.commands.TestCommand;
import com.foxy.twitchPolls.commands.TwitchCommand;

public final class TwitchPolls extends JavaPlugin {

    private TwitchManager twitchManager;
    private TikTokManager tikTokManager;
    private ActionManager actionManager;
    private UIManager uiManager;
    private SessionManager sessionManager;
    private TestCommand testCommand;
    private PlayerEffectRegistry effectRegistry;
    private CredentialsManager credentialsManager;
    private LanguageManager languageManager;
    private final Map<String, YamlConfiguration> eventConfigs = new HashMap<>();

    @Override
    public void onEnable() {
        migrateLegacyDataFolder();
        saveDefaultConfig();
        saveResourceIfMissing("gui.yml");
        saveResourceIfMissing("secrets.yml");
        saveResourceIfMissing("lang/en.yml");
        saveResourceIfMissing("lang/es.yml");
        
        // Initialize managers
        effectRegistry = new PlayerEffectRegistry(this);
        credentialsManager = new CredentialsManager(this);
        languageManager = new LanguageManager(this);
        
        migrateLegacyEventConfigs();
        saveResourceIfMissing("events/polls.yml");
        saveResourceIfMissing("events/donations.yml");
        saveResourceIfMissing("events/tiktok.yml");
        saveResourceIfMissing("events/points.yml");
        reloadEventConfigs();
        uiManager = new UIManager(this);
        sessionManager = new SessionManager(this);
        getServer().getPluginManager().registerEvents(sessionManager, this);
        actionManager = new ActionManager(this, sessionManager, uiManager, effectRegistry);
        twitchManager = new TwitchManager(this, actionManager, uiManager, sessionManager);
        tikTokManager = new TikTokManager(this, actionManager, uiManager, sessionManager);
        testCommand = new TestCommand(this, actionManager, uiManager);
        getServer().getPluginManager().registerEvents(testCommand, this);
        TwitchCommand twitchCommand = new TwitchCommand(this, twitchManager, tikTokManager, testCommand);
        getCommand("streammanager").setExecutor(twitchCommand);
        getCommand("streammanager").setTabCompleter(twitchCommand);
        reloadEventProviderConnections();
    }

    public void reloadEventProviderConnections() {
        if (twitchManager != null) {
            twitchManager.disconnect();
        }
        if (tikTokManager != null) {
            tikTokManager.disconnect();
        }

        String provider = getConfig().getString("settings.event-provider", "twitch").toLowerCase();
        if ("tiktok".equals(provider)) {
            tikTokManager.connect();
        } else if ("twitch".equals(provider)) {
            twitchManager.connect();
        } else {
            getLogger().warning("Unknown event provider '" + provider + "'. Use 'twitch' or 'tiktok'.");
        }
    }

    private void saveResourceIfMissing(String resourcePath) {
        if (!new File(getDataFolder(), resourcePath).exists()) {
            saveResource(resourcePath, false);
        }
    }

    private void migrateLegacyDataFolder() {
        Path currentFolder = getDataFolder().toPath();
        Path legacyFolder = currentFolder.getParent().resolve("TwitchPolls");
        if (!Files.isDirectory(legacyFolder)) {
            return;
        }

        try {
            if (Files.isDirectory(currentFolder)) {
                try (var entries = Files.list(currentFolder)) {
                    if (entries.findAny().isPresent()) {
                        return;
                    }
                }
            }
            try (var paths = Files.walk(legacyFolder)) {
                for (Path source : paths.toList()) {
                    Path target = currentFolder.resolve(legacyFolder.relativize(source));
                    if (Files.isDirectory(source)) {
                        Files.createDirectories(target);
                    } else {
                        Files.createDirectories(target.getParent());
                        Files.copy(source, target, StandardCopyOption.COPY_ATTRIBUTES);
                    }
                }
            }
            getLogger().info("Migrated data from TwitchPolls to StreamManager.");
        } catch (IOException exception) {
            getLogger().log(java.util.logging.Level.WARNING,
                    "Could not migrate data from TwitchPolls to StreamManager.", exception);
        }
    }

    @Override
    public void onDisable() {
        if (uiManager != null) {
            uiManager.cancelAll();
        }
        if (twitchManager != null) {
            twitchManager.disconnect();
        }
        if (tikTokManager != null) {
            tikTokManager.disconnect();
        }
    }

    public void reloadEventConfigs() {
        eventConfigs.clear();
        for (String eventType : new String[]{"polls", "donations", "tiktok", "points"}) {
            File file = new File(getDataFolder(), "events/" + eventType + ".yml");
            eventConfigs.put(eventType, YamlConfiguration.loadConfiguration(file));
        }
    }

    public YamlConfiguration getEventConfig(String eventType) {
        return eventConfigs.get(eventType);
    }

    public CredentialsManager getCredentialsManager() {
        return credentialsManager;
    }

    public PlayerEffectRegistry getEffectRegistry() {
        return effectRegistry;
    }

    public LanguageManager getLanguageManager() {
        return languageManager;
    }

    public ActionManager getActionManager() {
        return actionManager;
    }

    private void migrateLegacyEventConfigs() {
        if (!getConfig().isConfigurationSection("events")) {
            return;
        }

        boolean migrated = false;
        boolean migrationFailed = false;
        for (String eventType : new String[]{"polls", "donations", "tiktok", "points"}) {
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