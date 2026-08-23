# TwitchPolls

Paper plugin that creates Twitch polls from Minecraft events.

## Requirements

- Java 21
- Gradle 8+
- Paper 1.21.8
- A Twitch application and an OAuth token with permission to manage polls

## Configuration

1. Build the plugin with `gradle shadowJar`.
2. Copy `build/libs/TwitchPolls-1.0.jar` to the server `plugins/` directory.
3. Start the server once, then edit `plugins/TwitchPolls/config.yml`.
4. Replace the `YOUR_TWITCH_*` values with the Twitch application credentials and broadcaster ID.
5. Restart the server or use `/poll reload`.

The credentials in the local server configuration must never be committed. The `run/` directory is intentionally ignored by Git.

## Build

```text
gradle shadowJar
```

The distributable plugin is generated at `build/libs/TwitchPolls-1.0.jar`.

## Command

`/poll` starts a poll manually. `/poll reload` reloads the configuration.