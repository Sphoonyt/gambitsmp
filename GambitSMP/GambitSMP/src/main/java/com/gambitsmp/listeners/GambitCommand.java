package com.gambitsmp.listeners;

import com.gambitsmp.GambitSMP;
import com.gambitsmp.cards.CardType;
import com.gambitsmp.legendary.LegendaryType;
import com.gambitsmp.player.PlayerCardData;
import com.gambitsmp.shrine.ShrineType;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.attribute.Attribute;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class GambitCommand implements CommandExecutor, TabCompleter {

    private final GambitSMP plugin;

    public GambitCommand(GambitSMP plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sender.sendMessage(Component.text("/gambit give|shrine|cards|unequip|reset|info", NamedTextColor.YELLOW));
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "give" -> handleGive(sender, args);
            case "shrine" -> handleShrine(sender, args);
            case "cards" -> handleCards(sender, args);
            case "unequip" -> handleUnequip(sender, args);
            case "reset" -> handleReset(sender, args);
            case "info" -> sender.sendMessage(Component.text("GambitSMP - custom card SMP plugin", NamedTextColor.AQUA));
            default -> sender.sendMessage(Component.text("Unknown subcommand.", NamedTextColor.RED));
        }
        return true;
    }

    private void handleGive(CommandSender sender, String[] args) {
        if (args.length < 4) {
            sender.sendMessage(Component.text("Usage: /gambit give <card|legendary> <player> <type>", NamedTextColor.RED));
            return;
        }
        Player target = Bukkit.getPlayerExact(args[2]);
        if (target == null) {
            sender.sendMessage(Component.text("Player not found.", NamedTextColor.RED));
            return;
        }
        try {
            if (args[1].equalsIgnoreCase("card")) {
                CardType type = CardType.valueOf(args[3].toUpperCase());
                target.getInventory().addItem(plugin.cards().createCard(type));
            } else if (args[1].equalsIgnoreCase("legendary")) {
                LegendaryType type = LegendaryType.valueOf(args[3].toUpperCase());
                target.getInventory().addItem(plugin.legendaries().createLegendary(type));
            } else {
                sender.sendMessage(Component.text("Type must be 'card' or 'legendary'.", NamedTextColor.RED));
                return;
            }
            sender.sendMessage(Component.text("Given.", NamedTextColor.GREEN));
        } catch (IllegalArgumentException ex) {
            sender.sendMessage(Component.text("Unknown card/legendary name.", NamedTextColor.RED));
        }
    }

    private void handleShrine(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("Only players can place shrines.", NamedTextColor.RED));
            return;
        }
        if (args.length < 3 || !args[1].equalsIgnoreCase("create")) {
            sender.sendMessage(Component.text("Usage: /gambit shrine create <glory|abysmal|opportunity>", NamedTextColor.RED));
            return;
        }
        try {
            ShrineType type = ShrineType.valueOf(args[2].toUpperCase());
            Block target = player.getTargetBlockExact(10);
            if (target == null) {
                sender.sendMessage(Component.text("Look at a block within 10 blocks.", NamedTextColor.RED));
                return;
            }
            plugin.shrines().addShrine(type, target.getLocation());
            sender.sendMessage(Component.text("Shrine of " + type.name() + " created at "
                    + target.getX() + ", " + target.getY() + ", " + target.getZ(), NamedTextColor.GREEN));
        } catch (IllegalArgumentException ex) {
            sender.sendMessage(Component.text("Unknown shrine type.", NamedTextColor.RED));
        }
    }

    private void handleCards(CommandSender sender, String[] args) {
        Player target;
        if (args.length >= 2) {
            target = Bukkit.getPlayerExact(args[1]);
            if (target == null) {
                sender.sendMessage(Component.text("Player not found.", NamedTextColor.RED));
                return;
            }
        } else if (sender instanceof Player p) {
            target = p;
        } else {
            sender.sendMessage(Component.text("Usage: /gambit cards <player>", NamedTextColor.RED));
            return;
        }

        PlayerCardData data = plugin.data().get(target.getUniqueId());
        sender.sendMessage(Component.text("--- " + target.getName() + "'s cards ---", NamedTextColor.GOLD));
        if (data.equippedCards().isEmpty()) {
            sender.sendMessage(Component.text("No cards equipped.", NamedTextColor.GRAY));
        }
        for (CardType type : data.equippedCards()) {
            sender.sendMessage(Component.text("- " + type.displayName(), type.rarity().color()));
        }
        if (!data.equippedLegendaries().isEmpty()) {
            sender.sendMessage(Component.text("Legendaries:", NamedTextColor.LIGHT_PURPLE));
            data.equippedLegendaries().forEach(l -> sender.sendMessage(Component.text("- " + l.itemName(), NamedTextColor.GOLD)));
        }
        var maxAttr = target.getAttribute(Attribute.GENERIC_MAX_HEALTH);
        if (maxAttr != null) {
            sender.sendMessage(Component.text("Max hearts: " + (maxAttr.getBaseValue() / 2.0), NamedTextColor.RED));
        }
    }

    private void handleUnequip(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("Players only.", NamedTextColor.RED));
            return;
        }
        if (args.length < 2) {
            sender.sendMessage(Component.text("Usage: /gambit unequip <card>", NamedTextColor.RED));
            return;
        }
        try {
            CardType type = CardType.valueOf(args[1].toUpperCase());
            PlayerCardData data = plugin.data().get(player.getUniqueId());
            if (!data.hasCard(type)) {
                sender.sendMessage(Component.text("You don't have that card equipped.", NamedTextColor.RED));
                return;
            }
            data.equippedCards().remove(type);
            player.getInventory().addItem(plugin.cards().createCard(type));
            sender.sendMessage(Component.text("Unequipped " + type.displayName() + ".", NamedTextColor.GREEN));
        } catch (IllegalArgumentException ex) {
            sender.sendMessage(Component.text("Unknown card.", NamedTextColor.RED));
        }
    }

    private void handleReset(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(Component.text("Usage: /gambit reset <player>", NamedTextColor.RED));
            return;
        }
        Player target = Bukkit.getPlayerExact(args[1]);
        if (target == null) {
            sender.sendMessage(Component.text("Player not found.", NamedTextColor.RED));
            return;
        }
        PlayerCardData data = plugin.data().get(target.getUniqueId());
        data.equippedCards().clear();
        data.bannedCards().clear();
        data.equippedLegendaries().clear();
        var attr = target.getAttribute(Attribute.GENERIC_MAX_HEALTH);
        if (attr != null) {
            attr.setBaseValue(plugin.getConfig().getInt("max-hearts", 20) * 2.0);
            target.setHealth(attr.getBaseValue());
        }
        sender.sendMessage(Component.text("Reset " + target.getName() + ".", NamedTextColor.GREEN));
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return filter(List.of("give", "shrine", "cards", "unequip", "reset", "info"), args[0]);
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("give")) {
            return filter(List.of("card", "legendary"), args[1]);
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("shrine")) {
            return filter(List.of("create"), args[1]);
        }
        if (args.length == 3 && args[0].equalsIgnoreCase("shrine")) {
            return filter(Arrays.stream(ShrineType.values()).map(Enum::name).collect(Collectors.toList()), args[2]);
        }
        if (args.length == 4 && args[0].equalsIgnoreCase("give") && args[1].equalsIgnoreCase("card")) {
            return filter(Arrays.stream(CardType.values()).map(Enum::name).collect(Collectors.toList()), args[3]);
        }
        if (args.length == 4 && args[0].equalsIgnoreCase("give") && args[1].equalsIgnoreCase("legendary")) {
            return filter(Arrays.stream(LegendaryType.values()).map(Enum::name).collect(Collectors.toList()), args[3]);
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("unequip")) {
            return filter(Arrays.stream(CardType.values()).map(Enum::name).collect(Collectors.toList()), args[1]);
        }
        return new ArrayList<>();
    }

    private List<String> filter(List<String> options, String prefix) {
        return options.stream().filter(o -> o.toLowerCase().startsWith(prefix.toLowerCase())).collect(Collectors.toList());
    }
}
