package com.foxy.twitchPolls.actions.points;

import com.foxy.twitchPolls.TwitchPolls;
import com.foxy.twitchPolls.actions.ActionContext;
import com.foxy.twitchPolls.actions.ActionStrategy;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class ItemNameSwapAction implements ActionStrategy {
    
    @Override 
    public void execute(ActionContext context) {
        // Load item names from language file, or provide defaults
        List<String> configNames = ((TwitchPolls) context.plugin()).getLanguageManager()
            .getStringList("actions.item-name-swap.item-names");
        final List<String> names = configNames.isEmpty() 
            ? List.of("&c¿Esto qué es?", "&dPatata legendaria", "&eObjeto sospechoso", "&bCosa brillante")
            : configNames;
        
        var inventory = context.player().getInventory();
        for (int slot = 0; slot < 9; slot++) {
            ItemStack item = inventory.getItem(slot);
            if (item != null && !item.isEmpty()) {
                ItemStack renamed = item.clone();
                renamed.editMeta(meta -> meta.displayName(LegacyComponentSerializer.legacyAmpersand()
                    .deserialize(names.get(ThreadLocalRandom.current().nextInt(names.size())))));
                inventory.setItem(slot, renamed);
            }
        }
    }
}