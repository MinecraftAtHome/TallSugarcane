package feature;

import com.seedfinding.mccore.block.Blocks;
import com.seedfinding.mccore.rand.ChunkRand;
import com.seedfinding.mccore.util.block.BlockDirection;
import com.seedfinding.mccore.util.pos.BPos;
import com.seedfinding.mccore.util.pos.CPos;
import com.seedfinding.mcterrain.terrain.SurfaceGenerator;
import terrain.ShatteredSavannahSurfaceGenerator;

import java.util.List;

public class WaterfallFeatureFilter {
    private final ChunkRand rand = new ChunkRand();

    /**
     * Returns the maximum possible height of a sugarcane plant at sugarcaneRootPos
     * allowed by flowing waterfall features in the provided, adjacent chunk.
     */
    public int getMaxSugarcaneHeight(long structureSeed, BPos sugarcaneRootPos, CPos waterfallChunkPos) {
        CPos diff = waterfallChunkPos.subtract(sugarcaneRootPos.toChunkPos());
        if (diff.getMagnitudeSq() != 1) {
            throw new IllegalArgumentException("Waterfall chunk must be adjacent to sugarcane root chunk");
        }
        int colX = sugarcaneRootPos.getX() + diff.getX();
        int colZ = sugarcaneRootPos.getZ() + diff.getZ();

        List<BPos> goodWaterfalls = WaterfallFeature.getAllNearColumn(
                structureSeed, waterfallChunkPos.getX(), waterfallChunkPos.getZ(),
                colX, colZ, rand
        ).stream().filter(pos -> pos.getY() > sugarcaneRootPos.getY()).toList();
        if (goodWaterfalls.isEmpty())
            return 0;

        SurfaceGenerator sgen = new ShatteredSavannahSurfaceGenerator(structureSeed);
        int maxHeight = 0;
        for (BPos waterfallPos : goodWaterfalls) {
            if (!waterfallCanGenerate(sgen, waterfallPos, colX, colZ)) {
                continue;
            }
            if (hasSuitableTerrain(sgen, waterfallPos, sugarcaneRootPos)) {
                maxHeight = Math.max(maxHeight, waterfallPos.getY() - sugarcaneRootPos.getY() + 3);
            }
        }

        return 0;
    }

    /**
     * Tests if a waterfall can generate at waterfallPos and form a column of water at (colX, colZ)
     */
    private boolean waterfallCanGenerate(SurfaceGenerator sgen, BPos waterfallPos, int colX, int colZ) {
        // air block at column position
        if (sgen.getBlockAt(colX, waterfallPos.getY(), colZ).orElseThrow().getId() != Blocks.AIR.getId()) {
            return false;
        }

        // 5 solid blocks in total around the waterfall
        int solidBlocks = 0;
        for (BlockDirection dir : BlockDirection.values()) {
            if (sgen.getBlockAt(colX, waterfallPos.getY(), colZ).orElseThrow().getId() == Blocks.STONE.getId()) {
                solidBlocks++;
            }
        }

        return solidBlocks == 5;
    }

    /**
     * Tests whether the water column flowing at (colX, colZ) will land on suitable terrain
     * for sugarcane growth starting at sugarcaneRootPos, and whether the terrain height matches
     * the required surface y value.
     */
    private boolean hasSuitableTerrain(SurfaceGenerator sgen, BPos sugarcaneRootPos, int colX, int colZ) {
        int y1 = sgen.getHeightOnGround(sugarcaneRootPos.getX(), sugarcaneRootPos.getZ());
        if (y1 != sugarcaneRootPos.getY()) {
            return false;
        }

        int y2 = sgen.getHeightOnGround(colX, colZ);
        return y2 < y1; // so that the water also allows the first sugarcane to grow
    }
}
