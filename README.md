# StreamManager

Paper plugin that connects stream events from Twitch and TikTok to Minecraft actions.

## Requirements

- Java 21
- Gradle 8+
- Paper 1.21.8
- A Twitch application in the Dev Console
- A broadcaster account authenticated with the required scopes

Recommended scopes:
- `channel:manage:polls` to create polls
- `channel:read:polls` to receive poll results
- `channel:read:redemptions` to receive Channel Point redemptions
- `bits:read` to receive Bits events
- `channel:read:subscriptions` to receive subscriptions, gifts, and resubscriptions

## How authentication works

The plugin stores sensitive credentials in `plugins/StreamManager/secrets.yml` and uses the Twitch OAuth2 flow.

The important part is that the access token can expire, but the plugin attempts to refresh it automatically using the `refresh-token` when available. This prevents the streamer from manually editing the file every time the token expires.

In short:
- `oauth-token` = current access token
- `refresh-token` = token used to request a new one without re-authenticating manually
- The plugin saves the new token if Twitch returns an updated one

## Installation

1. Build the plugin:

```bash
gradle shadowJar
```

2. Copy the generated artifact from `build/libs/StreamManager-1.0.jar` into your server's `plugins/` folder.
3. Start the server once so the base files are generated.
4. Edit:
  - `plugins/StreamManager/secrets.yml`
  - `plugins/StreamManager/config.yml`
  - `plugins/StreamManager/events/*.yml`
5. Fill in your real Twitch credentials and broadcaster ID.
6. Restart the server or use `/streammanager reload` (or `/sm reload`).

## Twitch configuration

In `plugins/StreamManager/secrets.yml`, set something like:

```yaml
twitch:
  client-id: "YOUR_CLIENT_ID"
  client-secret: "YOUR_CLIENT_SECRET"
  oauth-token: "YOUR_OAUTH_TOKEN"
  refresh-token: "YOUR_REFRESH_TOKEN"
  broadcaster-id: "YOUR_BROADCASTER_ID"
```

Important:
- Do not share this file
- Do not commit it to Git
- If the token expires, the plugin will try to refresh it automatically as long as the `refresh-token` remains valid

## Token generation

1. Create an application in the Twitch Developer Console.
2. Copy the `Client ID` and `Client Secret`.
3. Generate an OAuth token for the streamer's channel with the required scopes.
4. Save both `oauth-token` and `refresh-token` in `secrets.yml`.
5. If the token needs to be renewed, the plugin will do it automatically when necessary.

## Build

```bash
gradle shadowJar
```

The final jar is generated at:

```text
build/libs/StreamManager-1.0.jar
```

## Commands

- `/streammanager reload` -> reloads `config.yml`, `gui.yml`, and all files in `events/`
- `/streammanager reconnect` -> reconnects the configured event provider
- `/streammanager test` -> opens the action test menu in-game

The short alias `/sm` can be used instead of `/streammanager`.

## Point events

Channel Point redemptions are handled separately from polls. A redemption activates the matching entry in `events/points.yml`. The test menu also executes these actions immediately without waiting for the poll countdown.