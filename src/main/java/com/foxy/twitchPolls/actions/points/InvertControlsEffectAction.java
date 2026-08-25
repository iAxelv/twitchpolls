package com.foxy.twitchPolls.actions.points;

import com.foxy.twitchPolls.actions.ActionContext;
import com.foxy.twitchPolls.actions.ActionStrategy;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class InvertControlsEffectAction implements ActionStrategy {
    @Override
    public void execute(ActionContext context) {
        int duration = Math.max(1, context.config().getInt("duration-seconds", 6)) * 20;
        context.player().addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, duration,
                context.config().getInt("slowness-amplifier", 5)));
        context.player().addPotionEffect(new PotionEffect(PotionEffectType.JUMP_BOOST, duration,
                context.config().getInt("jump-amplifier", 127)));
    }
}