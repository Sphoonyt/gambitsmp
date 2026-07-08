package com.gambitsmp.cards;

/**
 * Every card in GambitSMP.
 *
 * Cards fall into two behavioural buckets:
 *  - PASSIVE cards apply their effect automatically whenever the relevant
 *    game event happens (getting hit, taking fall damage, sneaking, etc).
 *  - ACTIVE cards have a cooldown and are triggered on purpose: sneak +
 *    right-click with an empty hand while the card is equipped. If a
 *    player has more than one active card equipped, the first one that
 *    is off cooldown fires.
 *
 * cooldownSeconds is only meaningful for ACTIVE cards. For PASSIVE cards
 * it is reused as an "internal" cooldown/tick rate to keep chance/on-hit
 * effects from being spammy - see the individual listeners.
 */
public enum CardType {

    BERSERK("Berserk", CardRarity.RARE, true, 30,
            "Right-click sneaking to enter a", "berserker state: +50% damage", "dealt for 5 seconds."),

    NEGATION("Negation", CardRarity.EPIC, true, 45,
            "Right-click sneaking to pause", "your health for 5 seconds.", "You cannot be healed or hurt."),

    NIGHTBRINGER("Nightbringer", CardRarity.EPIC, true, 180,
            "Right-click sneaking to curse", "nearby enemies with Slowness I", "and Blindness."),

    VAMPIRIC("Vampiric", CardRarity.RARE, false, 10,
            "Your attacks deal 3 extra hearts", "of damage and heal you for", "3 hearts."),

    WARDEN_BEAM("Warden Beam", CardRarity.LEGENDARY, true, 180,
            "Right-click sneaking to fire a", "sonic beam that damages and", "knocks back everything in front of you."),

    REINFORCED_ARMOR("Reinforced Armor", CardRarity.COMMON, false, 0,
            "Your armor takes 50% less", "durability damage."),

    BLOODTHIRSTY("Bloodthirsty", CardRarity.UNCOMMON, false, 0,
            "Consecutive hits make you", "faster. Stacks decay if you", "stop attacking."),

    SPEED_DEMON("Speed Demon", CardRarity.UNCOMMON, false, 0,
            "The lower your health, the", "faster you move."),

    GREEDS_TOUCH("Greed's Touch", CardRarity.RARE, false, 0,
            "Chance to steal an XP level", "from players you hit."),

    GREENS_TOUCH("Green's Touch", CardRarity.COMMON, false, 0,
            "You gain 50% more experience", "from all sources."),

    PIGGYS_WEIGHT("Piggy's Weight", CardRarity.UNCOMMON, false, 0,
            "Falling 10+ blocks negates your", "fall damage and smashes the", "ground, dealing 3 hearts to", "nearby enemies."),

    GLUTTONOUS("Gluttonous", CardRarity.COMMON, false, 0,
            "Hitting a player steals some", "of their saturation and hunger."),

    HELLFLAME("Hellflame", CardRarity.UNCOMMON, false, 0,
            "You deal 25% more damage", "while on fire."),

    GRUESOME_HARVEST("Gruesome Harvest", CardRarity.COMMON, false, 30,
            "Random chance every 30s to", "grant yourself a beneficial", "potion effect for 10 seconds."),

    FROZEN_ANCHOR("Frozen Anchor", CardRarity.RARE, false, 0,
            "Random chance on hit to freeze", "your opponent in place briefly."),

    GROUNDING_BOLT("Grounding Bolt", CardRarity.RARE, false, 0,
            "Random chance on hit to strike", "your opponent with lightning."),

    METAL_THIEF("Metal Thief", CardRarity.UNCOMMON, false, 0,
            "25% chance on hit to damage", "a piece of your opponent's armor."),

    LIGHTNING_SPEED("Lightning Speed", CardRarity.EPIC, false, 0,
            "Permanent Speed I. The longer", "you sprint continuously, the", "faster you get."),

    DARK_HOURS("Dark Hours", CardRarity.RARE, false, 60,
            "Random chance every minute to", "curse nearby enemies with", "Hunger and Blindness."),

    COMBO("Combo", CardRarity.UNCOMMON, false, 0,
            "Every 3rd hit on the same", "target deals 2 extra hearts", "with a blood effect."),

    BLOOD_TRAIL("Blood Trail", CardRarity.COMMON, false, 0,
            "You can see a particle trail", "on players below half health."),

    SPIDER("Spider", CardRarity.COMMON, false, 0,
            "Move 50% faster while standing", "in cobwebs."),

    HEAVY_SWING("Heavy Swing", CardRarity.UNCOMMON, false, 0,
            "5% chance on hit to disorient", "your opponent's view."),

    PHANTOM("Phantom", CardRarity.RARE, false, 0,
            "Crouching turns you invisible."),

    UNSTABLE("Unstable", CardRarity.EPIC, false, 60,
            "Every minute, randomly gain a", "beneficial potion effect for", "a short time."),

    GRAVITY("Gravity", CardRarity.EPIC, true, 120,
            "Right-click sneaking to pull", "nearby players 3 blocks toward", "you."),

    FEATHER("Feather", CardRarity.COMMON, false, 0,
            "You take no fall damage.");

    private final String displayName;
    private final CardRarity rarity;
    private final boolean active;
    private final int cooldownSeconds;
    private final String[] lore;

    CardType(String displayName, CardRarity rarity, boolean active, int cooldownSeconds, String... lore) {
        this.displayName = displayName;
        this.rarity = rarity;
        this.active = active;
        this.cooldownSeconds = cooldownSeconds;
        this.lore = lore;
    }

    public String displayName() {
        return displayName;
    }

    public CardRarity rarity() {
        return rarity;
    }

    public boolean isActive() {
        return active;
    }

    public int cooldownSeconds() {
        return cooldownSeconds;
    }

    public String[] lore() {
        return lore;
    }
}
