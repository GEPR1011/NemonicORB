package com.nemonicorp.orbs;

import com.google.gson.Gson;
import io.lumine.mythic.lib.api.item.NBTItem;
import net.Indyuce.mmoitems.api.item.mmoitem.LiveMMOItem;
import net.Indyuce.mmoitems.stat.data.DoubleData;
import net.Indyuce.mmoitems.stat.type.DoubleStat;
import org.bukkit.ChatColor;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;

public class ModifierAuditService {
    private final NemonicOrbPlugin plugin;
    private final Gson gson;

    private static final Map<String, String> STAT_TO_ATTRIBUTE = Map.ofEntries(
            Map.entry("attack-damage", "attack_damage"),
            Map.entry("attack-speed", "attack_speed"),
            Map.entry("armor", "armor"),
            Map.entry("armor-toughness", "armor_toughness"),
            Map.entry("max-health", "max_health"),
            Map.entry("max-absorption", "max_absorption"),
            Map.entry("knockback-resistance", "knockback_resistance"),
            Map.entry("luck", "luck")
    );

    private static final Map<String, org.bukkit.enchantments.Enchantment> ENCHANT_STATS = Map.of(
            "fortune", org.bukkit.enchantments.Enchantment.FORTUNE,
            "unbreaking", org.bukkit.enchantments.Enchantment.UNBREAKING,
            "knockback", org.bukkit.enchantments.Enchantment.KNOCKBACK
    );

    public ModifierAuditService(NemonicOrbPlugin plugin) {
        this.plugin = plugin;
        this.gson = plugin.getModifierEngine().getGson();
    }

    public void logBeforeAfter(String context, Player player, ItemStack before, ItemStack after) {
        if (!plugin.getConfig().getBoolean("audit.auto-log", true)) return;
        String beforeS = summarize(before);
        String afterS = summarize(after);
        plugin.getLogger().info("[AUDIT] " + context + " player=" + player.getName()
                + " before={" + beforeS + "} after={" + afterS + "}");
    }

    public List<String> auditPlayer(Player player, String mode) {
        List<String> out = new ArrayList<>();
        out.add("&6=== Auditoria de Modificadores ===");
        out.add("&7Jogador: &e" + player.getName());
        String m = mode == null ? "self" : mode.toLowerCase(Locale.ROOT);
        if (m.equals("hand") || m.equals("self")) {
            out.addAll(auditItem(player.getInventory().getItemInOffHand(), "Mao secundaria"));
        }
        if (m.equals("equip") || m.equals("self")) {
            int idx = 1;
            for (ItemStack piece : player.getInventory().getArmorContents()) {
                out.addAll(auditItem(piece, "Armadura " + idx));
                idx++;
            }
        }
        return out;
    }

    public List<String> auditItem(ItemStack item, String label) {
        List<String> out = new ArrayList<>();
        if (item == null || item.getType().isAir()) {
            out.add("&8[" + label + "] vazio");
            return out;
        }

        NBTItem nbt = NBTItem.get(item);
        int tier = nbt.hasTag(OrbListener.NBT_TIER) ? nbt.getInteger(OrbListener.NBT_TIER) : -1;
        List<ModifierEngine.RolledModifier> mods = readMods(nbt);
        out.add("&7[" + label + "] &e" + item.getType() + "&7 tier=&e" + tier + "&7 mods=&e" + mods.size());

        Map<String, Double> expectedStats = new LinkedHashMap<>();
        for (var mod : mods) {
            for (var e : mod.stats().entrySet()) {
                expectedStats.merge(e.getKey(), e.getValue(), Double::sum);
            }
        }

        int attrExpected = 0;
        int attrFound = 0;
        ItemMeta meta = item.getItemMeta();
        if (meta != null && meta.hasAttributeModifiers()) {
            for (var e : expectedStats.entrySet()) {
                String attrKey = STAT_TO_ATTRIBUTE.get(e.getKey());
                if (attrKey == null) continue;
                attrExpected++;
                if (hasAttribute(meta, attrKey)) attrFound++;
            }
        }
        out.add("&7Atributos esperados/aplicados: &e" + attrFound + "/" + attrExpected);

        for (var e : ENCHANT_STATS.entrySet()) {
            double expectedTier = expectedStats.getOrDefault(e.getKey(), 0.0);
            if (expectedTier <= 0) continue;
            int actual = item.getEnchantmentLevel(e.getValue());
            int expected = Math.max(1, (int) Math.round(expectedTier));
            if (actual < expected) {
                out.add("&cMismatch encantamento " + e.getKey() + ": esperado>=" + expected + " real=" + actual);
            } else {
                out.add("&aEncantamento OK " + e.getKey() + ": " + actual);
            }
        }

        if (nbt.hasType()) {
            try {
                LiveMMOItem live = new LiveMMOItem(item);
                int resolved = 0;
                int unresolved = 0;
                for (var e : expectedStats.entrySet()) {
                    DoubleStat st = plugin.getModifierEngine().resolveStat(e.getKey());
                    if (st == null) {
                        unresolved++;
                        continue;
                    }
                    var data = live.getData(st);
                    if (data instanceof DoubleData) resolved++;
                }
                out.add("&7MMO stats resolvidas: &e" + resolved + "&7 | nao resolvidas: &c" + unresolved);
            } catch (Exception ex) {
                out.add("&cFalha na auditoria MMOItem: " + ex.getMessage());
            }
        }

        if (expectedStats.isEmpty()) {
            out.add("&8Sem NBT_MODS neste item.");
        }
        return out;
    }

    private boolean hasAttribute(ItemMeta meta, String attrKey) {
        Attribute attr = org.bukkit.Registry.ATTRIBUTE.get(NamespacedKey.minecraft(attrKey));
        if (attr == null) attr = org.bukkit.Registry.ATTRIBUTE.get(NamespacedKey.minecraft("generic." + attrKey));
        if (attr == null) return false;
        Collection<AttributeModifier> mods = meta.getAttributeModifiers(attr);
        if (mods == null || mods.isEmpty()) return false;
        for (AttributeModifier mod : mods) {
            String key = mod.getKey().getKey();
            if (key.startsWith("orbmod_") || key.startsWith("suffix_")) return true;
        }
        return false;
    }

    private List<ModifierEngine.RolledModifier> readMods(NBTItem nbt) {
        if (!nbt.hasTag(OrbListener.NBT_MODS)) return Collections.emptyList();
        String json = nbt.getString(OrbListener.NBT_MODS);
        if (json == null || json.isEmpty() || json.equals("[]")) return Collections.emptyList();
        try {
            List<ModifierEngine.RolledModifier> list = gson.fromJson(json, ModifierEngine.ROLLED_LIST_TYPE);
            return list != null ? list : Collections.emptyList();
        } catch (Exception ignored) {
            return Collections.emptyList();
        }
    }

    private String summarize(ItemStack item) {
        if (item == null || item.getType().isAir()) return "empty";
        NBTItem nbt = NBTItem.get(item);
        int tier = nbt.hasTag(OrbListener.NBT_TIER) ? nbt.getInteger(OrbListener.NBT_TIER) : -1;
        int mods = readMods(nbt).size();
        return item.getType() + ",tier=" + tier + ",mods=" + mods + ",ench=" + item.getEnchantments().size();
    }

    public String cc(String msg) {
        return ChatColor.translateAlternateColorCodes('&', msg);
    }
}

