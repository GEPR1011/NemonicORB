/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.kyori.adventure.text.Component
 *  net.kyori.adventure.text.TextComponent$Builder
 *  net.kyori.adventure.text.format.NamedTextColor
 *  net.kyori.adventure.text.format.TextColor
 *  net.kyori.adventure.text.format.TextDecoration
 *  net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
 *  org.bukkit.Material
 *  org.bukkit.entity.Player
 *  org.bukkit.inventory.ItemStack
 *  org.bukkit.inventory.meta.BookMeta
 *  org.bukkit.inventory.meta.ItemMeta
 */
package com.nemonicorp.orbs;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;
import org.bukkit.inventory.meta.ItemMeta;

public class MesaGuideManager {
    private static final int BOOK_MAX_PAGES = 50;
    private static final int BOOK_MAX_LINES_PER_PAGE = 13;
    private static final int BOOK_MAX_CHARS_PER_LINE = 20;
    private static final int BOOK_MAX_CHARS_PER_PAGE = 256;
    private final Set<String> seenGuides = new HashSet<String>();

    public boolean showGuideIfNeeded(Player player, String mesaType) {
        String key = String.valueOf(player.getUniqueId()) + ":" + mesaType;
        if (this.seenGuides.contains(key)) {
            return false;
        }
        this.seenGuides.add(key);
        ItemStack book = this.createGuideBook(mesaType);
        if (book == null) {
            return false;
        }
        player.openBook(book);
        return true;
    }

    private ItemStack createGuideBook(String mesaType) {
        return switch (mesaType) {
            case "alquimista" -> this.createAlquimistaGuide();
            case "ferreiro" -> this.createFerreiroGuide();
            case "mercador" -> this.createMercadorGuide();
            default -> null;
        };
    }

    public ItemStack createClassBook(String classKey) {
        if (classKey == null) {
            return null;
        }
        return switch (classKey.toLowerCase(Locale.ROOT)) {
            case "alquimista" -> this.createAlquimistaGuide();
            case "ferreiro" -> this.createFerreiroGuide();
            case "mercador" -> this.createMercadorGuide();
            case "guerreiro" -> this.createGuerreiroGuide();
            default -> null;
        };
    }

    private ItemStack createAlquimistaGuide() {
        ItemStack book = new ItemStack(Material.WRITTEN_BOOK);
        BookMeta meta = (BookMeta)book.getItemMeta();
        if (meta == null) {
            return null;
        }
        meta.setTitle("\u00a7dGuia da Mesa de Transmutacao");
        meta.setAuthor("NemonicRP");
        meta.displayName((Component)Component.text((String)"Grimorio do Alquimista", (TextColor)NamedTextColor.LIGHT_PURPLE, (TextDecoration[])new TextDecoration[]{TextDecoration.BOLD}));
        meta.addPages(new Component[]{((TextComponent.Builder)((TextComponent.Builder)((TextComponent.Builder)((TextComponent.Builder)((TextComponent.Builder)((TextComponent.Builder)((TextComponent.Builder)((TextComponent.Builder)Component.text().append((Component)Component.text((String)"Mesa de Transmutacao\n", (TextColor)NamedTextColor.DARK_PURPLE, (TextDecoration[])new TextDecoration[]{TextDecoration.BOLD}))).append((Component)Component.text((String)"Guia do Alquimista\n\n", (TextColor)NamedTextColor.DARK_GRAY))).append((Component)Component.text((String)"Mesa da classe: ", (TextColor)NamedTextColor.BLACK))).append((Component)Component.text((String)"Mesa de Encantamento\n\n", (TextColor)NamedTextColor.LIGHT_PURPLE, (TextDecoration[])new TextDecoration[]{TextDecoration.BOLD}))).append((Component)Component.text((String)"Nivel 5+: fabrica orbs.\n", (TextColor)NamedTextColor.BLACK))).append((Component)Component.text((String)"Nivel 35+: transmutacao de itens.\n\n", (TextColor)NamedTextColor.BLACK))).append((Component)Component.text((String)"Especialidade: converter\n", (TextColor)NamedTextColor.DARK_AQUA))).append((Component)Component.text((String)"materiais e itens em orbs.", (TextColor)NamedTextColor.DARK_AQUA))).build()});
        meta.addPages(new Component[]{((TextComponent.Builder)((TextComponent.Builder)((TextComponent.Builder)((TextComponent.Builder)((TextComponent.Builder)((TextComponent.Builder)((TextComponent.Builder)((TextComponent.Builder)Component.text().append((Component)Component.text((String)"Orbs da Classe\n\n", (TextColor)NamedTextColor.GOLD, (TextDecoration[])new TextDecoration[]{TextDecoration.BOLD}))).append((Component)Component.text((String)"Fabrica direto na mesa:\n", (TextColor)NamedTextColor.BLACK))).append((Component)Component.text((String)"- Pedra de Encantamento\n", (TextColor)NamedTextColor.DARK_GREEN))).append((Component)Component.text((String)"- Pedra de Reforco\n\n", (TextColor)NamedTextColor.DARK_GREEN))).append((Component)Component.text((String)"Transmutacao (Nv 35+) gera:\n", (TextColor)NamedTextColor.BLACK))).append((Component)Component.text((String)"Encantamento, Reforco,\n", (TextColor)NamedTextColor.DARK_GRAY))).append((Component)Component.text((String)"Pedra Corrosiva ou Moeda\n", (TextColor)NamedTextColor.DARK_GRAY))).append((Component)Component.text((String)"da Sorte.", (TextColor)NamedTextColor.DARK_GRAY))).build()});
        meta.addPages(new Component[]{((TextComponent.Builder)((TextComponent.Builder)((TextComponent.Builder)((TextComponent.Builder)((TextComponent.Builder)((TextComponent.Builder)((TextComponent.Builder)Component.text().append((Component)Component.text((String)"Funcoes da Mesa\n\n", (TextColor)NamedTextColor.DARK_PURPLE, (TextDecoration[])new TextDecoration[]{TextDecoration.BOLD}))).append((Component)Component.text((String)"1. Craft de orb por botao\n", (TextColor)NamedTextColor.BLACK))).append((Component)Component.text((String)"2. Transmutar equipamento\n", (TextColor)NamedTextColor.BLACK))).append((Component)Component.text((String)"3. Escalar resultado por\n", (TextColor)NamedTextColor.BLACK))).append((Component)Component.text((String)"   eficiencia ambiental\n\n", (TextColor)NamedTextColor.BLACK))).append((Component)Component.text((String)"A eficiencia aumenta a\n", (TextColor)NamedTextColor.DARK_GREEN))).append((Component)Component.text((String)"quantidade final de orbs.", (TextColor)NamedTextColor.DARK_GREEN))).build()});
        meta.addPages(new Component[]{((TextComponent.Builder)((TextComponent.Builder)((TextComponent.Builder)((TextComponent.Builder)((TextComponent.Builder)((TextComponent.Builder)((TextComponent.Builder)Component.text().append((Component)Component.text((String)"Receitas Base\n\n", (TextColor)NamedTextColor.RED, (TextDecoration[])new TextDecoration[]{TextDecoration.BOLD}))).append((Component)Component.text((String)"Pedra de Encantamento:\n", (TextColor)NamedTextColor.BLACK))).append((Component)Component.text((String)"32 Carne Podre OU 32 Osso\n", (TextColor)NamedTextColor.DARK_GRAY))).append((Component)Component.text((String)"OU 10 Perola do End.\n\n", (TextColor)NamedTextColor.DARK_GRAY))).append((Component)Component.text((String)"Pedra de Reforco:\n", (TextColor)NamedTextColor.BLACK))).append((Component)Component.text((String)"8 Ovo Tartaruga OU 32\n", (TextColor)NamedTextColor.DARK_GRAY))).append((Component)Component.text((String)"Couro OU 32 La.", (TextColor)NamedTextColor.DARK_GRAY))).build()});
        meta.addPages(new Component[]{((TextComponent.Builder)((TextComponent.Builder)((TextComponent.Builder)((TextComponent.Builder)((TextComponent.Builder)((TextComponent.Builder)((TextComponent.Builder)Component.text().append((Component)Component.text((String)"Dica de Progresso\n\n", (TextColor)NamedTextColor.DARK_GREEN, (TextDecoration[])new TextDecoration[]{TextDecoration.BOLD}))).append((Component)Component.text((String)"Use o craft de orbs para\n", (TextColor)NamedTextColor.BLACK))).append((Component)Component.text((String)"subir economia cedo e\n", (TextColor)NamedTextColor.BLACK))).append((Component)Component.text((String)"deixe transmutacao para\n", (TextColor)NamedTextColor.BLACK))).append((Component)Component.text((String)"itens realmente valiosos.\n\n", (TextColor)NamedTextColor.BLACK))).append((Component)Component.text((String)"Alquimista bom abastece\n", (TextColor)NamedTextColor.DARK_GREEN))).append((Component)Component.text((String)"todo o servidor.", (TextColor)NamedTextColor.DARK_GREEN))).build()});
        this.normalizeBookPages(meta);
        book.setItemMeta((ItemMeta)meta);
        return book;
    }

