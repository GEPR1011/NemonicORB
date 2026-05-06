package com.nemonicorp.orbs;

import io.lumine.mythic.lib.api.item.ItemTag;
import io.lumine.mythic.lib.api.item.NBTItem;
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

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Drop global de equipamentos de mob para o sistema NemonicOrb:
 * - Chance de equipamento configuravel (padrao 5%)
 * - Chance de pergaminho configuravel (padrao 9%)
 * - Equipamentos sempre caem como NAO IDENTIFICADOS
 */
public class MobLootDropListener implements Listener {

    private final NemonicOrbPlugin plugin;
    private final OrbListener orbListener;
    private final ModifierEngine engine;
    private MerchantTableListener merchantListener;

    public MobLootDropListener(NemonicOrbPlugin plugin, OrbListener orbListener) {
        this.plugin = plugin;
        this.orbListener = orbListener;
        this.engine = plugin.getModifierEngine();
    }

    /** Setter chamado pelo NemonicOrbPlugin apos instanciar MerchantTableListener. */
    public void setMerchantListener(MerchantTableListener listener) {
        this.merchantListener = listener;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onMobDeath(EntityDeathEvent event) {
        if (!plugin.getConfig().getBoolean("mob-loot.enabled", true)) return;
        if (!(event.getEntity() instanceof Monster)) return;

        boolean requirePlayerKill = plugin.getConfig().getBoolean("mob-loot.require-player-kill", true);
        Player killer = event.getEntity().getKiller();
        if (requirePlayerKill && killer == null) return;

        ThreadLocalRandom rng = ThreadLocalRandom.current();

        // Drop de Pergaminho de Identificacao — exclusivo do Mercador.
        // Outras classes recebem chance reduzida via 'pergaminho-non-mercador-multiplier' (default 0.03 = -97%).
        // Mercadores recebem bonus por nivel via 'mercador-pergaminho-level-bonus' (default 0.02 = +2% por nivel).
        double pergaminhoChance = plugin.getConfig().getDouble("mob-loot.pergaminho-drop-chance", 0.09);
        boolean isMercador = merchantListener != null && killer != null && merchantListener.isMercador(killer);
        if (isMercador) {
            int mercadorLevel = merchantListener.getPlayerLevel(killer);
            double bonus = plugin.getConfig().getDouble("mob-loot.mercador-pergaminho-level-bonus", 0.02);
            pergaminhoChance *= (1.0 + mercadorLevel * bonus);
        } else {
            double mult = plugin.getConfig().getDouble("mob-loot.pergaminho-non-mercador-multiplier", 0.03);
            pergaminhoChance *= mult;
        }
        if (rng.nextDouble() < pergaminhoChance) {
            ItemStack pergaminho = createMMOItem("CONSUMABLE", "PERGAMINHO_DE_IDENTIFICACAO");
            if (pergaminho != null) {
                event.getDrops().add(pergaminho);
            }
        }

        double equipmentChance = plugin.getConfig().getDouble("mob-loot.equipment-drop-chance", 0.05);
        if (rng.nextDouble() >= equipmentChance) return;

        ItemStack dropped = createUnidentifiedEquipment();
        if (dropped == null) return;

        event.getDrops().add(dropped);
    }

    private ItemStack createUnidentifiedEquipment() {
        int tier = rollTier();
        int itemLevel = rollItemLevel();

        ItemStack base = switch (tier) {
            case 0 -> randomVanillaFrom("mob-loot.iron-materials");
            case 1 -> randomCopperItem();
            case 2, 3 -> randomVanillaFrom("mob-loot.diamond-materials");
            default -> null;
        };
        if (base == null) return null;

        NBTItem nbt = NBTItem.get(base);
        nbt.addTag(new ItemTag(OrbListener.NBT_TIER, tier));
        nbt.addTag(new ItemTag(OrbListener.NBT_IDENTIFIED, 0));
        nbt.addTag(new ItemTag(OrbListener.NBT_CRAFT_LEVEL, itemLevel));

        String category = resolveCategory(base, nbt);
        if (category != null && tier > 0) {
            int maxMods = plugin.getConfig().getInt("tiers." + tier + ".max-mods", tier == 1 ? 2 : 6);
            int modCount = tier == 1
                    ? ThreadLocalRandom.current().nextInt(1, 3)
                    : ThreadLocalRandom.current().nextInt(3, 7);
            modCount = Math.min(modCount, maxMods);

            List<ModifierEngine.RolledModifier> mods = engine.rollForTier(category, modCount, itemLevel, maxMods, tier);
            if (!mods.isEmpty()) {
                nbt.addTag(new ItemTag(OrbListener.NBT_MODS, engine.getGson().toJson(mods)));
            }
        }

        ItemStack result = nbt.toItem();

        // Aplicar prefixos (encantamentos) e sufixos (atributos) como no craft
        if (category != null) {
            int maxEnchants = plugin.getConfig().getInt("enchant-on-craft.max", 2);
            orbListener.applyRandomEnchantments(result, maxEnchants);
            orbListener.applyRandomAttributes(result, category, itemLevel);
        }

        orbListener.updateItemDisplay(result, NBTItem.get(result));
        return result;
    }

    private int rollTier() {
        ConfigurationSection sec = plugin.getConfig().getConfigurationSection("mob-loot.tier-weights");
        if (sec == null) return 0;

        List<Map.Entry<Integer, Integer>> entries = new ArrayList<>();
        int total = 0;
        for (String key : sec.getKeys(false)) {
            try {
                int tier = Integer.parseInt(key);
                int weight = Math.max(0, sec.getInt(key, 0));
                if (weight <= 0) continue;
                entries.add(Map.entry(tier, weight));
                total += weight;
            } catch (NumberFormatException ignored) {
            }
        }
        if (total <= 0 || entries.isEmpty()) return 0;

        int roll = ThreadLocalRandom.current().nextInt(total);
        int acc = 0;
        for (Map.Entry<Integer, Integer> e : entries) {
            acc += e.getValue();
            if (roll < acc) return e.getKey();
        }
        return 0;
    }

    private int rollItemLevel() {
        int base = plugin.getConfig().getInt("mob-loot.item-level-base", 20);
        int bonus = plugin.getConfig().getInt("mob-loot.item-level-random-bonus", 15);
        if (bonus <= 0) return Math.max(1, base);
        return Math.max(1, base + ThreadLocalRandom.current().nextInt(bonus + 1));
    }

    private ItemStack randomCopperItem() {
        List<String> pool = plugin.getConfig().getStringList("mob-loot.copper-mmoitems");
        if (pool.isEmpty()) {
            return randomVanillaFrom("mob-loot.iron-materials");
        }
        List<String> valid = pool.stream().filter(s -> s != null && s.contains(":")).toList();
        if (valid.isEmpty()) return randomVanillaFrom("mob-loot.iron-materials");

        String pick = valid.get(ThreadLocalRandom.current().nextInt(valid.size()));
        String[] split = pick.split(":", 2);
        if (split.length != 2) return randomVanillaFrom("mob-loot.iron-materials");

        ItemStack item = createMMOItem(split[0].trim(), split[1].trim());
        if (item != null) return item;
        return randomVanillaFrom("mob-loot.iron-materials");
    }

    private ItemStack randomVanillaFrom(String path) {
        List<String> names = plugin.getConfig().getStringList(path);
        List<Material> pool = new ArrayList<>();
        for (String name : names) {
            if (name == null || name.isBlank()) continue;
            try {
                Material mat = Material.valueOf(name.trim().toUpperCase(Locale.ROOT));
                if (!mat.isAir()) pool.add(mat);
            } catch (IllegalArgumentException ignored) {
            }
        }
        if (pool.isEmpty()) return null;

        Material pick = pool.get(ThreadLocalRandom.current().nextInt(pool.size()));
        return new ItemStack(pick);
    }

    private String resolveCategory(ItemStack item, NBTItem nbt) {
        if (nbt.hasType()) {
            String mmoType = nbt.getString("MMOITEMS_ITEM_TYPE");
            if (mmoType != null) {
                String upper = mmoType.toUpperCase(Locale.ROOT);
                for (String cat : List.of("weapon", "armor", "accessory", "tool", "shield")) {
                    List<String> types = plugin.getConfig().getStringList("type-categories." + cat);
                    if (types.stream().anyMatch(t -> t.equalsIgnoreCase(upper))) {
                        return cat;
                    }
                }
            }
        }

        Material mat = item.getType();
        String name = mat.name();
        if (name.endsWith("_SWORD") || name.endsWith("_AXE") || name.equals("BOW")
                || name.equals("CROSSBOW") || name.equals("TRIDENT") || name.equals("MACE")) {
            return "weapon";
        }
        if (name.endsWith("_HELMET") || name.endsWith("_CHESTPLATE")
                || name.endsWith("_LEGGINGS") || name.endsWith("_BOOTS")) {
            return "armor";
        }
        if (name.equals("SHIELD")) {
            return "shield";
        }
        return null;
    }

    private ItemStack createMMOItem(String type, String id) {
        try {
            Type mmoType = Type.get(type.toUpperCase(Locale.ROOT));
            if (mmoType == null) return null;
            return MMOItems.plugin.getItem(mmoType, id);
        } catch (Exception e) {
            return null;
        }
    }
}

