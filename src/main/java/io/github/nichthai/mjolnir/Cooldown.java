package io.github.nichthai.mjolnir;

import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.UUID;

public final class Cooldown {
    private final HashMap<UUID, Long> map = new HashMap<>();

    public boolean ready(final Entity entity) {
        return getSecondsRemaining(entity, false) <= 0;
    }

    public void put(final Player player, final double seconds) {
        map.put(player.getUniqueId(), (long) (System.currentTimeMillis() + seconds * 1000));
    }

    public double getSecondsRemaining(final Entity entity, final boolean round) {
        double seconds = (map.getOrDefault(entity.getUniqueId(), 0L) - System.currentTimeMillis())/1000D;
        if (round) seconds = Math.round(seconds * 10) / 10D;
        return seconds;
    }
}
