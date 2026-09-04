package com.foxy.twitchPolls;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import com.foxy.twitchPolls.actions.ActionContext;
import com.foxy.twitchPolls.actions.ActionStrategy;
import com.foxy.twitchPolls.actions.common.LightningStormAction;
import com.foxy.twitchPolls.actions.donations.*;
import com.foxy.twitchPolls.actions.polls.*;
import com.foxy.twitchPolls.actions.points.*;
import com.foxy.twitchPolls.actions.tiktok.FollowTntAction;
import com.foxy.twitchPolls.actions.tiktok.ChargedCreeperLikeAction;
import com.foxy.twitchPolls.actions.tiktok.DoughnutDeleteChunkAction;
import com.foxy.twitchPolls.actions.tiktok.RoseZombieGiftAction;

import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.concurrent.ThreadLocalRandom;

public class ActionManager {
    private final TwitchPolls plugin;
    private final SessionManager sessionManager;
    private final UIManager uiManager;
    private final PlayerEffectRegistry effectRegistry;
    private final Map<String, ActionStrategy> strategies = new HashMap<>();

    public ActionManager(TwitchPolls plugin, SessionManager sessionManager, UIManager uiManager, PlayerEffectRegistry effectRegistry) {
        this.plugin = plugin;
        this.sessionManager = sessionManager;
        this.uiManager = uiManager;
        this.effectRegistry = effectRegistry;
        registerStrategies();
    }

    public void executeAction(Player player, ConfigurationSection config) {
        if (config == null || !config.contains("action")) {
            return;
        }
        ActionStrategy strategy = strategies.get(config.getString("action"));
        if (strategy != null) {
            strategy.execute(new ActionContext(plugin, player, config, sessionManager.getStreamerUsername(), null, 1));
        }
    }

    public void executeDonationAction(Player player, ConfigurationSection config, String donorUsername) {
        executeDonationAction(player, config, donorUsername, 1);
    }

    public void executeDonationAction(Player player, ConfigurationSection config, String donorUsername,
                                      int eventMultiplier) {
        executeAction(player, config, donorUsername, eventMultiplier);
        uiManager.startEventCountdown(player, config);
    }

    public void executePointAction(Player player, ConfigurationSection config) {
        executePointAction(player, config, null);
    }

    public void executePointAction(Player player, ConfigurationSection config, String redeemerUsername) {
        executeAction(player, config, redeemerUsername);
        uiManager.startEventCountdown(player, config);
    }

    private void executeAction(Player player, ConfigurationSection config, String redeemerUsername) {
        executeAction(player, config, redeemerUsername, 1);
    }

