package com.gambitsmp.legendary;

/**
 * Legendary items. Each grants the player a bonus card-like passive
 * (some buff other cards, some stand alone). VAMPIRIC_CLAWS and FANG
 * can be combined at an anvil into VAMPIRIC_FANG, a standalone weapon.
 *
 * customModelData works the same way as CardType's - see that class's
 * javadoc. SUNKEN_ANCHOR/ROSEN_HELLFIRE/VAMPIRIC_CLAWS/FANG use
 * Material.NETHER_STAR (so their overrides live in nether_star.json);
 * VAMPIRIC_FANG uses Material.NETHERITE_SWORD (netherite_sword.json).
 */
public enum LegendaryType {

    SUNKEN_ANCHOR(
            "Sunken Anchor", "King of the Sea", 2010,
            "Deal 30% more damage while", "you are in water."
    ),

    ROSEN_HELLFIRE(
            "Rosen Hellfire", "Immortal Flames", 2020,
            "Your fire cards last much", "longer and are harder to", "extinguish."
    ),

    VAMPIRIC_CLAWS(
            "Vampiric Claws", "Hard Syphon", 2030,
            "Any card or item that heals", "you heals for 50% more."
    ),

    FANG(
            "Fang", "Venomous Snake", 2040,
            "Your attacks poison enemies.", "Rotten flesh feeds you like a", "golden carrot."
    ),

    VAMPIRIC_FANG(
            "Vampiric Fang", "Vampiric Fang", 2050,
            "A cursed blade that syphons", "half a heart of life with", "every strike."
    );

    private final String itemName;
    private final String grantedCardName;
    private final int customModelData;
    private final String[] lore;

    LegendaryType(String itemName, String grantedCardName, int customModelData, String... lore) {
        this.itemName = itemName;
        this.grantedCardName = grantedCardName;
        this.customModelData = customModelData;
        this.lore = lore;
    }

    public String itemName() {
        return itemName;
    }

    public String grantedCardName() {
        return grantedCardName;
    }

    public int customModelData() {
        return customModelData;
    }

    /** Lowercase snake_case key used for resource pack model/texture file names, e.g. "vampiric_fang". */
    public String textureKey() {
        return name().toLowerCase();
    }

    public String[] lore() {
        return lore;
    }
}
