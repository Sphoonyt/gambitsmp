package com.gambitsmp.listeners;

import com.gambitsmp.GambitSMP;
import com.gambitsmp.cards.CardType;
import com.gambitsmp.player.PlayerCardData;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import java.util.List;

public class ActiveCardListener implements Listener {

    private final GambitSMP plugin;

    public ActiveCardListener(GambitSMP plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onActivate(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        Player player = event.getPlayer();
        if (!player.isSneaking()) return;
        if (event.getItem() != null && !event.getItem().getType().isAir()) return; // must be empty hand

        PlayerCardData data = plugin.data().get(player.getUniqueId());
        for (CardType type : data.equippedCards()) {
            if (!type.isActive()) continue;
            if (data.isOnCooldown(type)) continue;
            event.setCancelled(true);
            trigger(player, data, type);
            return; // only fire one card per click
        }
    }

    private void trigger(Player player, PlayerCardData data, CardType type) {
        int cooldown = type.cooldownSeconds();
        switch (type) {
            case BERSERK -> {
                data.berserkActiveUntil = System.currentTimeMillis() + 5000;
                player.sendMessage(Component.text("Berserk activated! +50% damage for 5s.", NamedTextColor.RED));
                player.playSound(player.getLocation(), Sound.ENTITY_RAVAGER_ROAR, 1f, 1.4f);
            }
            case NEGATION -> {
                data.healthPausedUntil = System.currentTimeMillis() + 5000;
                data.pausedHealthValue = player.getHealth();
                player.sendMessage(Component.text("Negation activated! Your health is frozen for 5s.", NamedTextColor.AQUA));
                player.playSound(player.getLocation(), Sound.BLOCK_BEACON_ACTIVATE, 1f, 1.2f);
            }
            case NIGHTBRINGER -> {
                for (LivingEntity nearby : player.getWorld().getNearbyLivingEntities(player.getLocation(), 8)) {
                    if (nearby.equals(player) || !(nearby instanceof Player)) continue;
                    nearby.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, 100, 0));
                    nearby.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 100, 0));
                }
                player.getWorld().spawnParticle(Particle.SQUID_INK, player.getLocation(), 40, 4, 2, 4, 0);
                player.sendMessage(Component.text("Nightbringer unleashed!", NamedTextColor.DARK_PURPLE));
            }
            case WARDEN_BEAM -> fireWardenBeam(player);
            case GRAVITY -> {
                for (LivingEntity nearby : player.getWorld().getNearbyLivingEntities(player.getLocation(), 10)) {
                    if (nearby.equals(player) || !(nearby instanceof Player)) continue;
                    Vector pull = player.getLocation().toVector().subtract(nearby.getLocation().toVector());
                    if (pull.lengthSquared() < 0.01) continue;
                    pull.normalize().multiply(1.4);
                    nearby.setVelocity(pull.setY(Math.max(0.2, pull.getY())));
                }
                player.getWorld().spawnParticle(Particle.PORTAL, player.getLocation(), 80, 3, 1, 3, 0.3);
                player.sendMessage(Component.text("Gravity pulls everything toward you!", NamedTextColor.LIGHT_PURPLE));
            }
            default -> {}
        }
        data.setCooldown(type, cooldown);
    }

    private void fireWardenBeam(Player player) {
        Location eye = player.getEyeLocation();
        Vector direction = eye.getDirection().normalize();
        Location cursor = eye.clone();
        List<LivingEntity> hit = new java.util.ArrayList<>();

        for (int i = 0; i < 20; i++) {
            cursor.add(direction.clone().multiply(1.0));
            player.getWorld().spawnParticle(Particle.SONIC_BOOM, cursor, 1, 0, 0, 0, 0);
            for (LivingEntity le : player.getWorld().getNearbyLivingEntities(cursor, 1.5)) {
                if (le.equals(player) || hit.contains(le)) continue;
                hit.add(le);
            }
        }

        for (LivingEntity le : hit) {
            le.damage(10.0, player);
            var attr = le.getAttribute(Attribute.GENERIC_MAX_HEALTH);
            Vector knockback = le.getLocation().toVector().subtract(player.getLocation().toVector()).normalize().multiply(1.5);
            le.setVelocity(knockback.setY(0.4));
        }
        player.getWorld().playSound(eye, Sound.ENTITY_WARDEN_SONIC_BOOM, 2f, 1f);
        player.sendMessage(Component.text("You unleash a warden beam!", NamedTextColor.DARK_AQUA));
    }
}
