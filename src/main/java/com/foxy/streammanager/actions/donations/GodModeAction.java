package com.foxy.streammanager.actions.donations;

import com.foxy.streammanager.actions.ActionContext;
import com.foxy.streammanager.actions.ActionStrategy;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class GodModeAction implements ActionStrategy {
    @Override
    public void execute(ActionContext context) {
        int duration = Math.max(1, context.config().getInt("duration-seconds", 120)) * 20;
        context.player().addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, duration, 4));
        context.player().addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, duration, 4));
        context.player().addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, duration, 9));
        context.player().addPotionEffect(new PotionEffect(PotionEffectType.SPEED, duration, 1));
    }
}