package com.nemonicorp.orbs;

import io.lumine.mythic.lib.api.item.NBTItem;
import net.Indyuce.mmoitems.api.item.mmoitem.LiveMMOItem;
import net.Indyuce.mmoitems.stat.data.DoubleData;
import net.Indyuce.mmoitems.stat.type.DoubleStat;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.Set;

/**
 * Calcula DPH (Dano por Hit) de itens modificados pelo NemonicOrbPlugin.
 * Formula: DPH = baseDamage + soma de todos os danos dos modificadores
 * Inclui danos elementais (fire, ice, lightning, earth, water, wind).
 */
public class DpsCalculator {

    /** Stats de dano que contribuem para o DPH */
    private static final Set<String> DAMAGE_STATS = Set.of(
            "attack-damage", "physical-damage", "weapon-damage", "magic-damage",
            "fire-damage", "ice-damage", "lightning-damage",
            "earth-damage", "water-damage", "wind-damage"
    );

    /**
     * Calcula o DPH (Dano por Hit) de um item com base nos seus mods e stats base.
     * Retorna -1 se nao for possivel calcular (item nao e arma, tier < 1, etc.).
     */
    public static double calculate(ItemStack item, NBTItem nbt,
                                   List<ModifierEngine.RolledModifier> mods,
                                   int tier, boolean isMMOItem,
                                   ModifierEngine engine) {
        if (tier < 1) return -1;
        if (!isWeapon(item, nbt)) {
            if (NemonicOrbPlugin.getInstance().getConfig().getBoolean("debug-dph", false)) {
                NemonicOrbPlugin.getInstance().getLogger().info(
                        "[DEBUG-DPH] isWeapon=false material=" + item.getType().name()
                        + " hasType=" + nbt.hasType()
                        + " mmoType=" + nbt.getString("MMOITEMS_ITEM_TYPE"));
            }
            return -1;
        }

        double baseDamage = getBaseDamage(item, nbt, isMMOItem, engine);
        double modDamage = 0;

        for (ModifierEngine.RolledModifier mod : mods) {
            for (var entry : mod.stats().entrySet()) {
                String statKey = entry.getKey();
                double value = entry.getValue();
                if (DAMAGE_STATS.contains(statKey)) {
                    modDamage += value;
                }
            }
        }

        double totalDamage = baseDamage + modDamage;
        if (totalDamage < 0) totalDamage = 0;

        return Math.round(totalDamage * 10.0) / 10.0; // 1 casa decimal
    }

    /**
     * Obtem o dano base do item (vanilla attribute ou MMOItems stat).
     */
    private static double getBaseDamage(ItemStack item, NBTItem nbt,
                                        boolean isMMOItem, ModifierEngine engine) {
        if (isMMOItem) {
            try {
                LiveMMOItem live = new LiveMMOItem(item);
                DoubleStat attackDamageStat = engine.resolveStat("attack-damage");
                if (attackDamageStat != null) {
                    var data = live.getData(attackDamageStat);
                    if (data instanceof DoubleData dd) {
                        return dd.getValue();
                    }
                }
            } catch (Exception ignored) {}
        }

        // Fallback: vanilla base damage
        return getVanillaBaseDamage(item.getType());
    }

    /**
     * Retorna o dano base vanilla de um material.
     */
    private static double getVanillaBaseDamage(Material mat) {
        String name = mat.name();
        // Espadas
        if (name.contains("NETHERITE_SWORD")) return 8;
        if (name.contains("DIAMOND_SWORD")) return 7;
        if (name.contains("IRON_SWORD")) return 6;
        if (name.contains("STONE_SWORD")) return 5;
        if (name.contains("GOLDEN_SWORD")) return 4;
        if (name.contains("WOODEN_SWORD")) return 4;
        // Machados
        if (name.contains("NETHERITE_AXE")) return 10;
        if (name.contains("DIAMOND_AXE")) return 9;
        if (name.contains("IRON_AXE")) return 9;
        if (name.contains("STONE_AXE")) return 9;
        if (name.contains("GOLDEN_AXE")) return 7;
        if (name.contains("WOODEN_AXE")) return 7;
        // Tridentes
        if (name.equals("TRIDENT")) return 9;
        // Maces
        if (name.contains("MACE")) return 6;
        // Arcos/Bestas (dano medio)
        if (name.equals("BOW") || name.equals("CROSSBOW")) return 6;
        // Default para armas nao mapeadas
        return 1;
    }

    /**
     * Verifica se o item e uma arma (vanilla ou MMOItems).
     */
    private static boolean isWeapon(ItemStack item, NBTItem nbt) {
        // Verificar MMOItems type
        if (nbt.hasType()) {
            String type = nbt.getString("MMOITEMS_ITEM_TYPE");
            if (type != null) {
                return Set.of("SWORD", "DAGGER", "AXE", "BOW", "CROSSBOW", "STAFF",
                        "WAND", "WHIP", "MUSKET", "LUTE", "SPEAR", "GREATSTAFF",
                        "GREATSWORD", "HAMMER", "KATANA", "HALBERD", "GAUNTLET",
                        "TRIDENT", "MACE").contains(type);
            }
        }
        // Verificar vanilla
        Material mat = item.getType();
        String name = mat.name();
        return name.contains("SWORD") || name.contains("AXE") || name.contains("BOW")
                || name.contains("CROSSBOW") || name.contains("TRIDENT")
                || name.contains("MACE");
    }
}
