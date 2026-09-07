package com.foxy.streammanager.actions.polls;

import com.foxy.streammanager.actions.ActionContext;
import com.foxy.streammanager.actions.ActionStrategy;

import org.bukkit.Registry;
import org.bukkit.potion.PotionEffect;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class RandomEffectAction implements ActionStrategy {
    @Override public void execute(ActionContext context) {
        List<org.bukkit.potion.PotionEffectType> effects = new ArrayList<>();
        Registry.POTION_EFFECT_TYPE.forEach(effects::add);
        Collections.shuffle(effects);
        effects.stream()
                .limit(10)
                .forEach(effect -> context.player().addPotionEffect(new PotionEffect(effect, 100, 4)));
    }
}