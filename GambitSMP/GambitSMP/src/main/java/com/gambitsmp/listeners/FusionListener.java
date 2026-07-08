package com.gambitsmp.listeners;

import com.gambitsmp.GambitSMP;
import com.gambitsmp.legendary.LegendaryType;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.PrepareAnvilEvent;
import org.bukkit.inventory.AnvilInventory;
import org.bukkit.inventory.ItemStack;

public class FusionListener implements Listener {

    private final GambitSMP plugin;

    public FusionListener(GambitSMP plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPrepareAnvil(PrepareAnvilEvent event) {
        AnvilInventory inv = event.getInventory();
        ItemStack first = inv.getItem(0);
        ItemStack second = inv.getItem(1);
        if (first == null || second == null) return;

        LegendaryType a = plugin.legendaries().readLegendary(first);
        LegendaryType b = plugin.legendaries().readLegendary(second);
        if (a == null || b == null) return;

        boolean matchPair = (a == LegendaryType.VAMPIRIC_CLAWS && b == LegendaryType.FANG)
                || (a == LegendaryType.FANG && b == LegendaryType.VAMPIRIC_CLAWS);
        if (!matchPair) return;

        event.setResult(plugin.legendaries().createLegendary(LegendaryType.VAMPIRIC_FANG));
    }
}
