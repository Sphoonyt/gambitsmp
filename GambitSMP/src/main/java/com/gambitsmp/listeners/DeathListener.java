package com.gambitsmp.listeners;

import com.gambitsmp.GambitSMP;
import com.gambitsmp.cards.CardType;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.List;
import java.util.Random;

/**
 * Handles the kill-drop Mystery Card. The old deathban / permanent heart loss
 * system (and the Shrine of Glory / Shrine of Abysmal altars tied to it) has been
 * removed entirely - dying no longer costs anything beyond the usual vanilla death
 * penalties.
 */
public class DeathListener implements Listener {

    private final GambitSMP plugin;
    private final Random random = new Random();

    public DeathListener(GambitSMP plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        Player killer = player.getKiller();
        if (killer != null && plugin.getConfig().getBoolean("drop.mystery-card-enabled", true)) {
            ItemStack mystery = createMysteryCard();
            player.getWorld().dropItemNaturally(player.getLocation(), mystery);
        }
    }

    private ItemStack createMysteryCard() {
        ItemStack item = new ItemStack(Material.ENCHANTED_BOOK);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text("Mystery Card", NamedTextColor.LIGHT_PURPLE, TextDecoration.BOLD)
                .decoration(TextDecoration.ITALIC, false));
        meta.lore(List.of(
                Component.text("A card dropped in battle.", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false),
                Component.text("Right-click to open it.", NamedTextColor.GREEN).decoration(TextDecoration.ITALIC, false)
        ));

        // pre-roll the hidden card now so it's fixed for whoever opens it
        CardType[] all = CardType.values();
        CardType roll = all[random.nextInt(all.length)];
        meta.getPersistentDataContainer().set(plugin.mysteryDropKey(), PersistentDataType.STRING, roll.name());
        meta.setCustomModelData(3010); // see resource-pack-template/assets/minecraft/models/item/enchanted_book.json
        meta.addItemFlags(ItemFlag.values());
        item.setItemMeta(meta);
        return item;
    }
}
