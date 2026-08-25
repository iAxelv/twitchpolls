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
3. Start the server once, then edit `plugins/TwitchPolls/config.yml` and the files in `plugins/TwitchPolls/events/`.
4. Replace the `YOUR_TWITCH_*` values with the Twitch application credentials and broadcaster ID.
5. Grant the OAuth token the `channel:read:redemptions` scope and create Twitch rewards whose title and cost match `reward-title` and `value` in `events/points.yml`.
6. Restart the server or use `/twitch reload`.

The credentials in the local server configuration must never be committed. The `run/` directory is intentionally ignored by Git.

## Build

```text
gradle shadowJar
```

The distributable plugin is generated at `build/libs/TwitchPolls-1.0.jar`.

## Command

`/twitch poll` starts a poll manually. `/twitch reload` reloads `config.yml`, `gui.yml`, and all files in `events/`.

Channel Point redemptions are handled independently from polls. A redemption activates the matching active entry in `events/points.yml`; the points test menu also executes these actions immediately without showing a poll countdown.