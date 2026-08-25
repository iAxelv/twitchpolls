package com.foxy.twitchPolls.actions.polls;

import com.foxy.twitchPolls.actions.ActionContext;
import com.foxy.twitchPolls.actions.ActionStrategy;

import org.bukkit.Sound;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import java.util.concurrent.ThreadLocalRandom;

public class EffectLevitationAction implements ActionStrategy {
    @Override public void execute(ActionContext context) {
        int duration = ThreadLocalRandom.current().nextInt(context.config().getInt("time_min", 100), context.config().getInt("time_max", 300) + 1);
        context.player().addPotionEffect(new PotionEffect(PotionEffectType.LEVITATION, duration, context.config().getInt("amplifier", 5)));
        context.world().playSound(context.location(), Sound.ENTITY_ENDER_DRAGON_FLAP, 1.0f, 1.0f);
    }
}