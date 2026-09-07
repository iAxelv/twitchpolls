package com.foxy.streammanager.actions.points;

import com.foxy.streammanager.actions.ActionContext;
import com.foxy.streammanager.actions.ActionStrategy;
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
        int duration = Math.max(1, context.config().getInt("duration-seconds", 20));
        int strengthAmplifier = Math.max(0, context.config().getInt("strength-amplifier", 0));
        for (int index = 0; index < Math.max(1, context.config().getInt("amount", 3)); index++) {
            Phantom phantom = (Phantom) context.world().spawnEntity(context.location(), EntityType.PHANTOM);
            phantom.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, duration * 20, 0));
            phantom.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, duration * 20, 0));
            phantom.addPotionEffect(new PotionEffect(PotionEffectType.FIRE_RESISTANCE, duration * 20, 0));
            phantom.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, duration * 20, strengthAmplifier));
            phantoms.add(phantom);
        }
        context.world().playSound(context.location(), Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 0.8f, 1.8f);
        new BukkitRunnable() {
            @Override
            public void run() {
                phantoms.stream().filter(Phantom::isValid).forEach(Phantom::remove);
            }
        }.runTaskLater(context.plugin(), duration * 20L);
    }
}