    private ItemStack createFerreiroGuide() {
        ItemStack book = new ItemStack(Material.WRITTEN_BOOK);
        BookMeta meta = (BookMeta)book.getItemMeta();
        if (meta == null) {
            return null;
        }
        meta.setTitle("\u00a7cGuia da Mesa do Ferreiro");
        meta.setAuthor("NemonicRP");
        meta.displayName((Component)Component.text((String)"Manual da Forja", (TextColor)NamedTextColor.RED, (TextDecoration[])new TextDecoration[]{TextDecoration.BOLD}));
        meta.addPages(new Component[]{((TextComponent.Builder)((TextComponent.Builder)((TextComponent.Builder)((TextComponent.Builder)((TextComponent.Builder)((TextComponent.Builder)((TextComponent.Builder)((TextComponent.Builder)Component.text().append((Component)Component.text((String)"Mesa do Ferreiro\n", (TextColor)NamedTextColor.DARK_RED, (TextDecoration[])new TextDecoration[]{TextDecoration.BOLD}))).append((Component)Component.text((String)"Guia do Ferreiro\n\n", (TextColor)NamedTextColor.DARK_GRAY))).append((Component)Component.text((String)"Mesa da classe: ", (TextColor)NamedTextColor.BLACK))).append((Component)Component.text((String)"Bigorna\n\n", (TextColor)NamedTextColor.RED, (TextDecoration[])new TextDecoration[]{TextDecoration.BOLD}))).append((Component)Component.text((String)"Nivel 5+: acesso a forja,\n", (TextColor)NamedTextColor.BLACK))).append((Component)Component.text((String)"reparo e fabricacao de orb.\n\n", (TextColor)NamedTextColor.BLACK))).append((Component)Component.text((String)"A mesa trabalha com etapas\n", (TextColor)NamedTextColor.DARK_AQUA))).append((Component)Component.text((String)"de campo (multi-bloco).", (TextColor)NamedTextColor.DARK_AQUA))).build()});
        meta.addPages(new Component[]{((TextComponent.Builder)((TextComponent.Builder)((TextComponent.Builder)((TextComponent.Builder)((TextComponent.Builder)((TextComponent.Builder)((TextComponent.Builder)((TextComponent.Builder)Component.text().append((Component)Component.text((String)"Orb da Classe\n\n", (TextColor)NamedTextColor.GOLD, (TextDecoration[])new TextDecoration[]{TextDecoration.BOLD}))).append((Component)Component.text((String)"Fabrica: ", (TextColor)NamedTextColor.BLACK))).append((Component)Component.text((String)"Oleo de Polimento\n", (TextColor)NamedTextColor.DARK_GREEN, (TextDecoration[])new TextDecoration[]{TextDecoration.BOLD}))).append((Component)Component.text((String)"Usado para melhorar valores\n", (TextColor)NamedTextColor.DARK_GRAY))).append((Component)Component.text((String)"de mods ja existentes.\n", (TextColor)NamedTextColor.DARK_GRAY))).append((Component)Component.text((String)"Receita: 20 pepitas de ouro.\n\n", (TextColor)NamedTextColor.DARK_GRAY))).append((Component)Component.text((String)"Tambem participa da receita\n", (TextColor)NamedTextColor.BLACK))).append((Component)Component.text((String)"da Pedra de Refinamento.", (TextColor)NamedTextColor.BLACK))).build()});
        meta.addPages(new Component[]{((TextComponent.Builder)((TextComponent.Builder)((TextComponent.Builder)((TextComponent.Builder)((TextComponent.Builder)((TextComponent.Builder)((TextComponent.Builder)((TextComponent.Builder)((TextComponent.Builder)Component.text().append((Component)Component.text((String)"Fluxo da Forja\n\n", (TextColor)NamedTextColor.DARK_PURPLE, (TextDecoration[])new TextDecoration[]{TextDecoration.BOLD}))).append((Component)Component.text((String)"1. Iniciar na Bigorna\n", (TextColor)NamedTextColor.BLACK))).append((Component)Component.text((String)"2. Aquecer na Blast Furnace\n", (TextColor)NamedTextColor.BLACK))).append((Component)Component.text((String)"3. Modelar na Bigorna\n", (TextColor)NamedTextColor.BLACK))).append((Component)Component.text((String)"4. Resfriar no Caldeirao\n", (TextColor)NamedTextColor.BLACK))).append((Component)Component.text((String)"5. Refinar no Rebolo\n", (TextColor)NamedTextColor.BLACK))).append((Component)Component.text((String)"6. Coletar na mesa\n\n", (TextColor)NamedTextColor.BLACK))).append((Component)Component.text((String)"Cada acerto melhora a\n", (TextColor)NamedTextColor.DARK_GREEN))).append((Component)Component.text((String)"qualidade da forja.", (TextColor)NamedTextColor.DARK_GREEN))).build()});
        meta.addPages(new Component[]{((TextComponent.Builder)((TextComponent.Builder)((TextComponent.Builder)((TextComponent.Builder)((TextComponent.Builder)((TextComponent.Builder)((TextComponent.Builder)((TextComponent.Builder)((TextComponent.Builder)Component.text().append((Component)Component.text((String)"Estrutura da Forja\n\n", (TextColor)NamedTextColor.GOLD, (TextDecoration[])new TextDecoration[]{TextDecoration.BOLD}))).append((Component)Component.text((String)"Blocos necessarios:\n", (TextColor)NamedTextColor.BLACK))).append((Component)Component.text((String)"- Bancada de Ferraria\n", (TextColor)NamedTextColor.DARK_GRAY))).append((Component)Component.text((String)"- Alto-Forno\n", (TextColor)NamedTextColor.DARK_GRAY))).append((Component)Component.text((String)"- Bigorna\n", (TextColor)NamedTextColor.DARK_GRAY))).append((Component)Component.text((String)"- Caldeirao\n", (TextColor)NamedTextColor.DARK_GRAY))).append((Component)Component.text((String)"- Rebolo\n\n", (TextColor)NamedTextColor.DARK_GRAY))).append((Component)Component.text((String)"Sem essa estrutura completa,\n", (TextColor)NamedTextColor.DARK_RED))).append((Component)Component.text((String)"a forja nao finaliza.", (TextColor)NamedTextColor.DARK_RED))).build()});
        meta.addPages(new Component[]{((TextComponent.Builder)((TextComponent.Builder)((TextComponent.Builder)((TextComponent.Builder)((TextComponent.Builder)((TextComponent.Builder)Component.text().append((Component)Component.text((String)"Funcoes da Mesa\n\n", (TextColor)NamedTextColor.AQUA, (TextDecoration[])new TextDecoration[]{TextDecoration.BOLD}))).append((Component)Component.text((String)"- Melhorar stats por forja\n", (TextColor)NamedTextColor.BLACK))).append((Component)Component.text((String)"- Reparar equipamentos\n", (TextColor)NamedTextColor.BLACK))).append((Component)Component.text((String)"- Fabricar Oleo de Polimento\n\n", (TextColor)NamedTextColor.BLACK))).append((Component)Component.text((String)"Sem os blocos da forja\n", (TextColor)NamedTextColor.DARK_RED))).append((Component)Component.text((String)"o processo nao conclui.", (TextColor)NamedTextColor.DARK_RED))).build()});
        meta.addPages(new Component[]{((TextComponent.Builder)((TextComponent.Builder)((TextComponent.Builder)((TextComponent.Builder)((TextComponent.Builder)((TextComponent.Builder)Component.text().append((Component)Component.text((String)"Dica de Oficina\n\n", (TextColor)NamedTextColor.DARK_GREEN, (TextDecoration[])new TextDecoration[]{TextDecoration.BOLD}))).append((Component)Component.text((String)"Monte a estrutura completa\n", (TextColor)NamedTextColor.BLACK))).append((Component)Component.text((String)"perto da Bigorna para nao\n", (TextColor)NamedTextColor.BLACK))).append((Component)Component.text((String)"perder tempo no trajeto.\n\n", (TextColor)NamedTextColor.BLACK))).append((Component)Component.text((String)"Ferreiro forte sustenta o\n", (TextColor)NamedTextColor.DARK_GREEN))).append((Component)Component.text((String)"late-game do grupo.", (TextColor)NamedTextColor.DARK_GREEN))).build()});
        this.normalizeBookPages(meta);
        book.setItemMeta((ItemMeta)meta);
        return book;
    }

