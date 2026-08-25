package com.foxy.twitchPolls.actions.points;

import com.foxy.twitchPolls.actions.ActionContext;
import com.foxy.twitchPolls.actions.ActionStrategy;
import org.bukkit.Sound;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Phantom;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("null")
public class SilentPhantomsAction implements ActionStrategy {
    @Override
    public void execute(ActionContext context) {
        List<Phantom> phantoms = new ArrayList<>();
        for (int index = 0; index < Math.max(1, context.config().getInt("amount", 3)); index++) {
            Phantom phantom = (Phantom) context.world().spawnEntity(context.location(), EntityType.PHANTOM);
            phantom.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY,
                    Math.max(1, context.config().getInt("duration-seconds", 20)) * 20, 0));
            phantom.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING,
                    Math.max(1, context.config().getInt("duration-seconds", 20)) * 20, 0));
            phantoms.add(phantom);
        }
        context.world().playSound(context.location(), Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 0.8f, 1.8f);
        new BukkitRunnable() {
            @Override
            public void run() {
                phantoms.stream().filter(Phantom::isValid).forEach(Phantom::remove);
            }
        }.runTaskLater(context.plugin(), Math.max(1, context.config().getInt("duration-seconds", 20)) * 20L);
    }
}