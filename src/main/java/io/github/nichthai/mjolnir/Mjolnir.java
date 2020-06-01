package io.github.nichthai.mjolnir;

import org.bukkit.NamespacedKey;
import org.bukkit.plugin.java.JavaPlugin;

public final class Mjolnir extends JavaPlugin
{
	final NamespacedKey key = new NamespacedKey(this, "mjolnir");
	
	@Override
	public void onEnable()
	{
		getConfig().options().copyDefaults(true);
		saveConfig();
		
		getCommand("mjolnir").setExecutor(new Cmd(this));
	}
	
	@Override
	public void onDisable()
	{
		// Plugin shutdown logic
	}
}
