package com.foxy.twitchPolls;

import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.attribute.PosixFilePermission;
import java.util.HashSet;
import java.util.Set;

/**
 * Manages secure credentials storage and retrieval.
 * Separates sensitive credentials (client-id, tokens) from regular configuration.
 * 
 * Best practices:
 * - Credentials are stored in a separate secrets.yml file
 * - File permissions are restricted to owner only (0600) on Unix systems
 * - Credentials are NOT logged or printed
 * - File should be added to .gitignore
 */
public class CredentialsManager {
    private final Plugin plugin;
    private final File secretsFile;
    private YamlConfiguration secretsConfig;

    public CredentialsManager(Plugin plugin) {
        this.plugin = plugin;
        this.secretsFile = new File(plugin.getDataFolder(), "secrets.yml");
        loadSecrets();
    }

    /**
     * Load the secrets configuration file.
     * Creates a default file with instructions if it doesn't exist.
     */
    private void loadSecrets() {
        if (!secretsFile.exists()) {
            plugin.saveResource("secrets.yml", true);
            restrictFilePermissions();
            plugin.getLogger().info("Created secrets.yml - Please configure your Twitch credentials.");
        }
        secretsConfig = YamlConfiguration.loadConfiguration(secretsFile);
        restrictFilePermissions();
    }

    /**
     * Restrict file permissions to owner only (0600) on Unix systems.
     * On Windows, this is best-effort only.
     */
    private void restrictFilePermissions() {
        try {
            Set<PosixFilePermission> perms = new HashSet<>();
            perms.add(PosixFilePermission.OWNER_READ);
            perms.add(PosixFilePermission.OWNER_WRITE);
            Files.setPosixFilePermissions(secretsFile.toPath(), perms);
        } catch (Exception e) {
            // Silently fail on Windows where POSIX permissions aren't available
            // On production Linux servers, ensure proper file permissions manually
        }
    }

    /**
     * Reload the secrets configuration from disk.
     */
    public void reload() {
        loadSecrets();
    }

    /**
     * Get a credential value from the secrets configuration.
     *
     * @param path Dot-separated path (e.g., "twitch.client-id")
     * @param defaultValue Default value if credential is not found
     * @return The credential value or default value
     */
    public String getCredential(String path, String defaultValue) {
        String value = secretsConfig.getString(path, defaultValue);
        
        // Log warning if using placeholder
        if (value.startsWith("YOUR_") && value.endsWith("_ID")) {
            plugin.getLogger().warning("Credential not configured: " + path);
        }
        
        return value;
    }

    /**
     * Get a credential value from the secrets configuration.
     *
     * @param path Dot-separated path (e.g., "twitch.client-id")
     * @return The credential value, or null if not found
     */
    public String getCredential(String path) {
        return getCredential(path, null);
    }

    /**
     * Check if all required Twitch credentials are configured.
     *
     * @return true if all required credentials are present and not default placeholders
     */
    public boolean isTwitchConfigured() {
        String clientId = getCredential("twitch.client-id", "");
        String clientSecret = getCredential("twitch.client-secret", "");
        String oauthToken = getCredential("twitch.oauth-token", "");
        String broadcasterId = getCredential("twitch.broadcaster-id", "");
        
        return !clientId.isEmpty() && !clientId.startsWith("YOUR_") &&
               !clientSecret.isEmpty() && !clientSecret.startsWith("YOUR_") &&
               !oauthToken.isEmpty() && !oauthToken.startsWith("YOUR_") &&
               !broadcasterId.isEmpty() && !broadcasterId.startsWith("YOUR_");
    }
}
