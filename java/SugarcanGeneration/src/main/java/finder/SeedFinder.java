package finder;

import com.seedfinding.mccore.rand.ChunkRand;
import com.seedfinding.mccore.version.MCVersion;

import java.util.ArrayList;
import java.util.List;

public abstract class SeedFinder {
    protected final MCVersion version = MCVersion.v1_16_1;
    protected final ChunkRand rand = new ChunkRand();

    protected long seedMin;
    protected long seedMax;

    protected SeedFinder(long seedMin, long seedMax) {
        this.seedMin = seedMin;
        this.seedMax = seedMax;
    }

    /**
     * Factory-like method - returns a new instance of the particular SeedFinder subclass
     */
    public abstract SeedFinder forRange(long seedMin, long seedMax);

    /**
     * Seed filtering logic goes here.
     */
    protected abstract boolean checkSeed(long seed);

    /**
     * Runs the finder and appends results to resultsOut.
     */
    public void run(List<Long> resultsOut) {
        for (long seed = this.seedMin; seed < this.seedMax; seed++) {
            if (checkSeed(seed)) {
                resultsOut.add(seed);
            }
        }
    }

    /**
     * Runs the finder and appends results to resultsOut, splitting the assigned work range
     * into multiple parallel batches.
     */
    public void runThreaded(List<Long> resultsOut, int numThreads) {
        try {
            ArrayList<ArrayList<Long>> resultLists = new ArrayList<>();
            Thread[] threads = new Thread[numThreads];

            long seedsTotal = this.seedMax - this.seedMin;
            long seedsPerThread = Math.ceilDiv(seedsTotal, numThreads);

            for (int tid = 0; tid < numThreads; tid++) {
                resultLists.add(new ArrayList<>());
                final ArrayList<Long> finderOut = resultLists.get(tid);
                final long seedMinLocal = tid * seedsPerThread;
                final long seedMaxLocal = (tid + 1) * seedsPerThread;

                threads[tid] = new Thread(
                        () -> {
                            SeedFinder finder = this.forRange(seedMinLocal, seedMaxLocal);
                            finder.run(finderOut);
                        }
                );
            }

            for (int tid = 0; tid < numThreads; tid++) {
                threads[tid].start();
            }
            for (int tid = 0; tid < numThreads; tid++) {
                threads[tid].join();
            }

            // Combine result lists and
            for (var list : resultLists) {
                resultsOut.addAll(list);
            }
        }
        catch (InterruptedException ex) {
            System.err.println("SeedFinder.runThreaded stopped - interrupted.");
        }
    }
}
