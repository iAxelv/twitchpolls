package com.foxy.twitchPolls.actions;

import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class PotatoPremiumAction implements ActionStrategy {
    @Override public void execute(ActionContext context) {
        PlayerInventory inventory = context.player().getInventory();
        List<Integer> filledSlots = new ArrayList<>();
        for (int i = 0; i < 36; i++) {
            ItemStack item = inventory.getItem(i);
            if (item != null && item.getType() != Material.AIR) filledSlots.add(i);
        }
        if (filledSlots.isEmpty()) inventory.addItem(new ItemStack(Material.POTATO));
        else inventory.setItem(filledSlots.get(ThreadLocalRandom.current().nextInt(filledSlots.size())), new ItemStack(Material.POTATO));
        context.world().playSound(context.player().getLocation(), Sound.ENTITY_VILLAGER_YES, 1.0f, 1.0f);
    }
}