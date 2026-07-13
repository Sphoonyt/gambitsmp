package com.gambitsmp.util;

import com.gambitsmp.GambitSMP;
import com.gambitsmp.cards.CardType;
import com.gambitsmp.player.PlayerCardData;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.List;
import java.util.Random;
import java.util.UUID;

public class PeriodicTaskManager {

    private final GambitSMP plugin;
    private final Random random = new Random();
    private int tickCount = 0;
    private final java.util.Map<UUID, Long> darkHoursCooldown = new java.util.HashMap<>();

    private static final List<PotionEffectType> GOOD_EFFECTS = List.of(
            PotionEffectType.SPEED, PotionEffectType.INCREASE_DAMAGE, PotionEffectType.REGENERATION,
            PotionEffectType.DAMAGE_RESISTANCE, PotionEffectType.JUMP, PotionEffectType.FIRE_RESISTANCE,
            PotionEffectType.ABSORPTION
    );

    public PeriodicTaskManager(GambitSMP plugin) {
        this.plugin = plugin;
    }

    public void start() {
        // runs every second (20 ticks)
        new BukkitRunnable() {
            @Override
            public void run() {
                tickCount++;
                for (Player p : plugin.getServer().getOnlinePlayers()) {
                    PlayerCardData data = plugin.data().get(p.getUniqueId());

                    handleSpeedDemon(p, data);
                    handleLightningSpeed(p, data);
                    handleBloodthirstyDecay(p, data);
                    handleHealthPause(p, data);
                    handleBloodTrail(p, data);

                    if (tickCount % 30 == 0) handleGruesomeHarvest(p, data);
                    if (tickCount % 60 == 0) {
                        handleUnstable(p, data);
                        handleDarkHours(p, data);
                    }
                }
            }
        }.runTaskTimer(plugin, 20L, 20L);
    }

