package feature;

import com.seedfinding.mccore.rand.ChunkRand;
import com.seedfinding.mccore.util.pos.BPos;
import com.seedfinding.mccore.version.MCVersion;
import com.seedfinding.mcmath.util.Mth;

import java.util.ArrayList;
import java.util.List;

import static settings.SearchParameters.*;

public class WaterfallFeature {
    // salts are for the shattered savannah biome
    private static final int SALT_WATER = 80_007;
    private static final int SALT_LAVA = 80_008;

    /**
     * Returns a list of all waterfall source blocks in the provided chunk (chunkX, chunkZ) such that
     * the generated waterfall can form a column of water at (colX, colZ)
     */
    public static List<BPos> getAllNearColumn(long structureSeed, int chunkX, int chunkZ, int colX, int colZ, ChunkRand rand) {
        rand.setDecoratorSeed(structureSeed, chunkX << 4, chunkZ << 4, SALT_WATER, MCVersion.v1_16_1);

        //new CountRangeConfig(count:50, bottomOffset:8, topOffset:8, maximum:256)
        ArrayList<BPos> results = new ArrayList<>();
        for (int i = 0; i < 50; i++) {
            int x = rand.nextInt(16) + (chunkX << 4);
            int z = rand.nextInt(16) + (chunkZ << 4);
            int y = rand.nextInt(rand.nextInt(256 - 8) + 8);
            if (Math.abs(x - colX) + Math.abs(z - colZ) == 1)
                results.add(new BPos(x, y, z));
        }
        return results;
    }

    public static BPos goodWaterfallInChunk(long populationSeed, ChunkRand rand) {
        rand.setDecoratorSeed(populationSeed, SALT_WATER, MCVersion.v1_16_1);

        int x = rand.nextInt(16);
        if (x != WATERFALL_RELATIVE_X) {
            return null;
        }
        int z = rand.nextInt(16);
        if (z != WATERFALL_RELATIVE_Z) {
            return null;
        }
        int y = rand.nextInt(rand.nextInt(256 - 8) + 8);
        if (y < WATERFALL_MIN_Y || y > WATERFALL_MAX_Y) {
            return null;
        }

        return new BPos(x, y, z);
    }

    public static void main(String[] args) {
        long seed = -8981924485009184440L;
        int chunkX = 297;
        int chunkZ = 49;
        int colX = 4754;
        int colZ = 795;

        WaterfallFeature.getAllNearColumn(seed & Mth.MASK_48, chunkX, chunkZ, colX, colZ, new ChunkRand())
                .forEach(System.out::println);
    }
}
