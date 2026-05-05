/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  io.lumine.mythic.lib.api.item.NBTItem
 *  net.kyori.adventure.text.Component
 *  net.kyori.adventure.text.format.NamedTextColor
 *  net.kyori.adventure.text.format.TextColor
 *  net.kyori.adventure.text.format.TextDecoration
 *  org.bukkit.Bukkit
 *  org.bukkit.Location
 *  org.bukkit.Material
 *  org.bukkit.NamespacedKey
 *  org.bukkit.advancement.Advancement
 *  org.bukkit.advancement.AdvancementProgress
 *  org.bukkit.block.Block
 *  org.bukkit.entity.HumanEntity
 *  org.bukkit.entity.Player
 *  org.bukkit.event.EventHandler
 *  org.bukkit.event.EventPriority
 *  org.bukkit.event.Listener
 *  org.bukkit.event.block.Action
 *  org.bukkit.event.block.BlockPlaceEvent
 *  org.bukkit.event.block.CrafterCraftEvent
 *  org.bukkit.event.enchantment.EnchantItemEvent
 *  org.bukkit.event.enchantment.PrepareItemEnchantEvent
 *  org.bukkit.event.inventory.CraftItemEvent
 *  org.bukkit.event.inventory.InventoryOpenEvent
 *  org.bukkit.event.inventory.InventoryType
 *  org.bukkit.event.inventory.PrepareAnvilEvent
 *  org.bukkit.event.inventory.PrepareGrindstoneEvent
 *  org.bukkit.event.inventory.PrepareItemCraftEvent
 *  org.bukkit.event.inventory.PrepareSmithingEvent
 *  org.bukkit.event.player.PlayerInteractEvent
 *  org.bukkit.inventory.AnvilInventory
 *  org.bukkit.inventory.ItemStack
 *  org.bukkit.inventory.meta.ItemMeta
 *  org.bukkit.plugin.Plugin
 */
package com.nemonicorp.orbs;

import com.nemonicorp.orbs.BlacksmithTableListener;
import com.nemonicorp.orbs.MerchantTableListener;
import com.nemonicorp.orbs.NemonicOrbPlugin;
import com.nemonicorp.orbs.TransmutationTableListener;
import io.lumine.mythic.lib.api.item.NBTItem;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.advancement.Advancement;
import org.bukkit.advancement.AdvancementProgress;
import org.bukkit.block.Block;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.block.CrafterCraftEvent;
import org.bukkit.event.enchantment.EnchantItemEvent;
import org.bukkit.event.enchantment.PrepareItemEnchantEvent;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.inventory.PrepareAnvilEvent;
import org.bukkit.event.inventory.PrepareGrindstoneEvent;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.event.inventory.PrepareSmithingEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.AnvilInventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.Plugin;

