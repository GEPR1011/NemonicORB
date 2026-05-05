/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.gson.Gson
 *  io.lumine.mythic.lib.api.item.NBTItem
 *  org.bukkit.Bukkit
 *  org.bukkit.attribute.Attribute
 *  org.bukkit.attribute.AttributeInstance
 *  org.bukkit.entity.Entity
 *  org.bukkit.entity.Player
 *  org.bukkit.event.EventHandler
 *  org.bukkit.event.EventPriority
 *  org.bukkit.event.Listener
 *  org.bukkit.event.entity.EntityDamageEvent
 *  org.bukkit.event.player.PlayerChangedWorldEvent
 *  org.bukkit.event.player.PlayerItemConsumeEvent
 *  org.bukkit.event.player.PlayerJoinEvent
 *  org.bukkit.event.player.PlayerQuitEvent
 *  org.bukkit.event.player.PlayerRespawnEvent
 *  org.bukkit.inventory.ItemStack
 *  org.bukkit.plugin.Plugin
 *  org.bukkit.scheduler.BukkitTask
 */
package com.nemonicorp.orbs;

import com.google.gson.Gson;
import com.nemonicorp.orbs.ModifierEngine;
import com.nemonicorp.orbs.NemonicOrbPlugin;
import io.lumine.mythic.lib.api.item.NBTItem;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;

