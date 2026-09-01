package com.foxy.twitchPolls.actions.polls;

import com.foxy.twitchPolls.TwitchPolls;
import com.foxy.twitchPolls.actions.ActionContext;
import com.foxy.twitchPolls.actions.ActionStrategy;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.concurrent.ThreadLocalRandom;
import java.util.UUID;

public class HotPotatoAction implements ActionStrategy {
    @Override public void execute(ActionContext context) {
        TwitchPolls plugin = (TwitchPolls) context.plugin();
        var inventory = context.player().getInventory();
        Material[] items = {Material.POTATO, Material.BAKED_POTATO, Material.APPLE, Material.CARROT,
            Material.BREAD, Material.COOKIE, Material.PUMPKIN_PIE, Material.MELON_SLICE};
        ItemStack potato = new ItemStack(items[ThreadLocalRandom.current().nextInt(items.length)]);
        ItemMeta meta = potato.getItemMeta();
        NamespacedKey potatoKey = new NamespacedKey(context.plugin(), "hot_potato_id");
        String potatoId = UUID.randomUUID().toString();
        meta.getPersistentDataContainer().set(potatoKey, PersistentDataType.STRING, potatoId);
        potato.setItemMeta(meta);
        var leftovers = inventory.addItem(potato);
        leftovers.values().forEach(item -> context.player().getWorld().dropItemNaturally(
            context.player().getLocation(), item));
        int duration = Math.max(3, context.config().getInt("duration-seconds", 15));
        new BukkitRunnable() {
            int remaining = duration;
            @Override public void run() {
                if (!context.player().isOnline()) { cancel(); return; }
                String format = plugin.getLanguageManager().getString(
                        "messages.hot-potato-actionbar", "&cHot potato: %time%s");
                context.player().sendActionBar(LegacyComponentSerializer.legacyAmpersand()
                    .deserialize(format.replace("%time%", String.valueOf(remaining))));
                if (remaining-- <= 0) {
                    for (int slot = 0; slot < inventory.getSize(); slot++) {
                        ItemStack current = inventory.getItem(slot);
                        if (current == null || current.getItemMeta() == null) continue;
                        String currentId = current.getItemMeta().getPersistentDataContainer()
                            .get(potatoKey, PersistentDataType.STRING);
                        if (potatoId.equals(currentId)) {
                            inventory.setItem(slot, null);
                            context.world().createExplosion(context.player().getLocation(),
                                (float) context.config().getDouble("explosion-power", 1.0), false, false);
                            break;
                        }
                    }
                    cancel();
                }
            }
        }.runTaskTimer(context.plugin(), 0L, 20L);
    }
}