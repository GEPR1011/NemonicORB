/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.Bukkit
 *  org.bukkit.entity.Player
 *  org.bukkit.event.EventHandler
 *  org.bukkit.event.EventPriority
 *  org.bukkit.event.Listener
 *  org.bukkit.event.player.PlayerCommandPreprocessEvent
 *  org.bukkit.event.player.PlayerJoinEvent
 *  org.bukkit.inventory.ItemStack
 *  org.bukkit.plugin.Plugin
 */
package com.nemonicorp.orbs;

import com.nemonicorp.orbs.NemonicOrbPlugin;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

public class WelcomeBookListener
implements Listener {
    private final NemonicOrbPlugin plugin;
    private static final Map<UUID, Long> LAST_WELCOME_DELIVERY = new ConcurrentHashMap<UUID, Long>();

    public WelcomeBookListener(NemonicOrbPlugin plugin) {
        this.plugin = plugin;
    }

    public static long getLastWelcomeDelivery(UUID playerId) {
        return LAST_WELCOME_DELIVERY.getOrDefault(playerId, 0L);
    }

    @EventHandler(priority=EventPriority.MONITOR)
    public void onPlayerJoin(PlayerJoinEvent event) {
        if (!this.plugin.getConfig().getBoolean("welcome-book.enabled", true)) {
            return;
        }
        Player player = event.getPlayer();
        if (player.hasPlayedBefore()) {
            return;
        }
        Bukkit.getScheduler().runTaskLater((Plugin)this.plugin, () -> {
            if (!player.isOnline()) {
                return;
            }
            this.deliverWelcomeBook(player, true, "[WELCOME]");
        }, 40L);
    }

    @EventHandler(priority=EventPriority.MONITOR)
    public void onPlayerSpawnCommand(PlayerCommandPreprocessEvent event) {
        if (event.isCancelled()) {
            return;
        }
        if (!this.plugin.getConfig().getBoolean("welcome-book.on-spawn-command", true)) {
            return;
        }
        String raw = event.getMessage();
        if (raw == null || raw.isBlank()) {
            return;
        }
        String cmd = raw.trim().split("\\s+")[0].toLowerCase();
        if (!cmd.equals("/spawn") && !cmd.equals("/essentials:spawn")) {
            return;
        }
        Player player = event.getPlayer();
        Bukkit.getScheduler().runTaskLater((Plugin)this.plugin, () -> {
            if (!player.isOnline()) {
                return;
            }
            this.deliverWelcomeBook(player, false, "[WELCOME-SPAWN]");
        }, 2L);
    }

    private void deliverWelcomeBook(Player player, boolean openBook, String logPrefix) {
        ItemStack book = this.plugin.getGuideManager().createWelcomeBook();
        if (book == null) {
            return;
        }
        player.getInventory().addItem(new ItemStack[]{book});
        LAST_WELCOME_DELIVERY.put(player.getUniqueId(), System.currentTimeMillis());
        if (openBook) {
            player.openBook(book);
        }
        if (this.plugin.getConfig().getBoolean("debug", true)) {
            this.plugin.getLogger().info(logPrefix + " Guia de Embati entregue a " + player.getName());
        }
    }
}

