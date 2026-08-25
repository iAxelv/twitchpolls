package com.foxy.twitchPolls.actions.donations;

import com.foxy.twitchPolls.actions.ActionContext;
import com.foxy.twitchPolls.actions.ActionStrategy;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Warden;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.Bukkit;

public class WardenPitAction implements ActionStrategy {
    @Override
    public void execute(ActionContext context) {
        int size = Math.max(4, context.config().getInt("size", 12));
        int height = Math.max(3, context.config().getInt("height", 5));
        var center = context.player().getLocation().getBlock();
        int min = -size / 2;
        int max = min + size - 1;
        for (int x = min; x <= max; x++) {
            for (int z = min; z <= max; z++) {
                for (int y = 0; y <= height; y++) {
                    if (x == min || x == max || z == min || z == max || y == 0 || y == height) {
                        center.getRelative(x, y, z).setType(Material.OBSIDIAN, false);
                    }
                }
            }
        }
        context.player().addPotionEffect(new PotionEffect(PotionEffectType.DARKNESS,
                Math.max(1, context.config().getInt("darkness-seconds", 10)) * 20, 0));
        Location spawn = center.getLocation().add(0.5, 1, 0.5);
        Warden warden = (Warden) context.world().spawnEntity(spawn, EntityType.WARDEN);
        warden.setTarget(context.player());
        long duration = Math.max(1L, context.config().getLong("duration-seconds", 60L)) * 20L;
        Bukkit.getScheduler().runTaskLater(context.plugin(), () -> {
            if (warden.isValid()) {
                warden.remove();
            }
        }, duration);
    }
}