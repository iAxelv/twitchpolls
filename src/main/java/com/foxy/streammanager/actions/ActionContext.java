package com.foxy.streammanager.actions;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.concurrent.ThreadLocalRandom;

public record ActionContext(Plugin plugin, Player player, ConfigurationSection config,
                            String streamerUsername, String redeemerUsername, int eventMultiplier) {
    public World world() { return player.getWorld(); }
    public Location location() { return player.getLocation(); }

    public Location randomLocation(double radius, double yOffset) {
        double angle = ThreadLocalRandom.current().nextDouble(Math.PI * 2);
        double distance = ThreadLocalRandom.current().nextDouble(radius);
        return location().clone().add(Math.cos(angle) * distance, yOffset, Math.sin(angle) * distance);
    }
}