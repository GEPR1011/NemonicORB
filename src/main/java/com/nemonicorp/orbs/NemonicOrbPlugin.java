package com.nemonicorp.orbs;

import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;

public final class NemonicOrbPlugin extends JavaPlugin {

    private static NemonicOrbPlugin instance;
    private ModifierEngine modifierEngine;
    private MerchantRouteManager routeManager;
    private MesaGuideManager guideManager;
    private OrbListener orbListener;
    private ModifierAuditService modifierAuditService;
    private EffectiveArmorService effectiveArmorService;
    private PublicWarpManager publicWarpManager;

    @Override
    public void onEnable() {
        instance = this;
        migrateConfig();

        // Sempre extrair arquivo de modificadores (overwrite para garantir versao correta)
        saveResource("nemonicorp_orb_modifiers.yml", true);

        // Garante que as 9 orbs estao registradas no MMOItems (injeta se faltar).
        new MMOItemsBootstrap(this).run();

        modifierEngine = new ModifierEngine(this);
        modifierEngine.load();

        guideManager = new MesaGuideManager();
        modifierAuditService = new ModifierAuditService(this);
        effectiveArmorService = new EffectiveArmorService(this);

        orbListener = new OrbListener(this);
        getServer().getPluginManager().registerEvents(orbListener, this);
        getServer().getPluginManager().registerEvents(effectiveArmorService, this);

        CraftingBlockListener craftingBlockListener = new CraftingBlockListener(this);
        TransmutationTableListener transmutationListener = new TransmutationTableListener(this);
        BlacksmithTableListener blacksmithListener = new BlacksmithTableListener(this);
        craftingBlockListener.setTransmutationListener(transmutationListener);
        craftingBlockListener.setBlacksmithListener(blacksmithListener);
        getServer().getPluginManager().registerEvents(craftingBlockListener, this);
        getServer().getPluginManager().registerEvents(transmutationListener, this);
        getServer().getPluginManager().registerEvents(blacksmithListener, this);

        routeManager = new MerchantRouteManager(this);
        routeManager.load();
        routeManager.startMaintenance();
        getServer().getPluginManager().registerEvents(routeManager, this);

        MerchantTableListener merchantListener = new MerchantTableListener(this, orbListener, routeManager);
        craftingBlockListener.setMerchantListener(merchantListener);
        orbListener.setMerchantListener(merchantListener);
        getServer().getPluginManager().registerEvents(merchantListener, this);

        getServer().getPluginManager().registerEvents(new ElementalEffectListener(this, orbListener), this);
        getServer().getPluginManager().registerEvents(new GuerreiroDropListener(this), this);
        MobLootDropListener mobLootListener = new MobLootDropListener(this, orbListener);
        mobLootListener.setMerchantListener(merchantListener);
        getServer().getPluginManager().registerEvents(mobLootListener, this);
        getServer().getPluginManager().registerEvents(new WelcomeBookListener(this), this);
        new ClassSelectionBookListener(this).registerIfAvailable();

        // Warps Publicas (#9)
        publicWarpManager = new PublicWarpManager(this);
        publicWarpManager.load();
        getServer().getPluginManager().registerEvents(publicWarpManager, this);

        PluginCommand cmd = getCommand("nemonicorb");
        if (cmd != null) {
            OrbCommand handler = new OrbCommand(this);
            cmd.setExecutor(handler);
            cmd.setTabCompleter(handler);
        }

        // Registrar conquistas
        registerEdwardAdvancement();
        registerCoracaoDeAcoAdvancement();
        registerRotaDeOuroAdvancement();

        getLogger().info("NemonicOrbPlugin v" + getDescription().getVersion() + " habilitado!");
        var ids = getConfig().getStringList("orb-ids");
        getLogger().info("Orbs v2 registrados: " + ids.size() + " -> " + ids);
        getLogger().info("Config version: " + getConfig().getInt("config-version", 0));
    }

