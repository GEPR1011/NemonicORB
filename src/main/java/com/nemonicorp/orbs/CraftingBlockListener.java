package com.nemonicorp.orbs;

import io.lumine.mythic.lib.api.item.NBTItem;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.advancement.Advancement;
import org.bukkit.advancement.AdvancementProgress;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.block.Block;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.block.CrafterCraftEvent;
import org.bukkit.event.enchantment.EnchantItemEvent;
import org.bukkit.event.enchantment.PrepareItemEnchantEvent;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.inventory.PrepareAnvilEvent;
import org.bukkit.event.inventory.PrepareGrindstoneEvent;
import org.bukkit.event.inventory.PrepareSmithingEvent;
import org.bukkit.inventory.AnvilInventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

/**
 * Bloqueia interacoes de forja/encantamento para itens do NemonicOrbPlugin.
 * - Mesa de encantamento: desativada para todos EXCETO Alquimistas (que usam transmutacao)
 * - Anvil/Grindstone/Smithing: bloqueados para itens com tag NEMONICORB_TIER
 */
public class CraftingBlockListener implements Listener {

    private final NemonicOrbPlugin plugin;
    private TransmutationTableListener transmutationListener;
    private BlacksmithTableListener blacksmithListener;
    private MerchantTableListener merchantListener;

    public CraftingBlockListener(NemonicOrbPlugin plugin) {
        this.plugin = plugin;
    }

    public void setTransmutationListener(TransmutationTableListener listener) {
        this.transmutationListener = listener;
    }

    public void setBlacksmithListener(BlacksmithTableListener listener) {
        this.blacksmithListener = listener;
    }

    public void setMerchantListener(MerchantTableListener listener) {
        this.merchantListener = listener;
    }

