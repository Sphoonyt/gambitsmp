package com.gambitsmp.player;

import com.gambitsmp.GambitSMP;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class PlayerDataManager {

    private final GambitSMP plugin;
    private final Map<UUID, PlayerCardData> data = new ConcurrentHashMap<>();

    public PlayerDataManager(GambitSMP plugin) {
        this.plugin = plugin;
    }

    public PlayerCardData get(UUID uuid) {
        return data.computeIfAbsent(uuid, PlayerCardData::new);
    }

    public void remove(UUID uuid) {
        data.remove(uuid);
    }

    public int maxEquippedCards() {
        return plugin.getConfig().getInt("max-equipped-cards", 3);
    }

    /** Placeholder for future persistence to disk; currently in-memory only for the session. */
    public void saveAll() {
        // Equipped card state lives on the physical card items already equipped in a
        // player's "gambit belt" inventory, so nothing extra to flush here yet.
    }
}
