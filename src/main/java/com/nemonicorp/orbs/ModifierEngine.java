/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.gson.Gson
 *  com.google.gson.reflect.TypeToken
 *  net.Indyuce.mmoitems.MMOItems
 *  net.Indyuce.mmoitems.stat.type.DoubleStat
 *  net.Indyuce.mmoitems.stat.type.ItemStat
 *  org.bukkit.configuration.ConfigurationSection
 *  org.bukkit.configuration.file.YamlConfiguration
 */
package com.nemonicorp.orbs;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.nemonicorp.orbs.NemonicOrbPlugin;
import java.io.File;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.logging.Logger;
import net.Indyuce.mmoitems.MMOItems;
import net.Indyuce.mmoitems.stat.type.DoubleStat;
import net.Indyuce.mmoitems.stat.type.ItemStat;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

public class ModifierEngine {
    public static final Type ROLLED_LIST_TYPE = new TypeToken<List<RolledModifier>>(){}.getType();
    private final NemonicOrbPlugin plugin;
    private final Logger log;
    private final Gson gson = new Gson();
    private final Map<String, GroupDef> groups = new HashMap<String, GroupDef>();
    private final Map<String, IndividualMod> individuals = new HashMap<String, IndividualMod>();
    private final Map<String, DoubleStat> statCache = new HashMap<String, DoubleStat>();
    private static final Set<String> BLACKLISTED_STATS = Set.of("magic-damage", "skill-damage", "vanilla-exp-gain", "skill-exp-gain", "additional-experience");
    private static final Map<String, Integer> ENCHANT_TIER_MAX = Map.of("fortune", 3, "unbreaking", 3, "knockback", 2);

    public ModifierEngine(NemonicOrbPlugin plugin) {
        this.plugin = plugin;
        this.log = plugin.getLogger();
    }

    private boolean debug() {
        return this.plugin.getConfig().getBoolean("debug", true);
    }

    public void load() {
        this.groups.clear();
        this.individuals.clear();
        this.statCache.clear();
        File modFile = new File(this.plugin.getDataFolder(), "nemonicorp_orb_modifiers.yml");
        if (!modFile.exists()) {
            File mmoitemsDir = new File(this.plugin.getDataFolder().getParentFile(), "MMOItems");
            modFile = new File(mmoitemsDir, "modifiers/nemonicorp_orb_modifiers.yml");
        }
        if (!modFile.exists()) {
            this.log.severe("ERRO: Arquivo de modificadores NAO encontrado!");
            this.log.severe("  Verificado: " + new File(this.plugin.getDataFolder(), "nemonicorp_orb_modifiers.yml").getPath());
            this.log.severe("  Verificado: " + new File(new File(this.plugin.getDataFolder().getParentFile(), "MMOItems"), "modifiers/nemonicorp_orb_modifiers.yml").getPath());
            return;
        }
        this.log.info("Carregando modificadores de: " + modFile.getPath());
        this.log.info("Arquivo existe: " + modFile.exists() + " tamanho: " + modFile.length() + " bytes");
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration((File)modFile);
        Set topKeys = yaml.getKeys(false);
        this.log.info("YAML top-level keys: " + topKeys.size() + " -> " + String.valueOf(topKeys));
        for (String key : topKeys) {
            ConfigurationSection section = yaml.getConfigurationSection(key);
            if (section == null) continue;
            if (section.contains("modifiers")) {
                this.parseGroup(key, section);
            }
            if (!section.contains("stats")) continue;
            this.parseIndividual(key, section);
        }
        this.pruneInvalidGroupChildren();
        this.cacheStats();
        this.log.info("Modificadores v2 carregados: " + this.groups.size() + " grupos, " + this.individuals.size() + " individuais, " + this.statCache.size() + " stats cacheados.");
        if (this.groups.isEmpty() || this.individuals.isEmpty()) {
            this.log.severe("====================================================");
            this.log.severe("ERRO CRITICO: Pool de modificadores VAZIA!");
            this.log.severe("  Grupos: " + this.groups.size() + " | Individuais: " + this.individuals.size());
            this.log.severe("  Arquivo: " + modFile.getPath());
            this.log.severe("  Tamanho: " + modFile.length() + " bytes");
            this.log.severe("  Orbs NAO vao funcionar ate resolver isso!");
            this.log.severe("====================================================");
        }
        if (this.debug()) {
            this.log.info("[DEBUG-ENGINE] Grupos: " + String.valueOf(this.groups.keySet()));
            this.log.info("[DEBUG-ENGINE] Individuais: " + String.valueOf(this.individuals.keySet()));
            this.log.info("[DEBUG-ENGINE] Stats cacheados: " + String.valueOf(this.statCache.keySet()));
        }
    }

