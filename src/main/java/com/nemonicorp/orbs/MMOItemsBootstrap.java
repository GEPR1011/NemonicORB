package com.nemonicorp.orbs;

import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Garante que as 9 orbs do plugin estao presentes em
 * plugins/MMOItems/item/consumable.yml. Se faltar alguma, injeta a
 * partir do template embutido (orbs-mmoitems-template.yml).
 *
 * Roda no onEnable apos o MMOItems carregar. Se o MMOItems nao estiver
 * presente, apenas avisa e nao faz nada — o NemonicOrbPlugin tem
 * MMOItems como hard-depend, entao isso e seguro.
 *
 * Apos injecao, dispara `mi reload` para o MMOItems re-ler os items.
 */
public final class MMOItemsBootstrap {

    private static final String[] ORB_IDS = {
            "PERGAMINHO_DE_IDENTIFICACAO",
            "OLEO_DE_POLIMENTO",
            "PEDRA_DE_ENCANTAMENTO",
            "PEDRA_DE_REFORCO",
            "RUNA_NOBRE",
            "PEDRA_DE_REFINAMENTO",
            "RUNA_DE_PODER",
            "MOEDA_DA_SORTE",
            "PEDRA_CORROSIVA"
    };

    private final NemonicOrbPlugin plugin;

    public MMOItemsBootstrap(NemonicOrbPlugin plugin) {
        this.plugin = plugin;
    }

    public void run() {
        File pluginsDir = plugin.getDataFolder().getParentFile();
        if (pluginsDir == null) {
            plugin.getLogger().warning("[MMOItemsBootstrap] pluginsDir == null; abortando.");
            return;
        }
        File mmoItemsDir = new File(pluginsDir, "MMOItems");
        if (!mmoItemsDir.isDirectory()) {
            plugin.getLogger().warning("[MMOItemsBootstrap] Pasta plugins/MMOItems nao encontrada — orbs nao injetadas.");
            return;
        }
        File itemDir = new File(mmoItemsDir, "item");
        if (!itemDir.isDirectory()) {
            if (!itemDir.mkdirs()) {
                plugin.getLogger().warning("[MMOItemsBootstrap] Falha ao criar plugins/MMOItems/item.");
                return;
            }
        }
        File consumable = new File(itemDir, "consumable.yml");

        // Carregar config existente (ou criar vazia)
        YamlConfiguration current = consumable.exists()
                ? YamlConfiguration.loadConfiguration(consumable)
                : new YamlConfiguration();

        // Carregar template do classpath
        YamlConfiguration template = loadTemplate();
        if (template == null) {
            plugin.getLogger().warning("[MMOItemsBootstrap] Template orbs-mmoitems-template.yml nao encontrado no JAR.");
            return;
        }

        List<String> injected = new ArrayList<>();
        for (String orbId : ORB_IDS) {
            if (current.isConfigurationSection(orbId)) {
                continue; // ja existe — nao sobrescreve
            }
            ConfigurationSection src = template.getConfigurationSection(orbId);
            if (src == null) {
                plugin.getLogger().warning("[MMOItemsBootstrap] Template nao tem orb '" + orbId + "'.");
                continue;
            }
            // Copia recursiva chave por chave preservando ordem
            current.set(orbId, src);
            injected.add(orbId);
        }

        if (injected.isEmpty()) {
            plugin.getLogger().info("[MMOItemsBootstrap] Todas as 9 orbs ja registradas no MMOItems. Nada a fazer.");
            return;
        }

        // Salvar
        try {
            current.save(consumable);
        } catch (IOException e) {
            plugin.getLogger().severe("[MMOItemsBootstrap] Falha ao salvar consumable.yml: " + e.getMessage());
            return;
        }

        plugin.getLogger().info("[MMOItemsBootstrap] " + injected.size() + " orb(s) injetada(s) no MMOItems: " + injected);

        // Recarregar MMOItems para registrar as novas definicoes.
        // Atrasamos 2 ticks para garantir que o save foi finalizado em disco.
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            try {
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "mi reload");
                plugin.getLogger().info("[MMOItemsBootstrap] MMOItems reload disparado.");
            } catch (Exception e) {
                plugin.getLogger().warning("[MMOItemsBootstrap] Falha ao reloadar MMOItems via comando: " + e.getMessage()
                        + " — operadores podem rodar 'mi reload' manualmente.");
            }
        }, 2L);
    }

    private YamlConfiguration loadTemplate() {
        try (InputStream in = plugin.getResource("orbs-mmoitems-template.yml")) {
            if (in == null) return null;
            return YamlConfiguration.loadConfiguration(new InputStreamReader(in, StandardCharsets.UTF_8));
        } catch (IOException e) {
            plugin.getLogger().warning("[MMOItemsBootstrap] Erro lendo template: " + e.getMessage());
            return null;
        }
    }
}
