package com.nemonicorp.orbs;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/**
 * Gerenciador de Warps Publicas Globais (#9):
 *  - Apenas admins podem registrar (/norb publicwarp set/del).
 *  - Bloco visual identificador: BEACON (luminoso, distintivo).
 *  - Custo de teleporte: 40 mana base + 5 por passageiro adicional (max 4 passageiros).
 *  - Aberto a todos os jogadores.
 *  - Cast com 3 segundos, cancelado ao tomar dano (re-uso do padrao de teleporte).
 */
public class PublicWarpManager implements Listener {

    private final NemonicOrbPlugin plugin;
    private final File file;
    private final Map<String, PublicWarp> warps = new LinkedHashMap<>();
    private final Map<UUID, BukkitTask> activeCasts = new HashMap<>();

    public static final Material BEACON_BLOCK = Material.BEACON;
    public static final int CAST_TICKS = 60; // 3 segundos
    public static final int MAX_PASSENGERS = 4;
    public static final int MANA_COST_BASE = 40;
    public static final int MANA_COST_PER_PASSENGER = 5;
    public static final double PASSENGER_RADIUS = 5.0;

    public PublicWarpManager(NemonicOrbPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "public_warps.yml");
    }

    public record PublicWarp(String name, Location location, String creator) {}

    public void load() {
        if (!file.exists()) return;
        YamlConfiguration cfg = YamlConfiguration.loadConfiguration(file);
        warps.clear();
        for (String key : cfg.getKeys(false)) {
            String world = cfg.getString(key + ".world");
            double x = cfg.getDouble(key + ".x");
            double y = cfg.getDouble(key + ".y");
            double z = cfg.getDouble(key + ".z");
            float yaw = (float) cfg.getDouble(key + ".yaw");
            float pitch = (float) cfg.getDouble(key + ".pitch");
            String creator = cfg.getString(key + ".creator", "console");
            World w = Bukkit.getWorld(world);
            if (w == null) {
                plugin.getLogger().warning("[PublicWarp] mundo '" + world + "' nao carregado para warp '" + key + "'.");
                continue;
            }
            warps.put(key.toLowerCase(Locale.ROOT),
                    new PublicWarp(key, new Location(w, x, y, z, yaw, pitch), creator));
        }
        plugin.getLogger().info("[PublicWarp] " + warps.size() + " warp(s) publica(s) carregada(s).");
    }

    public void save() {
        YamlConfiguration cfg = new YamlConfiguration();
        for (var entry : warps.entrySet()) {
            PublicWarp w = entry.getValue();
            String k = w.name();
            cfg.set(k + ".world", w.location().getWorld().getName());
            cfg.set(k + ".x", w.location().getX());
            cfg.set(k + ".y", w.location().getY());
            cfg.set(k + ".z", w.location().getZ());
            cfg.set(k + ".yaw", w.location().getYaw());
            cfg.set(k + ".pitch", w.location().getPitch());
            cfg.set(k + ".creator", w.creator());
        }
        try {
            cfg.save(file);
        } catch (IOException e) {
            plugin.getLogger().warning("[PublicWarp] Falha ao salvar: " + e.getMessage());
        }
    }

    public boolean exists(String name) {
        return warps.containsKey(name.toLowerCase(Locale.ROOT));
    }

    public PublicWarp get(String name) {
        return warps.get(name.toLowerCase(Locale.ROOT));
    }

    public List<PublicWarp> all() {
        return new ArrayList<>(warps.values());
    }

    public void setWarp(String name, Location loc, String creator) {
        warps.put(name.toLowerCase(Locale.ROOT),
                new PublicWarp(name, loc.clone(), creator));
        save();
    }

    public boolean removeWarp(String name) {
        boolean removed = warps.remove(name.toLowerCase(Locale.ROOT)) != null;
        if (removed) save();
        return removed;
    }

    /** Inicia teleporte para warp publica. Retorna true se cast comecou. */
    public boolean startTeleport(Player player, String warpName) {
        PublicWarp warp = get(warpName);
        if (warp == null) {
            player.sendMessage(cc("&c[Warp] Warp publica '" + warpName + "' nao existe."));
            return false;
        }
        if (activeCasts.containsKey(player.getUniqueId())) {
            player.sendMessage(cc("&c[Warp] Voce ja esta teleportando."));
            return false;
        }

        // Coletar passageiros (jogadores no raio 5 do caster, max 4)
        List<Player> passengers = new ArrayList<>();
        for (Player nearby : player.getWorld().getPlayers()) {
            if (nearby.getUniqueId().equals(player.getUniqueId())) continue;
            if (nearby.getLocation().distance(player.getLocation()) <= PASSENGER_RADIUS) {
                passengers.add(nearby);
                if (passengers.size() >= MAX_PASSENGERS) break;
            }
        }

        int totalCost = MANA_COST_BASE + (passengers.size() * MANA_COST_PER_PASSENGER);
        if (!hasEnoughMana(player, totalCost)) {
            player.sendMessage(cc("&c[Warp] Mana insuficiente. Precisa de " + totalCost
                    + " mana (" + MANA_COST_BASE + " base + " + passengers.size()
                    + " passageiro(s) x " + MANA_COST_PER_PASSENGER + ")."));
            return false;
        }

        Location origin = player.getLocation().clone();

        // Anuncio
        player.sendMessage(cc("&b[Warp] Teleportando para &e" + warp.name() + "&b em 3s..."));
        if (!passengers.isEmpty()) {
            player.sendMessage(cc("&b[Warp] Levando " + passengers.size() + " passageiro(s)."));
            for (Player p : passengers) {
                p.sendMessage(cc("&b[Warp] Voce sera teleportado por " + player.getName() + " em 3s."));
            }
        }
        player.playSound(player.getLocation(), Sound.BLOCK_PORTAL_AMBIENT, 1.0f, 1.5f);

        BukkitTask task = new BukkitRunnable() {
            int ticks = 0;
            @Override public void run() {
                if (!player.isOnline()) { cancel(); activeCasts.remove(player.getUniqueId()); return; }
                if (player.getLocation().distance(origin) > 1.5) {
                    player.sendMessage(cc("&c[Warp] Cast cancelado (voce se moveu)."));
                    cancel();
                    activeCasts.remove(player.getUniqueId());
                    return;
                }
                player.spawnParticle(Particle.PORTAL, player.getLocation().add(0, 1, 0),
                        20, 0.5, 1.0, 0.5, 0.05);
                ticks += 5;
                if (ticks >= CAST_TICKS) {
                    cancel();
                    activeCasts.remove(player.getUniqueId());
                    finishTeleport(player, warp, passengers, totalCost);
                }
            }
        }.runTaskTimer(plugin, 0L, 5L);
        activeCasts.put(player.getUniqueId(), task);
        return true;
    }

    private void finishTeleport(Player player, PublicWarp warp, List<Player> passengers, int totalCost) {
        consumeMana(player, totalCost);
        player.teleport(warp.location());
        player.playSound(warp.location(), Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 1.0f);
        for (Player p : passengers) {
            if (!p.isOnline()) continue;
            p.teleport(warp.location());
            p.playSound(warp.location(), Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 1.0f);
        }
        player.sendMessage(cc("&a[Warp] Chegou em &e" + warp.name() + "&a!"));
    }

    /** Click direito no bloco BEACON registrado abre menu de tp para a warp. */
    @EventHandler
    public void onInteractBeacon(PlayerInteractEvent event) {
        if (event.getAction() != org.bukkit.event.block.Action.RIGHT_CLICK_BLOCK) return;
        Block b = event.getClickedBlock();
        if (b == null || b.getType() != BEACON_BLOCK) return;

        // Procurar warp na mesma localizacao do bloco
        for (PublicWarp w : warps.values()) {
            Block wb = w.location().getBlock();
            if (wb.getWorld().equals(b.getWorld())
                    && wb.getX() == b.getX() && wb.getY() == b.getY() && wb.getZ() == b.getZ()) {
                event.setCancelled(true);
                Player p = event.getPlayer();
                p.sendMessage(cc("&b[Warp Publica] &e" + w.name() + "&7 — registrada por &f" + w.creator()));
                p.sendMessage(cc("&7Use &e/norb publicwarp tp " + w.name() + "&7 para teleportar."));
                return;
            }
        }
    }

    // ─── Helpers MMOCore (reflexao para nao travar build sem MMOCore) ───
    private boolean hasEnoughMana(Player p, double amount) {
        try {
            Class<?> pdClass = Class.forName("net.Indyuce.mmocore.api.player.PlayerData");
            Object data = pdClass.getMethod("get", org.bukkit.OfflinePlayer.class).invoke(null, p);
            double mana = ((Number) pdClass.getMethod("getMana").invoke(data)).doubleValue();
            return mana >= amount;
        } catch (Exception e) {
            return true; // Se MMOCore ausente, libera teleporte sem custo
        }
    }

    private void consumeMana(Player p, double amount) {
        try {
            Class<?> pdClass = Class.forName("net.Indyuce.mmocore.api.player.PlayerData");
            Object data = pdClass.getMethod("get", org.bukkit.OfflinePlayer.class).invoke(null, p);
            double mana = ((Number) pdClass.getMethod("getMana").invoke(data)).doubleValue();
            pdClass.getMethod("setMana", double.class).invoke(data, Math.max(0, mana - amount));
        } catch (Exception ignored) {}
    }

    private static String cc(String s) {
        return org.bukkit.ChatColor.translateAlternateColorCodes('&', s);
    }
}
