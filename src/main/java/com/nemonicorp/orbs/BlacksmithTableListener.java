package com.nemonicorp.orbs;

import io.lumine.mythic.lib.api.item.NBTItem;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
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
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitTask;

import com.google.gson.Gson;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Mesa do Ferreiro - Sistema de forja multi-bloco.
 * Hub: Anvil/Bigorna (GUI 45 slots para colocar item e iniciar)
 * Etapa 1 - Aquecimento: Blast Furnace (clique direito no bloco + BossBar)
 * Etapa 2 - Modelagem: Anvil (clique direito no bloco + BossBar)
 * Etapa 3 - Resfriamento: Cauldron (clique direito no bloco + BossBar)
 * Etapa 4 - Refino: Grindstone (clique direito no bloco + BossBar)
 * Resultado: Voltar a Bigorna para coletar.
 */
public class BlacksmithTableListener implements Listener {

    private final NemonicOrbPlugin plugin;
    private static final Gson GSON = new Gson();

    private final Map<UUID, ForgingSession> activeSessions = new ConcurrentHashMap<>();
    private final Map<UUID, Long> cooldowns = new ConcurrentHashMap<>();
    private final Map<String, CachedScan> scanCache = new ConcurrentHashMap<>();
    private final Map<UUID, BukkitTask> auraTasks = new ConcurrentHashMap<>();

    // NBT tags for forge tracking
    public static final String NBT_FORGE_COUNT = "NEMONICORB_FORGE_COUNT";

    // ═══════════════════════════════
    // Enums
    // ═══════════════════════════════

    enum ForgeStage {
        IDLE,           // Na GUI do hub, ainda nao comecou
        HEATING,        // Blast Furnace
        SHAPING,        // Anvil
        COOLING,        // Cauldron
        REFINING,       // Grindstone
        COMPLETE        // Voltar a mesa para coletar
    }

    enum FailureType { LIGHT, MEDIUM, SEVERE, CRITICAL }
    enum HitQuality { PERFECT, GOOD, BAD, MISS }

    // ═══════════════════════════════
    // Session (mutable)
    // ═══════════════════════════════

    static class ForgingSession {
        final UUID playerId;
        final Location tableLoc;
        final ForgeEnvironment environment;
        final double efficiency;
        final int breakpoint;

        ForgeStage currentStage = ForgeStage.IDLE;
        ItemStack inputItem;
        double qualityScore = 50.0;
        List<FailureType> failures = new ArrayList<>();

        BukkitTask stageTask;
        BossBar bossBar;

        // Stage 1 - Heating
        double heatLevel = 0;
        long lastHeatClick = 0;

        // Stage 2 - Shaping
        double cursorPosition = 0;
        boolean cursorForward = true;
        int shapingHits = 0;
        int shapingPerfects = 0;
        int shapingCombo = 0;
        int shapingRequired;

        // Stage 3 - Cooling
        double coolingValue = 1.0;
        int coolingTick = 0;

        // Stage 4 - Refining
        int refiningClicks = 0;
        int refiningRequired;
        boolean refiningWindowOpen = false;
        int refiningTick = 0;

        // Forge count tracking
        int forgeCount = 0;
        int itemTier = 0;

        // Oil of Polishing crafting mode
        boolean isOilCrafting = false;

        ForgingSession(UUID playerId, Location tableLoc, ForgeEnvironment env, double efficiency, int breakpoint) {
            this.playerId = playerId;
            this.tableLoc = tableLoc;
            this.environment = env;
            this.efficiency = efficiency;
            this.breakpoint = breakpoint;
        }
    }

    record ForgeEnvironment(Map<Material, Integer> blockCounts, double totalBonus,
                            int nearbyPlayers, double playerBonus,
                            boolean mercadorNearby, boolean alquimistaNearby, boolean ferreiroNearby) {}
    record CachedScan(ForgeEnvironment env, long timestamp) {}

    // ForgeResult for consistent chat/lore output
    record ForgeResult(String qualityTag, NamedTextColor qualityColor, double melhoriaReal,
                       double capMaximo, List<String> improvements) {}

    // ═══════════════════════════════
    // GUI Layout (45 slots = 5 rows) - Hub apenas
    // ═══════════════════════════════
    // Row 0: [g][g][g][EFF][g][INFO][g][g][g]
    // Row 1: [g][INPUT][>][ACTION][>][OUTPUT][g][REPAIR][g]
    // Row 2: [g][g][g][g][g][g][g][g][g]
    // Row 3: [S1][>][S2][>][S3][>][S4][g][CANCEL]
    // Row 4: [g][g][g][g][g][g][g][g][g]

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

    // Forge environment blocks: {bonus, maxBlocks}
    // Removed: FURNACE, NETHERITE_BLOCK, CAULDRON(empty), CHAIN, GRINDSTONE
    private static final Map<Material, double[]> FORGE_BLOCKS = Map.ofEntries(
            Map.entry(Material.BLAST_FURNACE, new double[]{0.06, 10}),
            Map.entry(Material.WATER_CAULDRON, new double[]{0.08, 8}),
            Map.entry(Material.IRON_BLOCK, new double[]{0.05, 10}),
            Map.entry(Material.LAVA, new double[]{0.08, 6}),
            Map.entry(Material.ANVIL, new double[]{0.06, 5}),
            Map.entry(Material.CHIPPED_ANVIL, new double[]{0.06, 5}),
            Map.entry(Material.DAMAGED_ANVIL, new double[]{0.06, 5})
    );

    // Indicator thresholds - only show block in Nether Star after reaching this count
    private static final Map<String, Integer> INDICATOR_THRESHOLDS = Map.of(
            "BLAST_FURNACE", 3,
            "WATER_CAULDRON", 2,
            "IRON_BLOCK", 3,
            "LAVA", 2,
            "ANVIL", 2
    );

    // Required blocks for each stage
    private static final Set<Material> HEATING_BLOCKS = Set.of(Material.BLAST_FURNACE);
    private static final Set<Material> SHAPING_BLOCKS = Set.of(Material.ANVIL, Material.CHIPPED_ANVIL, Material.DAMAGED_ANVIL);
    private static final Set<Material> COOLING_BLOCKS = Set.of(Material.WATER_CAULDRON, Material.CAULDRON);
    private static final Set<Material> REFINING_BLOCKS = Set.of(Material.GRINDSTONE);

    // Blocks that are scanned but don't give efficiency bonus (only presence check)
    private static final Set<Material> STAGE_ONLY_BLOCKS = Set.of(Material.GRINDSTONE, Material.CAULDRON);

    // Accepted MMOItem types for forging
    private static final Set<String> ACCEPTED_TYPES = Set.of(
            "SWORD", "DAGGER", "AXE", "BOW", "CROSSBOW", "STAFF",
            "WAND", "WHIP", "MUSKET", "LUTE", "SPEAR", "GREATSTAFF",
            "GREATSWORD", "HAMMER", "KATANA", "HALBERD", "GAUNTLET",
            "TRIDENT", "MACE", "HELMET", "CHESTPLATE", "LEGGINGS", "BOOTS", "ARMOR",
            "PICKAXE", "SHOVEL", "HOE", "FISHING_ROD", "SHEARS", "TOOL", "SHIELD"
    );

    public BlacksmithTableListener(NemonicOrbPlugin plugin) {
        this.plugin = plugin;
    }

    private boolean debug() {
        return plugin.getConfig().getBoolean("debug", true);
    }

    /** Returns true if the player has an active forging session (not IDLE/COMPLETE). */
    public boolean hasActiveSession(Player player) {
        ForgingSession session = activeSessions.get(player.getUniqueId());
        return session != null && session.currentStage != ForgeStage.IDLE && session.currentStage != ForgeStage.COMPLETE;
    }

    // ═══════════════════════════════
    // Class Detection (reflection)
    // ═══════════════════════════════

    public boolean isFerreiro(Player player) {
        try {
            String className = getPlayerClassName(player);
            String configClassName = plugin.getConfig().getString("blacksmith.class-name", "Ferreiro");
            boolean result = className != null && className.equalsIgnoreCase(configClassName);
            if (debug()) plugin.getLogger().info("[FORGE] isFerreiro: " + player.getName()
                    + " classe='" + className + "' config='" + configClassName + "' resultado=" + result);
            return result;
        } catch (Exception e) {
            plugin.getLogger().warning("[FORGE] Erro ao verificar classe: " + e.getMessage());
            return false;
        }
    }

    private String getPlayerClassName(Player player) {
        try {
            Class<?> pdClass = Class.forName("net.Indyuce.mmocore.api.player.PlayerData");
            Object data = getPlayerData(pdClass, player);
            if (data == null) return null;
            Object playerClass = null;
            for (String m : new String[]{"getProfess", "getPlayerClass", "getMMOClass"}) {
                try { playerClass = pdClass.getMethod(m).invoke(data); break; }
                catch (NoSuchMethodException ignored) {}
            }
            if (playerClass == null) return null;
            for (String m : new String[]{"getName", "getId", "getKey"}) {
                try {
                    Object r = playerClass.getClass().getMethod(m).invoke(playerClass);
                    if (r instanceof String s && !s.isEmpty()) return s;
                } catch (NoSuchMethodException ignored) {}
            }
            return null;
        } catch (ClassNotFoundException e) { return null; }
        catch (Exception e) {
            plugin.getLogger().warning("[FORGE] Erro ao obter classe: " + e.getMessage());
            return null;
        }
    }

    public int getPlayerLevel(Player player) {
        try {
            Class<?> pdClass = Class.forName("net.Indyuce.mmocore.api.player.PlayerData");
            Object data = getPlayerData(pdClass, player);
            if (data == null) return 0;
            return (int) pdClass.getMethod("getLevel").invoke(data);
        } catch (Exception e) { return 0; }
    }

    private Object getPlayerData(Class<?> pdClass, Player player) throws Exception {
        try { return pdClass.getMethod("get", org.bukkit.OfflinePlayer.class).invoke(null, player); }
        catch (NoSuchMethodException e1) {
            try { return pdClass.getMethod("get", Player.class).invoke(null, player); }
            catch (NoSuchMethodException e2) {
                return pdClass.getMethod("get", UUID.class).invoke(null, player.getUniqueId());
            }
        }
    }

    // ═══════════════════════════════
    // Environment Scanning
    // ═══════════════════════════════

