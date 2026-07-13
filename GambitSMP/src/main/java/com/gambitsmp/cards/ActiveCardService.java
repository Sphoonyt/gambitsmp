package com.gambitsmp.cards;

import com.gambitsmp.GambitSMP;
import com.gambitsmp.player.PlayerCardData;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * All logic for actually firing an active card's effect. Both the sneak+right-click
 * listener and the /ability command call into this, so there's exactly one
 * implementation of each ability to keep them in sync.
 *
 * Every ability plays its sound/particle at the WORLD level (not just to the
 * caster) so nearby players get a genuine tell that something just happened -
 * important on a PvP-focused SMP where "did they just pop an ability" matters.
 *
 * Nerf: activating an active card is no longer guaranteed even when off cooldown -
 * there's now a flat chance (chances.active-ability-trigger) that it just fizzles.
 * The cooldown is consumed either way, so spam-retrying isn't a way around it.
 */
public class ActiveCardService {

    private final GambitSMP plugin;
    private final Random random = new Random();

    public ActiveCardService(GambitSMP plugin) {
        this.plugin = plugin;
    }

    /** Returns the list of this player's equipped ACTIVE cards, in equip order. */
    public List<CardType> activeCards(PlayerCardData data) {
        List<CardType> result = new ArrayList<>();
        for (CardType type : data.equippedCards()) {
            if (type.isActive()) result.add(type);
        }
        return result;
    }

    /** Fires the first equipped active card that's off cooldown. Used by sneak+right-click. */
    public boolean triggerNextReady(Player player) {
        PlayerCardData data = plugin.data().get(player.getUniqueId());
        for (CardType type : activeCards(data)) {
            if (data.isOnCooldown(type)) continue;
            trigger(player, data, type);
            return true; // attempted (fizzle or success) - either way the interact event should be consumed
        }
        return false;
    }

    /**
     * Fires the Nth equipped active card (1-based), regardless of order relative to
     * other cards. Used by /ability &lt;slot&gt;.
     */
    public TriggerResult triggerSlot(Player player, int slot) {
        PlayerCardData data = plugin.data().get(player.getUniqueId());
        List<CardType> active = activeCards(data);
        if (active.isEmpty()) return TriggerResult.NO_ACTIVE_CARDS;
        if (slot < 1 || slot > active.size()) return TriggerResult.INVALID_SLOT;

        CardType type = active.get(slot - 1);
        if (data.isOnCooldown(type)) return TriggerResult.ON_COOLDOWN;

        return trigger(player, data, type) ? TriggerResult.SUCCESS : TriggerResult.FIZZLED;
    }

    public enum TriggerResult {
        SUCCESS, FIZZLED, NO_ACTIVE_CARDS, INVALID_SLOT, ON_COOLDOWN
    }

    /** Returns true if the ability actually fired, false if it fizzled. Cooldown is consumed either way. */
    private boolean trigger(Player player, PlayerCardData data, CardType type) {
        data.setCooldown(type, type.cooldownSeconds());

        double chance = plugin.getConfig().getDouble("chances.active-ability-trigger", 0.15);
        if (random.nextDouble() >= chance) {
            player.sendMessage(Component.text(type.displayName() + " fizzled...", NamedTextColor.GRAY));
            player.playSound(player.getLocation(), Sound.BLOCK_FIRE_EXTINGUISH, 0.6f, 1.2f);
            return false;
        }

        switch (type) {
            case BERSERK -> fireBerserk(player, data);
            case NEGATION -> fireNegation(player, data);
            case NIGHTBRINGER -> fireNightbringer(player);
            case WARDEN_BEAM -> fireWardenBeam(player);
            case GRAVITY -> fireGravity(player);
            default -> {}
        }
        return true;
    }

