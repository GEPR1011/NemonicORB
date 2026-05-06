package com.nemonicorp.orbs;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import net.Indyuce.mmoitems.MMOItems;
import net.Indyuce.mmoitems.stat.type.DoubleStat;
import net.Indyuce.mmoitems.stat.type.ItemStat;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.lang.reflect.Type;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.logging.Logger;

/**
 * Motor de modificadores v2 — suporta sistema de tiers,
 * prefixos/sufixos separados e pools por categoria.
 */
public class ModifierEngine {

    // ── Estruturas de dados ──

    public record GroupDef(String id, int min, int max, LinkedHashMap<String, Integer> children) {}

    public record IndividualMod(String id, LinkedHashMap<String, StatValues> stats) {}

    public record StatValues(double base, double scale, double spread, double maxSpread) {}

    /**
     * Resultado de um modificador rolado.
     */
    public record RolledModifier(String id, String category,
                                 LinkedHashMap<String, Double> stats,
                                 boolean negative) {}

    public static final Type ROLLED_LIST_TYPE =
            new TypeToken<List<RolledModifier>>() {}.getType();

    // ── Campos ──

    private final NemonicOrbPlugin plugin;
    private final Logger log;
    private final Gson gson = new Gson();

    private final Map<String, GroupDef> groups = new HashMap<>();
    private final Map<String, IndividualMod> individuals = new HashMap<>();
    private final Map<String, DoubleStat> statCache = new HashMap<>();
    private static final Set<String> BLACKLISTED_STATS = Set.of(
            "magic-damage",
            "skill-damage",
            "vanilla-exp-gain",
            "skill-exp-gain",
            "additional-experience"
    );

    public ModifierEngine(NemonicOrbPlugin plugin) {
        this.plugin = plugin;
        this.log = plugin.getLogger();
    }

    private boolean debug() {
        return plugin.getConfig().getBoolean("debug", true);
    }

    // ── Carregamento ──

    public void load() {
        groups.clear();
        individuals.clear();
        statCache.clear();

        // Tentar carregar do data folder do plugin primeiro (auto-extraido)
        File modFile = new File(plugin.getDataFolder(), "nemonicorp_orb_modifiers.yml");
        if (!modFile.exists()) {
            // Fallback: tentar na pasta do MMOItems
            File mmoitemsDir = new File(plugin.getDataFolder().getParentFile(), "MMOItems");
            modFile = new File(mmoitemsDir, "modifiers/nemonicorp_orb_modifiers.yml");
        }
        if (!modFile.exists()) {
            log.severe("ERRO: Arquivo de modificadores NAO encontrado!");
            log.severe("  Verificado: " + new File(plugin.getDataFolder(), "nemonicorp_orb_modifiers.yml").getPath());
            log.severe("  Verificado: " + new File(new File(plugin.getDataFolder().getParentFile(), "MMOItems"), "modifiers/nemonicorp_orb_modifiers.yml").getPath());
            return;
        }
        log.info("Carregando modificadores de: " + modFile.getPath());
        log.info("Arquivo existe: " + modFile.exists() + " tamanho: " + modFile.length() + " bytes");

        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(modFile);
        Set<String> topKeys = yaml.getKeys(false);
        log.info("YAML top-level keys: " + topKeys.size() + " -> " + topKeys);
        for (String key : topKeys) {
            ConfigurationSection section = yaml.getConfigurationSection(key);
            if (section == null) continue;

            if (section.contains("modifiers")) parseGroup(key, section);
            if (section.contains("stats")) parseIndividual(key, section);
        }

        pruneInvalidGroupChildren();

        cacheStats();

        log.info("Modificadores v2 carregados: " + groups.size() + " grupos, "
                + individuals.size() + " individuais, " + statCache.size() + " stats cacheados.");

        if (groups.isEmpty() || individuals.isEmpty()) {
            log.severe("====================================================");
            log.severe("ERRO CRITICO: Pool de modificadores VAZIA!");
            log.severe("  Grupos: " + groups.size() + " | Individuais: " + individuals.size());
            log.severe("  Arquivo: " + modFile.getPath());
            log.severe("  Tamanho: " + modFile.length() + " bytes");
            log.severe("  Orbs NAO vao funcionar ate resolver isso!");
            log.severe("====================================================");
        }

        if (debug()) {
            log.info("[DEBUG-ENGINE] Grupos: " + groups.keySet());
            log.info("[DEBUG-ENGINE] Individuais: " + individuals.keySet());
            log.info("[DEBUG-ENGINE] Stats cacheados: " + statCache.keySet());
        }
    }