    private ItemStack createMercadorGuide() {
        ItemStack book = new ItemStack(Material.WRITTEN_BOOK);
        BookMeta meta = (BookMeta)book.getItemMeta();
        if (meta == null) {
            return null;
        }
        meta.setTitle("\u00a76Guia da Mesa do Mercador");
        meta.setAuthor("NemonicRP");
        meta.displayName((Component)Component.text((String)"Compendio Mercantil", (TextColor)NamedTextColor.GOLD, (TextDecoration[])new TextDecoration[]{TextDecoration.BOLD}));
        meta.addPages(new Component[]{((TextComponent.Builder)((TextComponent.Builder)((TextComponent.Builder)((TextComponent.Builder)((TextComponent.Builder)((TextComponent.Builder)((TextComponent.Builder)((TextComponent.Builder)Component.text().append((Component)Component.text((String)"Mesa do Mercador\n", (TextColor)NamedTextColor.GOLD, (TextDecoration[])new TextDecoration[]{TextDecoration.BOLD}))).append((Component)Component.text((String)"Guia do Mercador\n\n", (TextColor)NamedTextColor.DARK_GRAY))).append((Component)Component.text((String)"Mesa da classe: ", (TextColor)NamedTextColor.BLACK))).append((Component)Component.text((String)"Bancada de Cartografia\n\n", (TextColor)NamedTextColor.GOLD, (TextDecoration[])new TextDecoration[]{TextDecoration.BOLD}))).append((Component)Component.text((String)"A classe gerencia economia,\n", (TextColor)NamedTextColor.BLACK))).append((Component)Component.text((String)"identificacao e mobilidade.\n\n", (TextColor)NamedTextColor.BLACK))).append((Component)Component.text((String)"Parte das funcoes depende\n", (TextColor)NamedTextColor.DARK_GREEN))).append((Component)Component.text((String)"de mesas aliadas por perto.", (TextColor)NamedTextColor.DARK_GREEN))).build()});
        meta.addPages(new Component[]{((TextComponent.Builder)((TextComponent.Builder)((TextComponent.Builder)((TextComponent.Builder)((TextComponent.Builder)((TextComponent.Builder)Component.text().append((Component)Component.text((String)"Orb da Classe\n\n", (TextColor)NamedTextColor.DARK_PURPLE, (TextDecoration[])new TextDecoration[]{TextDecoration.BOLD}))).append((Component)Component.text((String)"Fabrica: ", (TextColor)NamedTextColor.BLACK))).append((Component)Component.text((String)"Pedra de Refinamento\n", (TextColor)NamedTextColor.DARK_GREEN, (TextDecoration[])new TextDecoration[]{TextDecoration.BOLD}))).append((Component)Component.text((String)"Usada para Tier 1 -> Tier 2.\n\n", (TextColor)NamedTextColor.DARK_GRAY))).append((Component)Component.text((String)"Tambem fabrica Pergaminho\n", (TextColor)NamedTextColor.BLACK))).append((Component)Component.text((String)"de Identificacao.", (TextColor)NamedTextColor.BLACK))).build()});
        meta.addPages(new Component[]{((TextComponent.Builder)((TextComponent.Builder)((TextComponent.Builder)((TextComponent.Builder)((TextComponent.Builder)((TextComponent.Builder)((TextComponent.Builder)((TextComponent.Builder)Component.text().append((Component)Component.text((String)"Funcoes da Mesa\n\n", (TextColor)NamedTextColor.YELLOW, (TextDecoration[])new TextDecoration[]{TextDecoration.BOLD}))).append((Component)Component.text((String)"1. Avaliar item (identificar)\n", (TextColor)NamedTextColor.BLACK))).append((Component)Component.text((String)"2. Fabricar Pergaminho\n", (TextColor)NamedTextColor.BLACK))).append((Component)Component.text((String)"3. Fabricar Pocao Retorno\n", (TextColor)NamedTextColor.BLACK))).append((Component)Component.text((String)"4. Aplicar Encantamento\n", (TextColor)NamedTextColor.BLACK))).append((Component)Component.text((String)"5. Fabricar Pedra Refino\n", (TextColor)NamedTextColor.BLACK))).append((Component)Component.text((String)"6. Rotas comerciais\n", (TextColor)NamedTextColor.BLACK))).append((Component)Component.text((String)"7. Fabricar Selo de Rota\n", (TextColor)NamedTextColor.BLACK))).build()});
        meta.addPages(new Component[]{((TextComponent.Builder)((TextComponent.Builder)((TextComponent.Builder)((TextComponent.Builder)((TextComponent.Builder)((TextComponent.Builder)((TextComponent.Builder)((TextComponent.Builder)((TextComponent.Builder)((TextComponent.Builder)((TextComponent.Builder)Component.text().append((Component)Component.text((String)"Pre-requisitos\n\n", (TextColor)NamedTextColor.AQUA, (TextDecoration[])new TextDecoration[]{TextDecoration.BOLD}))).append((Component)Component.text((String)"Cada funcao da mesa pode\n", (TextColor)NamedTextColor.BLACK))).append((Component)Component.text((String)"exigir uma mesa aliada\n", (TextColor)NamedTextColor.BLACK))).append((Component)Component.text((String)"num raio de 10 blocos:\n\n", (TextColor)NamedTextColor.BLACK))).append((Component)Component.text((String)"- Pocao Retorno: Mesa\n", (TextColor)NamedTextColor.DARK_GRAY))).append((Component)Component.text((String)"  de Transmutacao\n", (TextColor)NamedTextColor.DARK_GRAY))).append((Component)Component.text((String)"- Encantar: Bigorna\n", (TextColor)NamedTextColor.DARK_GRAY))).append((Component)Component.text((String)"- Pedra de Refinamento:\n", (TextColor)NamedTextColor.DARK_GRAY))).append((Component)Component.text((String)"  Bigorna\n\n", (TextColor)NamedTextColor.DARK_GRAY))).append((Component)Component.text((String)"Sem a mesa proxima, o\n", (TextColor)NamedTextColor.RED))).append((Component)Component.text((String)"botao fica bloqueado.", (TextColor)NamedTextColor.RED))).build()});
        meta.addPages(new Component[]{((TextComponent.Builder)((TextComponent.Builder)((TextComponent.Builder)((TextComponent.Builder)((TextComponent.Builder)((TextComponent.Builder)((TextComponent.Builder)((TextComponent.Builder)((TextComponent.Builder)Component.text().append((Component)Component.text((String)"Warps Publicas\n\n", (TextColor)NamedTextColor.GOLD, (TextDecoration[])new TextDecoration[]{TextDecoration.BOLD}))).append((Component)Component.text((String)"Pontos globais marcados\n", (TextColor)NamedTextColor.BLACK))).append((Component)Component.text((String)"com bloco BEACON. Apenas\n", (TextColor)NamedTextColor.BLACK))).append((Component)Component.text((String)"admin pode registrar.\n\n", (TextColor)NamedTextColor.BLACK))).append((Component)Component.text((String)"Custo: 40 mana base\n", (TextColor)NamedTextColor.DARK_AQUA))).append((Component)Component.text((String)"+ 5 mana por passageiro\n", (TextColor)NamedTextColor.DARK_AQUA))).append((Component)Component.text((String)"(max 4 passageiros).\n\n", (TextColor)NamedTextColor.DARK_AQUA))).append((Component)Component.text((String)"Cast: 3s parado.\n", (TextColor)NamedTextColor.DARK_GRAY))).append((Component)Component.text((String)"Use: /norb publicwarp tp", (TextColor)NamedTextColor.DARK_GRAY))).build()});
        meta.addPages(new Component[]{((TextComponent.Builder)((TextComponent.Builder)((TextComponent.Builder)((TextComponent.Builder)((TextComponent.Builder)((TextComponent.Builder)Component.text().append((Component)Component.text((String)"Dica de Operacao\n\n", (TextColor)NamedTextColor.DARK_GREEN, (TextDecoration[])new TextDecoration[]{TextDecoration.BOLD}))).append((Component)Component.text((String)"Mantenha estoque de papel,\n", (TextColor)NamedTextColor.BLACK))).append((Component)Component.text((String)"tinta, lapis, perola e ouro\n", (TextColor)NamedTextColor.BLACK))).append((Component)Component.text((String)"para nao parar a cadeia.\n\n", (TextColor)NamedTextColor.BLACK))).append((Component)Component.text((String)"Sem Mercador ativo, o\n", (TextColor)NamedTextColor.DARK_GREEN))).append((Component)Component.text((String)"servidor trava em escala.", (TextColor)NamedTextColor.DARK_GREEN))).build()});
        this.normalizeBookPages(meta);
        book.setItemMeta((ItemMeta)meta);
        return book;
    }

