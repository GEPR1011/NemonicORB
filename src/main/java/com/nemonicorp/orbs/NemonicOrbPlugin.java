/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.Bukkit
 *  org.bukkit.NamespacedKey
 *  org.bukkit.command.CommandExecutor
 *  org.bukkit.command.PluginCommand
 *  org.bukkit.command.TabCompleter
 *  org.bukkit.event.Listener
 *  org.bukkit.plugin.Plugin
 *  org.bukkit.plugin.java.JavaPlugin
 */
package com.nemonicorp.orbs;

import com.nemonicorp.orbs.BlacksmithTableListener;
import com.nemonicorp.orbs.ClassSelectionBookListener;
import com.nemonicorp.orbs.CraftingBlockListener;
import com.nemonicorp.orbs.EffectiveArmorService;
import com.nemonicorp.orbs.ElementalEffectListener;
import com.nemonicorp.orbs.GuerreiroDropListener;
import com.nemonicorp.orbs.MerchantRouteManager;
import com.nemonicorp.orbs.MerchantTableListener;
import com.nemonicorp.orbs.MesaGuideManager;
import com.nemonicorp.orbs.MobLootDropListener;
import com.nemonicorp.orbs.ModifierAuditService;
import com.nemonicorp.orbs.ModifierEngine;
import com.nemonicorp.orbs.OrbCommand;
import com.nemonicorp.orbs.OrbListener;
import com.nemonicorp.orbs.PublicWarpManager;
import com.nemonicorp.orbs.TransmutationTableListener;
import com.nemonicorp.orbs.WelcomeBookListener;
import java.io.File;
import java.util.List;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.PluginCommand;
import org.bukkit.command.TabCompleter;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

