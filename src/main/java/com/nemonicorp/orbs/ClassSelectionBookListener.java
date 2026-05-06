package com.nemonicorp.orbs;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.EventExecutor;

import java.util.Locale;
import java.util.UUID;

/**
 * Entrega automaticamente o livro da classe sempre que o jogador
 * seleciona/troca classe no MMOCore.
 *
 * Implementacao via reflexao para manter compatibilidade quando o MMOCore
 * nao estiver instalado.
 */
public class ClassSelectionBookListener implements Listener {

    private final NemonicOrbPlugin plugin;
    private Class<?> mmocoreClassChangeEvent;

    public ClassSelectionBookListener(NemonicOrbPlugin plugin) {
        this.plugin = plugin;
    }

    @SuppressWarnings("unchecked")
    public boolean registerIfAvailable() {
        try {
            mmocoreClassChangeEvent = Class.forName("net.Indyuce.mmocore.api.event.PlayerChangeClassEvent");
            Bukkit.getPluginManager().registerEvent(
                    (Class<? extends Event>) mmocoreClassChangeEvent,
                    this,
                    EventPriority.MONITOR,
                    classChangeExecutor(),
                    plugin,
                    true
            );
            if (debug()) plugin.getLogger().info("[CLASS-BOOK] Hook MMOCore registrado com sucesso.");
            return true;
        } catch (ClassNotFoundException e) {
            plugin.getLogger().info("[CLASS-BOOK] MMOCore nao detectado; entrega de livro por selecao de classe desativada.");
            return false;
        } catch (Exception e) {
            plugin.getLogger().warning("[CLASS-BOOK] Falha ao registrar hook MMOCore: " + e.getMessage());
            return false;
        }
    }

    private EventExecutor classChangeExecutor() {
        return (ignored, event) -> {
            if (event == null || mmocoreClassChangeEvent == null) return;
            if (!mmocoreClassChangeEvent.isInstance(event)) return;
            if (event instanceof Cancellable cancellable && cancellable.isCancelled()) return;
            handleClassChange(event);
        };
    }

    private void handleClassChange(Event event) {
        try {
            Object playerObj = event.getClass().getMethod("getPlayer").invoke(event);
            if (!(playerObj instanceof Player player)) return;

            Object newClass = event.getClass().getMethod("getNewClass").invoke(event);
            if (newClass == null) return;

            String classKey = mapClassKey(extractClassName(newClass));
            if (classKey == null) {
                if (debug()) plugin.getLogger().warning("[CLASS-BOOK] Classe nao mapeada para livro.");
                return;
            }

            ItemStack book = plugin.getGuideManager().createClassBook(classKey);
            if (book == null) {
                if (debug()) plugin.getLogger().warning("[CLASS-BOOK] Livro nao encontrado para classe=" + classKey);
                return;
            }

            long delayTicks = computeDeliveryDelayTicks(player.getUniqueId(), player.hasPlayedBefore());
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (!player.isOnline()) return;
                player.getInventory().addItem(book);
                player.openBook(book);
                if (debug()) {
                    plugin.getLogger().info("[CLASS-BOOK] Livro da classe '" + classKey
                            + "' entregue para " + player.getName());
                }
            }, delayTicks);
        } catch (Exception e) {
            plugin.getLogger().warning("[CLASS-BOOK] Erro ao processar troca de classe: " + e.getMessage());
        }
    }

    private long computeDeliveryDelayTicks(UUID playerId, boolean hasPlayedBefore) {
        if (hasPlayedBefore) return 1L;
        if (!plugin.getConfig().getBoolean("welcome-book.enabled", true)) return 1L;

        long lastWelcome = WelcomeBookListener.getLastWelcomeDelivery(playerId);
        if (lastWelcome <= 0L) {
            // Primeiro login e guia de boas-vindas ainda pode nao ter sido entregue.
            return 60L;
        }

        long elapsedMs = System.currentTimeMillis() - lastWelcome;
        long waitMs = 2500L - elapsedMs;
        if (waitMs <= 0L) return 1L;
        return Math.max(1L, (waitMs + 49L) / 50L);
    }

    private String extractClassName(Object playerClass) {
        for (String method : new String[]{"getId", "getName", "getKey"}) {
            try {
                Object value = playerClass.getClass().getMethod(method).invoke(playerClass);
                if (value instanceof String str && !str.isBlank()) return str;
            } catch (NoSuchMethodException ignored) {
            } catch (Exception e) {
                if (debug()) plugin.getLogger().warning("[CLASS-BOOK] Falha ao ler classe por " + method + ": " + e.getMessage());
            }
        }
        return null;
    }

    private String mapClassKey(String rawClassName) {
        if (rawClassName == null) return null;
        String key = rawClassName.toLowerCase(Locale.ROOT).trim();

        if (key.contains("alquim")) return "alquimista";
        if (key.contains("ferreir") || key.contains("blacksmith")) return "ferreiro";
        if (key.contains("mercad") || key.contains("merchant")) return "mercador";
        if (key.contains("guerreir") || key.contains("warrior")) return "guerreiro";

        return switch (key) {
            case "alquimista", "ferreiro", "mercador", "guerreiro" -> key;
            default -> null;
        };
    }

    private boolean debug() {
        return plugin.getConfig().getBoolean("debug", true);
    }
}