    // Continuous while low-health, so no per-tick sound (would be constant noise) - but
    // a quick pulse of a "heartbeat" sound plays the moment you cross into a faster tier,
    // which is the actual noteworthy event.
    private void handleSpeedDemon(Player p, PlayerCardData data) {
        if (!data.hasCard(CardType.SPEED_DEMON)) return;
        var maxAttr = p.getAttribute(Attribute.GENERIC_MAX_HEALTH);
        if (maxAttr == null) return;
        // nerfed: capped at Speed III (amplifier 2) instead of Speed IV
        double pct = p.getHealth() / maxAttr.getValue();
        int amplifier;
        if (pct <= 0.35) amplifier = 2;
        else if (pct <= 0.6) amplifier = 1;
        else amplifier = 0;
        if (amplifier > 0) {
            p.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 40, amplifier, true, false));
        }
        if (amplifier != data.lastSpeedDemonAmplifier && amplifier > data.lastSpeedDemonAmplifier) {
            p.getWorld().playSound(p.getLocation(), Sound.ENTITY_PLAYER_HURT, 0.4f, 1.8f);
            p.getWorld().spawnParticle(Particle.CRIT, p.getLocation().add(0, 1, 0), 8, 0.2, 0.3, 0.2, 0.1);
        }
        data.lastSpeedDemonAmplifier = amplifier;
    }

    // Same idea as Speed Demon: continuous effect, but a short "thunder crack" chime
    // plays only when you actually reach a new speed tier from sustained sprinting -
    // fittingly, since the card is literally called Lightning Speed.
    private void handleLightningSpeed(Player p, PlayerCardData data) {
        if (!data.hasCard(CardType.LIGHTNING_SPEED)) return;
        int amplifier = 0;
        if (p.isSprinting()) {
            if (data.sprintStartTime == 0L) data.sprintStartTime = System.currentTimeMillis();
            long sprintSeconds = (System.currentTimeMillis() - data.sprintStartTime) / 1000;
            // nerfed: capped at Speed III (amplifier 2) instead of Speed IV
            amplifier = Math.min(2, (int) (sprintSeconds / 10));
        } else {
            data.sprintStartTime = 0L;
        }
        p.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 40, amplifier, true, false));
        if (amplifier != data.lastLightningAmplifier && amplifier > data.lastLightningAmplifier) {
            p.getWorld().playSound(p.getLocation(), Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 0.3f, 1.8f);
            p.getWorld().spawnParticle(Particle.FIREWORKS_SPARK, p.getLocation().add(0, 0.5, 0), 15, 0.3, 0.2, 0.3, 0.05);
        }
        data.lastLightningAmplifier = amplifier;
    }

    // nerfed: capped at Speed II (amplifier 1) instead of scaling up to Speed IV -
    // also now cleared entirely the moment its owner takes a hit, see
    // CombatListener#onVictimDamaged
    private void handleBloodthirstyDecay(Player p, PlayerCardData data) {
        if (!data.hasCard(CardType.BLOODTHIRSTY)) return;
        if (data.bloodthirstyStacks <= 0) return;
        if (System.currentTimeMillis() - data.lastHitTime > 4000) {
            data.bloodthirstyStacks = Math.max(0, data.bloodthirstyStacks - 1);
        }
        if (data.bloodthirstyStacks > 0) {
            int amp = data.bloodthirstyStacks >= 2 ? 1 : 0; // Speed I at 1 stack, Speed II cap at 2+
            p.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 40, amp, true, false));
        }
    }

    private void handleHealthPause(Player p, PlayerCardData data) {
        if (data.healthPausedUntil <= 0) return;
        if (System.currentTimeMillis() > data.healthPausedUntil) {
            data.healthPausedUntil = 0L;
            data.pausedHealthValue = -1;
            return;
        }
        if (data.pausedHealthValue >= 0 && p.getHealth() != data.pausedHealthValue) {
            double clamped = Math.min(data.pausedHealthValue, p.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue());
            p.setHealth(Math.max(0.5, clamped));
        }
    }

    private static final Particle.DustOptions BLOOD_DUST =
            new Particle.DustOptions(org.bukkit.Color.fromRGB(180, 0, 0), 1.3f);

    // fixed: was using a fire/nether-themed particle (CRIMSON_SPORE) that doesn't read as
    // blood at all, and had no distance cap so it scanned every player on the server every
    // second. Now uses red dust and only checks players within a reasonable range.
    private void handleBloodTrail(Player viewer, PlayerCardData data) {
        if (!data.hasCard(CardType.BLOOD_TRAIL)) return;
        for (Player target : viewer.getWorld().getPlayers()) {
            if (target.equals(viewer)) continue;
            if (target.getLocation().distanceSquared(viewer.getLocation()) > 30 * 30) continue;
            var maxAttr = target.getAttribute(Attribute.GENERIC_MAX_HEALTH);
            if (maxAttr == null) continue;
            if (target.getHealth() / maxAttr.getValue() <= 0.5) {
                Location loc = target.getLocation().add(0, 2.2, 0);
                viewer.spawnParticle(Particle.REDSTONE, loc, 6, 0.25, 0.3, 0.25, 0, BLOOD_DUST);
            }
        }
    }

    // reworked: no longer a random effect from the good-effects pool - now always grants
    // Speed II and a bit of extra attack damage. A triumphant villager-chime + green
    // sparkle sells the "blessing" framing of a harvest.
    private void handleGruesomeHarvest(Player p, PlayerCardData data) {
        if (!data.hasCard(CardType.GRUESOME_HARVEST)) return;
        double chance = plugin.getConfig().getDouble("chances.gruesome-harvest-per-30s", 0.10);
        if (random.nextDouble() > chance) return;
        p.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 200, 1, true, true));
        p.addPotionEffect(new PotionEffect(PotionEffectType.INCREASE_DAMAGE, 200, 0, true, true));
        p.sendMessage(Component.text("Gruesome Harvest blessed you with Speed II and extra attack damage!", NamedTextColor.LIGHT_PURPLE));
        p.getWorld().playSound(p.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.7f, 1.3f);
        p.getWorld().spawnParticle(Particle.VILLAGER_HAPPY, p.getLocation().add(0, 1.5, 0), 12, 0.4, 0.5, 0.4, 0);
    }

    // A chaotic, unpredictable magic cast sound + witch-purple swirl fits "Unstable"
    // much better than a plain potion-drink noise would.
    private void handleUnstable(Player p, PlayerCardData data) {
        if (!data.hasCard(CardType.UNSTABLE)) return;
        PotionEffectType type = GOOD_EFFECTS.get(random.nextInt(GOOD_EFFECTS.size()));
        p.addPotionEffect(new PotionEffect(type, 200, 0));
        p.sendMessage(Component.text("Unstable shifts... you feel " + type.getName().toLowerCase() + ".", NamedTextColor.DARK_PURPLE));
        p.getWorld().playSound(p.getLocation(), Sound.ENTITY_EVOKER_CAST_SPELL, 0.6f, 1.1f);
        p.getWorld().spawnParticle(Particle.SMOKE_LARGE, p.getLocation().add(0, 1.2, 0), 20, 0.4, 0.6, 0.4, 0);
    }

    // Curses nearby enemies - a low, creepy ambience and dark soul particles around
    // each cursed target make it clear something just happened to them specifically,
    // not just a random debuff appearing. Now also has its own cooldown on top of the
    // per-minute chance roll, so a lucky string of proc rolls in a row can't chain.
    private void handleDarkHours(Player p, PlayerCardData data) {
        if (!data.hasCard(CardType.DARK_HOURS)) return;
        long ready = darkHoursCooldown.getOrDefault(p.getUniqueId(), 0L);
        if (System.currentTimeMillis() < ready) return;
        double chance = plugin.getConfig().getDouble("chances.dark-hours-per-minute", 0.10);
        if (random.nextDouble() > chance) return;

        int cooldownSeconds = plugin.getConfig().getInt("cooldowns.dark-hours-seconds", 45);
        darkHoursCooldown.put(p.getUniqueId(), System.currentTimeMillis() + cooldownSeconds * 1000L);

        for (Player target : p.getWorld().getPlayers()) {
            if (target.equals(p)) continue;
            if (target.getLocation().distance(p.getLocation()) > 20) continue;
            target.addPotionEffect(new PotionEffect(PotionEffectType.HUNGER, 200, 1));
            target.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 100, 0));
            target.sendMessage(Component.text("Dark Hours falls over you...", NamedTextColor.DARK_GRAY));
            target.getWorld().playSound(target.getLocation(), Sound.ENTITY_VEX_AMBIENT, 0.8f, 0.6f);
            target.getWorld().spawnParticle(Particle.SOUL, target.getLocation().add(0, 1, 0), 15, 0.3, 0.5, 0.3, 0.02);
        }
    }
}
