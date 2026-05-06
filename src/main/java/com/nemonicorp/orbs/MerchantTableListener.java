package com.nemonicorp.orbs;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import io.lumine.mythic.lib.api.item.ItemTag;
import io.lumine.mythic.lib.api.item.NBTItem;
import net.Indyuce.mmoitems.MMOItems;
import net.Indyuce.mmoitems.api.Type;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ItemFrame;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.block.Action;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitTask;

import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Consumer;

/**
 * Mesa do Mercador — exclusiva para a classe Mercador (MMOCore).
 * Funcoes:
 *   1. Avaliar Item (identificar + boost de mods)
 *   2. Fabricar Pergaminho de Identificacao
 *   3. Fabricar Pocao de Retorno (requer Mesa de Transmutacao proxima)
 *   4. Aplicar Encantamento (requer Mesa do Ferreiro proxima)
 *   5. Fabricar Pedra de Refinamento (requer Mesa do Ferreiro proxima)
 */
public class MerchantTableListener implements Listener {

    private final NemonicOrbPlugin plugin;
    private final OrbListener orbListener;
    private final MerchantRouteManager routeManager;
    private static final Gson GSON = new Gson();

    // Sessoes ativas: UUID -> sessao
    private final Map<UUID, MerchantSession> activeSessions = new ConcurrentHashMap<>();

    // Cooldowns por funcao: UUID -> timestamp do ultimo uso
    private final Map<UUID, Long> avaliacaoCooldowns = new ConcurrentHashMap<>();
    private final Map<UUID, Long> craftPergCooldowns = new ConcurrentHashMap<>();
    private final Map<UUID, Long> craftPocaoCooldowns = new ConcurrentHashMap<>();
    private final Map<UUID, Long> enchantCooldowns = new ConcurrentHashMap<>();
    private final Map<UUID, Long> craftPedraCooldowns = new ConcurrentHashMap<>();
    private final Map<UUID, Long> seloCraftCooldowns = new ConcurrentHashMap<>();
    private final Map<UUID, Long> teleportCooldowns = new ConcurrentHashMap<>();

    // Casting de teletransporte ativo: UUID -> task
    private final Map<UUID, BukkitTask> castingTasks = new ConcurrentHashMap<>();
    private final Map<UUID, Location> castingStartLocs = new ConcurrentHashMap<>();

    // Hotbar backup durante casting: UUID -> hotbar items (slots 0-8)
    private final Map<UUID, ItemStack[]> hotbarBackups = new ConcurrentHashMap<>();
    // Destinos disponiveis durante casting: UUID -> lista de waypoints
    private final Map<UUID, List<MerchantRouteManager.WaypointInfo>> castingDestinations = new ConcurrentHashMap<>();
    // Destino selecionado: UUID -> slot da hotbar selecionado (-1 = nenhum)
    private final Map<UUID, Integer> selectedDestination = new ConcurrentHashMap<>();

    // GUI de rotas: UUID -> gui title marker
    private static final String ROUTES_GUI_TITLE = "Rotas Comerciais";
    private static final int ROUTES_GUI_SIZE = 27;

    // Cache de scan ambiental: "world,x,y,z" -> (env, timestamp)
    private final Map<String, CachedScan> scanCache = new ConcurrentHashMap<>();

    // Tarefas de aura visual por jogador
    private final Map<UUID, BukkitTask> auraTasks = new ConcurrentHashMap<>();

    // ═══════════════════════════════════════════════════
    // Layout da GUI (54 slots - 6 linhas)
    // ═══════════════════════════════════════════════════
    // Row 0: [g][g][g][EFF=3][g][INFO=5][g][ROUTES=7][g]
    // Row 1: [g][ITEM_IN=10][>11][AVALIAR=12][>13][ITEM_OUT=14][g][CRAFT_PERG=16][g]
    // Row 2: gold glass separator (slots 18-26)
    // Row 3: [g][MAT_IN=28][>29][POCAO=30][>31][POC_OUT=32][POC_OUT2=33][g][g]
    // Row 4: [g][LIVRO=37][>38][ENCANTAR=39][>40][ENC_OUT=41][CRAFT_PEDRA=42][g][g]
    // Row 5: [g][g][g][g][g][g][g][g][CANCEL=53]
    // ═══════════════════════════════════════════════════

    private static final int GUI_SIZE = 54;

    // Row 0
    private static final int SLOT_EFFICIENCY = 3;
    private static final int SLOT_INFO = 5;
    private static final int SLOT_ROUTES = 7;

    // Row 1: Avaliacao
    private static final int SLOT_ITEM_IN = 10;
    private static final int SLOT_ARROW_AV_1 = 11;
    private static final int SLOT_AVALIAR = 12;
    private static final int SLOT_ARROW_AV_2 = 13;
    private static final int SLOT_ITEM_OUT = 14;
    private static final int SLOT_CRAFT_PERG = 16;

    // Row 3: Pocao de Retorno
    private static final int SLOT_MAT_IN = 28;
    private static final int SLOT_ARROW_POC_1 = 29;
    private static final int SLOT_POCAO = 30;
    private static final int SLOT_ARROW_POC_2 = 31;
    private static final int SLOT_POC_OUT = 32;
    private static final int SLOT_POC_OUT2 = 33;

    // Row 4: Encantamento
    private static final int SLOT_LIVRO = 37;
    private static final int SLOT_ARROW_ENC_1 = 38;
    private static final int SLOT_ENCANTAR = 39;
    private static final int SLOT_ARROW_ENC_2 = 40;
    private static final int SLOT_ENC_OUT = 41;
    private static final int SLOT_CRAFT_PEDRA = 42;

    // Row 5
    private static final int SLOT_CRAFT_SELO = 48;
    private static final int SLOT_CANCEL = 53;

    private static final String GUI_TITLE_RAW = "Mesa do Mercador";

    // Slots onde o jogador pode colocar/retirar itens
    private static final Set<Integer> INPUT_SLOTS = Set.of(SLOT_ITEM_IN, SLOT_MAT_IN, SLOT_LIVRO);
    private static final Set<Integer> OUTPUT_SLOTS = Set.of(SLOT_ITEM_OUT, SLOT_POC_OUT, SLOT_POC_OUT2, SLOT_ENC_OUT);
    private static final Set<Integer> BUTTON_SLOTS = Set.of(SLOT_AVALIAR, SLOT_CRAFT_PERG, SLOT_POCAO, SLOT_ENCANTAR, SLOT_CRAFT_PEDRA, SLOT_ROUTES, SLOT_CRAFT_SELO, SLOT_CANCEL);

    // ═══════════════════════════════════════════════════
    // Blocos de ambiente do Mercador e seus bonus {bonusPorBloco, maxBlocos}
    // ═══════════════════════════════════════════════════

    private static final Map<Material, double[]> MERCHANT_BLOCKS = Map.ofEntries(
            Map.entry(Material.BARREL, new double[]{0.04, 10}),
            Map.entry(Material.CHEST, new double[]{0.03, 8}),
            Map.entry(Material.LECTERN, new double[]{0.06, 4}),
            Map.entry(Material.EMERALD_BLOCK, new double[]{0.10, 4}),
            Map.entry(Material.CARTOGRAPHY_TABLE, new double[]{0.05, 3})
    );

    // Thresholds minimos para mostrar indicador de eficiencia
    private static final Map<String, Integer> INDICATOR_THRESHOLDS = Map.of(
            "BARREL", 3,
            "CHEST", 2,
            "LECTERN", 1,
            "EMERALD_BLOCK", 1,
            "CARTOGRAPHY_TABLE", 1,
            "ITEM_FRAME", 4
    );

    // ═══════════════════════════════════════════════════
    // Breakpoints
    // ═══════════════════════════════════════════════════
    // <110%  BP0 "Ambulante"
    // 130%+  BP1 "Lojista"
    // 170%+  BP2 "Comerciante"
    // 210%+  BP3 "Negociante"
    // 250%+  BP4 "Magnata"
    // 290%+  BP5 "Barao do Comercio"

    // ═══════════════════════════════════════════════════
    // Records
    // ═══════════════════════════════════════════════════

    record MerchantEnvironment(Map<Material, Integer> blockCounts, int itemFrameCount,
                               double totalBonus, int nearbyPlayers, double playerBonus,
                               boolean alquimistaNearby, boolean ferreiroNearby, boolean guerreiroNearby,
                               boolean mercadorNearby, boolean enchantingTablePresent, boolean anvilPresent) {}

    record MerchantSession(Location tableLoc, MerchantEnvironment env,
                           double efficiency, int breakpoint, int playerLevel,
                           boolean alquimistaRowActive, boolean ferreiroRowActive) {}

    record CachedScan(MerchantEnvironment env, long timestamp) {}

    record NearbyClassInfo(int count, boolean mercador, boolean alquimista, boolean ferreiro, boolean guerreiro) {}

    // ═══════════════════════════════════════════════════
    // Construtor
    // ═══════════════════════════════════════════════════

    public MerchantTableListener(NemonicOrbPlugin plugin, OrbListener orbListener, MerchantRouteManager routeManager) {
        this.plugin = plugin;
        this.orbListener = orbListener;
        this.routeManager = routeManager;
    }

    private boolean debug() {
        return plugin.getConfig().getBoolean("debug", true);
    }

    // ═══════════════════════════════════════════════════
    // Deteccao de Classe via Reflexao (MMOCore)
    // ═══════════════════════════════════════════════════