    // A furious roar + rising flame-and-smoke burst around the caster - reads as
    // "this person just got dangerous," audible/visible to anyone nearby.
    private void fireBerserk(Player player, PlayerCardData data) {
        data.berserkActiveUntil = System.currentTimeMillis() + 5000;
        player.sendMessage(Component.text("Berserk activated! +50% damage for 5s.", NamedTextColor.RED));
        Location loc = player.getLocation();
        player.getWorld().playSound(loc, Sound.ENTITY_RAVAGER_ROAR, 1f, 1.4f);
        player.getWorld().spawnParticle(Particle.FLAME, loc.clone().add(0, 1, 0), 30, 0.4, 0.6, 0.4, 0.02);
        player.getWorld().spawnParticle(Particle.VILLAGER_ANGRY, loc.clone().add(0, 2.1, 0), 3, 0.2, 0.1, 0.2, 0);
    }

    // A calm chime + totem-style shimmer - the classic "I cannot be touched right now" tell.
    private void fireNegation(Player player, PlayerCardData data) {
        data.healthPausedUntil = System.currentTimeMillis() + 5000;
        data.pausedHealthValue = player.getHealth();
        player.sendMessage(Component.text("Negation activated! Your health is frozen for 5s.", NamedTextColor.AQUA));
        Location loc = player.getLocation();
        player.getWorld().playSound(loc, Sound.BLOCK_BEACON_ACTIVATE, 1f, 1.2f);
        player.getWorld().spawnParticle(Particle.TOTEM, loc.clone().add(0, 1, 0), 40, 0.4, 0.8, 0.4, 0.3);
    }

    // Dark ink cloud + a low wither growl - unmistakably a curse being cast.
    private void fireNightbringer(Player player) {
        int hit = 0;
        // Hits every nearby living entity (players AND mobs) except the caster, so the
        // card visibly does something even when testing solo against mobs.
        for (LivingEntity nearby : player.getWorld().getNearbyLivingEntities(player.getLocation(), 8)) {
            if (nearby.equals(player)) continue;
            nearby.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, 100, 0));
            nearby.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 100, 0));
            hit++;
        }
        player.getWorld().spawnParticle(Particle.SQUID_INK, player.getLocation(), 40, 4, 2, 4, 0);
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_WITHER_AMBIENT, 1f, 0.6f);
        if (hit > 0) {
            player.sendMessage(Component.text("Nightbringer unleashed! Cursed " + hit + " nearby target(s).", NamedTextColor.DARK_PURPLE));
        } else {
            player.sendMessage(Component.text("Nightbringer unleashed! (nothing was in range)", NamedTextColor.DARK_PURPLE));
        }
    }

    // Sonic-boom particle trail along the beam's path + the Warden's own roar - already
    // about as unmistakable as it gets.
    private void fireWardenBeam(Player player) {
        Location eye = player.getEyeLocation();
        Vector direction = eye.getDirection().normalize();
        Location cursor = eye.clone();
        List<LivingEntity> hit = new ArrayList<>();

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
            Vector knockback = le.getLocation().toVector().subtract(player.getLocation().toVector()).normalize().multiply(1.5);
            le.setVelocity(knockback.setY(0.4));
        }
        player.getWorld().playSound(eye, Sound.ENTITY_WARDEN_SONIC_BOOM, 2f, 1f);
        player.sendMessage(Component.text("You unleash a warden beam!", NamedTextColor.DARK_AQUA));
    }

    // Portal-swirl pull effect + an Enderman-teleport-style warp sound (previously had
    // no sound at all, which made it easy to miss entirely).
    private void fireGravity(Player player) {
        for (LivingEntity nearby : player.getWorld().getNearbyLivingEntities(player.getLocation(), 10)) {
            if (nearby.equals(player) || !(nearby instanceof Player)) continue;
            Vector pull = player.getLocation().toVector().subtract(nearby.getLocation().toVector());
            if (pull.lengthSquared() < 0.01) continue;
            pull.normalize().multiply(1.4);
            nearby.setVelocity(pull.setY(Math.max(0.2, pull.getY())));
        }
        Location loc = player.getLocation();
        player.getWorld().spawnParticle(Particle.PORTAL, loc, 80, 3, 1, 3, 0.3);
        player.getWorld().playSound(loc, Sound.ENTITY_ENDERMAN_TELEPORT, 1.2f, 0.6f);
        player.sendMessage(Component.text("Gravity pulls everything toward you!", NamedTextColor.LIGHT_PURPLE));
    }
}
