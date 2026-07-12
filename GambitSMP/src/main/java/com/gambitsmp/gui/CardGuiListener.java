package com.gambitsmp.gui;

import com.gambitsmp.GambitSMP;
import com.gambitsmp.cards.CardType;
import com.gambitsmp.player.PlayerCardData;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Two related card GUIs:
 *
 * - Admin card browser ("/gambit menu", requires gambitsmp.admin): shows every card
 *   ordered by rarity. If the viewer is in Creative mode, clicking a card gives them a
 *   free copy of it (the display slot is never actually emptied). Anyone not in
 *   Creative can look but can't take.
 *
 * - Player equip GUI ("/gambit cards", no permission needed): shows the player's
 *   equipped cards in slots up to the configured max. Dragging a card out of a slot
 *   unequips it; dragging a card item in from your inventory equips it. This is the
 *   no-commands-needed way to manage/unequip cards.
 */
public class CardGuiListener implements Listener {

    private static final String ADMIN_MENU_TITLE = "Card Browser (Creative Only)";
    private static final String EQUIP_GUI_TITLE = "Your Cards";

    private final GambitSMP plugin;

    public CardGuiListener(GambitSMP plugin) {
        this.plugin = plugin;
    }

    // ---------------------------------------------------------------- opening

    public void openAdminMenu(Player player) {
        List<CardType> sorted = new ArrayList<>(List.of(CardType.values()));
        sorted.sort(Comparator.comparingInt(c -> c.rarity().ordinal()));

        int size = Math.max(9, ((sorted.size() + 8) / 9) * 9);
        Inventory inv = Bukkit.createInventory(null, size, Component.text(ADMIN_MENU_TITLE));
        for (int i = 0; i < sorted.size(); i++) {
            inv.setItem(i, plugin.cards().createCard(sorted.get(i)));
        }
        player.openInventory(inv);
    }

    public void openEquipGui(Player player) {
        PlayerCardData data = plugin.data().get(player.getUniqueId());
        int max = plugin.data().maxEquippedCards();
        int size = Math.max(9, ((max + 8) / 9) * 9);

        Inventory inv = Bukkit.createInventory(null, size, Component.text(EQUIP_GUI_TITLE));
        List<CardType> equipped = data.equippedCards();
        for (int i = 0; i < max; i++) {
            if (i < equipped.size()) {
                inv.setItem(i, plugin.cards().createCard(equipped.get(i)));
            }
        }
        ItemStack filler = createFiller();
        for (int i = max; i < size; i++) {
            inv.setItem(i, filler);
        }
        player.openInventory(inv);
    }

