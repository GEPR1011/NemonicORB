/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.Bukkit
 *  org.bukkit.ChatColor
 *  org.bukkit.Location
 *  org.bukkit.Material
 *  org.bukkit.OfflinePlayer
 *  org.bukkit.Particle
 *  org.bukkit.Sound
 *  org.bukkit.World
 *  org.bukkit.block.Block
 *  org.bukkit.configuration.file.YamlConfiguration
 *  org.bukkit.entity.Player
 *  org.bukkit.event.EventHandler
 *  org.bukkit.event.Listener
 *  org.bukkit.event.block.Action
 *  org.bukkit.event.player.PlayerInteractEvent
 *  org.bukkit.plugin.Plugin
 *  org.bukkit.scheduler.BukkitRunnable
 *  org.bukkit.scheduler.BukkitTask
 */
package com.nemonicorp.orbs;

import com.nemonicorp.orbs.NemonicOrbPlugin;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

public class PublicWarpManager
implements Listener {
    private final NemonicOrbPlugin plugin;
    private final File file;
    private final Map<String, PublicWarp> warps = new LinkedHashMap<String, PublicWarp>();
    private final Map<UUID, BukkitTask> activeCasts = new HashMap<UUID, BukkitTask>();
    public static final Material BEACON_BLOCK = Material.BEACON;
    public static final int CAST_TICKS = 60;
    public static final int MAX_PASSENGERS = 4;
    public static final int MANA_COST_BASE = 40;
    public static final int MANA_COST_PER_PASSENGER = 5;
    public static final double PASSENGER_RADIUS = 5.0;

    public PublicWarpManager(NemonicOrbPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "public_warps.yml");
    }

    public void load() {
        if (!this.file.exists()) {
            return;
        }
        YamlConfiguration cfg = YamlConfiguration.loadConfiguration((File)this.file);
        this.warps.clear();
        for (String key : cfg.getKeys(false)) {
            String world = cfg.getString(key + ".world");
            double x = cfg.getDouble(key + ".x");
            double y = cfg.getDouble(key + ".y");
            double z = cfg.getDouble(key + ".z");
            float yaw = (float)cfg.getDouble(key + ".yaw");
            float pitch = (float)cfg.getDouble(key + ".pitch");
            String creator = cfg.getString(key + ".creator", "console");
            World w = Bukkit.getWorld((String)world);
            if (w == null) {
                this.plugin.getLogger().warning("[PublicWarp] mundo '" + world + "' nao carregado para warp '" + key + "'.");
                continue;
            }
            this.warps.put(key.toLowerCase(Locale.ROOT), new PublicWarp(key, new Location(w, x, y, z, yaw, pitch), creator));
        }
        this.plugin.getLogger().info("[PublicWarp] " + this.warps.size() + " warp(s) publica(s) carregada(s).");
    }

    public void save() {
        YamlConfiguration cfg = new YamlConfiguration();
        for (Map.Entry<String, PublicWarp> entry : this.warps.entrySet()) {
            PublicWarp w = entry.getValue();
            String k = w.name();
            cfg.set(k + ".world", (Object)w.location().getWorld().getName());
            cfg.set(k + ".x", (Object)w.location().getX());
            cfg.set(k + ".y", (Object)w.location().getY());
            cfg.set(k + ".z", (Object)w.location().getZ());
            cfg.set(k + ".yaw", (Object)Float.valueOf(w.location().getYaw()));
            cfg.set(k + ".pitch", (Object)Float.valueOf(w.location().getPitch()));
            cfg.set(k + ".creator", (Object)w.creator());
        }
        try {
            cfg.save(this.file);
        }
        catch (IOException e) {
            this.plugin.getLogger().warning("[PublicWarp] Falha ao salvar: " + e.getMessage());
        }
    }

    public boolean exists(String name) {
        return this.warps.containsKey(name.toLowerCase(Locale.ROOT));
    }

    public PublicWarp get(String name) {
        return this.warps.get(name.toLowerCase(Locale.ROOT));
    }

    public List<PublicWarp> all() {
        return new ArrayList<PublicWarp>(this.warps.values());
    }

    public void setWarp(String name, Location loc, String creator) {
        this.warps.put(name.toLowerCase(Locale.ROOT), new PublicWarp(name, loc.clone(), creator));
        this.save();
    }

    public boolean removeWarp(String name) {
        boolean removed;
        boolean bl = removed = this.warps.remove(name.toLowerCase(Locale.ROOT)) != null;
        if (removed) {
            this.save();
        }
        return removed;
    }

    public boolean startTeleport(final Player player, String warpName) {
        int totalCost;
        final PublicWarp warp = this.get(warpName);
        if (warp == null) {
            player.sendMessage(PublicWarpManager.cc("&c[Warp] Warp publica '" + warpName + "' nao existe."));
            return false;
        }
        if (this.activeCasts.containsKey(player.getUniqueId())) {
            player.sendMessage(PublicWarpManager.cc("&c[Warp] Voce ja esta teleportando."));
            return false;
        }
        final ArrayList<Player> passengers = new ArrayList<Player>();
        for (Player nearby : player.getWorld().getPlayers()) {
            if (nearby.getUniqueId().equals(player.getUniqueId()) || !(nearby.getLocation().distance(player.getLocation()) <= 5.0)) continue;
            passengers.add(nearby);
            if (passengers.size() < 4) continue;
            break;
        }
        if (!this.hasEnoughMana(player, totalCost = 40 + passengers.size() * 5)) {
            player.sendMessage(PublicWarpManager.cc("&c[Warp] Mana insuficiente. Precisa de " + totalCost + " mana (40 base + " + passengers.size() + " passageiro(s) x 5)."));
            return false;
        }
        final Location origin = player.getLocation().clone();
        player.sendMessage(PublicWarpManager.cc("&b[Warp] Teleportando para &e" + warp.name() + "&b em 3s..."));
        if (!passengers.isEmpty()) {
            player.sendMessage(PublicWarpManager.cc("&b[Warp] Levando " + passengers.size() + " passageiro(s)."));
            for (Player p : passengers) {
                p.sendMessage(PublicWarpManager.cc("&b[Warp] Voce sera teleportado por " + player.getName() + " em 3s."));
            }
        }
        player.playSound(player.getLocation(), Sound.BLOCK_PORTAL_AMBIENT, 1.0f, 1.5f);
        BukkitTask task = new BukkitRunnable(){
            int ticks = 0;

            public void run() {
                if (!player.isOnline()) {
                    this.cancel();
                    PublicWarpManager.this.activeCasts.remove(player.getUniqueId());
                    return;
                }
                if (player.getLocation().distance(origin) > 1.5) {
                    player.sendMessage(PublicWarpManager.cc("&c[Warp] Cast cancelado (voce se moveu)."));
                    this.cancel();
                    PublicWarpManager.this.activeCasts.remove(player.getUniqueId());
                    return;
                }
                player.spawnParticle(Particle.PORTAL, player.getLocation().add(0.0, 1.0, 0.0), 20, 0.5, 1.0, 0.5, 0.05);
                this.ticks += 5;
                if (this.ticks >= 60) {
                    this.cancel();
                    PublicWarpManager.this.activeCasts.remove(player.getUniqueId());
                    PublicWarpManager.this.finishTeleport(player, warp, passengers, totalCost);
                }
            }
        }.runTaskTimer((Plugin)this.plugin, 0L, 5L);
        this.activeCasts.put(player.getUniqueId(), task);
        return true;
    }

    private void finishTeleport(Player player, PublicWarp warp, List<Player> passengers, int totalCost) {
        this.consumeMana(player, totalCost);
        player.teleport(warp.location());
        player.playSound(warp.location(), Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 1.0f);
        for (Player p : passengers) {
            if (!p.isOnline()) continue;
            p.teleport(warp.location());
            p.playSound(warp.location(), Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 1.0f);
        }
        player.sendMessage(PublicWarpManager.cc("&a[Warp] Chegou em &e" + warp.name() + "&a!"));
    }

    @EventHandler
    public void onInteractBeacon(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }
        Block b = event.getClickedBlock();
        if (b == null || b.getType() != BEACON_BLOCK) {
            return;
        }
        for (PublicWarp w : this.warps.values()) {
            Block wb = w.location().getBlock();
            if (!wb.getWorld().equals((Object)b.getWorld()) || wb.getX() != b.getX() || wb.getY() != b.getY() || wb.getZ() != b.getZ()) continue;
            event.setCancelled(true);
            Player p = event.getPlayer();
            p.sendMessage(PublicWarpManager.cc("&b[Warp Publica] &e" + w.name() + "&7 \u2014 registrada por &f" + w.creator()));
            p.sendMessage(PublicWarpManager.cc("&7Use &e/norb publicwarp tp " + w.name() + "&7 para teleportar."));
            return;
        }
    }

    private boolean hasEnoughMana(Player p, double amount) {
        try {
            Class<?> pdClass = Class.forName("net.Indyuce.mmocore.api.player.PlayerData");
            Object data = pdClass.getMethod("get", OfflinePlayer.class).invoke(null, p);
            double mana = ((Number)pdClass.getMethod("getMana", new Class[0]).invoke(data, new Object[0])).doubleValue();
            return mana >= amount;
        }
        catch (Exception e) {
            return true;
        }
    }

    private void consumeMana(Player p, double amount) {
        try {
            Class<?> pdClass = Class.forName("net.Indyuce.mmocore.api.player.PlayerData");
            Object data = pdClass.getMethod("get", OfflinePlayer.class).invoke(null, p);
            double mana = ((Number)pdClass.getMethod("getMana", new Class[0]).invoke(data, new Object[0])).doubleValue();
            pdClass.getMethod("setMana", Double.TYPE).invoke(data, Math.max(0.0, mana - amount));
        }
        catch (Exception exception) {
            // empty catch block
        }
    }

    private static String cc(String s) {
        return ChatColor.translateAlternateColorCodes((char)'&', (String)s);
    }

    public record PublicWarp(String name, Location location, String creator) {
    }
}

