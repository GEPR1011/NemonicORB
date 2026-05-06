package com.nemonicorp.orbs;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Sistema de guias em livro para mesas e onboarding.
 */
public class MesaGuideManager {
    // Limites alinhados ao livro padrao do Minecraft.
    private static final int BOOK_MAX_PAGES = 50;
    private static final int BOOK_MAX_LINES_PER_PAGE = 13;
    private static final int BOOK_MAX_CHARS_PER_LINE = 20;
    private static final int BOOK_MAX_CHARS_PER_PAGE = 256;

    // Jogadores que ja viram o guia por mesa: "UUID:tipo"
    private final Set<String> seenGuides = new HashSet<>();

    /**
     * Mostra o guia na primeira vez por tipo de mesa.
     *
     * @return true se abriu guia (mesa nao deve abrir ainda), false se ja viu.
     */
    public boolean showGuideIfNeeded(Player player, String mesaType) {
        String key = player.getUniqueId() + ":" + mesaType;
        if (seenGuides.contains(key)) return false;

        seenGuides.add(key);
        ItemStack book = createGuideBook(mesaType);
        if (book == null) return false;

        player.openBook(book);
        return true;
    }

    private ItemStack createGuideBook(String mesaType) {
        return switch (mesaType) {
            case "alquimista" -> createAlquimistaGuide();
            case "ferreiro" -> createFerreiroGuide();
            case "mercador" -> createMercadorGuide();
            default -> null;
        };
    }

    public ItemStack createClassBook(String classKey) {
        if (classKey == null) return null;
        return switch (classKey.toLowerCase(Locale.ROOT)) {
            case "alquimista" -> createAlquimistaGuide();
            case "ferreiro" -> createFerreiroGuide();
            case "mercador" -> createMercadorGuide();
            case "guerreiro" -> createGuerreiroGuide();
            default -> null;
        };
    }

    private ItemStack createAlquimistaGuide() {
        ItemStack book = new ItemStack(Material.WRITTEN_BOOK);
        BookMeta meta = (BookMeta) book.getItemMeta();
        if (meta == null) return null;

        meta.setTitle("\u00A7dGuia da Mesa de Transmutacao");
        meta.setAuthor("NemonicRP");
        meta.displayName(Component.text("Grimorio do Alquimista", NamedTextColor.LIGHT_PURPLE, TextDecoration.BOLD));

        meta.addPages(Component.text()
                .append(Component.text("Mesa de Transmutacao\n", NamedTextColor.DARK_PURPLE, TextDecoration.BOLD))
                .append(Component.text("Guia do Alquimista\n\n", NamedTextColor.DARK_GRAY))
                .append(Component.text("Mesa da classe: ", NamedTextColor.BLACK))
                .append(Component.text("Mesa de Encantamento\n\n", NamedTextColor.LIGHT_PURPLE, TextDecoration.BOLD))
                .append(Component.text("Nivel 5+: fabrica orbs.\n", NamedTextColor.BLACK))
                .append(Component.text("Nivel 35+: transmutacao de itens.\n\n", NamedTextColor.BLACK))
                .append(Component.text("Especialidade: converter\n", NamedTextColor.DARK_AQUA))
                .append(Component.text("materiais e itens em orbs.", NamedTextColor.DARK_AQUA))
                .build());

        meta.addPages(Component.text()
                .append(Component.text("Orbs da Classe\n\n", NamedTextColor.GOLD, TextDecoration.BOLD))
                .append(Component.text("Fabrica direto na mesa:\n", NamedTextColor.BLACK))
                .append(Component.text("- Pedra de Encantamento\n", NamedTextColor.DARK_GREEN))
                .append(Component.text("- Pedra de Reforco\n\n", NamedTextColor.DARK_GREEN))
                .append(Component.text("Transmutacao (Nv 35+) gera:\n", NamedTextColor.BLACK))
                .append(Component.text("Encantamento, Reforco,\n", NamedTextColor.DARK_GRAY))
                .append(Component.text("Pedra Corrosiva ou Moeda\n", NamedTextColor.DARK_GRAY))
                .append(Component.text("da Sorte.", NamedTextColor.DARK_GRAY))
                .build());

        meta.addPages(Component.text()
                .append(Component.text("Funcoes da Mesa\n\n", NamedTextColor.DARK_PURPLE, TextDecoration.BOLD))
                .append(Component.text("1. Craft de orb por botao\n", NamedTextColor.BLACK))
                .append(Component.text("2. Transmutar equipamento\n", NamedTextColor.BLACK))
                .append(Component.text("3. Escalar resultado por\n", NamedTextColor.BLACK))
                .append(Component.text("   eficiencia ambiental\n\n", NamedTextColor.BLACK))
                .append(Component.text("A eficiencia aumenta a\n", NamedTextColor.DARK_GREEN))
                .append(Component.text("quantidade final de orbs.", NamedTextColor.DARK_GREEN))
                .build());

        meta.addPages(Component.text()
                .append(Component.text("Receitas Base\n\n", NamedTextColor.RED, TextDecoration.BOLD))
                .append(Component.text("Pedra de Encantamento:\n", NamedTextColor.BLACK))
                .append(Component.text("32 Carne Podre OU 32 Osso\n", NamedTextColor.DARK_GRAY))
                .append(Component.text("OU 10 Perola do End.\n\n", NamedTextColor.DARK_GRAY))
                .append(Component.text("Pedra de Reforco:\n", NamedTextColor.BLACK))
                .append(Component.text("8 Ovo Tartaruga OU 32\n", NamedTextColor.DARK_GRAY))
                .append(Component.text("Couro OU 32 La.", NamedTextColor.DARK_GRAY))
                .build());

        meta.addPages(Component.text()
                .append(Component.text("Dica de Progresso\n\n", NamedTextColor.DARK_GREEN, TextDecoration.BOLD))
                .append(Component.text("Use o craft de orbs para\n", NamedTextColor.BLACK))
                .append(Component.text("subir economia cedo e\n", NamedTextColor.BLACK))
                .append(Component.text("deixe transmutacao para\n", NamedTextColor.BLACK))
                .append(Component.text("itens realmente valiosos.\n\n", NamedTextColor.BLACK))
                .append(Component.text("Alquimista bom abastece\n", NamedTextColor.DARK_GREEN))
                .append(Component.text("todo o servidor.", NamedTextColor.DARK_GREEN))
                .build());

        normalizeBookPages(meta);
        book.setItemMeta(meta);
        return book;
    }

