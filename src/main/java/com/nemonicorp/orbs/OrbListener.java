package com.nemonicorp.orbs;

import com.google.gson.Gson;
import io.lumine.mythic.lib.api.item.ItemTag;
import io.lumine.mythic.lib.api.item.NBTItem;
import net.Indyuce.mmoitems.api.item.mmoitem.LiveMMOItem;
import net.Indyuce.mmoitems.stat.data.DoubleData;
import net.Indyuce.mmoitems.stat.data.StringData;
import net.Indyuce.mmoitems.stat.data.StringListData;
import net.Indyuce.mmoitems.stat.type.DoubleStat;
import net.Indyuce.mmoitems.ItemStats;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Registry;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.EquipmentSlotGroup;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.bukkit.inventory.ItemFlag;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

/**
 * Listener v2.1 — 9 Orbs estilo PoE2 + suporte a itens vanilla.
 * Item alvo na off-hand, orb na mao principal, right-click.
 */
public class OrbListener implements Listener {

    // ── NBT Tags ──
    public static final String NBT_TIER = "NEMONICORB_TIER";
    public static final String NBT_MODS = "NEMONICORB_MODS";
    public static final String NBT_IDENTIFIED = "NEMONICORB_IDENTIFIED";
    public static final String NBT_POLISH = "NEMONICORB_POLISH";
    public static final String NBT_POLISHED = "NEMONICORB_POLISHED";
    public static final String NBT_CRAFT_LEVEL = "NEMONICORB_CRAFT_LEVEL";
    public static final String NBT_DPS = "NEMONICORB_DPH";
    public static final String NBT_FORGE_QUALITY = "NEMONICORB_FORGE_QUALITY";
    public static final String NBT_FORGE_SMITH = "NEMONICORB_FORGE_SMITH";
    public static final String NBT_FORGE_BONUS_PCT = "NEMONICORB_FORGE_BONUS";
    // Marcador invisível para linhas NemonicOrb no lore (\uFEFF = zero-width no-break space, sobrevive ao round-trip do Bukkit)
    private static final String NEM = "\uFEFF";

    // Regex para remover TODOS os caracteres residuais de tooltip/negative space
    private static final String TOOLTIP_CHARS_REGEX = "[\\uA000-\\uA00F\\uEA60-\\uEA75\\uF800-\\uF80F]";

    private final NemonicOrbPlugin plugin;
    private final ModifierEngine engine;
    private final Gson gson;
    private final Map<UUID, Long> cooldowns = new HashMap<>();
    private MerchantTableListener merchantListener;

    /** Setter chamado pelo NemonicOrbPlugin apos instanciar MerchantTableListener. */
    public void setMerchantListener(MerchantTableListener listener) {
        this.merchantListener = listener;
    }

    // Materiais vanilla classificados por categoria
    private static final Set<Material> VANILLA_WEAPONS = EnumSet.of(
            Material.WOODEN_SWORD, Material.STONE_SWORD, Material.IRON_SWORD,
            Material.GOLDEN_SWORD, Material.DIAMOND_SWORD, Material.NETHERITE_SWORD,
            Material.WOODEN_AXE, Material.STONE_AXE, Material.IRON_AXE,
            Material.GOLDEN_AXE, Material.DIAMOND_AXE, Material.NETHERITE_AXE,
            Material.BOW, Material.CROSSBOW, Material.TRIDENT, Material.MACE
    );
    private static final Set<Material> VANILLA_ARMOR = EnumSet.of(
            Material.LEATHER_HELMET, Material.LEATHER_CHESTPLATE, Material.LEATHER_LEGGINGS, Material.LEATHER_BOOTS,
            Material.CHAINMAIL_HELMET, Material.CHAINMAIL_CHESTPLATE, Material.CHAINMAIL_LEGGINGS, Material.CHAINMAIL_BOOTS,
            Material.IRON_HELMET, Material.IRON_CHESTPLATE, Material.IRON_LEGGINGS, Material.IRON_BOOTS,
            Material.GOLDEN_HELMET, Material.GOLDEN_CHESTPLATE, Material.GOLDEN_LEGGINGS, Material.GOLDEN_BOOTS,
            Material.DIAMOND_HELMET, Material.DIAMOND_CHESTPLATE, Material.DIAMOND_LEGGINGS, Material.DIAMOND_BOOTS,
            Material.NETHERITE_HELMET, Material.NETHERITE_CHESTPLATE, Material.NETHERITE_LEGGINGS, Material.NETHERITE_BOOTS,
            Material.TURTLE_HELMET
    );
    private static final Set<Material> VANILLA_TOOLS = EnumSet.of(
            Material.WOODEN_PICKAXE, Material.STONE_PICKAXE, Material.IRON_PICKAXE,
            Material.GOLDEN_PICKAXE, Material.DIAMOND_PICKAXE, Material.NETHERITE_PICKAXE,
            Material.WOODEN_SHOVEL, Material.STONE_SHOVEL, Material.IRON_SHOVEL,
            Material.GOLDEN_SHOVEL, Material.DIAMOND_SHOVEL, Material.NETHERITE_SHOVEL,
            Material.WOODEN_HOE, Material.STONE_HOE, Material.IRON_HOE,
            Material.GOLDEN_HOE, Material.DIAMOND_HOE, Material.NETHERITE_HOE,
            Material.FISHING_ROD, Material.SHEARS
    );
    private static final Set<Material> VANILLA_SHIELDS = EnumSet.of(
            Material.SHIELD
    );

    public OrbListener(NemonicOrbPlugin plugin) {
        this.plugin = plugin;
        this.engine = plugin.getModifierEngine();
        this.gson = engine.getGson();
    }

    private boolean debug() {
        return plugin.getConfig().getBoolean("debug", true);
    }

