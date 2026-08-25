package com.foxy.twitchPolls.actions.points;

import com.foxy.twitchPolls.actions.ActionContext;
import com.foxy.twitchPolls.actions.ActionStrategy;
import org.bukkit.Sound;
import org.bukkit.inventory.PlayerInventory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class DropRandomItemAction implements ActionStrategy {
    @Override public void execute(ActionContext context) {
        PlayerInventory inventory = context.player().getInventory();
        List<Integer> filled = new ArrayList<>();
        for (int slot = 0; slot < 36; slot++) if (inventory.getItem(slot) != null && !inventory.getItem(slot).isEmpty()) filled.add(slot);
        if (filled.isEmpty()) return;
        int slot = filled.get(ThreadLocalRandom.current().nextInt(filled.size()));
        var item = inventory.getItem(slot);
        inventory.setItem(slot, null);
        context.world().dropItemNaturally(context.location(), item);
        context.player().playSound(context.location(), Sound.ENTITY_ITEM_PICKUP, 1.0f, 0.7f);
    }
}