package com.foxy.twitchPolls.actions.points;

import com.foxy.twitchPolls.PlayerEffectRegistry;
import com.foxy.twitchPolls.TwitchPolls;
import com.foxy.twitchPolls.actions.ActionContext;
import com.foxy.twitchPolls.actions.ActionStrategy;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.title.Title;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.Locale;

public class DisableActionAction implements ActionStrategy, Listener {
    private final Map<UUID, ActiveAction> activePlayers = new HashMap<>();
    private final Map<String, String> actionNames;

    public DisableActionAction(TwitchPolls plugin, PlayerEffectRegistry effectRegistry) {
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
        
        // Load action names from language file with defaults
        this.actionNames = Map.ofEntries(
            Map.entry("WALK", plugin.getLanguageManager().getString("actions.disable-action.names.WALK", "Caminar")),
            Map.entry("JUMP", plugin.getLanguageManager().getString("actions.disable-action.names.JUMP", "Saltar")),
            Map.entry("BREAK_BLOCKS", plugin.getLanguageManager().getString("actions.disable-action.names.BREAK_BLOCKS", "Romper bloques")),
            Map.entry("PLACE_BLOCKS", plugin.getLanguageManager().getString("actions.disable-action.names.PLACE_BLOCKS", "Colocar bloques")),
            Map.entry("INTERACT_BLOCKS", plugin.getLanguageManager().getString("actions.disable-action.names.INTERACT_BLOCKS", "Interactuar con bloques")),
            Map.entry("ATTACK_MOBS", plugin.getLanguageManager().getString("actions.disable-action.names.ATTACK_MOBS", "Golpear mobs")),
            Map.entry("MOVE_ITEMS", plugin.getLanguageManager().getString("actions.disable-action.names.MOVE_ITEMS", "Mover objetos"))
        );
        
        // Register cleanup handler for when players quit
        effectRegistry.registerCleanupHandler(activePlayers::remove);
    }

    @Override
    public void execute(ActionContext context) {
        List<String> configuredActions = context.config().getStringList("blocked-actions").stream()
            .filter(action -> action != null)
            .map(action -> action.toUpperCase(Locale.ROOT))
            .filter(action -> actionNames.containsKey(action))
                .toList();
        if (configuredActions.isEmpty()) {
            configuredActions = List.copyOf(actionNames.keySet());
        }

        String action = configuredActions.get(ThreadLocalRandom.current().nextInt(configuredActions.size()));
        long durationMillis = Math.max(1L, context.config().getLong("duration-seconds", 30L)) * 1000L;
        activePlayers.put(context.player().getUniqueId(),
                new ActiveAction(action, System.currentTimeMillis() + durationMillis));

        String title = context.plugin().getConfig().getString("messages.points-event-title", "");
        String subtitle = ((TwitchPolls) context.plugin()).getLanguageManager().getString(
            "messages.disable-action-title", "&cAction blocked: &f" + actionNames.get(action))
            .replace("%action%", actionNames.get(action));
        context.player().showTitle(Title.title(
            LegacyComponentSerializer.legacyAmpersand().deserialize(title),
                LegacyComponentSerializer.legacyAmpersand().deserialize(subtitle),
                Title.Times.times(Duration.ZERO, Duration.ofSeconds(2), Duration.ofMillis(500))));
    }

    @EventHandler
    public void onMove(PlayerMoveEvent event) {
        ActiveAction action = getActiveAction(event.getPlayer());
        if (action == null || event.getTo() == null) {
            return;
        }
        boolean movedHorizontally = event.getFrom().getX() != event.getTo().getX()
                || event.getFrom().getZ() != event.getTo().getZ();
        boolean jumped = event.getTo().getY() > event.getFrom().getY() && !event.getPlayer().isFlying();
        if (("WALK".equals(action.name()) && movedHorizontally)
                || ("JUMP".equals(action.name()) && jumped)) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        if (hasAction(event.getPlayer(), "BREAK_BLOCKS")) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        if (hasAction(event.getPlayer(), "PLACE_BLOCKS")) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        if (event.getClickedBlock() != null && isInteractiveBlock(event.getClickedBlock().getType())
                && hasAction(event.getPlayer(), "INTERACT_BLOCKS")) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getWhoClicked() instanceof Player player && hasAction(player, "MOVE_ITEMS")) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        if (event.getWhoClicked() instanceof Player player && hasAction(player, "MOVE_ITEMS")) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onEntityDamage(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof LivingEntity target) || target instanceof Player) {
            return;
        }
        Player attacker = findPlayerAttacker(event.getDamager());
        if (attacker != null && hasAction(attacker, "ATTACK_MOBS")) {
            event.setCancelled(true);
        }
    }

    private ActiveAction getActiveAction(Player player) {
        ActiveAction action = activePlayers.get(player.getUniqueId());
        if (action != null && action.expiresAt() <= System.currentTimeMillis()) {
            activePlayers.remove(player.getUniqueId());
            return null;
        }
        return action;
    }

    private boolean hasAction(Player player, String name) {
        ActiveAction action = getActiveAction(player);
        return action != null && name.equals(action.name());
    }

    private boolean isInteractiveBlock(Material material) {
        return switch (material) {
            case CHEST, TRAPPED_CHEST, CRAFTING_TABLE, FURNACE, BLAST_FURNACE, SMOKER,
                    ENCHANTING_TABLE, ANVIL, CHIPPED_ANVIL, DAMAGED_ANVIL, STONECUTTER,
                    LOOM, CARTOGRAPHY_TABLE, GRINDSTONE, SMITHING_TABLE, BREWING_STAND -> true;
            default -> material.name().endsWith("_SHULKER_BOX");
        };
    }

    private Player findPlayerAttacker(Entity damager) {
        if (damager instanceof Player player) {
            return player;
        }
        if (damager instanceof Projectile projectile && projectile.getShooter() instanceof Player player) {
            return player;
        }
        return null;
    }

    private record ActiveAction(String name, long expiresAt) {
    }
}