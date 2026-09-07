package com.foxy.streammanager.actions.polls;

import com.foxy.streammanager.actions.ActionContext;
import com.foxy.streammanager.actions.ActionStrategy;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.DragonFireball;
import org.bukkit.scheduler.BukkitRunnable;
import java.util.ArrayList;
import java.util.List;

public class AirStrikeWarningAction implements ActionStrategy {
    @Override public void execute(ActionContext context) {
        Location center = context.player().getLocation().clone();
        List<Location> impacts = new ArrayList<>();
        for (int i = 0; i < 5; i++) impacts.add(context.randomLocation(
                Math.max(4.0, context.config().getDouble("radius", 12.0)), 0));
        new BukkitRunnable() {
            int ticks;
            @Override public void run() {
                if (!context.player().isOnline() || ticks >= 60) { cancel(); return; }
                for (Location impact : impacts) {
                    context.world().spawnParticle(Particle.DUST, impact.clone().add(0, 0.1, 0), 8,
                            1.2, 0.02, 1.2, new Particle.DustOptions(Color.RED, 1.4f));
                    context.world().playSound(center, Sound.BLOCK_NOTE_BLOCK_HAT, 1.0f,
                            ticks % 10 < 5 ? 0.65f : 1.1f);
                }
                if (ticks % 10 == 0) context.world().playSound(center, Sound.BLOCK_NOTE_BLOCK_BASEDRUM, 1.2f, 0.7f);
                ticks += 5;
            }
        }.runTaskTimer(context.plugin(), 0L, 5L);
        new BukkitRunnable() {
            @Override public void run() {
                for (Location impact : impacts) {
                    Location spawn = impact.clone().add(0, Math.max(12.0,
                            context.config().getDouble("height", 22.0)), 0);
                    DragonFireball fireball = context.world().spawn(spawn, DragonFireball.class);
                    fireball.setDirection(impact.toVector().subtract(spawn.toVector()).normalize());
                    fireball.setYield((float) context.config().getDouble("yield", 2.0));
                    fireball.setIsIncendiary(false);
                }
            }
        }.runTaskLater(context.plugin(), 70L);
    }
}
