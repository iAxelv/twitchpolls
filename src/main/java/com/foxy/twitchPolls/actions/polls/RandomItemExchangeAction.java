package com.foxy.twitchPolls.actions.polls;

import com.foxy.twitchPolls.actions.ActionContext;
import com.foxy.twitchPolls.actions.ActionStrategy;

import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

public class RandomItemExchangeAction implements ActionStrategy {
    private static final Set<Material> BLACKLIST = EnumSet.of(
            Material.BARRIER,
            Material.BEDROCK,
            Material.COMMAND_BLOCK,
            Material.END_PORTAL
    );

    @Override public void execute(ActionContext context) {
        PlayerInventory inventory = context.player().getInventory();
        List<Integer> filledSlots = new ArrayList<>();
        for (int i = 0; i < 36; i++) {
            ItemStack item = inventory.getItem(i);
            if (item != null && item.getType() != Material.AIR) filledSlots.add(i);
        }

        if (filledSlots.isEmpty()) return;

        int slot = filledSlots.get(ThreadLocalRandom.current().nextInt(filledSlots.size()));
        ItemStack removedItem = inventory.getItem(slot).clone();
        removedItem.setAmount(1);

        ItemStack remainingItem = inventory.getItem(slot).clone();
        if (remainingItem.getAmount() == 1) inventory.setItem(slot, null);
        else {
            remainingItem.setAmount(remainingItem.getAmount() - 1);
            inventory.setItem(slot, remainingItem);
        }

        Material randomMaterial = getRandomItemMaterial();
        Map<Integer, ItemStack> leftovers = inventory.addItem(new ItemStack(randomMaterial));
        if (!leftovers.isEmpty()) inventory.addItem(removedItem);

        context.world().playSound(context.player().getLocation(), Sound.ENTITY_ITEM_PICKUP, 1.0f, 1.0f);
    }

    private Material getRandomItemMaterial() {
        Material[] materials = Material.values();
        while (true) {
            Material material = materials[ThreadLocalRandom.current().nextInt(materials.length)];
            if (material.isItem() && !material.isAir() && !BLACKLIST.contains(material)) return material;
        }
    }
}