    private ItemStack createFerreiroGuide() {
        ItemStack book = new ItemStack(Material.WRITTEN_BOOK);
        BookMeta meta = (BookMeta) book.getItemMeta();
        if (meta == null) return null;

        meta.setTitle("\u00A7cGuia da Mesa do Ferreiro");
        meta.setAuthor("NemonicRP");
        meta.displayName(Component.text("Manual da Forja", NamedTextColor.RED, TextDecoration.BOLD));

        meta.addPages(Component.text()
                .append(Component.text("Mesa do Ferreiro\n", NamedTextColor.DARK_RED, TextDecoration.BOLD))
                .append(Component.text("Guia do Ferreiro\n\n", NamedTextColor.DARK_GRAY))
                .append(Component.text("Mesa da classe: ", NamedTextColor.BLACK))
                .append(Component.text("Bigorna\n\n", NamedTextColor.RED, TextDecoration.BOLD))
                .append(Component.text("Nivel 5+: acesso a forja,\n", NamedTextColor.BLACK))
                .append(Component.text("reparo e fabricacao de orb.\n\n", NamedTextColor.BLACK))
                .append(Component.text("A mesa trabalha com etapas\n", NamedTextColor.DARK_AQUA))
                .append(Component.text("de campo (multi-bloco).", NamedTextColor.DARK_AQUA))
                .build());

        meta.addPages(Component.text()
                .append(Component.text("Orb da Classe\n\n", NamedTextColor.GOLD, TextDecoration.BOLD))
                .append(Component.text("Fabrica: ", NamedTextColor.BLACK))
                .append(Component.text("Oleo de Polimento\n", NamedTextColor.DARK_GREEN, TextDecoration.BOLD))
                .append(Component.text("Usado para melhorar valores\n", NamedTextColor.DARK_GRAY))
                .append(Component.text("de mods ja existentes.\n", NamedTextColor.DARK_GRAY))
                .append(Component.text("Receita: 20 pepitas de ouro.\n\n", NamedTextColor.DARK_GRAY))
                .append(Component.text("Tambem participa da receita\n", NamedTextColor.BLACK))
                .append(Component.text("da Pedra de Refinamento.", NamedTextColor.BLACK))
                .build());

        meta.addPages(Component.text()
                .append(Component.text("Fluxo da Forja\n\n", NamedTextColor.DARK_PURPLE, TextDecoration.BOLD))
                .append(Component.text("1. Iniciar na Bigorna\n", NamedTextColor.BLACK))
                .append(Component.text("2. Aquecer na Blast Furnace\n", NamedTextColor.BLACK))
                .append(Component.text("3. Modelar na Bigorna\n", NamedTextColor.BLACK))
                .append(Component.text("4. Resfriar no Caldeirao\n", NamedTextColor.BLACK))
                .append(Component.text("5. Refinar no Rebolo\n", NamedTextColor.BLACK))
                .append(Component.text("6. Coletar na mesa\n\n", NamedTextColor.BLACK))
                .append(Component.text("Cada acerto melhora a\n", NamedTextColor.DARK_GREEN))
                .append(Component.text("qualidade da forja.", NamedTextColor.DARK_GREEN))
                .build());

        meta.addPages(Component.text()
                .append(Component.text("Estrutura da Forja\n\n", NamedTextColor.GOLD, TextDecoration.BOLD))
                .append(Component.text("Blocos necessarios:\n", NamedTextColor.BLACK))
                .append(Component.text("- Bancada de Ferraria\n", NamedTextColor.DARK_GRAY))
                .append(Component.text("- Alto-Forno\n", NamedTextColor.DARK_GRAY))
                .append(Component.text("- Bigorna\n", NamedTextColor.DARK_GRAY))
                .append(Component.text("- Caldeirao\n", NamedTextColor.DARK_GRAY))
                .append(Component.text("- Rebolo\n\n", NamedTextColor.DARK_GRAY))
                .append(Component.text("Sem essa estrutura completa,\n", NamedTextColor.DARK_RED))
                .append(Component.text("a forja nao finaliza.", NamedTextColor.DARK_RED))
                .build());

        meta.addPages(Component.text()
                .append(Component.text("Funcoes da Mesa\n\n", NamedTextColor.AQUA, TextDecoration.BOLD))
                .append(Component.text("- Melhorar stats por forja\n", NamedTextColor.BLACK))
                .append(Component.text("- Reparar equipamentos\n", NamedTextColor.BLACK))
                .append(Component.text("- Fabricar Oleo de Polimento\n\n", NamedTextColor.BLACK))
                .append(Component.text("Sem os blocos da forja\n", NamedTextColor.DARK_RED))
                .append(Component.text("o processo nao conclui.", NamedTextColor.DARK_RED))
                .build());

        meta.addPages(Component.text()
                .append(Component.text("Dica de Oficina\n\n", NamedTextColor.DARK_GREEN, TextDecoration.BOLD))
                .append(Component.text("Monte a estrutura completa\n", NamedTextColor.BLACK))
                .append(Component.text("perto da Bigorna para nao\n", NamedTextColor.BLACK))
                .append(Component.text("perder tempo no trajeto.\n\n", NamedTextColor.BLACK))
                .append(Component.text("Ferreiro forte sustenta o\n", NamedTextColor.DARK_GREEN))
                .append(Component.text("late-game do grupo.", NamedTextColor.DARK_GREEN))
                .build());

        normalizeBookPages(meta);
        book.setItemMeta(meta);
        return book;
    }

