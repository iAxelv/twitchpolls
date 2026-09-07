package com.foxy.streammanager.actions.points;

import com.foxy.streammanager.actions.ActionContext;
import com.foxy.streammanager.actions.ActionStrategy;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Item;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.concurrent.ThreadLocalRandom;

public class ItemRouletteShowerAction implements ActionStrategy {
    private static final Material[] ITEMS = {Material.WHEAT_SEEDS, Material.STONE_BUTTON,
            Material.POISONOUS_POTATO, Material.SPIDER_EYE, Material.OAK_DOOR};

    @Override public void execute(ActionContext context) {
        int amount = Math.max(1, context.config().getInt("amount", 150));
        double radius = Math.max(1.0, context.config().getDouble("radius", 8.0));
        double height = Math.max(2.0, context.config().getDouble("height", 15.0));
        new BukkitRunnable() {
            int spawned;
            @Override public void run() {
                if (!context.player().isOnline() || spawned >= amount) { cancel(); return; }
                Location location = context.randomLocation(radius, height
                        + ThreadLocalRandom.current().nextDouble(0, 8));
                Item item = context.world().dropItem(location, new ItemStack(
                        ITEMS[ThreadLocalRandom.current().nextInt(ITEMS.length)],
                        ThreadLocalRandom.current().nextInt(1, 4)));
                item.setVelocity(item.getVelocity().setY(-0.15));
                spawned++;
            }
        }.runTaskTimer(context.plugin(), 0L,
                Math.max(1L, context.config().getLong("spawn-interval-ticks", 1L)));
    }
}
