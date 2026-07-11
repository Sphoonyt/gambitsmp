package com.gambitsmp.dimension;

import com.gambitsmp.GambitSMP;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerSpawnLocationEvent;

public class FirstJoinListener implements Listener {

    private final GambitSMP plugin;

    public FirstJoinListener(GambitSMP plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onFirstSpawn(PlayerSpawnLocationEvent event) {
        // This event fires for every login, not just the first - only redirect brand
        // new players. Returning players who die/respawn normally are handled by
        // vanilla bed/anchor rules; anyone can always get back with /gambit spawn.
        if (event.getPlayer().hasPlayedBefore()) return;

        CardDimensionManager mgr = plugin.cardDimension();
        if (mgr.getWorld() == null) return;

        event.setSpawnLocation(mgr.getSpawnLocation());
    }
}
