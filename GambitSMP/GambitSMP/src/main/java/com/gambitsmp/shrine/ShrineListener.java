package com.gambitsmp.shrine;

import com.gambitsmp.GambitSMP;
import com.gambitsmp.cards.CardType;
import com.gambitsmp.player.PlayerCardData;
import com.gambitsmp.util.HealthUtil;
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

import java.util.*;

public class ShrineListener implements Listener {

    private static final String GLORY_TITLE = "Shrine of Glory";
    private static final String ABYSMAL_TITLE = "Shrine of Abysmal";
    private static final String OPPORTUNITY_TITLE = "Shrine of Opportunity";

    private final GambitSMP plugin;
    private final Map<UUID, List<CardType>> gloryOptions = new HashMap<>();
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
        switch (shrine.type()) {
            case GLORY -> openGlory(player);
            case ABYSMAL -> openAbysmal(player);
            case OPPORTUNITY -> openOpportunity(player);
        }
    }

    private void openGlory(Player player) {
        PlayerCardData data = plugin.data().get(player.getUniqueId());
        if (data.equippedCards().isEmpty()) {
            player.sendMessage(Component.text("You have no equipped cards to sacrifice.", NamedTextColor.RED));
            return;
        }
        Inventory inv = Bukkit.createInventory(null, 27, Component.text(GLORY_TITLE));
        List<CardType> options = new ArrayList<>(data.equippedCards());
        gloryOptions.put(player.getUniqueId(), options);
        for (int i = 0; i < options.size() && i < 27; i++) {
            ItemStack card = plugin.cards().createCard(options.get(i));
            addLore(card, "Sacrifice permanently for +1 max heart", NamedTextColor.RED);
            inv.setItem(i, card);
        }
        player.openInventory(inv);
    }

    private void openAbysmal(Player player) {
        int cost = plugin.getConfig().getInt("shrines.abysmal-heart-cost", 1);
        Inventory inv = Bukkit.createInventory(null, 9, Component.text(ABYSMAL_TITLE));
        ItemStack confirm = new ItemStack(Material.REDSTONE);
        ItemMeta meta = confirm.getItemMeta();
        meta.displayName(Component.text("Pay " + cost + " heart(s) for a random card", NamedTextColor.RED)
                .decoration(TextDecoration.ITALIC, false));
        confirm.setItemMeta(meta);
        inv.setItem(4, confirm);
        player.openInventory(inv);
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
        String title = plainTitle(event);
        if (title == null) return;
        event.setCancelled(true);
        Player player = (Player) event.getWhoClicked();
        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType().isAir()) return;

        switch (title) {
            case GLORY_TITLE -> handleGlory(player, event.getSlot());
            case ABYSMAL_TITLE -> handleAbysmal(player);
            case OPPORTUNITY_TITLE -> handleOpportunity(player, event.getSlot());
            default -> {}
        }
    }

    private String plainTitle(InventoryClickEvent event) {
        Component title = event.getView().title();
        for (String t : List.of(GLORY_TITLE, ABYSMAL_TITLE, OPPORTUNITY_TITLE)) {
            if (Component.text(t).equals(title)) return t;
        }
        return null;
    }

    private void handleGlory(Player player, int slot) {
        List<CardType> options = gloryOptions.get(player.getUniqueId());
        if (options == null || slot >= options.size()) return;
        CardType chosen = options.get(slot);

        PlayerCardData data = plugin.data().get(player.getUniqueId());
        data.banCard(chosen); // removes from equipped and blocks it forever
        int gain = plugin.getConfig().getInt("shrines.glory-heart-gain", 1);
        HealthUtil.gainHearts(player, gain);
        player.closeInventory();
        player.sendMessage(Component.text("You sacrificed " + chosen.displayName() + " forever for +" + gain + " max heart.", NamedTextColor.GOLD));
    }

    private void handleAbysmal(Player player) {
        int cost = plugin.getConfig().getInt("shrines.abysmal-heart-cost", 1);
        player.closeInventory();
        if (!HealthUtil.payCurrentHealth(player, cost)) {
            player.sendMessage(Component.text("You don't have enough health to pay that cost.", NamedTextColor.RED));
            return;
        }
        CardType roll = randomCard();
        player.getInventory().addItem(plugin.cards().createCard(roll));
        player.sendMessage(Component.text("The shrine grants you: ", NamedTextColor.DARK_RED)
                .append(Component.text(roll.displayName(), roll.rarity().color())));
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
