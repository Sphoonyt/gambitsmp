package com.gambitsmp.dimension;

import org.bukkit.block.Biome;
import org.bukkit.generator.BlockPopulator;
import org.bukkit.generator.LimitedRegion;
import org.bukkit.generator.WorldInfo;

import java.util.Random;

/**
 * Explicitly force-sets the biome across every generated chunk to the_void (whose
 * colors are overridden purple by the bundled datapack).
 *
 * This exists as a guaranteed backstop on top of CardDimensionGenerator's
 * getDefaultBiomeProvider(): the BiomeProvider hook is documented as composable with
 * vanilla noise generation, but in practice a preset like Amplified computes its own
 * biome as an integral part of its noise routing, and may not fully respect a custom
 * BiomeProvider for the actual generated terrain. Overwriting the biome here, after
 * terrain has already been shaped, sidesteps that uncertainty entirely - it's a
 * direct RegionAccessor call, not dependent on how any particular preset's internal
 * noise/biome coupling works.
 *
 * Biomes are stored at a coarser resolution than blocks - one Biome per 4x4x4
 * region ("quart" resolution) - so this steps by 4 blocks in each axis rather than
 * setting every single block.
 */
public class CardDimensionBiomePopulator extends BlockPopulator {

    @Override
    public void populate(WorldInfo worldInfo, Random random, int chunkX, int chunkZ, LimitedRegion limitedRegion) {
        int minY = worldInfo.getMinHeight();
        int maxY = worldInfo.getMaxHeight();

        int baseX = chunkX << 4;
        int baseZ = chunkZ << 4;

        for (int x = baseX; x < baseX + 16; x += 4) {
            for (int z = baseZ; z < baseZ + 16; z += 4) {
                for (int y = minY; y < maxY; y += 4) {
                    if (!limitedRegion.isInRegion(x, y, z)) continue;
                    limitedRegion.setBiome(x, y, z, Biome.THE_VOID);
                }
            }
        }
    }
}
