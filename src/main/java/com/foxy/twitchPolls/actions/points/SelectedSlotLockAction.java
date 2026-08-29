package com.foxy.twitchPolls.actions.points;

import com.foxy.twitchPolls.PlayerEffectRegistry;
import com.foxy.twitchPolls.TwitchPolls;
import com.foxy.twitchPolls.actions.ActionContext;
import com.foxy.twitchPolls.actions.ActionStrategy;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class SelectedSlotLockAction implements ActionStrategy, Listener {
    private final Map<UUID, Effect> active = new HashMap<>();
    
    public SelectedSlotLockAction(TwitchPolls plugin, PlayerEffectRegistry effectRegistry) {
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
        // Register cleanup handler for when players quit
        effectRegistry.registerCleanupHandler(active::remove);
    }
    @Override public void execute(ActionContext context) {
        int slot = Math.max(0, Math.min(8, context.config().getInt("slot", context.player().getInventory().getHeldItemSlot())));
        active.put(context.player().getUniqueId(), new Effect(slot, System.currentTimeMillis()
                + Math.max(1L, context.config().getLong("duration-seconds", 15)) * 1000L));
        context.player().getInventory().setHeldItemSlot(slot);
    }
    @EventHandler public void onHeld(PlayerItemHeldEvent event) { enforce(event.getPlayer().getUniqueId(), event.getNewSlot(), event); }
    @EventHandler public void onClick(InventoryClickEvent event) {
        if (event.getWhoClicked() instanceof org.bukkit.entity.Player player) {
            Effect effect = get(player.getUniqueId());
            if (effect != null && event.getHotbarButton() >= 0 && event.getHotbarButton() != effect.slot) event.setCancelled(true);
        }
    }
    private void enforce(UUID id, int slot, PlayerItemHeldEvent event) { Effect effect = get(id); if (effect != null && slot != effect.slot) { event.setCancelled(true); } }
    private Effect get(UUID id) { Effect effect = active.get(id); if (effect != null && effect.expiresAt < System.currentTimeMillis()) { active.remove(id); return null; } return effect; }
    private record Effect(int slot, long expiresAt) { }
}