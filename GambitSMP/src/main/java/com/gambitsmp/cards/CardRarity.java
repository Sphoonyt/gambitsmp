package com.gambitsmp.cards;

import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.NamedTextColor;

public enum CardRarity {
    COMMON(NamedTextColor.GRAY),
    UNCOMMON(NamedTextColor.GREEN),
    RARE(NamedTextColor.AQUA),
    EPIC(NamedTextColor.LIGHT_PURPLE),
    LEGENDARY(NamedTextColor.GOLD);

    private final TextColor color;

    CardRarity(TextColor color) {
        this.color = color;
    }

    public TextColor color() {
        return color;
    }
}
