package feature;

import com.seedfinding.mccore.rand.ChunkRand;
import com.seedfinding.mccore.util.pos.BPos;
import com.seedfinding.mccore.version.MCVersion;
import com.seedfinding.mcmath.util.Mth;

import java.util.ArrayList;
import java.util.List;

public class WaterfallFeature {
    // salts are for the desert biome
    private static final int SALT_WATER = 80_008;

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

            int dx = Math.abs(x - colX);
            int dz = Math.abs(z - colZ);
            if (dx + dz <= 2 && dx != dz)
                results.add(new BPos(x, y, z));
        }
        return results;
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
