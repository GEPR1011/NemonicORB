/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.gson.Gson
 *  io.lumine.mythic.lib.api.item.NBTItem
 *  net.Indyuce.mmoitems.api.item.mmoitem.LiveMMOItem
 *  net.Indyuce.mmoitems.stat.data.DoubleData
 *  net.Indyuce.mmoitems.stat.data.type.StatData
 *  net.Indyuce.mmoitems.stat.type.DoubleStat
 *  net.Indyuce.mmoitems.stat.type.ItemStat
 *  org.bukkit.ChatColor
 *  org.bukkit.NamespacedKey
 *  org.bukkit.Registry
 *  org.bukkit.attribute.Attribute
 *  org.bukkit.attribute.AttributeModifier
 *  org.bukkit.enchantments.Enchantment
 *  org.bukkit.entity.Player
 *  org.bukkit.inventory.ItemStack
 *  org.bukkit.inventory.meta.ItemMeta
 */
package com.nemonicorp.orbs;

import com.google.gson.Gson;
import com.nemonicorp.orbs.ModifierEngine;
import com.nemonicorp.orbs.NemonicOrbPlugin;
import io.lumine.mythic.lib.api.item.NBTItem;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import net.Indyuce.mmoitems.api.item.mmoitem.LiveMMOItem;
import net.Indyuce.mmoitems.stat.data.DoubleData;
import net.Indyuce.mmoitems.stat.data.type.StatData;
import net.Indyuce.mmoitems.stat.type.DoubleStat;
import net.Indyuce.mmoitems.stat.type.ItemStat;
import org.bukkit.ChatColor;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class ModifierAuditService {
    private final NemonicOrbPlugin plugin;
    private final Gson gson;
    private static final Map<String, String> STAT_TO_ATTRIBUTE = Map.ofEntries(Map.entry("attack-damage", "attack_damage"), Map.entry("attack-speed", "attack_speed"), Map.entry("armor", "armor"), Map.entry("armor-toughness", "armor_toughness"), Map.entry("max-health", "max_health"), Map.entry("max-absorption", "max_absorption"), Map.entry("knockback-resistance", "knockback_resistance"), Map.entry("luck", "luck"));
    private static final Map<String, Enchantment> ENCHANT_STATS = Map.of("fortune", Enchantment.FORTUNE, "unbreaking", Enchantment.UNBREAKING, "knockback", Enchantment.KNOCKBACK);

    public ModifierAuditService(NemonicOrbPlugin plugin) {
        this.plugin = plugin;
        this.gson = plugin.getModifierEngine().getGson();
    }

    public void logBeforeAfter(String context, Player player, ItemStack before, ItemStack after) {
        if (!this.plugin.getConfig().getBoolean("audit.auto-log", true)) {
            return;
        }
        String beforeS = this.summarize(before);
        String afterS = this.summarize(after);
        this.plugin.getLogger().info("[AUDIT] " + context + " player=" + player.getName() + " before={" + beforeS + "} after={" + afterS + "}");
    }

    public List<String> auditPlayer(Player player, String mode) {
        String m;
        ArrayList<String> out = new ArrayList<String>();
        out.add("&6=== Auditoria de Modificadores ===");
        out.add("&7Jogador: &e" + player.getName());
        String string = m = mode == null ? "self" : mode.toLowerCase(Locale.ROOT);
        if (m.equals("hand") || m.equals("self")) {
            out.addAll(this.auditItem(player.getInventory().getItemInOffHand(), "Mao secundaria"));
        }
        if (m.equals("equip") || m.equals("self")) {
            int idx = 1;
            for (ItemStack piece : player.getInventory().getArmorContents()) {
                out.addAll(this.auditItem(piece, "Armadura " + idx));
                ++idx;
            }
        }
        return out;
    }

    /*
     * WARNING - void declaration
     */
    public List<String> auditItem(ItemStack item, String label) {
        ArrayList<String> out = new ArrayList<String>();
        if (item == null || item.getType().isAir()) {
            out.add("&8[" + label + "] vazio");
            return out;
        }
        NBTItem nbt = NBTItem.get((ItemStack)item);
        int tier = nbt.hasTag("NEMONICORB_TIER") ? nbt.getInteger("NEMONICORB_TIER") : -1;
        List<ModifierEngine.RolledModifier> mods = this.readMods(nbt);
        out.add("&7[" + label + "] &e" + String.valueOf(item.getType()) + "&7 tier=&e" + tier + "&7 mods=&e" + mods.size());
        LinkedHashMap<String, Double> expectedStats = new LinkedHashMap<String, Double>();
        for (ModifierEngine.RolledModifier mod : mods) {
            for (Map.Entry<String, Double> entry : mod.stats().entrySet()) {
                expectedStats.merge(entry.getKey(), entry.getValue(), Double::sum);
            }
        }
        int attrExpected = 0;
        int attrFound = 0;
        ItemMeta meta = item.getItemMeta();
        if (meta != null && meta.hasAttributeModifiers()) {
            for (Map.Entry entry : expectedStats.entrySet()) {
                String attrKey = STAT_TO_ATTRIBUTE.get(entry.getKey());
                if (attrKey == null) continue;
                ++attrExpected;
                if (!this.hasAttribute(meta, attrKey)) continue;
                ++attrFound;
            }
        }
        out.add("&7Atributos esperados/aplicados: &e" + attrFound + "/" + attrExpected);
        for (Map.Entry<String, Enchantment> entry : ENCHANT_STATS.entrySet()) {
            int expected;
            double expectedTier = expectedStats.getOrDefault(entry.getKey(), 0.0);
            if (expectedTier <= 0.0) continue;
            int actual = item.getEnchantmentLevel(entry.getValue());
            if (actual < (expected = Math.max(1, (int)Math.round(expectedTier)))) {
                out.add("&cMismatch encantamento " + entry.getKey() + ": esperado>=" + expected + " real=" + actual);
                continue;
            }
            out.add("&aEncantamento OK " + entry.getKey() + ": " + actual);
        }
        if (nbt.hasType()) {
            try {
                void var12_23;
                LiveMMOItem liveMMOItem = new LiveMMOItem(item);
                boolean bl = false;
                int unresolved = 0;
                for (Map.Entry e : expectedStats.entrySet()) {
                    DoubleStat st = this.plugin.getModifierEngine().resolveStat((String)e.getKey());
                    if (st == null) {
                        ++unresolved;
                        continue;
                    }
                    StatData data = liveMMOItem.getData((ItemStat)st);
                    if (!(data instanceof DoubleData)) continue;
                    ++var12_23;
                }
                out.add("&7MMO stats resolvidas: &e" + (int)var12_23 + "&7 | nao resolvidas: &c" + unresolved);
            }
            catch (Exception exception) {
                out.add("&cFalha na auditoria MMOItem: " + exception.getMessage());
            }
        }
        if (expectedStats.isEmpty()) {
            out.add("&8Sem NBT_MODS neste item.");
        }
        return out;
    }

    private boolean hasAttribute(ItemMeta meta, String attrKey) {
        Attribute attr = (Attribute)Registry.ATTRIBUTE.get(NamespacedKey.minecraft((String)attrKey));
        if (attr == null) {
            attr = (Attribute)Registry.ATTRIBUTE.get(NamespacedKey.minecraft((String)("generic." + attrKey)));
        }
        if (attr == null) {
            return false;
        }
        Collection mods = meta.getAttributeModifiers(attr);
        if (mods == null || mods.isEmpty()) {
            return false;
        }
        for (AttributeModifier mod : mods) {
            String key = mod.getKey().getKey();
            if (!key.startsWith("orbmod_") && !key.startsWith("suffix_")) continue;
            return true;
        }
        return false;
    }

    private List<ModifierEngine.RolledModifier> readMods(NBTItem nbt) {
        if (!nbt.hasTag("NEMONICORB_MODS")) {
            return Collections.emptyList();
        }
        String json = nbt.getString("NEMONICORB_MODS");
        if (json == null || json.isEmpty() || json.equals("[]")) {
            return Collections.emptyList();
        }
        try {
            List<ModifierEngine.RolledModifier> list = (List<ModifierEngine.RolledModifier>)this.gson.fromJson(json, ModifierEngine.ROLLED_LIST_TYPE);
            return list != null ? list : Collections.emptyList();
        }
        catch (Exception ignored) {
            return Collections.emptyList();
        }
    }

    private String summarize(ItemStack item) {
        if (item == null || item.getType().isAir()) {
            return "empty";
        }
        NBTItem nbt = NBTItem.get((ItemStack)item);
        int tier = nbt.hasTag("NEMONICORB_TIER") ? nbt.getInteger("NEMONICORB_TIER") : -1;
        int mods = this.readMods(nbt).size();
        return String.valueOf(item.getType()) + ",tier=" + tier + ",mods=" + mods + ",ench=" + item.getEnchantments().size();
    }

    public String cc(String msg) {
        return ChatColor.translateAlternateColorCodes((char)'&', (String)msg);
    }
}

