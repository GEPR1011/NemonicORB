/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.Bukkit
 *  org.bukkit.entity.Player
 *  org.bukkit.event.Cancellable
 *  org.bukkit.event.Event
 *  org.bukkit.event.EventPriority
 *  org.bukkit.event.Listener
 *  org.bukkit.inventory.ItemStack
 *  org.bukkit.plugin.EventExecutor
 *  org.bukkit.plugin.Plugin
 */
package com.nemonicorp.orbs;

import com.nemonicorp.orbs.NemonicOrbPlugin;
import com.nemonicorp.orbs.WelcomeBookListener;
import java.util.Locale;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.EventExecutor;
import org.bukkit.plugin.Plugin;

public class ClassSelectionBookListener
implements Listener {
    private final NemonicOrbPlugin plugin;
    private Class<?> mmocoreClassChangeEvent;

    public ClassSelectionBookListener(NemonicOrbPlugin plugin) {
        this.plugin = plugin;
    }

    public boolean registerIfAvailable() {
        try {
            this.mmocoreClassChangeEvent = Class.forName("net.Indyuce.mmocore.api.event.PlayerChangeClassEvent");
            Bukkit.getPluginManager().registerEvent(this.mmocoreClassChangeEvent, (Listener)this, EventPriority.MONITOR, this.classChangeExecutor(), (Plugin)this.plugin, true);
            if (this.debug()) {
                this.plugin.getLogger().info("[CLASS-BOOK] Hook MMOCore registrado com sucesso.");
            }
            return true;
        }
        catch (ClassNotFoundException e) {
            this.plugin.getLogger().info("[CLASS-BOOK] MMOCore nao detectado; entrega de livro por selecao de classe desativada.");
            return false;
        }
        catch (Exception e) {
            this.plugin.getLogger().warning("[CLASS-BOOK] Falha ao registrar hook MMOCore: " + e.getMessage());
            return false;
        }
    }

    private EventExecutor classChangeExecutor() {
        return (ignored, event) -> {
            Cancellable cancellable;
            if (event == null || this.mmocoreClassChangeEvent == null) {
                return;
            }
            if (!this.mmocoreClassChangeEvent.isInstance(event)) {
                return;
            }
            if (event instanceof Cancellable && (cancellable = (Cancellable)event).isCancelled()) {
                return;
            }
            this.handleClassChange(event);
        };
    }

    private void handleClassChange(Event event) {
        try {
            Object playerObj = event.getClass().getMethod("getPlayer", new Class[0]).invoke((Object)event, new Object[0]);
            if (!(playerObj instanceof Player)) {
                return;
            }
            Player player = (Player)playerObj;
            Object newClass = event.getClass().getMethod("getNewClass", new Class[0]).invoke((Object)event, new Object[0]);
            if (newClass == null) {
                return;
            }
            String classKey = this.mapClassKey(this.extractClassName(newClass));
            if (classKey == null) {
                if (this.debug()) {
                    this.plugin.getLogger().warning("[CLASS-BOOK] Classe nao mapeada para livro.");
                }
                return;
            }
            ItemStack book = this.plugin.getGuideManager().createClassBook(classKey);
            if (book == null) {
                if (this.debug()) {
                    this.plugin.getLogger().warning("[CLASS-BOOK] Livro nao encontrado para classe=" + classKey);
                }
                return;
            }
            long delayTicks = this.computeDeliveryDelayTicks(player.getUniqueId(), player.hasPlayedBefore());
            Bukkit.getScheduler().runTaskLater((Plugin)this.plugin, () -> {
                if (!player.isOnline()) {
                    return;
                }
                player.getInventory().addItem(new ItemStack[]{book});
                player.openBook(book);
                if (this.debug()) {
                    this.plugin.getLogger().info("[CLASS-BOOK] Livro da classe '" + classKey + "' entregue para " + player.getName());
                }
            }, delayTicks);
        }
        catch (Exception e) {
            this.plugin.getLogger().warning("[CLASS-BOOK] Erro ao processar troca de classe: " + e.getMessage());
        }
    }

    private long computeDeliveryDelayTicks(UUID playerId, boolean hasPlayedBefore) {
        if (hasPlayedBefore) {
            return 1L;
        }
        if (!this.plugin.getConfig().getBoolean("welcome-book.enabled", true)) {
            return 1L;
        }
        long lastWelcome = WelcomeBookListener.getLastWelcomeDelivery(playerId);
        if (lastWelcome <= 0L) {
            return 60L;
        }
        long elapsedMs = System.currentTimeMillis() - lastWelcome;
        long waitMs = 2500L - elapsedMs;
        if (waitMs <= 0L) {
            return 1L;
        }
        return Math.max(1L, (waitMs + 49L) / 50L);
    }

    private String extractClassName(Object playerClass) {
        for (String method : new String[]{"getId", "getName", "getKey"}) {
            try {
                String str;
                Object value = playerClass.getClass().getMethod(method, new Class[0]).invoke(playerClass, new Object[0]);
                if (!(value instanceof String) || (str = (String)value).isBlank()) continue;
                return str;
            }
            catch (NoSuchMethodException value) {
            }
            catch (Exception e) {
                if (!this.debug()) continue;
                this.plugin.getLogger().warning("[CLASS-BOOK] Falha ao ler classe por " + method + ": " + e.getMessage());
            }
        }
        return null;
    }

    private String mapClassKey(String rawClassName) {
        if (rawClassName == null) {
            return null;
        }
        String key = rawClassName.toLowerCase(Locale.ROOT).trim();
        if (key.contains("alquim")) {
            return "alquimista";
        }
        if (key.contains("ferreir") || key.contains("blacksmith")) {
            return "ferreiro";
        }
        if (key.contains("mercad") || key.contains("merchant")) {
            return "mercador";
        }
        if (key.contains("guerreir") || key.contains("warrior")) {
            return "guerreiro";
        }
        return switch (key) {
            case "alquimista", "ferreiro", "mercador", "guerreiro" -> key;
            default -> null;
        };
    }

    private boolean debug() {
        return this.plugin.getConfig().getBoolean("debug", true);
    }
}