    private ItemStack createFiller() {
        ItemStack item = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text(" ", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false));
        item.setItemMeta(meta);
        return item;
    }

    // ---------------------------------------------------------------- clicks

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        Component title = event.getView().title();
        if (Component.text(ADMIN_MENU_TITLE).equals(title)) {
            handleAdminMenuClick(event);
        } else if (Component.text(EQUIP_GUI_TITLE).equals(title)) {
            handleEquipGuiClick(event);
        }
    }

    private void handleAdminMenuClick(InventoryClickEvent event) {
        // Always cancelled - this is a fixed catalog, we never let vanilla physically
        // remove/move the display items. A copy is granted manually instead.
        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player player)) return;

        ItemStack clicked = event.getCurrentItem();
        CardType type = plugin.cards().readCard(clicked);
        if (type == null) return;

        if (player.getGameMode() != GameMode.CREATIVE) {
            player.sendMessage(Component.text("You must be in Creative Mode to take cards from this menu.", NamedTextColor.RED));
            return;
        }

        ItemStack copy = plugin.cards().createCard(type);
        var leftover = player.getInventory().addItem(copy);
        if (!leftover.isEmpty()) {
            player.getWorld().dropItemNaturally(player.getLocation(), leftover.values().iterator().next());
        }
        player.sendMessage(Component.text("Copied: ", NamedTextColor.GREEN)
                .append(Component.text(type.displayName(), type.rarity().color())));
        player.playSound(player.getLocation(), Sound.ENTITY_ITEM_PICKUP, 0.6f, 1.4f);
    }

    private void handleEquipGuiClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        Inventory top = event.getView().getTopInventory();

        if (event.isShiftClick()) {
            event.setCancelled(true);
            player.sendMessage(Component.text("Shift-click is disabled here - drag cards one at a time.", NamedTextColor.YELLOW));
            return;
        }

        ClickType click = event.getClick();
        if (click != ClickType.LEFT && click != ClickType.RIGHT) {
            event.setCancelled(true);
            player.sendMessage(Component.text("Please use normal left/right click here.", NamedTextColor.YELLOW));
            return;
        }

        boolean clickedTop = event.getClickedInventory() != null && event.getClickedInventory().equals(top);
        if (!clickedTop) {
            return; // clicking in their own inventory is always fine
        }

        int max = plugin.data().maxEquippedCards();
        int slot = event.getSlot();
        if (slot >= max) {
            event.setCancelled(true); // filler zone
            return;
        }

        ItemStack cursor = event.getCursor();
        boolean placingSomething = cursor != null && !cursor.getType().isAir();
        if (!placingSomething) {
            return; // picking an existing card up onto an empty cursor = unequip, always fine
        }

        CardType incoming = plugin.cards().readCard(cursor);
        if (incoming == null) {
            event.setCancelled(true);
            player.sendMessage(Component.text("Only card items can go in these slots.", NamedTextColor.RED));
            return;
        }
        if (cursor.getAmount() > 1 && click == ClickType.LEFT) {
            event.setCancelled(true);
            player.sendMessage(Component.text("Place cards one at a time (right-click to place a single card).", NamedTextColor.RED));
            return;
        }

        PlayerCardData data = plugin.data().get(player.getUniqueId());
        if (data.isBanned(incoming)) {
            event.setCancelled(true);
            player.sendMessage(Component.text("You sacrificed that card forever - it can't be equipped.", NamedTextColor.RED));
            return;
        }

        for (int i = 0; i < max; i++) {
            if (i == slot) continue;
            CardType otherType = plugin.cards().readCard(top.getItem(i));
            if (otherType == incoming) {
                event.setCancelled(true);
                player.sendMessage(Component.text("You already have " + incoming.displayName() + " equipped.", NamedTextColor.RED));
                return;
            }
        }
        // valid placement/swap - let the click go through normally
    }

    @EventHandler
    public void onDrag(InventoryDragEvent event) {
        Component title = event.getView().title();
        if (!Component.text(EQUIP_GUI_TITLE).equals(title) && !Component.text(ADMIN_MENU_TITLE).equals(title)) return;

        int topSize = event.getView().getTopInventory().getSize();
        boolean touchesTop = event.getRawSlots().stream().anyMatch(slot -> slot < topSize);
        if (touchesTop) {
            event.setCancelled(true);
            if (event.getWhoClicked() instanceof Player player) {
                player.sendMessage(Component.text("Dragging isn't supported here - use single clicks.", NamedTextColor.YELLOW));
            }
        }
    }

    // ---------------------------------------------------------------- closing / reconcile

    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        Component title = event.getView().title();
        if (!Component.text(EQUIP_GUI_TITLE).equals(title)) return;
        if (!(event.getPlayer() instanceof Player player)) return;

        PlayerCardData data = plugin.data().get(player.getUniqueId());
        int max = plugin.data().maxEquippedCards();
        Inventory top = event.getInventory();

        List<CardType> visible = new ArrayList<>();
        for (int i = 0; i < max; i++) {
            CardType type = plugin.cards().readCard(top.getItem(i));
            if (type != null) visible.add(type);
        }

        List<CardType> before = new ArrayList<>(data.equippedCards());

        // Cards that were equipped but are no longer visible were dragged out - unequip
        // them. The physical item is already sitting in the player's inventory/cursor
        // from the normal click, so no need to give anything back here.
        for (CardType type : before) {
            if (!visible.contains(type)) {
                data.equippedCards().remove(type);
            }
        }

        // Cards that are newly visible were dragged in - equip them. The physical item
        // is consumed (it was only ever real inside this temporary GUI inventory).
        for (CardType type : visible) {
            if (data.hasCard(type)) continue;
            if (data.isBanned(type)) continue;
            if (data.equippedCards().size() >= max) continue;
            data.equippedCards().add(type);
        }

        if (!data.equippedCards().equals(before)) {
            player.sendMessage(Component.text("Your equipped cards have been updated.", NamedTextColor.GREEN));
        }
    }
}
