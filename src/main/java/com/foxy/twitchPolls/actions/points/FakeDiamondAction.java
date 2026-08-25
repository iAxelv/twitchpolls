package com.foxy.twitchPolls.actions.points;

import com.foxy.twitchPolls.TwitchPolls;
import com.foxy.twitchPolls.actions.ActionContext;
import com.foxy.twitchPolls.actions.ActionStrategy;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.block.Block;
import org.bukkit.entity.Chicken;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Zombie;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.FluidCollisionMode;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.RayTraceResult;

import java.util.HashMap;
import java.util.Map;

public class FakeDiamondAction implements ActionStrategy, Listener {
    private final Map<Location, Material> fakeBlocks = new HashMap<>();
    private final Plugin plugin;

    public FakeDiamondAction(TwitchPolls plugin) {
        this.plugin = plugin;
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @Override
    public void execute(ActionContext context) {
        Location eyeLocation = context.player().getEyeLocation();
        double distance = Math.max(1.0, context.config().getDouble("distance", 2.0));
        RayTraceResult hit = context.world().rayTraceBlocks(eyeLocation, eyeLocation.getDirection(), distance,
                FluidCollisionMode.NEVER, true);
        Block block;
        if (hit != null && hit.getHitBlock() != null && hit.getHitBlockFace() != null) {
            block = hit.getHitBlock().getRelative(hit.getHitBlockFace());
        } else {
            block = eyeLocation.clone().add(eyeLocation.getDirection().normalize().multiply(distance)).getBlock();
        }
        if (!block.getType().isAir()) {
            return;
        }
        Location key = block.getLocation();
        Material replacement = Material.matchMaterial(context.config().getString("replacement", "COBWEB"));
        fakeBlocks.put(key, replacement == null ? Material.COBWEB : replacement);
        block.setType(Material.DIAMOND_BLOCK, false);
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Location key = event.getBlock().getLocation();
        Material replacement = fakeBlocks.get(key);
        if (replacement == null) {
            return;
        }
        fakeBlocks.remove(key);
        event.setDropItems(false);
        event.getBlock().setType(replacement);
        event.getBlock().getWorld().spawnParticle(Particle.SMOKE, key.clone().add(0.5, 0.5, 0.5), 30,
                0.3, 0.3, 0.3, 0.02);
        spawnChickenJockeys(event.getPlayer().getLocation());
    }

    private void spawnChickenJockeys(Location location) {
        for (int index = 0; index < 2; index++) {
            Location spawnLocation = location.clone().add(index == 0 ? -1.0 : 1.0, 0.0, 0.0);
            Chicken chicken = (Chicken) location.getWorld().spawnEntity(spawnLocation, EntityType.CHICKEN);
            Zombie zombie = (Zombie) location.getWorld().spawnEntity(spawnLocation, EntityType.ZOMBIE);
            zombie.setBaby();
            chicken.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 200, 3));
            chicken.addPassenger(zombie);
            removeAfterTenSeconds(chicken, zombie);
        }
    }

    private void removeAfterTenSeconds(Entity chicken, Entity zombie) {
        new BukkitRunnable() {
            @Override
            public void run() {
                zombie.remove();
                chicken.remove();
            }
        }.runTaskLater(plugin, 200L);
    }
}