    private ItemStack createMercadorGuide() {
        ItemStack book = new ItemStack(Material.WRITTEN_BOOK);
        BookMeta meta = (BookMeta) book.getItemMeta();
        if (meta == null) return null;

        meta.setTitle("\u00A76Guia da Mesa do Mercador");
        meta.setAuthor("NemonicRP");
        meta.displayName(Component.text("Compendio Mercantil", NamedTextColor.GOLD, TextDecoration.BOLD));

        meta.addPages(Component.text()
                .append(Component.text("Mesa do Mercador\n", NamedTextColor.GOLD, TextDecoration.BOLD))
                .append(Component.text("Guia do Mercador\n\n", NamedTextColor.DARK_GRAY))
                .append(Component.text("Mesa da classe: ", NamedTextColor.BLACK))
                .append(Component.text("Bancada de Cartografia\n\n", NamedTextColor.GOLD, TextDecoration.BOLD))
                .append(Component.text("A classe gerencia economia,\n", NamedTextColor.BLACK))
                .append(Component.text("identificacao e mobilidade.\n\n", NamedTextColor.BLACK))
                .append(Component.text("Parte das funcoes depende\n", NamedTextColor.DARK_GREEN))
                .append(Component.text("de mesas aliadas por perto.", NamedTextColor.DARK_GREEN))
                .build());

        meta.addPages(Component.text()
                .append(Component.text("Orb da Classe\n\n", NamedTextColor.DARK_PURPLE, TextDecoration.BOLD))
                .append(Component.text("Fabrica: ", NamedTextColor.BLACK))
                .append(Component.text("Pedra de Refinamento\n", NamedTextColor.DARK_GREEN, TextDecoration.BOLD))
                .append(Component.text("Usada para Tier 1 -> Tier 2.\n\n", NamedTextColor.DARK_GRAY))
                .append(Component.text("Tambem fabrica Pergaminho\n", NamedTextColor.BLACK))
                .append(Component.text("de Identificacao.", NamedTextColor.BLACK))
                .build());

        meta.addPages(Component.text()
                .append(Component.text("Funcoes da Mesa\n\n", NamedTextColor.YELLOW, TextDecoration.BOLD))
                .append(Component.text("1. Avaliar item (identificar)\n", NamedTextColor.BLACK))
                .append(Component.text("2. Fabricar Pergaminho\n", NamedTextColor.BLACK))
                .append(Component.text("3. Fabricar Pocao Retorno\n", NamedTextColor.BLACK))
                .append(Component.text("4. Aplicar Encantamento\n", NamedTextColor.BLACK))
                .append(Component.text("5. Fabricar Pedra Refino\n", NamedTextColor.BLACK))
                .append(Component.text("6. Rotas comerciais\n", NamedTextColor.BLACK))
                .append(Component.text("7. Fabricar Selo de Rota\n", NamedTextColor.BLACK))
                .build());

        meta.addPages(Component.text()
                .append(Component.text("Pre-requisitos\n\n", NamedTextColor.AQUA, TextDecoration.BOLD))
                .append(Component.text("Cada funcao da mesa pode\n", NamedTextColor.BLACK))
                .append(Component.text("exigir uma mesa aliada\n", NamedTextColor.BLACK))
                .append(Component.text("num raio de 10 blocos:\n\n", NamedTextColor.BLACK))
                .append(Component.text("- Pocao Retorno: Mesa\n", NamedTextColor.DARK_GRAY))
                .append(Component.text("  de Transmutacao\n", NamedTextColor.DARK_GRAY))
                .append(Component.text("- Encantar: Bigorna\n", NamedTextColor.DARK_GRAY))
                .append(Component.text("- Pedra de Refinamento:\n", NamedTextColor.DARK_GRAY))
                .append(Component.text("  Bigorna\n\n", NamedTextColor.DARK_GRAY))
                .append(Component.text("Sem a mesa proxima, o\n", NamedTextColor.RED))
                .append(Component.text("botao fica bloqueado.", NamedTextColor.RED))
                .build());

        meta.addPages(Component.text()
                .append(Component.text("Warps Publicas\n\n", NamedTextColor.GOLD, TextDecoration.BOLD))
                .append(Component.text("Pontos globais marcados\n", NamedTextColor.BLACK))
                .append(Component.text("com bloco BEACON. Apenas\n", NamedTextColor.BLACK))
                .append(Component.text("admin pode registrar.\n\n", NamedTextColor.BLACK))
                .append(Component.text("Custo: 40 mana base\n", NamedTextColor.DARK_AQUA))
                .append(Component.text("+ 5 mana por passageiro\n", NamedTextColor.DARK_AQUA))
                .append(Component.text("(max 4 passageiros).\n\n", NamedTextColor.DARK_AQUA))
                .append(Component.text("Cast: 3s parado.\n", NamedTextColor.DARK_GRAY))
                .append(Component.text("Use: /norb publicwarp tp", NamedTextColor.DARK_GRAY))
                .build());

        meta.addPages(Component.text()
                .append(Component.text("Dica de Operacao\n\n", NamedTextColor.DARK_GREEN, TextDecoration.BOLD))
                .append(Component.text("Mantenha estoque de papel,\n", NamedTextColor.BLACK))
                .append(Component.text("tinta, lapis, perola e ouro\n", NamedTextColor.BLACK))
                .append(Component.text("para nao parar a cadeia.\n\n", NamedTextColor.BLACK))
                .append(Component.text("Sem Mercador ativo, o\n", NamedTextColor.DARK_GREEN))
                .append(Component.text("servidor trava em escala.", NamedTextColor.DARK_GREEN))
                .build());

        normalizeBookPages(meta);
        book.setItemMeta(meta);
        return book;
    }

