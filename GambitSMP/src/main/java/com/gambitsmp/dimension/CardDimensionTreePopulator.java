package com.gambitsmp.dimension;

import com.gambitsmp.GambitSMP;
import org.bukkit.Material;
import org.bukkit.generator.BlockPopulator;
import org.bukkit.generator.LimitedRegion;

import java.util.Random;

/**
 * Places sparse spruce trees. These are real Material.SPRUCE_LOG / Material.SPRUCE_LEAVES
 * blocks - the "purple" comes from a resource pack texture override on spruce_leaves,
 * not a different block type. See the resource pack README for why spruce specifically
 * needs a texture-level trick rather than a biome-color trick (spruce/birch leaves
 * ignore biome foliage color entirely in vanilla - a hardcoded rendering quirk).
 */
public class CardDimensionTreePopulator extends BlockPopulator {

    private final int surfaceY;
    private final Random random = new Random();

    public CardDimensionTreePopulator(int surfaceY) {
        this.surfaceY = surfaceY;
    }

    @Override
    public void populate(org.bukkit.generator.WorldInfo worldInfo, Random random, int chunkX, int chunkZ, LimitedRegion limitedRegion) {
        GambitSMP plugin = GambitSMP.get();
        double chance = plugin.getConfig().getDouble("card-dimension.tree.chance-per-chunk", 0.12);
        if (random.nextDouble() > chance) return; // most chunks get no tree at all - "spread out spaciously"

        int x = (chunkX << 4) + 4 + random.nextInt(8);
        int z = (chunkZ << 4) + 4 + random.nextInt(8);

        int groundY = findSurface(limitedRegion, x, z);
        if (groundY < 0) return;

        placeTree(limitedRegion, x, groundY + 1, z);
    }

    private int findSurface(LimitedRegion region, int x, int z) {
        for (int y = surfaceY + 10; y >= surfaceY - 10; y--) {
            if (!region.isInRegion(x, y, z)) continue;
            if (region.getBlockAt(x, y, z).getType() == Material.GRASS_BLOCK) {
                return y;
            }
        }
        return -1;
    }

    private void placeTree(LimitedRegion region, int x, int baseY, int z) {
        int height = 5 + random.nextInt(3); // 5-7 tall trunk

        for (int i = 0; i < height; i++) {
            setIfInRegion(region, x, baseY + i, z, Material.SPRUCE_LOG);
        }

        // simple conical spruce canopy
        int topY = baseY + height;
        for (int layer = 0; layer < 4; layer++) {
            int y = topY - layer;
            int radius = layer == 0 ? 1 : (layer < 3 ? 2 : 1);
            for (int dx = -radius; dx <= radius; dx++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    if (dx == 0 && dz == 0 && layer != 0) continue; // keep trunk visible except at the very top cap
                    if (Math.abs(dx) == radius && Math.abs(dz) == radius && radius > 1) continue; // round the corners off
                    setIfInRegion(region, x + dx, y, z + dz, Material.SPRUCE_LEAVES);
                }
            }
        }
        setIfInRegion(region, x, topY + 1, z, Material.SPRUCE_LEAVES);
    }

    private void setIfInRegion(LimitedRegion region, int x, int y, int z, Material material) {
        if (!region.isInRegion(x, y, z)) return;
        if (region.getBlockAt(x, y, z).getType() == Material.AIR || material == Material.SPRUCE_LOG) {
            region.getBlockAt(x, y, z).setType(material);
        }
    }
}
