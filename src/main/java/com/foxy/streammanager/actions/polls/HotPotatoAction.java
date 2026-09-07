package com.foxy.streammanager.actions.polls;

import com.foxy.streammanager.TwitchPolls;
import com.foxy.streammanager.actions.ActionContext;
import com.foxy.streammanager.actions.ActionStrategy;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.kyori.adventure.title.Title;

public class HotPotatoAction implements ActionStrategy {
    private final Map<UUID, ActionContext> pendingEvents = new HashMap<>();
    private final Map<UUID, Boolean> activePlayers = new HashMap<>();

    @Override public void execute(ActionContext context) {
        UUID playerId = context.player().getUniqueId();
        if (activePlayers.putIfAbsent(playerId, true) != null) {
            if (context.config().getBoolean("queue-while-active", false)) {
                pendingEvents.put(playerId, context);
            }
            return;
        }
        start(context);
    }

    private void start(ActionContext context) {
        TwitchPolls plugin = (TwitchPolls) context.plugin();
        Player player = context.player();
        int duration = Math.max(5, context.config().getInt("duration-seconds", 15));
        float explosionPower = (float) Math.max(0.0,
                context.config().getDouble("explosion-power", 1.0));
        boolean wasGlowing = player.isGlowing();
        final BukkitRunnable[] taskHolder = new BukkitRunnable[1];
        player.setGlowing(true);

        Listener potatoListener = new Listener() {
            @EventHandler
            public void onHitEntity(EntityDamageByEntityEvent event) {
                if (!(event.getDamager() instanceof Player attacker)
                        || !attacker.getUniqueId().equals(player.getUniqueId())
                        || !(event.getEntity() instanceof LivingEntity target)
                        || target instanceof ArmorStand) {
                    return;
                }

                player.setGlowing(wasGlowing);
                player.sendActionBar(LegacyComponentSerializer.legacyAmpersand()
                        .deserialize(plugin.getLanguageManager().getString(
                        "messages.hot-potato-saved", "&aHot potato passed!")));
                player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 2.0f);
                HandlerList.unregisterAll(this);
                cancelTask();
                finish(player, true);
            }

            @EventHandler
            public void onQuit(PlayerQuitEvent event) {
                if (event.getPlayer().getUniqueId().equals(player.getUniqueId())) {
                    player.setGlowing(wasGlowing);
                    HandlerList.unregisterAll(this);
                    cancelTask();
                    finish(player, false);
                }
            }

            @EventHandler
            public void onDeath(EntityDeathEvent event) {
                if (event.getEntity().getUniqueId().equals(player.getUniqueId())) {
                    player.setGlowing(wasGlowing);
                    HandlerList.unregisterAll(this);
                    cancelTask();
                    finish(player, false);
                }
            }

            private void cancelTask() {
                if (taskHolder[0] != null) taskHolder[0].cancel();
            }
        };

        taskHolder[0] = new BukkitRunnable() {
            int remainingTicks = duration * 20;

            @Override public void run() {
                if (!player.isOnline()) {
                    player.setGlowing(wasGlowing);
                    cancel();
                    HandlerList.unregisterAll(potatoListener);
                    finish(player, true);
                    return;
                }

                int remainingSeconds = (int) Math.ceil(remainingTicks / 20.0);
                String format = plugin.getLanguageManager().getString(
                    "messages.hot-potato-actionbar", "&cHot potato: %time%s");
                player.sendActionBar(LegacyComponentSerializer.legacyAmpersand()
                        .deserialize(format.replace("%time%", String.valueOf(remainingSeconds))));

                if (remainingTicks % 60 == 0 && remainingTicks > 0) {
                    player.setFireTicks(40);
                    player.getWorld().spawnParticle(Particle.FLAME,
                            player.getLocation().add(0, 1.0, 0), 8, 0.2, 0.3, 0.2, 0.02);
                    player.playSound(player.getLocation(), Sound.ENTITY_BLAZE_SHOOT, 0.7f, 1.4f);
                }

                if (remainingTicks <= 0) {
                    player.setGlowing(wasGlowing);
                    player.getWorld().createExplosion(player.getLocation(), explosionPower, false, false);
                    player.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 100, 2));
                    player.showTitle(Title.title(
                            LegacyComponentSerializer.legacyAmpersand().deserialize(
                                plugin.getLanguageManager().getString(
                                    "messages.hot-potato-boom-title", "&4&lBOOM!")),
                            LegacyComponentSerializer.legacyAmpersand().deserialize(
                                plugin.getLanguageManager().getString(
                                    "messages.hot-potato-boom-subtitle",
                                    "&cYou did not pass the hot potato in time")),
                            Title.Times.times(Duration.ZERO, Duration.ofSeconds(2), Duration.ofMillis(500))));
                    HandlerList.unregisterAll(potatoListener);
                    cancel();
                    finish(player, true);
                    return;
                }

                remainingTicks--;
            }
        };

        plugin.getServer().getPluginManager().registerEvents(potatoListener, plugin);
        taskHolder[0].runTaskTimer(plugin, 0L, 1L);
    }

    private void finish(Player player, boolean startPending) {
        UUID playerId = player.getUniqueId();
        activePlayers.remove(playerId);
        ActionContext next = startPending ? pendingEvents.remove(playerId) : null;
        if (!startPending) {
            pendingEvents.remove(playerId);
        }
        if (next != null && player.isOnline()) {
            activePlayers.put(playerId, true);
            start(next);
        }
    }
}