    private void parseGroup(String id, ConfigurationSection section) {
        int min = section.getInt("min", 0);
        int max = section.getInt("max", 1);
        LinkedHashMap<String, Integer> children = new LinkedHashMap<String, Integer>();
        ConfigurationSection modsSec = section.getConfigurationSection("modifiers");
        if (modsSec != null) {
            for (String childId : modsSec.getKeys(false)) {
                children.put(childId, modsSec.getInt(childId, 1));
            }
        }
        this.groups.put(id, new GroupDef(id, min, max, children));
        if (this.debug()) {
            this.log.info("[DEBUG-ENGINE] Grupo parseado: " + id + " -> filhos: " + String.valueOf(children.keySet()));
        }
    }

    private void parseIndividual(String id, ConfigurationSection section) {
        LinkedHashMap<String, StatValues> stats = new LinkedHashMap<String, StatValues>();
        ConfigurationSection statsSec = section.getConfigurationSection("stats");
        if (statsSec != null) {
            for (String statId : statsSec.getKeys(false)) {
                if (statId.equals("perm-effects") || this.isBlacklistedStat(statId)) continue;
                Object val = statsSec.get(statId);
                if (val instanceof ConfigurationSection) {
                    ConfigurationSection statSec = (ConfigurationSection)val;
                    stats.put(statId, new StatValues(statSec.getDouble("base", 0.0), statSec.getDouble("scale", 0.0), statSec.getDouble("spread", 0.0), statSec.getDouble("max-spread", 0.0)));
                    continue;
                }
                if (!(val instanceof Number)) continue;
                Number num = (Number)val;
                stats.put(statId, new StatValues(num.doubleValue(), 0.0, 0.0, 0.0));
            }
        }
        if (stats.isEmpty()) {
            if (this.debug()) {
                this.log.info("[DEBUG-ENGINE] Individual ignorado (apenas stats bloqueados): " + id);
            }
            return;
        }
        this.individuals.put(id, new IndividualMod(id, stats));
        if (this.debug()) {
            this.log.info("[DEBUG-ENGINE] Individual parseado: " + id + " stats=" + String.valueOf(stats.keySet()));
        }
    }

    private boolean isBlacklistedStat(String statId) {
        return statId != null && BLACKLISTED_STATS.contains(statId.toLowerCase(Locale.ROOT));
    }

    private void pruneInvalidGroupChildren() {
        for (Map.Entry<String, GroupDef> entry : new ArrayList<Map.Entry<String, GroupDef>>(this.groups.entrySet())) {
            GroupDef def = entry.getValue();
            LinkedHashMap<String, Integer> filtered = new LinkedHashMap<String, Integer>();
            for (Map.Entry<String, Integer> child : def.children().entrySet()) {
                String childId = child.getKey();
                if (!this.groups.containsKey(childId) && !this.individuals.containsKey(childId)) continue;
                filtered.put(childId, child.getValue());
            }
            this.groups.put(entry.getKey(), new GroupDef(def.id(), def.min(), def.max(), filtered));
        }
    }

