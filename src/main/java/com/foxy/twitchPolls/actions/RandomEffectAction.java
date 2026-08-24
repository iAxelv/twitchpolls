package com.foxy.twitchPolls.actions;

import org.bukkit.Registry;
import org.bukkit.potion.PotionEffect;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class RandomEffectAction implements ActionStrategy {
    @Override public void execute(ActionContext context) {
        List<org.bukkit.potion.PotionEffectType> effects = new ArrayList<>();
        Registry.POTION_EFFECT_TYPE.forEach(effects::add);
        context.player().addPotionEffect(new PotionEffect(effects.get(ThreadLocalRandom.current().nextInt(effects.size())), 100, 4));
    }
}