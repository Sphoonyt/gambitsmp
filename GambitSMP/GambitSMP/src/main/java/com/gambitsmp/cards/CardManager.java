package com.gambitsmp.cards;

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

public class CardManager {

    private final GambitSMP plugin;

    public CardManager(GambitSMP plugin) {
        this.plugin = plugin;
    }

    /** Builds a fresh card item for the given card type. */
    public ItemStack createCard(CardType type) {
        ItemStack item = new ItemStack(Material.PAPER);
        ItemMeta meta = item.getItemMeta();

        meta.displayName(Component.text(type.displayName(), type.rarity().color())
                .decoration(TextDecoration.ITALIC, false));

        List<Component> lore = new ArrayList<>();
        for (String line : type.lore()) {
            lore.add(Component.text(line, NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false));
        }
        lore.add(Component.empty());
        lore.add(Component.text(type.rarity().name(), type.rarity().color())
                .decoration(TextDecoration.ITALIC, false));
        if (type.isActive()) {
            lore.add(Component.text("Active - sneak + right-click to use", NamedTextColor.YELLOW)
                    .decoration(TextDecoration.ITALIC, false));
        } else {
            lore.add(Component.text("Passive - always on while equipped", NamedTextColor.DARK_GRAY)
                    .decoration(TextDecoration.ITALIC, false));
        }
        lore.add(Component.text("Right-click to equip", NamedTextColor.GREEN)
                .decoration(TextDecoration.ITALIC, false));
        meta.lore(lore);

        meta.getPersistentDataContainer().set(plugin.cardKey(), PersistentDataType.STRING, type.name());
        meta.addItemFlags(ItemFlag.values());
        item.setItemMeta(meta);
        return item;
    }

    /** Returns the CardType encoded on this item, or null if it isn't a card. */
    public CardType readCard(ItemStack item) {
        if (item == null || item.getType().isAir() || !item.hasItemMeta()) return null;
        ItemMeta meta = item.getItemMeta();
        String raw = meta.getPersistentDataContainer().get(plugin.cardKey(), PersistentDataType.STRING);
        if (raw == null) return null;
        try {
            return CardType.valueOf(raw);
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    public boolean isCard(ItemStack item) {
        return readCard(item) != null;
    }
}
