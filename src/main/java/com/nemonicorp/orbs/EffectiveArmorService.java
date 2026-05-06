package com.nemonicorp.orbs;

import com.google.gson.Gson;
import io.lumine.mythic.lib.api.item.NBTItem;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.inventory.ItemStack;

import java.util.List;

public class EffectiveArmorService implements Listener {
    private final NemonicOrbPlugin plugin;
    private final Gson gson;

    public EffectiveArmorService(NemonicOrbPlugin plugin) {
        this.plugin = plugin;
        this.gson = plugin.getModifierEngine().getGson();
    }

    public boolean isEnabled() {
        return plugin.getConfig().getBoolean("effective-armor.enabled", true);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onDamage(EntityDamageEvent event) {
        if (!isEnabled()) return;
        if (!(event.getEntity() instanceof Player p)) return;
        if (skipCause(event.getCause())) return;

        double effArmor = computeEffectiveArmor(p);
        if (effArmor <= 0) return;

        double baseScale = plugin.getConfig().getDouble("effective-armor.base-scale", 1.0);
        double maxReduction = clamp(plugin.getConfig().getDouble("effective-armor.max-reduction", 0.80), 0.0, 0.95);

        double scaled = effArmor * baseScale;
        double reduction = scaled / (scaled + 100.0);
        reduction = Math.min(maxReduction, reduction);
        reduction *= resolveDamageMultiplier(event);

        reduction = clamp(reduction, 0.0, maxReduction);
        if (reduction <= 0) return;
        event.setDamage(event.getDamage() * (1.0 - reduction));
    }

    private boolean skipCause(EntityDamageEvent.DamageCause cause) {
        return cause == EntityDamageEvent.DamageCause.VOID
                || cause == EntityDamageEvent.DamageCause.STARVATION
                || cause == EntityDamageEvent.DamageCause.SUICIDE
                || cause == EntityDamageEvent.DamageCause.KILL
                || cause == EntityDamageEvent.DamageCause.DROWNING
                || cause == EntityDamageEvent.DamageCause.SUFFOCATION
                || cause == EntityDamageEvent.DamageCause.FALL;
    }

    private double resolveDamageMultiplier(EntityDamageEvent event) {
        if (event instanceof EntityDamageByEntityEvent byEntity && byEntity.getDamager() instanceof Player) {
            return plugin.getConfig().getDouble("effective-armor.pvp-multiplier", 1.0);
        }
        if (event.getCause() == EntityDamageEvent.DamageCause.MAGIC
                || event.getCause() == EntityDamageEvent.DamageCause.POISON
                || event.getCause() == EntityDamageEvent.DamageCause.WITHER
                || event.getCause() == EntityDamageEvent.DamageCause.DRAGON_BREATH) {
            return plugin.getConfig().getDouble("effective-armor.magic-multiplier", 0.9);
        }
        return plugin.getConfig().getDouble("effective-armor.pve-multiplier", 1.0);
    }

    public double computeEffectiveArmor(Player p) {
        double armorScale = plugin.getConfig().getDouble("effective-armor.armor-scale", 1.0);
        double toughScale = plugin.getConfig().getDouble("effective-armor.toughness-scale", 0.7);
        double defenseScale = plugin.getConfig().getDouble("effective-armor.defense-scale", 0.6);

        double total = 0.0;
        for (ItemStack piece : p.getInventory().getArmorContents()) {
            total += readItemArmorValue(piece, armorScale, toughScale, defenseScale);
        }
        total += readItemArmorValue(p.getInventory().getItemInOffHand(), armorScale, toughScale, defenseScale);
        return Math.max(0.0, total);
    }

    private double readItemArmorValue(ItemStack item, double armorScale, double toughScale, double defenseScale) {
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
                Double armor = mod.stats().get("armor");
                Double tough = mod.stats().get("armor-toughness");
                Double defense = mod.stats().get("defense");
                if (armor != null) sum += armor * armorScale;
                if (tough != null) sum += tough * toughScale;
                if (defense != null) sum += defense * defenseScale;
            }
            return sum;
        } catch (Exception ignored) {
            return 0.0;
        }
    }

    private double clamp(double v, double min, double max) {
        return Math.max(min, Math.min(max, v));
    }
}

