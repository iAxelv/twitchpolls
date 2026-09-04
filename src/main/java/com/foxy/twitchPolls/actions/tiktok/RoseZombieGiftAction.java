package com.foxy.twitchPolls.actions.tiktok;

import com.foxy.twitchPolls.actions.ActionContext;
import com.foxy.twitchPolls.actions.ActionStrategy;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Zombie;

public class RoseZombieGiftAction implements ActionStrategy {
    @Override
    public void execute(ActionContext context) {
        long scaledAmount = (long) Math.max(1, context.config().getInt("amount", 30))
            * context.eventMultiplier();
        int amount = (int) Math.min(Integer.MAX_VALUE, scaledAmount);
        String donor = context.redeemerUsername() == null
                ? "unknow" : context.redeemerUsername();

        for (int index = 0; index < amount; index++) {
            Zombie zombie = (Zombie) context.world().spawnEntity(
                    context.randomLocation(context.config().getDouble("radius", 4.0), 0),
                    EntityType.ZOMBIE);
            zombie.customName(Component.text(donor));
            zombie.setCustomNameVisible(true);
        }
    }
}