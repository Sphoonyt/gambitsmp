package com.gambitsmp.cards;

/**
 * Every card in GambitSMP.
 *
 * Cards fall into two behavioural buckets:
 *  - PASSIVE cards apply their effect automatically whenever the relevant
 *    game event happens (getting hit, taking fall damage, sneaking, etc).
 *  - ACTIVE cards have a cooldown and are triggered on purpose: sneak +
 *    right-click with an empty hand while the card is equipped (or /ability).
 *    If a player has more than one active card equipped, the first one that
 *    is off cooldown fires.
 *
 * cooldownSeconds is only meaningful for ACTIVE cards. For PASSIVE cards
 * it is reused as an "internal" cooldown/tick rate to keep chance/on-hit
 * effects from being spammy - see the individual listeners.
 *
 * customModelData is the integer a resource pack's item model override
 * matches against to swap in a custom texture (all cards use Material.PAPER,
 * so the pack overrides assets/minecraft/models/item/paper.json). Numbers
 * are spaced by 10 and assigned explicitly (not derived from ordinal()) so
 * that inserting a new card later never renumbers - and therefore never
 * silently re-textures - an existing one. See the resource-pack-template/
 * folder shipped alongside the plugin for the placeholder textures and the
 * exact override wiring.
 */
public enum CardType {

    BERSERK("Berserk", CardRarity.RARE, true, 30, 1010,
            "Right-click sneaking to enter a", "berserker state: +50% damage", "dealt for 5 seconds."),

    NEGATION("Negation", CardRarity.EPIC, true, 45, 1020,
            "Right-click sneaking to pause", "your health for 5 seconds.", "You cannot be healed or hurt."),

    NIGHTBRINGER("Nightbringer", CardRarity.EPIC, true, 180, 1030,
            "Right-click sneaking to curse", "nearby enemies with Slowness I", "and Blindness."),

    VAMPIRIC("Vampiric", CardRarity.RARE, false, 10, 1040,
            "Small chance on hit to deal 1", "extra heart of damage and heal", "you for 1 heart. Can never kill -", "leaves victims at half a heart."),

    WARDEN_BEAM("Warden Beam", CardRarity.LEGENDARY, true, 180, 1050,
            "Right-click sneaking to fire a", "sonic beam that damages and", "knocks back everything in front of you."),

    REINFORCED_ARMOR("Reinforced Armor", CardRarity.COMMON, false, 0, 1060,
            "Your armor takes 50% less", "durability damage."),

    BLOODTHIRSTY("Bloodthirsty", CardRarity.UNCOMMON, false, 0, 1070,
            "Consecutive hits make you", "faster, up to Speed II. Getting", "hit clears your stacks instantly."),

    SPEED_DEMON("Speed Demon", CardRarity.UNCOMMON, false, 0, 1080,
            "The lower your health, the", "faster you move, up to Speed III."),

    GREEDS_TOUCH("Greed's Touch", CardRarity.RARE, false, 0, 1090,
            "Chance to steal an XP level", "from players you hit and gain", "a longer Absorption buff.", "Has a short cooldown between procs."),

    GREENS_TOUCH("Green's Touch", CardRarity.COMMON, false, 0, 1100,
            "You gain 50% more experience", "from all sources."),

    PIGGYS_WEIGHT("Piggy's Weight", CardRarity.UNCOMMON, false, 0, 1110,
            "Falling 10+ blocks negates your", "fall damage and smashes the", "ground, dealing 3 hearts to", "nearby enemies. Has a cooldown."),

    GLUTTONOUS("Gluttonous", CardRarity.COMMON, false, 0, 1120,
            "Hitting a player steals some", "of their saturation and hunger.", "Has a cooldown."),

    HELLFLAME("Hellflame", CardRarity.UNCOMMON, false, 0, 1130,
            "While on fire, hits deal 10%", "more damage and grant you Fire", "Resistance for 30s. Has a cooldown."),

    GRUESOME_HARVEST("Gruesome Harvest", CardRarity.COMMON, false, 30, 1140,
            "Random chance every 30s to", "grant yourself Speed II and", "increased attack damage for 10s."),

    FROZEN_ANCHOR("Frozen Anchor", CardRarity.RARE, false, 0, 1150,
            "Small chance on hit to root", "your opponent in place briefly.", "Has a cooldown."),

    GROUNDING_BOLT("Grounding Bolt", CardRarity.RARE, false, 0, 1160,
            "Random chance on hit to strike", "your opponent with lightning for", "3 true-damage hearts and Slowness I.", "Has a cooldown."),

    METAL_THIEF("Metal Thief", CardRarity.UNCOMMON, false, 0, 1170,
            "25% chance on hit to damage", "a piece of your opponent's armor."),

    LIGHTNING_SPEED("Lightning Speed", CardRarity.EPIC, false, 0, 1180,
            "Permanent Speed I. The longer", "you sprint continuously, the", "faster you get, up to Speed III."),

    DARK_HOURS("Dark Hours", CardRarity.RARE, false, 60, 1190,
            "Random chance every minute to", "curse nearby enemies with", "Hunger and Blindness. Has a cooldown."),

    COMBO("Combo", CardRarity.UNCOMMON, false, 0, 1200,
            "Every 4th hit on the same", "target deals half a heart extra", "with a blood effect."),

    BLOOD_TRAIL("Blood Trail", CardRarity.COMMON, false, 0, 1210,
            "You can see a particle trail", "on players below half health."),

    SPIDER("Spider", CardRarity.COMMON, false, 0, 1220,
            "Move 50% faster while standing", "in cobwebs."),

    HEAVY_SWING("Heavy Swing", CardRarity.UNCOMMON, false, 0, 1230,
            "Small chance on hit to briefly", "disorient your opponent's view."),

    PHANTOM("Phantom", CardRarity.RARE, false, 0, 1240,
            "Landing a few hits grants you", "a burst of true invisibility."),

    UNSTABLE("Unstable", CardRarity.EPIC, false, 60, 1250,
            "Every minute, randomly gain a", "beneficial potion effect for", "a short time."),

    GRAVITY("Gravity", CardRarity.EPIC, true, 120, 1260,
            "Right-click sneaking to pull", "nearby players 3 blocks toward", "you."),

    FEATHER("Feather", CardRarity.COMMON, false, 0, 1270,
            "You take no fall damage.");

    private final String displayName;
    private final CardRarity rarity;
    private final boolean active;
    private final int cooldownSeconds;
    private final int customModelData;
    private final String[] lore;

    CardType(String displayName, CardRarity rarity, boolean active, int cooldownSeconds, int customModelData, String... lore) {
        this.displayName = displayName;
        this.rarity = rarity;
        this.active = active;
        this.cooldownSeconds = cooldownSeconds;
        this.customModelData = customModelData;
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

    public int customModelData() {
        return customModelData;
    }

    /** Lowercase snake_case key used for resource pack model/texture file names, e.g. "berserk". */
    public String textureKey() {
        return name().toLowerCase();
    }

    public String[] lore() {
        return lore;
    }
}