    private void cacheStats() {
        try {
            for (ItemStat stat : MMOItems.plugin.getStats().getAll()) {
                if (!(stat instanceof DoubleStat)) continue;
                DoubleStat ds = (DoubleStat)stat;
                String id = stat.getId().toUpperCase();
                this.statCache.put(id, ds);
                String hyphenated = id.replace("_", "-");
                if (!hyphenated.equals(id)) {
                    this.statCache.put(hyphenated, ds);
                }
                this.statCache.put(this.canonicalKey(id), ds);
            }
        }
        catch (Exception e) {
            this.log.warning("Erro ao cachear stats do MMOItems: " + e.getMessage());
        }
        if (this.debug()) {
            this.log.info("[DEBUG-ENGINE] Stats cacheados: " + this.statCache.size() + " entradas");
        }
    }

    public DoubleStat resolveStat(String yamlId) {
        String normalized = yamlId.toUpperCase().replace("-", "_");
        DoubleStat stat = this.statCache.get(normalized);
        if (stat != null) {
            return stat;
        }
        String hyphenated = yamlId.toUpperCase();
        stat = this.statCache.get(hyphenated);
        if (stat != null) {
            return stat;
        }
        stat = this.statCache.get(yamlId);
        if (stat != null) {
            return stat;
        }
        stat = this.statCache.get(this.canonicalKey(yamlId));
        if (stat != null) {
            return stat;
        }
        String aliased = this.aliasStatKey(normalized);
        if (!aliased.equals(normalized)) {
            stat = this.statCache.get(aliased);
            if (stat != null) {
                return stat;
            }
            stat = this.statCache.get(this.canonicalKey(aliased));
            if (stat != null) {
                return stat;
            }
        }
        if (this.debug()) {
            this.log.warning("[DEBUG-ENGINE] Stat NAO encontrado: '" + yamlId + "' (tentou: '" + normalized + "', '" + hyphenated + "', canonical='" + this.canonicalKey(yamlId) + "', alias='" + aliased + "')");
        }
        return null;
    }

    private String canonicalKey(String key) {
        return key == null ? "" : key.toUpperCase().replace("-", "").replace("_", "");
    }

    private String aliasStatKey(String key) {
        return switch (key) {
            case "MAX_MANA", "MANA_MAX", "MANA" -> "MAX_MANA";
            case "MANA_REGEN", "MANA_REGENERATION", "MANA_REGEN_RATE" -> "MANA_REGENERATION";
            case "MAX_STAMINA", "STAMINA_MAX", "STAMINA" -> "MAX_STAMINA";
            case "STAMINA_REGEN", "STAMINA_REGENERATION", "STAMINA_REGEN_RATE" -> "STAMINA_REGENERATION";
            default -> key;
        };
    }

    public Gson getGson() {
        return this.gson;
    }

    public Map<String, IndividualMod> getIndividuals() {
        return this.individuals;
    }

    public Map<String, GroupDef> getGroups() {
        return this.groups;
    }

    public List<RolledModifier> rollForTier(String category, int total, int itemLevel, int maxMods) {
        return this.rollForTier(category, total, itemLevel, maxMods, 0);
    }

