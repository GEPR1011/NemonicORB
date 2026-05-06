package com.nemonicorp.orbs;

import io.lumine.mythic.lib.api.item.NBTItem;
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

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Aplica efeitos elementais quando o jogador ataca com arma que possui dano elemental.
 * Tambem soma o dano elemental ao dano base do hit.
 */
public class ElementalEffectListener implements Listener {

    private final NemonicOrbPlugin plugin;
    private final OrbListener orbListener;

    // Cooldown por jogador por elemento: UUID -> (elementKey -> lastProcTime)
    private final Map<UUID, Map<String, Long>> cooldowns = new ConcurrentHashMap<>();

    private static final Set<String> ELEMENTAL_STATS = Set.of(
            "fire-damage", "ice-damage", "lightning-damage",
            "earth-damage", "water-damage", "wind-damage"
    );

    public ElementalEffectListener(NemonicOrbPlugin plugin, OrbListener orbListener) {
        this.plugin = plugin;
        this.orbListener = orbListener;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (!plugin.getConfig().getBoolean("elemental-effects.enabled", true)) return;
        if (!(event.getDamager() instanceof Player player)) return;
        if (!(event.getEntity() instanceof LivingEntity target)) return;

        ItemStack weapon = player.getInventory().getItemInMainHand();
        if (weapon.getType().isAir()) return;

        NBTItem nbt = NBTItem.get(weapon);
        if (!nbt.hasTag(OrbListener.NBT_TIER)) return;

        int tier = nbt.getInteger(OrbListener.NBT_TIER);
        if (tier < 1) return;

        List<ModifierEngine.RolledModifier> mods = orbListener.readMods(nbt);
        if (mods.isEmpty()) return;

        long cooldownMs = plugin.getConfig().getLong("elemental-effects.cooldown-ms", 2000);
        long now = System.currentTimeMillis();
        Map<String, Long> playerCooldowns = cooldowns.computeIfAbsent(
                player.getUniqueId(), k -> new ConcurrentHashMap<>());

        double bonusDamage = 0;

        for (ModifierEngine.RolledModifier mod : mods) {
            for (var entry : mod.stats().entrySet()) {
                String statKey = entry.getKey();
                if (!ELEMENTAL_STATS.contains(statKey)) continue;

                double value = entry.getValue();
                if (value <= 0) continue;

                // Somar dano elemental ao hit
                bonusDamage += value;

                // Verificar cooldown para efeitos
                Long lastProc = playerCooldowns.get(statKey);
                if (lastProc != null && (now - lastProc) < cooldownMs) continue;
                playerCooldowns.put(statKey, now);

                // Aplicar efeito elemental
                applyElementalEffect(statKey, value, target, player);
            }
        }

        if (bonusDamage > 0) {
            event.setDamage(event.getDamage() + bonusDamage);
        }
    }

    private void applyElementalEffect(String statKey, double value,
                                       LivingEntity target, Player attacker) {
        switch (statKey) {
            case "fire-damage" -> {
                // Queimadura proporcional ao dano
                int ticks = (int) (value * 4);
                target.setFireTicks(Math.max(target.getFireTicks(), ticks));
            }
            case "ice-damage" -> {
                // Slowness + freeze
                int amplifier = Math.min((int) (value / 5), 2);
                int duration = (int) (value * 3);
                target.addPotionEffect(new PotionEffect(
                        PotionEffectType.SLOWNESS, duration, amplifier, true, false));
                target.setFreezeTicks(Math.min(
                        target.getFreezeTicks() + (int) (value * 5), 140));
            }
            case "lightning-damage" -> {
                // Stun: slowness IV + blindness breve
                int duration = (int) (value * 2);
                target.addPotionEffect(new PotionEffect(
                        PotionEffectType.SLOWNESS, duration, 3, true, false));
                target.addPotionEffect(new PotionEffect(
                        PotionEffectType.BLINDNESS, duration / 2, 0, true, false));
                // Efeito visual de raio (sem dano)
                if (plugin.getConfig().getBoolean("elemental-effects.lightning.visual-lightning", true)) {
                    target.getWorld().strikeLightningEffect(target.getLocation());
                }
            }
            case "earth-damage" -> {
                // Weakness no alvo
                int duration = (int) (value * 4);
                target.addPotionEffect(new PotionEffect(
                        PotionEffectType.WEAKNESS, duration, 0, true, false));
            }
            case "water-damage" -> {
                // Mining Fatigue + knockback
                int duration = (int) (value * 3);
                target.addPotionEffect(new PotionEffect(
                        PotionEffectType.MINING_FATIGUE, duration, 1, true, false));
                double knockbackMult = plugin.getConfig().getDouble(
                        "elemental-effects.water.knockback-multiplier", 0.05);
                Vector dir = target.getLocation().toVector()
                        .subtract(attacker.getLocation().toVector()).normalize();
                target.setVelocity(dir.multiply(value * knockbackMult));
            }
            case "wind-damage" -> {
                // Levitation breve + lancamento
                int duration = (int) (value * 2);
                target.addPotionEffect(new PotionEffect(
                        PotionEffectType.LEVITATION, duration, 0, true, false));
                double launchMult = plugin.getConfig().getDouble(
                        "elemental-effects.wind.launch-multiplier", 0.03);
                target.setVelocity(target.getVelocity().add(
                        new Vector(0, value * launchMult, 0)));
            }
        }
    }
}