    private ForgeEnvironment scanEnvironment(Location loc, Player player) {
        String cacheKey = loc.getWorld().getName() + "," + loc.getBlockX() + "," + loc.getBlockY() + "," + loc.getBlockZ();
        long cacheTTL = plugin.getConfig().getLong("blacksmith.scan-cache-seconds", 60) * 1000;
        CachedScan cached = scanCache.get(cacheKey);
        if (cached != null && (System.currentTimeMillis() - cached.timestamp()) < cacheTTL) {
            ForgeEnvironment old = cached.env();
            NearbyClassInfo classInfo = scanNearbyClasses(loc, player);
            double playerBonus = Math.min(classInfo.count * 0.05, 0.25);
            return new ForgeEnvironment(old.blockCounts(), old.totalBonus(), classInfo.count, playerBonus,
                    classInfo.mercador, classInfo.alquimista, classInfo.ferreiro);
        }

        int radius = plugin.getConfig().getInt("blacksmith.scan-radius", 20);
        Map<Material, Integer> blockCounts = new EnumMap<>(Material.class);
        World world = loc.getWorld();
        int cx = loc.getBlockX(), cy = loc.getBlockY(), cz = loc.getBlockZ();

        for (int x = cx - radius; x <= cx + radius; x++) {
            for (int y = Math.max(world.getMinHeight(), cy - radius); y <= Math.min(world.getMaxHeight() - 1, cy + radius); y++) {
                for (int z = cz - radius; z <= cz + radius; z++) {
                    double distSq = (x - cx) * (x - cx) + (y - cy) * (y - cy) + (z - cz) * (z - cz);
                    if (distSq > radius * radius) continue;
                    Material mat = world.getBlockAt(x, y, z).getType();
                    if (FORGE_BLOCKS.containsKey(mat) || STAGE_ONLY_BLOCKS.contains(mat)) {
                        blockCounts.merge(mat, 1, Integer::sum);
                    }
                }
            }
        }

        double totalBonus = 0;
        for (Map.Entry<Material, Integer> entry : blockCounts.entrySet()) {
            double[] cfg = FORGE_BLOCKS.get(entry.getKey());
            if (cfg == null) continue;
            int count = Math.min(entry.getValue(), (int) cfg[1]);
            totalBonus += count * cfg[0];
        }

        NearbyClassInfo classInfo = scanNearbyClasses(loc, player);
        double playerBonus = Math.min(classInfo.count * 0.05, 0.25);
        ForgeEnvironment env = new ForgeEnvironment(blockCounts, totalBonus, classInfo.count, playerBonus,
                classInfo.mercador, classInfo.alquimista, classInfo.ferreiro);
        scanCache.put(cacheKey, new CachedScan(env, System.currentTimeMillis()));

        if (debug()) plugin.getLogger().info("[FORGE-SCAN] bonus=" + String.format("%.0f%%", totalBonus * 100)
                + " jogadores=" + classInfo.count + " (+=" + String.format("%.0f%%", playerBonus * 100) + ")"
                + " mercador=" + classInfo.mercador + " alquimista=" + classInfo.alquimista + " ferreiro=" + classInfo.ferreiro);
        return env;
    }

    private record NearbyClassInfo(int count, boolean mercador, boolean alquimista, boolean ferreiro) {}

    private NearbyClassInfo scanNearbyClasses(Location loc, Player exclude) {
        int radius = plugin.getConfig().getInt("blacksmith.scan-radius", 20);
        int count = 0;
        boolean mercador = false, alquimista = false, ferreiro = false;
        String ferreiroClass = plugin.getConfig().getString("blacksmith.class-name", "Ferreiro");
        String alquimistaClass = plugin.getConfig().getString("transmutation.class-name", "Alquimista");
        String mercadorClass = "Mercador";

        for (Player p : loc.getWorld().getPlayers()) {
            if (p.equals(exclude)) continue;
            if (p.getLocation().distanceSquared(loc) > radius * radius) continue;
            count++;
            String cls = getPlayerClassName(p);
            if (cls == null) continue;
            if (ferreiroClass.equalsIgnoreCase(cls)) ferreiro = true;
            if (alquimistaClass.equalsIgnoreCase(cls)) alquimista = true;
            if (mercadorClass.equalsIgnoreCase(cls)) mercador = true;
        }
        return new NearbyClassInfo(count, mercador, alquimista, ferreiro);
    }

    private double calculateEfficiency(ForgeEnvironment env) {
        return 1.0 + env.totalBonus() + env.playerBonus();
    }

    private int getBreakpoint(double efficiency) {
        double pct = efficiency * 100;
        if (pct >= 290) return 5;
        if (pct >= 250) return 4;
        if (pct >= 210) return 3;
        if (pct >= 170) return 2;
        if (pct >= 130) return 1;
        return 0;
    }

    /** Checks if required blocks for all stages are nearby. */
    private Map<ForgeStage, Boolean> checkRequiredBlocks(ForgeEnvironment env) {
        Map<ForgeStage, Boolean> result = new EnumMap<>(ForgeStage.class);
        result.put(ForgeStage.HEATING, HEATING_BLOCKS.stream().anyMatch(m -> env.blockCounts().getOrDefault(m, 0) > 0));
        result.put(ForgeStage.SHAPING, SHAPING_BLOCKS.stream().anyMatch(m -> env.blockCounts().getOrDefault(m, 0) > 0));
        result.put(ForgeStage.COOLING, COOLING_BLOCKS.stream().anyMatch(m -> env.blockCounts().getOrDefault(m, 0) > 0));
        result.put(ForgeStage.REFINING, REFINING_BLOCKS.stream().anyMatch(m -> env.blockCounts().getOrDefault(m, 0) > 0));
        return result;
    }

    // ═══════════════════════════════
    // Input Validation
    // ═══════════════════════════════

    private boolean isValidForgeInput(ItemStack item) {
        if (item == null || item.getType().isAir()) return false;
        NBTItem nbt = NBTItem.get(item);
        if (nbt.hasTag(OrbListener.NBT_TIER)) return true;
        if (nbt.hasTag("MMOITEMS_ITEM_TYPE")) {
            String type = nbt.getString("MMOITEMS_ITEM_TYPE");
            return type != null && ACCEPTED_TYPES.contains(type);
        }
        return false;
    }

    // ═══════════════════════════════
    // Open Hub GUI (Anvil/Bigorna)
    // ═══════════════════════════════

