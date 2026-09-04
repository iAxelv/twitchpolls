package com.foxy.twitchPolls.actions.polls;

import com.foxy.twitchPolls.actions.ActionContext;
import com.foxy.twitchPolls.actions.ActionStrategy;

import org.bukkit.Material;
import org.bukkit.scheduler.BukkitRunnable;

public class FloorIsLavaAction implements ActionStrategy {
    @Override public void execute(ActionContext context) {
        int maxTicks = Math.max(1, context.config().getInt("duration-seconds", 10)) * 20;
        new BukkitRunnable() {
            int ticks;
            @Override public void run() {
                if (ticks >= maxTicks || !context.player().isOnline()) { cancel(); return; }
                if (context.player().getLocation().getBlock().getType() != Material.WATER) context.player().setFireTicks(40);
                ticks += 10;
            }
        }.runTaskTimer(context.plugin(), 0L, 10L);
    }
}