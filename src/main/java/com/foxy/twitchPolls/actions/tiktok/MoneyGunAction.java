package com.foxy.twitchPolls.actions.tiktok;

import com.foxy.twitchPolls.actions.ActionContext;
import com.foxy.twitchPolls.actions.ActionStrategy;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Wither;

public class MoneyGunAction implements ActionStrategy {
    @Override
    public void execute(ActionContext context) {
        long scaledAmount = (long) Math.max(1, context.config().getInt("amount", 3)) * context.eventMultiplier();
        int amount = (int) Math.min(Integer.MAX_VALUE, scaledAmount);
        String donor = context.redeemerUsername() == null || context.redeemerUsername().isBlank()
                ? "Donor" : context.redeemerUsername();
        for (int index = 0; index < amount; index++) {
            Wither wither = (Wither) context.world().spawnEntity(context.randomLocation(
                    context.config().getDouble("radius", 3.0), 1.0), EntityType.WITHER);
            wither.customName(Component.text(donor));
            wither.setCustomNameVisible(true);
            wither.setTarget(context.player());
        }
    }
}
