package com.foxy.twitchPolls.actions.points;

import com.foxy.twitchPolls.actions.ActionContext;
import com.foxy.twitchPolls.actions.ActionStrategy;
import org.bukkit.Location;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class SpinHeadAction implements ActionStrategy {
    @Override
    public void execute(ActionContext context) {
        Location turned = context.location().clone();
        turned.setYaw(turned.getYaw() + 180.0f);
        turned.setPitch(-turned.getPitch());
        context.player().teleport(turned);
        int duration = (int) Math.ceil(context.config().getDouble("blindness-seconds", 1.5) * 20.0);
        context.player().addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, duration, 0));
    }
}