package com.foxy.twitchPolls.actions.points;

import com.foxy.twitchPolls.actions.ActionContext;
import com.foxy.twitchPolls.actions.ActionStrategy;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.data.BlockData;

@SuppressWarnings("deprecation")
public class AnvilTrapAction implements ActionStrategy {
    @Override
    public void execute(ActionContext context) {
        BlockData data = Material.DAMAGED_ANVIL.createBlockData();
        var anvil = context.world().spawnFallingBlock(
            context.location().clone().add(0, context.config().getDouble("height", 6.0), 0), data);
        anvil.setDropItem(false);
        anvil.setHurtEntities(true);
        anvil.setDamagePerBlock((float) context.config().getDouble("damage-per-block", 2.0));
        anvil.setMaxDamage(context.config().getInt("max-damage", 40));
        context.world().playSound(context.location(), Sound.ENTITY_ARROW_SHOOT, 1.0f, 1.7f);
    }
}