public class AbsorptionHealthService
implements Listener {
    private final NemonicOrbPlugin plugin;
    private final Gson gson;
    private BukkitTask task;
    private final Map<UUID, Long> lastDamageMs = new HashMap<UUID, Long>();
    private final Map<UUID, Long> lastConsumeMs = new HashMap<UUID, Long>();
    private final Map<UUID, Double> lastKnownMaxHealth = new HashMap<UUID, Double>();
    private final Map<UUID, Double> pendingHealFromMaxIncrease = new HashMap<UUID, Double>();

    public AbsorptionHealthService(NemonicOrbPlugin plugin) {
        this.plugin = plugin;
        this.gson = plugin.getModifierEngine().getGson();
    }

    public boolean isEnabled() {
        return this.plugin.getConfig().getBoolean("absorption-health.enabled", true);
    }

    public void start() {
        if (!this.isEnabled()) {
            return;
        }
        int period = Math.max(10, this.plugin.getConfig().getInt("absorption-health.recalc-interval-ticks", 20));
        this.task = Bukkit.getScheduler().runTaskTimer((Plugin)this.plugin, this::tickAll, 20L, (long)period);
    }

    public void stop() {
        if (this.task != null) {
            this.task.cancel();
        }
        this.task = null;
        for (Player p : Bukkit.getOnlinePlayers()) {
            try {
                p.setHealthScaled(false);
            }
            catch (Throwable throwable) {}
        }
        this.lastDamageMs.clear();
        this.lastConsumeMs.clear();
        this.lastKnownMaxHealth.clear();
        this.pendingHealFromMaxIncrease.clear();
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Bukkit.getScheduler().runTaskLater((Plugin)this.plugin, () -> this.recalc(event.getPlayer(), true), 20L);
    }

    @EventHandler
    public void onRespawn(PlayerRespawnEvent event) {
        Bukkit.getScheduler().runTaskLater((Plugin)this.plugin, () -> this.recalc(event.getPlayer(), true), 5L);
    }

    @EventHandler
    public void onWorld(PlayerChangedWorldEvent event) {
        Bukkit.getScheduler().runTaskLater((Plugin)this.plugin, () -> this.recalc(event.getPlayer(), true), 5L);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        UUID id = event.getPlayer().getUniqueId();
        this.lastDamageMs.remove(id);
        this.lastConsumeMs.remove(id);
        this.lastKnownMaxHealth.remove(id);
        this.pendingHealFromMaxIncrease.remove(id);
    }

    @EventHandler(priority=EventPriority.MONITOR, ignoreCancelled=true)
    public void onDamage(EntityDamageEvent event) {
        if (!this.isEnabled()) {
            return;
        }
        Entity entity = event.getEntity();
        if (!(entity instanceof Player)) {
            return;
        }
        Player p = (Player)entity;
        if (event.getFinalDamage() <= 0.0) {
            return;
        }
        this.lastDamageMs.put(p.getUniqueId(), System.currentTimeMillis());
    }

    @EventHandler(priority=EventPriority.MONITOR, ignoreCancelled=true)
    public void onConsume(PlayerItemConsumeEvent event) {
        if (!this.isEnabled()) {
            return;
        }
        Player p = event.getPlayer();
        this.lastConsumeMs.put(p.getUniqueId(), System.currentTimeMillis());
        Bukkit.getScheduler().runTaskLater((Plugin)this.plugin, () -> this.recalc(p, true), 1L);
    }

    private void tickAll() {
        for (Player p : Bukkit.getOnlinePlayers()) {
            this.recalc(p, false);
        }
    }

    public void recalc(Player p, boolean immediate) {
        double pendingHeal;
        if (!this.isEnabled() || !p.isOnline()) {
            return;
        }
        AttributeInstance attr = p.getAttribute(Attribute.MAX_HEALTH);
        if (attr == null) {
            return;
        }
        try {
            p.setHealthScaled(false);
        }
        catch (Throwable throwable) {
            // empty catch block
        }
        UUID id = p.getUniqueId();
        double currentMaxHealth = attr.getValue();
        double previousMaxHealth = this.lastKnownMaxHealth.getOrDefault(id, currentMaxHealth);
        if (currentMaxHealth > previousMaxHealth + 0.001) {
            double inc = currentMaxHealth - previousMaxHealth;
            this.pendingHealFromMaxIncrease.merge(id, inc, Double::sum);
        }
        this.lastKnownMaxHealth.put(id, currentMaxHealth);
        double orbExtraHealth = this.computeOrbExtraHealth(p);
        double maxAbs = Math.max(0.0, this.plugin.getConfig().getDouble("absorption-health.max-absorption", 200.0));
        double targetAbsorption = Math.min(orbExtraHealth, maxAbs);
        if (p.getHealth() > currentMaxHealth) {
            p.setHealth(currentMaxHealth);
        }
        long delayMs = Math.max(0L, this.plugin.getConfig().getLong("absorption-health.recharge-delay-seconds", 10L) * 1000L);
        long lastHit = this.lastDamageMs.getOrDefault(id, 0L);
        long now = System.currentTimeMillis();
        boolean outOfCombat = now - lastHit >= delayMs;
        long consumeWindowMs = Math.max(1000L, this.plugin.getConfig().getLong("absorption-health.consume-heal-window-seconds", 5L) * 1000L);
        long lastConsume = this.lastConsumeMs.getOrDefault(id, 0L);
        boolean consumedRecently = now - lastConsume <= consumeWindowMs;
        boolean canRecoverNow = immediate || consumedRecently || outOfCombat;
        double currentAbs = p.getAbsorptionAmount();
        if (canRecoverNow) {
            if (Math.abs(currentAbs - targetAbsorption) > 0.25) {
                p.setAbsorptionAmount(targetAbsorption);
            }
        } else if (currentAbs > targetAbsorption) {
            p.setAbsorptionAmount(targetAbsorption);
        }
        if ((pendingHeal = this.pendingHealFromMaxIncrease.getOrDefault(id, 0.0).doubleValue()) > 0.001 && canRecoverNow) {
            double heal;
            double missing = currentMaxHealth - p.getHealth();
            if (missing > 0.0 && (heal = Math.min(missing, pendingHeal)) > 0.0) {
                p.setHealth(Math.min(currentMaxHealth, p.getHealth() + heal));
                pendingHeal -= heal;
            }
            if (pendingHeal <= 0.001) {
                this.pendingHealFromMaxIncrease.remove(id);
            } else {
                this.pendingHealFromMaxIncrease.put(id, pendingHeal);
            }
        }
        if (this.plugin.getConfig().getBoolean("absorption-health.debug", false)) {
            this.plugin.getLogger().info("[ABS] " + p.getName() + " max=" + String.format("%.2f", currentMaxHealth) + " orbExtra=" + String.format("%.2f", orbExtraHealth) + " targetAbs=" + String.format("%.2f", targetAbsorption) + " canRecover=" + canRecoverNow + " pendingHeal=" + String.format("%.2f", this.pendingHealFromMaxIncrease.getOrDefault(id, 0.0)));
        }
    }

    private double computeOrbExtraHealth(Player p) {
        double total = 0.0;
        total += this.readMaxHealthFromMods(p.getInventory().getItemInMainHand());
        total += this.readMaxHealthFromMods(p.getInventory().getItemInOffHand());
        for (ItemStack armor : p.getInventory().getArmorContents()) {
            total += this.readMaxHealthFromMods(armor);
        }
        return Math.max(0.0, total);
    }

    private double readMaxHealthFromMods(ItemStack item) {
        if (item == null || item.getType().isAir()) {
            return 0.0;
        }
        NBTItem nbt = NBTItem.get((ItemStack)item);
        if (!nbt.hasTag("NEMONICORB_MODS")) {
            return 0.0;
        }
        try {
            String json = nbt.getString("NEMONICORB_MODS");
            if (json == null || json.isEmpty()) {
                return 0.0;
            }
            List mods = (List)this.gson.fromJson(json, ModifierEngine.ROLLED_LIST_TYPE);
            if (mods == null) {
                return 0.0;
            }
            double sum = 0.0;
            for (ModifierEngine.RolledModifier mod : mods) {
                Double hp = mod.stats().get("max-health");
                if (hp == null) continue;
                sum += hp.doubleValue();
            }
            return sum;
        }
        catch (Exception ignored) {
            return 0.0;
        }
    }
}

