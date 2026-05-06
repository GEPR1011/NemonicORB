package com.nemonicorp.orbs;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.ItemStack;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Entrega o Guia de Embati ao jogador no primeiro login.
 * O livro e dado no inventario + aberto automaticamente.
 */
public class WelcomeBookListener implements Listener {

    private final NemonicOrbPlugin plugin;
    private static final Map<UUID, Long> LAST_WELCOME_DELIVERY = new ConcurrentHashMap<>();

    public WelcomeBookListener(NemonicOrbPlugin plugin) {
        this.plugin = plugin;
    }

    public static long getLastWelcomeDelivery(UUID playerId) {
        return LAST_WELCOME_DELIVERY.getOrDefault(playerId, 0L);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerJoin(PlayerJoinEvent event) {
        if (!plugin.getConfig().getBoolean("welcome-book.enabled", true)) return;

        Player player = event.getPlayer();

        if (player.hasPlayedBefore()) return;

        // Agendar para 2 segundos apos o login (dar tempo de carregar)
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (!player.isOnline()) return;
            deliverWelcomeBook(player, true, "[WELCOME]");
        }, 40L); // 2 segundos
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerSpawnCommand(PlayerCommandPreprocessEvent event) {
        if (event.isCancelled()) return;
        if (!plugin.getConfig().getBoolean("welcome-book.on-spawn-command", true)) return;

        String raw = event.getMessage();
        if (raw == null || raw.isBlank()) return;

        String cmd = raw.trim().split("\\s+")[0].toLowerCase();
        if (!cmd.equals("/spawn") && !cmd.equals("/essentials:spawn")) return;

        Player player = event.getPlayer();
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (!player.isOnline()) return;
            deliverWelcomeBook(player, false, "[WELCOME-SPAWN]");
        }, 2L);
    }

    private void deliverWelcomeBook(Player player, boolean openBook, String logPrefix) {
        ItemStack book = plugin.getGuideManager().createWelcomeBook();
        if (book == null) return;

        player.getInventory().addItem(book);
        LAST_WELCOME_DELIVERY.put(player.getUniqueId(), System.currentTimeMillis());

        if (openBook) {
            player.openBook(book);
        }

        if (plugin.getConfig().getBoolean("debug", true)) {
            plugin.getLogger().info(logPrefix + " Guia de Embati entregue a " + player.getName());
        }
    }
}