    public boolean isMercador(Player player) {
        try {
            String className = getPlayerClassName(player);
            String configClassName = plugin.getConfig().getString("merchant.class-name", "Mercador");
            boolean result = className != null && className.equalsIgnoreCase(configClassName);
            if (debug()) plugin.getLogger().info("[MERCHANT] isMercador: jogador=" + player.getName()
                    + " classeDetectada='" + className + "' configClasse='" + configClassName + "' resultado=" + result);
            return result;
        } catch (Exception e) {
            plugin.getLogger().warning("[MERCHANT] Erro ao verificar classe: " + e.getMessage());
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
            plugin.getLogger().warning("[MERCHANT] Erro ao obter classe: " + e.getMessage());
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

    // ═══════════════════════════════════════════════════
    // Mana Management via Reflexao (MMOCore)
    // ═══════════════════════════════════════════════════

    private double getPlayerMana(Player player) {
        try {
            Class<?> pdClass = Class.forName("net.Indyuce.mmocore.api.player.PlayerData");
            Object data = getPlayerData(pdClass, player);
            if (data == null) return 0;
            for (String m : new String[]{"getMana"}) {
                try {
                    Object result = pdClass.getMethod(m).invoke(data);
                    if (result instanceof Number n) return n.doubleValue();
                } catch (NoSuchMethodException ignored) {}
            }
            return 0;
        } catch (Exception e) { return 0; }
    }

    private boolean deductPlayerMana(Player player, double amount) {
        try {
            Class<?> pdClass = Class.forName("net.Indyuce.mmocore.api.player.PlayerData");
            Object data = getPlayerData(pdClass, player);
            if (data == null) return false;

            double currentMana = getPlayerMana(player);
            if (currentMana < amount) return false;

            // Try giveMana (negative)
            try {
                pdClass.getMethod("giveMana", double.class).invoke(data, -amount);
                return true;
            } catch (NoSuchMethodException ignored) {}

            // Try setMana
            try {
                pdClass.getMethod("setMana", double.class).invoke(data, currentMana - amount);
                return true;
            } catch (NoSuchMethodException ignored) {}

            return false;
        } catch (Exception e) {
            plugin.getLogger().warning("[MERCHANT] Erro ao deduzir mana: " + e.getMessage());
            return false;
        }
    }

    // ═══════════════════════════════════════════════════
    // XP Management via Reflexao
    // ═══════════════════════════════════════════════════

    private int getPlayerXP(Player player) {
        try {
            Class<?> pdClass = Class.forName("net.Indyuce.mmocore.api.player.PlayerData");
            Object data = getPlayerData(pdClass, player);
            if (data == null) return 0;
            // Try getExperience first, then getExp
            for (String m : new String[]{"getExperience", "getExp"}) {
                try {
                    Object result = pdClass.getMethod(m).invoke(data);
                    if (result instanceof Number n) return n.intValue();
                } catch (NoSuchMethodException ignored) {}
            }
            return 0;
        } catch (Exception e) { return 0; }
    }

    private boolean deductPlayerXP(Player player, int amount) {
        try {
            Class<?> pdClass = Class.forName("net.Indyuce.mmocore.api.player.PlayerData");
            Object data = getPlayerData(pdClass, player);
            if (data == null) return false;

            int currentXP = getPlayerXP(player);
            if (currentXP < amount) return false;

            // Try giveExperience with negative, or setExperience
            for (String m : new String[]{"giveExperience"}) {
                try {
                    // giveExperience(double, EXPSource) or giveExperience(int)
                    try {
                        pdClass.getMethod(m, double.class, null).invoke(data, (double) -amount, null);
                        return true;
                    } catch (Exception ignored) {}
                    try {
                        pdClass.getMethod(m, int.class).invoke(data, -amount);
                        return true;
                    } catch (Exception ignored) {}
                } catch (Exception ignored) {}
            }

            // Fallback: try setExperience
            for (String m : new String[]{"setExperience", "setExp"}) {
                try {
                    Method method = pdClass.getMethod(m, double.class);
                    method.invoke(data, (double) (currentXP - amount));
                    return true;
                } catch (NoSuchMethodException ignored) {}
                try {
                    Method method = pdClass.getMethod(m, int.class);
                    method.invoke(data, currentXP - amount);
                    return true;
                } catch (NoSuchMethodException ignored) {}
            }
            return false;
        } catch (Exception e) {
            plugin.getLogger().warning("[MERCHANT] Erro ao deduzir XP: " + e.getMessage());
            return false;
        }
    }

    // ═══════════════════════════════════════════════════
    // Environment Scanning
    // ═══════════════════════════════════════════════════

    private MerchantEnvironment scanEnvironment(Location loc, Player player) {
        String cacheKey = loc.getWorld().getName() + "," + loc.getBlockX() + "," + loc.getBlockY() + "," + loc.getBlockZ();
        long cacheTTL = plugin.getConfig().getLong("merchant.scan-cache-seconds", 60) * 1000;
        CachedScan cached = scanCache.get(cacheKey);
        if (cached != null && (System.currentTimeMillis() - cached.timestamp()) < cacheTTL) {
            MerchantEnvironment old = cached.env();
            NearbyClassInfo classInfo = scanNearbyClasses(loc, player);
            double playerBonus = Math.min(classInfo.count * 0.05, 0.25);
            return new MerchantEnvironment(old.blockCounts(), old.itemFrameCount(),
                    old.totalBonus(), classInfo.count, playerBonus,
                    classInfo.alquimista, classInfo.ferreiro, classInfo.guerreiro,
                    classInfo.mercador, old.enchantingTablePresent(), old.anvilPresent());
        }

        int radius = plugin.getConfig().getInt("merchant.scan-radius", 20);
        int mesaRadius = plugin.getConfig().getInt("merchant.mesa-detection-radius", 4);
        Map<Material, Integer> blockCounts = new EnumMap<>(Material.class);
        World world = loc.getWorld();
        int cx = loc.getBlockX(), cy = loc.getBlockY(), cz = loc.getBlockZ();

        boolean enchantingTablePresent = false;
        boolean anvilPresent = false;

        // Scan geral para blocos de eficiencia (raio grande)
        for (int x = cx - radius; x <= cx + radius; x++) {
            for (int y = Math.max(world.getMinHeight(), cy - radius); y <= Math.min(world.getMaxHeight() - 1, cy + radius); y++) {
                for (int z = cz - radius; z <= cz + radius; z++) {
                    double distSq = (x - cx) * (x - cx) + (y - cy) * (y - cy) + (z - cz) * (z - cz);
                    if (distSq > radius * radius) continue;

                    Material mat = world.getBlockAt(x, y, z).getType();

                    if (MERCHANT_BLOCKS.containsKey(mat)) {
                        blockCounts.merge(mat, 1, Integer::sum);
                    }
                }
            }
        }

        // Scan separado para mesas de outras classes (raio curto = 4 blocos)
        for (int x = cx - mesaRadius; x <= cx + mesaRadius; x++) {
            for (int y = Math.max(world.getMinHeight(), cy - mesaRadius); y <= Math.min(world.getMaxHeight() - 1, cy + mesaRadius); y++) {
                for (int z = cz - mesaRadius; z <= cz + mesaRadius; z++) {
                    double distSq = (x - cx) * (x - cx) + (y - cy) * (y - cy) + (z - cz) * (z - cz);
                    if (distSq > mesaRadius * mesaRadius) continue;

                    Material mat = world.getBlockAt(x, y, z).getType();

                    if (mat == Material.ENCHANTING_TABLE) {
                        enchantingTablePresent = true;
                    }
                    if (mat == Material.ANVIL || mat == Material.CHIPPED_ANVIL || mat == Material.DAMAGED_ANVIL) {
                        anvilPresent = true;
                    }
                }
            }
        }

        // Scan entity: Item Frames
        int itemFrameCount = 0;
        int maxItemFrames = 12;
        for (Entity entity : world.getNearbyEntities(loc, radius, radius, radius)) {
            if (entity instanceof ItemFrame frame) {
                ItemStack frameItem = frame.getItem();
                if (frameItem != null && !frameItem.getType().isAir()) {
                    itemFrameCount++;
                    if (itemFrameCount >= maxItemFrames) break;
                }
            }
        }

        // Calculate total block bonus
        double totalBonus = 0;
        for (Map.Entry<Material, Integer> entry : blockCounts.entrySet()) {
            double[] cfg = MERCHANT_BLOCKS.get(entry.getKey());
            if (cfg == null) continue;
            int count = Math.min(entry.getValue(), (int) cfg[1]);
            totalBonus += count * cfg[0];
        }

        NearbyClassInfo classInfo = scanNearbyClasses(loc, player);
        double playerBonus = Math.min(classInfo.count * 0.05, 0.25);

        MerchantEnvironment env = new MerchantEnvironment(blockCounts, itemFrameCount,
                totalBonus, classInfo.count, playerBonus,
                classInfo.alquimista, classInfo.ferreiro, classInfo.guerreiro,
                classInfo.mercador, enchantingTablePresent, anvilPresent);
        scanCache.put(cacheKey, new CachedScan(env, System.currentTimeMillis()));

        if (debug()) plugin.getLogger().info("[MERCHANT-SCAN] bonus=" + String.format("%.0f%%", totalBonus * 100)
                + " itemFrames=" + itemFrameCount
                + " jogadores=" + classInfo.count + " (+=" + String.format("%.0f%%", playerBonus * 100) + ")"
                + " alquimista=" + classInfo.alquimista + " ferreiro=" + classInfo.ferreiro
                + " guerreiro=" + classInfo.guerreiro + " mercador=" + classInfo.mercador
                + " enchTable=" + enchantingTablePresent + " anvil=" + anvilPresent);

        return env;
    }

    private NearbyClassInfo scanNearbyClasses(Location loc, Player exclude) {
        int radius = plugin.getConfig().getInt("merchant.scan-radius", 20);
        int count = 0;
        boolean mercador = false, alquimista = false, ferreiro = false, guerreiro = false;
        String mercadorClass = plugin.getConfig().getString("merchant.class-name", "Mercador");
        String alquimistaClass = plugin.getConfig().getString("transmutation.class-name", "Alquimista");
        String ferreiroClass = plugin.getConfig().getString("blacksmith.class-name", "Ferreiro");
        String guerreiroClass = plugin.getConfig().getString("merchant.guerreiro-class-name", "Guerreiro");

        for (Player p : loc.getWorld().getPlayers()) {
            if (p.equals(exclude)) continue;
            if (p.getLocation().distanceSquared(loc) > radius * radius) continue;
            count++;
            String cls = getPlayerClassName(p);
            if (cls == null) continue;
            if (mercadorClass.equalsIgnoreCase(cls)) mercador = true;
            if (alquimistaClass.equalsIgnoreCase(cls)) alquimista = true;
            if (ferreiroClass.equalsIgnoreCase(cls)) ferreiro = true;
            if (guerreiroClass.equalsIgnoreCase(cls)) guerreiro = true;
        }
        return new NearbyClassInfo(count, mercador, alquimista, ferreiro, guerreiro);
    }

    // ═══════════════════════════════════════════════════
    // Eficiencia e Breakpoints
    // ═══════════════════════════════════════════════════

    private double calculateEfficiency(MerchantEnvironment env) {
        double eff = 1.0 + env.totalBonus() + (env.itemFrameCount() * 0.02) + env.playerBonus();
        if (env.alquimistaNearby()) eff += 0.08;
        if (env.guerreiroNearby()) eff -= 0.03;
        if (env.mercadorNearby()) eff -= 0.05;
        return eff;
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

    // ═══════════════════════════════════════════════════
    // Abertura da GUI
    // ═══════════════════════════════════════════════════

    public void openGUI(Player player, Location tableLoc) {
        int minLevel = plugin.getConfig().getInt("merchant.min-level", 5);
        int playerLevel = getPlayerLevel(player);

        if (playerLevel < minLevel) {
            player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                    plugin.getConfig().getString("merchant.messages.level-too-low",
                            "&c[Mercador] Voce precisa de nivel %level% na classe Mercador!")
                            .replace("%level%", String.valueOf(minLevel))));
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
            return;
        }

        // Escanear ambiente
        MerchantEnvironment env = scanEnvironment(tableLoc, player);
        double efficiency = calculateEfficiency(env);
        int breakpoint = getBreakpoint(efficiency);

        boolean alquimistaRowActive = env.enchantingTablePresent();
        boolean ferreiroRowActive = env.anvilPresent();

        Inventory gui = Bukkit.createInventory(null, GUI_SIZE,
                Component.text(GUI_TITLE_RAW, NamedTextColor.GOLD, TextDecoration.BOLD));

        // Preencher tudo com vidro preto
        ItemStack blackGlass = createGlassPane(Material.BLACK_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < GUI_SIZE; i++) {
            gui.setItem(i, blackGlass);
        }

        ItemStack arrowPane = createArrowPane();

        // === ROW 0: Eficiencia + Info + Rotas ===
        gui.setItem(SLOT_EFFICIENCY, createEfficiencyItem(env, efficiency, playerLevel));
        gui.setItem(SLOT_INFO, createInfoItem(alquimistaRowActive, ferreiroRowActive));
        if (playerLevel >= 10) {
            gui.setItem(SLOT_ROUTES, createRoutesButton());
        }

        // === ROW 1: Avaliacao ===
        gui.setItem(SLOT_ITEM_IN, null);    // Empty input slot
        gui.setItem(SLOT_ARROW_AV_1, arrowPane);
        gui.setItem(SLOT_AVALIAR, createAvaliarButton());
        gui.setItem(SLOT_ARROW_AV_2, arrowPane);
        gui.setItem(SLOT_ITEM_OUT, null);   // Empty output slot
        gui.setItem(SLOT_CRAFT_PERG, createCraftPergButton());

        // === ROW 2: Separador dourado ===
        ItemStack goldGlass = createGlassPane(Material.YELLOW_STAINED_GLASS_PANE, " ");
        for (int i = 18; i <= 26; i++) {
            gui.setItem(i, goldGlass);
        }

        // === ROW 3: Pocao de Retorno (requer Mesa de Transmutacao proxima) ===
        if (alquimistaRowActive) {
            gui.setItem(SLOT_MAT_IN, null);   // Empty input for materials
            gui.setItem(SLOT_ARROW_POC_1, arrowPane);
            gui.setItem(SLOT_POCAO, createPocaoButton());
            gui.setItem(SLOT_ARROW_POC_2, arrowPane);
            gui.setItem(SLOT_POC_OUT, null);  // Output 1
            gui.setItem(SLOT_POC_OUT2, null); // Output 2
        } else {
            ItemStack grayGlass = createGlassPane(Material.GRAY_STAINED_GLASS_PANE, " ");
            ItemStack lockedGlass = createLockedRowPane("Requer Mesa de Transmutacao proxima");
            for (int i = 27; i <= 35; i++) {
                gui.setItem(i, grayGlass);
            }
            gui.setItem(SLOT_POCAO, lockedGlass);
        }

        // === ROW 4: Encantamento (requer Mesa do Ferreiro proxima) ===
        if (ferreiroRowActive) {
            // Label para o slot de livro
            ItemStack bookLabel = createGlassPane(Material.PURPLE_STAINED_GLASS_PANE, " ");
            ItemMeta blMeta = bookLabel.getItemMeta();
            if (blMeta != null) {
                blMeta.displayName(Component.text("Coloque o Livro Encantado abaixo", NamedTextColor.LIGHT_PURPLE));
                List<Component> blLore = new ArrayList<>();
                blLore.add(Component.text("O equipamento vai no slot", NamedTextColor.GRAY));
                blLore.add(Component.text("da Linha 1 (ITEM_IN)", NamedTextColor.GRAY));
                blMeta.lore(blLore);
                bookLabel.setItemMeta(blMeta);
            }
            gui.setItem(36, bookLabel); // Slot antes do LIVRO para indicar
            gui.setItem(SLOT_LIVRO, null);    // Empty input for enchanted book
            gui.setItem(SLOT_ARROW_ENC_1, arrowPane);
            gui.setItem(SLOT_ENCANTAR, createEncantarButton());
            gui.setItem(SLOT_ARROW_ENC_2, arrowPane);
            gui.setItem(SLOT_ENC_OUT, null);  // Output
            gui.setItem(SLOT_CRAFT_PEDRA, createCraftPedraButton());
        } else {
            ItemStack grayGlass = createGlassPane(Material.GRAY_STAINED_GLASS_PANE, " ");
            ItemStack lockedGlass = createLockedRowPane("Requer Mesa do Ferreiro proxima");
            for (int i = 36; i <= 44; i++) {
                gui.setItem(i, grayGlass);
            }
            gui.setItem(SLOT_ENCANTAR, lockedGlass);
        }

        // === ROW 5: Selo + Cancel ===
        if (playerLevel >= 10) {
            gui.setItem(SLOT_CRAFT_SELO, createCraftSeloButton());
        }
        gui.setItem(SLOT_CANCEL, createCancelButton());

        // Registrar sessao
        MerchantSession session = new MerchantSession(tableLoc, env, efficiency, breakpoint, playerLevel,
                alquimistaRowActive, ferreiroRowActive);
        activeSessions.put(player.getUniqueId(), session);

        player.openInventory(gui);
        player.playSound(tableLoc, Sound.BLOCK_ENCHANTMENT_TABLE_USE, 0.8f, 0.5f);

        // Iniciar aura visual
        startAura(player, tableLoc, efficiency);

        if (debug()) plugin.getLogger().info("[MERCHANT] GUI aberta para " + player.getName()
                + " nivel=" + playerLevel + " eficiencia=" + String.format("%.0f%%", efficiency * 100)
                + " breakpoint=" + breakpoint + " (" + getBreakpointName(breakpoint) + ")"
                + " alqRow=" + alquimistaRowActive + " ferRow=" + ferreiroRowActive);
    }

    // ═══════════════════════════════════════════════════
    // Event Handlers
    // ═══════════════════════════════════════════════════

    @EventHandler(priority = EventPriority.HIGH)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        String title = getInventoryTitle(event.getView());
        if (!GUI_TITLE_RAW.equals(title)) return;

        MerchantSession session = activeSessions.get(player.getUniqueId());
        if (session == null) {
            event.setCancelled(true);
            return;
        }

        int slot = event.getRawSlot();
        Inventory topInv = event.getView().getTopInventory();

        // Hard-block de click types que podem puxar itens da GUI (dupe/exploit)
        if (event.getClick() == ClickType.DOUBLE_CLICK
                || event.getClick() == ClickType.SWAP_OFFHAND
                || event.getAction() == InventoryAction.COLLECT_TO_CURSOR) {
            event.setCancelled(true);
            return;
        }

        // Block number-key swaps targeting non-input GUI slots (anti-dupe)
        if (event.getHotbarButton() >= 0 && slot < GUI_SIZE) {
            if (slot != SLOT_ITEM_IN
                    && !(slot == SLOT_MAT_IN && session.alquimistaRowActive())
                    && !(slot == SLOT_LIVRO && session.ferreiroRowActive())) {
                event.setCancelled(true);
                return;
            }
        }

        // Clique no inventario do jogador
        if (slot >= GUI_SIZE) {
            if (event.isShiftClick()) {
                event.setCancelled(true);
                ItemStack clicked = event.getCurrentItem();
                if (clicked == null || clicked.getType().isAir()) return;

                // Tentar mover para slots de input disponiveis
                if (isSlotEmpty(topInv, SLOT_ITEM_IN)) {
                    topInv.setItem(SLOT_ITEM_IN, clicked.clone());
                    event.setCurrentItem(null);
                    return;
                }
                if (session.alquimistaRowActive() && isSlotEmpty(topInv, SLOT_MAT_IN)) {
                    topInv.setItem(SLOT_MAT_IN, clicked.clone());
                    event.setCurrentItem(null);
                    return;
                }
                if (session.ferreiroRowActive() && isSlotEmpty(topInv, SLOT_LIVRO)) {
                    topInv.setItem(SLOT_LIVRO, clicked.clone());
                    event.setCurrentItem(null);
                    return;
                }
            }
            return;
        }

        // Input slots — permitir interacao se linha ativa
        if (slot == SLOT_ITEM_IN) return;
        if (slot == SLOT_MAT_IN && session.alquimistaRowActive()) return;
        if (slot == SLOT_LIVRO && session.ferreiroRowActive()) return;

        // Output slots — permitir retirar itens reais (shift-click seguro)
        if (OUTPUT_SLOTS.contains(slot)) {
            // Bloquear se a linha esta inativa
            if ((slot == SLOT_POC_OUT || slot == SLOT_POC_OUT2) && !session.alquimistaRowActive()) {
                event.setCancelled(true);
                return;
            }
            if (slot == SLOT_ENC_OUT && !session.ferreiroRowActive()) {
                event.setCancelled(true);
                return;
            }
            ItemStack outputItem = topInv.getItem(slot);
            if (outputItem != null && !outputItem.getType().isAir() && !isGUIDecoration(outputItem)) {
                if (event.isShiftClick()) {
                    // Handle shift-click manually to prevent dupe
                    event.setCancelled(true);
                    topInv.setItem(slot, null);
                    Map<Integer, ItemStack> overflow = player.getInventory().addItem(outputItem);
                    for (ItemStack drop : overflow.values()) {
                        player.getWorld().dropItemNaturally(player.getLocation(), drop);
                    }
                    syncInventoryLater(player);
                }
                syncInventoryLater(player);
                return;
            }
            event.setCancelled(true);
            return;
        }

        // Tudo o resto e bloqueado
        event.setCancelled(true);

        // Botoes
        if (BUTTON_SLOTS.contains(slot)) {
            switch (slot) {
                case SLOT_AVALIAR -> handleAvaliacao(player, topInv, session);
                case SLOT_CRAFT_PERG -> handleCraftPergaminho(player, topInv, session);
                case SLOT_POCAO -> {
                    if (session.alquimistaRowActive()) handleCraftPocao(player, topInv, session);
                    else sendLockedMessage(player, "Mesa de Transmutacao");
                }
                case SLOT_ENCANTAR -> {
                    if (session.ferreiroRowActive()) handleEncantar(player, topInv, session);
                    else sendLockedMessage(player, "Mesa do Ferreiro");
                }
                case SLOT_CRAFT_PEDRA -> {
                    if (session.ferreiroRowActive()) handleCraftPedra(player, topInv, session);
                    else sendLockedMessage(player, "Mesa do Ferreiro");
                }
                case SLOT_ROUTES -> handleRoutes(player, session);
                case SLOT_CRAFT_SELO -> handleCraftSelo(player, session);
                case SLOT_CANCEL -> player.closeInventory();
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onInventoryDrag(InventoryDragEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        String title = getInventoryTitle(event.getView());
        if (!GUI_TITLE_RAW.equals(title)) return;
        MerchantSession session = activeSessions.get(player.getUniqueId());
        if (session == null) {
            event.setCancelled(true);
            return;
        }

        Set<Integer> allowed = new HashSet<>();
        allowed.add(SLOT_ITEM_IN);
        if (session.alquimistaRowActive()) allowed.add(SLOT_MAT_IN);
        if (session.ferreiroRowActive()) allowed.add(SLOT_LIVRO);

        for (int slot : event.getRawSlots()) {
            if (slot < GUI_SIZE && !allowed.contains(slot)) {
                event.setCancelled(true);
                return;
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player player)) return;

        String title = getInventoryTitle(event.getView());

        // Routes GUI fechando — nao remover sessao, pode estar voltando para GUI principal
        if (ROUTES_GUI_TITLE.equals(title)) {
            // Verificar apos 2 ticks se o jogador ainda tem alguma GUI do mercador aberta
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (!player.isOnline()) {
                    MerchantSession s = activeSessions.remove(player.getUniqueId());
                    if (s != null) stopAura(player);
                    return;
                }
                String openTitle = getInventoryTitle(player.getOpenInventory());
                if (!GUI_TITLE_RAW.equals(openTitle) && !ROUTES_GUI_TITLE.equals(openTitle)) {
                    MerchantSession s = activeSessions.remove(player.getUniqueId());
                    if (s != null) stopAura(player);
                }
            }, 2L);
            return;
        }

        if (!GUI_TITLE_RAW.equals(title)) return;

        MerchantSession session = activeSessions.remove(player.getUniqueId());
        if (session == null) return;

        stopAura(player);

        Inventory inv = event.getView().getTopInventory();
        // Devolver itens de input e output (ignorar itens decorativos da GUI)
        // Clear slot BEFORE returning to prevent duplication on fast close
        int[] returnSlots = {SLOT_ITEM_IN, SLOT_ITEM_OUT, SLOT_MAT_IN, SLOT_POC_OUT, SLOT_POC_OUT2,
                SLOT_LIVRO, SLOT_ENC_OUT};
        for (int slot : returnSlots) {
            ItemStack item = inv.getItem(slot);
            inv.setItem(slot, null); // Clear first to prevent dupe
            if (item != null && !item.getType().isAir() && !isGUIDecoration(item)) {
                Map<Integer, ItemStack> leftover = player.getInventory().addItem(item);
                for (ItemStack drop : leftover.values()) {
                    player.getWorld().dropItemNaturally(player.getLocation(), drop);
                }
            }
        }
    }

    // ═══════════════════════════════════════════════════
    // Funcao 1: Avaliacao (slot 12)
    // ═══════════════════════════════════════════════════

    private void handleAvaliacao(Player player, Inventory gui, MerchantSession session) {
        int minLevel = plugin.getConfig().getInt("merchant.min-level", 5);
        if (session.playerLevel() < minLevel) {
            player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                    "&c[Mercador] Voce precisa de nivel " + minLevel + " para avaliar!"));
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
            return;
        }

