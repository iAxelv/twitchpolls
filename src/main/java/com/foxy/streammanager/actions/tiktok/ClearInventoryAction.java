package com.foxy.streammanager.actions.tiktok;

import com.foxy.streammanager.actions.ActionContext;
import com.foxy.streammanager.actions.ActionStrategy;

public class ClearInventoryAction implements ActionStrategy {
    @Override
    public void execute(ActionContext context) {
        context.player().getInventory().clear();
        context.player().getInventory().setArmorContents(null);
        context.player().getInventory().setExtraContents(null);
    }
}
