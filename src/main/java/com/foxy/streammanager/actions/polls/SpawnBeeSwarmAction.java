package com.foxy.streammanager.actions.polls;

import com.foxy.streammanager.actions.ActionContext;
import com.foxy.streammanager.actions.ActionStrategy;

import org.bukkit.entity.Bee;
import org.bukkit.entity.EntityType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import java.util.concurrent.ThreadLocalRandom;

public class SpawnBeeSwarmAction implements ActionStrategy {
    @Override public void execute(ActionContext context) {
        int amount = ThreadLocalRandom.current().nextInt(5, 11);
        for (int i = 0; i < amount; i++) {
            Bee bee = (Bee) context.world().spawnEntity(context.randomLocation(3.0, 1), EntityType.BEE);
            bee.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, 999999, 9));
            if (ThreadLocalRandom.current().nextBoolean()) bee.setTarget(context.player());
        }
    }
}