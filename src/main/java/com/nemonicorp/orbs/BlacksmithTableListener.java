/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.gson.Gson
 *  io.lumine.mythic.lib.api.item.ItemTag
 *  io.lumine.mythic.lib.api.item.NBTItem
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
 *  org.bukkit.block.Block
 *  org.bukkit.boss.BarColor
 *  org.bukkit.boss.BarFlag
 *  org.bukkit.boss.BarStyle
 *  org.bukkit.boss.BossBar
 *  org.bukkit.entity.HumanEntity
 *  org.bukkit.entity.Player
 *  org.bukkit.event.EventHandler
 *  org.bukkit.event.EventPriority
 *  org.bukkit.event.Listener
 *  org.bukkit.event.block.Action
 *  org.bukkit.event.inventory.InventoryClickEvent
 *  org.bukkit.event.inventory.InventoryCloseEvent
 *  org.bukkit.event.inventory.InventoryDragEvent
 *  org.bukkit.event.player.PlayerInteractEvent
 *  org.bukkit.event.player.PlayerQuitEvent
 *  org.bukkit.inventory.Inventory
 *  org.bukkit.inventory.InventoryView
 *  org.bukkit.inventory.ItemStack
 *  org.bukkit.inventory.PlayerInventory
 *  org.bukkit.inventory.meta.Damageable
 *  org.bukkit.inventory.meta.ItemMeta
 *  org.bukkit.plugin.Plugin
 *  org.bukkit.potion.PotionEffect
 *  org.bukkit.potion.PotionEffectType
 *  org.bukkit.scheduler.BukkitTask
 */
package com.nemonicorp.orbs;

import com.google.gson.Gson;
import com.nemonicorp.orbs.NemonicOrbPlugin;
import io.lumine.mythic.lib.api.item.ItemTag;
import io.lumine.mythic.lib.api.item.NBTItem;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
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
import org.bukkit.block.Block;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarFlag;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitTask;