    private ItemStack createGuerreiroGuide() {
        ItemStack book = new ItemStack(Material.WRITTEN_BOOK);
        BookMeta meta = (BookMeta)book.getItemMeta();
        if (meta == null) {
            return null;
        }
        meta.setTitle("\u00a74Guia do Guerreiro");
        meta.setAuthor("NemonicRP");
        meta.displayName((Component)Component.text((String)"Codice do Guerreiro", (TextColor)NamedTextColor.DARK_RED, (TextDecoration[])new TextDecoration[]{TextDecoration.BOLD}));
        meta.addPages(new Component[]{((TextComponent.Builder)((TextComponent.Builder)((TextComponent.Builder)((TextComponent.Builder)((TextComponent.Builder)((TextComponent.Builder)Component.text().append((Component)Component.text((String)"Guia do Guerreiro\n", (TextColor)NamedTextColor.DARK_RED, (TextDecoration[])new TextDecoration[]{TextDecoration.BOLD}))).append((Component)Component.text((String)"Linha de frente do grupo.\n\n", (TextColor)NamedTextColor.DARK_GRAY))).append((Component)Component.text((String)"Mesa da classe: ", (TextColor)NamedTextColor.BLACK))).append((Component)Component.text((String)"Nao possui mesa propria.\n\n", (TextColor)NamedTextColor.RED, (TextDecoration[])new TextDecoration[]{TextDecoration.BOLD}))).append((Component)Component.text((String)"Seu sistema principal e\n", (TextColor)NamedTextColor.BLACK))).append((Component)Component.text((String)"drop de orbs no combate.", (TextColor)NamedTextColor.BLACK))).build()});
        meta.addPages(new Component[]{((TextComponent.Builder)((TextComponent.Builder)((TextComponent.Builder)((TextComponent.Builder)((TextComponent.Builder)((TextComponent.Builder)((TextComponent.Builder)((TextComponent.Builder)Component.text().append((Component)Component.text((String)"Orbs da Classe\n\n", (TextColor)NamedTextColor.DARK_GREEN, (TextDecoration[])new TextDecoration[]{TextDecoration.BOLD}))).append((Component)Component.text((String)"Ao matar monstros, pode\n", (TextColor)NamedTextColor.BLACK))).append((Component)Component.text((String)"dropar diretamente:\n", (TextColor)NamedTextColor.BLACK))).append((Component)Component.text((String)"- Runa de Poder\n", (TextColor)NamedTextColor.DARK_GREEN))).append((Component)Component.text((String)"- Runa Nobre\n", (TextColor)NamedTextColor.DARK_GREEN))).append((Component)Component.text((String)"- Moeda da Sorte\n\n", (TextColor)NamedTextColor.DARK_GREEN))).append((Component)Component.text((String)"As chances sao definidas\n", (TextColor)NamedTextColor.DARK_GRAY))).append((Component)Component.text((String)"na configuracao do servidor.", (TextColor)NamedTextColor.DARK_GRAY))).build()});
        meta.addPages(new Component[]{((TextComponent.Builder)((TextComponent.Builder)((TextComponent.Builder)((TextComponent.Builder)((TextComponent.Builder)((TextComponent.Builder)((TextComponent.Builder)((TextComponent.Builder)Component.text().append((Component)Component.text((String)"Funcoes da Classe\n\n", (TextColor)NamedTextColor.GOLD, (TextDecoration[])new TextDecoration[]{TextDecoration.BOLD}))).append((Component)Component.text((String)"1. Iniciar confrontos\n", (TextColor)NamedTextColor.BLACK))).append((Component)Component.text((String)"2. Segurar a linha de frente\n", (TextColor)NamedTextColor.BLACK))).append((Component)Component.text((String)"3. Abrir janela de dano\n", (TextColor)NamedTextColor.BLACK))).append((Component)Component.text((String)"4. Garantir farm de elites\n", (TextColor)NamedTextColor.BLACK))).append((Component)Component.text((String)"   para orb drop\n\n", (TextColor)NamedTextColor.BLACK))).append((Component)Component.text((String)"Quanto mais ativo no pve,\n", (TextColor)NamedTextColor.DARK_AQUA))).append((Component)Component.text((String)"mais fluxo de orbs raras.", (TextColor)NamedTextColor.DARK_AQUA))).build()});
        meta.addPages(new Component[]{((TextComponent.Builder)((TextComponent.Builder)((TextComponent.Builder)((TextComponent.Builder)((TextComponent.Builder)((TextComponent.Builder)Component.text().append((Component)Component.text((String)"Sinergia\n\n", (TextColor)NamedTextColor.DARK_PURPLE, (TextDecoration[])new TextDecoration[]{TextDecoration.BOLD}))).append((Component)Component.text((String)"Ferreiro equipa.\n", (TextColor)NamedTextColor.BLACK))).append((Component)Component.text((String)"Alquimista abastece.\n", (TextColor)NamedTextColor.BLACK))).append((Component)Component.text((String)"Mercador gira economia.\n\n", (TextColor)NamedTextColor.BLACK))).append((Component)Component.text((String)"Guerreiro abre o caminho\n", (TextColor)NamedTextColor.DARK_GREEN))).append((Component)Component.text((String)"para todos progredirem.", (TextColor)NamedTextColor.DARK_GREEN))).build()});
        meta.addPages(new Component[]{((TextComponent.Builder)((TextComponent.Builder)((TextComponent.Builder)((TextComponent.Builder)((TextComponent.Builder)((TextComponent.Builder)((TextComponent.Builder)Component.text().append((Component)Component.text((String)"Evolucao de Jogador\n\n", (TextColor)NamedTextColor.RED, (TextDecoration[])new TextDecoration[]{TextDecoration.BOLD}))).append((Component)Component.text((String)"Treine tres pontos:\n", (TextColor)NamedTextColor.BLACK))).append((Component)Component.text((String)"- Posicionamento\n", (TextColor)NamedTextColor.BLACK))).append((Component)Component.text((String)"- Tempo de habilidade\n", (TextColor)NamedTextColor.BLACK))).append((Component)Component.text((String)"- Leitura da luta\n\n", (TextColor)NamedTextColor.BLACK))).append((Component)Component.text((String)"Quanto melhor sua leitura,\n", (TextColor)NamedTextColor.RED))).append((Component)Component.text((String)"mais seguro o progresso.", (TextColor)NamedTextColor.RED))).build()});
        this.normalizeBookPages(meta);
        book.setItemMeta((ItemMeta)meta);
        return book;
    }

