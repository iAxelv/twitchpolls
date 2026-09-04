package com.foxy.twitchPolls.actions.tiktok;

import com.foxy.twitchPolls.actions.ActionContext;
import com.foxy.twitchPolls.actions.ActionStrategy;
import org.bukkit.Location;
import org.bukkit.entity.TNTPrimed;

public class TntCircleAction implements ActionStrategy {
    @Override
    public void execute(ActionContext context) {
        long scaledAmount = (long) Math.max(1, context.config().getInt("amount", 10))
                * (context.config().getBoolean("scale-with-combo", false) ? context.eventMultiplier() : 1);
        int amount = (int) Math.min(Integer.MAX_VALUE, scaledAmount);
        double radius = Math.max(0.5, context.config().getDouble("radius", 4.0));
        int fuseTicks = Math.max(1, context.config().getInt("fuse-ticks", 40));
        Location center = context.location();
        for (int index = 0; index < amount; index++) {
            double angle = Math.PI * 2 * index / amount;
            Location location = center.clone().add(Math.cos(angle) * radius, 0, Math.sin(angle) * radius);
            TNTPrimed tnt = context.world().spawn(location, TNTPrimed.class);
            tnt.setFuseTicks(fuseTicks);
        }
    }
}