    // ── Listener de limpeza: remove caracteres de tooltip residuais de itens existentes ──
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        // Agendar para 1 segundo depois do join (dar tempo do inventario carregar)
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            Player p = event.getPlayer();
            if (p.isOnline()) cleanInventoryTooltipChars(p);
        }, 20L);
    }

    @EventHandler
    public void onInventoryOpen(InventoryOpenEvent event) {
        if (event.getPlayer() instanceof Player p) {
            cleanInventoryTooltipChars(p);
        }
    }

    private void cleanInventoryTooltipChars(Player player) {
        for (ItemStack item : player.getInventory().getContents()) {
            if (item == null || !item.hasItemMeta()) continue;
            NBTItem nbt = NBTItem.get(item);
            if (!nbt.hasTag(NBT_TIER)) continue; // So limpar itens do NemonicOrb
            updateItemDisplay(item, nbt);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_AIR
                && event.getAction() != Action.RIGHT_CLICK_BLOCK) return;

        Player player = event.getPlayer();

        // Cancelar evento da off-hand quando main hand e um orb
        // Previne auto-equip de armaduras na off-hand
        if (event.getHand() == EquipmentSlot.OFF_HAND) {
            ItemStack mh = player.getInventory().getItemInMainHand();
            if (mh.getType() != Material.AIR) {
                NBTItem mhNbt = NBTItem.get(mh);
                if (mhNbt.hasType() && "CONSUMABLE".equals(mhNbt.getString("MMOITEMS_ITEM_TYPE"))
                        && plugin.getConfig().getStringList("orb-ids").contains(mhNbt.getString("MMOITEMS_ITEM_ID"))) {
                    event.setCancelled(true);
                }
            }
            return;
        }
        if (event.getHand() != EquipmentSlot.HAND) return;

        ItemStack mainHand = player.getInventory().getItemInMainHand();
        if (mainHand.getType() == Material.AIR) return;

        NBTItem orbNbt = NBTItem.get(mainHand);
        if (!orbNbt.hasType()) return;

        String orbType = orbNbt.getString("MMOITEMS_ITEM_TYPE");
        String orbId = orbNbt.getString("MMOITEMS_ITEM_ID");

        if (debug()) {
            plugin.getLogger().info("[DEBUG] Right-click detectado por " + player.getName());
            plugin.getLogger().info("[DEBUG] Main hand: type=" + orbType + " id=" + orbId);
        }

        if (!"CONSUMABLE".equals(orbType)) return;

        List<String> orbIds = plugin.getConfig().getStringList("orb-ids");

        if (debug()) {
            plugin.getLogger().info("[DEBUG] Orb IDs na config: " + orbIds);
            plugin.getLogger().info("[DEBUG] ID do item: '" + orbId + "' contido? " + orbIds.contains(orbId));
        }

        if (!orbIds.contains(orbId)) return;

        event.setCancelled(true);

        if (!player.hasPermission("nemonicorb.use")) {
            send(player, "no-permission");
            return;
        }

        if (isOnCooldown(player)) {
            send(player, "cooldown");
            return;
        }

        ItemStack target = player.getInventory().getItemInOffHand();
        if (target.getType() == Material.AIR) {
            send(player, "no-target");
            return;
        }
        ItemStack targetBefore = target.clone();

        NBTItem targetNbt = NBTItem.get(target);
        boolean isMMOItem = targetNbt.hasType();

        // Itens vanilla tagueados pelo plugin (NEMONICORB_) nao sao MMOItems reais
        if (isMMOItem) {
            String targetMmoId = targetNbt.getString("MMOITEMS_ITEM_ID");
            if (targetMmoId != null && targetMmoId.startsWith("NEMONICORB_")) {
                isMMOItem = false;
            }
        }

        // Determinar categoria: MMOItem via config, ou vanilla via Material
        String category;
        if (isMMOItem) {
            String targetType = targetNbt.getString("MMOITEMS_ITEM_TYPE");
            category = resolveCategory(targetType);
            if (debug()) plugin.getLogger().info("[DEBUG] Target MMOItem: type=" + targetType + " cat=" + category);
        } else {
            category = resolveCategoryFromMaterial(target.getType());
            if (debug()) plugin.getLogger().info("[DEBUG] Target Vanilla: material=" + target.getType() + " cat=" + category);
        }

        int tier = getTier(targetNbt);

        if (category == null && !orbId.equals("PERGAMINHO_DE_IDENTIFICACAO")
                && !orbId.equals("PEDRA_CORROSIVA")) {
            send(player, "unsupported-type");
            return;
        }

        // Unicos: apenas identificacao, oleo de polimento e pedra corrosiva sao permitidos
        if (tier == 3 && !orbId.equals("PERGAMINHO_DE_IDENTIFICACAO")
                && !orbId.equals("OLEO_DE_POLIMENTO")
                && !orbId.equals("PEDRA_CORROSIVA")) {
            send(player, "is-unique");
            return;
        }

        if (debug()) plugin.getLogger().info("[DEBUG] Processando orb=" + orbId + " tier=" + tier
                + " cat=" + category + " isMMO=" + isMMOItem);

        // #15: bloqueio de uso do Pergaminho de Identificacao por nao-Mercadores.
        if ("PERGAMINHO_DE_IDENTIFICACAO".equals(orbId)
                && plugin.getConfig().getBoolean("identification-mercador-only", true)) {
            boolean isMercador = merchantListener != null && merchantListener.isMercador(player);
            if (!isMercador) {
                send(player, "identification-mercador-only");
                return;
            }
        }

        boolean success = switch (orbId) {
            case "PERGAMINHO_DE_IDENTIFICACAO" -> handleIdentify(player, target, targetNbt);
            case "OLEO_DE_POLIMENTO" -> handlePolish(player, target, targetNbt, isMMOItem);
            case "PEDRA_DE_ENCANTAMENTO" -> handleTransmute(player, target, targetNbt, category, tier, isMMOItem);
            case "PEDRA_DE_REFORCO" -> handleAugment(player, target, targetNbt, category, tier, isMMOItem);
            case "RUNA_NOBRE" -> handleRegal(player, target, targetNbt, category, tier, isMMOItem);
            case "PEDRA_DE_REFINAMENTO" -> handleAlchemy(player, target, targetNbt, category, tier, isMMOItem);
            case "RUNA_DE_PODER" -> handleExalt(player, target, targetNbt, category, tier, isMMOItem);
            case "MOEDA_DA_SORTE" -> handleChance(player, target, targetNbt, category, tier, isMMOItem);
            case "PEDRA_CORROSIVA" -> handleAnnul(player, target, targetNbt, tier, isMMOItem);
            default -> false;
        };

        if (success) {
            // Mantem o nivel do item coerente com o nivel de classe ao usar orbs.
            // (Pergaminho de identificacao segue a regra propria de revelacao/cap.)
            if (!"PERGAMINHO_DE_IDENTIFICACAO".equals(orbId)) {
                syncOffhandLevelToClass(player);
            }
            consumeOrb(player);
            setCooldown(player);
            spawnOrbParticles(player, orbId);
            try {
                ItemStack after = player.getInventory().getItemInOffHand();
                plugin.getModifierAuditService().logBeforeAfter("orb:" + orbId, player, targetBefore, after);
            } catch (Exception ignored) {}
            if (debug()) plugin.getLogger().info("[DEBUG] Orb " + orbId + " aplicada com sucesso por " + player.getName());
        } else {
            if (debug()) plugin.getLogger().info("[DEBUG] Orb " + orbId + " falhou para " + player.getName());
        }
    }

    // ══════════════════════════════════════════════
    // 1. PERGAMINHO DE IDENTIFICACAO — Revela mods ocultos
    // ══════════════════════════════════════════════
    private boolean handleIdentify(Player player, ItemStack target, NBTItem nbt) {
        if (isIdentified(nbt)) {
            send(player, "already-identified");
            return false;
        }

        int revealerLevel = getPlayerRevealLevel(player);
        int itemLevelBefore = getItemLevel(nbt);
        int cappedLevel = Math.max(1, Math.min(itemLevelBefore, revealerLevel));

        nbt.addTag(new ItemTag(NBT_IDENTIFIED, 1));
        nbt.addTag(new ItemTag(NBT_CRAFT_LEVEL, cappedLevel));
        ItemStack updated = nbt.toItem();
        updateItemDisplay(updated, NBTItem.get(updated));
        player.getInventory().setItemInOffHand(updated);

        if (debug() && cappedLevel != itemLevelBefore) {
            plugin.getLogger().info("[IDENTIFY-CAP] " + player.getName()
                    + " revelou item nivel " + itemLevelBefore
                    + " -> cap " + cappedLevel);
        }

        send(player, "identify-success");
        return true;
    }

    // ══════════════════════════════════════════════
    // 2. OLEO DE POLIMENTO — Melhora stats dos mods existentes + trava mods
    // ══════════════════════════════════════════════
    private boolean handlePolish(Player player, ItemStack target, NBTItem nbt, boolean isMMOItem) {
        if (!isIdentified(nbt)) {
            send(player, "not-identified");
            return false;
        }

        int maxPolish = plugin.getConfig().getInt("max-polish", 5);
        int currentPolish = nbt.hasTag(NBT_POLISH) ? nbt.getInteger(NBT_POLISH) : 0;

        if (currentPolish >= maxPolish) {
            send(player, "max-polish", "%max%", String.valueOf(maxPolish));
            return false;
        }

        List<ModifierEngine.RolledModifier> mods = readMods(nbt);
        if (mods.isEmpty()) {
            send(player, "no-mods-to-polish");
            return false;
        }

        // Boost each mod's stats by the polish percentage
        var polishSec = plugin.getConfig().getConfigurationSection("polish-percent");
        double defaultPct = plugin.getConfig().getDouble("polish-quality-boost", 5.0) / 100.0;

        List<ModifierEngine.RolledModifier> boostedMods = new ArrayList<>();
        for (var mod : mods) {
            LinkedHashMap<String, Double> boostedStats = new LinkedHashMap<>();
            for (var entry : mod.stats().entrySet()) {
                double pct;
                if (polishSec != null && polishSec.contains(entry.getKey())) {
                    pct = polishSec.getDouble(entry.getKey()) / 100.0;
                } else {
                    pct = defaultPct;
                }
                double oldVal = entry.getValue();
                double newVal;
                if (oldVal >= 0) {
                    newVal = Math.round(oldVal * (1.0 + pct) * 100.0) / 100.0;
                } else {
                    newVal = Math.round(oldVal * (1.0 - pct) * 100.0) / 100.0;
                }
                boostedStats.put(entry.getKey(), newVal);
            }
            boostedMods.add(new ModifierEngine.RolledModifier(mod.id(), mod.category(), boostedStats, mod.negative()));
        }

        ItemStack result;
        if (isMMOItem) {
            LiveMMOItem live = new LiveMMOItem(target);
            // Apply the difference (new - old) for each stat
            for (int i = 0; i < mods.size(); i++) {
                var oldMod = mods.get(i);
                var newMod = boostedMods.get(i);
                for (var entry : oldMod.stats().entrySet()) {
                    DoubleStat stat = engine.resolveStat(entry.getKey());
                    if (stat == null) continue;
                    double diff = newMod.stats().get(entry.getKey()) - entry.getValue();
                    var data = live.getData(stat);
                    double current = (data instanceof DoubleData dd) ? dd.getValue() : 0;
                    live.setData(stat, new DoubleData(Math.round((current + diff) * 100.0) / 100.0));
                }
            }
            ItemStack built = live.newBuilder().build();
            NBTItem builtNbt = NBTItem.get(built);
            preserveAllTags(nbt, builtNbt);
            builtNbt.addTag(new ItemTag(NBT_POLISH, currentPolish + 1));
            builtNbt.addTag(new ItemTag(NBT_POLISHED, 1));
            builtNbt.addTag(new ItemTag(NBT_MODS, gson.toJson(boostedMods)));
            result = builtNbt.toItem();
        } else {
            nbt.addTag(new ItemTag(NBT_POLISH, currentPolish + 1));
            nbt.addTag(new ItemTag(NBT_POLISHED, 1));
            nbt.addTag(new ItemTag(NBT_MODS, gson.toJson(boostedMods)));
            result = nbt.toItem();
        }

        updateItemDisplay(result, NBTItem.get(result));
        player.getInventory().setItemInOffHand(result);

        // Show boosted stats in chat
        StringBuilder details = new StringBuilder();
        for (int i = 0; i < mods.size(); i++) {
            var oldMod = mods.get(i);
            var newMod = boostedMods.get(i);
            for (var entry : oldMod.stats().entrySet()) {
                String statName = capitalize(entry.getKey().replace("-", " ").replace("_", " "));
                double oldVal = entry.getValue();
                double newVal = newMod.stats().get(entry.getKey());
                double diff = Math.round((newVal - oldVal) * 100.0) / 100.0;
                if (diff != 0) {
                    String color = diff > 0 ? "&a" : "&c";
                    String sign = diff > 0 ? "+" : "";
                    details.append("\n").append(cc("  " + color + statName + ": "
                            + String.format("%.1f", oldVal) + " -> " + String.format("%.1f", newVal)
                            + " (" + sign + String.format("%.1f", diff) + ")"));
                }
            }
        }

        send(player, "polish-success",
                "%count%", String.valueOf(currentPolish + 1),
                "%max%", String.valueOf(maxPolish));
        if (!details.isEmpty()) {
            player.sendMessage(cc("&b&lMelhorias aplicadas:") + details);
        }
        return true;
    }

    // ══════════════════════════════════════════════
    // 3. PEDRA DE ENCANTAMENTO — Comum → Magico (1-2 mods)
    // ══════════════════════════════════════════════
    private boolean handleTransmute(Player player, ItemStack target, NBTItem nbt,
                                    String category, int tier, boolean isMMOItem) {
        if (tier != 0) {
            send(player, "not-common");
            return false;
        }
        if (category == null) {
            send(player, "unsupported-type");
            return false;
        }

        int itemLevel = getPlayerClassLevel(player);
        int modCount = ThreadLocalRandom.current().nextInt(1, 3); // 1 ou 2
        List<ModifierEngine.RolledModifier> mods = engine.rollForTier(category, modCount, itemLevel, 2);

        if (debug()) plugin.getLogger().info("[DEBUG] Transmute: nivel=" + itemLevel + " rolou " + mods.size() + " mods para cat=" + category);

        if (mods.isEmpty()) {
            plugin.getLogger().warning("FALHA: Nenhum mod rolado para transmute! Verificar nemonicorp_orb_modifiers.yml");
            send(player, "no-mods");
            return false;
        }

        ItemStack result = buildModifiedItem(target, nbt, mods, isMMOItem);
        NBTItem resultNbt = NBTItem.get(result);
        resultNbt.addTag(new ItemTag(NBT_TIER, 1));
        resultNbt.addTag(new ItemTag(NBT_MODS, gson.toJson(mods)));
        resultNbt.addTag(new ItemTag(NBT_IDENTIFIED, 1));
        preservePolishTag(nbt, resultNbt);

        result = resultNbt.toItem();
        updateItemDisplay(result, NBTItem.get(result));
        player.getInventory().setItemInOffHand(result);

        send(player, "transmute-success", "%count%", String.valueOf(mods.size()));
        return true;
    }

    // ══════════════════════════════════════════════
    // 4. PEDRA DE REFORCO — Adiciona 1 mod a Magico (max 2)
    // ══════════════════════════════════════════════
    private boolean handleAugment(Player player, ItemStack target, NBTItem nbt,
                                  String category, int tier, boolean isMMOItem) {
        if (isPolished(nbt)) {
            send(player, "polished-locked");
            return false;
        }
        if (tier != 1) {
            send(player, "not-magic");
            return false;
        }
        if (category == null) {
            send(player, "unsupported-type");
            return false;
        }

        List<ModifierEngine.RolledModifier> mods = readMods(nbt);
        int maxMods = plugin.getConfig().getInt("tiers.1.max-mods", 2);
        if (mods.size() >= maxMods) {
            send(player, "mods-full");
            return false;
        }

        int itemLevel = getPlayerClassLevel(player);
        Set<String> used = new HashSet<>();
        Set<String> usedIds = new HashSet<>();
        for (var m : mods) {
            if (m.category() != null) used.add(m.category());
            usedIds.add(m.id());
        }

        if (debug()) plugin.getLogger().info("[DEBUG] Augment: nivel=" + itemLevel + " mods=" + mods.size()
                + "/" + maxMods + " used=" + used + " ids=" + usedIds);

        String groupId = "nemonicorp_" + category + "_positive";
        ModifierEngine.RolledModifier newMod = engine.rollFromGroup(
                groupId, itemLevel, used, usedIds, false, 1);

        // Fallback 1: categorias novas esgotadas — relaxa categoria, mantem dedup por ID.
        if (newMod == null && !used.isEmpty()) {
            if (debug()) {
                plugin.getLogger().info("[DEBUG] Augment fallback 1: sem categoria nova em "
                        + groupId + ", permitindo repeticao de categoria (dedup ID).");
            }
            newMod = engine.rollFromGroup(groupId, itemLevel,
                    Collections.emptySet(), usedIds, false, 1);
        }

        // Fallback 2: pool exausto de IDs unicos — aceita qualquer mod para nao travar.
        if (newMod == null) {
            if (debug()) {
                plugin.getLogger().warning("[DEBUG] Augment fallback 2: pool exausto em "
                        + groupId + ", rolando sem dedup.");
            }
            newMod = engine.rollFromGroup(groupId, itemLevel,
                    Collections.emptySet(), Collections.emptySet(), false, 1);
        }

        if (newMod == null) {
            send(player, "no-mods");
            return false;
        }

        if (debug()) plugin.getLogger().info("[DEBUG] Augment: novo mod=" + newMod.id());

        List<ModifierEngine.RolledModifier> updated = new ArrayList<>(mods);
        updated.add(newMod);

        ItemStack result = buildModifiedItem(target, nbt, List.of(newMod), isMMOItem);
        NBTItem resultNbt = NBTItem.get(result);
        resultNbt.addTag(new ItemTag(NBT_TIER, 1));
        resultNbt.addTag(new ItemTag(NBT_MODS, gson.toJson(updated)));
        resultNbt.addTag(new ItemTag(NBT_IDENTIFIED, 1));
        preservePolishTag(nbt, resultNbt);

        result = resultNbt.toItem();
        updateItemDisplay(result, NBTItem.get(result));
        player.getInventory().setItemInOffHand(result);

        send(player, "augment-success");
        return true;
    }

    // ══════════════════════════════════════════════
    // 5. RUNA NOBRE — Magico (2 mods) → Raro (+1 mod)
    // ══════════════════════════════════════════════
    private boolean handleRegal(Player player, ItemStack target, NBTItem nbt,
                                String category, int tier, boolean isMMOItem) {
        if (tier != 1) {
            send(player, "not-magic");
            return false;
        }
        if (category == null) {
            send(player, "unsupported-type");
            return false;
        }

        List<ModifierEngine.RolledModifier> mods = readMods(nbt);
        if (mods.size() < 2) {
            send(player, "needs-2-mods");
            return false;
        }

        int itemLevel = getPlayerClassLevel(player);
        Set<String> used = new HashSet<>();
        Set<String> usedIds = new HashSet<>();
        for (var m : mods) {
            if (m.category() != null) used.add(m.category());
            usedIds.add(m.id());
        }

        ModifierEngine.RolledModifier newMod = engine.rollFromGroup(
                "nemonicorp_" + category + "_positive", itemLevel, used, usedIds, false, 2);

        // Fallback 1: categorias novas esgotadas — relaxa categoria, mantem dedup por ID.
        if (newMod == null && !used.isEmpty()) {
            if (debug()) {
                plugin.getLogger().info("[DEBUG] Fallback 1: sem categoria nova em "
                        + "nemonicorp_" + category + "_positive"
                        + ", permitindo repeticao de categoria (dedup ID).");
            }
            newMod = engine.rollFromGroup(
                    "nemonicorp_" + category + "_positive", itemLevel,
                    Collections.emptySet(), usedIds, false, 2);
        }

        // Fallback 2: pool exausto de IDs — aceita qualquer mod para nao travar em X/6.
        if (newMod == null) {
            if (debug()) {
                plugin.getLogger().warning("[DEBUG] Fallback 2: pool exausto em "
                        + "nemonicorp_" + category + "_positive"
                        + ", rolando sem dedup.");
            }
            newMod = engine.rollFromGroup(
                    "nemonicorp_" + category + "_positive", itemLevel,
                    Collections.emptySet(), Collections.emptySet(), false, 2);
        }

        if (newMod == null) {
            send(player, "no-mods");
            return false;
        }

        List<ModifierEngine.RolledModifier> updated = new ArrayList<>(mods);
        updated.add(newMod);

        ItemStack result = buildModifiedItem(target, nbt, List.of(newMod), isMMOItem);
        NBTItem resultNbt = NBTItem.get(result);
        resultNbt.addTag(new ItemTag(NBT_TIER, 2)); // Promovido a Raro
        resultNbt.addTag(new ItemTag(NBT_MODS, gson.toJson(updated)));
        resultNbt.addTag(new ItemTag(NBT_IDENTIFIED, 1));
        preservePolishTag(nbt, resultNbt);

        result = resultNbt.toItem();
        updateItemDisplay(result, NBTItem.get(result));
        player.getInventory().setItemInOffHand(result);

        send(player, "regal-success");
        return true;
    }

    // ══════════════════════════════════════════════
    // 6. PEDRA DE REFINAMENTO — Comum → Raro direto (3-6 mods)
    // ══════════════════════════════════════════════
    private boolean handleAlchemy(Player player, ItemStack target, NBTItem nbt,
                                  String category, int tier, boolean isMMOItem) {
        if (tier != 0) {
            send(player, "not-common");
            return false;
        }
        if (category == null) {
            send(player, "unsupported-type");
            return false;
        }

        int itemLevel = getPlayerClassLevel(player);
        int modCount = ThreadLocalRandom.current().nextInt(3, 7); // 3 a 6
        List<ModifierEngine.RolledModifier> mods = engine.rollForTier(category, modCount, itemLevel, 6);

        if (mods.isEmpty()) {
            plugin.getLogger().warning("FALHA: Nenhum mod rolado para alchemy! Verificar nemonicorp_orb_modifiers.yml");
            send(player, "no-mods");
            return false;
        }

        ItemStack result = buildModifiedItem(target, nbt, mods, isMMOItem);
        NBTItem resultNbt = NBTItem.get(result);
        resultNbt.addTag(new ItemTag(NBT_TIER, 2));
        resultNbt.addTag(new ItemTag(NBT_MODS, gson.toJson(mods)));
        resultNbt.addTag(new ItemTag(NBT_IDENTIFIED, 1));
        preservePolishTag(nbt, resultNbt);

        result = resultNbt.toItem();
        updateItemDisplay(result, NBTItem.get(result));
        player.getInventory().setItemInOffHand(result);

        send(player, "alchemy-success", "%count%", String.valueOf(mods.size()));
        return true;
    }

    // ══════════════════════════════════════════════
    // 7. RUNA DE PODER — Adiciona 1 mod a Raro (max 6)
    // ══════════════════════════════════════════════
    private boolean handleExalt(Player player, ItemStack target, NBTItem nbt,
                                String category, int tier, boolean isMMOItem) {
        if (isPolished(nbt)) {
            send(player, "polished-locked");
            return false;
        }
        if (tier != 2) {
            send(player, "not-rare");
            return false;
        }
        if (category == null) {
            send(player, "unsupported-type");
            return false;
        }

        List<ModifierEngine.RolledModifier> mods = readMods(nbt);
        int maxMods = plugin.getConfig().getInt("tiers.2.max-mods", 6);

        if (mods.size() >= maxMods) {
            send(player, "mods-full");
            return false;
        }

        int itemLevel = getPlayerClassLevel(player);
        Set<String> used = new HashSet<>();
        Set<String> usedIds = new HashSet<>();
        for (var m : mods) {
            if (m.category() != null) used.add(m.category());
            usedIds.add(m.id());
        }

        ModifierEngine.RolledModifier newMod = engine.rollFromGroup(
                "nemonicorp_" + category + "_positive", itemLevel, used, usedIds, false, 2);

        // Fallback 1: categorias novas esgotadas — relaxa categoria, mantem dedup por ID.
        if (newMod == null && !used.isEmpty()) {
            if (debug()) {
                plugin.getLogger().info("[DEBUG] Fallback 1: sem categoria nova em "
                        + "nemonicorp_" + category + "_positive"
                        + ", permitindo repeticao de categoria (dedup ID).");
            }
            newMod = engine.rollFromGroup(
                    "nemonicorp_" + category + "_positive", itemLevel,
                    Collections.emptySet(), usedIds, false, 2);
        }

        // Fallback 2: pool exausto de IDs — aceita qualquer mod para nao travar em X/6.
        if (newMod == null) {
            if (debug()) {
                plugin.getLogger().warning("[DEBUG] Fallback 2: pool exausto em "
                        + "nemonicorp_" + category + "_positive"
                        + ", rolando sem dedup.");
            }
            newMod = engine.rollFromGroup(
                    "nemonicorp_" + category + "_positive", itemLevel,
                    Collections.emptySet(), Collections.emptySet(), false, 2);
        }

        if (newMod == null) {
            send(player, "no-mods");
            return false;
        }

        List<ModifierEngine.RolledModifier> updated = new ArrayList<>(mods);
        updated.add(newMod);

        ItemStack result = buildModifiedItem(target, nbt, List.of(newMod), isMMOItem);
        NBTItem resultNbt = NBTItem.get(result);
        preserveAllTags(nbt, resultNbt);
        resultNbt.addTag(new ItemTag(NBT_MODS, gson.toJson(updated)));

        result = resultNbt.toItem();
        updateItemDisplay(result, NBTItem.get(result));
        player.getInventory().setItemInOffHand(result);

        send(player, "exalt-success", "%modifier%", newMod.id());
        return true;
    }

    // ══════════════════════════════════════════════
    // 8. MOEDA DA SORTE — Comum → Tier aleatorio
    // ══════════════════════════════════════════════
    private boolean handleChance(Player player, ItemStack target, NBTItem nbt,
                                 String category, int tier, boolean isMMOItem) {
        if (tier != 0) {
            send(player, "not-common");
            return false;
        }
        if (category == null) {
            send(player, "unsupported-type");
            return false;
        }

        int magicChance = plugin.getConfig().getInt("chance-orb.magic", 70);
        int rareChance = plugin.getConfig().getInt("chance-orb.rare", 25);

        int roll = ThreadLocalRandom.current().nextInt(100);
        int newTier;
        int modCount;
        int maxMods;

        if (roll < magicChance) {
            newTier = 1;
            modCount = ThreadLocalRandom.current().nextInt(1, 3);
            maxMods = 2;
        } else if (roll < magicChance + rareChance) {
            newTier = 2;
            modCount = ThreadLocalRandom.current().nextInt(3, 7);
            maxMods = 6;
        } else {
            newTier = 3; // Unico!
            modCount = 6;
            maxMods = 6;
        }

        int itemLevel = getPlayerClassLevel(player);
        List<ModifierEngine.RolledModifier> mods;
        if (newTier == 3) {
            // Unicos: 6 mods, valores dobrados, 2 mods buffados x2
            mods = engine.rollForUnique(category, itemLevel);
        } else {
            mods = engine.rollForTier(category, modCount, itemLevel, maxMods);
        }

        if (mods.isEmpty()) {
            plugin.getLogger().warning("FALHA: Nenhum mod rolado para chance! Verificar nemonicorp_orb_modifiers.yml");
            send(player, "no-mods");
            return false;
        }

        ItemStack result = buildModifiedItem(target, nbt, mods, isMMOItem);
        NBTItem resultNbt = NBTItem.get(result);
        resultNbt.addTag(new ItemTag(NBT_TIER, newTier));
        resultNbt.addTag(new ItemTag(NBT_MODS, gson.toJson(mods)));
        resultNbt.addTag(new ItemTag(NBT_IDENTIFIED, 1));
        preservePolishTag(nbt, resultNbt);

        result = resultNbt.toItem();
        updateItemDisplay(result, NBTItem.get(result));
        player.getInventory().setItemInOffHand(result);

        String tierName = getTierName(newTier);
        send(player, "chance-success", "%tier%", tierName);
        return true;
    }

    // ══════════════════════════════════════════════
    // 9. PEDRA CORROSIVA — Remove 1 mod aleatorio
    // ══════════════════════════════════════════════
    private boolean handleAnnul(Player player, ItemStack target, NBTItem nbt, int tier, boolean isMMOItem) {
        if (isPolished(nbt)) {
            send(player, "polished-locked");
            return false;
        }
        if (tier != 1 && tier != 2 && tier != 3) {
            send(player, "not-magic-or-rare");
            return false;
        }

        List<ModifierEngine.RolledModifier> mods = readMods(nbt);
        if (mods.isEmpty()) {
            send(player, "no-mods");
            return false;
        }

        int idx = ThreadLocalRandom.current().nextInt(mods.size());
        ModifierEngine.RolledModifier removed = mods.get(idx);

        List<ModifierEngine.RolledModifier> remaining = new ArrayList<>(mods);
        remaining.remove(idx);

        ItemStack result;
        if (isMMOItem) {
            LiveMMOItem live = new LiveMMOItem(target);
            removeModStats(live, removed);
            ItemStack built = live.newBuilder().build();
            NBTItem builtNbt = NBTItem.get(built);
            preserveAllTags(nbt, builtNbt);
            builtNbt.addTag(new ItemTag(NBT_MODS, gson.toJson(remaining)));
            result = builtNbt.toItem();
        } else {
            nbt.addTag(new ItemTag(NBT_MODS, gson.toJson(remaining)));
            result = nbt.toItem();
        }

        // Se o mod removido tinha enchant-stats (fortune/unbreaking/knockback),
        // decrementar os niveis correspondentes no item.
        removeEnchantMod(result, removed);

        updateItemDisplay(result, NBTItem.get(result));
        player.getInventory().setItemInOffHand(result);

        send(player, "annul-success", "%modifier%", removed.id());
        return true;
    }

    // ══════════════════════════════════════════════
    // Construir item modificado (MMOItem ou vanilla)
    // ══════════════════════════════════════════════

    /**
     * Aplica mods ao item. Para MMOItems, usa LiveMMOItem. Para vanilla, retorna item com NBT apenas.
     */
    // Mapa de stat-key do YAML → Enchantment vanilla (mods que aplicam encantamentos reais).
    // Ao aplicar, o tier do mod (valor double arredondado) e SOMADO ao nivel ja existente
    // no item, com cap em 5. Isso garante que Fortune I no item base + mod fortune:2 = Fortune III.
    private static final Map<String, Enchantment> STAT_TO_ENCHANTMENT = Map.of(
            "fortune", Enchantment.FORTUNE,
            "unbreaking", Enchantment.UNBREAKING,
            "knockback", Enchantment.KNOCKBACK
    );

    // Mapa de stat-key do YAML → Attribute do Bukkit (para aplicar stats reais em itens vanilla)
    private static final Map<String, String> STAT_TO_ATTRIBUTE = Map.ofEntries(
            Map.entry("attack-damage", "attack_damage"),
            Map.entry("attack-speed", "attack_speed"),
            Map.entry("armor", "armor"),
            Map.entry("armor-toughness", "armor_toughness"),
            Map.entry("max-health", "max_health"),
            Map.entry("max-absorption", "max_absorption"),
            Map.entry("knockback-resistance", "knockback_resistance"),
            Map.entry("explosion-knockback-resistance", "explosion_knockback_resistance"),
            Map.entry("mining-efficiency", "mining_efficiency"),
            Map.entry("sweeping-damage-ratio", "sweeping_damage_ratio"),
            Map.entry("fall-damage-multiplier", "fall_damage_multiplier"),
            // Danos elementais somam ao dano real do item
            Map.entry("fire-damage", "attack_damage"),
            Map.entry("ice-damage", "attack_damage"),
            Map.entry("lightning-damage", "attack_damage"),
            Map.entry("earth-damage", "attack_damage"),
            Map.entry("water-damage", "attack_damage"),
            Map.entry("wind-damage", "attack_damage"),
            // Dano fisico/magico/weapon/projectile somam ao dano real
            Map.entry("physical-damage", "attack_damage"),
            Map.entry("magic-damage", "attack_damage"),
            Map.entry("weapon-damage", "attack_damage"),
            Map.entry("projectile-damage", "attack_damage"),
            // Luck mapeia para luck do vanilla
            Map.entry("luck", "luck")
    );

    /** Gera um prefixo unico para NamespacedKeys de atributos de um item,
     *  garantindo que itens diferentes nao colidam (stackam corretamente). */
    private static String uniqueModPrefix() {
        return Long.toHexString(ThreadLocalRandom.current().nextLong()).substring(0, 8);
    }

    private ItemStack buildModifiedItem(ItemStack target, NBTItem nbt,
                                        List<ModifierEngine.RolledModifier> newMods, boolean isMMOItem) {
        if (isMMOItem) {
            try {
                LiveMMOItem live = new LiveMMOItem(target);
                if (live.getType() == null) {
                    if (debug()) {
                        plugin.getLogger().warning("[DEBUG] buildModifiedItem: MMOItem invalido (Type null). "
                                + "Aplicando fallback vanilla para " + target.getType());
                    }
                    return buildModifiedItem(target, nbt, newMods, false);
                }
                int applied = 0;
                int skipped = 0;
                for (var mod : newMods) {
                    for (var entry : mod.stats().entrySet()) {
                        DoubleStat stat = engine.resolveStat(entry.getKey());
                        if (stat == null) {
                            skipped++;
                            if (debug()) plugin.getLogger().warning("[DEBUG] Stat nao resolvido: '"
                                    + entry.getKey() + "' no mod " + mod.id());
                            continue;
                        }
                        var data = live.getData(stat);
                        double current = (data instanceof DoubleData dd) ? dd.getValue() : 0;
                        live.setData(stat, new DoubleData(current + entry.getValue()));
                        applied++;
                        if (debug()) plugin.getLogger().info("[DEBUG] Stat aplicado: " + entry.getKey()
                                + " = " + current + " + " + entry.getValue() + " = " + (current + entry.getValue()));
                    }
                }
                if (debug()) plugin.getLogger().info("[DEBUG] buildModifiedItem: " + applied + " stats aplicados, " + skipped + " pulados");
                ItemStack built = live.newBuilder().build();
                applyEnchantMods(built, newMods);
                return built;
            } catch (Exception ex) {
                plugin.getLogger().warning("[OrbListener] Falha ao aplicar mods em MMOItem, fallback vanilla: " + ex.getMessage());
                return buildModifiedItem(target, nbt, newMods, false);
            }
        } else {
            // Vanilla: aplicar stats reais via AttributeModifier do Bukkit
            ItemStack result = nbt.toItem();
            ItemMeta meta = result.getItemMeta();
            if (meta == null) return result;

            // Preservar atributos default antes de adicionar customizados
            if (!meta.hasAttributeModifiers()) {
                preserveDefaultAttributes(result, meta);
            }

            String category = resolveCategoryFromMaterial(result.getType());
            EquipmentSlotGroup slotGroup;
            if ("weapon".equals(category) || "tool".equals(category)) {
                slotGroup = EquipmentSlotGroup.MAINHAND;
            } else if ("shield".equals(category)) {
                slotGroup = EquipmentSlotGroup.OFFHAND;
            } else {
                slotGroup = EquipmentSlotGroup.ARMOR;
            }

            String prefix = uniqueModPrefix();
            int modIdx = 0;
            for (var mod : newMods) {
                for (var entry : mod.stats().entrySet()) {
                    String attrKey = STAT_TO_ATTRIBUTE.get(entry.getKey());
                    if (attrKey == null) {
                        if (debug()) plugin.getLogger().info("[DEBUG] Vanilla: stat '" + entry.getKey()
                                + "' nao tem Attribute equivalente, apenas lore");
                        continue;
                    }
                    Attribute attr = Registry.ATTRIBUTE.get(NamespacedKey.minecraft(attrKey));
                    if (attr == null) attr = Registry.ATTRIBUTE.get(NamespacedKey.minecraft("generic." + attrKey));
                    if (attr == null) {
                        if (debug()) plugin.getLogger().warning("[DEBUG] Vanilla: Attribute nao encontrado: " + attrKey);
                        continue;
                    }

                    NamespacedKey key = new NamespacedKey(plugin, "orbmod_" + prefix + "_" + modIdx);
                    AttributeModifier modifier = new AttributeModifier(
                            key, entry.getValue(), AttributeModifier.Operation.ADD_NUMBER, slotGroup);
                    meta.addAttributeModifier(attr, modifier);
                    modIdx++;

                    if (debug()) plugin.getLogger().info("[DEBUG] Vanilla stat aplicado: "
                            + attrKey + " +" + String.format("%.2f", entry.getValue()));
                }
            }

            result.setItemMeta(meta);
            applyEnchantMods(result, newMods);
            if (debug()) plugin.getLogger().info("[DEBUG] buildModifiedItem: vanilla, " + modIdx + " attrs aplicados");
            return result;
        }
    }

    /**
     * Aplica enchant-stats dos mods (fortune/unbreaking/knockback) somando ao
     * nivel ja existente no item. Cap em 5. Resolve issue #8 onde o merge nao
     * estava acontecendo e mods sobrescreviam o enchant base.
     */
    private void applyEnchantMods(ItemStack item, List<ModifierEngine.RolledModifier> newMods) {
        if (item == null || newMods == null || newMods.isEmpty()) return;
        // Sumarizar tiers de cada enchant-mod
        Map<Enchantment, Integer> sumByEnch = new LinkedHashMap<>();
        for (var mod : newMods) {
            for (var entry : mod.stats().entrySet()) {
                Enchantment ench = STAT_TO_ENCHANTMENT.get(entry.getKey());
                if (ench == null) continue;
                int tier = Math.max(1, (int) Math.round(entry.getValue()));
                sumByEnch.merge(ench, tier, Integer::sum);
            }
        }
        if (sumByEnch.isEmpty()) return;
        for (var e : sumByEnch.entrySet()) {
            int existing = item.getEnchantmentLevel(e.getKey());
            int newLevel = Math.min(5, existing + e.getValue());
            item.addUnsafeEnchantment(e.getKey(), newLevel);
            if (debug()) plugin.getLogger().info("[DEBUG-ENCH] " + e.getKey().getKey().getKey()
                    + ": " + existing + " + " + e.getValue() + " = " + newLevel);
        }
    }

    /** Decrementa enchant-stats do item ao remover um mod via Pedra Corrosiva. */
    private void removeEnchantMod(ItemStack item, ModifierEngine.RolledModifier mod) {
        if (item == null || mod == null) return;
        for (var entry : mod.stats().entrySet()) {
            Enchantment ench = STAT_TO_ENCHANTMENT.get(entry.getKey());
            if (ench == null) continue;
            int tier = Math.max(1, (int) Math.round(entry.getValue()));
            int current = item.getEnchantmentLevel(ench);
            int newLevel = current - tier;
            if (newLevel <= 0) item.removeEnchantment(ench);
            else item.addUnsafeEnchantment(ench, newLevel);
        }
    }

    private void removeModStats(LiveMMOItem live, ModifierEngine.RolledModifier mod) {
        for (var entry : mod.stats().entrySet()) {
            DoubleStat stat = engine.resolveStat(entry.getKey());
            if (stat == null) continue;
            var data = live.getData(stat);
            double current = (data instanceof DoubleData dd) ? dd.getValue() : 0;
            live.setData(stat, new DoubleData(current - entry.getValue()));
        }
    }

    // ══════════════════════════════════════════════
    // Display / Tooltip — Estilo PoE2
    // ══════════════════════════════════════════════

    public void updateItemDisplay(ItemStack item, NBTItem nbt) {
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;

        boolean isMMOItem = nbt.hasType();
        int tier = getTier(nbt);
        boolean identified = isIdentified(nbt);
        List<ModifierEngine.RolledModifier> mods = readMods(nbt);
        int polish = nbt.hasTag(NBT_POLISH) ? nbt.getInteger(NBT_POLISH) : 0;
        int maxPolish = plugin.getConfig().getInt("max-polish", 5);
        boolean polished = nbt.hasTag(NBT_POLISHED) && nbt.getInteger(NBT_POLISHED) == 1;

        String baseName = "";
        if (meta.hasItemName()) {
            baseName = ChatColor.stripColor(LegacyComponentSerializer.legacySection()
                    .serialize(meta.itemName()));
        } else if (meta.hasDisplayName()) {
            baseName = ChatColor.stripColor(meta.getDisplayName());
        }
        baseName = baseName.replaceAll("[\\uA000-\\uA00F\\uEA60-\\uEA75\\uF800-\\uF80F\\uFEFF]", "").trim();
        if (baseName.isEmpty() || baseName.equals("??? Item Desconhecido")) {
            baseName = formatMaterialName(item.getType());
        }
        String tierColor = getTierColor(tier);

        if (!identified) {
            meta.itemName(toComponent("&7&l??? Item Desconhecido"));
            List<String> lore = new ArrayList<>();
            lore.add(cc("&7━━━━━━━━━━━━━━━━━━━━━"));
            lore.add(cc("&8  Tipo: &7████████"));
            lore.add(cc("&8  Tier: &7████████"));
            lore.add(cc(""));
            lore.add(cc("&8  ██ &7+██ ████████████"));
            lore.add(cc("&8  ██ &7+██ ████████████"));
            lore.add(cc("&8  ██ &7+██ ████████████"));
            lore.add(cc(""));
            lore.add(cc("&c  [Item Nao Identificado]"));
            lore.add(cc("&7  Use um Pergaminho de"));
            lore.add(cc("&7  Identificacao para revelar."));
            lore.add(cc("&7━━━━━━━━━━━━━━━━━━━━━"));
            meta.setLore(lore);
            hideAllVanilla(meta);
            item.setItemMeta(meta);
            hideAllVanillaOnItem(item);
            return;
        }

        // ── Para MMOItems reais (nao itens vanilla tagueados pelo plugin): usar display MMOItems ──
        String mmoItemId = nbt.hasTag("MMOITEMS_ITEM_ID") ? nbt.getString("MMOITEMS_ITEM_ID") : "";
        boolean isOurTaggedItem = mmoItemId != null && mmoItemId.startsWith("NEMONICORB_");
        if (isMMOItem && !isOurTaggedItem) {
            try {
                LiveMMOItem live = new LiveMMOItem(item);
                if (live.getType() != null) {
                    updateMMOItemDisplay(item, nbt, tier, mods, polish, maxPolish, polished, baseName, tierColor);
                    return;
                }
                if (debug()) plugin.getLogger().warning("[DEBUG] updateItemDisplay: MMOItem com Type nulo, usando display vanilla.");
            } catch (Exception ex) {
                plugin.getLogger().warning("[OrbListener] Falha no display MMOItem, fallback vanilla: " + ex.getMessage());
            }
        }

        // ── Para Vanilla: gerar lore customizada ──
        meta.itemName(toComponent(tierColor + "&l" + baseName));

        List<String> lore = new ArrayList<>();

        String border = tierColor + "━━━━━━━━━━━━━━━━━━━━━";
        lore.add(cc(border));

        // Encantamentos Vanilla
        Map<Enchantment, Integer> enchants = item.getEnchantments();
        if (!enchants.isEmpty()) {
            for (var entry : enchants.entrySet()) {
                String enchName = formatEnchantmentName(entry.getKey());
                int lvl = entry.getValue();
                lore.add(cc("&f" + enchName + " " + toRoman(lvl)));
            }
            lore.add(cc(""));
        }

        // Atributos Vanilla
        List<String> attrLines = buildAttributeLines(item);
        if (!attrLines.isEmpty()) {
            for (String line : attrLines) {
                lore.add(cc("&f" + ChatColor.stripColor(cc(line))));
            }
            lore.add(cc(""));
        }

        // Modificadores (Sistema de Orbs)
        appendModifierLines(lore, mods, tier, polish, maxPolish, polished, tierColor);

        // DPH (apenas para armas tier >= 1)
        double dps = DpsCalculator.calculate(item, nbt, mods, tier, false, engine);
        if (debug()) plugin.getLogger().info("[DEBUG-DPH] vanilla tier=" + tier
                + " material=" + item.getType().name() + " dps=" + dps);
        if (dps >= 0) {
            lore.add(cc(""));
            lore.add(cc("  &6\u2694 DPH: &f" + String.format("%.1f", dps)));
        }

        // Nivel e Tier
        int itemLevel = getItemLevel(nbt);
        lore.add(cc("  &7Nivel: &f" + itemLevel));
        lore.add(cc("  &7Tier: " + tierColor + "&l" + getTierName(tier).toUpperCase()));

        // Bloco Forja (persistente via NBT_FORGE_*)
        appendForgeLines(lore, nbt);

        lore.add(cc(border));

        meta.setLore(lore);
        hideAllVanilla(meta);

        // Re-aplicar stats dos mods como AttributeModifiers reais (somente se tem mods)
        if (!mods.isEmpty()) {
            reapplyVanillaModStats(item, meta, mods);
        }

        item.setItemMeta(meta);
        hideAllVanillaOnItem(item);

        // Salvar DPH no NBT (apos setItemMeta, usando NBTItem para preservar todas as tags)
        if (dps >= 0) {
            NBTItem dpsNbt = NBTItem.get(item);
            dpsNbt.addTag(new io.lumine.mythic.lib.api.item.ItemTag(NBT_DPS, dps));
            ItemStack withDps = dpsNbt.toItem();
            item.setType(withDps.getType());
            item.setAmount(withDps.getAmount());
            item.setItemMeta(withDps.getItemMeta());
        }
    }

    /**
     * Renderiza bloco "Forjado" na lore vanilla a partir dos NBTs de forja.
     * Garante que a info da forja nao desaparece em rebuilds posteriores.
     */
    private void appendForgeLines(List<String> lore, NBTItem nbt) {
        if (!nbt.hasTag(NBT_FORGE_QUALITY)) return;
        String quality = nbt.getString(NBT_FORGE_QUALITY);
        if (quality == null || quality.isEmpty()) return;
        String smith = nbt.hasTag(NBT_FORGE_SMITH) ? nbt.getString(NBT_FORGE_SMITH) : "";
        double bonus = nbt.hasTag(NBT_FORGE_BONUS_PCT) ? nbt.getDouble(NBT_FORGE_BONUS_PCT) : 0.0;
        String qColor = forgeQualityColor(quality);
        lore.add(cc("  " + qColor + "Forjado: &l" + quality + "&r" + qColor
                + " (+" + String.format("%.1f", bonus) + "%)"));
        if (!smith.isEmpty()) {
            String[] parts = smith.split("\\|");
            String smithName = parts[0];
            String smithLvl = parts.length > 1 ? parts[1] : "?";
            lore.add(cc("  &8Ferreiro: " + smithName + " (Nv." + smithLvl + ")"));
        }
    }

    private String forgeQualityColor(String quality) {
        if (quality == null) return "&7";
        return switch (quality) {
            case "Obra-Prima" -> "&6";
            case "Excelente" -> "&a";
            case "Bom" -> "&f";
            default -> "&7";
        };
    }

    /**
     * Para itens MMOItems: reconstruir via LiveMMOItem com tier e mods.
     */
    private void updateMMOItemDisplay(ItemStack item, NBTItem nbt, int tier,
                                       List<ModifierEngine.RolledModifier> mods, int polish, int maxPolish,
                                       boolean polished, String baseName, String tierColor) {
        // Montar texto de lore com modificadores para injetar no MMOItem via #lore#
        StringBuilder loreText = new StringBuilder();

        int maxMods = getMaxMods(tier);
        if (maxMods > 0 || !mods.isEmpty()) {
            String modLabel = polished
                    ? "&d&lModificadores &8(&f" + mods.size() + "&7/&f" + maxMods + "&8) &cTravado"
                    : "&d&lModificadores &8(&f" + mods.size() + "&7/&f" + maxMods + "&8)";
            loreText.append(modLabel).append("\n");
            for (var mod : mods) {
                for (var entry : mod.stats().entrySet()) {
                    String line = formatModifierLine(entry.getKey(), entry.getValue());
                    if (line != null) loreText.append(line).append("\n");
                }
            }
        }

        if (polish > 0) {
            loreText.append("&b Polimento: &f").append(polish).append("&7/&f").append(maxPolish).append("\n");
        }

        // DPH (apenas para armas tier >= 1)
        double dps = DpsCalculator.calculate(item, nbt, mods, tier, true, engine);
        if (dps >= 0) {
            loreText.append("\n&6\u2694 DPH: &f").append(String.format("%.1f", dps)).append("\n");
        }

        // Nivel do item
        int itemLevel = getItemLevel(nbt);
        loreText.append("&7Nivel: &f").append(itemLevel).append("\n");

        // Aplicar lore-text e tier via LiveMMOItem
        LiveMMOItem live = new LiveMMOItem(item);

        // Setar o tier do MMOItems para sincronizar com o tier do OrbPlugin
        String mmoTierName = switch (tier) {
            case 1 -> "MAGICAL";
            case 2 -> "RARE";
            case 3 -> "UNIQUE";
            default -> "COMMON";
        };
        live.setData(ItemStats.TIER, new StringData(mmoTierName));

        // Re-aplicar os modifier stats ao LiveMMOItem (sem isso o rebuild reseta para stats base)
        for (var mod : mods) {
            for (var entry : mod.stats().entrySet()) {
                DoubleStat stat = engine.resolveStat(entry.getKey());
                if (stat == null) continue;
                var data = live.getData(stat);
                double current = (data instanceof DoubleData dd) ? dd.getValue() : 0;
                live.setData(stat, new DoubleData(current + entry.getValue()));
            }
        }

        // Setar lore text com modificadores
        if (loreText.length() > 0) {
            String loreStr = loreText.toString().trim();
            live.setData(ItemStats.LORE, new StringListData(
                    java.util.Arrays.asList(loreStr.split("\n"))));
        }

        // Rebuild item (regenera lore usando lore-format.yml + stats.yml do MMOItems)
        ItemStack rebuilt = live.newBuilder().build();

        // Preservar tags customizadas do OrbPlugin
        NBTItem rebuiltNbt = NBTItem.get(rebuilt);
        preserveAllTags(nbt, rebuiltNbt);
        rebuilt = rebuiltNbt.toItem();

        // Setar nome e esconder TUDO vanilla
        ItemMeta rebuiltMeta = rebuilt.getItemMeta();
        if (rebuiltMeta != null) {
            rebuiltMeta.itemName(toComponent(tierColor + "&l" + baseName));
            hideAllVanilla(rebuiltMeta);

            // Limpar caracteres residuais de tooltip de TODAS as linhas de lore
            if (rebuiltMeta.hasLore()) {
                List<String> cleanLore = new ArrayList<>();
                for (String line : rebuiltMeta.getLore()) {
                    cleanLore.add(line.replaceAll(TOOLTIP_CHARS_REGEX, ""));
                }
                rebuiltMeta.setLore(cleanLore);
            }

            rebuilt.setItemMeta(rebuiltMeta);
        }

        // Copiar dados do item reconstruido de volta ao item original
        item.setType(rebuilt.getType());
        item.setAmount(rebuilt.getAmount());
        item.setItemMeta(rebuilt.getItemMeta());
        hideAllVanillaOnItem(item);

        if (debug()) plugin.getLogger().info("[DEBUG] Display MMOItem atualizado: nome='" + baseName + "' mods=" + mods.size());
    }

    /**
     * Re-aplica stats dos modificadores como AttributeModifiers reais em itens vanilla.
     * Remove orbmod_ antigos e re-adiciona para manter sincronizado.
     */
    private void reapplyVanillaModStats(ItemStack item, ItemMeta meta,
                                         List<ModifierEngine.RolledModifier> mods) {
        // Preservar atributos default antes de adicionar customizados
        if (!meta.hasAttributeModifiers()) {
            preserveDefaultAttributes(item, meta);
        }

        // Remover orbmod_ antigos para evitar acumulo
        if (meta.hasAttributeModifiers()) {
            for (Attribute attr : Attribute.values()) {
                var existing = meta.getAttributeModifiers(attr);
                if (existing == null) continue;
                for (AttributeModifier mod : existing) {
                    if (mod.getKey().getKey().startsWith("orbmod_")) {
                        meta.removeAttributeModifier(attr, mod);
                    }
                }
            }
        }

        String category = resolveCategoryFromMaterial(item.getType());
        EquipmentSlotGroup slotGroup;
        if ("weapon".equals(category) || "tool".equals(category)) {
            slotGroup = EquipmentSlotGroup.MAINHAND;
        } else if ("shield".equals(category)) {
            slotGroup = EquipmentSlotGroup.OFFHAND;
        } else {
            slotGroup = EquipmentSlotGroup.ARMOR;
        }

        String prefix = uniqueModPrefix();
        int modIdx = 0;
        for (var mod : mods) {
            for (var entry : mod.stats().entrySet()) {
                String attrKey = STAT_TO_ATTRIBUTE.get(entry.getKey());
                if (attrKey == null) continue;
                Attribute attr = Registry.ATTRIBUTE.get(NamespacedKey.minecraft(attrKey));
                if (attr == null) attr = Registry.ATTRIBUTE.get(NamespacedKey.minecraft("generic." + attrKey));
                if (attr == null) continue;

                NamespacedKey key = new NamespacedKey(plugin, "orbmod_" + prefix + "_" + modIdx);
                AttributeModifier modifier = new AttributeModifier(
                        key, entry.getValue(), AttributeModifier.Operation.ADD_NUMBER, slotGroup);
                meta.addAttributeModifier(attr, modifier);
                modIdx++;
            }
        }
    }

    /**
     * Adiciona linhas de modificadores a uma lista de lore.
     */
    private void appendModifierLines(List<String> lore, List<ModifierEngine.RolledModifier> mods,
                                      int tier, int polish, int maxPolish, boolean polished,
                                      String tierColor) {
        int maxMods = getMaxMods(tier);
        if (maxMods > 0 || !mods.isEmpty()) {
            String modHeader = polished
                    ? "&d&lModificadores &8(&f" + mods.size() + "&7/&f" + maxMods + "&8) &cTravado"
                    : "&d&lModificadores &8(&f" + mods.size() + "&7/&f" + maxMods + "&8)";
            lore.add(cc(modHeader));
            for (var mod : mods) {
                for (var entry : mod.stats().entrySet()) {
                    String line = formatModifierLine(entry.getKey(), entry.getValue());
                    if (line != null) lore.add(cc(line));
                }
            }
            lore.add(cc(""));
        }

        if (polish > 0) {
            lore.add(cc("&b Polimento: &f" + polish + "&7/&f" + maxPolish));
            lore.add(cc(""));
        }
    }

    /**
     * Monta linhas de atributos vanilla do item (apenas sufixos customizados).
     */
    private List<String> buildAttributeLines(ItemStack item) {
        List<String> lines = new ArrayList<>();
        ItemMeta meta = item.getItemMeta();
        if (meta == null || !meta.hasAttributeModifiers()) return lines;

        var multimap = meta.getAttributeModifiers();
        if (multimap == null) return lines;

        for (var entry : multimap.entries()) {
            Attribute attr = entry.getKey();
            AttributeModifier mod = entry.getValue();

            // Mostrar apenas nossos sufixos customizados
            if (!mod.getKey().getKey().startsWith("suffix_")) continue;

            double val = mod.getAmount();
            if (val == 0) continue;
            String name = formatAttributeName(attr);
            String sign = val >= 0 ? "+" : "";
            if (mod.getOperation() == AttributeModifier.Operation.ADD_NUMBER) {
                lines.add(sign + String.format("%.1f", val) + " " + name);
            } else {
                lines.add(sign + String.format("%.0f%%", val * 100) + " " + name);
            }
        }
        return lines;
    }

    private String formatEnchantmentName(Enchantment ench) {
        String key = ench.getKey().getKey();
        return capitalize(key.replace("_", " "));
    }

    private String formatAttributeName(Attribute attr) {
        String key = attr.getKey().getKey();
        if (key.startsWith("generic.")) key = key.substring(8);
        return capitalize(key.replace("_", " "));
    }

    private String toRoman(int number) {
        return switch (number) {
            case 1 -> "I";
            case 2 -> "II";
            case 3 -> "III";
            case 4 -> "IV";
            case 5 -> "V";
            default -> String.valueOf(number);
        };
    }

    // ══════════════════════════════════════════════
    // RARE_DIAMOND bypass — MMOItems tem disable-crafting:true,
    // que anula o resultado. Aqui detectamos esse item no matrix,
    // trocamos por DIAMOND vanilla numa copia, e restauramos o
    // resultado da receita manualmente.
    // ══════════════════════════════════════════════

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPrepareItemCraftRareDiamond(PrepareItemCraftEvent event) {
        ItemStack[] matrix = event.getInventory().getMatrix();
        if (matrix == null) return;

        boolean hasRareDiamond = false;
        ItemStack[] replaced = new ItemStack[matrix.length];
        for (int i = 0; i < matrix.length; i++) {
            ItemStack s = matrix[i];
            if (s == null || s.getType() == Material.AIR) { replaced[i] = s; continue; }
            try {
                NBTItem nbt = NBTItem.get(s);
                if (nbt.hasType() && "RARE_DIAMOND".equals(nbt.getString("MMOITEMS_ITEM_ID"))) {
                    replaced[i] = new ItemStack(Material.DIAMOND, s.getAmount());
                    hasRareDiamond = true;
                    continue;
                }
            } catch (Exception ignored) {}
            replaced[i] = s;
        }
        if (!hasRareDiamond) return;

        // Se ja existe resultado valido, nada a fazer
        ItemStack current = event.getInventory().getResult();
        if (current != null && current.getType() != Material.AIR) return;

        // Tenta obter a receita via API do Bukkit usando matrix "normalizado"
        Player viewer = null;
        if (!event.getViewers().isEmpty() && event.getViewers().get(0) instanceof Player pp) {
            viewer = pp;
        }
        if (viewer == null) return;

        try {
            org.bukkit.inventory.Recipe recipe = Bukkit.getCraftingRecipe(replaced, viewer.getWorld());
            if (recipe != null) {
                ItemStack result = recipe.getResult();
                if (result != null && result.getType() != Material.AIR) {
                    event.getInventory().setResult(result);
                    if (debug()) plugin.getLogger().info("[RARE-DIAMOND] Resultado restaurado: " + result.getType());
                }
            }
        } catch (Throwable t) {
            if (debug()) plugin.getLogger().warning("[RARE-DIAMOND] Falha ao restaurar receita: " + t.getMessage());
        }
    }

    // ══════════════════════════════════════════════
    // Crafting Listener — auto-tag itens craftados
    // ══════════════════════════════════════════════

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onCraftItem(CraftItemEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;

        ItemStack result = event.getCurrentItem();
        if (result == null || result.getType() == Material.AIR) return;

        NBTItem resultNbt = NBTItem.get(result);
        String category = null;
        if (resultNbt.hasType()) {
            category = resolveCategory(resultNbt.getString("MMOITEMS_ITEM_TYPE"));
        }
        if (category == null) {
            category = resolveCategoryFromMaterial(result.getType());
        }

        if (category == null) return;

        Material mat = result.getType();

        if (debug()) plugin.getLogger().info("[DEBUG] Craft detectado: " + mat + " cat=" + category
                + " por " + player.getName());

        NBTItem nbt = NBTItem.get(result);
        if (nbt.hasTag(NBT_TIER)) return;
        ItemStack before = result.clone();

        nbt.addTag(new ItemTag(NBT_TIER, 0));
        nbt.addTag(new ItemTag(NBT_IDENTIFIED, 1));
        nbt.addTag(new ItemTag(NBT_CRAFT_LEVEL, getPlayerClassLevel(player)));

        // NÃO injetar tags MMOItems em itens vanilla — causa conflito com display do MMOItems
        // O sistema reconhece itens vanilla via NBT_TIER + resolveCategoryFromMaterial()

        ItemStack tagged = nbt.toItem();

        // ── Gerar encantamentos aleatorios (Prefixos) ──
        int maxEnchants = plugin.getConfig().getInt("enchant-on-craft.max", 2);
        applyRandomEnchantments(tagged, maxEnchants);

        // ── Gerar atributos aleatorios (Sufixos) ──
        applyRandomAttributes(tagged, category, getPlayerClassLevel(player));

        try {
            updateItemDisplay(tagged, NBTItem.get(tagged));
        } catch (Exception ex) {
            plugin.getLogger().warning("[CRAFT] Falha ao atualizar display do item craftado: " + ex.getMessage());
        }
        event.setCurrentItem(tagged);
        try {
            plugin.getModifierAuditService().logBeforeAfter("craft:common-tag", player, before, tagged);
        } catch (Exception ignored) {}

        if (debug()) plugin.getLogger().info("[DEBUG] Item craftado tagueado como Comum: " + mat
                + " enchants=" + tagged.getEnchantments().size());
    }

    /**
     * Aplica encantamentos aleatorios ao item com raridade por nivel.
     * Usa two-chance/one-chance para determinar quantidade.
     */
    void applyRandomEnchantments(ItemStack item, int maxEnchants) {
        List<Enchantment> valid = new ArrayList<>();
        for (Enchantment ench : Enchantment.values()) {
            if (ench.canEnchantItem(item) && ench != Enchantment.MENDING) {
                valid.add(ench);
            }
        }
        if (valid.isEmpty()) return;

        double twoChance = plugin.getConfig().getDouble("enchant-on-craft.two-chance", 15.0);
        double oneChance = plugin.getConfig().getDouble("enchant-on-craft.one-chance", 50.0);

        double roll = ThreadLocalRandom.current().nextDouble(100);
        int count;
        if (roll < twoChance) {
            count = 2;
        } else if (roll < twoChance + oneChance) {
            count = 1;
        } else {
            count = 0;
        }
        count = Math.min(count, maxEnchants);
        if (count == 0) return;

        Collections.shuffle(valid);
        int applied = 0;
        Set<Enchantment> used = new HashSet<>();

        for (Enchantment ench : valid) {
            if (applied >= count) break;
            if (used.contains(ench)) continue;

            boolean conflicts = false;
            for (Enchantment existing : used) {
                if (ench.conflictsWith(existing)) {
                    conflicts = true;
                    break;
                }
            }
            if (conflicts) continue;

            int level = rollEnchantmentLevel(ench.getMaxLevel());
            item.addUnsafeEnchantment(ench, level);
            used.add(ench);
            applied++;

            if (debug()) plugin.getLogger().info("[DEBUG] Encantamento aplicado: "
                    + ench.getKey().getKey() + " " + level);
        }
    }

    /**
     * Aplica atributos vanilla aleatorios (Sufixos) ao item.
     * Usa two-chance/one-chance para determinar quantidade.
     */
    void applyRandomAttributes(ItemStack item, String category) {
        applyRandomAttributes(item, category, 1);
    }

    void applyRandomAttributes(ItemStack item, String category, int playerLevel) {
        int maxAttrs = plugin.getConfig().getInt("suffix-on-craft.max", 2);
        double twoChance = plugin.getConfig().getDouble("suffix-on-craft.two-chance", 15.0);
        double oneChance = plugin.getConfig().getDouble("suffix-on-craft.one-chance", 50.0);

        double roll = ThreadLocalRandom.current().nextDouble(100);
        int count;
        if (roll < twoChance) {
            count = 2;
        } else if (roll < twoChance + oneChance) {
            count = 1;
        } else {
            count = 0;
        }
        count = Math.min(count, maxAttrs);
        if (count == 0) return;

        List<Attribute> pool = getAttributePool(category);
        if (pool.isEmpty()) return;
        Collections.shuffle(pool);

        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;

        // Preservar atributos default antes de adicionar customizados
        if (!meta.hasAttributeModifiers()) {
            preserveDefaultAttributes(item, meta);
        }

        EquipmentSlotGroup slotGroup;
        if ("weapon".equals(category) || "tool".equals(category)) {
            slotGroup = EquipmentSlotGroup.MAINHAND;
        } else if ("shield".equals(category)) {
            slotGroup = EquipmentSlotGroup.OFFHAND;
        } else {
            slotGroup = EquipmentSlotGroup.ARMOR;
        }

        String sfxPrefix = uniqueModPrefix();
        int applied = 0;
        for (Attribute attr : pool) {
            if (applied >= count) break;
            double value = rollAttributeValue(attr, playerLevel);
            if (value == 0) continue;

            NamespacedKey key = new NamespacedKey(plugin, "suffix_" + sfxPrefix + "_" + applied);
            AttributeModifier modifier = new AttributeModifier(
                    key, value, AttributeModifier.Operation.ADD_NUMBER, slotGroup);
            meta.addAttributeModifier(attr, modifier);
            applied++;

            if (debug()) plugin.getLogger().info("[DEBUG] Sufixo aplicado: "
                    + attr.getKey().getKey() + " +" + String.format("%.2f", value));
        }

        item.setItemMeta(meta);
    }

    /**
     * Preserva atributos default do item (ex: dano base de espadas).
     */
    @SuppressWarnings("deprecation")
    private void preserveDefaultAttributes(ItemStack item, ItemMeta meta) {
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            try {
                var defaults = item.getType().getDefaultAttributeModifiers(slot);
                for (var entry : defaults.entries()) {
                    meta.addAttributeModifier(entry.getKey(), entry.getValue());
                }
            } catch (Exception ignored) {}
        }
    }

    /**
     * Retorna pool de atributos por categoria.
     */
    private List<Attribute> getAttributePool(String category) {
        List<Attribute> pool = new ArrayList<>();
        if ("weapon".equals(category)) {
            addAttrToPool(pool, "attack_damage");
            addAttrToPool(pool, "attack_speed");
            addAttrToPool(pool, "attack_knockback");
        } else if ("armor".equals(category)) {
            addAttrToPool(pool, "armor");
            addAttrToPool(pool, "armor_toughness");
            addAttrToPool(pool, "knockback_resistance");
            addAttrToPool(pool, "max_health");
        } else if ("tool".equals(category)) {
            addAttrToPool(pool, "attack_damage");
            addAttrToPool(pool, "attack_speed");
        } else if ("shield".equals(category)) {
            addAttrToPool(pool, "armor");
            addAttrToPool(pool, "knockback_resistance");
            addAttrToPool(pool, "max_health");
        }
        return pool;
    }

    private void addAttrToPool(List<Attribute> pool, String key) {
        Attribute attr = Registry.ATTRIBUTE.get(NamespacedKey.minecraft(key));
        if (attr == null) attr = Registry.ATTRIBUTE.get(NamespacedKey.minecraft("generic." + key));
        if (attr != null) pool.add(attr);
    }

    /**
     * Rola valor de atributo com ranges por tipo.
     */
    private double rollAttributeValue(Attribute attr, int playerLevel) {
        String key = attr.getKey().getKey();
        double levelScaling = plugin.getConfig().getDouble("suffix-on-craft.level-scaling", 0.02);
        int clampedLevel = Math.max(1, playerLevel);
        double levelMultiplier = 1.0 + ((clampedLevel - 1) * levelScaling);

        if (key.endsWith("attack_damage")) return scaleWithLevel(randomRange(0.5, 2.5), levelMultiplier);
        if (key.endsWith("attack_speed")) return scaleWithLevel(randomRange(0.05, 0.2), levelMultiplier);
        if (key.endsWith("attack_knockback")) return scaleWithLevel(randomRange(0.3, 1.0), levelMultiplier);
        if (key.endsWith("armor_toughness")) return scaleWithLevel(randomRange(0.3, 1.0), levelMultiplier);
        if (key.endsWith("knockback_resistance")) return scaleWithLevel(randomRange(0.02, 0.08), levelMultiplier);
        if (key.endsWith("max_health")) return scaleWithLevel(randomRange(0.5, 2.0), levelMultiplier);
        if (key.endsWith("armor")) return scaleWithLevel(randomRange(0.5, 2.0), levelMultiplier);
        return 0;
    }

    private double scaleWithLevel(double baseValue, double multiplier) {
        return Math.round((baseValue * multiplier) * 100.0) / 100.0;
    }

    private double randomRange(double min, double max) {
        double val = ThreadLocalRandom.current().nextDouble(min, max);
        return Math.round(val * 100.0) / 100.0;
    }

    /**
     * Rola nivel do encantamento com raridade exponencial.
     * Nivel 1: comum, Nivel 2: ~10%, Nivel 3: ~1%, Nivel 4: ~0.1%, Nivel 5: ~0.01%
     */
    private int rollEnchantmentLevel(int maxLevel) {
        if (maxLevel <= 1) return 1;

        double roll = ThreadLocalRandom.current().nextDouble(100);

        double lvl5 = plugin.getConfig().getDouble("enchant-on-craft.level-chances.5", 0.01);
        double lvl4 = plugin.getConfig().getDouble("enchant-on-craft.level-chances.4", 0.1);
        double lvl3 = plugin.getConfig().getDouble("enchant-on-craft.level-chances.3", 1.0);
        double lvl2 = plugin.getConfig().getDouble("enchant-on-craft.level-chances.2", 10.0);

        int level;
        if (roll < lvl5 && maxLevel >= 5) {
            level = 5;
        } else if (roll < lvl5 + lvl4 && maxLevel >= 4) {
            level = 4;
        } else if (roll < lvl5 + lvl4 + lvl3 && maxLevel >= 3) {
            level = 3;
        } else if (roll < lvl5 + lvl4 + lvl3 + lvl2 && maxLevel >= 2) {
            level = 2;
        } else {
            level = 1;
        }

        return Math.min(level, maxLevel);
    }

    // ══════════════════════════════════════════════
    // Utilitarios
    // ══════════════════════════════════════════════

    public List<ModifierEngine.RolledModifier> readMods(NBTItem nbt) {
        if (!nbt.hasTag(NBT_MODS)) return new ArrayList<>();
        String json = nbt.getString(NBT_MODS);
        if (json == null || json.isEmpty() || json.equals("[]")) return new ArrayList<>();
        try {
            List<ModifierEngine.RolledModifier> list = gson.fromJson(json, ModifierEngine.ROLLED_LIST_TYPE);
            return list != null ? new ArrayList<>(list) : new ArrayList<>();
        } catch (Exception e) {
            plugin.getLogger().warning("Erro ao ler mods: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    private int getTier(NBTItem nbt) {
        return nbt.hasTag(NBT_TIER) ? nbt.getInteger(NBT_TIER) : 0;
    }

    private boolean isIdentified(NBTItem nbt) {
        if (!nbt.hasTag(NBT_IDENTIFIED)) return true; // itens sem tag = identificado (crafted)
        return nbt.getInteger(NBT_IDENTIFIED) == 1;
    }

    private boolean isPolished(NBTItem nbt) {
        return nbt.hasTag(NBT_POLISHED) && nbt.getInteger(NBT_POLISHED) == 1;
    }

    private int getItemLevel(NBTItem nbt) {
        // Prioridade: nivel de craft (pontos skill tree) > MMOITEMS level > fallback 1
        if (nbt.hasTag(NBT_CRAFT_LEVEL)) return nbt.getInteger(NBT_CRAFT_LEVEL);
        if (nbt.hasTag("MMOITEMS_ITEM_LEVEL")) return nbt.getInteger("MMOITEMS_ITEM_LEVEL");
        if (nbt.hasTag("MMOITEMS_UPGRADE_LEVEL")) return nbt.getInteger("MMOITEMS_UPGRADE_LEVEL");
        return 1;
    }

    /**
     * Nivel "real" do jogador para revelacao/identificacao (sem bonus de sorte).
     */
    private int getPlayerRevealLevel(Player player) {
        try {
            Class<?> pdClass = Class.forName("net.Indyuce.mmocore.api.player.PlayerData");
            Object data = null;
            try {
                data = pdClass.getMethod("get", org.bukkit.OfflinePlayer.class).invoke(null, player);
            } catch (NoSuchMethodException e1) {
                try {
                    data = pdClass.getMethod("get", org.bukkit.entity.Player.class).invoke(null, player);
                } catch (NoSuchMethodException e2) {
                    data = pdClass.getMethod("get", java.util.UUID.class).invoke(null, player.getUniqueId());
                }
            }
            if (data == null) return Math.max(1, player.getLevel());

            Object levelObj = pdClass.getMethod("getLevel").invoke(data);
            if (levelObj instanceof Number n) return Math.max(1, n.intValue());
        } catch (Exception ignored) {
        }
        return Math.max(1, player.getLevel());
    }

    /**
     * Obtem os pontos totais de skill tree do jogador via MMOCore (reflection).
     * Quanto mais pontos investidos na skill tree, maior o nivel efetivo para craft.
     * Inclui fator de sorte: chance de rolar como se fosse nivel mais alto.
     */
    private int getPlayerClassLevel(Player player) {
        // Regra unificada: nivel de classe real do jogador (sem bonus aleatorio).
        int level = getPlayerRevealLevel(player);
        if (debug()) plugin.getLogger().info("[DEBUG] Nivel de classe para orbs de " + player.getName() + ": " + level);
        return Math.max(1, level);
    }

    private void syncOffhandLevelToClass(Player player) {
        ItemStack offhand = player.getInventory().getItemInOffHand();
        if (offhand == null || offhand.getType() == Material.AIR) return;

        NBTItem nbt = NBTItem.get(offhand);
        if (!nbt.hasTag(NBT_TIER)) return;

        int classLevel = getPlayerClassLevel(player);
        int oldLevel = getItemLevel(nbt);
        if (classLevel <= 0) classLevel = 1;

        // Sempre ancora no nivel de classe do jogador durante uso de orb.
        if (nbt.hasTag(NBT_CRAFT_LEVEL) && oldLevel == classLevel) return;

        nbt.addTag(new ItemTag(NBT_CRAFT_LEVEL, classLevel));
        ItemStack updated = nbt.toItem();
        updateItemDisplay(updated, NBTItem.get(updated));
        player.getInventory().setItemInOffHand(updated);

        if (debug() && oldLevel != classLevel) {
            plugin.getLogger().info("[LEVEL-SYNC] " + player.getName()
                    + " item nivel " + oldLevel + " -> " + classLevel);
        }
    }

    private void preserveAllTags(NBTItem src, NBTItem dst) {
        if (src.hasTag(NBT_TIER)) dst.addTag(new ItemTag(NBT_TIER, src.getInteger(NBT_TIER)));
        if (src.hasTag(NBT_MODS)) dst.addTag(new ItemTag(NBT_MODS, src.getString(NBT_MODS)));
        if (src.hasTag(NBT_IDENTIFIED)) dst.addTag(new ItemTag(NBT_IDENTIFIED, src.getInteger(NBT_IDENTIFIED)));
        if (src.hasTag(NBT_POLISH)) dst.addTag(new ItemTag(NBT_POLISH, src.getInteger(NBT_POLISH)));
        if (src.hasTag(NBT_POLISHED)) dst.addTag(new ItemTag(NBT_POLISHED, src.getInteger(NBT_POLISHED)));
        if (src.hasTag(NBT_CRAFT_LEVEL)) dst.addTag(new ItemTag(NBT_CRAFT_LEVEL, src.getInteger(NBT_CRAFT_LEVEL)));
        if (src.hasTag(NBT_FORGE_QUALITY)) dst.addTag(new ItemTag(NBT_FORGE_QUALITY, src.getString(NBT_FORGE_QUALITY)));
        if (src.hasTag(NBT_FORGE_SMITH)) dst.addTag(new ItemTag(NBT_FORGE_SMITH, src.getString(NBT_FORGE_SMITH)));
        if (src.hasTag(NBT_FORGE_BONUS_PCT)) dst.addTag(new ItemTag(NBT_FORGE_BONUS_PCT, src.getDouble(NBT_FORGE_BONUS_PCT)));
    }

    private void preservePolishTag(NBTItem src, NBTItem dst) {
        if (src.hasTag(NBT_POLISH)) dst.addTag(new ItemTag(NBT_POLISH, src.getInteger(NBT_POLISH)));
    }

    private String resolveCategory(String mmoitemsType) {
        if (mmoitemsType == null) return null;
        String upper = mmoitemsType.toUpperCase();
        for (String cat : List.of("weapon", "armor", "accessory", "tool", "shield")) {
            List<String> types = plugin.getConfig().getStringList("type-categories." + cat);
            if (types.stream().anyMatch(t -> t.equalsIgnoreCase(upper))) return cat;
        }
        return null;
    }

    /**
     * Resolve categoria a partir do Material vanilla.
     */
    private String resolveCategoryFromMaterial(Material material) {
        if (VANILLA_WEAPONS.contains(material)) return "weapon";
        if (VANILLA_ARMOR.contains(material)) return "armor";
        if (VANILLA_TOOLS.contains(material)) return "tool";
        if (VANILLA_SHIELDS.contains(material)) return "shield";
        return null;
    }

    /**
     * Mapeia Material vanilla para tipo MMOItems equivalente.
     */
    private String resolveMMOItemType(Material mat) {
        String name = mat.name();
        if (name.contains("SWORD")) return "SWORD";
        if (name.contains("AXE")) return "AXE";
        if (name.contains("BOW") && !name.contains("CROSS")) return "BOW";
        if (name.contains("CROSSBOW")) return "CROSSBOW";
        if (name.equals("TRIDENT")) return "TRIDENT";
        if (name.contains("MACE")) return "MACE";
        if (name.contains("HELMET")) return "HELMET";
        if (name.contains("CHESTPLATE")) return "CHESTPLATE";
        if (name.contains("LEGGINGS")) return "LEGGINGS";
        if (name.contains("BOOTS")) return "BOOTS";
        if (name.contains("PICKAXE")) return "PICKAXE";
        if (name.contains("SHOVEL")) return "SHOVEL";
        if (name.contains("HOE")) return "HOE";
        if (name.equals("FISHING_ROD")) return "FISHING_ROD";
        if (name.equals("SHEARS")) return "SHEARS";
        if (name.equals("SHIELD")) return "SHIELD";
        return null;
    }

    String getTierName(int tier) {
        return plugin.getConfig().getString("tiers." + tier + ".name", "Desconhecido");
    }

    String getTierColor(int tier) {
        return plugin.getConfig().getString("tiers." + tier + ".color", "&7");
    }

    /**
     * Retorna o numero maximo de mods para o tier (estilo PoE2).
     * Comum=0, Magico=2, Raro=6, Unico=fixo.
     */
    int getMaxMods(int tier) {
        return plugin.getConfig().getInt("tiers." + tier + ".max-mods", 0);
    }

    private String formatMaterialName(Material mat) {
        String name = mat.name().toLowerCase().replace('_', ' ');
        StringBuilder sb = new StringBuilder();
        for (String word : name.split(" ")) {
            if (!sb.isEmpty()) sb.append(" ");
            sb.append(word.substring(0, 1).toUpperCase()).append(word.substring(1));
        }
        return sb.toString();
    }

    private void consumeOrb(Player player) {
        ItemStack mainHand = player.getInventory().getItemInMainHand();
        if (mainHand.getAmount() > 1) {
            mainHand.setAmount(mainHand.getAmount() - 1);
        } else {
            player.getInventory().setItemInMainHand(new ItemStack(Material.AIR));
        }
    }

    // ══════════════════════════════════════════════
    // Efeitos visuais — particulas e sons por orb
    // ══════════════════════════════════════════════
    private void spawnOrbParticles(Player player, String orbId) {
        Location loc = player.getLocation().add(0, 1, 0);

        switch (orbId) {
            case "PERGAMINHO_DE_IDENTIFICACAO" -> {
                // Amarelo brilhante — revelacao
                player.getWorld().spawnParticle(Particle.ENCHANT, loc, 40, 0.5, 0.5, 0.5, 0.5);
                player.getWorld().playSound(loc, Sound.BLOCK_ENCHANTMENT_TABLE_USE, 1.0f, 1.2f);
            }
            case "OLEO_DE_POLIMENTO" -> {
                // Azul claro — polimento/aprimoramento
                player.getWorld().spawnParticle(Particle.DOLPHIN, loc, 35, 0.4, 0.5, 0.4, 0.1);
                player.getWorld().spawnParticle(Particle.WAX_ON, loc, 20, 0.3, 0.4, 0.3, 0.1);
                player.getWorld().playSound(loc, Sound.BLOCK_ANVIL_USE, 0.8f, 1.5f);
            }
            case "PEDRA_DE_ENCANTAMENTO" -> {
                // Azul magico — transmutacao
                player.getWorld().spawnParticle(Particle.WITCH, loc, 30, 0.4, 0.5, 0.4, 0.1);
                player.getWorld().spawnParticle(Particle.PORTAL, loc, 25, 0.5, 0.5, 0.5, 0.5);
                player.getWorld().playSound(loc, Sound.ENTITY_EVOKER_CAST_SPELL, 0.8f, 1.0f);
            }
            case "PEDRA_DE_REFORCO" -> {
                // Verde — reforco
                player.getWorld().spawnParticle(Particle.HAPPY_VILLAGER, loc, 30, 0.5, 0.5, 0.5, 0.1);
                player.getWorld().playSound(loc, Sound.BLOCK_ANVIL_USE, 0.8f, 1.2f);
            }
            case "RUNA_NOBRE" -> {
                // Dourado — promocao nobre
                player.getWorld().spawnParticle(Particle.TOTEM_OF_UNDYING, loc, 40, 0.5, 0.8, 0.5, 0.3);
                player.getWorld().playSound(loc, Sound.UI_TOAST_CHALLENGE_COMPLETE, 0.6f, 1.2f);
            }
            case "PEDRA_DE_REFINAMENTO" -> {
                // Roxo intenso — alquimia
                player.getWorld().spawnParticle(Particle.DRAGON_BREATH, loc, 30, 0.4, 0.4, 0.4, 0.05);
                player.getWorld().spawnParticle(Particle.END_ROD, loc, 15, 0.3, 0.5, 0.3, 0.05);
                player.getWorld().playSound(loc, Sound.ENTITY_ELDER_GUARDIAN_CURSE, 0.5f, 1.5f);
            }
            case "RUNA_DE_PODER" -> {
                // Vermelho/laranja — poder
                player.getWorld().spawnParticle(Particle.FLAME, loc, 35, 0.5, 0.5, 0.5, 0.05);
                player.getWorld().spawnParticle(Particle.LAVA, loc, 10, 0.3, 0.3, 0.3, 0);
                player.getWorld().playSound(loc, Sound.ITEM_FIRECHARGE_USE, 0.8f, 0.8f);
            }
            case "MOEDA_DA_SORTE" -> {
                // Dourado cintilante — sorte
                player.getWorld().spawnParticle(Particle.TRIAL_SPAWNER_DETECTION, loc, 30, 0.5, 0.6, 0.5, 0.1);
                player.getWorld().spawnParticle(Particle.COMPOSTER, loc, 20, 0.4, 0.5, 0.4, 0.1);
                player.getWorld().playSound(loc, Sound.ENTITY_PLAYER_LEVELUP, 0.7f, 1.5f);
            }
            case "PEDRA_CORROSIVA" -> {
                // Vermelho escuro — corrosao/destruicao
                player.getWorld().spawnParticle(Particle.SMOKE, loc, 30, 0.4, 0.4, 0.4, 0.05);
                player.getWorld().spawnParticle(Particle.DAMAGE_INDICATOR, loc, 10, 0.3, 0.3, 0.3, 0.1);
                player.getWorld().playSound(loc, Sound.ENTITY_ITEM_BREAK, 1.0f, 0.8f);
            }
        }
    }

    private boolean isOnCooldown(Player player) {
        Long last = cooldowns.get(player.getUniqueId());
        if (last == null) return false;
        long cdMs = plugin.getConfig().getLong("cooldown-ms", 5000);
        return (System.currentTimeMillis() - last) < cdMs;
    }

    private void setCooldown(Player player) {
        cooldowns.put(player.getUniqueId(), System.currentTimeMillis());
    }

    private void send(Player player, String key, String... replacements) {
        String pfx = plugin.getConfig().getString("messages.prefix", "");
        String msg = plugin.getConfig().getString("messages." + key, key);
        for (int i = 0; i < replacements.length - 1; i += 2) {
            msg = msg.replace(replacements[i], replacements[i + 1]);
        }
        player.sendMessage(cc(pfx + msg));
    }

    private String cc(String s) {
        return ChatColor.translateAlternateColorCodes('&', s);
    }

    /** Converte texto legacy &-codes para Component sem italico (para itemName). */
    private Component toComponent(String raw) {
        return LegacyComponentSerializer.legacySection()
                .deserialize(cc(raw))
                .decoration(TextDecoration.ITALIC, false);
    }

    /** Esconde TODOS os elementos vanilla do tooltip (encantamentos, atributos, etc). */
    private void hideAllVanilla(ItemMeta meta) {
        meta.addItemFlags(ItemFlag.values());
    }

    /** Aplica hideAllVanilla ao ItemStack diretamente (para DataComponents de nivel item). */
    private void hideAllVanillaOnItem(ItemStack item) {
        try {
            // Esconder tooltip de atributos mas PRESERVAR os modifiers existentes
            var existingAttrs = item.getData(io.papermc.paper.datacomponent.DataComponentTypes.ATTRIBUTE_MODIFIERS);
            var builder = io.papermc.paper.datacomponent.item.ItemAttributeModifiers.itemAttributes()
                    .showInTooltip(false);
            if (existingAttrs != null) {
                for (var entry : existingAttrs.modifiers()) {
                    builder.addModifier(entry.attribute(), entry.modifier());
                }
            }
            item.setData(io.papermc.paper.datacomponent.DataComponentTypes.ATTRIBUTE_MODIFIERS, builder.build());
        } catch (Throwable ignored) {}
        try {
            // Esconder tooltip adicional ("Minecraft", categorias, etc)
            item.setData(io.papermc.paper.datacomponent.DataComponentTypes.HIDE_ADDITIONAL_TOOLTIP);
        } catch (Throwable ignored) {}
        try {
            // Esconder tooltip de encantamentos mas manter os enchants
            var enchants = item.getEnchantments();
            if (!enchants.isEmpty()) {
                var builder = io.papermc.paper.datacomponent.item.ItemEnchantments.itemEnchantments()
                        .showInTooltip(false);
                for (var e : enchants.entrySet()) {
                    builder.add(e.getKey(), e.getValue());
                }
                item.setData(io.papermc.paper.datacomponent.DataComponentTypes.ENCHANTMENTS, builder.build());
            }
        } catch (Throwable ignored) {}
        try {
            // Remover rarity tooltip ("COMMON", "UNCOMMON", etc)
            item.unsetData(io.papermc.paper.datacomponent.DataComponentTypes.RARITY);
        } catch (Throwable ignored) {}
    }

    private String capitalize(String s) {
        if (s == null || s.isEmpty()) return s;
        StringBuilder sb = new StringBuilder();
        for (String word : s.split(" ")) {
            if (!sb.isEmpty()) sb.append(" ");
            sb.append(word.substring(0, 1).toUpperCase()).append(word.substring(1).toLowerCase());
        }
        return sb.toString();
    }

    // ══════════════════════════════════════════════
    // Bonus Armor — bypass do cap vanilla de 30
    // ══════════════════════════════════════════════
    // Para itens vanilla, o Attribute.ARMOR e capado em 30 pela Minecraft.
    // Este listener le a stat "armor" dos mods nos itens equipados e aplica
    // reducao de dano adicional por cima do calculo vanilla, removendo o cap.
    // Itens MMOItems sao ignorados (ja aplicam via MythicLib sem cap).
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onPlayerDamageArmorBonus(EntityDamageEvent event) {
        if (plugin.getEffectiveArmorService() != null && plugin.getEffectiveArmorService().isEnabled()) return;
        if (!(event.getEntity() instanceof Player p)) return;
        var cause = event.getCause();
        if (cause == EntityDamageEvent.DamageCause.VOID
                || cause == EntityDamageEvent.DamageCause.STARVATION
                || cause == EntityDamageEvent.DamageCause.SUICIDE
                || cause == EntityDamageEvent.DamageCause.KILL
                || cause == EntityDamageEvent.DamageCause.DROWNING
                || cause == EntityDamageEvent.DamageCause.SUFFOCATION
                || cause == EntityDamageEvent.DamageCause.FALL) return;

        double bonusArmor = 0;
        for (ItemStack piece : p.getInventory().getArmorContents()) {
            if (piece == null || piece.getType() == Material.AIR) continue;
            NBTItem nbt = NBTItem.get(piece);
            // Pular MMOItems (aplicam ARMOR via MythicLib sem cap)
            if (nbt.hasType()) continue;
            if (!nbt.hasTag(NBT_MODS)) continue;
            try {
                String json = nbt.getString(NBT_MODS);
                if (json == null || json.isEmpty()) continue;
                List<ModifierEngine.RolledModifier> mods = gson.fromJson(json, ModifierEngine.ROLLED_LIST_TYPE);
                if (mods == null) continue;
                for (var mod : mods) {
                    Double armorVal = mod.stats().get("armor");
                    if (armorVal != null) bonusArmor += armorVal;
                }
            } catch (Exception ignored) {}
        }

        if (bonusArmor <= 0) return;
        // Reducao diminishing: 2.5% por ponto, cap de 75%
        double reduction = Math.min(0.75, bonusArmor * 0.025);
        event.setDamage(event.getDamage() * (1.0 - reduction));
    }

    private String shortStatName(String rawKey) {
        String lower = rawKey.replace("-", " ").replace("_", " ").toLowerCase();
        return capitalize(lower);
    }

    // ══════════════════════════════════════════════
    // Formatacao de stats de modificadores para lore
    // ══════════════════════════════════════════════

    // Stats que correspondem a niveis de encantamento vanilla (exibidos como "+N Tier")
    private static final java.util.Map<String, Integer> ENCHANT_TIER_STATS = java.util.Map.of(
            "fortune", 3,
            "unbreaking", 3,
            "knockback", 2
    );

    // Stats que exibem valores inteiros (dano, vida) — nao %, nao tier
    private static final java.util.Set<String> INTEGER_STATS = java.util.Set.of(
            "attack-damage", "max-health",
            "skill-damage", "magic-damage", "physical-damage",
            "weapon-damage", "projectile-damage",
            "fire-damage", "ice-damage", "lightning-damage",
            "earth-damage", "water-damage", "wind-damage",
            "fire-spell-damage", "ice-spell-damage", "lightning-spell-damage",
            "earth-spell-damage", "water-spell-damage", "wind-spell-damage"
    );

    // Stats cujo valor e percentual (exibidos com sufixo "%")
    private static final java.util.Set<String> PERCENT_STATS = java.util.Set.of(
            "critical-strike-chance", "critical-strike-power",
            "skill-critical-strike-chance", "skill-critical-strike-power",
            "lifesteal", "spell-vampirism",
            "cooldown-reduction", "block-cooldown-reduction",
            "dodge-cooldown-reduction", "parry-cooldown-reduction",
            "pve-damage", "pvp-damage",
            "fire-defense", "ice-defense", "lightning-defense",
            "earth-defense", "water-defense", "wind-defense",
            "speed-malus-reduction",
            "skill-exp-gain", "vanilla-exp-gain", "additional-experience",
            "sweeping-damage-ratio", "fall-damage-multiplier"
    );

    /**
     * Formata uma linha de modificador para lore.
     * Retorna null se o valor for zero (pular).
     * Para stats de encantamento (fortune/unbreaking/knockback) exibe "+N Tier".
     * Para stats percentuais exibe sufixo "%".
     */
    private String formatModifierLine(String statKey, double val) {
        if (val == 0) return null;
        String name = shortStatName(statKey);

        // Encantamento vanilla → tier (valor rolado ja e um inteiro 1/2/3)
        Integer maxTier = ENCHANT_TIER_STATS.get(statKey);
        if (maxTier != null) {
            int tier = Math.max(1, Math.min(maxTier, (int) Math.round(Math.abs(val))));
            String sign = val >= 0 ? "&a+" : "&c-";
            return "  &7" + name + ": " + sign + tier + " Tier";
        }

        // Valores inteiros (dano, vida)
        if (INTEGER_STATS.contains(statKey)) {
            long rounded = Math.round(val);
            if (rounded == 0) return null;
            if (rounded > 0) {
                return "  &7" + name + ": &a+" + rounded;
            } else {
                return "  &7" + name + ": &c" + rounded;
            }
        }

        // Percentual
        if (PERCENT_STATS.contains(statKey)) {
            if (val >= 0) {
                return "  &7" + name + ": &a+" + String.format("%.1f", val) + "%";
            } else {
                return "  &7" + name + ": &c" + String.format("%.1f", val) + "%";
            }
        }

        // Flat
        if (val >= 0) {
            return "  &7" + name + ": &a+" + String.format("%.1f", val);
        } else {
            return "  &7" + name + ": &c" + String.format("%.1f", val);
        }
    }
}