        // Check cooldown
        long cooldownMs = plugin.getConfig().getLong("merchant.avaliacao-cooldown-seconds", 15) * 1000;
        Long lastUse = avaliacaoCooldowns.get(player.getUniqueId());
        long now = System.currentTimeMillis();
        if (lastUse != null && (now - lastUse) < cooldownMs) {
            long remaining = (cooldownMs - (now - lastUse)) / 1000;
            player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                    "&c[Mercador] Aguarde " + remaining + "s antes de avaliar novamente!"));
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
            return;
        }

        // XP cost removed — avaliacao is free

        // Check item in slot 10
        ItemStack input = gui.getItem(SLOT_ITEM_IN);
        if (input == null || input.getType().isAir()) {
            player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                    "&c[Mercador] Coloque um item no slot de entrada!"));
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
            return;
        }

        // Check output slot empty
        ItemStack existingOut = gui.getItem(SLOT_ITEM_OUT);
        if (existingOut != null && !existingOut.getType().isAir()) {
            player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                    "&c[Mercador] Retire o item do slot de saida primeiro!"));
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
            return;
        }

        NBTItem nbt = NBTItem.get(input);

        // Must be MMOItem with NEMONICORB_TIER
        if (!nbt.hasTag(OrbListener.NBT_TIER)) {
            player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                    "&c[Mercador] Este item nao pode ser avaliado!"));
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
            return;
        }

        // Must be unidentified: NBT_IDENTIFIED == 0 or no tag
        int identified = nbt.hasTag(OrbListener.NBT_IDENTIFIED) ? nbt.getInteger(OrbListener.NBT_IDENTIFIED) : 0;
        if (identified != 0) {
            player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                    "&c[Mercador] Este item ja foi avaliado/identificado!"));
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
            return;
        }

        // Clone item and process
        ItemStack result = input.clone();
        NBTItem resultNbt = NBTItem.get(result);

        int revealerLevel = Math.max(1, session.playerLevel());
        int itemLevelBefore = getItemDefinitionLevel(resultNbt);
        int cappedLevel = Math.max(1, Math.min(itemLevelBefore, revealerLevel));

        // Set identified
        resultNbt.addTag(new ItemTag(OrbListener.NBT_IDENTIFIED, 1));
        resultNbt.addTag(new ItemTag(OrbListener.NBT_CRAFT_LEVEL, cappedLevel));

        // Boost mod values based on efficiency
        double eff = session.efficiency();
        if (eff >= 1.50 && resultNbt.hasTag(OrbListener.NBT_MODS)) {
            String modsJson = resultNbt.getString(OrbListener.NBT_MODS);
            try {
                java.lang.reflect.Type listType = new TypeToken<List<Map<String, Object>>>() {}.getType();
                List<Map<String, Object>> mods = GSON.fromJson(modsJson, listType);

                if (mods != null && !mods.isEmpty()) {
                    double multiplier;
                    if (eff >= 2.50) {
                        multiplier = 1.30;
                    } else if (eff >= 2.00) {
                        multiplier = 1.20;
                    } else {
                        multiplier = 1.10;
                    }

                    for (Map<String, Object> mod : mods) {
                        // Boost "value" field if present
                        if (mod.containsKey("value") && mod.get("value") instanceof Number) {
                            double oldVal = ((Number) mod.get("value")).doubleValue();
                            double newVal = Math.round(oldVal * multiplier * 100.0) / 100.0;
                            mod.put("value", newVal);
                        }

                        // Also boost values inside "stats" map if present
                        if (mod.containsKey("stats") && mod.get("stats") instanceof Map) {
                            @SuppressWarnings("unchecked")
                            Map<String, Object> stats = (Map<String, Object>) mod.get("stats");
                            for (Map.Entry<String, Object> statEntry : stats.entrySet()) {
                                if (statEntry.getValue() instanceof Number) {
                                    double oldVal = ((Number) statEntry.getValue()).doubleValue();
                                    double newVal = Math.round(oldVal * multiplier * 100.0) / 100.0;
                                    stats.put(statEntry.getKey(), newVal);
                                }
                            }
                        }
                    }

                    // 250%+: 15% chance of extra mod
                    if (eff >= 2.50 && ThreadLocalRandom.current().nextDouble() < 0.15) {
                        // Duplicate a random existing mod with reduced values
                        Map<String, Object> randomMod = mods.get(ThreadLocalRandom.current().nextInt(mods.size()));
                        Map<String, Object> extraMod = new LinkedHashMap<>(randomMod);
                        // Reduce the extra mod values by 50%
                        if (extraMod.containsKey("value") && extraMod.get("value") instanceof Number) {
                            double val = ((Number) extraMod.get("value")).doubleValue();
                            extraMod.put("value", Math.round(val * 0.5 * 100.0) / 100.0);
                        }
                        if (extraMod.containsKey("stats") && extraMod.get("stats") instanceof Map) {
                            @SuppressWarnings("unchecked")
                            Map<String, Object> stats = new LinkedHashMap<>((Map<String, Object>) extraMod.get("stats"));
                            for (Map.Entry<String, Object> statEntry : stats.entrySet()) {
                                if (statEntry.getValue() instanceof Number) {
                                    double val = ((Number) statEntry.getValue()).doubleValue();
                                    stats.put(statEntry.getKey(), Math.round(val * 0.5 * 100.0) / 100.0);
                                }
                            }
                            extraMod.put("stats", stats);
                        }
                        extraMod.put("id", randomMod.get("id") + "_bonus");
                        mods.add(extraMod);

                        player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                                "&6[Mercador] &eMod bonus adicionado pela eficiencia excepcional!"));
                    }

                    resultNbt.addTag(new ItemTag(OrbListener.NBT_MODS, GSON.toJson(mods)));
                }
            } catch (Exception e) {
                if (debug()) plugin.getLogger().warning("[MERCHANT] Erro ao processar mods: " + e.getMessage());
            }
        }

        // Build final item
        result = resultNbt.toItem();

        // Update display via OrbListener
        NBTItem finalNbt = NBTItem.get(result);
        orbListener.updateItemDisplay(result, finalNbt);

        // Place result in output, clear input
        gui.setItem(SLOT_ITEM_OUT, result);
        gui.setItem(SLOT_ITEM_IN, null);

        avaliacaoCooldowns.put(player.getUniqueId(), now);

        // Effects
        Location tableLoc = session.tableLoc();
        player.playSound(tableLoc, Sound.BLOCK_ENCHANTMENT_TABLE_USE, 1.0f, 1.2f);
        player.playSound(tableLoc, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.8f, 1.5f);
        tableLoc.getWorld().spawnParticle(Particle.ENCHANT,
                tableLoc.clone().add(0.5, 1.5, 0.5), 40, 0.3, 0.3, 0.3, 1.0);

        player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                "&a[Mercador] Item avaliado com sucesso! (" + String.format("%.0f%%", session.efficiency() * 100) + " eficiencia)"));

        if (debug()) plugin.getLogger().info("[MERCHANT-AVALIAR] " + player.getName()
                + " avaliou item com eficiencia " + String.format("%.0f%%", session.efficiency() * 100));
        if (debug() && cappedLevel != itemLevelBefore) {
            plugin.getLogger().info("[MERCHANT-AVALIAR-CAP] " + player.getName()
                    + " revelou item nivel " + itemLevelBefore + " -> cap " + cappedLevel);
        }
    }

    private int getItemDefinitionLevel(NBTItem nbt) {
        if (nbt.hasTag(OrbListener.NBT_CRAFT_LEVEL)) return nbt.getInteger(OrbListener.NBT_CRAFT_LEVEL);
        if (nbt.hasTag("MMOITEMS_ITEM_LEVEL")) return nbt.getInteger("MMOITEMS_ITEM_LEVEL");
        if (nbt.hasTag("MMOITEMS_UPGRADE_LEVEL")) return nbt.getInteger("MMOITEMS_UPGRADE_LEVEL");
        return 1;
    }

    // ═══════════════════════════════════════════════════
    // Funcao 2: Craft Pergaminho (slot 16)
    // ═══════════════════════════════════════════════════

    private void handleCraftPergaminho(Player player, Inventory gui, MerchantSession session) {
        int minLevel = plugin.getConfig().getInt("merchant.min-level", 5);
        if (session.playerLevel() < minLevel) {
            player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                    "&c[Mercador] Voce precisa de nivel " + minLevel + "!"));
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
            return;
        }

        // Check cooldown
        long cooldownMs = plugin.getConfig().getLong("merchant.craft-cooldown-seconds", 30) * 1000;
        Long lastUse = craftPergCooldowns.get(player.getUniqueId());
        long now = System.currentTimeMillis();
        if (lastUse != null && (now - lastUse) < cooldownMs) {
            long remaining = (cooldownMs - (now - lastUse)) / 1000;
            player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                    "&c[Mercador] Aguarde " + remaining + "s antes de fabricar novamente!"));
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
            return;
        }

        // Check materials: 4x PAPER, 2x GLOW_INK_SAC, 1x LAPIS_LAZULI
        if (!hasMaterials(player, Material.PAPER, 4)) {
            player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                    "&c[Mercador] Voce precisa de 4x Papel!"));
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
            return;
        }
        if (!hasMaterials(player, Material.GLOW_INK_SAC, 2)) {
            player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                    "&c[Mercador] Voce precisa de 2x Saco de Tinta Brilhante!"));
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
            return;
        }
        if (!hasMaterials(player, Material.LAPIS_LAZULI, 1)) {
            player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                    "&c[Mercador] Voce precisa de 1x Lapis Lazuli!"));
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
            return;
        }

        // Consume materials
        consumeMaterials(player, Material.PAPER, 4);
        consumeMaterials(player, Material.LAPIS_LAZULI, 1);

        // 250%+ and 10% chance: don't consume Glow Ink Sac
        boolean preserveInk = session.efficiency() >= 2.50 && ThreadLocalRandom.current().nextDouble() < 0.10;
        if (!preserveInk) {
            consumeMaterials(player, Material.GLOW_INK_SAC, 2);
        } else {
            player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                    "&6[Mercador] &eA eficiencia preservou o Saco de Tinta Brilhante!"));
        }

        // Calculate output quantity by efficiency
        double eff = session.efficiency();
        int outputAmount;
        if (eff >= 2.50) outputAmount = 5;
        else if (eff >= 2.00) outputAmount = 4;
        else if (eff >= 1.50) outputAmount = 3;
        else outputAmount = 2;

        // Create pergaminho
        ItemStack pergaminho = createMMOItem("CONSUMABLE", "PERGAMINHO_DE_IDENTIFICACAO");
        if (pergaminho == null) {
            player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                    "&c[Mercador] Erro ao criar Pergaminho!"));
            return;
        }

        // Give to player
        for (int i = 0; i < outputAmount; i++) {
            ItemStack single = pergaminho.clone();
            single.setAmount(1);
            Map<Integer, ItemStack> leftover = player.getInventory().addItem(single);
            for (ItemStack drop : leftover.values()) {
                player.getWorld().dropItemNaturally(player.getLocation(), drop);
            }
        }

        craftPergCooldowns.put(player.getUniqueId(), now);

        // Effects
        Location tableLoc = session.tableLoc();
        player.playSound(tableLoc, Sound.BLOCK_ENCHANTMENT_TABLE_USE, 1.0f, 1.0f);
        tableLoc.getWorld().spawnParticle(Particle.ENCHANT,
                tableLoc.clone().add(0.5, 1.5, 0.5), 30, 0.3, 0.3, 0.3, 1.0);

        player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                "&a[Mercador] Fabricacao concluida! " + outputAmount + " Pergaminho(s) de Identificacao."));

        if (debug()) plugin.getLogger().info("[MERCHANT-CRAFT-PERG] " + player.getName()
                + " fabricou " + outputAmount + "x Pergaminho");
    }

    // ═══════════════════════════════════════════════════
    // Funcao 3: Craft Pocao de Retorno (slot 30)
    // ═══════════════════════════════════════════════════

    private void handleCraftPocao(Player player, Inventory gui, MerchantSession session) {
        if (!session.alquimistaRowActive()) {
            sendLockedMessage(player, "Mesa de Transmutacao");
            return;
        }

        // Check cooldown: 60s
        long cooldownMs = plugin.getConfig().getLong("merchant.pocao-cooldown-seconds", 60) * 1000;
        Long lastUse = craftPocaoCooldowns.get(player.getUniqueId());
        long now = System.currentTimeMillis();
        if (lastUse != null && (now - lastUse) < cooldownMs) {
            long remaining = (cooldownMs - (now - lastUse)) / 1000;
            player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                    "&c[Mercador] Aguarde " + remaining + "s antes de fabricar novamente!"));
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
            return;
        }

        // Check materials: 4x ENDER_PEARL, 8x REDSTONE, 1x COMPASS
        if (!hasMaterials(player, Material.ENDER_PEARL, 4)) {
            player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                    "&c[Mercador] Voce precisa de 4x Ender Pearl!"));
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
            return;
        }
        if (!hasMaterials(player, Material.REDSTONE, 8)) {
            player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                    "&c[Mercador] Voce precisa de 8x Redstone!"));
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
            return;
        }
        if (!hasMaterials(player, Material.COMPASS, 1)) {
            player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                    "&c[Mercador] Voce precisa de 1x Bussola!"));
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
            return;
        }

        // Check output slots
        ItemStack out1 = gui.getItem(SLOT_POC_OUT);
        ItemStack out2 = gui.getItem(SLOT_POC_OUT2);
        if ((out1 != null && !out1.getType().isAir()) && (out2 != null && !out2.getType().isAir())) {
            player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                    "&c[Mercador] Retire os itens dos slots de saida primeiro!"));
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
            return;
        }

        // Consume materials
        consumeMaterials(player, Material.ENDER_PEARL, 4);
        consumeMaterials(player, Material.REDSTONE, 8);
        consumeMaterials(player, Material.COMPASS, 1);

        // Create Pocao de Retorno (item custom do NemonicOrbPlugin)
        ItemStack pocao = createRecallPotion();

        // Place in output slots
        if (out1 == null || out1.getType().isAir()) {
            gui.setItem(SLOT_POC_OUT, pocao);
        } else {
            gui.setItem(SLOT_POC_OUT2, pocao);
        }

        craftPocaoCooldowns.put(player.getUniqueId(), now);

        // Effects
        Location tableLoc = session.tableLoc();
        player.playSound(tableLoc, Sound.BLOCK_BREWING_STAND_BREW, 1.0f, 1.0f);
        tableLoc.getWorld().spawnParticle(Particle.WITCH,
                tableLoc.clone().add(0.5, 1.5, 0.5), 30, 0.3, 0.5, 0.3, 0.1);

        player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                "&a[Mercador] Pocao de Retorno fabricada com sucesso!"));

        if (debug()) plugin.getLogger().info("[MERCHANT-CRAFT-POCAO] " + player.getName()
                + " fabricou Pocao de Retorno");
    }

    // ═══════════════════════════════════════════════════
    // Funcao 4: Aplicar Encantamento (slot 39)
    // ═══════════════════════════════════════════════════

    private void handleEncantar(Player player, Inventory gui, MerchantSession session) {
        if (!session.ferreiroRowActive()) {
            sendLockedMessage(player, "Mesa do Ferreiro");
            return;
        }

        int enchantMinLevel = plugin.getConfig().getInt("merchant.enchant-min-level", 35);
        if (session.playerLevel() < enchantMinLevel) {
            player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                    "&c[Mercador] Voce precisa de nivel " + enchantMinLevel + " para encantar!"));
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
            return;
        }

        // Check cooldown
        long cooldownMs = plugin.getConfig().getLong("merchant.enchant-cooldown-seconds", 30) * 1000;
        Long lastUse = enchantCooldowns.get(player.getUniqueId());
        long now = System.currentTimeMillis();
        if (lastUse != null && (now - lastUse) < cooldownMs) {
            long remaining = (cooldownMs - (now - lastUse)) / 1000;
            player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                    "&c[Mercador] Aguarde " + remaining + "s antes de encantar novamente!"));
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
            return;
        }

        // Item in slot 10 must be equipment (MMOItem)
        ItemStack equipment = gui.getItem(SLOT_ITEM_IN);
        if (equipment == null || equipment.getType().isAir()) {
            player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                    "&c[Mercador] Coloque um equipamento no slot de entrada (linha 1)!"));
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
            return;
        }

        // Item in slot 37 must be ENCHANTED_BOOK
        ItemStack book = gui.getItem(SLOT_LIVRO);
        if (book == null || book.getType() != Material.ENCHANTED_BOOK) {
            player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                    "&c[Mercador] Coloque um Livro Encantado no slot de entrada (linha 4)!"));
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
            return;
        }

        // Check output slot empty
        ItemStack existingOut = gui.getItem(SLOT_ENC_OUT);
        if (existingOut != null && !existingOut.getType().isAir()) {
            player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                    "&c[Mercador] Retire o item do slot de saida primeiro!"));
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
            return;
        }

        // Get stored enchantments from book
        EnchantmentStorageMeta bookMeta = (EnchantmentStorageMeta) book.getItemMeta();
        if (bookMeta == null || bookMeta.getStoredEnchants().isEmpty()) {
            player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                    "&c[Mercador] O Livro Encantado nao tem encantamentos!"));
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
            return;
        }

        Map<Enchantment, Integer> storedEnchants = bookMeta.getStoredEnchants();
        Map<Enchantment, Integer> applicableEnchants = new LinkedHashMap<>();
        for (Map.Entry<Enchantment, Integer> entry : storedEnchants.entrySet()) {
            if (entry.getKey().canEnchantItem(equipment)) {
                applicableEnchants.put(entry.getKey(), entry.getValue());
            }
        }
        if (applicableEnchants.isEmpty()) {
            player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                    plugin.getConfig().getString("merchant.messages.not-enchantable",
                            "&c[Mercador] Este item nao pode receber encantamentos!")));
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
            return;
        }

        // XP cost removed — enchantment is free

        // Calculate success chance
        double eff = session.efficiency();
        int bp = session.breakpoint();
        double successChance = 60 + (eff * 20) + (session.playerLevel() * 0.15);

        // Breakpoint modifiers
        if (bp >= 3) successChance += 10;
        // Ferreiro nearby: +5
        if (session.env().ferreiroNearby()) successChance += 5;

        // Cap at 99%
        successChance = Math.min(99, successChance);

        // Roll
        double roll = ThreadLocalRandom.current().nextDouble() * 100;
        boolean criticalSuccess = eff >= 2.00 && ThreadLocalRandom.current().nextDouble() < 0.15;
        boolean criticalFail = eff < 1.10 && ThreadLocalRandom.current().nextDouble() < 0.10;

        enchantCooldowns.put(player.getUniqueId(), now);

        if (roll <= successChance || criticalSuccess) {
            // SUCCESS
            ItemStack result = equipment.clone();
            ItemMeta resultMeta = result.getItemMeta();
            if (resultMeta != null) {
                for (Map.Entry<Enchantment, Integer> entry : applicableEnchants.entrySet()) {
                    resultMeta.addEnchant(entry.getKey(), entry.getValue(), true);
                }
                result.setItemMeta(resultMeta);
            }

            gui.setItem(SLOT_ENC_OUT, result);
            gui.setItem(SLOT_ITEM_IN, null);

            if (criticalSuccess) {
                // Critical success: preserve book
                player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                        "&6[Mercador] &eSucesso critico! Encantamentos aplicados e livro preservado!"));
                player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.5f);
            } else {
                // Normal success: consume book
                gui.setItem(SLOT_LIVRO, null);

                // BP4: 15% chance to preserve book
                boolean preserveBook = bp >= 4 && ThreadLocalRandom.current().nextDouble() < 0.15;
                if (preserveBook) {
                    gui.setItem(SLOT_LIVRO, book);
                    player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                            "&6[Mercador] &eBreakpoint Magnata preservou o livro!"));
                }

                player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                        "&a[Mercador] Encantamento aplicado com sucesso!"));
                player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_USE, 1.0f, 1.2f);
            }

            // Effects
            Location tableLoc = session.tableLoc();
            tableLoc.getWorld().spawnParticle(Particle.ENCHANT,
                    tableLoc.clone().add(0.5, 1.5, 0.5), 50, 0.5, 0.5, 0.5, 1.0);

        } else if (criticalFail) {
            // CRITICAL FAIL: consume book + item loses 25% durability
            gui.setItem(SLOT_LIVRO, null);

            ItemStack damaged = equipment.clone();
            if (damaged.getType().getMaxDurability() > 0) {
                ItemMeta damagedMeta = damaged.getItemMeta();
                if (damagedMeta instanceof org.bukkit.inventory.meta.Damageable damageable) {
                    int maxDur = damaged.getType().getMaxDurability();
                    int currentDamage = damageable.getDamage();
                    int addDamage = (int) Math.ceil(maxDur * 0.25);
                    damageable.setDamage(Math.min(maxDur - 1, currentDamage + addDamage));
                    damaged.setItemMeta(damagedMeta);
                }
            }
            gui.setItem(SLOT_ITEM_IN, damaged);

            player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                    "&c[Mercador] Falha critica! O livro foi destruido e o item perdeu 25%% de durabilidade!"));
            player.playSound(player.getLocation(), Sound.ENTITY_ITEM_BREAK, 1.0f, 0.5f);

            Location tableLoc = session.tableLoc();
            tableLoc.getWorld().spawnParticle(Particle.SMOKE,
                    tableLoc.clone().add(0.5, 1.5, 0.5), 40, 0.3, 0.3, 0.3, 0.05);

        } else {
            // NORMAL FAIL: consume book, item untouched
            gui.setItem(SLOT_LIVRO, null);

            player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                    "&c[Mercador] Encantamento falhou! O livro foi destruido."));
            player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_LAND, 0.8f, 0.5f);

            Location tableLoc = session.tableLoc();
            tableLoc.getWorld().spawnParticle(Particle.SMOKE,
                    tableLoc.clone().add(0.5, 1.5, 0.5), 20, 0.2, 0.2, 0.2, 0.03);
        }

        if (debug()) plugin.getLogger().info("[MERCHANT-ENCANTAR] " + player.getName()
                + " tentou encantar. roll=" + String.format("%.1f", roll)
                + " chance=" + String.format("%.1f%%", successChance)
                + " critSuccess=" + criticalSuccess + " critFail=" + criticalFail);
    }

    // ═══════════════════════════════════════════════════
    // Funcao 5: Craft Pedra de Refinamento (slot 42)
    // ═══════════════════════════════════════════════════

    private void handleCraftPedra(Player player, Inventory gui, MerchantSession session) {
        if (!session.ferreiroRowActive()) {
            sendLockedMessage(player, "Mesa do Ferreiro");
            return;
        }

        int minLevel = plugin.getConfig().getInt("merchant.enchant-min-level", 35);
        if (session.playerLevel() < minLevel) {
            player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                    "&c[Mercador] Voce precisa de nivel " + minLevel + " para fabricar!"));
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
            return;
        }

        // Check cooldown: 120s
        long cooldownMs = plugin.getConfig().getLong("merchant.pedra-cooldown-seconds", 120) * 1000;
        Long lastUse = craftPedraCooldowns.get(player.getUniqueId());
        long now = System.currentTimeMillis();
        if (lastUse != null && (now - lastUse) < cooldownMs) {
            long remaining = (cooldownMs - (now - lastUse)) / 1000;
            player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                    "&c[Mercador] Aguarde " + remaining + "s antes de fabricar novamente!"));
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
            return;
        }

        // Check materials: 25x DIAMOND, 20x IRON_INGOT, 15x Oleo de Polimento (MMOItem), 10x GOLD_INGOT
        if (!hasMaterials(player, Material.DIAMOND, 25)) {
            player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                    "&c[Mercador] Voce precisa de 25x Diamante!"));
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
            return;
        }
        if (!hasMaterials(player, Material.IRON_INGOT, 20)) {
            player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                    "&c[Mercador] Voce precisa de 20x Lingote de Ferro!"));
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
            return;
        }
        if (!hasMMOItemInInventory(player, "OLEO_DE_POLIMENTO", 15)) {
            player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                    "&c[Mercador] Voce precisa de 15x Oleo de Polimento!"));
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
            return;
        }
        if (!hasMaterials(player, Material.GOLD_INGOT, 10)) {
            player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                    "&c[Mercador] Voce precisa de 10x Lingote de Ouro!"));
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
            return;
        }

        // Consume materials
        consumeMaterials(player, Material.DIAMOND, 25);
        consumeMaterials(player, Material.IRON_INGOT, 20);
        consumeMMOItemFromInventory(player, "OLEO_DE_POLIMENTO", 15);
        consumeMaterials(player, Material.GOLD_INGOT, 10);

        // Create Pedra de Refinamento
        ItemStack pedra = createMMOItem("CONSUMABLE", "PEDRA_DE_REFINAMENTO");
        if (pedra == null) {
            player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                    "&c[Mercador] Erro ao criar Pedra de Refinamento!"));
            return;
        }

        // Give to player inventory
        Map<Integer, ItemStack> leftover = player.getInventory().addItem(pedra);
        for (ItemStack drop : leftover.values()) {
            player.getWorld().dropItemNaturally(player.getLocation(), drop);
        }

        craftPedraCooldowns.put(player.getUniqueId(), now);

        // Effects
        Location tableLoc = session.tableLoc();
        player.playSound(tableLoc, Sound.BLOCK_ANVIL_USE, 1.0f, 1.0f);
        tableLoc.getWorld().spawnParticle(Particle.ENCHANT,
                tableLoc.clone().add(0.5, 1.5, 0.5), 30, 0.3, 0.3, 0.3, 1.0);

        player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                "&a[Mercador] Pedra de Refinamento fabricada com sucesso!"));

        if (debug()) plugin.getLogger().info("[MERCHANT-CRAFT-PEDRA] " + player.getName()
                + " fabricou Pedra de Refinamento");
    }

    // ═══════════════════════════════════════════════════
    // Rotas Comerciais (slot 7) — Sub-GUI 27 slots
    // ═══════════════════════════════════════════════════

    private void handleRoutes(Player player, MerchantSession session) {
        if (session.playerLevel() < 10) {
            player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                    "&c[Mercador] Voce precisa de nivel 10 para acessar Rotas Comerciais!"));
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
            return;
        }

        if (!plugin.getConfig().getBoolean("merchant.teleport.enabled", true)) {
            player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                    "&c[Mercador] Sistema de rotas desativado!"));
            return;
        }

        openRoutesGUI(player, session);
    }

    private void openRoutesGUI(Player player, MerchantSession session) {
        Inventory gui = Bukkit.createInventory(null, ROUTES_GUI_SIZE,
                Component.text(ROUTES_GUI_TITLE, NamedTextColor.GOLD, TextDecoration.BOLD));

        ItemStack blackGlass = createGlassPane(Material.BLACK_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < ROUTES_GUI_SIZE; i++) gui.setItem(i, blackGlass);

        UUID uuid = player.getUniqueId();
        List<MerchantRouteManager.Waypoint> waypoints = routeManager.getWaypoints(uuid);
        int maxSlots = routeManager.getMaxSlots(session.playerLevel());

        // Info item (slot 4)
        ItemStack info = new ItemStack(Material.COMPASS);
        ItemMeta infoMeta = info.getItemMeta();
        if (infoMeta != null) {
            infoMeta.displayName(Component.text("Rotas Comerciais", NamedTextColor.GOLD, TextDecoration.BOLD));
            List<Component> lore = new ArrayList<>();
            lore.add(Component.text(""));
            lore.add(Component.text("Rotas: " + waypoints.size() + "/" + maxSlots, NamedTextColor.AQUA));
            lore.add(Component.text(""));
            lore.add(Component.text("Clique num waypoint para viajar.", NamedTextColor.GRAY));
            lore.add(Component.text("Shift+clique para remover.", NamedTextColor.RED));
            infoMeta.lore(lore);
            info.setItemMeta(infoMeta);
        }
        gui.setItem(4, info);

        // Registrar Rota (slot 0)
        if (waypoints.size() < maxSlots) {
            ItemStack registerBtn = new ItemStack(Material.LIME_CONCRETE);
            ItemMeta regMeta = registerBtn.getItemMeta();
            if (regMeta != null) {
                regMeta.displayName(Component.text("Registrar Rota Aqui", NamedTextColor.GREEN, TextDecoration.BOLD));
                List<Component> lore = new ArrayList<>();
                lore.add(Component.text(""));
                lore.add(Component.text("Consome 1x Selo de Rota Comercial", NamedTextColor.YELLOW));
                lore.add(Component.text("Registra esta mesa como waypoint.", NamedTextColor.GRAY));
                regMeta.lore(lore);
                registerBtn.setItemMeta(regMeta);
            }
            gui.setItem(0, registerBtn);
        }

        // Waypoints do jogador (slots 9+)
        List<MerchantRouteManager.WaypointInfo> accessible = routeManager.getAccessibleWaypoints(uuid);
        int slotIdx = 9;
        for (MerchantRouteManager.WaypointInfo wpInfo : accessible) {
            if (slotIdx >= ROUTES_GUI_SIZE - 1) break;
            MerchantRouteManager.Waypoint wp = wpInfo.waypoint();

            ItemStack wpItem = new ItemStack(Material.ENDER_EYE);
            ItemMeta wpMeta = wpItem.getItemMeta();
            if (wpMeta != null) {
                wpMeta.displayName(Component.text(wp.name(), NamedTextColor.GREEN, TextDecoration.BOLD));

                List<Component> lore = new ArrayList<>();
                lore.add(Component.text(""));
                lore.add(Component.text("Mundo: " + wp.worldName(), NamedTextColor.GRAY));
                lore.add(Component.text("Posicao: " + wp.x() + ", " + wp.y() + ", " + wp.z(), NamedTextColor.GRAY));
                boolean valid = routeManager.isWaypointValid(wp);
                lore.add(Component.text("Status: " + (valid ? "Ativo" : "Mesa destruida!"),
                        valid ? NamedTextColor.GREEN : NamedTextColor.RED));
                lore.add(Component.text(""));
                lore.add(Component.text("Clique para info da rota.", NamedTextColor.YELLOW));
                lore.add(Component.text("Shift+clique para remover.", NamedTextColor.RED));

                wpMeta.lore(lore);
                wpItem.setItemMeta(wpMeta);
            }

            gui.setItem(slotIdx, wpItem);
            slotIdx++;
        }

        // Voltar (slot 26)
        ItemStack back = new ItemStack(Material.ARROW);
        ItemMeta backMeta = back.getItemMeta();
        if (backMeta != null) {
            backMeta.displayName(Component.text("Voltar", NamedTextColor.RED, TextDecoration.BOLD));
            back.setItemMeta(backMeta);
        }
        gui.setItem(ROUTES_GUI_SIZE - 1, back);

        player.openInventory(gui);
        player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.0f);
    }

    // Handler para cliques na GUI de Rotas
    @EventHandler(priority = EventPriority.HIGH)
    public void onRoutesGUIClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        String title = getInventoryTitle(event.getView());
        if (!ROUTES_GUI_TITLE.equals(title)) return;

        event.setCancelled(true);
        int slot = event.getRawSlot();
        if (slot < 0 || slot >= ROUTES_GUI_SIZE) return;

        UUID uuid = player.getUniqueId();
        MerchantSession session = activeSessions.get(uuid);
        if (session == null) return;

        // Voltar
        if (slot == ROUTES_GUI_SIZE - 1) {
            player.closeInventory();
            Bukkit.getScheduler().runTask(plugin, () -> openGUI(player, session.tableLoc()));
            return;
        }

        // Registrar Rota (slot 0)
        if (slot == 0) {
            handleRegisterWaypoint(player, session);
            return;
        }

        // Waypoints (slots 9+)
        if (slot >= 9 && slot < ROUTES_GUI_SIZE - 1) {
            List<MerchantRouteManager.WaypointInfo> accessible = routeManager.getAccessibleWaypoints(uuid);
            int idx = slot - 9;
            if (idx < 0 || idx >= accessible.size()) return;

            MerchantRouteManager.WaypointInfo wpInfo = accessible.get(idx);

            if (event.isShiftClick()) {
                // Remover waypoint (todas as rotas sao do proprio jogador)
                routeManager.removeWaypoint(uuid, wpInfo.index());
                player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                        "&c[Mercador] Rota '" + wpInfo.waypoint().name() + "' removida!"));
                player.playSound(player.getLocation(), Sound.ENTITY_ITEM_BREAK, 1.0f, 0.8f);
                // Reabrir GUI
                Bukkit.getScheduler().runTask(plugin, () -> openRoutesGUI(player, session));
                return;
            }

            // Informar que viagem e via Pocao de Retorno
            MerchantRouteManager.Waypoint wp = wpInfo.waypoint();
            player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                    "&6[Mercador] &eRota: &f" + wp.name() + " &7(" + wp.worldName() + " " + wp.x() + ", " + wp.y() + ", " + wp.z() + ")"));
            player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                    "&6[Mercador] &eUse uma &bPocao de Retorno &epara viajar!"));
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, 1.5f);
        }
    }

    // ═══════════════════════════════════════════════════
    // Registrar Waypoint
    // ═══════════════════════════════════════════════════

    private void handleRegisterWaypoint(Player player, MerchantSession session) {
        UUID uuid = player.getUniqueId();
        int maxSlots = routeManager.getMaxSlots(session.playerLevel());
        List<MerchantRouteManager.Waypoint> existing = routeManager.getWaypoints(uuid);

        if (existing.size() >= maxSlots) {
            player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                    "&c[Mercador] Voce ja possui o maximo de rotas! (" + maxSlots + ")"));
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
            return;
        }

        // Verificar se ja tem waypoint nesta mesa
        if (routeManager.hasWaypointAt(session.tableLoc())) {
            player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                    "&c[Mercador] Esta mesa ja possui uma rota registrada!"));
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
            return;
        }

        // Verificar se tem Selo de Rota Comercial no inventario
        ItemStack selo = findSeloInInventory(player);
        if (selo == null) {
            player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                    "&c[Mercador] Voce precisa de um Selo de Rota Comercial!"));
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
            return;
        }

        // Consumir selo
        if (selo.getAmount() > 1) {
            selo.setAmount(selo.getAmount() - 1);
        } else {
            player.getInventory().remove(selo);
        }

        // Registrar
        Location loc = session.tableLoc();
        String wpName = "Rota " + player.getName() + " #" + (existing.size() + 1);
        MerchantRouteManager.Waypoint wp = new MerchantRouteManager.Waypoint(
                wpName, loc.getWorld().getName(),
                loc.getBlockX(), loc.getBlockY(), loc.getBlockZ(),
                System.currentTimeMillis(), System.currentTimeMillis(),
                0, "public", List.of(), true
        );
        routeManager.addWaypoint(uuid, wp);

        player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                "&a[Mercador] Rota '" + wpName + "' registrada com sucesso!"));
        player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.2f);

        // Particulas verdes na mesa
        loc.getWorld().spawnParticle(Particle.HAPPY_VILLAGER,
                loc.clone().add(0.5, 1.5, 0.5), 30, 0.5, 0.5, 0.5, 0);

        // Reabrir rotas GUI
        Bukkit.getScheduler().runTask(plugin, () -> openRoutesGUI(player, session));
    }

    private ItemStack findSeloInInventory(Player player) {
        for (ItemStack item : player.getInventory().getContents()) {
            if (item == null || item.getType() != Material.FILLED_MAP) continue;
            ItemMeta meta = item.getItemMeta();
            if (meta == null) continue;
            // Identificar pelo nome do item (Selo de Rota Comercial)
            Component displayName = meta.displayName();
            if (displayName != null) {
                String plain = net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText().serialize(displayName);
                if (plain.contains("Selo de Rota Comercial")) return item;
            }
        }
        return null;
    }

    // ═══════════════════════════════════════════════════
    // Craft Selo de Rota Comercial (slot 48)
    // ═══════════════════════════════════════════════════

    private void handleCraftSelo(Player player, MerchantSession session) {
        int minLevel = plugin.getConfig().getInt("merchant.teleport.min-level", 10);
        if (session.playerLevel() < minLevel) {
            player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                    "&c[Mercador] Voce precisa de nivel " + minLevel + " para fabricar Selos!"));
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
            return;
        }

        // Cooldown
        long cooldownMs = plugin.getConfig().getLong("merchant.selo-craft.cooldown-seconds", 120) * 1000;
        Long lastUse = seloCraftCooldowns.get(player.getUniqueId());
        long now = System.currentTimeMillis();
        if (lastUse != null && (now - lastUse) < cooldownMs) {
            long remaining = (cooldownMs - (now - lastUse)) / 1000;
            player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                    "&c[Mercador] Aguarde " + remaining + "s antes de fabricar novamente!"));
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
            return;
        }

        // Materiais: 16x Ender Pearl, 1x Honey Bottle, 1x Filled Map, 4x Lapis, 2x Gold Ingot
        int enderPearls = plugin.getConfig().getInt("merchant.selo-craft.ender-pearls", 16);
        int honeyBottles = plugin.getConfig().getInt("merchant.selo-craft.honey-bottles", 1);
        int filledMaps = plugin.getConfig().getInt("merchant.selo-craft.filled-maps", 1);
        int lapisLazuli = plugin.getConfig().getInt("merchant.selo-craft.lapis-lazuli", 4);
        int goldIngots = plugin.getConfig().getInt("merchant.selo-craft.gold-ingots", 2);

        if (!hasMaterials(player, Material.ENDER_PEARL, enderPearls)) {
            player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                    "&c[Mercador] Voce precisa de " + enderPearls + "x Ender Pearl!"));
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
            return;
        }
        if (!hasMaterials(player, Material.HONEY_BOTTLE, honeyBottles)) {
            player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                    "&c[Mercador] Voce precisa de " + honeyBottles + "x Pote de Mel!"));
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
            return;
        }
        if (!hasMaterials(player, Material.FILLED_MAP, filledMaps)) {
            player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                    "&c[Mercador] Voce precisa de " + filledMaps + "x Mapa Preenchido!"));
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
            return;
        }
        if (!hasMaterials(player, Material.LAPIS_LAZULI, lapisLazuli)) {
            player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                    "&c[Mercador] Voce precisa de " + lapisLazuli + "x Lapis Lazuli!"));
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
            return;
        }
        if (!hasMaterials(player, Material.GOLD_INGOT, goldIngots)) {
            player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                    "&c[Mercador] Voce precisa de " + goldIngots + "x Lingote de Ouro!"));
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
            return;
        }

        // Consumir
        consumeMaterials(player, Material.ENDER_PEARL, enderPearls);
        consumeMaterials(player, Material.HONEY_BOTTLE, honeyBottles);
        consumeMaterials(player, Material.FILLED_MAP, filledMaps);
        consumeMaterials(player, Material.LAPIS_LAZULI, lapisLazuli);
        consumeMaterials(player, Material.GOLD_INGOT, goldIngots);

        // Criar Selo de Rota Comercial (item custom com FILLED_MAP)
        ItemStack selo = new ItemStack(Material.FILLED_MAP);
        ItemMeta seloMeta = selo.getItemMeta();
        if (seloMeta != null) {
            seloMeta.displayName(Component.text("Selo de Rota Comercial", NamedTextColor.GOLD, TextDecoration.BOLD));
            List<Component> lore = new ArrayList<>();
            lore.add(Component.text(""));
            lore.add(Component.text("Use na Mesa do Mercador para", NamedTextColor.GRAY));
            lore.add(Component.text("registrar uma rota comercial.", NamedTextColor.GRAY));
            lore.add(Component.text(""));
            lore.add(Component.text("Item consumido ao registrar.", NamedTextColor.RED));
            seloMeta.lore(lore);
            selo.setItemMeta(seloMeta);
        }

        Map<Integer, ItemStack> leftover = player.getInventory().addItem(selo);
        for (ItemStack drop : leftover.values()) {
            player.getWorld().dropItemNaturally(player.getLocation(), drop);
        }

        seloCraftCooldowns.put(player.getUniqueId(), now);

        Location tableLoc = session.tableLoc();
        player.playSound(tableLoc, Sound.BLOCK_ENCHANTMENT_TABLE_USE, 1.0f, 0.8f);
        tableLoc.getWorld().spawnParticle(Particle.ENCHANT,
                tableLoc.clone().add(0.5, 1.5, 0.5), 30, 0.3, 0.3, 0.3, 1.0);

        player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                "&a[Mercador] Selo de Rota Comercial fabricado!"));

        if (debug()) plugin.getLogger().info("[MERCHANT-SELO] " + player.getName() + " fabricou Selo de Rota Comercial");
    }

    // ═══════════════════════════════════════════════════
    // Teletransporte via Recall Potion — Casting 5s + Hotbar Overlay
    // ═══════════════════════════════════════════════════

    private static final String RECALL_POTION_TAG = "NEMONICORB_RECALL";

    private ItemStack createRecallPotion() {
        ItemStack potion = new ItemStack(Material.POTION);
        ItemMeta meta = potion.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text("Pocao de Retorno", NamedTextColor.GOLD, TextDecoration.BOLD));
            List<Component> lore = new ArrayList<>();
            lore.add(Component.text(""));
            lore.add(Component.text("Beba para iniciar o Recall.", NamedTextColor.GRAY));
            lore.add(Component.text("Selecione o destino na hotbar.", NamedTextColor.GRAY));
            lore.add(Component.text(""));
            lore.add(Component.text("Exclusivo: Mercador", NamedTextColor.YELLOW));
            lore.add(Component.text("Custo: 50 Mana + 10/passageiro", NamedTextColor.RED));
            meta.lore(lore);
            if (meta instanceof org.bukkit.inventory.meta.PotionMeta potionMeta) {
                potionMeta.setColor(Color.fromRGB(255, 200, 50)); // Dourado
            }
            potion.setItemMeta(meta);
        }
        // Adicionar NBT tag custom
        NBTItem nbt = NBTItem.get(potion);
        nbt.addTag(new ItemTag(RECALL_POTION_TAG, 1));
        return nbt.toItem();
    }

    private boolean isRecallPotion(ItemStack item) {
        if (item == null || item.getType() != Material.POTION) return false;
        try {
            NBTItem nbt = NBTItem.get(item);
            return nbt.hasTag(RECALL_POTION_TAG) && nbt.getInteger(RECALL_POTION_TAG) == 1;
        } catch (Exception e) { return false; }
    }

    // Recall Potion: somente Mercador pode consumir
    @EventHandler(priority = EventPriority.HIGH)
    public void onConsumeRecallPotion(PlayerItemConsumeEvent event) {
        Player player = event.getPlayer();
        ItemStack item = event.getItem();
        if (item == null) return;

        // Verificar se e Recall Potion via NBT custom
        if (!isRecallPotion(item)) return;

        // Bloquear consumo vanilla — nós vamos lidar com o efeito
        event.setCancelled(true);

        // Somente Mercador pode usar
        if (!isMercador(player)) {
            player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                    "&c[Mercador] Apenas Mercadores podem usar a Pocao de Retorno!"));
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
            return;
        }

        // Ja esta em casting?
        if (castingTasks.containsKey(player.getUniqueId())) {
            player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                    "&c[Mercador] Ja esta canalizando um teletransporte!"));
            return;
        }

        // Cooldown
        long cooldownMs = plugin.getConfig().getLong("merchant.teleport.cooldown-seconds", 30) * 1000;
        Long lastUse = teleportCooldowns.get(player.getUniqueId());
        long now = System.currentTimeMillis();
        if (lastUse != null && (now - lastUse) < cooldownMs) {
            long remaining = (cooldownMs - (now - lastUse)) / 1000;
            player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                    "&c[Mercador] Aguarde " + remaining + "s antes de teleportar novamente!"));
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
            return;
        }

        // Buscar destinos acessiveis
        UUID uuid = player.getUniqueId();
        List<MerchantRouteManager.WaypointInfo> destinations = routeManager.getAccessibleWaypoints(uuid);
        if (destinations.isEmpty()) {
            player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                    "&c[Mercador] Voce nao possui rotas registradas!"));
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
            return;
        }

        // Verificar mana (50 base + 25 por passageiro proximo)
        double manaCostBase = plugin.getConfig().getDouble("merchant.teleport.mana-cost", 50);
        double manaCostPerPassenger = plugin.getConfig().getDouble("merchant.teleport.mana-cost-per-passenger", 25);
        double passRadius = plugin.getConfig().getDouble("merchant.teleport.passenger-radius", 5);

        // Contar passageiros potenciais
        int potentialPassengers = 0;
        for (Player nearby : player.getWorld().getPlayers()) {
            if (nearby.equals(player)) continue;
            if (nearby.getLocation().distanceSquared(player.getLocation()) <= passRadius * passRadius) {
                potentialPassengers++;
            }
        }
        int maxPassengers = plugin.getConfig().getInt("merchant.teleport.max-passengers", 3);
        potentialPassengers = Math.min(potentialPassengers, maxPassengers);
        double totalManaCost = manaCostBase + (potentialPassengers * manaCostPerPassenger);

        double currentMana = getPlayerMana(player);
        if (currentMana < totalManaCost) {
            player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                    "&c[Mercador] Mana insuficiente! Precisa de " + (int) totalManaCost
                            + " mana (" + (int) manaCostBase + " base + " + potentialPassengers + " passageiros x " + (int) manaCostPerPassenger + ")"));
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
            return;
        }

        // Deduzir mana
        if (!deductPlayerMana(player, totalManaCost)) {
            player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                    "&c[Mercador] Erro ao deduzir mana!"));
            return;
        }

        // Consumir a pocao (remover 1 da mao)
        if (item.getAmount() > 1) {
            item.setAmount(item.getAmount() - 1);
        } else {
            player.getInventory().setItemInMainHand(null);
        }

        // Iniciar casting com hotbar overlay
        startRecallCasting(player, destinations);
    }

    private void startRecallCasting(Player player, List<MerchantRouteManager.WaypointInfo> destinations) {
        UUID uuid = player.getUniqueId();
        int castingSeconds = plugin.getConfig().getInt("merchant.teleport.casting-seconds", 5);
        int castingTicks = castingSeconds * 20;

        // Backup da hotbar (slots 0-8) — copiar cada item
        ItemStack[] hotbarBackup = new ItemStack[9];
        for (int i = 0; i < 9; i++) {
            ItemStack slot = player.getInventory().getItem(i);
            hotbarBackup[i] = slot != null ? slot.clone() : null;
        }
        hotbarBackups.put(uuid, hotbarBackup);

        // Colocar destinos na hotbar (max 9 destinos, slots 0-8)
        int maxDests = Math.min(destinations.size(), 9);
        List<MerchantRouteManager.WaypointInfo> validDests = destinations.subList(0, maxDests);
        castingDestinations.put(uuid, new ArrayList<>(validDests));
        selectedDestination.put(uuid, 0); // default = primeiro

        for (int i = 0; i < 9; i++) {
            if (i < validDests.size()) {
                MerchantRouteManager.WaypointInfo wpInfo = validDests.get(i);
                MerchantRouteManager.Waypoint wp = wpInfo.waypoint();
                boolean isOwn = wpInfo.ownerUUID().equals(uuid);

                ItemStack destItem = new ItemStack(isOwn ? Material.ENDER_EYE : Material.ENDER_PEARL);
                ItemMeta meta = destItem.getItemMeta();
                if (meta != null) {
                    meta.displayName(Component.text((i + 1) + ". " + wp.name(),
                            isOwn ? NamedTextColor.GREEN : NamedTextColor.AQUA, TextDecoration.BOLD));
                    List<Component> lore = new ArrayList<>();
                    lore.add(Component.text(wp.worldName() + " (" + wp.x() + ", " + wp.y() + ", " + wp.z() + ")", NamedTextColor.GRAY));
                    if (i == 0) {
                        lore.add(Component.text(""));
                        lore.add(Component.text(">>> SELECIONADO <<<", NamedTextColor.YELLOW, TextDecoration.BOLD));
                    }
                    lore.add(Component.text(""));
                    lore.add(Component.text("Segure este slot para viajar aqui", NamedTextColor.YELLOW));
                    meta.lore(lore);
                    destItem.setItemMeta(meta);
                }
                player.getInventory().setItem(i, destItem);
            } else {
                // Slot vazio = vidro cinza
                ItemStack glass = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
                ItemMeta gMeta = glass.getItemMeta();
                if (gMeta != null) {
                    gMeta.displayName(Component.text("Sem rota", NamedTextColor.DARK_GRAY));
                    glass.setItemMeta(gMeta);
                }
                player.getInventory().setItem(i, glass);
            }
        }

        player.getInventory().setHeldItemSlot(0);
        castingStartLocs.put(uuid, player.getLocation().clone());

        // Som e mensagem para todos proximos
        Location pLoc = player.getLocation();
        double passRadius = plugin.getConfig().getDouble("merchant.teleport.passenger-radius", 5);
        pLoc.getWorld().playSound(pLoc, Sound.BLOCK_PORTAL_TRIGGER, 1.0f, 1.5f);

        player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                "&6[Mercador] Recall iniciado! Selecione o destino na hotbar. Nao se mova! (" + castingSeconds + "s)"));

        // Notificar passageiros proximos
        for (Player nearby : pLoc.getWorld().getPlayers()) {
            if (nearby.equals(player)) continue;
            if (nearby.getLocation().distanceSquared(pLoc) <= passRadius * passRadius) {
                nearby.sendMessage(ChatColor.translateAlternateColorCodes('&',
                        "&6[Mercador] " + player.getName() + " esta canalizando Recall! Fique proximo para viajar junto!"));
                nearby.playSound(pLoc, Sound.BLOCK_PORTAL_TRIGGER, 0.5f, 1.5f);
            }
        }

        // Task de casting com action bar + particulas
        BukkitTask castTask = Bukkit.getScheduler().runTaskTimer(plugin, new Runnable() {
            int ticks = 0;
            @Override
            public void run() {
                ticks += 4;

                if (!player.isOnline() || !castingTasks.containsKey(uuid)) {
                    cancelCasting(uuid, false);
                    return;
                }

                double remainingSec = Math.max(0, (castingTicks - ticks) / 20.0);
                int bars = 20;
                int filled = (int) ((double) ticks / castingTicks * bars);

                StringBuilder barStr = new StringBuilder("&6Recall &a");
                for (int i = 0; i < bars; i++) {
                    barStr.append(i < filled ? "|" : "&7|");
                }
                barStr.append(" &e").append(String.format("%.1fs", remainingSec));

                // Mostrar destino selecionado
                int selIdx = selectedDestination.getOrDefault(uuid, 0);
                List<MerchantRouteManager.WaypointInfo> dests = castingDestinations.get(uuid);
                if (dests != null && selIdx >= 0 && selIdx < dests.size()) {
                    barStr.append(" &f→ &b").append(dests.get(selIdx).waypoint().name());
                }

                player.sendActionBar(Component.text(
                        ChatColor.translateAlternateColorCodes('&', barStr.toString())));

                // Particulas para todos
                World world = player.getWorld();
                Location loc = player.getLocation();
                double angle = (ticks % 40) / 40.0 * 2 * Math.PI;
                Particle.DustOptions dust = new Particle.DustOptions(Color.fromRGB(50, 200, 50), 1.2f);
                for (int i = 0; i < 4; i++) {
                    double a = angle + (i * Math.PI / 2);
                    double px = loc.getX() + 1.5 * Math.cos(a);
                    double pz = loc.getZ() + 1.5 * Math.sin(a);
                    world.spawnParticle(Particle.DUST, px, loc.getY() + 1.0, pz, 2, 0, 0.3, 0, 0, dust);
                }
                world.spawnParticle(Particle.ENCHANT, loc.clone().add(0, 1.5, 0), 5, 0.5, 0.5, 0.5, 0.5);

                // Som de tick a cada segundo
                if (ticks % 20 == 0) {
                    world.playSound(loc, Sound.BLOCK_NOTE_BLOCK_BELL, 0.8f, 1.0f + (float) ticks / castingTicks);
                }

                // Casting completo
                if (ticks >= castingTicks) {
                    completeRecallTeleport(player);
                }
            }
        }, 4L, 4L);

        castingTasks.put(uuid, castTask);
    }

    // Mudar destino selecionado ao trocar slot da hotbar
    @EventHandler(priority = EventPriority.MONITOR)
    public void onHeldItemChange(PlayerItemHeldEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();
        if (!castingTasks.containsKey(uuid)) return;

        List<MerchantRouteManager.WaypointInfo> dests = castingDestinations.get(uuid);
        if (dests == null) return;

        int newSlot = event.getNewSlot();
        if (newSlot >= 0 && newSlot < dests.size()) {
            selectedDestination.put(uuid, newSlot);

            // Atualizar lore do item selecionado para mostrar "SELECIONADO"
            Player player = event.getPlayer();
            for (int i = 0; i < dests.size(); i++) {
                ItemStack slotItem = player.getInventory().getItem(i);
                if (slotItem == null) continue;
                ItemMeta meta = slotItem.getItemMeta();
                if (meta == null) continue;

                MerchantRouteManager.Waypoint wp = dests.get(i).waypoint();
                List<Component> lore = new ArrayList<>();
                lore.add(Component.text(wp.worldName() + " (" + wp.x() + ", " + wp.y() + ", " + wp.z() + ")", NamedTextColor.GRAY));
                if (i == newSlot) {
                    lore.add(Component.text(""));
                    lore.add(Component.text(">>> SELECIONADO <<<", NamedTextColor.YELLOW, TextDecoration.BOLD));
                }
                lore.add(Component.text(""));
                lore.add(Component.text("Segure este slot para viajar aqui", NamedTextColor.YELLOW));
                meta.lore(lore);
                slotItem.setItemMeta(meta);
            }
        }
    }

    private void completeRecallTeleport(Player player) {
        UUID uuid = player.getUniqueId();

        List<MerchantRouteManager.WaypointInfo> dests = castingDestinations.get(uuid);
        int selIdx = selectedDestination.getOrDefault(uuid, 0);

        // Restaurar hotbar ANTES de teleportar
        restoreHotbar(player);

        if (dests == null || selIdx < 0 || selIdx >= dests.size()) {
            cancelCasting(uuid, false);
            player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                    "&c[Mercador] Erro: destino invalido!"));
            return;
        }

        MerchantRouteManager.WaypointInfo wpInfo = dests.get(selIdx);
        MerchantRouteManager.Waypoint wp = wpInfo.waypoint();

        if (!routeManager.isWaypointValid(wp)) {
            cancelCasting(uuid, false);
            player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                    "&c[Mercador] A mesa de destino foi destruida!"));
            return;
        }

        Location destination = wp.toLocation();
        if (destination == null) {
            cancelCasting(uuid, false);
            player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                    "&c[Mercador] Mundo de destino nao encontrado!"));
            return;
        }

        // Coletar passageiros proximos (raio 5)
        Location startLoc = player.getLocation();
        double passRadius = plugin.getConfig().getDouble("merchant.teleport.passenger-radius", 5);
        int maxPassengers = plugin.getConfig().getInt("merchant.teleport.max-passengers", 3);
        List<Player> passengers = new ArrayList<>();
        for (Player nearby : startLoc.getWorld().getPlayers()) {
            if (nearby.equals(player)) continue;
            if (nearby.getLocation().distanceSquared(startLoc) <= passRadius * passRadius) {
                if (passengers.size() < maxPassengers) {
                    passengers.add(nearby);
                }
            }
        }

        // Teleportar jogador
        player.teleport(destination);

        // Efeitos para TODOS
        World destWorld = destination.getWorld();
        destWorld.playSound(destination, Sound.ENTITY_ENDERMAN_TELEPORT, 1.5f, 1.0f);
        destWorld.playSound(destination, Sound.BLOCK_BEACON_ACTIVATE, 1.0f, 1.5f);
        destWorld.spawnParticle(Particle.PORTAL, destination.clone().add(0, 1, 0), 80, 0.5, 1, 0.5, 0.5);
        destWorld.spawnParticle(Particle.ENCHANT, destination.clone().add(0, 2, 0), 40, 1, 1, 1, 1);

        // Efeitos no ponto de partida
        startLoc.getWorld().spawnParticle(Particle.PORTAL, startLoc.clone().add(0, 1, 0), 50, 0.5, 1, 0.5, 0.5);
        startLoc.getWorld().playSound(startLoc, Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 0.8f);

        player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                "&a[Mercador] Recall concluido! Bem-vindo a '" + wp.name() + "'!"));
        player.sendActionBar(Component.text("Recall concluido! → " + wp.name(), NamedTextColor.GREEN, TextDecoration.BOLD));

        // Teleportar passageiros
        for (Player passenger : passengers) {
            if (!passenger.isOnline()) continue;
            passenger.teleport(destination);
            passenger.playSound(destination, Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 1.0f);
            passenger.sendMessage(ChatColor.translateAlternateColorCodes('&',
                    "&a[Mercador] Voce viajou com " + player.getName() + " para '" + wp.name() + "'!"));
        }

        teleportCooldowns.put(uuid, System.currentTimeMillis());

        // Limpar estado de casting
        BukkitTask task = castingTasks.remove(uuid);
        if (task != null) task.cancel();
        castingStartLocs.remove(uuid);
        castingDestinations.remove(uuid);
        selectedDestination.remove(uuid);
    }

    private void restoreHotbar(Player player) {
        UUID uuid = player.getUniqueId();
        ItemStack[] backup = hotbarBackups.remove(uuid);
        if (backup == null) return;

        for (int i = 0; i < 9; i++) {
            player.getInventory().setItem(i, backup[i]);
        }
    }

    private void cancelCasting(UUID playerUUID, boolean interrupted) {
        BukkitTask task = castingTasks.remove(playerUUID);
        if (task != null) task.cancel();
        castingStartLocs.remove(playerUUID);
        castingDestinations.remove(playerUUID);
        selectedDestination.remove(playerUUID);

        // Restaurar hotbar
        Player player = Bukkit.getPlayer(playerUUID);
        if (player != null) {
            restoreHotbar(player);
        } else {
            hotbarBackups.remove(playerUUID);
        }

        if (interrupted && player != null) {
            player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                    "&c[Mercador] Recall cancelado!"));
            player.playSound(player.getLocation(), Sound.ENTITY_ITEM_BREAK, 1.0f, 0.5f);
            player.sendActionBar(Component.text("Recall cancelado!", NamedTextColor.RED, TextDecoration.BOLD));

            // Cooldown reduzido
            long cancelledCooldownMs = plugin.getConfig().getLong("merchant.teleport.cancelled-cooldown-seconds", 10) * 1000;
            teleportCooldowns.put(playerUUID, System.currentTimeMillis() - (30000 - cancelledCooldownMs));
        }
    }

    // Cancelar casting ao tomar dano
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        if (castingTasks.containsKey(player.getUniqueId())) {
            cancelCasting(player.getUniqueId(), true);
        }
    }

    // Cancelar casting ao se mover
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerMove(PlayerMoveEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();
        if (!castingTasks.containsKey(uuid)) return;

        Location from = event.getFrom();
        Location to = event.getTo();
        if (to == null) return;

        if (from.getBlockX() != to.getBlockX() || from.getBlockY() != to.getBlockY() || from.getBlockZ() != to.getBlockZ()) {
            cancelCasting(uuid, true);
        }
    }

    // ═══════════════════════════════════════════════════
    // Selo: registrar waypoint via right-click na mesa
    // ═══════════════════════════════════════════════════

    @EventHandler(priority = EventPriority.HIGH)
    public void onSealRightClickMesa(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        if (event.getClickedBlock() == null || event.getClickedBlock().getType() != Material.CARTOGRAPHY_TABLE) return;

        Player player = event.getPlayer();
        if (!isMercador(player)) return;

        ItemStack mainHand = player.getInventory().getItemInMainHand();
        if (mainHand.getType() != Material.FILLED_MAP) return;

        // Verificar se e Selo de Rota Comercial
        ItemMeta meta = mainHand.getItemMeta();
        if (meta == null) return;
        Component displayName = meta.displayName();
        if (displayName == null) return;
        String plain = net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText().serialize(displayName);
        if (!plain.contains("Selo de Rota Comercial")) return;

        // E um selo! Registrar waypoint
        event.setCancelled(true);

        UUID uuid = player.getUniqueId();
        int playerLevel = getPlayerLevel(player);
        int maxSlots = routeManager.getMaxSlots(playerLevel);
        List<MerchantRouteManager.Waypoint> existing = routeManager.getWaypoints(uuid);

        if (existing.size() >= maxSlots) {
            player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                    "&c[Mercador] Voce ja possui o maximo de rotas! (" + maxSlots + ")"));
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
            return;
        }

        Location blockLoc = event.getClickedBlock().getLocation();
        if (routeManager.hasWaypointAt(blockLoc)) {
            player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                    "&c[Mercador] Esta mesa ja possui uma rota registrada!"));
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
            return;
        }

        // Consumir selo
        if (mainHand.getAmount() > 1) {
            mainHand.setAmount(mainHand.getAmount() - 1);
        } else {
            player.getInventory().setItemInMainHand(null);
        }

        String wpName = "Rota " + player.getName() + " #" + (existing.size() + 1);
        MerchantRouteManager.Waypoint wp = new MerchantRouteManager.Waypoint(
                wpName, blockLoc.getWorld().getName(),
                blockLoc.getBlockX(), blockLoc.getBlockY(), blockLoc.getBlockZ(),
                System.currentTimeMillis(), System.currentTimeMillis(),
                0, "public", List.of(), true
        );
        routeManager.addWaypoint(uuid, wp);

        player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                "&a[Mercador] Rota '" + wpName + "' registrada com sucesso!"));
        player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.2f);
        blockLoc.getWorld().spawnParticle(Particle.HAPPY_VILLAGER,
                blockLoc.clone().add(0.5, 1.5, 0.5), 30, 0.5, 0.5, 0.5, 0);
        blockLoc.getWorld().playSound(blockLoc, Sound.BLOCK_BEACON_ACTIVATE, 1.0f, 1.5f);

        if (debug()) plugin.getLogger().info("[MERCHANT-SELO] " + player.getName()
                + " registrou rota '" + wpName + "' em " + blockLoc);
    }

    // ═══════════════════════════════════════════════════
    // Aura VFX (4 tiers)
    // ═══════════════════════════════════════════════════

    private int getAuraLevel(double efficiency) {
        if (efficiency >= 2.50) return 4;
        if (efficiency >= 2.00) return 3;
        if (efficiency >= 1.60) return 2;
        if (efficiency >= 1.10) return 1;
        return 0;
    }

    private void startAura(Player player, Location tableLoc, double efficiency) {
        stopAura(player);
        int level = getAuraLevel(efficiency);
        if (level == 0) return;

        BukkitTask task = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (!player.isOnline() || !activeSessions.containsKey(player.getUniqueId())) {
                stopAura(player);
                return;
            }

            World world = tableLoc.getWorld();
            if (world == null) return;

            double cx = tableLoc.getX() + 0.5;
            double cy = tableLoc.getY() + 1.0;
            double cz = tableLoc.getZ() + 0.5;

            Color color = switch (level) {
                case 1 -> Color.fromRGB(180, 180, 180);       // Gray dust
                case 2 -> Color.fromRGB(50, 200, 50);         // Green
                case 3 -> Color.fromRGB(255, 200, 50);        // Gold
                case 4 -> Color.fromRGB(255, 215, 0);         // Intense gold
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

            int particleCount = level == 4 ? 40 : 15 + (level * 5);

            for (int i = 0; i < particleCount; i++) {
                double angle = (2 * Math.PI / particleCount) * i;
                double px = cx + radius * Math.cos(angle);
                double pz = cz + radius * Math.sin(angle);
                world.spawnParticle(Particle.DUST, px, cy + 0.5, pz, 1, 0, 0.3, 0, 0, dustOptions);
            }

            world.spawnParticle(Particle.DUST, cx, cy + 1.5, cz, 3, 0.2, 0.5, 0.2, 0, dustOptions);

            // Level 2: HAPPY_VILLAGER
            if (level >= 2) {
                world.spawnParticle(Particle.HAPPY_VILLAGER, cx, cy + 1.0, cz, 5, 0.5, 0.3, 0.5, 0);
            }

            // Level 4: END_ROD + Luck effect
            if (level == 4) {
                for (int i = 0; i < 8; i++) {
                    double angle = (2 * Math.PI / 8) * i + (System.currentTimeMillis() % 5000) / 5000.0 * 2 * Math.PI;
                    double px = cx + radius * Math.cos(angle);
                    double pz = cz + radius * Math.sin(angle);
                    world.spawnParticle(Particle.END_ROD, px, cy + 1.0, pz, 2, 0.1, 0.3, 0.1, 0.02);
                }

                for (Player nearby : world.getPlayers()) {
                    if (nearby.getLocation().distanceSquared(tableLoc) <= 12.5 * 12.5) {
                        if (!nearby.hasPotionEffect(PotionEffectType.LUCK)) {
                            nearby.addPotionEffect(new PotionEffect(
                                    PotionEffectType.LUCK, 100, 0, true, true, true));
                        }
                    }
                }
            }
        }, 10L, 10L);

        auraTasks.put(player.getUniqueId(), task);
    }

    private void stopAura(Player player) {
        BukkitTask task = auraTasks.remove(player.getUniqueId());
        if (task != null) task.cancel();
    }

    // ═══════════════════════════════════════════════════
    // Criacao de Itens da GUI
    // ═══════════════════════════════════════════════════

    private ItemStack createGlassPane(Material mat, String name) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text(name, NamedTextColor.DARK_GRAY));
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack createArrowPane() {
        ItemStack item = new ItemStack(Material.LIME_STAINED_GLASS_PANE);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text(">>>", NamedTextColor.GREEN, TextDecoration.BOLD));
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack createLockedRowPane(String reason) {
        ItemStack item = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text("Bloqueado", NamedTextColor.RED, TextDecoration.BOLD));
            List<Component> lore = new ArrayList<>();
            lore.add(Component.text(""));
            lore.add(Component.text(reason, NamedTextColor.GRAY));
            meta.lore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack createEfficiencyItem(MerchantEnvironment env, double efficiency, int playerLevel) {
        int breakpoint = getBreakpoint(efficiency);
        ItemStack item = new ItemStack(Material.NETHER_STAR);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text("Eficiencia: " + String.format("%.0f%%", efficiency * 100),
                    NamedTextColor.GREEN, TextDecoration.BOLD));

            List<Component> lore = new ArrayList<>();
            lore.add(Component.text(""));
            lore.add(Component.text("Titulo: " + getBreakpointName(breakpoint),
                    getBreakpointColor(breakpoint), TextDecoration.BOLD));
            lore.add(Component.text("Nivel: " + playerLevel, NamedTextColor.AQUA));
            lore.add(Component.text(""));

            // Block indicators (only show above threshold)
            lore.add(Component.text("Blocos detectados:", NamedTextColor.GRAY));

            for (Map.Entry<Material, Integer> entry : env.blockCounts().entrySet()) {
                double[] cfg = MERCHANT_BLOCKS.get(entry.getKey());
                if (cfg == null) continue;
                int threshold = INDICATOR_THRESHOLDS.getOrDefault(entry.getKey().name(), 1);
                if (entry.getValue() < threshold) continue;
                int count = Math.min(entry.getValue(), (int) cfg[1]);
                String bonus = String.format("+%.0f%%", count * cfg[0] * 100);
                lore.add(Component.text("  \u2726 " + formatMaterial(entry.getKey()) + ": " + count
                        + " (" + bonus + ")", NamedTextColor.GRAY));
            }

            // Item Frames
            int frameThreshold = INDICATOR_THRESHOLDS.getOrDefault("ITEM_FRAME", 4);
            if (env.itemFrameCount() >= frameThreshold) {
                int effective = Math.min(env.itemFrameCount(), 12);
                String bonus = String.format("+%.0f%%", effective * 0.02 * 100);
                lore.add(Component.text("  \u2726 Quadros de Item: " + env.itemFrameCount()
                        + " (" + bonus + ")", NamedTextColor.GRAY));
            }

            // Nearby players
            if (env.nearbyPlayers() > 0) {
                lore.add(Component.text("  \u2726 Jogadores proximos: " + env.nearbyPlayers()
                        + " (+" + String.format("%.0f%%", env.playerBonus() * 100) + ")", NamedTextColor.YELLOW));
            }

            // Hub block presence
            lore.add(Component.text(""));
            lore.add(Component.text((env.enchantingTablePresent() ? "\u2714" : "\u2718")
                    + " Transmutacao", env.enchantingTablePresent() ? NamedTextColor.GREEN : NamedTextColor.DARK_GRAY));
            lore.add(Component.text((env.anvilPresent() ? "\u2714" : "\u2718")
                    + " Ferreiro", env.anvilPresent() ? NamedTextColor.GREEN : NamedTextColor.DARK_GRAY));

            meta.lore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack createInfoItem(boolean alquimistaRowActive, boolean ferreiroRowActive) {
        ItemStack item = new ItemStack(Material.BOOK);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text("Mesa do Mercador", NamedTextColor.GOLD, TextDecoration.BOLD));
            List<Component> lore = new ArrayList<>();
            lore.add(Component.text(""));
            lore.add(Component.text("Avaliar Item | Pergaminho", NamedTextColor.GREEN));

            if (alquimistaRowActive) {
                lore.add(Component.text("Pocao de Retorno", NamedTextColor.AQUA));
            }
            if (ferreiroRowActive) {
                lore.add(Component.text("Encantamento | Pedra de Refinamento", NamedTextColor.LIGHT_PURPLE));
            }

            if (!alquimistaRowActive || !ferreiroRowActive) {
                lore.add(Component.text(""));
                lore.add(Component.text("Coloque mesas proximas (4 blocos)", NamedTextColor.DARK_GRAY));
                lore.add(Component.text("para desbloquear mais funcoes.", NamedTextColor.DARK_GRAY));
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
            meta.displayName(Component.text("Rotas Comerciais", NamedTextColor.GOLD, TextDecoration.BOLD));
            List<Component> lore = new ArrayList<>();
            lore.add(Component.text("Gerenciar waypoints.", NamedTextColor.GRAY));
            meta.lore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack createAvaliarButton() {
        ItemStack item = new ItemStack(Material.EMERALD);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text("Avaliar Item", NamedTextColor.GREEN, TextDecoration.BOLD));
            List<Component> lore = new ArrayList<>();
            lore.add(Component.text("Identifica o item a esquerda.", NamedTextColor.GRAY));
            lore.add(Component.text("Gratuito!", NamedTextColor.GREEN));
            meta.lore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack createCraftPergButton() {
        ItemStack item = new ItemStack(Material.PAPER);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text("Fabricar Pergaminho", NamedTextColor.GREEN, TextDecoration.BOLD));
            List<Component> lore = new ArrayList<>();
            lore.add(Component.text("4 Papel + 2 Tinta Brilhante + 1 Lapis", NamedTextColor.AQUA));
            lore.add(Component.text("Cooldown: 30s", NamedTextColor.RED));
            meta.lore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack createPocaoButton() {
        ItemStack item = new ItemStack(Material.POTION);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text("Fabricar Pocao de Retorno", NamedTextColor.AQUA, TextDecoration.BOLD));
            List<Component> lore = new ArrayList<>();
            lore.add(Component.text("4 Ender Pearl + 8 Redstone + 1 Bussola", NamedTextColor.AQUA));
            lore.add(Component.text("Cooldown: 60s", NamedTextColor.RED));
            if (meta instanceof org.bukkit.inventory.meta.PotionMeta potionMeta) {
                potionMeta.setColor(Color.fromRGB(255, 200, 50));
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
            meta.displayName(Component.text("Aplicar Encantamento", NamedTextColor.LIGHT_PURPLE, TextDecoration.BOLD));
            List<Component> lore = new ArrayList<>();
            lore.add(Component.text("Item na linha 1 + Livro na linha 4", NamedTextColor.GRAY));
            lore.add(Component.text("Nivel " + plugin.getConfig().getInt("merchant.enchant-min-level", 35) + "+ | Cooldown: 30s", NamedTextColor.RED));
            meta.lore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack createCraftPedraButton() {
        ItemStack item = new ItemStack(Material.GOLD_INGOT);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text("Fabricar Pedra de Refinamento", NamedTextColor.GOLD, TextDecoration.BOLD));
            List<Component> lore = new ArrayList<>();
            lore.add(Component.text("25 Diamante + 20 Ferro + 15 Oleo + 10 Ouro", NamedTextColor.AQUA));
            lore.add(Component.text("Nivel " + plugin.getConfig().getInt("merchant.enchant-min-level", 35) + "+ | Cooldown: 120s", NamedTextColor.RED));
            meta.lore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack createCraftSeloButton() {
        ItemStack item = new ItemStack(Material.FILLED_MAP);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            int ep = plugin.getConfig().getInt("merchant.selo-craft.ender-pearls", 16);
            int hb = plugin.getConfig().getInt("merchant.selo-craft.honey-bottles", 1);
            int fm = plugin.getConfig().getInt("merchant.selo-craft.filled-maps", 1);
            int ll = plugin.getConfig().getInt("merchant.selo-craft.lapis-lazuli", 8);
            int gi = plugin.getConfig().getInt("merchant.selo-craft.gold-ingots", 6);
            meta.displayName(Component.text("Fabricar Selo de Rota", NamedTextColor.GOLD, TextDecoration.BOLD));
            List<Component> lore = new ArrayList<>();
            lore.add(Component.text(""));
            lore.add(Component.text("Clique para fabricar!", NamedTextColor.YELLOW));
            lore.add(Component.text(""));
            lore.add(Component.text("Materiais:", NamedTextColor.WHITE, TextDecoration.UNDERLINED));
            lore.add(Component.text("  - " + ep + "x Ender Pearl", NamedTextColor.AQUA));
            lore.add(Component.text("  - " + hb + "x Pote de Mel", NamedTextColor.AQUA));
            lore.add(Component.text("  - " + fm + "x Mapa Preenchido", NamedTextColor.AQUA));
            lore.add(Component.text("  - " + ll + "x Lapis Lazuli", NamedTextColor.AQUA));
            lore.add(Component.text("  - " + gi + "x Lingote de Ouro", NamedTextColor.AQUA));
            lore.add(Component.text(""));
            lore.add(Component.text("Requer nivel 10+", NamedTextColor.RED));
            meta.lore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack createCancelButton() {
        ItemStack item = new ItemStack(Material.BARRIER);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text("Fechar", NamedTextColor.RED, TextDecoration.BOLD));
            item.setItemMeta(meta);
        }
        return item;
    }

    // ═══════════════════════════════════════════════════
    // Utilitarios de Material
    // ═══════════════════════════════════════════════════

    private boolean hasMaterials(Player player, Material material, int amount) {
        int count = 0;
        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null && item.getType() == material) {
                count += item.getAmount();
                if (count >= amount) return true;
            }
        }
        return false;
    }

    private boolean consumeMaterials(Player player, Material material, int amount) {
        int remaining = amount;
        ItemStack[] contents = player.getInventory().getContents();
        for (int i = 0; i < contents.length && remaining > 0; i++) {
            ItemStack item = contents[i];
            if (item != null && item.getType() == material) {
                int take = Math.min(item.getAmount(), remaining);
                remaining -= take;
                if (take >= item.getAmount()) {
                    player.getInventory().setItem(i, null);
                } else {
                    item.setAmount(item.getAmount() - take);
                }
            }
        }
        return remaining <= 0;
    }

    private boolean hasMMOItemInInventory(Player player, String mmoItemId, int amount) {
        int count = 0;
        for (ItemStack item : player.getInventory().getContents()) {
            if (item == null || item.getType().isAir()) continue;
            try {
                NBTItem nbt = NBTItem.get(item);
                if (nbt.hasType() && mmoItemId.equals(nbt.getString("MMOITEMS_ITEM_ID"))) {
                    count += item.getAmount();
                    if (count >= amount) return true;
                }
            } catch (Exception ignored) {}
        }
        return false;
    }

    private boolean consumeMMOItemFromInventory(Player player, String mmoItemId, int amount) {
        int remaining = amount;
        ItemStack[] contents = player.getInventory().getContents();
        for (int i = 0; i < contents.length && remaining > 0; i++) {
            ItemStack item = contents[i];
            if (item == null || item.getType().isAir()) continue;
            try {
                NBTItem nbt = NBTItem.get(item);
                if (nbt.hasType() && mmoItemId.equals(nbt.getString("MMOITEMS_ITEM_ID"))) {
                    int take = Math.min(item.getAmount(), remaining);
                    remaining -= take;
                    if (take >= item.getAmount()) {
                        player.getInventory().setItem(i, null);
                    } else {
                        item.setAmount(item.getAmount() - take);
                    }
                }
            } catch (Exception ignored) {}
        }
        return remaining <= 0;
    }

    private ItemStack createMMOItem(String type, String id) {
        try {
            Type mmoType = Type.get(type);
            if (mmoType == null) {
                plugin.getLogger().warning("[MERCHANT] Tipo MMOItem nao encontrado: " + type);
                return null;
            }
            ItemStack result = MMOItems.plugin.getItem(mmoType, id);
            if (result == null) {
                plugin.getLogger().warning("[MERCHANT] Item MMOItem nao encontrado: " + type + "/" + id);
            }
            return result;
        } catch (Exception e) {
            plugin.getLogger().warning("[MERCHANT] Erro ao criar MMOItem " + type + "/" + id + ": " + e.getMessage());
            return null;
        }
    }

    // ═══════════════════════════════════════════════════
    // Utilitarios Gerais
    // ═══════════════════════════════════════════════════

    private boolean isGUIDecoration(ItemStack item) {
        if (item == null) return true;
        Material mat = item.getType();
        return mat.name().endsWith("_STAINED_GLASS_PANE")
                || mat == Material.GLASS_PANE
                || mat == Material.BARRIER
                || mat == Material.LIME_CONCRETE
                || mat == Material.ARROW;
    }

    private boolean isSlotEmpty(Inventory inv, int slot) {
        ItemStack item = inv.getItem(slot);
        return item == null || item.getType().isAir();
    }

    private void sendLockedMessage(Player player, String requirement) {
        player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                "&c[Mercador] &lFuncao bloqueada!"));
        player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                "&7Esta funcao precisa de uma &e" + requirement + "&7 num raio de 10 blocos."));
        player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                "&8Veja a pagina 'Pre-requisitos' do livro guia do Mercador."));
        player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
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
            case BARREL -> "Barril";
            case CHEST -> "Bau";
            case LECTERN -> "Estante de Livros";
            case EMERALD_BLOCK -> "Bloco de Esmeralda";
            case CARTOGRAPHY_TABLE -> "Mesa de Cartografia";
            default -> {
                String name = mat.name().toLowerCase().replace("_", " ");
                StringBuilder sb = new StringBuilder();
                for (String word : name.split(" ")) {
                    if (!word.isEmpty()) {
                        sb.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1)).append(" ");
                    }
                }
                yield sb.toString().trim();
            }
        };
    }
}
