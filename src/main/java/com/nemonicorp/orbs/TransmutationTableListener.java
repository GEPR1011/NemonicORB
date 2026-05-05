/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
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
 *  org.bukkit.block.Block
 *  org.bukkit.entity.HumanEntity
 *  org.bukkit.entity.Player
 *  org.bukkit.event.EventHandler
 *  org.bukkit.event.EventPriority
 *  org.bukkit.event.Listener
 *  org.bukkit.event.inventory.ClickType
 *  org.bukkit.event.inventory.InventoryAction
 *  org.bukkit.event.inventory.InventoryClickEvent
 *  org.bukkit.event.inventory.InventoryCloseEvent
 *  org.bukkit.event.inventory.InventoryDragEvent
 *  org.bukkit.inventory.Inventory
 *  org.bukkit.inventory.InventoryView
 *  org.bukkit.inventory.ItemStack
 *  org.bukkit.inventory.meta.ItemMeta
 *  org.bukkit.plugin.Plugin
 *  org.bukkit.potion.PotionEffect
 *  org.bukkit.potion.PotionEffectType
 *  org.bukkit.scheduler.BukkitTask
 */
package com.nemonicorp.orbs;

import com.nemonicorp.orbs.NemonicOrbPlugin;
import io.lumine.mythic.lib.api.item.NBTItem;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import net.Indyuce.mmoitems.MMOItems;
import net.Indyuce.mmoitems.api.Type;
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
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitTask;

