package com.foxy.twitchPolls.actions.points;

import com.foxy.twitchPolls.actions.ActionContext;
import com.foxy.twitchPolls.actions.ActionStrategy;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashMap;
import java.util.Map;

public class WebPrisonAction implements ActionStrategy {
    @Override
    public void execute(ActionContext context) {
        Map<Location, Material> replaced = new HashMap<>();
        Location center = context.player().getLocation().getBlock().getLocation();
        int radius = Math.max(1, context.config().getInt("radius", 1));
        for (int x = -radius; x <= radius; x++) {
            for (int y = 0; y <= 2; y++) {
                for (int z = -radius; z <= radius; z++) {
                    Block block = center.clone().add(x, y, z).getBlock();
                    if (block.getType().isAir()) {
                        replaced.put(block.getLocation(), block.getType());
                        block.setType(Material.COBWEB);
                    }
                }
            }
        }
        context.world().playSound(context.location(), Sound.ENTITY_SPIDER_AMBIENT, 1.0f, 1.0f);
        long duration = Math.max(1L, context.config().getLong("duration-seconds", 10L)) * 20L;
        int poisonAmplifier = Math.max(0, context.config().getInt("poison-amplifier", 0));
        int poisonDuration = (int) Math.min(Integer.MAX_VALUE, duration);
        context.player().addPotionEffect(new PotionEffect(PotionEffectType.POISON, poisonDuration, poisonAmplifier));
        new BukkitRunnable() {
            @Override
            public void run() {
                replaced.forEach((location, material) -> {
                    if (location.getBlock().getType() == Material.COBWEB) {
                        location.getBlock().setType(material);
                    }
                });
            }
        }.runTaskLater(context.plugin(), duration);
    }
}