    // ══════════════════════════════════════════════
    // Mesa de Encantamento — desativada globalmente
    // ══════════════════════════════════════════════

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInteractEnchantingTable(PlayerInteractEvent event) {
        if (!plugin.getConfig().getBoolean("blocking.enchanting-table", true)) return;
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;

        Block block = event.getClickedBlock();
        if (block == null || block.getType() != Material.ENCHANTING_TABLE) return;
        if (!(event.getPlayer() instanceof Player p)) return;

        boolean transmutEnabled = plugin.getConfig().getBoolean("transmutation.enabled", true);
        boolean isAlquimista = transmutationListener != null && transmutationListener.isAlquimista(p);

        // Cancela antes da abertura da GUI vanilla para evitar qualquer uso/consumo
        // acidental do item na mao (ex.: garrafas/consumiveis).
        event.setCancelled(true);

        if (transmutEnabled && isAlquimista) {
            final org.bukkit.Location tableLoc = block.getLocation();
            if (plugin.getGuideManager().showGuideIfNeeded(p, "alquimista")) {
                Bukkit.getScheduler().runTaskLater(plugin, () ->
                        transmutationListener.openGUI(p, tableLoc), 1L);
            } else {
                Bukkit.getScheduler().runTask(plugin, () ->
                        transmutationListener.openGUI(p, tableLoc));
            }
            return;
        }

        p.sendMessage(Component.text(
                "[NemonicOrb] Mesa de encantamento desativada.", NamedTextColor.RED));
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInteractAnvilTable(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        Block block = event.getClickedBlock();
        if (block == null) return;
        Material type = block.getType();
        if (type != Material.ANVIL && type != Material.CHIPPED_ANVIL && type != Material.DAMAGED_ANVIL) return;
        if (!(event.getPlayer() instanceof Player p)) return;

        boolean blacksmithEnabled = plugin.getConfig().getBoolean("blacksmith.enabled", true);
        if (!blacksmithEnabled) return;

        boolean isFerreiro = blacksmithListener != null && blacksmithListener.isFerreiro(p);
        if (!isFerreiro) return;

        // Forca abertura da mesa custom mesmo com bloqueio externo (ex.: WorldGuard).
        event.setCancelled(true);
        final org.bukkit.Location tableLoc = block.getLocation();
        if (plugin.getGuideManager().showGuideIfNeeded(p, "ferreiro")) {
            Bukkit.getScheduler().runTaskLater(plugin, () ->
                    blacksmithListener.openMainGUI(p, tableLoc), 1L);
        } else {
            Bukkit.getScheduler().runTask(plugin, () ->
                    blacksmithListener.openMainGUI(p, tableLoc));
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onOpenEnchantingTable(InventoryOpenEvent event) {
        if (!plugin.getConfig().getBoolean("blocking.enchanting-table", true)) return;
        if (event.getInventory().getType() != InventoryType.ENCHANTING) return;

        if (event.getPlayer() instanceof Player p) {
            boolean transmutEnabled = plugin.getConfig().getBoolean("transmutation.enabled", true);
            boolean isAlquimista = transmutationListener != null && transmutationListener.isAlquimista(p);

            if (plugin.getConfig().getBoolean("debug", false)) {
                plugin.getLogger().info("[DEBUG-ENCHANT] Jogador=" + p.getName()
                        + " transmutEnabled=" + transmutEnabled
                        + " listenerExists=" + (transmutationListener != null)
                        + " isAlquimista=" + isAlquimista);
            }

            if (transmutEnabled && isAlquimista) {
                event.setCancelled(true);
                org.bukkit.Location tableLoc = event.getInventory().getLocation();
                if (tableLoc == null) {
                    tableLoc = p.getLocation();
                }
                final org.bukkit.Location finalLoc = tableLoc;
                // Mostrar guia na primeira vez, depois abrir mesa
                if (plugin.getGuideManager().showGuideIfNeeded(p, "alquimista")) {
                    // Guia aberto — agendar abertura da mesa apos fechar o livro
                    Bukkit.getScheduler().runTaskLater(plugin, () ->
                            transmutationListener.openGUI(p, finalLoc), 1L);
                } else {
                    Bukkit.getScheduler().runTask(plugin, () ->
                            transmutationListener.openGUI(p, finalLoc));
                }
                return;
            }

            event.setCancelled(true);
            p.sendMessage(Component.text(
                    "[NemonicOrb] Mesa de encantamento desativada.", NamedTextColor.RED));
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onEnchant(EnchantItemEvent event) {
        if (!plugin.getConfig().getBoolean("blocking.enchanting-table", true)) return;
        event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPrepareEnchant(PrepareItemEnchantEvent event) {
        if (!plugin.getConfig().getBoolean("blocking.enchanting-table", true)) return;
        event.setCancelled(true);
    }

    // ══════════════════════════════════════════════
    // Anvil — Hub do Ferreiro (Bigorna = Mesa do Ferreiro)
    // ══════════════════════════════════════════════

    // Bloqueia a Mesa de Trabalho Automatica (Crafter vanilla 1.21).
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPrepareCraftCrafter(PrepareItemCraftEvent event) {
        if (!plugin.getConfig().getBoolean("blocking.crafter", true)) return;
        if (event.getRecipe() == null) return;
        if (event.getRecipe().getResult().getType() != Material.CRAFTER) return;

        event.getInventory().setResult(null);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onCraftCrafter(CraftItemEvent event) {
        if (!plugin.getConfig().getBoolean("blocking.crafter", true)) return;
        if (event.getRecipe().getResult().getType() != Material.CRAFTER) return;

        event.setCancelled(true);
        if (event.getWhoClicked() instanceof Player p) {
            p.sendMessage(Component.text(
                    "[NemonicOrb] A Mesa de Trabalho Automatica esta bloqueada.",
                    NamedTextColor.RED));
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlaceCrafter(BlockPlaceEvent event) {
        if (!plugin.getConfig().getBoolean("blocking.crafter", true)) return;
        if (event.getBlockPlaced().getType() != Material.CRAFTER) return;

        event.setCancelled(true);
        event.getPlayer().sendMessage(Component.text(
                "[NemonicOrb] Voce nao pode colocar Mesa de Trabalho Automatica.",
                NamedTextColor.RED));
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInteractCrafter(PlayerInteractEvent event) {
        if (!plugin.getConfig().getBoolean("blocking.crafter", true)) return;
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;

        Block block = event.getClickedBlock();
        if (block == null || block.getType() != Material.CRAFTER) return;

        event.setCancelled(true);
        event.getPlayer().sendMessage(Component.text(
                "[NemonicOrb] A Mesa de Trabalho Automatica esta bloqueada.",
                NamedTextColor.RED));
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onCrafterCraft(CrafterCraftEvent event) {
        if (!plugin.getConfig().getBoolean("blocking.crafter", true)) return;
        event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onOpenAnvil(InventoryOpenEvent event) {
        if (event.getInventory().getType() != InventoryType.ANVIL) return;
        if (!(event.getPlayer() instanceof Player p)) return;

        boolean blacksmithEnabled = plugin.getConfig().getBoolean("blacksmith.enabled", true);
        if (!blacksmithEnabled) return;

        boolean isFerreiro = blacksmithListener != null && blacksmithListener.isFerreiro(p);

        if (plugin.getConfig().getBoolean("debug", false)) {
            plugin.getLogger().info("[DEBUG-ANVIL] Jogador=" + p.getName()
                    + " isFerreiro=" + isFerreiro);
        }

        if (isFerreiro) {
            event.setCancelled(true);
            org.bukkit.Location tableLoc = event.getInventory().getLocation();
            if (tableLoc == null) tableLoc = p.getLocation();
            final org.bukkit.Location finalLoc = tableLoc;
            if (plugin.getGuideManager().showGuideIfNeeded(p, "ferreiro")) {
                Bukkit.getScheduler().runTaskLater(plugin, () ->
                        blacksmithListener.openMainGUI(p, finalLoc), 1L);
            } else {
                Bukkit.getScheduler().runTask(plugin, () ->
                        blacksmithListener.openMainGUI(p, finalLoc));
            }
            return;
        }

        // Nao-Ferreiro: bloqueia bigorna
        event.setCancelled(true);
        p.sendMessage(Component.text(
                "[NemonicOrb] Apenas Ferreiros podem usar a bigorna.", NamedTextColor.RED));
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPrepareAnvil(PrepareAnvilEvent event) {
        if (!plugin.getConfig().getBoolean("blocking.anvil-orb-items", true)) return;

        AnvilInventory inv = event.getInventory();
        if (hasOrbTag(inv.getFirstItem()) || hasOrbTag(inv.getSecondItem())) {
            event.setResult(null);
            for (HumanEntity viewer : event.getViewers()) {
                if (viewer instanceof Player p) {
                    p.sendMessage(Component.text(
                            "[NemonicOrb] Itens modificados por orbs nao podem ser combinados na bigorna.",
                            NamedTextColor.RED));
                }
            }
        }
    }

    // ══════════════════════════════════════════════
    // Grindstone/Blast Furnace
    // - Rebolo: acesso publico (nao-Ferreiro)
    // - Blast Furnace: restrito ao fluxo do Ferreiro
    // ══════════════════════════════════════════════

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onOpenForgeBlock(InventoryOpenEvent event) {
        if (!(event.getPlayer() instanceof Player p)) return;
        InventoryType type = event.getInventory().getType();

        if (type != InventoryType.BLAST_FURNACE && type != InventoryType.GRINDSTONE) return;

        boolean blacksmithEnabled = plugin.getConfig().getBoolean("blacksmith.enabled", true);
        if (!blacksmithEnabled) return;

        boolean isFerreiro = blacksmithListener != null && blacksmithListener.isFerreiro(p);

        // Ferreiro: sempre cancelar GUI vanilla (interacao via PlayerInteractEvent)
        if (isFerreiro) {
            event.setCancelled(true);
            return;
        }

        // Nao-Ferreiro: Rebolo e publico; Blast Furnace segue restrito
        if (type == InventoryType.GRINDSTONE) {
            return;
        }

        event.setCancelled(true);
        p.sendMessage(Component.text(
                "[NemonicOrb] Apenas Ferreiros podem usar o Auto Forno.", NamedTextColor.RED));
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPrepareGrindstone(PrepareGrindstoneEvent event) {
        if (!plugin.getConfig().getBoolean("blocking.grindstone-orb-items", true)) return;

        for (ItemStack item : event.getInventory().getContents()) {
            if (hasOrbTag(item)) {
                event.setResult(null);
                for (HumanEntity viewer : event.getViewers()) {
                    if (viewer instanceof Player p) {
                        p.sendMessage(Component.text(
                                "[NemonicOrb] Itens modificados por orbs nao podem ser usados na rebarbadora.",
                                NamedTextColor.RED));
                    }
                }
                return;
            }
        }
    }

    // ══════════════════════════════════════════════
    // Smithing Table — publica (armor trims), bloqueia orbs
    // ══════════════════════════════════════════════

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPrepareSmithing(PrepareSmithingEvent event) {
        if (!plugin.getConfig().getBoolean("blocking.smithing-orb-items", true)) return;

        for (ItemStack item : event.getInventory().getContents()) {
            if (hasOrbTag(item)) {
                event.setResult(null);
                for (HumanEntity viewer : event.getViewers()) {
                    if (viewer instanceof Player p) {
                        p.sendMessage(Component.text(
                                "[NemonicOrb] Itens modificados por orbs nao podem ser usados na mesa de ferraria.",
                                NamedTextColor.RED));
                    }
                }
                return;
            }
        }
    }

    // ══════════════════════════════════════════════
    // Conquista "Edward?" ao craftar mesa de encantamento
    // ══════════════════════════════════════════════

    @EventHandler
    public void onCraftEnchantingTable(CraftItemEvent event) {
        if (event.getRecipe().getResult().getType() != Material.ENCHANTING_TABLE) return;
        if (!(event.getWhoClicked() instanceof Player player)) return;

        // Renomear para "Mesa de Transmutacao"
        ItemStack result = event.getCurrentItem();
        if (result != null) {
            ItemMeta meta = result.getItemMeta();
            if (meta != null) {
                meta.itemName(net.kyori.adventure.text.Component.text(
                        "Mesa de Transmutacao",
                        net.kyori.adventure.text.format.NamedTextColor.GOLD,
                        net.kyori.adventure.text.format.TextDecoration.BOLD));
                result.setItemMeta(meta);
            }
        }

        NamespacedKey key = new NamespacedKey(plugin, "edward");
        Advancement adv = Bukkit.getAdvancement(key);
        if (adv == null) return;

        AdvancementProgress progress = player.getAdvancementProgress(adv);
        if (!progress.isDone()) {
            for (String criteria : progress.getRemainingCriteria()) {
                progress.awardCriteria(criteria);
            }
        }
    }

    // ══════════════════════════════════════════════
    // Renomear Anvil ao craftar -> "Mesa do Ferreiro"
    // ══════════════════════════════════════════════

    @EventHandler
    public void onCraftAnvil(CraftItemEvent event) {
        Material resultType = event.getRecipe().getResult().getType();
        if (resultType != Material.ANVIL) return;
        if (!(event.getWhoClicked() instanceof Player player)) return;

        ItemStack result = event.getCurrentItem();
        if (result != null) {
            ItemMeta meta = result.getItemMeta();
            if (meta != null) {
                meta.itemName(Component.text(
                        "Mesa do Ferreiro",
                        NamedTextColor.GOLD,
                        net.kyori.adventure.text.format.TextDecoration.BOLD));
                result.setItemMeta(meta);
            }
        }

        // Conquista "Coração de Aço"
        NamespacedKey key = new NamespacedKey(plugin, "coracao_de_aco");
        Advancement adv = Bukkit.getAdvancement(key);
        if (adv != null) {
            AdvancementProgress progress = player.getAdvancementProgress(adv);
            if (!progress.isDone()) {
                for (String criteria : progress.getRemainingCriteria()) {
                    progress.awardCriteria(criteria);
                }
            }
        }
    }

    // ══════════════════════════════════════════════
    // Cartography Table — Hub do Mercador
    // ══════════════════════════════════════════════

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onCartographyInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        Block block = event.getClickedBlock();
        if (block == null || block.getType() != Material.CARTOGRAPHY_TABLE) return;
        if (!(event.getPlayer() instanceof Player p)) return;

        boolean merchantEnabled = plugin.getConfig().getBoolean("merchant.enabled", true);
        if (!merchantEnabled) return;

        boolean isMercador = merchantListener != null && merchantListener.isMercador(p);

        if (isMercador) {
            event.setCancelled(true);
            org.bukkit.Location loc = block.getLocation();
            if (plugin.getGuideManager().showGuideIfNeeded(p, "mercador")) {
                Bukkit.getScheduler().runTaskLater(plugin, () -> merchantListener.openGUI(p, loc), 1L);
            } else {
                Bukkit.getScheduler().runTask(plugin, () -> merchantListener.openGUI(p, loc));
            }
            return;
        }

        // Nao-Mercador: bloquear acesso
        event.setCancelled(true);
        p.sendMessage(Component.text(
                "[NemonicOrb] Apenas Mercadores podem usar a mesa de cartografia.", NamedTextColor.RED));
    }

    // ══════════════════════════════════════════════
    // Renomear Cartography Table ao craftar -> "Mesa do Mercador"
    // ══════════════════════════════════════════════

    @EventHandler
    public void onCraftCartographyTable(CraftItemEvent event) {
        if (event.getRecipe().getResult().getType() != Material.CARTOGRAPHY_TABLE) return;
        if (!(event.getWhoClicked() instanceof Player player)) return;

        ItemStack result = event.getCurrentItem();
        if (result != null) {
            ItemMeta meta = result.getItemMeta();
            if (meta != null) {
                meta.itemName(Component.text(
                        "Mesa do Mercador",
                        NamedTextColor.GOLD,
                        net.kyori.adventure.text.format.TextDecoration.BOLD));
                result.setItemMeta(meta);
            }
        }

        // Conquista "Rota de Ouro"
        NamespacedKey key = new NamespacedKey(plugin, "rota_de_ouro");
        Advancement adv = Bukkit.getAdvancement(key);
        if (adv != null) {
            AdvancementProgress progress = player.getAdvancementProgress(adv);
            if (!progress.isDone()) {
                for (String criteria : progress.getRemainingCriteria()) {
                    progress.awardCriteria(criteria);
                }
            }
        }
    }

    // ══════════════════════════════════════════════
    // Utilitario
    // ══════════════════════════════════════════════

    private boolean hasOrbTag(ItemStack item) {
        if (item == null || item.getType().isAir()) return false;
        try {
            NBTItem nbt = NBTItem.get(item);
            return nbt.hasTag(OrbListener.NBT_TIER);
        } catch (Exception e) {
            return false;
        }
    }
}
