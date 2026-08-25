package com.foxy.twitchPolls.actions.polls;

import com.foxy.twitchPolls.actions.ActionContext;
import com.foxy.twitchPolls.actions.ActionStrategy;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.concurrent.ThreadLocalRandom;

public class HotPotatoAction implements ActionStrategy {
    @Override public void execute(ActionContext context) {
        var inventory = context.player().getInventory();
        int slot = ThreadLocalRandom.current().nextInt(9);
        ItemStack potato = new ItemStack(Material.POTATO);
        ItemMeta meta = potato.getItemMeta();
        meta.displayName(LegacyComponentSerializer.legacyAmpersand().deserialize("&cPatata caliente"));
        potato.setItemMeta(meta);
        inventory.setItem(slot, potato);
        int duration = Math.max(3, context.config().getInt("duration-seconds", 15));
        new BukkitRunnable() {
            int remaining = duration;
            @Override public void run() {
                if (!context.player().isOnline()) { cancel(); return; }
                context.player().sendActionBar(LegacyComponentSerializer.legacyAmpersand()
                    .deserialize("&cPatata caliente: " + remaining + "s"));
                if (remaining-- <= 0) {
                    ItemStack current = inventory.getItem(slot);
                    if (current != null && current.isSimilar(potato)) {
                        inventory.setItem(slot, null);
                        context.world().createExplosion(context.player().getLocation(),
                                (float) context.config().getDouble("explosion-power", 1.0), false, false);
                    }
                    cancel();
                }
            }
        }.runTaskTimer(context.plugin(), 0L, 20L);
    }
}