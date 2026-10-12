package com.gambitsmp.listeners;

import com.gambitsmp.GambitSMP;
import com.gambitsmp.cards.ActiveCardService;
import com.gambitsmp.cards.CardType;
import com.gambitsmp.player.PlayerCardData;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class AbilityCommand implements CommandExecutor, TabCompleter {

    private final GambitSMP plugin;

    public AbilityCommand(GambitSMP plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("Players only.", NamedTextColor.RED));
            return true;
        }

        PlayerCardData data = plugin.data().get(player.getUniqueId());
        List<CardType> active = plugin.activeCards().activeCards(data);

        if (args.length == 0) {
            if (active.isEmpty()) {
                player.sendMessage(Component.text("You have no active cards equipped.", NamedTextColor.RED));
                return true;
            }
            player.sendMessage(Component.text("--- Your active cards ---", NamedTextColor.GOLD));
            for (int i = 0; i < active.size(); i++) {
                CardType type = active.get(i);
                String status = data.isOnCooldown(type)
                        ? "(cooldown: " + data.cooldownRemainingSeconds(type) + "s)"
                        : "(ready)";
                player.sendMessage(Component.text("/ability " + (i + 1) + " -> ", NamedTextColor.YELLOW)
                        .append(Component.text(type.displayName(), type.rarity().color()))
                        .append(Component.text(" " + status, NamedTextColor.GRAY)));
            }
            return true;
        }

        int slot;
        try {
            slot = Integer.parseInt(args[0]);
        } catch (NumberFormatException ex) {
            player.sendMessage(Component.text("Usage: /ability <slot number>", NamedTextColor.RED));
            return true;
        }

        ActiveCardService.TriggerResult result = plugin.activeCards().triggerSlot(player, slot);
        switch (result) {
            case NO_ACTIVE_CARDS -> player.sendMessage(Component.text("You have no active cards equipped.", NamedTextColor.RED));
            case INVALID_SLOT -> player.sendMessage(Component.text("You only have " + active.size() + " active card(s). Use /ability to list them.", NamedTextColor.RED));
            case ON_COOLDOWN -> player.sendMessage(Component.text("That ability is still on cooldown.", NamedTextColor.RED));
            case SUCCESS, FIZZLED -> {} // trigger() already sends its own feedback message either way
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length != 1 || !(sender instanceof Player player)) return new ArrayList<>();
        PlayerCardData data = plugin.data().get(player.getUniqueId());
        int count = plugin.activeCards().activeCards(data).size();
        List<String> options = new ArrayList<>();
        for (int i = 1; i <= count; i++) options.add(String.valueOf(i));
        return options;
    }
}
