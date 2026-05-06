package com.nemonicorp.orbs;

import io.lumine.mythic.lib.api.item.NBTItem;
import net.Indyuce.mmoitems.MMOItems;
import net.Indyuce.mmoitems.api.Type;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Mesa de Transmutacao — exclusiva para a classe Alquimista (MMOCore).
 * Lv5+: pode fabricar orbs usando materiais.
 * Lv35+: pode tambem transmutar equipamentos em orbs.
 */
public class TransmutationTableListener implements Listener {

    private final NemonicOrbPlugin plugin;

    // Sessoes ativas: UUID -> sessao
    private final Map<UUID, TransmutationSession> activeSessions = new ConcurrentHashMap<>();

    // Cooldown por jogador: UUID -> timestamp do ultimo uso (transmutacao)
    private final Map<UUID, Long> cooldowns = new ConcurrentHashMap<>();

    // Cooldown de craft: UUID -> timestamp do ultimo craft
    private final Map<UUID, Long> craftCooldowns = new ConcurrentHashMap<>();

    // Cache de scan ambiental: "world,x,y,z" -> (bonus, timestamp)
    private final Map<String, CachedScan> scanCache = new ConcurrentHashMap<>();

    // Tarefas de aura visual por jogador
    private final Map<UUID, BukkitTask> auraTasks = new ConcurrentHashMap<>();

    // ═══════════════════════════════════════════════════
    // Layout da GUI (27 slots - 3 linhas)
    // ═══════════════════════════════════════════════════
    // Row 0: [⭐][▓][▓][▓][📖][▓][▓][▓][▓]         Info + Eficiencia
    // Row 1: [▓][_mat][ENC][_out][▓][_mat][REF][_out][▓]  Craft ENC + REF
    // Row 2: [▓][_eqp][TRA][_out][_out][▓][▓][▓][▓]      Transmutacao
    // ═══════════════════════════════════════════════════

    private static final int GUI_SIZE = 54;

    // Row 0: Info + Eficiencia
    private static final int SLOT_EFFICIENCY = 4;
    private static final int SLOT_INFO = 8;

    // Row 1: Craft Encantamento
    private static final int SLOT_MAT_ENC = 10;
    private static final int SLOT_ARROW_ENC_1 = 11;
    private static final int SLOT_CRAFT_ENC = 12;
    private static final int SLOT_ARROW_ENC_2 = 13;
    private static final int SLOT_OUT_ENC_1 = 14;
    private static final int SLOT_OUT_ENC_2 = 15;

    // Row 2: Craft Reforco
    private static final int SLOT_MAT_REF = 19;
    private static final int SLOT_ARROW_REF_1 = 20;
    private static final int SLOT_CRAFT_REF = 21;
    private static final int SLOT_ARROW_REF_2 = 22;
    private static final int SLOT_OUT_REF_1 = 23;
    private static final int SLOT_OUT_REF_2 = 24;

    // Row 3: Separador roxo (slots 27-35)

    // Row 4: Transmutacao
    private static final int SLOT_TRANSMUTE_INPUT = 37;
    private static final int SLOT_ARROW_T_1 = 38;
    private static final int SLOT_TRANSMUTE = 39;
    private static final int SLOT_ARROW_T_2 = 40;
    private static final int SLOT_TRANSMUTE_OUT1 = 41;
    private static final int SLOT_TRANSMUTE_OUT2 = 42;

    // Titulo da GUI
    private static final String GUI_TITLE_RAW = "Mesa de Transmutacao";

    // Tipos MMOItems aceitos para transmutacao
    private static final Set<String> ACCEPTED_MMOITEM_TYPES = Set.of(
            "SWORD", "DAGGER", "AXE", "BOW", "CROSSBOW", "STAFF",
            "WAND", "WHIP", "MUSKET", "LUTE", "SPEAR", "GREATSTAFF",
            "GREATSWORD", "HAMMER", "KATANA", "HALBERD", "GAUNTLET",
            "TRIDENT", "MACE", "HELMET", "CHESTPLATE", "LEGGINGS", "BOOTS", "ARMOR"
    );

    // Orbs que podem ser geradas na transmutacao e seus pesos
    private static final String[] ORB_IDS = {
            "PEDRA_DE_ENCANTAMENTO", "PEDRA_DE_REFORCO", "PEDRA_CORROSIVA", "MOEDA_DA_SORTE"
    };
    private static final int[] ORB_WEIGHTS = {40, 30, 15, 3};
    private static final int TOTAL_WEIGHT = 88;

    // Blocos alquimicos e seus bonus {bonusPorBloco, maxBlocos}
    private static final Map<Material, double[]> ALCHEMY_BLOCKS = Map.ofEntries(
            Map.entry(Material.CAULDRON, new double[]{0.05, 6}),
            Map.entry(Material.WATER_CAULDRON, new double[]{0.05, 6}),
            Map.entry(Material.LAVA_CAULDRON, new double[]{0.05, 6}),
            Map.entry(Material.BREWING_STAND, new double[]{0.08, 6}),
            Map.entry(Material.SOUL_SAND, new double[]{0.01, 10}),
            Map.entry(Material.SOUL_SOIL, new double[]{0.01, 10}),
            Map.entry(Material.SOUL_LANTERN, new double[]{0.02, 5}),
            Map.entry(Material.DRAGON_EGG, new double[]{0.50, 1})
    );

    // Thresholds minimos para mostrar indicador de eficiencia
    private static final Map<String, Integer> INDICATOR_THRESHOLDS = Map.of(
            "CAULDRON", 6,
            "BREWING_STAND", 6,
            "SOUL_SAND", 6,
            "SOUL_LANTERN", 5,
            "BOOKSHELF", 30,
            "DRAGON_EGG", 1
    );

    // ═══════════════════════════════════════════════════
    // Receitas de Craft
    // ═══════════════════════════════════════════════════

