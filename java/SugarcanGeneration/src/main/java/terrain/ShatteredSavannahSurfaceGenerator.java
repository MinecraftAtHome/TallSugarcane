package terrain;

import com.seedfinding.mcbiome.biome.Biome;
import com.seedfinding.mcbiome.biome.Biomes;
import com.seedfinding.mcbiome.source.BiomeSource;
import com.seedfinding.mccore.block.Block;
import com.seedfinding.mccore.state.Dimension;
import com.seedfinding.mccore.version.MCVersion;
import com.seedfinding.mcterrain.terrain.OverworldTerrainGenerator;

public class ShatteredSavannahSurfaceGenerator extends OverworldTerrainGenerator {
    private static final double depth;
    private static final double scale;
    static {
        // assuming we're in middle of shattered savannah we can precompute depth and scale
        int sampleRange = 2;
        float weightedScale = 0.0F;
        float weightedDepth = 0.0F;
        float totalWeight = 0.0F;
        Biome biome = Biomes.SHATTERED_SAVANNA;
        float depthAtCenter = biome.getDepth();
        for(int rx = -sampleRange; rx <= sampleRange; ++rx) {
            for(int rz = -sampleRange; rz <= sampleRange; ++rz) {
                float depth = biome.getDepth();
                float scale = biome.getScale();

                float weight = BIOME_WEIGHT_TABLE[rx + 2 + (rz + 2) * 5] / (depth + 2.0F);
                if(biome.getDepth() > depthAtCenter) {
                    weight /= 2.0F;
                }

                weightedScale += scale * weight;
                weightedDepth += depth * weight;
                totalWeight += weight;
            }
        }
        weightedScale /= totalWeight;
        weightedDepth /= totalWeight;
        weightedScale = weightedScale * 0.9F + 0.1F;
        weightedDepth = (weightedDepth * 4.0F - 1.0F) / 8.0F;
        depth = weightedDepth * 17.0D / 64.0D;
        scale = 96.0D / weightedScale;
    }

    public ShatteredSavannahSurfaceGenerator(long structureSeed) {
        super(BiomeSource.of(Dimension.OVERWORLD, MCVersion.v1_16_1, structureSeed));
    }

    @Override
    public int getBedrockRoofPosition() {
        return 0;
    }

    @Override
    protected double[] getDepthAndScale(int x, int z) {
        return new double[]{depth, scale};
    }

    public static void main(String[] args) {
        // using a single floating block for testing
        ShatteredSavannahSurfaceGenerator sgen = new ShatteredSavannahSurfaceGenerator(123L);
        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                for (int dz = -1; dz <= 1; dz++) {
                    Block b = sgen.getBlockAt(3272+dx, 80+dy, -1520+dz).get(); ///setblock 3272 80 -1520 minecraft:coarse_dirt
                    System.out.println(b);
                }
                System.out.println();
            }
            System.out.println();
        }
    }
}