public class BlacksmithTableListener
implements Listener {
    private final NemonicOrbPlugin plugin;
    private static final Gson GSON = new Gson();
    private final Map<UUID, ForgingSession> activeSessions = new ConcurrentHashMap<UUID, ForgingSession>();
    private final Map<UUID, Long> cooldowns = new ConcurrentHashMap<UUID, Long>();
    private final Map<String, CachedScan> scanCache = new ConcurrentHashMap<String, CachedScan>();
    private final Map<UUID, BukkitTask> auraTasks = new ConcurrentHashMap<UUID, BukkitTask>();
    public static final String NBT_FORGE_COUNT = "NEMONICORB_FORGE_COUNT";
    private static final int GUI_SIZE = 45;
    private static final int SLOT_EFFICIENCY = 3;
    private static final int SLOT_INFO = 5;
    private static final int SLOT_INPUT = 10;
    private static final int SLOT_ARROW_1 = 11;
    private static final int SLOT_ACTION = 12;
    private static final int SLOT_ARROW_2 = 13;
    private static final int SLOT_OUTPUT = 14;
    private static final int SLOT_REPAIR = 16;
    private static final int SLOT_S1 = 27;
    private static final int SLOT_S2 = 29;
    private static final int SLOT_S3 = 31;
    private static final int SLOT_S4 = 33;
    private static final int SLOT_CANCEL = 35;
    private static final String GUI_TITLE_RAW = "Mesa do Ferreiro";
    private static final Map<Material, double[]> FORGE_BLOCKS = Map.ofEntries(Map.entry(Material.BLAST_FURNACE, new double[]{0.06, 10.0}), Map.entry(Material.WATER_CAULDRON, new double[]{0.08, 8.0}), Map.entry(Material.IRON_BLOCK, new double[]{0.05, 10.0}), Map.entry(Material.LAVA, new double[]{0.08, 6.0}), Map.entry(Material.ANVIL, new double[]{0.06, 5.0}), Map.entry(Material.CHIPPED_ANVIL, new double[]{0.06, 5.0}), Map.entry(Material.DAMAGED_ANVIL, new double[]{0.06, 5.0}));
    private static final Map<String, Integer> INDICATOR_THRESHOLDS = Map.of("BLAST_FURNACE", 3, "WATER_CAULDRON", 2, "IRON_BLOCK", 3, "LAVA", 2, "ANVIL", 2);
    private static final Set<Material> HEATING_BLOCKS = Set.of(Material.BLAST_FURNACE);
    private static final Set<Material> SHAPING_BLOCKS = Set.of(Material.ANVIL, Material.CHIPPED_ANVIL, Material.DAMAGED_ANVIL);
    private static final Set<Material> COOLING_BLOCKS = Set.of(Material.WATER_CAULDRON, Material.CAULDRON);
    private static final Set<Material> REFINING_BLOCKS = Set.of(Material.GRINDSTONE);
    private static final Set<Material> STAGE_ONLY_BLOCKS = Set.of(Material.GRINDSTONE, Material.CAULDRON);
    private static final Set<String> ACCEPTED_TYPES = Set.of("SWORD", "DAGGER", "AXE", "BOW", "CROSSBOW", "STAFF", "WAND", "WHIP", "MUSKET", "LUTE", "SPEAR", "GREATSTAFF", "GREATSWORD", "HAMMER", "KATANA", "HALBERD", "GAUNTLET", "TRIDENT", "MACE", "HELMET", "CHESTPLATE", "LEGGINGS", "BOOTS", "ARMOR", "PICKAXE", "SHOVEL", "HOE", "FISHING_ROD", "SHEARS", "TOOL", "SHIELD");

    public BlacksmithTableListener(NemonicOrbPlugin plugin) {
        this.plugin = plugin;
    }

    private boolean debug() {
        return this.plugin.getConfig().getBoolean("debug", true);
    }

    public boolean hasActiveSession(Player player) {
        ForgingSession session = this.activeSessions.get(player.getUniqueId());
        return session != null && session.currentStage != ForgeStage.IDLE && session.currentStage != ForgeStage.COMPLETE;
    }

    public boolean isFerreiro(Player player) {
        try {
            boolean result;
            String className = this.getPlayerClassName(player);
            String configClassName = this.plugin.getConfig().getString("blacksmith.class-name", "Ferreiro");
            boolean bl = result = className != null && className.equalsIgnoreCase(configClassName);
            if (this.debug()) {
                this.plugin.getLogger().info("[FORGE] isFerreiro: " + player.getName() + " classe='" + className + "' config='" + configClassName + "' resultado=" + result);
            }
            return result;
        }
        catch (Exception e) {
            this.plugin.getLogger().warning("[FORGE] Erro ao verificar classe: " + e.getMessage());
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
            this.plugin.getLogger().warning("[FORGE] Erro ao obter classe: " + e.getMessage());
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

    private ForgeEnvironment scanEnvironment(Location loc, Player player) {
        String cacheKey = loc.getWorld().getName() + "," + loc.getBlockX() + "," + loc.getBlockY() + "," + loc.getBlockZ();
        long cacheTTL = this.plugin.getConfig().getLong("blacksmith.scan-cache-seconds", 60L) * 1000L;
        CachedScan cached = this.scanCache.get(cacheKey);
        if (cached != null && System.currentTimeMillis() - cached.timestamp() < cacheTTL) {
            ForgeEnvironment old = cached.env();
            NearbyClassInfo classInfo = this.scanNearbyClasses(loc, player);
            double playerBonus = Math.min((double)classInfo.count * 0.05, 0.25);
            return new ForgeEnvironment(old.blockCounts(), old.totalBonus(), classInfo.count, playerBonus, classInfo.mercador, classInfo.alquimista, classInfo.ferreiro);
        }
        int radius = this.plugin.getConfig().getInt("blacksmith.scan-radius", 20);
        EnumMap<Material, Integer> blockCounts = new EnumMap<Material, Integer>(Material.class);
        World world = loc.getWorld();
        int cx = loc.getBlockX();
        int cy = loc.getBlockY();
        int cz = loc.getBlockZ();
        for (int x = cx - radius; x <= cx + radius; ++x) {
            for (int y = Math.max(world.getMinHeight(), cy - radius); y <= Math.min(world.getMaxHeight() - 1, cy + radius); ++y) {
                for (int z = cz - radius; z <= cz + radius; ++z) {
                    Material mat;
                    double distSq = (x - cx) * (x - cx) + (y - cy) * (y - cy) + (z - cz) * (z - cz);
                    if (distSq > (double)(radius * radius) || !FORGE_BLOCKS.containsKey(mat = world.getBlockAt(x, y, z).getType()) && !STAGE_ONLY_BLOCKS.contains(mat)) continue;
                    blockCounts.merge(mat, 1, Integer::sum);
                }
            }
        }
        double totalBonus = 0.0;
        for (Map.Entry entry : blockCounts.entrySet()) {
            double[] cfg = FORGE_BLOCKS.get(entry.getKey());
            if (cfg == null) continue;
            int count = Math.min((Integer)entry.getValue(), (int)cfg[1]);
            totalBonus += (double)count * cfg[0];
        }
        NearbyClassInfo classInfo = this.scanNearbyClasses(loc, player);
        double playerBonus = Math.min((double)classInfo.count * 0.05, 0.25);
        ForgeEnvironment env = new ForgeEnvironment(blockCounts, totalBonus, classInfo.count, playerBonus, classInfo.mercador, classInfo.alquimista, classInfo.ferreiro);
        this.scanCache.put(cacheKey, new CachedScan(env, System.currentTimeMillis()));
        if (this.debug()) {
            this.plugin.getLogger().info("[FORGE-SCAN] bonus=" + String.format("%.0f%%", totalBonus * 100.0) + " jogadores=" + classInfo.count + " (+=" + String.format("%.0f%%", playerBonus * 100.0) + ") mercador=" + classInfo.mercador + " alquimista=" + classInfo.alquimista + " ferreiro=" + classInfo.ferreiro);
        }
        return env;
    }

    private NearbyClassInfo scanNearbyClasses(Location loc, Player exclude) {
        int radius = this.plugin.getConfig().getInt("blacksmith.scan-radius", 20);
        int count = 0;
        boolean mercador = false;
        boolean alquimista = false;
        boolean ferreiro = false;
        String ferreiroClass = this.plugin.getConfig().getString("blacksmith.class-name", "Ferreiro");
        String alquimistaClass = this.plugin.getConfig().getString("transmutation.class-name", "Alquimista");
        String mercadorClass = "Mercador";
        for (Player p : loc.getWorld().getPlayers()) {
            if (p.equals((Object)exclude) || p.getLocation().distanceSquared(loc) > (double)(radius * radius)) continue;
            ++count;
            String cls = this.getPlayerClassName(p);
            if (cls == null) continue;
            if (ferreiroClass.equalsIgnoreCase(cls)) {
                ferreiro = true;
            }
            if (alquimistaClass.equalsIgnoreCase(cls)) {
                alquimista = true;
            }
            if (!mercadorClass.equalsIgnoreCase(cls)) continue;
            mercador = true;
        }
        return new NearbyClassInfo(count, mercador, alquimista, ferreiro);
    }

    private double calculateEfficiency(ForgeEnvironment env) {
        return 1.0 + env.totalBonus() + env.playerBonus();
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

    private Map<ForgeStage, Boolean> checkRequiredBlocks(ForgeEnvironment env) {
        EnumMap<ForgeStage, Boolean> result = new EnumMap<ForgeStage, Boolean>(ForgeStage.class);
        result.put(ForgeStage.HEATING, HEATING_BLOCKS.stream().anyMatch(m -> env.blockCounts().getOrDefault(m, 0) > 0));
        result.put(ForgeStage.SHAPING, SHAPING_BLOCKS.stream().anyMatch(m -> env.blockCounts().getOrDefault(m, 0) > 0));
        result.put(ForgeStage.COOLING, COOLING_BLOCKS.stream().anyMatch(m -> env.blockCounts().getOrDefault(m, 0) > 0));
        result.put(ForgeStage.REFINING, REFINING_BLOCKS.stream().anyMatch(m -> env.blockCounts().getOrDefault(m, 0) > 0));
        return result;
    }

    private boolean isValidForgeInput(ItemStack item) {
        if (item == null || item.getType().isAir()) {
            return false;
        }
        NBTItem nbt = NBTItem.get((ItemStack)item);
        if (nbt.hasTag("NEMONICORB_TIER")) {
            return true;
        }
        if (nbt.hasTag("MMOITEMS_ITEM_TYPE")) {
            String type = nbt.getString("MMOITEMS_ITEM_TYPE");
            return type != null && ACCEPTED_TYPES.contains(type);
        }
        return false;
    }

    public void openMainGUI(Player player, Location tableLoc) {
        int minLevel = this.plugin.getConfig().getInt("blacksmith.min-level", 5);
        int playerLevel = this.getPlayerLevel(player);
        if (playerLevel < minLevel) {
            player.sendMessage(ChatColor.translateAlternateColorCodes((char)'&', (String)this.plugin.getConfig().getString("blacksmith.messages.level-too-low", "&c[Forja] Voce precisa de nivel %level% na classe Ferreiro!").replace("%level%", String.valueOf(minLevel))));
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
            return;
        }
        ForgingSession existing = this.activeSessions.get(player.getUniqueId());
        if (existing != null && existing.currentStage == ForgeStage.COMPLETE) {
            this.openCollectGUI(player, existing);
            return;
        }
        if (existing != null && existing.currentStage != ForgeStage.IDLE) {
            if (existing.currentStage == ForgeStage.REFINING && existing.refiningClicks >= existing.refiningRequired) {
                if (this.debug()) {
                    this.plugin.getLogger().warning("[FORGE] Fallback: finalizando sessao presa no refino para " + player.getName() + " (" + existing.refiningClicks + "/" + existing.refiningRequired + ")");
                }
                this.completeForging(player, existing);
                this.openCollectGUI(player, existing);
                return;
            }
            String stageName = this.getStageBlockName(existing.currentStage);
            player.sendMessage(ChatColor.translateAlternateColorCodes((char)'&', (String)("&e[Forja] Voce tem uma forja em andamento! Va ate: &6" + stageName)));
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
            return;
        }
        long cooldownMs = this.plugin.getConfig().getLong("blacksmith.cooldown-seconds", 30L) * 1000L;
        Long lastUse = this.cooldowns.get(player.getUniqueId());
        long now = System.currentTimeMillis();
        if (lastUse != null && now - lastUse < cooldownMs) {
            long remaining = (cooldownMs - (now - lastUse)) / 1000L;
            player.sendMessage(ChatColor.translateAlternateColorCodes((char)'&', (String)this.plugin.getConfig().getString("blacksmith.messages.cooldown", "&c[Forja] Aguarde %seconds%s antes de forjar novamente!").replace("%seconds%", String.valueOf(remaining))));
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
            return;
        }
        ForgeEnvironment env = this.scanEnvironment(tableLoc, player);
        double efficiency = this.calculateEfficiency(env);
        int breakpoint = this.getBreakpoint(efficiency);
        Map<ForgeStage, Boolean> blocksAvailable = this.checkRequiredBlocks(env);
        Inventory gui = Bukkit.createInventory(null, (int)45, (Component)Component.text((String)GUI_TITLE_RAW, (TextColor)NamedTextColor.GOLD, (TextDecoration[])new TextDecoration[]{TextDecoration.BOLD}));
        ItemStack glass = this.createGlassPane(Material.BLACK_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < 45; ++i) {
            gui.setItem(i, glass);
        }
        gui.setItem(3, this.createEfficiencyItem(env, efficiency, breakpoint, playerLevel));
        gui.setItem(5, this.createInfoItem());
        gui.setItem(10, null);
        gui.setItem(11, this.createArrowPane());
        gui.setItem(12, this.createStartButton());
        gui.setItem(13, this.createArrowPane());
        gui.setItem(14, this.createLockedOutput());
        gui.setItem(16, this.createRepairButton(player, null));
        gui.setItem(27, this.createStageBlockIndicator("Aquecimento", Material.BLAST_FURNACE, blocksAvailable.get((Object)ForgeStage.HEATING)));
        gui.setItem(28, this.createArrowPane());
        gui.setItem(29, this.createStageBlockIndicator("Modelagem", Material.ANVIL, blocksAvailable.get((Object)ForgeStage.SHAPING)));
        gui.setItem(30, this.createArrowPane());
        gui.setItem(31, this.createStageBlockIndicator("Resfriamento", Material.WATER_BUCKET, blocksAvailable.get((Object)ForgeStage.COOLING)));
        gui.setItem(32, this.createArrowPane());
        gui.setItem(33, this.createStageBlockIndicator("Refino", Material.GRINDSTONE, blocksAvailable.get((Object)ForgeStage.REFINING)));
        gui.setItem(34, glass);
        gui.setItem(35, this.createCancelButton());
        ForgingSession session = new ForgingSession(player.getUniqueId(), tableLoc, env, efficiency, breakpoint);
        this.activeSessions.put(player.getUniqueId(), session);
        player.openInventory(gui);
        player.playSound(tableLoc, Sound.BLOCK_ANVIL_PLACE, 0.6f, 0.8f);
        if (this.debug()) {
            this.plugin.getLogger().info("[FORGE] GUI aberta para " + player.getName() + " nivel=" + playerLevel + " eficiencia=" + String.format("%.0f%%", efficiency * 100.0) + " breakpoint=" + breakpoint + " blocks=" + String.valueOf(blocksAvailable));
        }
    }

    private void openCollectGUI(Player player, ForgingSession session) {
        Inventory gui = Bukkit.createInventory(null, (int)45, (Component)Component.text((String)GUI_TITLE_RAW, (TextColor)NamedTextColor.GOLD, (TextDecoration[])new TextDecoration[]{TextDecoration.BOLD}));
        ItemStack glass = this.createGlassPane(Material.BLACK_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < 45; ++i) {
            gui.setItem(i, glass);
        }
        gui.setItem(3, this.createEfficiencyItem(session.environment, session.efficiency, session.breakpoint, this.getPlayerLevel(player)));
        gui.setItem(14, session.inputItem);
        gui.setItem(27, this.createGlassPane(Material.LIME_STAINED_GLASS_PANE, "Aquecimento - Concluido"));
        gui.setItem(29, this.createGlassPane(Material.LIME_STAINED_GLASS_PANE, "Modelagem - Concluido"));
        gui.setItem(31, this.createGlassPane(Material.LIME_STAINED_GLASS_PANE, "Resfriamento - Concluido"));
        gui.setItem(33, this.createGlassPane(Material.LIME_STAINED_GLASS_PANE, "Refino - Concluido"));
        player.openInventory(gui);
        player.playSound(session.tableLoc, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.0f);
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
        ForgingSession session = this.activeSessions.get(player.getUniqueId());
        if (session == null) {
            event.setCancelled(true);
            return;
        }
        int slot = event.getRawSlot();
        Inventory topInv = event.getView().getTopInventory();
        if (event.getHotbarButton() >= 0 && slot < 45 && slot != 10) {
            event.setCancelled(true);
            return;
        }
        if (event.getHotbarButton() >= 0 && slot == 10 && session.currentStage != ForgeStage.IDLE) {
            event.setCancelled(true);
            return;
        }
        if (slot >= 45) {
            if (event.isShiftClick() && session.currentStage == ForgeStage.IDLE) {
                event.setCancelled(true);
                ItemStack clicked = event.getCurrentItem();
                if (clicked != null && !clicked.getType().isAir() && this.isSlotEmpty(topInv, 10)) {
                    topInv.setItem(10, clicked.clone());
                    event.setCurrentItem(null);
                    Bukkit.getScheduler().runTask((Plugin)this.plugin, () -> {
                        if (player.isOnline() && this.activeSessions.containsKey(player.getUniqueId())) {
                            topInv.setItem(16, this.createRepairButton(player, topInv.getItem(10)));
                        }
                    });
                }
            } else if (event.isShiftClick()) {
                event.setCancelled(true);
            }
            return;
        }
        if (slot == 10 && session.currentStage == ForgeStage.IDLE) {
            return;
        }
        if (slot == 14 && session.currentStage == ForgeStage.COMPLETE) {
            if (event.isShiftClick()) {
                event.setCancelled(true);
                ItemStack outputItem = topInv.getItem(14);
                if (outputItem != null && !outputItem.getType().isAir() && outputItem.getType() != Material.BARRIER) {
                    topInv.setItem(14, null);
                    HashMap overflow = player.getInventory().addItem(new ItemStack[]{outputItem});
                    for (ItemStack drop : overflow.values()) {
                        player.getWorld().dropItemNaturally(player.getLocation(), drop);
                    }
                    session.inputItem = null;
                    this.syncInventoryLater(player);
                }
            } else {
                ItemStack outputItem = topInv.getItem(14);
                if (outputItem != null && !outputItem.getType().isAir() && outputItem.getType() != Material.BARRIER) {
                    session.inputItem = null;
                    this.syncInventoryLater(player);
                    return;
                }
            }
            event.setCancelled(true);
            return;
        }
        event.setCancelled(true);
        if (slot == 12 && session.currentStage == ForgeStage.IDLE) {
            this.startForging(player, topInv, session);
        } else if (slot == 16 && session.currentStage == ForgeStage.IDLE) {
            this.handleRepair(player, topInv, session);
            topInv.setItem(16, this.createRepairButton(player, topInv.getItem(10)));
        } else if (slot == 35) {
            this.cancelForging(player, session, true);
        }
        if (slot == 10 && session.currentStage == ForgeStage.IDLE) {
            Bukkit.getScheduler().runTaskLater((Plugin)this.plugin, () -> {
                if (player.isOnline() && this.activeSessions.containsKey(player.getUniqueId())) {
                    topInv.setItem(16, this.createRepairButton(player, topInv.getItem(10)));
                }
            }, 1L);
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
        ForgingSession session = this.activeSessions.get(player.getUniqueId());
        if (session == null) {
            event.setCancelled(true);
            return;
        }
        Iterator iterator = event.getRawSlots().iterator();
        while (iterator.hasNext()) {
            int slot = (Integer)iterator.next();
            if (slot < 45 && slot != 10) {
                event.setCancelled(true);
                return;
            }
            if (slot != 10 || session.currentStage == ForgeStage.IDLE) continue;
            event.setCancelled(true);
            return;
        }
    }

    @EventHandler(priority=EventPriority.HIGH)
    public void onInventoryClose(InventoryCloseEvent event) {
        HumanEntity humanEntity = event.getPlayer();
        if (!(humanEntity instanceof Player)) {
            return;
        }
        Player player = (Player)humanEntity;
        String title = this.getInventoryTitle(event.getView());
        if (!GUI_TITLE_RAW.equals(title)) {
            return;
        }
        ForgingSession session = this.activeSessions.get(player.getUniqueId());
        if (session == null) {
            return;
        }
        Inventory inv = event.getView().getTopInventory();
        if (session.currentStage == ForgeStage.IDLE) {
            this.activeSessions.remove(player.getUniqueId());
            ItemStack input = inv.getItem(10);
            inv.setItem(10, null);
            this.returnItem(player, input);
        } else if (session.currentStage == ForgeStage.COMPLETE) {
            ItemStack output = inv.getItem(14);
            inv.setItem(14, null);
            if (output != null && !output.getType().isAir() && output.getType() != Material.BARRIER) {
                this.returnItem(player, output);
            }
            this.activeSessions.remove(player.getUniqueId());
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        this.stopForgeAura(player);
        ForgingSession session = this.activeSessions.remove(player.getUniqueId());
        if (session != null) {
            this.removeBossBar(session);
            if (session.stageTask != null) {
                session.stageTask.cancel();
            }
            if (session.inputItem != null) {
                this.returnItem(player, session.inputItem);
            }
        }
    }

    @EventHandler(priority=EventPriority.LOWEST)
    public void onBlockInteract(PlayerInteractEvent event) {
        boolean isForgeBlock;
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }
        Player player = event.getPlayer();
        ForgingSession session = this.activeSessions.get(player.getUniqueId());
        if (session == null) {
            return;
        }
        if (session.currentStage == ForgeStage.IDLE || session.currentStage == ForgeStage.COMPLETE) {
            return;
        }
        Block block = event.getClickedBlock();
        if (block == null) {
            return;
        }
        Material mat = block.getType();
        boolean bl = isForgeBlock = HEATING_BLOCKS.contains(mat) || SHAPING_BLOCKS.contains(mat) || COOLING_BLOCKS.contains(mat) || REFINING_BLOCKS.contains(mat);
        if (isForgeBlock) {
            event.setCancelled(true);
        }
        int radius = this.plugin.getConfig().getInt("blacksmith.scan-radius", 20);
        if (block.getLocation().distanceSquared(session.tableLoc) > (double)(radius * radius)) {
            return;
        }
        switch (session.currentStage.ordinal()) {
            case 1: {
                if (!HEATING_BLOCKS.contains(mat)) {
                    return;
                }
                this.handleHeatingAction(player, session);
                break;
            }
            case 2: {
                if (!SHAPING_BLOCKS.contains(mat)) {
                    return;
                }
                this.handleShapingAction(player, session);
                break;
            }
            case 3: {
                if (!COOLING_BLOCKS.contains(mat)) {
                    return;
                }
                this.handleCoolingAction(player, session);
                break;
            }
            case 4: {
                if (!REFINING_BLOCKS.contains(mat)) {
                    return;
                }
                this.handleRefiningAction(player, session);
                break;
            }
        }
    }

    private boolean isOilCraftInput(ItemStack item) {
        return item != null && item.getType() == Material.GOLD_NUGGET && item.getAmount() >= 9;
    }

    private void startForging(Player player, Inventory gui, ForgingSession session) {
        Map<ForgeStage, Boolean> blocks;
        int tier;
        ItemStack input = gui.getItem(10);
        if (this.isOilCraftInput(input)) {
            this.startOilCrafting(player, gui, session, input);
            return;
        }
        if (!this.isValidForgeInput(input)) {
            player.sendMessage(ChatColor.translateAlternateColorCodes((char)'&', (String)this.plugin.getConfig().getString("blacksmith.messages.not-forgeable", "&c[Forja] Este item nao pode ser forjado!")));
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
            return;
        }
        NBTItem inputNbt = NBTItem.get((ItemStack)input);
        session.itemTier = tier = inputNbt.hasTag("NEMONICORB_TIER") ? inputNbt.getInteger("NEMONICORB_TIER") : 0;
        int currentForgeCount = inputNbt.hasTag(NBT_FORGE_COUNT) ? inputNbt.getInteger(NBT_FORGE_COUNT) : 0;
        session.forgeCount = currentForgeCount + 1;
        if (tier == 3) {
            int uniqueMinLevel = this.plugin.getConfig().getInt("blacksmith.unique-min-level", 35);
            int playerLevel = this.getPlayerLevel(player);
            if (playerLevel < uniqueMinLevel) {
                player.sendMessage(ChatColor.translateAlternateColorCodes((char)'&', (String)("&c[Forja] Itens Unicos requerem nivel " + uniqueMinLevel + " de Ferreiro!")));
                player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
                return;
            }
        }
        if (!(blocks = this.checkRequiredBlocks(session.environment)).getOrDefault((Object)ForgeStage.HEATING, false).booleanValue()) {
            player.sendMessage(ChatColor.translateAlternateColorCodes((char)'&', (String)"&c[Forja] Nenhuma Blast Furnace encontrada nas proximidades!"));
            return;
        }
        if (!blocks.getOrDefault((Object)ForgeStage.SHAPING, false).booleanValue()) {
            player.sendMessage(ChatColor.translateAlternateColorCodes((char)'&', (String)"&c[Forja] Nenhuma Bigorna encontrada nas proximidades!"));
            return;
        }
        if (!blocks.getOrDefault((Object)ForgeStage.COOLING, false).booleanValue()) {
            player.sendMessage(ChatColor.translateAlternateColorCodes((char)'&', (String)"&c[Forja] Nenhum Caldeirao encontrado nas proximidades!"));
            return;
        }
        if (!blocks.getOrDefault((Object)ForgeStage.REFINING, false).booleanValue()) {
            player.sendMessage(ChatColor.translateAlternateColorCodes((char)'&', (String)"&c[Forja] Nenhum Rebolo encontrado nas proximidades!"));
            return;
        }
        int fuelAmount = this.plugin.getConfig().getInt("blacksmith.fuel.amount-by-tier." + tier, this.plugin.getConfig().getInt("blacksmith.fuel.amount", 10));
        if (!this.consumeFuel(player, fuelAmount)) {
            player.sendMessage(ChatColor.translateAlternateColorCodes((char)'&', (String)this.plugin.getConfig().getString("blacksmith.messages.no-fuel", "&c[Forja] Voce precisa de %amount%x Carvao para forjar!").replace("%amount%", String.valueOf(fuelAmount))));
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
            return;
        }
        session.inputItem = input.clone();
        gui.setItem(10, null);
        session.shapingRequired = this.plugin.getConfig().getInt("blacksmith.shaping.hits-required", 4);
        session.refiningRequired = this.plugin.getConfig().getInt("blacksmith.refining.clicks-required", 2);
        double effScale = 1.0 + (session.efficiency - 1.0) * 0.5;
        session.shapingRequired = (int)Math.ceil((double)session.shapingRequired * effScale);
        session.refiningRequired = (int)Math.ceil((double)session.refiningRequired * effScale);
        player.sendMessage(ChatColor.translateAlternateColorCodes((char)'&', (String)"&6[Forja] A forja comeca! Va ate a &eBLAST FURNACE &6para aquecer o metal!"));
        player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_USE, 0.8f, 0.8f);
        session.currentStage = ForgeStage.HEATING;
        player.closeInventory();
        this.startHeatingTicker(player, session);
        this.startForgeAura(player, session);
        if (this.debug()) {
            this.plugin.getLogger().info("[FORGE] " + player.getName() + " iniciou forja. Tier=" + tier + " forgeCount=" + session.forgeCount + " fuel=" + fuelAmount + " eficiencia=" + String.format("%.0f%%", session.efficiency * 100.0) + " shapeHits=" + session.shapingRequired + " refineClicks=" + session.refiningRequired);
        }
    }

    private void startOilCrafting(Player player, Inventory gui, ForgingSession session, ItemStack input) {
        int nuggetsReq = this.plugin.getConfig().getInt("blacksmith.oil-crafting.gold-nuggets-required", 9);
        if (input.getAmount() < nuggetsReq) {
            player.sendMessage(ChatColor.translateAlternateColorCodes((char)'&', (String)("&c[Forja] Voce precisa de " + nuggetsReq + "x Pepitas de Ouro!")));
            return;
        }
        Map<ForgeStage, Boolean> blocks = this.checkRequiredBlocks(session.environment);
        for (Map.Entry<ForgeStage, Boolean> e : blocks.entrySet()) {
            if (e.getValue().booleanValue()) continue;
            player.sendMessage(ChatColor.translateAlternateColorCodes((char)'&', (String)"&c[Forja] Blocos necessarios nao encontrados!"));
            return;
        }
        int fuelCost = this.plugin.getConfig().getInt("blacksmith.oil-crafting.fuel-cost", 10);
        if (!this.consumeFuel(player, fuelCost)) {
            player.sendMessage(ChatColor.translateAlternateColorCodes((char)'&', (String)("&c[Forja] Voce precisa de " + fuelCost + "x Carvao para fabricar o oleo!")));
            return;
        }
        input.setAmount(input.getAmount() - nuggetsReq);
        if (input.getAmount() <= 0) {
            gui.setItem(10, null);
        }
        session.isOilCrafting = true;
        session.inputItem = new ItemStack(Material.GOLD_NUGGET, nuggetsReq);
        session.shapingRequired = this.plugin.getConfig().getInt("blacksmith.shaping.hits-required", 4);
        session.refiningRequired = this.plugin.getConfig().getInt("blacksmith.refining.clicks-required", 2);
        player.sendMessage(ChatColor.translateAlternateColorCodes((char)'&', (String)"&6[Forja] Fabricacao de Oleo de Polimento! Va ate a &eBLAST FURNACE&6!"));
        player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_USE, 0.8f, 0.8f);
        session.currentStage = ForgeStage.HEATING;
        player.closeInventory();
        this.startHeatingTicker(player, session);
        this.startForgeAura(player, session);
    }

    private void advanceStage(Player player, ForgingSession session) {
        String stageMsg;
        if (session.stageTask != null) {
            session.stageTask.cancel();
            session.stageTask = null;
        }
        this.removeBossBar(session);
        switch (session.currentStage.ordinal()) {
            case 1: {
                session.currentStage = ForgeStage.SHAPING;
                this.startShapingTicker(player, session);
                String string = "&6[Forja] Metal aquecido! Agora va ate a &eBIGORNA &6para modelar!";
                break;
            }
            case 2: {
                session.currentStage = ForgeStage.COOLING;
                this.startCoolingTicker(player, session);
                String string = "&6[Forja] Modelado! Agora va ate o &eCALDEIRAO &6para resfriar!";
                break;
            }
            case 3: {
                session.currentStage = ForgeStage.REFINING;
                this.startRefiningTicker(player, session);
                String string = "&6[Forja] Resfriado! Agora va ate o &eREBOLO &6para refinar!";
                break;
            }
            case 4: {
                this.completeForging(player, session);
                String string = null;
                break;
            }
            default: {
                String string = stageMsg = null;
            }
        }
        if (stageMsg != null) {
            player.sendMessage(ChatColor.translateAlternateColorCodes((char)'&', (String)stageMsg));
            player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.8f, 1.2f);
        }
    }

    private void startHeatingTicker(Player player, ForgingSession session) {
        session.heatLevel = 0.0;
        session.bossBar = Bukkit.createBossBar((String)"Aquecimento [0%] - Clique na Blast Furnace!", (BarColor)BarColor.RED, (BarStyle)BarStyle.SEGMENTED_10, (BarFlag[])new BarFlag[0]);
        session.bossBar.setProgress(0.0);
        session.bossBar.addPlayer(player);
        int tickInterval = this.plugin.getConfig().getInt("blacksmith.heating.tick-interval", 2);
        double decayPerTick = this.plugin.getConfig().getDouble("blacksmith.heating.decay-per-tick", 0.5);
        session.stageTask = Bukkit.getScheduler().runTaskTimer((Plugin)this.plugin, () -> {
            if (!player.isOnline()) {
                this.cancelForging(player, session, false);
                return;
            }
            session.heatLevel = Math.max(0.0, session.heatLevel - decayPerTick);
            double progress = Math.max(0.0, Math.min(1.0, session.heatLevel / 100.0));
            session.bossBar.setProgress(progress);
            BarColor color = session.heatLevel < 40.0 ? BarColor.RED : (session.heatLevel < 60.0 ? BarColor.YELLOW : (session.heatLevel <= 85.0 ? BarColor.GREEN : BarColor.RED));
            session.bossBar.setColor(color);
            session.bossBar.setTitle("Aquecimento [" + (int)session.heatLevel + "%] - Clique na Blast Furnace!");
            Location tl = session.tableLoc.clone().add(0.5, 1.2, 0.5);
            if (session.heatLevel > 60.0) {
                tl.getWorld().spawnParticle(Particle.FLAME, tl, 3, 0.2, 0.1, 0.2, 0.01);
            } else if (session.heatLevel > 30.0) {
                tl.getWorld().spawnParticle(Particle.SMOKE, tl, 2, 0.2, 0.1, 0.2, 0.01);
            }
        }, (long)tickInterval, (long)tickInterval);
    }

    private void handleHeatingAction(Player player, ForgingSession session) {
        long now = System.currentTimeMillis();
        if (now - session.lastHeatClick < 100L) {
            return;
        }
        session.lastHeatClick = now;
        double heatPerClick = this.plugin.getConfig().getDouble("blacksmith.heating.heat-per-click", 8.0);
        if (player.isSneaking()) {
            this.evaluateHeating(player, session);
            return;
        }
        session.heatLevel = Math.min(100.0, session.heatLevel + heatPerClick);
        player.playSound(player.getLocation(), Sound.BLOCK_FURNACE_FIRE_CRACKLE, 0.5f, 1.0f + (float)(session.heatLevel / 200.0));
    }

    private void evaluateHeating(Player player, ForgingSession session) {
        double center = this.plugin.getConfig().getDouble("blacksmith.heating.ideal-center", 70.0);
        double zoneBase = this.plugin.getConfig().getDouble("blacksmith.heating.ideal-zone-base", 20.0);
        double zoneMin = this.plugin.getConfig().getDouble("blacksmith.heating.ideal-zone-min", 8.0);
        double effFactor = Math.min((session.efficiency - 1.0) / 1.92, 1.0);
        double zoneWidth = zoneBase - (zoneBase - zoneMin) * effFactor;
        double lower = center - zoneWidth / 2.0;
        double upper = center + zoneWidth / 2.0;
        double heat = session.heatLevel;
        if (heat >= lower && heat <= upper) {
            double centerDist = Math.abs(heat - center) / (zoneWidth / 2.0);
            if (centerDist < 0.3) {
                session.qualityScore += 15.0;
                player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_USE, 0.8f, 1.5f);
                player.sendActionBar((Component)Component.text((String)"Aquecimento PERFEITO!", (TextColor)NamedTextColor.GREEN));
            } else {
                session.qualityScore += 8.0;
                player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_USE, 0.6f, 1.2f);
                player.sendActionBar((Component)Component.text((String)"Aquecimento Bom", (TextColor)NamedTextColor.YELLOW));
            }
        } else {
            double distance = heat < lower ? lower - heat : heat - upper;
            double penalty = Math.min(distance * 0.5, 20.0);
            session.qualityScore -= penalty;
            if (distance > 20.0) {
                session.failures.add(FailureType.LIGHT);
            }
            player.playSound(player.getLocation(), Sound.ENTITY_ITEM_BREAK, 0.8f, 0.5f);
            player.sendActionBar((Component)Component.text((String)"Aquecimento RUIM!", (TextColor)NamedTextColor.RED));
        }
        session.qualityScore = Math.max(0.0, Math.min(100.0, session.qualityScore));
        if (this.debug()) {
            this.plugin.getLogger().info("[FORGE] Heating: heat=" + (int)heat + " zone=[" + (int)lower + "-" + (int)upper + "] quality=" + String.format("%.1f", session.qualityScore));
        }
        this.advanceStage(player, session);
    }

    private void startShapingTicker(Player player, ForgingSession session) {
        session.cursorPosition = 0.0;
        session.cursorForward = true;
        session.shapingHits = 0;
        session.shapingPerfects = 0;
        session.shapingCombo = 0;
        session.bossBar = Bukkit.createBossBar((String)("Modelagem - Clique na Bigorna no momento certo! [0/" + session.shapingRequired + "]"), (BarColor)BarColor.BLUE, (BarStyle)BarStyle.SEGMENTED_20, (BarFlag[])new BarFlag[0]);
        session.bossBar.setProgress(0.0);
        session.bossBar.addPlayer(player);
        double speedBase = this.plugin.getConfig().getDouble("blacksmith.shaping.cursor-speed-base", 0.04);
        double speedMax = this.plugin.getConfig().getDouble("blacksmith.shaping.cursor-speed-max", 0.1);
        double effFactor = Math.min((session.efficiency - 1.0) / 1.92, 1.0);
        double speed = speedBase + (speedMax - speedBase) * effFactor;
        session.stageTask = Bukkit.getScheduler().runTaskTimer((Plugin)this.plugin, () -> {
            if (!player.isOnline()) {
                this.cancelForging(player, session, false);
                return;
            }
            if (session.cursorForward) {
                session.cursorPosition += speed;
                if (session.cursorPosition >= 1.0) {
                    session.cursorPosition = 1.0;
                    session.cursorForward = false;
                }
            } else {
                session.cursorPosition -= speed;
                if (session.cursorPosition <= 0.0) {
                    session.cursorPosition = 0.0;
                    session.cursorForward = true;
                }
            }
            session.bossBar.setProgress(Math.max(0.0, Math.min(1.0, session.cursorPosition)));
            double greenBase = this.plugin.getConfig().getDouble("blacksmith.shaping.green-zone-base", 0.3);
            double greenMin = this.plugin.getConfig().getDouble("blacksmith.shaping.green-zone-min", 0.12);
            double greenZone = greenBase - (greenBase - greenMin) * effFactor;
            double lower = 0.5 - greenZone / 2.0;
            double upper = 0.5 + greenZone / 2.0;
            BarColor c = session.cursorPosition >= lower && session.cursorPosition <= upper ? BarColor.GREEN : (Math.abs(session.cursorPosition - 0.5) < greenZone ? BarColor.YELLOW : BarColor.RED);
            session.bossBar.setColor(c);
            session.bossBar.setTitle("Modelagem - Clique na Bigorna! [" + session.shapingHits + "/" + session.shapingRequired + "]");
        }, 1L, 1L);
    }

    private void handleShapingAction(Player player, ForgingSession session) {
        HitQuality quality;
        double greenBase = this.plugin.getConfig().getDouble("blacksmith.shaping.green-zone-base", 0.3);
        double greenMin = this.plugin.getConfig().getDouble("blacksmith.shaping.green-zone-min", 0.12);
        double effFactor = Math.min((session.efficiency - 1.0) / 1.92, 1.0);
        double greenZone = greenBase - (greenBase - greenMin) * effFactor;
        double lower = 0.5 - greenZone / 2.0;
        double upper = 0.5 + greenZone / 2.0;
        double pos = session.cursorPosition;
        if (pos >= lower && pos <= upper) {
            double centerDist = Math.abs(pos - 0.5) / (greenZone / 2.0);
            if (centerDist < 0.25) {
                quality = HitQuality.PERFECT;
                session.qualityScore += 10.0;
                ++session.shapingPerfects;
                ++session.shapingCombo;
                player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_USE, 1.0f, 1.5f);
                player.getLocation().getWorld().spawnParticle(Particle.CRIT, player.getLocation().add(0.0, 1.5, 0.0), 10, 0.3, 0.2, 0.3, 0.1);
            } else {
                quality = HitQuality.GOOD;
                session.qualityScore += 5.0;
                session.shapingCombo = 0;
                player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_USE, 0.8f, 1.2f);
            }
        } else {
            double dist;
            double d = dist = pos < lower ? lower - pos : pos - upper;
            if (dist < 0.15) {
                quality = HitQuality.BAD;
                session.qualityScore -= 5.0;
            } else {
                quality = HitQuality.MISS;
                session.qualityScore -= 10.0;
                session.failures.add(FailureType.LIGHT);
            }
            session.shapingCombo = 0;
            player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_LAND, 0.6f, 0.5f);
        }
        int comboThreshold = this.plugin.getConfig().getInt("blacksmith.shaping.combo-threshold", 3);
        double comboBonus = this.plugin.getConfig().getDouble("blacksmith.shaping.combo-bonus", 5.0);
        if (session.shapingCombo >= comboThreshold) {
            session.qualityScore += comboBonus;
            session.shapingCombo = 0;
            player.sendActionBar((Component)Component.text((String)("COMBO! +" + (int)comboBonus + " qualidade!"), (TextColor)NamedTextColor.GOLD, (TextDecoration[])new TextDecoration[]{TextDecoration.BOLD}));
            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.5f, 1.5f);
        } else {
            NamedTextColor c = quality == HitQuality.PERFECT ? NamedTextColor.GREEN : (quality == HitQuality.GOOD ? NamedTextColor.YELLOW : NamedTextColor.RED);
            player.sendActionBar((Component)Component.text((String)(quality.name() + " | Combo: " + session.shapingCombo + " | Qualidade: " + (int)session.qualityScore + "%"), (TextColor)c));
        }
        session.qualityScore = Math.max(0.0, Math.min(100.0, session.qualityScore));
        ++session.shapingHits;
        if (session.shapingHits >= session.shapingRequired) {
            this.advanceStage(player, session);
        }
    }

    private void startCoolingTicker(Player player, ForgingSession session) {
        session.coolingValue = 1.0;
        session.coolingTick = 0;
        session.bossBar = Bukkit.createBossBar((String)"Resfriamento [100%] - Clique no Caldeirao para parar!", (BarColor)BarColor.BLUE, (BarStyle)BarStyle.SEGMENTED_20, (BarFlag[])new BarFlag[0]);
        session.bossBar.setProgress(1.0);
        session.bossBar.addPlayer(player);
        double decBase = this.plugin.getConfig().getDouble("blacksmith.cooling.decrement-base", 0.008);
        double ampBase = this.plugin.getConfig().getDouble("blacksmith.cooling.fluctuation-amplitude-base", 0.0);
        double ampMax = this.plugin.getConfig().getDouble("blacksmith.cooling.fluctuation-amplitude-max", 0.006);
        double effFactor = Math.min((session.efficiency - 1.0) / 1.92, 1.0);
        double amplitude = ampBase + (ampMax - ampBase) * effFactor;
        int tickInterval = this.plugin.getConfig().getInt("blacksmith.cooling.tick-interval", 2);
        session.stageTask = Bukkit.getScheduler().runTaskTimer((Plugin)this.plugin, () -> {
            if (!player.isOnline()) {
                this.cancelForging(player, session, false);
                return;
            }
            ++session.coolingTick;
            double fluctuation = amplitude * Math.sin((double)session.coolingTick * 0.15);
            double dec = decBase + fluctuation;
            session.coolingValue = Math.max(0.0, session.coolingValue - dec);
            session.bossBar.setProgress(Math.max(0.0, Math.min(1.0, session.coolingValue)));
            double idealCenter = this.plugin.getConfig().getDouble("blacksmith.cooling.ideal-center", 0.35);
            double zoneBase = this.plugin.getConfig().getDouble("blacksmith.cooling.ideal-zone-base", 0.15);
            double zoneMin = this.plugin.getConfig().getDouble("blacksmith.cooling.ideal-zone-min", 0.06);
            double zoneWidth = zoneBase - (zoneBase - zoneMin) * effFactor;
            double lower = idealCenter - zoneWidth / 2.0;
            double upper = idealCenter + zoneWidth / 2.0;
            BarColor c = session.coolingValue >= lower && session.coolingValue <= upper ? BarColor.GREEN : (Math.abs(session.coolingValue - idealCenter) < zoneWidth * 1.5 ? BarColor.YELLOW : BarColor.RED);
            session.bossBar.setColor(c);
            int pct = (int)(session.coolingValue * 100.0);
            session.bossBar.setTitle("Resfriamento [" + pct + "%] - Clique no Caldeirao para parar!");
            Location tl = session.tableLoc.clone().add(0.5, 1.2, 0.5);
            tl.getWorld().spawnParticle(Particle.CLOUD, tl, 1, 0.2, 0.1, 0.2, 0.01);
            if (session.coolingValue <= 0.0) {
                session.failures.add(FailureType.MEDIUM);
                session.qualityScore -= 15.0;
                player.sendActionBar((Component)Component.text((String)"Resfriou demais! Penalidade!", (TextColor)NamedTextColor.RED));
                player.playSound(player.getLocation(), Sound.ENTITY_ITEM_BREAK, 0.8f, 0.5f);
                this.advanceStage(player, session);
            }
        }, (long)tickInterval, (long)tickInterval);
    }

    private void handleCoolingAction(Player player, ForgingSession session) {
        double idealCenter = this.plugin.getConfig().getDouble("blacksmith.cooling.ideal-center", 0.35);
        double zoneBase = this.plugin.getConfig().getDouble("blacksmith.cooling.ideal-zone-base", 0.15);
        double zoneMin = this.plugin.getConfig().getDouble("blacksmith.cooling.ideal-zone-min", 0.06);
        double effFactor = Math.min((session.efficiency - 1.0) / 1.92, 1.0);
        double zoneWidth = zoneBase - (zoneBase - zoneMin) * effFactor;
        double lower = idealCenter - zoneWidth / 2.0;
        double upper = idealCenter + zoneWidth / 2.0;
        double val = session.coolingValue;
        if (val >= lower && val <= upper) {
            double centerDist = Math.abs(val - idealCenter) / (zoneWidth / 2.0);
            if (centerDist < 0.3) {
                session.qualityScore += 15.0;
                player.sendActionBar((Component)Component.text((String)"Resfriamento PERFEITO!", (TextColor)NamedTextColor.GREEN));
                player.playSound(player.getLocation(), Sound.ENTITY_GENERIC_SPLASH, 0.8f, 1.5f);
            } else {
                session.qualityScore += 8.0;
                player.sendActionBar((Component)Component.text((String)"Resfriamento Bom", (TextColor)NamedTextColor.YELLOW));
                player.playSound(player.getLocation(), Sound.ENTITY_GENERIC_SPLASH, 0.6f, 1.2f);
            }
        } else {
            double distance = val < lower ? lower - val : val - upper;
            double penalty = Math.min(distance * 30.0, 20.0);
            session.qualityScore -= penalty;
            if (distance > 0.2) {
                session.failures.add(FailureType.LIGHT);
            }
            player.sendActionBar((Component)Component.text((String)"Resfriamento RUIM!", (TextColor)NamedTextColor.RED));
            player.playSound(player.getLocation(), Sound.ENTITY_ITEM_BREAK, 0.8f, 0.5f);
        }
        session.qualityScore = Math.max(0.0, Math.min(100.0, session.qualityScore));
        if (this.debug()) {
            this.plugin.getLogger().info("[FORGE] Cooling: val=" + String.format("%.2f", val) + " zone=[" + String.format("%.2f", lower) + "-" + String.format("%.2f", upper) + "] quality=" + String.format("%.1f", session.qualityScore));
        }
        this.advanceStage(player, session);
    }

    private void startRefiningTicker(Player player, ForgingSession session) {
        session.refiningClicks = 0;
        session.refiningTick = 0;
        session.refiningWindowOpen = false;
        session.bossBar = Bukkit.createBossBar((String)("Refino - Espere... [0/" + session.refiningRequired + "]"), (BarColor)BarColor.RED, (BarStyle)BarStyle.SOLID, (BarFlag[])new BarFlag[0]);
        session.bossBar.setProgress(0.0);
        session.bossBar.addPlayer(player);
        int timingWindowTicks = this.plugin.getConfig().getInt("blacksmith.refining.timing-window-ticks", 16);
        int gapBase = this.plugin.getConfig().getInt("blacksmith.refining.timing-gap-ticks-base", 40);
        int gapMin = this.plugin.getConfig().getInt("blacksmith.refining.timing-gap-ticks-min", 20);
        double effFactor = Math.min((session.efficiency - 1.0) / 1.92, 1.0);
        int gap = (int)((double)gapBase - (double)(gapBase - gapMin) * effFactor);
        session.stageTask = Bukkit.getScheduler().runTaskTimer((Plugin)this.plugin, () -> {
            if (!player.isOnline()) {
                this.cancelForging(player, session, false);
                return;
            }
            ++session.refiningTick;
            int cycleLength = gap + timingWindowTicks;
            int posInCycle = session.refiningTick % cycleLength;
            if (posInCycle >= gap) {
                if (!session.refiningWindowOpen) {
                    session.refiningWindowOpen = true;
                    player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BELL, 0.8f, 2.0f);
                }
                session.bossBar.setColor(BarColor.GREEN);
                session.bossBar.setTitle("Refino - AGORA! Clique no Rebolo! [" + session.refiningClicks + "/" + session.refiningRequired + "]");
                double windowProgress = (double)(posInCycle - gap) / (double)timingWindowTicks;
                session.bossBar.setProgress(Math.max(0.0, Math.min(1.0, windowProgress)));
            } else {
                session.refiningWindowOpen = false;
                session.bossBar.setColor(BarColor.RED);
                session.bossBar.setTitle("Refino - Espere... [" + session.refiningClicks + "/" + session.refiningRequired + "]");
                session.bossBar.setProgress(0.0);
            }
        }, 1L, 1L);
    }

    private void handleRefiningAction(Player player, ForgingSession session) {
        double qualityBonus = this.plugin.getConfig().getDouble("blacksmith.refining.quality-bonus", 3.0);
        double penaltyChance = this.plugin.getConfig().getDouble("blacksmith.refining.penalty-removal-chance", 0.8);
        if (session.refiningWindowOpen) {
            ++session.refiningClicks;
            if (!session.failures.isEmpty() && Math.random() < penaltyChance) {
                session.failures.remove(session.failures.size() - 1);
                player.sendActionBar((Component)Component.text((String)("Falha removida! [" + session.refiningClicks + "/" + session.refiningRequired + "]"), (TextColor)NamedTextColor.GREEN));
            } else {
                session.qualityScore = Math.min(100.0, session.qualityScore + qualityBonus);
                player.sendActionBar((Component)Component.text((String)("+" + (int)qualityBonus + " qualidade! [" + session.refiningClicks + "/" + session.refiningRequired + "]"), (TextColor)NamedTextColor.GREEN));
            }
            player.playSound(player.getLocation(), Sound.BLOCK_GRINDSTONE_USE, 1.0f, 1.5f);
            if (session.refiningClicks >= session.refiningRequired) {
                this.advanceStage(player, session);
            }
        } else {
            session.qualityScore = Math.max(0.0, session.qualityScore - 5.0);
            session.failures.add(FailureType.LIGHT);
            player.sendActionBar((Component)Component.text((String)"Fora do timing! -5 qualidade", (TextColor)NamedTextColor.RED));
            player.playSound(player.getLocation(), Sound.BLOCK_GRINDSTONE_USE, 0.5f, 0.5f);
        }
    }

    private void completeForging(Player player, ForgingSession session) {
        Location center;
        World world;
        double breakChance;
        if (session.stageTask != null) {
            session.stageTask.cancel();
            session.stageTask = null;
        }
        this.removeBossBar(session);
        this.stopForgeAura(player);
        if (session.isOilCrafting) {
            this.completeOilCrafting(player, session);
            return;
        }
        switch (session.breakpoint) {
            case 1: {
                session.qualityScore += 5.0;
                break;
            }
            case 2: {
                session.qualityScore += 10.0;
                break;
            }
            case 3: {
                session.qualityScore += 15.0;
                break;
            }
            case 4: {
                session.qualityScore += 20.0;
                break;
            }
            case 5: {
                session.qualityScore += 25.0;
            }
        }
        for (FailureType f : session.failures) {
            switch (f.ordinal()) {
                case 0: {
                    session.qualityScore -= 3.0;
                    break;
                }
                case 1: {
                    session.qualityScore -= 8.0;
                    break;
                }
                case 2: {
                    session.qualityScore -= 15.0;
                    break;
                }
                case 3: {
                    session.qualityScore -= 30.0;
                }
            }
        }
        session.qualityScore = Math.max(0.0, Math.min(100.0, session.qualityScore));
        switch (session.forgeCount) {
            case 1: 
            case 2: {
                double d = 0.0;
                break;
            }
            case 3: {
                double d = 0.25;
                break;
            }
            case 4: {
                double d = 0.45;
                break;
            }
            default: {
                double d = breakChance = 0.75;
            }
        }
        if (breakChance > 0.0 && Math.random() < breakChance) {
            session.inputItem = null;
            session.currentStage = ForgeStage.COMPLETE;
            this.cooldowns.put(player.getUniqueId(), System.currentTimeMillis());
            this.activeSessions.remove(player.getUniqueId());
            Bukkit.getScheduler().runTask((Plugin)this.plugin, () -> ((Player)player).closeInventory());
            player.sendMessage(ChatColor.translateAlternateColorCodes((char)'&', (String)("&c[Forja] O item nao resistiu a " + session.forgeCount + "a forja e quebrou!")));
            player.playSound(player.getLocation(), Sound.ENTITY_ITEM_BREAK, 1.0f, 0.5f);
            return;
        }
        double critChance = this.plugin.getConfig().getDouble("blacksmith.critical-failure-chance", 0.01);
        if (session.breakpoint >= 4 && session.qualityScore < 25.0 && Math.random() < critChance) {
            session.inputItem = null;
            session.currentStage = ForgeStage.COMPLETE;
            this.cooldowns.put(player.getUniqueId(), System.currentTimeMillis());
            this.activeSessions.remove(player.getUniqueId());
            Bukkit.getScheduler().runTask((Plugin)this.plugin, () -> ((Player)player).closeInventory());
            player.sendMessage(ChatColor.translateAlternateColorCodes((char)'&', (String)this.plugin.getConfig().getString("blacksmith.messages.forging-failed", "&c[Forja] O item quebrou durante a forja!")));
            player.playSound(player.getLocation(), Sound.ENTITY_ITEM_BREAK, 1.0f, 0.5f);
            return;
        }
        ForgeResult result = this.applyQualityToItem(session);
        session.currentStage = ForgeStage.COMPLETE;
        this.cooldowns.put(player.getUniqueId(), System.currentTimeMillis());
        if (result != null) {
            player.sendMessage(ChatColor.translateAlternateColorCodes((char)'&', (String)("&a[Forja] Forja concluida! Qualidade: &" + this.getColorCode(result.qualityColor()) + result.qualityTag() + " &a(+" + String.format("%.1f", result.melhoriaReal()) + "%)")));
            if (!result.improvements().isEmpty()) {
                player.sendMessage(ChatColor.translateAlternateColorCodes((char)'&', (String)"&e[Forja] Melhorias aplicadas:"));
                for (String imp : result.improvements()) {
                    player.sendMessage(ChatColor.translateAlternateColorCodes((char)'&', (String)("  &7- " + imp)));
                }
            }
        }
        player.sendMessage(ChatColor.translateAlternateColorCodes((char)'&', (String)"&e[Forja] Volte a Mesa do Ferreiro (Bigorna) para coletar o item!"));
        if (result != null && session.qualityScore >= (double)this.plugin.getConfig().getInt("blacksmith.quality.masterwork", 90)) {
            player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.0f);
            player.getLocation().getWorld().spawnParticle(Particle.TOTEM_OF_UNDYING, player.getLocation().add(0.0, 1.0, 0.0), 30, 0.5, 0.5, 0.5, 0.1);
        } else if (session.qualityScore >= (double)this.plugin.getConfig().getInt("blacksmith.quality.excellent", 75)) {
            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);
        } else {
            player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_USE, 0.8f, 1.0f);
        }
        if (session.breakpoint >= 5 && (world = (center = session.tableLoc.clone().add(0.5, 1.0, 0.5)).getWorld()) != null) {
            try {
                player.playSound(center, Sound.ENTITY_WIND_CHARGE_WIND_BURST, 1.5f, 0.8f);
            }
            catch (Exception exception) {
                // empty catch block
            }
            world.spawnParticle(Particle.EXPLOSION, center, 3, 0.5, 0.5, 0.5, 0.0);
        }
        if (this.debug()) {
            this.plugin.getLogger().info("[FORGE] " + player.getName() + " completou forja. Qualidade=" + String.format("%.1f", session.qualityScore) + " falhas=" + session.failures.size() + " breakpoint=" + session.breakpoint + " forgeCount=" + session.forgeCount);
        }
    }

    private void completeOilCrafting(Player player, ForgingSession session) {
        ItemStack oil = null;
        try {
            Class<?> miClass;
            Object miPlugin;
            Class<?> typeClass = Class.forName("net.Indyuce.mmoitems.api.Type");
            Object consumableType = typeClass.getMethod("get", String.class).invoke(null, "CONSUMABLE");
            if (consumableType != null && (miPlugin = (miClass = Class.forName("net.Indyuce.mmoitems.MMOItems")).getField("plugin").get(null)) != null) {
                oil = (ItemStack)miClass.getMethod("getItem", typeClass, String.class).invoke(miPlugin, consumableType, "OLEO_DE_POLIMENTO");
            }
        }
        catch (Exception e) {
            this.plugin.getLogger().warning("[FORGE-OIL] Erro ao criar Oleo via MMOItems: " + e.getMessage());
        }
        if (oil == null) {
            oil = new ItemStack(Material.HONEY_BOTTLE);
            ItemMeta meta = oil.getItemMeta();
            if (meta != null) {
                meta.displayName((Component)Component.text((String)"Oleo de Polimento", (TextColor)NamedTextColor.GOLD, (TextDecoration[])new TextDecoration[]{TextDecoration.BOLD}));
                ArrayList<TextComponent> lore = new ArrayList<TextComponent>();
                lore.add(Component.text((String)"Aplique em um equipamento para polir.", (TextColor)NamedTextColor.GRAY));
                lore.add(Component.text((String)("Fabricado por " + player.getName()), (TextColor)NamedTextColor.DARK_GRAY));
                meta.lore(lore);
                oil.setItemMeta(meta);
            }
            this.plugin.getLogger().warning("[FORGE-OIL] MMOItem OLEO_DE_POLIMENTO nao encontrado, usando fallback.");
        }
        session.inputItem = oil;
        session.currentStage = ForgeStage.COMPLETE;
        this.cooldowns.put(player.getUniqueId(), System.currentTimeMillis());
        player.sendMessage(ChatColor.translateAlternateColorCodes((char)'&', (String)"&a[Forja] Oleo de Polimento fabricado com sucesso!"));
        player.sendMessage(ChatColor.translateAlternateColorCodes((char)'&', (String)"&e[Forja] Volte a Mesa do Ferreiro (Bigorna) para coletar!"));
        player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.0f);
    }

    private String getColorCode(NamedTextColor color) {
        if (color == NamedTextColor.GOLD) {
            return "6";
        }
        if (color == NamedTextColor.GREEN) {
            return "a";
        }
        if (color == NamedTextColor.WHITE) {
            return "f";
        }
        if (color == NamedTextColor.GRAY) {
            return "7";
        }
        return "f";
    }

    private ForgeResult applyQualityToItem(ForgingSession session) {
        double melhoriaExibida;
        ArrayList<String> improvements;
        NamedTextColor qualityColor;
        String qualityTag;
        double bonusMult;
        double capMaximo;
        int playerLevel;
        ItemStack beforeForge;
        block34: {
            if (session.inputItem == null) {
                return null;
            }
            beforeForge = session.inputItem.clone();
            Player player = Bukkit.getPlayer((UUID)session.playerId);
            playerLevel = player != null ? this.getPlayerLevel(player) : 0;
            String playerName = player != null ? player.getName() : "Desconhecido";
            double baseCap = this.plugin.getConfig().getDouble("blacksmith.improvement.base-cap", 8.0);
            double perLevel = this.plugin.getConfig().getDouble("blacksmith.improvement.per-level", 0.05);
            double bpBonus = this.plugin.getConfig().getDouble("blacksmith.improvement.breakpoint-bonus." + session.breakpoint, 0.0);
            capMaximo = baseCap + (double)playerLevel * perLevel + bpBonus;
            if (session.environment.mercadorNearby()) {
                capMaximo *= 1.1;
            }
            if (session.environment.alquimistaNearby() || session.environment.ferreiroNearby()) {
                capMaximo *= 0.95;
            }
            if (session.itemTier == 3) {
                capMaximo = Math.max(capMaximo, 5.0);
            }
            double melhoriaReal = session.qualityScore / 100.0 * capMaximo;
            bonusMult = melhoriaReal / 100.0;
            int masterwork = this.plugin.getConfig().getInt("blacksmith.quality.masterwork", 90);
            int excellent = this.plugin.getConfig().getInt("blacksmith.quality.excellent", 75);
            int good = this.plugin.getConfig().getInt("blacksmith.quality.good", 50);
            if (session.qualityScore >= (double)masterwork) {
                qualityTag = "Obra-Prima";
                qualityColor = NamedTextColor.GOLD;
            } else if (session.qualityScore >= (double)excellent) {
                qualityTag = "Excelente";
                qualityColor = NamedTextColor.GREEN;
            } else if (session.qualityScore >= (double)good) {
                qualityTag = "Bom";
                qualityColor = NamedTextColor.WHITE;
            } else {
                qualityTag = "Pobre";
                qualityColor = NamedTextColor.GRAY;
            }
            improvements = new ArrayList<String>();
            boolean hasMods = false;
            boolean modImproved = false;
            try {
                NBTItem nbt;
                block33: {
                    String modsJson;
                    nbt = NBTItem.get((ItemStack)session.inputItem);
                    nbt.addTag(new ItemTag[]{new ItemTag(NBT_FORGE_COUNT, (Object)session.forgeCount)});
                    String string = modsJson = nbt.hasTag("NEMONICORB_MODS") ? nbt.getString("NEMONICORB_MODS") : null;
                    if (modsJson != null && !modsJson.isBlank() && !"[]".equals(modsJson.trim()) && bonusMult > 0.0) {
                        try {
                            List mods = (List)GSON.fromJson(modsJson, List.class);
                            if (mods != null && !mods.isEmpty()) {
                                hasMods = true;
                                int modImprovements = 1;
                                if (Math.random() < 0.15) {
                                    modImprovements = 2;
                                }
                                if (session.itemTier == 3) {
                                    modImprovements = Math.max(modImprovements, 1);
                                }
                                if ((modImprovements = Math.min(modImprovements, mods.size())) >= 1) {
                                    ArrayList<Integer> indices = new ArrayList<Integer>();
                                    for (int i = 0; i < mods.size(); ++i) {
                                        indices.add(i);
                                    }
                                    Collections.shuffle(indices);
                                    int improved = 0;
                                    Iterator iterator = indices.iterator();
                                    while (iterator.hasNext()) {
                                        int idx = (Integer)iterator.next();
                                        if (improved >= modImprovements) break;
                                        Map mod = (Map)mods.get(idx);
                                        Map modStats = (Map)mod.get("stats");
                                        if (modStats == null) continue;
                                        double modBoost = Math.max(0.03, bonusMult * (0.85 + Math.random() * 0.3));
                                        boolean thisModImproved = false;
                                        for (Map.Entry e : modStats.entrySet()) {
                                            Object v = e.getValue();
                                            if (!(v instanceof Number)) continue;
                                            Number num = (Number)v;
                                            double oldVal = num.doubleValue();
                                            double newVal = oldVal * (1.0 + modBoost);
                                            modStats.put((String)e.getKey(), newVal);
                                            thisModImproved = true;
                                            improvements.add("Mod " + String.valueOf(mod.getOrDefault("id", "?")) + "/" + (String)e.getKey() + ": +" + String.format("%.1f%%", modBoost * 100.0));
                                        }
                                        if (thisModImproved) {
                                            modImproved = true;
                                        }
                                        ++improved;
                                    }
                                    nbt.addTag(new ItemTag[]{new ItemTag("NEMONICORB_MODS", (Object)GSON.toJson((Object)mods))});
                                }
                            }
                        }
                        catch (Exception e) {
                            if (!this.debug()) break block33;
                            this.plugin.getLogger().warning("[FORGE] Erro ao melhorar mods: " + e.getMessage());
                        }
                    }
                }
                if (!hasMods) {
                    improvements.add("Sem mods: nenhum aprimoramento aplicado");
                }
                session.inputItem = nbt.toItem();
                try {
                    this.plugin.getOrbListener().updateItemDisplay(session.inputItem, NBTItem.get((ItemStack)session.inputItem));
                }
                catch (Exception e) {
                    if (this.debug()) {
                        this.plugin.getLogger().warning("[FORGE] updateItemDisplay falhou: " + e.getMessage());
                    }
                }
            }
            catch (Exception e) {
                this.plugin.getLogger().warning("[FORGE] Erro ao aplicar bonus NBT: " + e.getMessage());
            }
            melhoriaExibida = modImproved ? melhoriaReal : 0.0;
            try {
                NBTItem forgeNbt = NBTItem.get((ItemStack)session.inputItem);
                forgeNbt.addTag(new ItemTag[]{new ItemTag("NEMONICORB_FORGE_QUALITY", (Object)qualityTag)});
                forgeNbt.addTag(new ItemTag[]{new ItemTag("NEMONICORB_FORGE_SMITH", (Object)(playerName + "|" + playerLevel))});
                forgeNbt.addTag(new ItemTag[]{new ItemTag("NEMONICORB_FORGE_BONUS", (Object)melhoriaExibida)});
                session.inputItem = forgeNbt.toItem();
                this.plugin.getOrbListener().updateItemDisplay(session.inputItem, NBTItem.get((ItemStack)session.inputItem));
            }
            catch (Exception e) {
                if (!this.debug()) break block34;
                this.plugin.getLogger().warning("[FORGE] Falha ao gravar NBT da forja: " + e.getMessage());
            }
        }
        if (this.debug()) {
            this.plugin.getLogger().info("[FORGE] Melhoria: cap=" + String.format("%.1f%%", capMaximo) + " real=" + String.format("%.1f%%", melhoriaExibida) + " mult=" + String.format("%.4f", bonusMult) + " nivel=" + playerLevel + " bp=" + session.breakpoint + " forgeCount=" + session.forgeCount + " mercador=" + session.environment.mercadorNearby() + " improvements=" + improvements.size());
        }
        try {
            Player p = Bukkit.getPlayer((UUID)session.playerId);
            if (p != null) {
                this.plugin.getModifierAuditService().logBeforeAfter("forja:resultado", p, beforeForge, session.inputItem);
            }
        }
        catch (Exception exception) {
            // empty catch block
        }
        return new ForgeResult(qualityTag, qualityColor, melhoriaExibida, capMaximo, improvements);
    }

    private void cancelForging(Player player, ForgingSession session, boolean notify) {
        this.removeBossBar(session);
        this.stopForgeAura(player);
        if (session.stageTask != null) {
            session.stageTask.cancel();
            session.stageTask = null;
        }
        ItemStack returnCandidate = null;
        String returnSource = "NONE";
        if (session.currentStage == ForgeStage.IDLE) {
            try {
                Inventory top;
                ItemStack inputSlot;
                String title;
                InventoryView view = player.getOpenInventory();
                if (view != null && GUI_TITLE_RAW.equals(title = this.getInventoryTitle(view)) && (inputSlot = (top = view.getTopInventory()).getItem(10)) != null && !inputSlot.getType().isAir()) {
                    returnCandidate = inputSlot.clone();
                    returnSource = "GUI_SLOT";
                    top.setItem(10, null);
                }
            }
            catch (Exception exception) {
                // empty catch block
            }
        }
        if (returnCandidate == null && session.inputItem != null && !session.inputItem.getType().isAir()) {
            returnCandidate = session.inputItem;
            returnSource = "SESSION_ITEM";
        }
        if (returnCandidate != null && !returnCandidate.getType().isAir()) {
            this.returnItem(player, returnCandidate);
            if (this.debug()) {
                this.plugin.getLogger().info("[FORGE] Cancel return (" + returnSource + "): " + player.getName() + " -> " + returnCandidate.getType().name() + " x" + returnCandidate.getAmount());
            }
        } else if (this.debug()) {
            this.plugin.getLogger().info("[FORGE] Cancel return (NONE): " + player.getName() + " stage=" + session.currentStage.name());
        }
        session.inputItem = null;
        this.activeSessions.remove(player.getUniqueId());
        if (notify) {
            player.sendMessage(ChatColor.translateAlternateColorCodes((char)'&', (String)this.plugin.getConfig().getString("blacksmith.messages.cancel", "&c[Forja] Forja cancelada.")));
            player.playSound(player.getLocation(), Sound.ENTITY_ITEM_BREAK, 0.6f, 0.8f);
            Bukkit.getScheduler().runTask((Plugin)this.plugin, () -> ((Player)player).closeInventory());
        }
    }

    private void removeBossBar(ForgingSession session) {
        if (session.bossBar != null) {
            session.bossBar.removeAll();
            session.bossBar = null;
        }
    }

    private void startForgeAura(Player player, ForgingSession session) {
        if (session.breakpoint < 5) {
            return;
        }
        this.stopForgeAura(player);
        BukkitTask task = Bukkit.getScheduler().runTaskTimer((Plugin)this.plugin, () -> {
            if (!player.isOnline() || !this.activeSessions.containsKey(player.getUniqueId())) {
                this.stopForgeAura(player);
                return;
            }
            Location loc = session.tableLoc;
            World world = loc.getWorld();
            if (world == null) {
                return;
            }
            double cx = loc.getX() + 0.5;
            double cy = loc.getY() + 1.0;
            double cz = loc.getZ() + 0.5;
            Particle.DustOptions dust = new Particle.DustOptions(Color.fromRGB((int)255, (int)165, (int)0), 1.5f);
            double time = (double)(System.currentTimeMillis() % 3000L) / 3000.0 * 2.0 * Math.PI;
            for (int i = 0; i < 20; ++i) {
                double angle = 0.3141592653589793 * (double)i + time;
                world.spawnParticle(Particle.DUST, cx + 3.0 * Math.cos(angle), cy + 0.5, cz + 3.0 * Math.sin(angle), 1, 0.0, 0.0, 0.0, 0.0, (Object)dust);
            }
            if (!player.hasPotionEffect(PotionEffectType.RESISTANCE)) {
                player.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, 100, 0, true, true, true));
            }
        }, 10L, 10L);
        this.auraTasks.put(player.getUniqueId(), task);
    }

    private void stopForgeAura(Player player) {
        BukkitTask task = this.auraTasks.remove(player.getUniqueId());
        if (task != null) {
            task.cancel();
        }
    }

    private ItemStack createGlassPane(Material material, String name) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName((Component)Component.text((String)name, (TextColor)NamedTextColor.DARK_GRAY));
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack createArrowPane() {
        ItemStack item = new ItemStack(Material.YELLOW_STAINED_GLASS_PANE);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName((Component)Component.text((String)">>>", (TextColor)NamedTextColor.YELLOW));
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack createEfficiencyItem(ForgeEnvironment env, double efficiency, int breakpoint, int playerLevel) {
        ItemStack item = new ItemStack(Material.NETHER_STAR);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName((Component)Component.text((String)"Eficiencia da Forja", (TextColor)NamedTextColor.GOLD, (TextDecoration[])new TextDecoration[]{TextDecoration.BOLD}));
            ArrayList<TextComponent> lore = new ArrayList<TextComponent>();
            lore.add(Component.text((String)("Eficiencia: " + String.format("%.0f%%", efficiency * 100.0)), (TextColor)NamedTextColor.WHITE));
            lore.add(Component.text((String)("Breakpoint: " + breakpoint), (TextColor)NamedTextColor.YELLOW));
            lore.add(Component.text((String)("Nivel: " + playerLevel), (TextColor)NamedTextColor.AQUA));
            lore.add(Component.empty());
            lore.add(Component.text((String)"Blocos detectados:", (TextColor)NamedTextColor.GRAY));
            for (Map.Entry<Material, Integer> entry : env.blockCounts().entrySet()) {
                double[] cfg = FORGE_BLOCKS.get(entry.getKey());
                if (cfg == null) continue;
                int threshold = INDICATOR_THRESHOLDS.getOrDefault(entry.getKey().name(), 1);
                if (entry.getValue() < threshold) continue;
                int count = Math.min(entry.getValue(), (int)cfg[1]);
                String bonus = String.format("+%.0f%%", (double)count * cfg[0] * 100.0);
                lore.add(Component.text((String)("  \u2726 " + this.formatMaterial(entry.getKey()) + ": " + count + " (" + bonus + ")"), (TextColor)NamedTextColor.GRAY));
            }
            if (env.nearbyPlayers() > 0) {
                lore.add(Component.text((String)("  \u2726 Jogadores proximos: " + env.nearbyPlayers() + " (+" + String.format("%.0f%%", env.playerBonus() * 100.0) + ")"), (TextColor)NamedTextColor.GRAY));
            }
            lore.add(Component.empty());
            lore.add(Component.text((String)"Maior eficiencia = maior dificuldade", (TextColor)NamedTextColor.RED));
            lore.add(Component.text((String)"+ melhor resultado potencial!", (TextColor)NamedTextColor.GREEN));
            meta.lore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack createInfoItem() {
        ItemStack item = new ItemStack(Material.ENCHANTED_BOOK);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName((Component)Component.text((String)"Como Forjar", (TextColor)NamedTextColor.AQUA, (TextDecoration[])new TextDecoration[]{TextDecoration.BOLD}));
            ArrayList<TextComponent> lore = new ArrayList<TextComponent>();
            lore.add(Component.text((String)"1. Coloque um equipamento no slot", (TextColor)NamedTextColor.WHITE));
            lore.add(Component.text((String)"2. Clique em Iniciar Forja", (TextColor)NamedTextColor.WHITE));
            lore.add(Component.text((String)"3. Va ate a Blast Furnace - aquecer", (TextColor)NamedTextColor.GOLD));
            lore.add(Component.text((String)"   Clique = aquecer, Agachar+Clique = confirmar", (TextColor)NamedTextColor.GRAY));
            lore.add(Component.text((String)"4. Va ate a Bigorna - modelar", (TextColor)NamedTextColor.GOLD));
            lore.add(Component.text((String)"   Clique quando BossBar estiver verde", (TextColor)NamedTextColor.GRAY));
            lore.add(Component.text((String)"5. Va ate o Caldeirao - resfriar", (TextColor)NamedTextColor.GOLD));
            lore.add(Component.text((String)"   Clique quando BossBar estiver na zona ideal", (TextColor)NamedTextColor.GRAY));
            lore.add(Component.text((String)"6. Va ate o Rebolo - refinar", (TextColor)NamedTextColor.GOLD));
            lore.add(Component.text((String)"   Clique quando BossBar piscar verde", (TextColor)NamedTextColor.GRAY));
            lore.add(Component.text((String)"7. Volte a Mesa do Ferreiro para coletar", (TextColor)NamedTextColor.GREEN));
            lore.add(Component.empty());
            lore.add(Component.text((String)"Blocos necessarios nas proximidades:", (TextColor)NamedTextColor.YELLOW));
            lore.add(Component.text((String)"  Blast Furnace, Bigorna, Caldeirao, Rebolo", (TextColor)NamedTextColor.GRAY));
            meta.lore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack createStartButton() {
        ItemStack item = new ItemStack(Material.ANVIL);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName((Component)Component.text((String)"Iniciar Forja", (TextColor)NamedTextColor.GREEN, (TextDecoration[])new TextDecoration[]{TextDecoration.BOLD}));
            ArrayList<TextComponent> lore = new ArrayList<TextComponent>();
            lore.add(Component.text((String)"Clique para comecar!", (TextColor)NamedTextColor.GRAY));
            meta.lore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack createLockedOutput() {
        ItemStack item = new ItemStack(Material.BARRIER);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName((Component)Component.text((String)"Resultado", (TextColor)NamedTextColor.RED));
            ArrayList<TextComponent> lore = new ArrayList<TextComponent>();
            lore.add(Component.text((String)"Complete a forja para ver", (TextColor)NamedTextColor.GRAY));
            meta.lore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack createStageBlockIndicator(String name, Material icon, boolean available) {
        ItemStack item;
        if (available) {
            item = new ItemStack(icon);
            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                meta.displayName((Component)Component.text((String)name, (TextColor)NamedTextColor.GREEN));
                meta.lore(List.of(Component.text((String)"Bloco encontrado!", (TextColor)NamedTextColor.GRAY)));
                item.setItemMeta(meta);
            }
        } else {
            item = new ItemStack(Material.RED_STAINED_GLASS_PANE);
            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                meta.displayName((Component)Component.text((String)name, (TextColor)NamedTextColor.RED));
                meta.lore(List.of(Component.text((String)"Bloco NAO encontrado!", (TextColor)NamedTextColor.RED)));
                item.setItemMeta(meta);
            }
        }
        return item;
    }

    private ItemStack createCancelButton() {
        ItemStack item = new ItemStack(Material.BARRIER);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName((Component)Component.text((String)"Cancelar", (TextColor)NamedTextColor.RED, (TextDecoration[])new TextDecoration[]{TextDecoration.BOLD}));
            item.setItemMeta(meta);
        }
        return item;
    }

    private boolean consumeFuel(Player player, int amount) {
        int found = 0;
        PlayerInventory inv = player.getInventory();
        for (ItemStack item : inv.getContents()) {
            if (item == null || item.getType() != Material.COAL && item.getType() != Material.CHARCOAL) continue;
            found += item.getAmount();
        }
        if (found < amount) {
            return false;
        }
        int toRemove = amount;
        for (int i = 0; i < inv.getSize() && toRemove > 0; ++i) {
            ItemStack item = inv.getItem(i);
            if (item == null || item.getType() != Material.COAL && item.getType() != Material.CHARCOAL) continue;
            int take = Math.min(item.getAmount(), toRemove);
            item.setAmount(item.getAmount() - take);
            toRemove -= take;
        }
        return true;
    }

    private Material getRepairMaterial(ItemStack item) {
        String name = item.getType().name();
        if (name.startsWith("DIAMOND_")) {
            return Material.DIAMOND;
        }
        if (name.startsWith("GOLDEN_") || name.startsWith("GOLD_")) {
            return Material.GOLD_INGOT;
        }
        if (name.startsWith("NETHERITE_")) {
            return Material.DIAMOND;
        }
        return Material.IRON_INGOT;
    }

    private String formatRepairMaterial(Material mat) {
        return switch (mat) {
            case Material.DIAMOND -> "Diamante(s)";
            case Material.GOLD_INGOT -> "Barra(s) de Ouro";
            default -> "Barra(s) de Ferro";
        };
    }

    private int calculateRepairCost(Player player, ItemStack item) {
        if (item == null || !item.getType().isItem()) {
            return -1;
        }
        ItemMeta itemMeta = item.getItemMeta();
        if (!(itemMeta instanceof Damageable)) {
            return -1;
        }
        Damageable damageable = (Damageable)itemMeta;
        int damage = damageable.getDamage();
        if (damage <= 0) {
            return -1;
        }
        short maxDur = item.getType().getMaxDurability();
        if (maxDur <= 0) {
            return -1;
        }
        double damagePercent = (double)damage / (double)maxDur;
        int maxCost = this.plugin.getConfig().getInt("blacksmith.repair.max-material-cost", 8);
        double levelDiscount = this.plugin.getConfig().getDouble("blacksmith.repair.level-discount", 0.005);
        double minMult = this.plugin.getConfig().getDouble("blacksmith.repair.min-multiplier", 0.5);
        int playerLevel = this.getPlayerLevel(player);
        double discount = Math.max(minMult, 1.0 - (double)playerLevel * levelDiscount);
        int cost = (int)Math.ceil(damagePercent * (double)maxCost * discount);
        return Math.max(1, Math.min(maxCost, cost));
    }

    private boolean consumeRepairMaterials(Player player, Material mat, int amount) {
        int remaining = amount;
        ItemStack[] contents = player.getInventory().getContents();
        for (int i = 0; i < contents.length && remaining > 0; ++i) {
            ItemStack slot = contents[i];
            if (slot == null || slot.getType() != mat) continue;
            int take = Math.min(slot.getAmount(), remaining);
            slot.setAmount(slot.getAmount() - take);
            remaining -= take;
        }
        return remaining <= 0;
    }

    private int countMaterial(Player player, Material mat) {
        int count = 0;
        for (ItemStack slot : player.getInventory().getContents()) {
            if (slot == null || slot.getType() != mat) continue;
            count += slot.getAmount();
        }
        return count;
    }

    private void handleRepair(Player player, Inventory gui, ForgingSession session) {
        ItemStack input = gui.getItem(10);
        if (input == null || input.getType().isAir()) {
            player.sendMessage(ChatColor.translateAlternateColorCodes((char)'&', (String)"&c[Forja] Coloque um item no slot para reparar!"));
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
            return;
        }
        int cost = this.calculateRepairCost(player, input);
        if (cost < 0) {
            player.sendMessage(ChatColor.translateAlternateColorCodes((char)'&', (String)this.plugin.getConfig().getString("blacksmith.messages.repair-not-damaged", "&c[Forja] Este item nao esta danificado!")));
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
            return;
        }
        Material repairMat = this.getRepairMaterial(input);
        String matName = this.formatRepairMaterial(repairMat);
        if (this.countMaterial(player, repairMat) < cost) {
            player.sendMessage(ChatColor.translateAlternateColorCodes((char)'&', (String)this.plugin.getConfig().getString("blacksmith.messages.repair-no-materials", "&c[Forja] Voce precisa de %amount%x %material% para reparar!").replace("%amount%", String.valueOf(cost)).replace("%material%", matName)));
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
            return;
        }
        this.consumeRepairMaterials(player, repairMat, cost);
        ItemMeta itemMeta = input.getItemMeta();
        if (itemMeta instanceof Damageable) {
            Damageable damageable = (Damageable)itemMeta;
            damageable.setDamage(0);
            input.setItemMeta((ItemMeta)damageable);
        }
        player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_USE, 1.0f, 1.2f);
        player.sendMessage(ChatColor.translateAlternateColorCodes((char)'&', (String)this.plugin.getConfig().getString("blacksmith.messages.repair-success", "&a[Forja] Item reparado com sucesso! (-%amount%x %material%)").replace("%amount%", String.valueOf(cost)).replace("%material%", matName)));
        if (this.debug()) {
            this.plugin.getLogger().info("[FORGE] Reparo: " + player.getName() + " custo=" + cost + "x " + repairMat.name());
        }
    }

    private ItemStack createRepairButton(Player player, ItemStack input) {
        ItemStack item = new ItemStack(Material.GOLDEN_PICKAXE);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName((Component)Component.text((String)"Reparar Item", (TextColor)NamedTextColor.GOLD, (TextDecoration[])new TextDecoration[]{TextDecoration.BOLD}));
            ArrayList<TextComponent> lore = new ArrayList<TextComponent>();
            if (input != null && !input.getType().isAir()) {
                int cost = this.calculateRepairCost(player, input);
                if (cost > 0) {
                    Material repairMat = this.getRepairMaterial(input);
                    String matName = this.formatRepairMaterial(repairMat);
                    int playerHas = this.countMaterial(player, repairMat);
                    boolean canAfford = playerHas >= cost;
                    lore.add(Component.text((String)("Custo: " + cost + "x " + matName), (TextColor)(canAfford ? NamedTextColor.GREEN : NamedTextColor.RED)));
                    lore.add(Component.text((String)("Voce tem: " + playerHas + "x " + matName), (TextColor)NamedTextColor.GRAY));
                    lore.add(Component.empty());
                    lore.add(Component.text((String)"Clique para reparar!", (TextColor)NamedTextColor.YELLOW));
                } else {
                    lore.add(Component.text((String)"Item nao esta danificado", (TextColor)NamedTextColor.GRAY));
                }
            } else {
                lore.add(Component.text((String)"Coloque um item no slot", (TextColor)NamedTextColor.GRAY));
            }
            meta.lore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }

    private boolean isSlotEmpty(Inventory inv, int slot) {
        ItemStack item = inv.getItem(slot);
        return item == null || item.getType().isAir();
    }

    private void returnItem(Player player, ItemStack item) {
        if (item == null || item.getType().isAir()) {
            return;
        }
        HashMap overflow = player.getInventory().addItem(new ItemStack[]{item});
        for (ItemStack drop : overflow.values()) {
            player.getWorld().dropItemNaturally(player.getLocation(), drop);
        }
        this.syncInventoryLater(player);
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
            case Material.BLAST_FURNACE -> "Blast Furnace";
            case Material.WATER_CAULDRON -> "Caldeirao (agua)";
            case Material.IRON_BLOCK -> "Bloco de Ferro";
            case Material.LAVA -> "Lava";
            case Material.ANVIL, Material.CHIPPED_ANVIL, Material.DAMAGED_ANVIL -> "Bigorna";
            default -> mat.name();
        };
    }

    private String getStageBlockName(ForgeStage stage) {
        return switch (stage.ordinal()) {
            case 1 -> "Blast Furnace";
            case 2 -> "Bigorna";
            case 3 -> "Caldeirao";
            case 4 -> "Rebolo";
            default -> GUI_TITLE_RAW;
        };
    }

    static class ForgingSession {
        final UUID playerId;
        final Location tableLoc;
        final ForgeEnvironment environment;
        final double efficiency;
        final int breakpoint;
        ForgeStage currentStage = ForgeStage.IDLE;
        ItemStack inputItem;
        double qualityScore = 50.0;
        List<FailureType> failures = new ArrayList<FailureType>();
        BukkitTask stageTask;
        BossBar bossBar;
        double heatLevel = 0.0;
        long lastHeatClick = 0L;
        double cursorPosition = 0.0;
        boolean cursorForward = true;
        int shapingHits = 0;
        int shapingPerfects = 0;
        int shapingCombo = 0;
        int shapingRequired;
        double coolingValue = 1.0;
        int coolingTick = 0;
        int refiningClicks = 0;
        int refiningRequired;
        boolean refiningWindowOpen = false;
        int refiningTick = 0;
        int forgeCount = 0;
        int itemTier = 0;
        boolean isOilCrafting = false;

        ForgingSession(UUID playerId, Location tableLoc, ForgeEnvironment env, double efficiency, int breakpoint) {
            this.playerId = playerId;
            this.tableLoc = tableLoc;
            this.environment = env;
            this.efficiency = efficiency;
            this.breakpoint = breakpoint;
        }
    }

    static enum ForgeStage {
        IDLE,
        HEATING,
        SHAPING,
        COOLING,
        REFINING,
        COMPLETE;

    }

    record CachedScan(ForgeEnvironment env, long timestamp) {
    }

    record ForgeEnvironment(Map<Material, Integer> blockCounts, double totalBonus, int nearbyPlayers, double playerBonus, boolean mercadorNearby, boolean alquimistaNearby, boolean ferreiroNearby) {
    }

    private record NearbyClassInfo(int count, boolean mercador, boolean alquimista, boolean ferreiro) {
    }

    static enum FailureType {
        LIGHT,
        MEDIUM,
        SEVERE,
        CRITICAL;

    }

    static enum HitQuality {
        PERFECT,
        GOOD,
        BAD,
        MISS;

    }

    record ForgeResult(String qualityTag, NamedTextColor qualityColor, double melhoriaReal, double capMaximo, List<String> improvements) {
    }
}