    public ItemStack createWelcomeBook() {
        ItemStack book = new ItemStack(Material.WRITTEN_BOOK);
        BookMeta meta = (BookMeta)book.getItemMeta();
        if (meta == null) {
            return null;
        }
        meta.setTitle("\u00a7bGuia de Embati");
        meta.setAuthor("Embati");
        meta.displayName((Component)Component.text((String)"Cronicas de Embati", (TextColor)NamedTextColor.AQUA, (TextDecoration[])new TextDecoration[]{TextDecoration.BOLD}));
        meta.addPages(new Component[]{((TextComponent.Builder)((TextComponent.Builder)((TextComponent.Builder)((TextComponent.Builder)((TextComponent.Builder)((TextComponent.Builder)((TextComponent.Builder)((TextComponent.Builder)((TextComponent.Builder)((TextComponent.Builder)((TextComponent.Builder)Component.text().append((Component)Component.text((String)"Bem-vindo(a) a Embati\n\n", (TextColor)NamedTextColor.GOLD, (TextDecoration[])new TextDecoration[]{TextDecoration.BOLD}))).append((Component)Component.text((String)"Voce parece meio perdido\n", (TextColor)NamedTextColor.BLACK))).append((Component)Component.text((String)"ne, fica tranquilo\n", (TextColor)NamedTextColor.BLACK))).append((Component)Component.text((String)"vou te explicar como as ", (TextColor)NamedTextColor.BLACK))).append((Component)Component.text((String)"*coisas*", (TextColor)NamedTextColor.DARK_GREEN, (TextDecoration[])new TextDecoration[]{TextDecoration.ITALIC}))).append((Component)Component.text((String)"funcionam aqui\n\n", (TextColor)NamedTextColor.BLACK))).append((Component)Component.text((String)"Neste mundo, ninguem\n", (TextColor)NamedTextColor.BLACK))).append((Component)Component.text((String)"sobrevive sozinho.\n", (TextColor)NamedTextColor.BLACK))).append((Component)Component.text((String)"Escolha um caminho e\n", (TextColor)NamedTextColor.BLACK))).append((Component)Component.text((String)"encontre amigos.\n\n", (TextColor)NamedTextColor.BLACK))).append((Component)Component.text((String)"Vire a pagina...", (TextColor)NamedTextColor.DARK_GREEN))).build()});
        meta.addPages(new Component[]{((TextComponent.Builder)((TextComponent.Builder)((TextComponent.Builder)((TextComponent.Builder)((TextComponent.Builder)((TextComponent.Builder)((TextComponent.Builder)((TextComponent.Builder)((TextComponent.Builder)((TextComponent.Builder)((TextComponent.Builder)Component.text().append((Component)Component.text((String)"Mestres de Embati\n\n", (TextColor)NamedTextColor.DARK_PURPLE, (TextDecoration[])new TextDecoration[]{TextDecoration.BOLD}))).append((Component)Component.text((String)"Alquimista: ", (TextColor)NamedTextColor.BLACK))).append((Component)Component.text((String)"Karmila\n", (TextColor)NamedTextColor.LIGHT_PURPLE, (TextDecoration[])new TextDecoration[]{TextDecoration.BOLD}))).append((Component)Component.text((String)"Ferreiro: ", (TextColor)NamedTextColor.BLACK))).append((Component)Component.text((String)"Doran\n", (TextColor)NamedTextColor.RED, (TextDecoration[])new TextDecoration[]{TextDecoration.BOLD}))).append((Component)Component.text((String)"Mercador: ", (TextColor)NamedTextColor.BLACK))).append((Component)Component.text((String)"Lyra\n", (TextColor)NamedTextColor.GOLD, (TextDecoration[])new TextDecoration[]{TextDecoration.BOLD}))).append((Component)Component.text((String)"Guerreiro: ", (TextColor)NamedTextColor.BLACK))).append((Component)Component.text((String)"Gareth\n\n", (TextColor)NamedTextColor.DARK_RED, (TextDecoration[])new TextDecoration[]{TextDecoration.BOLD}))).append((Component)Component.text((String)"Cada mestre governa uma\n", (TextColor)NamedTextColor.DARK_GRAY))).append((Component)Component.text((String)"parte do destino de Embati.", (TextColor)NamedTextColor.DARK_GRAY))).build()});
        meta.addPages(new Component[]{((TextComponent.Builder)((TextComponent.Builder)((TextComponent.Builder)((TextComponent.Builder)((TextComponent.Builder)((TextComponent.Builder)((TextComponent.Builder)((TextComponent.Builder)((TextComponent.Builder)((TextComponent.Builder)((TextComponent.Builder)((TextComponent.Builder)((TextComponent.Builder)Component.text().append((Component)Component.text((String)"Os Mestres\n\n", (TextColor)NamedTextColor.GOLD, (TextDecoration[])new TextDecoration[]{TextDecoration.BOLD}))).append((Component)Component.text((String)"Karmila ", (TextColor)NamedTextColor.LIGHT_PURPLE, (TextDecoration[])new TextDecoration[]{TextDecoration.BOLD}))).append((Component)Component.text((String)"aprendeu a arrancar\n", (TextColor)NamedTextColor.BLACK))).append((Component)Component.text((String)"forca de itens esquecidos.\n", (TextColor)NamedTextColor.BLACK))).append((Component)Component.text((String)"Doran ", (TextColor)NamedTextColor.RED, (TextDecoration[])new TextDecoration[]{TextDecoration.BOLD}))).append((Component)Component.text((String)"ergueu as forjas apos\n", (TextColor)NamedTextColor.BLACK))).append((Component)Component.text((String)"a queda da muralha.\n\n", (TextColor)NamedTextColor.BLACK))).append((Component)Component.text((String)"Lyra ", (TextColor)NamedTextColor.GOLD, (TextDecoration[])new TextDecoration[]{TextDecoration.BOLD}))).append((Component)Component.text((String)"manteve as rotas vivas,\n", (TextColor)NamedTextColor.BLACK))).append((Component)Component.text((String)"e ", (TextColor)NamedTextColor.BLACK))).append((Component)Component.text((String)"Gareth ", (TextColor)NamedTextColor.DARK_RED, (TextDecoration[])new TextDecoration[]{TextDecoration.BOLD}))).append((Component)Component.text((String)"formou a linha que\n", (TextColor)NamedTextColor.BLACK))).append((Component)Component.text((String)"defende Embati ate hoje.", (TextColor)NamedTextColor.BLACK))).build()});
        meta.addPages(new Component[]{((TextComponent.Builder)((TextComponent.Builder)((TextComponent.Builder)((TextComponent.Builder)((TextComponent.Builder)((TextComponent.Builder)((TextComponent.Builder)Component.text().append((Component)Component.text((String)"Tiers de Itens\n\n", (TextColor)NamedTextColor.DARK_AQUA, (TextDecoration[])new TextDecoration[]{TextDecoration.BOLD}))).append((Component)Component.text((String)"Tier 0 Comum\n", (TextColor)NamedTextColor.GRAY, (TextDecoration[])new TextDecoration[]{TextDecoration.BOLD}))).append((Component)Component.text((String)"Sem modificadores.\n\n", (TextColor)NamedTextColor.DARK_GRAY))).append((Component)Component.text((String)"Tier 1 Magico\n", (TextColor)NamedTextColor.BLUE, (TextDecoration[])new TextDecoration[]{TextDecoration.BOLD}))).append((Component)Component.text((String)"Ate 2 modificadores.\n\n", (TextColor)NamedTextColor.DARK_GRAY))).append((Component)Component.text((String)"Tier 2 Raro\n", (TextColor)NamedTextColor.DARK_PURPLE, (TextDecoration[])new TextDecoration[]{TextDecoration.BOLD}))).append((Component)Component.text((String)"Ate 6 modificadores.", (TextColor)NamedTextColor.DARK_GRAY))).build()});
        meta.addPages(new Component[]{((TextComponent.Builder)((TextComponent.Builder)((TextComponent.Builder)((TextComponent.Builder)((TextComponent.Builder)((TextComponent.Builder)Component.text().append((Component)Component.text((String)"Tier 3 Unico\n\n", (TextColor)NamedTextColor.GOLD, (TextDecoration[])new TextDecoration[]{TextDecoration.BOLD}))).append((Component)Component.text((String)"Itens mais poderosos.\n", (TextColor)NamedTextColor.DARK_GRAY))).append((Component)Component.text((String)"Itens dropam em Tier 0\n", (TextColor)NamedTextColor.BLACK))).append((Component)Component.text((String)"e sobem com orbs.\n\n", (TextColor)NamedTextColor.BLACK))).append((Component)Component.text((String)"Antes de tudo, o item\n", (TextColor)NamedTextColor.DARK_RED))).append((Component)Component.text((String)"precisa ser identificado!", (TextColor)NamedTextColor.DARK_RED, (TextDecoration[])new TextDecoration[]{TextDecoration.BOLD}))).build()});
        meta.addPages(new Component[]{((TextComponent.Builder)((TextComponent.Builder)((TextComponent.Builder)((TextComponent.Builder)((TextComponent.Builder)((TextComponent.Builder)((TextComponent.Builder)((TextComponent.Builder)((TextComponent.Builder)((TextComponent.Builder)((TextComponent.Builder)Component.text().append((Component)Component.text((String)"Prefixos e Sufixos\n\n", (TextColor)NamedTextColor.DARK_GREEN, (TextDecoration[])new TextDecoration[]{TextDecoration.BOLD}))).append((Component)Component.text((String)"Essas terras sempre foram\n", (TextColor)NamedTextColor.BLACK))).append((Component)Component.text((String)"estranhas, tudo o que e\n", (TextColor)NamedTextColor.BLACK))).append((Component)Component.text((String)"feito aqui vem com ", (TextColor)NamedTextColor.BLACK))).append((Component)Component.text((String)"*coisinhas*", (TextColor)NamedTextColor.DARK_GREEN, (TextDecoration[])new TextDecoration[]{TextDecoration.ITALIC}))).append((Component)Component.text((String)" a mais...\n\n", (TextColor)NamedTextColor.BLACK))).append((Component)Component.text((String)"Na fabricacao da arma/item,\n", (TextColor)NamedTextColor.DARK_GRAY))).append((Component)Component.text((String)"Prefixos e Sufixos podem\n", (TextColor)NamedTextColor.DARK_GRAY))).append((Component)Component.text((String)"vir junto no resultado.\n\n", (TextColor)NamedTextColor.DARK_GRAY))).append((Component)Component.text((String)"Prefixos: encantamentos\n", (TextColor)NamedTextColor.DARK_AQUA))).append((Component)Component.text((String)"Sufixos: atributos", (TextColor)NamedTextColor.DARK_AQUA))).build()});
        meta.addPages(new Component[]{((TextComponent.Builder)((TextComponent.Builder)((TextComponent.Builder)((TextComponent.Builder)((TextComponent.Builder)((TextComponent.Builder)Component.text().append((Component)Component.text((String)"Modificadores\n\n", (TextColor)NamedTextColor.DARK_GREEN, (TextDecoration[])new TextDecoration[]{TextDecoration.BOLD}))).append((Component)Component.text((String)"Modificadores sao os\n", (TextColor)NamedTextColor.BLACK))).append((Component)Component.text((String)"bonus do item, em forma\n", (TextColor)NamedTextColor.BLACK))).append((Component)Component.text((String)"de Prefixos e Sufixos.\n\n", (TextColor)NamedTextColor.BLACK))).append((Component)Component.text((String)"Mais tier = mais espaco\n", (TextColor)NamedTextColor.DARK_GREEN))).append((Component)Component.text((String)"para modificadores.", (TextColor)NamedTextColor.DARK_GREEN))).build()});
        meta.addPages(new Component[]{((TextComponent.Builder)((TextComponent.Builder)((TextComponent.Builder)((TextComponent.Builder)((TextComponent.Builder)((TextComponent.Builder)((TextComponent.Builder)((TextComponent.Builder)((TextComponent.Builder)((TextComponent.Builder)((TextComponent.Builder)((TextComponent.Builder)((TextComponent.Builder)((TextComponent.Builder)Component.text().append((Component)Component.text((String)"Sistema de Orbs\n\n", (TextColor)NamedTextColor.GOLD, (TextDecoration[])new TextDecoration[]{TextDecoration.BOLD}))).append((Component)Component.text((String)"Pergaminho\n", (TextColor)NamedTextColor.DARK_GREEN, (TextDecoration[])new TextDecoration[]{TextDecoration.BOLD}))).append((Component)Component.text((String)"Identifica o item.\n\n", (TextColor)NamedTextColor.DARK_GRAY))).append((Component)Component.text((String)"Oleo de Polimento\n", (TextColor)NamedTextColor.DARK_GREEN, (TextDecoration[])new TextDecoration[]{TextDecoration.BOLD}))).append((Component)Component.text((String)"Melhora valores atuais.\n", (TextColor)NamedTextColor.DARK_GRAY))).append((Component)Component.text((String)"Fabricado na Mesa do\n", (TextColor)NamedTextColor.DARK_GRAY))).append((Component)Component.text((String)"Ferreiro com 20 pepitas\n", (TextColor)NamedTextColor.DARK_GRAY))).append((Component)Component.text((String)"de ouro.\n\n", (TextColor)NamedTextColor.DARK_GRAY))).append((Component)Component.text((String)"Pedra de Encantamento\n", (TextColor)NamedTextColor.DARK_GREEN, (TextDecoration[])new TextDecoration[]{TextDecoration.BOLD}))).append((Component)Component.text((String)"Comum -> Magico\n", (TextColor)NamedTextColor.DARK_GRAY))).append((Component)Component.text((String)"(adiciona 1-2 mods).\n\n", (TextColor)NamedTextColor.DARK_GRAY))).append((Component)Component.text((String)"Pedra de Reforco\n", (TextColor)NamedTextColor.DARK_GREEN, (TextDecoration[])new TextDecoration[]{TextDecoration.BOLD}))).append((Component)Component.text((String)"Adiciona 1 mod em item\n", (TextColor)NamedTextColor.DARK_GRAY))).append((Component)Component.text((String)"Magico com 1 slot livre.", (TextColor)NamedTextColor.DARK_GRAY))).build()});
        meta.addPages(new Component[]{((TextComponent.Builder)((TextComponent.Builder)((TextComponent.Builder)((TextComponent.Builder)((TextComponent.Builder)((TextComponent.Builder)((TextComponent.Builder)((TextComponent.Builder)((TextComponent.Builder)((TextComponent.Builder)((TextComponent.Builder)((TextComponent.Builder)((TextComponent.Builder)((TextComponent.Builder)((TextComponent.Builder)((TextComponent.Builder)((TextComponent.Builder)((TextComponent.Builder)Component.text().append((Component)Component.text((String)"Mais Orbs\n\n", (TextColor)NamedTextColor.GOLD, (TextDecoration[])new TextDecoration[]{TextDecoration.BOLD}))).append((Component)Component.text((String)"Pedra de Refinamento\n", (TextColor)NamedTextColor.DARK_GREEN, (TextDecoration[])new TextDecoration[]{TextDecoration.BOLD}))).append((Component)Component.text((String)"Comum -> Raro direto\n", (TextColor)NamedTextColor.DARK_GRAY))).append((Component)Component.text((String)"(3-6 mods aleatorios).\n\n", (TextColor)NamedTextColor.DARK_GRAY))).append((Component)Component.text((String)"Runa de Poder\n", (TextColor)NamedTextColor.DARK_GREEN, (TextDecoration[])new TextDecoration[]{TextDecoration.BOLD}))).append((Component)Component.text((String)"Adiciona 1 mod aleatorio\n", (TextColor)NamedTextColor.DARK_GRAY))).append((Component)Component.text((String)"em item Raro com slot\n", (TextColor)NamedTextColor.DARK_GRAY))).append((Component)Component.text((String)"livre.\n\n", (TextColor)NamedTextColor.DARK_GRAY))).append((Component)Component.text((String)"Runa Nobre\n", (TextColor)NamedTextColor.DARK_GREEN, (TextDecoration[])new TextDecoration[]{TextDecoration.BOLD}))).append((Component)Component.text((String)"Magico -> Raro\n", (TextColor)NamedTextColor.DARK_GRAY))).append((Component)Component.text((String)"(adiciona 1 mod extra).\n\n", (TextColor)NamedTextColor.DARK_GRAY))).append((Component)Component.text((String)"Moeda da Sorte\n", (TextColor)NamedTextColor.DARK_GREEN, (TextDecoration[])new TextDecoration[]{TextDecoration.BOLD}))).append((Component)Component.text((String)"Tier aleatorio:\n", (TextColor)NamedTextColor.DARK_GRAY))).append((Component)Component.text((String)"75% Magico / 23% Raro /\n", (TextColor)NamedTextColor.DARK_GRAY))).append((Component)Component.text((String)"2% Unico.\n\n", (TextColor)NamedTextColor.DARK_GRAY))).append((Component)Component.text((String)"Pedra Corrosiva\n", (TextColor)NamedTextColor.DARK_RED, (TextDecoration[])new TextDecoration[]{TextDecoration.BOLD}))).append((Component)Component.text((String)"Remove 1 mod aleatorio\n", (TextColor)NamedTextColor.DARK_GRAY))).append((Component)Component.text((String)"(pode ser o melhor).", (TextColor)NamedTextColor.DARK_GRAY))).build()});
        meta.addPages(new Component[]{((TextComponent.Builder)((TextComponent.Builder)((TextComponent.Builder)((TextComponent.Builder)((TextComponent.Builder)((TextComponent.Builder)Component.text().append((Component)Component.text((String)"Como Usar Orbs\n\n", (TextColor)NamedTextColor.DARK_AQUA, (TextDecoration[])new TextDecoration[]{TextDecoration.BOLD}))).append((Component)Component.text((String)"1. Orb na mao principal\n", (TextColor)NamedTextColor.BLACK))).append((Component)Component.text((String)"2. Item alvo na secundaria\n", (TextColor)NamedTextColor.BLACK))).append((Component)Component.text((String)"3. Clique direito\n\n", (TextColor)NamedTextColor.BLACK))).append((Component)Component.text((String)"Item precisa estar\n", (TextColor)NamedTextColor.DARK_RED))).append((Component)Component.text((String)"identificado antes!", (TextColor)NamedTextColor.DARK_RED, (TextDecoration[])new TextDecoration[]{TextDecoration.BOLD}))).build()});
        meta.addPages(new Component[]{((TextComponent.Builder)((TextComponent.Builder)((TextComponent.Builder)((TextComponent.Builder)((TextComponent.Builder)((TextComponent.Builder)((TextComponent.Builder)((TextComponent.Builder)Component.text().append((Component)Component.text((String)"Warps Publicas\n\n", (TextColor)NamedTextColor.GOLD, (TextDecoration[])new TextDecoration[]{TextDecoration.BOLD}))).append((Component)Component.text((String)"Pontos globais marcados\n", (TextColor)NamedTextColor.BLACK))).append((Component)Component.text((String)"com BEACON. Apenas admin\n", (TextColor)NamedTextColor.BLACK))).append((Component)Component.text((String)"pode registrar.\n\n", (TextColor)NamedTextColor.BLACK))).append((Component)Component.text((String)"/norb publicwarp tp\n", (TextColor)NamedTextColor.DARK_AQUA))).append((Component)Component.text((String)"Custo: 40 mana base.\n", (TextColor)NamedTextColor.DARK_AQUA))).append((Component)Component.text((String)"+5 mana por passageiro\n", (TextColor)NamedTextColor.DARK_AQUA))).append((Component)Component.text((String)"(max 4).", (TextColor)NamedTextColor.DARK_AQUA))).build()});
        meta.addPages(new Component[]{((TextComponent.Builder)((TextComponent.Builder)((TextComponent.Builder)((TextComponent.Builder)((TextComponent.Builder)Component.text().append((Component)Component.text((String)"Sua Jornada Comeca\n\n", (TextColor)NamedTextColor.GOLD, (TextDecoration[])new TextDecoration[]{TextDecoration.BOLD}))).append((Component)Component.text((String)"Converse com os mestres\n", (TextColor)NamedTextColor.DARK_GRAY))).append((Component)Component.text((String)"e forme seu grupo.\n\n", (TextColor)NamedTextColor.DARK_GRAY))).append((Component)Component.text((String)"Boa sorte, aventureiro.\n", (TextColor)NamedTextColor.DARK_GREEN, (TextDecoration[])new TextDecoration[]{TextDecoration.ITALIC}))).append((Component)Component.text((String)"Embati precisa de voce.", (TextColor)NamedTextColor.DARK_GREEN, (TextDecoration[])new TextDecoration[]{TextDecoration.ITALIC}))).build()});
        this.normalizeBookPages(meta);
        book.setItemMeta((ItemMeta)meta);
        return book;
    }