    public List<RolledModifier> rollForTier(String category, int total, int itemLevel, int maxMods, int tier) {
        int minMods;
        ArrayList<RolledModifier> result = new ArrayList<RolledModifier>();
        HashSet<String> usedCategories = new HashSet<String>();
        HashSet<String> usedModIds = new HashSet<String>();
        String groupId = "nemonicorp_" + category + "_positive";
        if (this.debug()) {
            this.log.info("[DEBUG-ENGINE] rollForTier: cat=" + category + " total=" + total + " lvl=" + itemLevel + " maxMods=" + maxMods + " tier=" + tier + " grupo=" + groupId + " existe=" + this.groups.containsKey(groupId));
        }
        for (int i = 0; i < total * 3 && result.size() < total && result.size() < maxMods; ++i) {
            RolledModifier mod = this.rollFromGroup(groupId, itemLevel, usedCategories, usedModIds, false, tier);
            if (mod == null) {
                if (!this.debug()) break;
                this.log.warning("[DEBUG-ENGINE] rollFromGroup retornou null na iteracao " + i);
                break;
            }
            result.add(mod);
            usedModIds.add(mod.id());
            if (mod.category() != null) {
                usedCategories.add(mod.category());
            }
            if (!this.debug()) continue;
            this.log.info("[DEBUG-ENGINE] Mod rolado: " + mod.id() + " cat=" + mod.category() + " stats=" + String.valueOf(mod.stats()));
        }
        int n = minMods = tier == 2 || tier == 3 ? 3 : 0;
        if (result.size() < minMods && result.size() < maxMods) {
            RolledModifier mod;
            if (this.debug()) {
                this.log.info("[DEBUG-ENGINE] rollForTier: forcando minimo " + minMods + " mods (atual=" + result.size() + ") relaxando categorias");
            }
            for (int i = 0; i < 20 && result.size() < minMods && result.size() < maxMods && (mod = this.rollFromGroup(groupId, itemLevel, Collections.emptySet(), usedModIds, false, tier)) != null; ++i) {
                result.add(mod);
                usedModIds.add(mod.id());
                if (mod.category() == null) continue;
                usedCategories.add(mod.category());
            }
        }
        if (this.debug()) {
            this.log.info("[DEBUG-ENGINE] rollForTier resultado: " + result.size() + " mods");
        }
        return result;
    }

    public List<RolledModifier> rollForUnique(String category, int itemLevel) {
        RolledModifier mod;
        int i;
        RolledModifier eleMod;
        String eleGroup;
        ArrayList<RolledModifier> result = new ArrayList<RolledModifier>();
        HashSet<String> usedCategories = new HashSet<String>();
        HashSet<String> usedModIds = new HashSet<String>();
        String groupId = "nemonicorp_" + category + "_positive";
        if ("weapon".equals(category) && this.groups.containsKey(eleGroup = "orb_w_elemental_cat") && (eleMod = this.rollFromGroup(eleGroup, itemLevel, usedCategories, usedModIds, false, 3)) != null) {
            result.add(eleMod);
            usedModIds.add(eleMod.id());
            if (eleMod.category() != null) {
                usedCategories.add(eleMod.category());
            }
        }
        for (i = 0; i < 30 && result.size() < 6 && (mod = this.rollFromGroup(groupId, itemLevel, usedCategories, usedModIds, false, 3)) != null; ++i) {
            result.add(mod);
            usedModIds.add(mod.id());
            if (mod.category() == null) continue;
            usedCategories.add(mod.category());
        }
        if (result.size() < 3) {
            for (i = 0; i < 20 && result.size() < 6 && (mod = this.rollFromGroup(groupId, itemLevel, Collections.emptySet(), usedModIds, false, 3)) != null; ++i) {
                result.add(mod);
                usedModIds.add(mod.id());
            }
        }
        if (result.size() >= 2) {
            ArrayList<Integer> indices = new ArrayList<Integer>();
            for (int i2 = 0; i2 < result.size(); ++i2) {
                indices.add(i2);
            }
            Collections.shuffle(indices);
            for (int b = 0; b < 2; ++b) {
                int idx = (Integer)indices.get(b);
                RolledModifier original = (RolledModifier)result.get(idx);
                LinkedHashMap<String, Double> buffedStats = new LinkedHashMap<String, Double>();
                for (Map.Entry<String, Double> entry : original.stats().entrySet()) {
                    buffedStats.put(entry.getKey(), entry.getValue() * 2.0);
                }
                result.set(idx, new RolledModifier(original.id(), original.category(), buffedStats, original.negative()));
                if (!this.debug()) continue;
                this.log.info("[DEBUG-ENGINE] Unique buff x2 no mod[" + idx + "]: " + original.id());
            }
        }
        if (this.debug()) {
            this.log.info("[DEBUG-ENGINE] rollForUnique resultado: " + result.size() + " mods");
        }
        return result;
    }

    public RolledModifier rollFromGroup(String groupId, int itemLevel, Set<String> excludeCategories, boolean negative) {
        return this.rollFromGroup(groupId, itemLevel, excludeCategories, Collections.emptySet(), negative, 0);
    }

