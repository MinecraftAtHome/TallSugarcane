import com.seedfinding.mccore.version.MCVersion;
import com.seedfinding.mcreversal.ChunkRandomReverser;
import com.seedfinding.mcreversal.TwoChunkCRR;

import java.util.Random;
import java.util.function.Supplier;

public class PopReversalBench {
    private static long t0, t1;

    private static void benchMethod(Supplier<Integer> method, String displayName) {
        final int runs = 5;
        double rpsAvg = 0.0;
        for (int i = 0; i < runs; i++) {
            t0 = System.nanoTime();
            int results = method.get();
            t1 = System.nanoTime();
            double elapsedSecs = (t1 - t0) / 1.0e9;
            rpsAvg += (results / elapsedSecs) / runs;
        }
        System.out.printf("Method \"%s\" - Average RPS: %f\n", displayName, rpsAvg);
    }

    private static int singleChunkPopseedReversal() {
        int results = 0;
        long popseed = new Random().nextLong();
        for (int x = 1; x < 20; x++) {
            for (int z = 1; z < 20; z++) {
                results += ChunkRandomReverser.reversePopulationSeed(popseed, (x * 2 + 1) << 4, (z * 2 + 1) << 4, MCVersion.v1_16_1).size();
            }
        }
        return results;
    }

    private static int twoChunkPopseedReversal() {
        int results = 0;
        long popseedA = new Random().nextLong();
        long popseedB = ((new Random().nextLong() >> 4) << 4) | (popseedA & 15);
        TwoChunkCRR crr = new TwoChunkCRR();

        for (int i = 0; i < 100; i++) {
            results += crr.getWorldseedFromTwoChunkseeds(popseedA, popseedB + 16 * i, 0, 16, MCVersion.v1_16_1).size();
        }
        return results;
    }

    public static void main(String[] args) {
        benchMethod(PopReversalBench::singleChunkPopseedReversal, "one chunk CRR");
        benchMethod(PopReversalBench::twoChunkPopseedReversal, "two chunk CRR");
    }
}
