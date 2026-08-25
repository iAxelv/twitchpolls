package com.foxy.twitchPolls.actions.donations;

import com.foxy.twitchPolls.actions.ActionContext;
import com.foxy.twitchPolls.actions.ActionStrategy;
import net.kyori.adventure.text.Component;
import org.bukkit.Location;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Vindicator;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class MobArmyAction implements ActionStrategy {
    @Override
    public void execute(ActionContext context) {
        int amount = Math.max(1, context.config().getInt("amount", 5));
        double radius = Math.max(0.0, context.config().getDouble("radius", 4.0));
        int speedAmplifier = Math.max(0, context.config().getInt("speed-amplifier", 1));
        String username = context.redeemerUsername() == null || context.redeemerUsername().isBlank()
                ? "Donador" : context.redeemerUsername();

        for (int index = 0; index < amount; index++) {
            Location spawnLocation = context.randomLocation(radius, 0);
            Vindicator vindicator = (Vindicator) context.world().spawnEntity(spawnLocation, EntityType.VINDICATOR);
            vindicator.customName(Component.text(username));
            vindicator.setCustomNameVisible(true);
            vindicator.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, Integer.MAX_VALUE, speedAmplifier));
            vindicator.setTarget(context.player());
        }
    }
}