public class CraftingBlockListener
implements Listener {
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

    @EventHandler(priority=EventPriority.HIGHEST)
    public void onInteractEnchantingTable(PlayerInteractEvent event) {
        if (!this.plugin.getConfig().getBoolean("blocking.enchanting-table", true)) {
            return;
        }
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }
        Block block = event.getClickedBlock();
        if (block == null || block.getType() != Material.ENCHANTING_TABLE) {
            return;
        }
        Player player = event.getPlayer();
        if (!(player instanceof Player)) {
            return;
        }
        Player p = player;
        boolean transmutEnabled = this.plugin.getConfig().getBoolean("transmutation.enabled", true);
        boolean isAlquimista = this.transmutationListener != null && this.transmutationListener.isAlquimista(p);
        event.setCancelled(true);
        if (transmutEnabled && isAlquimista) {
            Location tableLoc = block.getLocation();
            if (this.plugin.getGuideManager().showGuideIfNeeded(p, "alquimista")) {
                Bukkit.getScheduler().runTaskLater((Plugin)this.plugin, () -> this.transmutationListener.openGUI(p, tableLoc), 1L);
            } else {
                Bukkit.getScheduler().runTask((Plugin)this.plugin, () -> this.transmutationListener.openGUI(p, tableLoc));
            }
            return;
        }
        p.sendMessage((Component)Component.text((String)"[NemonicOrb] Mesa de encantamento desativada.", (TextColor)NamedTextColor.RED));
    }

    @EventHandler(priority=EventPriority.HIGHEST)
    public void onInteractAnvilTable(PlayerInteractEvent event) {
        boolean isFerreiro;
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }
        Block block = event.getClickedBlock();
        if (block == null) {
            return;
        }
        Material type = block.getType();
        if (type != Material.ANVIL && type != Material.CHIPPED_ANVIL && type != Material.DAMAGED_ANVIL) {
            return;
        }
        Player player = event.getPlayer();
        if (!(player instanceof Player)) {
            return;
        }
        Player p = player;
        boolean blacksmithEnabled = this.plugin.getConfig().getBoolean("blacksmith.enabled", true);
        if (!blacksmithEnabled) {
            return;
        }
        boolean bl = isFerreiro = this.blacksmithListener != null && this.blacksmithListener.isFerreiro(p);
        if (!isFerreiro) {
            return;
        }
        event.setCancelled(true);
        Location tableLoc = block.getLocation();
        if (this.plugin.getGuideManager().showGuideIfNeeded(p, "ferreiro")) {
            Bukkit.getScheduler().runTaskLater((Plugin)this.plugin, () -> this.blacksmithListener.openMainGUI(p, tableLoc), 1L);
        } else {
            Bukkit.getScheduler().runTask((Plugin)this.plugin, () -> this.blacksmithListener.openMainGUI(p, tableLoc));
        }
    }

    @EventHandler(priority=EventPriority.HIGHEST)
    public void onOpenEnchantingTable(InventoryOpenEvent event) {
        if (!this.plugin.getConfig().getBoolean("blocking.enchanting-table", true)) {
            return;
        }
        if (event.getInventory().getType() != InventoryType.ENCHANTING) {
            return;
        }
        HumanEntity humanEntity = event.getPlayer();
        if (humanEntity instanceof Player) {
            boolean isAlquimista;
            Player p = (Player)humanEntity;
            boolean transmutEnabled = this.plugin.getConfig().getBoolean("transmutation.enabled", true);
            boolean bl = isAlquimista = this.transmutationListener != null && this.transmutationListener.isAlquimista(p);
            if (this.plugin.getConfig().getBoolean("debug", false)) {
                this.plugin.getLogger().info("[DEBUG-ENCHANT] Jogador=" + p.getName() + " transmutEnabled=" + transmutEnabled + " listenerExists=" + (this.transmutationListener != null) + " isAlquimista=" + isAlquimista);
            }
            if (transmutEnabled && isAlquimista) {
                event.setCancelled(true);
                Location tableLoc = event.getInventory().getLocation();
                if (tableLoc == null) {
                    tableLoc = p.getLocation();
                }
                Location finalLoc = tableLoc;
                if (this.plugin.getGuideManager().showGuideIfNeeded(p, "alquimista")) {
                    Bukkit.getScheduler().runTaskLater((Plugin)this.plugin, () -> this.transmutationListener.openGUI(p, finalLoc), 1L);
                } else {
                    Bukkit.getScheduler().runTask((Plugin)this.plugin, () -> this.transmutationListener.openGUI(p, finalLoc));
                }
                return;
            }
            event.setCancelled(true);
            p.sendMessage((Component)Component.text((String)"[NemonicOrb] Mesa de encantamento desativada.", (TextColor)NamedTextColor.RED));
        }
    }

    @EventHandler(priority=EventPriority.HIGHEST)
    public void onEnchant(EnchantItemEvent event) {
        if (!this.plugin.getConfig().getBoolean("blocking.enchanting-table", true)) {
            return;
        }
        event.setCancelled(true);
    }

    @EventHandler(priority=EventPriority.HIGHEST)
    public void onPrepareEnchant(PrepareItemEnchantEvent event) {
        if (!this.plugin.getConfig().getBoolean("blocking.enchanting-table", true)) {
            return;
        }
        event.setCancelled(true);
    }

    @EventHandler(priority=EventPriority.HIGHEST)
    public void onPrepareCraftCrafter(PrepareItemCraftEvent event) {
        if (!this.plugin.getConfig().getBoolean("blocking.crafter", true)) {
            return;
        }
        if (event.getRecipe() == null) {
            return;
        }
        if (event.getRecipe().getResult().getType() != Material.CRAFTER) {
            return;
        }
        event.getInventory().setResult(null);
    }

    @EventHandler(priority=EventPriority.HIGHEST)
    public void onCraftCrafter(CraftItemEvent event) {
        if (!this.plugin.getConfig().getBoolean("blocking.crafter", true)) {
            return;
        }
        if (event.getRecipe().getResult().getType() != Material.CRAFTER) {
            return;
        }
        event.setCancelled(true);
        HumanEntity humanEntity = event.getWhoClicked();
        if (humanEntity instanceof Player) {
            Player p = (Player)humanEntity;
            p.sendMessage((Component)Component.text((String)"[NemonicOrb] A Mesa de Trabalho Automatica esta bloqueada.", (TextColor)NamedTextColor.RED));
        }
    }

    @EventHandler(priority=EventPriority.HIGHEST)
    public void onPlaceCrafter(BlockPlaceEvent event) {
        if (!this.plugin.getConfig().getBoolean("blocking.crafter", true)) {
            return;
        }
        if (event.getBlockPlaced().getType() != Material.CRAFTER) {
            return;
        }
        event.setCancelled(true);
        event.getPlayer().sendMessage((Component)Component.text((String)"[NemonicOrb] Voce nao pode colocar Mesa de Trabalho Automatica.", (TextColor)NamedTextColor.RED));
    }

    @EventHandler(priority=EventPriority.HIGHEST)
    public void onInteractCrafter(PlayerInteractEvent event) {
        if (!this.plugin.getConfig().getBoolean("blocking.crafter", true)) {
            return;
        }
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }
        Block block = event.getClickedBlock();
        if (block == null || block.getType() != Material.CRAFTER) {
            return;
        }
        event.setCancelled(true);
        event.getPlayer().sendMessage((Component)Component.text((String)"[NemonicOrb] A Mesa de Trabalho Automatica esta bloqueada.", (TextColor)NamedTextColor.RED));
    }

    @EventHandler(priority=EventPriority.HIGHEST)
    public void onCrafterCraft(CrafterCraftEvent event) {
        if (!this.plugin.getConfig().getBoolean("blocking.crafter", true)) {
            return;
        }
        event.setCancelled(true);
    }

    @EventHandler(priority=EventPriority.HIGHEST)
    public void onOpenAnvil(InventoryOpenEvent event) {
        boolean isFerreiro;
        if (event.getInventory().getType() != InventoryType.ANVIL) {
            return;
        }
        HumanEntity humanEntity = event.getPlayer();
        if (!(humanEntity instanceof Player)) {
            return;
        }
        Player p = (Player)humanEntity;
        boolean blacksmithEnabled = this.plugin.getConfig().getBoolean("blacksmith.enabled", true);
        if (!blacksmithEnabled) {
            return;
        }
        boolean bl = isFerreiro = this.blacksmithListener != null && this.blacksmithListener.isFerreiro(p);
        if (this.plugin.getConfig().getBoolean("debug", false)) {
            this.plugin.getLogger().info("[DEBUG-ANVIL] Jogador=" + p.getName() + " isFerreiro=" + isFerreiro);
        }
        if (isFerreiro) {
            event.setCancelled(true);
            Location tableLoc = event.getInventory().getLocation();
            if (tableLoc == null) {
                tableLoc = p.getLocation();
            }
            Location finalLoc = tableLoc;
            if (this.plugin.getGuideManager().showGuideIfNeeded(p, "ferreiro")) {
                Bukkit.getScheduler().runTaskLater((Plugin)this.plugin, () -> this.blacksmithListener.openMainGUI(p, finalLoc), 1L);
            } else {
                Bukkit.getScheduler().runTask((Plugin)this.plugin, () -> this.blacksmithListener.openMainGUI(p, finalLoc));
            }
            return;
        }
        event.setCancelled(true);
        p.sendMessage((Component)Component.text((String)"[NemonicOrb] Apenas Ferreiros podem usar a bigorna.", (TextColor)NamedTextColor.RED));
    }

    @EventHandler(priority=EventPriority.HIGHEST)
    public void onPrepareAnvil(PrepareAnvilEvent event) {
        if (!this.plugin.getConfig().getBoolean("blocking.anvil-orb-items", true)) {
            return;
        }
        AnvilInventory inv = event.getInventory();
        if (this.hasOrbTag(inv.getFirstItem()) || this.hasOrbTag(inv.getSecondItem())) {
            event.setResult(null);
            for (HumanEntity viewer : event.getViewers()) {
                if (!(viewer instanceof Player)) continue;
                Player p = (Player)viewer;
                p.sendMessage((Component)Component.text((String)"[NemonicOrb] Itens modificados por orbs nao podem ser combinados na bigorna.", (TextColor)NamedTextColor.RED));
            }
        }
    }

    @EventHandler(priority=EventPriority.HIGHEST)
    public void onOpenForgeBlock(InventoryOpenEvent event) {
        boolean isFerreiro;
        HumanEntity humanEntity = event.getPlayer();
        if (!(humanEntity instanceof Player)) {
            return;
        }
        Player p = (Player)humanEntity;
        InventoryType type = event.getInventory().getType();
        if (type != InventoryType.BLAST_FURNACE && type != InventoryType.GRINDSTONE) {
            return;
        }
        boolean blacksmithEnabled = this.plugin.getConfig().getBoolean("blacksmith.enabled", true);
        if (!blacksmithEnabled) {
            return;
        }
        boolean bl = isFerreiro = this.blacksmithListener != null && this.blacksmithListener.isFerreiro(p);
        if (isFerreiro) {
            event.setCancelled(true);
            return;
        }
        if (type == InventoryType.GRINDSTONE) {
            return;
        }
        event.setCancelled(true);
        p.sendMessage((Component)Component.text((String)"[NemonicOrb] Apenas Ferreiros podem usar o Auto Forno.", (TextColor)NamedTextColor.RED));
    }

    @EventHandler(priority=EventPriority.HIGHEST)
    public void onPrepareGrindstone(PrepareGrindstoneEvent event) {
        if (!this.plugin.getConfig().getBoolean("blocking.grindstone-orb-items", true)) {
            return;
        }
        for (ItemStack item : event.getInventory().getContents()) {
            if (!this.hasOrbTag(item)) continue;
            event.setResult(null);
            for (HumanEntity viewer : event.getViewers()) {
                if (!(viewer instanceof Player)) continue;
                Player p = (Player)viewer;
                p.sendMessage((Component)Component.text((String)"[NemonicOrb] Itens modificados por orbs nao podem ser usados na rebarbadora.", (TextColor)NamedTextColor.RED));
            }
            return;
        }
    }

    @EventHandler(priority=EventPriority.HIGHEST)
    public void onPrepareSmithing(PrepareSmithingEvent event) {
        if (!this.plugin.getConfig().getBoolean("blocking.smithing-orb-items", true)) {
            return;
        }
        for (ItemStack item : event.getInventory().getContents()) {
            if (!this.hasOrbTag(item)) continue;
            event.setResult(null);
            for (HumanEntity viewer : event.getViewers()) {
                if (!(viewer instanceof Player)) continue;
                Player p = (Player)viewer;
                p.sendMessage((Component)Component.text((String)"[NemonicOrb] Itens modificados por orbs nao podem ser usados na mesa de ferraria.", (TextColor)NamedTextColor.RED));
            }
            return;
        }
    }

    @EventHandler
    public void onCraftEnchantingTable(CraftItemEvent event) {
        NamespacedKey key;
        Advancement adv;
        ItemMeta meta;
        if (event.getRecipe().getResult().getType() != Material.ENCHANTING_TABLE) {
            return;
        }
        HumanEntity humanEntity = event.getWhoClicked();
        if (!(humanEntity instanceof Player)) {
            return;
        }
        Player player = (Player)humanEntity;
        ItemStack result = event.getCurrentItem();
        if (result != null && (meta = result.getItemMeta()) != null) {
            meta.itemName((Component)Component.text((String)"Mesa de Transmutacao", (TextColor)NamedTextColor.GOLD, (TextDecoration[])new TextDecoration[]{TextDecoration.BOLD}));
            result.setItemMeta(meta);
        }
        if ((adv = Bukkit.getAdvancement((NamespacedKey)(key = new NamespacedKey((Plugin)this.plugin, "edward")))) == null) {
            return;
        }
        AdvancementProgress progress = player.getAdvancementProgress(adv);
        if (!progress.isDone()) {
            for (String criteria : progress.getRemainingCriteria()) {
                progress.awardCriteria(criteria);
            }
        }
    }

    @EventHandler
    public void onCraftAnvil(CraftItemEvent event) {
        AdvancementProgress progress;
        NamespacedKey key;
        Advancement adv;
        ItemMeta meta;
        Material resultType = event.getRecipe().getResult().getType();
        if (resultType != Material.ANVIL) {
            return;
        }
        HumanEntity humanEntity = event.getWhoClicked();
        if (!(humanEntity instanceof Player)) {
            return;
        }
        Player player = (Player)humanEntity;
        ItemStack result = event.getCurrentItem();
        if (result != null && (meta = result.getItemMeta()) != null) {
            meta.itemName((Component)Component.text((String)"Mesa do Ferreiro", (TextColor)NamedTextColor.GOLD, (TextDecoration[])new TextDecoration[]{TextDecoration.BOLD}));
            result.setItemMeta(meta);
        }
        if ((adv = Bukkit.getAdvancement((NamespacedKey)(key = new NamespacedKey((Plugin)this.plugin, "coracao_de_aco")))) != null && !(progress = player.getAdvancementProgress(adv)).isDone()) {
            for (String criteria : progress.getRemainingCriteria()) {
                progress.awardCriteria(criteria);
            }
        }
    }

    @EventHandler(priority=EventPriority.HIGHEST)
    public void onCartographyInteract(PlayerInteractEvent event) {
        boolean isMercador;
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }
        Block block = event.getClickedBlock();
        if (block == null || block.getType() != Material.CARTOGRAPHY_TABLE) {
            return;
        }
        Player player = event.getPlayer();
        if (!(player instanceof Player)) {
            return;
        }
        Player p = player;
        boolean merchantEnabled = this.plugin.getConfig().getBoolean("merchant.enabled", true);
        if (!merchantEnabled) {
            return;
        }
        boolean bl = isMercador = this.merchantListener != null && this.merchantListener.isMercador(p);
        if (isMercador) {
            event.setCancelled(true);
            Location loc = block.getLocation();
            if (this.plugin.getGuideManager().showGuideIfNeeded(p, "mercador")) {
                Bukkit.getScheduler().runTaskLater((Plugin)this.plugin, () -> this.merchantListener.openGUI(p, loc), 1L);
            } else {
                Bukkit.getScheduler().runTask((Plugin)this.plugin, () -> this.merchantListener.openGUI(p, loc));
            }
            return;
        }
        event.setCancelled(true);
        p.sendMessage((Component)Component.text((String)"[NemonicOrb] Apenas Mercadores podem usar a mesa de cartografia.", (TextColor)NamedTextColor.RED));
    }

    @EventHandler
    public void onCraftCartographyTable(CraftItemEvent event) {
        AdvancementProgress progress;
        NamespacedKey key;
        Advancement adv;
        ItemMeta meta;
        if (event.getRecipe().getResult().getType() != Material.CARTOGRAPHY_TABLE) {
            return;
        }
        HumanEntity humanEntity = event.getWhoClicked();
        if (!(humanEntity instanceof Player)) {
            return;
        }
        Player player = (Player)humanEntity;
        ItemStack result = event.getCurrentItem();
        if (result != null && (meta = result.getItemMeta()) != null) {
            meta.itemName((Component)Component.text((String)"Mesa do Mercador", (TextColor)NamedTextColor.GOLD, (TextDecoration[])new TextDecoration[]{TextDecoration.BOLD}));
            result.setItemMeta(meta);
        }
        if ((adv = Bukkit.getAdvancement((NamespacedKey)(key = new NamespacedKey((Plugin)this.plugin, "rota_de_ouro")))) != null && !(progress = player.getAdvancementProgress(adv)).isDone()) {
            for (String criteria : progress.getRemainingCriteria()) {
                progress.awardCriteria(criteria);
            }
        }
    }

    private boolean hasOrbTag(ItemStack item) {
        if (item == null || item.getType().isAir()) {
            return false;
        }
        try {
            NBTItem nbt = NBTItem.get((ItemStack)item);
            return nbt.hasTag("NEMONICORB_TIER");
        }
        catch (Exception e) {
            return false;
        }
    }
}

