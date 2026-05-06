package com.nemonicorp.orbs;

import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.scheduler.BukkitTask;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class MerchantRouteManager implements Listener {

    private final NemonicOrbPlugin plugin;

    // In-memory waypoint storage: UUID -> list of waypoints
    private final Map<UUID, List<Waypoint>> playerRoutes = new ConcurrentHashMap<>();

    // Active waypoint locations for fast block-break lookup: "world,x,y,z" -> owner UUID
    private final Map<String, UUID> waypointLocations = new ConcurrentHashMap<>();

    private File routeFile;
    private BukkitTask maintenanceTask;

    // Waypoint record
    public record Waypoint(
        String name,
        String worldName,
        int x, int y, int z,
        long registered,
        long lastActive,
        int price,
        String access,   // "public", "guild", "whitelist"
        List<UUID> whitelist,
        boolean active    // false = deactivated due to no XP maintenance
    ) {
        public Location toLocation() {
            World w = Bukkit.getWorld(worldName);
            if (w == null) return null;
            return new Location(w, x + 0.5, y + 1.0, z + 0.5);
        }

        public String locationKey() {
            return worldName + "," + x + "," + y + "," + z;
        }

        public Waypoint withActive(boolean active) {
            return new Waypoint(name, worldName, x, y, z, registered, System.currentTimeMillis(), price, access, whitelist, active);
        }

        public Waypoint withLastActive(long lastActive) {
            return new Waypoint(name, worldName, x, y, z, registered, lastActive, price, access, whitelist, active);
        }
    }

    public MerchantRouteManager(NemonicOrbPlugin plugin) {
        this.plugin = plugin;
        this.routeFile = new File(plugin.getDataFolder(), "merchant_routes.yml");
    }

    // ═══════ Lifecycle ═══════

    public void load() {
        playerRoutes.clear();
        waypointLocations.clear();

        if (!routeFile.exists()) return;

        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(routeFile);
        ConfigurationSection routes = yaml.getConfigurationSection("routes");
        if (routes == null) return;

        for (String uuidStr : routes.getKeys(false)) {
            UUID uuid;
            try { uuid = UUID.fromString(uuidStr); }
            catch (IllegalArgumentException e) { continue; }

            List<Waypoint> waypoints = new ArrayList<>();
            ConfigurationSection wpSection = routes.getConfigurationSection(uuidStr);
            if (wpSection == null) continue;

            List<Map<?, ?>> wpList = wpSection.getMapList("waypoints");
            for (Map<?, ?> wpMap : wpList) {
                try {
                    String name = wpMap.containsKey("name") ? String.valueOf(wpMap.get("name")) : "Unnamed";
                    String world = wpMap.containsKey("world") ? String.valueOf(wpMap.get("world")) : "world";
                    int x = wpMap.containsKey("x") ? ((Number) wpMap.get("x")).intValue() : 0;
                    int y = wpMap.containsKey("y") ? ((Number) wpMap.get("y")).intValue() : 64;
                    int z = wpMap.containsKey("z") ? ((Number) wpMap.get("z")).intValue() : 0;
                    long registered = wpMap.containsKey("registered") ? ((Number) wpMap.get("registered")).longValue() : System.currentTimeMillis();
                    long lastActive = wpMap.containsKey("last-active") ? ((Number) wpMap.get("last-active")).longValue() : System.currentTimeMillis();
                    int price = wpMap.containsKey("price") ? ((Number) wpMap.get("price")).intValue() : 0;
                    String access = wpMap.containsKey("access") ? String.valueOf(wpMap.get("access")) : "public";
                    boolean active = wpMap.containsKey("active") ? (boolean) wpMap.get("active") : true;

                    List<UUID> whitelist = new ArrayList<>();
                    Object wlObj = wpMap.get("whitelist");
                    if (wlObj instanceof List<?> wlList) {
                        for (Object o : wlList) {
                            try { whitelist.add(UUID.fromString(String.valueOf(o))); }
                            catch (IllegalArgumentException ignored) {}
                        }
                    }

                    Waypoint wp = new Waypoint(name, world, x, y, z, registered, lastActive, price, access, whitelist, active);
                    waypoints.add(wp);
                    waypointLocations.put(wp.locationKey(), uuid);
                } catch (Exception e) {
                    plugin.getLogger().warning("[ROUTES] Erro ao carregar waypoint: " + e.getMessage());
                }
            }

            if (!waypoints.isEmpty()) {
                playerRoutes.put(uuid, waypoints);
            }
        }

        plugin.getLogger().info("[ROUTES] Carregados " + playerRoutes.size() + " jogadores com rotas.");
    }

    public void save() {
        YamlConfiguration yaml = new YamlConfiguration();

        for (Map.Entry<UUID, List<Waypoint>> entry : playerRoutes.entrySet()) {
            String path = "routes." + entry.getKey().toString();
            List<Map<String, Object>> wpList = new ArrayList<>();

            for (Waypoint wp : entry.getValue()) {
                Map<String, Object> wpMap = new LinkedHashMap<>();
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

                List<String> wlStrings = new ArrayList<>();
                for (UUID u : wp.whitelist()) wlStrings.add(u.toString());
                wpMap.put("whitelist", wlStrings);

                wpList.add(wpMap);
            }

            yaml.set(path + ".waypoints", wpList);
        }

        try {
            yaml.save(routeFile);
        } catch (IOException e) {
            plugin.getLogger().warning("[ROUTES] Erro ao salvar rotas: " + e.getMessage());
        }
    }

    public void startMaintenance() {
        // Run every 5 minutes (6000 ticks)
        maintenanceTask = Bukkit.getScheduler().runTaskTimer(plugin, this::tickMaintenance, 6000L, 6000L);
    }

    public void shutdown() {
        if (maintenanceTask != null) maintenanceTask.cancel();
        save();
    }

    // ═══════ Maintenance ═══════

    private void tickMaintenance() {
        long now = System.currentTimeMillis();
        int xpPerHour = plugin.getConfig().getInt("merchant.teleport.maintenance-xp-per-hour", 1);
        int inactiveDays = plugin.getConfig().getInt("merchant.teleport.inactive-days-removal", 7);
        long removalThresholdMs = inactiveDays * 24L * 60L * 60L * 1000L;
        boolean changed = false;

        for (Map.Entry<UUID, List<Waypoint>> entry : playerRoutes.entrySet()) {
            UUID uuid = entry.getKey();
            Player player = Bukkit.getPlayer(uuid);
            List<Waypoint> waypoints = entry.getValue();
            Iterator<Waypoint> it = waypoints.iterator();

            while (it.hasNext()) {
                Waypoint wp = it.next();

                // Remove permanently inactive waypoints (7 days)
                if (!wp.active() && (now - wp.lastActive()) > removalThresholdMs) {
                    waypointLocations.remove(wp.locationKey());
                    it.remove();
                    changed = true;
                    if (player != null) {
                        player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                            "&c[Mercador] Rota '" + wp.name() + "' removida por inatividade prolongada!"));
                    }
                    continue;
                }

                // Maintenance cost: only if player is online and waypoint is active
                if (player != null && wp.active()) {
                    // Check if enough time has passed (maintenance every 5 min = 1/12 of hourly cost)
                    // We just deduct vanilla XP levels proportionally
                    // Since this runs every 5 min, cost = xpPerHour / 12 per tick
                    // Simplify: deduct 1 level every hour (12 ticks of 5 min)
                    // Track via lastActive: if more than 1 hour since last maintenance
                    long hoursSinceActive = (now - wp.lastActive()) / (60L * 60L * 1000L);
                    if (hoursSinceActive >= 1) {
                        int cost = (int) Math.max(1, hoursSinceActive * xpPerHour);
                        if (player.getLevel() >= cost) {
                            player.setLevel(player.getLevel() - cost);
                            int idx = waypoints.indexOf(wp);
                            waypoints.set(idx, wp.withLastActive(now));
                            changed = true;
                        } else {
                            // Deactivate
                            int idx = waypoints.indexOf(wp);
                            waypoints.set(idx, wp.withActive(false));
                            changed = true;
                            player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                                "&c[Mercador] Rota '" + wp.name() + "' desativada! XP insuficiente para manutencao."));
                        }
                    }
                }
            }

            // Clean up empty lists
            if (waypoints.isEmpty()) {
                playerRoutes.remove(uuid);
            }
        }

        if (changed) save();
    }

    // ═══════ Block Break Listener ═══════

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        Block block = event.getBlock();
        if (block.getType() != Material.CARTOGRAPHY_TABLE) return;

        String key = block.getWorld().getName() + "," + block.getX() + "," + block.getY() + "," + block.getZ();
        UUID ownerUUID = waypointLocations.remove(key);
        if (ownerUUID == null) return;

        List<Waypoint> waypoints = playerRoutes.get(ownerUUID);
        if (waypoints == null) return;

        waypoints.removeIf(wp -> wp.locationKey().equals(key));
        if (waypoints.isEmpty()) playerRoutes.remove(ownerUUID);
        save();

        // Notify owner if online
        Player owner = Bukkit.getPlayer(ownerUUID);
        if (owner != null) {
            owner.sendMessage(ChatColor.translateAlternateColorCodes('&',
                "&c[Mercador] Uma de suas mesas de rota comercial foi destruida! Rota removida."));
            owner.playSound(owner.getLocation(), Sound.ENTITY_ITEM_BREAK, 1.0f, 0.5f);
        }

        // Notify breaker
        Player breaker = event.getPlayer();
        if (!breaker.equals(owner)) {
            breaker.sendMessage(ChatColor.translateAlternateColorCodes('&',
                "&6[Mercador] &eVoce destruiu uma Mesa de Rota Comercial!"));
        }

        plugin.getLogger().info("[ROUTES] Waypoint removido em " + key + " (dono: " + ownerUUID + ", destruido por: " + breaker.getName() + ")");
    }

    // ═══════ Public API ═══════

    public List<Waypoint> getWaypoints(UUID player) {
        return playerRoutes.getOrDefault(player, Collections.emptyList());
    }

    public int getMaxSlots(int playerLevel) {
        // Read from config: merchant.teleport.max-waypoints-by-level
        ConfigurationSection cfg = plugin.getConfig().getConfigurationSection("merchant.teleport.max-waypoints-by-level");
        if (cfg == null) return playerLevel >= 35 ? 3 : (playerLevel >= 10 ? 2 : 0);

        int maxSlots = 0;
        for (String key : cfg.getKeys(false)) {
            try {
                int lvl = Integer.parseInt(key);
                if (playerLevel >= lvl) {
                    maxSlots = Math.max(maxSlots, cfg.getInt(key));
                }
            } catch (NumberFormatException ignored) {}
        }
        return maxSlots;
    }

    public boolean addWaypoint(UUID playerUUID, Waypoint wp) {
        List<Waypoint> waypoints = playerRoutes.computeIfAbsent(playerUUID, k -> new ArrayList<>());
        waypoints.add(wp);
        waypointLocations.put(wp.locationKey(), playerUUID);
        save();
        return true;
    }

    public boolean removeWaypoint(UUID playerUUID, int index) {
        List<Waypoint> waypoints = playerRoutes.get(playerUUID);
        if (waypoints == null || index < 0 || index >= waypoints.size()) return false;

        Waypoint removed = waypoints.remove(index);
        waypointLocations.remove(removed.locationKey());
        if (waypoints.isEmpty()) playerRoutes.remove(playerUUID);
        save();
        return true;
    }

    public boolean hasWaypointAt(Location loc) {
        String key = loc.getWorld().getName() + "," + loc.getBlockX() + "," + loc.getBlockY() + "," + loc.getBlockZ();
        return waypointLocations.containsKey(key);
    }

    public boolean isWaypointValid(Waypoint wp) {
        World world = Bukkit.getWorld(wp.worldName());
        if (world == null) return false;
        Block block = world.getBlockAt(wp.x(), wp.y(), wp.z());
        return block.getType() == Material.CARTOGRAPHY_TABLE;
    }

    /**
     * Reactivate a deactivated waypoint (player pays XP to reactivate)
     */
    public boolean reactivateWaypoint(UUID playerUUID, int index) {
        List<Waypoint> waypoints = playerRoutes.get(playerUUID);
        if (waypoints == null || index < 0 || index >= waypoints.size()) return false;

        Waypoint wp = waypoints.get(index);
        if (wp.active()) return true;

        waypoints.set(index, wp.withActive(true));
        save();
        return true;
    }

    /**
     * Get only the player's own waypoints (each player sees only their routes).
     */
    public List<WaypointInfo> getAccessibleWaypoints(UUID travellerUUID) {
        List<WaypointInfo> accessible = new ArrayList<>();
        List<Waypoint> own = playerRoutes.getOrDefault(travellerUUID, List.of());
        for (int i = 0; i < own.size(); i++) {
            Waypoint wp = own.get(i);
            if (!wp.active()) continue;
            accessible.add(new WaypointInfo(travellerUUID, i, wp));
        }
        return accessible;
    }

    public record WaypointInfo(UUID ownerUUID, int index, Waypoint waypoint) {}
}
