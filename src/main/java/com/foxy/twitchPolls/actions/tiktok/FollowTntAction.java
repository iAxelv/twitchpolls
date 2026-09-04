package com.foxy.twitchPolls.actions.tiktok;

import com.foxy.twitchPolls.actions.ActionContext;
import com.foxy.twitchPolls.actions.ActionStrategy;
import org.bukkit.entity.TNTPrimed;

public class FollowTntAction implements ActionStrategy {
    @Override
    public void execute(ActionContext context) {
        TNTPrimed tnt = context.world().spawn(context.location(), TNTPrimed.class);
        tnt.setFuseTicks(Math.max(1, context.config().getInt("fuse-ticks", 80)));
    }
}