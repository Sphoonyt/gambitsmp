package com.gambitsmp.dimension;

import com.gambitsmp.GambitSMP;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.WorldType;
import org.bukkit.block.Block;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public class CardDimensionManager {

    private final GambitSMP plugin;
    private final File stateFile;

    private World world;
    private boolean activated;
    private final Set<Long> frameBlocks = new HashSet<>();   // packed x,y,z of the unbreakable obsidian frame
    private final Set<Long> interiorBlocks = new HashSet<>(); // packed x,y,z of the portal-plane interior
    private int lodestoneX, lodestoneY, lodestoneZ;
    private double spawnX, spawnY, spawnZ;
    private float spawnYaw;

    public CardDimensionManager(GambitSMP plugin) {
        this.plugin = plugin;
        this.stateFile = new File(plugin.getDataFolder(), "carddimension.yml");
    }

    /** Call once during onEnable, before anything else touches CardDimension. */
    public void setup() {
        extractDatapack();

        int surfaceY = plugin.getConfig().getInt("card-dimension.surface-y", 64);
        String worldName = plugin.getConfig().getString("card-dimension.world-name", "CardDimension");

        boolean firstCreation = Bukkit.getWorld(worldName) == null && !stateFile.exists();

        WorldCreator creator = new WorldCreator(worldName)
                .type(WorldType.AMPLIFIED)
                .generator(new CardDimensionGenerator(surfaceY))
                .generateStructures(false);
        world = creator.createWorld();

        if (world == null) {
            plugin.getLogger().severe("Failed to create/load the CardDimension world!");
            return;
        }
        world.setSpawnFlags(false, false); // no natural monster/animal spawns clutter the hub area

        if (stateFile.exists()) {
            load();
        } else if (firstCreation) {
            buildPortalStructure(surfaceY);
            save();
        }
    }

    // ------------------------------------------------------------- datapack

    /**
     * Copies the bundled biome-recolor datapack out of the plugin jar into the
     * default world's datapacks folder. Dynamic registry data (like biome colors)
     * only loads at server boot, so if this is the FIRST time it's copied in, a
     * restart is required before the purple color actually shows up - everything
     * else (world, portal, gameplay) works immediately regardless.
     */
    private void extractDatapack() {
        World defaultWorld = Bukkit.getWorlds().get(0);
        File targetDir = new File(defaultWorld.getWorldFolder(), "datapacks/gambitsmp_card_dimension");
        boolean isNew = !targetDir.exists();

        try {
            copyResourceFolder("carddimension_datapack", targetDir);
            if (isNew) {
                plugin.getLogger().warning("=================================================================");
                plugin.getLogger().warning("GambitSMP: installed the CardDimension biome-color datapack for");
                plugin.getLogger().warning("the first time. The purple grass/foliage color will NOT appear");
                plugin.getLogger().warning("until you fully RESTART the server (not /reload) - Minecraft only");
                plugin.getLogger().warning("loads new biome registry data at boot. Everything else about");
                plugin.getLogger().warning("CardDimension works right away.");
                plugin.getLogger().warning("=================================================================");
            }
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to extract CardDimension datapack: " + e.getMessage());
        }
    }

    private void copyResourceFolder(String resourcePrefix, File targetDir) throws IOException {
        URL jarUrl = plugin.getClass().getProtectionDomain().getCodeSource().getLocation();
        if (!jarUrl.getProtocol().equals("file")) {
            plugin.getLogger().warning("Can't extract datapack - plugin isn't running from a plain jar file.");
            return;
        }
        File jarFile = new File(jarUrl.getPath());
        try (JarFile jar = new JarFile(jarFile)) {
            Enumeration<JarEntry> entries = jar.entries();
            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();
                String name = entry.getName();
                if (!name.startsWith(resourcePrefix + "/") || entry.isDirectory()) continue;

                String relative = name.substring(resourcePrefix.length() + 1);
                File outFile = new File(targetDir, relative);
                outFile.getParentFile().mkdirs();
                try (InputStream in = jar.getInputStream(entry);
                     OutputStream out = Files.newOutputStream(outFile.toPath())) {
                    in.transferTo(out);
                }
            }
        }
    }

    // ------------------------------------------------------------- structure building

    private void buildPortalStructure(int surfaceY) {
        int width = plugin.getConfig().getInt("card-dimension.portal-width", 5);
        int height = plugin.getConfig().getInt("card-dimension.portal-height", 4);

        int baseX = 0;
        int baseZ = 0;
        int groundY = findGroundY(baseX, baseZ, surfaceY);
        int baseY = groundY + 1;

        frameBlocks.clear();
        interiorBlocks.clear();

        // Frame occupies a (width+2) x (height+2) rectangle in the X-Y plane at z=baseZ,
        // centered on baseX. Interior (the actual portal-plane blocks) is the width x height
        // region inside that frame.
        int halfSpan = (width + 2) / 2;
        for (int dx = -halfSpan; dx <= halfSpan; dx++) {
            for (int dy = 0; dy < height + 2; dy++) {
                boolean isEdge = (dx == -halfSpan || dx == halfSpan || dy == 0 || dy == height + 1);
                int x = baseX + dx;
                int y = baseY + dy;
                Block block = world.getBlockAt(x, y, baseZ);
                if (isEdge) {
                    block.setType(Material.OBSIDIAN, false);
                    frameBlocks.add(pack(x, y, baseZ));
                } else {
                    block.setType(Material.AIR, false); // stays unlit until activated via SpecialMatter
                    interiorBlocks.add(pack(x, y, baseZ));
                }
            }
        }

        // Lodestone a few blocks in front of the portal, at its base.
        lodestoneX = baseX;
        lodestoneY = baseY;
        lodestoneZ = baseZ + 3;
        world.getBlockAt(lodestoneX, lodestoneY, lodestoneZ).setType(Material.LODESTONE, false);

        // Spawn point: standing between the lodestone and the portal, facing the portal.
        spawnX = baseX + 0.5;
        spawnY = baseY;
        spawnZ = baseZ + 2.0;
        spawnYaw = 180f; // facing back toward -Z, i.e. toward the portal at baseZ

        activated = false;
    }

    private int findGroundY(int x, int z, int fallback) {
        Block highest = world.getHighestBlockAt(x, z);
        if (highest != null && highest.getType() != Material.AIR) {
            return highest.getY();
        }
        return fallback;
    }

    // ------------------------------------------------------------- activation

    public boolean isActivated() {
        return activated;
    }

    /** Fills the interior with real nether portal blocks and marks the portal usable. */
    public void activate() {
        if (activated) return;
        for (long packed : interiorBlocks) {
            Location loc = unpack(packed);
            loc.getBlock().setType(Material.NETHER_PORTAL, false);
        }
        activated = true;
        save();
    }

    // ------------------------------------------------------------- queries

    public boolean isFrameBlock(Location loc) {
        return world != null && loc.getWorld().equals(world) && frameBlocks.contains(pack(loc));
    }

    public boolean isPortalBlock(Location loc) {
        return world != null && loc.getWorld().equals(world) && interiorBlocks.contains(pack(loc));
    }

    public boolean isLodestoneLocation(Location loc) {
        return world != null && loc.getWorld().equals(world)
                && loc.getBlockX() == lodestoneX && loc.getBlockY() == lodestoneY && loc.getBlockZ() == lodestoneZ;
    }

    public World getWorld() {
        return world;
    }

    public Location getSpawnLocation() {
        return new Location(world, spawnX, spawnY, spawnZ, spawnYaw, 0f);
    }

    public String getOverworldName() {
        return plugin.getConfig().getString("card-dimension.overworld-world-name", "world");
    }

    // ------------------------------------------------------------- packing helpers

    private long pack(int x, int y, int z) {
        return (((long) (x & 0x3FFFFFF)) << 38) | (((long) (y & 0xFFF)) << 26) | (z & 0x3FFFFFF);
    }

    private long pack(Location loc) {
        return pack(loc.getBlockX(), loc.getBlockY(), loc.getBlockZ());
    }

    private Location unpack(long packed) {
        long x = (packed >> 38) & 0x3FFFFFF;
        long y = (packed >> 26) & 0xFFF;
        long z = packed & 0x3FFFFFF;
        // sign-extend the 26/12-bit fields back to normal ints
        if (x >= (1L << 25)) x -= (1L << 26);
        if (y >= (1L << 11)) y -= (1L << 12);
        if (z >= (1L << 25)) z -= (1L << 26);
        return new Location(world, x, y, z);
    }

    // ------------------------------------------------------------- persistence

    private void save() {
        FileConfiguration yaml = new YamlConfiguration();
        yaml.set("activated", activated);
        yaml.set("lodestone.x", lodestoneX);
        yaml.set("lodestone.y", lodestoneY);
        yaml.set("lodestone.z", lodestoneZ);
        yaml.set("spawn.x", spawnX);
        yaml.set("spawn.y", spawnY);
        yaml.set("spawn.z", spawnZ);
        yaml.set("spawn.yaw", (double) spawnYaw);

        List<String> frameList = new ArrayList<>();
        for (long packed : frameBlocks) frameList.add(Long.toString(packed));
        yaml.set("frame-blocks", frameList);

        List<String> interiorList = new ArrayList<>();
        for (long packed : interiorBlocks) interiorList.add(Long.toString(packed));
        yaml.set("interior-blocks", interiorList);

        try {
            if (!plugin.getDataFolder().exists()) plugin.getDataFolder().mkdirs();
            yaml.save(stateFile);
        } catch (IOException e) {
            plugin.getLogger().warning("Failed to save carddimension.yml: " + e.getMessage());
        }
    }

    private void load() {
        FileConfiguration yaml = YamlConfiguration.loadConfiguration(stateFile);
        activated = yaml.getBoolean("activated", false);
        lodestoneX = yaml.getInt("lodestone.x");
        lodestoneY = yaml.getInt("lodestone.y");
        lodestoneZ = yaml.getInt("lodestone.z");
        spawnX = yaml.getDouble("spawn.x");
        spawnY = yaml.getDouble("spawn.y");
        spawnZ = yaml.getDouble("spawn.z");
        spawnYaw = (float) yaml.getDouble("spawn.yaw", 180.0);

        frameBlocks.clear();
        for (String s : yaml.getStringList("frame-blocks")) {
            frameBlocks.add(Long.parseLong(s));
        }
        interiorBlocks.clear();
        for (String s : yaml.getStringList("interior-blocks")) {
            interiorBlocks.add(Long.parseLong(s));
        }
    }
}