public final class NemonicOrbPlugin
extends JavaPlugin {
    private static NemonicOrbPlugin instance;
    private ModifierEngine modifierEngine;
    private MerchantRouteManager routeManager;
    private MesaGuideManager guideManager;
    private OrbListener orbListener;
    private ModifierAuditService modifierAuditService;
    private EffectiveArmorService effectiveArmorService;
    private PublicWarpManager publicWarpManager;

    public void onEnable() {
        instance = this;
        this.migrateConfig();
        this.saveResource("nemonicorp_orb_modifiers.yml", true);
        this.modifierEngine = new ModifierEngine(this);
        this.modifierEngine.load();
        this.guideManager = new MesaGuideManager();
        this.modifierAuditService = new ModifierAuditService(this);
        this.effectiveArmorService = new EffectiveArmorService(this);
        this.orbListener = new OrbListener(this);
        this.getServer().getPluginManager().registerEvents((Listener)this.orbListener, (Plugin)this);
        this.getServer().getPluginManager().registerEvents((Listener)this.effectiveArmorService, (Plugin)this);
        CraftingBlockListener craftingBlockListener = new CraftingBlockListener(this);
        TransmutationTableListener transmutationListener = new TransmutationTableListener(this);
        BlacksmithTableListener blacksmithListener = new BlacksmithTableListener(this);
        craftingBlockListener.setTransmutationListener(transmutationListener);
        craftingBlockListener.setBlacksmithListener(blacksmithListener);
        this.getServer().getPluginManager().registerEvents((Listener)craftingBlockListener, (Plugin)this);
        this.getServer().getPluginManager().registerEvents((Listener)transmutationListener, (Plugin)this);
        this.getServer().getPluginManager().registerEvents((Listener)blacksmithListener, (Plugin)this);
        this.routeManager = new MerchantRouteManager(this);
        this.routeManager.load();
        this.routeManager.startMaintenance();
        this.getServer().getPluginManager().registerEvents((Listener)this.routeManager, (Plugin)this);
        MerchantTableListener merchantListener = new MerchantTableListener(this, this.orbListener, this.routeManager);
        craftingBlockListener.setMerchantListener(merchantListener);
        this.getServer().getPluginManager().registerEvents((Listener)merchantListener, (Plugin)this);
        this.getServer().getPluginManager().registerEvents((Listener)new ElementalEffectListener(this, this.orbListener), (Plugin)this);
        this.getServer().getPluginManager().registerEvents((Listener)new GuerreiroDropListener(this), (Plugin)this);
        this.getServer().getPluginManager().registerEvents((Listener)new MobLootDropListener(this, this.orbListener), (Plugin)this);
        this.getServer().getPluginManager().registerEvents((Listener)new WelcomeBookListener(this), (Plugin)this);
        new ClassSelectionBookListener(this).registerIfAvailable();
        this.publicWarpManager = new PublicWarpManager(this);
        this.publicWarpManager.load();
        this.getServer().getPluginManager().registerEvents((Listener)this.publicWarpManager, (Plugin)this);
        PluginCommand cmd = this.getCommand("nemonicorb");
        if (cmd != null) {
            OrbCommand handler = new OrbCommand(this);
            cmd.setExecutor((CommandExecutor)handler);
            cmd.setTabCompleter((TabCompleter)handler);
        }
        this.registerEdwardAdvancement();
        this.registerCoracaoDeAcoAdvancement();
        this.registerRotaDeOuroAdvancement();
        this.getLogger().info("NemonicOrbPlugin v" + this.getDescription().getVersion() + " habilitado!");
        List ids = this.getConfig().getStringList("orb-ids");
        this.getLogger().info("Orbs v2 registrados: " + ids.size() + " -> " + String.valueOf(ids));
        this.getLogger().info("Config version: " + this.getConfig().getInt("config-version", 0));
    }

    private void registerEdwardAdvancement() {
        NamespacedKey key = new NamespacedKey((Plugin)this, "edward");
        if (Bukkit.getAdvancement((NamespacedKey)key) != null) {
            return;
        }
        try {
            String json = "{\n    \"display\": {\n        \"icon\": {\"id\": \"minecraft:enchanting_table\"},\n        \"title\": {\"text\": \"Edward?\", \"color\": \"gold\"},\n        \"description\": {\"text\": \"Tem certeza disso?\"},\n        \"frame\": \"task\",\n        \"show_toast\": true,\n        \"announce_to_chat\": true\n    },\n    \"criteria\": {\n        \"crafted\": {\n            \"trigger\": \"minecraft:impossible\"\n        }\n    }\n}\n";
            Bukkit.getUnsafe().loadAdvancement(key, json);
            this.getLogger().info("Conquista 'Edward?' registrada com sucesso!");
        }
        catch (Exception e) {
            this.getLogger().warning("Erro ao registrar advancement Edward: " + e.getMessage());
        }
    }

    private void registerCoracaoDeAcoAdvancement() {
        NamespacedKey key = new NamespacedKey((Plugin)this, "coracao_de_aco");
        if (Bukkit.getAdvancement((NamespacedKey)key) != null) {
            return;
        }
        try {
            String json = "{\n    \"display\": {\n        \"icon\": {\"id\": \"minecraft:anvil\"},\n        \"title\": {\"text\": \"Cora\u00e7\u00e3o de A\u00e7o\", \"color\": \"gold\"},\n        \"description\": {\"text\": \"Ferreiros... Ja ouvi falar deles...\"},\n        \"frame\": \"task\",\n        \"show_toast\": true,\n        \"announce_to_chat\": true\n    },\n    \"criteria\": {\n        \"crafted\": {\n            \"trigger\": \"minecraft:impossible\"\n        }\n    }\n}\n";
            Bukkit.getUnsafe().loadAdvancement(key, json);
            this.getLogger().info("Conquista 'Coracao de Aco' registrada com sucesso!");
        }
        catch (Exception e) {
            this.getLogger().warning("Erro ao registrar advancement Coracao de Aco: " + e.getMessage());
        }
    }

    private void registerRotaDeOuroAdvancement() {
        NamespacedKey key = new NamespacedKey((Plugin)this, "rota_de_ouro");
        if (Bukkit.getAdvancement((NamespacedKey)key) != null) {
            return;
        }
        try {
            String json = "{\n    \"display\": {\n        \"icon\": {\"id\": \"minecraft:cartography_table\"},\n        \"title\": {\"text\": \"Rota de Ouro\", \"color\": \"gold\"},\n        \"description\": {\"text\": \"Tudo tem um preco, isso me lembra alguem...\"},\n        \"frame\": \"task\",\n        \"show_toast\": true,\n        \"announce_to_chat\": true\n    },\n    \"criteria\": {\n        \"crafted\": {\n            \"trigger\": \"minecraft:impossible\"\n        }\n    }\n}\n";
            Bukkit.getUnsafe().loadAdvancement(key, json);
            this.getLogger().info("Conquista 'Rota de Ouro' registrada com sucesso!");
        }
        catch (Exception e) {
            this.getLogger().warning("Erro ao registrar advancement Rota de Ouro: " + e.getMessage());
        }
    }

    private void migrateConfig() {
        this.saveDefaultConfig();
        int ver = this.getConfig().getInt("config-version", 0);
        if (ver < 13) {
            this.getLogger().warning("Config antiga detectada (v" + ver + "). Substituindo por config v13...");
            File configFile = new File(this.getDataFolder(), "config.yml");
            if (configFile.exists()) {
                configFile.delete();
            }
            this.saveDefaultConfig();
            this.reloadConfig();
            this.getLogger().info("Config v13 instalada com sucesso!");
        }
    }

    public void onDisable() {
        if (this.routeManager != null) {
            this.routeManager.shutdown();
        }
        instance = null;
        this.getLogger().info("NemonicOrbPlugin desabilitado.");
    }

    public static NemonicOrbPlugin getInstance() {
        return instance;
    }

    public ModifierEngine getModifierEngine() {
        return this.modifierEngine;
    }

    public MesaGuideManager getGuideManager() {
        return this.guideManager;
    }

    public OrbListener getOrbListener() {
        return this.orbListener;
    }

    public ModifierAuditService getModifierAuditService() {
        return this.modifierAuditService;
    }

    public PublicWarpManager getPublicWarpManager() {
        return this.publicWarpManager;
    }

    public EffectiveArmorService getEffectiveArmorService() {
        return this.effectiveArmorService;
    }
}