    private ItemStack createGuerreiroGuide() {
        ItemStack book = new ItemStack(Material.WRITTEN_BOOK);
        BookMeta meta = (BookMeta) book.getItemMeta();
        if (meta == null) return null;

        meta.setTitle("\u00A74Guia do Guerreiro");
        meta.setAuthor("NemonicRP");
        meta.displayName(Component.text("Codice do Guerreiro", NamedTextColor.DARK_RED, TextDecoration.BOLD));

        meta.addPages(Component.text()
                .append(Component.text("Guia do Guerreiro\n", NamedTextColor.DARK_RED, TextDecoration.BOLD))
                .append(Component.text("Linha de frente do grupo.\n\n", NamedTextColor.DARK_GRAY))
                .append(Component.text("Mesa da classe: ", NamedTextColor.BLACK))
                .append(Component.text("Nao possui mesa propria.\n\n", NamedTextColor.RED, TextDecoration.BOLD))
                .append(Component.text("Seu sistema principal e\n", NamedTextColor.BLACK))
                .append(Component.text("drop de orbs no combate.", NamedTextColor.BLACK))
                .build());

        meta.addPages(Component.text()
                .append(Component.text("Orbs da Classe\n\n", NamedTextColor.DARK_GREEN, TextDecoration.BOLD))
                .append(Component.text("Ao matar monstros, pode\n", NamedTextColor.BLACK))
                .append(Component.text("dropar diretamente:\n", NamedTextColor.BLACK))
                .append(Component.text("- Runa de Poder\n", NamedTextColor.DARK_GREEN))
                .append(Component.text("- Runa Nobre\n", NamedTextColor.DARK_GREEN))
                .append(Component.text("- Moeda da Sorte\n\n", NamedTextColor.DARK_GREEN))
                .append(Component.text("As chances sao definidas\n", NamedTextColor.DARK_GRAY))
                .append(Component.text("na configuracao do servidor.", NamedTextColor.DARK_GRAY))
                .build());

        meta.addPages(Component.text()
                .append(Component.text("Funcoes da Classe\n\n", NamedTextColor.GOLD, TextDecoration.BOLD))
                .append(Component.text("1. Iniciar confrontos\n", NamedTextColor.BLACK))
                .append(Component.text("2. Segurar a linha de frente\n", NamedTextColor.BLACK))
                .append(Component.text("3. Abrir janela de dano\n", NamedTextColor.BLACK))
                .append(Component.text("4. Garantir farm de elites\n", NamedTextColor.BLACK))
                .append(Component.text("   para orb drop\n\n", NamedTextColor.BLACK))
                .append(Component.text("Quanto mais ativo no pve,\n", NamedTextColor.DARK_AQUA))
                .append(Component.text("mais fluxo de orbs raras.", NamedTextColor.DARK_AQUA))
                .build());

        meta.addPages(Component.text()
                .append(Component.text("Sinergia\n\n", NamedTextColor.DARK_PURPLE, TextDecoration.BOLD))
                .append(Component.text("Ferreiro equipa.\n", NamedTextColor.BLACK))
                .append(Component.text("Alquimista abastece.\n", NamedTextColor.BLACK))
                .append(Component.text("Mercador gira economia.\n\n", NamedTextColor.BLACK))
                .append(Component.text("Guerreiro abre o caminho\n", NamedTextColor.DARK_GREEN))
                .append(Component.text("para todos progredirem.", NamedTextColor.DARK_GREEN))
                .build());

        meta.addPages(Component.text()
                .append(Component.text("Evolucao de Jogador\n\n", NamedTextColor.RED, TextDecoration.BOLD))
                .append(Component.text("Treine tres pontos:\n", NamedTextColor.BLACK))
                .append(Component.text("- Posicionamento\n", NamedTextColor.BLACK))
                .append(Component.text("- Tempo de habilidade\n", NamedTextColor.BLACK))
                .append(Component.text("- Leitura da luta\n\n", NamedTextColor.BLACK))
                .append(Component.text("Quanto melhor sua leitura,\n", NamedTextColor.RED))
                .append(Component.text("mais seguro o progresso.", NamedTextColor.RED))
                .build());

        normalizeBookPages(meta);
        book.setItemMeta(meta);
        return book;
    }