public class TransmutationTableListener
implements Listener {
    private final NemonicOrbPlugin plugin;
    private final Map<UUID, TransmutationSession> activeSessions = new ConcurrentHashMap<UUID, TransmutationSession>();
    private final Map<UUID, Long> cooldowns = new ConcurrentHashMap<UUID, Long>();
    private final Map<UUID, Long> craftCooldowns = new ConcurrentHashMap<UUID, Long>();
    private final Map<String, CachedScan> scanCache = new ConcurrentHashMap<String, CachedScan>();
    private final Map<UUID, BukkitTask> auraTasks = new ConcurrentHashMap<UUID, BukkitTask>();
    private static final int GUI_SIZE = 54;
    private static final int SLOT_EFFICIENCY = 4;
    private static final int SLOT_INFO = 8;
    private static final int SLOT_MAT_ENC = 10;
    private static final int SLOT_ARROW_ENC_1 = 11;
    private static final int SLOT_CRAFT_ENC = 12;
    private static final int SLOT_ARROW_ENC_2 = 13;
    private static final int SLOT_OUT_ENC_1 = 14;
    private static final int SLOT_OUT_ENC_2 = 15;
    private static final int SLOT_MAT_REF = 19;
    private static final int SLOT_ARROW_REF_1 = 20;
    private static final int SLOT_CRAFT_REF = 21;
    private static final int SLOT_ARROW_REF_2 = 22;
    private static final int SLOT_OUT_REF_1 = 23;
    private static final int SLOT_OUT_REF_2 = 24;
    private static final int SLOT_TRANSMUTE_INPUT = 37;
    private static final int SLOT_ARROW_T_1 = 38;
    private static final int SLOT_TRANSMUTE = 39;
    private static final int SLOT_ARROW_T_2 = 40;
    private static final int SLOT_TRANSMUTE_OUT1 = 41;
    private static final int SLOT_TRANSMUTE_OUT2 = 42;
    private static final String GUI_TITLE_RAW = "Mesa de Transmutacao";
    private static final Set<String> ACCEPTED_MMOITEM_TYPES = Set.of("SWORD", "DAGGER", "AXE", "BOW", "CROSSBOW", "STAFF", "WAND", "WHIP", "MUSKET", "LUTE", "SPEAR", "GREATSTAFF", "GREATSWORD", "HAMMER", "KATANA", "HALBERD", "GAUNTLET", "TRIDENT", "MACE", "HELMET", "CHESTPLATE", "LEGGINGS", "BOOTS", "ARMOR");
    private static final String[] ORB_IDS = new String[]{"PEDRA_DE_ENCANTAMENTO", "PEDRA_DE_REFORCO", "PEDRA_CORROSIVA", "MOEDA_DA_SORTE"};
    private static final int[] ORB_WEIGHTS = new int[]{40, 30, 15, 3};
    private static final int TOTAL_WEIGHT = 88;
    private static final Map<Material, double[]> ALCHEMY_BLOCKS = Map.ofEntries(Map.entry(Material.CAULDRON, new double[]{0.05, 6.0}), Map.entry(Material.WATER_CAULDRON, new double[]{0.05, 6.0}), Map.entry(Material.LAVA_CAULDRON, new double[]{0.05, 6.0}), Map.entry(Material.BREWING_STAND, new double[]{0.08, 6.0}), Map.entry(Material.SOUL_SAND, new double[]{0.01, 10.0}), Map.entry(Material.SOUL_SOIL, new double[]{0.01, 10.0}), Map.entry(Material.SOUL_LANTERN, new double[]{0.02, 5.0}), Map.entry(Material.DRAGON_EGG, new double[]{0.5, 1.0}));
    private static final Map<String, Integer> INDICATOR_THRESHOLDS = Map.of("CAULDRON", 6, "BREWING_STAND", 6, "SOUL_SAND", 6, "SOUL_LANTERN", 5, "BOOKSHELF", 30, "DRAGON_EGG", 1);
    private static final List<CraftRecipe> RECIPES_ENCANTAMENTO = List.of(new CraftRecipe(Material.ROTTEN_FLESH, 32, false), new CraftRecipe(Material.BONE, 32, false), new CraftRecipe(Material.ENDER_PEARL, 10, false));
    private static final List<CraftRecipe> RECIPES_REFORCO = List.of(new CraftRecipe(Material.TURTLE_EGG, 8, false), new CraftRecipe(Material.LEATHER, 32, false), new CraftRecipe(null, 32, true));
    private static final Set<Integer> INPUT_SLOTS = Set.of(Integer.valueOf(10), Integer.valueOf(19));
    private static final Set<Integer> OUTPUT_SLOTS = Set.of(Integer.valueOf(14), Integer.valueOf(15), Integer.valueOf(23), Integer.valueOf(24), Integer.valueOf(41), Integer.valueOf(42));
    private static final Set<Integer> TRANSMUTE_SLOTS = Set.of(Integer.valueOf(37), Integer.valueOf(41), Integer.valueOf(42));

    public TransmutationTableListener(NemonicOrbPlugin plugin) {
        this.plugin = plugin;
    }

    private boolean debug() {
        return this.plugin.getConfig().getBoolean("debug", true);
    }

    public boolean isAlquimista(Player player) {
        try {
            boolean result;
            String className = this.getPlayerClassName(player);
            String configClassName = this.plugin.getConfig().getString("transmutation.class-name", "Alquimista");
            boolean bl = result = className != null && className.equalsIgnoreCase(configClassName);
            if (this.debug()) {
                this.plugin.getLogger().info("[TRANSMUTE] isAlquimista: jogador=" + player.getName() + " classeDetectada='" + className + "' configClasse='" + configClassName + "' resultado=" + result);
            }
            return result;
        }
        catch (Exception e) {
            this.plugin.getLogger().warning("[TRANSMUTE] Erro ao verificar classe: " + e.getMessage());
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
            for (String methodName : new String[]{"getProfess", "getPlayerClass", "getMMOClass"}) {
                try {
                    playerClass = pdClass.getMethod(methodName, new Class[0]).invoke(data, new Object[0]);
                    break;
                }
                catch (NoSuchMethodException noSuchMethodException) {
                }
            }
            if (playerClass == null) {
                return null;
            }
            for (String methodName : new String[]{"getName", "getId", "getKey"}) {
                try {
                    String s;
                    Object result = playerClass.getClass().getMethod(methodName, new Class[0]).invoke(playerClass, new Object[0]);
                    if (!(result instanceof String) || (s = (String)result).isEmpty()) continue;
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
            this.plugin.getLogger().warning("[TRANSMUTE] Erro ao obter classe: " + e.getMessage());
            return null;
        }
    }

    public int getAlquimistaLevel(Player player) {
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

    public void openGUI(Player player, Location tableLoc) {
        int minLevel = this.plugin.getConfig().getInt("transmutation.min-level", 5);
        int transmuteLevel = this.plugin.getConfig().getInt("transmutation.transmute-level", 35);
        int playerLevel = this.getAlquimistaLevel(player);
        if (playerLevel < minLevel) {
            player.sendMessage(ChatColor.translateAlternateColorCodes((char)'&', (String)this.plugin.getConfig().getString("transmutation.messages.level-too-low", "&c[NemonicOrb] Voce precisa de nivel %level% na classe Alquimista!").replace("%level%", String.valueOf(minLevel))));
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
            return;
        }
        boolean canTransmute = playerLevel >= transmuteLevel;
        EnvironmentBonus bonus = this.scanEnvironment(tableLoc, player);
        double efficiency = this.calculateEfficiency(player, bonus);
        Inventory gui = Bukkit.createInventory(null, (int)54, (Component)Component.text((String)GUI_TITLE_RAW, (TextColor)NamedTextColor.GOLD, (TextDecoration[])new TextDecoration[]{TextDecoration.BOLD}));
        ItemStack blackGlass = this.createGlassPane(Material.BLACK_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < 54; ++i) {
            gui.setItem(i, blackGlass);
        }
        gui.setItem(4, this.createEfficiencyItem(bonus, efficiency, playerLevel));
        gui.setItem(8, this.createInfoItem(canTransmute));
        ItemStack arrowPane = this.createArrowPane();
        gui.setItem(10, null);
        gui.setItem(11, arrowPane);
        gui.setItem(12, this.createCraftButton("PEDRA_DE_ENCANTAMENTO", RECIPES_ENCANTAMENTO));
        gui.setItem(13, arrowPane);
        gui.setItem(14, null);
        gui.setItem(15, null);
        gui.setItem(19, null);
        gui.setItem(20, arrowPane);
        gui.setItem(21, this.createCraftButton("PEDRA_DE_REFORCO", RECIPES_REFORCO));
        gui.setItem(22, arrowPane);
        gui.setItem(23, null);
        gui.setItem(24, null);
        ItemStack purpleGlass = this.createGlassPane(Material.PURPLE_STAINED_GLASS_PANE, " ");
        for (int i = 27; i <= 35; ++i) {
            gui.setItem(i, purpleGlass);
        }
        if (canTransmute) {
            gui.setItem(37, null);
            gui.setItem(38, arrowPane);
            gui.setItem(39, this.createTransmuteButton(true));
            gui.setItem(40, arrowPane);
            gui.setItem(41, null);
            gui.setItem(42, null);
        } else {
            ItemStack redGlass = this.createGlassPane(Material.RED_STAINED_GLASS_PANE, " ");
            for (int i = 36; i <= 44; ++i) {
                gui.setItem(i, redGlass);
            }
            gui.setItem(39, this.createLockedTransmuteButton(transmuteLevel));
        }
        TransmutationSession session = new TransmutationSession(tableLoc, bonus, efficiency, canTransmute);
        this.activeSessions.put(player.getUniqueId(), session);
        player.openInventory(gui);
        player.playSound(tableLoc, Sound.BLOCK_ENCHANTMENT_TABLE_USE, 0.8f, 0.5f);
        this.startAura(player, tableLoc, efficiency);
        if (this.debug()) {
            this.plugin.getLogger().info("[TRANSMUTE] GUI aberta para " + player.getName() + " nivel=" + playerLevel + " canTransmute=" + canTransmute + " eficiencia=" + String.format("%.0f%%", efficiency * 100.0));
        }
    }

    private EnvironmentBonus scanEnvironment(Location loc, Player player) {
        String cacheKey = loc.getWorld().getName() + "," + loc.getBlockX() + "," + loc.getBlockY() + "," + loc.getBlockZ();
        long cacheTTL = this.plugin.getConfig().getLong("transmutation.scan-cache-seconds", 60L) * 1000L;
        CachedScan cached = this.scanCache.get(cacheKey);
        if (cached != null && System.currentTimeMillis() - cached.timestamp() < cacheTTL) {
            int nearbyPlayers = this.countNearbyPlayers(loc, player);
            double maxPlayerBonus = this.plugin.getConfig().getDouble("transmutation.nearby-players.max-bonus", 0.25);
            double perPlayerBonus = this.plugin.getConfig().getDouble("transmutation.nearby-players.bonus-per-player", 0.05);
            double playerBonus = Math.min((double)nearbyPlayers * perPlayerBonus, maxPlayerBonus);
            EnvironmentBonus old = cached.bonus();
            return new EnvironmentBonus(old.bookshelves(), old.bookshelfBonus(), old.alchemyBlocks(), old.alchemyBonus(), nearbyPlayers, playerBonus);
        }
        int radius = this.plugin.getConfig().getInt("transmutation.scan-radius", 20);
        int bookshelves = 0;
        EnumMap<Material, Integer> alchemyBlockCounts = new EnumMap<Material, Integer>(Material.class);
        World world = loc.getWorld();
        int cx = loc.getBlockX();
        int cy = loc.getBlockY();
        int cz = loc.getBlockZ();
        for (int x = cx - radius; x <= cx + radius; ++x) {
            for (int y = Math.max(world.getMinHeight(), cy - radius); y <= Math.min(world.getMaxHeight() - 1, cy + radius); ++y) {
                for (int z = cz - radius; z <= cz + radius; ++z) {
                    double distSq = (x - cx) * (x - cx) + (y - cy) * (y - cy) + (z - cz) * (z - cz);
                    if (distSq > (double)(radius * radius)) continue;
                    Block block = world.getBlockAt(x, y, z);
                    Material mat = block.getType();
                    if (mat == Material.BOOKSHELF) {
                        ++bookshelves;
                        continue;
                    }
                    if (!ALCHEMY_BLOCKS.containsKey(mat)) continue;
                    alchemyBlockCounts.merge(mat, 1, Integer::sum);
                }
            }
        }
        double bookshelfBonusPerBlock = this.plugin.getConfig().getDouble("transmutation.bookshelf.bonus-per-block", 0.01);
        double maxBookshelfBonus = this.plugin.getConfig().getDouble("transmutation.bookshelf.max-bonus", 0.4);
        double bookshelfBonus = Math.min((double)bookshelves * bookshelfBonusPerBlock, maxBookshelfBonus);
        double alchemyBonus = 0.0;
        for (Map.Entry entry : alchemyBlockCounts.entrySet()) {
            double[] config = ALCHEMY_BLOCKS.get(entry.getKey());
            if (config == null) continue;
            int count = Math.min((Integer)entry.getValue(), (int)config[1]);
            alchemyBonus += (double)count * config[0];
        }
        int nearbyPlayers = this.countNearbyPlayers(loc, player);
        double maxPlayerBonus = this.plugin.getConfig().getDouble("transmutation.nearby-players.max-bonus", 0.25);
        double perPlayerBonus = this.plugin.getConfig().getDouble("transmutation.nearby-players.bonus-per-player", 0.05);
        double playerBonus = Math.min((double)nearbyPlayers * perPlayerBonus, maxPlayerBonus);
        EnvironmentBonus bonus = new EnvironmentBonus(bookshelves, bookshelfBonus, alchemyBlockCounts, alchemyBonus, nearbyPlayers, playerBonus);
        this.scanCache.put(cacheKey, new CachedScan(bonus, System.currentTimeMillis()));
        if (this.debug()) {
            this.plugin.getLogger().info("[TRANSMUTE-SCAN] estantes=" + bookshelves + " (+=" + String.format("%.0f%%", bookshelfBonus * 100.0) + ") alquimia=" + String.format("%.0f%%", alchemyBonus * 100.0) + " jogadores=" + nearbyPlayers + " (+=" + String.format("%.0f%%", playerBonus * 100.0) + ")");
        }
        return bonus;
    }

    private int countNearbyPlayers(Location loc, Player exclude) {
        int radius = this.plugin.getConfig().getInt("transmutation.scan-radius", 20);
        int count = 0;
        for (Player p : loc.getWorld().getPlayers()) {
            if (p.equals((Object)exclude) || !(p.getLocation().distanceSquared(loc) <= (double)(radius * radius))) continue;
            ++count;
        }
        return count;
    }

    private double calculateEfficiency(Player player, EnvironmentBonus bonus) {
        double base = 1.0 + bonus.bookshelfBonus() + bonus.alchemyBonus() + bonus.playerBonus();
        int level = this.getAlquimistaLevel(player);
        double levelMult = 1.0;
        if (level >= 100) {
            levelMult = 1.5;
        } else if (level >= 75) {
            levelMult = 1.3;
        } else if (level >= 50) {
            levelMult = 1.15;
        }
        return base * levelMult;
    }

    private boolean isValidTransmuteInput(ItemStack item) {
        if (item == null || item.getType().isAir()) {
            return false;
        }
        NBTItem nbt = NBTItem.get((ItemStack)item);
        if (nbt.hasTag("MMOITEMS_ITEM_ID")) {
            String itemId = nbt.getString("MMOITEMS_ITEM_ID");
            List orbIds = this.plugin.getConfig().getStringList("orb-ids");
            if (orbIds.contains(itemId)) {
                return false;
            }
        }
        if (nbt.hasTag("NEMONICORB_TIER")) {
            return true;
        }
        if (nbt.hasTag("MMOITEMS_ITEM_TYPE")) {
            String type = nbt.getString("MMOITEMS_ITEM_TYPE");
            return type != null && ACCEPTED_MMOITEM_TYPES.contains(type);
        }
        return false;
    }

    private int getBaseOrbs(ItemStack item) {
        NBTItem nbt = NBTItem.get((ItemStack)item);
        if (nbt.hasTag("NEMONICORB_TIER")) {
            int tier = nbt.getInteger("NEMONICORB_TIER");
            return tier == 3 ? 2 : 1;
        }
        return 1;
    }

    private List<ItemStack> rollOrbs(int count) {
        ArrayList<ItemStack> orbs = new ArrayList<ItemStack>();
        for (int i = 0; i < count; ++i) {
            String orbId = this.rollSingleOrb();
            ItemStack orbItem = this.createOrbItem(orbId);
            if (orbItem == null) continue;
            orbs.add(orbItem);
        }
        return orbs;
    }

    private String rollSingleOrb() {
        int roll = ThreadLocalRandom.current().nextInt(88);
        int cumulative = 0;
        for (int i = 0; i < ORB_IDS.length; ++i) {
            if (roll >= (cumulative += ORB_WEIGHTS[i])) continue;
            return ORB_IDS[i];
        }
        return ORB_IDS[0];
    }

    private ItemStack createOrbItem(String orbId) {
        try {
            Type consumableType = Type.get((String)"CONSUMABLE");
            if (consumableType == null) {
                return null;
            }
            return MMOItems.plugin.getItem(consumableType, orbId);
        }
        catch (Exception e) {
            this.plugin.getLogger().warning("[TRANSMUTE] Erro ao criar orb " + orbId + ": " + e.getMessage());
            return null;
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
        TransmutationSession session = this.activeSessions.get(player.getUniqueId());
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
        if (!(event.getHotbarButton() < 0 || slot >= 54 || INPUT_SLOTS.contains(slot) || slot == 37 && session.canTransmute())) {
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
                if (this.isSlotEmpty(topInv, 19)) {
                    topInv.setItem(19, clicked.clone());
                    event.setCurrentItem(null);
                    return;
                }
                if (session.canTransmute() && this.isSlotEmpty(topInv, 37)) {
                    topInv.setItem(37, clicked.clone());
                    event.setCurrentItem(null);
                    return;
                }
            }
            return;
        }
        if (INPUT_SLOTS.contains(slot)) {
            return;
        }
        if (slot == 37 && session.canTransmute()) {
            return;
        }
        if (OUTPUT_SLOTS.contains(slot)) {
            if (!session.canTransmute() && TRANSMUTE_SLOTS.contains(slot)) {
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
        if (slot == 12) {
            this.handleCraft(player, topInv, session, "PEDRA_DE_ENCANTAMENTO", RECIPES_ENCANTAMENTO, 10, new int[]{14, 15});
        } else if (slot == 21) {
            this.handleCraft(player, topInv, session, "PEDRA_DE_REFORCO", RECIPES_REFORCO, 19, new int[]{23, 24});
        } else if (slot == 39 && session.canTransmute()) {
            this.handleTransmute(player, topInv, session);
        } else if (slot == 39 && !session.canTransmute()) {
            int transmuteLevel = this.plugin.getConfig().getInt("transmutation.transmute-level", 35);
            player.sendMessage(ChatColor.translateAlternateColorCodes((char)'&', (String)("&c[NemonicOrb] Requer nivel " + transmuteLevel + " na classe Alquimista!")));
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
        }
    }

    private boolean isSlotEmpty(Inventory inv, int slot) {
        ItemStack item = inv.getItem(slot);
        return item == null || item.getType().isAir();
    }

    private void syncInventoryLater(Player player) {
        Bukkit.getScheduler().runTask((Plugin)this.plugin, () -> {
            if (player.isOnline()) {
                player.updateInventory();
            }
        });
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
        TransmutationSession session = this.activeSessions.get(player.getUniqueId());
        if (session == null) {
            event.setCancelled(true);
            return;
        }
        HashSet<Integer> allowed = new HashSet<Integer>(INPUT_SLOTS);
        if (session.canTransmute()) {
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
        TransmutationSession session = this.activeSessions.remove(player.getUniqueId());
        if (session == null) {
            return;
        }
        String title = this.getInventoryTitle(event.getView());
        if (!GUI_TITLE_RAW.equals(title)) {
            return;
        }
        this.stopAura(player);
        Inventory inv = event.getView().getTopInventory();
        for (int slot : returnSlots = new int[]{10, 14, 15, 19, 23, 24, 37, 41, 42}) {
            ItemStack item = inv.getItem(slot);
            inv.setItem(slot, null);
            if (item == null || item.getType().isAir() || this.isGUIDecoration(item)) continue;
            HashMap leftover = player.getInventory().addItem(new ItemStack[]{item});
            for (ItemStack drop : leftover.values()) {
                player.getWorld().dropItemNaturally(player.getLocation(), drop);
            }
        }
    }

    private void handleCraft(Player player, Inventory gui, TransmutationSession session, String orbId, List<CraftRecipe> recipes, int materialSlot, int[] outputSlots) {
        int i;
        long craftCooldownMs = this.plugin.getConfig().getLong("transmutation.craft-cooldown-seconds", 10L) * 1000L;
        Long lastCraft = this.craftCooldowns.get(player.getUniqueId());
        long now = System.currentTimeMillis();
        if (lastCraft != null && now - lastCraft < craftCooldownMs) {
            long remaining = (craftCooldownMs - (now - lastCraft)) / 1000L;
            player.sendMessage(ChatColor.translateAlternateColorCodes((char)'&', (String)("&c[NemonicOrb] Aguarde " + remaining + "s antes de fabricar novamente!")));
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
            return;
        }
        for (int slot : outputSlots) {
            ItemStack existing = gui.getItem(slot);
            if (existing == null || existing.getType().isAir()) continue;
            player.sendMessage(ChatColor.translateAlternateColorCodes((char)'&', (String)"&c[NemonicOrb] Retire as orbs dos slots de saida primeiro!"));
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
            return;
        }
        ItemStack material = gui.getItem(materialSlot);
        CraftRecipe matchedRecipe = null;
        for (CraftRecipe recipe : recipes) {
            if (!recipe.matches(material)) continue;
            matchedRecipe = recipe;
            break;
        }
        if (matchedRecipe == null) {
            player.sendMessage(ChatColor.translateAlternateColorCodes((char)'&', (String)"&c[NemonicOrb] Coloque o material no slot e clique no botao!"));
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
            return;
        }
        int amountToConsume = matchedRecipe.amount();
        int remaining = material.getAmount() - amountToConsume;
        if (remaining > 0) {
            material.setAmount(remaining);
        } else {
            gui.setItem(materialSlot, null);
        }
        double yieldMultiplier = this.plugin.getConfig().getDouble("transmutation.orb-yield-multiplier", 0.55);
        int totalOrbs = Math.max(1, (int)Math.floor(session.efficiency() * Math.max(0.1, yieldMultiplier)));
        ArrayList<ItemStack> orbs = new ArrayList<ItemStack>();
        for (i = 0; i < totalOrbs; ++i) {
            ItemStack orb = this.createOrbItem(orbId);
            if (orb == null) continue;
            orbs.add(orb);
        }
        if (orbs.isEmpty()) {
            player.sendMessage(ChatColor.translateAlternateColorCodes((char)'&', (String)"&c[NemonicOrb] Erro ao gerar orbs!"));
            return;
        }
        for (i = 0; i < orbs.size(); ++i) {
            if (i < outputSlots.length) {
                gui.setItem(outputSlots[i], (ItemStack)orbs.get(i));
                continue;
            }
            HashMap leftover = player.getInventory().addItem(new ItemStack[]{(ItemStack)orbs.get(i)});
            for (ItemStack drop : leftover.values()) {
                player.getWorld().dropItemNaturally(player.getLocation(), drop);
            }
        }
        this.craftCooldowns.put(player.getUniqueId(), now);
        Location tableLoc = session.tableLoc();
        player.playSound(tableLoc, Sound.BLOCK_ENCHANTMENT_TABLE_USE, 1.0f, 1.2f);
        tableLoc.getWorld().spawnParticle(Particle.ENCHANT, tableLoc.clone().add(0.5, 1.5, 0.5), 30, 0.3, 0.3, 0.3, 1.0);
        player.sendMessage(ChatColor.translateAlternateColorCodes((char)'&', (String)("&a[NemonicOrb] Fabricacao concluida! " + totalOrbs + " orb(s) gerada(s).")));
        if (this.debug()) {
            this.plugin.getLogger().info("[TRANSMUTE-CRAFT] " + player.getName() + " fabricou " + totalOrbs + "x " + orbId);
        }
    }

    private void handleTransmute(Player player, Inventory gui, TransmutationSession session) {
        long cooldownMs = this.plugin.getConfig().getLong("transmutation.cooldown-seconds", 120L) * 1000L;
        Long lastUse = this.cooldowns.get(player.getUniqueId());
        long now = System.currentTimeMillis();
        if (lastUse != null && now - lastUse < cooldownMs) {
            long remaining = (cooldownMs - (now - lastUse)) / 1000L;
            player.sendMessage(ChatColor.translateAlternateColorCodes((char)'&', (String)this.plugin.getConfig().getString("transmutation.messages.cooldown", "&c[NemonicOrb] Aguarde %seconds%s antes de transmutar novamente!").replace("%seconds%", String.valueOf(remaining))));
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
            return;
        }
        ItemStack input = gui.getItem(37);
        if (!this.isValidTransmuteInput(input)) {
            player.sendMessage(ChatColor.translateAlternateColorCodes((char)'&', (String)"&c[NemonicOrb] Coloque um equipamento no slot e clique no rebolo!"));
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
            return;
        }
        ItemStack out1 = gui.getItem(41);
        ItemStack out2 = gui.getItem(42);
        if (out1 != null && !out1.getType().isAir() || out2 != null && !out2.getType().isAir()) {
            player.sendMessage(ChatColor.translateAlternateColorCodes((char)'&', (String)"&c[NemonicOrb] Retire as orbs dos slots de saida primeiro!"));
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
            return;
        }
        int baseOrbs = this.getBaseOrbs(input);
        double yieldMultiplier = this.plugin.getConfig().getDouble("transmutation.orb-yield-multiplier", 0.55);
        int totalOrbs = Math.max(1, (int)Math.floor((double)baseOrbs * session.efficiency() * Math.max(0.1, yieldMultiplier)));
        List<ItemStack> orbs = this.rollOrbs(totalOrbs);
        if (orbs.isEmpty()) {
            player.sendMessage(ChatColor.translateAlternateColorCodes((char)'&', (String)"&c[NemonicOrb] Erro ao gerar orbs!"));
            return;
        }
        gui.setItem(37, null);
        int[] outSlots = new int[]{41, 42};
        for (int i = 0; i < orbs.size(); ++i) {
            if (i < outSlots.length) {
                gui.setItem(outSlots[i], orbs.get(i));
                continue;
            }
            HashMap leftover = player.getInventory().addItem(new ItemStack[]{orbs.get(i)});
            for (ItemStack drop : leftover.values()) {
                player.getWorld().dropItemNaturally(player.getLocation(), drop);
            }
        }
        this.cooldowns.put(player.getUniqueId(), now);
        Location tableLoc = session.tableLoc();
        player.playSound(tableLoc, Sound.BLOCK_ENCHANTMENT_TABLE_USE, 1.0f, 1.0f);
        player.playSound(tableLoc, Sound.BLOCK_BREWING_STAND_BREW, 0.8f, 1.2f);
        tableLoc.getWorld().spawnParticle(Particle.ENCHANT, tableLoc.clone().add(0.5, 1.5, 0.5), 50, 0.5, 0.5, 0.5, 1.0);
        tableLoc.getWorld().spawnParticle(Particle.WITCH, player.getLocation().add(0.0, 1.0, 0.0), 30, 0.3, 0.5, 0.3, 0.1);
        player.sendMessage(ChatColor.translateAlternateColorCodes((char)'&', (String)this.plugin.getConfig().getString("transmutation.messages.transmute-success", "&a[NemonicOrb] Transmutacao concluida! %count% orb(s) gerada(s).").replace("%count%", String.valueOf(orbs.size()))));
        if (this.debug()) {
            this.plugin.getLogger().info("[TRANSMUTE] " + player.getName() + " transmutou item, gerou " + orbs.size() + " orbs");
        }
    }

    private boolean isGUIDecoration(ItemStack item) {
        if (item == null) {
            return true;
        }
        Material mat = item.getType();
        return mat.name().endsWith("_STAINED_GLASS_PANE") || mat == Material.GLASS_PANE || mat == Material.BARRIER || mat == Material.LIME_CONCRETE || mat == Material.ARROW;
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
            meta.displayName((Component)Component.text((String)"\u27a1", (TextColor)NamedTextColor.GREEN, (TextDecoration[])new TextDecoration[]{TextDecoration.BOLD}));
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack createCraftButton(String orbId, List<CraftRecipe> recipes) {
        Material displayMat = orbId.equals("PEDRA_DE_ENCANTAMENTO") ? Material.ENCHANTING_TABLE : Material.ANVIL;
        ItemStack item = new ItemStack(displayMat);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            String displayName = orbId.equals("PEDRA_DE_ENCANTAMENTO") ? "Fabricar Pedra de Encantamento" : "Fabricar Pedra de Reforco";
            meta.displayName((Component)Component.text((String)displayName, (TextColor)NamedTextColor.GREEN, (TextDecoration[])new TextDecoration[]{TextDecoration.BOLD}));
            ArrayList<TextComponent> lore = new ArrayList<TextComponent>();
            lore.add(Component.text((String)""));
            lore.add(Component.text((String)"Clique para fabricar!", (TextColor)NamedTextColor.YELLOW));
            lore.add(Component.text((String)"Tenha os materiais no inventario.", (TextColor)NamedTextColor.GRAY));
            lore.add(Component.text((String)""));
            lore.add(Component.text((String)"Receitas aceitas:", (TextColor)NamedTextColor.WHITE, (TextDecoration[])new TextDecoration[]{TextDecoration.UNDERLINED}));
            for (CraftRecipe recipe : recipes) {
                String matName = recipe.anyWool() ? recipe.amount() + "x La (qualquer cor)" : recipe.amount() + "x " + this.formatMaterialName(recipe.material());
                lore.add(Component.text((String)("  - " + matName), (TextColor)NamedTextColor.AQUA));
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
            meta.displayName((Component)Component.text((String)"Transmutar Equipamento", (TextColor)NamedTextColor.LIGHT_PURPLE, (TextDecoration[])new TextDecoration[]{TextDecoration.BOLD}));
            ArrayList<TextComponent> lore = new ArrayList<TextComponent>();
            lore.add(Component.text((String)""));
            lore.add(Component.text((String)"Clique para transmutar!", (TextColor)NamedTextColor.YELLOW));
            lore.add(Component.text((String)"Destroi um equipamento do seu", (TextColor)NamedTextColor.GRAY));
            lore.add(Component.text((String)"inventario e gera orbs aleatorias.", (TextColor)NamedTextColor.GRAY));
            lore.add(Component.text((String)""));
            lore.add(Component.text((String)"Aceita: Equipamentos com orbs ou MMOItem", (TextColor)NamedTextColor.AQUA));
            lore.add(Component.text((String)"Cooldown: 2 minutos", (TextColor)NamedTextColor.RED));
            meta.lore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack createLockedTransmuteButton(int requiredLevel) {
        ItemStack item = new ItemStack(Material.RED_STAINED_GLASS_PANE);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName((Component)Component.text((String)"Transmutacao Bloqueada", (TextColor)NamedTextColor.RED, (TextDecoration[])new TextDecoration[]{TextDecoration.BOLD}));
            ArrayList<TextComponent> lore = new ArrayList<TextComponent>();
            lore.add(Component.text((String)""));
            lore.add(Component.text((String)("Requer Nivel " + requiredLevel + " Alquimista"), (TextColor)NamedTextColor.GRAY));
            meta.lore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack createInfoItem(boolean canTransmute) {
        ItemStack item = new ItemStack(Material.ENCHANTED_BOOK);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName((Component)Component.text((String)GUI_TITLE_RAW, (TextColor)NamedTextColor.GOLD, (TextDecoration[])new TextDecoration[]{TextDecoration.BOLD}));
            ArrayList<TextComponent> lore = new ArrayList<TextComponent>();
            lore.add(Component.text((String)""));
            lore.add(Component.text((String)"Fabricacao de Orbs:", (TextColor)NamedTextColor.GREEN, (TextDecoration[])new TextDecoration[]{TextDecoration.BOLD}));
            lore.add(Component.text((String)"Tenha materiais no inventario e", (TextColor)NamedTextColor.GRAY));
            lore.add(Component.text((String)"clique no botao da orb desejada.", (TextColor)NamedTextColor.GRAY));
            if (canTransmute) {
                lore.add(Component.text((String)""));
                lore.add(Component.text((String)"Transmutacao:", (TextColor)NamedTextColor.LIGHT_PURPLE, (TextDecoration[])new TextDecoration[]{TextDecoration.BOLD}));
                lore.add(Component.text((String)"Tenha um equipamento no inventario", (TextColor)NamedTextColor.GRAY));
                lore.add(Component.text((String)"e clique no rebolo para transmutar.", (TextColor)NamedTextColor.GRAY));
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
            int dragonEggs;
            int soulLanterns;
            int totalSoulSand;
            int brewingStands;
            meta.displayName((Component)Component.text((String)("Eficiencia: " + String.format("%.0f%%", efficiency * 100.0)), (TextColor)NamedTextColor.GREEN, (TextDecoration[])new TextDecoration[]{TextDecoration.BOLD}));
            ArrayList<TextComponent> lore = new ArrayList<TextComponent>();
            lore.add(Component.text((String)""));
            lore.add(Component.text((String)("\u2726 Alquimistas Proximos: " + bonus.nearbyPlayers() + " (+" + String.format("%.0f%%", bonus.playerBonus() * 100.0) + ")"), (TextColor)NamedTextColor.YELLOW));
            if (bonus.bookshelves() >= INDICATOR_THRESHOLDS.getOrDefault("BOOKSHELF", 30)) {
                lore.add(Component.text((String)("\u2726 Estantes: " + bonus.bookshelves() + " (+" + String.format("%.0f%%", bonus.bookshelfBonus() * 100.0) + ")"), (TextColor)NamedTextColor.AQUA));
            }
            int totalCauldrons = 0;
            for (Material m : new Material[]{Material.CAULDRON, Material.WATER_CAULDRON, Material.LAVA_CAULDRON}) {
                totalCauldrons += bonus.alchemyBlocks().getOrDefault(m, 0).intValue();
            }
            if (totalCauldrons >= INDICATOR_THRESHOLDS.getOrDefault("CAULDRON", 6)) {
                int effective = Math.min(totalCauldrons, 6);
                double cauldronBonus = (double)effective * 0.05;
                lore.add(Component.text((String)("\u2726 Caldeiroes: " + totalCauldrons + " (+" + String.format("%.0f%%", cauldronBonus * 100.0) + ")"), (TextColor)NamedTextColor.DARK_PURPLE));
            }
            if ((brewingStands = bonus.alchemyBlocks().getOrDefault(Material.BREWING_STAND, 0).intValue()) >= INDICATOR_THRESHOLDS.getOrDefault("BREWING_STAND", 6)) {
                int effective = Math.min(brewingStands, 6);
                double brewBonus = (double)effective * 0.08;
                lore.add(Component.text((String)("\u2726 Suportes de Pocao: " + brewingStands + " (+" + String.format("%.0f%%", brewBonus * 100.0) + ")"), (TextColor)NamedTextColor.DARK_PURPLE));
            }
            if ((totalSoulSand = bonus.alchemyBlocks().getOrDefault(Material.SOUL_SAND, 0) + bonus.alchemyBlocks().getOrDefault(Material.SOUL_SOIL, 0)) >= INDICATOR_THRESHOLDS.getOrDefault("SOUL_SAND", 6)) {
                int effective = Math.min(totalSoulSand, 10);
                double soulBonus = (double)effective * 0.01;
                lore.add(Component.text((String)("\u2726 Areia das Almas: " + totalSoulSand + " (+" + String.format("%.0f%%", soulBonus * 100.0) + ")"), (TextColor)NamedTextColor.DARK_PURPLE));
            }
            if ((soulLanterns = bonus.alchemyBlocks().getOrDefault(Material.SOUL_LANTERN, 0).intValue()) >= INDICATOR_THRESHOLDS.getOrDefault("SOUL_LANTERN", 5)) {
                int effective = Math.min(soulLanterns, 5);
                double lanternBonus = (double)effective * 0.02;
                lore.add(Component.text((String)("\u2726 Lanternas das Almas: " + soulLanterns + " (+" + String.format("%.0f%%", lanternBonus * 100.0) + ")"), (TextColor)NamedTextColor.DARK_PURPLE));
            }
            if ((dragonEggs = bonus.alchemyBlocks().getOrDefault(Material.DRAGON_EGG, 0).intValue()) >= INDICATOR_THRESHOLDS.getOrDefault("DRAGON_EGG", 1)) {
                lore.add(Component.text((String)("\u2726 Ovo de Dragao: " + dragonEggs + " (+50%)"), (TextColor)NamedTextColor.LIGHT_PURPLE));
            }
            double levelMult = 1.0;
            if (playerLevel >= 100) {
                levelMult = 1.5;
            } else if (playerLevel >= 75) {
                levelMult = 1.3;
            } else if (playerLevel >= 50) {
                levelMult = 1.15;
            }
            lore.add(Component.text((String)""));
            lore.add(Component.text((String)("\u2605 Nivel Alquimista: " + playerLevel + " (x" + String.format("%.2f", levelMult) + ")"), (TextColor)NamedTextColor.LIGHT_PURPLE));
            meta.lore(lore);
            item.setItemMeta(meta);
        }
        return item;
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
            if (!player.isOnline()) {
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
                case 2 -> Color.fromRGB((int)60, (int)120, (int)255);
                case 3 -> Color.fromRGB((int)160, (int)50, (int)220);
                case 4 -> Color.fromRGB((int)255, (int)200, (int)50);
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
            if (level == 4) {
                for (i = 0; i < 8; ++i) {
                    angle = 0.7853981633974483 * (double)i + (double)(System.currentTimeMillis() % 5000L) / 5000.0 * 2.0 * Math.PI;
                    px = cx + radius * Math.cos(angle);
                    pz = cz + radius * Math.sin(angle);
                    world.spawnParticle(Particle.END_ROD, px, cy + 1.0, pz, 2, 0.1, 0.3, 0.1, 0.02);
                }
                for (Player nearby : world.getPlayers()) {
                    if (!(nearby.getLocation().distanceSquared(tableLoc) <= 156.25) || nearby.hasPotionEffect(PotionEffectType.REGENERATION)) continue;
                    nearby.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 100, 0, true, true, true));
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

    private String getInventoryTitle(InventoryView view) {
        try {
            Component titleComp = view.title();
            return PlainTextComponentSerializer.plainText().serialize(titleComp);
        }
        catch (Exception e) {
            return "";
        }
    }

    private String formatMaterialName(Material mat) {
        if (mat == null) {
            return "?";
        }
        String name = mat.name().toLowerCase().replace("_", " ");
        StringBuilder sb = new StringBuilder();
        for (String word : name.split(" ")) {
            if (word.isEmpty()) continue;
            sb.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1)).append(" ");
        }
        return sb.toString().trim();
    }

    record EnvironmentBonus(int bookshelves, double bookshelfBonus, Map<Material, Integer> alchemyBlocks, double alchemyBonus, int nearbyPlayers, double playerBonus) {
    }

    record TransmutationSession(Location tableLoc, EnvironmentBonus bonus, double efficiency, boolean canTransmute) {
    }

    record CachedScan(EnvironmentBonus bonus, long timestamp) {
    }

    record CraftRecipe(Material material, int amount, boolean anyWool) {
        boolean matches(ItemStack item) {
            if (item == null || item.getType().isAir()) {
                return false;
            }
            if (this.anyWool) {
                return item.getType().name().endsWith("_WOOL") && item.getAmount() >= this.amount;
            }
            return item.getType() == this.material && item.getAmount() >= this.amount;
        }
    }
}

