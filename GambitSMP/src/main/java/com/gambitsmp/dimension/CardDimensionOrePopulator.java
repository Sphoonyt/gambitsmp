package com.gambitsmp.dimension;

import com.gambitsmp.GambitSMP;
import org.bukkit.Material;
import org.bukkit.generator.BlockPopulator;
import org.bukkit.generator.LimitedRegion;
import org.bukkit.generator.WorldInfo;

import java.util.Random;

/**
 * Scatters ore veins through CardDimension's stone and deepslate layers.
 * Deliberately excludes diamond ore per spec. Ancient Debris (the netherite source
 * block) is included as a stand-in for "netherite spawns" - vanilla Netherite
 * doesn't have its own "ore" block, Ancient Debris smelted into scrap is the actual
 * source, so that's what generates here instead of a fictional "netherite ore."
 *
 * Y-bands are real world coordinates (not relative to surfaceY) so they correctly
 * span both the stone layer and the deepslate layer, matching where vanilla's own
 * Amplified generation actually places stone vs. deepslate (the transition is
 * around Y=0, tapering to fully deepslate by about Y=-8, regardless of how tall the
 * surface terrain happens to be at any given spot).
 */
public class CardDimensionOrePopulator extends BlockPopulator {

    private static final int DEEPSLATE_TRANSITION_Y = 0;

    private final int surfaceY;
    private final Random random = new Random();

    public CardDimensionOrePopulator(int surfaceY) {
        this.surfaceY = surfaceY;
    }

    @Override
    public void populate(WorldInfo worldInfo, Random random, int chunkX, int chunkZ, LimitedRegion limitedRegion) {
        GambitSMP plugin = GambitSMP.get();
        int coal = plugin.getConfig().getInt("card-dimension.ore.coal-per-chunk", 20);
        int iron = plugin.getConfig().getInt("card-dimension.ore.iron-per-chunk", 18);
        int gold = plugin.getConfig().getInt("card-dimension.ore.gold-per-chunk", 10);
        int redstone = plugin.getConfig().getInt("card-dimension.ore.redstone-per-chunk", 16);
        int lapis = plugin.getConfig().getInt("card-dimension.ore.lapis-per-chunk", 10);
        int copper = plugin.getConfig().getInt("card-dimension.ore.copper-per-chunk", 18);
        int ancientDebris = plugin.getConfig().getInt("card-dimension.ore.ancient-debris-per-chunk", 4);

        int minY = worldInfo.getMinHeight();

        // wide, generous bands spanning well down into deepslate territory
        placeVein(limitedRegion, chunkX, chunkZ, Material.COAL_ORE, Material.DEEPSLATE_COAL_ORE, coal, surfaceY - 45, surfaceY);
        placeVein(limitedRegion, chunkX, chunkZ, Material.IRON_ORE, Material.DEEPSLATE_IRON_ORE, iron, surfaceY - 65, surfaceY - 5);
        placeVein(limitedRegion, chunkX, chunkZ, Material.COPPER_ORE, Material.DEEPSLATE_COPPER_ORE, copper, surfaceY - 55, surfaceY - 10);
        placeVein(limitedRegion, chunkX, chunkZ, Material.GOLD_ORE, Material.DEEPSLATE_GOLD_ORE, gold, Math.max(minY, minY + 25), surfaceY - 30);
        placeVein(limitedRegion, chunkX, chunkZ, Material.REDSTONE_ORE, Material.DEEPSLATE_REDSTONE_ORE, redstone, Math.max(minY, minY + 15), surfaceY - 40);
        placeVein(limitedRegion, chunkX, chunkZ, Material.LAPIS_ORE, Material.DEEPSLATE_LAPIS_ORE, lapis, Math.max(minY, minY + 20), surfaceY - 35);
        // deepest and rarest, mirroring how ancient debris sits low in the Nether
        placeVein(limitedRegion, chunkX, chunkZ, Material.ANCIENT_DEBRIS, Material.ANCIENT_DEBRIS, ancientDebris, minY, Math.max(minY + 1, minY + 30));
    }

    /**
     * Places up to `attempts` ore blocks in [minY, maxY), replacing stone with
     * stoneOre above the deepslate transition and deepslate with deepslateOre below
     * it (Ancient Debris passes the same material for both, since it replaces either
     * host block identically).
     */
    private void placeVein(LimitedRegion region, int chunkX, int chunkZ, Material stoneOre, Material deepslateOre,
                            int attempts, int minY, int maxY) {
        if (attempts <= 0 || minY >= maxY) return;
        for (int i = 0; i < attempts; i++) {
            int x = (chunkX << 4) + random.nextInt(16);
            int z = (chunkZ << 4) + random.nextInt(16);
            int y = minY + random.nextInt(Math.max(1, maxY - minY));
            if (!region.isInRegion(x, y, z)) continue;

            Material host = region.getType(x, y, z);
            if (host == Material.STONE) {
                region.setType(x, y, z, stoneOre);
            } else if (host == Material.DEEPSLATE) {
                region.setType(x, y, z, deepslateOre);
            }
        }
    }
}