    public ItemStack createWelcomeBook() {
        ItemStack book = new ItemStack(Material.WRITTEN_BOOK);
        BookMeta meta = (BookMeta) book.getItemMeta();
        if (meta == null) return null;

        meta.setTitle("\u00A7bGuia de Embati");
        meta.setAuthor("Embati");
        meta.displayName(Component.text("Cronicas de Embati", NamedTextColor.AQUA, TextDecoration.BOLD));

        // Pagina 1
        meta.addPages(Component.text()
                .append(Component.text("Bem-vindo(a) a Embati\n\n", NamedTextColor.GOLD, TextDecoration.BOLD))
                .append(Component.text("Voce parece meio perdido\n", NamedTextColor.BLACK))
                .append(Component.text("ne, fica tranquilo\n", NamedTextColor.BLACK))
                .append(Component.text("vou te explicar como as ", NamedTextColor.BLACK))
                .append(Component.text("*coisas*", NamedTextColor.DARK_GREEN, TextDecoration.ITALIC))
                .append(Component.text("funcionam aqui\n\n", NamedTextColor.BLACK))
                .append(Component.text("Neste mundo, ninguem\n", NamedTextColor.BLACK))
                .append(Component.text("sobrevive sozinho.\n", NamedTextColor.BLACK))
                .append(Component.text("Escolha um caminho e\n", NamedTextColor.BLACK))
                .append(Component.text("encontre amigos.\n\n", NamedTextColor.BLACK))
                .append(Component.text("Vire a pagina...", NamedTextColor.DARK_GREEN))
                .build());

        // Pagina 2
        meta.addPages(Component.text()
                .append(Component.text("Mestres de Embati\n\n", NamedTextColor.DARK_PURPLE, TextDecoration.BOLD))
                .append(Component.text("Alquimista: ", NamedTextColor.BLACK))
                .append(Component.text("Karmila\n", NamedTextColor.LIGHT_PURPLE, TextDecoration.BOLD))
                .append(Component.text("Ferreiro: ", NamedTextColor.BLACK))
                .append(Component.text("Doran\n", NamedTextColor.RED, TextDecoration.BOLD))
                .append(Component.text("Mercador: ", NamedTextColor.BLACK))
                .append(Component.text("Lyra\n", NamedTextColor.GOLD, TextDecoration.BOLD))
                .append(Component.text("Guerreiro: ", NamedTextColor.BLACK))
                .append(Component.text("Gareth\n\n", NamedTextColor.DARK_RED, TextDecoration.BOLD))
                .append(Component.text("Cada mestre governa uma\n", NamedTextColor.DARK_GRAY))
                .append(Component.text("parte do destino de Embati.", NamedTextColor.DARK_GRAY))
                .build());

        // Pagina 3
        meta.addPages(Component.text()
                .append(Component.text("Os Mestres\n\n", NamedTextColor.GOLD, TextDecoration.BOLD))
                .append(Component.text("Karmila ", NamedTextColor.LIGHT_PURPLE, TextDecoration.BOLD))
                .append(Component.text("aprendeu a arrancar\n", NamedTextColor.BLACK))
                .append(Component.text("forca de itens esquecidos.\n", NamedTextColor.BLACK))
                .append(Component.text("Doran ", NamedTextColor.RED, TextDecoration.BOLD))
                .append(Component.text("ergueu as forjas apos\n", NamedTextColor.BLACK))
                .append(Component.text("a queda da muralha.\n\n", NamedTextColor.BLACK))
                .append(Component.text("Lyra ", NamedTextColor.GOLD, TextDecoration.BOLD))
                .append(Component.text("manteve as rotas vivas,\n", NamedTextColor.BLACK))
                .append(Component.text("e ", NamedTextColor.BLACK))
                .append(Component.text("Gareth ", NamedTextColor.DARK_RED, TextDecoration.BOLD))
                .append(Component.text("formou a linha que\n", NamedTextColor.BLACK))
                .append(Component.text("defende Embati ate hoje.", NamedTextColor.BLACK))
                .build());

        // Pagina 4
        meta.addPages(Component.text()
                .append(Component.text("Tiers de Itens\n\n", NamedTextColor.DARK_AQUA, TextDecoration.BOLD))
                .append(Component.text("Tier 0 Comum\n", NamedTextColor.GRAY, TextDecoration.BOLD))
                .append(Component.text("Sem modificadores.\n\n", NamedTextColor.DARK_GRAY))
                .append(Component.text("Tier 1 Magico\n", NamedTextColor.BLUE, TextDecoration.BOLD))
                .append(Component.text("Ate 2 modificadores.\n\n", NamedTextColor.DARK_GRAY))
                .append(Component.text("Tier 2 Raro\n", NamedTextColor.DARK_PURPLE, TextDecoration.BOLD))
                .append(Component.text("Ate 6 modificadores.", NamedTextColor.DARK_GRAY))
                .build());

        // Pagina 5
        meta.addPages(Component.text()
                .append(Component.text("Tier 3 Unico\n\n", NamedTextColor.GOLD, TextDecoration.BOLD))
                .append(Component.text("Itens mais poderosos.\n", NamedTextColor.DARK_GRAY))
                .append(Component.text("Itens dropam em Tier 0\n", NamedTextColor.BLACK))
                .append(Component.text("e sobem com orbs.\n\n", NamedTextColor.BLACK))
                .append(Component.text("Antes de tudo, o item\n", NamedTextColor.DARK_RED))
                .append(Component.text("precisa ser identificado!", NamedTextColor.DARK_RED, TextDecoration.BOLD))
                .build());

        // Pagina 6
        meta.addPages(Component.text()
                .append(Component.text("Prefixos e Sufixos\n\n", NamedTextColor.DARK_GREEN, TextDecoration.BOLD))
                .append(Component.text("Essas terras sempre foram\n", NamedTextColor.BLACK))
                .append(Component.text("estranhas, tudo o que e\n", NamedTextColor.BLACK))
                .append(Component.text("feito aqui vem com ", NamedTextColor.BLACK))
                .append(Component.text("*coisinhas*", NamedTextColor.DARK_GREEN, TextDecoration.ITALIC))
                .append(Component.text(" a mais...\n\n", NamedTextColor.BLACK))
                .append(Component.text("Na fabricacao da arma/item,\n", NamedTextColor.DARK_GRAY))
                .append(Component.text("Prefixos e Sufixos podem\n", NamedTextColor.DARK_GRAY))
                .append(Component.text("vir junto no resultado.\n\n", NamedTextColor.DARK_GRAY))
                .append(Component.text("Prefixos: encantamentos\n", NamedTextColor.DARK_AQUA))
                .append(Component.text("Sufixos: atributos", NamedTextColor.DARK_AQUA))
                .build());

        // Pagina 7
        meta.addPages(Component.text()
                .append(Component.text("Modificadores\n\n", NamedTextColor.DARK_GREEN, TextDecoration.BOLD))
                .append(Component.text("Modificadores sao os\n", NamedTextColor.BLACK))
                .append(Component.text("bonus do item, em forma\n", NamedTextColor.BLACK))
                .append(Component.text("de Prefixos e Sufixos.\n\n", NamedTextColor.BLACK))
                .append(Component.text("Mais tier = mais espaco\n", NamedTextColor.DARK_GREEN))
                .append(Component.text("para modificadores.", NamedTextColor.DARK_GREEN))
                .build());

        // Pagina 8
        meta.addPages(Component.text()
                .append(Component.text("Sistema de Orbs\n\n", NamedTextColor.GOLD, TextDecoration.BOLD))
                .append(Component.text("Pergaminho\n", NamedTextColor.DARK_GREEN, TextDecoration.BOLD))
                .append(Component.text("Identifica o item.\n\n", NamedTextColor.DARK_GRAY))
                .append(Component.text("Oleo de Polimento\n", NamedTextColor.DARK_GREEN, TextDecoration.BOLD))
                .append(Component.text("Melhora valores atuais.\n", NamedTextColor.DARK_GRAY))
                .append(Component.text("Fabricado na Mesa do\n", NamedTextColor.DARK_GRAY))
                .append(Component.text("Ferreiro com 20 pepitas\n", NamedTextColor.DARK_GRAY))
                .append(Component.text("de ouro.\n\n", NamedTextColor.DARK_GRAY))
                .append(Component.text("Pedra de Encantamento\n", NamedTextColor.DARK_GREEN, TextDecoration.BOLD))
                .append(Component.text("Comum -> Magico\n", NamedTextColor.DARK_GRAY))
                .append(Component.text("(adiciona 1-2 mods).\n\n", NamedTextColor.DARK_GRAY))
                .append(Component.text("Pedra de Reforco\n", NamedTextColor.DARK_GREEN, TextDecoration.BOLD))
                .append(Component.text("Adiciona 1 mod em item\n", NamedTextColor.DARK_GRAY))
                .append(Component.text("Magico com 1 slot livre.", NamedTextColor.DARK_GRAY))
                .build());

        // Pagina 9
        meta.addPages(Component.text()
                .append(Component.text("Mais Orbs\n\n", NamedTextColor.GOLD, TextDecoration.BOLD))
                .append(Component.text("Pedra de Refinamento\n", NamedTextColor.DARK_GREEN, TextDecoration.BOLD))
                .append(Component.text("Comum -> Raro direto\n", NamedTextColor.DARK_GRAY))
                .append(Component.text("(3-6 mods aleatorios).\n\n", NamedTextColor.DARK_GRAY))
                .append(Component.text("Runa de Poder\n", NamedTextColor.DARK_GREEN, TextDecoration.BOLD))
                .append(Component.text("Adiciona 1 mod aleatorio\n", NamedTextColor.DARK_GRAY))
                .append(Component.text("em item Raro com slot\n", NamedTextColor.DARK_GRAY))
                .append(Component.text("livre.\n\n", NamedTextColor.DARK_GRAY))
                .append(Component.text("Runa Nobre\n", NamedTextColor.DARK_GREEN, TextDecoration.BOLD))
                .append(Component.text("Magico -> Raro\n", NamedTextColor.DARK_GRAY))
                .append(Component.text("(adiciona 1 mod extra).\n\n", NamedTextColor.DARK_GRAY))
                .append(Component.text("Moeda da Sorte\n", NamedTextColor.DARK_GREEN, TextDecoration.BOLD))
                .append(Component.text("Tier aleatorio:\n", NamedTextColor.DARK_GRAY))
                .append(Component.text("75% Magico / 23% Raro /\n", NamedTextColor.DARK_GRAY))
                .append(Component.text("2% Unico.\n\n", NamedTextColor.DARK_GRAY))
                .append(Component.text("Pedra Corrosiva\n", NamedTextColor.DARK_RED, TextDecoration.BOLD))
                .append(Component.text("Remove 1 mod aleatorio\n", NamedTextColor.DARK_GRAY))
                .append(Component.text("(pode ser o melhor).", NamedTextColor.DARK_GRAY))
                .build());

        // Pagina 10
        meta.addPages(Component.text()
                .append(Component.text("Como Usar Orbs\n\n", NamedTextColor.DARK_AQUA, TextDecoration.BOLD))
                .append(Component.text("1. Orb na mao principal\n", NamedTextColor.BLACK))
                .append(Component.text("2. Item alvo na secundaria\n", NamedTextColor.BLACK))
                .append(Component.text("3. Clique direito\n\n", NamedTextColor.BLACK))
                .append(Component.text("Item precisa estar\n", NamedTextColor.DARK_RED))
                .append(Component.text("identificado antes!", NamedTextColor.DARK_RED, TextDecoration.BOLD))
                .build());

        // Pagina 11 - Warps Publicas
        meta.addPages(Component.text()
                .append(Component.text("Warps Publicas\n\n", NamedTextColor.GOLD, TextDecoration.BOLD))
                .append(Component.text("Pontos globais marcados\n", NamedTextColor.BLACK))
                .append(Component.text("com BEACON. Apenas admin\n", NamedTextColor.BLACK))
                .append(Component.text("pode registrar.\n\n", NamedTextColor.BLACK))
                .append(Component.text("/norb publicwarp tp\n", NamedTextColor.DARK_AQUA))
                .append(Component.text("Custo: 40 mana base.\n", NamedTextColor.DARK_AQUA))
                .append(Component.text("+5 mana por passageiro\n", NamedTextColor.DARK_AQUA))
                .append(Component.text("(max 4).", NamedTextColor.DARK_AQUA))
                .build());

        // Pagina 12
        meta.addPages(Component.text()
                .append(Component.text("Sua Jornada Comeca\n\n", NamedTextColor.GOLD, TextDecoration.BOLD))
                .append(Component.text("Converse com os mestres\n", NamedTextColor.DARK_GRAY))
                .append(Component.text("e forme seu grupo.\n\n", NamedTextColor.DARK_GRAY))
                .append(Component.text("Boa sorte, aventureiro.\n", NamedTextColor.DARK_GREEN, TextDecoration.ITALIC))
                .append(Component.text("Embati precisa de voce.", NamedTextColor.DARK_GREEN, TextDecoration.ITALIC))
                .build());

        normalizeBookPages(meta);
        book.setItemMeta(meta);
        return book;
    }

