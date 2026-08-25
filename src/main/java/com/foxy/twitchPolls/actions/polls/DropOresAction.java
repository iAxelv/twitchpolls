package com.foxy.twitchPolls.actions.polls;

import com.foxy.twitchPolls.actions.ActionContext;
import com.foxy.twitchPolls.actions.ActionStrategy;

import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.inventory.ItemStack;
import java.util.concurrent.ThreadLocalRandom;

public class DropOresAction implements ActionStrategy {
    private static final Material[] ORES = {Material.IRON_INGOT, Material.LAPIS_LAZULI, Material.REDSTONE, Material.COAL, Material.EMERALD, Material.DIAMOND};
    @Override public void execute(ActionContext context) {
        int amount = ThreadLocalRandom.current().nextInt(context.config().getInt("spawn_min", 1), context.config().getInt("spawn_max", 5) + 1);
        double radius = context.config().getDouble("radius", 1.0);
        for (int i = 0; i < amount; i++) {
            Material ore = ORES[ThreadLocalRandom.current().nextInt(ORES.length)];
            context.world().dropItem(context.randomLocation(radius, 1), new ItemStack(ore));
        }
        context.world().playSound(context.location(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 2.0f);
    }
}