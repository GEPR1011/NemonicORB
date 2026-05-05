/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.gson.Gson
 *  io.lumine.mythic.lib.api.item.ItemTag
 *  io.lumine.mythic.lib.api.item.NBTItem
 *  net.Indyuce.mmoitems.api.item.mmoitem.LiveMMOItem
 *  net.Indyuce.mmoitems.stat.data.DoubleData
 *  net.Indyuce.mmoitems.stat.data.type.StatData
 *  net.Indyuce.mmoitems.stat.type.DoubleStat
 *  net.Indyuce.mmoitems.stat.type.ItemStat
 *  org.bukkit.Bukkit
 *  org.bukkit.ChatColor
 *  org.bukkit.Location
 *  org.bukkit.Material
 *  org.bukkit.command.Command
 *  org.bukkit.command.CommandExecutor
 *  org.bukkit.command.CommandSender
 *  org.bukkit.command.TabCompleter
 *  org.bukkit.configuration.ConfigurationSection
 *  org.bukkit.entity.Player
 *  org.bukkit.event.HandlerList
 *  org.bukkit.event.Listener
 *  org.bukkit.inventory.ItemStack
 *  org.bukkit.plugin.Plugin
 *  org.bukkit.plugin.RegisteredListener
 */
package com.nemonicorp.orbs;

import com.google.gson.Gson;
import com.nemonicorp.orbs.ModifierEngine;
import com.nemonicorp.orbs.NemonicOrbPlugin;
import com.nemonicorp.orbs.OrbListener;
import com.nemonicorp.orbs.PublicWarpManager;
import io.lumine.mythic.lib.api.item.ItemTag;
import io.lumine.mythic.lib.api.item.NBTItem;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;
import net.Indyuce.mmoitems.api.item.mmoitem.LiveMMOItem;
import net.Indyuce.mmoitems.stat.data.DoubleData;
import net.Indyuce.mmoitems.stat.data.type.StatData;
import net.Indyuce.mmoitems.stat.type.DoubleStat;
import net.Indyuce.mmoitems.stat.type.ItemStat;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.RegisteredListener;

