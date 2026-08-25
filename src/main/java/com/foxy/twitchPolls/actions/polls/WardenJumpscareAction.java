package com.foxy.twitchPolls.actions.polls;

import com.foxy.twitchPolls.actions.ActionContext;
import com.foxy.twitchPolls.actions.ActionStrategy;

import org.bukkit.Location;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Warden;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

public class WardenJumpscareAction implements ActionStrategy {
    @Override public void execute(ActionContext context) {
        Vector direction = context.location().getDirection().normalize().multiply(-10);
        Location wardenLocation = context.location().clone().add(direction);
        wardenLocation.setY(context.world().getHighestBlockYAt(wardenLocation));
        Warden warden = (Warden) context.world().spawnEntity(wardenLocation, EntityType.WARDEN);
        new BukkitRunnable() {
            @Override public void run() { if (warden.isValid()) warden.remove(); }
        }.runTaskLater(context.plugin(), 100L);
    }
}