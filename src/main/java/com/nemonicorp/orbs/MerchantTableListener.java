/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.gson.Gson
 *  com.google.gson.reflect.TypeToken
 *  io.lumine.mythic.lib.api.item.ItemTag
 *  io.lumine.mythic.lib.api.item.NBTItem
 *  net.Indyuce.mmoitems.MMOItems
 *  net.Indyuce.mmoitems.api.Type
 *  net.kyori.adventure.text.Component
 *  net.kyori.adventure.text.TextComponent
 *  net.kyori.adventure.text.format.NamedTextColor
 *  net.kyori.adventure.text.format.TextColor
 *  net.kyori.adventure.text.format.TextDecoration
 *  net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer
 *  org.bukkit.Bukkit
 *  org.bukkit.ChatColor
 *  org.bukkit.Color
 *  org.bukkit.Location
 *  org.bukkit.Material
 *  org.bukkit.OfflinePlayer
 *  org.bukkit.Particle
 *  org.bukkit.Particle$DustOptions
 *  org.bukkit.Sound
 *  org.bukkit.World
 *  org.bukkit.enchantments.Enchantment
 *  org.bukkit.entity.Entity
 *  org.bukkit.entity.HumanEntity
 *  org.bukkit.entity.ItemFrame
 *  org.bukkit.entity.Player
 *  org.bukkit.event.EventHandler
 *  org.bukkit.event.EventPriority
 *  org.bukkit.event.Listener
 *  org.bukkit.event.block.Action
 *  org.bukkit.event.entity.EntityDamageEvent
 *  org.bukkit.event.inventory.ClickType
 *  org.bukkit.event.inventory.InventoryAction
 *  org.bukkit.event.inventory.InventoryClickEvent
 *  org.bukkit.event.inventory.InventoryCloseEvent
 *  org.bukkit.event.inventory.InventoryDragEvent
 *  org.bukkit.event.player.PlayerInteractEvent
 *  org.bukkit.event.player.PlayerItemConsumeEvent
 *  org.bukkit.event.player.PlayerItemHeldEvent
 *  org.bukkit.event.player.PlayerMoveEvent
 *  org.bukkit.inventory.Inventory
 *  org.bukkit.inventory.InventoryView
 *  org.bukkit.inventory.ItemStack
 *  org.bukkit.inventory.meta.Damageable
 *  org.bukkit.inventory.meta.EnchantmentStorageMeta
 *  org.bukkit.inventory.meta.ItemMeta
 *  org.bukkit.inventory.meta.PotionMeta
 *  org.bukkit.plugin.Plugin
 *  org.bukkit.potion.PotionEffect
 *  org.bukkit.potion.PotionEffectType
 *  org.bukkit.scheduler.BukkitTask
 */
package com.nemonicorp.orbs;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.nemonicorp.orbs.MerchantRouteManager;
import com.nemonicorp.orbs.NemonicOrbPlugin;
import com.nemonicorp.orbs.OrbListener;
import io.lumine.mythic.lib.api.item.ItemTag;
import io.lumine.mythic.lib.api.item.NBTItem;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import net.Indyuce.mmoitems.MMOItems;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Entity;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.ItemFrame;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitTask;

