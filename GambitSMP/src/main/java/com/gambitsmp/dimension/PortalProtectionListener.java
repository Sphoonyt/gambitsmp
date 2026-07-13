package com.gambitsmp.dimension;

import com.gambitsmp.GambitSMP;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.world.PortalCreateEvent;

public class PortalProtectionListener implements Listener {

    private final GambitSMP plugin;

    public PortalProtectionListener(GambitSMP plugin) {
        this.plugin = plugin;
    }

    private boolean isProtected(Block block) {
        CardDimensionManager mgr = plugin.cardDimension();
        return mgr.isFrameBlock(block.getLocation())
                || mgr.isPortalBlock(block.getLocation())
                || mgr.isReturnPortalBlock(block.getLocation());
    }

    // Only our own pre-built portal is allowed to exist in CardDimension - this
    // blocks players from lighting their own nether portals there entirely (flint &
    // steel, portal frame regeneration, etc). Our own activation of the real portal
    // (see SpecialMatterListener/CardDimensionManager) sets block types directly
    // rather than going through the game's actual ignition mechanic, so it never
    // fires this event and isn't affected by this cancellation.
    @EventHandler(ignoreCancelled = true)
    public void onPortalCreate(PortalCreateEvent event) {
        CardDimensionManager mgr = plugin.cardDimension();
        if (event.getWorld().equals(mgr.getWorld())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onBreak(BlockBreakEvent event) {
        if (!isProtected(event.getBlock())) return;
        event.setCancelled(true);
        Player player = event.getPlayer();
        player.sendMessage(Component.text("This portal can't be broken.", NamedTextColor.RED));
    }

    @EventHandler(ignoreCancelled = true)
    public void onBlockExplode(BlockExplodeEvent event) {
        event.blockList().removeIf(this::isProtected);
    }

    @EventHandler(ignoreCancelled = true)
    public void onEntityExplode(EntityExplodeEvent event) {
        event.blockList().removeIf(this::isProtected);
    }
}