    record CraftRecipe(Material material, int amount, boolean anyWool) {
        boolean matches(ItemStack item) {
            if (item == null || item.getType().isAir()) return false;
            if (anyWool) {
                return item.getType().name().endsWith("_WOOL") && item.getAmount() >= amount;
            }
            return item.getType() == material && item.getAmount() >= amount;
        }
    }

    // Pedra de Encantamento: 32 carne podre | 32 ossos | 10 ender pearls
    private static final List<CraftRecipe> RECIPES_ENCANTAMENTO = List.of(
            new CraftRecipe(Material.ROTTEN_FLESH, 32, false),
            new CraftRecipe(Material.BONE, 32, false),
            new CraftRecipe(Material.ENDER_PEARL, 10, false)
    );

    // Pedra de Reforco: 8 ovos de tartaruga | 32 couro | 32 la (qualquer cor)
    private static final List<CraftRecipe> RECIPES_REFORCO = List.of(
            new CraftRecipe(Material.TURTLE_EGG, 8, false),
            new CraftRecipe(Material.LEATHER, 32, false),
            new CraftRecipe(null, 32, true) // Qualquer la
    );

    record TransmutationSession(Location tableLoc, EnvironmentBonus bonus, double efficiency, boolean canTransmute) {}
    record EnvironmentBonus(int bookshelves, double bookshelfBonus,
                            Map<Material, Integer> alchemyBlocks, double alchemyBonus,
                            int nearbyPlayers, double playerBonus) {}
    record CachedScan(EnvironmentBonus bonus, long timestamp) {}

    public TransmutationTableListener(NemonicOrbPlugin plugin) {
        this.plugin = plugin;
    }

    private boolean debug() {
        return plugin.getConfig().getBoolean("debug", true);
    }

    // ══════════════════════════════════════════════
    // Deteccao de Classe via Reflexao
    // ══════════════════════════════════════════════

    public boolean isAlquimista(Player player) {
        try {
            String className = getPlayerClassName(player);
            String configClassName = plugin.getConfig().getString("transmutation.class-name", "Alquimista");
            boolean result = className != null && className.equalsIgnoreCase(configClassName);
            if (debug()) plugin.getLogger().info("[TRANSMUTE] isAlquimista: jogador=" + player.getName()
                    + " classeDetectada='" + className + "' configClasse='" + configClassName + "' resultado=" + result);
            return result;
        } catch (Exception e) {
            plugin.getLogger().warning("[TRANSMUTE] Erro ao verificar classe: " + e.getMessage());
            return false;
        }
    }

    private String getPlayerClassName(Player player) {
        try {
            Class<?> pdClass = Class.forName("net.Indyuce.mmocore.api.player.PlayerData");
            Object data = getPlayerData(pdClass, player);
            if (data == null) return null;

            Object playerClass = null;
            for (String methodName : new String[]{"getProfess", "getPlayerClass", "getMMOClass"}) {
                try {
                    playerClass = pdClass.getMethod(methodName).invoke(data);
                    break;
                } catch (NoSuchMethodException ignored) {}
            }
            if (playerClass == null) return null;

            for (String methodName : new String[]{"getName", "getId", "getKey"}) {
                try {
                    Object result = playerClass.getClass().getMethod(methodName).invoke(playerClass);
                    if (result instanceof String s && !s.isEmpty()) return s;
                } catch (NoSuchMethodException ignored) {}
            }
            return null;
        } catch (ClassNotFoundException e) {
            return null;
        } catch (Exception e) {
            plugin.getLogger().warning("[TRANSMUTE] Erro ao obter classe: " + e.getMessage());
            return null;
        }
    }

    public int getAlquimistaLevel(Player player) {
        try {
            Class<?> pdClass = Class.forName("net.Indyuce.mmocore.api.player.PlayerData");
            Object data = getPlayerData(pdClass, player);
            if (data == null) return 0;
            return (int) pdClass.getMethod("getLevel").invoke(data);
        } catch (Exception e) {
            return 0;
        }
    }

    private Object getPlayerData(Class<?> pdClass, Player player) throws Exception {
        try {
            return pdClass.getMethod("get", org.bukkit.OfflinePlayer.class).invoke(null, player);
        } catch (NoSuchMethodException e1) {
            try {
                return pdClass.getMethod("get", Player.class).invoke(null, player);
            } catch (NoSuchMethodException e2) {
                return pdClass.getMethod("get", UUID.class).invoke(null, player.getUniqueId());
            }
        }
    }

    // ══════════════════════════════════════════════
    // Abertura da GUI
    // ══════════════════════════════════════════════

