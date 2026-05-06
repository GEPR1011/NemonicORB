package com.nemonicorp.orbs;

import net.Indyuce.mmoitems.MMOItems;
import net.Indyuce.mmoitems.api.Type;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Monster;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Guerreiro — sem mesa propria, mas ao matar/ajudar a matar monstros
 * tem chance de dropar orbs raras diretamente no chao.
 */
public class GuerreiroDropListener implements Listener {

    private final NemonicOrbPlugin plugin;

    public GuerreiroDropListener(NemonicOrbPlugin plugin) {
        this.plugin = plugin;
    }

    private boolean debug() {
        return plugin.getConfig().getBoolean("debug", true);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityDeath(EntityDeathEvent event) {
        if (!plugin.getConfig().getBoolean("guerreiro.drops.enabled", true)) return;

        LivingEntity entity = event.getEntity();
        if (!(entity instanceof Monster)) return;

        Player killer = entity.getKiller();
        if (killer == null) return;
        if (!isGuerreiro(killer)) return;

        List<String> dropTable = List.of(
                "RUNA_DE_PODER",
                "RUNA_NOBRE",
                "MOEDA_DA_SORTE"
        );
        List<Double> chances = List.of(
                plugin.getConfig().getDouble("guerreiro.drops.runa-de-poder-chance", 0.05),
                plugin.getConfig().getDouble("guerreiro.drops.runa-nobre-chance", 0.03),
                plugin.getConfig().getDouble("guerreiro.drops.moeda-da-sorte-chance", 0.02)
        );

        ThreadLocalRandom rng = ThreadLocalRandom.current();

        for (int i = 0; i < dropTable.size(); i++) {
            if (rng.nextDouble() < chances.get(i)) {
                ItemStack orb = createMMOItem("CONSUMABLE", dropTable.get(i));
                if (orb != null) {
                    event.getDrops().add(orb);
                    if (debug()) plugin.getLogger().info("[GUERREIRO-DROP] " + killer.getName()
                            + " dropou " + dropTable.get(i) + " ao matar " + entity.getType().name());
                }
            }
        }
    }

    private boolean isGuerreiro(Player player) {
        try {
            String className = getPlayerClassName(player);
            String configClassName = plugin.getConfig().getString("guerreiro.class-name", "Guerreiro");
            return className != null && className.equalsIgnoreCase(configClassName);
        } catch (Exception e) {
            return false;
        }
    }

    private String getPlayerClassName(Player player) {
        try {
            Class<?> pdClass = Class.forName("net.Indyuce.mmocore.api.player.PlayerData");
            Object data = getPlayerData(pdClass, player);
            if (data == null) return null;
            Object playerClass = null;
            for (String m : new String[]{"getProfess", "getPlayerClass", "getMMOClass"}) {
                try { playerClass = pdClass.getMethod(m).invoke(data); break; }
                catch (NoSuchMethodException ignored) {}
            }
            if (playerClass == null) return null;
            for (String m : new String[]{"getName", "getId", "getKey"}) {
                try {
                    Object r = playerClass.getClass().getMethod(m).invoke(playerClass);
                    if (r instanceof String s && !s.isEmpty()) return s;
                } catch (NoSuchMethodException ignored) {}
            }
            return null;
        } catch (ClassNotFoundException e) { return null; }
        catch (Exception e) { return null; }
    }

    private Object getPlayerData(Class<?> pdClass, Player player) throws Exception {
        try { return pdClass.getMethod("get", org.bukkit.OfflinePlayer.class).invoke(null, player); }
        catch (NoSuchMethodException e1) {
            try { return pdClass.getMethod("get", Player.class).invoke(null, player); }
            catch (NoSuchMethodException e2) {
                return pdClass.getMethod("get", UUID.class).invoke(null, player.getUniqueId());
            }
        }
    }

    private ItemStack createMMOItem(String type, String id) {
        try {
            Type mmoType = Type.get(type);
            if (mmoType == null) return null;
            return MMOItems.plugin.getItem(mmoType, id);
        } catch (Exception e) {
            plugin.getLogger().warning("[GUERREIRO-DROP] Erro ao criar MMOItem " + type + "/" + id + ": " + e.getMessage());
            return null;
        }
    }
}
