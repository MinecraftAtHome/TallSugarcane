package finder;

import feature.WaterfallFeature;

import java.util.List;

import static settings.SearchParameters.*;
import static settings.SearchParameters.WATERFALL_MAX_Y;

class WaterfallChunkFinder extends SeedFinder {
    private int low4;

    protected WaterfallChunkFinder(long seedMin, long seedMax) {
        super(seedMin, seedMax);
    }

    @Override
    public SeedFinder forRange(long seedMin, long seedMax) {
        return new WaterfallChunkFinder(seedMin, seedMax);
    }

    public WaterfallChunkFinder withLowerBits(long populationSeed) {
        this.low4 = (int) (populationSeed & 15);
        return this;
    }

    @Override
    public void run(List<Long> resultsOut) {
        for (long seed = this.seedMin; seed < this.seedMax; seed++) {
            long realSeed = (seed << 4) | low4;
            if (checkSeed(realSeed)) {
                resultsOut.add(realSeed);
            }
        }
    }

    @Override
    protected boolean checkSeed(long seed) {
        var goodWaterfall = WaterfallFeature.getAllNearColumn(seed, 0, 0, WATERCOL_RELATIVE_X, WATERCOL_RELATIVE_Z, rand)
                .stream().filter(bpos -> WATERFALL_MIN_Y <= bpos.getY() && bpos.getY() <= WATERFALL_MAX_Y)
                .toList();
        return !goodWaterfall.isEmpty();
    }
}
