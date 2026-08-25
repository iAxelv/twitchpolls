package com.foxy.twitchPolls.actions.polls;

import com.foxy.twitchPolls.actions.ActionContext;
import com.foxy.twitchPolls.actions.ActionStrategy;

import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class ConfusionAction implements ActionStrategy {
    @Override public void execute(ActionContext context) {
        context.player().addPotionEffect(new PotionEffect(PotionEffectType.NAUSEA, context.config().getInt("time", 300), context.config().getInt("amplifier", 2)));
    }
}