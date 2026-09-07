package com.foxy.twitchPolls.actions.polls;

import com.foxy.twitchPolls.TwitchPolls;
import com.foxy.twitchPolls.actions.ActionContext;
import com.foxy.twitchPolls.actions.ActionStrategy;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.attribute.Attribute;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class SlimeSplitMobsAction implements ActionStrategy, Listener {
    private final Map<UUID, Long> activeUntil = new HashMap<>();

    public SlimeSplitMobsAction(TwitchPolls plugin) {
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @Override public void execute(ActionContext context) {
        activeUntil.put(context.player().getUniqueId(), System.currentTimeMillis()
                + Math.max(1, context.config().getInt("duration-seconds", 40)) * 1000L);
    }

    @EventHandler
    public void onDamage(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player player)
                || !(event.getEntity() instanceof LivingEntity target)
                || target instanceof Player || target instanceof ArmorStand) return;
        Long expiry = activeUntil.get(player.getUniqueId());
        if (expiry == null) return;
        if (expiry < System.currentTimeMillis()) {
            activeUntil.remove(player.getUniqueId());
            return;
        }
        event.setCancelled(true);
        double maxHealth = target.getAttribute(Attribute.MAX_HEALTH) == null
                ? target.getHealth() : target.getAttribute(Attribute.MAX_HEALTH).getValue();
        double health = Math.max(1.0, target.getHealth() / 2.0);
        for (int i = 0; i < 2; i++) {
            LivingEntity copy = (LivingEntity) target.getWorld().spawnEntity(
                    target.getLocation().clone().add(i == 0 ? -0.4 : 0.4, 0, 0), target.getType());
            if (copy.getAttribute(Attribute.MAX_HEALTH) != null) {
                copy.getAttribute(Attribute.MAX_HEALTH).setBaseValue(maxHealth);
            }
            copy.setHealth(Math.min(maxHealth, health));
        }
        target.remove();
    }

    @EventHandler public void onQuit(PlayerQuitEvent event) {
        activeUntil.remove(event.getPlayer().getUniqueId());
    }
}
