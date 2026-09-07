package com.foxy.streammanager.actions.points;

import com.foxy.streammanager.PlayerEffectRegistry;
import com.foxy.streammanager.TwitchPolls;
import com.foxy.streammanager.actions.ActionContext;
import com.foxy.streammanager.actions.ActionStrategy;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class DisableDamageAction implements ActionStrategy, Listener {
    private final Map<UUID, Long> activePlayers = new HashMap<>();

    public DisableDamageAction(TwitchPolls plugin, PlayerEffectRegistry effectRegistry) {
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
        // Register cleanup handler for when players quit
        effectRegistry.registerCleanupHandler(activePlayers::remove);
    }

    @Override
    public void execute(ActionContext context) {
        long durationMillis = Math.max(1L, context.config().getLong("duration-seconds", 30L)) * 1000L;
        activePlayers.put(context.player().getUniqueId(), System.currentTimeMillis() + durationMillis);
    }

    @EventHandler
    public void onEntityDamage(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof LivingEntity target) || target instanceof Player) {
            return;
        }

        Player attacker = findPlayerAttacker(event.getDamager());
        if (attacker == null) {
            return;
        }

        Long expiresAt = activePlayers.get(attacker.getUniqueId());
        if (expiresAt == null) {
            return;
        }
        if (expiresAt <= System.currentTimeMillis()) {
            activePlayers.remove(attacker.getUniqueId());
            return;
        }

        event.setCancelled(true);
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
}