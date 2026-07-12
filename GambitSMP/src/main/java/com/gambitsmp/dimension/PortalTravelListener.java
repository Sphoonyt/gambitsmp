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
        if (from.getWorld() == null || !from.getWorld().equals(mgr.getWorld())) return; // not our portal
        if (!mgr.isPortalBlock(from) && !mgr.isPortalBlock(from.getBlock().getLocation())) return;

        if (!mgr.isActivated()) {
            // shouldn't normally happen since the interior is plain air until activated,
            // but guard anyway in case something else placed nether portal blocks there
            event.setCancelled(true);
            return;
        }

        World overworld = Bukkit.getWorld(mgr.getOverworldName());
        if (overworld == null) {
            overworld = Bukkit.getWorlds().get(0);
        }

        event.setCancelled(false);
        event.setTo(overworld.getSpawnLocation());
        event.setCanCreatePortal(false);
    }
}