    private void normalizeBookPages(BookMeta meta) {
        ArrayList originalPages = new ArrayList(meta.pages());
        ArrayList<Component> normalizedPages = new ArrayList<Component>();
        for (Component page : originalPages) {
            String legacy = LegacyComponentSerializer.legacySection().serialize(page).replace("\r", "");
            List<String> wrappedLines = this.wrapForBook(legacy);
            normalizedPages.addAll(this.splitLinesIntoPages(wrappedLines));
        }
        if (!normalizedPages.isEmpty()) {
            if (normalizedPages.size() > 50) {
                normalizedPages = new ArrayList(normalizedPages.subList(0, 50));
            }
            meta.pages(normalizedPages);
        }
    }

    private List<String> wrapForBook(String text) {
        ArrayList<String> out = new ArrayList<String>();
        String[] rawLines = text.split("\n", -1);
        String carryStyle = "";
        for (String raw : rawLines) {
            if (raw.isBlank()) {
                out.add("");
                continue;
            }
            String[] words = raw.trim().split("\\s+");
            StringBuilder line = new StringBuilder(carryStyle);
            int visibleLen = this.visibleLength(carryStyle);
            for (String word : words) {
                int wordVisible = this.visibleLength(word);
                if (wordVisible > 20) {
                    if (!line.isEmpty()) {
                        out.add(line.toString());
                        carryStyle = this.activeLegacyCodes(line.toString());
                        line.setLength(0);
                        line.append(carryStyle);
                        visibleLen = this.visibleLength(carryStyle);
                    }
                    List<String> splitWord = this.splitLegacyWord(word, 20, carryStyle);
                    for (int i = 0; i < splitWord.size(); ++i) {
                        String part = splitWord.get(i);
                        if (i == splitWord.size() - 1) {
                            line.append(part);
                            visibleLen = this.visibleLength(line.toString());
                            continue;
                        }
                        out.add(part);
                    }
                    carryStyle = this.activeLegacyCodes(line.toString());
                    continue;
                }
                if (visibleLen == 0) {
                    line.append(word);
                    visibleLen += wordVisible;
                    continue;
                }
                if (visibleLen + 1 + wordVisible <= 20) {
                    line.append(' ').append(word);
                    visibleLen += 1 + wordVisible;
                    continue;
                }
                out.add(line.toString());
                carryStyle = this.activeLegacyCodes(line.toString());
                line.setLength(0);
                line.append(carryStyle).append(word);
                visibleLen = this.visibleLength(carryStyle) + wordVisible;
            }
            if (this.visibleLength(line.toString()) <= 0) continue;
            out.add(line.toString());
            carryStyle = this.activeLegacyCodes(line.toString());
        }
        return out;
    }

