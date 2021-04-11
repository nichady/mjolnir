package com.github.nichady.mjolnir;

import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.UUID;

public final class Cooldown {
    private final HashMap<UUID, Long> map = new HashMap<>();

    public boolean ready(Entity entity) {
        return getSecondsRemaining(entity, false) <= 0;
    }

    public void put(Player player, double seconds) {
        map.put(player.getUniqueId(), (long) (System.currentTimeMillis() + seconds * 1000));
    }

    public double getSecondsRemaining(Entity entity, boolean round) {
        double seconds = (map.getOrDefault(entity.getUniqueId(), 0L) - System.currentTimeMillis())/1000D;
        if (round) seconds = Math.round(seconds * 10) / 10D;
        return seconds;
    }
}
