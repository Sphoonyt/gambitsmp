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
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityCombustEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
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
    private final java.util.Map<UUID, Long> greedsTouchCooldown = new java.util.HashMap<>();
    private final java.util.Map<UUID, Long> hellflameCooldown = new java.util.HashMap<>();
    private final java.util.Map<UUID, Long> frozenAnchorCooldown = new java.util.HashMap<>();
    private final java.util.Map<UUID, Long> groundingBoltCooldown = new java.util.HashMap<>();
    private final java.util.Map<UUID, Long> gluttonousCooldown = new java.util.HashMap<>();

    public CombatListener(GambitSMP plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onDamage(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player attacker)) return;
        if (!(event.getEntity() instanceof LivingEntity victim)) return;

        PlayerCardData data = plugin.data().get(attacker.getUniqueId());
        double damage = event.getDamage();
        boolean vampiricProcced = false;

        // ---- Berserk ----
        if (data.hasCard(CardType.BERSERK) && data.berserkActiveUntil > System.currentTimeMillis()) {
            damage *= 1.5;
        }

        // ---- Hellflame (reworked: was a constant always-on +10% while on fire with
        //      no cooldown; now a discrete proc on a cooldown that also grants Fire
        //      Resistance, so it reads as an actual ability moment rather than a
        //      silent passive multiplier). ----
        if (data.hasCard(CardType.HELLFLAME) && attacker.getFireTicks() > 0) {
            long ready = hellflameCooldown.getOrDefault(attacker.getUniqueId(), 0L);
            if (System.currentTimeMillis() >= ready) {
                damage *= 1.10;
                int cooldownSeconds = plugin.getConfig().getInt("cooldowns.hellflame-seconds", 15);
                hellflameCooldown.put(attacker.getUniqueId(), System.currentTimeMillis() + cooldownSeconds * 1000L);
                attacker.addPotionEffect(new PotionEffect(PotionEffectType.FIRE_RESISTANCE, 600, 0)); // 30s
                attacker.sendMessage(Component.text("Hellflame surges - Fire Resistance for 30s!", NamedTextColor.GOLD));
                victim.getWorld().spawnParticle(Particle.FLAME, victim.getLocation().add(0, 1, 0), 12, 0.3, 0.4, 0.3, 0.02);
            }
        }

        // ---- Sunken Anchor (King of the Sea): a burst of bubbles on the victim marks
        //      the water-damage bonus landing. ----
        if (data.hasKingOfTheSea() && (attacker.isInWater() || attacker.isSwimming())) {
            damage *= 1.3;
            victim.getWorld().spawnParticle(Particle.BUBBLE_POP, victim.getLocation().add(0, 1, 0), 12, 0.3, 0.3, 0.3, 0.05);
        }

        // ---- Vampiric Fang weapon lifesteal: procs on literally every hit while
        //      holding it, so feedback stays to a quiet, cheap particle only - a sound
        //      here would get old within about five seconds of PvP. ----
        ItemStack hand = attacker.getInventory().getItemInMainHand();
        LegendaryType heldLegendary = plugin.legendaries().readLegendary(hand);
        if (heldLegendary == LegendaryType.VAMPIRIC_FANG) {
            heal(attacker, data, 1.0); // half a heart
            victim.getWorld().spawnParticle(Particle.DAMAGE_INDICATOR, victim.getLocation().add(0, 1, 0), 2, 0.1, 0.1, 0.1, 0);
        }

        // ---- Vampiric card (nerfed again: damage/heal down to 1 heart, chance down
        //      to 15%, and now has its own explicit cooldown on top of the chance.
        //      Also can never be lethal - see the cap applied right before
        //      event.setDamage() at the bottom of this method. ----
        if (data.hasCard(CardType.VAMPIRIC)) {
            long ready = vampiricCooldown.getOrDefault(attacker.getUniqueId(), 0L);
            double chance = plugin.getConfig().getDouble("chances.vampiric", 0.15);
            if (System.currentTimeMillis() >= ready && random.nextDouble() < chance) {
                damage += 2.0; // 1 extra heart (was 3)
                heal(attacker, data, 2.0); // 1 heart (was 3)
                vampiricProcced = true;
                int cooldownSeconds = plugin.getConfig().getInt("cooldowns.vampiric-seconds", 8);
                vampiricCooldown.put(attacker.getUniqueId(), System.currentTimeMillis() + cooldownSeconds * 1000L);
                attacker.sendMessage(Component.text("Vampiric strike!", NamedTextColor.DARK_RED));
                attacker.getWorld().spawnParticle(Particle.HEART, attacker.getLocation().add(0, 2.1, 0), 5, 0.3, 0.2, 0.3, 0);
                attacker.getWorld().playSound(attacker.getLocation(), Sound.ENTITY_GENERIC_DRINK, 0.8f, 0.8f);
            }
        }

        // ---- Combo (nerfed again: was +1 heart on every 3rd hit, now +half a heart
        //      on every 4th hit). ----
        if (data.hasCard(CardType.COMBO) && victim instanceof Player) {
            int count = data.comboCounters.merge(victim.getUniqueId(), 1, Integer::sum);
            if (count % 4 == 0) {
                damage += 1.0; // half a heart (was 1 full heart, every 3rd hit)
                victim.getWorld().spawnParticle(Particle.DAMAGE_INDICATOR, victim.getLocation().add(0, 1, 0), 12, 0.3, 0.3, 0.3, 0);
                victim.getWorld().playSound(victim.getLocation(), Sound.ENTITY_PLAYER_ATTACK_CRIT, 1f, 1f);
            }
        }

        // ---- Bloodthirsty stacking: capped lower now (see PeriodicTaskManager for
        //      the Speed II cap) and cleared entirely the moment its owner takes a
        //      hit (see onVictimDamaged below) - a faint sweep-attack flicker marks
        //      each stack gained. ----
        if (data.hasCard(CardType.BLOODTHIRSTY)) {
            data.bloodthirstyStacks = Math.min(4, data.bloodthirstyStacks + 1);
            data.lastHitTime = System.currentTimeMillis();
            attacker.getWorld().spawnParticle(Particle.SWEEP_ATTACK, attacker.getLocation().add(0, 1, 0), 1, 0, 0, 0, 0);
        }

        // ---- Venomous Snake (Fang legendary): the vanilla Poison effect already gives
        //      its own green particles on the victim, so no extra feedback needed here. ----
        if (data.hasVenomousSnake()) {
            victim.addPotionEffect(new PotionEffect(PotionEffectType.POISON, 60, 0));
        }

        // ---- Greed's Touch: steal an XP level, and now also grants the attacker a
        //      generous, longer-lasting Absorption buff on proc. ----
        if (data.hasCard(CardType.GREEDS_TOUCH) && victim instanceof Player victimPlayer) {
            long ready = greedsTouchCooldown.getOrDefault(attacker.getUniqueId(), 0L);
            double chance = plugin.getConfig().getDouble("chances.greeds-touch", 0.25);
            if (System.currentTimeMillis() >= ready && random.nextDouble() < chance && victimPlayer.getLevel() > 0) {
                victimPlayer.setLevel(victimPlayer.getLevel() - 1);
                attacker.setLevel(attacker.getLevel() + 1);
                greedsTouchCooldown.put(attacker.getUniqueId(), System.currentTimeMillis() + 5_000L);
                attacker.addPotionEffect(new PotionEffect(PotionEffectType.ABSORPTION, 400, 1, true, true)); // 20s, longer than before
                attacker.sendMessage(Component.text("Greed's Touch stole a level!", NamedTextColor.GOLD));
                victimPlayer.getWorld().spawnParticle(Particle.ENCHANTMENT_TABLE, victimPlayer.getLocation().add(0, 1.2, 0), 20, 0.3, 0.4, 0.3, 1);
                attacker.getWorld().playSound(attacker.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1f);
            }
        }

        // ---- Gluttonous: steal saturation/food. Now on a cooldown - was previously
        //      able to trigger on literally every hit landed. ----
        if (data.hasCard(CardType.GLUTTONOUS) && victim instanceof Player victimPlayer) {
            long ready = gluttonousCooldown.getOrDefault(attacker.getUniqueId(), 0L);
            if (System.currentTimeMillis() >= ready) {
                int steal = Math.min(2, victimPlayer.getFoodLevel());
                victimPlayer.setFoodLevel(Math.max(0, victimPlayer.getFoodLevel() - steal));
                victimPlayer.setSaturation(Math.max(0, victimPlayer.getSaturation() - steal));
                attacker.setFoodLevel(Math.min(20, attacker.getFoodLevel() + steal));
                attacker.setSaturation(Math.min(20f, attacker.getSaturation() + steal));
                if (steal > 0) {
                    int cooldownSeconds = plugin.getConfig().getInt("cooldowns.gluttonous-seconds", 6);
                    gluttonousCooldown.put(attacker.getUniqueId(), System.currentTimeMillis() + cooldownSeconds * 1000L);
                    attacker.getWorld().playSound(attacker.getLocation(), Sound.ENTITY_GENERIC_EAT, 0.6f, 1.2f);
                    attacker.getWorld().spawnParticle(Particle.VILLAGER_HAPPY, attacker.getLocation().add(0, 1.5, 0), 3, 0.3, 0.2, 0.3, 0);
                }
            }
        }

        // ---- Frozen Anchor: flat 10% chance now, plus its own cooldown on top (was
        //      chance-only, no cooldown). ----
        if (data.hasCard(CardType.FROZEN_ANCHOR) && victim instanceof Player victimPlayer) {
            long ready = frozenAnchorCooldown.getOrDefault(attacker.getUniqueId(), 0L);
            double chance = plugin.getConfig().getDouble("chances.frozen-anchor", 0.10);
            if (System.currentTimeMillis() >= ready && random.nextDouble() < chance) {
                PlayerCardData victimData = plugin.data().get(victimPlayer.getUniqueId());
                victimData.frozenUntil = System.currentTimeMillis() + 1500;
                int cooldownSeconds = plugin.getConfig().getInt("cooldowns.frozen-anchor-seconds", 12);
                frozenAnchorCooldown.put(attacker.getUniqueId(), System.currentTimeMillis() + cooldownSeconds * 1000L);
                victimPlayer.getWorld().spawnParticle(Particle.SNOWFLAKE, victimPlayer.getLocation(), 30, 0.5, 1, 0.5, 0);
                victimPlayer.getWorld().playSound(victimPlayer.getLocation(), Sound.BLOCK_GLASS_PLACE, 1f, 0.7f);
                victimPlayer.sendMessage(Component.text("You've been frozen in place!", NamedTextColor.AQUA));
            }
        }

        // ---- Grounding Bolt: now deals 3 hearts of TRUE damage (bypasses armor -
        //      applied directly via setHealth rather than the normal damage pipeline)
        //      plus Slowness I for 5s, and has its own cooldown. Was 2 damage through
        //      the normal (armor-reducible) pipeline with no cooldown. ----
        if (data.hasCard(CardType.GROUNDING_BOLT)) {
            long ready = groundingBoltCooldown.getOrDefault(attacker.getUniqueId(), 0L);
            double chance = plugin.getConfig().getDouble("chances.grounding-bolt", 0.10);
            if (System.currentTimeMillis() >= ready && random.nextDouble() < chance) {
                victim.getWorld().strikeLightningEffect(victim.getLocation());
                victim.setHealth(Math.max(0.0, victim.getHealth() - 6.0)); // 3 true-damage hearts, bypasses armor
                if (victim instanceof LivingEntity le) {
                    le.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, 100, 0)); // 5s
                }
                int cooldownSeconds = plugin.getConfig().getInt("cooldowns.grounding-bolt-seconds", 8);
                groundingBoltCooldown.put(attacker.getUniqueId(), System.currentTimeMillis() + cooldownSeconds * 1000L);
            }
        }

        // ---- Metal Thief: a brittle "item breaking" sound plus a couple of sparks at
        //      the victim's chest height. ----
        if (data.hasCard(CardType.METAL_THIEF) && victim instanceof Player victimPlayer) {
            double chance = plugin.getConfig().getDouble("chances.metal-thief", 0.25);
            if (random.nextDouble() < chance) {
                damageRandomArmorPiece(victimPlayer);
                victimPlayer.getWorld().playSound(victimPlayer.getLocation(), Sound.ENTITY_ITEM_BREAK, 0.7f, 1f);
                victimPlayer.getWorld().spawnParticle(Particle.CRIT, victimPlayer.getLocation().add(0, 1.2, 0), 10, 0.3, 0.3, 0.3, 0.1);
            }
        }

        // ---- Heavy Swing: a heavy anvil-land thud plus a wide sweep shockwave sells
        //      the "your whole screen just shook" moment. ----
        if (data.hasCard(CardType.HEAVY_SWING) && victim instanceof Player victimPlayer) {
            double chance = plugin.getConfig().getDouble("chances.heavy-swing", 0.03);
            if (random.nextDouble() < chance) {
                victimPlayer.addPotionEffect(new PotionEffect(PotionEffectType.CONFUSION, 20, 0));
                victimPlayer.getWorld().playSound(victimPlayer.getLocation(), Sound.BLOCK_ANVIL_LAND, 0.6f, 0.6f);
                victimPlayer.getWorld().spawnParticle(Particle.SWEEP_ATTACK, victimPlayer.getLocation().add(0, 1, 0), 3, 0.3, 0.1, 0.3, 0);
            }
        }

        // ---- Phantom (reworked): landing enough hits grants a burst of true
        //      invisibility - an Enderman-teleport warp sound plus a smoke poof makes
        //      the "vanish" moment actually read as a vanish. ----
        if (data.hasCard(CardType.PHANTOM)) {
            data.phantomHitCounter++;
            int hitsNeeded = plugin.getConfig().getInt("phantom.hits-needed", 3);
            if (data.phantomHitCounter >= hitsNeeded) {
                data.phantomHitCounter = 0;
                int durationTicks = plugin.getConfig().getInt("phantom.duration-ticks", 100); // 5s
                attacker.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, durationTicks, 0, true, false));
                attacker.sendMessage(Component.text("Phantom: you slip out of sight!", NamedTextColor.DARK_GRAY));
                attacker.getWorld().playSound(attacker.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 0.8f, 1.4f);
                attacker.getWorld().spawnParticle(Particle.EXPLOSION_NORMAL, attacker.getLocation().add(0, 1, 0), 25, 0.4, 0.6, 0.4, 0.02);
            }
        }

        // Vampiric can never be lethal on its own - if this hit (with Vampiric's
        // bonus included) would drop the victim below half a heart, clamp it so they
        // survive at exactly half a heart instead.
        if (vampiricProcced) {
            double healthAfter = victim.getHealth() - damage;
            if (healthAfter < 0.5) {
                damage = Math.max(0.0, victim.getHealth() - 0.5);
            }
        }

        event.setDamage(damage);
    }

    /**
     * Bloodthirsty nerf: taking a hit immediately clears all stacks, removing the
     * speed bonus instantly rather than letting it decay over a few seconds.
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onVictimDamaged(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player victim)) return;
        if (event.getFinalDamage() <= 0) return;
        PlayerCardData data = plugin.data().get(victim.getUniqueId());
        if (data.hasCard(CardType.BLOODTHIRSTY) && data.bloodthirstyStacks > 0) {
            data.bloodthirstyStacks = 0;
            victim.removePotionEffect(PotionEffectType.SPEED);
        }
    }

    /**
     * Rosen Hellfire (Immortal Flames legendary): extends how long the wearer burns
     * for whenever they catch fire. (Full "can't be extinguished by water" isn't
     * exposed by the public Paper API without NMS, so this covers the practical half
     * of the effect - longer burn time - which is what "last much longer" means in
     * practice.) A soul-fire colored puff marks the extension so it's not silent.
     */
    @EventHandler(ignoreCancelled = true)
    public void onCombust(EntityCombustEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        PlayerCardData data = plugin.data().get(player.getUniqueId());
        if (!data.hasImmortalFlames()) return;

        event.setDuration((int) (event.getDuration() * 1.75));
        player.getWorld().spawnParticle(Particle.SOUL_FIRE_FLAME, player.getLocation().add(0, 1, 0), 15, 0.3, 0.5, 0.3, 0.02);
        player.getWorld().playSound(player.getLocation(), Sound.ITEM_FIRECHARGE_USE, 0.6f, 0.8f);
    }

    /** Applies healing to a player, respecting the Hard Syphon (Vampiric Claws) legendary bonus. */
    private void heal(Player player, PlayerCardData data, double amount) {
        double finalAmount = data.hasHardSyphon() ? amount * 1.5 : amount;
        var maxAttr = player.getAttribute(Attribute.GENERIC_MAX_HEALTH);
        double max = maxAttr != null ? maxAttr.getValue() : 20.0;
        player.setHealth(Math.min(max, player.getHealth() + finalAmount));
        if (data.hasHardSyphon()) {
            player.getWorld().spawnParticle(Particle.HEART, player.getLocation().add(0, 2.3, 0), 2, 0.15, 0.1, 0.15, 0);
        }
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
