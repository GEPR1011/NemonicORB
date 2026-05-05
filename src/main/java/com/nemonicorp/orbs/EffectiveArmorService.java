/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.gson.Gson
 *  io.lumine.mythic.lib.api.item.NBTItem
 *  org.bukkit.entity.Entity
 *  org.bukkit.entity.Player
 *  org.bukkit.event.EventHandler
 *  org.bukkit.event.EventPriority
 *  org.bukkit.event.Listener
 *  org.bukkit.event.entity.EntityDamageByEntityEvent
 *  org.bukkit.event.entity.EntityDamageEvent
 *  org.bukkit.event.entity.EntityDamageEvent$DamageCause
 *  org.bukkit.inventory.ItemStack
 */
package com.nemonicorp.orbs;

import com.google.gson.Gson;
import com.nemonicorp.orbs.ModifierEngine;
import com.nemonicorp.orbs.NemonicOrbPlugin;
import io.lumine.mythic.lib.api.item.NBTItem;
import java.util.List;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.inventory.ItemStack;

public class EffectiveArmorService
implements Listener {
    private final NemonicOrbPlugin plugin;
    private final Gson gson;

    public EffectiveArmorService(NemonicOrbPlugin plugin) {
        this.plugin = plugin;
        this.gson = plugin.getModifierEngine().getGson();
    }

    public boolean isEnabled() {
        return this.plugin.getConfig().getBoolean("effective-armor.enabled", true);
    }

    @EventHandler(priority=EventPriority.HIGHEST, ignoreCancelled=true)
    public void onDamage(EntityDamageEvent event) {
        if (!this.isEnabled()) {
            return;
        }
        Entity entity = event.getEntity();
        if (!(entity instanceof Player)) {
            return;
        }
        Player p = (Player)entity;
        if (this.skipCause(event.getCause())) {
            return;
        }
        double effArmor = this.computeEffectiveArmor(p);
        if (effArmor <= 0.0) {
            return;
        }
        double baseScale = this.plugin.getConfig().getDouble("effective-armor.base-scale", 1.0);
        double maxReduction = this.clamp(this.plugin.getConfig().getDouble("effective-armor.max-reduction", 0.8), 0.0, 0.95);
        double scaled = effArmor * baseScale;
        double reduction = scaled / (scaled + 100.0);
        reduction = Math.min(maxReduction, reduction);
        reduction *= this.resolveDamageMultiplier(event);
        if ((reduction = this.clamp(reduction, 0.0, maxReduction)) <= 0.0) {
            return;
        }
        event.setDamage(event.getDamage() * (1.0 - reduction));
    }

    private boolean skipCause(EntityDamageEvent.DamageCause cause) {
        return cause == EntityDamageEvent.DamageCause.VOID || cause == EntityDamageEvent.DamageCause.STARVATION || cause == EntityDamageEvent.DamageCause.SUICIDE || cause == EntityDamageEvent.DamageCause.KILL || cause == EntityDamageEvent.DamageCause.DROWNING || cause == EntityDamageEvent.DamageCause.SUFFOCATION || cause == EntityDamageEvent.DamageCause.FALL;
    }

    private double resolveDamageMultiplier(EntityDamageEvent event) {
        EntityDamageByEntityEvent byEntity;
        if (event instanceof EntityDamageByEntityEvent && (byEntity = (EntityDamageByEntityEvent)event).getDamager() instanceof Player) {
            return this.plugin.getConfig().getDouble("effective-armor.pvp-multiplier", 1.0);
        }
        if (event.getCause() == EntityDamageEvent.DamageCause.MAGIC || event.getCause() == EntityDamageEvent.DamageCause.POISON || event.getCause() == EntityDamageEvent.DamageCause.WITHER || event.getCause() == EntityDamageEvent.DamageCause.DRAGON_BREATH) {
            return this.plugin.getConfig().getDouble("effective-armor.magic-multiplier", 0.9);
        }
        return this.plugin.getConfig().getDouble("effective-armor.pve-multiplier", 1.0);
    }

    public double computeEffectiveArmor(Player p) {
        double armorScale = this.plugin.getConfig().getDouble("effective-armor.armor-scale", 1.0);
        double toughScale = this.plugin.getConfig().getDouble("effective-armor.toughness-scale", 0.7);
        double defenseScale = this.plugin.getConfig().getDouble("effective-armor.defense-scale", 0.6);
        double total = 0.0;
        for (ItemStack piece : p.getInventory().getArmorContents()) {
            total += this.readItemArmorValue(piece, armorScale, toughScale, defenseScale);
        }
        return Math.max(0.0, total += this.readItemArmorValue(p.getInventory().getItemInOffHand(), armorScale, toughScale, defenseScale));
    }

    private double readItemArmorValue(ItemStack item, double armorScale, double toughScale, double defenseScale) {
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
                Double armor = mod.stats().get("armor");
                Double tough = mod.stats().get("armor-toughness");
                Double defense = mod.stats().get("defense");
                if (armor != null) {
                    sum += armor * armorScale;
                }
                if (tough != null) {
                    sum += tough * toughScale;
                }
                if (defense == null) continue;
                sum += defense * defenseScale;
            }
            return sum;
        }
        catch (Exception ignored) {
            return 0.0;
        }
    }

    private double clamp(double v, double min, double max) {
        return Math.max(min, Math.min(max, v));
    }
}

