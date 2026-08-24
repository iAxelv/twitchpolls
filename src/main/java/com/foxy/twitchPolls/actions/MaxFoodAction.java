package com.foxy.twitchPolls.actions;

import org.bukkit.Sound;

public class MaxFoodAction implements ActionStrategy {
    @Override public void execute(ActionContext context) {
        context.player().setFoodLevel(20);
        context.player().setSaturation(20.0f);
        context.world().playSound(context.player().getLocation(), Sound.ENTITY_PLAYER_BURP, 1.0f, 1.0f);
    }
}