public class MerchantTableListener
implements Listener {
    private final NemonicOrbPlugin plugin;
    private final OrbListener orbListener;
    private final MerchantRouteManager routeManager;
    private static final Gson GSON = new Gson();
    private final Map<UUID, MerchantSession> activeSessions = new ConcurrentHashMap<UUID, MerchantSession>();
    private final Map<UUID, Long> avaliacaoCooldowns = new ConcurrentHashMap<UUID, Long>();
    private final Map<UUID, Long> craftPergCooldowns = new ConcurrentHashMap<UUID, Long>();
    private final Map<UUID, Long> craftPocaoCooldowns = new ConcurrentHashMap<UUID, Long>();
    private final Map<UUID, Long> enchantCooldowns = new ConcurrentHashMap<UUID, Long>();
    private final Map<UUID, Long> craftPedraCooldowns = new ConcurrentHashMap<UUID, Long>();
    private final Map<UUID, Long> seloCraftCooldowns = new ConcurrentHashMap<UUID, Long>();
    private final Map<UUID, Long> teleportCooldowns = new ConcurrentHashMap<UUID, Long>();
    private final Map<UUID, BukkitTask> castingTasks = new ConcurrentHashMap<UUID, BukkitTask>();
    private final Map<UUID, Location> castingStartLocs = new ConcurrentHashMap<UUID, Location>();
    private final Map<UUID, ItemStack[]> hotbarBackups = new ConcurrentHashMap<UUID, ItemStack[]>();
    private final Map<UUID, List<MerchantRouteManager.WaypointInfo>> castingDestinations = new ConcurrentHashMap<UUID, List<MerchantRouteManager.WaypointInfo>>();
    private final Map<UUID, Integer> selectedDestination = new ConcurrentHashMap<UUID, Integer>();
    private static final String ROUTES_GUI_TITLE = "Rotas Comerciais";
    private static final int ROUTES_GUI_SIZE = 27;
    private final Map<String, CachedScan> scanCache = new ConcurrentHashMap<String, CachedScan>();
    private final Map<UUID, BukkitTask> auraTasks = new ConcurrentHashMap<UUID, BukkitTask>();
    private static final int GUI_SIZE = 54;
    private static final int SLOT_EFFICIENCY = 3;
    private static final int SLOT_INFO = 5;
    private static final int SLOT_ROUTES = 7;
    private static final int SLOT_ITEM_IN = 10;
    private static final int SLOT_ARROW_AV_1 = 11;
    private static final int SLOT_AVALIAR = 12;
    private static final int SLOT_ARROW_AV_2 = 13;
    private static final int SLOT_ITEM_OUT = 14;
    private static final int SLOT_CRAFT_PERG = 16;
    private static final int SLOT_MAT_IN = 28;
    private static final int SLOT_ARROW_POC_1 = 29;
    private static final int SLOT_POCAO = 30;
    private static final int SLOT_ARROW_POC_2 = 31;
    private static final int SLOT_POC_OUT = 32;
    private static final int SLOT_POC_OUT2 = 33;
    private static final int SLOT_LIVRO = 37;
    private static final int SLOT_ARROW_ENC_1 = 38;
    private static final int SLOT_ENCANTAR = 39;
    private static final int SLOT_ARROW_ENC_2 = 40;
    private static final int SLOT_ENC_OUT = 41;
    private static final int SLOT_CRAFT_PEDRA = 42;
    private static final int SLOT_CRAFT_SELO = 48;
    private static final int SLOT_CANCEL = 53;
    private static final String GUI_TITLE_RAW = "Mesa do Mercador";
    private static final Set<Integer> INPUT_SLOTS = Set.of(Integer.valueOf(10), Integer.valueOf(28), Integer.valueOf(37));
    private static final Set<Integer> OUTPUT_SLOTS = Set.of(Integer.valueOf(14), Integer.valueOf(32), Integer.valueOf(33), Integer.valueOf(41));
    private static final Set<Integer> BUTTON_SLOTS = Set.of(Integer.valueOf(12), Integer.valueOf(16), Integer.valueOf(30), Integer.valueOf(39), Integer.valueOf(42), Integer.valueOf(7), Integer.valueOf(48), Integer.valueOf(53));
    private static final Map<Material, double[]> MERCHANT_BLOCKS = Map.ofEntries(Map.entry(Material.BARREL, new double[]{0.04, 10.0}), Map.entry(Material.CHEST, new double[]{0.03, 8.0}), Map.entry(Material.LECTERN, new double[]{0.06, 4.0}), Map.entry(Material.EMERALD_BLOCK, new double[]{0.1, 4.0}), Map.entry(Material.CARTOGRAPHY_TABLE, new double[]{0.05, 3.0}));
    private static final Map<String, Integer> INDICATOR_THRESHOLDS = Map.of("BARREL", 3, "CHEST", 2, "LECTERN", 1, "EMERALD_BLOCK", 1, "CARTOGRAPHY_TABLE", 1, "ITEM_FRAME", 4);
    private static final String RECALL_POTION_TAG = "NEMONICORB_RECALL";

    public MerchantTableListener(NemonicOrbPlugin plugin, OrbListener orbListener, MerchantRouteManager routeManager) {
        this.plugin = plugin;
        this.orbListener = orbListener;
        this.routeManager = routeManager;
    }

    private boolean debug() {
        return this.plugin.getConfig().getBoolean("debug", true);
    }

    public boolean isMercador(Player player) {
        try {
            boolean result;
            String className = this.getPlayerClassName(player);
            String configClassName = this.plugin.getConfig().getString("merchant.class-name", "Mercador");
            boolean bl = result = className != null && className.equalsIgnoreCase(configClassName);
            if (this.debug()) {
                this.plugin.getLogger().info("[MERCHANT] isMercador: jogador=" + player.getName() + " classeDetectada='" + className + "' configClasse='" + configClassName + "' resultado=" + result);
            }
            return result;
        }
        catch (Exception e) {
            this.plugin.getLogger().warning("[MERCHANT] Erro ao verificar classe: " + e.getMessage());
            return false;
        }
    }

    private String getPlayerClassName(Player player) {
        try {
            Class<?> pdClass = Class.forName("net.Indyuce.mmocore.api.player.PlayerData");
            Object data = this.getPlayerData(pdClass, player);
            if (data == null) {
                return null;
            }
            Object playerClass = null;
            for (String m : new String[]{"getProfess", "getPlayerClass", "getMMOClass"}) {
                try {
                    playerClass = pdClass.getMethod(m, new Class[0]).invoke(data, new Object[0]);
                    break;
                }
                catch (NoSuchMethodException noSuchMethodException) {
                }
            }
            if (playerClass == null) {
                return null;
            }
            for (String m : new String[]{"getName", "getId", "getKey"}) {
                try {
                    String s;
                    Object r = playerClass.getClass().getMethod(m, new Class[0]).invoke(playerClass, new Object[0]);
                    if (!(r instanceof String) || (s = (String)r).isEmpty()) continue;
                    return s;
                }
                catch (NoSuchMethodException noSuchMethodException) {
                    // empty catch block
                }
            }
            return null;
        }
        catch (ClassNotFoundException e) {
            return null;
        }
        catch (Exception e) {
            this.plugin.getLogger().warning("[MERCHANT] Erro ao obter classe: " + e.getMessage());
            return null;
        }
    }

    public int getPlayerLevel(Player player) {
        try {
            Class<?> pdClass = Class.forName("net.Indyuce.mmocore.api.player.PlayerData");
            Object data = this.getPlayerData(pdClass, player);
            if (data == null) {
                return 0;
            }
            return (Integer)pdClass.getMethod("getLevel", new Class[0]).invoke(data, new Object[0]);
        }
        catch (Exception e) {
            return 0;
        }
    }

    private Object getPlayerData(Class<?> pdClass, Player player) throws Exception {
        try {
            return pdClass.getMethod("get", OfflinePlayer.class).invoke(null, player);
        }
        catch (NoSuchMethodException e1) {
            try {
                return pdClass.getMethod("get", Player.class).invoke(null, player);
            }
            catch (NoSuchMethodException e2) {
                return pdClass.getMethod("get", UUID.class).invoke(null, player.getUniqueId());
            }
        }
    }

    private double getPlayerMana(Player player) {
        try {
            Class<?> pdClass = Class.forName("net.Indyuce.mmocore.api.player.PlayerData");
            Object data = this.getPlayerData(pdClass, player);
            if (data == null) {
                return 0.0;
            }
            for (String m : new String[]{"getMana"}) {
                try {
                    Object result = pdClass.getMethod(m, new Class[0]).invoke(data, new Object[0]);
                    if (!(result instanceof Number)) continue;
                    Number n = (Number)result;
                    return n.doubleValue();
                }
                catch (NoSuchMethodException noSuchMethodException) {
                    // empty catch block
                }
            }
            return 0.0;
        }
        catch (Exception e) {
            return 0.0;
        }
    }

    private boolean deductPlayerMana(Player player, double amount) {
        try {
            Class<?> pdClass = Class.forName("net.Indyuce.mmocore.api.player.PlayerData");
            Object data = this.getPlayerData(pdClass, player);
            if (data == null) {
                return false;
            }
            double currentMana = this.getPlayerMana(player);
            if (currentMana < amount) {
                return false;
            }
            try {
                pdClass.getMethod("giveMana", Double.TYPE).invoke(data, -amount);
                return true;
            }
            catch (NoSuchMethodException noSuchMethodException) {
                try {
                    pdClass.getMethod("setMana", Double.TYPE).invoke(data, currentMana - amount);
                    return true;
                }
                catch (NoSuchMethodException noSuchMethodException2) {
                    return false;
                }
            }
        }
        catch (Exception e) {
            this.plugin.getLogger().warning("[MERCHANT] Erro ao deduzir mana: " + e.getMessage());
            return false;
        }
    }

    private int getPlayerXP(Player player) {
        try {
            Class<?> pdClass = Class.forName("net.Indyuce.mmocore.api.player.PlayerData");
            Object data = this.getPlayerData(pdClass, player);
            if (data == null) {
                return 0;
            }
            for (String m : new String[]{"getExperience", "getExp"}) {
                try {
                    Object result = pdClass.getMethod(m, new Class[0]).invoke(data, new Object[0]);
                    if (!(result instanceof Number)) continue;
                    Number n = (Number)result;
                    return n.intValue();
                }
                catch (NoSuchMethodException noSuchMethodException) {
                    // empty catch block
                }
            }
            return 0;
        }
        catch (Exception e) {
            return 0;
        }
    }

    private boolean deductPlayerXP(Player player, int amount) {
        try {
            Class<?> pdClass = Class.forName("net.Indyuce.mmocore.api.player.PlayerData");
            Object data = this.getPlayerData(pdClass, player);
            if (data == null) {
                return false;
            }
            int currentXP = this.getPlayerXP(player);
            if (currentXP < amount) {
                return false;
            }
            for (String m : new String[]{"giveExperience"}) {
                try {
                    pdClass.getMethod(m, Double.TYPE, null).invoke(data, -amount, null);
                    return true;
                }
                catch (Exception exception) {
                    try {
                        try {
                            pdClass.getMethod(m, Integer.TYPE).invoke(data, -amount);
                            return true;
                        }
                        catch (Exception exception2) {
                        }
                    }
                    catch (Exception exception3) {
                        // empty catch block
                    }
                }
            }
            for (String m : new String[]{"setExperience", "setExp"}) {
                try {
                    Method method = pdClass.getMethod(m, Double.TYPE);
                    method.invoke(data, currentXP - amount);
                    return true;
                }
                catch (NoSuchMethodException method) {
                    try {
                        Method method2 = pdClass.getMethod(m, Integer.TYPE);
                        method2.invoke(data, currentXP - amount);
                        return true;
                    }
                    catch (NoSuchMethodException noSuchMethodException) {
                    }
                }
            }
            return false;
        }
        catch (Exception e) {
            this.plugin.getLogger().warning("[MERCHANT] Erro ao deduzir XP: " + e.getMessage());
            return false;
        }
    }

    private MerchantEnvironment scanEnvironment(Location loc, Player player) {
        ItemFrame frame;
        ItemStack itemStack;
        Entity entity;
        double distSq;
        int y;
        int x;
        String cacheKey = loc.getWorld().getName() + "," + loc.getBlockX() + "," + loc.getBlockY() + "," + loc.getBlockZ();
        long cacheTTL = this.plugin.getConfig().getLong("merchant.scan-cache-seconds", 60L) * 1000L;
        CachedScan cached = this.scanCache.get(cacheKey);
        if (cached != null && System.currentTimeMillis() - cached.timestamp() < cacheTTL) {
            MerchantEnvironment old = cached.env();
            NearbyClassInfo classInfo = this.scanNearbyClasses(loc, player);
            double playerBonus = Math.min((double)classInfo.count * 0.05, 0.25);
            return new MerchantEnvironment(old.blockCounts(), old.itemFrameCount(), old.totalBonus(), classInfo.count, playerBonus, classInfo.alquimista, classInfo.ferreiro, classInfo.guerreiro, classInfo.mercador, old.enchantingTablePresent(), old.anvilPresent());
        }
        int radius = this.plugin.getConfig().getInt("merchant.scan-radius", 20);
        int mesaRadius = this.plugin.getConfig().getInt("merchant.mesa-detection-radius", 4);
        EnumMap<Material, Integer> blockCounts = new EnumMap<Material, Integer>(Material.class);
        World world = loc.getWorld();
        int cx = loc.getBlockX();
        int cy = loc.getBlockY();
        int cz = loc.getBlockZ();
        boolean enchantingTablePresent = false;
        boolean anvilPresent = false;
        for (x = cx - radius; x <= cx + radius; ++x) {
            for (y = Math.max(world.getMinHeight(), cy - radius); y <= Math.min(world.getMaxHeight() - 1, cy + radius); ++y) {
                for (int z = cz - radius; z <= cz + radius; ++z) {
                    Material material;
                    distSq = (x - cx) * (x - cx) + (y - cy) * (y - cy) + (z - cz) * (z - cz);
                    if (distSq > (double)(radius * radius) || !MERCHANT_BLOCKS.containsKey(material = world.getBlockAt(x, y, z).getType())) continue;
                    blockCounts.merge(material, 1, Integer::sum);
                }
            }
        }
        for (x = cx - mesaRadius; x <= cx + mesaRadius; ++x) {
            for (y = Math.max(world.getMinHeight(), cy - mesaRadius); y <= Math.min(world.getMaxHeight() - 1, cy + mesaRadius); ++y) {
                for (int z2 = cz - mesaRadius; z2 <= cz + mesaRadius; ++z2) {
                    distSq = (x - cx) * (x - cx) + (y - cy) * (y - cy) + (z2 - cz) * (z2 - cz);
                    if (distSq > (double)(mesaRadius * mesaRadius)) continue;
                    Material material = world.getBlockAt(x, y, z2).getType();
                    if (material == Material.ENCHANTING_TABLE) {
                        enchantingTablePresent = true;
                    }
                    if (material != Material.ANVIL && material != Material.CHIPPED_ANVIL && material != Material.DAMAGED_ANVIL) continue;
                    anvilPresent = true;
                }
            }
        }
        int itemFrameCount = 0;
        int maxItemFrames = 12;
        Iterator z2 = world.getNearbyEntities(loc, (double)radius, (double)radius, (double)radius).iterator();
        while (z2.hasNext() && (!((entity = (Entity)z2.next()) instanceof ItemFrame) || (itemStack = (frame = (ItemFrame)entity).getItem()) == null || itemStack.getType().isAir() || ++itemFrameCount < maxItemFrames)) {
        }
        double totalBonus = 0.0;
        for (Map.Entry entry : blockCounts.entrySet()) {
            double[] cfg = MERCHANT_BLOCKS.get(entry.getKey());
            if (cfg == null) continue;
            int count = Math.min((Integer)entry.getValue(), (int)cfg[1]);
            totalBonus += (double)count * cfg[0];
        }
        NearbyClassInfo classInfo = this.scanNearbyClasses(loc, player);
        double d = Math.min((double)classInfo.count * 0.05, 0.25);
        MerchantEnvironment env = new MerchantEnvironment(blockCounts, itemFrameCount, totalBonus, classInfo.count, d, classInfo.alquimista, classInfo.ferreiro, classInfo.guerreiro, classInfo.mercador, enchantingTablePresent, anvilPresent);
        this.scanCache.put(cacheKey, new CachedScan(env, System.currentTimeMillis()));
        if (this.debug()) {
            this.plugin.getLogger().info("[MERCHANT-SCAN] bonus=" + String.format("%.0f%%", totalBonus * 100.0) + " itemFrames=" + itemFrameCount + " jogadores=" + classInfo.count + " (+=" + String.format("%.0f%%", d * 100.0) + ") alquimista=" + classInfo.alquimista + " ferreiro=" + classInfo.ferreiro + " guerreiro=" + classInfo.guerreiro + " mercador=" + classInfo.mercador + " enchTable=" + enchantingTablePresent + " anvil=" + anvilPresent);
        }
        return env;
    }

    private NearbyClassInfo scanNearbyClasses(Location loc, Player exclude) {
        int radius = this.plugin.getConfig().getInt("merchant.scan-radius", 20);
        int count = 0;
        boolean mercador = false;
        boolean alquimista = false;
        boolean ferreiro = false;
        boolean guerreiro = false;
        String mercadorClass = this.plugin.getConfig().getString("merchant.class-name", "Mercador");
        String alquimistaClass = this.plugin.getConfig().getString("transmutation.class-name", "Alquimista");
        String ferreiroClass = this.plugin.getConfig().getString("blacksmith.class-name", "Ferreiro");
        String guerreiroClass = this.plugin.getConfig().getString("merchant.guerreiro-class-name", "Guerreiro");
        for (Player p : loc.getWorld().getPlayers()) {
            if (p.equals((Object)exclude) || p.getLocation().distanceSquared(loc) > (double)(radius * radius)) continue;
            ++count;
            String cls = this.getPlayerClassName(p);
            if (cls == null) continue;
            if (mercadorClass.equalsIgnoreCase(cls)) {
                mercador = true;
            }
            if (alquimistaClass.equalsIgnoreCase(cls)) {
                alquimista = true;
            }
            if (ferreiroClass.equalsIgnoreCase(cls)) {
                ferreiro = true;
            }
            if (!guerreiroClass.equalsIgnoreCase(cls)) continue;
            guerreiro = true;
        }
        return new NearbyClassInfo(count, mercador, alquimista, ferreiro, guerreiro);
    }

    private double calculateEfficiency(MerchantEnvironment env) {
        double eff = 1.0 + env.totalBonus() + (double)env.itemFrameCount() * 0.02 + env.playerBonus();
        if (env.alquimistaNearby()) {
            eff += 0.08;
        }
        if (env.guerreiroNearby()) {
            eff -= 0.03;
        }
        if (env.mercadorNearby()) {
            eff -= 0.05;
        }
        return eff;
    }

    private int getBreakpoint(double efficiency) {
        double pct = efficiency * 100.0;
        if (pct >= 290.0) {
            return 5;
        }
        if (pct >= 250.0) {
            return 4;
        }
        if (pct >= 210.0) {
            return 3;
        }
        if (pct >= 170.0) {
            return 2;
        }
        if (pct >= 130.0) {
            return 1;
        }
        return 0;
    }

    private String getBreakpointName(int breakpoint) {
        return switch (breakpoint) {
            case 0 -> "Ambulante";
            case 1 -> "Lojista";
            case 2 -> "Comerciante";
            case 3 -> "Negociante";
            case 4 -> "Magnata";
            case 5 -> "Barao do Comercio";
            default -> "Desconhecido";
        };
    }

    private NamedTextColor getBreakpointColor(int breakpoint) {
        return switch (breakpoint) {
            case 0 -> NamedTextColor.GRAY;
            case 1 -> NamedTextColor.WHITE;
            case 2 -> NamedTextColor.GREEN;
            case 3 -> NamedTextColor.AQUA;
            case 4 -> NamedTextColor.GOLD;
            case 5 -> NamedTextColor.LIGHT_PURPLE;
            default -> NamedTextColor.GRAY;
        };
    }

    public void openGUI(Player player, Location tableLoc) {
        int i;
        ItemStack lockedGlass;
        int minLevel = this.plugin.getConfig().getInt("merchant.min-level", 5);
        int playerLevel = this.getPlayerLevel(player);
        if (playerLevel < minLevel) {
            player.sendMessage(ChatColor.translateAlternateColorCodes((char)'&', (String)this.plugin.getConfig().getString("merchant.messages.level-too-low", "&c[Mercador] Voce precisa de nivel %level% na classe Mercador!").replace("%level%", String.valueOf(minLevel))));
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
            return;
        }
        MerchantEnvironment env = this.scanEnvironment(tableLoc, player);
        double efficiency = this.calculateEfficiency(env);
        int breakpoint = this.getBreakpoint(efficiency);
        boolean alquimistaRowActive = env.enchantingTablePresent();
        boolean ferreiroRowActive = env.anvilPresent();
        Inventory gui = Bukkit.createInventory(null, (int)54, (Component)Component.text((String)GUI_TITLE_RAW, (TextColor)NamedTextColor.GOLD, (TextDecoration[])new TextDecoration[]{TextDecoration.BOLD}));
        ItemStack blackGlass = this.createGlassPane(Material.BLACK_STAINED_GLASS_PANE, " ");
        for (int i2 = 0; i2 < 54; ++i2) {
            gui.setItem(i2, blackGlass);
        }
        ItemStack arrowPane = this.createArrowPane();
        gui.setItem(3, this.createEfficiencyItem(env, efficiency, playerLevel));
        gui.setItem(5, this.createInfoItem(alquimistaRowActive, ferreiroRowActive));
        if (playerLevel >= 10) {
            gui.setItem(7, this.createRoutesButton());
        }
        gui.setItem(10, null);
        gui.setItem(11, arrowPane);
        gui.setItem(12, this.createAvaliarButton());
        gui.setItem(13, arrowPane);
        gui.setItem(14, null);
        gui.setItem(16, this.createCraftPergButton());
        ItemStack goldGlass = this.createGlassPane(Material.YELLOW_STAINED_GLASS_PANE, " ");
        for (int i3 = 18; i3 <= 26; ++i3) {
            gui.setItem(i3, goldGlass);
        }
        if (alquimistaRowActive) {
            gui.setItem(28, null);
            gui.setItem(29, arrowPane);
            gui.setItem(30, this.createPocaoButton());
            gui.setItem(31, arrowPane);
            gui.setItem(32, null);
            gui.setItem(33, null);
        } else {
            ItemStack grayGlass = this.createGlassPane(Material.GRAY_STAINED_GLASS_PANE, " ");
            lockedGlass = this.createLockedRowPane("Requer Mesa de Transmutacao proxima");
            for (i = 27; i <= 35; ++i) {
                gui.setItem(i, grayGlass);
            }
            gui.setItem(30, lockedGlass);
        }
        if (ferreiroRowActive) {
            ItemStack bookLabel = this.createGlassPane(Material.PURPLE_STAINED_GLASS_PANE, " ");
            ItemMeta blMeta = bookLabel.getItemMeta();
            if (blMeta != null) {
                blMeta.displayName((Component)Component.text((String)"Coloque o Livro Encantado abaixo", (TextColor)NamedTextColor.LIGHT_PURPLE));
                ArrayList<TextComponent> blLore = new ArrayList<TextComponent>();
                blLore.add(Component.text((String)"O equipamento vai no slot", (TextColor)NamedTextColor.GRAY));
                blLore.add(Component.text((String)"da Linha 1 (ITEM_IN)", (TextColor)NamedTextColor.GRAY));
                blMeta.lore(blLore);
                bookLabel.setItemMeta(blMeta);
            }
            gui.setItem(36, bookLabel);
            gui.setItem(37, null);
            gui.setItem(38, arrowPane);
            gui.setItem(39, this.createEncantarButton());
            gui.setItem(40, arrowPane);
            gui.setItem(41, null);
            gui.setItem(42, this.createCraftPedraButton());
        } else {
            ItemStack grayGlass = this.createGlassPane(Material.GRAY_STAINED_GLASS_PANE, " ");
            lockedGlass = this.createLockedRowPane("Requer Mesa do Ferreiro proxima");
            for (i = 36; i <= 44; ++i) {
                gui.setItem(i, grayGlass);
            }
            gui.setItem(39, lockedGlass);
        }
        if (playerLevel >= 10) {
            gui.setItem(48, this.createCraftSeloButton());
        }
        gui.setItem(53, this.createCancelButton());
        MerchantSession session = new MerchantSession(tableLoc, env, efficiency, breakpoint, playerLevel, alquimistaRowActive, ferreiroRowActive);
        this.activeSessions.put(player.getUniqueId(), session);
        player.openInventory(gui);
        player.playSound(tableLoc, Sound.BLOCK_ENCHANTMENT_TABLE_USE, 0.8f, 0.5f);
        this.startAura(player, tableLoc, efficiency);
        if (this.debug()) {
            this.plugin.getLogger().info("[MERCHANT] GUI aberta para " + player.getName() + " nivel=" + playerLevel + " eficiencia=" + String.format("%.0f%%", efficiency * 100.0) + " breakpoint=" + breakpoint + " (" + this.getBreakpointName(breakpoint) + ") alqRow=" + alquimistaRowActive + " ferRow=" + ferreiroRowActive);
        }
    }

    @EventHandler(priority=EventPriority.HIGH)
    public void onInventoryClick(InventoryClickEvent event) {
        HumanEntity humanEntity = event.getWhoClicked();
        if (!(humanEntity instanceof Player)) {
            return;
        }
        Player player = (Player)humanEntity;
        String title = this.getInventoryTitle(event.getView());
        if (!GUI_TITLE_RAW.equals(title)) {
            return;
        }
        MerchantSession session = this.activeSessions.get(player.getUniqueId());
        if (session == null) {
            event.setCancelled(true);
            return;
        }
        int slot = event.getRawSlot();
        Inventory topInv = event.getView().getTopInventory();
        if (event.getClick() == ClickType.DOUBLE_CLICK || event.getClick() == ClickType.SWAP_OFFHAND || event.getAction() == InventoryAction.COLLECT_TO_CURSOR) {
            event.setCancelled(true);
            return;
        }
        if (!(event.getHotbarButton() < 0 || slot >= 54 || slot == 10 || slot == 28 && session.alquimistaRowActive() || slot == 37 && session.ferreiroRowActive())) {
            event.setCancelled(true);
            return;
        }
        if (slot >= 54) {
            if (event.isShiftClick()) {
                event.setCancelled(true);
                ItemStack clicked = event.getCurrentItem();
                if (clicked == null || clicked.getType().isAir()) {
                    return;
                }
                if (this.isSlotEmpty(topInv, 10)) {
                    topInv.setItem(10, clicked.clone());
                    event.setCurrentItem(null);
                    return;
                }
                if (session.alquimistaRowActive() && this.isSlotEmpty(topInv, 28)) {
                    topInv.setItem(28, clicked.clone());
                    event.setCurrentItem(null);
                    return;
                }
                if (session.ferreiroRowActive() && this.isSlotEmpty(topInv, 37)) {
                    topInv.setItem(37, clicked.clone());
                    event.setCurrentItem(null);
                    return;
                }
            }
            return;
        }
        if (slot == 10) {
            return;
        }
        if (slot == 28 && session.alquimistaRowActive()) {
            return;
        }
        if (slot == 37 && session.ferreiroRowActive()) {
            return;
        }
        if (OUTPUT_SLOTS.contains(slot)) {
            if (!(slot != 32 && slot != 33 || session.alquimistaRowActive())) {
                event.setCancelled(true);
                return;
            }
            if (slot == 41 && !session.ferreiroRowActive()) {
                event.setCancelled(true);
                return;
            }
            ItemStack outputItem = topInv.getItem(slot);
            if (outputItem != null && !outputItem.getType().isAir() && !this.isGUIDecoration(outputItem)) {
                if (event.isShiftClick()) {
                    event.setCancelled(true);
                    topInv.setItem(slot, null);
                    HashMap overflow = player.getInventory().addItem(new ItemStack[]{outputItem});
                    for (ItemStack drop : overflow.values()) {
                        player.getWorld().dropItemNaturally(player.getLocation(), drop);
                    }
                    this.syncInventoryLater(player);
                }
                this.syncInventoryLater(player);
                return;
            }
            event.setCancelled(true);
            return;
        }
        event.setCancelled(true);
        if (BUTTON_SLOTS.contains(slot)) {
            switch (slot) {
                case 12: {
                    this.handleAvaliacao(player, topInv, session);
                    break;
                }
                case 16: {
                    this.handleCraftPergaminho(player, topInv, session);
                    break;
                }
                case 30: {
                    if (session.alquimistaRowActive()) {
                        this.handleCraftPocao(player, topInv, session);
                        break;
                    }
                    this.sendLockedMessage(player, "Mesa de Transmutacao");
                    break;
                }
                case 39: {
                    if (session.ferreiroRowActive()) {
                        this.handleEncantar(player, topInv, session);
                        break;
                    }
                    this.sendLockedMessage(player, "Mesa do Ferreiro");
                    break;
                }
                case 42: {
                    if (session.ferreiroRowActive()) {
                        this.handleCraftPedra(player, topInv, session);
                        break;
                    }
                    this.sendLockedMessage(player, "Mesa do Ferreiro");
                    break;
                }
                case 7: {
                    this.handleRoutes(player, session);
                    break;
                }
                case 48: {
                    this.handleCraftSelo(player, session);
                    break;
                }
                case 53: {
                    player.closeInventory();
                }
            }
        }
    }

    @EventHandler(priority=EventPriority.HIGH)
    public void onInventoryDrag(InventoryDragEvent event) {
        HumanEntity humanEntity = event.getWhoClicked();
        if (!(humanEntity instanceof Player)) {
            return;
        }
        Player player = (Player)humanEntity;
        String title = this.getInventoryTitle(event.getView());
        if (!GUI_TITLE_RAW.equals(title)) {
            return;
        }
        MerchantSession session = this.activeSessions.get(player.getUniqueId());
        if (session == null) {
            event.setCancelled(true);
            return;
        }
        HashSet<Integer> allowed = new HashSet<Integer>();
        allowed.add(10);
        if (session.alquimistaRowActive()) {
            allowed.add(28);
        }
        if (session.ferreiroRowActive()) {
            allowed.add(37);
        }
        Iterator iterator = event.getRawSlots().iterator();
        while (iterator.hasNext()) {
            int slot = (Integer)iterator.next();
            if (slot >= 54 || allowed.contains(slot)) continue;
            event.setCancelled(true);
            return;
        }
    }

    @EventHandler(priority=EventPriority.HIGH)
    public void onInventoryClose(InventoryCloseEvent event) {
        int[] returnSlots;
        HumanEntity humanEntity = event.getPlayer();
        if (!(humanEntity instanceof Player)) {
            return;
        }
        Player player = (Player)humanEntity;
        String title = this.getInventoryTitle(event.getView());
        if (ROUTES_GUI_TITLE.equals(title)) {
            Bukkit.getScheduler().runTaskLater((Plugin)this.plugin, () -> {
                MerchantSession s;
                if (!player.isOnline()) {
                    MerchantSession s2 = this.activeSessions.remove(player.getUniqueId());
                    if (s2 != null) {
                        this.stopAura(player);
                    }
                    return;
                }
                String openTitle = this.getInventoryTitle(player.getOpenInventory());
                if (!GUI_TITLE_RAW.equals(openTitle) && !ROUTES_GUI_TITLE.equals(openTitle) && (s = this.activeSessions.remove(player.getUniqueId())) != null) {
                    this.stopAura(player);
                }
            }, 2L);
            return;
        }
        if (!GUI_TITLE_RAW.equals(title)) {
            return;
        }
        MerchantSession session = this.activeSessions.remove(player.getUniqueId());
        if (session == null) {
            return;
        }
        this.stopAura(player);
        Inventory inv = event.getView().getTopInventory();
        for (int slot : returnSlots = new int[]{10, 14, 28, 32, 33, 37, 41}) {
            ItemStack item = inv.getItem(slot);
            inv.setItem(slot, null);
            if (item == null || item.getType().isAir() || this.isGUIDecoration(item)) continue;
            HashMap leftover = player.getInventory().addItem(new ItemStack[]{item});
            for (ItemStack drop : leftover.values()) {
                player.getWorld().dropItemNaturally(player.getLocation(), drop);
            }
        }
    }

    private void handleAvaliacao(Player player, Inventory gui, MerchantSession session) {
        int cappedLevel;
        int itemLevelBefore;
        NBTItem resultNbt;
        ItemStack result;
        long now;
        block19: {
            int identified;
            int minLevel = this.plugin.getConfig().getInt("merchant.min-level", 5);
            if (session.playerLevel() < minLevel) {
                player.sendMessage(ChatColor.translateAlternateColorCodes((char)'&', (String)("&c[Mercador] Voce precisa de nivel " + minLevel + " para avaliar!")));
                player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
                return;
            }
            long cooldownMs = this.plugin.getConfig().getLong("merchant.avaliacao-cooldown-seconds", 15L) * 1000L;
            Long lastUse = this.avaliacaoCooldowns.get(player.getUniqueId());
            now = System.currentTimeMillis();
            if (lastUse != null && now - lastUse < cooldownMs) {
                long remaining = (cooldownMs - (now - lastUse)) / 1000L;
                player.sendMessage(ChatColor.translateAlternateColorCodes((char)'&', (String)("&c[Mercador] Aguarde " + remaining + "s antes de avaliar novamente!")));
                player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
                return;
            }
            ItemStack input = gui.getItem(10);
            if (input == null || input.getType().isAir()) {
                player.sendMessage(ChatColor.translateAlternateColorCodes((char)'&', (String)"&c[Mercador] Coloque um item no slot de entrada!"));
                player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
                return;
            }
            ItemStack existingOut = gui.getItem(14);
            if (existingOut != null && !existingOut.getType().isAir()) {
                player.sendMessage(ChatColor.translateAlternateColorCodes((char)'&', (String)"&c[Mercador] Retire o item do slot de saida primeiro!"));
                player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
                return;
            }
            NBTItem nbt = NBTItem.get((ItemStack)input);
            if (!nbt.hasTag("NEMONICORB_TIER")) {
                player.sendMessage(ChatColor.translateAlternateColorCodes((char)'&', (String)"&c[Mercador] Este item nao pode ser avaliado!"));
                player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
                return;
            }
            int n = identified = nbt.hasTag("NEMONICORB_IDENTIFIED") ? nbt.getInteger("NEMONICORB_IDENTIFIED") : 0;
            if (identified != 0) {
                player.sendMessage(ChatColor.translateAlternateColorCodes((char)'&', (String)"&c[Mercador] Este item ja foi avaliado/identificado!"));
                player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
                return;
            }
            result = input.clone();
            resultNbt = NBTItem.get((ItemStack)result);
            int revealerLevel = Math.max(1, session.playerLevel());
            itemLevelBefore = this.getItemDefinitionLevel(resultNbt);
            cappedLevel = Math.max(1, Math.min(itemLevelBefore, revealerLevel));
            resultNbt.addTag(new ItemTag[]{new ItemTag("NEMONICORB_IDENTIFIED", (Object)1)});
            resultNbt.addTag(new ItemTag[]{new ItemTag("NEMONICORB_CRAFT_LEVEL", (Object)cappedLevel)});
            double eff = session.efficiency();
            if (eff >= 1.5 && resultNbt.hasTag("NEMONICORB_MODS")) {
                String modsJson = resultNbt.getString("NEMONICORB_MODS");
                try {
                    Type listType = new TypeToken<List<Map<String, Object>>>(this){}.getType();
                    List mods = (List)GSON.fromJson(modsJson, listType);
                    if (mods != null && !mods.isEmpty()) {
                        double multiplier = eff >= 2.5 ? 1.3 : (eff >= 2.0 ? 1.2 : 1.1);
                        for (Map mod : mods) {
                            if (mod.containsKey("value") && mod.get("value") instanceof Number) {
                                double oldVal = ((Number)mod.get("value")).doubleValue();
                                double newVal = (double)Math.round(oldVal * multiplier * 100.0) / 100.0;
                                mod.put("value", newVal);
                            }
                            if (!mod.containsKey("stats") || !(mod.get("stats") instanceof Map)) continue;
                            Map stats = (Map)mod.get("stats");
                            for (Map.Entry statEntry : stats.entrySet()) {
                                if (!(statEntry.getValue() instanceof Number)) continue;
                                double oldVal = ((Number)statEntry.getValue()).doubleValue();
                                double newVal = (double)Math.round(oldVal * multiplier * 100.0) / 100.0;
                                stats.put((String)statEntry.getKey(), newVal);
                            }
                        }
                        if (eff >= 2.5 && ThreadLocalRandom.current().nextDouble() < 0.15) {
                            Map randomMod = (Map)mods.get(ThreadLocalRandom.current().nextInt(mods.size()));
                            LinkedHashMap<String, Object> extraMod = new LinkedHashMap<String, Object>(randomMod);
                            if (extraMod.containsKey("value") && extraMod.get("value") instanceof Number) {
                                double val = ((Number)extraMod.get("value")).doubleValue();
                                extraMod.put("value", (double)Math.round(val * 0.5 * 100.0) / 100.0);
                            }
                            if (extraMod.containsKey("stats") && extraMod.get("stats") instanceof Map) {
                                LinkedHashMap<String, Double> stats = new LinkedHashMap<String, Double>((Map)extraMod.get("stats"));
                                for (Map.Entry statEntry : stats.entrySet()) {
                                    if (!(statEntry.getValue() instanceof Number)) continue;
                                    double val = ((Number)statEntry.getValue()).doubleValue();
                                    stats.put((String)statEntry.getKey(), (double)Math.round(val * 0.5 * 100.0) / 100.0);
                                }
                                extraMod.put("stats", stats);
                            }
                            extraMod.put("id", String.valueOf(randomMod.get("id")) + "_bonus");
                            mods.add(extraMod);
                            player.sendMessage(ChatColor.translateAlternateColorCodes((char)'&', (String)"&6[Mercador] &eMod bonus adicionado pela eficiencia excepcional!"));
                        }
                        resultNbt.addTag(new ItemTag[]{new ItemTag("NEMONICORB_MODS", (Object)GSON.toJson((Object)mods))});
                    }
                }
                catch (Exception e) {
                    if (!this.debug()) break block19;
                    this.plugin.getLogger().warning("[MERCHANT] Erro ao processar mods: " + e.getMessage());
                }
            }
        }
        result = resultNbt.toItem();
        NBTItem finalNbt = NBTItem.get((ItemStack)result);
        this.orbListener.updateItemDisplay(result, finalNbt);
        gui.setItem(14, result);
        gui.setItem(10, null);
        this.avaliacaoCooldowns.put(player.getUniqueId(), now);
        Location tableLoc = session.tableLoc();
        player.playSound(tableLoc, Sound.BLOCK_ENCHANTMENT_TABLE_USE, 1.0f, 1.2f);
        player.playSound(tableLoc, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.8f, 1.5f);
        tableLoc.getWorld().spawnParticle(Particle.ENCHANT, tableLoc.clone().add(0.5, 1.5, 0.5), 40, 0.3, 0.3, 0.3, 1.0);
        player.sendMessage(ChatColor.translateAlternateColorCodes((char)'&', (String)("&a[Mercador] Item avaliado com sucesso! (" + String.format("%.0f%%", session.efficiency() * 100.0) + " eficiencia)")));
        if (this.debug()) {
            this.plugin.getLogger().info("[MERCHANT-AVALIAR] " + player.getName() + " avaliou item com eficiencia " + String.format("%.0f%%", session.efficiency() * 100.0));
        }
        if (this.debug() && cappedLevel != itemLevelBefore) {
            this.plugin.getLogger().info("[MERCHANT-AVALIAR-CAP] " + player.getName() + " revelou item nivel " + itemLevelBefore + " -> cap " + cappedLevel);
        }
    }

    private int getItemDefinitionLevel(NBTItem nbt) {
        if (nbt.hasTag("NEMONICORB_CRAFT_LEVEL")) {
            return nbt.getInteger("NEMONICORB_CRAFT_LEVEL");
        }
        if (nbt.hasTag("MMOITEMS_ITEM_LEVEL")) {
            return nbt.getInteger("MMOITEMS_ITEM_LEVEL");
        }
        if (nbt.hasTag("MMOITEMS_UPGRADE_LEVEL")) {
            return nbt.getInteger("MMOITEMS_UPGRADE_LEVEL");
        }
        return 1;
    }

    private void handleCraftPergaminho(Player player, Inventory gui, MerchantSession session) {
        boolean preserveInk;
        int minLevel = this.plugin.getConfig().getInt("merchant.min-level", 5);
        if (session.playerLevel() < minLevel) {
            player.sendMessage(ChatColor.translateAlternateColorCodes((char)'&', (String)("&c[Mercador] Voce precisa de nivel " + minLevel + "!")));
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
            return;
        }
        long cooldownMs = this.plugin.getConfig().getLong("merchant.craft-cooldown-seconds", 30L) * 1000L;
        Long lastUse = this.craftPergCooldowns.get(player.getUniqueId());
        long now = System.currentTimeMillis();
        if (lastUse != null && now - lastUse < cooldownMs) {
            long remaining = (cooldownMs - (now - lastUse)) / 1000L;
            player.sendMessage(ChatColor.translateAlternateColorCodes((char)'&', (String)("&c[Mercador] Aguarde " + remaining + "s antes de fabricar novamente!")));
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
            return;
        }
        if (!this.hasMaterials(player, Material.PAPER, 4)) {
            player.sendMessage(ChatColor.translateAlternateColorCodes((char)'&', (String)"&c[Mercador] Voce precisa de 4x Papel!"));
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
            return;
        }
        if (!this.hasMaterials(player, Material.GLOW_INK_SAC, 2)) {
            player.sendMessage(ChatColor.translateAlternateColorCodes((char)'&', (String)"&c[Mercador] Voce precisa de 2x Saco de Tinta Brilhante!"));
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
            return;
        }
        if (!this.hasMaterials(player, Material.LAPIS_LAZULI, 1)) {
            player.sendMessage(ChatColor.translateAlternateColorCodes((char)'&', (String)"&c[Mercador] Voce precisa de 1x Lapis Lazuli!"));
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
            return;
        }
        this.consumeMaterials(player, Material.PAPER, 4);
        this.consumeMaterials(player, Material.LAPIS_LAZULI, 1);
        boolean bl = preserveInk = session.efficiency() >= 2.5 && ThreadLocalRandom.current().nextDouble() < 0.1;
        if (!preserveInk) {
            this.consumeMaterials(player, Material.GLOW_INK_SAC, 2);
        } else {
            player.sendMessage(ChatColor.translateAlternateColorCodes((char)'&', (String)"&6[Mercador] &eA eficiencia preservou o Saco de Tinta Brilhante!"));
        }
        double eff = session.efficiency();
        int outputAmount = eff >= 2.5 ? 5 : (eff >= 2.0 ? 4 : (eff >= 1.5 ? 3 : 2));
        ItemStack pergaminho = this.createMMOItem("CONSUMABLE", "PERGAMINHO_DE_IDENTIFICACAO");
        if (pergaminho == null) {
            player.sendMessage(ChatColor.translateAlternateColorCodes((char)'&', (String)"&c[Mercador] Erro ao criar Pergaminho!"));
            return;
        }
        for (int i = 0; i < outputAmount; ++i) {
            ItemStack single = pergaminho.clone();
            single.setAmount(1);
            HashMap leftover = player.getInventory().addItem(new ItemStack[]{single});
            for (ItemStack drop : leftover.values()) {
                player.getWorld().dropItemNaturally(player.getLocation(), drop);
            }
        }
        this.craftPergCooldowns.put(player.getUniqueId(), now);
        Location tableLoc = session.tableLoc();
        player.playSound(tableLoc, Sound.BLOCK_ENCHANTMENT_TABLE_USE, 1.0f, 1.0f);
        tableLoc.getWorld().spawnParticle(Particle.ENCHANT, tableLoc.clone().add(0.5, 1.5, 0.5), 30, 0.3, 0.3, 0.3, 1.0);
        player.sendMessage(ChatColor.translateAlternateColorCodes((char)'&', (String)("&a[Mercador] Fabricacao concluida! " + outputAmount + " Pergaminho(s) de Identificacao.")));
        if (this.debug()) {
            this.plugin.getLogger().info("[MERCHANT-CRAFT-PERG] " + player.getName() + " fabricou " + outputAmount + "x Pergaminho");
        }
    }

    private void handleCraftPocao(Player player, Inventory gui, MerchantSession session) {
        if (!session.alquimistaRowActive()) {
            this.sendLockedMessage(player, "Mesa de Transmutacao");
            return;
        }
        long cooldownMs = this.plugin.getConfig().getLong("merchant.pocao-cooldown-seconds", 60L) * 1000L;
        Long lastUse = this.craftPocaoCooldowns.get(player.getUniqueId());
        long now = System.currentTimeMillis();
        if (lastUse != null && now - lastUse < cooldownMs) {
            long remaining = (cooldownMs - (now - lastUse)) / 1000L;
            player.sendMessage(ChatColor.translateAlternateColorCodes((char)'&', (String)("&c[Mercador] Aguarde " + remaining + "s antes de fabricar novamente!")));
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
            return;
        }
        if (!this.hasMaterials(player, Material.ENDER_PEARL, 4)) {
            player.sendMessage(ChatColor.translateAlternateColorCodes((char)'&', (String)"&c[Mercador] Voce precisa de 4x Ender Pearl!"));
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
            return;
        }
        if (!this.hasMaterials(player, Material.REDSTONE, 8)) {
            player.sendMessage(ChatColor.translateAlternateColorCodes((char)'&', (String)"&c[Mercador] Voce precisa de 8x Redstone!"));
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
            return;
        }
        if (!this.hasMaterials(player, Material.COMPASS, 1)) {
            player.sendMessage(ChatColor.translateAlternateColorCodes((char)'&', (String)"&c[Mercador] Voce precisa de 1x Bussola!"));
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
            return;
        }
        ItemStack out1 = gui.getItem(32);
        ItemStack out2 = gui.getItem(33);
        if (out1 != null && !out1.getType().isAir() && out2 != null && !out2.getType().isAir()) {
            player.sendMessage(ChatColor.translateAlternateColorCodes((char)'&', (String)"&c[Mercador] Retire os itens dos slots de saida primeiro!"));
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
            return;
        }
        this.consumeMaterials(player, Material.ENDER_PEARL, 4);
        this.consumeMaterials(player, Material.REDSTONE, 8);
        this.consumeMaterials(player, Material.COMPASS, 1);
        ItemStack pocao = this.createRecallPotion();
        if (out1 == null || out1.getType().isAir()) {
            gui.setItem(32, pocao);
        } else {
            gui.setItem(33, pocao);
        }
        this.craftPocaoCooldowns.put(player.getUniqueId(), now);
        Location tableLoc = session.tableLoc();
        player.playSound(tableLoc, Sound.BLOCK_BREWING_STAND_BREW, 1.0f, 1.0f);
        tableLoc.getWorld().spawnParticle(Particle.WITCH, tableLoc.clone().add(0.5, 1.5, 0.5), 30, 0.3, 0.5, 0.3, 0.1);
        player.sendMessage(ChatColor.translateAlternateColorCodes((char)'&', (String)"&a[Mercador] Pocao de Retorno fabricada com sucesso!"));
        if (this.debug()) {
            this.plugin.getLogger().info("[MERCHANT-CRAFT-POCAO] " + player.getName() + " fabricou Pocao de Retorno");
        }
    }

    private void handleEncantar(Player player, Inventory gui, MerchantSession session) {
        if (!session.ferreiroRowActive()) {
            this.sendLockedMessage(player, "Mesa do Ferreiro");
            return;
        }
        int enchantMinLevel = this.plugin.getConfig().getInt("merchant.enchant-min-level", 35);
        if (session.playerLevel() < enchantMinLevel) {
            player.sendMessage(ChatColor.translateAlternateColorCodes((char)'&', (String)("&c[Mercador] Voce precisa de nivel " + enchantMinLevel + " para encantar!")));
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
            return;
        }
        long cooldownMs = this.plugin.getConfig().getLong("merchant.enchant-cooldown-seconds", 30L) * 1000L;
        Long lastUse = this.enchantCooldowns.get(player.getUniqueId());
        long now = System.currentTimeMillis();
        if (lastUse != null && now - lastUse < cooldownMs) {
            long remaining = (cooldownMs - (now - lastUse)) / 1000L;
            player.sendMessage(ChatColor.translateAlternateColorCodes((char)'&', (String)("&c[Mercador] Aguarde " + remaining + "s antes de encantar novamente!")));
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
            return;
        }
        ItemStack equipment = gui.getItem(10);
        if (equipment == null || equipment.getType().isAir()) {
            player.sendMessage(ChatColor.translateAlternateColorCodes((char)'&', (String)"&c[Mercador] Coloque um equipamento no slot de entrada (linha 1)!"));
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
            return;
        }
        ItemStack book = gui.getItem(37);
        if (book == null || book.getType() != Material.ENCHANTED_BOOK) {
            player.sendMessage(ChatColor.translateAlternateColorCodes((char)'&', (String)"&c[Mercador] Coloque um Livro Encantado no slot de entrada (linha 4)!"));
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
            return;
        }
        ItemStack existingOut = gui.getItem(41);
        if (existingOut != null && !existingOut.getType().isAir()) {
            player.sendMessage(ChatColor.translateAlternateColorCodes((char)'&', (String)"&c[Mercador] Retire o item do slot de saida primeiro!"));
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
            return;
        }
        EnchantmentStorageMeta bookMeta = (EnchantmentStorageMeta)book.getItemMeta();
        if (bookMeta == null || bookMeta.getStoredEnchants().isEmpty()) {
            player.sendMessage(ChatColor.translateAlternateColorCodes((char)'&', (String)"&c[Mercador] O Livro Encantado nao tem encantamentos!"));
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
            return;
        }
        Map storedEnchants = bookMeta.getStoredEnchants();
        LinkedHashMap<Enchantment, Integer> applicableEnchants = new LinkedHashMap<Enchantment, Integer>();
        for (Map.Entry entry : storedEnchants.entrySet()) {
            if (!((Enchantment)entry.getKey()).canEnchantItem(equipment)) continue;
            applicableEnchants.put((Enchantment)entry.getKey(), (Integer)entry.getValue());
        }
        if (applicableEnchants.isEmpty()) {
            player.sendMessage(ChatColor.translateAlternateColorCodes((char)'&', (String)this.plugin.getConfig().getString("merchant.messages.not-enchantable", "&c[Mercador] Este item nao pode receber encantamentos!")));
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
            return;
        }
        double eff = session.efficiency();
        int bp = session.breakpoint();
        double successChance = 60.0 + eff * 20.0 + (double)session.playerLevel() * 0.15;
        if (bp >= 3) {
            successChance += 10.0;
        }
        if (session.env().ferreiroNearby()) {
            successChance += 5.0;
        }
        successChance = Math.min(99.0, successChance);
        double roll = ThreadLocalRandom.current().nextDouble() * 100.0;
        boolean criticalSuccess = eff >= 2.0 && ThreadLocalRandom.current().nextDouble() < 0.15;
        boolean criticalFail = eff < 1.1 && ThreadLocalRandom.current().nextDouble() < 0.1;
        this.enchantCooldowns.put(player.getUniqueId(), now);
        if (roll <= successChance || criticalSuccess) {
            ItemStack result = equipment.clone();
            ItemMeta resultMeta = result.getItemMeta();
            if (resultMeta != null) {
                for (Map.Entry entry : applicableEnchants.entrySet()) {
                    resultMeta.addEnchant((Enchantment)entry.getKey(), ((Integer)entry.getValue()).intValue(), true);
                }
                result.setItemMeta(resultMeta);
            }
            gui.setItem(41, result);
            gui.setItem(10, null);
            if (criticalSuccess) {
                player.sendMessage(ChatColor.translateAlternateColorCodes((char)'&', (String)"&6[Mercador] &eSucesso critico! Encantamentos aplicados e livro preservado!"));
                player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.5f);
            } else {
                boolean preserveBook;
                gui.setItem(37, null);
                boolean bl = preserveBook = bp >= 4 && ThreadLocalRandom.current().nextDouble() < 0.15;
                if (preserveBook) {
                    gui.setItem(37, book);
                    player.sendMessage(ChatColor.translateAlternateColorCodes((char)'&', (String)"&6[Mercador] &eBreakpoint Magnata preservou o livro!"));
                }
                player.sendMessage(ChatColor.translateAlternateColorCodes((char)'&', (String)"&a[Mercador] Encantamento aplicado com sucesso!"));
                player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_USE, 1.0f, 1.2f);
            }
            Location tableLoc = session.tableLoc();
            tableLoc.getWorld().spawnParticle(Particle.ENCHANT, tableLoc.clone().add(0.5, 1.5, 0.5), 50, 0.5, 0.5, 0.5, 1.0);
        } else if (criticalFail) {
            ItemMeta damagedMeta;
            gui.setItem(37, null);
            ItemStack damaged = equipment.clone();
            if (damaged.getType().getMaxDurability() > 0 && (damagedMeta = damaged.getItemMeta()) instanceof Damageable) {
                Damageable damageable = (Damageable)damagedMeta;
                short maxDur = damaged.getType().getMaxDurability();
                int currentDamage = damageable.getDamage();
                int addDamage = (int)Math.ceil((double)maxDur * 0.25);
                damageable.setDamage(Math.min(maxDur - 1, currentDamage + addDamage));
                damaged.setItemMeta(damagedMeta);
            }
            gui.setItem(10, damaged);
            player.sendMessage(ChatColor.translateAlternateColorCodes((char)'&', (String)"&c[Mercador] Falha critica! O livro foi destruido e o item perdeu 25%% de durabilidade!"));
            player.playSound(player.getLocation(), Sound.ENTITY_ITEM_BREAK, 1.0f, 0.5f);
            Location tableLoc = session.tableLoc();
            tableLoc.getWorld().spawnParticle(Particle.SMOKE, tableLoc.clone().add(0.5, 1.5, 0.5), 40, 0.3, 0.3, 0.3, 0.05);
        } else {
            gui.setItem(37, null);
            player.sendMessage(ChatColor.translateAlternateColorCodes((char)'&', (String)"&c[Mercador] Encantamento falhou! O livro foi destruido."));
            player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_LAND, 0.8f, 0.5f);
            Location tableLoc = session.tableLoc();
            tableLoc.getWorld().spawnParticle(Particle.SMOKE, tableLoc.clone().add(0.5, 1.5, 0.5), 20, 0.2, 0.2, 0.2, 0.03);
        }
        if (this.debug()) {
            this.plugin.getLogger().info("[MERCHANT-ENCANTAR] " + player.getName() + " tentou encantar. roll=" + String.format("%.1f", roll) + " chance=" + String.format("%.1f%%", successChance) + " critSuccess=" + criticalSuccess + " critFail=" + criticalFail);
        }
    }

    private void handleCraftPedra(Player player, Inventory gui, MerchantSession session) {
        if (!session.ferreiroRowActive()) {
            this.sendLockedMessage(player, "Mesa do Ferreiro");
            return;
        }
        int minLevel = this.plugin.getConfig().getInt("merchant.enchant-min-level", 35);
        if (session.playerLevel() < minLevel) {
            player.sendMessage(ChatColor.translateAlternateColorCodes((char)'&', (String)("&c[Mercador] Voce precisa de nivel " + minLevel + " para fabricar!")));
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
            return;
        }
        long cooldownMs = this.plugin.getConfig().getLong("merchant.pedra-cooldown-seconds", 120L) * 1000L;
        Long lastUse = this.craftPedraCooldowns.get(player.getUniqueId());
        long now = System.currentTimeMillis();
        if (lastUse != null && now - lastUse < cooldownMs) {
            long remaining = (cooldownMs - (now - lastUse)) / 1000L;
            player.sendMessage(ChatColor.translateAlternateColorCodes((char)'&', (String)("&c[Mercador] Aguarde " + remaining + "s antes de fabricar novamente!")));
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
            return;
        }
        if (!this.hasMaterials(player, Material.DIAMOND, 25)) {
            player.sendMessage(ChatColor.translateAlternateColorCodes((char)'&', (String)"&c[Mercador] Voce precisa de 25x Diamante!"));
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
            return;
        }
        if (!this.hasMaterials(player, Material.IRON_INGOT, 20)) {
            player.sendMessage(ChatColor.translateAlternateColorCodes((char)'&', (String)"&c[Mercador] Voce precisa de 20x Lingote de Ferro!"));
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
            return;
        }
        if (!this.hasMMOItemInInventory(player, "OLEO_DE_POLIMENTO", 15)) {
            player.sendMessage(ChatColor.translateAlternateColorCodes((char)'&', (String)"&c[Mercador] Voce precisa de 15x Oleo de Polimento!"));
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
            return;
        }
        if (!this.hasMaterials(player, Material.GOLD_INGOT, 10)) {
            player.sendMessage(ChatColor.translateAlternateColorCodes((char)'&', (String)"&c[Mercador] Voce precisa de 10x Lingote de Ouro!"));
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
            return;
        }
        this.consumeMaterials(player, Material.DIAMOND, 25);
        this.consumeMaterials(player, Material.IRON_INGOT, 20);
        this.consumeMMOItemFromInventory(player, "OLEO_DE_POLIMENTO", 15);
        this.consumeMaterials(player, Material.GOLD_INGOT, 10);
        ItemStack pedra = this.createMMOItem("CONSUMABLE", "PEDRA_DE_REFINAMENTO");
        if (pedra == null) {
            player.sendMessage(ChatColor.translateAlternateColorCodes((char)'&', (String)"&c[Mercador] Erro ao criar Pedra de Refinamento!"));
            return;
        }
        HashMap leftover = player.getInventory().addItem(new ItemStack[]{pedra});
        for (ItemStack drop : leftover.values()) {
            player.getWorld().dropItemNaturally(player.getLocation(), drop);
        }
        this.craftPedraCooldowns.put(player.getUniqueId(), now);
        Location tableLoc = session.tableLoc();
        player.playSound(tableLoc, Sound.BLOCK_ANVIL_USE, 1.0f, 1.0f);
        tableLoc.getWorld().spawnParticle(Particle.ENCHANT, tableLoc.clone().add(0.5, 1.5, 0.5), 30, 0.3, 0.3, 0.3, 1.0);
        player.sendMessage(ChatColor.translateAlternateColorCodes((char)'&', (String)"&a[Mercador] Pedra de Refinamento fabricada com sucesso!"));
        if (this.debug()) {
            this.plugin.getLogger().info("[MERCHANT-CRAFT-PEDRA] " + player.getName() + " fabricou Pedra de Refinamento");
        }
    }

    private void handleRoutes(Player player, MerchantSession session) {
        if (session.playerLevel() < 10) {
            player.sendMessage(ChatColor.translateAlternateColorCodes((char)'&', (String)"&c[Mercador] Voce precisa de nivel 10 para acessar Rotas Comerciais!"));
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
            return;
        }
        if (!this.plugin.getConfig().getBoolean("merchant.teleport.enabled", true)) {
            player.sendMessage(ChatColor.translateAlternateColorCodes((char)'&', (String)"&c[Mercador] Sistema de rotas desativado!"));
            return;
        }
        this.openRoutesGUI(player, session);
    }

    private void openRoutesGUI(Player player, MerchantSession session) {
        ItemStack back;
        ItemMeta backMeta;
        Inventory gui = Bukkit.createInventory(null, (int)27, (Component)Component.text((String)ROUTES_GUI_TITLE, (TextColor)NamedTextColor.GOLD, (TextDecoration[])new TextDecoration[]{TextDecoration.BOLD}));
        ItemStack blackGlass = this.createGlassPane(Material.BLACK_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < 27; ++i) {
            gui.setItem(i, blackGlass);
        }
        UUID uuid = player.getUniqueId();
        List<MerchantRouteManager.Waypoint> waypoints = this.routeManager.getWaypoints(uuid);
        int maxSlots = this.routeManager.getMaxSlots(session.playerLevel());
        ItemStack info = new ItemStack(Material.COMPASS);
        ItemMeta infoMeta = info.getItemMeta();
        if (infoMeta != null) {
            infoMeta.displayName((Component)Component.text((String)ROUTES_GUI_TITLE, (TextColor)NamedTextColor.GOLD, (TextDecoration[])new TextDecoration[]{TextDecoration.BOLD}));
            ArrayList<TextComponent> lore = new ArrayList<TextComponent>();
            lore.add(Component.text((String)""));
            lore.add(Component.text((String)("Rotas: " + waypoints.size() + "/" + maxSlots), (TextColor)NamedTextColor.AQUA));
            lore.add(Component.text((String)""));
            lore.add(Component.text((String)"Clique num waypoint para viajar.", (TextColor)NamedTextColor.GRAY));
            lore.add(Component.text((String)"Shift+clique para remover.", (TextColor)NamedTextColor.RED));
            infoMeta.lore(lore);
            info.setItemMeta(infoMeta);
        }
        gui.setItem(4, info);
        if (waypoints.size() < maxSlots) {
            ItemStack registerBtn = new ItemStack(Material.LIME_CONCRETE);
            ItemMeta regMeta = registerBtn.getItemMeta();
            if (regMeta != null) {
                regMeta.displayName((Component)Component.text((String)"Registrar Rota Aqui", (TextColor)NamedTextColor.GREEN, (TextDecoration[])new TextDecoration[]{TextDecoration.BOLD}));
                ArrayList lore = new ArrayList();
                lore.add(Component.text((String)""));
                lore.add(Component.text((String)"Consome 1x Selo de Rota Comercial", (TextColor)NamedTextColor.YELLOW));
                lore.add(Component.text((String)"Registra esta mesa como waypoint.", (TextColor)NamedTextColor.GRAY));
                regMeta.lore((List)lore);
                registerBtn.setItemMeta(regMeta);
            }
            gui.setItem(0, registerBtn);
        }
        List<MerchantRouteManager.WaypointInfo> accessible = this.routeManager.getAccessibleWaypoints(uuid);
        int slotIdx = 9;
        for (MerchantRouteManager.WaypointInfo wpInfo : accessible) {
            if (slotIdx >= 26) break;
            MerchantRouteManager.Waypoint wp = wpInfo.waypoint();
            ItemStack wpItem = new ItemStack(Material.ENDER_EYE);
            ItemMeta wpMeta = wpItem.getItemMeta();
            if (wpMeta != null) {
                wpMeta.displayName((Component)Component.text((String)wp.name(), (TextColor)NamedTextColor.GREEN, (TextDecoration[])new TextDecoration[]{TextDecoration.BOLD}));
                ArrayList<TextComponent> lore = new ArrayList<TextComponent>();
                lore.add(Component.text((String)""));
                lore.add(Component.text((String)("Mundo: " + wp.worldName()), (TextColor)NamedTextColor.GRAY));
                lore.add(Component.text((String)("Posicao: " + wp.x() + ", " + wp.y() + ", " + wp.z()), (TextColor)NamedTextColor.GRAY));
                boolean valid = this.routeManager.isWaypointValid(wp);
                lore.add(Component.text((String)("Status: " + (valid ? "Ativo" : "Mesa destruida!")), (TextColor)(valid ? NamedTextColor.GREEN : NamedTextColor.RED)));
                lore.add(Component.text((String)""));
                lore.add(Component.text((String)"Clique para info da rota.", (TextColor)NamedTextColor.YELLOW));
                lore.add(Component.text((String)"Shift+clique para remover.", (TextColor)NamedTextColor.RED));
                wpMeta.lore(lore);
                wpItem.setItemMeta(wpMeta);
            }
            gui.setItem(slotIdx, wpItem);
            ++slotIdx;
        }
        if ((backMeta = (back = new ItemStack(Material.ARROW)).getItemMeta()) != null) {
            backMeta.displayName((Component)Component.text((String)"Voltar", (TextColor)NamedTextColor.RED, (TextDecoration[])new TextDecoration[]{TextDecoration.BOLD}));
            back.setItemMeta(backMeta);
        }
        gui.setItem(26, back);
        player.openInventory(gui);
        player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.0f);
    }

    @EventHandler(priority=EventPriority.HIGH)
    public void onRoutesGUIClick(InventoryClickEvent event) {
        HumanEntity humanEntity = event.getWhoClicked();
        if (!(humanEntity instanceof Player)) {
            return;
        }
        Player player = (Player)humanEntity;
        String title = this.getInventoryTitle(event.getView());
        if (!ROUTES_GUI_TITLE.equals(title)) {
            return;
        }
        event.setCancelled(true);
        int slot = event.getRawSlot();
        if (slot < 0 || slot >= 27) {
            return;
        }
        UUID uuid = player.getUniqueId();
        MerchantSession session = this.activeSessions.get(uuid);
        if (session == null) {
            return;
        }
        if (slot == 26) {
            player.closeInventory();
            Bukkit.getScheduler().runTask((Plugin)this.plugin, () -> this.openGUI(player, session.tableLoc()));
            return;
        }
        if (slot == 0) {
            this.handleRegisterWaypoint(player, session);
            return;
        }
        if (slot >= 9 && slot < 26) {
            List<MerchantRouteManager.WaypointInfo> accessible = this.routeManager.getAccessibleWaypoints(uuid);
            int idx = slot - 9;
            if (idx < 0 || idx >= accessible.size()) {
                return;
            }
            MerchantRouteManager.WaypointInfo wpInfo = accessible.get(idx);
            if (event.isShiftClick()) {
                this.routeManager.removeWaypoint(uuid, wpInfo.index());
                player.sendMessage(ChatColor.translateAlternateColorCodes((char)'&', (String)("&c[Mercador] Rota '" + wpInfo.waypoint().name() + "' removida!")));
                player.playSound(player.getLocation(), Sound.ENTITY_ITEM_BREAK, 1.0f, 0.8f);
                Bukkit.getScheduler().runTask((Plugin)this.plugin, () -> this.openRoutesGUI(player, session));
                return;
            }
            MerchantRouteManager.Waypoint wp = wpInfo.waypoint();
            player.sendMessage(ChatColor.translateAlternateColorCodes((char)'&', (String)("&6[Mercador] &eRota: &f" + wp.name() + " &7(" + wp.worldName() + " " + wp.x() + ", " + wp.y() + ", " + wp.z() + ")")));
            player.sendMessage(ChatColor.translateAlternateColorCodes((char)'&', (String)"&6[Mercador] &eUse uma &bPocao de Retorno &epara viajar!"));
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, 1.5f);
        }
    }

    private void handleRegisterWaypoint(Player player, MerchantSession session) {
        UUID uuid = player.getUniqueId();
        int maxSlots = this.routeManager.getMaxSlots(session.playerLevel());
        List<MerchantRouteManager.Waypoint> existing = this.routeManager.getWaypoints(uuid);
        if (existing.size() >= maxSlots) {
            player.sendMessage(ChatColor.translateAlternateColorCodes((char)'&', (String)("&c[Mercador] Voce ja possui o maximo de rotas! (" + maxSlots + ")")));
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
            return;
        }
        if (this.routeManager.hasWaypointAt(session.tableLoc())) {
            player.sendMessage(ChatColor.translateAlternateColorCodes((char)'&', (String)"&c[Mercador] Esta mesa ja possui uma rota registrada!"));
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
            return;
        }
        ItemStack selo = this.findSeloInInventory(player);
        if (selo == null) {
            player.sendMessage(ChatColor.translateAlternateColorCodes((char)'&', (String)"&c[Mercador] Voce precisa de um Selo de Rota Comercial!"));
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
            return;
        }
        if (selo.getAmount() > 1) {
            selo.setAmount(selo.getAmount() - 1);
        } else {
            player.getInventory().remove(selo);
        }
        Location loc = session.tableLoc();
        String wpName = "Rota " + player.getName() + " #" + (existing.size() + 1);
        MerchantRouteManager.Waypoint wp = new MerchantRouteManager.Waypoint(wpName, loc.getWorld().getName(), loc.getBlockX(), loc.getBlockY(), loc.getBlockZ(), System.currentTimeMillis(), System.currentTimeMillis(), 0, "public", List.of(), true);
        this.routeManager.addWaypoint(uuid, wp);
        player.sendMessage(ChatColor.translateAlternateColorCodes((char)'&', (String)("&a[Mercador] Rota '" + wpName + "' registrada com sucesso!")));
        player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.2f);
        loc.getWorld().spawnParticle(Particle.HAPPY_VILLAGER, loc.clone().add(0.5, 1.5, 0.5), 30, 0.5, 0.5, 0.5, 0.0);
        Bukkit.getScheduler().runTask((Plugin)this.plugin, () -> this.openRoutesGUI(player, session));
    }

    private ItemStack findSeloInInventory(Player player) {
        for (ItemStack item : player.getInventory().getContents()) {
            String plain;
            Component displayName;
            ItemMeta meta;
            if (item == null || item.getType() != Material.FILLED_MAP || (meta = item.getItemMeta()) == null || (displayName = meta.displayName()) == null || !(plain = PlainTextComponentSerializer.plainText().serialize(displayName)).contains("Selo de Rota Comercial")) continue;
            return item;
        }
        return null;
    }

    private void handleCraftSelo(Player player, MerchantSession session) {
        int minLevel = this.plugin.getConfig().getInt("merchant.teleport.min-level", 10);
        if (session.playerLevel() < minLevel) {
            player.sendMessage(ChatColor.translateAlternateColorCodes((char)'&', (String)("&c[Mercador] Voce precisa de nivel " + minLevel + " para fabricar Selos!")));
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
            return;
        }
        long cooldownMs = this.plugin.getConfig().getLong("merchant.selo-craft.cooldown-seconds", 120L) * 1000L;
        Long lastUse = this.seloCraftCooldowns.get(player.getUniqueId());
        long now = System.currentTimeMillis();
        if (lastUse != null && now - lastUse < cooldownMs) {
            long remaining = (cooldownMs - (now - lastUse)) / 1000L;
            player.sendMessage(ChatColor.translateAlternateColorCodes((char)'&', (String)("&c[Mercador] Aguarde " + remaining + "s antes de fabricar novamente!")));
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
            return;
        }
        int enderPearls = this.plugin.getConfig().getInt("merchant.selo-craft.ender-pearls", 16);
        int honeyBottles = this.plugin.getConfig().getInt("merchant.selo-craft.honey-bottles", 1);
        int filledMaps = this.plugin.getConfig().getInt("merchant.selo-craft.filled-maps", 1);
        int lapisLazuli = this.plugin.getConfig().getInt("merchant.selo-craft.lapis-lazuli", 4);
        int goldIngots = this.plugin.getConfig().getInt("merchant.selo-craft.gold-ingots", 2);
        if (!this.hasMaterials(player, Material.ENDER_PEARL, enderPearls)) {
            player.sendMessage(ChatColor.translateAlternateColorCodes((char)'&', (String)("&c[Mercador] Voce precisa de " + enderPearls + "x Ender Pearl!")));
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
            return;
        }
        if (!this.hasMaterials(player, Material.HONEY_BOTTLE, honeyBottles)) {
            player.sendMessage(ChatColor.translateAlternateColorCodes((char)'&', (String)("&c[Mercador] Voce precisa de " + honeyBottles + "x Pote de Mel!")));
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
            return;
        }
        if (!this.hasMaterials(player, Material.FILLED_MAP, filledMaps)) {
            player.sendMessage(ChatColor.translateAlternateColorCodes((char)'&', (String)("&c[Mercador] Voce precisa de " + filledMaps + "x Mapa Preenchido!")));
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
            return;
        }
        if (!this.hasMaterials(player, Material.LAPIS_LAZULI, lapisLazuli)) {
            player.sendMessage(ChatColor.translateAlternateColorCodes((char)'&', (String)("&c[Mercador] Voce precisa de " + lapisLazuli + "x Lapis Lazuli!")));
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
            return;
        }
        if (!this.hasMaterials(player, Material.GOLD_INGOT, goldIngots)) {
            player.sendMessage(ChatColor.translateAlternateColorCodes((char)'&', (String)("&c[Mercador] Voce precisa de " + goldIngots + "x Lingote de Ouro!")));
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
            return;
        }
        this.consumeMaterials(player, Material.ENDER_PEARL, enderPearls);
        this.consumeMaterials(player, Material.HONEY_BOTTLE, honeyBottles);
        this.consumeMaterials(player, Material.FILLED_MAP, filledMaps);
        this.consumeMaterials(player, Material.LAPIS_LAZULI, lapisLazuli);
        this.consumeMaterials(player, Material.GOLD_INGOT, goldIngots);
        ItemStack selo = new ItemStack(Material.FILLED_MAP);
        ItemMeta seloMeta = selo.getItemMeta();
        if (seloMeta != null) {
            seloMeta.displayName((Component)Component.text((String)"Selo de Rota Comercial", (TextColor)NamedTextColor.GOLD, (TextDecoration[])new TextDecoration[]{TextDecoration.BOLD}));
            ArrayList<TextComponent> lore = new ArrayList<TextComponent>();
            lore.add(Component.text((String)""));
            lore.add(Component.text((String)"Use na Mesa do Mercador para", (TextColor)NamedTextColor.GRAY));
            lore.add(Component.text((String)"registrar uma rota comercial.", (TextColor)NamedTextColor.GRAY));
            lore.add(Component.text((String)""));
            lore.add(Component.text((String)"Item consumido ao registrar.", (TextColor)NamedTextColor.RED));
            seloMeta.lore(lore);
            selo.setItemMeta(seloMeta);
        }
        HashMap leftover = player.getInventory().addItem(new ItemStack[]{selo});
        for (ItemStack drop : leftover.values()) {
            player.getWorld().dropItemNaturally(player.getLocation(), drop);
        }
        this.seloCraftCooldowns.put(player.getUniqueId(), now);
        Location tableLoc = session.tableLoc();
        player.playSound(tableLoc, Sound.BLOCK_ENCHANTMENT_TABLE_USE, 1.0f, 0.8f);
        tableLoc.getWorld().spawnParticle(Particle.ENCHANT, tableLoc.clone().add(0.5, 1.5, 0.5), 30, 0.3, 0.3, 0.3, 1.0);
        player.sendMessage(ChatColor.translateAlternateColorCodes((char)'&', (String)"&a[Mercador] Selo de Rota Comercial fabricado!"));
        if (this.debug()) {
            this.plugin.getLogger().info("[MERCHANT-SELO] " + player.getName() + " fabricou Selo de Rota Comercial");
        }
    }

    private ItemStack createRecallPotion() {
        ItemStack potion = new ItemStack(Material.POTION);
        ItemMeta meta = potion.getItemMeta();
        if (meta != null) {
            meta.displayName((Component)Component.text((String)"Pocao de Retorno", (TextColor)NamedTextColor.GOLD, (TextDecoration[])new TextDecoration[]{TextDecoration.BOLD}));
            ArrayList<TextComponent> lore = new ArrayList<TextComponent>();
            lore.add(Component.text((String)""));
            lore.add(Component.text((String)"Beba para iniciar o Recall.", (TextColor)NamedTextColor.GRAY));
            lore.add(Component.text((String)"Selecione o destino na hotbar.", (TextColor)NamedTextColor.GRAY));
            lore.add(Component.text((String)""));
            lore.add(Component.text((String)"Exclusivo: Mercador", (TextColor)NamedTextColor.YELLOW));
            lore.add(Component.text((String)"Custo: 50 Mana + 10/passageiro", (TextColor)NamedTextColor.RED));
            meta.lore(lore);
            if (meta instanceof PotionMeta) {
                PotionMeta potionMeta = (PotionMeta)meta;
                potionMeta.setColor(Color.fromRGB((int)255, (int)200, (int)50));
            }
            potion.setItemMeta(meta);
        }
        NBTItem nbt = NBTItem.get((ItemStack)potion);
        nbt.addTag(new ItemTag[]{new ItemTag(RECALL_POTION_TAG, (Object)1)});
        return nbt.toItem();
    }

    private boolean isRecallPotion(ItemStack item) {
        if (item == null || item.getType() != Material.POTION) {
            return false;
        }
        try {
            NBTItem nbt = NBTItem.get((ItemStack)item);
            return nbt.hasTag(RECALL_POTION_TAG) && nbt.getInteger(RECALL_POTION_TAG) == 1;
        }
        catch (Exception e) {
            return false;
        }
    }

    @EventHandler(priority=EventPriority.HIGH)
    public void onConsumeRecallPotion(PlayerItemConsumeEvent event) {
        Player player = event.getPlayer();
        ItemStack item = event.getItem();
        if (item == null) {
            return;
        }
        if (!this.isRecallPotion(item)) {
            return;
        }
        event.setCancelled(true);
        if (!this.isMercador(player)) {
            player.sendMessage(ChatColor.translateAlternateColorCodes((char)'&', (String)"&c[Mercador] Apenas Mercadores podem usar a Pocao de Retorno!"));
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
            return;
        }
        if (this.castingTasks.containsKey(player.getUniqueId())) {
            player.sendMessage(ChatColor.translateAlternateColorCodes((char)'&', (String)"&c[Mercador] Ja esta canalizando um teletransporte!"));
            return;
        }
        long cooldownMs = this.plugin.getConfig().getLong("merchant.teleport.cooldown-seconds", 30L) * 1000L;
        Long lastUse = this.teleportCooldowns.get(player.getUniqueId());
        long now = System.currentTimeMillis();
        if (lastUse != null && now - lastUse < cooldownMs) {
            long remaining = (cooldownMs - (now - lastUse)) / 1000L;
            player.sendMessage(ChatColor.translateAlternateColorCodes((char)'&', (String)("&c[Mercador] Aguarde " + remaining + "s antes de teleportar novamente!")));
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
            return;
        }
        UUID uuid = player.getUniqueId();
        List<MerchantRouteManager.WaypointInfo> destinations = this.routeManager.getAccessibleWaypoints(uuid);
        if (destinations.isEmpty()) {
            player.sendMessage(ChatColor.translateAlternateColorCodes((char)'&', (String)"&c[Mercador] Voce nao possui rotas registradas!"));
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
            return;
        }
        double manaCostBase = this.plugin.getConfig().getDouble("merchant.teleport.mana-cost", 50.0);
        double manaCostPerPassenger = this.plugin.getConfig().getDouble("merchant.teleport.mana-cost-per-passenger", 25.0);
        double passRadius = this.plugin.getConfig().getDouble("merchant.teleport.passenger-radius", 5.0);
        int potentialPassengers = 0;
        for (Player nearby : player.getWorld().getPlayers()) {
            if (nearby.equals((Object)player) || !(nearby.getLocation().distanceSquared(player.getLocation()) <= passRadius * passRadius)) continue;
            ++potentialPassengers;
        }
        int maxPassengers = this.plugin.getConfig().getInt("merchant.teleport.max-passengers", 3);
        potentialPassengers = Math.min(potentialPassengers, maxPassengers);
        double totalManaCost = manaCostBase + (double)potentialPassengers * manaCostPerPassenger;
        double currentMana = this.getPlayerMana(player);
        if (currentMana < totalManaCost) {
            player.sendMessage(ChatColor.translateAlternateColorCodes((char)'&', (String)("&c[Mercador] Mana insuficiente! Precisa de " + (int)totalManaCost + " mana (" + (int)manaCostBase + " base + " + potentialPassengers + " passageiros x " + (int)manaCostPerPassenger + ")")));
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
            return;
        }
        if (!this.deductPlayerMana(player, totalManaCost)) {
            player.sendMessage(ChatColor.translateAlternateColorCodes((char)'&', (String)"&c[Mercador] Erro ao deduzir mana!"));
            return;
        }
        if (item.getAmount() > 1) {
            item.setAmount(item.getAmount() - 1);
        } else {
            player.getInventory().setItemInMainHand(null);
        }
        this.startRecallCasting(player, destinations);
    }

    private void startRecallCasting(final Player player, List<MerchantRouteManager.WaypointInfo> destinations) {
        final UUID uuid = player.getUniqueId();
        int castingSeconds = this.plugin.getConfig().getInt("merchant.teleport.casting-seconds", 5);
        final int castingTicks = castingSeconds * 20;
        ItemStack[] hotbarBackup = new ItemStack[9];
        for (int i = 0; i < 9; ++i) {
            ItemStack slot = player.getInventory().getItem(i);
            hotbarBackup[i] = slot != null ? slot.clone() : null;
        }
        this.hotbarBackups.put(uuid, hotbarBackup);
        int maxDests = Math.min(destinations.size(), 9);
        List<MerchantRouteManager.WaypointInfo> validDests = destinations.subList(0, maxDests);
        this.castingDestinations.put(uuid, new ArrayList<MerchantRouteManager.WaypointInfo>(validDests));
        this.selectedDestination.put(uuid, 0);
        for (int i = 0; i < 9; ++i) {
            if (i < validDests.size()) {
                MerchantRouteManager.WaypointInfo wpInfo = validDests.get(i);
                MerchantRouteManager.Waypoint wp = wpInfo.waypoint();
                boolean isOwn = wpInfo.ownerUUID().equals(uuid);
                ItemStack destItem = new ItemStack(isOwn ? Material.ENDER_EYE : Material.ENDER_PEARL);
                ItemMeta meta = destItem.getItemMeta();
                if (meta != null) {
                    meta.displayName((Component)Component.text((String)(i + 1 + ". " + wp.name()), (TextColor)(isOwn ? NamedTextColor.GREEN : NamedTextColor.AQUA), (TextDecoration[])new TextDecoration[]{TextDecoration.BOLD}));
                    ArrayList<TextComponent> lore = new ArrayList<TextComponent>();
                    lore.add(Component.text((String)(wp.worldName() + " (" + wp.x() + ", " + wp.y() + ", " + wp.z() + ")"), (TextColor)NamedTextColor.GRAY));
                    if (i == 0) {
                        lore.add(Component.text((String)""));
                        lore.add(Component.text((String)">>> SELECIONADO <<<", (TextColor)NamedTextColor.YELLOW, (TextDecoration[])new TextDecoration[]{TextDecoration.BOLD}));
                    }
                    lore.add(Component.text((String)""));
                    lore.add(Component.text((String)"Segure este slot para viajar aqui", (TextColor)NamedTextColor.YELLOW));
                    meta.lore(lore);
                    destItem.setItemMeta(meta);
                }
                player.getInventory().setItem(i, destItem);
                continue;
            }
            ItemStack glass = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
            ItemMeta gMeta = glass.getItemMeta();
            if (gMeta != null) {
                gMeta.displayName((Component)Component.text((String)"Sem rota", (TextColor)NamedTextColor.DARK_GRAY));
                glass.setItemMeta(gMeta);
            }
            player.getInventory().setItem(i, glass);
        }
        player.getInventory().setHeldItemSlot(0);
        this.castingStartLocs.put(uuid, player.getLocation().clone());
        Location pLoc = player.getLocation();
        double passRadius = this.plugin.getConfig().getDouble("merchant.teleport.passenger-radius", 5.0);
        pLoc.getWorld().playSound(pLoc, Sound.BLOCK_PORTAL_TRIGGER, 1.0f, 1.5f);
        player.sendMessage(ChatColor.translateAlternateColorCodes((char)'&', (String)("&6[Mercador] Recall iniciado! Selecione o destino na hotbar. Nao se mova! (" + castingSeconds + "s)")));
        for (Player nearby : pLoc.getWorld().getPlayers()) {
            if (nearby.equals((Object)player) || !(nearby.getLocation().distanceSquared(pLoc) <= passRadius * passRadius)) continue;
            nearby.sendMessage(ChatColor.translateAlternateColorCodes((char)'&', (String)("&6[Mercador] " + player.getName() + " esta canalizando Recall! Fique proximo para viajar junto!")));
            nearby.playSound(pLoc, Sound.BLOCK_PORTAL_TRIGGER, 0.5f, 1.5f);
        }
        BukkitTask castTask = Bukkit.getScheduler().runTaskTimer((Plugin)this.plugin, new Runnable(){
            int ticks = 0;

            @Override
            public void run() {
                this.ticks += 4;
                if (!player.isOnline() || !MerchantTableListener.this.castingTasks.containsKey(uuid)) {
                    MerchantTableListener.this.cancelCasting(uuid, false);
                    return;
                }
                double remainingSec = Math.max(0.0, (double)(castingTicks - this.ticks) / 20.0);
                int bars = 20;
                int filled = (int)((double)this.ticks / (double)castingTicks * (double)bars);
                StringBuilder barStr = new StringBuilder("&6Recall &a");
                for (int i = 0; i < bars; ++i) {
                    barStr.append(i < filled ? "|" : "&7|");
                }
                barStr.append(" &e").append(String.format("%.1fs", remainingSec));
                int selIdx = MerchantTableListener.this.selectedDestination.getOrDefault(uuid, 0);
                List<MerchantRouteManager.WaypointInfo> dests = MerchantTableListener.this.castingDestinations.get(uuid);
                if (dests != null && selIdx >= 0 && selIdx < dests.size()) {
                    barStr.append(" &f\u2192 &b").append(dests.get(selIdx).waypoint().name());
                }
                player.sendActionBar((Component)Component.text((String)ChatColor.translateAlternateColorCodes((char)'&', (String)barStr.toString())));
                World world = player.getWorld();
                Location loc = player.getLocation();
                double angle = (double)(this.ticks % 40) / 40.0 * 2.0 * Math.PI;
                Particle.DustOptions dust = new Particle.DustOptions(Color.fromRGB((int)50, (int)200, (int)50), 1.2f);
                for (int i = 0; i < 4; ++i) {
                    double a = angle + (double)i * Math.PI / 2.0;
                    double px = loc.getX() + 1.5 * Math.cos(a);
                    double pz = loc.getZ() + 1.5 * Math.sin(a);
                    world.spawnParticle(Particle.DUST, px, loc.getY() + 1.0, pz, 2, 0.0, 0.3, 0.0, 0.0, (Object)dust);
                }
                world.spawnParticle(Particle.ENCHANT, loc.clone().add(0.0, 1.5, 0.0), 5, 0.5, 0.5, 0.5, 0.5);
                if (this.ticks % 20 == 0) {
                    world.playSound(loc, Sound.BLOCK_NOTE_BLOCK_BELL, 0.8f, 1.0f + (float)this.ticks / (float)castingTicks);
                }
                if (this.ticks >= castingTicks) {
                    MerchantTableListener.this.completeRecallTeleport(player);
                }
            }
        }, 4L, 4L);
        this.castingTasks.put(uuid, castTask);
    }

    @EventHandler(priority=EventPriority.MONITOR)
    public void onHeldItemChange(PlayerItemHeldEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();
        if (!this.castingTasks.containsKey(uuid)) {
            return;
        }
        List<MerchantRouteManager.WaypointInfo> dests = this.castingDestinations.get(uuid);
        if (dests == null) {
            return;
        }
        int newSlot = event.getNewSlot();
        if (newSlot >= 0 && newSlot < dests.size()) {
            this.selectedDestination.put(uuid, newSlot);
            Player player = event.getPlayer();
            for (int i = 0; i < dests.size(); ++i) {
                ItemMeta meta;
                ItemStack slotItem = player.getInventory().getItem(i);
                if (slotItem == null || (meta = slotItem.getItemMeta()) == null) continue;
                MerchantRouteManager.Waypoint wp = dests.get(i).waypoint();
                ArrayList<TextComponent> lore = new ArrayList<TextComponent>();
                lore.add(Component.text((String)(wp.worldName() + " (" + wp.x() + ", " + wp.y() + ", " + wp.z() + ")"), (TextColor)NamedTextColor.GRAY));
                if (i == newSlot) {
                    lore.add(Component.text((String)""));
                    lore.add(Component.text((String)">>> SELECIONADO <<<", (TextColor)NamedTextColor.YELLOW, (TextDecoration[])new TextDecoration[]{TextDecoration.BOLD}));
                }
                lore.add(Component.text((String)""));
                lore.add(Component.text((String)"Segure este slot para viajar aqui", (TextColor)NamedTextColor.YELLOW));
                meta.lore(lore);
                slotItem.setItemMeta(meta);
            }
        }
    }

    private void completeRecallTeleport(Player player) {
        UUID uuid = player.getUniqueId();
        List<MerchantRouteManager.WaypointInfo> dests = this.castingDestinations.get(uuid);
        int selIdx = this.selectedDestination.getOrDefault(uuid, 0);
        this.restoreHotbar(player);
        if (dests == null || selIdx < 0 || selIdx >= dests.size()) {
            this.cancelCasting(uuid, false);
            player.sendMessage(ChatColor.translateAlternateColorCodes((char)'&', (String)"&c[Mercador] Erro: destino invalido!"));
            return;
        }
        MerchantRouteManager.WaypointInfo wpInfo = dests.get(selIdx);
        MerchantRouteManager.Waypoint wp = wpInfo.waypoint();
        if (!this.routeManager.isWaypointValid(wp)) {
            this.cancelCasting(uuid, false);
            player.sendMessage(ChatColor.translateAlternateColorCodes((char)'&', (String)"&c[Mercador] A mesa de destino foi destruida!"));
            return;
        }
        Location destination = wp.toLocation();
        if (destination == null) {
            this.cancelCasting(uuid, false);
            player.sendMessage(ChatColor.translateAlternateColorCodes((char)'&', (String)"&c[Mercador] Mundo de destino nao encontrado!"));
            return;
        }
        Location startLoc = player.getLocation();
        double passRadius = this.plugin.getConfig().getDouble("merchant.teleport.passenger-radius", 5.0);
        int maxPassengers = this.plugin.getConfig().getInt("merchant.teleport.max-passengers", 3);
        ArrayList<Player> passengers = new ArrayList<Player>();
        for (Object nearby : startLoc.getWorld().getPlayers()) {
            if (nearby.equals((Object)player) || !(nearby.getLocation().distanceSquared(startLoc) <= passRadius * passRadius) || passengers.size() >= maxPassengers) continue;
            passengers.add((Player)nearby);
        }
        player.teleport(destination);
        World destWorld = destination.getWorld();
        destWorld.playSound(destination, Sound.ENTITY_ENDERMAN_TELEPORT, 1.5f, 1.0f);
        destWorld.playSound(destination, Sound.BLOCK_BEACON_ACTIVATE, 1.0f, 1.5f);
        destWorld.spawnParticle(Particle.PORTAL, destination.clone().add(0.0, 1.0, 0.0), 80, 0.5, 1.0, 0.5, 0.5);
        destWorld.spawnParticle(Particle.ENCHANT, destination.clone().add(0.0, 2.0, 0.0), 40, 1.0, 1.0, 1.0, 1.0);
        startLoc.getWorld().spawnParticle(Particle.PORTAL, startLoc.clone().add(0.0, 1.0, 0.0), 50, 0.5, 1.0, 0.5, 0.5);
        startLoc.getWorld().playSound(startLoc, Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 0.8f);
        player.sendMessage(ChatColor.translateAlternateColorCodes((char)'&', (String)("&a[Mercador] Recall concluido! Bem-vindo a '" + wp.name() + "'!")));
        player.sendActionBar((Component)Component.text((String)("Recall concluido! \u2192 " + wp.name()), (TextColor)NamedTextColor.GREEN, (TextDecoration[])new TextDecoration[]{TextDecoration.BOLD}));
        for (Player passenger : passengers) {
            if (!passenger.isOnline()) continue;
            passenger.teleport(destination);
            passenger.playSound(destination, Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 1.0f);
            passenger.sendMessage(ChatColor.translateAlternateColorCodes((char)'&', (String)("&a[Mercador] Voce viajou com " + player.getName() + " para '" + wp.name() + "'!")));
        }
        this.teleportCooldowns.put(uuid, System.currentTimeMillis());
        BukkitTask task = this.castingTasks.remove(uuid);
        if (task != null) {
            task.cancel();
        }
        this.castingStartLocs.remove(uuid);
        this.castingDestinations.remove(uuid);
        this.selectedDestination.remove(uuid);
    }

    private void restoreHotbar(Player player) {
        UUID uuid = player.getUniqueId();
        ItemStack[] backup = this.hotbarBackups.remove(uuid);
        if (backup == null) {
            return;
        }
        for (int i = 0; i < 9; ++i) {
            player.getInventory().setItem(i, backup[i]);
        }
    }

    private void cancelCasting(UUID playerUUID, boolean interrupted) {
        BukkitTask task = this.castingTasks.remove(playerUUID);
        if (task != null) {
            task.cancel();
        }
        this.castingStartLocs.remove(playerUUID);
        this.castingDestinations.remove(playerUUID);
        this.selectedDestination.remove(playerUUID);
        Player player = Bukkit.getPlayer((UUID)playerUUID);
        if (player != null) {
            this.restoreHotbar(player);
        } else {
            this.hotbarBackups.remove(playerUUID);
        }
        if (interrupted && player != null) {
            player.sendMessage(ChatColor.translateAlternateColorCodes((char)'&', (String)"&c[Mercador] Recall cancelado!"));
            player.playSound(player.getLocation(), Sound.ENTITY_ITEM_BREAK, 1.0f, 0.5f);
            player.sendActionBar((Component)Component.text((String)"Recall cancelado!", (TextColor)NamedTextColor.RED, (TextDecoration[])new TextDecoration[]{TextDecoration.BOLD}));
            long cancelledCooldownMs = this.plugin.getConfig().getLong("merchant.teleport.cancelled-cooldown-seconds", 10L) * 1000L;
            this.teleportCooldowns.put(playerUUID, System.currentTimeMillis() - (30000L - cancelledCooldownMs));
        }
    }

    @EventHandler(priority=EventPriority.MONITOR, ignoreCancelled=true)
    public void onPlayerDamage(EntityDamageEvent event) {
        Entity entity = event.getEntity();
        if (!(entity instanceof Player)) {
            return;
        }
        Player player = (Player)entity;
        if (this.castingTasks.containsKey(player.getUniqueId())) {
            this.cancelCasting(player.getUniqueId(), true);
        }
    }

    @EventHandler(priority=EventPriority.MONITOR, ignoreCancelled=true)
    public void onPlayerMove(PlayerMoveEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();
        if (!this.castingTasks.containsKey(uuid)) {
            return;
        }
        Location from = event.getFrom();
        Location to = event.getTo();
        if (to == null) {
            return;
        }
        if (from.getBlockX() != to.getBlockX() || from.getBlockY() != to.getBlockY() || from.getBlockZ() != to.getBlockZ()) {
            this.cancelCasting(uuid, true);
        }
    }

    @EventHandler(priority=EventPriority.HIGH)
    public void onSealRightClickMesa(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }
        if (event.getClickedBlock() == null || event.getClickedBlock().getType() != Material.CARTOGRAPHY_TABLE) {
            return;
        }
        Player player = event.getPlayer();
        if (!this.isMercador(player)) {
            return;
        }
        ItemStack mainHand = player.getInventory().getItemInMainHand();
        if (mainHand.getType() != Material.FILLED_MAP) {
            return;
        }
        ItemMeta meta = mainHand.getItemMeta();
        if (meta == null) {
            return;
        }
        Component displayName = meta.displayName();
        if (displayName == null) {
            return;
        }
        String plain = PlainTextComponentSerializer.plainText().serialize(displayName);
        if (!plain.contains("Selo de Rota Comercial")) {
            return;
        }
        event.setCancelled(true);
        UUID uuid = player.getUniqueId();
        int playerLevel = this.getPlayerLevel(player);
        int maxSlots = this.routeManager.getMaxSlots(playerLevel);
        List<MerchantRouteManager.Waypoint> existing = this.routeManager.getWaypoints(uuid);
        if (existing.size() >= maxSlots) {
            player.sendMessage(ChatColor.translateAlternateColorCodes((char)'&', (String)("&c[Mercador] Voce ja possui o maximo de rotas! (" + maxSlots + ")")));
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
            return;
        }
        Location blockLoc = event.getClickedBlock().getLocation();
        if (this.routeManager.hasWaypointAt(blockLoc)) {
            player.sendMessage(ChatColor.translateAlternateColorCodes((char)'&', (String)"&c[Mercador] Esta mesa ja possui uma rota registrada!"));
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
            return;
        }
        if (mainHand.getAmount() > 1) {
            mainHand.setAmount(mainHand.getAmount() - 1);
        } else {
            player.getInventory().setItemInMainHand(null);
        }
        String wpName = "Rota " + player.getName() + " #" + (existing.size() + 1);
        MerchantRouteManager.Waypoint wp = new MerchantRouteManager.Waypoint(wpName, blockLoc.getWorld().getName(), blockLoc.getBlockX(), blockLoc.getBlockY(), blockLoc.getBlockZ(), System.currentTimeMillis(), System.currentTimeMillis(), 0, "public", List.of(), true);
        this.routeManager.addWaypoint(uuid, wp);
        player.sendMessage(ChatColor.translateAlternateColorCodes((char)'&', (String)("&a[Mercador] Rota '" + wpName + "' registrada com sucesso!")));
        player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.2f);
        blockLoc.getWorld().spawnParticle(Particle.HAPPY_VILLAGER, blockLoc.clone().add(0.5, 1.5, 0.5), 30, 0.5, 0.5, 0.5, 0.0);
        blockLoc.getWorld().playSound(blockLoc, Sound.BLOCK_BEACON_ACTIVATE, 1.0f, 1.5f);
        if (this.debug()) {
            this.plugin.getLogger().info("[MERCHANT-SELO] " + player.getName() + " registrou rota '" + wpName + "' em " + String.valueOf(blockLoc));
        }
    }

    private int getAuraLevel(double efficiency) {
        if (efficiency >= 2.5) {
            return 4;
        }
        if (efficiency >= 2.0) {
            return 3;
        }
        if (efficiency >= 1.6) {
            return 2;
        }
        if (efficiency >= 1.1) {
            return 1;
        }
        return 0;
    }

    private void startAura(Player player, Location tableLoc, double efficiency) {
        this.stopAura(player);
        int level = this.getAuraLevel(efficiency);
        if (level == 0) {
            return;
        }
        BukkitTask task = Bukkit.getScheduler().runTaskTimer((Plugin)this.plugin, () -> {
            double pz;
            double px;
            double angle;
            int i;
            if (!player.isOnline() || !this.activeSessions.containsKey(player.getUniqueId())) {
                this.stopAura(player);
                return;
            }
            World world = tableLoc.getWorld();
            if (world == null) {
                return;
            }
            double cx = tableLoc.getX() + 0.5;
            double cy = tableLoc.getY() + 1.0;
            double cz = tableLoc.getZ() + 0.5;
            Color color = switch (level) {
                case 1 -> Color.fromRGB((int)180, (int)180, (int)180);
                case 2 -> Color.fromRGB((int)50, (int)200, (int)50);
                case 3 -> Color.fromRGB((int)255, (int)200, (int)50);
                case 4 -> Color.fromRGB((int)255, (int)215, (int)0);
                default -> Color.WHITE;
            };
            Particle.DustOptions dustOptions = new Particle.DustOptions(color, 1.2f);
            double radius = switch (level) {
                case 1 -> 2.0;
                case 2 -> 3.5;
                case 3 -> 5.0;
                case 4 -> 12.5;
                default -> 2.0;
            };
            int particleCount = level == 4 ? 40 : 15 + level * 5;
            for (i = 0; i < particleCount; ++i) {
                angle = Math.PI * 2 / (double)particleCount * (double)i;
                px = cx + radius * Math.cos(angle);
                pz = cz + radius * Math.sin(angle);
                world.spawnParticle(Particle.DUST, px, cy + 0.5, pz, 1, 0.0, 0.3, 0.0, 0.0, (Object)dustOptions);
            }
            world.spawnParticle(Particle.DUST, cx, cy + 1.5, cz, 3, 0.2, 0.5, 0.2, 0.0, (Object)dustOptions);
            if (level >= 2) {
                world.spawnParticle(Particle.HAPPY_VILLAGER, cx, cy + 1.0, cz, 5, 0.5, 0.3, 0.5, 0.0);
            }
            if (level == 4) {
                for (i = 0; i < 8; ++i) {
                    angle = 0.7853981633974483 * (double)i + (double)(System.currentTimeMillis() % 5000L) / 5000.0 * 2.0 * Math.PI;
                    px = cx + radius * Math.cos(angle);
                    pz = cz + radius * Math.sin(angle);
                    world.spawnParticle(Particle.END_ROD, px, cy + 1.0, pz, 2, 0.1, 0.3, 0.1, 0.02);
                }
                for (Player nearby : world.getPlayers()) {
                    if (!(nearby.getLocation().distanceSquared(tableLoc) <= 156.25) || nearby.hasPotionEffect(PotionEffectType.LUCK)) continue;
                    nearby.addPotionEffect(new PotionEffect(PotionEffectType.LUCK, 100, 0, true, true, true));
                }
            }
        }, 10L, 10L);
        this.auraTasks.put(player.getUniqueId(), task);
    }

    private void stopAura(Player player) {
        BukkitTask task = this.auraTasks.remove(player.getUniqueId());
        if (task != null) {
            task.cancel();
        }
    }

    private ItemStack createGlassPane(Material mat, String name) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName((Component)Component.text((String)name, (TextColor)NamedTextColor.DARK_GRAY));
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack createArrowPane() {
        ItemStack item = new ItemStack(Material.LIME_STAINED_GLASS_PANE);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName((Component)Component.text((String)">>>", (TextColor)NamedTextColor.GREEN, (TextDecoration[])new TextDecoration[]{TextDecoration.BOLD}));
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack createLockedRowPane(String reason) {
        ItemStack item = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName((Component)Component.text((String)"Bloqueado", (TextColor)NamedTextColor.RED, (TextDecoration[])new TextDecoration[]{TextDecoration.BOLD}));
            ArrayList<TextComponent> lore = new ArrayList<TextComponent>();
            lore.add(Component.text((String)""));
            lore.add(Component.text((String)reason, (TextColor)NamedTextColor.GRAY));
            meta.lore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack createEfficiencyItem(MerchantEnvironment env, double efficiency, int playerLevel) {
        int breakpoint = this.getBreakpoint(efficiency);
        ItemStack item = new ItemStack(Material.NETHER_STAR);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName((Component)Component.text((String)("Eficiencia: " + String.format("%.0f%%", efficiency * 100.0)), (TextColor)NamedTextColor.GREEN, (TextDecoration[])new TextDecoration[]{TextDecoration.BOLD}));
            ArrayList<TextComponent> lore = new ArrayList<TextComponent>();
            lore.add(Component.text((String)""));
            lore.add(Component.text((String)("Titulo: " + this.getBreakpointName(breakpoint)), (TextColor)this.getBreakpointColor(breakpoint), (TextDecoration[])new TextDecoration[]{TextDecoration.BOLD}));
            lore.add(Component.text((String)("Nivel: " + playerLevel), (TextColor)NamedTextColor.AQUA));
            lore.add(Component.text((String)""));
            lore.add(Component.text((String)"Blocos detectados:", (TextColor)NamedTextColor.GRAY));
            for (Map.Entry<Material, Integer> entry : env.blockCounts().entrySet()) {
                double[] cfg = MERCHANT_BLOCKS.get(entry.getKey());
                if (cfg == null) continue;
                int threshold = INDICATOR_THRESHOLDS.getOrDefault(entry.getKey().name(), 1);
                if (entry.getValue() < threshold) continue;
                int count = Math.min(entry.getValue(), (int)cfg[1]);
                String bonus = String.format("+%.0f%%", (double)count * cfg[0] * 100.0);
                lore.add(Component.text((String)("  \u2726 " + this.formatMaterial(entry.getKey()) + ": " + count + " (" + bonus + ")"), (TextColor)NamedTextColor.GRAY));
            }
            int frameThreshold = INDICATOR_THRESHOLDS.getOrDefault("ITEM_FRAME", 4);
            if (env.itemFrameCount() >= frameThreshold) {
                int effective = Math.min(env.itemFrameCount(), 12);
                String bonus = String.format("+%.0f%%", (double)effective * 0.02 * 100.0);
                lore.add(Component.text((String)("  \u2726 Quadros de Item: " + env.itemFrameCount() + " (" + bonus + ")"), (TextColor)NamedTextColor.GRAY));
            }
            if (env.nearbyPlayers() > 0) {
                lore.add(Component.text((String)("  \u2726 Jogadores proximos: " + env.nearbyPlayers() + " (+" + String.format("%.0f%%", env.playerBonus() * 100.0) + ")"), (TextColor)NamedTextColor.YELLOW));
            }
            lore.add(Component.text((String)""));
            lore.add(Component.text((String)((env.enchantingTablePresent() ? "\u2714" : "\u2718") + " Transmutacao"), (TextColor)(env.enchantingTablePresent() ? NamedTextColor.GREEN : NamedTextColor.DARK_GRAY)));
            lore.add(Component.text((String)((env.anvilPresent() ? "\u2714" : "\u2718") + " Ferreiro"), (TextColor)(env.anvilPresent() ? NamedTextColor.GREEN : NamedTextColor.DARK_GRAY)));
            meta.lore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack createInfoItem(boolean alquimistaRowActive, boolean ferreiroRowActive) {
        ItemStack item = new ItemStack(Material.BOOK);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName((Component)Component.text((String)GUI_TITLE_RAW, (TextColor)NamedTextColor.GOLD, (TextDecoration[])new TextDecoration[]{TextDecoration.BOLD}));
            ArrayList<TextComponent> lore = new ArrayList<TextComponent>();
            lore.add(Component.text((String)""));
            lore.add(Component.text((String)"Avaliar Item | Pergaminho", (TextColor)NamedTextColor.GREEN));
            if (alquimistaRowActive) {
                lore.add(Component.text((String)"Pocao de Retorno", (TextColor)NamedTextColor.AQUA));
            }
            if (ferreiroRowActive) {
                lore.add(Component.text((String)"Encantamento | Pedra de Refinamento", (TextColor)NamedTextColor.LIGHT_PURPLE));
            }
            if (!alquimistaRowActive || !ferreiroRowActive) {
                lore.add(Component.text((String)""));
                lore.add(Component.text((String)"Coloque mesas proximas (4 blocos)", (TextColor)NamedTextColor.DARK_GRAY));
                lore.add(Component.text((String)"para desbloquear mais funcoes.", (TextColor)NamedTextColor.DARK_GRAY));
            }
            meta.lore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack createRoutesButton() {
        ItemStack item = new ItemStack(Material.COMPASS);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName((Component)Component.text((String)ROUTES_GUI_TITLE, (TextColor)NamedTextColor.GOLD, (TextDecoration[])new TextDecoration[]{TextDecoration.BOLD}));
            ArrayList<TextComponent> lore = new ArrayList<TextComponent>();
            lore.add(Component.text((String)"Gerenciar waypoints.", (TextColor)NamedTextColor.GRAY));
            meta.lore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack createAvaliarButton() {
        ItemStack item = new ItemStack(Material.EMERALD);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName((Component)Component.text((String)"Avaliar Item", (TextColor)NamedTextColor.GREEN, (TextDecoration[])new TextDecoration[]{TextDecoration.BOLD}));
            ArrayList<TextComponent> lore = new ArrayList<TextComponent>();
            lore.add(Component.text((String)"Identifica o item a esquerda.", (TextColor)NamedTextColor.GRAY));
            lore.add(Component.text((String)"Gratuito!", (TextColor)NamedTextColor.GREEN));
            meta.lore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack createCraftPergButton() {
        ItemStack item = new ItemStack(Material.PAPER);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName((Component)Component.text((String)"Fabricar Pergaminho", (TextColor)NamedTextColor.GREEN, (TextDecoration[])new TextDecoration[]{TextDecoration.BOLD}));
            ArrayList<TextComponent> lore = new ArrayList<TextComponent>();
            lore.add(Component.text((String)"4 Papel + 2 Tinta Brilhante + 1 Lapis", (TextColor)NamedTextColor.AQUA));
            lore.add(Component.text((String)"Cooldown: 30s", (TextColor)NamedTextColor.RED));
            meta.lore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack createPocaoButton() {
        ItemStack item = new ItemStack(Material.POTION);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName((Component)Component.text((String)"Fabricar Pocao de Retorno", (TextColor)NamedTextColor.AQUA, (TextDecoration[])new TextDecoration[]{TextDecoration.BOLD}));
            ArrayList<TextComponent> lore = new ArrayList<TextComponent>();
            lore.add(Component.text((String)"4 Ender Pearl + 8 Redstone + 1 Bussola", (TextColor)NamedTextColor.AQUA));
            lore.add(Component.text((String)"Cooldown: 60s", (TextColor)NamedTextColor.RED));
            if (meta instanceof PotionMeta) {
                PotionMeta potionMeta = (PotionMeta)meta;
                potionMeta.setColor(Color.fromRGB((int)255, (int)200, (int)50));
            }
            meta.lore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack createEncantarButton() {
        ItemStack item = new ItemStack(Material.ENCHANTED_BOOK);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName((Component)Component.text((String)"Aplicar Encantamento", (TextColor)NamedTextColor.LIGHT_PURPLE, (TextDecoration[])new TextDecoration[]{TextDecoration.BOLD}));
            ArrayList<TextComponent> lore = new ArrayList<TextComponent>();
            lore.add(Component.text((String)"Item na linha 1 + Livro na linha 4", (TextColor)NamedTextColor.GRAY));
            lore.add(Component.text((String)("Nivel " + this.plugin.getConfig().getInt("merchant.enchant-min-level", 35) + "+ | Cooldown: 30s"), (TextColor)NamedTextColor.RED));
            meta.lore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack createCraftPedraButton() {
        ItemStack item = new ItemStack(Material.GOLD_INGOT);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName((Component)Component.text((String)"Fabricar Pedra de Refinamento", (TextColor)NamedTextColor.GOLD, (TextDecoration[])new TextDecoration[]{TextDecoration.BOLD}));
            ArrayList<TextComponent> lore = new ArrayList<TextComponent>();
            lore.add(Component.text((String)"25 Diamante + 20 Ferro + 15 Oleo + 10 Ouro", (TextColor)NamedTextColor.AQUA));
            lore.add(Component.text((String)("Nivel " + this.plugin.getConfig().getInt("merchant.enchant-min-level", 35) + "+ | Cooldown: 120s"), (TextColor)NamedTextColor.RED));
            meta.lore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack createCraftSeloButton() {
        ItemStack item = new ItemStack(Material.FILLED_MAP);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            int ep = this.plugin.getConfig().getInt("merchant.selo-craft.ender-pearls", 16);
            int hb = this.plugin.getConfig().getInt("merchant.selo-craft.honey-bottles", 1);
            int fm = this.plugin.getConfig().getInt("merchant.selo-craft.filled-maps", 1);
            int ll = this.plugin.getConfig().getInt("merchant.selo-craft.lapis-lazuli", 8);
            int gi = this.plugin.getConfig().getInt("merchant.selo-craft.gold-ingots", 6);
            meta.displayName((Component)Component.text((String)"Fabricar Selo de Rota", (TextColor)NamedTextColor.GOLD, (TextDecoration[])new TextDecoration[]{TextDecoration.BOLD}));
            ArrayList<TextComponent> lore = new ArrayList<TextComponent>();
            lore.add(Component.text((String)""));
            lore.add(Component.text((String)"Clique para fabricar!", (TextColor)NamedTextColor.YELLOW));
            lore.add(Component.text((String)""));
            lore.add(Component.text((String)"Materiais:", (TextColor)NamedTextColor.WHITE, (TextDecoration[])new TextDecoration[]{TextDecoration.UNDERLINED}));
            lore.add(Component.text((String)("  - " + ep + "x Ender Pearl"), (TextColor)NamedTextColor.AQUA));
            lore.add(Component.text((String)("  - " + hb + "x Pote de Mel"), (TextColor)NamedTextColor.AQUA));
            lore.add(Component.text((String)("  - " + fm + "x Mapa Preenchido"), (TextColor)NamedTextColor.AQUA));
            lore.add(Component.text((String)("  - " + ll + "x Lapis Lazuli"), (TextColor)NamedTextColor.AQUA));
            lore.add(Component.text((String)("  - " + gi + "x Lingote de Ouro"), (TextColor)NamedTextColor.AQUA));
            lore.add(Component.text((String)""));
            lore.add(Component.text((String)"Requer nivel 10+", (TextColor)NamedTextColor.RED));
            meta.lore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack createCancelButton() {
        ItemStack item = new ItemStack(Material.BARRIER);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName((Component)Component.text((String)"Fechar", (TextColor)NamedTextColor.RED, (TextDecoration[])new TextDecoration[]{TextDecoration.BOLD}));
            item.setItemMeta(meta);
        }
        return item;
    }

    private boolean hasMaterials(Player player, Material material, int amount) {
        int count = 0;
        for (ItemStack item : player.getInventory().getContents()) {
            if (item == null || item.getType() != material || (count += item.getAmount()) < amount) continue;
            return true;
        }
        return false;
    }

    private boolean consumeMaterials(Player player, Material material, int amount) {
        int remaining = amount;
        ItemStack[] contents = player.getInventory().getContents();
        for (int i = 0; i < contents.length && remaining > 0; ++i) {
            ItemStack item = contents[i];
            if (item == null || item.getType() != material) continue;
            int take = Math.min(item.getAmount(), remaining);
            remaining -= take;
            if (take >= item.getAmount()) {
                player.getInventory().setItem(i, null);
                continue;
            }
            item.setAmount(item.getAmount() - take);
        }
        return remaining <= 0;
    }

    private boolean hasMMOItemInInventory(Player player, String mmoItemId, int amount) {
        int count = 0;
        for (ItemStack item : player.getInventory().getContents()) {
            if (item == null || item.getType().isAir()) continue;
            try {
                NBTItem nbt = NBTItem.get((ItemStack)item);
                if (!nbt.hasType() || !mmoItemId.equals(nbt.getString("MMOITEMS_ITEM_ID")) || (count += item.getAmount()) < amount) continue;
                return true;
            }
            catch (Exception exception) {
                // empty catch block
            }
        }
        return false;
    }

    private boolean consumeMMOItemFromInventory(Player player, String mmoItemId, int amount) {
        int remaining = amount;
        ItemStack[] contents = player.getInventory().getContents();
        for (int i = 0; i < contents.length && remaining > 0; ++i) {
            ItemStack item = contents[i];
            if (item == null || item.getType().isAir()) continue;
            try {
                NBTItem nbt = NBTItem.get((ItemStack)item);
                if (!nbt.hasType() || !mmoItemId.equals(nbt.getString("MMOITEMS_ITEM_ID"))) continue;
                int take = Math.min(item.getAmount(), remaining);
                remaining -= take;
                if (take >= item.getAmount()) {
                    player.getInventory().setItem(i, null);
                    continue;
                }
                item.setAmount(item.getAmount() - take);
                continue;
            }
            catch (Exception exception) {
                // empty catch block
            }
        }
        return remaining <= 0;
    }

    private ItemStack createMMOItem(String type, String id) {
        try {
            net.Indyuce.mmoitems.api.Type mmoType = net.Indyuce.mmoitems.api.Type.get((String)type);
            if (mmoType == null) {
                this.plugin.getLogger().warning("[MERCHANT] Tipo MMOItem nao encontrado: " + type);
                return null;
            }
            ItemStack result = MMOItems.plugin.getItem(mmoType, id);
            if (result == null) {
                this.plugin.getLogger().warning("[MERCHANT] Item MMOItem nao encontrado: " + type + "/" + id);
            }
            return result;
        }
        catch (Exception e) {
            this.plugin.getLogger().warning("[MERCHANT] Erro ao criar MMOItem " + type + "/" + id + ": " + e.getMessage());
            return null;
        }
    }

    private boolean isGUIDecoration(ItemStack item) {
        if (item == null) {
            return true;
        }
        Material mat = item.getType();
        return mat.name().endsWith("_STAINED_GLASS_PANE") || mat == Material.GLASS_PANE || mat == Material.BARRIER || mat == Material.LIME_CONCRETE || mat == Material.ARROW;
    }

    private boolean isSlotEmpty(Inventory inv, int slot) {
        ItemStack item = inv.getItem(slot);
        return item == null || item.getType().isAir();
    }

    private void sendLockedMessage(Player player, String requirement) {
        player.sendMessage(ChatColor.translateAlternateColorCodes((char)'&', (String)"&c[Mercador] &lFuncao bloqueada!"));
        player.sendMessage(ChatColor.translateAlternateColorCodes((char)'&', (String)("&7Esta funcao precisa de uma &e" + requirement + "&7 num raio de 10 blocos.")));
        player.sendMessage(ChatColor.translateAlternateColorCodes((char)'&', (String)"&8Veja a pagina 'Pre-requisitos' do livro guia do Mercador."));
        player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
    }

    private void syncInventoryLater(Player player) {
        Bukkit.getScheduler().runTask((Plugin)this.plugin, () -> {
            if (player.isOnline()) {
                player.updateInventory();
            }
        });
    }

    private String getInventoryTitle(InventoryView view) {
        try {
            Component titleComp = view.title();
            if (titleComp instanceof TextComponent) {
                TextComponent tc = (TextComponent)titleComp;
                return tc.content();
            }
            return PlainTextComponentSerializer.plainText().serialize(titleComp);
        }
        catch (Exception e) {
            return "";
        }
    }

    private String formatMaterial(Material mat) {
        return switch (mat) {
            case Material.BARREL -> "Barril";
            case Material.CHEST -> "Bau";
            case Material.LECTERN -> "Estante de Livros";
            case Material.EMERALD_BLOCK -> "Bloco de Esmeralda";
            case Material.CARTOGRAPHY_TABLE -> "Mesa de Cartografia";
            default -> {
                String name = mat.name().toLowerCase().replace("_", " ");
                StringBuilder sb = new StringBuilder();
                for (String word : name.split(" ")) {
                    if (word.isEmpty()) continue;
                    sb.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1)).append(" ");
                }
                yield sb.toString().trim();
            }
        };
    }

    record CachedScan(MerchantEnvironment env, long timestamp) {
    }

    record MerchantEnvironment(Map<Material, Integer> blockCounts, int itemFrameCount, double totalBonus, int nearbyPlayers, double playerBonus, boolean alquimistaNearby, boolean ferreiroNearby, boolean guerreiroNearby, boolean mercadorNearby, boolean enchantingTablePresent, boolean anvilPresent) {
    }

    record NearbyClassInfo(int count, boolean mercador, boolean alquimista, boolean ferreiro, boolean guerreiro) {
    }

    record MerchantSession(Location tableLoc, MerchantEnvironment env, double efficiency, int breakpoint, int playerLevel, boolean alquimistaRowActive, boolean ferreiroRowActive) {
    }
}

