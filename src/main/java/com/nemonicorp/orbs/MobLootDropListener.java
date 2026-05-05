/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  io.lumine.mythic.lib.api.item.ItemTag
 *  io.lumine.mythic.lib.api.item.NBTItem
 *  net.Indyuce.mmoitems.MMOItems
 *  net.Indyuce.mmoitems.api.Type
 *  org.bukkit.Material
 *  org.bukkit.configuration.ConfigurationSection
 *  org.bukkit.entity.Monster
 *  org.bukkit.entity.Player
 *  org.bukkit.event.EventHandler
 *  org.bukkit.event.EventPriority
 *  org.bukkit.event.Listener
 *  org.bukkit.event.entity.EntityDeathEvent
 *  org.bukkit.inventory.ItemStack
 */
package com.nemonicorp.orbs;

import com.nemonicorp.orbs.ModifierEngine;
import com.nemonicorp.orbs.NemonicOrbPlugin;
import com.nemonicorp.orbs.OrbListener;
import io.lumine.mythic.lib.api.item.ItemTag;
import io.lumine.mythic.lib.api.item.NBTItem;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import net.Indyuce.mmoitems.MMOItems;
import net.Indyuce.mmoitems.api.Type;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Monster;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.inventory.ItemStack;

public class MobLootDropListener
implements Listener {
    private final NemonicOrbPlugin plugin;
    private final OrbListener orbListener;
    private final ModifierEngine engine;

    public MobLootDropListener(NemonicOrbPlugin plugin, OrbListener orbListener) {
        this.plugin = plugin;
        this.orbListener = orbListener;
        this.engine = plugin.getModifierEngine();
    }

    @EventHandler(priority=EventPriority.MONITOR, ignoreCancelled=true)
    public void onMobDeath(EntityDeathEvent event) {
        ItemStack pergaminho;
        if (!this.plugin.getConfig().getBoolean("mob-loot.enabled", true)) {
            return;
        }
        if (!(event.getEntity() instanceof Monster)) {
            return;
        }
        boolean requirePlayerKill = this.plugin.getConfig().getBoolean("mob-loot.require-player-kill", true);
        Player killer = event.getEntity().getKiller();
        if (requirePlayerKill && killer == null) {
            return;
        }
        ThreadLocalRandom rng = ThreadLocalRandom.current();
        double pergaminhoChance = this.plugin.getConfig().getDouble("mob-loot.pergaminho-drop-chance", 0.09);
        if (rng.nextDouble() < pergaminhoChance && (pergaminho = this.createMMOItem("CONSUMABLE", "PERGAMINHO_DE_IDENTIFICACAO")) != null) {
            event.getDrops().add(pergaminho);
        }
        double equipmentChance = this.plugin.getConfig().getDouble("mob-loot.equipment-drop-chance", 0.05);
        if (rng.nextDouble() >= equipmentChance) {
            return;
        }
        ItemStack dropped = this.createUnidentifiedEquipment();
        if (dropped == null) {
            return;
        }
        event.getDrops().add(dropped);
    }

    private ItemStack createUnidentifiedEquipment() {
        ItemStack base;
        int tier = this.rollTier();
        int itemLevel = this.rollItemLevel();
        switch (tier) {
            case 0: {
                ItemStack itemStack = this.randomVanillaFrom("mob-loot.iron-materials");
                break;
            }
            case 1: {
                ItemStack itemStack = this.randomCopperItem();
                break;
            }
            case 2: 
            case 3: {
                ItemStack itemStack = this.randomVanillaFrom("mob-loot.diamond-materials");
                break;
            }
            default: {
                ItemStack itemStack = base = null;
            }
        }
        if (base == null) {
            return null;
        }
        NBTItem nbt = NBTItem.get((ItemStack)base);
        nbt.addTag(new ItemTag[]{new ItemTag("NEMONICORB_TIER", (Object)tier)});
        nbt.addTag(new ItemTag[]{new ItemTag("NEMONICORB_IDENTIFIED", (Object)0)});
        nbt.addTag(new ItemTag[]{new ItemTag("NEMONICORB_CRAFT_LEVEL", (Object)itemLevel)});
        String category = this.resolveCategory(base, nbt);
        if (category != null && tier > 0) {
            int maxMods = this.plugin.getConfig().getInt("tiers." + tier + ".max-mods", tier == 1 ? 2 : 6);
            int modCount = tier == 1 ? ThreadLocalRandom.current().nextInt(1, 3) : ThreadLocalRandom.current().nextInt(3, 7);
            List<ModifierEngine.RolledModifier> mods = this.engine.rollForTier(category, modCount = Math.min(modCount, maxMods), itemLevel, maxMods, tier);
            if (!mods.isEmpty()) {
                nbt.addTag(new ItemTag[]{new ItemTag("NEMONICORB_MODS", (Object)this.engine.getGson().toJson(mods))});
            }
        }
        ItemStack result = nbt.toItem();
        if (category != null) {
            int maxEnchants = this.plugin.getConfig().getInt("enchant-on-craft.max", 2);
            this.orbListener.applyRandomEnchantments(result, maxEnchants);
            this.orbListener.applyRandomAttributes(result, category, itemLevel);
        }
        this.orbListener.updateItemDisplay(result, NBTItem.get((ItemStack)result));
        return result;
    }

    private int rollTier() {
        ConfigurationSection sec = this.plugin.getConfig().getConfigurationSection("mob-loot.tier-weights");
        if (sec == null) {
            return 0;
        }
        ArrayList<Map.Entry<Integer, Integer>> entries = new ArrayList<Map.Entry<Integer, Integer>>();
        int total = 0;
        for (String key : sec.getKeys(false)) {
            try {
                int tier = Integer.parseInt(key);
                int n = Math.max(0, sec.getInt(key, 0));
                if (n <= 0) continue;
                entries.add(Map.entry(tier, n));
                total += n;
            }
            catch (NumberFormatException numberFormatException) {}
        }
        if (total <= 0 || entries.isEmpty()) {
            return 0;
        }
        int roll = ThreadLocalRandom.current().nextInt(total);
        int acc = 0;
        for (Map.Entry entry : entries) {
            if (roll >= (acc += ((Integer)entry.getValue()).intValue())) continue;
            return (Integer)entry.getKey();
        }
        return 0;
    }

    private int rollItemLevel() {
        int base = this.plugin.getConfig().getInt("mob-loot.item-level-base", 20);
        int bonus = this.plugin.getConfig().getInt("mob-loot.item-level-random-bonus", 15);
        if (bonus <= 0) {
            return Math.max(1, base);
        }
        return Math.max(1, base + ThreadLocalRandom.current().nextInt(bonus + 1));
    }

    private ItemStack randomCopperItem() {
        List pool = this.plugin.getConfig().getStringList("mob-loot.copper-mmoitems");
        if (pool.isEmpty()) {
            return this.randomVanillaFrom("mob-loot.iron-materials");
        }
        List<String> valid = pool.stream().filter(s -> s != null && s.contains(":")).toList();
        if (valid.isEmpty()) {
            return this.randomVanillaFrom("mob-loot.iron-materials");
        }
        String pick = valid.get(ThreadLocalRandom.current().nextInt(valid.size()));
        String[] split = pick.split(":", 2);
        if (split.length != 2) {
            return this.randomVanillaFrom("mob-loot.iron-materials");
        }
        ItemStack item = this.createMMOItem(split[0].trim(), split[1].trim());
        if (item != null) {
            return item;
        }
        return this.randomVanillaFrom("mob-loot.iron-materials");
    }

    private ItemStack randomVanillaFrom(String path) {
        List names = this.plugin.getConfig().getStringList(path);
        ArrayList<Material> pool = new ArrayList<Material>();
        for (String name : names) {
            if (name == null || name.isBlank()) continue;
            try {
                Material mat = Material.valueOf((String)name.trim().toUpperCase(Locale.ROOT));
                if (mat.isAir()) continue;
                pool.add(mat);
            }
            catch (IllegalArgumentException illegalArgumentException) {}
        }
        if (pool.isEmpty()) {
            return null;
        }
        Material pick = (Material)pool.get(ThreadLocalRandom.current().nextInt(pool.size()));
        return new ItemStack(pick);
    }

    private String resolveCategory(ItemStack item, NBTItem nbt) {
        Material mat;
        String name;
        String mmoType;
        if (nbt.hasType() && (mmoType = nbt.getString("MMOITEMS_ITEM_TYPE")) != null) {
            String upper = mmoType.toUpperCase(Locale.ROOT);
            for (String cat : List.of("weapon", "armor", "accessory", "tool", "shield")) {
                List types = this.plugin.getConfig().getStringList("type-categories." + cat);
                if (!types.stream().anyMatch(t -> t.equalsIgnoreCase(upper))) continue;
                return cat;
            }
        }
        if ((name = (mat = item.getType()).name()).endsWith("_SWORD") || name.endsWith("_AXE") || name.equals("BOW") || name.equals("CROSSBOW") || name.equals("TRIDENT") || name.equals("MACE")) {
            return "weapon";
        }
        if (name.endsWith("_HELMET") || name.endsWith("_CHESTPLATE") || name.endsWith("_LEGGINGS") || name.endsWith("_BOOTS")) {
            return "armor";
        }
        if (name.equals("SHIELD")) {
            return "shield";
        }
        return null;
    }

    private ItemStack createMMOItem(String type, String id) {
        try {
            Type mmoType = Type.get((String)type.toUpperCase(Locale.ROOT));
            if (mmoType == null) {
                return null;
            }
            return MMOItems.plugin.getItem(mmoType, id);
        }
        catch (Exception e) {
            return null;
        }
    }
}

