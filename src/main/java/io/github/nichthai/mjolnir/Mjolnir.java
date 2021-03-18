package io.github.nichthai.mjolnir;

import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.plugin.java.JavaPlugin;

public final class Mjolnir extends JavaPlugin {
    final NamespacedKey key = new NamespacedKey(this, "mjolnir");
    private final Events e = new Events(this);

    @Override
    public void onEnable() {
        getConfig().options().copyDefaults(true);
        saveConfig();

        getCommand("mjolnir").setExecutor(new Cmd(this));

        Bukkit.getPluginManager().registerEvents(e, this);
        e.schedule();
    }

    @Override
    public void onDisable() {
        e.cleanUp();
    }
}
