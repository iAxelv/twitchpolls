package com.foxy.streammanager.actions.polls;

import com.foxy.streammanager.actions.ActionContext;
import com.foxy.streammanager.actions.ActionStrategy;
import org.bukkit.attribute.Attribute;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.concurrent.ThreadLocalRandom;

public class RandomSizeAction implements ActionStrategy {
    @Override
    public void execute(ActionContext context) {
        var attribute = context.player().getAttribute(Attribute.SCALE);
        if (attribute == null) return;
        double original = attribute.getBaseValue();
        double scale = ThreadLocalRandom.current().nextBoolean() ? 2.0 : 0.5;
        attribute.setBaseValue(scale);
        new BukkitRunnable() {
            @Override public void run() {
                if (context.player().isOnline()) attribute.setBaseValue(original);
            }
        }.runTaskLater(context.plugin(), Math.max(1, context.config().getInt("duration-seconds", 20)) * 20L);
    }
}