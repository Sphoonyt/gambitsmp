package com.gambitsmp.legendary;

/**
 * Legendary items. Each grants the player a bonus card-like passive
 * (some buff other cards, some stand alone). VAMPIRIC_CLAWS and FANG
 * can be combined at an anvil into VAMPIRIC_FANG, a standalone weapon.
 */
public enum LegendaryType {

    SUNKEN_ANCHOR(
            "Sunken Anchor",
            "King of the Sea",
            "Deal 30% more damage while", "you are in water."
    ),

    ROSEN_HELLFIRE(
            "Rosen Hellfire",
            "Immortal Flames",
            "Your fire cards last much", "longer and are harder to", "extinguish."
    ),

    VAMPIRIC_CLAWS(
            "Vampiric Claws",
            "Hard Syphon",
            "Any card or item that heals", "you heals for 50% more."
    ),

    FANG(
            "Fang",
            "Venomous Snake",
            "Your attacks poison enemies.", "Rotten flesh feeds you like a", "golden carrot."
    ),

    VAMPIRIC_FANG(
            "Vampiric Fang",
            "Vampiric Fang",
            "A cursed blade that syphons", "half a heart of life with", "every strike."
    );

    private final String itemName;
    private final String grantedCardName;
    private final String[] lore;

    LegendaryType(String itemName, String grantedCardName, String... lore) {
        this.itemName = itemName;
        this.grantedCardName = grantedCardName;
        this.lore = lore;
    }

    public String itemName() {
        return itemName;
    }

    public String grantedCardName() {
        return grantedCardName;
    }

    public String[] lore() {
        return lore;
    }
}
