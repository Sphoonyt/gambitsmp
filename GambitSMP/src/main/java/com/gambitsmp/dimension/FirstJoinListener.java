package com.gambitsmp.dimension;

import com.gambitsmp.GambitSMP;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

/**
 * Sends brand-new players to CardDimension (in front of the portal) on their very
 * first join.
 *
 * Uses PlayerJoinEvent + hasPlayedBefore() rather than a spawn-location-override
 * event: it's less elegant (the player briefly exists at the default world's spawn
 * before being moved one tick later) but PlayerJoinEvent is guaranteed-stable core
 * Bukkit API with zero naming/package risk, unlike the spawn-location events which
 * turned out to live somewhere other than expected on this Paper version.
 */
public class FirstJoinListener implements Listener {

    private final GambitSMP plugin;

    public FirstJoinListener(GambitSMP plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        if (player.hasPlayedBefore()) return;

        CardDimensionManager mgr = plugin.cardDimension();
        if (mgr.getWorld() == null) return;

        // Delay a tick so the player is fully placed in the world before we move
        // them - teleporting in the same tick as join can be unreliable.
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            Location location = mgr.getSpawnLocation();
            if (location != null && location.getWorld() != null) {
                player.teleport(location);
            }
        });
    }
}
