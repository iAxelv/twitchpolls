package com.foxy.streammanager.actions.tiktok;

import com.foxy.streammanager.actions.ActionContext;
import com.foxy.streammanager.actions.ActionStrategy;
import net.kyori.adventure.text.Component;
import org.bukkit.Location;
import org.bukkit.entity.Blaze;
import org.bukkit.entity.EntityType;

public class PerfumeAction implements ActionStrategy {
    @Override
    public void execute(ActionContext context) {
        int amount = Math.max(1, context.config().getInt("amount", 5));
        double radius = Math.max(0.5, context.config().getDouble("radius", 4.0));
        String donor = context.redeemerUsername() == null || context.redeemerUsername().isBlank()
                ? "Donor" : context.redeemerUsername();
        Location center = context.location();
        for (int index = 0; index < amount; index++) {
            double angle = Math.PI * 2 * index / amount;
            Location location = center.clone().add(Math.cos(angle) * radius, 0, Math.sin(angle) * radius);
            Blaze blaze = (Blaze) context.world().spawnEntity(location, EntityType.BLAZE);
            blaze.customName(Component.text(donor));
            blaze.setCustomNameVisible(true);
            blaze.setTarget(context.player());
        }
    }
}
