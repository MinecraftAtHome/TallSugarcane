package feature;

import com.seedfinding.mccore.rand.ChunkRand;
import com.seedfinding.mccore.util.pos.BPos;
import com.seedfinding.mccore.version.MCVersion;
import com.seedfinding.mcmath.util.Mth;

import java.util.ArrayList;
import java.util.List;

public class WaterfallFeature {
    // salts are for the shattered savannah biome
    private static final int SALT_WATER = 80_007;
    private static final int SALT_LAVA = 80_008;

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

    /*
   public class HeightBiasedRange extends SimplePlacement<CountRangeConfig> {
   public HeightBiasedRange(Codec<CountRangeConfig> p_i232071_1_) {
      super(p_i232071_1_);
   }

   public Stream<BlockPos> getPositions(Random random, CountRangeConfig p_212852_2_, BlockPos pos) {
      return IntStream.range(0, p_212852_2_.count).mapToObj((p_227436_3_) -> {
         int i = random.nextInt(16) + pos.getX();
         int j = random.nextInt(16) + pos.getZ();
         int k = random.nextInt(random.nextInt(p_212852_2_.maximum - p_212852_2_.topOffset) + p_212852_2_.bottomOffset);
         return new BlockPos(i, k, j);
      });
   }
}
     */

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