    private void normalizeBookPages(BookMeta meta) {
        List<Component> originalPages = new ArrayList<>(meta.pages());
        List<Component> normalizedPages = new ArrayList<>();

        for (Component page : originalPages) {
            String legacy = LegacyComponentSerializer.legacySection()
                    .serialize(page)
                    .replace("\r", "");
            List<String> wrappedLines = wrapForBook(legacy);
            normalizedPages.addAll(splitLinesIntoPages(wrappedLines));
        }

        if (!normalizedPages.isEmpty()) {
            if (normalizedPages.size() > BOOK_MAX_PAGES) {
                normalizedPages = new ArrayList<>(normalizedPages.subList(0, BOOK_MAX_PAGES));
            }
            meta.pages(normalizedPages);
        }
    }

    private List<String> wrapForBook(String text) {
        List<String> out = new ArrayList<>();
        String[] rawLines = text.split("\n", -1);
        String carryStyle = "";

        for (String raw : rawLines) {
            if (raw.isBlank()) {
                out.add("");
                continue;
            }

            String[] words = raw.trim().split("\\s+");
            StringBuilder line = new StringBuilder(carryStyle);
            int visibleLen = visibleLength(carryStyle);

            for (String word : words) {
                int wordVisible = visibleLength(word);

                if (wordVisible > BOOK_MAX_CHARS_PER_LINE) {
                    if (!line.isEmpty()) {
                        out.add(line.toString());
                        carryStyle = activeLegacyCodes(line.toString());
                        line.setLength(0);
                        line.append(carryStyle);
                        visibleLen = visibleLength(carryStyle);
                    }
                    List<String> splitWord = splitLegacyWord(word, BOOK_MAX_CHARS_PER_LINE, carryStyle);
                    for (int i = 0; i < splitWord.size(); i++) {
                        String part = splitWord.get(i);
                        if (i == splitWord.size() - 1) {
                            line.append(part);
                            visibleLen = visibleLength(line.toString());
                        } else {
                            out.add(part);
                        }
                    }
                    carryStyle = activeLegacyCodes(line.toString());
                    continue;
                }

                if (visibleLen == 0) {
                    line.append(word);
                    visibleLen += wordVisible;
                } else if (visibleLen + 1 + wordVisible <= BOOK_MAX_CHARS_PER_LINE) {
                    line.append(' ').append(word);
                    visibleLen += 1 + wordVisible;
                } else {
                    out.add(line.toString());
                    carryStyle = activeLegacyCodes(line.toString());
                    line.setLength(0);
                    line.append(carryStyle).append(word);
                    visibleLen = visibleLength(carryStyle) + wordVisible;
                }
            }

            if (visibleLength(line.toString()) > 0) {
                out.add(line.toString());
                carryStyle = activeLegacyCodes(line.toString());
            }
        }

        return out;
    }

