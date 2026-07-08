package com.gambitsmp.player;

import com.gambitsmp.cards.CardType;
import com.gambitsmp.legendary.LegendaryType;

import java.util.*;

public class PlayerCardData {

    private final UUID uuid;
    private final List<CardType> equippedCards = new ArrayList<>();
    private final Set<CardType> bannedCards = new HashSet<>(); // sacrificed at Shrine of Glory, can never re-equip
    private final Set<LegendaryType> equippedLegendaries = new HashSet<>();
    private final Map<CardType, Long> cooldowns = new HashMap<>();

    // combat/movement state
    public int bloodthirstyStacks = 0;
    public long lastHitTime = 0L;
    public final Map<UUID, Integer> comboCounters = new HashMap<>();
    public long healthPausedUntil = 0L;
    public double pausedHealthValue = -1;
    public long berserkActiveUntil = 0L;
    public long sprintStartTime = 0L;
    public boolean phantomActive = false;

    public PlayerCardData(UUID uuid) {
        this.uuid = uuid;
    }

    public UUID uuid() {
        return uuid;
    }

    public List<CardType> equippedCards() {
        return equippedCards;
    }

    public boolean hasCard(CardType type) {
        return equippedCards.contains(type);
    }

    public boolean isBanned(CardType type) {
        return bannedCards.contains(type);
    }

    public void banCard(CardType type) {
        bannedCards.add(type);
        equippedCards.remove(type);
    }

    public Set<CardType> bannedCards() {
        return bannedCards;
    }

    public Set<LegendaryType> equippedLegendaries() {
        return equippedLegendaries;
    }

    public boolean hasLegendary(LegendaryType type) {
        return equippedLegendaries.contains(type);
    }

    /** True if the player has any card whose fire-related effect should be extended. */
    public boolean hasImmortalFlames() {
        return equippedLegendaries.contains(LegendaryType.ROSEN_HELLFIRE);
    }

    public boolean hasHardSyphon() {
        return equippedLegendaries.contains(LegendaryType.VAMPIRIC_CLAWS);
    }

    public boolean hasKingOfTheSea() {
        return equippedLegendaries.contains(LegendaryType.SUNKEN_ANCHOR);
    }

    public boolean hasVenomousSnake() {
        return equippedLegendaries.contains(LegendaryType.FANG);
    }

    public boolean isOnCooldown(CardType type) {
        return cooldowns.getOrDefault(type, 0L) > System.currentTimeMillis();
    }

    public long cooldownRemainingSeconds(CardType type) {
        long rem = cooldowns.getOrDefault(type, 0L) - System.currentTimeMillis();
        return Math.max(0, rem / 1000);
    }

    public void setCooldown(CardType type, int seconds) {
        cooldowns.put(type, System.currentTimeMillis() + (seconds * 1000L));
    }
}