    public RolledModifier rollFromGroup(String groupId, int itemLevel, Set<String> excludeCategories, boolean negative, int tier) {
        return this.rollFromGroup(groupId, itemLevel, excludeCategories, Collections.emptySet(), negative, tier);
    }

    public RolledModifier rollFromGroup(String groupId, int itemLevel, Set<String> excludeCategories, Set<String> excludeModIds, boolean negative, int tier) {
        if (this.debug()) {
            this.log.info("[DEBUG-ENGINE] rollFromGroup: grupo=" + groupId + " excluidos=" + String.valueOf(excludeCategories) + " ids=" + String.valueOf(excludeModIds) + " existe=" + this.groups.containsKey(groupId));
        }
        for (int attempt = 0; attempt < 20; ++attempt) {
            String[] resolved = this.resolveToModifier(groupId, excludeCategories);
            if (resolved == null) {
                if (this.debug()) {
                    this.log.warning("[DEBUG-ENGINE] resolveToModifier retornou null para " + groupId);
                }
                return null;
            }
            String modId = resolved[0];
            String categoryId = resolved[1];
            if (categoryId != null && excludeCategories.contains(categoryId)) {
                if (!this.debug()) continue;
                this.log.info("[DEBUG-ENGINE] Categoria ja usada: " + categoryId + ", tentando novamente...");
                continue;
            }
            if (excludeModIds != null && excludeModIds.contains(modId)) {
                if (!this.debug()) continue;
                this.log.info("[DEBUG-ENGINE] Mod ID ja usado: " + modId + ", tentando novamente...");
                continue;
            }
            IndividualMod def = this.individuals.get(modId);
            if (def == null) {
                if (!this.debug()) continue;
                this.log.warning("[DEBUG-ENGINE] Individual nao encontrado: " + modId);
                continue;
            }
            RolledModifier rolled = this.rollValues(def, categoryId, itemLevel, negative, tier);
            if (this.debug()) {
                this.log.info("[DEBUG-ENGINE] Rolado com sucesso: " + rolled.id() + " cat=" + rolled.category() + "'");
            }
            return rolled;
        }
        if (this.debug()) {
            this.log.warning("[DEBUG-ENGINE] rollFromGroup esgotou tentativas para " + groupId);
        }
        return null;
    }

    private String[] resolveToModifier(String id, Set<String> excludeCategories) {
        if (this.individuals.containsKey(id) && !this.groups.containsKey(id)) {
            if (this.debug()) {
                this.log.info("[DEBUG-ENGINE] resolveToModifier: " + id + " -> individual direto");
            }
            return new String[]{id, null};
        }
        GroupDef group = this.groups.get(id);
        if (group == null) {
            if (this.debug()) {
                this.log.warning("[DEBUG-ENGINE] resolveToModifier: grupo '" + id + "' NAO encontrado!");
            }
            return null;
        }
        String selected = this.weightedRandom(group.children(), excludeCategories);
        if (selected == null) {
            if (this.debug()) {
                this.log.warning("[DEBUG-ENGINE] resolveToModifier: weightedRandom retornou null para " + id + " (filhos=" + String.valueOf(group.children().keySet()) + " excluidos=" + String.valueOf(excludeCategories) + ")");
            }
            return null;
        }
        if (this.debug()) {
            this.log.info("[DEBUG-ENGINE] resolveToModifier: " + id + " -> selecionou " + selected + " (isGroup=" + this.groups.containsKey(selected) + " isIndividual=" + this.individuals.containsKey(selected) + ")");
        }
        if (this.groups.containsKey(selected) && !this.individuals.containsKey(selected)) {
            String[] deeper = this.resolveToModifier(selected, Collections.emptySet());
            if (deeper == null) {
                return null;
            }
            return new String[]{deeper[0], selected};
        }
        if (this.individuals.containsKey(selected)) {
            return new String[]{selected, id.contains("_cat") ? id : selected};
        }
        if (this.debug()) {
            this.log.warning("[DEBUG-ENGINE] resolveToModifier: '" + selected + "' nao e grupo nem individual!");
        }
        return null;
    }

