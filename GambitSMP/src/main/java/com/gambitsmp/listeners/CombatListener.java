package com.gambitsmp.listeners;

import com.gambitsmp.GambitSMP;
import com.gambitsmp.cards.CardType;
import com.gambitsmp.legendary.LegendaryType;
import com.gambitsmp.player.PlayerCardData;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.Random;
import java.util.UUID;

public class CombatListener implements Listener {

    private final GambitSMP plugin;
    private final Random random = new Random();

    // internal per-player cooldown tracking for on-hit cards that need pacing (not player-facing)
    private final java.util.Map<UUID, Long> vampiricCooldown = new java.util.HashMap<>();

    public CombatListener(GambitSMP plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onDamage(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player attacker)) return;
        if (!(event.getEntity() instanceof LivingEntity victim)) return;

        PlayerCardData data = plugin.data().get(attacker.getUniqueId());
        double damage = event.getDamage();

        // ---- Berserk ----
        if (data.hasCard(CardType.BERSERK) && data.berserkActiveUntil > System.currentTimeMillis()) {
            damage *= 1.5;
        }

        // ---- Hellflame ----
        if (data.hasCard(CardType.HELLFLAME) && attacker.getFireTicks() > 0) {
            damage *= 1.25;
        }

        // ---- Sunken Anchor (King of the Sea) ----
        if (data.hasKingOfTheSea() && (attacker.isInWater() || attacker.isSwimming())) {
            damage *= 1.3;
        }

        // ---- Vampiric Fang weapon lifesteal ----
        ItemStack hand = attacker.getInventory().getItemInMainHand();
        LegendaryType heldLegendary = plugin.legendaries().readLegendary(hand);
        if (heldLegendary == LegendaryType.VAMPIRIC_FANG) {
            heal(attacker, data, 1.0); // half a heart
        }

        // ---- Vampiric card ----
        if (data.hasCard(CardType.VAMPIRIC)) {
            long ready = vampiricCooldown.getOrDefault(attacker.getUniqueId(), 0L);
            if (System.currentTimeMillis() >= ready) {
                damage += 6.0; // 3 extra hearts
                heal(attacker, data, 6.0); // 3 hearts
                vampiricCooldown.put(attacker.getUniqueId(), System.currentTimeMillis() + 10_000L);
                attacker.sendMessage(Component.text("Vampiric strike!", NamedTextColor.DARK_RED));
            }
        }

        // ---- Combo ----
        if (data.hasCard(CardType.COMBO) && victim instanceof Player) {
            int count = data.comboCounters.merge(victim.getUniqueId(), 1, Integer::sum);
            if (count % 3 == 0) {
                damage += 4.0; // 2 extra hearts
                victim.getWorld().spawnParticle(Particle.DAMAGE_INDICATOR, victim.getLocation().add(0, 1, 0), 12, 0.3, 0.3, 0.3, 0);
            }
        }

        // ---- Bloodthirsty stacking ----
        if (data.hasCard(CardType.BLOODTHIRSTY)) {
            data.bloodthirstyStacks = Math.min(4, data.bloodthirstyStacks + 1);
            data.lastHitTime = System.currentTimeMillis();
        }

        // ---- Venomous Snake (Fang legendary) ----
        if (data.hasVenomousSnake()) {
            victim.addPotionEffect(new PotionEffect(PotionEffectType.POISON, 60, 0));
        }

        // ---- Greed's Touch: steal an XP level ----
        if (data.hasCard(CardType.GREEDS_TOUCH) && victim instanceof Player victimPlayer) {
            double chance = plugin.getConfig().getDouble("chances.greeds-touch", 0.25);
            if (random.nextDouble() < chance && victimPlayer.getLevel() > 0) {
                victimPlayer.setLevel(victimPlayer.getLevel() - 1);
                attacker.setLevel(attacker.getLevel() + 1);
                attacker.sendMessage(Component.text("Greed's Touch stole a level!", NamedTextColor.GOLD));
            }
        }

