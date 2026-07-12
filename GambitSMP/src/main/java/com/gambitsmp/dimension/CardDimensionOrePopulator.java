package com.gambitsmp.dimension;

import com.gambitsmp.GambitSMP;
import org.bukkit.Material;
import org.bukkit.generator.BlockPopulator;
import org.bukkit.generator.LimitedRegion;

import java.util.Random;

/**
 * Scatters ore veins through CardDimension's stone layer. Deliberately excludes
 * diamond ore per spec. Ancient Debris (the netherite source block) is included as
 * a stand-in for "netherite spawns" - vanilla Netherite doesn't have its own "ore"
 * block, Ancient Debris smelted into scrap is the actual source, so that's what
 * generates here instead of a fictional "netherite ore."
 */
public class CardDimensionOrePopulator extends BlockPopulator {

    private final int surfaceY;
    private final Random random = new Random();

    public CardDimensionOrePopulator(int surfaceY) {
        this.surfaceY = surfaceY;
    }

    @Override
    public void populate(org.bukkit.generator.WorldInfo worldInfo, Random random, int chunkX, int chunkZ, LimitedRegion limitedRegion) {
        GambitSMP plugin = GambitSMP.get();
        int coal = plugin.getConfig().getInt("card-dimension.ore.coal-per-chunk", 6);
        int iron = plugin.getConfig().getInt("card-dimension.ore.iron-per-chunk", 5);
        int gold = plugin.getConfig().getInt("card-dimension.ore.gold-per-chunk", 2);
        int redstone = plugin.getConfig().getInt("card-dimension.ore.redstone-per-chunk", 4);
        int lapis = plugin.getConfig().getInt("card-dimension.ore.lapis-per-chunk", 2);
        int copper = plugin.getConfig().getInt("card-dimension.ore.copper-per-chunk", 5);
        int ancientDebris = plugin.getConfig().getInt("card-dimension.ore.ancient-debris-per-chunk", 1);

        placeVein(limitedRegion, chunkX, chunkZ, Material.COAL_ORE, coal, surfaceY - 5, surfaceY - 1);
        placeVein(limitedRegion, chunkX, chunkZ, Material.IRON_ORE, iron, surfaceY - 15, surfaceY - 5);
        placeVein(limitedRegion, chunkX, chunkZ, Material.GOLD_ORE, gold, surfaceY - 25, surfaceY - 10);
        placeVein(limitedRegion, chunkX, chunkZ, Material.REDSTONE_ORE, redstone, surfaceY - 30, surfaceY - 15);
        placeVein(limitedRegion, chunkX, chunkZ, Material.LAPIS_ORE, lapis, surfaceY - 30, surfaceY - 10);
        placeVein(limitedRegion, chunkX, chunkZ, Material.COPPER_ORE, copper, surfaceY - 20, surfaceY - 5);
        // deepest and rarest, mirroring how ancient debris sits low in the Nether
        placeVein(limitedRegion, chunkX, chunkZ, Material.ANCIENT_DEBRIS, ancientDebris, surfaceY - 40, surfaceY - 25);
    }

    private void placeVein(LimitedRegion region, int chunkX, int chunkZ, Material ore, int attempts, int minY, int maxY) {
        if (attempts <= 0 || minY >= maxY) return;
        for (int i = 0; i < attempts; i++) {
            int x = (chunkX << 4) + random.nextInt(16);
            int z = (chunkZ << 4) + random.nextInt(16);
            int y = minY + random.nextInt(Math.max(1, maxY - minY));
            if (!region.isInRegion(x, y, z)) continue;
            if (region.getType(x, y, z) == Material.STONE) {
                region.setType(x, y, z, ore);
            }
        }
    }
}
