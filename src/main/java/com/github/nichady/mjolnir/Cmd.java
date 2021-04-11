package com.github.nichady.mjolnir;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.util.StringUtil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

final class Cmd implements TabExecutor {
    private final Mjolnir plugin;
    private ItemStack item;

    Cmd(Mjolnir plugin) {
        this.plugin = plugin;
        reload();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) sendMainMessage(sender);
        else switch (args[0]) {
            case "?":
            case "help":
                if (sender.hasPermission("mjolnir.command.help")) sendHelpMessage(sender);
                else sender.sendMessage(ChatColor.RED + "You don't have permission to do this.");
                break;
            case "get":
                if (!sender.hasPermission("mjolnir.command.get")) sender.sendMessage(ChatColor.RED + "You don't have permission to do this.");
                else if (sender instanceof Player) giveItem((Player) sender);
                else sender.sendMessage(ChatColor.RED + "Only players can use this command!");
                break;
            case "reload":
                if (sender.hasPermission("mjolnir.command.reload")) {
                    plugin.reloadConfig();
                    reload();
                    sender.sendMessage(ChatColor.BLUE + "Plugin reloaded.");
                }
                else sender.sendMessage(ChatColor.RED + "You don't have permission to do this.");
                break;
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length > 1) return new ArrayList<>();
        return StringUtil.copyPartialMatches(args[0], Arrays.asList("help", "get", "reload"), new ArrayList<>());
    }

    private void sendMainMessage(CommandSender sender) {
        sender.sendMessage(ChatColor.BLUE + "Running " + ChatColor.GOLD + ChatColor.BOLD + plugin.getDescription().getFullName());
        if (sender.hasPermission("mjolnir.command.help")) sender.sendMessage(ChatColor.BLUE + "Use " + ChatColor.GOLD + "/mjolnir help " + ChatColor.BLUE + "to view available commands.");
    }

    private void sendHelpMessage(CommandSender sender) {
        sender.sendMessage("");
        sender.sendMessage(ChatColor.GOLD + (ChatColor.BOLD + plugin.getDescription().getFullName()));
        if (sender.hasPermission("mjolnir.command.help")) sender.sendMessage(ChatColor.BLUE + "/mjolnir help" + ChatColor.GRAY + " Displays this");
        if (sender.hasPermission("mjolnir.command.get")) sender.sendMessage(ChatColor.BLUE + "/mjolnir get" + ChatColor.GRAY + " Gets Mjolnir");
        if (sender.hasPermission("mjolnir.command.reload")) sender.sendMessage(ChatColor.BLUE + "/mjolnir reload" + ChatColor.GRAY + " Reloads config");
        sender.sendMessage("");
    }

    private void giveItem(Player player) {
        if (player.getInventory().firstEmpty() == -1) player.sendMessage(ChatColor.RED + "Please empty your inventory first!");
        else player.getInventory().addItem(item.clone());
    }

    private void reload() {
        ConfigurationSection config = plugin.getConfig();
        item = new ItemStack(Material.matchMaterial(config.getString("material")));
        ItemMeta meta = item.getItemMeta();

        for (String s : config.getStringList("ench")) {
            String enchName = s.split(":")[0].toLowerCase();
            int lvl = Integer.parseInt(s.split(":")[1]);
            Enchantment ench = Enchantment.getByKey(NamespacedKey.minecraft(enchName));
            if (ench != null) meta.addEnchant(ench, lvl, true);
            else throw new IllegalArgumentException(enchName + " is not a valid enchantment");
        }

        List<String> lore = new ArrayList<>();
        List<String> cfgLore = config.getStringList("lore");
        for (String s : cfgLore) lore.add(ChatColor.translateAlternateColorCodes('&', s));

        meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', config.getString("name")));
        meta.addItemFlags(ItemFlag.HIDE_UNBREAKABLE);
        meta.setUnbreakable(config.getBoolean("unbreakable"));
        meta.setCustomModelData(config.getInt("custom_model_data"));
        meta.getPersistentDataContainer().set(plugin.key, PersistentDataType.INTEGER, 0);

        meta.setLore(lore);
        item.setItemMeta(meta);
    }
}