    private List<Component> splitLinesIntoPages(List<String> lines) {
        List<Component> pages = new ArrayList<>();
        List<String> pageLines = new ArrayList<>();
        int pageChars = 0;

        for (String line : lines) {
            int addedChars = visibleLength(line) + (pageLines.isEmpty() ? 0 : 1);
            boolean exceedsLines = pageLines.size() >= BOOK_MAX_LINES_PER_PAGE;
            boolean exceedsChars = pageChars + addedChars > BOOK_MAX_CHARS_PER_PAGE;

            if ((exceedsLines || exceedsChars) && !pageLines.isEmpty()) {
                pages.add(LegacyComponentSerializer.legacySection().deserialize(String.join("\n", pageLines)));
                pageLines.clear();
                pageChars = 0;
                addedChars = visibleLength(line);
            }

            pageLines.add(line);
            pageChars += addedChars;
        }

        if (!pageLines.isEmpty()) {
            pages.add(LegacyComponentSerializer.legacySection().deserialize(String.join("\n", pageLines)));
        }

        return pages;
    }

    private int visibleLength(String legacyText) {
        int visible = 0;
        for (int i = 0; i < legacyText.length(); i++) {
            char c = legacyText.charAt(i);
            if (c == '\u00A7' && i + 1 < legacyText.length()) {
                i++;
                continue;
            }
            visible++;
        }
        return visible;
    }