    private void executeAction(Player player, ConfigurationSection config, String redeemerUsername,
                               int eventMultiplier) {
        if (config == null || !config.contains("action")) {
            return;
        }
        ActionStrategy strategy = strategies.get(config.getString("action"));
        if (strategy != null) {
            strategy.execute(new ActionContext(plugin, player, config, sessionManager.getStreamerUsername(),
                    redeemerUsername, Math.max(1, eventMultiplier)));
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
        strategies.put("INVENTORY_BOMB", new InventoryBombAction());
        strategies.put("NUKE_STRIKE", new NukeStrikeAction());
        strategies.put("MOB_ARMY", new MobArmyAction());
        strategies.put("RAIN_WEALTH", new RainWealthAction());
        strategies.put("GOD_MODE", new GodModeAction());
        strategies.put("SPAWN_CREEPER", new SpawnCreeperAction());
        strategies.put("RANDOM_SIZE", new RandomSizeAction());
        strategies.put("HOT_POTATO", new HotPotatoAction());
        strategies.put("BLOCK_SWAP", new BlockSwapAction(plugin));
        strategies.put("TORNADO", new TornadoAction());
        strategies.put("TSUNAMI", new TsunamiAction());
        strategies.put("ZERO_GRAVITY", new ZeroGravityAction());
        strategies.put("DELETE_CHUNK", new DeleteChunkAction());
        strategies.put("SPARK", new LightningStormAction());
        strategies.put("EFFECT_LEVITATION", new EffectLevitationAction());
        strategies.put("DROP_ORES", new DropOresAction());
        strategies.put("LAUNCH_PLAYER", new LaunchPlayerAction());
        strategies.put("RANDOM_TELEPORT", new RandomTeleportAction());
        strategies.put("RANDOM_ITEM_EXCHANGE", new RandomItemExchangeAction());
        strategies.put("INVENTORY_RANDOM", new InventoryRandomAction());
        strategies.put("FLOOR_IS_LAVA", new FloorIsLavaAction());
        strategies.put("INSTANT_HEAL", new InstantHealAction());
        strategies.put("SPAWN_GUARD", new SpawnGuardAction());
        strategies.put("MINI_ZOMBIE", new MiniZombieAction());
        strategies.put("RANDOM_EFFECT", new RandomEffectAction());
        strategies.put("SPAWN_BEE_SWARM", new SpawnBeeSwarmAction());
        strategies.put("SPAWN_VENECO", new SpawnVenecoAction());
        strategies.put("DROP_HAND_ITEM", new DropHandItemAction());
        strategies.put("HOTBAR_SHUFFLE", new HotbarShuffleAction());
        strategies.put("SPIN_HEAD", new SpinHeadAction());
        strategies.put("DIVINE_PUNISHMENT", new LightningStormAction());
        strategies.put("FAKE_DIAMOND", new FakeDiamondAction(plugin));
        strategies.put("REPLACE_BLOCKS", new ReplaceBlocksAction(plugin, effectRegistry));
        strategies.put("ANVIL_TRAP", new AnvilTrapAction());
        strategies.put("PIG_STACK_ATTACK", new PigStackAttackAction());
        strategies.put("INVERT_CONTROLS_EFFECT", new InvertControlsEffectAction());
        strategies.put("SWAP_MAIN_OFFHAND", new SwapMainOffhandAction());
        strategies.put("PUMPKIN_HEAD", new PumpkinHeadAction());
        strategies.put("WEB_PRISON", new WebPrisonAction());
        strategies.put("SILENT_PHANTOMS", new SilentPhantomsAction());
        strategies.put("CLEAN_ARMOR", new CleanArmorAction());
        strategies.put("DISABLE_DAMAGE", new DisableDamageAction(plugin, effectRegistry));
        strategies.put("DISABLE_ACTION", new DisableActionAction(plugin, effectRegistry));
        strategies.put("GIVE_TOTEMS", new GiveTotemsAction());
        strategies.put("HAPPY_HOLIDAYS", new HappyHolidaysAction());
        strategies.put("WITHER_STRIKE", new WitherStrikeAction());
        strategies.put("DRAGON_ATTACK", new DragonAttackAction());
        strategies.put("CHARGED_CREEPER_ARMY", new ChargedCreeperArmyAction());
        strategies.put("APOCALYPSE_RAIN", new ApocalypseRainAction());
        strategies.put("WELCOME_TO_HELL", new WelcomeToHellAction());
        strategies.put("WARDEN_PIT", new WardenPitAction());
        strategies.put("PIT_OF_DOOM", new PitOfDoomAction());
        strategies.put("MIDAS_TOUCH", new MidasTouchAction(plugin, effectRegistry));
        strategies.put("WIPE_ENEMIES", new WipeEnemiesAction());
        strategies.put("ULTIMATE_CARE_PACKAGE", new UltimateCarePackageAction());
        strategies.put("TIME_FREEZE", new TimeFreezeAction());
        strategies.put("THUNDERSTORM", new LightningStormAction());
        strategies.put("METEOR_SHOWER", new MeteorShowerAction());
        strategies.put("BLACK_HOLE", new BlackHoleAction());
        strategies.put("EARTHQUAKE", new EarthquakeAction());
        strategies.put("WORLD_ROTATION", new WorldRotationAction());
        strategies.put("SELECTED_SLOT_LOCK", new SelectedSlotLockAction(plugin, effectRegistry));
        strategies.put("RANDOM_SLOT_SWITCH", new RandomSlotSwitchAction());
        strategies.put("DROP_RANDOM_ITEM", new DropRandomItemAction());
        strategies.put("ITEM_NAME_SWAP", new ItemNameSwapAction());
        strategies.put("DELETED_CHUNKS", new DeleteChunkAction());
        strategies.put("ONE_HEART_CHALLENGE", new OneHeartChallengeAction());
        strategies.put("SURPRISE", context -> executeSurpriseAction(context.player()));
        strategies.put("ROSE_ZOMBIES", new RoseZombieGiftAction());
        strategies.put("DOUGHNUT_DELETE_CHUNK", new DoughnutDeleteChunkAction());
        strategies.put("FOLLOW_TNT", new FollowTntAction());
        strategies.put("TIKTOK_CHARGED_CREEPER_ARMY", new ChargedCreeperLikeAction());
    }

    private void executeSurpriseAction(Player player) {
        ConfigurationSection donations = plugin.getEventConfig("donations");
        if (donations == null) {
            return;
        }

        List<ConfigurationSection> activeDonations = new ArrayList<>();
        for (String key : donations.getKeys(false)) {
            ConfigurationSection donation = donations.getConfigurationSection(key);
            if (donation != null && donation.getBoolean("active", true)) {
                activeDonations.add(donation);
            }
        }
        if (activeDonations.isEmpty()) {
            plugin.getLogger().warning("SURPRISE was tested, but no active donation events are configured.");
            return;
        }

        ConfigurationSection selectedDonation = activeDonations.get(
                ThreadLocalRandom.current().nextInt(activeDonations.size()));
        uiManager.showDonationEvent(player, selectedDonation);
        executeDonationAction(player, selectedDonation, "SURPRISE");
    }
}