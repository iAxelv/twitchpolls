package com.foxy.twitchPolls.actions.points;

import com.foxy.twitchPolls.actions.ActionContext;
import com.foxy.twitchPolls.actions.ActionStrategy;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class ItemNameSwapAction implements ActionStrategy {
    private static final List<String> NAMES = List.of("&c¿Esto qué es?", "&dPatata legendaria", "&eObjeto sospechoso", "&bCosa brillante");
    @Override public void execute(ActionContext context) {
        var inventory = context.player().getInventory();
        List<ItemStack> originals = new ArrayList<>();
        for (int slot = 0; slot < 9; slot++) {
            ItemStack item = inventory.getItem(slot);
            originals.add(item == null ? null : item.clone());
            if (item != null && !item.isEmpty()) {
                ItemStack renamed = item.clone();
                renamed.editMeta(meta -> meta.displayName(LegacyComponentSerializer.legacyAmpersand()
                    .deserialize(NAMES.get(ThreadLocalRandom.current().nextInt(NAMES.size())))));
                inventory.setItem(slot, renamed);
            }
        }
        new BukkitRunnable() { @Override public void run() { if (context.player().isOnline()) for (int slot = 0; slot < 9; slot++) inventory.setItem(slot, originals.get(slot)); } }
                .runTaskLater(context.plugin(), Math.max(1, context.config().getInt("duration-seconds", 15)) * 20L);
    }
}