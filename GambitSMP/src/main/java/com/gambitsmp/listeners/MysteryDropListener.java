package com.gambitsmp.listeners;

import com.gambitsmp.GambitSMP;
import com.gambitsmp.cards.CardType;
import com.gambitsmp.player.PlayerCardData;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class MysteryDropListener implements Listener {

    private static final String GUI_TITLE = "Mystery Card";
    private final GambitSMP plugin;
    private final Map<UUID, CardType> pendingChoice = new HashMap<>();

    public MysteryDropListener(GambitSMP plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        ItemStack item = event.getItem();
        if (item == null || !item.hasItemMeta()) return;
        ItemMeta meta = item.getItemMeta();
        String raw = meta.getPersistentDataContainer().get(plugin.mysteryDropKey(), PersistentDataType.STRING);
        if (raw == null) return;

        event.setCancelled(true);
        CardType type;
        try {
            type = CardType.valueOf(raw);
        } catch (IllegalArgumentException ex) {
            return;
        }

        Player player = event.getPlayer();
        item.setAmount(item.getAmount() - 1);
        pendingChoice.put(player.getUniqueId(), type);
        player.openInventory(buildGui(type));
    }

    private Inventory buildGui(CardType type) {
        Inventory inv = Bukkit.createInventory(null, 9, Component.text(GUI_TITLE));

        ItemStack use = new ItemStack(Material.LIME_DYE);
        ItemMeta useMeta = use.getItemMeta();
        useMeta.displayName(Component.text("Use now: " + type.displayName(), NamedTextColor.GREEN)
                .decoration(TextDecoration.ITALIC, false));
        useMeta.lore(List.of(Component.text("Equip this card immediately.", NamedTextColor.GRAY)
                .decoration(TextDecoration.ITALIC, false)));
        use.setItemMeta(useMeta);
        inv.setItem(3, use);

        ItemStack keep = new ItemStack(Material.CHEST);
        ItemMeta keepMeta = keep.getItemMeta();
        keepMeta.displayName(Component.text("Keep in inventory", NamedTextColor.YELLOW)
                .decoration(TextDecoration.ITALIC, false));
        keepMeta.lore(List.of(Component.text("Receive the physical card item", NamedTextColor.GRAY)
                        .decoration(TextDecoration.ITALIC, false),
                Component.text("to equip later.", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false)));
        keep.setItemMeta(keepMeta);
        inv.setItem(5, keep);

        return inv;
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!Component.text(GUI_TITLE).equals(event.getView().title())) return;
        event.setCancelled(true);

        Player player = (Player) event.getWhoClicked();
        CardType type = pendingChoice.remove(player.getUniqueId());
        if (type == null || event.getCurrentItem() == null) return;

        Material clicked = event.getCurrentItem().getType();
        player.closeInventory();

        if (clicked == Material.LIME_DYE) {
            PlayerCardData data = plugin.data().get(player.getUniqueId());
            if (data.isBanned(type)) {
                player.sendMessage(Component.text("You've sacrificed that card forever - it fades away.", NamedTextColor.RED));
                return;
            }
            if (data.hasCard(type)) {
                player.sendMessage(Component.text("You already have " + type.displayName() + " - it fades away.", NamedTextColor.YELLOW));
                return;
            }
            int max = plugin.data().maxEquippedCards();
            if (data.equippedCards().size() >= max) {
                player.sendMessage(Component.text("Your card slots are full! Giving you the physical card instead.", NamedTextColor.RED));
                player.getInventory().addItem(plugin.cards().createCard(type));
                return;
            }
            data.equippedCards().add(type);
            player.sendMessage(Component.text("Equipped card: ", NamedTextColor.GREEN)
                    .append(Component.text(type.displayName(), type.rarity().color())));
        } else if (clicked == Material.CHEST) {
            player.getInventory().addItem(plugin.cards().createCard(type));
            player.sendMessage(Component.text("Stored " + type.displayName() + " in your inventory.", NamedTextColor.YELLOW));
        }
    }
}
