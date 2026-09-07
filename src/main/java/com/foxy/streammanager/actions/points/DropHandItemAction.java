package com.foxy.streammanager.actions.points;

import com.foxy.streammanager.actions.ActionContext;
import com.foxy.streammanager.actions.ActionStrategy;
import org.bukkit.Sound;
import org.bukkit.scheduler.BukkitRunnable;

public class DropHandItemAction implements ActionStrategy {
    @Override
    public void execute(ActionContext context) {
        long duration = Math.max(1L, context.config().getLong("duration-seconds", 15)) * 20L;
        long interval = Math.max(1L, context.config().getLong("interval-seconds", 3)) * 20L;
        new BukkitRunnable() {
            long remaining = duration;
            @Override public void run() {
                if (!context.player().isOnline() || remaining <= 0) { cancel(); return; }
                if (!context.player().getInventory().getItemInMainHand().isEmpty()) context.player().dropItem(false);
                context.world().playSound(context.location(), Sound.ENTITY_SLIME_SQUISH, 1.0f, 1.0f);
                remaining -= interval;
            }
        }.runTaskTimer(context.plugin(), 0L, interval);
    }
}