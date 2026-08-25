package com.foxy.twitchPolls.actions.points;

import com.foxy.twitchPolls.actions.ActionContext;
import com.foxy.twitchPolls.actions.ActionStrategy;

public class InstantHealAction implements ActionStrategy {
    @Override
    public void execute(ActionContext context) {
        context.player().setHealth(context.player().getMaxHealth());
        context.player().setFoodLevel(20);
        context.player().setSaturation(20.0f);
    }
}