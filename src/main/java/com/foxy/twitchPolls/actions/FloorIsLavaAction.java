package com.foxy.twitchPolls.actions;

import org.bukkit.Material;
import org.bukkit.scheduler.BukkitRunnable;

public class FloorIsLavaAction implements ActionStrategy {
    @Override public void execute(ActionContext context) {
        new BukkitRunnable() {
            int ticks;
            @Override public void run() {
                if (ticks >= 200 || !context.player().isOnline()) { cancel(); return; }
                if (context.player().getLocation().getBlock().getType() != Material.WATER) context.player().setFireTicks(40);
                ticks += 10;
            }
        }.runTaskTimer(context.plugin(), 0L, 10L);
    }
}