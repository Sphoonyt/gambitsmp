package com.gambitsmp.listeners;

import com.destroystokyo.paper.event.player.PlayerJumpEvent;
import com.gambitsmp.GambitSMP;
import com.gambitsmp.cards.CardType;
import com.gambitsmp.player.PlayerCardData;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Jet Kick: double jump (two jumps within a short window) to dash in the direction
 * you're looking, damaging anyone you pass through along the way.
 *
 * Not an "active" (sneak+right-click) card - it has its own trigger mechanism, so it
 * deliberately doesn't show up in /ability or respond to sneaking. It still shares
 * the same CardType-keyed cooldown map on PlayerCardData as active cards do, purely
 * for convenience/consistency.
 */
public class JetKickListener implements Listener {

    private final GambitSMP plugin;

    public JetKickListener(GambitSMP plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onJump(PlayerJumpEvent event) {
        Player player = event.getPlayer();
        PlayerCardData data = plugin.data().get(player.getUniqueId());
        if (!data.hasCard(CardType.JET_KICK)) return;

        long now = System.currentTimeMillis();
        long windowMs = plugin.getConfig().getLong("jet-kick.double-jump-window-ms", 400);
        boolean isDoubleJump = (now - data.lastJumpTime) <= windowMs;
        data.lastJumpTime = now;

        if (!isDoubleJump) return;
        if (data.isOnCooldown(CardType.JET_KICK)) {
            player.sendMessage(Component.text("Jet Kick is still on cooldown.", NamedTextColor.GRAY));
            return;
        }

        data.setCooldown(CardType.JET_KICK, CardType.JET_KICK.cooldownSeconds());
        dash(player);
    }

    private void dash(Player player) {
        double dashPower = plugin.getConfig().getDouble("jet-kick.dash-power", 1.8);
        double damageHearts = plugin.getConfig().getInt("jet-kick.damage-hearts", 2);

        Vector direction = player.getLocation().getDirection().normalize();
        player.setVelocity(direction.clone().multiply(dashPower).setY(Math.max(0.15, direction.getY() * dashPower)));

        player.getWorld().playSound(player.getLocation(), Sound.ITEM_TRIDENT_RIPTIDE_1, 1f, 1.4f);
        player.sendMessage(Component.text("Jet Kick!", NamedTextColor.AQUA));

        Set<UUID> alreadyHit = new HashSet<>();
        new BukkitRunnable() {
            int ticks = 0;

            @Override
            public void run() {
                if (!player.isOnline() || ticks >= 8) {
                    cancel();
                    return;
                }
                Location loc = player.getLocation();
                player.getWorld().spawnParticle(Particle.CLOUD, loc, 6, 0.2, 0.1, 0.2, 0.01);

                for (LivingEntity nearby : player.getWorld().getNearbyLivingEntities(loc, 1.3)) {
                    if (nearby.equals(player)) continue;
                    if (!alreadyHit.add(nearby.getUniqueId())) continue;
                    nearby.damage(damageHearts * 2.0, player);
                    nearby.getWorld().spawnParticle(Particle.SWEEP_ATTACK, nearby.getLocation().add(0, 1, 0), 3, 0.2, 0.2, 0.2, 0);
                }
                ticks++;
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }
}
