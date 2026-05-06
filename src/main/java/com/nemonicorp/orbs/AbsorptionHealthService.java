package com.nemonicorp.orbs;

import org.bukkit.Bukkit;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitTask;

import com.google.gson.Gson;
import io.lumine.mythic.lib.api.item.NBTItem;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Mantem vida vermelha base em 20/20 e converte excesso em absorcao (coracao dourado).
 * Nao regenera continuamente: recarrega apenas apos delay sem dano.
 */
public class AbsorptionHealthService implements Listener {
    private final NemonicOrbPlugin plugin;
    private final Gson gson;
    private BukkitTask task;
    private final Map<UUID, Long> lastDamageMs = new HashMap<>();
    private final Map<UUID, Long> lastConsumeMs = new HashMap<>();
    private final Map<UUID, Double> lastKnownMaxHealth = new HashMap<>();
    private final Map<UUID, Double> pendingHealFromMaxIncrease = new HashMap<>();

    public AbsorptionHealthService(NemonicOrbPlugin plugin) {
        this.plugin = plugin;
        this.gson = plugin.getModifierEngine().getGson();
    }

    public boolean isEnabled() {
        return plugin.getConfig().getBoolean("absorption-health.enabled", true);
    }

    public void start() {
        if (!isEnabled()) return;
        int period = Math.max(10, plugin.getConfig().getInt("absorption-health.recalc-interval-ticks", 20));
        task = Bukkit.getScheduler().runTaskTimer(plugin, this::tickAll, 20L, period);
    }

    public void stop() {
        if (task != null) task.cancel();
        task = null;
        for (Player p : Bukkit.getOnlinePlayers()) {
            try {
                p.setHealthScaled(false);
            } catch (Throwable ignored) {}
        }
        lastDamageMs.clear();
        lastConsumeMs.clear();
        lastKnownMaxHealth.clear();
        pendingHealFromMaxIncrease.clear();
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Bukkit.getScheduler().runTaskLater(plugin, () -> recalc(event.getPlayer(), true), 20L);
    }

    @EventHandler
    public void onRespawn(PlayerRespawnEvent event) {
        Bukkit.getScheduler().runTaskLater(plugin, () -> recalc(event.getPlayer(), true), 5L);
    }

