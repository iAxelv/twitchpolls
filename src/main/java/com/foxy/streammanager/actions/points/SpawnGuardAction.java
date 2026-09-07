package com.foxy.streammanager.actions.points;

import com.foxy.streammanager.actions.ActionContext;
import com.foxy.streammanager.actions.ActionStrategy;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Wolf;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class SpawnGuardAction implements ActionStrategy {
    @Override
    public void execute(ActionContext context) {
        Wolf wolf = (Wolf) context.world().spawnEntity(context.location(), EntityType.WOLF);
        wolf.setAdult();
        wolf.setTamed(true);
        wolf.setOwner(context.player());

        String username = context.redeemerUsername();
        if (username == null || username.isBlank()) {
            username = "unknown";
        }
        wolf.customName(Component.text(username));
        wolf.setCustomNameVisible(true);

        int durationSeconds = context.config().getInt("resistance-duration-seconds", 99999);
        int amplifier = context.config().getInt("resistance-amplifier", 4);
        wolf.addPotionEffect(new PotionEffect(
                PotionEffectType.RESISTANCE, durationSeconds * 20, amplifier));
        context.plugin().getServer().getScheduler().runTaskLater(
            context.plugin(), wolf::remove, 5 * 60 * 20L);
    }
}