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

        // ---- Hellflame (nerfed: was +25%, now +10%). Small flame burst on the victim
        //      whenever the fire bonus actually applies, so the extra damage has a tell
        //      instead of just being an invisible number change. ----
        if (data.hasCard(CardType.HELLFLAME) && attacker.getFireTicks() > 0) {
            damage *= 1.10;
            victim.getWorld().spawnParticle(Particle.FLAME, victim.getLocation().add(0, 1, 0), 8, 0.3, 0.4, 0.3, 0.01);
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

        // ---- Vampiric card (chance-based): a rising heart burst plus a drinking sound
        //      on the attacker - proc is rare enough (20% + cooldown) to afford being
        //      a bit more theatrical than the Fang's passive trickle. ----
        if (data.hasCard(CardType.VAMPIRIC)) {
            long ready = vampiricCooldown.getOrDefault(attacker.getUniqueId(), 0L);
            double chance = plugin.getConfig().getDouble("chances.vampiric", 0.20);
            if (System.currentTimeMillis() >= ready && random.nextDouble() < chance) {
                damage += 6.0; // 3 extra hearts
                heal(attacker, data, 6.0); // 3 hearts
                // small anti-spam cooldown so multi-hit weapons/sweep can't double-proc instantly
                vampiricCooldown.put(attacker.getUniqueId(), System.currentTimeMillis() + 2_000L);
                attacker.sendMessage(Component.text("Vampiric strike!", NamedTextColor.DARK_RED));
                attacker.getWorld().spawnParticle(Particle.HEART, attacker.getLocation().add(0, 2.1, 0), 5, 0.3, 0.2, 0.3, 0);
                attacker.getWorld().playSound(attacker.getLocation(), Sound.ENTITY_GENERIC_DRINK, 0.8f, 0.8f);
            }
        }

        // ---- Combo (nerfed: was +2 hearts on every 3rd hit, now +1 heart). Existing
        //      blood particle now paired with a sharp crit sound so the "finisher" hit
        //      is felt, not just seen. ----
        if (data.hasCard(CardType.COMBO) && victim instanceof Player) {
            int count = data.comboCounters.merge(victim.getUniqueId(), 1, Integer::sum);
            if (count % 3 == 0) {
                damage += 2.0; // 1 extra heart
                victim.getWorld().spawnParticle(Particle.DAMAGE_INDICATOR, victim.getLocation().add(0, 1, 0), 12, 0.3, 0.3, 0.3, 0);
                victim.getWorld().playSound(victim.getLocation(), Sound.ENTITY_PLAYER_ATTACK_CRIT, 1f, 1f);
            }
        }

        // ---- Bloodthirsty stacking: a faint sweep-attack flicker on the attacker each
        //      time a stack is gained - deliberately no sound, since this fires on
        //      every single hit and would get noisy fast. ----
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

        // ---- Greed's Touch: steal an XP level. XP orb pickup sound + a swirl of
        //      enchant-table particles on the victim reads as "something was taken." ----
        if (data.hasCard(CardType.GREEDS_TOUCH) && victim instanceof Player victimPlayer) {
            long ready = greedsTouchCooldown.getOrDefault(attacker.getUniqueId(), 0L);
            double chance = plugin.getConfig().getDouble("chances.greeds-touch", 0.25);
            if (System.currentTimeMillis() >= ready && random.nextDouble() < chance && victimPlayer.getLevel() > 0) {
                victimPlayer.setLevel(victimPlayer.getLevel() - 1);
                attacker.setLevel(attacker.getLevel() + 1);
                greedsTouchCooldown.put(attacker.getUniqueId(), System.currentTimeMillis() + 5_000L);
                attacker.sendMessage(Component.text("Greed's Touch stole a level!", NamedTextColor.GOLD));
                victimPlayer.getWorld().spawnParticle(Particle.ENCHANTMENT_TABLE, victimPlayer.getLocation().add(0, 1.2, 0), 20, 0.3, 0.4, 0.3, 1);
                attacker.getWorld().playSound(attacker.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1f);
            }
        }

        // ---- Gluttonous: steal saturation/food. A soft eating sound on the attacker
        //      and a couple of happy-villager sparkles sell the "you fed off them" idea. ----
        if (data.hasCard(CardType.GLUTTONOUS) && victim instanceof Player victimPlayer) {
            int steal = Math.min(2, victimPlayer.getFoodLevel());
            victimPlayer.setFoodLevel(Math.max(0, victimPlayer.getFoodLevel() - steal));
            victimPlayer.setSaturation(Math.max(0, victimPlayer.getSaturation() - steal));
            attacker.setFoodLevel(Math.min(20, attacker.getFoodLevel() + steal));
            attacker.setSaturation(Math.min(20f, attacker.getSaturation() + steal));
            if (steal > 0) {
                attacker.getWorld().playSound(attacker.getLocation(), Sound.ENTITY_GENERIC_EAT, 0.6f, 1.2f);
                attacker.getWorld().spawnParticle(Particle.VILLAGER_HAPPY, attacker.getLocation().add(0, 1.5, 0), 3, 0.3, 0.2, 0.3, 0);
            }
        }

        // ---- Frozen Anchor (nerfed + fixed - see MovementListener for the real freeze
        //      mechanic that replaced the old Jump Boost hack). A crystallizing glass
        //      sound sells the "you're rooted" moment alongside the existing snowflakes. ----
        if (data.hasCard(CardType.FROZEN_ANCHOR) && victim instanceof Player victimPlayer) {
            double chance = plugin.getConfig().getDouble("chances.frozen-anchor", 0.08);
            if (random.nextDouble() < chance) {
                PlayerCardData victimData = plugin.data().get(victimPlayer.getUniqueId());
                victimData.frozenUntil = System.currentTimeMillis() + 1500; // 1.5s, was 3s
                victimPlayer.getWorld().spawnParticle(Particle.SNOWFLAKE, victimPlayer.getLocation(), 30, 0.5, 1, 0.5, 0);
                victimPlayer.getWorld().playSound(victimPlayer.getLocation(), Sound.BLOCK_GLASS_PLACE, 1f, 0.7f);
                victimPlayer.sendMessage(Component.text("You've been frozen in place!", NamedTextColor.AQUA));
            }
        }

        // ---- Grounding Bolt: strikeLightningEffect() already carries its own vanilla
        //      thunder sound and bolt visual, so no extra feedback needed. ----
        if (data.hasCard(CardType.GROUNDING_BOLT)) {
            double chance = plugin.getConfig().getDouble("chances.grounding-bolt", 0.10);
            if (random.nextDouble() < chance) {
                victim.getWorld().strikeLightningEffect(victim.getLocation());
                victim.damage(2.0, attacker);
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

        event.setDamage(damage);
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
