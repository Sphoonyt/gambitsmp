package com.gambitsmp.listeners;

import com.gambitsmp.GambitSMP;
import com.gambitsmp.cards.CardType;
import com.gambitsmp.legendary.LegendaryType;
import com.gambitsmp.player.PlayerCardData;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

public class CardEquipListener implements Listener {

    private final GambitSMP plugin;

    public CardEquipListener(GambitSMP plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) return;

        Player player = event.getPlayer();
        // sneak + right-click is reserved for activating already-equipped active cards
        if (player.isSneaking()) return;

        ItemStack item = event.getItem();
        if (item == null) return;

        CardType cardType = plugin.cards().readCard(item);
        if (cardType != null) {
            event.setCancelled(true);
            equipCard(player, item, cardType);
            return;
        }

        LegendaryType legendaryType = plugin.legendaries().readLegendary(item);
        if (legendaryType != null && legendaryType != LegendaryType.VAMPIRIC_FANG) {
            event.setCancelled(true);
            equipLegendary(player, item, legendaryType);
        }
    }

    private void equipCard(Player player, ItemStack item, CardType type) {
        PlayerCardData data = plugin.data().get(player.getUniqueId());

        if (data.isBanned(type)) {
            player.sendMessage(Component.text("You sacrificed this card at a shrine - you can never equip it again.", NamedTextColor.RED));
            return;
        }
        if (data.hasCard(type)) {
            player.sendMessage(Component.text("You already have " + type.displayName() + " equipped.", NamedTextColor.YELLOW));
            return;
        }
        int max = plugin.data().maxEquippedCards();
        if (data.equippedCards().size() >= max) {
            player.sendMessage(Component.text("You can only equip " + max + " cards at once. Unequip one with /gambit cards.", NamedTextColor.RED));
            return;
        }

        data.equippedCards().add(type);
        item.setAmount(item.getAmount() - 1);
        player.sendMessage(Component.text("Equipped card: ", NamedTextColor.GREEN)
                .append(Component.text(type.displayName(), type.rarity().color())));
        player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_PLAYER_LEVELUP, 0.6f, 1.6f);
    }

    private void equipLegendary(Player player, ItemStack item, LegendaryType type) {
        PlayerCardData data = plugin.data().get(player.getUniqueId());
        if (data.hasLegendary(type)) {
            player.sendMessage(Component.text("You already have " + type.itemName() + " equipped.", NamedTextColor.YELLOW));
            return;
        }
        data.equippedLegendaries().add(type);
        item.setAmount(item.getAmount() - 1);
        player.sendMessage(Component.text("Equipped legendary: ", NamedTextColor.GOLD)
                .append(Component.text(type.itemName(), NamedTextColor.LIGHT_PURPLE)));
        player.playSound(player.getLocation(), org.bukkit.Sound.UI_TOAST_CHALLENGE_COMPLETE, 0.8f, 1f);
    }
}