    public void openMainGUI(Player player, Location tableLoc) {
        int minLevel = plugin.getConfig().getInt("blacksmith.min-level", 5);
        int playerLevel = getPlayerLevel(player);

        if (playerLevel < minLevel) {
            player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                    plugin.getConfig().getString("blacksmith.messages.level-too-low",
                            "&c[Forja] Voce precisa de nivel %level% na classe Ferreiro!")
                            .replace("%level%", String.valueOf(minLevel))));
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
            return;
        }

        // If player already has an active session in COMPLETE stage, reopen to collect
        ForgingSession existing = activeSessions.get(player.getUniqueId());
        if (existing != null && existing.currentStage == ForgeStage.COMPLETE) {
            openCollectGUI(player, existing);
            return;
        }

        // If player has an active session in a stage, remind them
        if (existing != null && existing.currentStage != ForgeStage.IDLE) {
            // Fallback de seguranca: se o refino ja bateu o requisito, finaliza a forja
            // para nao travar no estado "ainda precisa passar pelo rebolo".
            if (existing.currentStage == ForgeStage.REFINING
                    && existing.refiningClicks >= existing.refiningRequired) {
                if (debug()) plugin.getLogger().warning("[FORGE] Fallback: finalizando sessao presa no refino para " + player.getName()
                        + " (" + existing.refiningClicks + "/" + existing.refiningRequired + ")");
                completeForging(player, existing);
                openCollectGUI(player, existing);
                return;
            }
            String stageName = getStageBlockName(existing.currentStage);
            player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                    "&e[Forja] Voce tem uma forja em andamento! Va ate: &6" + stageName));
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
            return;
        }

        // Cooldown check
        long cooldownMs = plugin.getConfig().getLong("blacksmith.cooldown-seconds", 30) * 1000;
        Long lastUse = cooldowns.get(player.getUniqueId());
        long now = System.currentTimeMillis();
        if (lastUse != null && (now - lastUse) < cooldownMs) {
            long remaining = (cooldownMs - (now - lastUse)) / 1000;
            player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                    plugin.getConfig().getString("blacksmith.messages.cooldown",
                            "&c[Forja] Aguarde %seconds%s antes de forjar novamente!")
                            .replace("%seconds%", String.valueOf(remaining))));
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
            return;
        }

        ForgeEnvironment env = scanEnvironment(tableLoc, player);
        double efficiency = calculateEfficiency(env);
        int breakpoint = getBreakpoint(efficiency);
        Map<ForgeStage, Boolean> blocksAvailable = checkRequiredBlocks(env);

        Inventory gui = Bukkit.createInventory(null, GUI_SIZE,
                Component.text(GUI_TITLE_RAW, NamedTextColor.GOLD, TextDecoration.BOLD));

        // Fill with black glass
        ItemStack glass = createGlassPane(Material.BLACK_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < GUI_SIZE; i++) gui.setItem(i, glass);

        // Row 0: Efficiency + Info
        gui.setItem(SLOT_EFFICIENCY, createEfficiencyItem(env, efficiency, breakpoint, playerLevel));
        gui.setItem(SLOT_INFO, createInfoItem());

        // Row 1: Input > Start > Output
        gui.setItem(SLOT_INPUT, null); // empty for player to place item
        gui.setItem(SLOT_ARROW_1, createArrowPane());
        gui.setItem(SLOT_ACTION, createStartButton());
        gui.setItem(SLOT_ARROW_2, createArrowPane());
        gui.setItem(SLOT_OUTPUT, createLockedOutput());
        gui.setItem(SLOT_REPAIR, createRepairButton(player, null));

        // Row 3: Stage indicators (show if required block is present)
        gui.setItem(SLOT_S1, createStageBlockIndicator("Aquecimento", Material.BLAST_FURNACE, blocksAvailable.get(ForgeStage.HEATING)));
        gui.setItem(28, createArrowPane());
        gui.setItem(SLOT_S2, createStageBlockIndicator("Modelagem", Material.ANVIL, blocksAvailable.get(ForgeStage.SHAPING)));
        gui.setItem(30, createArrowPane());
        gui.setItem(SLOT_S3, createStageBlockIndicator("Resfriamento", Material.WATER_BUCKET, blocksAvailable.get(ForgeStage.COOLING)));
        gui.setItem(32, createArrowPane());
        gui.setItem(SLOT_S4, createStageBlockIndicator("Refino", Material.GRINDSTONE, blocksAvailable.get(ForgeStage.REFINING)));
        gui.setItem(34, glass);
        gui.setItem(SLOT_CANCEL, createCancelButton());

        ForgingSession session = new ForgingSession(player.getUniqueId(), tableLoc, env, efficiency, breakpoint);
        activeSessions.put(player.getUniqueId(), session);

        player.openInventory(gui);
        player.playSound(tableLoc, Sound.BLOCK_ANVIL_PLACE, 0.6f, 0.8f);

        if (debug()) plugin.getLogger().info("[FORGE] GUI aberta para " + player.getName()
                + " nivel=" + playerLevel + " eficiencia=" + String.format("%.0f%%", efficiency * 100)
                + " breakpoint=" + breakpoint + " blocks=" + blocksAvailable);
    }

    private void openCollectGUI(Player player, ForgingSession session) {
        Inventory gui = Bukkit.createInventory(null, GUI_SIZE,
                Component.text(GUI_TITLE_RAW, NamedTextColor.GOLD, TextDecoration.BOLD));

        ItemStack glass = createGlassPane(Material.BLACK_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < GUI_SIZE; i++) gui.setItem(i, glass);

        gui.setItem(SLOT_EFFICIENCY, createEfficiencyItem(session.environment, session.efficiency, session.breakpoint, getPlayerLevel(player)));
        gui.setItem(SLOT_OUTPUT, session.inputItem); // the forged item

        // Green indicators for all stages
        gui.setItem(SLOT_S1, createGlassPane(Material.LIME_STAINED_GLASS_PANE, "Aquecimento - Concluido"));
        gui.setItem(SLOT_S2, createGlassPane(Material.LIME_STAINED_GLASS_PANE, "Modelagem - Concluido"));
        gui.setItem(SLOT_S3, createGlassPane(Material.LIME_STAINED_GLASS_PANE, "Resfriamento - Concluido"));
        gui.setItem(SLOT_S4, createGlassPane(Material.LIME_STAINED_GLASS_PANE, "Refino - Concluido"));

        player.openInventory(gui);
        player.playSound(session.tableLoc, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.0f);
    }

    // ═══════════════════════════════
    // Hub GUI Event Handlers
    // ═══════════════════════════════

    @EventHandler(priority = EventPriority.HIGH)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        String title = getInventoryTitle(event.getView());
        if (!GUI_TITLE_RAW.equals(title)) return;

        ForgingSession session = activeSessions.get(player.getUniqueId());
        if (session == null) {
            event.setCancelled(true);
            return;
        }

        int slot = event.getRawSlot();
        Inventory topInv = event.getView().getTopInventory();

        // Block number-key swaps targeting GUI slots (anti-dupe)
        if (event.getHotbarButton() >= 0 && slot < GUI_SIZE && slot != SLOT_INPUT) {
            event.setCancelled(true);
            return;
        }
        if (event.getHotbarButton() >= 0 && slot == SLOT_INPUT && session.currentStage != ForgeStage.IDLE) {
            event.setCancelled(true);
            return;
        }

        // Player inventory clicks - allow shift-click to input in IDLE
        if (slot >= GUI_SIZE) {
            if (event.isShiftClick() && session.currentStage == ForgeStage.IDLE) {
                event.setCancelled(true);
                ItemStack clicked = event.getCurrentItem();
                if (clicked != null && !clicked.getType().isAir() && isSlotEmpty(topInv, SLOT_INPUT)) {
                    topInv.setItem(SLOT_INPUT, clicked.clone());
                    event.setCurrentItem(null);
                    // Update repair button
                    Bukkit.getScheduler().runTask(plugin, () -> {
                        if (player.isOnline() && activeSessions.containsKey(player.getUniqueId())) {
                            topInv.setItem(SLOT_REPAIR, createRepairButton(player, topInv.getItem(SLOT_INPUT)));
                        }
                    });
                }
            } else if (event.isShiftClick()) {
                // Block shift-click from player inv when not IDLE (anti-dupe)
                event.setCancelled(true);
            }
            return;
        }

        // Input slot - allow only in IDLE
        if (slot == SLOT_INPUT && session.currentStage == ForgeStage.IDLE) return;

        // Output slot - allow pickup only when COMPLETE (manual click only, no shift-click)
        if (slot == SLOT_OUTPUT && session.currentStage == ForgeStage.COMPLETE) {
            if (event.isShiftClick()) {
                // Handle shift-click manually to prevent duplication
                event.setCancelled(true);
                ItemStack outputItem = topInv.getItem(SLOT_OUTPUT);
                if (outputItem != null && !outputItem.getType().isAir()
                        && outputItem.getType() != Material.BARRIER) {
                    topInv.setItem(SLOT_OUTPUT, null);
                    Map<Integer, ItemStack> overflow = player.getInventory().addItem(outputItem);
                    for (ItemStack drop : overflow.values()) {
                        player.getWorld().dropItemNaturally(player.getLocation(), drop);
                    }
                    session.inputItem = null; // Clear so close handler won't return it again
                    syncInventoryLater(player);
                }
            } else {
                ItemStack outputItem = topInv.getItem(SLOT_OUTPUT);
                if (outputItem != null && !outputItem.getType().isAir()
                        && outputItem.getType() != Material.BARRIER) {
                    // Allow normal pickup — clear session reference so close handler won't dupe
                    session.inputItem = null;
                    syncInventoryLater(player);
                    return;
                }
            }
            event.setCancelled(true);
            return;
        }

        // Block everything else
        event.setCancelled(true);

        // Handle button clicks
        if (slot == SLOT_ACTION && session.currentStage == ForgeStage.IDLE) {
            startForging(player, topInv, session);
        } else if (slot == SLOT_REPAIR && session.currentStage == ForgeStage.IDLE) {
            handleRepair(player, topInv, session);
            // Update repair button after repair
            topInv.setItem(SLOT_REPAIR, createRepairButton(player, topInv.getItem(SLOT_INPUT)));
        } else if (slot == SLOT_CANCEL) {
            cancelForging(player, session, true);
        }

        // Update repair button when input slot changes
        if (slot == SLOT_INPUT && session.currentStage == ForgeStage.IDLE) {
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (player.isOnline() && activeSessions.containsKey(player.getUniqueId())) {
                    topInv.setItem(SLOT_REPAIR, createRepairButton(player, topInv.getItem(SLOT_INPUT)));
                }
            }, 1L);
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onInventoryDrag(InventoryDragEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        String title = getInventoryTitle(event.getView());
        if (!GUI_TITLE_RAW.equals(title)) return;
        ForgingSession session = activeSessions.get(player.getUniqueId());
        if (session == null) {
            event.setCancelled(true);
            return;
        }

        for (int slot : event.getRawSlots()) {
            if (slot < GUI_SIZE && slot != SLOT_INPUT) {
                event.setCancelled(true);
                return;
            }
            if (slot == SLOT_INPUT && session.currentStage != ForgeStage.IDLE) {
                event.setCancelled(true);
                return;
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player player)) return;
        String title = getInventoryTitle(event.getView());
        if (!GUI_TITLE_RAW.equals(title)) return;

        ForgingSession session = activeSessions.get(player.getUniqueId());
        if (session == null) return;

        Inventory inv = event.getView().getTopInventory();

        if (session.currentStage == ForgeStage.IDLE) {
            // Not forging yet - return input item and remove session
            activeSessions.remove(player.getUniqueId());
            ItemStack input = inv.getItem(SLOT_INPUT);
            inv.setItem(SLOT_INPUT, null); // Clear slot first to prevent dupe
            returnItem(player, input);
        } else if (session.currentStage == ForgeStage.COMPLETE) {
            // Collect phase - if output was not yet taken, return it
            ItemStack output = inv.getItem(SLOT_OUTPUT);
            inv.setItem(SLOT_OUTPUT, null); // Clear slot first to prevent dupe
            if (output != null && !output.getType().isAir() && output.getType() != Material.BARRIER) {
                returnItem(player, output);
            }
            activeSessions.remove(player.getUniqueId());
        }
        // If in a stage (HEATING, SHAPING, etc.), keep session alive - player went to a block
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        stopForgeAura(player);
        ForgingSession session = activeSessions.remove(player.getUniqueId());
        if (session != null) {
            removeBossBar(session);
            if (session.stageTask != null) session.stageTask.cancel();
            if (session.inputItem != null) {
                returnItem(player, session.inputItem);
            }
        }
    }

    // ═══════════════════════════════
    // Block Interaction (stages)
    // ═══════════════════════════════

    @EventHandler(priority = EventPriority.LOWEST)
    public void onBlockInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        Player player = event.getPlayer();
        ForgingSession session = activeSessions.get(player.getUniqueId());
        if (session == null) return;
        if (session.currentStage == ForgeStage.IDLE || session.currentStage == ForgeStage.COMPLETE) return;

        Block block = event.getClickedBlock();
        if (block == null) return;
        Material mat = block.getType();

        // Always cancel interaction with forge stage blocks to prevent vanilla GUIs
        boolean isForgeBlock = HEATING_BLOCKS.contains(mat) || SHAPING_BLOCKS.contains(mat)
                || COOLING_BLOCKS.contains(mat) || REFINING_BLOCKS.contains(mat);
        if (isForgeBlock) {
            event.setCancelled(true);
        }

        int radius = plugin.getConfig().getInt("blacksmith.scan-radius", 20);

        // Check if the block is within range of the smithing table
        if (block.getLocation().distanceSquared(session.tableLoc) > radius * radius) return;

        switch (session.currentStage) {
            case HEATING -> {
                if (!HEATING_BLOCKS.contains(mat)) return;
                handleHeatingAction(player, session);
            }
            case SHAPING -> {
                if (!SHAPING_BLOCKS.contains(mat)) return;
                handleShapingAction(player, session);
            }
            case COOLING -> {
                if (!COOLING_BLOCKS.contains(mat)) return;
                handleCoolingAction(player, session);
            }
            case REFINING -> {
                if (!REFINING_BLOCKS.contains(mat)) return;
                handleRefiningAction(player, session);
            }
            default -> {}
        }
    }

    // ═══════════════════════════════
    // Forging Flow
    // ═══════════════════════════════

    /** Checks if input is 9 gold nuggets for Oil of Polishing crafting. */
    private boolean isOilCraftInput(ItemStack item) {
        return item != null && item.getType() == Material.GOLD_NUGGET && item.getAmount() >= 9;
    }

    private void startForging(Player player, Inventory gui, ForgingSession session) {
        ItemStack input = gui.getItem(SLOT_INPUT);

        // Oil of Polishing: 9 gold nuggets
        if (isOilCraftInput(input)) {
            startOilCrafting(player, gui, session, input);
            return;
        }

        if (!isValidForgeInput(input)) {
            player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                    plugin.getConfig().getString("blacksmith.messages.not-forgeable",
                            "&c[Forja] Este item nao pode ser forjado!")));
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
            return;
        }

        // Read item tier for fuel cost and level gate
        NBTItem inputNbt = NBTItem.get(input);
        int tier = inputNbt.hasTag(OrbListener.NBT_TIER) ? inputNbt.getInteger(OrbListener.NBT_TIER) : 0;
        session.itemTier = tier;

        // Read forge count from item
        int currentForgeCount = inputNbt.hasTag(NBT_FORGE_COUNT) ? inputNbt.getInteger(NBT_FORGE_COUNT) : 0;
        session.forgeCount = currentForgeCount + 1;

        // Unique items (tier 3) require level 35+
        if (tier == 3) {
            int uniqueMinLevel = plugin.getConfig().getInt("blacksmith.unique-min-level", 35);
            int playerLevel = getPlayerLevel(player);
            if (playerLevel < uniqueMinLevel) {
                player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                        "&c[Forja] Itens Unicos requerem nivel " + uniqueMinLevel + " de Ferreiro!"));
                player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
                return;
            }
        }

        // Check all required blocks are present
        Map<ForgeStage, Boolean> blocks = checkRequiredBlocks(session.environment);
        if (!blocks.getOrDefault(ForgeStage.HEATING, false)) {
            player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&c[Forja] Nenhuma Blast Furnace encontrada nas proximidades!"));
            return;
        }
        if (!blocks.getOrDefault(ForgeStage.SHAPING, false)) {
            player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&c[Forja] Nenhuma Bigorna encontrada nas proximidades!"));
            return;
        }
        if (!blocks.getOrDefault(ForgeStage.COOLING, false)) {
            player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&c[Forja] Nenhum Caldeirao encontrado nas proximidades!"));
            return;
        }
        if (!blocks.getOrDefault(ForgeStage.REFINING, false)) {
            player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&c[Forja] Nenhum Rebolo encontrado nas proximidades!"));
            return;
        }

        // Fuel cost by tier: 10 (Comum) / 12 (Magico) / 14 (Raro/Unico)
        int fuelAmount = plugin.getConfig().getInt("blacksmith.fuel.amount-by-tier." + tier,
                plugin.getConfig().getInt("blacksmith.fuel.amount", 10));
        if (!consumeFuel(player, fuelAmount)) {
            player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                    plugin.getConfig().getString("blacksmith.messages.no-fuel",
                            "&c[Forja] Voce precisa de %amount%x Carvao para forjar!")
                            .replace("%amount%", String.valueOf(fuelAmount))));
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
            return;
        }

        session.inputItem = input.clone();
        gui.setItem(SLOT_INPUT, null);
        session.shapingRequired = plugin.getConfig().getInt("blacksmith.shaping.hits-required", 4);
        session.refiningRequired = plugin.getConfig().getInt("blacksmith.refining.clicks-required", 2);

        // Scale with efficiency: higher efficiency = more actions needed
        double effScale = 1.0 + (session.efficiency - 1.0) * 0.5;
        session.shapingRequired = (int) Math.ceil(session.shapingRequired * effScale);
        session.refiningRequired = (int) Math.ceil(session.refiningRequired * effScale);

        player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                "&6[Forja] A forja comeca! Va ate a &eBLAST FURNACE &6para aquecer o metal!"));
        player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_USE, 0.8f, 0.8f);

        session.currentStage = ForgeStage.HEATING;
        player.closeInventory();

        startHeatingTicker(player, session);
        startForgeAura(player, session);

        if (debug()) plugin.getLogger().info("[FORGE] " + player.getName() + " iniciou forja. Tier=" + tier
                + " forgeCount=" + session.forgeCount + " fuel=" + fuelAmount
                + " eficiencia=" + String.format("%.0f%%", session.efficiency * 100)
                + " shapeHits=" + session.shapingRequired + " refineClicks=" + session.refiningRequired);
    }

    private void startOilCrafting(Player player, Inventory gui, ForgingSession session, ItemStack input) {
        int nuggetsReq = plugin.getConfig().getInt("blacksmith.oil-crafting.gold-nuggets-required", 9);
        if (input.getAmount() < nuggetsReq) {
            player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                    "&c[Forja] Voce precisa de " + nuggetsReq + "x Pepitas de Ouro!"));
            return;
        }

        // Check all required blocks
        Map<ForgeStage, Boolean> blocks = checkRequiredBlocks(session.environment);
        for (Map.Entry<ForgeStage, Boolean> e : blocks.entrySet()) {
            if (!e.getValue()) {
                player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                        "&c[Forja] Blocos necessarios nao encontrados!"));
                return;
            }
        }

        // Fuel for oil crafting
        int fuelCost = plugin.getConfig().getInt("blacksmith.oil-crafting.fuel-cost", 10);
        if (!consumeFuel(player, fuelCost)) {
            player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                    "&c[Forja] Voce precisa de " + fuelCost + "x Carvao para fabricar o oleo!"));
            return;
        }

        // Consume nuggets
        input.setAmount(input.getAmount() - nuggetsReq);
        if (input.getAmount() <= 0) gui.setItem(SLOT_INPUT, null);

        session.isOilCrafting = true;
        session.inputItem = new ItemStack(Material.GOLD_NUGGET, nuggetsReq); // track
        session.shapingRequired = plugin.getConfig().getInt("blacksmith.shaping.hits-required", 4);
        session.refiningRequired = plugin.getConfig().getInt("blacksmith.refining.clicks-required", 2);

        player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                "&6[Forja] Fabricacao de Oleo de Polimento! Va ate a &eBLAST FURNACE&6!"));
        player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_USE, 0.8f, 0.8f);

        session.currentStage = ForgeStage.HEATING;
        player.closeInventory();
        startHeatingTicker(player, session);
        startForgeAura(player, session);
    }

    private void advanceStage(Player player, ForgingSession session) {
        if (session.stageTask != null) { session.stageTask.cancel(); session.stageTask = null; }
        removeBossBar(session);

        String stageMsg = switch (session.currentStage) {
            case HEATING -> {
                session.currentStage = ForgeStage.SHAPING;
                startShapingTicker(player, session);
                yield "&6[Forja] Metal aquecido! Agora va ate a &eBIGORNA &6para modelar!";
            }
            case SHAPING -> {
                session.currentStage = ForgeStage.COOLING;
                startCoolingTicker(player, session);
                yield "&6[Forja] Modelado! Agora va ate o &eCALDEIRAO &6para resfriar!";
            }
            case COOLING -> {
                session.currentStage = ForgeStage.REFINING;
                startRefiningTicker(player, session);
                yield "&6[Forja] Resfriado! Agora va ate o &eREBOLO &6para refinar!";
            }
            case REFINING -> {
                completeForging(player, session);
                yield null;
            }
            default -> null;
        };

        if (stageMsg != null) {
            player.sendMessage(ChatColor.translateAlternateColorCodes('&', stageMsg));
            player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.8f, 1.2f);
        }
    }

    // ═══════════════════════════════
    // Stage 1 - Heating (Blast Furnace)
    // ═══════════════════════════════

    private void startHeatingTicker(Player player, ForgingSession session) {
        session.heatLevel = 0;

        session.bossBar = Bukkit.createBossBar(
                "Aquecimento [0%] - Clique na Blast Furnace!",
                BarColor.RED, BarStyle.SEGMENTED_10);
        session.bossBar.setProgress(0);
        session.bossBar.addPlayer(player);

        int tickInterval = plugin.getConfig().getInt("blacksmith.heating.tick-interval", 2);
        double decayPerTick = plugin.getConfig().getDouble("blacksmith.heating.decay-per-tick", 0.5);

        session.stageTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (!player.isOnline()) { cancelForging(player, session, false); return; }

            // Decay
            session.heatLevel = Math.max(0, session.heatLevel - decayPerTick);

            double progress = Math.max(0, Math.min(1.0, session.heatLevel / 100.0));
            session.bossBar.setProgress(progress);

            BarColor color;
            if (session.heatLevel < 40) color = BarColor.RED;
            else if (session.heatLevel < 60) color = BarColor.YELLOW;
            else if (session.heatLevel <= 85) color = BarColor.GREEN;
            else color = BarColor.RED;
            session.bossBar.setColor(color);
            session.bossBar.setTitle("Aquecimento [" + (int) session.heatLevel + "%] - Clique na Blast Furnace!");

            // Particles at table
            Location tl = session.tableLoc.clone().add(0.5, 1.2, 0.5);
            if (session.heatLevel > 60) {
                tl.getWorld().spawnParticle(Particle.FLAME, tl, 3, 0.2, 0.1, 0.2, 0.01);
            } else if (session.heatLevel > 30) {
                tl.getWorld().spawnParticle(Particle.SMOKE, tl, 2, 0.2, 0.1, 0.2, 0.01);
            }
        }, tickInterval, tickInterval);
    }

    private void handleHeatingAction(Player player, ForgingSession session) {
        long now = System.currentTimeMillis();
        if (now - session.lastHeatClick < 100) return; // anti-spam
        session.lastHeatClick = now;

        double heatPerClick = plugin.getConfig().getDouble("blacksmith.heating.heat-per-click", 8.0);

        if (player.isSneaking()) {
            // Sneak + click = confirm temperature
            evaluateHeating(player, session);
            return;
        }

        session.heatLevel = Math.min(100, session.heatLevel + heatPerClick);
        player.playSound(player.getLocation(), Sound.BLOCK_FURNACE_FIRE_CRACKLE, 0.5f, 1.0f + (float)(session.heatLevel / 200.0));
    }

    private void evaluateHeating(Player player, ForgingSession session) {
        double center = plugin.getConfig().getDouble("blacksmith.heating.ideal-center", 70.0);
        double zoneBase = plugin.getConfig().getDouble("blacksmith.heating.ideal-zone-base", 20.0);
        double zoneMin = plugin.getConfig().getDouble("blacksmith.heating.ideal-zone-min", 8.0);

        double effFactor = Math.min((session.efficiency - 1.0) / 1.92, 1.0);
        double zoneWidth = zoneBase - (zoneBase - zoneMin) * effFactor;
        double lower = center - zoneWidth / 2;
        double upper = center + zoneWidth / 2;

        double heat = session.heatLevel;

        if (heat >= lower && heat <= upper) {
            double centerDist = Math.abs(heat - center) / (zoneWidth / 2);
            if (centerDist < 0.3) {
                session.qualityScore += 15;
                player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_USE, 0.8f, 1.5f);
                player.sendActionBar(Component.text("Aquecimento PERFEITO!", NamedTextColor.GREEN));
            } else {
                session.qualityScore += 8;
                player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_USE, 0.6f, 1.2f);
                player.sendActionBar(Component.text("Aquecimento Bom", NamedTextColor.YELLOW));
            }
        } else {
            double distance = heat < lower ? lower - heat : heat - upper;
            double penalty = Math.min(distance * 0.5, 20);
            session.qualityScore -= penalty;
            if (distance > 20) session.failures.add(FailureType.LIGHT);
            player.playSound(player.getLocation(), Sound.ENTITY_ITEM_BREAK, 0.8f, 0.5f);
            player.sendActionBar(Component.text("Aquecimento RUIM!", NamedTextColor.RED));
        }

        session.qualityScore = Math.max(0, Math.min(100, session.qualityScore));

        if (debug()) plugin.getLogger().info("[FORGE] Heating: heat=" + (int) heat
                + " zone=[" + (int) lower + "-" + (int) upper + "] quality=" + String.format("%.1f", session.qualityScore));

        advanceStage(player, session);
    }

    // ═══════════════════════════════
    // Stage 2 - Shaping (Anvil)
    // ═══════════════════════════════

    private void startShapingTicker(Player player, ForgingSession session) {
        session.cursorPosition = 0;
        session.cursorForward = true;
        session.shapingHits = 0;
        session.shapingPerfects = 0;
        session.shapingCombo = 0;

        session.bossBar = Bukkit.createBossBar(
                "Modelagem - Clique na Bigorna no momento certo! [0/" + session.shapingRequired + "]",
                BarColor.BLUE, BarStyle.SEGMENTED_20);
        session.bossBar.setProgress(0);
        session.bossBar.addPlayer(player);

        double speedBase = plugin.getConfig().getDouble("blacksmith.shaping.cursor-speed-base", 0.04);
        double speedMax = plugin.getConfig().getDouble("blacksmith.shaping.cursor-speed-max", 0.10);
        double effFactor = Math.min((session.efficiency - 1.0) / 1.92, 1.0);
        double speed = speedBase + (speedMax - speedBase) * effFactor;

        session.stageTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (!player.isOnline()) { cancelForging(player, session, false); return; }

            if (session.cursorForward) {
                session.cursorPosition += speed;
                if (session.cursorPosition >= 1.0) { session.cursorPosition = 1.0; session.cursorForward = false; }
            } else {
                session.cursorPosition -= speed;
                if (session.cursorPosition <= 0.0) { session.cursorPosition = 0.0; session.cursorForward = true; }
            }

            session.bossBar.setProgress(Math.max(0, Math.min(1.0, session.cursorPosition)));

            double greenBase = plugin.getConfig().getDouble("blacksmith.shaping.green-zone-base", 0.30);
            double greenMin = plugin.getConfig().getDouble("blacksmith.shaping.green-zone-min", 0.12);
            double greenZone = greenBase - (greenBase - greenMin) * effFactor;
            double lower = 0.5 - greenZone / 2;
            double upper = 0.5 + greenZone / 2;

            BarColor c;
            if (session.cursorPosition >= lower && session.cursorPosition <= upper) c = BarColor.GREEN;
            else if (Math.abs(session.cursorPosition - 0.5) < greenZone) c = BarColor.YELLOW;
            else c = BarColor.RED;
            session.bossBar.setColor(c);

            session.bossBar.setTitle("Modelagem - Clique na Bigorna! ["
                    + session.shapingHits + "/" + session.shapingRequired + "]");
        }, 1, 1);
    }

    private void handleShapingAction(Player player, ForgingSession session) {
        double greenBase = plugin.getConfig().getDouble("blacksmith.shaping.green-zone-base", 0.30);
        double greenMin = plugin.getConfig().getDouble("blacksmith.shaping.green-zone-min", 0.12);
        double effFactor = Math.min((session.efficiency - 1.0) / 1.92, 1.0);
        double greenZone = greenBase - (greenBase - greenMin) * effFactor;
        double lower = 0.5 - greenZone / 2;
        double upper = 0.5 + greenZone / 2;

        double pos = session.cursorPosition;
        HitQuality quality;

        if (pos >= lower && pos <= upper) {
            double centerDist = Math.abs(pos - 0.5) / (greenZone / 2);
            if (centerDist < 0.25) {
                quality = HitQuality.PERFECT;
                session.qualityScore += 10;
                session.shapingPerfects++;
                session.shapingCombo++;
                player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_USE, 1.0f, 1.5f);
                player.getLocation().getWorld().spawnParticle(Particle.CRIT, player.getLocation().add(0, 1.5, 0), 10, 0.3, 0.2, 0.3, 0.1);
            } else {
                quality = HitQuality.GOOD;
                session.qualityScore += 5;
                session.shapingCombo = 0;
                player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_USE, 0.8f, 1.2f);
            }
        } else {
            double dist = pos < lower ? lower - pos : pos - upper;
            if (dist < 0.15) {
                quality = HitQuality.BAD;
                session.qualityScore -= 5;
            } else {
                quality = HitQuality.MISS;
                session.qualityScore -= 10;
                session.failures.add(FailureType.LIGHT);
            }
            session.shapingCombo = 0;
            player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_LAND, 0.6f, 0.5f);
        }

        // Combo bonus
        int comboThreshold = plugin.getConfig().getInt("blacksmith.shaping.combo-threshold", 3);
        double comboBonus = plugin.getConfig().getDouble("blacksmith.shaping.combo-bonus", 5.0);
        if (session.shapingCombo >= comboThreshold) {
            session.qualityScore += comboBonus;
            session.shapingCombo = 0;
            player.sendActionBar(Component.text("COMBO! +" + (int) comboBonus + " qualidade!",
                    NamedTextColor.GOLD, TextDecoration.BOLD));
            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.5f, 1.5f);
        } else {
            NamedTextColor c = quality == HitQuality.PERFECT ? NamedTextColor.GREEN
                    : quality == HitQuality.GOOD ? NamedTextColor.YELLOW : NamedTextColor.RED;
            player.sendActionBar(Component.text(quality.name() + " | Combo: " + session.shapingCombo
                    + " | Qualidade: " + (int) session.qualityScore + "%", c));
        }

        session.qualityScore = Math.max(0, Math.min(100, session.qualityScore));
        session.shapingHits++;

        if (session.shapingHits >= session.shapingRequired) {
            advanceStage(player, session);
        }
    }

    // ═══════════════════════════════
    // Stage 3 - Cooling (Cauldron)
    // ═══════════════════════════════

    private void startCoolingTicker(Player player, ForgingSession session) {
        session.coolingValue = 1.0;
        session.coolingTick = 0;

        session.bossBar = Bukkit.createBossBar(
                "Resfriamento [100%] - Clique no Caldeirao para parar!",
                BarColor.BLUE, BarStyle.SEGMENTED_20);
        session.bossBar.setProgress(1.0);
        session.bossBar.addPlayer(player);

        double decBase = plugin.getConfig().getDouble("blacksmith.cooling.decrement-base", 0.008);
        double ampBase = plugin.getConfig().getDouble("blacksmith.cooling.fluctuation-amplitude-base", 0.0);
        double ampMax = plugin.getConfig().getDouble("blacksmith.cooling.fluctuation-amplitude-max", 0.006);
        double effFactor = Math.min((session.efficiency - 1.0) / 1.92, 1.0);
        double amplitude = ampBase + (ampMax - ampBase) * effFactor;

        int tickInterval = plugin.getConfig().getInt("blacksmith.cooling.tick-interval", 2);

        session.stageTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (!player.isOnline()) { cancelForging(player, session, false); return; }

            session.coolingTick++;
            double fluctuation = amplitude * Math.sin(session.coolingTick * 0.15);
            double dec = decBase + fluctuation;
            session.coolingValue = Math.max(0, session.coolingValue - dec);

            session.bossBar.setProgress(Math.max(0, Math.min(1.0, session.coolingValue)));

            double idealCenter = plugin.getConfig().getDouble("blacksmith.cooling.ideal-center", 0.35);
            double zoneBase = plugin.getConfig().getDouble("blacksmith.cooling.ideal-zone-base", 0.15);
            double zoneMin = plugin.getConfig().getDouble("blacksmith.cooling.ideal-zone-min", 0.06);
            double zoneWidth = zoneBase - (zoneBase - zoneMin) * effFactor;
            double lower = idealCenter - zoneWidth / 2;
            double upper = idealCenter + zoneWidth / 2;

            BarColor c;
            if (session.coolingValue >= lower && session.coolingValue <= upper) c = BarColor.GREEN;
            else if (Math.abs(session.coolingValue - idealCenter) < zoneWidth * 1.5) c = BarColor.YELLOW;
            else c = BarColor.RED;
            session.bossBar.setColor(c);

            int pct = (int) (session.coolingValue * 100);
            session.bossBar.setTitle("Resfriamento [" + pct + "%] - Clique no Caldeirao para parar!");

            // Particles
            Location tl = session.tableLoc.clone().add(0.5, 1.2, 0.5);
            tl.getWorld().spawnParticle(Particle.CLOUD, tl, 1, 0.2, 0.1, 0.2, 0.01);

            // Auto-fail if reaches 0
            if (session.coolingValue <= 0) {
                session.failures.add(FailureType.MEDIUM);
                session.qualityScore -= 15;
                player.sendActionBar(Component.text("Resfriou demais! Penalidade!", NamedTextColor.RED));
                player.playSound(player.getLocation(), Sound.ENTITY_ITEM_BREAK, 0.8f, 0.5f);
                advanceStage(player, session);
            }
        }, tickInterval, tickInterval);
    }

    private void handleCoolingAction(Player player, ForgingSession session) {
        // Click on cauldron = stop cooling at current value
        double idealCenter = plugin.getConfig().getDouble("blacksmith.cooling.ideal-center", 0.35);
        double zoneBase = plugin.getConfig().getDouble("blacksmith.cooling.ideal-zone-base", 0.15);
        double zoneMin = plugin.getConfig().getDouble("blacksmith.cooling.ideal-zone-min", 0.06);
        double effFactor = Math.min((session.efficiency - 1.0) / 1.92, 1.0);
        double zoneWidth = zoneBase - (zoneBase - zoneMin) * effFactor;
        double lower = idealCenter - zoneWidth / 2;
        double upper = idealCenter + zoneWidth / 2;

        double val = session.coolingValue;

        if (val >= lower && val <= upper) {
            double centerDist = Math.abs(val - idealCenter) / (zoneWidth / 2);
            if (centerDist < 0.3) {
                session.qualityScore += 15;
                player.sendActionBar(Component.text("Resfriamento PERFEITO!", NamedTextColor.GREEN));
                player.playSound(player.getLocation(), Sound.ENTITY_GENERIC_SPLASH, 0.8f, 1.5f);
            } else {
                session.qualityScore += 8;
                player.sendActionBar(Component.text("Resfriamento Bom", NamedTextColor.YELLOW));
                player.playSound(player.getLocation(), Sound.ENTITY_GENERIC_SPLASH, 0.6f, 1.2f);
            }
        } else {
            double distance = val < lower ? lower - val : val - upper;
            double penalty = Math.min(distance * 30, 20);
            session.qualityScore -= penalty;
            if (distance > 0.2) session.failures.add(FailureType.LIGHT);
            player.sendActionBar(Component.text("Resfriamento RUIM!", NamedTextColor.RED));
            player.playSound(player.getLocation(), Sound.ENTITY_ITEM_BREAK, 0.8f, 0.5f);
        }

        session.qualityScore = Math.max(0, Math.min(100, session.qualityScore));

        if (debug()) plugin.getLogger().info("[FORGE] Cooling: val=" + String.format("%.2f", val)
                + " zone=[" + String.format("%.2f", lower) + "-" + String.format("%.2f", upper) + "]"
                + " quality=" + String.format("%.1f", session.qualityScore));

        advanceStage(player, session);
    }

    // ═══════════════════════════════
    // Stage 4 - Refining (Grindstone)
    // ═══════════════════════════════

    private void startRefiningTicker(Player player, ForgingSession session) {
        session.refiningClicks = 0;
        session.refiningTick = 0;
        session.refiningWindowOpen = false;

        session.bossBar = Bukkit.createBossBar(
                "Refino - Espere... [0/" + session.refiningRequired + "]",
                BarColor.RED, BarStyle.SOLID);
        session.bossBar.setProgress(0);
        session.bossBar.addPlayer(player);

        int timingWindowTicks = plugin.getConfig().getInt("blacksmith.refining.timing-window-ticks", 16);
        int gapBase = plugin.getConfig().getInt("blacksmith.refining.timing-gap-ticks-base", 40);
        int gapMin = plugin.getConfig().getInt("blacksmith.refining.timing-gap-ticks-min", 20);
        double effFactor = Math.min((session.efficiency - 1.0) / 1.92, 1.0);
        int gap = (int) (gapBase - (gapBase - gapMin) * effFactor);

        session.stageTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (!player.isOnline()) { cancelForging(player, session, false); return; }

            session.refiningTick++;
            int cycleLength = gap + timingWindowTicks;
            int posInCycle = session.refiningTick % cycleLength;

            if (posInCycle >= gap) {
                // Window open
                if (!session.refiningWindowOpen) {
                    session.refiningWindowOpen = true;
                    player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BELL, 0.8f, 2.0f);
                }
                session.bossBar.setColor(BarColor.GREEN);
                session.bossBar.setTitle("Refino - AGORA! Clique no Rebolo! [" + session.refiningClicks + "/" + session.refiningRequired + "]");
                double windowProgress = (double)(posInCycle - gap) / timingWindowTicks;
                session.bossBar.setProgress(Math.max(0, Math.min(1.0, windowProgress)));
            } else {
                // Window closed
                session.refiningWindowOpen = false;
                session.bossBar.setColor(BarColor.RED);
                session.bossBar.setTitle("Refino - Espere... [" + session.refiningClicks + "/" + session.refiningRequired + "]");
                session.bossBar.setProgress(0);
            }
        }, 1, 1);
    }

    private void handleRefiningAction(Player player, ForgingSession session) {
        double qualityBonus = plugin.getConfig().getDouble("blacksmith.refining.quality-bonus", 3.0);
        double penaltyChance = plugin.getConfig().getDouble("blacksmith.refining.penalty-removal-chance", 0.8);

        if (session.refiningWindowOpen) {
            // Success!
            session.refiningClicks++;
            if (!session.failures.isEmpty() && Math.random() < penaltyChance) {
                session.failures.remove(session.failures.size() - 1);
                player.sendActionBar(Component.text("Falha removida! [" + session.refiningClicks + "/" + session.refiningRequired + "]",
                        NamedTextColor.GREEN));
            } else {
                session.qualityScore = Math.min(100, session.qualityScore + qualityBonus);
                player.sendActionBar(Component.text("+" + (int) qualityBonus + " qualidade! [" + session.refiningClicks + "/" + session.refiningRequired + "]",
                        NamedTextColor.GREEN));
            }
            player.playSound(player.getLocation(), Sound.BLOCK_GRINDSTONE_USE, 1.0f, 1.5f);

            if (session.refiningClicks >= session.refiningRequired) {
                advanceStage(player, session);
            }
        } else {
            // Clicked outside window - penalty
            session.qualityScore = Math.max(0, session.qualityScore - 5);
            session.failures.add(FailureType.LIGHT);
            player.sendActionBar(Component.text("Fora do timing! -5 qualidade", NamedTextColor.RED));
            player.playSound(player.getLocation(), Sound.BLOCK_GRINDSTONE_USE, 0.5f, 0.5f);
        }
    }

    // ═══════════════════════════════
    // Complete Forging
    // ═══════════════════════════════

    private void completeForging(Player player, ForgingSession session) {
        if (session.stageTask != null) { session.stageTask.cancel(); session.stageTask = null; }
        removeBossBar(session);
        stopForgeAura(player);

        // Oil crafting has a separate completion path
        if (session.isOilCrafting) {
            completeOilCrafting(player, session);
            return;
        }

        // Apply breakpoint bonuses
        switch (session.breakpoint) {
            case 1 -> session.qualityScore += 5;
            case 2 -> session.qualityScore += 10;
            case 3 -> session.qualityScore += 15;
            case 4 -> session.qualityScore += 20;
            case 5 -> session.qualityScore += 25;
        }

        // Apply failure penalties
        for (FailureType f : session.failures) {
            switch (f) {
                case LIGHT -> session.qualityScore -= 3;
                case MEDIUM -> session.qualityScore -= 8;
                case SEVERE -> session.qualityScore -= 15;
                case CRITICAL -> session.qualityScore -= 30;
            }
        }

        session.qualityScore = Math.max(0, Math.min(100, session.qualityScore));

        // Break chance based on forge count (3+ forges risk breaking)
        double breakChance = switch (session.forgeCount) {
            case 1, 2 -> 0.0;
            case 3 -> 0.25;
            case 4 -> 0.45;
            default -> 0.75; // 5+
        };
        if (breakChance > 0 && Math.random() < breakChance) {
            session.inputItem = null;
            session.currentStage = ForgeStage.COMPLETE;
            cooldowns.put(player.getUniqueId(), System.currentTimeMillis());
            activeSessions.remove(player.getUniqueId());
            Bukkit.getScheduler().runTask(plugin, (Runnable) player::closeInventory);
            player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                    "&c[Forja] O item nao resistiu a " + session.forgeCount + "a forja e quebrou!"));
            player.playSound(player.getLocation(), Sound.ENTITY_ITEM_BREAK, 1.0f, 0.5f);
            return;
        }

        // Critical failure check for high breakpoints
        double critChance = plugin.getConfig().getDouble("blacksmith.critical-failure-chance", 0.01);
        if (session.breakpoint >= 4 && session.qualityScore < 25 && Math.random() < critChance) {
            session.inputItem = null;
            session.currentStage = ForgeStage.COMPLETE;
            cooldowns.put(player.getUniqueId(), System.currentTimeMillis());
            activeSessions.remove(player.getUniqueId());
            Bukkit.getScheduler().runTask(plugin, (Runnable) player::closeInventory);
            player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                    plugin.getConfig().getString("blacksmith.messages.forging-failed",
                            "&c[Forja] O item quebrou durante a forja!")));
            player.playSound(player.getLocation(), Sound.ENTITY_ITEM_BREAK, 1.0f, 0.5f);
            return;
        }

        // Apply quality result to item - returns consistent data for chat
        ForgeResult result = applyQualityToItem(session);

        session.currentStage = ForgeStage.COMPLETE;
        cooldowns.put(player.getUniqueId(), System.currentTimeMillis());

        if (result != null) {
            // Chat message using SAME data as lore
            player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                    "&a[Forja] Forja concluida! Qualidade: &" + getColorCode(result.qualityColor())
                            + result.qualityTag() + " &a(+" + String.format("%.1f", result.melhoriaReal()) + "%)"));
            // Show improvements in chat
            if (!result.improvements().isEmpty()) {
                player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&e[Forja] Melhorias aplicadas:"));
                for (String imp : result.improvements()) {
                    player.sendMessage(ChatColor.translateAlternateColorCodes('&', "  &7- " + imp));
                }
            }
        }

        player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                "&e[Forja] Volte a Mesa do Ferreiro (Bigorna) para coletar o item!"));

        // Sound/VFX based on quality
        if (result != null && session.qualityScore >= plugin.getConfig().getInt("blacksmith.quality.masterwork", 90)) {
            player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.0f);
            player.getLocation().getWorld().spawnParticle(Particle.TOTEM_OF_UNDYING, player.getLocation().add(0, 1, 0), 30, 0.5, 0.5, 0.5, 0.1);
        } else if (session.qualityScore >= plugin.getConfig().getInt("blacksmith.quality.excellent", 75)) {
            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);
        } else {
            player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_USE, 0.8f, 1.0f);
        }

        // BP5 completion: Mace wind burst effect
        if (session.breakpoint >= 5) {
            Location center = session.tableLoc.clone().add(0.5, 1, 0.5);
            World world = center.getWorld();
            if (world != null) {
                try { player.playSound(center, Sound.ENTITY_WIND_CHARGE_WIND_BURST, 1.5f, 0.8f); } catch (Exception ignored) {}
                world.spawnParticle(Particle.EXPLOSION, center, 3, 0.5, 0.5, 0.5, 0);
            }
        }

        if (debug()) plugin.getLogger().info("[FORGE] " + player.getName() + " completou forja. Qualidade="
                + String.format("%.1f", session.qualityScore) + " falhas=" + session.failures.size()
                + " breakpoint=" + session.breakpoint + " forgeCount=" + session.forgeCount);
    }

    private void completeOilCrafting(Player player, ForgingSession session) {
        // Get Oleo de Polimento from MMOItems (CONSUMABLE / OLEO_DE_POLIMENTO)
        ItemStack oil = null;
        try {
            Class<?> typeClass = Class.forName("net.Indyuce.mmoitems.api.Type");
            Object consumableType = typeClass.getMethod("get", String.class).invoke(null, "CONSUMABLE");
            if (consumableType != null) {
                Class<?> miClass = Class.forName("net.Indyuce.mmoitems.MMOItems");
                Object miPlugin = miClass.getField("plugin").get(null);
                if (miPlugin != null) {
                    oil = (ItemStack) miClass.getMethod("getItem", typeClass, String.class)
                            .invoke(miPlugin, consumableType, "OLEO_DE_POLIMENTO");
                }
            }
        } catch (Exception e) {
            plugin.getLogger().warning("[FORGE-OIL] Erro ao criar Oleo via MMOItems: " + e.getMessage());
        }

        // Fallback if MMOItems item not found
        if (oil == null) {
            oil = new ItemStack(Material.HONEY_BOTTLE);
            ItemMeta meta = oil.getItemMeta();
            if (meta != null) {
                meta.displayName(Component.text("Oleo de Polimento", NamedTextColor.GOLD, TextDecoration.BOLD));
                List<Component> lore = new ArrayList<>();
                lore.add(Component.text("Aplique em um equipamento para polir.", NamedTextColor.GRAY));
                lore.add(Component.text("Fabricado por " + player.getName(), NamedTextColor.DARK_GRAY));
                meta.lore(lore);
                oil.setItemMeta(meta);
            }
            plugin.getLogger().warning("[FORGE-OIL] MMOItem OLEO_DE_POLIMENTO nao encontrado, usando fallback.");
        }

        session.inputItem = oil;
        session.currentStage = ForgeStage.COMPLETE;
        cooldowns.put(player.getUniqueId(), System.currentTimeMillis());

        player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                "&a[Forja] Oleo de Polimento fabricado com sucesso!"));
        player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                "&e[Forja] Volte a Mesa do Ferreiro (Bigorna) para coletar!"));
        player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.0f);
    }

    private String getColorCode(NamedTextColor color) {
        if (color == NamedTextColor.GOLD) return "6";
        if (color == NamedTextColor.GREEN) return "a";
        if (color == NamedTextColor.WHITE) return "f";
        if (color == NamedTextColor.GRAY) return "7";
        return "f";
    }

    private ForgeResult applyQualityToItem(ForgingSession session) {
        if (session.inputItem == null) return null;
        ItemStack beforeForge = session.inputItem.clone();

        Player player = Bukkit.getPlayer(session.playerId);
        int playerLevel = player != null ? getPlayerLevel(player) : 0;
        String playerName = player != null ? player.getName() : "Desconhecido";

        // Calculate improvement cap
        double baseCap = plugin.getConfig().getDouble("blacksmith.improvement.base-cap", 8.0);
        double perLevel = plugin.getConfig().getDouble("blacksmith.improvement.per-level", 0.05);
        double bpBonus = plugin.getConfig().getDouble("blacksmith.improvement.breakpoint-bonus." + session.breakpoint, 0.0);
        double capMaximo = baseCap + (playerLevel * perLevel) + bpBonus;

        // Hidden Mercador bonus (+10%) - NOT shown anywhere
        if (session.environment.mercadorNearby()) {
            capMaximo *= 1.10;
        }
        // Hidden Alquimista/Ferreiro debuff (-5%)
        if (session.environment.alquimistaNearby() || session.environment.ferreiroNearby()) {
            capMaximo *= 0.95;
        }

        // Unique items: minimum 5% bonus
        if (session.itemTier == 3) {
            capMaximo = Math.max(capMaximo, 5.0);
        }

        double melhoriaReal = (session.qualityScore / 100.0) * capMaximo;
        double bonusMult = melhoriaReal / 100.0;

        int masterwork = plugin.getConfig().getInt("blacksmith.quality.masterwork", 90);
        int excellent = plugin.getConfig().getInt("blacksmith.quality.excellent", 75);
        int good = plugin.getConfig().getInt("blacksmith.quality.good", 50);

        String qualityTag;
        NamedTextColor qualityColor;
        if (session.qualityScore >= masterwork) {
            qualityTag = "Obra-Prima"; qualityColor = NamedTextColor.GOLD;
        } else if (session.qualityScore >= excellent) {
            qualityTag = "Excelente"; qualityColor = NamedTextColor.GREEN;
        } else if (session.qualityScore >= good) {
            qualityTag = "Bom"; qualityColor = NamedTextColor.WHITE;
        } else {
            qualityTag = "Pobre"; qualityColor = NamedTextColor.GRAY;
        }

        List<String> improvements = new ArrayList<>();
        boolean hasMods = false;
        boolean modImproved = false;

        try {
            NBTItem nbt = NBTItem.get(session.inputItem);

            // Write forge count
            nbt.addTag(new io.lumine.mythic.lib.api.item.ItemTag(NBT_FORGE_COUNT, session.forgeCount));

            // Melhoria ativa de modificadores (NEMONICORB_MODS)
            String modsJson = nbt.hasTag(OrbListener.NBT_MODS) ? nbt.getString(OrbListener.NBT_MODS) : null;
            if (modsJson != null && !modsJson.isBlank() && !"[]".equals(modsJson.trim()) && bonusMult > 0) {
                try {
                    @SuppressWarnings("unchecked")
                    List<Map<String, Object>> mods = GSON.fromJson(modsJson, List.class);
                    if (mods != null && !mods.isEmpty()) {
                        hasMods = true;
                        int modImprovements = 1;
                        if (Math.random() < 0.15) modImprovements = 2;
                        if (session.itemTier == 3) modImprovements = Math.max(modImprovements, 1);
                        modImprovements = Math.min(modImprovements, mods.size());
                        if (modImprovements >= 1) {
                            List<Integer> indices = new ArrayList<>();
                            for (int i = 0; i < mods.size(); i++) indices.add(i);
                            Collections.shuffle(indices);
                            int improved = 0;
                            for (int idx : indices) {
                                if (improved >= modImprovements) break;
                                Map<String, Object> mod = mods.get(idx);
                                @SuppressWarnings("unchecked")
                                Map<String, Object> modStats = (Map<String, Object>) mod.get("stats");
                                if (modStats == null) continue;
                                double modBoost = Math.max(0.03, bonusMult * (0.85 + (Math.random() * 0.30)));
                                boolean thisModImproved = false;
                                for (Map.Entry<String, Object> e : modStats.entrySet()) {
                                    if (e.getValue() instanceof Number num) {
                                        double oldVal = num.doubleValue();
                                        double newVal = oldVal * (1 + modBoost);
                                        modStats.put(e.getKey(), newVal);
                                        thisModImproved = true;
                                        improvements.add("Mod " + mod.getOrDefault("id", "?") + "/" + e.getKey()
                                                + ": +" + String.format("%.1f%%", modBoost * 100));
                                    }
                                }
                                if (thisModImproved) modImproved = true;
                                improved++;
                            }
                            nbt.addTag(new io.lumine.mythic.lib.api.item.ItemTag(OrbListener.NBT_MODS, GSON.toJson(mods)));
                        }
                    }
                } catch (Exception e) {
                    if (debug()) plugin.getLogger().warning("[FORGE] Erro ao melhorar mods: " + e.getMessage());
                }
            }
            if (!hasMods) {
                improvements.add("Sem mods: nenhum aprimoramento aplicado");
            }

            session.inputItem = nbt.toItem();

            // Re-render orb modifier lore with boosted values (NBT_MODS was mutated above).
            // Without this, the lore keeps showing the old numbers even though NBT holds the new ones.
            try {
                plugin.getOrbListener().updateItemDisplay(session.inputItem, NBTItem.get(session.inputItem));
            } catch (Exception e) {
                if (debug()) plugin.getLogger().warning("[FORGE] updateItemDisplay falhou: " + e.getMessage());
            }
        } catch (Exception e) {
            plugin.getLogger().warning("[FORGE] Erro ao aplicar bonus NBT: " + e.getMessage());
        }

        double melhoriaExibida = modImproved ? melhoriaReal : 0.0;

        // Persistir metadados da forja em NBT (renderizados por OrbListener.updateItemDisplay).
        // Isso garante que a lore da forja sobrevive a qualquer rebuild posterior (orbs, reparo, etc.).
        try {
            NBTItem forgeNbt = NBTItem.get(session.inputItem);
            forgeNbt.addTag(new io.lumine.mythic.lib.api.item.ItemTag(OrbListener.NBT_FORGE_QUALITY, qualityTag));
            forgeNbt.addTag(new io.lumine.mythic.lib.api.item.ItemTag(OrbListener.NBT_FORGE_SMITH,
                    playerName + "|" + playerLevel));
            forgeNbt.addTag(new io.lumine.mythic.lib.api.item.ItemTag(OrbListener.NBT_FORGE_BONUS_PCT, melhoriaExibida));
            session.inputItem = forgeNbt.toItem();

            // Re-renderizar lore ja incluindo bloco de forja (via updateItemDisplay).
            plugin.getOrbListener().updateItemDisplay(session.inputItem, NBTItem.get(session.inputItem));
        } catch (Exception e) {
            if (debug()) plugin.getLogger().warning("[FORGE] Falha ao gravar NBT da forja: " + e.getMessage());
        }

        if (debug()) plugin.getLogger().info("[FORGE] Melhoria: cap=" + String.format("%.1f%%", capMaximo)
                + " real=" + String.format("%.1f%%", melhoriaExibida) + " mult=" + String.format("%.4f", bonusMult)
                + " nivel=" + playerLevel + " bp=" + session.breakpoint + " forgeCount=" + session.forgeCount
                + " mercador=" + session.environment.mercadorNearby()
                + " improvements=" + improvements.size());

        try {
            Player p = Bukkit.getPlayer(session.playerId);
            if (p != null) {
                plugin.getModifierAuditService().logBeforeAfter("forja:resultado", p, beforeForge, session.inputItem);
            }
        } catch (Exception ignored) {}

        return new ForgeResult(qualityTag, qualityColor, melhoriaExibida, capMaximo, improvements);
    }

    // ═══════════════════════════════
    // Cancel Forging
    // ═══════════════════════════════

    private void cancelForging(Player player, ForgingSession session, boolean notify) {
        removeBossBar(session);
        stopForgeAura(player);
        if (session.stageTask != null) { session.stageTask.cancel(); session.stageTask = null; }

        ItemStack returnCandidate = null;
        String returnSource = "NONE";

        // In IDLE, the item may still be in the GUI input slot (session.inputItem is usually null).
        if (session.currentStage == ForgeStage.IDLE) {
            try {
                org.bukkit.inventory.InventoryView view = player.getOpenInventory();
                if (view != null) {
                    String title = getInventoryTitle(view);
                    if (GUI_TITLE_RAW.equals(title)) {
                        Inventory top = view.getTopInventory();
                        ItemStack inputSlot = top.getItem(SLOT_INPUT);
                        if (inputSlot != null && !inputSlot.getType().isAir()) {
                            returnCandidate = inputSlot.clone();
                            returnSource = "GUI_SLOT";
                            top.setItem(SLOT_INPUT, null); // clear before returning to avoid dupes
                        }
                    }
                }
            } catch (Exception ignored) {}
        }

        if (returnCandidate == null && session.inputItem != null && !session.inputItem.getType().isAir()) {
            returnCandidate = session.inputItem;
            returnSource = "SESSION_ITEM";
        }

        if (returnCandidate != null && !returnCandidate.getType().isAir()) {
            returnItem(player, returnCandidate);
            if (debug()) {
                plugin.getLogger().info("[FORGE] Cancel return (" + returnSource + "): "
                        + player.getName() + " -> " + returnCandidate.getType().name() + " x" + returnCandidate.getAmount());
            }
        } else if (debug()) {
            plugin.getLogger().info("[FORGE] Cancel return (NONE): " + player.getName()
                    + " stage=" + session.currentStage.name());
        }
        session.inputItem = null;

        activeSessions.remove(player.getUniqueId());

        if (notify) {
            player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                    plugin.getConfig().getString("blacksmith.messages.cancel", "&c[Forja] Forja cancelada.")));
            player.playSound(player.getLocation(), Sound.ENTITY_ITEM_BREAK, 0.6f, 0.8f);
            // Fechar a GUI para impedir que o jogador pegue itens decorativos
            // apos a sessao ter sido removida (o handler de click ignora sem sessao)
            Bukkit.getScheduler().runTask(plugin, (Runnable) player::closeInventory);
        }
    }

    // ═══════════════════════════════
    // BossBar helpers
    // ═══════════════════════════════

    private void removeBossBar(ForgingSession session) {
        if (session.bossBar != null) {
            session.bossBar.removeAll();
            session.bossBar = null;
        }
    }

    // ═══════════════════════════════
    // Aura + VFX (BP5 / 290%+)
    // ═══════════════════════════════

    private void startForgeAura(Player player, ForgingSession session) {
        if (session.breakpoint < 5) return;
        stopForgeAura(player);

        BukkitTask task = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (!player.isOnline() || !activeSessions.containsKey(player.getUniqueId())) {
                stopForgeAura(player);
                return;
            }

            Location loc = session.tableLoc;
            World world = loc.getWorld();
            if (world == null) return;
            double cx = loc.getX() + 0.5, cy = loc.getY() + 1.0, cz = loc.getZ() + 0.5;

            // Orange/gold particle ring
            Particle.DustOptions dust = new Particle.DustOptions(Color.fromRGB(255, 165, 0), 1.5f);
            double time = (System.currentTimeMillis() % 3000) / 3000.0 * 2 * Math.PI;
            for (int i = 0; i < 20; i++) {
                double angle = (2 * Math.PI / 20) * i + time;
                world.spawnParticle(Particle.DUST, cx + 3 * Math.cos(angle), cy + 0.5, cz + 3 * Math.sin(angle), 1, 0, 0, 0, 0, dust);
            }

            // Resistance effect
            if (!player.hasPotionEffect(PotionEffectType.RESISTANCE)) {
                player.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, 100, 0, true, true, true));
            }
        }, 10L, 10L);

        auraTasks.put(player.getUniqueId(), task);
    }

    private void stopForgeAura(Player player) {
        BukkitTask task = auraTasks.remove(player.getUniqueId());
        if (task != null) task.cancel();
    }

    // ═══════════════════════════════
    // Item helpers
    // ═══════════════════════════════

    private ItemStack createGlassPane(Material material, String name) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text(name, NamedTextColor.DARK_GRAY));
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack createArrowPane() {
        ItemStack item = new ItemStack(Material.YELLOW_STAINED_GLASS_PANE);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text(">>>", NamedTextColor.YELLOW));
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack createEfficiencyItem(ForgeEnvironment env, double efficiency, int breakpoint, int playerLevel) {
        ItemStack item = new ItemStack(Material.NETHER_STAR);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text("Eficiencia da Forja", NamedTextColor.GOLD, TextDecoration.BOLD));
            List<Component> lore = new ArrayList<>();
            lore.add(Component.text("Eficiencia: " + String.format("%.0f%%", efficiency * 100), NamedTextColor.WHITE));
            lore.add(Component.text("Breakpoint: " + breakpoint, NamedTextColor.YELLOW));
            lore.add(Component.text("Nivel: " + playerLevel, NamedTextColor.AQUA));
            lore.add(Component.empty());
            lore.add(Component.text("Blocos detectados:", NamedTextColor.GRAY));
            for (Map.Entry<Material, Integer> entry : env.blockCounts().entrySet()) {
                double[] cfg = FORGE_BLOCKS.get(entry.getKey());
                if (cfg == null) continue;
                // Only show if count meets threshold
                int threshold = INDICATOR_THRESHOLDS.getOrDefault(entry.getKey().name(), 1);
                if (entry.getValue() < threshold) continue;
                int count = Math.min(entry.getValue(), (int) cfg[1]);
                String bonus = String.format("+%.0f%%", count * cfg[0] * 100);
                lore.add(Component.text("  \u2726 " + formatMaterial(entry.getKey()) + ": " + count + " (" + bonus + ")",
                        NamedTextColor.GRAY));
            }
            if (env.nearbyPlayers() > 0) {
                lore.add(Component.text("  \u2726 Jogadores proximos: " + env.nearbyPlayers()
                        + " (+" + String.format("%.0f%%", env.playerBonus() * 100) + ")", NamedTextColor.GRAY));
            }
            lore.add(Component.empty());
            lore.add(Component.text("Maior eficiencia = maior dificuldade", NamedTextColor.RED));
            lore.add(Component.text("+ melhor resultado potencial!", NamedTextColor.GREEN));
            meta.lore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack createInfoItem() {
        ItemStack item = new ItemStack(Material.ENCHANTED_BOOK);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text("Como Forjar", NamedTextColor.AQUA, TextDecoration.BOLD));
            List<Component> lore = new ArrayList<>();
            lore.add(Component.text("1. Coloque um equipamento no slot", NamedTextColor.WHITE));
            lore.add(Component.text("2. Clique em Iniciar Forja", NamedTextColor.WHITE));
            lore.add(Component.text("3. Va ate a Blast Furnace - aquecer", NamedTextColor.GOLD));
            lore.add(Component.text("   Clique = aquecer, Agachar+Clique = confirmar", NamedTextColor.GRAY));
            lore.add(Component.text("4. Va ate a Bigorna - modelar", NamedTextColor.GOLD));
            lore.add(Component.text("   Clique quando BossBar estiver verde", NamedTextColor.GRAY));
            lore.add(Component.text("5. Va ate o Caldeirao - resfriar", NamedTextColor.GOLD));
            lore.add(Component.text("   Clique quando BossBar estiver na zona ideal", NamedTextColor.GRAY));
            lore.add(Component.text("6. Va ate o Rebolo - refinar", NamedTextColor.GOLD));
            lore.add(Component.text("   Clique quando BossBar piscar verde", NamedTextColor.GRAY));
            lore.add(Component.text("7. Volte a Mesa do Ferreiro para coletar", NamedTextColor.GREEN));
            lore.add(Component.empty());
            lore.add(Component.text("Blocos necessarios nas proximidades:", NamedTextColor.YELLOW));
            lore.add(Component.text("  Blast Furnace, Bigorna, Caldeirao, Rebolo", NamedTextColor.GRAY));
            meta.lore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack createStartButton() {
        ItemStack item = new ItemStack(Material.ANVIL);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text("Iniciar Forja", NamedTextColor.GREEN, TextDecoration.BOLD));
            List<Component> lore = new ArrayList<>();
            lore.add(Component.text("Clique para comecar!", NamedTextColor.GRAY));
            meta.lore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack createLockedOutput() {
        ItemStack item = new ItemStack(Material.BARRIER);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text("Resultado", NamedTextColor.RED));
            List<Component> lore = new ArrayList<>();
            lore.add(Component.text("Complete a forja para ver", NamedTextColor.GRAY));
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
                meta.displayName(Component.text(name, NamedTextColor.GREEN));
                meta.lore(List.of(Component.text("Bloco encontrado!", NamedTextColor.GRAY)));
                item.setItemMeta(meta);
            }
        } else {
            item = new ItemStack(Material.RED_STAINED_GLASS_PANE);
            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                meta.displayName(Component.text(name, NamedTextColor.RED));
                meta.lore(List.of(Component.text("Bloco NAO encontrado!", NamedTextColor.RED)));
                item.setItemMeta(meta);
            }
        }
        return item;
    }

    private ItemStack createCancelButton() {
        ItemStack item = new ItemStack(Material.BARRIER);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text("Cancelar", NamedTextColor.RED, TextDecoration.BOLD));
            item.setItemMeta(meta);
        }
        return item;
    }

    // ═══════════════════════════════
    // Utility
    // ═══════════════════════════════

    /** Checks if player has enough coal/charcoal and removes it. Returns false if not enough. */
    private boolean consumeFuel(Player player, int amount) {
        int found = 0;
        org.bukkit.inventory.PlayerInventory inv = player.getInventory();
        // Count coal + charcoal
        for (ItemStack item : inv.getContents()) {
            if (item != null && (item.getType() == Material.COAL || item.getType() == Material.CHARCOAL)) {
                found += item.getAmount();
            }
        }
        if (found < amount) return false;
        // Remove
        int toRemove = amount;
        for (int i = 0; i < inv.getSize() && toRemove > 0; i++) {
            ItemStack item = inv.getItem(i);
            if (item != null && (item.getType() == Material.COAL || item.getType() == Material.CHARCOAL)) {
                int take = Math.min(item.getAmount(), toRemove);
                item.setAmount(item.getAmount() - take);
                toRemove -= take;
            }
        }
        return true;
    }

    // ═══════════════════════════════
    // Repair System
    // ═══════════════════════════════

    /**
     * Determines the repair material based on the item's material name.
     * Diamond items → DIAMOND, Gold items → GOLD_INGOT, everything else → IRON_INGOT.
     */
    private Material getRepairMaterial(ItemStack item) {
        String name = item.getType().name();
        if (name.startsWith("DIAMOND_")) return Material.DIAMOND;
        if (name.startsWith("GOLDEN_") || name.startsWith("GOLD_")) return Material.GOLD_INGOT;
        // Netherite uses diamonds too
        if (name.startsWith("NETHERITE_")) return Material.DIAMOND;
        // Copper/MMOItems and everything else → iron
        return Material.IRON_INGOT;
    }

    private String formatRepairMaterial(Material mat) {
        return switch (mat) {
            case DIAMOND -> "Diamante(s)";
            case GOLD_INGOT -> "Barra(s) de Ouro";
            default -> "Barra(s) de Ferro";
        };
    }

    private int calculateRepairCost(Player player, ItemStack item) {
        if (item == null || !item.getType().isItem()) return -1;
        if (!(item.getItemMeta() instanceof org.bukkit.inventory.meta.Damageable damageable)) return -1;
        int damage = damageable.getDamage();
        if (damage <= 0) return -1;
        int maxDur = item.getType().getMaxDurability();
        if (maxDur <= 0) return -1;

        double damagePercent = (double) damage / maxDur;
        int maxCost = plugin.getConfig().getInt("blacksmith.repair.max-material-cost", 8);
        double levelDiscount = plugin.getConfig().getDouble("blacksmith.repair.level-discount", 0.005);
        double minMult = plugin.getConfig().getDouble("blacksmith.repair.min-multiplier", 0.50);

        int playerLevel = getPlayerLevel(player);
        double discount = Math.max(minMult, 1.0 - (playerLevel * levelDiscount));
        int cost = (int) Math.ceil(damagePercent * maxCost * discount);
        return Math.max(1, Math.min(maxCost, cost));
    }

    private boolean consumeRepairMaterials(Player player, Material mat, int amount) {
        int remaining = amount;
        ItemStack[] contents = player.getInventory().getContents();
        for (int i = 0; i < contents.length && remaining > 0; i++) {
            ItemStack slot = contents[i];
            if (slot != null && slot.getType() == mat) {
                int take = Math.min(slot.getAmount(), remaining);
                slot.setAmount(slot.getAmount() - take);
                remaining -= take;
            }
        }
        return remaining <= 0;
    }

    private int countMaterial(Player player, Material mat) {
        int count = 0;
        for (ItemStack slot : player.getInventory().getContents()) {
            if (slot != null && slot.getType() == mat) count += slot.getAmount();
        }
        return count;
    }

    private void handleRepair(Player player, Inventory gui, ForgingSession session) {
        ItemStack input = gui.getItem(SLOT_INPUT);
        if (input == null || input.getType().isAir()) {
            player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                    "&c[Forja] Coloque um item no slot para reparar!"));
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
            return;
        }

        int cost = calculateRepairCost(player, input);
        if (cost < 0) {
            player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                    plugin.getConfig().getString("blacksmith.messages.repair-not-damaged",
                            "&c[Forja] Este item nao esta danificado!")));
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
            return;
        }

        Material repairMat = getRepairMaterial(input);
        String matName = formatRepairMaterial(repairMat);

        if (countMaterial(player, repairMat) < cost) {
            player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                    plugin.getConfig().getString("blacksmith.messages.repair-no-materials",
                            "&c[Forja] Voce precisa de %amount%x %material% para reparar!")
                            .replace("%amount%", String.valueOf(cost))
                            .replace("%material%", matName)));
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
            return;
        }

        consumeRepairMaterials(player, repairMat, cost);

        // Repair the item
        if (input.getItemMeta() instanceof org.bukkit.inventory.meta.Damageable damageable) {
            damageable.setDamage(0);
            input.setItemMeta((ItemMeta) damageable);
        }

        player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_USE, 1.0f, 1.2f);
        player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                plugin.getConfig().getString("blacksmith.messages.repair-success",
                        "&a[Forja] Item reparado com sucesso! (-%amount%x %material%)")
                        .replace("%amount%", String.valueOf(cost))
                        .replace("%material%", matName)));

        if (debug()) plugin.getLogger().info("[FORGE] Reparo: " + player.getName()
                + " custo=" + cost + "x " + repairMat.name());
    }

    private ItemStack createRepairButton(Player player, ItemStack input) {
        ItemStack item = new ItemStack(Material.GOLDEN_PICKAXE);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text("Reparar Item", NamedTextColor.GOLD, TextDecoration.BOLD));
            List<Component> lore = new ArrayList<>();
            if (input != null && !input.getType().isAir()) {
                int cost = calculateRepairCost(player, input);
                if (cost > 0) {
                    Material repairMat = getRepairMaterial(input);
                    String matName = formatRepairMaterial(repairMat);
                    int playerHas = countMaterial(player, repairMat);
                    boolean canAfford = playerHas >= cost;
                    lore.add(Component.text("Custo: " + cost + "x " + matName,
                            canAfford ? NamedTextColor.GREEN : NamedTextColor.RED));
                    lore.add(Component.text("Voce tem: " + playerHas + "x " + matName, NamedTextColor.GRAY));
                    lore.add(Component.empty());
                    lore.add(Component.text("Clique para reparar!", NamedTextColor.YELLOW));
                } else {
                    lore.add(Component.text("Item nao esta danificado", NamedTextColor.GRAY));
                }
            } else {
                lore.add(Component.text("Coloque um item no slot", NamedTextColor.GRAY));
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
        if (item == null || item.getType().isAir()) return;
        Map<Integer, ItemStack> overflow = player.getInventory().addItem(item);
        for (ItemStack drop : overflow.values()) {
            player.getWorld().dropItemNaturally(player.getLocation(), drop);
        }
        syncInventoryLater(player);
    }

    private void syncInventoryLater(Player player) {
        Bukkit.getScheduler().runTask(plugin, () -> {
            if (player.isOnline()) player.updateInventory();
        });
    }

    private String getInventoryTitle(org.bukkit.inventory.InventoryView view) {
        try {
            Component titleComp = view.title();
            if (titleComp instanceof net.kyori.adventure.text.TextComponent tc) {
                return tc.content();
            }
            return net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText().serialize(titleComp);
        } catch (Exception e) {
            return "";
        }
    }

    private String formatMaterial(Material mat) {
        return switch (mat) {
            case BLAST_FURNACE -> "Blast Furnace";
            case WATER_CAULDRON -> "Caldeirao (agua)";
            case IRON_BLOCK -> "Bloco de Ferro";
            case LAVA -> "Lava";
            case ANVIL, CHIPPED_ANVIL, DAMAGED_ANVIL -> "Bigorna";
            default -> mat.name();
        };
    }

    private String getStageBlockName(ForgeStage stage) {
        return switch (stage) {
            case HEATING -> "Blast Furnace";
            case SHAPING -> "Bigorna";
            case COOLING -> "Caldeirao";
            case REFINING -> "Rebolo";
            default -> "Mesa do Ferreiro";
        };
    }
}
