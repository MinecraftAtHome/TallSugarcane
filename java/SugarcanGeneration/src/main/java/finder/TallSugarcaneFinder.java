package finder;

import com.seedfinding.mcbiome.biome.Biomes;
import com.seedfinding.mcbiome.source.BiomeSource;
import com.seedfinding.mccore.block.Blocks;
import com.seedfinding.mccore.rand.seed.WorldSeed;
import com.seedfinding.mccore.state.Dimension;
import com.seedfinding.mccore.util.pos.BPos;
import com.seedfinding.mccore.version.MCVersion;
import com.seedfinding.mcreversal.TwoChunkCRR;
import feature.DirtPatchFeature;
import feature.SugarCaneFeature;
import feature.WaterfallFeature;
import org.apache.commons.lang3.NotImplementedException;
import terrain.ShatteredSavannahSurfaceGenerator;

import java.util.ArrayList;
import java.util.List;

import static settings.SearchParameters.*;

public class TallSugarcaneFinder extends SeedFinder {
    private final long sugarcanePopseed = 56977517902894L;
    private final TwoChunkCRR tccrr = new TwoChunkCRR();

    protected TallSugarcaneFinder(long seedMin, long seedMax) {
        super(seedMin, seedMax);
    }

    @Override
    public SeedFinder forRange(long seedMin, long seedMax) {
        return new TallSugarcaneFinder(seedMin, seedMax);
    }

    @Override
    protected boolean checkSeed(long seed) {
        throw new NotImplementedException("not implemented");
    }

    @Override
    public void run(List<Long> resultsOut) {
        ArrayList<Long> chunk1Seeds = new ArrayList<>();
        new WaterfallChunkFinder(seedMin, seedMax).withLowerBits(sugarcanePopseed).run(chunk1Seeds);
        System.out.printf("-- candidates: %d \n", chunk1Seeds.size());

        // Double pop seed reversal

        int maxCount = 1_000, count = 0;
        for (long c1 : chunk1Seeds) {
            if (count++ > maxCount) {
                break;
            }
            tccrr.getWorldseedFromTwoChunkseeds(c1, sugarcanePopseed, 16, 0, MCVersion.v1_16_1)
                    .forEach(this::postFilter);
        }
    }

    private void postFilter(TwoChunkCRR.Result res) {
        var waterfalls = WaterfallFeature.getAllNearColumn(
                res.structureSeed(), res.blockX() >> 4, res.blockZ() >> 4,
                res.blockX() + WATERCOL_RELATIVE_X, res.blockZ() + WATERCOL_RELATIVE_Z, rand
                )
                .stream().filter(bpos -> WATERFALL_MIN_Y <= bpos.getY() && bpos.getY() <= WATERFALL_MAX_Y)
                .toList();
        if (waterfalls.isEmpty()) {
            return;
        }

        long popseed2 = rand.setPopulationSeed(res.structureSeed(), res.blockX() + 16, res.blockZ(), version);
        BPos dirt = new DirtPatchFeature().getFirstPos(res.structureSeed(), (res.blockX() + 16) >> 4, res.blockZ() >> 4);
        BPos sugarcane = SugarCaneFeature.findSugarCaneStack(popseed2, (res.blockX() + 16) >> 4, res.blockZ() >> 4, SUGARCANE_MIN_HEIGHT, SUGARCANE_ROOT_Y, dirt, rand);

        if (sugarcane == null) {
            return;
        }

        // TODO cubiomes ravine check

        System.out.println("-- reached world seed check for candidate " + res);
        WorldSeed.getSisterSeeds(res.structureSeed()).asStream().boxed()
                .forEach(worldSeed -> {
                    BiomeSource obs = BiomeSource.of(Dimension.OVERWORLD, MCVersion.v1_16_1, worldSeed);

                    final int qRange = 3;
                    int quartX = (res.blockX() + 8) >> 2;
                    int quartZ = (res.blockZ() + 15) >> 2;
                    for (int dx = -qRange; dx <= qRange; dx++) {
                        for (int dz = -qRange; dz <= qRange; dz++) {
                            if (obs.getBiomeForNoiseGen(quartX + dx, 0, quartZ + dz) != Biomes.DESERT) {
                                return;
                            }
                        }
                    }

                    System.out.println(
                            "GOOD SEED! " + worldSeed
                            + "  at /tp @s " + res.blockX() + " 128 " + res.blockZ()
                    );
                });
    }


    public static void main(String[] args) {

        // sugarcane terrain height good Result[structureSeed=226376607997515, blockX=19593600, blockZ=9832256]
        // Result[structureSeed=153744200864954, blockX=26863392, blockZ=-19060736]

        //TwoChunkCRR.Result res = new TwoChunkCRR.Result(153744200864954L, 26863392, -19060736);
        //new TallSugarcaneFinder(0, 0).postFilter(res);
        //if (true) return;

        long offset = 0;
        long batchSize = 50_000_000L;
        int numBatches = 100;

        for (int b = 0; b < numBatches; b++) {
            System.out.printf("===== batch %d/%d\n", b+1, numBatches);
            long start = b * batchSize + offset;
            long end = (b + 1) * batchSize + offset;
            new TallSugarcaneFinder(start, end).runThreaded(new ArrayList<>(), 8);
        }
    }
}
