package com.foxy.twitchPolls;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import com.foxy.twitchPolls.actions.ActionContext;
import com.foxy.twitchPolls.actions.ActionStrategy;
import com.foxy.twitchPolls.actions.polls.*;
import com.foxy.twitchPolls.actions.points.*;

import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

public class ActionManager {
    private final TwitchPolls plugin;
    private final Map<String, ActionStrategy> strategies = new HashMap<>();

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
            strategy.execute(new ActionContext(plugin, player, config, streamerUsername));
        }
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
        strategies.put("WEB_PRISON", new WebPrisonAction());
        strategies.put("SILENT_PHANTOMS", new SilentPhantomsAction());
        strategies.put("CLEAN_ARMOR", new CleanArmorAction());
        strategies.put("DISABLE_DAMAGE", new DisableDamageAction(plugin));
        strategies.put("DISABLE_ACTION", new DisableActionAction(plugin));
    }
}