    @SuppressWarnings("deprecation")
    private void registerEdwardAdvancement() {
        NamespacedKey key = new NamespacedKey(this, "edward");
        if (Bukkit.getAdvancement(key) != null) return;

        try {
            String json = """
                    {
                        "display": {
                            "icon": {"id": "minecraft:enchanting_table"},
                            "title": {"text": "Edward?", "color": "gold"},
                            "description": {"text": "Tem certeza disso?"},
                            "frame": "task",
                            "show_toast": true,
                            "announce_to_chat": true
                        },
                        "criteria": {
                            "crafted": {
                                "trigger": "minecraft:impossible"
                            }
                        }
                    }
                    """;
            Bukkit.getUnsafe().loadAdvancement(key, json);
            getLogger().info("Conquista 'Edward?' registrada com sucesso!");
        } catch (Exception e) {
            getLogger().warning("Erro ao registrar advancement Edward: " + e.getMessage());
        }
    }

    @SuppressWarnings("deprecation")
    private void registerCoracaoDeAcoAdvancement() {
        NamespacedKey key = new NamespacedKey(this, "coracao_de_aco");
        if (Bukkit.getAdvancement(key) != null) return;

        try {
            String json = """
                    {
                        "display": {
                            "icon": {"id": "minecraft:anvil"},
                            "title": {"text": "Coração de Aço", "color": "gold"},
                            "description": {"text": "Ferreiros... Ja ouvi falar deles..."},
                            "frame": "task",
                            "show_toast": true,
                            "announce_to_chat": true
                        },
                        "criteria": {
                            "crafted": {
                                "trigger": "minecraft:impossible"
                            }
                        }
                    }
                    """;
            Bukkit.getUnsafe().loadAdvancement(key, json);
            getLogger().info("Conquista 'Coracao de Aco' registrada com sucesso!");
        } catch (Exception e) {
            getLogger().warning("Erro ao registrar advancement Coracao de Aco: " + e.getMessage());
        }
    }

    @SuppressWarnings("deprecation")
    private void registerRotaDeOuroAdvancement() {
        NamespacedKey key = new NamespacedKey(this, "rota_de_ouro");
        if (Bukkit.getAdvancement(key) != null) return;

        try {
            String json = """
                    {
                        "display": {
                            "icon": {"id": "minecraft:cartography_table"},
                            "title": {"text": "Rota de Ouro", "color": "gold"},
                            "description": {"text": "Tudo tem um preco, isso me lembra alguem..."},
                            "frame": "task",
                            "show_toast": true,
                            "announce_to_chat": true
                        },
                        "criteria": {
                            "crafted": {
                                "trigger": "minecraft:impossible"
                            }
                        }
                    }
                    """;
            Bukkit.getUnsafe().loadAdvancement(key, json);
            getLogger().info("Conquista 'Rota de Ouro' registrada com sucesso!");
        } catch (Exception e) {
            getLogger().warning("Erro ao registrar advancement Rota de Ouro: " + e.getMessage());
        }
    }

    private void migrateConfig() {
        saveDefaultConfig();
        int ver = getConfig().getInt("config-version", 0);
        if (ver < 13) {
            getLogger().warning("Config antiga detectada (v" + ver + "). Substituindo por config v13...");
            File configFile = new File(getDataFolder(), "config.yml");
            if (configFile.exists()) configFile.delete();
            saveDefaultConfig();
            reloadConfig();
            getLogger().info("Config v13 instalada com sucesso!");
        }
    }

    @Override
    public void onDisable() {
        if (routeManager != null) routeManager.shutdown();
        instance = null;
        getLogger().info("NemonicOrbPlugin desabilitado.");
    }

    public static NemonicOrbPlugin getInstance() {
        return instance;
    }

    public ModifierEngine getModifierEngine() {
        return modifierEngine;
    }

    public MesaGuideManager getGuideManager() {
        return guideManager;
    }

    public OrbListener getOrbListener() {
        return orbListener;
    }

    public ModifierAuditService getModifierAuditService() {
        return modifierAuditService;
    }

    public PublicWarpManager getPublicWarpManager() {
        return publicWarpManager;
    }

    public EffectiveArmorService getEffectiveArmorService() {
        return effectiveArmorService;
    }
}
