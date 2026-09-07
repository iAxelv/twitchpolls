package com.foxy.streammanager.actions.donations;

import com.foxy.streammanager.actions.ActionContext;
import com.foxy.streammanager.actions.ActionStrategy;
import org.bukkit.Location;
import org.bukkit.entity.TNTPrimed;

public class NukeStrikeAction implements ActionStrategy {
    @Override
    public void execute(ActionContext context) {
        Location location = context.player().getLocation().clone().add(0, context.config().getDouble("height", 18.0), 0);
        int amount = Math.max(1, context.config().getInt("amount", 5));
        int fuseTicks = Math.max(1, context.config().getInt("fuse-ticks", 60));
        for (int index = 0; index < amount; index++) {
            TNTPrimed tnt = context.world().spawn(location, TNTPrimed.class);
            tnt.setFuseTicks(fuseTicks + index * 2);
        }
    }
}