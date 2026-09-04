package com.foxy.twitchPolls.actions.tiktok;

import com.foxy.twitchPolls.actions.ActionContext;
import com.foxy.twitchPolls.actions.ActionStrategy;

public class ClearInventoryAction implements ActionStrategy {
    @Override
    public void execute(ActionContext context) {
        context.player().getInventory().clear();
        context.player().getInventory().setArmorContents(null);
        context.player().getInventory().setExtraContents(null);
    }
}
