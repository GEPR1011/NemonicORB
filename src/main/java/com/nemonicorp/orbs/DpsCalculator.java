/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  io.lumine.mythic.lib.api.item.NBTItem
 *  net.Indyuce.mmoitems.api.item.mmoitem.LiveMMOItem
 *  net.Indyuce.mmoitems.stat.data.DoubleData
 *  net.Indyuce.mmoitems.stat.data.type.StatData
 *  net.Indyuce.mmoitems.stat.type.DoubleStat
 *  net.Indyuce.mmoitems.stat.type.ItemStat
 *  org.bukkit.Material
 *  org.bukkit.inventory.ItemStack
 */
package com.nemonicorp.orbs;

import com.nemonicorp.orbs.ModifierEngine;
import com.nemonicorp.orbs.NemonicOrbPlugin;
import io.lumine.mythic.lib.api.item.NBTItem;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.Indyuce.mmoitems.api.item.mmoitem.LiveMMOItem;
import net.Indyuce.mmoitems.stat.data.DoubleData;
import net.Indyuce.mmoitems.stat.data.type.StatData;
import net.Indyuce.mmoitems.stat.type.DoubleStat;
import net.Indyuce.mmoitems.stat.type.ItemStat;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

public class DpsCalculator {
    private static final Set<String> DAMAGE_STATS = Set.of("attack-damage", "physical-damage", "weapon-damage", "magic-damage", "fire-damage", "ice-damage", "lightning-damage", "earth-damage", "water-damage", "wind-damage");

    public static double calculate(ItemStack item, NBTItem nbt, List<ModifierEngine.RolledModifier> mods, int tier, boolean isMMOItem, ModifierEngine engine) {
        if (tier < 1) {
            return -1.0;
        }
        if (!DpsCalculator.isWeapon(item, nbt)) {
            NemonicOrbPlugin.getInstance().getLogger().info("[DEBUG-DPH] isWeapon=false material=" + item.getType().name() + " hasType=" + nbt.hasType() + " mmoType=" + nbt.getString("MMOITEMS_ITEM_TYPE"));
            return -1.0;
        }
        double baseDamage = DpsCalculator.getBaseDamage(item, nbt, isMMOItem, engine);
        double modDamage = 0.0;
        for (ModifierEngine.RolledModifier mod : mods) {
            for (Map.Entry<String, Double> entry : mod.stats().entrySet()) {
                String statKey = entry.getKey();
                double value = entry.getValue();
                if (!DAMAGE_STATS.contains(statKey)) continue;
                modDamage += value;
            }
        }
        double totalDamage = baseDamage + modDamage;
        if (totalDamage < 0.0) {
            totalDamage = 0.0;
        }
        return (double)Math.round(totalDamage * 10.0) / 10.0;
    }

    private static double getBaseDamage(ItemStack item, NBTItem nbt, boolean isMMOItem, ModifierEngine engine) {
        if (isMMOItem) {
            try {
                StatData data;
                LiveMMOItem live = new LiveMMOItem(item);
                DoubleStat attackDamageStat = engine.resolveStat("attack-damage");
                if (attackDamageStat != null && (data = live.getData((ItemStat)attackDamageStat)) instanceof DoubleData) {
                    DoubleData dd = (DoubleData)data;
                    return dd.getValue();
                }
            }
            catch (Exception exception) {
                // empty catch block
            }
        }
        return DpsCalculator.getVanillaBaseDamage(item.getType());
    }

    private static double getVanillaBaseDamage(Material mat) {
        String name = mat.name();
        if (name.contains("NETHERITE_SWORD")) {
            return 8.0;
        }
        if (name.contains("DIAMOND_SWORD")) {
            return 7.0;
        }
        if (name.contains("IRON_SWORD")) {
            return 6.0;
        }
        if (name.contains("STONE_SWORD")) {
            return 5.0;
        }
        if (name.contains("GOLDEN_SWORD")) {
            return 4.0;
        }
        if (name.contains("WOODEN_SWORD")) {
            return 4.0;
        }
        if (name.contains("NETHERITE_AXE")) {
            return 10.0;
        }
        if (name.contains("DIAMOND_AXE")) {
            return 9.0;
        }
        if (name.contains("IRON_AXE")) {
            return 9.0;
        }
        if (name.contains("STONE_AXE")) {
            return 9.0;
        }
        if (name.contains("GOLDEN_AXE")) {
            return 7.0;
        }
        if (name.contains("WOODEN_AXE")) {
            return 7.0;
        }
        if (name.equals("TRIDENT")) {
            return 9.0;
        }
        if (name.contains("MACE")) {
            return 6.0;
        }
        if (name.equals("BOW") || name.equals("CROSSBOW")) {
            return 6.0;
        }
        return 1.0;
    }

    private static boolean isWeapon(ItemStack item, NBTItem nbt) {
        String type;
        if (nbt.hasType() && (type = nbt.getString("MMOITEMS_ITEM_TYPE")) != null) {
            return Set.of("SWORD", "DAGGER", "AXE", "BOW", "CROSSBOW", "STAFF", "WAND", "WHIP", "MUSKET", "LUTE", "SPEAR", "GREATSTAFF", "GREATSWORD", "HAMMER", "KATANA", "HALBERD", "GAUNTLET", "TRIDENT", "MACE").contains(type);
        }
        Material mat = item.getType();
        String name = mat.name();
        return name.contains("SWORD") || name.contains("AXE") || name.contains("BOW") || name.contains("CROSSBOW") || name.contains("TRIDENT") || name.contains("MACE");
    }
}