    private void parseGroup(String id, ConfigurationSection section) {
        int min = section.getInt("min", 0);
        int max = section.getInt("max", 1);
        LinkedHashMap<String, Integer> children = new LinkedHashMap<>();

        ConfigurationSection modsSec = section.getConfigurationSection("modifiers");
        if (modsSec != null) {
            for (String childId : modsSec.getKeys(false)) {
                children.put(childId, modsSec.getInt(childId, 1));
            }
        }
        groups.put(id, new GroupDef(id, min, max, children));
        if (debug()) log.info("[DEBUG-ENGINE] Grupo parseado: " + id + " -> filhos: " + children.keySet());
    }

    private void parseIndividual(String id, ConfigurationSection section) {
        LinkedHashMap<String, StatValues> stats = new LinkedHashMap<>();

        ConfigurationSection statsSec = section.getConfigurationSection("stats");
        if (statsSec != null) {
            for (String statId : statsSec.getKeys(false)) {
                if (statId.equals("perm-effects")) continue;
                if (isBlacklistedStat(statId)) continue;

                Object val = statsSec.get(statId);
                if (val instanceof ConfigurationSection statSec) {
                    stats.put(statId, new StatValues(
                            statSec.getDouble("base", 0),
                            statSec.getDouble("scale", 0),
                            statSec.getDouble("spread", 0),
                            statSec.getDouble("max-spread", 0)
                    ));
                } else if (val instanceof Number num) {
                    stats.put(statId, new StatValues(num.doubleValue(), 0, 0, 0));
                }
            }
        }
        if (stats.isEmpty()) {
            if (debug()) log.info("[DEBUG-ENGINE] Individual ignorado (apenas stats bloqueados): " + id);
            return;
        }
        individuals.put(id, new IndividualMod(id, stats));
        if (debug()) log.info("[DEBUG-ENGINE] Individual parseado: " + id + " stats=" + stats.keySet());
    }

    private boolean isBlacklistedStat(String statId) {
        return statId != null && BLACKLISTED_STATS.contains(statId.toLowerCase(Locale.ROOT));
    }

    private void pruneInvalidGroupChildren() {
        for (Map.Entry<String, GroupDef> entry : new ArrayList<>(groups.entrySet())) {
            GroupDef def = entry.getValue();
            LinkedHashMap<String, Integer> filtered = new LinkedHashMap<>();
            for (Map.Entry<String, Integer> child : def.children().entrySet()) {
                String childId = child.getKey();
                if (groups.containsKey(childId) || individuals.containsKey(childId)) {
                    filtered.put(childId, child.getValue());
                }
            }
            groups.put(entry.getKey(), new GroupDef(def.id(), def.min(), def.max(), filtered));
        }
    }

    private void cacheStats() {
        try {
            for (ItemStat<?, ?> stat : MMOItems.plugin.getStats().getAll()) {
                if (stat instanceof DoubleStat ds) {
                    String id = stat.getId().toUpperCase();
                    statCache.put(id, ds);
                    // Tambem cachear com hifens para match direto
                    String hyphenated = id.replace("_", "-");
                    if (!hyphenated.equals(id)) {
                        statCache.put(hyphenated, ds);
                    }
                    // Cache canônico sem separadores (ex.: MAX-MANA / MAX_MANA -> MAXMANA)
                    statCache.put(canonicalKey(id), ds);
                }
            }
        } catch (Exception e) {
            log.warning("Erro ao cachear stats do MMOItems: " + e.getMessage());
        }
        if (debug()) log.info("[DEBUG-ENGINE] Stats cacheados: " + statCache.size() + " entradas");
    }

