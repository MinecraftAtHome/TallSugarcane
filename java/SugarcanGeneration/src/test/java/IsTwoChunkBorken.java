import com.seedfinding.mccore.rand.ChunkRand;
import com.seedfinding.mccore.version.MCVersion;
import com.seedfinding.mcmath.util.Mth;
import com.seedfinding.mcreversal.ChunkRandomReverser;
import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class IsTwoChunkBorken {
    @Test
    public void testTwoChunk() {

        for (int i = 0; i < 100; i++) {
            long seed1 = 29846539632599L;
            for (int j = 0; j < 100; j++) {
                long seed2 = (new Random().nextLong() & (~0xF)) | (seed1 & 0xF);
                ChunkRandomReverser.getWorldseedFromTwoChunkseeds(seed1, seed2, 1, 0, MCVersion.v1_16_1)
                        .forEach(res -> {
                            long structureSeed = res.getBitsOfSeed();

                            long realSeed1 = new ChunkRand().setPopulationSeed(structureSeed, res.getX(), res.getZ(), MCVersion.v1_16_1);
                            long realSeed2 = new ChunkRand().setPopulationSeed(structureSeed, res.getX() + 16, res.getZ(), MCVersion.v1_16_1);

                            System.out.println("check");
                            assertEquals(realSeed1 & Mth.MASK_48, seed1 & Mth.MASK_48);
                            assertEquals(realSeed2 & Mth.MASK_48, seed2 & Mth.MASK_48);
                        });
            }
        }


    }
}
