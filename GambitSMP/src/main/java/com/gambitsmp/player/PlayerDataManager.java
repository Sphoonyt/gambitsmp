package com.gambitsmp.player;

import com.gambitsmp.GambitSMP;
import com.gambitsmp.cards.CardType;
import com.gambitsmp.legendary.LegendaryType;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Tracks each player's PlayerCardData in memory during a session, and persists
 * equipped cards / legendaries / banned cards to disk (playerdata.yml) so they
 * survive logging out, and server restarts - not just an in-memory session.
 *
 * Combat/movement state on PlayerCardData (cooldowns, hit counters, etc.) is
 * deliberately NOT persisted - that's live session state, not meant to survive
 * a logout.
 */
public class PlayerDataManager {

    private final GambitSMP plugin;
    private final Map<UUID, PlayerCardData> data = new ConcurrentHashMap<>();
    private final File file;

    public PlayerDataManager(GambitSMP plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "playerdata.yml");
    }

    public PlayerCardData get(UUID uuid) {
        return data.computeIfAbsent(uuid, PlayerCardData::new);
    }

    public void remove(UUID uuid) {
        data.remove(uuid);
    }

    public int maxEquippedCards() {
        return plugin.getConfig().getInt("max-equipped-cards", 3);
    }

    /** Loads every player's saved cards/legendaries from disk. Call once during onEnable. */
    public void loadAll() {
        if (!file.exists()) return;
        FileConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection playersSection = yaml.getConfigurationSection("players");
        if (playersSection == null) return;

        int count = 0;
        for (String uuidStr : playersSection.getKeys(false)) {
            UUID uuid;
            try {
                uuid = UUID.fromString(uuidStr);
            } catch (IllegalArgumentException ex) {
                continue;
            }
            PlayerCardData pdata = get(uuid);

            for (String cardName : playersSection.getStringList(uuidStr + ".cards")) {
                try {
                    pdata.equippedCards().add(CardType.valueOf(cardName));
                } catch (IllegalArgumentException ignored) {}
            }
            for (String legName : playersSection.getStringList(uuidStr + ".legendaries")) {
                try {
                    pdata.equippedLegendaries().add(LegendaryType.valueOf(legName));
                } catch (IllegalArgumentException ignored) {}
            }
            for (String bannedName : playersSection.getStringList(uuidStr + ".banned")) {
                try {
                    pdata.bannedCards().add(CardType.valueOf(bannedName));
                } catch (IllegalArgumentException ignored) {}
            }
            count++;
        }
        plugin.getLogger().info("Loaded saved card data for " + count + " player(s).");
    }

    /**
     * Writes every currently-tracked player's cards/legendaries/banned cards to
     * disk. Rewrites the whole file each time rather than patching incrementally -
     * simple and safe at the scale this plugin is meant for.
     */
    public void saveAll() {
        FileConfiguration yaml = new YamlConfiguration();
        for (Map.Entry<UUID, PlayerCardData> entry : data.entrySet()) {
            String path = "players." + entry.getKey();
            PlayerCardData pdata = entry.getValue();

            List<String> cards = new ArrayList<>();
            for (CardType type : pdata.equippedCards()) cards.add(type.name());
            yaml.set(path + ".cards", cards);

            List<String> legendaries = new ArrayList<>();
            for (LegendaryType type : pdata.equippedLegendaries()) legendaries.add(type.name());
            yaml.set(path + ".legendaries", legendaries);

            List<String> banned = new ArrayList<>();
            for (CardType type : pdata.bannedCards()) banned.add(type.name());
            yaml.set(path + ".banned", banned);
        }

        try {
            if (!plugin.getDataFolder().exists()) plugin.getDataFolder().mkdirs();
            yaml.save(file);
        } catch (IOException e) {
            plugin.getLogger().warning("Failed to save playerdata.yml: " + e.getMessage());
        }
    }
}
