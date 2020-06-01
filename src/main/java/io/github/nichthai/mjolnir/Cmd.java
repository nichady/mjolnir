package io.github.nichthai.mjolnir;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.util.StringUtil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

final class Cmd implements TabExecutor
{
	private final Mjolnir plugin;
	private final ItemStack item;
	
	Cmd(final Mjolnir plugin)
	{
		this.plugin = plugin;
		
		final ConfigurationSection config = plugin.getConfig();
		item = new ItemStack(Material.matchMaterial(config.getString("material")));
		final ItemMeta meta = item.getItemMeta();
		
		for (final String s : config.getStringList("ench"))
		{
			final String enchName = s.split(":")[0].toLowerCase();
			final int lvl = Integer.parseInt(s.split(":")[1]);
			final Enchantment ench = Enchantment.getByKey(NamespacedKey.minecraft(enchName));
			if (ench != null) meta.addEnchant(ench, lvl, true);
			else throw new IllegalArgumentException(enchName + " is not a valid enchantment");
		}
		
		final List<String> lore = new ArrayList<>();
		final List<String> cfgLore = config.getStringList("lore");
		for (final String s : cfgLore) lore.add(ChatColor.translateAlternateColorCodes('&', s));
		
		meta.setUnbreakable(config.getBoolean("unbreakable"));
		meta.setCustomModelData(config.getInt("custom_model_data"));
		meta.getPersistentDataContainer().set(plugin.key, PersistentDataType.INTEGER, 0);
		
		meta.setLore(lore);
		item.setItemMeta(meta);
	}
	
	@Override
	public boolean onCommand(final CommandSender sender, final Command command, final String label, final String[] args)
	{
		if (args.length == 0) sendMainMessage(sender);
		else switch (args[0])
		{
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
				if (sender.hasPermission("mjolnir.command.reload")) { reload(); sender.sendMessage(ChatColor.BLUE + "Plugin reloaded."); }
				else sender.sendMessage(ChatColor.RED + "You don't have permission to do this.");
				break;
		}
		return true;
	}
	
	@Override
	public List<String> onTabComplete(final CommandSender sender, final Command command, final String alias, final String[] args)
	{
		if (args.length > 1) return new ArrayList<>();
		return StringUtil.copyPartialMatches(args[0], Arrays.asList("help", "get", "reload"), new ArrayList<>());
	}
	
	private void sendMainMessage(final CommandSender sender)
	{
		sender.sendMessage(ChatColor.BLUE + "Running " + ChatColor.GOLD + ChatColor.BOLD + plugin.getDescription().getFullName());
		if (sender.hasPermission("mjolnir.command.help")) sender.sendMessage(ChatColor.BLUE + "Use " + ChatColor.GOLD + "/mjolnir help " + ChatColor.BLUE + "to view available commands.");
	}
	
	private void sendHelpMessage(final CommandSender sender)
	{
		sender.sendMessage("");
		sender.sendMessage(ChatColor.GOLD + (ChatColor.BOLD + plugin.getDescription().getFullName()));
		if (sender.hasPermission("mjolnir.command.help")) sender.sendMessage(ChatColor.BLUE + "/mjolnir help" + ChatColor.GRAY + " Displays this");
		if (sender.hasPermission("mjolnir.command.items")) sender.sendMessage(ChatColor.BLUE + "/mjolnir get" + ChatColor.GRAY + " Gets Mjolnir");
		if (sender.hasPermission("mjolnir.command.give")) sender.sendMessage(ChatColor.BLUE + "/mjolnir reload" + ChatColor.GRAY + " Reloads config");
		sender.sendMessage("");
	}
	
	private void giveItem(final Player player)
	{
		if (player.getInventory().firstEmpty() == -1) player.sendMessage(ChatColor.RED + "Please empty your inventory first!");
		else player.getInventory().addItem(item.clone());
	}
	
	private void reload()
	{
		plugin.reloadConfig();
		
	}
}
