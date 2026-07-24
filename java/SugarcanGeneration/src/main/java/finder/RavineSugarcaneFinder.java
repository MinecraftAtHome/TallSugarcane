package finder;

import com.seedfinding.mccore.rand.ChunkRand;
import feature.DirtPatchFeature;

public class RavineSugarcaneFinder {
    public static void main(String[] args) {
        DirtPatchFeature dirt = new DirtPatchFeature();
        ChunkRand rand = new ChunkRand();

        // state-predict for ravine at 0,0
        long spMax = (1L << 48) / 50;
        for (long iseed = 0; iseed < spMax; iseed++) {

        }
    }
}
