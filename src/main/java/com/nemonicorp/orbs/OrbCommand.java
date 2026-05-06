package com.nemonicorp.orbs;

import com.google.gson.Gson;
import io.lumine.mythic.lib.api.item.ItemTag;
import io.lumine.mythic.lib.api.item.NBTItem;
import net.Indyuce.mmoitems.api.item.mmoitem.LiveMMOItem;
import net.Indyuce.mmoitems.stat.data.DoubleData;
import net.Indyuce.mmoitems.stat.type.DoubleStat;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Comando admin /nemonicorb v2.
 *
 * Subcomandos:
 *   identify <jogador>         — Pergaminho de Identificacao
 *   polish <jogador>           — Oleo de Polimento
 *   transmute <jogador>        — Pedra de Encantamento (Comum → Magico)
 *   augment <jogador>          — Pedra de Reforco (+1 mod Magico)
 *   regal <jogador>            — Runa Nobre (Magico → Raro)
 *   alchemy <jogador>          — Pedra de Refinamento (Comum → Raro)
 *   exalt <jogador>            — Runa de Poder (+1 mod Raro)
 *   chance <jogador>           — Moeda da Sorte (Tier aleatorio)
 *   annul <jogador>            — Pedra Corrosiva (Remove 1 mod)
 *   set-tier <jogador> <0-3>   — Define tier + rola mods
 *   set-id <jogador> <0|1>     — Define identificacao
 *   info <jogador>             — Mostra info
 *   reload                     — Recarrega config
 */
public class OrbCommand implements CommandExecutor, TabCompleter {

    private static final List<String> SUBCOMMANDS = List.of(
            "identify", "polish", "transmute", "augment", "regal",
            "alchemy", "exalt", "chance", "annul",
            "set-tier", "set-id", "info", "audit", "giveguide", "publicwarp", "reload"
    );

    private final NemonicOrbPlugin plugin;
    private final Gson gson;

    public OrbCommand(NemonicOrbPlugin plugin) {
        this.plugin = plugin;
        this.gson = plugin.getModifierEngine().getGson();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }

        String action = args[0].toLowerCase();

        if (action.equals("reload")) {
            plugin.reloadConfig();
            plugin.getModifierEngine().load();
            msg(sender, "reload-success");
            return true;
        }

        if (action.equals("audit")) {
            cmdAudit(sender, args);
            return true;
        }

        if (action.equals("publicwarp")) {
            cmdPublicWarp(sender, args);
            return true;
        }

        if (args.length < 2) {
            sender.sendMessage(cc("&cUso: /" + label + " <acao> <jogador>"));
            return true;
        }

        Player target = Bukkit.getPlayer(args[1]);
        if (target == null || !target.isOnline()) {
            sender.sendMessage(cc("&cJogador nao encontrado: " + args[1]));
            return true;
        }

        if (action.equals("giveguide")) {
            cmdGiveGuide(sender, target, args);
            return true;
        }

        ItemStack offHand = target.getInventory().getItemInOffHand();
        if (offHand.getType() == Material.AIR) {
            sender.sendMessage(cc("&cO jogador nao tem item na mao secundaria!"));
            return true;
        }

        NBTItem nbt = NBTItem.get(offHand);
        boolean isMMOItem = nbt.hasType();

        switch (action) {
            case "identify" -> cmdIdentify(sender, target, offHand, nbt);
            case "polish" -> cmdPolish(sender, target, offHand, nbt, isMMOItem);
            case "transmute" -> cmdTier(sender, target, offHand, nbt, 0, 1, isMMOItem);
            case "augment" -> cmdAugment(sender, target, offHand, nbt, isMMOItem);
            case "regal" -> cmdTier(sender, target, offHand, nbt, 1, 2, isMMOItem);
            case "alchemy" -> cmdTier(sender, target, offHand, nbt, 0, 2, isMMOItem);
            case "exalt" -> cmdExalt(sender, target, offHand, nbt, isMMOItem);
            case "chance" -> cmdChance(sender, target, offHand, nbt, isMMOItem);
            case "annul" -> cmdAnnul(sender, target, offHand, nbt, isMMOItem);
            case "set-tier" -> cmdSetTier(sender, target, nbt, args);
            case "set-id" -> cmdSetId(sender, target, nbt, args);
            case "info" -> cmdInfo(sender, nbt, offHand);
            case "giveguide" -> cmdGiveGuide(sender, target, args);
            default -> sendHelp(sender);
        }