    private List<Component> splitLinesIntoPages(List<String> lines) {
        ArrayList<Component> pages = new ArrayList<Component>();
        ArrayList<String> pageLines = new ArrayList<String>();
        int pageChars = 0;
        for (String line : lines) {
            boolean exceedsChars;
            int addedChars = this.visibleLength(line) + (pageLines.isEmpty() ? 0 : 1);
            boolean exceedsLines = pageLines.size() >= 13;
            boolean bl = exceedsChars = pageChars + addedChars > 256;
            if ((exceedsLines || exceedsChars) && !pageLines.isEmpty()) {
                pages.add((Component)LegacyComponentSerializer.legacySection().deserialize(String.join((CharSequence)"\n", pageLines)));
                pageLines.clear();
                pageChars = 0;
                addedChars = this.visibleLength(line);
            }
            pageLines.add(line);
            pageChars += addedChars;
        }
        if (!pageLines.isEmpty()) {
            pages.add((Component)LegacyComponentSerializer.legacySection().deserialize(String.join((CharSequence)"\n", pageLines)));
        }
        return pages;
    }

    private int visibleLength(String legacyText) {
        int visible = 0;
        for (int i = 0; i < legacyText.length(); ++i) {
            char c = legacyText.charAt(i);
            if (c == '\u00a7' && i + 1 < legacyText.length()) {
                ++i;
                continue;
            }
            ++visible;
        }
        return visible;
    }

