/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.Bukkit
 *  org.bukkit.ChatColor
 *  org.bukkit.Location
 *  org.bukkit.Material
 *  org.bukkit.Sound
 *  org.bukkit.World
 *  org.bukkit.block.Block
 *  org.bukkit.configuration.ConfigurationSection
 *  org.bukkit.configuration.file.YamlConfiguration
 *  org.bukkit.entity.Player
 *  org.bukkit.event.EventHandler
 *  org.bukkit.event.EventPriority
 *  org.bukkit.event.Listener
 *  org.bukkit.event.block.BlockBreakEvent
 *  org.bukkit.plugin.Plugin
 *  org.bukkit.scheduler.BukkitTask
 */
package com.nemonicorp.orbs;

import com.nemonicorp.orbs.NemonicOrbPlugin;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;

public class MerchantRouteManager
implements Listener {
    private final NemonicOrbPlugin plugin;
    private final Map<UUID, List<Waypoint>> playerRoutes = new ConcurrentHashMap<UUID, List<Waypoint>>();
    private final Map<String, UUID> waypointLocations = new ConcurrentHashMap<String, UUID>();
    private File routeFile;
    private BukkitTask maintenanceTask;

    public MerchantRouteManager(NemonicOrbPlugin plugin) {
        this.plugin = plugin;
        this.routeFile = new File(plugin.getDataFolder(), "merchant_routes.yml");
    }

    public void load() {
        this.playerRoutes.clear();
        this.waypointLocations.clear();
        if (!this.routeFile.exists()) {
            return;
        }
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration((File)this.routeFile);
        ConfigurationSection routes = yaml.getConfigurationSection("routes");
        if (routes == null) {
            return;
        }
        for (String uuidStr : routes.getKeys(false)) {
            UUID uuid;
            try {
                uuid = UUID.fromString(uuidStr);
            }
            catch (IllegalArgumentException e) {
                continue;
            }
            ArrayList<Waypoint> waypoints = new ArrayList<Waypoint>();
            ConfigurationSection wpSection = routes.getConfigurationSection(uuidStr);
            if (wpSection == null) continue;
            List wpList = wpSection.getMapList("waypoints");
            for (Map wpMap : wpList) {
                try {
                    String name = wpMap.containsKey("name") ? String.valueOf(wpMap.get("name")) : "Unnamed";
                    String world = wpMap.containsKey("world") ? String.valueOf(wpMap.get("world")) : "world";
                    int x = wpMap.containsKey("x") ? ((Number)wpMap.get("x")).intValue() : 0;
                    int y = wpMap.containsKey("y") ? ((Number)wpMap.get("y")).intValue() : 64;
                    int z = wpMap.containsKey("z") ? ((Number)wpMap.get("z")).intValue() : 0;
                    long registered = wpMap.containsKey("registered") ? ((Number)wpMap.get("registered")).longValue() : System.currentTimeMillis();
                    long lastActive = wpMap.containsKey("last-active") ? ((Number)wpMap.get("last-active")).longValue() : System.currentTimeMillis();
                    int price = wpMap.containsKey("price") ? ((Number)wpMap.get("price")).intValue() : 0;
                    String access = wpMap.containsKey("access") ? String.valueOf(wpMap.get("access")) : "public";
                    boolean active = wpMap.containsKey("active") ? (Boolean)wpMap.get("active") : true;
                    ArrayList<UUID> whitelist = new ArrayList<UUID>();
                    Object wlObj = wpMap.get("whitelist");
                    if (wlObj instanceof List) {
                        List wlList = (List)wlObj;
                        for (Object o : wlList) {
                            try {
                                whitelist.add(UUID.fromString(String.valueOf(o)));
                            }
                            catch (IllegalArgumentException illegalArgumentException) {}
                        }
                    }
                    Waypoint wp = new Waypoint(name, world, x, y, z, registered, lastActive, price, access, whitelist, active);
                    waypoints.add(wp);
                    this.waypointLocations.put(wp.locationKey(), uuid);
                }
                catch (Exception e) {
                    this.plugin.getLogger().warning("[ROUTES] Erro ao carregar waypoint: " + e.getMessage());
                }
            }
            if (waypoints.isEmpty()) continue;
            this.playerRoutes.put(uuid, waypoints);
        }
        this.plugin.getLogger().info("[ROUTES] Carregados " + this.playerRoutes.size() + " jogadores com rotas.");
    }

    public void save() {
        YamlConfiguration yaml = new YamlConfiguration();
        for (Map.Entry<UUID, List<Waypoint>> entry : this.playerRoutes.entrySet()) {
            String path = "routes." + entry.getKey().toString();
            ArrayList wpList = new ArrayList();
            for (Waypoint wp : entry.getValue()) {
                LinkedHashMap<String, Object> wpMap = new LinkedHashMap<String, Object>();
                wpMap.put("name", wp.name());
                wpMap.put("world", wp.worldName());
                wpMap.put("x", wp.x());
                wpMap.put("y", wp.y());
                wpMap.put("z", wp.z());
                wpMap.put("registered", wp.registered());
                wpMap.put("last-active", wp.lastActive());
                wpMap.put("price", wp.price());
                wpMap.put("access", wp.access());
                wpMap.put("active", wp.active());
                ArrayList<String> wlStrings = new ArrayList<String>();
                for (UUID u : wp.whitelist()) {
                    wlStrings.add(u.toString());
                }
                wpMap.put("whitelist", wlStrings);
                wpList.add(wpMap);
            }
            yaml.set(path + ".waypoints", wpList);
        }
        try {
            yaml.save(this.routeFile);
        }
        catch (IOException e) {
            this.plugin.getLogger().warning("[ROUTES] Erro ao salvar rotas: " + e.getMessage());
        }
    }

    public void startMaintenance() {
        this.maintenanceTask = Bukkit.getScheduler().runTaskTimer((Plugin)this.plugin, this::tickMaintenance, 6000L, 6000L);
    }

    public void shutdown() {
        if (this.maintenanceTask != null) {
            this.maintenanceTask.cancel();
        }
        this.save();
    }

    private void tickMaintenance() {
        long now = System.currentTimeMillis();
        int xpPerHour = this.plugin.getConfig().getInt("merchant.teleport.maintenance-xp-per-hour", 1);
        int inactiveDays = this.plugin.getConfig().getInt("merchant.teleport.inactive-days-removal", 7);
        long removalThresholdMs = (long)inactiveDays * 24L * 60L * 60L * 1000L;
        boolean changed = false;
        for (Map.Entry<UUID, List<Waypoint>> entry : this.playerRoutes.entrySet()) {
            UUID uuid = entry.getKey();
            Player player = Bukkit.getPlayer((UUID)uuid);
            List<Waypoint> waypoints = entry.getValue();
            Iterator<Waypoint> it = waypoints.iterator();
            while (it.hasNext()) {
                int idx;
                long hoursSinceActive;
                Waypoint wp = it.next();
                if (!wp.active() && now - wp.lastActive() > removalThresholdMs) {
                    this.waypointLocations.remove(wp.locationKey());
                    it.remove();
                    changed = true;
                    if (player == null) continue;
                    player.sendMessage(ChatColor.translateAlternateColorCodes((char)'&', (String)("&c[Mercador] Rota '" + wp.name() + "' removida por inatividade prolongada!")));
                    continue;
                }
                if (player == null || !wp.active() || (hoursSinceActive = (now - wp.lastActive()) / 3600000L) < 1L) continue;
                int cost = (int)Math.max(1L, hoursSinceActive * (long)xpPerHour);
                if (player.getLevel() >= cost) {
                    player.setLevel(player.getLevel() - cost);
                    idx = waypoints.indexOf(wp);
                    waypoints.set(idx, wp.withLastActive(now));
                    changed = true;
                    continue;
                }
                idx = waypoints.indexOf(wp);
                waypoints.set(idx, wp.withActive(false));
                changed = true;
                player.sendMessage(ChatColor.translateAlternateColorCodes((char)'&', (String)("&c[Mercador] Rota '" + wp.name() + "' desativada! XP insuficiente para manutencao.")));
            }
            if (!waypoints.isEmpty()) continue;
            this.playerRoutes.remove(uuid);
        }
        if (changed) {
            this.save();
        }
    }

    @EventHandler(priority=EventPriority.MONITOR, ignoreCancelled=true)
    public void onBlockBreak(BlockBreakEvent event) {
        Player breaker;
        Block block = event.getBlock();
        if (block.getType() != Material.CARTOGRAPHY_TABLE) {
            return;
        }
        String key = block.getWorld().getName() + "," + block.getX() + "," + block.getY() + "," + block.getZ();
        UUID ownerUUID = this.waypointLocations.remove(key);
        if (ownerUUID == null) {
            return;
        }
        List<Waypoint> waypoints = this.playerRoutes.get(ownerUUID);
        if (waypoints == null) {
            return;
        }
        waypoints.removeIf(wp -> wp.locationKey().equals(key));
        if (waypoints.isEmpty()) {
            this.playerRoutes.remove(ownerUUID);
        }
        this.save();
        Player owner = Bukkit.getPlayer((UUID)ownerUUID);
        if (owner != null) {
            owner.sendMessage(ChatColor.translateAlternateColorCodes((char)'&', (String)"&c[Mercador] Uma de suas mesas de rota comercial foi destruida! Rota removida."));
            owner.playSound(owner.getLocation(), Sound.ENTITY_ITEM_BREAK, 1.0f, 0.5f);
        }
        if (!(breaker = event.getPlayer()).equals((Object)owner)) {
            breaker.sendMessage(ChatColor.translateAlternateColorCodes((char)'&', (String)"&6[Mercador] &eVoce destruiu uma Mesa de Rota Comercial!"));
        }
        this.plugin.getLogger().info("[ROUTES] Waypoint removido em " + key + " (dono: " + String.valueOf(ownerUUID) + ", destruido por: " + breaker.getName() + ")");
    }

    public List<Waypoint> getWaypoints(UUID player) {
        return this.playerRoutes.getOrDefault(player, Collections.emptyList());
    }

    public int getMaxSlots(int playerLevel) {
        ConfigurationSection cfg = this.plugin.getConfig().getConfigurationSection("merchant.teleport.max-waypoints-by-level");
        if (cfg == null) {
            return playerLevel >= 35 ? 3 : (playerLevel >= 10 ? 2 : 0);
        }
        int maxSlots = 0;
        for (String key : cfg.getKeys(false)) {
            try {
                int lvl = Integer.parseInt(key);
                if (playerLevel < lvl) continue;
                maxSlots = Math.max(maxSlots, cfg.getInt(key));
            }
            catch (NumberFormatException numberFormatException) {}
        }
        return maxSlots;
    }

    public boolean addWaypoint(UUID playerUUID, Waypoint wp) {
        List waypoints = this.playerRoutes.computeIfAbsent(playerUUID, k -> new ArrayList());
        waypoints.add(wp);
        this.waypointLocations.put(wp.locationKey(), playerUUID);
        this.save();
        return true;
    }

    public boolean removeWaypoint(UUID playerUUID, int index) {
        List<Waypoint> waypoints = this.playerRoutes.get(playerUUID);
        if (waypoints == null || index < 0 || index >= waypoints.size()) {
            return false;
        }
        Waypoint removed = waypoints.remove(index);
        this.waypointLocations.remove(removed.locationKey());
        if (waypoints.isEmpty()) {
            this.playerRoutes.remove(playerUUID);
        }
        this.save();
        return true;
    }

    public boolean hasWaypointAt(Location loc) {
        String key = loc.getWorld().getName() + "," + loc.getBlockX() + "," + loc.getBlockY() + "," + loc.getBlockZ();
        return this.waypointLocations.containsKey(key);
    }

    public boolean isWaypointValid(Waypoint wp) {
        World world = Bukkit.getWorld((String)wp.worldName());
        if (world == null) {
            return false;
        }
        Block block = world.getBlockAt(wp.x(), wp.y(), wp.z());
        return block.getType() == Material.CARTOGRAPHY_TABLE;
    }

    public boolean reactivateWaypoint(UUID playerUUID, int index) {
        List<Waypoint> waypoints = this.playerRoutes.get(playerUUID);
        if (waypoints == null || index < 0 || index >= waypoints.size()) {
            return false;
        }
        Waypoint wp = waypoints.get(index);
        if (wp.active()) {
            return true;
        }
        waypoints.set(index, wp.withActive(true));
        this.save();
        return true;
    }

    public List<WaypointInfo> getAccessibleWaypoints(UUID travellerUUID) {
        ArrayList<WaypointInfo> accessible = new ArrayList<WaypointInfo>();
        List own = this.playerRoutes.getOrDefault(travellerUUID, List.of());
        for (int i = 0; i < own.size(); ++i) {
            Waypoint wp = (Waypoint)own.get(i);
            if (!wp.active()) continue;
            accessible.add(new WaypointInfo(travellerUUID, i, wp));
        }
        return accessible;
    }

    public record Waypoint(String name, String worldName, int x, int y, int z, long registered, long lastActive, int price, String access, List<UUID> whitelist, boolean active) {
        public Location toLocation() {
            World w = Bukkit.getWorld((String)this.worldName);
            if (w == null) {
                return null;
            }
            return new Location(w, (double)this.x + 0.5, (double)this.y + 1.0, (double)this.z + 0.5);
        }

        public String locationKey() {
            return this.worldName + "," + this.x + "," + this.y + "," + this.z;
        }

        public Waypoint withActive(boolean active) {
            return new Waypoint(this.name, this.worldName, this.x, this.y, this.z, this.registered, System.currentTimeMillis(), this.price, this.access, this.whitelist, active);
        }

        public Waypoint withLastActive(long lastActive) {
            return new Waypoint(this.name, this.worldName, this.x, this.y, this.z, this.registered, lastActive, this.price, this.access, this.whitelist, this.active);
        }
    }

    public record WaypointInfo(UUID ownerUUID, int index, Waypoint waypoint) {
    }
}

