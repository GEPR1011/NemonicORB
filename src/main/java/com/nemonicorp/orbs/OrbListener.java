/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.collect.Multimap
 *  com.google.gson.Gson
 *  io.lumine.mythic.lib.api.item.ItemTag
 *  io.lumine.mythic.lib.api.item.NBTItem
 *  io.papermc.paper.datacomponent.DataComponentType
 *  io.papermc.paper.datacomponent.DataComponentTypes
 *  io.papermc.paper.datacomponent.item.ItemAttributeModifiers
 *  io.papermc.paper.datacomponent.item.ItemAttributeModifiers$Builder
 *  io.papermc.paper.datacomponent.item.ItemAttributeModifiers$Entry
 *  io.papermc.paper.datacomponent.item.ItemEnchantments
 *  io.papermc.paper.datacomponent.item.ItemEnchantments$Builder
 *  net.Indyuce.mmoitems.ItemStats
 *  net.Indyuce.mmoitems.api.item.mmoitem.LiveMMOItem
 *  net.Indyuce.mmoitems.stat.data.DoubleData
 *  net.Indyuce.mmoitems.stat.data.StringData
 *  net.Indyuce.mmoitems.stat.data.StringListData
 *  net.Indyuce.mmoitems.stat.data.type.StatData
 *  net.Indyuce.mmoitems.stat.type.DoubleStat
 *  net.Indyuce.mmoitems.stat.type.ItemStat
 *  net.kyori.adventure.text.Component
 *  net.kyori.adventure.text.format.TextDecoration
 *  net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
 *  org.bukkit.Bukkit
 *  org.bukkit.ChatColor
 *  org.bukkit.Location
 *  org.bukkit.Material
 *  org.bukkit.NamespacedKey
 *  org.bukkit.OfflinePlayer
 *  org.bukkit.Particle
 *  org.bukkit.Registry
 *  org.bukkit.Sound
 *  org.bukkit.World
 *  org.bukkit.attribute.Attribute
 *  org.bukkit.attribute.AttributeModifier
 *  org.bukkit.attribute.AttributeModifier$Operation
 *  org.bukkit.configuration.ConfigurationSection
 *  org.bukkit.enchantments.Enchantment
 *  org.bukkit.entity.Entity
 *  org.bukkit.entity.HumanEntity
 *  org.bukkit.entity.Player
 *  org.bukkit.event.EventHandler
 *  org.bukkit.event.EventPriority
 *  org.bukkit.event.Listener
 *  org.bukkit.event.block.Action
 *  org.bukkit.event.entity.EntityDamageEvent
 *  org.bukkit.event.entity.EntityDamageEvent$DamageCause
 *  org.bukkit.event.inventory.CraftItemEvent
 *  org.bukkit.event.inventory.InventoryOpenEvent
 *  org.bukkit.event.inventory.PrepareItemCraftEvent
 *  org.bukkit.event.player.PlayerInteractEvent
 *  org.bukkit.event.player.PlayerJoinEvent
 *  org.bukkit.inventory.EquipmentSlot
 *  org.bukkit.inventory.EquipmentSlotGroup
 *  org.bukkit.inventory.ItemFlag
 *  org.bukkit.inventory.ItemStack
 *  org.bukkit.inventory.Recipe
 *  org.bukkit.inventory.meta.ItemMeta
 *  org.bukkit.plugin.Plugin
 */
package com.nemonicorp.orbs;

import com.google.common.collect.Multimap;
import com.google.gson.Gson;
import com.nemonicorp.orbs.DpsCalculator;
import com.nemonicorp.orbs.ModifierEngine;
import com.nemonicorp.orbs.NemonicOrbPlugin;
import io.lumine.mythic.lib.api.item.ItemTag;
import io.lumine.mythic.lib.api.item.NBTItem;
import io.papermc.paper.datacomponent.DataComponentType;
import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.ItemAttributeModifiers;
import io.papermc.paper.datacomponent.item.ItemEnchantments;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import net.Indyuce.mmoitems.ItemStats;
import net.Indyuce.mmoitems.api.item.mmoitem.LiveMMOItem;
import net.Indyuce.mmoitems.stat.data.DoubleData;
import net.Indyuce.mmoitems.stat.data.StringData;
import net.Indyuce.mmoitems.stat.data.StringListData;
import net.Indyuce.mmoitems.stat.data.type.StatData;
import net.Indyuce.mmoitems.stat.type.DoubleStat;
import net.Indyuce.mmoitems.stat.type.ItemStat;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.OfflinePlayer;
import org.bukkit.Particle;
import org.bukkit.Registry;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Entity;
import org.bukkit.entity.HumanEntity;
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
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.Plugin;