    private List<String> splitLegacyWord(String word, int maxVisible, String carryStyle) {
        ArrayList<String> parts = new ArrayList<String>();
        StringBuilder part = new StringBuilder(carryStyle);
        int visible = this.visibleLength(carryStyle);
        for (int i = 0; i < word.length(); ++i) {
            char c = word.charAt(i);
            if (c == '\u00a7' && i + 1 < word.length()) {
                part.append(c).append(word.charAt(i + 1));
                ++i;
                continue;
            }
            if (visible >= maxVisible) {
                parts.add(part.toString());
                String nextStyle = this.activeLegacyCodes(part.toString());
                part.setLength(0);
                part.append(nextStyle);
                visible = this.visibleLength(nextStyle);
            }
            part.append(c);
            ++visible;
        }
        if (this.visibleLength(part.toString()) > 0) {
            parts.add(part.toString());
        }
        return parts;
    }

    private String activeLegacyCodes(String text) {
        char color = '\u0000';
        StringBuilder formats = new StringBuilder();
        for (int i = 0; i < text.length() - 1; ++i) {
            String token;
            char c = text.charAt(i);
            if (c != '\u00a7') continue;
            char code = Character.toLowerCase(text.charAt(i + 1));
            ++i;
            if (code == 'r') {
                color = '\u0000';
                formats.setLength(0);
                continue;
            }
            if (code >= '0' && code <= '9' || code >= 'a' && code <= 'f') {
                color = code;
                formats.setLength(0);
                continue;
            }
            if ("klmno".indexOf(code) < 0 || formats.indexOf(token = "\u00a7" + code) >= 0) continue;
            formats.append(token);
        }
        StringBuilder result = new StringBuilder();
        if (color != '\u0000') {
            result.append('\u00a7').append(color);
        }
        result.append((CharSequence)formats);
        return result.toString();
    }
}