    @EventHandler
    public void onWorld(PlayerChangedWorldEvent event) {
        Bukkit.getScheduler().runTaskLater(plugin, () -> recalc(event.getPlayer(), true), 5L);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        UUID id = event.getPlayer().getUniqueId();
        lastDamageMs.remove(id);
        lastConsumeMs.remove(id);
        lastKnownMaxHealth.remove(id);
        pendingHealFromMaxIncrease.remove(id);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onDamage(EntityDamageEvent event) {
        if (!isEnabled()) return;
        if (!(event.getEntity() instanceof Player p)) return;
        if (event.getFinalDamage() <= 0) return;
        lastDamageMs.put(p.getUniqueId(), System.currentTimeMillis());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onConsume(PlayerItemConsumeEvent event) {
        if (!isEnabled()) return;
        Player p = event.getPlayer();
        lastConsumeMs.put(p.getUniqueId(), System.currentTimeMillis());
        Bukkit.getScheduler().runTaskLater(plugin, () -> recalc(p, true), 1L);
    }

    private void tickAll() {
        for (Player p : Bukkit.getOnlinePlayers()) {
            recalc(p, false);
        }
    }

    public void recalc(Player p, boolean immediate) {
        if (!isEnabled() || !p.isOnline()) return;
        AttributeInstance attr = p.getAttribute(Attribute.MAX_HEALTH);
        if (attr == null) return;

        // Remover qualquer scale de HUD legado
        try {
            p.setHealthScaled(false);
        } catch (Throwable ignored) {}

        UUID id = p.getUniqueId();
        double currentMaxHealth = attr.getValue();
        double previousMaxHealth = lastKnownMaxHealth.getOrDefault(id, currentMaxHealth);
        if (currentMaxHealth > previousMaxHealth + 0.001) {
            double inc = currentMaxHealth - previousMaxHealth;
            pendingHealFromMaxIncrease.merge(id, inc, Double::sum);
        }
        lastKnownMaxHealth.put(id, currentMaxHealth);

        double orbExtraHealth = computeOrbExtraHealth(p);
        double maxAbs = Math.max(0.0, plugin.getConfig().getDouble("absorption-health.max-absorption", 200.0));
        double targetAbsorption = Math.min(orbExtraHealth, maxAbs);

        // Garantir clamp de vida para prevenir bug visual
        if (p.getHealth() > currentMaxHealth) {
            p.setHealth(currentMaxHealth);
        }

        long delayMs = Math.max(0L, plugin.getConfig().getLong("absorption-health.recharge-delay-seconds", 10) * 1000L);
        long lastHit = lastDamageMs.getOrDefault(id, 0L);
        long now = System.currentTimeMillis();
        boolean outOfCombat = (now - lastHit) >= delayMs;
        long consumeWindowMs = Math.max(1000L, plugin.getConfig().getLong("absorption-health.consume-heal-window-seconds", 5) * 1000L);
        long lastConsume = lastConsumeMs.getOrDefault(id, 0L);
        boolean consumedRecently = (now - lastConsume) <= consumeWindowMs;
        boolean canRecoverNow = immediate || consumedRecently || outOfCombat;

        double currentAbs = p.getAbsorptionAmount();
        if (canRecoverNow) {
            if (Math.abs(currentAbs - targetAbsorption) > 0.25f) {
                p.setAbsorptionAmount(targetAbsorption);
            }
        } else {
            // Em combate, nunca aumentar absorcao.
            if (currentAbs > targetAbsorption) {
                p.setAbsorptionAmount(targetAbsorption);
            }
        }

        // Cura por aumento de vida maxima: somente em consumo ou fora de combate.
        double pendingHeal = pendingHealFromMaxIncrease.getOrDefault(id, 0.0);
        if (pendingHeal > 0.001 && canRecoverNow) {
            double missing = currentMaxHealth - p.getHealth();
            if (missing > 0.0) {
                double heal = Math.min(missing, pendingHeal);
                if (heal > 0.0) {
                    p.setHealth(Math.min(currentMaxHealth, p.getHealth() + heal));
                    pendingHeal -= heal;
                }
            }
            if (pendingHeal <= 0.001) pendingHealFromMaxIncrease.remove(id);
            else pendingHealFromMaxIncrease.put(id, pendingHeal);
        }

        if (plugin.getConfig().getBoolean("absorption-health.debug", false)) {
            plugin.getLogger().info("[ABS] " + p.getName()
                    + " max=" + String.format("%.2f", currentMaxHealth)
                    + " orbExtra=" + String.format("%.2f", orbExtraHealth)
                    + " targetAbs=" + String.format("%.2f", targetAbsorption)
                    + " canRecover=" + canRecoverNow
                    + " pendingHeal=" + String.format("%.2f", pendingHealFromMaxIncrease.getOrDefault(id, 0.0)));
        }
    }

    private double computeOrbExtraHealth(Player p) {
        double total = 0.0;
        total += readMaxHealthFromMods(p.getInventory().getItemInMainHand());
        total += readMaxHealthFromMods(p.getInventory().getItemInOffHand());
        for (ItemStack armor : p.getInventory().getArmorContents()) {
            total += readMaxHealthFromMods(armor);
        }
        return Math.max(0.0, total);
    }

    private double readMaxHealthFromMods(ItemStack item) {
        if (item == null || item.getType().isAir()) return 0.0;
        NBTItem nbt = NBTItem.get(item);
        if (!nbt.hasTag(OrbListener.NBT_MODS)) return 0.0;
        try {
            String json = nbt.getString(OrbListener.NBT_MODS);
            if (json == null || json.isEmpty()) return 0.0;
            List<ModifierEngine.RolledModifier> mods = gson.fromJson(json, ModifierEngine.ROLLED_LIST_TYPE);
            if (mods == null) return 0.0;
            double sum = 0.0;
            for (var mod : mods) {
                Double hp = mod.stats().get("max-health");
                if (hp != null) sum += hp;
            }
            return sum;
        } catch (Exception ignored) {
            return 0.0;
        }
    }
}