        return true;
    }

    // ── Subcomandos ──

    private void cmdIdentify(CommandSender sender, Player target, ItemStack item, NBTItem nbt) {
        nbt.addTag(new ItemTag(OrbListener.NBT_IDENTIFIED, 1));
        ItemStack updated = nbt.toItem();
        OrbListener listener = getListener();
        if (listener != null) listener.updateItemDisplay(updated, NBTItem.get(updated));
        target.getInventory().setItemInOffHand(updated);
        sender.sendMessage(cc("&aItem identificado para " + target.getName() + "."));
    }

    private void cmdPolish(CommandSender sender, Player target, ItemStack item, NBTItem nbt, boolean isMMOItem) {
        int maxPolish = plugin.getConfig().getInt("max-polish", 5);
        int current = nbt.hasTag(OrbListener.NBT_POLISH) ? nbt.getInteger(OrbListener.NBT_POLISH) : 0;

        if (current >= maxPolish) {
            sender.sendMessage(cc("&cItem ja polido ao maximo (" + maxPolish + ")."));
            return;
        }

        ItemStack result;
        if (isMMOItem) {
            var polishSec = plugin.getConfig().getConfigurationSection("polish-percent");
            if (polishSec == null) return;

            LiveMMOItem live = new LiveMMOItem(item);
            ModifierEngine engine = plugin.getModifierEngine();
            int count = 0;

            for (String statId : polishSec.getKeys(false)) {
                double pct = polishSec.getDouble(statId) / 100.0;
                DoubleStat stat = engine.resolveStat(statId);
                if (stat == null) continue;
                var data = live.getData(stat);
                if (data instanceof DoubleData dd && dd.getValue() != 0) {
                    live.setData(stat, new DoubleData(Math.round(dd.getValue() * (1.0 + pct) * 100.0) / 100.0));
                    count++;
                }
            }

            ItemStack built = live.newBuilder().build();
            NBTItem builtNbt = NBTItem.get(built);
            preserveTags(nbt, builtNbt);
            builtNbt.addTag(new ItemTag(OrbListener.NBT_POLISH, current + 1));
            result = builtNbt.toItem();
        } else {
            nbt.addTag(new ItemTag(OrbListener.NBT_POLISH, current + 1));
            result = nbt.toItem();
        }

        OrbListener listener = getListener();
        if (listener != null) listener.updateItemDisplay(result, NBTItem.get(result));
        target.getInventory().setItemInOffHand(result);
        sender.sendMessage(cc("&aPolimento aplicado. (" + (current + 1) + "/" + maxPolish + ")"));
    }

    private void cmdTier(CommandSender sender, Player target, ItemStack item, NBTItem nbt,
                         int requiredTier, int newTier, boolean isMMOItem) {
        int currentTier = nbt.hasTag(OrbListener.NBT_TIER) ? nbt.getInteger(OrbListener.NBT_TIER) : 0;
        if (currentTier != requiredTier) {
            sender.sendMessage(cc("&cItem precisa ser tier " + requiredTier + ". Atual: " + currentTier));
            return;
        }

        String category;
        if (isMMOItem) {
            category = resolveCategory(nbt.getString("MMOITEMS_ITEM_TYPE"));
        } else {
            category = resolveCategoryFromMaterial(item.getType());
        }

        if (category == null) {
            sender.sendMessage(cc("&cTipo nao suportado."));
            return;
        }

        ModifierEngine engine = plugin.getModifierEngine();
        int itemLevel = getItemLevel(nbt);
        int modCount, maxMods;

        if (newTier == 1) {
            modCount = new Random().nextInt(2) + 1; maxMods = 2;
        } else {
            modCount = new Random().nextInt(4) + 3; maxMods = 6;
        }

        List<ModifierEngine.RolledModifier> mods = engine.rollForTier(category, modCount, itemLevel, maxMods);

        ItemStack result;
        if (isMMOItem) {
            LiveMMOItem live = new LiveMMOItem(item);
            for (var m : mods) {
                for (var entry : m.stats().entrySet()) {
                    DoubleStat stat = engine.resolveStat(entry.getKey());
                    if (stat == null) continue;
                    var data = live.getData(stat);
                    double cur = (data instanceof DoubleData dd) ? dd.getValue() : 0;
                    live.setData(stat, new DoubleData(cur + entry.getValue()));
                }
            }
            ItemStack built = live.newBuilder().build();
            NBTItem builtNbt = NBTItem.get(built);
            builtNbt.addTag(new ItemTag(OrbListener.NBT_TIER, newTier));
            builtNbt.addTag(new ItemTag(OrbListener.NBT_MODS, gson.toJson(mods)));
            builtNbt.addTag(new ItemTag(OrbListener.NBT_IDENTIFIED, 1));
            if (nbt.hasTag(OrbListener.NBT_POLISH))
                builtNbt.addTag(new ItemTag(OrbListener.NBT_POLISH, nbt.getInteger(OrbListener.NBT_POLISH)));
            result = builtNbt.toItem();
        } else {
            nbt.addTag(new ItemTag(OrbListener.NBT_TIER, newTier));
            nbt.addTag(new ItemTag(OrbListener.NBT_MODS, gson.toJson(mods)));
            nbt.addTag(new ItemTag(OrbListener.NBT_IDENTIFIED, 1));
            result = nbt.toItem();
        }

        OrbListener listener = getListener();
        if (listener != null) listener.updateItemDisplay(result, NBTItem.get(result));
        target.getInventory().setItemInOffHand(result);
        sender.sendMessage(cc("&aTier alterado para " + newTier + ". " + mods.size() + " mods adicionados."));
    }

    private void cmdAugment(CommandSender sender, Player target, ItemStack item, NBTItem nbt, boolean isMMOItem) {
        if (nbt.hasTag(OrbListener.NBT_POLISHED) && nbt.getInteger(OrbListener.NBT_POLISHED) == 1) {
            sender.sendMessage(cc("&cItem polido! Mods travados. Apenas upgrade de tier permitido."));
            return;
        }
        int tier = nbt.hasTag(OrbListener.NBT_TIER) ? nbt.getInteger(OrbListener.NBT_TIER) : 0;
        if (tier != 1) {
            sender.sendMessage(cc("&cItem precisa ser Magico (tier 1)."));
            return;
        }

        String category = isMMOItem ? resolveCategory(nbt.getString("MMOITEMS_ITEM_TYPE"))
                : resolveCategoryFromMaterial(item.getType());
        if (category == null) { sender.sendMessage(cc("&cTipo nao suportado.")); return; }

        OrbListener listener = getListener();
        List<ModifierEngine.RolledModifier> mods = listener != null ? listener.readMods(nbt) : new ArrayList<>();
        if (mods.size() >= 2) {
            sender.sendMessage(cc("&cItem Magico ja tem 2 mods."));
            return;
        }

        ModifierEngine engine = plugin.getModifierEngine();
        int itemLevel = getItemLevel(nbt);
        Set<String> used = mods.stream().map(ModifierEngine.RolledModifier::category)
                .filter(Objects::nonNull).collect(Collectors.toSet());

        ModifierEngine.RolledModifier newMod = engine.rollFromGroup(
                "nemonicorp_" + category + "_positive", itemLevel, used, false);
        if (newMod == null) { sender.sendMessage(cc("&cNao conseguiu rolar mod.")); return; }

        mods.add(newMod);

        ItemStack result;
        if (isMMOItem) {
            LiveMMOItem live = new LiveMMOItem(item);
            for (var entry : newMod.stats().entrySet()) {
                DoubleStat stat = engine.resolveStat(entry.getKey());
                if (stat == null) continue;
                var data = live.getData(stat);
                double cur = (data instanceof DoubleData dd) ? dd.getValue() : 0;
                live.setData(stat, new DoubleData(cur + entry.getValue()));
            }
            ItemStack built = live.newBuilder().build();
            NBTItem builtNbt = NBTItem.get(built);
            preserveTags(nbt, builtNbt);
            builtNbt.addTag(new ItemTag(OrbListener.NBT_MODS, gson.toJson(mods)));
            result = builtNbt.toItem();
        } else {
            nbt.addTag(new ItemTag(OrbListener.NBT_MODS, gson.toJson(mods)));
            result = nbt.toItem();
        }

        if (listener != null) listener.updateItemDisplay(result, NBTItem.get(result));
        target.getInventory().setItemInOffHand(result);
        sender.sendMessage(cc("&aMod adicionado: " + newMod.id()));
    }

    private void cmdExalt(CommandSender sender, Player target, ItemStack item, NBTItem nbt, boolean isMMOItem) {
        if (nbt.hasTag(OrbListener.NBT_POLISHED) && nbt.getInteger(OrbListener.NBT_POLISHED) == 1) {
            sender.sendMessage(cc("&cItem polido! Mods travados. Apenas upgrade de tier permitido."));
            return;
        }
        int tier = nbt.hasTag(OrbListener.NBT_TIER) ? nbt.getInteger(OrbListener.NBT_TIER) : 0;
        if (tier != 2) {
            sender.sendMessage(cc("&cItem precisa ser Raro (tier 2)."));
            return;
        }

        String category = isMMOItem ? resolveCategory(nbt.getString("MMOITEMS_ITEM_TYPE"))
                : resolveCategoryFromMaterial(item.getType());
        if (category == null) { sender.sendMessage(cc("&cTipo nao suportado.")); return; }

        OrbListener listener = getListener();
        List<ModifierEngine.RolledModifier> mods = listener != null ? listener.readMods(nbt) : new ArrayList<>();
        if (mods.size() >= 6) {
            sender.sendMessage(cc("&cItem ja tem 6 mods (maximo)."));
            return;
        }

        ModifierEngine engine = plugin.getModifierEngine();
        int itemLevel = getItemLevel(nbt);
        Set<String> used = mods.stream().map(ModifierEngine.RolledModifier::category)
                .filter(Objects::nonNull).collect(Collectors.toSet());

        ModifierEngine.RolledModifier newMod = engine.rollFromGroup(
                "nemonicorp_" + category + "_positive", itemLevel, used, false);
        if (newMod == null) { sender.sendMessage(cc("&cNao conseguiu rolar mod.")); return; }

        mods.add(newMod);

        ItemStack result;
        if (isMMOItem) {
            LiveMMOItem live = new LiveMMOItem(item);
            for (var entry : newMod.stats().entrySet()) {
                DoubleStat stat = engine.resolveStat(entry.getKey());
                if (stat == null) continue;
                var data = live.getData(stat);
                double cur = (data instanceof DoubleData dd) ? dd.getValue() : 0;
                live.setData(stat, new DoubleData(cur + entry.getValue()));
            }
            ItemStack built = live.newBuilder().build();
            NBTItem builtNbt = NBTItem.get(built);
            preserveTags(nbt, builtNbt);
            builtNbt.addTag(new ItemTag(OrbListener.NBT_MODS, gson.toJson(mods)));
            result = builtNbt.toItem();
        } else {
            nbt.addTag(new ItemTag(OrbListener.NBT_MODS, gson.toJson(mods)));
            result = nbt.toItem();
        }

        if (listener != null) listener.updateItemDisplay(result, NBTItem.get(result));
        target.getInventory().setItemInOffHand(result);
        sender.sendMessage(cc("&aMod adicionado: " + newMod.id()));
    }

    private void cmdChance(CommandSender sender, Player target, ItemStack item, NBTItem nbt, boolean isMMOItem) {
        int tier = nbt.hasTag(OrbListener.NBT_TIER) ? nbt.getInteger(OrbListener.NBT_TIER) : 0;
        if (tier != 0) {
            sender.sendMessage(cc("&cItem precisa ser Comum (tier 0)."));
            return;
        }

        String category = isMMOItem ? resolveCategory(nbt.getString("MMOITEMS_ITEM_TYPE"))
                : resolveCategoryFromMaterial(item.getType());
        if (category == null) { sender.sendMessage(cc("&cTipo nao suportado.")); return; }

        int roll = new Random().nextInt(100);
        int newTier;
        int modCount;
        int maxMods;
        int magic = plugin.getConfig().getInt("chance-orb.magic", 70);
        int rare = plugin.getConfig().getInt("chance-orb.rare", 25);

        if (roll < magic) { newTier = 1; modCount = new Random().nextInt(2) + 1; maxMods = 2; }
        else if (roll < magic + rare) { newTier = 2; modCount = new Random().nextInt(4) + 3; maxMods = 6; }
        else { newTier = 3; modCount = 6; maxMods = 6; }

        ModifierEngine engine = plugin.getModifierEngine();
        int itemLevel = getItemLevel(nbt);
        List<ModifierEngine.RolledModifier> mods;
        if (newTier == 3) {
            mods = engine.rollForUnique(category, itemLevel);
        } else {
            mods = engine.rollForTier(category, modCount, itemLevel, maxMods, newTier);
        }

        ItemStack result;
        if (isMMOItem) {
            LiveMMOItem live = new LiveMMOItem(item);
            for (var m : mods) {
                for (var entry : m.stats().entrySet()) {
                    DoubleStat stat = engine.resolveStat(entry.getKey());
                    if (stat == null) continue;
                    var data = live.getData(stat);
                    double cur = (data instanceof DoubleData dd) ? dd.getValue() : 0;
                    live.setData(stat, new DoubleData(cur + entry.getValue()));
                }
            }
            ItemStack built = live.newBuilder().build();
            NBTItem builtNbt = NBTItem.get(built);
            builtNbt.addTag(new ItemTag(OrbListener.NBT_TIER, newTier));
            builtNbt.addTag(new ItemTag(OrbListener.NBT_MODS, gson.toJson(mods)));
            builtNbt.addTag(new ItemTag(OrbListener.NBT_IDENTIFIED, 1));
            if (nbt.hasTag(OrbListener.NBT_POLISH))
                builtNbt.addTag(new ItemTag(OrbListener.NBT_POLISH, nbt.getInteger(OrbListener.NBT_POLISH)));
            result = builtNbt.toItem();
        } else {
            nbt.addTag(new ItemTag(OrbListener.NBT_TIER, newTier));
            nbt.addTag(new ItemTag(OrbListener.NBT_MODS, gson.toJson(mods)));
            nbt.addTag(new ItemTag(OrbListener.NBT_IDENTIFIED, 1));
            result = nbt.toItem();
        }

        OrbListener listener = getListener();
        if (listener != null) listener.updateItemDisplay(result, NBTItem.get(result));
        target.getInventory().setItemInOffHand(result);

        String tierName = listener != null ? listener.getTierName(newTier) : "Tier " + newTier;
        sender.sendMessage(cc("&aMoeda da Sorte aplicada. Novo tier: " + tierName));
    }

    private void cmdAnnul(CommandSender sender, Player target, ItemStack item, NBTItem nbt, boolean isMMOItem) {
        if (nbt.hasTag(OrbListener.NBT_POLISHED) && nbt.getInteger(OrbListener.NBT_POLISHED) == 1) {
            sender.sendMessage(cc("&cItem polido! Mods travados. Apenas upgrade de tier permitido."));
            return;
        }
        OrbListener listener = getListener();
        List<ModifierEngine.RolledModifier> mods = listener != null ? listener.readMods(nbt) : new ArrayList<>();
        if (mods.isEmpty()) {
            sender.sendMessage(cc("&cItem nao tem mods."));
            return;
        }

        int idx = new Random().nextInt(mods.size());
        ModifierEngine.RolledModifier removed = mods.get(idx);

        mods.remove(idx);

        ItemStack result;
        if (isMMOItem) {
            LiveMMOItem live = new LiveMMOItem(item);
            ModifierEngine engine = plugin.getModifierEngine();
            for (var entry : removed.stats().entrySet()) {
                DoubleStat stat = engine.resolveStat(entry.getKey());
                if (stat == null) continue;
                var data = live.getData(stat);
                double cur = (data instanceof DoubleData dd) ? dd.getValue() : 0;
                live.setData(stat, new DoubleData(cur - entry.getValue()));
            }
            ItemStack built = live.newBuilder().build();
            NBTItem builtNbt = NBTItem.get(built);
            preserveTags(nbt, builtNbt);
            builtNbt.addTag(new ItemTag(OrbListener.NBT_MODS, gson.toJson(mods)));
            result = builtNbt.toItem();
        } else {
            nbt.addTag(new ItemTag(OrbListener.NBT_MODS, gson.toJson(mods)));
            result = nbt.toItem();
        }

        if (listener != null) listener.updateItemDisplay(result, NBTItem.get(result));
        target.getInventory().setItemInOffHand(result);
        sender.sendMessage(cc("&aMod removido: " + removed.id()));
    }

    private void cmdSetTier(CommandSender sender, Player target, NBTItem nbt, String[] args) {
        if (args.length < 3) {
            sender.sendMessage(cc("&cUso: /nemonicorb set-tier <jogador> <0-3>"));
            return;
        }
        int newTier;
        try { newTier = Integer.parseInt(args[2]); } catch (NumberFormatException e) {
            sender.sendMessage(cc("&cTier invalido. Use 0-3.")); return;
        }
        if (newTier < 0 || newTier > 3) {
            sender.sendMessage(cc("&cTier invalido. Use 0-3.")); return;
        }

        ItemStack item = target.getInventory().getItemInOffHand();
        boolean isMMOItem = nbt.hasType();

        if (newTier == 0) {
            // Tier 0 (Comum): remove mods
            nbt.addTag(new ItemTag(OrbListener.NBT_TIER, 0));
            nbt.addTag(new ItemTag(OrbListener.NBT_MODS, "[]"));
            nbt.addTag(new ItemTag(OrbListener.NBT_IDENTIFIED, 1));
            ItemStack updated = nbt.toItem();
            OrbListener listener = getListener();
            if (listener != null) listener.updateItemDisplay(updated, NBTItem.get(updated));
            target.getInventory().setItemInOffHand(updated);
            sender.sendMessage(cc("&aTier definido para Comum (0). Mods removidos."));
            return;
        }

        // Tiers 1-3: rolar modificadores
        String category = isMMOItem ? resolveCategory(nbt.getString("MMOITEMS_ITEM_TYPE"))
                : resolveCategoryFromMaterial(item.getType());
        if (category == null) {
            sender.sendMessage(cc("&cTipo nao suportado para rolar mods."));
            return;
        }

        ModifierEngine engine = plugin.getModifierEngine();
        int itemLevel = getItemLevel(nbt);
        List<ModifierEngine.RolledModifier> mods;

        if (newTier == 3) {
            // Unico: 6 mods, valores dobrados, 2 mods buffados x2
            mods = engine.rollForUnique(category, itemLevel);
        } else if (newTier == 2) {
            int modCount = new Random().nextInt(4) + 3; // 3-6
            mods = engine.rollForTier(category, modCount, itemLevel, 6, newTier);
        } else {
            int modCount = new Random().nextInt(2) + 1; // 1-2
            mods = engine.rollForTier(category, modCount, itemLevel, 2, newTier);
        }

        ItemStack result;
        if (isMMOItem) {
            LiveMMOItem live = new LiveMMOItem(item);
            for (var m : mods) {
                for (var entry : m.stats().entrySet()) {
                    DoubleStat stat = engine.resolveStat(entry.getKey());
                    if (stat == null) continue;
                    var data = live.getData(stat);
                    double cur = (data instanceof DoubleData dd) ? dd.getValue() : 0;
                    live.setData(stat, new DoubleData(cur + entry.getValue()));
                }
            }
            ItemStack built = live.newBuilder().build();
            NBTItem builtNbt = NBTItem.get(built);
            builtNbt.addTag(new ItemTag(OrbListener.NBT_TIER, newTier));
            builtNbt.addTag(new ItemTag(OrbListener.NBT_MODS, gson.toJson(mods)));
            builtNbt.addTag(new ItemTag(OrbListener.NBT_IDENTIFIED, 1));
            if (nbt.hasTag(OrbListener.NBT_POLISH))
                builtNbt.addTag(new ItemTag(OrbListener.NBT_POLISH, nbt.getInteger(OrbListener.NBT_POLISH)));
            result = builtNbt.toItem();
        } else {
            nbt.addTag(new ItemTag(OrbListener.NBT_TIER, newTier));
            nbt.addTag(new ItemTag(OrbListener.NBT_MODS, gson.toJson(mods)));
            nbt.addTag(new ItemTag(OrbListener.NBT_IDENTIFIED, 1));
            result = nbt.toItem();
        }

        OrbListener listener = getListener();
        if (listener != null) listener.updateItemDisplay(result, NBTItem.get(result));
        target.getInventory().setItemInOffHand(result);

        String tierName = listener != null ? listener.getTierName(newTier) : "Tier " + newTier;
        sender.sendMessage(cc("&aTier definido para " + tierName + " (" + newTier + "). " + mods.size() + " mods rolados."));
    }

    private void cmdSetId(CommandSender sender, Player target, NBTItem nbt, String[] args) {
        if (args.length < 3) {
            sender.sendMessage(cc("&cUso: /nemonicorb set-id <jogador> <0|1>"));
            return;
        }
        int val;
        try { val = Integer.parseInt(args[2]); } catch (NumberFormatException e) {
            sender.sendMessage(cc("&cValor invalido. Use 0 ou 1.")); return;
        }
        if (val != 0 && val != 1) {
            sender.sendMessage(cc("&cValor invalido. Use 0 (nao identificado) ou 1 (identificado).")); return;
        }

        nbt.addTag(new ItemTag(OrbListener.NBT_IDENTIFIED, val));
        ItemStack updated = nbt.toItem();
        OrbListener listener = getListener();
        if (listener != null) listener.updateItemDisplay(updated, NBTItem.get(updated));
        target.getInventory().setItemInOffHand(updated);

        String status = val == 1 ? "identificado" : "nao identificado";
        sender.sendMessage(cc("&aItem marcado como " + status + "."));
    }

    private void cmdInfo(CommandSender sender, NBTItem nbt, ItemStack item) {
        int tier = nbt.hasTag(OrbListener.NBT_TIER) ? nbt.getInteger(OrbListener.NBT_TIER) : 0;
        boolean identified = !nbt.hasTag(OrbListener.NBT_IDENTIFIED) || nbt.getInteger(OrbListener.NBT_IDENTIFIED) == 1;
        int polish = nbt.hasTag(OrbListener.NBT_POLISH) ? nbt.getInteger(OrbListener.NBT_POLISH) : 0;
        int maxPolish = plugin.getConfig().getInt("max-polish", 5);

        OrbListener listener = getListener();
        List<ModifierEngine.RolledModifier> mods = listener != null ? listener.readMods(nbt) : new ArrayList<>();

        boolean isMMOItem = nbt.hasType();
        String type = isMMOItem ? nbt.getString("MMOITEMS_ITEM_TYPE") : item.getType().name();
        String id = isMMOItem ? nbt.getString("MMOITEMS_ITEM_ID") : "VANILLA";
        String category = isMMOItem ? resolveCategory(type) : resolveCategoryFromMaterial(item.getType());
        String tierName = listener != null ? listener.getTierName(tier) : "Tier " + tier;
        String tierColor = listener != null ? listener.getTierColor(tier) : "&7";

        sender.sendMessage(cc("&6=== NemonicOrb v2.1 Info ==="));
        sender.sendMessage(cc("&7Item: &e" + type + " / " + id + (isMMOItem ? "" : " &8(vanilla)")));
        sender.sendMessage(cc("&7Tier: " + tierColor + tierName + " &8(" + tier + ")"));
        sender.sendMessage(cc("&7Identificado: " + (identified ? "&aSim" : "&cNao")));
        sender.sendMessage(cc("&7Polimento: &e" + polish + "&7/&e" + maxPolish));
        sender.sendMessage(cc("&7Categoria: &e" + (category != null ? category : "N/A")));

        sender.sendMessage(cc("&7Mods: &e" + mods.size()));

        if (mods.isEmpty()) {
            sender.sendMessage(cc("&7  (nenhum mod)"));
        } else {
            for (var mod : mods) {
                StringBuilder sb = new StringBuilder();
                for (var e : mod.stats().entrySet()) {
                    if (!sb.isEmpty()) sb.append(", ");
                    sb.append(e.getKey()).append(": ").append(String.format("%.2f", e.getValue()));
                }
                sender.sendMessage(cc("&7  - &e" + mod.id() + " &8(" + sb + ")"));
            }
        }
    }

    // ── Utilitarios ──

    private OrbListener getListener() {
        var listeners = org.bukkit.event.HandlerList.getRegisteredListeners(plugin);
        for (var rl : listeners) {
            if (rl.getListener() instanceof OrbListener ol) return ol;
        }
        return null;
    }

    private void preserveTags(NBTItem src, NBTItem dst) {
        if (src.hasTag(OrbListener.NBT_TIER)) dst.addTag(new ItemTag(OrbListener.NBT_TIER, src.getInteger(OrbListener.NBT_TIER)));
        if (src.hasTag(OrbListener.NBT_MODS)) dst.addTag(new ItemTag(OrbListener.NBT_MODS, src.getString(OrbListener.NBT_MODS)));
        if (src.hasTag(OrbListener.NBT_IDENTIFIED)) dst.addTag(new ItemTag(OrbListener.NBT_IDENTIFIED, src.getInteger(OrbListener.NBT_IDENTIFIED)));
        if (src.hasTag(OrbListener.NBT_POLISH)) dst.addTag(new ItemTag(OrbListener.NBT_POLISH, src.getInteger(OrbListener.NBT_POLISH)));
    }

    private int getItemLevel(NBTItem nbt) {
        if (nbt.hasTag("MMOITEMS_ITEM_LEVEL")) return nbt.getInteger("MMOITEMS_ITEM_LEVEL");
        return 1;
    }

    private String resolveCategory(String type) {
        if (type == null) return null;
        String upper = type.toUpperCase();
        for (String cat : List.of("weapon", "armor", "accessory")) {
            List<String> types = plugin.getConfig().getStringList("type-categories." + cat);
            if (types.stream().anyMatch(t -> t.equalsIgnoreCase(upper))) return cat;
        }
        return null;
    }

    private String resolveCategoryFromMaterial(Material material) {
        if (material == null) return null;
        String name = material.name();
        if (name.contains("SWORD") || name.contains("AXE") || name.contains("BOW")
                || name.contains("CROSSBOW") || name.contains("TRIDENT") || name.contains("MACE")) {
            return "weapon";
        }
        if (name.contains("HELMET") || name.contains("CHESTPLATE") || name.contains("LEGGINGS")
                || name.contains("BOOTS") || name.equals("TURTLE_HELMET")) {
            return "armor";
        }
        return null;
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage(cc("&6=== NemonicOrb v2 Admin ==="));
        sender.sendMessage(cc("&e/norb identify <jogador> &7- Pergaminho de Identificacao"));
        sender.sendMessage(cc("&e/norb polish <jogador> &7- Oleo de Polimento (+qualidade)"));
        sender.sendMessage(cc("&e/norb transmute <jogador> &7- Pedra de Encantamento (Comum → Magico)"));
        sender.sendMessage(cc("&e/norb augment <jogador> &7- Pedra de Reforco (+1 mod em Magico)"));
        sender.sendMessage(cc("&e/norb regal <jogador> &7- Runa Nobre (Magico → Raro)"));
        sender.sendMessage(cc("&e/norb alchemy <jogador> &7- Pedra de Refinamento (Comum → Raro)"));
        sender.sendMessage(cc("&e/norb exalt <jogador> &7- Runa de Poder (+1 mod em Raro)"));
        sender.sendMessage(cc("&e/norb chance <jogador> &7- Moeda da Sorte (Tier aleatorio)"));
        sender.sendMessage(cc("&e/norb annul <jogador> &7- Pedra Corrosiva (Remove 1 mod)"));
        sender.sendMessage(cc("&e/norb set-tier <jogador> <0-3> &7- Define tier + rola mods"));
        sender.sendMessage(cc("&e/norb set-id <jogador> <0|1> &7- Define identificacao"));
        sender.sendMessage(cc("&e/norb info <jogador> &7- Info do item"));
        sender.sendMessage(cc("&e/norb audit [self|hand|equip] &7- Auditoria de status/mods"));
        sender.sendMessage(cc("&e/norb giveguide <jogador> [classe] &7- Entrega guia geral ou de classe"));
        sender.sendMessage(cc("&e/norb reload &7- Recarrega config"));
    }

    private void cmdAudit(CommandSender sender, String[] args) {
        if (!(sender instanceof Player p)) {
            sender.sendMessage(cc("&cUse este comando em jogo."));
            return;
        }
        String mode = args.length >= 2 ? args[1].toLowerCase(Locale.ROOT) : "self";
        if (!mode.equals("self") && !mode.equals("hand") && !mode.equals("equip")) mode = "self";
        var lines = plugin.getModifierAuditService().auditPlayer(p, mode);
        for (String line : lines) {
            sender.sendMessage(cc(line));
        }
    }

    /**
     * /norb publicwarp <set|del|list|tp> [args]
     *  - set <nome>:    cria warp publica na posicao do admin (precisa nemonicorb.admin)
     *  - del <nome>:    remove warp publica (precisa nemonicorb.admin)
     *  - list:          lista todas as warps (qualquer um)
     *  - tp <nome>:     teleporta para warp (qualquer jogador, custo 40 mana + 5/passageiro)
     */
    private void cmdPublicWarp(CommandSender sender, String[] args) {
        PublicWarpManager mgr = plugin.getPublicWarpManager();
        if (mgr == null) {
            sender.sendMessage(cc("&cSistema de warps publicas nao inicializado."));
            return;
        }
        if (args.length < 2) {
            sender.sendMessage(cc("&eUso: &f/norb publicwarp <set|del|list|tp> [nome]"));
            return;
        }
        String sub = args[1].toLowerCase();

        switch (sub) {
            case "list" -> {
                var all = mgr.all();
                if (all.isEmpty()) {
                    sender.sendMessage(cc("&7Nenhuma warp publica registrada."));
                    return;
                }
                sender.sendMessage(cc("&6=== Warps Publicas (" + all.size() + ") ==="));
                for (PublicWarpManager.PublicWarp w : all) {
                    org.bukkit.Location loc = w.location();
                    sender.sendMessage(cc("&e" + w.name() + " &7- &f" + loc.getWorld().getName()
                            + " &7(" + (int) loc.getX() + ", " + (int) loc.getY() + ", " + (int) loc.getZ() + ")"
                            + " &8por " + w.creator()));
                }
            }
            case "set" -> {
                if (!sender.hasPermission("nemonicorb.admin")) {
                    sender.sendMessage(cc("&cApenas administradores podem registrar warps publicas."));
                    return;
                }
                if (!(sender instanceof Player p)) {
                    sender.sendMessage(cc("&cApenas jogadores podem usar 'set' (precisa de localizacao)."));
                    return;
                }
                if (args.length < 3) {
                    sender.sendMessage(cc("&eUso: &f/norb publicwarp set <nome>"));
                    return;
                }
                String name = args[2];
                mgr.setWarp(name, p.getLocation(), p.getName());
                p.sendMessage(cc("&a[Warp] Warp publica &e" + name + "&a registrada na sua posicao."));
                p.sendMessage(cc("&7Coloque um bloco BEACON neste local como sinalizador visual."));
            }
            case "del", "delete", "remove" -> {
                if (!sender.hasPermission("nemonicorb.admin")) {
                    sender.sendMessage(cc("&cApenas administradores podem remover warps publicas."));
                    return;
                }
                if (args.length < 3) {
                    sender.sendMessage(cc("&eUso: &f/norb publicwarp del <nome>"));
                    return;
                }
                String name = args[2];
                if (mgr.removeWarp(name)) {
                    sender.sendMessage(cc("&a[Warp] Warp '" + name + "' removida."));
                } else {
                    sender.sendMessage(cc("&c[Warp] Warp '" + name + "' nao existe."));
                }
            }
            case "tp", "teleport" -> {
                if (!(sender instanceof Player p)) {
                    sender.sendMessage(cc("&cApenas jogadores podem se teleportar."));
                    return;
                }
                if (args.length < 3) {
                    sender.sendMessage(cc("&eUso: &f/norb publicwarp tp <nome>"));
                    return;
                }
                mgr.startTeleport(p, args[2]);
            }
            default -> sender.sendMessage(cc("&eUso: &f/norb publicwarp <set|del|list|tp> [nome]"));
        }
    }

    private void cmdGiveGuide(CommandSender sender, Player target, String[] args) {
        String classKey = args.length >= 3 ? args[2].toLowerCase(Locale.ROOT) : "";
        boolean welcomeGuide = classKey.isBlank() || classKey.equals("geral") || classKey.equals("welcome");
        ItemStack guide = welcomeGuide
                ? plugin.getGuideManager().createWelcomeBook()
                : plugin.getGuideManager().createClassBook(classKey);

        if (guide == null) {
            sender.sendMessage(cc("&cGuia invalido. Use: geral, alquimista, ferreiro, mercador ou guerreiro."));
            return;
        }

        target.getInventory().addItem(guide);
        String guideName = welcomeGuide ? "Cronicas de Embati" : "guia de " + classKey;
        sender.sendMessage(cc("&a" + guideName + " entregue para " + target.getName() + "."));
    }

    private void msg(CommandSender sender, String key) {
        String prefix = plugin.getConfig().getString("messages.prefix", "");
        String raw = plugin.getConfig().getString("messages." + key, key);
        sender.sendMessage(cc(prefix + raw));
    }

    private String cc(String s) {
        return ChatColor.translateAlternateColorCodes('&', s);
    }

    // ── Tab Completer ──

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String label, String[] args) {
        if (args.length == 1) {
            return SUBCOMMANDS.stream()
                    .filter(s -> s.startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        }
        if (args.length == 2 && !args[0].equalsIgnoreCase("reload")) {
            if (args[0].equalsIgnoreCase("audit")) return List.of("self", "hand", "equip");
            return null; // Nomes de jogadores online
        }
        if (args.length == 3) {
            String a = args[0].toLowerCase();
            if (a.equals("set-tier")) return List.of("0", "1", "2", "3");
            if (a.equals("set-id")) return List.of("0", "1");
            if (a.equals("giveguide")) return List.of("geral", "alquimista", "ferreiro", "mercador", "guerreiro");
        }
        return Collections.emptyList();
    }
}
