package com.gambitsmp.listeners;

import com.gambitsmp.GambitSMP;
import com.gambitsmp.cards.CardType;
import com.gambitsmp.player.PlayerCardData;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerExpChangeEvent;
import org.bukkit.event.player.PlayerItemDamageEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class MovementListener implements Listener {

    private final GambitSMP plugin;
    private final Map<UUID, Long> piggysWeightCooldown = new HashMap<>();

    public MovementListener(GambitSMP plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onFallDamage(EntityDamageEvent event) {
        if (event.getCause() != EntityDamageEvent.DamageCause.FALL) return;
        if (!(event.getEntity() instanceof Player player)) return;
        PlayerCardData data = plugin.data().get(player.getUniqueId());

        // Feather: a soft poof of cloud particles + a light landing sound instead of
        // total silence, so it's clear the card caught you rather than the fall just
        // not having hurt for some other reason.
        if (data.hasCard(CardType.FEATHER)) {
            event.setCancelled(true);
            player.getWorld().spawnParticle(Particle.CLOUD, player.getLocation(), 12, 0.4, 0.1, 0.4, 0.02);
            player.getWorld().playSound(player.getLocation(), Sound.ENTITY_PLAYER_SMALL_FALL, 0.5f, 1.3f);
            return;
        }

        if (data.hasCard(CardType.PIGGYS_WEIGHT) && event.getDamage() >= 5.0) { // roughly 10+ blocks
            long ready = piggysWeightCooldown.getOrDefault(player.getUniqueId(), 0L);
            if (System.currentTimeMillis() < ready) return; // on cooldown - takes normal fall damage instead
            int cooldownSeconds = plugin.getConfig().getInt("cooldowns.piggys-weight-seconds", 10);
            piggysWeightCooldown.put(player.getUniqueId(), System.currentTimeMillis() + cooldownSeconds * 1000L);

            event.setCancelled(true);
            player.getWorld().spawnParticle(Particle.EXPLOSION_LARGE, player.getLocation(), 1);
            player.getWorld().playSound(player.getLocation(), Sound.ENTITY_GENERIC_EXPLODE, 1f, 0.7f);
            // a little character on top of the shockwave, since it IS called "Piggy's Weight"
            player.getWorld().playSound(player.getLocation(), Sound.ENTITY_PIG_AMBIENT, 0.8f, 0.6f);
            for (LivingEntity nearby : player.getWorld().getNearbyLivingEntities(player.getLocation(), 3)) {
                if (nearby.equals(player)) continue;
                nearby.damage(6.0, player); // 3 hearts
            }
        }
    }

    @EventHandler
    public void onArmorDamage(PlayerItemDamageEvent event) {
        Player player = event.getPlayer();
        PlayerCardData data = plugin.data().get(player.getUniqueId());
        if (!data.hasCard(CardType.REINFORCED_ARMOR)) return;
        if (!isArmor(event.getItem())) return;
        int reduced = Math.max(1, event.getDamage() / 2);
        event.setDamage(reduced);
        // Cheap, quiet spark to mark "armor absorbed some of that" - deliberately no
        // sound here since it can trigger many times per fight.
        player.getWorld().spawnParticle(Particle.CRIT_MAGIC, player.getLocation().add(0, 1, 0), 4, 0.2, 0.3, 0.2, 0);
    }

    private boolean isArmor(ItemStack item) {
        String name = item.getType().name();
        return name.endsWith("_HELMET") || name.endsWith("_CHESTPLATE")
                || name.endsWith("_LEGGINGS") || name.endsWith("_BOOTS");
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        PlayerCardData data = plugin.data().get(player.getUniqueId());

        // Frozen Anchor: block actual position changes while frozen, but still let the
        // player look around freely. This replaces the old Jump Boost potion hack, which
        // could accidentally launch frozen players into the air instead of stopping them.
        if (data.frozenUntil > System.currentTimeMillis()) {
            Location from = event.getFrom();
            Location to = event.getTo();
            if (to != null && (from.getX() != to.getX() || from.getY() != to.getY() || from.getZ() != to.getZ())) {
                Location held = from.clone();
                held.setYaw(to.getYaw());
                held.setPitch(to.getPitch());
                event.setTo(held);
            }
            return;
        }

        if (!data.hasCard(CardType.SPIDER)) return;

        Material feet = player.getLocation().getBlock().getType();
        boolean inWeb = feet == Material.COBWEB;
        float target = inWeb ? 0.3f : 0.2f;
        if (player.getWalkSpeed() != target) {
            player.setWalkSpeed(target);
        }

        // Only fire feedback on the actual transition into/out of a web, not every
        // single tick while standing in one.
        if (inWeb && !data.inWebBoost) {
            data.inWebBoost = true;
            player.getWorld().playSound(player.getLocation(), Sound.BLOCK_WOOL_BREAK, 0.4f, 1.6f);
            player.getWorld().spawnParticle(Particle.CRIT, player.getLocation().add(0, 0.3, 0), 6, 0.2, 0.1, 0.2, 0);
        } else if (!inWeb && data.inWebBoost) {
            data.inWebBoost = false;
        }
    }

    @EventHandler
    public void onExpChange(PlayerExpChangeEvent event) {
        Player player = event.getPlayer();
        PlayerCardData data = plugin.data().get(player.getUniqueId());
        if (!data.hasCard(CardType.GREENS_TOUCH)) return;
        event.setAmount((int) Math.ceil(event.getAmount() * 1.5));
        player.getWorld().spawnParticle(Particle.ENCHANTMENT_TABLE, player.getLocation().add(0, 1.2, 0), 10, 0.3, 0.3, 0.3, 1);
    }
}
