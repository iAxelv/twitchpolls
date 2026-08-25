package com.foxy.twitchPolls.actions.points;

import com.foxy.twitchPolls.actions.ActionContext;
import com.foxy.twitchPolls.actions.ActionStrategy;
import org.bukkit.attribute.Attribute;

public class InstantHealAction implements ActionStrategy {
    @Override
    public void execute(ActionContext context) {
        double maxHealth = context.player().getAttribute(Attribute.MAX_HEALTH).getValue();
        context.player().setHealth(maxHealth);
        context.player().setFoodLevel(20);
        context.player().setSaturation(20.0f);
    }
}