package com.gambitsmp.dimension;

import com.gambitsmp.GambitSMP;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

public class SpecialMatterListener implements Listener {

    private final GambitSMP plugin;

    public SpecialMatterListener(GambitSMP plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        if (event.getClickedBlock() == null) return;

        CardDimensionManager mgr = plugin.cardDimension();
        if (!mgr.isLodestoneLocation(event.getClickedBlock().getLocation())) return;

        // This lodestone is repurposed entirely for the portal ritual - suppress the
        // normal "bind compass" behavior so it doesn't do both at once.
        event.setCancelled(true);
        Player player = event.getPlayer();

        if (mgr.isActivated()) {
            player.sendMessage(Component.text("The portal is already open.", NamedTextColor.GRAY));
            return;
        }

        ItemStack held = event.getItem();
        if (!plugin.specialMatter().isSpecialMatter(held)) {
            player.sendMessage(Component.text("This lodestone needs Special Matter to open the portal.", NamedTextColor.YELLOW));
            return;
        }

        held.setAmount(held.getAmount() - 1);
        mgr.activate();

        player.getWorld().playSound(player.getLocation(), Sound.BLOCK_PORTAL_TRIGGER, 1.5f, 0.8f);
        player.getWorld().spawnParticle(Particle.PORTAL, event.getClickedBlock().getLocation().add(0.5, 1, 0.5), 100, 1.5, 2, 1.5, 0.5);
        player.sendMessage(Component.text("The portal flickers to life - it now leads to the overworld.", NamedTextColor.LIGHT_PURPLE));
    }
}