public class OrbCommand
implements CommandExecutor,
TabCompleter {
    private static final List<String> SUBCOMMANDS = List.of("identify", "polish", "transmute", "augment", "regal", "alchemy", "exalt", "chance", "annul", "set-tier", "set-id", "info", "audit", "giveguide", "publicwarp", "reload");
    private final NemonicOrbPlugin plugin;
    private final Gson gson;

    public OrbCommand(NemonicOrbPlugin plugin) {
        this.plugin = plugin;
        this.gson = plugin.getModifierEngine().getGson();
    }

    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (args.length == 0) {
            this.sendHelp(sender);
            return true;
        }
        String action = args[0].toLowerCase();
        if (action.equals("reload")) {
            this.plugin.reloadConfig();
            this.plugin.getModifierEngine().load();
            this.msg(sender, "reload-success");
            return true;
        }
        if (action.equals("audit")) {
            this.cmdAudit(sender, args);
            return true;
        }
        if (action.equals("publicwarp")) {
            this.cmdPublicWarp(sender, args);
            return true;
        }
        if (args.length < 2) {
            sender.sendMessage(this.cc("&cUso: /" + label + " <acao> <jogador>"));
            return true;
        }
        Player target = Bukkit.getPlayer((String)args[1]);
        if (target == null || !target.isOnline()) {
            sender.sendMessage(this.cc("&cJogador nao encontrado: " + args[1]));
            return true;
        }
        if (action.equals("giveguide")) {
            this.cmdGiveGuide(sender, target);
            return true;
        }
        ItemStack offHand = target.getInventory().getItemInOffHand();
        if (offHand.getType() == Material.AIR) {
            sender.sendMessage(this.cc("&cO jogador nao tem item na mao secundaria!"));
            return true;
        }
        NBTItem nbt = NBTItem.get((ItemStack)offHand);
        boolean isMMOItem = nbt.hasType();
        switch (action) {
            case "identify": {
                this.cmdIdentify(sender, target, offHand, nbt);
                break;
            }
            case "polish": {
                this.cmdPolish(sender, target, offHand, nbt, isMMOItem);
                break;
            }
            case "transmute": {
                this.cmdTier(sender, target, offHand, nbt, 0, 1, isMMOItem);
                break;
            }
            case "augment": {
                this.cmdAugment(sender, target, offHand, nbt, isMMOItem);
                break;
            }
            case "regal": {
                this.cmdTier(sender, target, offHand, nbt, 1, 2, isMMOItem);
                break;
            }
            case "alchemy": {
                this.cmdTier(sender, target, offHand, nbt, 0, 2, isMMOItem);
                break;
            }
            case "exalt": {
                this.cmdExalt(sender, target, offHand, nbt, isMMOItem);
                break;
            }
            case "chance": {
                this.cmdChance(sender, target, offHand, nbt, isMMOItem);
                break;
            }
            case "annul": {
                this.cmdAnnul(sender, target, offHand, nbt, isMMOItem);
                break;
            }
            case "set-tier": {
                this.cmdSetTier(sender, target, nbt, args);
                break;
            }
            case "set-id": {
                this.cmdSetId(sender, target, nbt, args);
                break;
            }
            case "info": {
                this.cmdInfo(sender, nbt, offHand);
                break;
            }
            case "giveguide": {
                this.cmdGiveGuide(sender, target);
                break;
            }
            default: {
                this.sendHelp(sender);
            }
        }
        return true;
    }

    private void cmdIdentify(CommandSender sender, Player target, ItemStack item, NBTItem nbt) {
        nbt.addTag(new ItemTag[]{new ItemTag("NEMONICORB_IDENTIFIED", (Object)1)});
        ItemStack updated = nbt.toItem();
        OrbListener listener = this.getListener();
        if (listener != null) {
            listener.updateItemDisplay(updated, NBTItem.get((ItemStack)updated));
        }
        target.getInventory().setItemInOffHand(updated);
        sender.sendMessage(this.cc("&aItem identificado para " + target.getName() + "."));
    }

    private void cmdPolish(CommandSender sender, Player target, ItemStack item, NBTItem nbt, boolean isMMOItem) {
        ItemStack result;
        int current;
        int maxPolish = this.plugin.getConfig().getInt("max-polish", 5);
        int n = current = nbt.hasTag("NEMONICORB_POLISH") ? nbt.getInteger("NEMONICORB_POLISH") : 0;
        if (current >= maxPolish) {
            sender.sendMessage(this.cc("&cItem ja polido ao maximo (" + maxPolish + ")."));
            return;
        }
        if (isMMOItem) {
            ConfigurationSection polishSec = this.plugin.getConfig().getConfigurationSection("polish-percent");
            if (polishSec == null) {
                return;
            }
            LiveMMOItem live = new LiveMMOItem(item);
            ModifierEngine engine = this.plugin.getModifierEngine();
            int count = 0;
            for (String statId : polishSec.getKeys(false)) {
                DoubleData dd;
                StatData data;
                double pct = polishSec.getDouble(statId) / 100.0;
                DoubleStat stat = engine.resolveStat(statId);
                if (stat == null || !((data = live.getData((ItemStat)stat)) instanceof DoubleData) || (dd = (DoubleData)data).getValue() == 0.0) continue;
                live.setData((ItemStat)stat, (StatData)new DoubleData((double)Math.round(dd.getValue() * (1.0 + pct) * 100.0) / 100.0));
                ++count;
            }
            ItemStack built = live.newBuilder().build();
            NBTItem builtNbt = NBTItem.get((ItemStack)built);
            this.preserveTags(nbt, builtNbt);
            builtNbt.addTag(new ItemTag[]{new ItemTag("NEMONICORB_POLISH", (Object)(current + 1))});
            result = builtNbt.toItem();
        } else {
            nbt.addTag(new ItemTag[]{new ItemTag("NEMONICORB_POLISH", (Object)(current + 1))});
            result = nbt.toItem();
        }
        OrbListener listener = this.getListener();
        if (listener != null) {
            listener.updateItemDisplay(result, NBTItem.get((ItemStack)result));
        }
        target.getInventory().setItemInOffHand(result);
        sender.sendMessage(this.cc("&aPolimento aplicado. (" + (current + 1) + "/" + maxPolish + ")"));
    }

    private void cmdTier(CommandSender sender, Player target, ItemStack item, NBTItem nbt, int requiredTier, int newTier, boolean isMMOItem) {
        ItemStack result;
        int maxMods;
        int modCount;
        int currentTier;
        int n = currentTier = nbt.hasTag("NEMONICORB_TIER") ? nbt.getInteger("NEMONICORB_TIER") : 0;
        if (currentTier != requiredTier) {
            sender.sendMessage(this.cc("&cItem precisa ser tier " + requiredTier + ". Atual: " + currentTier));
            return;
        }
        String category = isMMOItem ? this.resolveCategory(nbt.getString("MMOITEMS_ITEM_TYPE")) : this.resolveCategoryFromMaterial(item.getType());
        if (category == null) {
            sender.sendMessage(this.cc("&cTipo nao suportado."));
            return;
        }
        ModifierEngine engine = this.plugin.getModifierEngine();
        int itemLevel = this.getItemLevel(nbt);
        if (newTier == 1) {
            modCount = new Random().nextInt(2) + 1;
            maxMods = 2;
        } else {
            modCount = new Random().nextInt(4) + 3;
            maxMods = 6;
        }
        List<ModifierEngine.RolledModifier> mods = engine.rollForTier(category, modCount, itemLevel, maxMods);
        if (isMMOItem) {
            LiveMMOItem live = new LiveMMOItem(item);
            for (ModifierEngine.RolledModifier m : mods) {
                for (Map.Entry<String, Double> entry : m.stats().entrySet()) {
                    double d;
                    DoubleStat stat = engine.resolveStat(entry.getKey());
                    if (stat == null) continue;
                    StatData data = live.getData((ItemStat)stat);
                    if (data instanceof DoubleData) {
                        DoubleData dd = (DoubleData)data;
                        d = dd.getValue();
                    } else {
                        d = 0.0;
                    }
                    double cur = d;
                    live.setData((ItemStat)stat, (StatData)new DoubleData(cur + entry.getValue()));
                }
            }
            ItemStack built = live.newBuilder().build();
            NBTItem builtNbt = NBTItem.get((ItemStack)built);
            builtNbt.addTag(new ItemTag[]{new ItemTag("NEMONICORB_TIER", (Object)newTier)});
            builtNbt.addTag(new ItemTag[]{new ItemTag("NEMONICORB_MODS", (Object)this.gson.toJson(mods))});
            builtNbt.addTag(new ItemTag[]{new ItemTag("NEMONICORB_IDENTIFIED", (Object)1)});
            if (nbt.hasTag("NEMONICORB_POLISH")) {
                builtNbt.addTag(new ItemTag[]{new ItemTag("NEMONICORB_POLISH", (Object)nbt.getInteger("NEMONICORB_POLISH"))});
            }
            result = builtNbt.toItem();
        } else {
            nbt.addTag(new ItemTag[]{new ItemTag("NEMONICORB_TIER", (Object)newTier)});
            nbt.addTag(new ItemTag[]{new ItemTag("NEMONICORB_MODS", (Object)this.gson.toJson(mods))});
            nbt.addTag(new ItemTag[]{new ItemTag("NEMONICORB_IDENTIFIED", (Object)1)});
            result = nbt.toItem();
        }
        OrbListener listener = this.getListener();
        if (listener != null) {
            listener.updateItemDisplay(result, NBTItem.get((ItemStack)result));
        }
        target.getInventory().setItemInOffHand(result);
        sender.sendMessage(this.cc("&aTier alterado para " + newTier + ". " + mods.size() + " mods adicionados."));
    }

    private void cmdAugment(CommandSender sender, Player target, ItemStack item, NBTItem nbt, boolean isMMOItem) {
        ItemStack result;
        List<Object> mods;
        String category;
        int tier;
        if (nbt.hasTag("NEMONICORB_POLISHED") && nbt.getInteger("NEMONICORB_POLISHED") == 1) {
            sender.sendMessage(this.cc("&cItem polido! Mods travados. Apenas upgrade de tier permitido."));
            return;
        }
        int n = tier = nbt.hasTag("NEMONICORB_TIER") ? nbt.getInteger("NEMONICORB_TIER") : 0;
        if (tier != 1) {
            sender.sendMessage(this.cc("&cItem precisa ser Magico (tier 1)."));
            return;
        }
        String string = category = isMMOItem ? this.resolveCategory(nbt.getString("MMOITEMS_ITEM_TYPE")) : this.resolveCategoryFromMaterial(item.getType());
        if (category == null) {
            sender.sendMessage(this.cc("&cTipo nao suportado."));
            return;
        }
        OrbListener listener = this.getListener();
        List<Object> list = mods = listener != null ? listener.readMods(nbt) : new ArrayList();
        if (mods.size() >= 2) {
            sender.sendMessage(this.cc("&cItem Magico ja tem 2 mods."));
            return;
        }
        ModifierEngine engine = this.plugin.getModifierEngine();
        int itemLevel = this.getItemLevel(nbt);
        Set<String> used = mods.stream().map(ModifierEngine.RolledModifier::category).filter(Objects::nonNull).collect(Collectors.toSet());
        ModifierEngine.RolledModifier newMod = engine.rollFromGroup("nemonicorp_" + category + "_positive", itemLevel, used, false);
        if (newMod == null) {
            sender.sendMessage(this.cc("&cNao conseguiu rolar mod."));
            return;
        }
        mods.add(newMod);
        if (isMMOItem) {
            LiveMMOItem live = new LiveMMOItem(item);
            for (Map.Entry<String, Double> entry : newMod.stats().entrySet()) {
                double d;
                DoubleStat stat = engine.resolveStat(entry.getKey());
                if (stat == null) continue;
                StatData data = live.getData((ItemStat)stat);
                if (data instanceof DoubleData) {
                    DoubleData dd = (DoubleData)data;
                    d = dd.getValue();
                } else {
                    d = 0.0;
                }
                double cur = d;
                live.setData((ItemStat)stat, (StatData)new DoubleData(cur + entry.getValue()));
            }
            ItemStack built = live.newBuilder().build();
            NBTItem builtNbt = NBTItem.get((ItemStack)built);
            this.preserveTags(nbt, builtNbt);
            builtNbt.addTag(new ItemTag[]{new ItemTag("NEMONICORB_MODS", (Object)this.gson.toJson(mods))});
            result = builtNbt.toItem();
        } else {
            nbt.addTag(new ItemTag[]{new ItemTag("NEMONICORB_MODS", (Object)this.gson.toJson(mods))});
            result = nbt.toItem();
        }
        if (listener != null) {
            listener.updateItemDisplay(result, NBTItem.get((ItemStack)result));
        }
        target.getInventory().setItemInOffHand(result);
        sender.sendMessage(this.cc("&aMod adicionado: " + newMod.id()));
    }

    private void cmdExalt(CommandSender sender, Player target, ItemStack item, NBTItem nbt, boolean isMMOItem) {
        ItemStack result;
        List<Object> mods;
        String category;
        int tier;
        if (nbt.hasTag("NEMONICORB_POLISHED") && nbt.getInteger("NEMONICORB_POLISHED") == 1) {
            sender.sendMessage(this.cc("&cItem polido! Mods travados. Apenas upgrade de tier permitido."));
            return;
        }
        int n = tier = nbt.hasTag("NEMONICORB_TIER") ? nbt.getInteger("NEMONICORB_TIER") : 0;
        if (tier != 2) {
            sender.sendMessage(this.cc("&cItem precisa ser Raro (tier 2)."));
            return;
        }
        String string = category = isMMOItem ? this.resolveCategory(nbt.getString("MMOITEMS_ITEM_TYPE")) : this.resolveCategoryFromMaterial(item.getType());
        if (category == null) {
            sender.sendMessage(this.cc("&cTipo nao suportado."));
            return;
        }
        OrbListener listener = this.getListener();
        List<Object> list = mods = listener != null ? listener.readMods(nbt) : new ArrayList();
        if (mods.size() >= 6) {
            sender.sendMessage(this.cc("&cItem ja tem 6 mods (maximo)."));
            return;
        }
        ModifierEngine engine = this.plugin.getModifierEngine();
        int itemLevel = this.getItemLevel(nbt);
        Set<String> used = mods.stream().map(ModifierEngine.RolledModifier::category).filter(Objects::nonNull).collect(Collectors.toSet());
        ModifierEngine.RolledModifier newMod = engine.rollFromGroup("nemonicorp_" + category + "_positive", itemLevel, used, false);
        if (newMod == null) {
            sender.sendMessage(this.cc("&cNao conseguiu rolar mod."));
            return;
        }
        mods.add(newMod);
        if (isMMOItem) {
            LiveMMOItem live = new LiveMMOItem(item);
            for (Map.Entry<String, Double> entry : newMod.stats().entrySet()) {
                double d;
                DoubleStat stat = engine.resolveStat(entry.getKey());
                if (stat == null) continue;
                StatData data = live.getData((ItemStat)stat);
                if (data instanceof DoubleData) {
                    DoubleData dd = (DoubleData)data;
                    d = dd.getValue();
                } else {
                    d = 0.0;
                }
                double cur = d;
                live.setData((ItemStat)stat, (StatData)new DoubleData(cur + entry.getValue()));
            }
            ItemStack built = live.newBuilder().build();
            NBTItem builtNbt = NBTItem.get((ItemStack)built);
            this.preserveTags(nbt, builtNbt);
            builtNbt.addTag(new ItemTag[]{new ItemTag("NEMONICORB_MODS", (Object)this.gson.toJson(mods))});
            result = builtNbt.toItem();
        } else {
            nbt.addTag(new ItemTag[]{new ItemTag("NEMONICORB_MODS", (Object)this.gson.toJson(mods))});
            result = nbt.toItem();
        }
        if (listener != null) {
            listener.updateItemDisplay(result, NBTItem.get((ItemStack)result));
        }
        target.getInventory().setItemInOffHand(result);
        sender.sendMessage(this.cc("&aMod adicionado: " + newMod.id()));
    }

    private void cmdChance(CommandSender sender, Player target, ItemStack item, NBTItem nbt, boolean isMMOItem) {
        ItemStack result;
        int maxMods;
        int modCount;
        int newTier;
        String category;
        int tier;
        int n = tier = nbt.hasTag("NEMONICORB_TIER") ? nbt.getInteger("NEMONICORB_TIER") : 0;
        if (tier != 0) {
            sender.sendMessage(this.cc("&cItem precisa ser Comum (tier 0)."));
            return;
        }
        String string = category = isMMOItem ? this.resolveCategory(nbt.getString("MMOITEMS_ITEM_TYPE")) : this.resolveCategoryFromMaterial(item.getType());
        if (category == null) {
            sender.sendMessage(this.cc("&cTipo nao suportado."));
            return;
        }
        int roll = new Random().nextInt(100);
        int magic = this.plugin.getConfig().getInt("chance-orb.magic", 70);
        int rare = this.plugin.getConfig().getInt("chance-orb.rare", 25);
        if (roll < magic) {
            newTier = 1;
            modCount = new Random().nextInt(2) + 1;
            maxMods = 2;
        } else if (roll < magic + rare) {
            newTier = 2;
            modCount = new Random().nextInt(4) + 3;
            maxMods = 6;
        } else {
            newTier = 3;
            modCount = 6;
            maxMods = 6;
        }
        ModifierEngine engine = this.plugin.getModifierEngine();
        int itemLevel = this.getItemLevel(nbt);
        List<ModifierEngine.RolledModifier> mods = newTier == 3 ? engine.rollForUnique(category, itemLevel) : engine.rollForTier(category, modCount, itemLevel, maxMods, newTier);
        if (isMMOItem) {
            LiveMMOItem live = new LiveMMOItem(item);
            for (ModifierEngine.RolledModifier m : mods) {
                for (Map.Entry<String, Double> entry : m.stats().entrySet()) {
                    double d;
                    DoubleStat stat = engine.resolveStat(entry.getKey());
                    if (stat == null) continue;
                    StatData data = live.getData((ItemStat)stat);
                    if (data instanceof DoubleData) {
                        DoubleData dd = (DoubleData)data;
                        d = dd.getValue();
                    } else {
                        d = 0.0;
                    }
                    double cur = d;
                    live.setData((ItemStat)stat, (StatData)new DoubleData(cur + entry.getValue()));
                }
            }
            ItemStack built = live.newBuilder().build();
            NBTItem builtNbt = NBTItem.get((ItemStack)built);
            builtNbt.addTag(new ItemTag[]{new ItemTag("NEMONICORB_TIER", (Object)newTier)});
            builtNbt.addTag(new ItemTag[]{new ItemTag("NEMONICORB_MODS", (Object)this.gson.toJson(mods))});
            builtNbt.addTag(new ItemTag[]{new ItemTag("NEMONICORB_IDENTIFIED", (Object)1)});
            if (nbt.hasTag("NEMONICORB_POLISH")) {
                builtNbt.addTag(new ItemTag[]{new ItemTag("NEMONICORB_POLISH", (Object)nbt.getInteger("NEMONICORB_POLISH"))});
            }
            result = builtNbt.toItem();
        } else {
            nbt.addTag(new ItemTag[]{new ItemTag("NEMONICORB_TIER", (Object)newTier)});
            nbt.addTag(new ItemTag[]{new ItemTag("NEMONICORB_MODS", (Object)this.gson.toJson(mods))});
            nbt.addTag(new ItemTag[]{new ItemTag("NEMONICORB_IDENTIFIED", (Object)1)});
            result = nbt.toItem();
        }
        OrbListener listener = this.getListener();
        if (listener != null) {
            listener.updateItemDisplay(result, NBTItem.get((ItemStack)result));
        }
        target.getInventory().setItemInOffHand(result);
        Object tierName = listener != null ? listener.getTierName(newTier) : "Tier " + newTier;
        sender.sendMessage(this.cc("&aMoeda da Sorte aplicada. Novo tier: " + (String)tierName));
    }

    private void cmdAnnul(CommandSender sender, Player target, ItemStack item, NBTItem nbt, boolean isMMOItem) {
        ItemStack result;
        List<Object> mods;
        if (nbt.hasTag("NEMONICORB_POLISHED") && nbt.getInteger("NEMONICORB_POLISHED") == 1) {
            sender.sendMessage(this.cc("&cItem polido! Mods travados. Apenas upgrade de tier permitido."));
            return;
        }
        OrbListener listener = this.getListener();
        List<Object> list = mods = listener != null ? listener.readMods(nbt) : new ArrayList();
        if (mods.isEmpty()) {
            sender.sendMessage(this.cc("&cItem nao tem mods."));
            return;
        }
        int idx = new Random().nextInt(mods.size());
        ModifierEngine.RolledModifier removed = (ModifierEngine.RolledModifier)mods.get(idx);
        mods.remove(idx);
        if (isMMOItem) {
            LiveMMOItem live = new LiveMMOItem(item);
            ModifierEngine engine = this.plugin.getModifierEngine();
            for (Map.Entry<String, Double> entry : removed.stats().entrySet()) {
                double d;
                DoubleStat stat = engine.resolveStat(entry.getKey());
                if (stat == null) continue;
                StatData data = live.getData((ItemStat)stat);
                if (data instanceof DoubleData) {
                    DoubleData dd = (DoubleData)data;
                    d = dd.getValue();
                } else {
                    d = 0.0;
                }
                double cur = d;
                live.setData((ItemStat)stat, (StatData)new DoubleData(cur - entry.getValue()));
            }
            ItemStack built = live.newBuilder().build();
            NBTItem builtNbt = NBTItem.get((ItemStack)built);
            this.preserveTags(nbt, builtNbt);
            builtNbt.addTag(new ItemTag[]{new ItemTag("NEMONICORB_MODS", (Object)this.gson.toJson(mods))});
            result = builtNbt.toItem();
        } else {
            nbt.addTag(new ItemTag[]{new ItemTag("NEMONICORB_MODS", (Object)this.gson.toJson(mods))});
            result = nbt.toItem();
        }
        if (listener != null) {
            listener.updateItemDisplay(result, NBTItem.get((ItemStack)result));
        }
        target.getInventory().setItemInOffHand(result);
        sender.sendMessage(this.cc("&aMod removido: " + removed.id()));
    }

    private void cmdSetTier(CommandSender sender, Player target, NBTItem nbt, String[] args) {
        ItemStack result;
        List<ModifierEngine.RolledModifier> mods;
        String category;
        int newTier;
        if (args.length < 3) {
            sender.sendMessage(this.cc("&cUso: /nemonicorb set-tier <jogador> <0-3>"));
            return;
        }
        try {
            newTier = Integer.parseInt(args[2]);
        }
        catch (NumberFormatException e) {
            sender.sendMessage(this.cc("&cTier invalido. Use 0-3."));
            return;
        }
        if (newTier < 0 || newTier > 3) {
            sender.sendMessage(this.cc("&cTier invalido. Use 0-3."));
            return;
        }
        ItemStack item = target.getInventory().getItemInOffHand();
        boolean isMMOItem = nbt.hasType();
        if (newTier == 0) {
            nbt.addTag(new ItemTag[]{new ItemTag("NEMONICORB_TIER", (Object)0)});
            nbt.addTag(new ItemTag[]{new ItemTag("NEMONICORB_MODS", (Object)"[]")});
            nbt.addTag(new ItemTag[]{new ItemTag("NEMONICORB_IDENTIFIED", (Object)1)});
            ItemStack updated = nbt.toItem();
            OrbListener listener = this.getListener();
            if (listener != null) {
                listener.updateItemDisplay(updated, NBTItem.get((ItemStack)updated));
            }
            target.getInventory().setItemInOffHand(updated);
            sender.sendMessage(this.cc("&aTier definido para Comum (0). Mods removidos."));
            return;
        }
        String string = category = isMMOItem ? this.resolveCategory(nbt.getString("MMOITEMS_ITEM_TYPE")) : this.resolveCategoryFromMaterial(item.getType());
        if (category == null) {
            sender.sendMessage(this.cc("&cTipo nao suportado para rolar mods."));
            return;
        }
        ModifierEngine engine = this.plugin.getModifierEngine();
        int itemLevel = this.getItemLevel(nbt);
        if (newTier == 3) {
            mods = engine.rollForUnique(category, itemLevel);
        } else if (newTier == 2) {
            modCount = new Random().nextInt(4) + 3;
            mods = engine.rollForTier(category, modCount, itemLevel, 6, newTier);
        } else {
            modCount = new Random().nextInt(2) + 1;
            mods = engine.rollForTier(category, modCount, itemLevel, 2, newTier);
        }
        if (isMMOItem) {
            LiveMMOItem live = new LiveMMOItem(item);
            for (ModifierEngine.RolledModifier m : mods) {
                for (Map.Entry<String, Double> entry : m.stats().entrySet()) {
                    double d;
                    DoubleStat stat = engine.resolveStat(entry.getKey());
                    if (stat == null) continue;
                    StatData data = live.getData((ItemStat)stat);
                    if (data instanceof DoubleData) {
                        DoubleData dd = (DoubleData)data;
                        d = dd.getValue();
                    } else {
                        d = 0.0;
                    }
                    double cur = d;
                    live.setData((ItemStat)stat, (StatData)new DoubleData(cur + entry.getValue()));
                }
            }
            ItemStack built = live.newBuilder().build();
            NBTItem builtNbt = NBTItem.get((ItemStack)built);
            builtNbt.addTag(new ItemTag[]{new ItemTag("NEMONICORB_TIER", (Object)newTier)});
            builtNbt.addTag(new ItemTag[]{new ItemTag("NEMONICORB_MODS", (Object)this.gson.toJson(mods))});
            builtNbt.addTag(new ItemTag[]{new ItemTag("NEMONICORB_IDENTIFIED", (Object)1)});
            if (nbt.hasTag("NEMONICORB_POLISH")) {
                builtNbt.addTag(new ItemTag[]{new ItemTag("NEMONICORB_POLISH", (Object)nbt.getInteger("NEMONICORB_POLISH"))});
            }
            result = builtNbt.toItem();
        } else {
            nbt.addTag(new ItemTag[]{new ItemTag("NEMONICORB_TIER", (Object)newTier)});
            nbt.addTag(new ItemTag[]{new ItemTag("NEMONICORB_MODS", (Object)this.gson.toJson(mods))});
            nbt.addTag(new ItemTag[]{new ItemTag("NEMONICORB_IDENTIFIED", (Object)1)});
            result = nbt.toItem();
        }
        OrbListener listener = this.getListener();
        if (listener != null) {
            listener.updateItemDisplay(result, NBTItem.get((ItemStack)result));
        }
        target.getInventory().setItemInOffHand(result);
        Object tierName = listener != null ? listener.getTierName(newTier) : "Tier " + newTier;
        sender.sendMessage(this.cc("&aTier definido para " + (String)tierName + " (" + newTier + "). " + mods.size() + " mods rolados."));
    }

    private void cmdSetId(CommandSender sender, Player target, NBTItem nbt, String[] args) {
        int val;
        if (args.length < 3) {
            sender.sendMessage(this.cc("&cUso: /nemonicorb set-id <jogador> <0|1>"));
            return;
        }
        try {
            val = Integer.parseInt(args[2]);
        }
        catch (NumberFormatException e) {
            sender.sendMessage(this.cc("&cValor invalido. Use 0 ou 1."));
            return;
        }
        if (val != 0 && val != 1) {
            sender.sendMessage(this.cc("&cValor invalido. Use 0 (nao identificado) ou 1 (identificado)."));
            return;
        }
        nbt.addTag(new ItemTag[]{new ItemTag("NEMONICORB_IDENTIFIED", (Object)val)});
        ItemStack updated = nbt.toItem();
        OrbListener listener = this.getListener();
        if (listener != null) {
            listener.updateItemDisplay(updated, NBTItem.get((ItemStack)updated));
        }
        target.getInventory().setItemInOffHand(updated);
        String status = val == 1 ? "identificado" : "nao identificado";
        sender.sendMessage(this.cc("&aItem marcado como " + status + "."));
    }

    private void cmdInfo(CommandSender sender, NBTItem nbt, ItemStack item) {
        int tier = nbt.hasTag("NEMONICORB_TIER") ? nbt.getInteger("NEMONICORB_TIER") : 0;
        boolean identified = !nbt.hasTag("NEMONICORB_IDENTIFIED") || nbt.getInteger("NEMONICORB_IDENTIFIED") == 1;
        int polish = nbt.hasTag("NEMONICORB_POLISH") ? nbt.getInteger("NEMONICORB_POLISH") : 0;
        int maxPolish = this.plugin.getConfig().getInt("max-polish", 5);
        OrbListener listener = this.getListener();
        List<Object> mods = listener != null ? listener.readMods(nbt) : new ArrayList();
        boolean isMMOItem = nbt.hasType();
        String type = isMMOItem ? nbt.getString("MMOITEMS_ITEM_TYPE") : item.getType().name();
        String id = isMMOItem ? nbt.getString("MMOITEMS_ITEM_ID") : "VANILLA";
        String category = isMMOItem ? this.resolveCategory(type) : this.resolveCategoryFromMaterial(item.getType());
        Object tierName = listener != null ? listener.getTierName(tier) : "Tier " + tier;
        String tierColor = listener != null ? listener.getTierColor(tier) : "&7";
        sender.sendMessage(this.cc("&6=== NemonicOrb v2.1 Info ==="));
        sender.sendMessage(this.cc("&7Item: &e" + type + " / " + id + (isMMOItem ? "" : " &8(vanilla)")));
        sender.sendMessage(this.cc("&7Tier: " + tierColor + (String)tierName + " &8(" + tier + ")"));
        sender.sendMessage(this.cc("&7Identificado: " + (identified ? "&aSim" : "&cNao")));
        sender.sendMessage(this.cc("&7Polimento: &e" + polish + "&7/&e" + maxPolish));
        sender.sendMessage(this.cc("&7Categoria: &e" + (category != null ? category : "N/A")));
        sender.sendMessage(this.cc("&7Mods: &e" + mods.size()));
        if (mods.isEmpty()) {
            sender.sendMessage(this.cc("&7  (nenhum mod)"));
        } else {
            for (ModifierEngine.RolledModifier rolledModifier : mods) {
                StringBuilder sb = new StringBuilder();
                for (Map.Entry<String, Double> e : rolledModifier.stats().entrySet()) {
                    if (!sb.isEmpty()) {
                        sb.append(", ");
                    }
                    sb.append(e.getKey()).append(": ").append(String.format("%.2f", e.getValue()));
                }
                sender.sendMessage(this.cc("&7  - &e" + rolledModifier.id() + " &8(" + String.valueOf(sb) + ")"));
            }
        }
    }

    private OrbListener getListener() {
        ArrayList listeners = HandlerList.getRegisteredListeners((Plugin)this.plugin);
        for (RegisteredListener rl : listeners) {
            Listener listener = rl.getListener();
            if (!(listener instanceof OrbListener)) continue;
            OrbListener ol = (OrbListener)listener;
            return ol;
        }
        return null;
    }

    private void preserveTags(NBTItem src, NBTItem dst) {
        if (src.hasTag("NEMONICORB_TIER")) {
            dst.addTag(new ItemTag[]{new ItemTag("NEMONICORB_TIER", (Object)src.getInteger("NEMONICORB_TIER"))});
        }
        if (src.hasTag("NEMONICORB_MODS")) {
            dst.addTag(new ItemTag[]{new ItemTag("NEMONICORB_MODS", (Object)src.getString("NEMONICORB_MODS"))});
        }
        if (src.hasTag("NEMONICORB_IDENTIFIED")) {
            dst.addTag(new ItemTag[]{new ItemTag("NEMONICORB_IDENTIFIED", (Object)src.getInteger("NEMONICORB_IDENTIFIED"))});
        }
        if (src.hasTag("NEMONICORB_POLISH")) {
            dst.addTag(new ItemTag[]{new ItemTag("NEMONICORB_POLISH", (Object)src.getInteger("NEMONICORB_POLISH"))});
        }
    }

    private int getItemLevel(NBTItem nbt) {
        if (nbt.hasTag("MMOITEMS_ITEM_LEVEL")) {
            return nbt.getInteger("MMOITEMS_ITEM_LEVEL");
        }
        return 1;
    }

    private String resolveCategory(String type) {
        if (type == null) {
            return null;
        }
        String upper = type.toUpperCase();
        for (String cat : List.of("weapon", "armor", "accessory")) {
            List types = this.plugin.getConfig().getStringList("type-categories." + cat);
            if (!types.stream().anyMatch(t -> t.equalsIgnoreCase(upper))) continue;
            return cat;
        }
        return null;
    }

    private String resolveCategoryFromMaterial(Material material) {
        if (material == null) {
            return null;
        }
        String name = material.name();
        if (name.contains("SWORD") || name.contains("AXE") || name.contains("BOW") || name.contains("CROSSBOW") || name.contains("TRIDENT") || name.contains("MACE")) {
            return "weapon";
        }
        if (name.contains("HELMET") || name.contains("CHESTPLATE") || name.contains("LEGGINGS") || name.contains("BOOTS") || name.equals("TURTLE_HELMET")) {
            return "armor";
        }
        return null;
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage(this.cc("&6=== NemonicOrb v2 Admin ==="));
        sender.sendMessage(this.cc("&e/norb identify <jogador> &7- Pergaminho de Identificacao"));
        sender.sendMessage(this.cc("&e/norb polish <jogador> &7- Oleo de Polimento (+qualidade)"));
        sender.sendMessage(this.cc("&e/norb transmute <jogador> &7- Pedra de Encantamento (Comum \u2192 Magico)"));
        sender.sendMessage(this.cc("&e/norb augment <jogador> &7- Pedra de Reforco (+1 mod em Magico)"));
        sender.sendMessage(this.cc("&e/norb regal <jogador> &7- Runa Nobre (Magico \u2192 Raro)"));
        sender.sendMessage(this.cc("&e/norb alchemy <jogador> &7- Pedra de Refinamento (Comum \u2192 Raro)"));
        sender.sendMessage(this.cc("&e/norb exalt <jogador> &7- Runa de Poder (+1 mod em Raro)"));
        sender.sendMessage(this.cc("&e/norb chance <jogador> &7- Moeda da Sorte (Tier aleatorio)"));
        sender.sendMessage(this.cc("&e/norb annul <jogador> &7- Pedra Corrosiva (Remove 1 mod)"));
        sender.sendMessage(this.cc("&e/norb set-tier <jogador> <0-3> &7- Define tier + rola mods"));
        sender.sendMessage(this.cc("&e/norb set-id <jogador> <0|1> &7- Define identificacao"));
        sender.sendMessage(this.cc("&e/norb info <jogador> &7- Info do item"));
        sender.sendMessage(this.cc("&e/norb audit [self|hand|equip] &7- Auditoria de status/mods"));
        sender.sendMessage(this.cc("&e/norb giveguide <jogador> &7- Entrega Cronicas de Embati"));
        sender.sendMessage(this.cc("&e/norb reload &7- Recarrega config"));
    }

    private void cmdAudit(CommandSender sender, String[] args) {
        String mode;
        if (!(sender instanceof Player)) {
            sender.sendMessage(this.cc("&cUse este comando em jogo."));
            return;
        }
        Player p = (Player)sender;
        String string = mode = args.length >= 2 ? args[1].toLowerCase(Locale.ROOT) : "self";
        if (!(mode.equals("self") || mode.equals("hand") || mode.equals("equip"))) {
            mode = "self";
        }
        List<String> lines = this.plugin.getModifierAuditService().auditPlayer(p, mode);
        for (String line : lines) {
            sender.sendMessage(this.cc(line));
        }
    }

    private void cmdPublicWarp(CommandSender sender, String[] args) {
        String sub;
        PublicWarpManager mgr = this.plugin.getPublicWarpManager();
        if (mgr == null) {
            sender.sendMessage(this.cc("&cSistema de warps publicas nao inicializado."));
            return;
        }
        if (args.length < 2) {
            sender.sendMessage(this.cc("&eUso: &f/norb publicwarp <set|del|list|tp> [nome]"));
            return;
        }
        switch (sub = args[1].toLowerCase()) {
            case "list": {
                List<PublicWarpManager.PublicWarp> all = mgr.all();
                if (all.isEmpty()) {
                    sender.sendMessage(this.cc("&7Nenhuma warp publica registrada."));
                    return;
                }
                sender.sendMessage(this.cc("&6=== Warps Publicas (" + all.size() + ") ==="));
                for (PublicWarpManager.PublicWarp w : all) {
                    Location loc = w.location();
                    sender.sendMessage(this.cc("&e" + w.name() + " &7- &f" + loc.getWorld().getName() + " &7(" + (int)loc.getX() + ", " + (int)loc.getY() + ", " + (int)loc.getZ() + ") &8por " + w.creator()));
                }
                break;
            }
            case "set": {
                if (!sender.hasPermission("nemonicorb.admin")) {
                    sender.sendMessage(this.cc("&cApenas administradores podem registrar warps publicas."));
                    return;
                }
                if (!(sender instanceof Player)) {
                    sender.sendMessage(this.cc("&cApenas jogadores podem usar 'set' (precisa de localizacao)."));
                    return;
                }
                Player p = (Player)sender;
                if (args.length < 3) {
                    sender.sendMessage(this.cc("&eUso: &f/norb publicwarp set <nome>"));
                    return;
                }
                String name = args[2];
                mgr.setWarp(name, p.getLocation(), p.getName());
                p.sendMessage(this.cc("&a[Warp] Warp publica &e" + name + "&a registrada na sua posicao."));
                p.sendMessage(this.cc("&7Coloque um bloco BEACON neste local como sinalizador visual."));
                break;
            }
            case "del": 
            case "delete": 
            case "remove": {
                if (!sender.hasPermission("nemonicorb.admin")) {
                    sender.sendMessage(this.cc("&cApenas administradores podem remover warps publicas."));
                    return;
                }
                if (args.length < 3) {
                    sender.sendMessage(this.cc("&eUso: &f/norb publicwarp del <nome>"));
                    return;
                }
                String name = args[2];
                if (mgr.removeWarp(name)) {
                    sender.sendMessage(this.cc("&a[Warp] Warp '" + name + "' removida."));
                    break;
                }
                sender.sendMessage(this.cc("&c[Warp] Warp '" + name + "' nao existe."));
                break;
            }
            case "tp": 
            case "teleport": {
                if (!(sender instanceof Player)) {
                    sender.sendMessage(this.cc("&cApenas jogadores podem se teleportar."));
                    return;
                }
                Player p = (Player)sender;
                if (args.length < 3) {
                    sender.sendMessage(this.cc("&eUso: &f/norb publicwarp tp <nome>"));
                    return;
                }
                mgr.startTeleport(p, args[2]);
                break;
            }
            default: {
                sender.sendMessage(this.cc("&eUso: &f/norb publicwarp <set|del|list|tp> [nome]"));
            }
        }
    }

    private void cmdGiveGuide(CommandSender sender, Player target) {
        ItemStack guide = this.plugin.getGuideManager().createWelcomeBook();
        if (guide == null) {
            sender.sendMessage(this.cc("&cNao foi possivel criar o livro guia."));
            return;
        }
        target.getInventory().addItem(new ItemStack[]{guide});
        sender.sendMessage(this.cc("&aCronicas de Embati entregue para " + target.getName() + "."));
    }

    private void msg(CommandSender sender, String key) {
        String prefix = this.plugin.getConfig().getString("messages.prefix", "");
        String raw = this.plugin.getConfig().getString("messages." + key, key);
        sender.sendMessage(this.cc(prefix + raw));
    }

    private String cc(String s) {
        return ChatColor.translateAlternateColorCodes((char)'&', (String)s);
    }

    public List<String> onTabComplete(CommandSender sender, Command cmd, String label, String[] args) {
        if (args.length == 1) {
            return SUBCOMMANDS.stream().filter(s -> s.startsWith(args[0].toLowerCase())).collect(Collectors.toList());
        }
        if (args.length == 2 && !args[0].equalsIgnoreCase("reload")) {
            if (args[0].equalsIgnoreCase("audit")) {
                return List.of("self", "hand", "equip");
            }
            return null;
        }
        if (args.length == 3) {
            String a = args[0].toLowerCase();
            if (a.equals("set-tier")) {
                return List.of("0", "1", "2", "3");
            }
            if (a.equals("set-id")) {
                return List.of("0", "1");
            }
        }
        return Collections.emptyList();
    }
}

