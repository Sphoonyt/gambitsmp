package com.gambitsmp.util;

import com.gambitsmp.GambitSMP;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Player;

import java.util.Date;

public class HealthUtil {

    private HealthUtil() {}

    public static double heartsToHealth(double hearts) {
        return hearts * 2.0;
    }

    /**
     * Permanently reduces a player's max health by the given number of hearts.
     * If this drops their max health to 0 or below, they are banned from the
     * server as per the GambitSMP elimination rule.
     */
    public static void loseHearts(Player player, double hearts) {
        var attr = player.getAttribute(Attribute.GENERIC_MAX_HEALTH);
        if (attr == null) return;

        double newMax = Math.max(0, attr.getBaseValue() - heartsToHealth(hearts));
        attr.setBaseValue(newMax);

        if (player.getHealth() > newMax) {
            player.setHealth(Math.max(newMax, 0.0));
        }

        player.sendMessage(Component.text("You permanently lost " + hearts + " heart(s)! ("
                + (newMax / 2.0) + " max hearts remaining)", NamedTextColor.RED));

        if (newMax <= 0) {
            eliminate(player);
        }
    }

    /** Permanently increases a player's max health, capped at the configured max-hearts. */
    public static void gainHearts(Player player, double hearts) {
        var attr = player.getAttribute(Attribute.GENERIC_MAX_HEALTH);
        if (attr == null) return;
        double cap = heartsToHealth(GambitSMP.get().getConfig().getInt("max-hearts", 20));
        double newMax = Math.min(cap, attr.getBaseValue() + heartsToHealth(hearts));
        attr.setBaseValue(newMax);
    }

    /**
     * Attempts to spend current health (not max health) as a cost. Returns false and
     * does nothing if the player doesn't have enough health to survive the cost
     * (always leaves at least half a heart).
     */
    public static boolean payCurrentHealth(Player player, double hearts) {
        double cost = heartsToHealth(hearts);
        if (player.getHealth() - cost < 0.5) return false;
        player.setHealth(player.getHealth() - cost);
        return true;
    }

    private static void eliminate(Player player) {
        String reason = "GambitSMP: Eliminated - reached 0 max hearts.";
        Bukkit.getBanList(org.bukkit.BanList.Type.NAME).addBan(player.getName(), reason, (Date) null, "GambitSMP");
        Bukkit.broadcast(Component.text(player.getName() + " has been eliminated from GambitSMP!", NamedTextColor.DARK_RED));
        Bukkit.getScheduler().runTask(GambitSMP.get(), () -> player.kickPlayer(reason));
    }
}