        // ---- Gluttonous: steal saturation/food ----
        if (data.hasCard(CardType.GLUTTONOUS) && victim instanceof Player victimPlayer) {
            int steal = Math.min(2, victimPlayer.getFoodLevel());
            victimPlayer.setFoodLevel(Math.max(0, victimPlayer.getFoodLevel() - steal));
            victimPlayer.setSaturation(Math.max(0, victimPlayer.getSaturation() - steal));
            attacker.setFoodLevel(Math.min(20, attacker.getFoodLevel() + steal));
            attacker.setSaturation(Math.min(20f, attacker.getSaturation() + steal));
        }

        // ---- Frozen Anchor ----
        if (data.hasCard(CardType.FROZEN_ANCHOR) && victim instanceof Player victimPlayer) {
            double chance = plugin.getConfig().getDouble("chances.frozen-anchor", 0.15);
            if (random.nextDouble() < chance) {
                victimPlayer.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 60, 6));
                victimPlayer.addPotionEffect(new PotionEffect(PotionEffectType.JUMP_BOOST, 60, 128)); // negates jump
                victimPlayer.getWorld().spawnParticle(Particle.SNOWFLAKE, victimPlayer.getLocation(), 30, 0.5, 1, 0.5, 0);
                victimPlayer.sendMessage(Component.text("You've been frozen in place!", NamedTextColor.AQUA));
            }
        }

        // ---- Grounding Bolt ----
        if (data.hasCard(CardType.GROUNDING_BOLT)) {
            double chance = plugin.getConfig().getDouble("chances.grounding-bolt", 0.10);
            if (random.nextDouble() < chance) {
                victim.getWorld().strikeLightningEffect(victim.getLocation());
                victim.damage(2.0, attacker);
            }
        }

        // ---- Metal Thief ----
        if (data.hasCard(CardType.METAL_THIEF) && victim instanceof Player victimPlayer) {
            double chance = plugin.getConfig().getDouble("chances.metal-thief", 0.25);
            if (random.nextDouble() < chance) {
                damageRandomArmorPiece(victimPlayer);
            }
        }

        // ---- Heavy Swing ----
        if (data.hasCard(CardType.HEAVY_SWING) && victim instanceof Player victimPlayer) {
            double chance = plugin.getConfig().getDouble("chances.heavy-swing", 0.05);
            if (random.nextDouble() < chance) {
                victimPlayer.addPotionEffect(new PotionEffect(PotionEffectType.NAUSEA, 40, 1));
            }
        }

        event.setDamage(damage);
    }

    /** Applies healing to a player, respecting the Hard Syphon (Vampiric Claws) legendary bonus. */
    private void heal(Player player, PlayerCardData data, double amount) {
        double finalAmount = data.hasHardSyphon() ? amount * 1.5 : amount;
        var maxAttr = player.getAttribute(Attribute.GENERIC_MAX_HEALTH);
        double max = maxAttr != null ? maxAttr.getValue() : 20.0;
        player.setHealth(Math.min(max, player.getHealth() + finalAmount));
    }

    private void damageRandomArmorPiece(Player player) {
        EntityEquipment eq = player.getEquipment();
        if (eq == null) return;
        java.util.List<ItemStack> pieces = new java.util.ArrayList<>();
        for (ItemStack piece : new ItemStack[]{eq.getHelmet(), eq.getChestplate(), eq.getLeggings(), eq.getBoots()}) {
            if (piece != null && !piece.getType().isAir() && piece.getType() != Material.AIR) pieces.add(piece);
        }
        if (pieces.isEmpty()) return;
        ItemStack chosen = pieces.get(random.nextInt(pieces.size()));
        if (chosen.getItemMeta() instanceof org.bukkit.inventory.meta.Damageable dmgMeta) {
            dmgMeta.setDamage(dmgMeta.getDamage() + 25);
            chosen.setItemMeta((org.bukkit.inventory.meta.ItemMeta) dmgMeta);
        }
    }
}
