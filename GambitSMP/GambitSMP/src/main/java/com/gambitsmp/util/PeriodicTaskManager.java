package com.gambitsmp.util;

import com.gambitsmp.GambitSMP;
import com.gambitsmp.cards.CardType;
import com.gambitsmp.player.PlayerCardData;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.List;
import java.util.Random;

public class PeriodicTaskManager {

    private final GambitSMP plugin;
    private final Random random = new Random();
    private int tickCount = 0;

    private static final List<PotionEffectType> GOOD_EFFECTS = List.of(
            PotionEffectType.SPEED, PotionEffectType.STRENGTH, PotionEffectType.REGENERATION,
            PotionEffectType.RESISTANCE, PotionEffectType.JUMP, PotionEffectType.FIRE_RESISTANCE,
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

    private void handleSpeedDemon(Player p, PlayerCardData data) {
        if (!data.hasCard(CardType.SPEED_DEMON)) return;
        var maxAttr = p.getAttribute(Attribute.GENERIC_MAX_HEALTH);
        if (maxAttr == null) return;
        double pct = p.getHealth() / maxAttr.getValue();
        int amplifier;
        if (pct <= 0.15) amplifier = 3;
        else if (pct <= 0.35) amplifier = 2;
        else if (pct <= 0.6) amplifier = 1;
        else amplifier = 0;
        if (amplifier > 0) {
            p.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 40, amplifier, true, false));
        }
    }

    private void handleLightningSpeed(Player p, PlayerCardData data) {
        if (!data.hasCard(CardType.LIGHTNING_SPEED)) return;
        int amplifier = 0;
        if (p.isSprinting()) {
            if (data.sprintStartTime == 0L) data.sprintStartTime = System.currentTimeMillis();
            long sprintSeconds = (System.currentTimeMillis() - data.sprintStartTime) / 1000;
            amplifier = Math.min(3, (int) (sprintSeconds / 10));
        } else {
            data.sprintStartTime = 0L;
        }
        p.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 40, amplifier, true, false));
    }

    private void handleBloodthirstyDecay(Player p, PlayerCardData data) {
        if (!data.hasCard(CardType.BLOODTHIRSTY)) return;
        if (data.bloodthirstyStacks <= 0) return;
        if (System.currentTimeMillis() - data.lastHitTime > 4000) {
            data.bloodthirstyStacks = Math.max(0, data.bloodthirstyStacks - 1);
        }
        if (data.bloodthirstyStacks > 0) {
            int amp = Math.min(4, data.bloodthirstyStacks) - 1;
            p.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 40, Math.max(0, amp), true, false));
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

    private void handleBloodTrail(Player viewer, PlayerCardData data) {
        if (!data.hasCard(CardType.BLOOD_TRAIL)) return;
        for (Player target : viewer.getWorld().getPlayers()) {
            if (target.equals(viewer)) continue;
            var maxAttr = target.getAttribute(Attribute.GENERIC_MAX_HEALTH);
            if (maxAttr == null) continue;
            if (target.getHealth() / maxAttr.getValue() <= 0.5) {
                Location loc = target.getLocation().add(0, 1, 0);
                viewer.spawnParticle(Particle.CRIMSON_SPORE, loc, 3, 0.3, 0.5, 0.3, 0);
            }
        }
    }

    private void handleGruesomeHarvest(Player p, PlayerCardData data) {
        if (!data.hasCard(CardType.GRUESOME_HARVEST)) return;
        double chance = plugin.getConfig().getDouble("chances.gruesome-harvest-per-30s", 0.10);
        if (random.nextDouble() > chance) return;
        PotionEffectType type = GOOD_EFFECTS.get(random.nextInt(GOOD_EFFECTS.size()));
        p.addPotionEffect(new PotionEffect(type, 200, 1));
        p.sendMessage(Component.text("Gruesome Harvest blessed you with " + type.getName() + "!", NamedTextColor.LIGHT_PURPLE));
    }

    private void handleUnstable(Player p, PlayerCardData data) {
        if (!data.hasCard(CardType.UNSTABLE)) return;
        PotionEffectType type = GOOD_EFFECTS.get(random.nextInt(GOOD_EFFECTS.size()));
        p.addPotionEffect(new PotionEffect(type, 200, 0));
        p.sendMessage(Component.text("Unstable shifts... you feel " + type.getName().toLowerCase() + ".", NamedTextColor.DARK_PURPLE));
    }

    private void handleDarkHours(Player p, PlayerCardData data) {
        if (!data.hasCard(CardType.DARK_HOURS)) return;
        double chance = plugin.getConfig().getDouble("chances.dark-hours-per-minute", 0.10);
        if (random.nextDouble() > chance) return;
        for (Player target : p.getWorld().getPlayers()) {
            if (target.equals(p)) continue;
            if (target.getLocation().distance(p.getLocation()) > 20) continue;
            target.addPotionEffect(new PotionEffect(PotionEffectType.HUNGER, 200, 1));
            target.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 100, 0));
            target.sendMessage(Component.text("Dark Hours falls over you...", NamedTextColor.DARK_GRAY));
        }
    }
}
