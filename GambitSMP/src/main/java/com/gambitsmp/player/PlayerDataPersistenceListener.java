package com.gambitsmp.player;

import com.gambitsmp.GambitSMP;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

public class PlayerDataPersistenceListener implements Listener {

    private final GambitSMP plugin;

    public PlayerDataPersistenceListener(GambitSMP plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        // Saves everyone currently tracked, not just the player leaving - simple and
        // safe at this plugin's scale, and guarantees the file is always fully
        // consistent rather than patched incrementally.
        plugin.data().saveAll();
    }
}