    public RolledModifier rollValues(IndividualMod def, String categoryId, int itemLevel, boolean negative) {
        return this.rollValues(def, categoryId, itemLevel, negative, 0);
    }

    private int rollEnchantTier(int maxTier) {
        double roll = ThreadLocalRandom.current().nextDouble();
        if (maxTier >= 3 && roll < 0.001) {
            return 3;
        }
        if (roll < 0.031) {
            return 2;
        }
        return 1;
    }

    public RolledModifier rollValues(IndividualMod def, String categoryId, int itemLevel, boolean negative, int tier) {
        LinkedHashMap<String, Double> rolledStats = new LinkedHashMap<String, Double>();
        double levelRatio = this.plugin.getConfig().getDouble("orb-level-scaling-ratio", 0.45);
        levelRatio = Math.max(0.0, Math.min(1.0, levelRatio));
        double effectiveLevel = 1.0 + ((double)Math.max(1, itemLevel) - 1.0) * levelRatio;
        double tierBonus = switch (tier) {
            case 2 -> 0.3;
            case 3 -> 1.0;
            default -> 0.0;
        };
        for (Map.Entry<String, StatValues> entry : def.stats().entrySet()) {
            double rounded;
            StatValues sv = entry.getValue();
            Integer enchantMax = ENCHANT_TIER_MAX.get(entry.getKey());
            if (enchantMax != null) {
                int enchantTier = this.rollEnchantTier(enchantMax);
                rolledStats.put(entry.getKey(), Double.valueOf(enchantTier));
                continue;
            }
            double raw = sv.base() + sv.scale() * effectiveLevel;
            raw *= 1.0 + tierBonus;
            if (sv.spread() > 0.0) {
                double spreadFactor = ThreadLocalRandom.current().nextDouble(-sv.spread(), sv.spread());
                spreadFactor = Math.max(-sv.maxSpread(), Math.min(sv.maxSpread(), spreadFactor));
                raw *= 1.0 + spreadFactor;
            }
            if ((rounded = (double)Math.round(raw * 100.0) / 100.0) == 0.0 && (sv.base() != 0.0 || sv.scale() != 0.0)) {
                rounded = raw >= 0.0 ? 0.1 : -0.1;
            }
            rolledStats.put(entry.getKey(), rounded);
        }
        return new RolledModifier(def.id(), categoryId, rolledStats, negative);
    }

    public RolledModifier rerollExisting(RolledModifier existing, int itemLevel) {
        IndividualMod def = this.individuals.get(existing.id());
        if (def == null) {
            return existing;
        }
        return this.rollValues(def, existing.category(), itemLevel, existing.negative());
    }

    private String weightedRandom(Map<String, Integer> weights, Set<String> exclude) {
        LinkedHashMap<String, Integer> filtered = new LinkedHashMap<String, Integer>();
        for (Map.Entry<String, Integer> e : weights.entrySet()) {
            if (exclude.contains(e.getKey())) continue;
            filtered.put(e.getKey(), e.getValue());
        }
        int total = filtered.values().stream().mapToInt(Integer::intValue).sum();
        if (total <= 0) {
            return null;
        }
        int roll = ThreadLocalRandom.current().nextInt(total);
        int cumulative = 0;
        for (Map.Entry e : filtered.entrySet()) {
            if (roll >= (cumulative += ((Integer)e.getValue()).intValue())) continue;
            return (String)e.getKey();
        }
        return null;
    }

    public record GroupDef(String id, int min, int max, LinkedHashMap<String, Integer> children) {
    }

    public record StatValues(double base, double scale, double spread, double maxSpread) {
    }

    public record IndividualMod(String id, LinkedHashMap<String, StatValues> stats) {
    }

    public record RolledModifier(String id, String category, LinkedHashMap<String, Double> stats, boolean negative) {
    }
}

