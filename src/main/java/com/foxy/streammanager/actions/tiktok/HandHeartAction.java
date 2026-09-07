package com.foxy.streammanager.actions.tiktok;

import com.foxy.streammanager.actions.ActionContext;
import com.foxy.streammanager.actions.ActionStrategy;
import net.kyori.adventure.text.Component;
import org.bukkit.Location;
import org.bukkit.entity.Enderman;
import org.bukkit.entity.EntityType;

public class HandHeartAction implements ActionStrategy {
    @Override
    public void execute(ActionContext context) {
        int amount = Math.max(1, context.config().getInt("amount", 10));
        double radius = Math.max(0.5, context.config().getDouble("radius", 4.0));
        String donor = context.redeemerUsername() == null || context.redeemerUsername().isBlank()
                ? "Donor" : context.redeemerUsername();
        Location center = context.location();
        for (int index = 0; index < amount; index++) {
            double angle = Math.PI * 2 * index / amount;
            Location location = center.clone().add(Math.cos(angle) * radius, 0, Math.sin(angle) * radius);
            Enderman enderman = (Enderman) context.world().spawnEntity(location, EntityType.ENDERMAN);
            enderman.customName(Component.text(donor));
            enderman.setCustomNameVisible(true);
            enderman.setTarget(context.player());
        }
    }
}
