package com.gambitsmp.dimension;

import com.gambitsmp.GambitSMP;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.List;

public class SpecialMatterManager {

    private static final int CUSTOM_MODEL_DATA = 4010;

    private final GambitSMP plugin;

    public SpecialMatterManager(GambitSMP plugin) {
        this.plugin = plugin;
    }

    public ItemStack createItem() {
        ItemStack item = new ItemStack(Material.ECHO_SHARD);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text("Special Matter", NamedTextColor.LIGHT_PURPLE, TextDecoration.BOLD)
                .decoration(TextDecoration.ITALIC, false));
        meta.lore(List.of(
                Component.text("A fragment of unstable dimensional", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false),
                Component.text("matter. Use it on the lodestone", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false),
                Component.text("beside the portal to open a way", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false),
                Component.text("back to the overworld.", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false)
        ));
        meta.getPersistentDataContainer().set(plugin.specialMatterKey(), PersistentDataType.BYTE, (byte) 1);
        meta.setCustomModelData(CUSTOM_MODEL_DATA);
        meta.addItemFlags(ItemFlag.values());
        item.setItemMeta(meta);
        return item;
    }

    public boolean isSpecialMatter(ItemStack item) {
        if (item == null || item.getType().isAir() || !item.hasItemMeta()) return false;
        return item.getItemMeta().getPersistentDataContainer().has(plugin.specialMatterKey(), PersistentDataType.BYTE);
    }
}
