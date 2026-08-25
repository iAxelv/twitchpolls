package com.foxy.twitchPolls.actions.points;

import com.foxy.twitchPolls.actions.ActionContext;
import com.foxy.twitchPolls.actions.ActionStrategy;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.data.BlockData;

public class AnvilTrapAction implements ActionStrategy {
    @Override
    public void execute(ActionContext context) {
        BlockData data = Material.DAMAGED_ANVIL.createBlockData();
        context.world().spawnFallingBlock(context.location().clone().add(0, context.config().getDouble("height", 6.0), 0), data)
                .setDropItem(false);
        context.world().playSound(context.location(), Sound.ENTITY_ARROW_SHOOT, 1.0f, 1.7f);
    }
}