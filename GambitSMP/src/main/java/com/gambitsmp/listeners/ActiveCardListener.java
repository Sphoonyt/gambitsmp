package com.gambitsmp.listeners;

import com.gambitsmp.GambitSMP;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;

public class ActiveCardListener implements Listener {

    private final GambitSMP plugin;

    public ActiveCardListener(GambitSMP plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onActivate(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        Player player = event.getPlayer();
        if (!player.isSneaking()) return;
        if (event.getItem() != null && !event.getItem().getType().isAir()) return; // must be empty hand

        // Note: sneak+right-click can be unreliable on some clients/latency conditions.
        // Players can also use /ability <1|2|3> as a guaranteed alternative trigger.
        if (plugin.activeCards().triggerNextReady(player)) {
            event.setCancelled(true);
        }
    }
}
