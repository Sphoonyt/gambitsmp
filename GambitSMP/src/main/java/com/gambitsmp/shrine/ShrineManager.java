package com.gambitsmp.shrine;

import com.gambitsmp.GambitSMP;
import org.bukkit.Location;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class ShrineManager {

    public record Shrine(ShrineType type, String world, int x, int y, int z) {
        public boolean matches(Location loc) {
            return loc.getWorld() != null
                    && loc.getWorld().getName().equals(world)
                    && loc.getBlockX() == x && loc.getBlockY() == y && loc.getBlockZ() == z;
        }
    }

    private final GambitSMP plugin;
    private final List<Shrine> shrines = new ArrayList<>();
    private final File file;

    public ShrineManager(GambitSMP plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "shrines.yml");
        load();
    }

    public void addShrine(ShrineType type, Location loc) {
        shrines.add(new Shrine(type, loc.getWorld().getName(), loc.getBlockX(), loc.getBlockY(), loc.getBlockZ()));
        save();
    }

    public Shrine findShrine(Location loc) {
        for (Shrine s : shrines) {
            if (s.matches(loc)) return s;
        }
        return null;
    }

    public List<Shrine> all() {
        return shrines;
    }

    public void load() {
        if (!file.exists()) return;
        FileConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        List<?> raw = yaml.getList("shrines");
        if (raw == null) return;
        for (Object o : raw) {
            if (!(o instanceof java.util.Map<?, ?> map)) continue;
            try {
                ShrineType type = ShrineType.valueOf((String) map.get("type"));
                String world = (String) map.get("world");
                int x = (int) map.get("x");
                int y = (int) map.get("y");
                int z = (int) map.get("z");
                shrines.add(new Shrine(type, world, x, y, z));
            } catch (Exception ignored) {}
        }
    }

    public void save() {
        FileConfiguration yaml = new YamlConfiguration();
        List<java.util.Map<String, Object>> raw = new ArrayList<>();
        for (Shrine s : shrines) {
            java.util.Map<String, Object> map = new java.util.HashMap<>();
            map.put("type", s.type().name());
            map.put("world", s.world());
            map.put("x", s.x());
            map.put("y", s.y());
            map.put("z", s.z());
            raw.add(map);
        }
        yaml.set("shrines", raw);
        try {
            if (!plugin.getDataFolder().exists()) plugin.getDataFolder().mkdirs();
            yaml.save(file);
        } catch (IOException e) {
            plugin.getLogger().warning("Failed to save shrines.yml: " + e.getMessage());
        }
    }
}
