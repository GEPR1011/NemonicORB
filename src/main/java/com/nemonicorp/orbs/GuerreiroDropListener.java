/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.Indyuce.mmoitems.MMOItems
 *  net.Indyuce.mmoitems.api.Type
 *  org.bukkit.OfflinePlayer
 *  org.bukkit.entity.LivingEntity
 *  org.bukkit.entity.Monster
 *  org.bukkit.entity.Player
 *  org.bukkit.event.EventHandler
 *  org.bukkit.event.EventPriority
 *  org.bukkit.event.Listener
 *  org.bukkit.event.entity.EntityDeathEvent
 *  org.bukkit.inventory.ItemStack
 */
package com.nemonicorp.orbs;

import com.nemonicorp.orbs.NemonicOrbPlugin;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import net.Indyuce.mmoitems.MMOItems;
import net.Indyuce.mmoitems.api.Type;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Monster;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.inventory.ItemStack;

public class GuerreiroDropListener
implements Listener {
    private final NemonicOrbPlugin plugin;

    public GuerreiroDropListener(NemonicOrbPlugin plugin) {
        this.plugin = plugin;
    }

    private boolean debug() {
        return this.plugin.getConfig().getBoolean("debug", true);
    }

    @EventHandler(priority=EventPriority.MONITOR, ignoreCancelled=true)
    public void onEntityDeath(EntityDeathEvent event) {
        if (!this.plugin.getConfig().getBoolean("guerreiro.drops.enabled", true)) {
            return;
        }
        LivingEntity entity = event.getEntity();
        if (!(entity instanceof Monster)) {
            return;
        }
        Player killer = entity.getKiller();
        if (killer == null) {
            return;
        }
        if (!this.isGuerreiro(killer)) {
            return;
        }
        List<String> dropTable = List.of("RUNA_DE_PODER", "RUNA_NOBRE", "MOEDA_DA_SORTE");
        List<Double> chances = List.of(Double.valueOf(this.plugin.getConfig().getDouble("guerreiro.drops.runa-de-poder-chance", 0.05)), Double.valueOf(this.plugin.getConfig().getDouble("guerreiro.drops.runa-nobre-chance", 0.03)), Double.valueOf(this.plugin.getConfig().getDouble("guerreiro.drops.moeda-da-sorte-chance", 0.02)));
        ThreadLocalRandom rng = ThreadLocalRandom.current();
        for (int i = 0; i < dropTable.size(); ++i) {
            ItemStack orb;
            if (!(rng.nextDouble() < chances.get(i)) || (orb = this.createMMOItem("CONSUMABLE", dropTable.get(i))) == null) continue;
            event.getDrops().add(orb);
            if (!this.debug()) continue;
            this.plugin.getLogger().info("[GUERREIRO-DROP] " + killer.getName() + " dropou " + dropTable.get(i) + " ao matar " + entity.getType().name());
        }
    }

    private boolean isGuerreiro(Player player) {
        try {
            String className = this.getPlayerClassName(player);
            String configClassName = this.plugin.getConfig().getString("guerreiro.class-name", "Guerreiro");
            return className != null && className.equalsIgnoreCase(configClassName);
        }
        catch (Exception e) {
            return false;
        }
    }

    private String getPlayerClassName(Player player) {
        try {
            Class<?> pdClass = Class.forName("net.Indyuce.mmocore.api.player.PlayerData");
            Object data = this.getPlayerData(pdClass, player);
            if (data == null) {
                return null;
            }
            Object playerClass = null;
            for (String m : new String[]{"getProfess", "getPlayerClass", "getMMOClass"}) {
                try {
                    playerClass = pdClass.getMethod(m, new Class[0]).invoke(data, new Object[0]);
                    break;
                }
                catch (NoSuchMethodException noSuchMethodException) {
                }
            }
            if (playerClass == null) {
                return null;
            }
            for (String m : new String[]{"getName", "getId", "getKey"}) {
                try {
                    String s;
                    Object r = playerClass.getClass().getMethod(m, new Class[0]).invoke(playerClass, new Object[0]);
                    if (!(r instanceof String) || (s = (String)r).isEmpty()) continue;
                    return s;
                }
                catch (NoSuchMethodException noSuchMethodException) {
                    // empty catch block
                }
            }
            return null;
        }
        catch (ClassNotFoundException e) {
            return null;
        }
        catch (Exception e) {
            return null;
        }
    }

    private Object getPlayerData(Class<?> pdClass, Player player) throws Exception {
        try {
            return pdClass.getMethod("get", OfflinePlayer.class).invoke(null, player);
        }
        catch (NoSuchMethodException e1) {
            try {
                return pdClass.getMethod("get", Player.class).invoke(null, player);
            }
            catch (NoSuchMethodException e2) {
                return pdClass.getMethod("get", UUID.class).invoke(null, player.getUniqueId());
            }
        }
    }

    private ItemStack createMMOItem(String type, String id) {
        try {
            Type mmoType = Type.get((String)type);
            if (mmoType == null) {
                return null;
            }
            return MMOItems.plugin.getItem(mmoType, id);
        }
        catch (Exception e) {
            this.plugin.getLogger().warning("[GUERREIRO-DROP] Erro ao criar MMOItem " + type + "/" + id + ": " + e.getMessage());
            return null;
        }
    }
}

