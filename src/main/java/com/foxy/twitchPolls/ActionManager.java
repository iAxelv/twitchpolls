package com.foxy.twitchPolls;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

import com.foxy.twitchPolls.actions.ActionContext;
import com.foxy.twitchPolls.actions.ActionStrategy;
import com.foxy.twitchPolls.actions.donations.*;
import com.foxy.twitchPolls.actions.polls.*;
import com.foxy.twitchPolls.actions.points.*;

import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class ActionManager {
    private final TwitchPolls plugin;
    private final Map<String, ActionStrategy> strategies = new HashMap<>();
    private final Map<UUID, BukkitTask> eventCountdowns = new HashMap<>();

    public ActionManager(TwitchPolls plugin) {
        this.plugin = plugin;
        registerStrategies();
    }

    public void executeAction(Player player, ConfigurationSection config) {
        if (config == null || !config.contains("action")) {
            return;
        }
        ActionStrategy strategy = strategies.get(config.getString("action"));
        if (strategy != null) {
            String streamerUsername = plugin.getConfig().getString("settings.streamer-username", "Streamer");
            strategy.execute(new ActionContext(plugin, player, config, streamerUsername, null));
        }
    }

    public void executeDonationAction(Player player, ConfigurationSection config, String donorUsername) {
        executeAction(player, config, donorUsername);
        startDurationCountdown(player, config);
    }

    public void executePointAction(Player player, ConfigurationSection config) {
        executePointAction(player, config, null);
    }

    public void executePointAction(Player player, ConfigurationSection config, String redeemerUsername) {
        executeAction(player, config, redeemerUsername);
        startDurationCountdown(player, config);
    }

    private void startDurationCountdown(Player player, ConfigurationSection config) {
        if (!config.contains("duration-seconds")) {
            return;
        }

        int durationSeconds = Math.max(1, config.getInt("duration-seconds"));
        UUID playerId = player.getUniqueId();
        BukkitTask previousTask = eventCountdowns.remove(playerId);
        if (previousTask != null) {
            previousTask.cancel();
        }

        String messageFormat = plugin.getConfig().getString(
                "messages.points-event-duration", "&eFinaliza: &f%time%s");
        sendPointCountdown(player, messageFormat, durationSeconds);

        BukkitTask countdownTask = new BukkitRunnable() {
            private int timeRemaining = durationSeconds;

            @Override
            public void run() {
                timeRemaining--;
                if (timeRemaining <= 0) {
                    player.sendActionBar(LegacyComponentSerializer.legacyAmpersand().deserialize(""));
                    eventCountdowns.remove(playerId);
                    cancel();
                    return;
                }
                sendPointCountdown(player, messageFormat, timeRemaining);
            }
        }.runTaskTimer(plugin, 20L, 20L);
        eventCountdowns.put(playerId, countdownTask);
    }

    private void executeAction(Player player, ConfigurationSection config, String redeemerUsername) {
        if (config == null || !config.contains("action")) {
            return;
        }
        ActionStrategy strategy = strategies.get(config.getString("action"));
        if (strategy != null) {
            String streamerUsername = plugin.getConfig().getString("settings.streamer-username", "Streamer");
            strategy.execute(new ActionContext(plugin, player, config, streamerUsername, redeemerUsername));
        }
    }

    public void cancelPointCountdowns() {
        for (BukkitTask task : eventCountdowns.values()) {
            task.cancel();
        }
        eventCountdowns.clear();
    }

    private void sendPointCountdown(Player player, String messageFormat, int timeRemaining) {
        player.sendActionBar(LegacyComponentSerializer.legacyAmpersand()
                .deserialize(messageFormat.replace("%time%", String.valueOf(timeRemaining))));
    }

    public ConfigurationSection findActionConfig(String actionName) {
        for (String eventType : new String[]{"polls", "donations", "points"}) {
            ConfigurationSection action = findActionConfig(actionName, eventType);
            if (action != null) {
                return action;
            }
        }
        return null;
    }

    public ConfigurationSection findActionConfig(String actionName, String eventType) {
        ConfigurationSection events = plugin.getEventConfig(eventType);
        if (events == null) {
            return null;
        }
        for (String eventName : events.getKeys(false)) {
            ConfigurationSection event = events.getConfigurationSection(eventName);
            if (event != null && actionName.equalsIgnoreCase(event.getString("action"))) {
                return event;
            }
        }
        return null;
    }

    public Set<String> getRegisteredActions() {
        return new LinkedHashSet<>(strategies.keySet());
    }

    private void registerStrategies() {
        strategies.put("INVENTORY_BOMB", new InventoryBombAction());
        strategies.put("NUKE_STRIKE", new NukeStrikeAction());
        strategies.put("MOB_ARMY", new MobArmyAction());
        strategies.put("RAIN_WEALTH", new RainWealthAction());
        strategies.put("GOD_MODE", new GodModeAction());
        strategies.put("SPAWN_CREEPER", new SpawnCreeperAction());
        strategies.put("EFFECT_LEVITATION", new EffectLevitationAction());
        strategies.put("DROP_ORES", new DropOresAction());
        strategies.put("LAUNCH_PLAYER", new LaunchPlayerAction());
        strategies.put("RANDOM_TELEPORT", new RandomTeleportAction());
        strategies.put("RANDOM_ITEM_EXCHANGE", new RandomItemExchangeAction());
        strategies.put("INVENTORY_RANDOM", new InventoryRandomAction());
        strategies.put("FLOOR_IS_LAVA", new FloorIsLavaAction());
        strategies.put("WARDEN_JUMPSCARE", new WardenJumpscareAction());
        strategies.put("MAX_FOOD", new MaxFoodAction());
        strategies.put("INSTANT_HEAL", new InstantHealAction());
        strategies.put("SPAWN_GUARD", new SpawnGuardAction());
        strategies.put("MINI_ZOMBIE", new MiniZombieAction());
        strategies.put("NOTHING", new NothingAction());
        strategies.put("RANDOM_EFFECT", new RandomEffectAction());
        strategies.put("SPAWN_BEE_SWARM", new SpawnBeeSwarmAction());
        strategies.put("SPAWN_VENECO", new SpawnVenecoAction());
        strategies.put("RANDOM_SOUND", new RandomSoundAction());
        strategies.put("CONFUSION", new ConfusionAction());
        strategies.put("DROP_HAND_ITEM", new DropHandItemAction());
        strategies.put("SPIN_HEAD", new SpinHeadAction());
        strategies.put("FAKE_DIAMOND", new FakeDiamondAction(plugin));
        strategies.put("REPLACE_BLOCKS", new ReplaceBlocksAction(plugin));
        strategies.put("ANVIL_TRAP", new AnvilTrapAction());
        strategies.put("PIG_STACK_ATTACK", new PigStackAttackAction());
        strategies.put("INVERT_CONTROLS_EFFECT", new InvertControlsEffectAction());
        strategies.put("SWAP_MAIN_OFFHAND", new SwapMainOffhandAction());
        strategies.put("PUMPKIN_HEAD", new PumpkinHeadAction());
        strategies.put("WEB_PRISON", new WebPrisonAction());
        strategies.put("SILENT_PHANTOMS", new SilentPhantomsAction());
        strategies.put("CLEAN_ARMOR", new CleanArmorAction());
        strategies.put("DISABLE_DAMAGE", new DisableDamageAction(plugin));
        strategies.put("DISABLE_ACTION", new DisableActionAction(plugin));
        strategies.put("GIVE_TOTEMS", new GiveTotemsAction());
        strategies.put("WITHER_STRIKE", new WitherStrikeAction());
        strategies.put("DRAGON_ATTACK", new DragonAttackAction());
        strategies.put("CHARGED_CREEPER_ARMY", new ChargedCreeperArmyAction());
        strategies.put("APOCALYPSE_RAIN", new ApocalypseRainAction());
        strategies.put("WELCOME_TO_HELL", new WelcomeToHellAction());
        strategies.put("WARDEN_PIT", new WardenPitAction());
        strategies.put("PIT_OF_DOOM", new PitOfDoomAction());
        strategies.put("MIDAS_TOUCH", new MidasTouchAction(plugin));
        strategies.put("WIPE_ENEMIES", new WipeEnemiesAction());
        strategies.put("ULTIMATE_CARE_PACKAGE", new UltimateCarePackageAction());
        strategies.put("TIME_FREEZE", new TimeFreezeAction());
    }
}