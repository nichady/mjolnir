package me.nichady.mjolnir;

import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;

import java.util.HashSet;
import java.util.UUID;

public class Duration {
    private final HashSet<UUID> set = new HashSet<>();
    private final Mjolnir plugin;

    public Duration(Mjolnir plugin) { this.plugin = plugin; }

    public void put(Entity entity, double seconds) {
        final UUID uuid = entity.getUniqueId();
        set.add(uuid);
        Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, () -> set.remove(uuid), Math.round(seconds * 20));
    }

    public boolean has(Entity entity) {
        return set.contains(entity.getUniqueId());
    }

    public HashSet<UUID> getAll() {
        return set;
    }
}
