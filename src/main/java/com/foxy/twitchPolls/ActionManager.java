package com.foxy.twitchPolls;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import com.foxy.twitchPolls.actions.ActionContext;
import com.foxy.twitchPolls.actions.ActionStrategy;
import com.foxy.twitchPolls.actions.*;

import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

public class ActionManager {
    private final Plugin plugin;
    private final Map<String, ActionStrategy> strategies = new HashMap<>();

    public ActionManager(Plugin plugin) {
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
            strategy.execute(new ActionContext(plugin, player, config, streamerUsername));
        }
    }

    public ConfigurationSection findActionConfig(String actionName) {
        ConfigurationSection events = plugin.getConfig().getConfigurationSection("events");
        if (events == null) {
            return null;
        }

        for (String groupName : events.getKeys(false)) {
            ConfigurationSection group = events.getConfigurationSection(groupName);
            if (group == null) {
                continue;
            }
            for (String eventName : group.getKeys(false)) {
                ConfigurationSection event = group.getConfigurationSection(eventName);
                if (event != null && actionName.equalsIgnoreCase(event.getString("action"))) {
                    return event;
                }
            }
        }
        return null;
    }

    public Set<String> getRegisteredActions() {
        return new LinkedHashSet<>(strategies.keySet());
    }

    private void registerStrategies() {
        strategies.put("SPAWN_CREEPER", new SpawnCreeperAction());
        strategies.put("EFFECT_LEVITATION", new EffectLevitationAction());
        strategies.put("DROP_ORES", new DropOresAction());
        strategies.put("LAUNCH_PLAYER", new LaunchPlayerAction());
        strategies.put("RANDOM_TELEPORT", new RandomTeleportAction());
        strategies.put("POTATO_PREMIUM", new PotatoPremiumAction());
        strategies.put("INVENTORY_RANDOM", new InventoryRandomAction());
        strategies.put("FLOOR_IS_LAVA", new FloorIsLavaAction());
        strategies.put("WARDEN_JUMPSCARE", new WardenJumpscareAction());
        strategies.put("MAX_FOOD", new MaxFoodAction());
        strategies.put("MINI_ZOMBIE", new MiniZombieAction());
        strategies.put("NOTHING", new NothingAction());
        strategies.put("RANDOM_EFFECT", new RandomEffectAction());
        strategies.put("SPAWN_BEE_SWARM", new SpawnBeeSwarmAction());
        strategies.put("SPAWN_VENECO", new SpawnVenecoAction());
        strategies.put("RANDOM_SOUND", new RandomSoundAction());
        strategies.put("CONFUSION", new ConfusionAction());
    }
}