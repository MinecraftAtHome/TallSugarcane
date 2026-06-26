package finder;

import com.seedfinding.mccore.util.pos.BPos;
import feature.SugarCaneFeature;

import static settings.SearchParameters.*;

class SugarcaneChunkFinder extends SeedFinder {
    protected SugarcaneChunkFinder(long seedMin, long seedMax) {
        super(seedMin, seedMax);
    }

    @Override
    public SeedFinder forRange(long seedMin, long seedMax) {
        return new SugarcaneChunkFinder(seedMin, seedMax);
    }

    @Override
    protected boolean checkSeed(long seed) {
        BPos goodSugarcane = SugarCaneFeature.findSugarCaneStack(
                seed, 0, 0, // population seeded prng
                SUGARCANE_MIN_HEIGHT, SUGARCANE_ROOT_Y,
                rand
        );
        return goodSugarcane != null;
    }
}