    public void openGUI(Player player, Location tableLoc) {
        int minLevel = plugin.getConfig().getInt("transmutation.min-level", 5);
        int transmuteLevel = plugin.getConfig().getInt("transmutation.transmute-level", 35);
        int playerLevel = getAlquimistaLevel(player);

        if (playerLevel < minLevel) {
            player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                    plugin.getConfig().getString("transmutation.messages.level-too-low",
                            "&c[NemonicOrb] Voce precisa de nivel %level% na classe Alquimista!")
                            .replace("%level%", String.valueOf(minLevel))));
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
            return;
        }

        boolean canTransmute = playerLevel >= transmuteLevel;

        // Escanear ambiente
        EnvironmentBonus bonus = scanEnvironment(tableLoc, player);
        double efficiency = calculateEfficiency(player, bonus);

        Inventory gui = Bukkit.createInventory(null, GUI_SIZE,
                Component.text(GUI_TITLE_RAW, NamedTextColor.GOLD, TextDecoration.BOLD));

        // Preencher tudo com vidro preto
        ItemStack blackGlass = createGlassPane(Material.BLACK_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < GUI_SIZE; i++) {
            gui.setItem(i, blackGlass);
        }

        // === ROW 0: Eficiencia + Info ===
        gui.setItem(SLOT_EFFICIENCY, createEfficiencyItem(bonus, efficiency, playerLevel));
        gui.setItem(SLOT_INFO, createInfoItem(canTransmute));

        ItemStack arrowPane = createArrowPane();

        // === ROW 1: Craft Encantamento ===
        gui.setItem(SLOT_MAT_ENC, null);
        gui.setItem(SLOT_ARROW_ENC_1, arrowPane);
        gui.setItem(SLOT_CRAFT_ENC, createCraftButton("PEDRA_DE_ENCANTAMENTO", RECIPES_ENCANTAMENTO));
        gui.setItem(SLOT_ARROW_ENC_2, arrowPane);
        gui.setItem(SLOT_OUT_ENC_1, null);
        gui.setItem(SLOT_OUT_ENC_2, null);

        // === ROW 2: Craft Reforco ===
        gui.setItem(SLOT_MAT_REF, null);
        gui.setItem(SLOT_ARROW_REF_1, arrowPane);
        gui.setItem(SLOT_CRAFT_REF, createCraftButton("PEDRA_DE_REFORCO", RECIPES_REFORCO));
        gui.setItem(SLOT_ARROW_REF_2, arrowPane);
        gui.setItem(SLOT_OUT_REF_1, null);
        gui.setItem(SLOT_OUT_REF_2, null);

        // === ROW 3: Separador roxo ===
        ItemStack purpleGlass = createGlassPane(Material.PURPLE_STAINED_GLASS_PANE, " ");
        for (int i = 27; i <= 35; i++) {
            gui.setItem(i, purpleGlass);
        }

        // === ROW 4: Transmutacao ===
        if (canTransmute) {
            gui.setItem(SLOT_TRANSMUTE_INPUT, null);
            gui.setItem(SLOT_ARROW_T_1, arrowPane);
            gui.setItem(SLOT_TRANSMUTE, createTransmuteButton(true));
            gui.setItem(SLOT_ARROW_T_2, arrowPane);
            gui.setItem(SLOT_TRANSMUTE_OUT1, null);
            gui.setItem(SLOT_TRANSMUTE_OUT2, null);
        } else {
            ItemStack redGlass = createGlassPane(Material.RED_STAINED_GLASS_PANE, " ");
            for (int i = 36; i <= 44; i++) {
                gui.setItem(i, redGlass);
            }
            gui.setItem(SLOT_TRANSMUTE, createLockedTransmuteButton(transmuteLevel));
        }

        // Registrar sessao
        TransmutationSession session = new TransmutationSession(tableLoc, bonus, efficiency, canTransmute);
        activeSessions.put(player.getUniqueId(), session);

        player.openInventory(gui);
        player.playSound(tableLoc, Sound.BLOCK_ENCHANTMENT_TABLE_USE, 0.8f, 0.5f);

        // Iniciar aura visual
        startAura(player, tableLoc, efficiency);

        if (debug()) plugin.getLogger().info("[TRANSMUTE] GUI aberta para " + player.getName()
                + " nivel=" + playerLevel + " canTransmute=" + canTransmute
                + " eficiencia=" + String.format("%.0f%%", efficiency * 100));
    }

    // ══════════════════════════════════════════════
    // Scan Ambiental
    // ══════════════════════════════════════════════

    private EnvironmentBonus scanEnvironment(Location loc, Player player) {
        String cacheKey = loc.getWorld().getName() + "," + loc.getBlockX() + "," + loc.getBlockY() + "," + loc.getBlockZ();
        long cacheTTL = plugin.getConfig().getLong("transmutation.scan-cache-seconds", 60) * 1000;

        CachedScan cached = scanCache.get(cacheKey);
        if (cached != null && (System.currentTimeMillis() - cached.timestamp()) < cacheTTL) {
            int nearbyPlayers = countNearbyPlayers(loc, player);
            double maxPlayerBonus = plugin.getConfig().getDouble("transmutation.nearby-players.max-bonus", 0.25);
            double perPlayerBonus = plugin.getConfig().getDouble("transmutation.nearby-players.bonus-per-player", 0.05);
            double playerBonus = Math.min(nearbyPlayers * perPlayerBonus, maxPlayerBonus);
            EnvironmentBonus old = cached.bonus();
            return new EnvironmentBonus(old.bookshelves(), old.bookshelfBonus(),
                    old.alchemyBlocks(), old.alchemyBonus(), nearbyPlayers, playerBonus);
        }

        int radius = plugin.getConfig().getInt("transmutation.scan-radius", 20);
        int bookshelves = 0;
        Map<Material, Integer> alchemyBlockCounts = new EnumMap<>(Material.class);

        World world = loc.getWorld();
        int cx = loc.getBlockX(), cy = loc.getBlockY(), cz = loc.getBlockZ();

        for (int x = cx - radius; x <= cx + radius; x++) {
            for (int y = Math.max(world.getMinHeight(), cy - radius); y <= Math.min(world.getMaxHeight() - 1, cy + radius); y++) {
                for (int z = cz - radius; z <= cz + radius; z++) {
                    double distSq = (x - cx) * (x - cx) + (y - cy) * (y - cy) + (z - cz) * (z - cz);
                    if (distSq > radius * radius) continue;

                    Block block = world.getBlockAt(x, y, z);
                    Material mat = block.getType();

                    if (mat == Material.BOOKSHELF) {
                        bookshelves++;
                    } else if (ALCHEMY_BLOCKS.containsKey(mat)) {
                        alchemyBlockCounts.merge(mat, 1, Integer::sum);
                    }
                }
            }
        }

        double bookshelfBonusPerBlock = plugin.getConfig().getDouble("transmutation.bookshelf.bonus-per-block", 0.01);
        double maxBookshelfBonus = plugin.getConfig().getDouble("transmutation.bookshelf.max-bonus", 0.40);
        double bookshelfBonus = Math.min(bookshelves * bookshelfBonusPerBlock, maxBookshelfBonus);

        double alchemyBonus = 0;
        for (Map.Entry<Material, Integer> entry : alchemyBlockCounts.entrySet()) {
            double[] config = ALCHEMY_BLOCKS.get(entry.getKey());
            if (config == null) continue;
            int count = Math.min(entry.getValue(), (int) config[1]);
            alchemyBonus += count * config[0];
        }

        int nearbyPlayers = countNearbyPlayers(loc, player);
        double maxPlayerBonus = plugin.getConfig().getDouble("transmutation.nearby-players.max-bonus", 0.25);
        double perPlayerBonus = plugin.getConfig().getDouble("transmutation.nearby-players.bonus-per-player", 0.05);
        double playerBonus = Math.min(nearbyPlayers * perPlayerBonus, maxPlayerBonus);

        EnvironmentBonus bonus = new EnvironmentBonus(bookshelves, bookshelfBonus,
                alchemyBlockCounts, alchemyBonus, nearbyPlayers, playerBonus);
        scanCache.put(cacheKey, new CachedScan(bonus, System.currentTimeMillis()));

        if (debug()) plugin.getLogger().info("[TRANSMUTE-SCAN] estantes=" + bookshelves
                + " (+=" + String.format("%.0f%%", bookshelfBonus * 100)
                + ") alquimia=" + String.format("%.0f%%", alchemyBonus * 100)
                + " jogadores=" + nearbyPlayers + " (+=" + String.format("%.0f%%", playerBonus * 100) + ")");

        return bonus;
    }

    private int countNearbyPlayers(Location loc, Player exclude) {
        int radius = plugin.getConfig().getInt("transmutation.scan-radius", 20);
        int count = 0;
        for (Player p : loc.getWorld().getPlayers()) {
            if (p.equals(exclude)) continue;
            if (p.getLocation().distanceSquared(loc) <= radius * radius) count++;
        }
        return count;
    }

    private double calculateEfficiency(Player player, EnvironmentBonus bonus) {
        double base = 1.0 + bonus.bookshelfBonus() + bonus.alchemyBonus() + bonus.playerBonus();
        int level = getAlquimistaLevel(player);
        double levelMult = 1.0;
        if (level >= 100) levelMult = 1.5;
        else if (level >= 75) levelMult = 1.3;
        else if (level >= 50) levelMult = 1.15;
        return base * levelMult;
    }

    // ══════════════════════════════════════════════
    // Validacao de Input (Transmutacao)
    // ══════════════════════════════════════════════

    private boolean isValidTransmuteInput(ItemStack item) {
        if (item == null || item.getType().isAir()) return false;
        NBTItem nbt = NBTItem.get(item);

        // Rejeitar orbs
        if (nbt.hasTag("MMOITEMS_ITEM_ID")) {
            String itemId = nbt.getString("MMOITEMS_ITEM_ID");
            List<String> orbIds = plugin.getConfig().getStringList("orb-ids");
            if (orbIds.contains(itemId)) return false;
        }

        if (nbt.hasTag(OrbListener.NBT_TIER)) return true;

        if (nbt.hasTag("MMOITEMS_ITEM_TYPE")) {
            String type = nbt.getString("MMOITEMS_ITEM_TYPE");
            return type != null && ACCEPTED_MMOITEM_TYPES.contains(type);
        }
        return false;
    }

    private int getBaseOrbs(ItemStack item) {
        NBTItem nbt = NBTItem.get(item);
        if (nbt.hasTag(OrbListener.NBT_TIER)) {
            int tier = nbt.getInteger(OrbListener.NBT_TIER);
            return tier == 3 ? 2 : 1;
        }
        return 1;
    }

    // ══════════════════════════════════════════════
    // Rolagem de Orbs (Transmutacao)
    // ══════════════════════════════════════════════

    private List<ItemStack> rollOrbs(int count) {
        List<ItemStack> orbs = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            String orbId = rollSingleOrb();
            ItemStack orbItem = createOrbItem(orbId);
            if (orbItem != null) orbs.add(orbItem);
        }
        return orbs;
    }

    private String rollSingleOrb() {
        int roll = ThreadLocalRandom.current().nextInt(TOTAL_WEIGHT);
        int cumulative = 0;
        for (int i = 0; i < ORB_IDS.length; i++) {
            cumulative += ORB_WEIGHTS[i];
            if (roll < cumulative) return ORB_IDS[i];
        }
        return ORB_IDS[0];
    }

    private ItemStack createOrbItem(String orbId) {
        try {
            Type consumableType = Type.get("CONSUMABLE");
            if (consumableType == null) return null;
            return MMOItems.plugin.getItem(consumableType, orbId);
        } catch (Exception e) {
            plugin.getLogger().warning("[TRANSMUTE] Erro ao criar orb " + orbId + ": " + e.getMessage());
            return null;
        }
    }

    // ══════════════════════════════════════════════
    // Event Handlers
    // ══════════════════════════════════════════════

    // Slots onde o jogador pode colocar/retirar itens
    private static final Set<Integer> INPUT_SLOTS = Set.of(SLOT_MAT_ENC, SLOT_MAT_REF);
    private static final Set<Integer> OUTPUT_SLOTS = Set.of(SLOT_OUT_ENC_1, SLOT_OUT_ENC_2, SLOT_OUT_REF_1, SLOT_OUT_REF_2, SLOT_TRANSMUTE_OUT1, SLOT_TRANSMUTE_OUT2);
    private static final Set<Integer> TRANSMUTE_SLOTS = Set.of(SLOT_TRANSMUTE_INPUT, SLOT_TRANSMUTE_OUT1, SLOT_TRANSMUTE_OUT2);

    @EventHandler(priority = EventPriority.HIGH)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        String title = getInventoryTitle(event.getView());
        if (!GUI_TITLE_RAW.equals(title)) return;

        TransmutationSession session = activeSessions.get(player.getUniqueId());
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

        // Block number-key swaps targeting GUI slots (anti-dupe)
        if (event.getHotbarButton() >= 0 && slot < GUI_SIZE) {
            if (!INPUT_SLOTS.contains(slot)
                    && !(slot == SLOT_TRANSMUTE_INPUT && session.canTransmute())) {
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
                // Tentar mover para slot de material ENC
                if (isSlotEmpty(topInv, SLOT_MAT_ENC)) {
                    topInv.setItem(SLOT_MAT_ENC, clicked.clone());
                    event.setCurrentItem(null);
                    return;
                }
                // Tentar mover para slot de material REF
                if (isSlotEmpty(topInv, SLOT_MAT_REF)) {
                    topInv.setItem(SLOT_MAT_REF, clicked.clone());
                    event.setCurrentItem(null);
                    return;
                }
                // Tentar mover para slot de transmutacao
                if (session.canTransmute() && isSlotEmpty(topInv, SLOT_TRANSMUTE_INPUT)) {
                    topInv.setItem(SLOT_TRANSMUTE_INPUT, clicked.clone());
                    event.setCurrentItem(null);
                    return;
                }
            }
            return;
        }

        // Slots de input de material — permitir interacao
        if (INPUT_SLOTS.contains(slot)) return;

        // Slot de transmutacao input — permitir se canTransmute
        if (slot == SLOT_TRANSMUTE_INPUT && session.canTransmute()) return;

        // Slots de output — permitir retirar itens reais (shift-click seguro)
        if (OUTPUT_SLOTS.contains(slot)) {
            // Bloquear se transmutacao esta trancada e e slot de transmutacao
            if (!session.canTransmute() && TRANSMUTE_SLOTS.contains(slot)) {
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
        if (slot == SLOT_CRAFT_ENC) {
            handleCraft(player, topInv, session, "PEDRA_DE_ENCANTAMENTO", RECIPES_ENCANTAMENTO,
                    SLOT_MAT_ENC, new int[]{SLOT_OUT_ENC_1, SLOT_OUT_ENC_2});
        } else if (slot == SLOT_CRAFT_REF) {
            handleCraft(player, topInv, session, "PEDRA_DE_REFORCO", RECIPES_REFORCO,
                    SLOT_MAT_REF, new int[]{SLOT_OUT_REF_1, SLOT_OUT_REF_2});
        } else if (slot == SLOT_TRANSMUTE && session.canTransmute()) {
            handleTransmute(player, topInv, session);
        } else if (slot == SLOT_TRANSMUTE && !session.canTransmute()) {
            int transmuteLevel = plugin.getConfig().getInt("transmutation.transmute-level", 35);
            player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                    "&c[NemonicOrb] Requer nivel " + transmuteLevel + " na classe Alquimista!"));
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
        }
    }

    private boolean isSlotEmpty(Inventory inv, int slot) {
        ItemStack item = inv.getItem(slot);
        return item == null || item.getType().isAir();
    }

    private void syncInventoryLater(Player player) {
        Bukkit.getScheduler().runTask(plugin, () -> {
            if (player.isOnline()) player.updateInventory();
        });
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onInventoryDrag(InventoryDragEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        String title = getInventoryTitle(event.getView());
        if (!GUI_TITLE_RAW.equals(title)) return;
        TransmutationSession session = activeSessions.get(player.getUniqueId());
        if (session == null) {
            event.setCancelled(true);
            return;
        }

        Set<Integer> allowed = new HashSet<>(INPUT_SLOTS);
        if (session.canTransmute()) allowed.add(SLOT_TRANSMUTE_INPUT);
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
        TransmutationSession session = activeSessions.remove(player.getUniqueId());
        if (session == null) return;

        String title = getInventoryTitle(event.getView());
        if (!GUI_TITLE_RAW.equals(title)) return;

        stopAura(player);

        Inventory inv = event.getView().getTopInventory();
        // Devolver materiais e outputs (ignorar decoracoes)
        // Clear slot BEFORE returning to prevent duplication on fast close
        int[] returnSlots = {SLOT_MAT_ENC, SLOT_OUT_ENC_1, SLOT_OUT_ENC_2, SLOT_MAT_REF, SLOT_OUT_REF_1, SLOT_OUT_REF_2,
                SLOT_TRANSMUTE_INPUT, SLOT_TRANSMUTE_OUT1, SLOT_TRANSMUTE_OUT2};
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

    // ══════════════════════════════════════════════
    // Logica de Craft (Fabricacao de Orbs)
    // ══════════════════════════════════════════════

    private void handleCraft(Player player, Inventory gui, TransmutationSession session,
                             String orbId, List<CraftRecipe> recipes, int materialSlot, int[] outputSlots) {
        // Verificar cooldown
        long craftCooldownMs = plugin.getConfig().getLong("transmutation.craft-cooldown-seconds", 10) * 1000;
        Long lastCraft = craftCooldowns.get(player.getUniqueId());
        long now = System.currentTimeMillis();
        if (lastCraft != null && (now - lastCraft) < craftCooldownMs) {
            long remaining = (craftCooldownMs - (now - lastCraft)) / 1000;
            player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                    "&c[NemonicOrb] Aguarde " + remaining + "s antes de fabricar novamente!"));
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
            return;
        }

        // Verificar outputs vazios
        for (int slot : outputSlots) {
            ItemStack existing = gui.getItem(slot);
            if (existing != null && !existing.getType().isAir()) {
                player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                        "&c[NemonicOrb] Retire as orbs dos slots de saida primeiro!"));
                player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
                return;
            }
        }

        // Verificar material no slot de input
        ItemStack material = gui.getItem(materialSlot);
        CraftRecipe matchedRecipe = null;
        for (CraftRecipe recipe : recipes) {
            if (recipe.matches(material)) {
                matchedRecipe = recipe;
                break;
            }
        }

        if (matchedRecipe == null) {
            player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                    "&c[NemonicOrb] Coloque o material no slot e clique no botao!"));
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
            return;
        }

        // Consumir material
        int amountToConsume = matchedRecipe.amount();
        int remaining = material.getAmount() - amountToConsume;
        if (remaining > 0) {
            material.setAmount(remaining);
        } else {
            gui.setItem(materialSlot, null);
        }

        // Calcular quantidade de orbs baseado na eficiencia
        double yieldMultiplier = plugin.getConfig().getDouble("transmutation.orb-yield-multiplier", 0.55);
        int totalOrbs = Math.max(1, (int) Math.floor(session.efficiency() * Math.max(0.1, yieldMultiplier)));

        // Gerar orbs
        List<ItemStack> orbs = new ArrayList<>();
        for (int i = 0; i < totalOrbs; i++) {
            ItemStack orb = createOrbItem(orbId);
            if (orb != null) orbs.add(orb);
        }

        if (orbs.isEmpty()) {
            player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                    "&c[NemonicOrb] Erro ao gerar orbs!"));
            return;
        }

        // Colocar nos slots de output, extras no inventario
        for (int i = 0; i < orbs.size(); i++) {
            if (i < outputSlots.length) {
                gui.setItem(outputSlots[i], orbs.get(i));
            } else {
                Map<Integer, ItemStack> leftover = player.getInventory().addItem(orbs.get(i));
                for (ItemStack drop : leftover.values()) {
                    player.getWorld().dropItemNaturally(player.getLocation(), drop);
                }
            }
        }

        craftCooldowns.put(player.getUniqueId(), now);

        Location tableLoc = session.tableLoc();
        player.playSound(tableLoc, Sound.BLOCK_ENCHANTMENT_TABLE_USE, 1.0f, 1.2f);
        tableLoc.getWorld().spawnParticle(Particle.ENCHANT,
                tableLoc.clone().add(0.5, 1.5, 0.5), 30, 0.3, 0.3, 0.3, 1.0);

        player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                "&a[NemonicOrb] Fabricacao concluida! " + totalOrbs + " orb(s) gerada(s)."));

        if (debug()) plugin.getLogger().info("[TRANSMUTE-CRAFT] " + player.getName()
                + " fabricou " + totalOrbs + "x " + orbId);
    }

    // ══════════════════════════════════════════════
    // Logica de Transmutacao
    // ══════════════════════════════════════════════

    private void handleTransmute(Player player, Inventory gui, TransmutationSession session) {
        long cooldownMs = plugin.getConfig().getLong("transmutation.cooldown-seconds", 120) * 1000;
        Long lastUse = cooldowns.get(player.getUniqueId());
        long now = System.currentTimeMillis();
        if (lastUse != null && (now - lastUse) < cooldownMs) {
            long remaining = (cooldownMs - (now - lastUse)) / 1000;
            player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                    plugin.getConfig().getString("transmutation.messages.cooldown",
                            "&c[NemonicOrb] Aguarde %seconds%s antes de transmutar novamente!")
                            .replace("%seconds%", String.valueOf(remaining))));
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
            return;
        }

        ItemStack input = gui.getItem(SLOT_TRANSMUTE_INPUT);
        if (!isValidTransmuteInput(input)) {
            player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                    "&c[NemonicOrb] Coloque um equipamento no slot e clique no rebolo!"));
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
            return;
        }

        // Verificar outputs vazios
        ItemStack out1 = gui.getItem(SLOT_TRANSMUTE_OUT1);
        ItemStack out2 = gui.getItem(SLOT_TRANSMUTE_OUT2);
        if ((out1 != null && !out1.getType().isAir()) || (out2 != null && !out2.getType().isAir())) {
            player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                    "&c[NemonicOrb] Retire as orbs dos slots de saida primeiro!"));
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
            return;
        }

        int baseOrbs = getBaseOrbs(input);
        double yieldMultiplier = plugin.getConfig().getDouble("transmutation.orb-yield-multiplier", 0.55);
        int totalOrbs = Math.max(1, (int) Math.floor(baseOrbs * session.efficiency() * Math.max(0.1, yieldMultiplier)));

        List<ItemStack> orbs = rollOrbs(totalOrbs);
        if (orbs.isEmpty()) {
            player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                    "&c[NemonicOrb] Erro ao gerar orbs!"));
            return;
        }

        // Consumir equipamento
        gui.setItem(SLOT_TRANSMUTE_INPUT, null);

        // Colocar orbs nos 2 slots de output, extras no inventario
        int[] outSlots = {SLOT_TRANSMUTE_OUT1, SLOT_TRANSMUTE_OUT2};
        for (int i = 0; i < orbs.size(); i++) {
            if (i < outSlots.length) {
                gui.setItem(outSlots[i], orbs.get(i));
            } else {
                Map<Integer, ItemStack> leftover = player.getInventory().addItem(orbs.get(i));
                for (ItemStack drop : leftover.values()) {
                    player.getWorld().dropItemNaturally(player.getLocation(), drop);
                }
            }
        }

        cooldowns.put(player.getUniqueId(), now);

        Location tableLoc = session.tableLoc();
        player.playSound(tableLoc, Sound.BLOCK_ENCHANTMENT_TABLE_USE, 1.0f, 1.0f);
        player.playSound(tableLoc, Sound.BLOCK_BREWING_STAND_BREW, 0.8f, 1.2f);
        tableLoc.getWorld().spawnParticle(Particle.ENCHANT,
                tableLoc.clone().add(0.5, 1.5, 0.5), 50, 0.5, 0.5, 0.5, 1.0);
        tableLoc.getWorld().spawnParticle(Particle.WITCH,
                player.getLocation().add(0, 1, 0), 30, 0.3, 0.5, 0.3, 0.1);

        player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                plugin.getConfig().getString("transmutation.messages.transmute-success",
                        "&a[NemonicOrb] Transmutacao concluida! %count% orb(s) gerada(s).")
                        .replace("%count%", String.valueOf(orbs.size()))));

        if (debug()) plugin.getLogger().info("[TRANSMUTE] " + player.getName()
                + " transmutou item, gerou " + orbs.size() + " orbs");
    }

    // ══════════════════════════════════════════════
    // Criacao de Itens da GUI
    // ══════════════════════════════════════════════

    private boolean isGUIDecoration(ItemStack item) {
        if (item == null) return true;
        Material mat = item.getType();
        return mat.name().endsWith("_STAINED_GLASS_PANE")
                || mat == Material.GLASS_PANE
                || mat == Material.BARRIER
                || mat == Material.LIME_CONCRETE
                || mat == Material.ARROW;
    }

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
            meta.displayName(Component.text("\u27A1", NamedTextColor.GREEN, TextDecoration.BOLD));
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack createCraftButton(String orbId, List<CraftRecipe> recipes) {
        Material displayMat = orbId.equals("PEDRA_DE_ENCANTAMENTO")
                ? Material.ENCHANTING_TABLE : Material.ANVIL;
        ItemStack item = new ItemStack(displayMat);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            String displayName = orbId.equals("PEDRA_DE_ENCANTAMENTO")
                    ? "Fabricar Pedra de Encantamento" : "Fabricar Pedra de Reforco";
            meta.displayName(Component.text(displayName, NamedTextColor.GREEN, TextDecoration.BOLD));

            List<Component> lore = new ArrayList<>();
            lore.add(Component.text(""));
            lore.add(Component.text("Clique para fabricar!", NamedTextColor.YELLOW));
            lore.add(Component.text("Tenha os materiais no inventario.", NamedTextColor.GRAY));
            lore.add(Component.text(""));
            lore.add(Component.text("Receitas aceitas:", NamedTextColor.WHITE, TextDecoration.UNDERLINED));

            for (CraftRecipe recipe : recipes) {
                String matName;
                if (recipe.anyWool()) {
                    matName = recipe.amount() + "x La (qualquer cor)";
                } else {
                    matName = recipe.amount() + "x " + formatMaterialName(recipe.material());
                }
                lore.add(Component.text("  - " + matName, NamedTextColor.AQUA));
            }

            meta.lore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack createTransmuteButton(boolean active) {
        ItemStack item = new ItemStack(Material.GRINDSTONE);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text("Transmutar Equipamento", NamedTextColor.LIGHT_PURPLE, TextDecoration.BOLD));
            List<Component> lore = new ArrayList<>();
            lore.add(Component.text(""));
            lore.add(Component.text("Clique para transmutar!", NamedTextColor.YELLOW));
            lore.add(Component.text("Destroi um equipamento do seu", NamedTextColor.GRAY));
            lore.add(Component.text("inventario e gera orbs aleatorias.", NamedTextColor.GRAY));
            lore.add(Component.text(""));
            lore.add(Component.text("Aceita: Equipamentos com orbs ou MMOItem", NamedTextColor.AQUA));
            lore.add(Component.text("Cooldown: 2 minutos", NamedTextColor.RED));
            meta.lore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack createLockedTransmuteButton(int requiredLevel) {
        ItemStack item = new ItemStack(Material.RED_STAINED_GLASS_PANE);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text("Transmutacao Bloqueada", NamedTextColor.RED, TextDecoration.BOLD));
            List<Component> lore = new ArrayList<>();
            lore.add(Component.text(""));
            lore.add(Component.text("Requer Nivel " + requiredLevel + " Alquimista", NamedTextColor.GRAY));
            meta.lore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack createInfoItem(boolean canTransmute) {
        ItemStack item = new ItemStack(Material.ENCHANTED_BOOK);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text("Mesa de Transmutacao", NamedTextColor.GOLD, TextDecoration.BOLD));
            List<Component> lore = new ArrayList<>();
            lore.add(Component.text(""));
            lore.add(Component.text("Fabricacao de Orbs:", NamedTextColor.GREEN, TextDecoration.BOLD));
            lore.add(Component.text("Tenha materiais no inventario e", NamedTextColor.GRAY));
            lore.add(Component.text("clique no botao da orb desejada.", NamedTextColor.GRAY));
            if (canTransmute) {
                lore.add(Component.text(""));
                lore.add(Component.text("Transmutacao:", NamedTextColor.LIGHT_PURPLE, TextDecoration.BOLD));
                lore.add(Component.text("Tenha um equipamento no inventario", NamedTextColor.GRAY));
                lore.add(Component.text("e clique no rebolo para transmutar.", NamedTextColor.GRAY));
            }
            meta.lore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack createEfficiencyItem(EnvironmentBonus bonus, double efficiency, int playerLevel) {
        ItemStack item = new ItemStack(Material.NETHER_STAR);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text("Eficiencia: " + String.format("%.0f%%", efficiency * 100),
                    NamedTextColor.GREEN, TextDecoration.BOLD));

            List<Component> lore = new ArrayList<>();
            lore.add(Component.text(""));

            // Alquimistas Proximos — sempre mostra
            lore.add(Component.text("\u2726 Alquimistas Proximos: " + bonus.nearbyPlayers()
                    + " (+" + String.format("%.0f%%", bonus.playerBonus() * 100) + ")", NamedTextColor.YELLOW));

            // Estantes — so mostra com 30+
            if (bonus.bookshelves() >= INDICATOR_THRESHOLDS.getOrDefault("BOOKSHELF", 30)) {
                lore.add(Component.text("\u2726 Estantes: " + bonus.bookshelves()
                        + " (+" + String.format("%.0f%%", bonus.bookshelfBonus() * 100) + ")", NamedTextColor.AQUA));
            }

            // Caldeiroes
            int totalCauldrons = 0;
            for (Material m : new Material[]{Material.CAULDRON, Material.WATER_CAULDRON, Material.LAVA_CAULDRON}) {
                totalCauldrons += bonus.alchemyBlocks().getOrDefault(m, 0);
            }
            if (totalCauldrons >= INDICATOR_THRESHOLDS.getOrDefault("CAULDRON", 6)) {
                int effective = Math.min(totalCauldrons, 6);
                double cauldronBonus = effective * 0.05;
                lore.add(Component.text("\u2726 Caldeiroes: " + totalCauldrons
                        + " (+" + String.format("%.0f%%", cauldronBonus * 100) + ")", NamedTextColor.DARK_PURPLE));
            }

            // Brewing Stand
            int brewingStands = bonus.alchemyBlocks().getOrDefault(Material.BREWING_STAND, 0);
            if (brewingStands >= INDICATOR_THRESHOLDS.getOrDefault("BREWING_STAND", 6)) {
                int effective = Math.min(brewingStands, 6);
                double brewBonus = effective * 0.08;
                lore.add(Component.text("\u2726 Suportes de Pocao: " + brewingStands
                        + " (+" + String.format("%.0f%%", brewBonus * 100) + ")", NamedTextColor.DARK_PURPLE));
            }

            // Soul Sand + Soul Soil
            int totalSoulSand = bonus.alchemyBlocks().getOrDefault(Material.SOUL_SAND, 0)
                    + bonus.alchemyBlocks().getOrDefault(Material.SOUL_SOIL, 0);
            if (totalSoulSand >= INDICATOR_THRESHOLDS.getOrDefault("SOUL_SAND", 6)) {
                int effective = Math.min(totalSoulSand, 10);
                double soulBonus = effective * 0.01;
                lore.add(Component.text("\u2726 Areia das Almas: " + totalSoulSand
                        + " (+" + String.format("%.0f%%", soulBonus * 100) + ")", NamedTextColor.DARK_PURPLE));
            }

            // Soul Lantern
            int soulLanterns = bonus.alchemyBlocks().getOrDefault(Material.SOUL_LANTERN, 0);
            if (soulLanterns >= INDICATOR_THRESHOLDS.getOrDefault("SOUL_LANTERN", 5)) {
                int effective = Math.min(soulLanterns, 5);
                double lanternBonus = effective * 0.02;
                lore.add(Component.text("\u2726 Lanternas das Almas: " + soulLanterns
                        + " (+" + String.format("%.0f%%", lanternBonus * 100) + ")", NamedTextColor.DARK_PURPLE));
            }

            // Dragon Egg
            int dragonEggs = bonus.alchemyBlocks().getOrDefault(Material.DRAGON_EGG, 0);
            if (dragonEggs >= INDICATOR_THRESHOLDS.getOrDefault("DRAGON_EGG", 1)) {
                lore.add(Component.text("\u2726 Ovo de Dragao: " + dragonEggs
                        + " (+50%)", NamedTextColor.LIGHT_PURPLE));
            }

            // Nivel Alquimista
            double levelMult = 1.0;
            if (playerLevel >= 100) levelMult = 1.5;
            else if (playerLevel >= 75) levelMult = 1.3;
            else if (playerLevel >= 50) levelMult = 1.15;
            lore.add(Component.text(""));
            lore.add(Component.text("\u2605 Nivel Alquimista: " + playerLevel
                    + " (x" + String.format("%.2f", levelMult) + ")", NamedTextColor.LIGHT_PURPLE));

            meta.lore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }

    // ══════════════════════════════════════════════
    // Sistema de Aura Visual
    // ══════════════════════════════════════════════

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
            if (!player.isOnline()) { stopAura(player); return; }

            World world = tableLoc.getWorld();
            if (world == null) return;

            double cx = tableLoc.getX() + 0.5;
            double cy = tableLoc.getY() + 1.0;
            double cz = tableLoc.getZ() + 0.5;

            Color color = switch (level) {
                case 1 -> Color.fromRGB(180, 180, 180);
                case 2 -> Color.fromRGB(60, 120, 255);
                case 3 -> Color.fromRGB(160, 50, 220);
                case 4 -> Color.fromRGB(255, 200, 50);
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

            if (level == 4) {
                for (int i = 0; i < 8; i++) {
                    double angle = (2 * Math.PI / 8) * i + (System.currentTimeMillis() % 5000) / 5000.0 * 2 * Math.PI;
                    double px = cx + radius * Math.cos(angle);
                    double pz = cz + radius * Math.sin(angle);
                    world.spawnParticle(Particle.END_ROD, px, cy + 1.0, pz, 2, 0.1, 0.3, 0.1, 0.02);
                }

                for (Player nearby : world.getPlayers()) {
                    if (nearby.getLocation().distanceSquared(tableLoc) <= 12.5 * 12.5) {
                        if (!nearby.hasPotionEffect(PotionEffectType.REGENERATION)) {
                            nearby.addPotionEffect(new PotionEffect(
                                    PotionEffectType.REGENERATION, 100, 0, true, true, true));
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

    // ══════════════════════════════════════════════
    // Utilitarios
    // ══════════════════════════════════════════════

    private String getInventoryTitle(org.bukkit.inventory.InventoryView view) {
        try {
            Component titleComp = view.title();
            return net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText().serialize(titleComp);
        } catch (Exception e) {
            return "";
        }
    }

    private String formatMaterialName(Material mat) {
        if (mat == null) return "?";
        String name = mat.name().toLowerCase().replace("_", " ");
        StringBuilder sb = new StringBuilder();
        for (String word : name.split(" ")) {
            if (!word.isEmpty()) {
                sb.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1)).append(" ");
            }
        }
        return sb.toString().trim();
    }
}
