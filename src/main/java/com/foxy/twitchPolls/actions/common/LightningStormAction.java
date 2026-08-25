package com.foxy.twitchPolls.actions.common;

import com.foxy.twitchPolls.actions.ActionContext;
import com.foxy.twitchPolls.actions.ActionStrategy;
import io.papermc.paper.registry.RegistryAccess;
import io.papermc.paper.registry.RegistryKey;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.WitherSkeleton;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Locale;

public class LightningStormAction implements ActionStrategy {
    @Override
    public void execute(ActionContext context) {
        int amount = Math.max(1, context.config().getInt("lightning-amount", 1));
        int amountPerWave = Math.max(1, context.config().getInt("lightning-per-wave", amount));
        long intervalTicks = Math.max(0L, context.config().getLong("interval-ticks", 0L));
        boolean spawnZeus = context.config().getBoolean("spawn-zeus", false);

        new BukkitRunnable() {
            private int remaining = amount;

            @Override
            public void run() {
                if (!context.player().isOnline() || remaining <= 0) {
                    if (remaining <= 0 && context.player().isOnline() && spawnZeus) {
                        spawnZeus(context);
                    }
                    cancel();
                    return;
                }

                int strikes = Math.min(amountPerWave, remaining);
                for (int index = 0; index < strikes; index++) {
                    context.world().strikeLightning(context.player().getLocation());
                }
                remaining -= strikes;
                if (remaining <= 0) {
                    if (spawnZeus) {
                        spawnZeus(context);
                    }
                    cancel();
                }
            }
        }.runTaskTimer(context.plugin(), 0L, intervalTicks == 0L ? 1L : intervalTicks);
    }

    private void spawnZeus(ActionContext context) {
        WitherSkeleton zeus = (WitherSkeleton) context.world().spawnEntity(
                context.player().getLocation(), EntityType.WITHER_SKELETON);
        ConfigurationSection config = context.config().getConfigurationSection("zeus");
        if (config == null) {
            return;
        }

        String name = config.getString("nametag", "&dZEUS");
        zeus.customName(LegacyComponentSerializer.legacyAmpersand().deserialize(name));
        zeus.setCustomNameVisible(config.getBoolean("nametag-visible", true));
        zeus.setRemoveWhenFarAway(false);
        zeus.setTarget(context.player());
        applyEquipment(zeus, config);

        int speedDuration = Math.max(1, config.getInt("speed-duration-ticks", 99999));
        int speedAmplifier = Math.max(0, config.getInt("speed-amplifier", 10));
        zeus.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, speedDuration, speedAmplifier));

        long despawnTicks = Math.max(1L, config.getLong("despawn-seconds", 60L) * 20L);
        Bukkit.getScheduler().runTaskLater(context.plugin(), () -> {
            if (zeus.isValid()) {
                zeus.remove();
            }
        }, despawnTicks);
    }

    private void applyEquipment(WitherSkeleton zeus, ConfigurationSection config) {
        EntityEquipment equipment = zeus.getEquipment();
        if (equipment == null) {
            return;
        }
        equipment.setHelmet(item(config.getString("equipment.helmet", "NETHERITE_HELMET"), config, "helmet"));
        equipment.setChestplate(item(config.getString("equipment.chestplate", "NETHERITE_CHESTPLATE"), config, "chestplate"));
        equipment.setLeggings(item(config.getString("equipment.leggings", "NETHERITE_LEGGINGS"), config, "leggings"));
        equipment.setBoots(item(config.getString("equipment.boots", "NETHERITE_BOOTS"), config, "boots"));
        equipment.setItemInMainHand(item(config.getString("equipment.weapon", "GOLDEN_SWORD"), config, "weapon"));
        equipment.setHelmetDropChance(0.0f);
        equipment.setChestplateDropChance(0.0f);
        equipment.setLeggingsDropChance(0.0f);
        equipment.setBootsDropChance(0.0f);
        equipment.setItemInMainHandDropChance(0.0f);
    }

    private ItemStack item(String materialName, ConfigurationSection config, String slot) {
        Material material = Material.matchMaterial(materialName);
        ItemStack item = new ItemStack(material == null ? Material.NETHERITE_HELMET : material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setUnbreakable(config.getBoolean("equipment." + slot + "-unbreakable", true));
            var enchantments = config.getConfigurationSection("equipment." + slot + "-enchantments");
            if (enchantments != null) {
                for (String key : enchantments.getKeys(false)) {
                    Enchantment enchantment = RegistryAccess.registryAccess()
                            .getRegistry(RegistryKey.ENCHANTMENT)
                            .get(NamespacedKey.minecraft(key.toLowerCase(Locale.ROOT)));
                    if (enchantment != null) {
                        meta.addEnchant(enchantment, Math.max(1, enchantments.getInt(key)), true);
                    }
                }
            }
            item.setItemMeta(meta);
        }
        return item;
    }
}