public class OrbListener
implements Listener {
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
    private static final String NEM = "\ufeff";
    private static final String TOOLTIP_CHARS_REGEX = "[\\uA000-\\uA00F\\uEA60-\\uEA75\\uF800-\\uF80F]";
    private final NemonicOrbPlugin plugin;
    private final ModifierEngine engine;
    private final Gson gson;
    private final Map<UUID, Long> cooldowns = new HashMap<UUID, Long>();
    private static final Set<Material> VANILLA_WEAPONS = EnumSet.of(Material.WOODEN_SWORD, new Material[]{Material.STONE_SWORD, Material.IRON_SWORD, Material.GOLDEN_SWORD, Material.DIAMOND_SWORD, Material.NETHERITE_SWORD, Material.WOODEN_AXE, Material.STONE_AXE, Material.IRON_AXE, Material.GOLDEN_AXE, Material.DIAMOND_AXE, Material.NETHERITE_AXE, Material.BOW, Material.CROSSBOW, Material.TRIDENT, Material.MACE});
    private static final Set<Material> VANILLA_ARMOR = EnumSet.of(Material.LEATHER_HELMET, new Material[]{Material.LEATHER_CHESTPLATE, Material.LEATHER_LEGGINGS, Material.LEATHER_BOOTS, Material.CHAINMAIL_HELMET, Material.CHAINMAIL_CHESTPLATE, Material.CHAINMAIL_LEGGINGS, Material.CHAINMAIL_BOOTS, Material.IRON_HELMET, Material.IRON_CHESTPLATE, Material.IRON_LEGGINGS, Material.IRON_BOOTS, Material.GOLDEN_HELMET, Material.GOLDEN_CHESTPLATE, Material.GOLDEN_LEGGINGS, Material.GOLDEN_BOOTS, Material.DIAMOND_HELMET, Material.DIAMOND_CHESTPLATE, Material.DIAMOND_LEGGINGS, Material.DIAMOND_BOOTS, Material.NETHERITE_HELMET, Material.NETHERITE_CHESTPLATE, Material.NETHERITE_LEGGINGS, Material.NETHERITE_BOOTS, Material.TURTLE_HELMET});
    private static final Set<Material> VANILLA_TOOLS = EnumSet.of(Material.WOODEN_PICKAXE, new Material[]{Material.STONE_PICKAXE, Material.IRON_PICKAXE, Material.GOLDEN_PICKAXE, Material.DIAMOND_PICKAXE, Material.NETHERITE_PICKAXE, Material.WOODEN_SHOVEL, Material.STONE_SHOVEL, Material.IRON_SHOVEL, Material.GOLDEN_SHOVEL, Material.DIAMOND_SHOVEL, Material.NETHERITE_SHOVEL, Material.WOODEN_HOE, Material.STONE_HOE, Material.IRON_HOE, Material.GOLDEN_HOE, Material.DIAMOND_HOE, Material.NETHERITE_HOE, Material.FISHING_ROD, Material.SHEARS});
    private static final Set<Material> VANILLA_SHIELDS = EnumSet.of(Material.SHIELD);
    private static final Map<String, Enchantment> STAT_TO_ENCHANTMENT = Map.of("fortune", Enchantment.FORTUNE, "unbreaking", Enchantment.UNBREAKING, "knockback", Enchantment.KNOCKBACK);
    private static final Map<String, String> STAT_TO_ATTRIBUTE = Map.ofEntries(Map.entry("attack-damage", "attack_damage"), Map.entry("attack-speed", "attack_speed"), Map.entry("armor", "armor"), Map.entry("armor-toughness", "armor_toughness"), Map.entry("max-health", "max_health"), Map.entry("max-absorption", "max_absorption"), Map.entry("knockback-resistance", "knockback_resistance"), Map.entry("explosion-knockback-resistance", "explosion_knockback_resistance"), Map.entry("mining-efficiency", "mining_efficiency"), Map.entry("sweeping-damage-ratio", "sweeping_damage_ratio"), Map.entry("fall-damage-multiplier", "fall_damage_multiplier"), Map.entry("fire-damage", "attack_damage"), Map.entry("ice-damage", "attack_damage"), Map.entry("lightning-damage", "attack_damage"), Map.entry("earth-damage", "attack_damage"), Map.entry("water-damage", "attack_damage"), Map.entry("wind-damage", "attack_damage"), Map.entry("physical-damage", "attack_damage"), Map.entry("magic-damage", "attack_damage"), Map.entry("weapon-damage", "attack_damage"), Map.entry("projectile-damage", "attack_damage"), Map.entry("luck", "luck"));
    private static final Map<String, Integer> ENCHANT_TIER_STATS = Map.of("fortune", 3, "unbreaking", 3, "knockback", 2);
    private static final Set<String> INTEGER_STATS = Set.of("attack-damage", "max-health", "skill-damage", "magic-damage", "physical-damage", "weapon-damage", "projectile-damage", "fire-damage", "ice-damage", "lightning-damage", "earth-damage", "water-damage", "wind-damage", "fire-spell-damage", "ice-spell-damage", "lightning-spell-damage", "earth-spell-damage", "water-spell-damage", "wind-spell-damage");
    private static final Set<String> PERCENT_STATS = Set.of("critical-strike-chance", "critical-strike-power", "skill-critical-strike-chance", "skill-critical-strike-power", "lifesteal", "spell-vampirism", "cooldown-reduction", "block-cooldown-reduction", "dodge-cooldown-reduction", "parry-cooldown-reduction", "pve-damage", "pvp-damage", "fire-defense", "ice-defense", "lightning-defense", "earth-defense", "water-defense", "wind-defense", "speed-malus-reduction", "skill-exp-gain", "vanilla-exp-gain", "additional-experience", "sweeping-damage-ratio", "fall-damage-multiplier");

    public OrbListener(NemonicOrbPlugin plugin) {
        this.plugin = plugin;
        this.engine = plugin.getModifierEngine();
        this.gson = this.engine.getGson();
    }

    private boolean debug() {
        return this.plugin.getConfig().getBoolean("debug", true);
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        this.plugin.getServer().getScheduler().runTaskLater((Plugin)this.plugin, () -> {
            Player p = event.getPlayer();
            if (p.isOnline()) {
                this.cleanInventoryTooltipChars(p);
            }
        }, 20L);
    }

    @EventHandler
    public void onInventoryOpen(InventoryOpenEvent event) {
        HumanEntity humanEntity = event.getPlayer();
        if (humanEntity instanceof Player) {
            Player p = (Player)humanEntity;
            this.cleanInventoryTooltipChars(p);
        }
    }

    private void cleanInventoryTooltipChars(Player player) {
        for (ItemStack item : player.getInventory().getContents()) {
            NBTItem nbt;
            if (item == null || !item.hasItemMeta() || !(nbt = NBTItem.get((ItemStack)item)).hasTag(NBT_TIER)) continue;
            this.updateItemDisplay(item, nbt);
        }
    }

    @EventHandler(priority=EventPriority.LOWEST)
    public void onPlayerInteract(PlayerInteractEvent event) {
        boolean success;
        String category;
        String targetMmoId;
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }
        Player player = event.getPlayer();
        if (event.getHand() == EquipmentSlot.OFF_HAND) {
            NBTItem mhNbt;
            ItemStack mh = player.getInventory().getItemInMainHand();
            if (mh.getType() != Material.AIR && (mhNbt = NBTItem.get((ItemStack)mh)).hasType() && "CONSUMABLE".equals(mhNbt.getString("MMOITEMS_ITEM_TYPE")) && this.plugin.getConfig().getStringList("orb-ids").contains(mhNbt.getString("MMOITEMS_ITEM_ID"))) {
                event.setCancelled(true);
            }
            return;
        }
        if (event.getHand() != EquipmentSlot.HAND) {
            return;
        }
        ItemStack mainHand = player.getInventory().getItemInMainHand();
        if (mainHand.getType() == Material.AIR) {
            return;
        }
        NBTItem orbNbt = NBTItem.get((ItemStack)mainHand);
        if (!orbNbt.hasType()) {
            return;
        }
        String orbType = orbNbt.getString("MMOITEMS_ITEM_TYPE");
        String orbId = orbNbt.getString("MMOITEMS_ITEM_ID");
        if (this.debug()) {
            this.plugin.getLogger().info("[DEBUG] Right-click detectado por " + player.getName());
            this.plugin.getLogger().info("[DEBUG] Main hand: type=" + orbType + " id=" + orbId);
        }
        if (!"CONSUMABLE".equals(orbType)) {
            return;
        }
        List orbIds = this.plugin.getConfig().getStringList("orb-ids");
        if (this.debug()) {
            this.plugin.getLogger().info("[DEBUG] Orb IDs na config: " + String.valueOf(orbIds));
            this.plugin.getLogger().info("[DEBUG] ID do item: '" + orbId + "' contido? " + orbIds.contains(orbId));
        }
        if (!orbIds.contains(orbId)) {
            return;
        }
        event.setCancelled(true);
        if (!player.hasPermission("nemonicorb.use")) {
            this.send(player, "no-permission", new String[0]);
            return;
        }
        if (this.isOnCooldown(player)) {
            this.send(player, "cooldown", new String[0]);
            return;
        }
        ItemStack target = player.getInventory().getItemInOffHand();
        if (target.getType() == Material.AIR) {
            this.send(player, "no-target", new String[0]);
            return;
        }
        ItemStack targetBefore = target.clone();
        NBTItem targetNbt = NBTItem.get((ItemStack)target);
        boolean isMMOItem = targetNbt.hasType();
        if (isMMOItem && (targetMmoId = targetNbt.getString("MMOITEMS_ITEM_ID")) != null && targetMmoId.startsWith("NEMONICORB_")) {
            isMMOItem = false;
        }
        if (isMMOItem) {
            String targetType = targetNbt.getString("MMOITEMS_ITEM_TYPE");
            category = this.resolveCategory(targetType);
            if (this.debug()) {
                this.plugin.getLogger().info("[DEBUG] Target MMOItem: type=" + targetType + " cat=" + category);
            }
        } else {
            category = this.resolveCategoryFromMaterial(target.getType());
            if (this.debug()) {
                this.plugin.getLogger().info("[DEBUG] Target Vanilla: material=" + String.valueOf(target.getType()) + " cat=" + category);
            }
        }
        int tier = this.getTier(targetNbt);
        if (category == null && !orbId.equals("PERGAMINHO_DE_IDENTIFICACAO") && !orbId.equals("PEDRA_CORROSIVA")) {
            this.send(player, "unsupported-type", new String[0]);
            return;
        }
        if (!(tier != 3 || orbId.equals("PERGAMINHO_DE_IDENTIFICACAO") || orbId.equals("OLEO_DE_POLIMENTO") || orbId.equals("PEDRA_CORROSIVA"))) {
            this.send(player, "is-unique", new String[0]);
            return;
        }
        if (this.debug()) {
            this.plugin.getLogger().info("[DEBUG] Processando orb=" + orbId + " tier=" + tier + " cat=" + category + " isMMO=" + isMMOItem);
        }
        switch (orbId) {
            case "PERGAMINHO_DE_IDENTIFICACAO": {
                boolean bl = this.handleIdentify(player, target, targetNbt);
                break;
            }
            case "OLEO_DE_POLIMENTO": {
                boolean bl = this.handlePolish(player, target, targetNbt, isMMOItem);
                break;
            }
            case "PEDRA_DE_ENCANTAMENTO": {
                boolean bl = this.handleTransmute(player, target, targetNbt, category, tier, isMMOItem);
                break;
            }
            case "PEDRA_DE_REFORCO": {
                boolean bl = this.handleAugment(player, target, targetNbt, category, tier, isMMOItem);
                break;
            }
            case "RUNA_NOBRE": {
                boolean bl = this.handleRegal(player, target, targetNbt, category, tier, isMMOItem);
                break;
            }
            case "PEDRA_DE_REFINAMENTO": {
                boolean bl = this.handleAlchemy(player, target, targetNbt, category, tier, isMMOItem);
                break;
            }
            case "RUNA_DE_PODER": {
                boolean bl = this.handleExalt(player, target, targetNbt, category, tier, isMMOItem);
                break;
            }
            case "MOEDA_DA_SORTE": {
                boolean bl = this.handleChance(player, target, targetNbt, category, tier, isMMOItem);
                break;
            }
            case "PEDRA_CORROSIVA": {
                boolean bl = this.handleAnnul(player, target, targetNbt, tier, isMMOItem);
                break;
            }
            default: {
                boolean bl = success = false;
            }
        }
        if (success) {
            if (!"PERGAMINHO_DE_IDENTIFICACAO".equals(orbId)) {
                this.syncOffhandLevelToClass(player);
            }
            this.consumeOrb(player);
            this.setCooldown(player);
            this.spawnOrbParticles(player, orbId);
            try {
                ItemStack after = player.getInventory().getItemInOffHand();
                this.plugin.getModifierAuditService().logBeforeAfter("orb:" + orbId, player, targetBefore, after);
            }
            catch (Exception exception) {
                // empty catch block
            }
            if (this.debug()) {
                this.plugin.getLogger().info("[DEBUG] Orb " + orbId + " aplicada com sucesso por " + player.getName());
            }
        } else if (this.debug()) {
            this.plugin.getLogger().info("[DEBUG] Orb " + orbId + " falhou para " + player.getName());
        }
    }

    private boolean handleIdentify(Player player, ItemStack target, NBTItem nbt) {
        if (this.isIdentified(nbt)) {
            this.send(player, "already-identified", new String[0]);
            return false;
        }
        int revealerLevel = this.getPlayerRevealLevel(player);
        int itemLevelBefore = this.getItemLevel(nbt);
        int cappedLevel = Math.max(1, Math.min(itemLevelBefore, revealerLevel));
        nbt.addTag(new ItemTag[]{new ItemTag(NBT_IDENTIFIED, (Object)1)});
        nbt.addTag(new ItemTag[]{new ItemTag(NBT_CRAFT_LEVEL, (Object)cappedLevel)});
        ItemStack updated = nbt.toItem();
        this.updateItemDisplay(updated, NBTItem.get((ItemStack)updated));
        player.getInventory().setItemInOffHand(updated);
        if (this.debug() && cappedLevel != itemLevelBefore) {
            this.plugin.getLogger().info("[IDENTIFY-CAP] " + player.getName() + " revelou item nivel " + itemLevelBefore + " -> cap " + cappedLevel);
        }
        this.send(player, "identify-success", new String[0]);
        return true;
    }

    private boolean handlePolish(Player player, ItemStack target, NBTItem nbt, boolean isMMOItem) {
        ItemStack result;
        ModifierEngine.RolledModifier newMod;
        ModifierEngine.RolledModifier oldMod;
        int currentPolish;
        if (!this.isIdentified(nbt)) {
            this.send(player, "not-identified", new String[0]);
            return false;
        }
        int maxPolish = this.plugin.getConfig().getInt("max-polish", 5);
        int n = currentPolish = nbt.hasTag(NBT_POLISH) ? nbt.getInteger(NBT_POLISH) : 0;
        if (currentPolish >= maxPolish) {
            this.send(player, "max-polish", "%max%", String.valueOf(maxPolish));
            return false;
        }
        List<ModifierEngine.RolledModifier> mods = this.readMods(nbt);
        if (mods.isEmpty()) {
            this.send(player, "no-mods-to-polish", new String[0]);
            return false;
        }
        ConfigurationSection polishSec = this.plugin.getConfig().getConfigurationSection("polish-percent");
        double defaultPct = this.plugin.getConfig().getDouble("polish-quality-boost", 5.0) / 100.0;
        ArrayList<ModifierEngine.RolledModifier> boostedMods = new ArrayList<ModifierEngine.RolledModifier>();
        for (ModifierEngine.RolledModifier mod : mods) {
            LinkedHashMap<String, Double> boostedStats = new LinkedHashMap<String, Double>();
            for (Map.Entry<String, Double> entry : mod.stats().entrySet()) {
                double pct = polishSec != null && polishSec.contains(entry.getKey()) ? polishSec.getDouble(entry.getKey()) / 100.0 : defaultPct;
                double oldVal = entry.getValue();
                double newVal = oldVal >= 0.0 ? (double)Math.round(oldVal * (1.0 + pct) * 100.0) / 100.0 : (double)Math.round(oldVal * (1.0 - pct) * 100.0) / 100.0;
                boostedStats.put(entry.getKey(), newVal);
            }
            boostedMods.add(new ModifierEngine.RolledModifier(mod.id(), mod.category(), boostedStats, mod.negative()));
        }
        if (isMMOItem) {
            LiveMMOItem live = new LiveMMOItem(target);
            for (int i = 0; i < mods.size(); ++i) {
                oldMod = mods.get(i);
                newMod = (ModifierEngine.RolledModifier)boostedMods.get(i);
                for (Map.Entry<String, Double> entry : oldMod.stats().entrySet()) {
                    double d;
                    DoubleStat stat = this.engine.resolveStat(entry.getKey());
                    if (stat == null) continue;
                    double diff = newMod.stats().get(entry.getKey()) - entry.getValue();
                    StatData data = live.getData((ItemStat)stat);
                    if (data instanceof DoubleData) {
                        DoubleData dd = (DoubleData)data;
                        d = dd.getValue();
                    } else {
                        d = 0.0;
                    }
                    double current = d;
                    live.setData((ItemStat)stat, (StatData)new DoubleData((double)Math.round((current + diff) * 100.0) / 100.0));
                }
            }
            ItemStack built = live.newBuilder().build();
            NBTItem builtNbt = NBTItem.get((ItemStack)built);
            this.preserveAllTags(nbt, builtNbt);
            builtNbt.addTag(new ItemTag[]{new ItemTag(NBT_POLISH, (Object)(currentPolish + 1))});
            builtNbt.addTag(new ItemTag[]{new ItemTag(NBT_POLISHED, (Object)1)});
            builtNbt.addTag(new ItemTag[]{new ItemTag(NBT_MODS, (Object)this.gson.toJson(boostedMods))});
            result = builtNbt.toItem();
        } else {
            nbt.addTag(new ItemTag[]{new ItemTag(NBT_POLISH, (Object)(currentPolish + 1))});
            nbt.addTag(new ItemTag[]{new ItemTag(NBT_POLISHED, (Object)1)});
            nbt.addTag(new ItemTag[]{new ItemTag(NBT_MODS, (Object)this.gson.toJson(boostedMods))});
            result = nbt.toItem();
        }
        this.updateItemDisplay(result, NBTItem.get((ItemStack)result));
        player.getInventory().setItemInOffHand(result);
        StringBuilder details = new StringBuilder();
        for (int i = 0; i < mods.size(); ++i) {
            oldMod = mods.get(i);
            newMod = (ModifierEngine.RolledModifier)boostedMods.get(i);
            for (Map.Entry<String, Double> entry : oldMod.stats().entrySet()) {
                String statName = this.capitalize(entry.getKey().replace("-", " ").replace("_", " "));
                double oldVal = entry.getValue();
                double newVal = newMod.stats().get(entry.getKey());
                double diff = (double)Math.round((newVal - oldVal) * 100.0) / 100.0;
                if (diff == 0.0) continue;
                String color = diff > 0.0 ? "&a" : "&c";
                String sign = diff > 0.0 ? "+" : "";
                details.append("\n").append(this.cc("  " + color + statName + ": " + String.format("%.1f", oldVal) + " -> " + String.format("%.1f", newVal) + " (" + sign + String.format("%.1f", diff) + ")"));
            }
        }
        this.send(player, "polish-success", "%count%", String.valueOf(currentPolish + 1), "%max%", String.valueOf(maxPolish));
        if (!details.isEmpty()) {
            player.sendMessage(this.cc("&b&lMelhorias aplicadas:") + String.valueOf(details));
        }
        return true;
    }

    private boolean handleTransmute(Player player, ItemStack target, NBTItem nbt, String category, int tier, boolean isMMOItem) {
        if (tier != 0) {
            this.send(player, "not-common", new String[0]);
            return false;
        }
        if (category == null) {
            this.send(player, "unsupported-type", new String[0]);
            return false;
        }
        int itemLevel = this.getPlayerClassLevel(player);
        int modCount = ThreadLocalRandom.current().nextInt(1, 3);
        List<ModifierEngine.RolledModifier> mods = this.engine.rollForTier(category, modCount, itemLevel, 2);
        if (this.debug()) {
            this.plugin.getLogger().info("[DEBUG] Transmute: nivel=" + itemLevel + " rolou " + mods.size() + " mods para cat=" + category);
        }
        if (mods.isEmpty()) {
            this.plugin.getLogger().warning("FALHA: Nenhum mod rolado para transmute! Verificar nemonicorp_orb_modifiers.yml");
            this.send(player, "no-mods", new String[0]);
            return false;
        }
        ItemStack result = this.buildModifiedItem(target, nbt, mods, isMMOItem);
        NBTItem resultNbt = NBTItem.get((ItemStack)result);
        resultNbt.addTag(new ItemTag[]{new ItemTag(NBT_TIER, (Object)1)});
        resultNbt.addTag(new ItemTag[]{new ItemTag(NBT_MODS, (Object)this.gson.toJson(mods))});
        resultNbt.addTag(new ItemTag[]{new ItemTag(NBT_IDENTIFIED, (Object)1)});
        this.preservePolishTag(nbt, resultNbt);
        result = resultNbt.toItem();
        this.updateItemDisplay(result, NBTItem.get((ItemStack)result));
        player.getInventory().setItemInOffHand(result);
        this.send(player, "transmute-success", "%count%", String.valueOf(mods.size()));
        return true;
    }

    private boolean handleAugment(Player player, ItemStack target, NBTItem nbt, String category, int tier, boolean isMMOItem) {
        String groupId;
        ModifierEngine.RolledModifier newMod;
        if (this.isPolished(nbt)) {
            this.send(player, "polished-locked", new String[0]);
            return false;
        }
        if (tier != 1) {
            this.send(player, "not-magic", new String[0]);
            return false;
        }
        if (category == null) {
            this.send(player, "unsupported-type", new String[0]);
            return false;
        }
        List<ModifierEngine.RolledModifier> mods = this.readMods(nbt);
        int maxMods = this.plugin.getConfig().getInt("tiers.1.max-mods", 2);
        if (mods.size() >= maxMods) {
            this.send(player, "mods-full", new String[0]);
            return false;
        }
        int itemLevel = this.getPlayerClassLevel(player);
        HashSet<String> used = new HashSet<String>();
        HashSet<String> usedIds = new HashSet<String>();
        for (ModifierEngine.RolledModifier m : mods) {
            if (m.category() != null) {
                used.add(m.category());
            }
            usedIds.add(m.id());
        }
        if (this.debug()) {
            this.plugin.getLogger().info("[DEBUG] Augment: nivel=" + itemLevel + " mods=" + mods.size() + "/" + maxMods + " used=" + String.valueOf(used) + " ids=" + String.valueOf(usedIds));
        }
        if ((newMod = this.engine.rollFromGroup(groupId = "nemonicorp_" + category + "_positive", itemLevel, used, usedIds, false, 1)) == null && !used.isEmpty()) {
            if (this.debug()) {
                this.plugin.getLogger().info("[DEBUG] Augment fallback 1: sem categoria nova em " + groupId + ", permitindo repeticao de categoria (dedup ID).");
            }
            newMod = this.engine.rollFromGroup(groupId, itemLevel, Collections.emptySet(), usedIds, false, 1);
        }
        if (newMod == null) {
            if (this.debug()) {
                this.plugin.getLogger().warning("[DEBUG] Augment fallback 2: pool exausto em " + groupId + ", rolando sem dedup.");
            }
            newMod = this.engine.rollFromGroup(groupId, itemLevel, Collections.emptySet(), Collections.emptySet(), false, 1);
        }
        if (newMod == null) {
            this.send(player, "no-mods", new String[0]);
            return false;
        }
        if (this.debug()) {
            this.plugin.getLogger().info("[DEBUG] Augment: novo mod=" + newMod.id());
        }
        ArrayList<ModifierEngine.RolledModifier> updated = new ArrayList<ModifierEngine.RolledModifier>(mods);
        updated.add(newMod);
        ItemStack result = this.buildModifiedItem(target, nbt, List.of(newMod), isMMOItem);
        NBTItem resultNbt = NBTItem.get((ItemStack)result);
        resultNbt.addTag(new ItemTag[]{new ItemTag(NBT_TIER, (Object)1)});
        resultNbt.addTag(new ItemTag[]{new ItemTag(NBT_MODS, (Object)this.gson.toJson(updated))});
        resultNbt.addTag(new ItemTag[]{new ItemTag(NBT_IDENTIFIED, (Object)1)});
        this.preservePolishTag(nbt, resultNbt);
        result = resultNbt.toItem();
        this.updateItemDisplay(result, NBTItem.get((ItemStack)result));
        player.getInventory().setItemInOffHand(result);
        this.send(player, "augment-success", new String[0]);
        return true;
    }

    private boolean handleRegal(Player player, ItemStack target, NBTItem nbt, String category, int tier, boolean isMMOItem) {
        if (tier != 1) {
            this.send(player, "not-magic", new String[0]);
            return false;
        }
        if (category == null) {
            this.send(player, "unsupported-type", new String[0]);
            return false;
        }
        List<ModifierEngine.RolledModifier> mods = this.readMods(nbt);
        if (mods.size() < 2) {
            this.send(player, "needs-2-mods", new String[0]);
            return false;
        }
        int itemLevel = this.getPlayerClassLevel(player);
        HashSet<String> used = new HashSet<String>();
        HashSet<String> usedIds = new HashSet<String>();
        for (ModifierEngine.RolledModifier m : mods) {
            if (m.category() != null) {
                used.add(m.category());
            }
            usedIds.add(m.id());
        }
        ModifierEngine.RolledModifier newMod = this.engine.rollFromGroup("nemonicorp_" + category + "_positive", itemLevel, used, usedIds, false, 2);
        if (newMod == null && !used.isEmpty()) {
            if (this.debug()) {
                this.plugin.getLogger().info("[DEBUG] Fallback 1: sem categoria nova em nemonicorp_" + category + "_positive, permitindo repeticao de categoria (dedup ID).");
            }
            newMod = this.engine.rollFromGroup("nemonicorp_" + category + "_positive", itemLevel, Collections.emptySet(), usedIds, false, 2);
        }
        if (newMod == null) {
            if (this.debug()) {
                this.plugin.getLogger().warning("[DEBUG] Fallback 2: pool exausto em nemonicorp_" + category + "_positive, rolando sem dedup.");
            }
            newMod = this.engine.rollFromGroup("nemonicorp_" + category + "_positive", itemLevel, Collections.emptySet(), Collections.emptySet(), false, 2);
        }
        if (newMod == null) {
            this.send(player, "no-mods", new String[0]);
            return false;
        }
        ArrayList<ModifierEngine.RolledModifier> updated = new ArrayList<ModifierEngine.RolledModifier>(mods);
        updated.add(newMod);
        ItemStack result = this.buildModifiedItem(target, nbt, List.of(newMod), isMMOItem);
        NBTItem resultNbt = NBTItem.get((ItemStack)result);
        resultNbt.addTag(new ItemTag[]{new ItemTag(NBT_TIER, (Object)2)});
        resultNbt.addTag(new ItemTag[]{new ItemTag(NBT_MODS, (Object)this.gson.toJson(updated))});
        resultNbt.addTag(new ItemTag[]{new ItemTag(NBT_IDENTIFIED, (Object)1)});
        this.preservePolishTag(nbt, resultNbt);
        result = resultNbt.toItem();
        this.updateItemDisplay(result, NBTItem.get((ItemStack)result));
        player.getInventory().setItemInOffHand(result);
        this.send(player, "regal-success", new String[0]);
        return true;
    }

    private boolean handleAlchemy(Player player, ItemStack target, NBTItem nbt, String category, int tier, boolean isMMOItem) {
        if (tier != 0) {
            this.send(player, "not-common", new String[0]);
            return false;
        }
        if (category == null) {
            this.send(player, "unsupported-type", new String[0]);
            return false;
        }
        int itemLevel = this.getPlayerClassLevel(player);
        int modCount = ThreadLocalRandom.current().nextInt(3, 7);
        List<ModifierEngine.RolledModifier> mods = this.engine.rollForTier(category, modCount, itemLevel, 6);
        if (mods.isEmpty()) {
            this.plugin.getLogger().warning("FALHA: Nenhum mod rolado para alchemy! Verificar nemonicorp_orb_modifiers.yml");
            this.send(player, "no-mods", new String[0]);
            return false;
        }
        ItemStack result = this.buildModifiedItem(target, nbt, mods, isMMOItem);
        NBTItem resultNbt = NBTItem.get((ItemStack)result);
        resultNbt.addTag(new ItemTag[]{new ItemTag(NBT_TIER, (Object)2)});
        resultNbt.addTag(new ItemTag[]{new ItemTag(NBT_MODS, (Object)this.gson.toJson(mods))});
        resultNbt.addTag(new ItemTag[]{new ItemTag(NBT_IDENTIFIED, (Object)1)});
        this.preservePolishTag(nbt, resultNbt);
        result = resultNbt.toItem();
        this.updateItemDisplay(result, NBTItem.get((ItemStack)result));
        player.getInventory().setItemInOffHand(result);
        this.send(player, "alchemy-success", "%count%", String.valueOf(mods.size()));
        return true;
    }

    private boolean handleExalt(Player player, ItemStack target, NBTItem nbt, String category, int tier, boolean isMMOItem) {
        if (this.isPolished(nbt)) {
            this.send(player, "polished-locked", new String[0]);
            return false;
        }
        if (tier != 2) {
            this.send(player, "not-rare", new String[0]);
            return false;
        }
        if (category == null) {
            this.send(player, "unsupported-type", new String[0]);
            return false;
        }
        List<ModifierEngine.RolledModifier> mods = this.readMods(nbt);
        int maxMods = this.plugin.getConfig().getInt("tiers.2.max-mods", 6);
        if (mods.size() >= maxMods) {
            this.send(player, "mods-full", new String[0]);
            return false;
        }
        int itemLevel = this.getPlayerClassLevel(player);
        HashSet<String> used = new HashSet<String>();
        HashSet<String> usedIds = new HashSet<String>();
        for (ModifierEngine.RolledModifier m : mods) {
            if (m.category() != null) {
                used.add(m.category());
            }
            usedIds.add(m.id());
        }
        ModifierEngine.RolledModifier newMod = this.engine.rollFromGroup("nemonicorp_" + category + "_positive", itemLevel, used, usedIds, false, 2);
        if (newMod == null && !used.isEmpty()) {
            if (this.debug()) {
                this.plugin.getLogger().info("[DEBUG] Fallback 1: sem categoria nova em nemonicorp_" + category + "_positive, permitindo repeticao de categoria (dedup ID).");
            }
            newMod = this.engine.rollFromGroup("nemonicorp_" + category + "_positive", itemLevel, Collections.emptySet(), usedIds, false, 2);
        }
        if (newMod == null) {
            if (this.debug()) {
                this.plugin.getLogger().warning("[DEBUG] Fallback 2: pool exausto em nemonicorp_" + category + "_positive, rolando sem dedup.");
            }
            newMod = this.engine.rollFromGroup("nemonicorp_" + category + "_positive", itemLevel, Collections.emptySet(), Collections.emptySet(), false, 2);
        }
        if (newMod == null) {
            this.send(player, "no-mods", new String[0]);
            return false;
        }
        ArrayList<ModifierEngine.RolledModifier> updated = new ArrayList<ModifierEngine.RolledModifier>(mods);
        updated.add(newMod);
        ItemStack result = this.buildModifiedItem(target, nbt, List.of(newMod), isMMOItem);
        NBTItem resultNbt = NBTItem.get((ItemStack)result);
        this.preserveAllTags(nbt, resultNbt);
        resultNbt.addTag(new ItemTag[]{new ItemTag(NBT_MODS, (Object)this.gson.toJson(updated))});
        result = resultNbt.toItem();
        this.updateItemDisplay(result, NBTItem.get((ItemStack)result));
        player.getInventory().setItemInOffHand(result);
        this.send(player, "exalt-success", "%modifier%", newMod.id());
        return true;
    }

    private boolean handleChance(Player player, ItemStack target, NBTItem nbt, String category, int tier, boolean isMMOItem) {
        int maxMods;
        int modCount;
        int newTier;
        if (tier != 0) {
            this.send(player, "not-common", new String[0]);
            return false;
        }
        if (category == null) {
            this.send(player, "unsupported-type", new String[0]);
            return false;
        }
        int magicChance = this.plugin.getConfig().getInt("chance-orb.magic", 70);
        int rareChance = this.plugin.getConfig().getInt("chance-orb.rare", 25);
        int roll = ThreadLocalRandom.current().nextInt(100);
        if (roll < magicChance) {
            newTier = 1;
            modCount = ThreadLocalRandom.current().nextInt(1, 3);
            maxMods = 2;
        } else if (roll < magicChance + rareChance) {
            newTier = 2;
            modCount = ThreadLocalRandom.current().nextInt(3, 7);
            maxMods = 6;
        } else {
            newTier = 3;
            modCount = 6;
            maxMods = 6;
        }
        int itemLevel = this.getPlayerClassLevel(player);
        List<ModifierEngine.RolledModifier> mods = newTier == 3 ? this.engine.rollForUnique(category, itemLevel) : this.engine.rollForTier(category, modCount, itemLevel, maxMods);
        if (mods.isEmpty()) {
            this.plugin.getLogger().warning("FALHA: Nenhum mod rolado para chance! Verificar nemonicorp_orb_modifiers.yml");
            this.send(player, "no-mods", new String[0]);
            return false;
        }
        ItemStack result = this.buildModifiedItem(target, nbt, mods, isMMOItem);
        NBTItem resultNbt = NBTItem.get((ItemStack)result);
        resultNbt.addTag(new ItemTag[]{new ItemTag(NBT_TIER, (Object)newTier)});
        resultNbt.addTag(new ItemTag[]{new ItemTag(NBT_MODS, (Object)this.gson.toJson(mods))});
        resultNbt.addTag(new ItemTag[]{new ItemTag(NBT_IDENTIFIED, (Object)1)});
        this.preservePolishTag(nbt, resultNbt);
        result = resultNbt.toItem();
        this.updateItemDisplay(result, NBTItem.get((ItemStack)result));
        player.getInventory().setItemInOffHand(result);
        String tierName = this.getTierName(newTier);
        this.send(player, "chance-success", "%tier%", tierName);
        return true;
    }

    private boolean handleAnnul(Player player, ItemStack target, NBTItem nbt, int tier, boolean isMMOItem) {
        ItemStack result;
        if (this.isPolished(nbt)) {
            this.send(player, "polished-locked", new String[0]);
            return false;
        }
        if (tier != 1 && tier != 2 && tier != 3) {
            this.send(player, "not-magic-or-rare", new String[0]);
            return false;
        }
        List<ModifierEngine.RolledModifier> mods = this.readMods(nbt);
        if (mods.isEmpty()) {
            this.send(player, "no-mods", new String[0]);
            return false;
        }
        int idx = ThreadLocalRandom.current().nextInt(mods.size());
        ModifierEngine.RolledModifier removed = mods.get(idx);
        ArrayList<ModifierEngine.RolledModifier> remaining = new ArrayList<ModifierEngine.RolledModifier>(mods);
        remaining.remove(idx);
        if (isMMOItem) {
            LiveMMOItem live = new LiveMMOItem(target);
            this.removeModStats(live, removed);
            ItemStack built = live.newBuilder().build();
            NBTItem builtNbt = NBTItem.get((ItemStack)built);
            this.preserveAllTags(nbt, builtNbt);
            builtNbt.addTag(new ItemTag[]{new ItemTag(NBT_MODS, (Object)this.gson.toJson(remaining))});
            result = builtNbt.toItem();
        } else {
            nbt.addTag(new ItemTag[]{new ItemTag(NBT_MODS, (Object)this.gson.toJson(remaining))});
            result = nbt.toItem();
        }
        this.removeEnchantMod(result, removed);
        this.updateItemDisplay(result, NBTItem.get((ItemStack)result));
        player.getInventory().setItemInOffHand(result);
        this.send(player, "annul-success", "%modifier%", removed.id());
        return true;
    }

    private static String uniqueModPrefix() {
        return Long.toHexString(ThreadLocalRandom.current().nextLong()).substring(0, 8);
    }

    private ItemStack buildModifiedItem(ItemStack target, NBTItem nbt, List<ModifierEngine.RolledModifier> newMods, boolean isMMOItem) {
        String category;
        if (isMMOItem) {
            try {
                LiveMMOItem live = new LiveMMOItem(target);
                if (live.getType() == null) {
                    if (this.debug()) {
                        this.plugin.getLogger().warning("[DEBUG] buildModifiedItem: MMOItem invalido (Type null). Aplicando fallback vanilla para " + String.valueOf(target.getType()));
                    }
                    return this.buildModifiedItem(target, nbt, newMods, false);
                }
                int applied = 0;
                int skipped = 0;
                for (ModifierEngine.RolledModifier mod : newMods) {
                    for (Map.Entry<String, Double> entry : mod.stats().entrySet()) {
                        double d;
                        DoubleStat stat = this.engine.resolveStat(entry.getKey());
                        if (stat == null) {
                            ++skipped;
                            if (!this.debug()) continue;
                            this.plugin.getLogger().warning("[DEBUG] Stat nao resolvido: '" + entry.getKey() + "' no mod " + mod.id());
                            continue;
                        }
                        StatData data = live.getData((ItemStat)stat);
                        if (data instanceof DoubleData) {
                            DoubleData dd = (DoubleData)data;
                            d = dd.getValue();
                        } else {
                            d = 0.0;
                        }
                        double current = d;
                        live.setData((ItemStat)stat, (StatData)new DoubleData(current + entry.getValue()));
                        ++applied;
                        if (!this.debug()) continue;
                        this.plugin.getLogger().info("[DEBUG] Stat aplicado: " + entry.getKey() + " = " + current + " + " + String.valueOf(entry.getValue()) + " = " + (current + entry.getValue()));
                    }
                }
                if (this.debug()) {
                    this.plugin.getLogger().info("[DEBUG] buildModifiedItem: " + applied + " stats aplicados, " + skipped + " pulados");
                }
                ItemStack built = live.newBuilder().build();
                this.applyEnchantMods(built, newMods);
                return built;
            }
            catch (Exception ex) {
                this.plugin.getLogger().warning("[OrbListener] Falha ao aplicar mods em MMOItem, fallback vanilla: " + ex.getMessage());
                return this.buildModifiedItem(target, nbt, newMods, false);
            }
        }
        ItemStack result = nbt.toItem();
        ItemMeta meta = result.getItemMeta();
        if (meta == null) {
            return result;
        }
        if (!meta.hasAttributeModifiers()) {
            this.preserveDefaultAttributes(result, meta);
        }
        EquipmentSlotGroup slotGroup = "weapon".equals(category = this.resolveCategoryFromMaterial(result.getType())) || "tool".equals(category) ? EquipmentSlotGroup.MAINHAND : ("shield".equals(category) ? EquipmentSlotGroup.OFFHAND : EquipmentSlotGroup.ARMOR);
        String prefix = OrbListener.uniqueModPrefix();
        int modIdx = 0;
        for (ModifierEngine.RolledModifier mod : newMods) {
            for (Map.Entry<String, Double> entry : mod.stats().entrySet()) {
                String attrKey = STAT_TO_ATTRIBUTE.get(entry.getKey());
                if (attrKey == null) {
                    if (!this.debug()) continue;
                    this.plugin.getLogger().info("[DEBUG] Vanilla: stat '" + entry.getKey() + "' nao tem Attribute equivalente, apenas lore");
                    continue;
                }
                Attribute attr = (Attribute)Registry.ATTRIBUTE.get(NamespacedKey.minecraft((String)attrKey));
                if (attr == null) {
                    attr = (Attribute)Registry.ATTRIBUTE.get(NamespacedKey.minecraft((String)("generic." + attrKey)));
                }
                if (attr == null) {
                    if (!this.debug()) continue;
                    this.plugin.getLogger().warning("[DEBUG] Vanilla: Attribute nao encontrado: " + attrKey);
                    continue;
                }
                NamespacedKey key = new NamespacedKey((Plugin)this.plugin, "orbmod_" + prefix + "_" + modIdx);
                AttributeModifier modifier = new AttributeModifier(key, entry.getValue().doubleValue(), AttributeModifier.Operation.ADD_NUMBER, slotGroup);
                meta.addAttributeModifier(attr, modifier);
                ++modIdx;
                if (!this.debug()) continue;
                this.plugin.getLogger().info("[DEBUG] Vanilla stat aplicado: " + attrKey + " +" + String.format("%.2f", entry.getValue()));
            }
        }
        result.setItemMeta(meta);
        this.applyEnchantMods(result, newMods);
        if (this.debug()) {
            this.plugin.getLogger().info("[DEBUG] buildModifiedItem: vanilla, " + modIdx + " attrs aplicados");
        }
        return result;
    }

    private void applyEnchantMods(ItemStack item, List<ModifierEngine.RolledModifier> newMods) {
        if (item == null || newMods == null || newMods.isEmpty()) {
            return;
        }
        LinkedHashMap<Enchantment, Integer> sumByEnch = new LinkedHashMap<Enchantment, Integer>();
        for (ModifierEngine.RolledModifier rolledModifier : newMods) {
            for (Map.Entry<String, Double> entry : rolledModifier.stats().entrySet()) {
                Enchantment ench = STAT_TO_ENCHANTMENT.get(entry.getKey());
                if (ench == null) continue;
                int tier = Math.max(1, (int)Math.round(entry.getValue()));
                sumByEnch.merge(ench, tier, Integer::sum);
            }
        }
        if (sumByEnch.isEmpty()) {
            return;
        }
        for (Map.Entry entry : sumByEnch.entrySet()) {
            int existing = item.getEnchantmentLevel((Enchantment)entry.getKey());
            int newLevel = Math.min(5, existing + (Integer)entry.getValue());
            item.addUnsafeEnchantment((Enchantment)entry.getKey(), newLevel);
            if (!this.debug()) continue;
            this.plugin.getLogger().info("[DEBUG-ENCH] " + ((Enchantment)entry.getKey()).getKey().getKey() + ": " + existing + " + " + String.valueOf(entry.getValue()) + " = " + newLevel);
        }
    }

    private void removeEnchantMod(ItemStack item, ModifierEngine.RolledModifier mod) {
        if (item == null || mod == null) {
            return;
        }
        for (Map.Entry<String, Double> entry : mod.stats().entrySet()) {
            Enchantment ench = STAT_TO_ENCHANTMENT.get(entry.getKey());
            if (ench == null) continue;
            int tier = Math.max(1, (int)Math.round(entry.getValue()));
            int current = item.getEnchantmentLevel(ench);
            int newLevel = current - tier;
            if (newLevel <= 0) {
                item.removeEnchantment(ench);
                continue;
            }
            item.addUnsafeEnchantment(ench, newLevel);
        }
    }

    private void removeModStats(LiveMMOItem live, ModifierEngine.RolledModifier mod) {
        for (Map.Entry<String, Double> entry : mod.stats().entrySet()) {
            double d;
            DoubleStat stat = this.engine.resolveStat(entry.getKey());
            if (stat == null) continue;
            StatData data = live.getData((ItemStat)stat);
            if (data instanceof DoubleData) {
                DoubleData dd = (DoubleData)data;
                d = dd.getValue();
            } else {
                d = 0.0;
            }
            double current = d;
            live.setData((ItemStat)stat, (StatData)new DoubleData(current - entry.getValue()));
        }
    }

    public void updateItemDisplay(ItemStack item, NBTItem nbt) {
        List<String> attrLines;
        boolean isOurTaggedItem;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return;
        }
        boolean isMMOItem = nbt.hasType();
        int tier = this.getTier(nbt);
        boolean identified = this.isIdentified(nbt);
        List<ModifierEngine.RolledModifier> mods = this.readMods(nbt);
        int polish = nbt.hasTag(NBT_POLISH) ? nbt.getInteger(NBT_POLISH) : 0;
        int maxPolish = this.plugin.getConfig().getInt("max-polish", 5);
        boolean polished = nbt.hasTag(NBT_POLISHED) && nbt.getInteger(NBT_POLISHED) == 1;
        String baseName = "";
        if (meta.hasItemName()) {
            baseName = ChatColor.stripColor((String)LegacyComponentSerializer.legacySection().serialize(meta.itemName()));
        } else if (meta.hasDisplayName()) {
            baseName = ChatColor.stripColor((String)meta.getDisplayName());
        }
        baseName = baseName.replaceAll("[\\uA000-\\uA00F\\uEA60-\\uEA75\\uF800-\\uF80F\\uFEFF]", "").trim();
        if (baseName.isEmpty() || baseName.equals("??? Item Desconhecido")) {
            baseName = this.formatMaterialName(item.getType());
        }
        String tierColor = this.getTierColor(tier);
        if (!identified) {
            meta.itemName(this.toComponent("&7&l??? Item Desconhecido"));
            ArrayList<String> lore = new ArrayList<String>();
            lore.add(this.cc("&7\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501"));
            lore.add(this.cc("&8  Tipo: &7\u2588\u2588\u2588\u2588\u2588\u2588\u2588\u2588"));
            lore.add(this.cc("&8  Tier: &7\u2588\u2588\u2588\u2588\u2588\u2588\u2588\u2588"));
            lore.add(this.cc(""));
            lore.add(this.cc("&8  \u2588\u2588 &7+\u2588\u2588 \u2588\u2588\u2588\u2588\u2588\u2588\u2588\u2588\u2588\u2588\u2588\u2588"));
            lore.add(this.cc("&8  \u2588\u2588 &7+\u2588\u2588 \u2588\u2588\u2588\u2588\u2588\u2588\u2588\u2588\u2588\u2588\u2588\u2588"));
            lore.add(this.cc("&8  \u2588\u2588 &7+\u2588\u2588 \u2588\u2588\u2588\u2588\u2588\u2588\u2588\u2588\u2588\u2588\u2588\u2588"));
            lore.add(this.cc(""));
            lore.add(this.cc("&c  [Item Nao Identificado]"));
            lore.add(this.cc("&7  Use um Pergaminho de"));
            lore.add(this.cc("&7  Identificacao para revelar."));
            lore.add(this.cc("&7\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501"));
            meta.setLore(lore);
            this.hideAllVanilla(meta);
            item.setItemMeta(meta);
            this.hideAllVanillaOnItem(item);
            return;
        }
        String mmoItemId = nbt.hasTag("MMOITEMS_ITEM_ID") ? nbt.getString("MMOITEMS_ITEM_ID") : "";
        boolean bl = isOurTaggedItem = mmoItemId != null && mmoItemId.startsWith("NEMONICORB_");
        if (isMMOItem && !isOurTaggedItem) {
            try {
                LiveMMOItem live = new LiveMMOItem(item);
                if (live.getType() != null) {
                    this.updateMMOItemDisplay(item, nbt, tier, mods, polish, maxPolish, polished, baseName, tierColor);
                    return;
                }
                if (this.debug()) {
                    this.plugin.getLogger().warning("[DEBUG] updateItemDisplay: MMOItem com Type nulo, usando display vanilla.");
                }
            }
            catch (Exception ex) {
                this.plugin.getLogger().warning("[OrbListener] Falha no display MMOItem, fallback vanilla: " + ex.getMessage());
            }
        }
        meta.itemName(this.toComponent(tierColor + "&l" + baseName));
        ArrayList<String> lore = new ArrayList<String>();
        String border = tierColor + "\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501\u2501";
        lore.add(this.cc(border));
        Map enchants = item.getEnchantments();
        if (!enchants.isEmpty()) {
            for (Map.Entry entry : enchants.entrySet()) {
                String enchName = this.formatEnchantmentName((Enchantment)entry.getKey());
                int lvl = (Integer)entry.getValue();
                lore.add(this.cc("&f" + enchName + " " + this.toRoman(lvl)));
            }
            lore.add(this.cc(""));
        }
        if (!(attrLines = this.buildAttributeLines(item)).isEmpty()) {
            for (String line : attrLines) {
                lore.add(this.cc("&f" + ChatColor.stripColor((String)this.cc(line))));
            }
            lore.add(this.cc(""));
        }
        this.appendModifierLines(lore, mods, tier, polish, maxPolish, polished, tierColor);
        double d = DpsCalculator.calculate(item, nbt, mods, tier, false, this.engine);
        if (this.debug()) {
            this.plugin.getLogger().info("[DEBUG-DPH] vanilla tier=" + tier + " material=" + item.getType().name() + " dps=" + d);
        }
        if (d >= 0.0) {
            lore.add(this.cc(""));
            lore.add(this.cc("  &6\u2694 DPH: &f" + String.format("%.1f", d)));
        }
        int itemLevel = this.getItemLevel(nbt);
        lore.add(this.cc("  &7Nivel: &f" + itemLevel));
        lore.add(this.cc("  &7Tier: " + tierColor + "&l" + this.getTierName(tier).toUpperCase()));
        this.appendForgeLines(lore, nbt);
        lore.add(this.cc(border));
        meta.setLore(lore);
        this.hideAllVanilla(meta);
        if (!mods.isEmpty()) {
            this.reapplyVanillaModStats(item, meta, mods);
        }
        item.setItemMeta(meta);
        this.hideAllVanillaOnItem(item);
        if (d >= 0.0) {
            NBTItem dpsNbt = NBTItem.get((ItemStack)item);
            dpsNbt.addTag(new ItemTag[]{new ItemTag(NBT_DPS, (Object)d)});
            ItemStack withDps = dpsNbt.toItem();
            item.setType(withDps.getType());
            item.setAmount(withDps.getAmount());
            item.setItemMeta(withDps.getItemMeta());
        }
    }

    private void appendForgeLines(List<String> lore, NBTItem nbt) {
        if (!nbt.hasTag(NBT_FORGE_QUALITY)) {
            return;
        }
        String quality = nbt.getString(NBT_FORGE_QUALITY);
        if (quality == null || quality.isEmpty()) {
            return;
        }
        String smith = nbt.hasTag(NBT_FORGE_SMITH) ? nbt.getString(NBT_FORGE_SMITH) : "";
        double bonus = nbt.hasTag(NBT_FORGE_BONUS_PCT) ? nbt.getDouble(NBT_FORGE_BONUS_PCT) : 0.0;
        String qColor = this.forgeQualityColor(quality);
        lore.add(this.cc("  " + qColor + "Forjado: &l" + quality + "&r" + qColor + " (+" + String.format("%.1f", bonus) + "%)"));
        if (!smith.isEmpty()) {
            String[] parts = smith.split("\\|");
            String smithName = parts[0];
            String smithLvl = parts.length > 1 ? parts[1] : "?";
            lore.add(this.cc("  &8Ferreiro: " + smithName + " (Nv." + smithLvl + ")"));
        }
    }

    private String forgeQualityColor(String quality) {
        if (quality == null) {
            return "&7";
        }
        return switch (quality) {
            case "Obra-Prima" -> "&6";
            case "Excelente" -> "&a";
            case "Bom" -> "&f";
            default -> "&7";
        };
    }

    private void updateMMOItemDisplay(ItemStack item, NBTItem nbt, int tier, List<ModifierEngine.RolledModifier> mods, int polish, int maxPolish, boolean polished, String baseName, String tierColor) {
        double dps;
        StringBuilder loreText = new StringBuilder();
        int maxMods = this.getMaxMods(tier);
        if (maxMods > 0 || !mods.isEmpty()) {
            String modLabel = polished ? "&d&lModificadores &8(&f" + mods.size() + "&7/&f" + maxMods + "&8) &cTravado" : "&d&lModificadores &8(&f" + mods.size() + "&7/&f" + maxMods + "&8)";
            loreText.append(modLabel).append("\n");
            for (ModifierEngine.RolledModifier mod : mods) {
                for (Map.Entry<String, Double> entry : mod.stats().entrySet()) {
                    String line = this.formatModifierLine(entry.getKey(), entry.getValue());
                    if (line == null) continue;
                    loreText.append(line).append("\n");
                }
            }
        }
        if (polish > 0) {
            loreText.append("&b Polimento: &f").append(polish).append("&7/&f").append(maxPolish).append("\n");
        }
        if ((dps = DpsCalculator.calculate(item, nbt, mods, tier, true, this.engine)) >= 0.0) {
            loreText.append("\n&6\u2694 DPH: &f").append(String.format("%.1f", dps)).append("\n");
        }
        int itemLevel = this.getItemLevel(nbt);
        loreText.append("&7Nivel: &f").append(itemLevel).append("\n");
        LiveMMOItem live = new LiveMMOItem(item);
        String mmoTierName = switch (tier) {
            case 1 -> "MAGICAL";
            case 2 -> "RARE";
            case 3 -> "UNIQUE";
            default -> "COMMON";
        };
        live.setData(ItemStats.TIER, (StatData)new StringData(mmoTierName));
        for (ModifierEngine.RolledModifier mod : mods) {
            for (Map.Entry<String, Double> entry : mod.stats().entrySet()) {
                double d;
                DoubleStat stat = this.engine.resolveStat(entry.getKey());
                if (stat == null) continue;
                StatData data = live.getData((ItemStat)stat);
                if (data instanceof DoubleData) {
                    DoubleData dd = (DoubleData)data;
                    d = dd.getValue();
                } else {
                    d = 0.0;
                }
                double current = d;
                live.setData((ItemStat)stat, (StatData)new DoubleData(current + entry.getValue()));
            }
        }
        if (loreText.length() > 0) {
            String loreStr = loreText.toString().trim();
            live.setData(ItemStats.LORE, (StatData)new StringListData(Arrays.asList(loreStr.split("\n"))));
        }
        ItemStack rebuilt = live.newBuilder().build();
        NBTItem rebuiltNbt = NBTItem.get((ItemStack)rebuilt);
        this.preserveAllTags(nbt, rebuiltNbt);
        rebuilt = rebuiltNbt.toItem();
        ItemMeta rebuiltMeta = rebuilt.getItemMeta();
        if (rebuiltMeta != null) {
            rebuiltMeta.itemName(this.toComponent(tierColor + "&l" + baseName));
            this.hideAllVanilla(rebuiltMeta);
            if (rebuiltMeta.hasLore()) {
                ArrayList<String> cleanLore = new ArrayList<String>();
                for (String line : rebuiltMeta.getLore()) {
                    cleanLore.add(line.replaceAll(TOOLTIP_CHARS_REGEX, ""));
                }
                rebuiltMeta.setLore(cleanLore);
            }
            rebuilt.setItemMeta(rebuiltMeta);
        }
        item.setType(rebuilt.getType());
        item.setAmount(rebuilt.getAmount());
        item.setItemMeta(rebuilt.getItemMeta());
        this.hideAllVanillaOnItem(item);
        if (this.debug()) {
            this.plugin.getLogger().info("[DEBUG] Display MMOItem atualizado: nome='" + baseName + "' mods=" + mods.size());
        }
    }

    private void reapplyVanillaModStats(ItemStack item, ItemMeta meta, List<ModifierEngine.RolledModifier> mods) {
        String category;
        if (!meta.hasAttributeModifiers()) {
            this.preserveDefaultAttributes(item, meta);
        }
        if (meta.hasAttributeModifiers()) {
            for (Attribute attr : Attribute.values()) {
                Collection existing = meta.getAttributeModifiers(attr);
                if (existing == null) continue;
                for (AttributeModifier mod : existing) {
                    if (!mod.getKey().getKey().startsWith("orbmod_")) continue;
                    meta.removeAttributeModifier(attr, mod);
                }
            }
        }
        EquipmentSlotGroup slotGroup = "weapon".equals(category = this.resolveCategoryFromMaterial(item.getType())) || "tool".equals(category) ? EquipmentSlotGroup.MAINHAND : ("shield".equals(category) ? EquipmentSlotGroup.OFFHAND : EquipmentSlotGroup.ARMOR);
        String prefix = OrbListener.uniqueModPrefix();
        int modIdx = 0;
        for (ModifierEngine.RolledModifier mod : mods) {
            for (Map.Entry<String, Double> entry : mod.stats().entrySet()) {
                String attrKey = STAT_TO_ATTRIBUTE.get(entry.getKey());
                if (attrKey == null) continue;
                Attribute attr = (Attribute)Registry.ATTRIBUTE.get(NamespacedKey.minecraft((String)attrKey));
                if (attr == null) {
                    attr = (Attribute)Registry.ATTRIBUTE.get(NamespacedKey.minecraft((String)("generic." + attrKey)));
                }
                if (attr == null) continue;
                NamespacedKey key = new NamespacedKey((Plugin)this.plugin, "orbmod_" + prefix + "_" + modIdx);
                AttributeModifier modifier = new AttributeModifier(key, entry.getValue().doubleValue(), AttributeModifier.Operation.ADD_NUMBER, slotGroup);
                meta.addAttributeModifier(attr, modifier);
                ++modIdx;
            }
        }
    }

    private void appendModifierLines(List<String> lore, List<ModifierEngine.RolledModifier> mods, int tier, int polish, int maxPolish, boolean polished, String tierColor) {
        int maxMods = this.getMaxMods(tier);
        if (maxMods > 0 || !mods.isEmpty()) {
            String modHeader = polished ? "&d&lModificadores &8(&f" + mods.size() + "&7/&f" + maxMods + "&8) &cTravado" : "&d&lModificadores &8(&f" + mods.size() + "&7/&f" + maxMods + "&8)";
            lore.add(this.cc(modHeader));
            for (ModifierEngine.RolledModifier mod : mods) {
                for (Map.Entry<String, Double> entry : mod.stats().entrySet()) {
                    String line = this.formatModifierLine(entry.getKey(), entry.getValue());
                    if (line == null) continue;
                    lore.add(this.cc(line));
                }
            }
            lore.add(this.cc(""));
        }
        if (polish > 0) {
            lore.add(this.cc("&b Polimento: &f" + polish + "&7/&f" + maxPolish));
            lore.add(this.cc(""));
        }
    }

    private List<String> buildAttributeLines(ItemStack item) {
        ArrayList<String> lines = new ArrayList<String>();
        ItemMeta meta = item.getItemMeta();
        if (meta == null || !meta.hasAttributeModifiers()) {
            return lines;
        }
        Multimap multimap = meta.getAttributeModifiers();
        if (multimap == null) {
            return lines;
        }
        for (Map.Entry entry : multimap.entries()) {
            String sign;
            double val;
            Attribute attr = (Attribute)entry.getKey();
            AttributeModifier mod = (AttributeModifier)entry.getValue();
            if (!mod.getKey().getKey().startsWith("suffix_") || (val = mod.getAmount()) == 0.0) continue;
            String name = this.formatAttributeName(attr);
            String string = sign = val >= 0.0 ? "+" : "";
            if (mod.getOperation() == AttributeModifier.Operation.ADD_NUMBER) {
                lines.add(sign + String.format("%.1f", val) + " " + name);
                continue;
            }
            lines.add(sign + String.format("%.0f%%", val * 100.0) + " " + name);
        }
        return lines;
    }

    private String formatEnchantmentName(Enchantment ench) {
        String key = ench.getKey().getKey();
        return this.capitalize(key.replace("_", " "));
    }

    private String formatAttributeName(Attribute attr) {
        String key = attr.getKey().getKey();
        if (key.startsWith("generic.")) {
            key = key.substring(8);
        }
        return this.capitalize(key.replace("_", " "));
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

    @EventHandler(priority=EventPriority.MONITOR)
    public void onPrepareItemCraftRareDiamond(PrepareItemCraftEvent event) {
        block14: {
            Object e;
            ItemStack[] matrix = event.getInventory().getMatrix();
            if (matrix == null) {
                return;
            }
            boolean hasRareDiamond = false;
            ItemStack[] replaced = new ItemStack[matrix.length];
            for (int i = 0; i < matrix.length; ++i) {
                ItemStack s = matrix[i];
                if (s == null || s.getType() == Material.AIR) {
                    replaced[i] = s;
                    continue;
                }
                try {
                    NBTItem nbt = NBTItem.get((ItemStack)s);
                    if (nbt.hasType() && "RARE_DIAMOND".equals(nbt.getString("MMOITEMS_ITEM_ID"))) {
                        replaced[i] = new ItemStack(Material.DIAMOND, s.getAmount());
                        hasRareDiamond = true;
                        continue;
                    }
                }
                catch (Exception nbt) {
                    // empty catch block
                }
                replaced[i] = s;
            }
            if (!hasRareDiamond) {
                return;
            }
            ItemStack current = event.getInventory().getResult();
            if (current != null && current.getType() != Material.AIR) {
                return;
            }
            Player viewer = null;
            if (!event.getViewers().isEmpty() && (e = event.getViewers().get(0)) instanceof Player) {
                Player pp;
                viewer = pp = (Player)e;
            }
            if (viewer == null) {
                return;
            }
            try {
                ItemStack result;
                Recipe recipe = Bukkit.getCraftingRecipe((ItemStack[])replaced, (World)viewer.getWorld());
                if (recipe != null && (result = recipe.getResult()) != null && result.getType() != Material.AIR) {
                    event.getInventory().setResult(result);
                    if (this.debug()) {
                        this.plugin.getLogger().info("[RARE-DIAMOND] Resultado restaurado: " + String.valueOf(result.getType()));
                    }
                }
            }
            catch (Throwable t) {
                if (!this.debug()) break block14;
                this.plugin.getLogger().warning("[RARE-DIAMOND] Falha ao restaurar receita: " + t.getMessage());
            }
        }
    }

    @EventHandler(priority=EventPriority.HIGHEST)
    public void onCraftItem(CraftItemEvent event) {
        NBTItem nbt;
        HumanEntity humanEntity = event.getWhoClicked();
        if (!(humanEntity instanceof Player)) {
            return;
        }
        Player player = (Player)humanEntity;
        ItemStack result = event.getCurrentItem();
        if (result == null || result.getType() == Material.AIR) {
            return;
        }
        NBTItem resultNbt = NBTItem.get((ItemStack)result);
        String category = null;
        if (resultNbt.hasType()) {
            category = this.resolveCategory(resultNbt.getString("MMOITEMS_ITEM_TYPE"));
        }
        if (category == null) {
            category = this.resolveCategoryFromMaterial(result.getType());
        }
        if (category == null) {
            return;
        }
        Material mat = result.getType();
        if (this.debug()) {
            this.plugin.getLogger().info("[DEBUG] Craft detectado: " + String.valueOf(mat) + " cat=" + category + " por " + player.getName());
        }
        if ((nbt = NBTItem.get((ItemStack)result)).hasTag(NBT_TIER)) {
            return;
        }
        ItemStack before = result.clone();
        nbt.addTag(new ItemTag[]{new ItemTag(NBT_TIER, (Object)0)});
        nbt.addTag(new ItemTag[]{new ItemTag(NBT_IDENTIFIED, (Object)1)});
        nbt.addTag(new ItemTag[]{new ItemTag(NBT_CRAFT_LEVEL, (Object)this.getPlayerClassLevel(player))});
        ItemStack tagged = nbt.toItem();
        int maxEnchants = this.plugin.getConfig().getInt("enchant-on-craft.max", 2);
        this.applyRandomEnchantments(tagged, maxEnchants);
        this.applyRandomAttributes(tagged, category, this.getPlayerClassLevel(player));
        try {
            this.updateItemDisplay(tagged, NBTItem.get((ItemStack)tagged));
        }
        catch (Exception ex) {
            this.plugin.getLogger().warning("[CRAFT] Falha ao atualizar display do item craftado: " + ex.getMessage());
        }
        event.setCurrentItem(tagged);
        try {
            this.plugin.getModifierAuditService().logBeforeAfter("craft:common-tag", player, before, tagged);
        }
        catch (Exception exception) {
            // empty catch block
        }
        if (this.debug()) {
            this.plugin.getLogger().info("[DEBUG] Item craftado tagueado como Comum: " + String.valueOf(mat) + " enchants=" + tagged.getEnchantments().size());
        }
    }

    void applyRandomEnchantments(ItemStack item, int maxEnchants) {
        ArrayList<Enchantment> valid = new ArrayList<Enchantment>();
        for (Enchantment ench : Enchantment.values()) {
            if (!ench.canEnchantItem(item) || ench == Enchantment.MENDING) continue;
            valid.add(ench);
        }
        if (valid.isEmpty()) {
            return;
        }
        double twoChance = this.plugin.getConfig().getDouble("enchant-on-craft.two-chance", 15.0);
        double oneChance = this.plugin.getConfig().getDouble("enchant-on-craft.one-chance", 50.0);
        double roll = ThreadLocalRandom.current().nextDouble(100.0);
        int count = roll < twoChance ? 2 : (roll < twoChance + oneChance ? 1 : 0);
        if ((count = Math.min(count, maxEnchants)) == 0) {
            return;
        }
        Collections.shuffle(valid);
        int applied = 0;
        HashSet<Enchantment> used = new HashSet<Enchantment>();
        for (Enchantment ench : valid) {
            if (applied >= count) break;
            if (used.contains(ench)) continue;
            boolean conflicts = false;
            for (Enchantment existing : used) {
                if (!ench.conflictsWith(existing)) continue;
                conflicts = true;
                break;
            }
            if (conflicts) continue;
            int level = this.rollEnchantmentLevel(ench.getMaxLevel());
            item.addUnsafeEnchantment(ench, level);
            used.add(ench);
            ++applied;
            if (!this.debug()) continue;
            this.plugin.getLogger().info("[DEBUG] Encantamento aplicado: " + ench.getKey().getKey() + " " + level);
        }
    }

    void applyRandomAttributes(ItemStack item, String category) {
        this.applyRandomAttributes(item, category, 1);
    }

    void applyRandomAttributes(ItemStack item, String category, int playerLevel) {
        int maxAttrs = this.plugin.getConfig().getInt("suffix-on-craft.max", 2);
        double twoChance = this.plugin.getConfig().getDouble("suffix-on-craft.two-chance", 15.0);
        double oneChance = this.plugin.getConfig().getDouble("suffix-on-craft.one-chance", 50.0);
        double roll = ThreadLocalRandom.current().nextDouble(100.0);
        int count = roll < twoChance ? 2 : (roll < twoChance + oneChance ? 1 : 0);
        if ((count = Math.min(count, maxAttrs)) == 0) {
            return;
        }
        List<Attribute> pool = this.getAttributePool(category);
        if (pool.isEmpty()) {
            return;
        }
        Collections.shuffle(pool);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return;
        }
        if (!meta.hasAttributeModifiers()) {
            this.preserveDefaultAttributes(item, meta);
        }
        EquipmentSlotGroup slotGroup = "weapon".equals(category) || "tool".equals(category) ? EquipmentSlotGroup.MAINHAND : ("shield".equals(category) ? EquipmentSlotGroup.OFFHAND : EquipmentSlotGroup.ARMOR);
        String sfxPrefix = OrbListener.uniqueModPrefix();
        int applied = 0;
        for (Attribute attr : pool) {
            if (applied >= count) break;
            double value = this.rollAttributeValue(attr, playerLevel);
            if (value == 0.0) continue;
            NamespacedKey key = new NamespacedKey((Plugin)this.plugin, "suffix_" + sfxPrefix + "_" + applied);
            AttributeModifier modifier = new AttributeModifier(key, value, AttributeModifier.Operation.ADD_NUMBER, slotGroup);
            meta.addAttributeModifier(attr, modifier);
            ++applied;
            if (!this.debug()) continue;
            this.plugin.getLogger().info("[DEBUG] Sufixo aplicado: " + attr.getKey().getKey() + " +" + String.format("%.2f", value));
        }
        item.setItemMeta(meta);
    }

    private void preserveDefaultAttributes(ItemStack item, ItemMeta meta) {
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            try {
                Multimap defaults = item.getType().getDefaultAttributeModifiers(slot);
                for (Map.Entry entry : defaults.entries()) {
                    meta.addAttributeModifier((Attribute)entry.getKey(), (AttributeModifier)entry.getValue());
                }
            }
            catch (Exception exception) {
                // empty catch block
            }
        }
    }

    private List<Attribute> getAttributePool(String category) {
        ArrayList<Attribute> pool = new ArrayList<Attribute>();
        if ("weapon".equals(category)) {
            this.addAttrToPool(pool, "attack_damage");
            this.addAttrToPool(pool, "attack_speed");
            this.addAttrToPool(pool, "attack_knockback");
        } else if ("armor".equals(category)) {
            this.addAttrToPool(pool, "armor");
            this.addAttrToPool(pool, "armor_toughness");
            this.addAttrToPool(pool, "knockback_resistance");
            this.addAttrToPool(pool, "max_health");
        } else if ("tool".equals(category)) {
            this.addAttrToPool(pool, "attack_damage");
            this.addAttrToPool(pool, "attack_speed");
        } else if ("shield".equals(category)) {
            this.addAttrToPool(pool, "armor");
            this.addAttrToPool(pool, "knockback_resistance");
            this.addAttrToPool(pool, "max_health");
        }
        return pool;
    }

    private void addAttrToPool(List<Attribute> pool, String key) {
        Attribute attr = (Attribute)Registry.ATTRIBUTE.get(NamespacedKey.minecraft((String)key));
        if (attr == null) {
            attr = (Attribute)Registry.ATTRIBUTE.get(NamespacedKey.minecraft((String)("generic." + key)));
        }
        if (attr != null) {
            pool.add(attr);
        }
    }

    private double rollAttributeValue(Attribute attr, int playerLevel) {
        String key = attr.getKey().getKey();
        double levelScaling = this.plugin.getConfig().getDouble("suffix-on-craft.level-scaling", 0.02);
        int clampedLevel = Math.max(1, playerLevel);
        double levelMultiplier = 1.0 + (double)(clampedLevel - 1) * levelScaling;
        if (key.endsWith("attack_damage")) {
            return this.scaleWithLevel(this.randomRange(0.5, 2.5), levelMultiplier);
        }
        if (key.endsWith("attack_speed")) {
            return this.scaleWithLevel(this.randomRange(0.05, 0.2), levelMultiplier);
        }
        if (key.endsWith("attack_knockback")) {
            return this.scaleWithLevel(this.randomRange(0.3, 1.0), levelMultiplier);
        }
        if (key.endsWith("armor_toughness")) {
            return this.scaleWithLevel(this.randomRange(0.3, 1.0), levelMultiplier);
        }
        if (key.endsWith("knockback_resistance")) {
            return this.scaleWithLevel(this.randomRange(0.02, 0.08), levelMultiplier);
        }
        if (key.endsWith("max_health")) {
            return this.scaleWithLevel(this.randomRange(0.5, 2.0), levelMultiplier);
        }
        if (key.endsWith("armor")) {
            return this.scaleWithLevel(this.randomRange(0.5, 2.0), levelMultiplier);
        }
        return 0.0;
    }

    private double scaleWithLevel(double baseValue, double multiplier) {
        return (double)Math.round(baseValue * multiplier * 100.0) / 100.0;
    }

    private double randomRange(double min, double max) {
        double val = ThreadLocalRandom.current().nextDouble(min, max);
        return (double)Math.round(val * 100.0) / 100.0;
    }

    private int rollEnchantmentLevel(int maxLevel) {
        if (maxLevel <= 1) {
            return 1;
        }
        double roll = ThreadLocalRandom.current().nextDouble(100.0);
        double lvl5 = this.plugin.getConfig().getDouble("enchant-on-craft.level-chances.5", 0.01);
        double lvl4 = this.plugin.getConfig().getDouble("enchant-on-craft.level-chances.4", 0.1);
        double lvl3 = this.plugin.getConfig().getDouble("enchant-on-craft.level-chances.3", 1.0);
        double lvl2 = this.plugin.getConfig().getDouble("enchant-on-craft.level-chances.2", 10.0);
        int level = roll < lvl5 && maxLevel >= 5 ? 5 : (roll < lvl5 + lvl4 && maxLevel >= 4 ? 4 : (roll < lvl5 + lvl4 + lvl3 && maxLevel >= 3 ? 3 : (roll < lvl5 + lvl4 + lvl3 + lvl2 && maxLevel >= 2 ? 2 : 1)));
        return Math.min(level, maxLevel);
    }

    public List<ModifierEngine.RolledModifier> readMods(NBTItem nbt) {
        if (!nbt.hasTag(NBT_MODS)) {
            return new ArrayList<ModifierEngine.RolledModifier>();
        }
        String json = nbt.getString(NBT_MODS);
        if (json == null || json.isEmpty() || json.equals("[]")) {
            return new ArrayList<ModifierEngine.RolledModifier>();
        }
        try {
            List list = (List)this.gson.fromJson(json, ModifierEngine.ROLLED_LIST_TYPE);
            return list != null ? new ArrayList<ModifierEngine.RolledModifier>(list) : new ArrayList();
        }
        catch (Exception e) {
            this.plugin.getLogger().warning("Erro ao ler mods: " + e.getMessage());
            return new ArrayList<ModifierEngine.RolledModifier>();
        }
    }

    private int getTier(NBTItem nbt) {
        return nbt.hasTag(NBT_TIER) ? nbt.getInteger(NBT_TIER) : 0;
    }

    private boolean isIdentified(NBTItem nbt) {
        if (!nbt.hasTag(NBT_IDENTIFIED)) {
            return true;
        }
        return nbt.getInteger(NBT_IDENTIFIED) == 1;
    }

    private boolean isPolished(NBTItem nbt) {
        return nbt.hasTag(NBT_POLISHED) && nbt.getInteger(NBT_POLISHED) == 1;
    }

    private int getItemLevel(NBTItem nbt) {
        if (nbt.hasTag(NBT_CRAFT_LEVEL)) {
            return nbt.getInteger(NBT_CRAFT_LEVEL);
        }
        if (nbt.hasTag("MMOITEMS_ITEM_LEVEL")) {
            return nbt.getInteger("MMOITEMS_ITEM_LEVEL");
        }
        if (nbt.hasTag("MMOITEMS_UPGRADE_LEVEL")) {
            return nbt.getInteger("MMOITEMS_UPGRADE_LEVEL");
        }
        return 1;
    }

    private int getPlayerRevealLevel(Player player) {
        try {
            Class<?> pdClass = Class.forName("net.Indyuce.mmocore.api.player.PlayerData");
            Object data = null;
            try {
                data = pdClass.getMethod("get", OfflinePlayer.class).invoke(null, player);
            }
            catch (NoSuchMethodException e1) {
                try {
                    data = pdClass.getMethod("get", Player.class).invoke(null, player);
                }
                catch (NoSuchMethodException e2) {
                    data = pdClass.getMethod("get", UUID.class).invoke(null, player.getUniqueId());
                }
            }
            if (data == null) {
                return Math.max(1, player.getLevel());
            }
            Object levelObj = pdClass.getMethod("getLevel", new Class[0]).invoke(data, new Object[0]);
            if (levelObj instanceof Number) {
                Number n = (Number)levelObj;
                return Math.max(1, n.intValue());
            }
        }
        catch (Exception exception) {
            // empty catch block
        }
        return Math.max(1, player.getLevel());
    }

    private int getPlayerClassLevel(Player player) {
        int level = this.getPlayerRevealLevel(player);
        if (this.debug()) {
            this.plugin.getLogger().info("[DEBUG] Nivel de classe para orbs de " + player.getName() + ": " + level);
        }
        return Math.max(1, level);
    }

    private void syncOffhandLevelToClass(Player player) {
        ItemStack offhand = player.getInventory().getItemInOffHand();
        if (offhand == null || offhand.getType() == Material.AIR) {
            return;
        }
        NBTItem nbt = NBTItem.get((ItemStack)offhand);
        if (!nbt.hasTag(NBT_TIER)) {
            return;
        }
        int classLevel = this.getPlayerClassLevel(player);
        int oldLevel = this.getItemLevel(nbt);
        if (classLevel <= 0) {
            classLevel = 1;
        }
        if (nbt.hasTag(NBT_CRAFT_LEVEL) && oldLevel == classLevel) {
            return;
        }
        nbt.addTag(new ItemTag[]{new ItemTag(NBT_CRAFT_LEVEL, (Object)classLevel)});
        ItemStack updated = nbt.toItem();
        this.updateItemDisplay(updated, NBTItem.get((ItemStack)updated));
        player.getInventory().setItemInOffHand(updated);
        if (this.debug() && oldLevel != classLevel) {
            this.plugin.getLogger().info("[LEVEL-SYNC] " + player.getName() + " item nivel " + oldLevel + " -> " + classLevel);
        }
    }

    private void preserveAllTags(NBTItem src, NBTItem dst) {
        if (src.hasTag(NBT_TIER)) {
            dst.addTag(new ItemTag[]{new ItemTag(NBT_TIER, (Object)src.getInteger(NBT_TIER))});
        }
        if (src.hasTag(NBT_MODS)) {
            dst.addTag(new ItemTag[]{new ItemTag(NBT_MODS, (Object)src.getString(NBT_MODS))});
        }
        if (src.hasTag(NBT_IDENTIFIED)) {
            dst.addTag(new ItemTag[]{new ItemTag(NBT_IDENTIFIED, (Object)src.getInteger(NBT_IDENTIFIED))});
        }
        if (src.hasTag(NBT_POLISH)) {
            dst.addTag(new ItemTag[]{new ItemTag(NBT_POLISH, (Object)src.getInteger(NBT_POLISH))});
        }
        if (src.hasTag(NBT_POLISHED)) {
            dst.addTag(new ItemTag[]{new ItemTag(NBT_POLISHED, (Object)src.getInteger(NBT_POLISHED))});
        }
        if (src.hasTag(NBT_CRAFT_LEVEL)) {
            dst.addTag(new ItemTag[]{new ItemTag(NBT_CRAFT_LEVEL, (Object)src.getInteger(NBT_CRAFT_LEVEL))});
        }
        if (src.hasTag(NBT_FORGE_QUALITY)) {
            dst.addTag(new ItemTag[]{new ItemTag(NBT_FORGE_QUALITY, (Object)src.getString(NBT_FORGE_QUALITY))});
        }
        if (src.hasTag(NBT_FORGE_SMITH)) {
            dst.addTag(new ItemTag[]{new ItemTag(NBT_FORGE_SMITH, (Object)src.getString(NBT_FORGE_SMITH))});
        }
        if (src.hasTag(NBT_FORGE_BONUS_PCT)) {
            dst.addTag(new ItemTag[]{new ItemTag(NBT_FORGE_BONUS_PCT, (Object)src.getDouble(NBT_FORGE_BONUS_PCT))});
        }
    }

    private void preservePolishTag(NBTItem src, NBTItem dst) {
        if (src.hasTag(NBT_POLISH)) {
            dst.addTag(new ItemTag[]{new ItemTag(NBT_POLISH, (Object)src.getInteger(NBT_POLISH))});
        }
    }

    private String resolveCategory(String mmoitemsType) {
        if (mmoitemsType == null) {
            return null;
        }
        String upper = mmoitemsType.toUpperCase();
        for (String cat : List.of("weapon", "armor", "accessory", "tool", "shield")) {
            List types = this.plugin.getConfig().getStringList("type-categories." + cat);
            if (!types.stream().anyMatch(t -> t.equalsIgnoreCase(upper))) continue;
            return cat;
        }
        return null;
    }

    private String resolveCategoryFromMaterial(Material material) {
        if (VANILLA_WEAPONS.contains(material)) {
            return "weapon";
        }
        if (VANILLA_ARMOR.contains(material)) {
            return "armor";
        }
        if (VANILLA_TOOLS.contains(material)) {
            return "tool";
        }
        if (VANILLA_SHIELDS.contains(material)) {
            return "shield";
        }
        return null;
    }

    private String resolveMMOItemType(Material mat) {
        String name = mat.name();
        if (name.contains("SWORD")) {
            return "SWORD";
        }
        if (name.contains("AXE")) {
            return "AXE";
        }
        if (name.contains("BOW") && !name.contains("CROSS")) {
            return "BOW";
        }
        if (name.contains("CROSSBOW")) {
            return "CROSSBOW";
        }
        if (name.equals("TRIDENT")) {
            return "TRIDENT";
        }
        if (name.contains("MACE")) {
            return "MACE";
        }
        if (name.contains("HELMET")) {
            return "HELMET";
        }
        if (name.contains("CHESTPLATE")) {
            return "CHESTPLATE";
        }
        if (name.contains("LEGGINGS")) {
            return "LEGGINGS";
        }
        if (name.contains("BOOTS")) {
            return "BOOTS";
        }
        if (name.contains("PICKAXE")) {
            return "PICKAXE";
        }
        if (name.contains("SHOVEL")) {
            return "SHOVEL";
        }
        if (name.contains("HOE")) {
            return "HOE";
        }
        if (name.equals("FISHING_ROD")) {
            return "FISHING_ROD";
        }
        if (name.equals("SHEARS")) {
            return "SHEARS";
        }
        if (name.equals("SHIELD")) {
            return "SHIELD";
        }
        return null;
    }

    String getTierName(int tier) {
        return this.plugin.getConfig().getString("tiers." + tier + ".name", "Desconhecido");
    }

    String getTierColor(int tier) {
        return this.plugin.getConfig().getString("tiers." + tier + ".color", "&7");
    }

    int getMaxMods(int tier) {
        return this.plugin.getConfig().getInt("tiers." + tier + ".max-mods", 0);
    }

    private String formatMaterialName(Material mat) {
        String name = mat.name().toLowerCase().replace('_', ' ');
        StringBuilder sb = new StringBuilder();
        for (String word : name.split(" ")) {
            if (!sb.isEmpty()) {
                sb.append(" ");
            }
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

    private void spawnOrbParticles(Player player, String orbId) {
        Location loc = player.getLocation().add(0.0, 1.0, 0.0);
        switch (orbId) {
            case "PERGAMINHO_DE_IDENTIFICACAO": {
                player.getWorld().spawnParticle(Particle.ENCHANT, loc, 40, 0.5, 0.5, 0.5, 0.5);
                player.getWorld().playSound(loc, Sound.BLOCK_ENCHANTMENT_TABLE_USE, 1.0f, 1.2f);
                break;
            }
            case "OLEO_DE_POLIMENTO": {
                player.getWorld().spawnParticle(Particle.DOLPHIN, loc, 35, 0.4, 0.5, 0.4, 0.1);
                player.getWorld().spawnParticle(Particle.WAX_ON, loc, 20, 0.3, 0.4, 0.3, 0.1);
                player.getWorld().playSound(loc, Sound.BLOCK_ANVIL_USE, 0.8f, 1.5f);
                break;
            }
            case "PEDRA_DE_ENCANTAMENTO": {
                player.getWorld().spawnParticle(Particle.WITCH, loc, 30, 0.4, 0.5, 0.4, 0.1);
                player.getWorld().spawnParticle(Particle.PORTAL, loc, 25, 0.5, 0.5, 0.5, 0.5);
                player.getWorld().playSound(loc, Sound.ENTITY_EVOKER_CAST_SPELL, 0.8f, 1.0f);
                break;
            }
            case "PEDRA_DE_REFORCO": {
                player.getWorld().spawnParticle(Particle.HAPPY_VILLAGER, loc, 30, 0.5, 0.5, 0.5, 0.1);
                player.getWorld().playSound(loc, Sound.BLOCK_ANVIL_USE, 0.8f, 1.2f);
                break;
            }
            case "RUNA_NOBRE": {
                player.getWorld().spawnParticle(Particle.TOTEM_OF_UNDYING, loc, 40, 0.5, 0.8, 0.5, 0.3);
                player.getWorld().playSound(loc, Sound.UI_TOAST_CHALLENGE_COMPLETE, 0.6f, 1.2f);
                break;
            }
            case "PEDRA_DE_REFINAMENTO": {
                player.getWorld().spawnParticle(Particle.DRAGON_BREATH, loc, 30, 0.4, 0.4, 0.4, 0.05);
                player.getWorld().spawnParticle(Particle.END_ROD, loc, 15, 0.3, 0.5, 0.3, 0.05);
                player.getWorld().playSound(loc, Sound.ENTITY_ELDER_GUARDIAN_CURSE, 0.5f, 1.5f);
                break;
            }
            case "RUNA_DE_PODER": {
                player.getWorld().spawnParticle(Particle.FLAME, loc, 35, 0.5, 0.5, 0.5, 0.05);
                player.getWorld().spawnParticle(Particle.LAVA, loc, 10, 0.3, 0.3, 0.3, 0.0);
                player.getWorld().playSound(loc, Sound.ITEM_FIRECHARGE_USE, 0.8f, 0.8f);
                break;
            }
            case "MOEDA_DA_SORTE": {
                player.getWorld().spawnParticle(Particle.TRIAL_SPAWNER_DETECTION, loc, 30, 0.5, 0.6, 0.5, 0.1);
                player.getWorld().spawnParticle(Particle.COMPOSTER, loc, 20, 0.4, 0.5, 0.4, 0.1);
                player.getWorld().playSound(loc, Sound.ENTITY_PLAYER_LEVELUP, 0.7f, 1.5f);
                break;
            }
            case "PEDRA_CORROSIVA": {
                player.getWorld().spawnParticle(Particle.SMOKE, loc, 30, 0.4, 0.4, 0.4, 0.05);
                player.getWorld().spawnParticle(Particle.DAMAGE_INDICATOR, loc, 10, 0.3, 0.3, 0.3, 0.1);
                player.getWorld().playSound(loc, Sound.ENTITY_ITEM_BREAK, 1.0f, 0.8f);
            }
        }
    }

    private boolean isOnCooldown(Player player) {
        Long last = this.cooldowns.get(player.getUniqueId());
        if (last == null) {
            return false;
        }
        long cdMs = this.plugin.getConfig().getLong("cooldown-ms", 5000L);
        return System.currentTimeMillis() - last < cdMs;
    }

    private void setCooldown(Player player) {
        this.cooldowns.put(player.getUniqueId(), System.currentTimeMillis());
    }

    private void send(Player player, String key, String ... replacements) {
        String pfx = this.plugin.getConfig().getString("messages.prefix", "");
        String msg = this.plugin.getConfig().getString("messages." + key, key);
        for (int i = 0; i < replacements.length - 1; i += 2) {
            msg = msg.replace(replacements[i], replacements[i + 1]);
        }
        player.sendMessage(this.cc(pfx + msg));
    }

    private String cc(String s) {
        return ChatColor.translateAlternateColorCodes((char)'&', (String)s);
    }

    private Component toComponent(String raw) {
        return LegacyComponentSerializer.legacySection().deserialize(this.cc(raw)).decoration(TextDecoration.ITALIC, false);
    }

    private void hideAllVanilla(ItemMeta meta) {
        meta.addItemFlags(ItemFlag.values());
    }

    private void hideAllVanillaOnItem(ItemStack item) {
        ItemAttributeModifiers.Builder builder;
        try {
            ItemAttributeModifiers existingAttrs = (ItemAttributeModifiers)item.getData(DataComponentTypes.ATTRIBUTE_MODIFIERS);
            builder = (ItemAttributeModifiers.Builder)ItemAttributeModifiers.itemAttributes().showInTooltip(false);
            if (existingAttrs != null) {
                for (ItemAttributeModifiers.Entry entry : existingAttrs.modifiers()) {
                    builder.addModifier(entry.attribute(), entry.modifier());
                }
            }
            item.setData(DataComponentTypes.ATTRIBUTE_MODIFIERS, (Object)((ItemAttributeModifiers)builder.build()));
        }
        catch (Throwable existingAttrs) {
            // empty catch block
        }
        try {
            item.setData(DataComponentTypes.HIDE_ADDITIONAL_TOOLTIP);
        }
        catch (Throwable existingAttrs) {
            // empty catch block
        }
        try {
            Map enchants = item.getEnchantments();
            if (!enchants.isEmpty()) {
                builder = (ItemEnchantments.Builder)ItemEnchantments.itemEnchantments().showInTooltip(false);
                for (Map.Entry entry : enchants.entrySet()) {
                    builder.add((Enchantment)entry.getKey(), ((Integer)entry.getValue()).intValue());
                }
                item.setData(DataComponentTypes.ENCHANTMENTS, (Object)((ItemEnchantments)builder.build()));
            }
        }
        catch (Throwable throwable) {
            // empty catch block
        }
        try {
            item.unsetData((DataComponentType)DataComponentTypes.RARITY);
        }
        catch (Throwable throwable) {
            // empty catch block
        }
    }

    private String capitalize(String s) {
        if (s == null || s.isEmpty()) {
            return s;
        }
        StringBuilder sb = new StringBuilder();
        for (String word : s.split(" ")) {
            if (!sb.isEmpty()) {
                sb.append(" ");
            }
            sb.append(word.substring(0, 1).toUpperCase()).append(word.substring(1).toLowerCase());
        }
        return sb.toString();
    }

    @EventHandler(priority=EventPriority.LOW, ignoreCancelled=true)
    public void onPlayerDamageArmorBonus(EntityDamageEvent event) {
        if (this.plugin.getEffectiveArmorService() != null && this.plugin.getEffectiveArmorService().isEnabled()) {
            return;
        }
        Entity entity = event.getEntity();
        if (!(entity instanceof Player)) {
            return;
        }
        Player p = (Player)entity;
        EntityDamageEvent.DamageCause cause = event.getCause();
        if (cause == EntityDamageEvent.DamageCause.VOID || cause == EntityDamageEvent.DamageCause.STARVATION || cause == EntityDamageEvent.DamageCause.SUICIDE || cause == EntityDamageEvent.DamageCause.KILL || cause == EntityDamageEvent.DamageCause.DROWNING || cause == EntityDamageEvent.DamageCause.SUFFOCATION || cause == EntityDamageEvent.DamageCause.FALL) {
            return;
        }
        double bonusArmor = 0.0;
        for (ItemStack piece : p.getInventory().getArmorContents()) {
            NBTItem nbt;
            if (piece == null || piece.getType() == Material.AIR || (nbt = NBTItem.get((ItemStack)piece)).hasType() || !nbt.hasTag(NBT_MODS)) continue;
            try {
                List mods;
                String json = nbt.getString(NBT_MODS);
                if (json == null || json.isEmpty() || (mods = (List)this.gson.fromJson(json, ModifierEngine.ROLLED_LIST_TYPE)) == null) continue;
                for (ModifierEngine.RolledModifier mod : mods) {
                    Double armorVal = mod.stats().get("armor");
                    if (armorVal == null) continue;
                    bonusArmor += armorVal.doubleValue();
                }
            }
            catch (Exception exception) {
                // empty catch block
            }
        }
        if (bonusArmor <= 0.0) {
            return;
        }
        double reduction = Math.min(0.75, bonusArmor * 0.025);
        event.setDamage(event.getDamage() * (1.0 - reduction));
    }

    private String shortStatName(String rawKey) {
        String lower = rawKey.replace("-", " ").replace("_", " ").toLowerCase();
        return this.capitalize(lower);
    }

    private String formatModifierLine(String statKey, double val) {
        if (val == 0.0) {
            return null;
        }
        String name = this.shortStatName(statKey);
        Integer maxTier = ENCHANT_TIER_STATS.get(statKey);
        if (maxTier != null) {
            int tier = Math.max(1, Math.min(maxTier, (int)Math.round(Math.abs(val))));
            String sign = val >= 0.0 ? "&a+" : "&c-";
            return "  &7" + name + ": " + sign + tier + " Tier";
        }
        if (INTEGER_STATS.contains(statKey)) {
            long rounded = Math.round(val);
            if (rounded == 0L) {
                return null;
            }
            if (rounded > 0L) {
                return "  &7" + name + ": &a+" + rounded;
            }
            return "  &7" + name + ": &c" + rounded;
        }
        if (PERCENT_STATS.contains(statKey)) {
            if (val >= 0.0) {
                return "  &7" + name + ": &a+" + String.format("%.1f", val) + "%";
            }
            return "  &7" + name + ": &c" + String.format("%.1f", val) + "%";
        }
        if (val >= 0.0) {
            return "  &7" + name + ": &a+" + String.format("%.1f", val);
        }
        return "  &7" + name + ": &c" + String.format("%.1f", val);
    }
}