    // ── Acesso ──

    public DoubleStat resolveStat(String yamlId) {
        // Tentar exatamente como esta (uppercase)
        String normalized = yamlId.toUpperCase().replace("-", "_");
        DoubleStat stat = statCache.get(normalized);
        if (stat != null) return stat;

        // Tentar com hifens (backup)
        String hyphenated = yamlId.toUpperCase();
        stat = statCache.get(hyphenated);
        if (stat != null) return stat;

        // Tentar lowercase do original
        stat = statCache.get(yamlId);
        if (stat != null) return stat;

        // Tentar chave canônica (sem "_" e "-")
        stat = statCache.get(canonicalKey(yamlId));
        if (stat != null) return stat;

        // Aliases comuns de mana/stamina entre packs/configs
        String aliased = aliasStatKey(normalized);
        if (!aliased.equals(normalized)) {
            stat = statCache.get(aliased);
            if (stat != null) return stat;
            stat = statCache.get(canonicalKey(aliased));
            if (stat != null) return stat;
        }

        if (debug()) log.warning("[DEBUG-ENGINE] Stat NAO encontrado: '" + yamlId
                + "' (tentou: '" + normalized + "', '" + hyphenated + "', canonical='" + canonicalKey(yamlId)
                + "', alias='" + aliased + "')");
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

    public Gson getGson() { return gson; }
    public Map<String, IndividualMod> getIndividuals() { return individuals; }
    public Map<String, GroupDef> getGroups() { return groups; }

    // ── Rolagem v2 — respeita limites de prefixos e sufixos ──

    /**
     * Rola modificadores para um tier.
     * @param category  weapon, armor, accessory
     * @param total     quantidade total de mods desejados
     * @param itemLevel nivel do item
     * @param maxMods   maximo de mods permitidos
     */
    public List<RolledModifier> rollForTier(String category, int total, int itemLevel,
                                            int maxMods) {
        return rollForTier(category, total, itemLevel, maxMods, 0);
    }

    /**
     * Rola modificadores para um tier com bonus de tier.
     * @param category  weapon, armor, accessory
     * @param total     quantidade total de mods desejados
     * @param itemLevel nivel do item
     * @param maxMods   maximo de mods permitidos
     * @param tier      tier do item (0=Comum, 1=Magico, 2=Raro, 3=Unico) — afeta tierBonus
     */
    public List<RolledModifier> rollForTier(String category, int total, int itemLevel,
                                            int maxMods, int tier) {
        List<RolledModifier> result = new ArrayList<>();
        Set<String> usedCategories = new HashSet<>();
        Set<String> usedModIds = new HashSet<>();

        String groupId = "nemonicorp_" + category + "_positive";

        if (debug()) log.info("[DEBUG-ENGINE] rollForTier: cat=" + category + " total=" + total
                + " lvl=" + itemLevel + " maxMods=" + maxMods + " tier=" + tier
                + " grupo=" + groupId + " existe=" + groups.containsKey(groupId));

        for (int i = 0; i < total * 3 && result.size() < total && result.size() < maxMods; i++) {
            RolledModifier mod = rollFromGroup(groupId, itemLevel, usedCategories, usedModIds, false, tier);
            if (mod == null) {
                if (debug()) log.warning("[DEBUG-ENGINE] rollFromGroup retornou null na iteracao " + i);
                break;
            }

            result.add(mod);
            usedModIds.add(mod.id());
            if (mod.category() != null) usedCategories.add(mod.category());
            if (debug()) log.info("[DEBUG-ENGINE] Mod rolado: " + mod.id()
                    + " cat=" + mod.category() + " stats=" + mod.stats());
        }

        // Garantia minima de mods por tier — relaxa categoria, mantem dedup por ID.
        // Tier 2 (Pedra de Refinamento) precisa minimo 3, Tier 3 (Unicos) tambem 3.
        int minMods = (tier == 2 || tier == 3) ? 3 : 0;
        if (result.size() < minMods && result.size() < maxMods) {
            if (debug()) log.info("[DEBUG-ENGINE] rollForTier: forcando minimo "
                    + minMods + " mods (atual=" + result.size() + ") relaxando categorias");
            for (int i = 0; i < 20 && result.size() < minMods && result.size() < maxMods; i++) {
                RolledModifier mod = rollFromGroup(groupId, itemLevel,
                        Collections.emptySet(), usedModIds, false, tier);
                if (mod == null) break;
                result.add(mod);
                usedModIds.add(mod.id());
                if (mod.category() != null) usedCategories.add(mod.category());
            }
        }

        if (debug()) log.info("[DEBUG-ENGINE] rollForTier resultado: " + result.size() + " mods");
        return result;
    }

    /**
     * Rola mods para itens Unicos (Tier 3): sempre 6 mods, com pelo menos 1 dano elemental garantido.
     * Valores recebem tierBonus = 3 (mais altos que itens normais).
     */
    public List<RolledModifier> rollForUnique(String category, int itemLevel) {
        List<RolledModifier> result = new ArrayList<>();
        Set<String> usedCategories = new HashSet<>();
        Set<String> usedModIds = new HashSet<>();
        String groupId = "nemonicorp_" + category + "_positive";

        // Garantir 1 elemental para armas
        if ("weapon".equals(category)) {
            String eleGroup = "orb_w_elemental_cat";
            if (groups.containsKey(eleGroup)) {
                RolledModifier eleMod = rollFromGroup(eleGroup, itemLevel, usedCategories, usedModIds, false, 3);
                if (eleMod != null) {
                    result.add(eleMod);
                    usedModIds.add(eleMod.id());
                    if (eleMod.category() != null) usedCategories.add(eleMod.category());
                }
            }
        }

        // Preencher ate 6 mods (dedup por ID)
        for (int i = 0; i < 30 && result.size() < 6; i++) {
            RolledModifier mod = rollFromGroup(groupId, itemLevel, usedCategories, usedModIds, false, 3);
            if (mod == null) break;
            result.add(mod);
            usedModIds.add(mod.id());
            if (mod.category() != null) usedCategories.add(mod.category());
        }

        // Garantir minimo 3 mods em Unicos (relaxa categoria, dedup ID)
        if (result.size() < 3) {
            for (int i = 0; i < 20 && result.size() < 6; i++) {
                RolledModifier mod = rollFromGroup(groupId, itemLevel,
                        Collections.emptySet(), usedModIds, false, 3);
                if (mod == null) break;
                result.add(mod);
                usedModIds.add(mod.id());
            }
        }

        // Buff 2 random mods x2 (dobrar os valores de 2 mods aleatorios)
        if (result.size() >= 2) {
            List<Integer> indices = new ArrayList<>();
            for (int i = 0; i < result.size(); i++) indices.add(i);
            Collections.shuffle(indices);
            for (int b = 0; b < 2; b++) {
                int idx = indices.get(b);
                RolledModifier original = result.get(idx);
                LinkedHashMap<String, Double> buffedStats = new LinkedHashMap<>();
                for (Map.Entry<String, Double> entry : original.stats().entrySet()) {
                    buffedStats.put(entry.getKey(), entry.getValue() * 2.0);
                }
                result.set(idx, new RolledModifier(original.id(), original.category(), buffedStats, original.negative()));
                if (debug()) log.info("[DEBUG-ENGINE] Unique buff x2 no mod[" + idx + "]: " + original.id());
            }
        }

        if (debug()) log.info("[DEBUG-ENGINE] rollForUnique resultado: " + result.size() + " mods");
        return result;
    }

    /**
     * Rola um modificador de um grupo, respeitando categorias excluidas.
     */
    public RolledModifier rollFromGroup(String groupId, int itemLevel,
                                        Set<String> excludeCategories, boolean negative) {
        return rollFromGroup(groupId, itemLevel, excludeCategories, Collections.emptySet(), negative, 0);
    }

    public RolledModifier rollFromGroup(String groupId, int itemLevel,
                                        Set<String> excludeCategories, boolean negative, int tier) {
        return rollFromGroup(groupId, itemLevel, excludeCategories, Collections.emptySet(), negative, tier);
    }

    public RolledModifier rollFromGroup(String groupId, int itemLevel,
                                        Set<String> excludeCategories, Set<String> excludeModIds,
                                        boolean negative, int tier) {
        if (debug()) log.info("[DEBUG-ENGINE] rollFromGroup: grupo=" + groupId
                + " excluidos=" + excludeCategories + " ids=" + excludeModIds
                + " existe=" + groups.containsKey(groupId));
        for (int attempt = 0; attempt < 20; attempt++) {
            String[] resolved = resolveToModifier(groupId, excludeCategories);
            if (resolved == null) {
                if (debug()) log.warning("[DEBUG-ENGINE] resolveToModifier retornou null para " + groupId);
                return null;
            }

            String modId = resolved[0];
            String categoryId = resolved[1];

            if (categoryId != null && excludeCategories.contains(categoryId)) {
                if (debug()) log.info("[DEBUG-ENGINE] Categoria ja usada: " + categoryId + ", tentando novamente...");
                continue;
            }

            if (excludeModIds != null && excludeModIds.contains(modId)) {
                if (debug()) log.info("[DEBUG-ENGINE] Mod ID ja usado: " + modId + ", tentando novamente...");
                continue;
            }

            IndividualMod def = individuals.get(modId);
            if (def == null) {
                if (debug()) log.warning("[DEBUG-ENGINE] Individual nao encontrado: " + modId);
                continue;
            }

            RolledModifier rolled = rollValues(def, categoryId, itemLevel, negative, tier);
            if (debug()) log.info("[DEBUG-ENGINE] Rolado com sucesso: " + rolled.id()
                    + " cat=" + rolled.category() + "'");
            return rolled;
        }
        if (debug()) log.warning("[DEBUG-ENGINE] rollFromGroup esgotou tentativas para " + groupId);
        return null;
    }

    private String[] resolveToModifier(String id, Set<String> excludeCategories) {
        if (individuals.containsKey(id) && !groups.containsKey(id)) {
            if (debug()) log.info("[DEBUG-ENGINE] resolveToModifier: " + id + " -> individual direto");
            return new String[]{id, null};
        }

        GroupDef group = groups.get(id);
        if (group == null) {
            if (debug()) log.warning("[DEBUG-ENGINE] resolveToModifier: grupo '" + id + "' NAO encontrado!");
            return null;
        }

        String selected = weightedRandom(group.children(), excludeCategories);
        if (selected == null) {
            if (debug()) log.warning("[DEBUG-ENGINE] resolveToModifier: weightedRandom retornou null para " + id
                    + " (filhos=" + group.children().keySet() + " excluidos=" + excludeCategories + ")");
            return null;
        }

        if (debug()) log.info("[DEBUG-ENGINE] resolveToModifier: " + id + " -> selecionou " + selected
                + " (isGroup=" + groups.containsKey(selected) + " isIndividual=" + individuals.containsKey(selected) + ")");

        if (groups.containsKey(selected) && !individuals.containsKey(selected)) {
            String[] deeper = resolveToModifier(selected, Collections.emptySet());
            if (deeper == null) return null;
            return new String[]{deeper[0], selected};
        }

        if (individuals.containsKey(selected)) {
            return new String[]{selected, id.contains("_cat") ? id : selected};
        }

        if (debug()) log.warning("[DEBUG-ENGINE] resolveToModifier: '" + selected + "' nao e grupo nem individual!");
        return null;
    }

    public RolledModifier rollValues(IndividualMod def, String categoryId,
                                     int itemLevel, boolean negative) {
        return rollValues(def, categoryId, itemLevel, negative, 0);
    }

    /**
     * Rola valores para um modificador com bonus de tier.
     * Formula: raw = (base + scale * level) * (1 + tierBonus)
     * tierBonus: 0=Comum/Magico, 0.3=Raro, 0.8=Unico
     */
    // Stats que correspondem a niveis de encantamento vanilla — valor rolado = tier (1, 2 ou 3)
    // com distribuicao de raridade independente de base/scale do YAML.
    private static final Map<String, Integer> ENCHANT_TIER_MAX = Map.of(
            "fortune", 3,
            "unbreaking", 3,
            "knockback", 2
    );

    /**
     * Rola um tier para stat de encantamento segundo a distribuicao:
     *  - Tier 1: padrao
     *  - Tier 2: 3%
     *  - Tier 3: 0.1% (apenas se maxTier >= 3)
     */
    private int rollEnchantTier(int maxTier) {
        double roll = ThreadLocalRandom.current().nextDouble();
        if (maxTier >= 3 && roll < 0.001) return 3;
        if (roll < 0.031) return 2; // 3% (inclui o range acima de T3 ate 0.031)
        return 1;
    }

    public RolledModifier rollValues(IndividualMod def, String categoryId,
                                     int itemLevel, boolean negative, int tier) {
        LinkedHashMap<String, Double> rolledStats = new LinkedHashMap<>();
        double levelRatio = plugin.getConfig().getDouble("orb-level-scaling-ratio", 0.45);
        levelRatio = Math.max(0.0, Math.min(1.0, levelRatio));
        double effectiveLevel = 1.0 + (Math.max(1, itemLevel) - 1.0) * levelRatio;

        double tierBonus = switch (tier) {
            case 2 -> 0.3;   // Raro: +30%
            case 3 -> 1.0;   // Unico: +100% (dobro)
            default -> 0.0;  // Comum/Magico: sem bonus
        };

        for (Map.Entry<String, StatValues> entry : def.stats().entrySet()) {
            StatValues sv = entry.getValue();

            // Stats de encantamento vanilla usam rolagem de tier (raridade) independente
            Integer enchantMax = ENCHANT_TIER_MAX.get(entry.getKey());
            if (enchantMax != null) {
                int enchantTier = rollEnchantTier(enchantMax);
                rolledStats.put(entry.getKey(), (double) enchantTier);
                continue;
            }

            double raw = sv.base() + sv.scale() * effectiveLevel;

            // Aplicar tier bonus (multiplicativo sobre a base)
            raw *= (1.0 + tierBonus);

            if (sv.spread() > 0) {
                double spreadFactor = ThreadLocalRandom.current().nextDouble(-sv.spread(), sv.spread());
                spreadFactor = Math.max(-sv.maxSpread(), Math.min(sv.maxSpread(), spreadFactor));
                raw *= (1.0 + spreadFactor);
            }

            double rounded = Math.round(raw * 100.0) / 100.0;
            // Prevent 0.0 values — minimum absolute value of 0.1
            if (rounded == 0.0 && (sv.base() != 0 || sv.scale() != 0)) {
                rounded = raw >= 0 ? 0.1 : -0.1;
            }
            rolledStats.put(entry.getKey(), rounded);
        }

        return new RolledModifier(def.id(), categoryId, rolledStats, negative);
    }

    public RolledModifier rerollExisting(RolledModifier existing, int itemLevel) {
        IndividualMod def = individuals.get(existing.id());
        if (def == null) return existing;
        return rollValues(def, existing.category(), itemLevel, existing.negative());
    }

    // ── Utilitarios ──

    private String weightedRandom(Map<String, Integer> weights, Set<String> exclude) {
        Map<String, Integer> filtered = new LinkedHashMap<>();
        for (Map.Entry<String, Integer> e : weights.entrySet()) {
            if (!exclude.contains(e.getKey())) filtered.put(e.getKey(), e.getValue());
        }

        int total = filtered.values().stream().mapToInt(Integer::intValue).sum();
        if (total <= 0) return null;

        int roll = ThreadLocalRandom.current().nextInt(total);
        int cumulative = 0;
        for (Map.Entry<String, Integer> e : filtered.entrySet()) {
            cumulative += e.getValue();
            if (roll < cumulative) return e.getKey();
        }
        return null;
    }
}
