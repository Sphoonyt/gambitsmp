package com.gambitsmp.dimension;

import com.gambitsmp.GambitSMP;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerPortalEvent;

public class PortalTravelListener implements Listener {

    private final GambitSMP plugin;

    public PortalTravelListener(GambitSMP plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPortal(PlayerPortalEvent event) {
        CardDimensionManager mgr = plugin.cardDimension();
        Location from = event.getFrom();
        if (from.getWorld() == null) return;

        // CardDimension -> overworld: any portal use anywhere in CardDimension redirects
        // to the overworld spawn. This is deliberately broad (not limited to exactly the
        // blocks of the portal we built) since CardDimension is only ever meant to have
        // one meaningful portal destination - the overworld - regardless of exactly which
        // portal blocks triggered it.
        if (from.getWorld().equals(mgr.getWorld())) {
            if (!mgr.isActivated()) {
                // shouldn't normally happen since the interior is plain air until
                // activated, but guard anyway in case something else lit portal blocks
                event.setCancelled(true);
                return;
            }
            World overworld = Bukkit.getWorld(mgr.getOverworldName());
            if (overworld == null) overworld = Bukkit.getWorlds().get(0);

            event.setCancelled(false);
            event.setTo(overworld.getSpawnLocation());
            event.setCanCreatePortal(false);
            return;
        }

        // overworld -> CardDimension: only the specific return portal we built redirects
        // back - everything else in the overworld (a player's own Nether portals, etc.)
        // is left completely alone and behaves normally.
        String overworldName = mgr.getOverworldName();
        if (from.getWorld().getName().equals(overworldName) && mgr.isReturnPortalBlock(from)) {
            event.setCancelled(false);
            event.setTo(mgr.getSpawnLocation());
            event.setCanCreatePortal(false);
        }
    }
}
