package com.foxy.twitchPolls.actions.points;

import com.foxy.twitchPolls.actions.ActionContext;
import com.foxy.twitchPolls.actions.ActionStrategy;
import org.bukkit.Sound;

public class DropHandItemAction implements ActionStrategy {
    @Override
    public void execute(ActionContext context) {
        if (!context.player().getInventory().getItemInMainHand().isEmpty()) {
            context.player().dropItem(false);
        }
        context.world().playSound(context.location(), Sound.ENTITY_SLIME_SQUISH, 1.0f, 1.0f);
    }
}