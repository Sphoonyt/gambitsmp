package com.gambitsmp.listeners;

import com.gambitsmp.GambitSMP;
import com.gambitsmp.cards.CardType;
import com.gambitsmp.player.PlayerCardData;
import org.bukkit.Material;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerExpChangeEvent;
import org.bukkit.event.player.PlayerItemDamageEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class MovementListener implements Listener {

    private final GambitSMP plugin;

    public MovementListener(GambitSMP plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onFallDamage(EntityDamageEvent event) {
        if (event.getCause() != EntityDamageEvent.DamageCause.FALL) return;
        if (!(event.getEntity() instanceof Player player)) return;
        PlayerCardData data = plugin.data().get(player.getUniqueId());

        if (data.hasCard(CardType.FEATHER)) {
            event.setCancelled(true);
            return;
        }

        if (data.hasCard(CardType.PIGGYS_WEIGHT) && event.getDamage() >= 5.0) { // roughly 10+ blocks
            event.setCancelled(true);
            player.getWorld().spawnParticle(org.bukkit.Particle.EXPLOSION_LARGE, player.getLocation(), 1);
            player.getWorld().playSound(player.getLocation(), org.bukkit.Sound.ENTITY_GENERIC_EXPLODE, 1f, 0.7f);
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
    }

    private boolean isArmor(ItemStack item) {
        String name = item.getType().name();
        return name.endsWith("_HELMET") || name.endsWith("_CHESTPLATE")
                || name.endsWith("_LEGGINGS") || name.endsWith("_BOOTS");
    }

    @EventHandler
    public void onMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        PlayerCardData data = plugin.data().get(player.getUniqueId());
        if (!data.hasCard(CardType.SPIDER)) return;

        Material feet = player.getLocation().getBlock().getType();
        boolean inWeb = feet == Material.COBWEB;
        float target = inWeb ? 0.3f : 0.2f;
        if (player.getWalkSpeed() != target) {
            player.setWalkSpeed(target);
        }
    }

    @EventHandler
    public void onSneak(PlayerToggleSneakEvent event) {
        Player player = event.getPlayer();
        PlayerCardData data = plugin.data().get(player.getUniqueId());
        if (!data.hasCard(CardType.PHANTOM)) return;

        if (event.isSneaking()) {
            player.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, Integer.MAX_VALUE, 0, true, false));
            data.phantomActive = true;
        } else if (data.phantomActive) {
            player.removePotionEffect(PotionEffectType.INVISIBILITY);
            data.phantomActive = false;
        }
    }

    @EventHandler
    public void onExpChange(PlayerExpChangeEvent event) {
        Player player = event.getPlayer();
        PlayerCardData data = plugin.data().get(player.getUniqueId());
        if (!data.hasCard(CardType.GREENS_TOUCH)) return;
        event.setAmount((int) Math.ceil(event.getAmount() * 1.5));
    }
}