    private List<String> splitLegacyWord(String word, int maxVisible, String carryStyle) {
        List<String> parts = new ArrayList<>();
        StringBuilder part = new StringBuilder(carryStyle);
        int visible = visibleLength(carryStyle);

        for (int i = 0; i < word.length(); i++) {
            char c = word.charAt(i);
            if (c == '\u00A7' && i + 1 < word.length()) {
                part.append(c).append(word.charAt(i + 1));
                i++;
                continue;
            }

            if (visible >= maxVisible) {
                parts.add(part.toString());
                String nextStyle = activeLegacyCodes(part.toString());
                part.setLength(0);
                part.append(nextStyle);
                visible = visibleLength(nextStyle);
            }

            part.append(c);
            visible++;
        }

        if (visibleLength(part.toString()) > 0) {
            parts.add(part.toString());
        }
        return parts;
    }

    private String activeLegacyCodes(String text) {
        char color = 0;
        StringBuilder formats = new StringBuilder();

        for (int i = 0; i < text.length() - 1; i++) {
            char c = text.charAt(i);
            if (c != '\u00A7') continue;
            char code = Character.toLowerCase(text.charAt(i + 1));
            i++;

            if (code == 'r') {
                color = 0;
                formats.setLength(0);
                continue;
            }

            if ((code >= '0' && code <= '9') || (code >= 'a' && code <= 'f')) {
                color = code;
                formats.setLength(0);
                continue;
            }

            if ("klmno".indexOf(code) >= 0) {
                String token = "\u00A7" + code;
                if (formats.indexOf(token) < 0) {
                    formats.append(token);
                }
            }
        }

        StringBuilder result = new StringBuilder();
        if (color != 0) result.append('\u00A7').append(color);
        result.append(formats);
        return result.toString();
    }
}
