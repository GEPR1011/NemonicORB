/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  io.lumine.mythic.lib.api.item.NBTItem
 *  org.bukkit.entity.Entity
 *  org.bukkit.entity.LivingEntity
 *  org.bukkit.entity.Player
 *  org.bukkit.event.EventHandler
 *  org.bukkit.event.EventPriority
 *  org.bukkit.event.Listener
 *  org.bukkit.event.entity.EntityDamageByEntityEvent
 *  org.bukkit.inventory.ItemStack
 *  org.bukkit.potion.PotionEffect
 *  org.bukkit.potion.PotionEffectType
 *  org.bukkit.util.Vector
 */
package com.nemonicorp.orbs;

import com.nemonicorp.orbs.ModifierEngine;
import com.nemonicorp.orbs.NemonicOrbPlugin;
import com.nemonicorp.orbs.OrbListener;
import io.lumine.mythic.lib.api.item.NBTItem;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

public class ElementalEffectListener
implements Listener {
    private final NemonicOrbPlugin plugin;
    private final OrbListener orbListener;
    private final Map<UUID, Map<String, Long>> cooldowns = new ConcurrentHashMap<UUID, Map<String, Long>>();
    private static final Set<String> ELEMENTAL_STATS = Set.of("fire-damage", "ice-damage", "lightning-damage", "earth-damage", "water-damage", "wind-damage");

    public ElementalEffectListener(NemonicOrbPlugin plugin, OrbListener orbListener) {
        this.plugin = plugin;
        this.orbListener = orbListener;
    }

    @EventHandler(priority=EventPriority.MONITOR, ignoreCancelled=true)
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (!this.plugin.getConfig().getBoolean("elemental-effects.enabled", true)) {
            return;
        }
        Entity entity = event.getDamager();
        if (!(entity instanceof Player)) {
            return;
        }
        Player player = (Player)entity;
        Entity entity2 = event.getEntity();
        if (!(entity2 instanceof LivingEntity)) {
            return;
        }
        LivingEntity target = (LivingEntity)entity2;
        ItemStack weapon = player.getInventory().getItemInMainHand();
        if (weapon.getType().isAir()) {
            return;
        }
        NBTItem nbt = NBTItem.get((ItemStack)weapon);
        if (!nbt.hasTag("NEMONICORB_TIER")) {
            return;
        }
        int tier = nbt.getInteger("NEMONICORB_TIER");
        if (tier < 1) {
            return;
        }
        List<ModifierEngine.RolledModifier> mods = this.orbListener.readMods(nbt);
        if (mods.isEmpty()) {
            return;
        }
        long cooldownMs = this.plugin.getConfig().getLong("elemental-effects.cooldown-ms", 2000L);
        long now = System.currentTimeMillis();
        Map playerCooldowns = this.cooldowns.computeIfAbsent(player.getUniqueId(), k -> new ConcurrentHashMap());
        double bonusDamage = 0.0;
        for (ModifierEngine.RolledModifier mod : mods) {
            for (Map.Entry<String, Double> entry : mod.stats().entrySet()) {
                double value;
                String statKey = entry.getKey();
                if (!ELEMENTAL_STATS.contains(statKey) || (value = entry.getValue().doubleValue()) <= 0.0) continue;
                bonusDamage += value;
                Long lastProc = (Long)playerCooldowns.get(statKey);
                if (lastProc != null && now - lastProc < cooldownMs) continue;
                playerCooldowns.put(statKey, now);
                this.applyElementalEffect(statKey, value, target, player);
            }
        }
        if (bonusDamage > 0.0) {
            event.setDamage(event.getDamage() + bonusDamage);
        }
    }

    private void applyElementalEffect(String statKey, double value, LivingEntity target, Player attacker) {
        switch (statKey) {
            case "fire-damage": {
                int ticks = (int)(value * 4.0);
                target.setFireTicks(Math.max(target.getFireTicks(), ticks));
                break;
            }
            case "ice-damage": {
                int amplifier = Math.min((int)(value / 5.0), 2);
                int duration = (int)(value * 3.0);
                target.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, duration, amplifier, true, false));
                target.setFreezeTicks(Math.min(target.getFreezeTicks() + (int)(value * 5.0), 140));
                break;
            }
            case "lightning-damage": {
                int duration = (int)(value * 2.0);
                target.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, duration, 3, true, false));
                target.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, duration / 2, 0, true, false));
                if (!this.plugin.getConfig().getBoolean("elemental-effects.lightning.visual-lightning", true)) break;
                target.getWorld().strikeLightningEffect(target.getLocation());
                break;
            }
            case "earth-damage": {
                int duration = (int)(value * 4.0);
                target.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, duration, 0, true, false));
                break;
            }
            case "water-damage": {
                int duration = (int)(value * 3.0);
                target.addPotionEffect(new PotionEffect(PotionEffectType.MINING_FATIGUE, duration, 1, true, false));
                double knockbackMult = this.plugin.getConfig().getDouble("elemental-effects.water.knockback-multiplier", 0.05);
                Vector dir = target.getLocation().toVector().subtract(attacker.getLocation().toVector()).normalize();
                target.setVelocity(dir.multiply(value * knockbackMult));
                break;
            }
            case "wind-damage": {
                int duration = (int)(value * 2.0);
                target.addPotionEffect(new PotionEffect(PotionEffectType.LEVITATION, duration, 0, true, false));
                double launchMult = this.plugin.getConfig().getDouble("elemental-effects.wind.launch-multiplier", 0.03);
                target.setVelocity(target.getVelocity().add(new Vector(0.0, value * launchMult, 0.0)));
            }
        }
    }
}

