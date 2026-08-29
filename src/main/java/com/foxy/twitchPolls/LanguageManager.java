package com.foxy.twitchPolls;

import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;

import java.io.File;

/**
 * Manages language-specific configuration loading.
 * Loads the appropriate language file based on config.yml setting.
 */
public class LanguageManager {
    private final Plugin plugin;
    private final String currentLanguage;
    private YamlConfiguration languageConfig;

    public LanguageManager(Plugin plugin) {
        this.plugin = plugin;
        this.currentLanguage = plugin.getConfig().getString("lang", "es");
        loadLanguageConfig();
    }

    /**
     * Load the language configuration file.
     */
    private void loadLanguageConfig() {
        String fileName = currentLanguage.toLowerCase() + ".yml";
        File file = new File(plugin.getDataFolder(), "lang/" + fileName);
        
        if (!file.exists()) {
            plugin.getLogger().warning("Language file not found: " + fileName + ", falling back to es.yml");
            file = new File(plugin.getDataFolder(), "lang/es.yml");
        }
        
        languageConfig = YamlConfiguration.loadConfiguration(file);
    }

    /**
     * Reload the language configuration.
     */
    public void reload() {
        loadLanguageConfig();
    }

    /**
     * Get a value from the language configuration.
     *
     * @param path Dot-separated path (e.g., "messages.poll-title")
     * @param defaultValue Default value if path is not found
     * @return The configuration value or default
     */
    public String getString(String path, String defaultValue) {
        return languageConfig.getString(path, defaultValue);
    }

    /**
     * Get a list value from the language configuration.
     *
     * @param path Dot-separated path (e.g., "actions.item-name-swap.item-names")
     * @return The list from configuration
     */
    public java.util.List<String> getStringList(String path) {
        return languageConfig.getStringList(path);
    }

    /**
     * Get the currently loaded language.
     *
     * @return The language code (e.g., "es", "en")
     */
    public String getCurrentLanguage() {
        return currentLanguage;
    }
}
