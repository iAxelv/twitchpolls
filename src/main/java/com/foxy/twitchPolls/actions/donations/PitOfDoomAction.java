package com.foxy.twitchPolls.actions.donations;

import com.foxy.twitchPolls.actions.ActionContext;
import com.foxy.twitchPolls.actions.ActionStrategy;
import org.bukkit.Material;

public class PitOfDoomAction implements ActionStrategy {
    @Override
    public void execute(ActionContext context) {
        var feet = context.player().getLocation().getBlock();
        boolean preserveBedrock = context.config().getBoolean("preserve-bedrock", true);
        for (int x = -1; x <= 1; x++) {
            for (int z = -1; z <= 1; z++) {
                for (int y = feet.getY() - 1; y >= context.world().getMinHeight(); y--) {
                    var block = feet.getWorld().getBlockAt(feet.getX() + x, y, feet.getZ() + z);
                    if (!preserveBedrock || block.getType() != Material.BEDROCK) {
                        block.setType(Material.AIR, false);
                    }
                }
            }
        }
    }
}