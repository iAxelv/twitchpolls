package com.foxy.streammanager.actions.donations;

import com.foxy.streammanager.actions.ActionContext;
import com.foxy.streammanager.actions.ActionStrategy;
import org.bukkit.Material;
import org.bukkit.block.data.Orientable;
import org.bukkit.Axis;

public class WelcomeToHellAction implements ActionStrategy {
    @Override
    public void execute(ActionContext context) {
        int innerSize = Math.max(2, Math.min(21, context.config().getInt("size", 3)));
        var base = context.player().getLocation().getBlock();
        int outerSize = innerSize + 2;
        int half = outerSize / 2;
        for (int x = -half; x <= half; x++) {
            for (int y = 0; y <= innerSize + 1; y++) {
                boolean frame = x == -half || x == half || y == 0 || y == innerSize + 1;
                var block = base.getRelative(x, y, 0);
                if (frame) {
                    block.setType(Material.OBSIDIAN, false);
                } else {
                    block.setType(Material.NETHER_PORTAL, false);
                    if (block.getBlockData() instanceof Orientable portal) {
                        portal.setAxis(Axis.X);
                        block.setBlockData(portal, false);
                    }
                }
            }
        }
    }
}