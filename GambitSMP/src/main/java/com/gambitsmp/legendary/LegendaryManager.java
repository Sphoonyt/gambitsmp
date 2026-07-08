package com.gambitsmp.legendary;

import com.gambitsmp.GambitSMP;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;

public class LegendaryManager {

    private final GambitSMP plugin;

    public LegendaryManager(GambitSMP plugin) {
        this.plugin = plugin;
    }

    public ItemStack createLegendary(LegendaryType type) {
        Material material = type == LegendaryType.VAMPIRIC_FANG ? Material.NETHERITE_SWORD : Material.NETHER_STAR;
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();

        meta.displayName(Component.text(type.itemName(), NamedTextColor.GOLD, TextDecoration.BOLD)
                .decoration(TextDecoration.ITALIC, false));

        List<Component> lore = new ArrayList<>();
        lore.add(Component.text("LEGENDARY", NamedTextColor.GOLD).decoration(TextDecoration.ITALIC, false));
        lore.add(Component.empty());
        lore.add(Component.text("Grants: " + type.grantedCardName(), NamedTextColor.LIGHT_PURPLE)
                .decoration(TextDecoration.ITALIC, false));
        for (String line : type.lore()) {
            lore.add(Component.text(line, NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false));
        }
        if (type != LegendaryType.VAMPIRIC_FANG) {
            lore.add(Component.empty());
            lore.add(Component.text("Right-click to equip", NamedTextColor.GREEN)
                    .decoration(TextDecoration.ITALIC, false));
        }
        meta.lore(lore);

        meta.getPersistentDataContainer().set(plugin.legendaryKey(), PersistentDataType.STRING, type.name());
        meta.addItemFlags(ItemFlag.values());
        item.setItemMeta(meta);
        return item;
    }

    public LegendaryType readLegendary(ItemStack item) {
        if (item == null || item.getType().isAir() || !item.hasItemMeta()) return null;
        ItemMeta meta = item.getItemMeta();
        String raw = meta.getPersistentDataContainer().get(plugin.legendaryKey(), PersistentDataType.STRING);
        if (raw == null) return null;
        try {
            return LegendaryType.valueOf(raw);
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    public boolean isLegendary(ItemStack item) {
        return readLegendary(item) != null;
    }
}
