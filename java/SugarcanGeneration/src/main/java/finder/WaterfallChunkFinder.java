package finder;

import com.seedfinding.mccore.util.pos.BPos;
import feature.WaterfallFeature;

class WaterfallChunkFinder extends SeedFinder {
    protected WaterfallChunkFinder(long seedMin, long seedMax) {
        super(seedMin, seedMax);
    }

    @Override
    public SeedFinder forRange(long seedMin, long seedMax) {
        return new WaterfallChunkFinder(seedMin, seedMax);
    }

    @Override
    protected boolean checkSeed(long seed) {
        BPos goodWaterfall = WaterfallFeature.goodWaterfallInChunk(seed, rand);
        return goodWaterfall != null;
    }
}
