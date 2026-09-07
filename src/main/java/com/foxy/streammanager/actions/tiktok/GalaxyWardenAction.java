package com.foxy.streammanager.actions.tiktok;

import com.foxy.streammanager.actions.ActionContext;
import com.foxy.streammanager.actions.ActionStrategy;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Warden;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class GalaxyWardenAction implements ActionStrategy {
    @Override public void execute(ActionContext context) {
        String donor = context.redeemerUsername() == null || context.redeemerUsername().isBlank()
                ? "Donador" : context.redeemerUsername();
        Warden warden = (Warden) context.world().spawnEntity(
                context.player().getLocation(), EntityType.WARDEN);
        warden.customName(Component.text(donor));
        warden.setCustomNameVisible(true);
        warden.setTarget(context.player());
        int duration = (int) Math.min(Integer.MAX_VALUE,
                Math.max(1L, context.config().getLong("duration-seconds", 9_999_999L)) * 20L);
        int amplifier = Math.max(0, context.config().getInt("speed-amplifier", 2));
        warden.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, duration, amplifier));
    }
}