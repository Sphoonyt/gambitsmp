package com.gambitsmp.shrine;

import com.gambitsmp.GambitSMP;
import com.gambitsmp.cards.CardType;
import com.gambitsmp.player.PlayerCardData;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;

/**
 * Shrine of Glory and Shrine of Abysmal were removed along with the deathban/heart
 * system they were tied to. Only Shrine of Opportunity remains - reroll one of your
 * equipped cards into a new random one, no health cost involved.
 */
public class ShrineListener implements Listener {

    private static final String OPPORTUNITY_TITLE = "Shrine of Opportunity";

    private final GambitSMP plugin;
    private final Map<UUID, List<CardType>> opportunityOptions = new HashMap<>();
    private final Random random = new Random();

    public ShrineListener(GambitSMP plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        if (event.getClickedBlock() == null) return;

        ShrineManager.Shrine shrine = plugin.shrines().findShrine(event.getClickedBlock().getLocation());
        if (shrine == null) return;

        event.setCancelled(true);
        Player player = event.getPlayer();
        if (shrine.type() == ShrineType.OPPORTUNITY) {
            openOpportunity(player);
        }
    }

    private void openOpportunity(Player player) {
        PlayerCardData data = plugin.data().get(player.getUniqueId());
        if (data.equippedCards().isEmpty()) {
            player.sendMessage(Component.text("You have no equipped cards to reroll.", NamedTextColor.RED));
            return;
        }
        Inventory inv = Bukkit.createInventory(null, 27, Component.text(OPPORTUNITY_TITLE));
        List<CardType> options = new ArrayList<>(data.equippedCards());
        opportunityOptions.put(player.getUniqueId(), options);
        for (int i = 0; i < options.size() && i < 27; i++) {
            ItemStack card = plugin.cards().createCard(options.get(i));
            addLore(card, "Reroll into a random new card", NamedTextColor.LIGHT_PURPLE);
            inv.setItem(i, card);
        }
        player.openInventory(inv);
    }

    private void addLore(ItemStack item, String line, NamedTextColor color) {
        ItemMeta meta = item.getItemMeta();
        List<Component> lore = meta.lore() != null ? new ArrayList<>(meta.lore()) : new ArrayList<>();
        lore.add(Component.empty());
        lore.add(Component.text(line, color).decoration(TextDecoration.ITALIC, false));
        meta.lore(lore);
        item.setItemMeta(meta);
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!Component.text(OPPORTUNITY_TITLE).equals(event.getView().title())) return;
        event.setCancelled(true);
        Player player = (Player) event.getWhoClicked();
        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType().isAir()) return;

        handleOpportunity(player, event.getSlot());
    }

    private void handleOpportunity(Player player, int slot) {
        List<CardType> options = opportunityOptions.get(player.getUniqueId());
        if (options == null || slot >= options.size()) return;
        CardType oldCard = options.get(slot);

        PlayerCardData data = plugin.data().get(player.getUniqueId());
        CardType newCard;
        int attempts = 0;
        do {
            newCard = randomCard();
            attempts++;
        } while ((data.hasCard(newCard) || data.isBanned(newCard) || newCard == oldCard) && attempts < 50);

        data.equippedCards().remove(oldCard);
        data.equippedCards().add(newCard);
        player.closeInventory();
        player.sendMessage(Component.text("Rerolled ", NamedTextColor.LIGHT_PURPLE)
                .append(Component.text(oldCard.displayName(), oldCard.rarity().color()))
                .append(Component.text(" into ", NamedTextColor.LIGHT_PURPLE))
                .append(Component.text(newCard.displayName(), newCard.rarity().color())));
    }

    private CardType randomCard() {
        CardType[] all = CardType.values();
        return all[random.nextInt(all.length)];
    }
}
