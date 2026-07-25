package finder;

import com.seedfinding.mccore.rand.ChunkRand;
import com.seedfinding.mccore.util.pos.BPos;
import com.seedfinding.mcseed.lcg.LCG;
import feature.DirtPatchFeature;
import feature.LazyDesertSugarCane;
import feature.SugarCaneFeature;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import static settings.SearchParameters.DIRT_INDENT;
import static settings.SearchParameters.DIRT_Y;

public class DirtPlusSugarcanePopseed {
    private static final long MAX_SEED = 1L << 40;
    private static final long BATCH_SIZE = 100_000_000L;
    private static final int THREAD_COUNT = 10;

    // found this: 56977517902894
    public static void main(String[] args) {
        AtomicLong currentSeed = new AtomicLong(0);
        ExecutorService executor = Executors.newFixedThreadPool(THREAD_COUNT);

        for (int i = 0; i < THREAD_COUNT; i++) {
            executor.submit(() -> {
                while (true) {
                    long iseedMin = currentSeed.getAndAdd(BATCH_SIZE);
                    if (iseedMin >= MAX_SEED) {
                        break;
                    }

                    long iseedMax = Math.min(iseedMin + BATCH_SIZE, MAX_SEED);
                    search(iseedMin, iseedMax);

                    System.out.printf("[%s] Completed seed range: %,d to %,d%n",
                            Thread.currentThread().getName(), iseedMin, iseedMax);
                }
            });
        }

        // Wait for all threads to finish processing
        executor.shutdown();
        try {
            executor.awaitTermination(Long.MAX_VALUE, TimeUnit.DAYS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.err.println("Main execution interrupted.");
        }
    }

    public static void search(long iseedMin, long iseedMax) {
        DirtPatchFeature dirt = new DirtPatchFeature();
        LCG back3 = LCG.JAVA.combine(-3);
        LazyDesertSugarCane sugarcane = new LazyDesertSugarCane();

        for (long iseed = iseedMin; iseed < iseedMax; iseed++) {
            long iseedFull = iseed | ((long)DIRT_Y << 40);
            long idecoseed = back3.nextSeed(iseedFull);
            long popseed = (idecoseed ^ LCG.JAVA.multiplier) - dirt.getSalt();

            BPos dirtPos = dirt.getFirstPosFromPopulationSeed(popseed, 0, 0);
            if (!atChunkBorder(dirtPos)) {
                continue;
            }
//            if (dirtPos.getY() != DIRT_Y) {
//                System.err.println("messed up");
//                return;
//            }

//            BPos sugarcaneStack = SugarCaneFeature.findSugarCaneStack(
//                    popseed, 0, 0,
//                    12, dirtPos.getY() - 2, dirtPos,
//                    rand
//            );

            int height = sugarcane.getStackHeightAt(popseed, dirtPos.add(0, -DIRT_INDENT, 0));
            if (height < 14) {
                continue;
            }

            System.out.println(height);
            System.out.println(dirtPos);
            System.out.println(popseed);
        }
    }

    private static boolean atChunkBorder(BPos pos) {
        int rx = pos.getX() & 15;
        int rz = pos.getZ() & 15;
        return rx == 0 || rz == 0 || rx == 15 || rz == 15;
    }
}
