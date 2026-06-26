package finder;

import com.seedfinding.mcbiome.biome.Biomes;
import com.seedfinding.mcbiome.source.BiomeSource;
import com.seedfinding.mccore.block.Blocks;
import com.seedfinding.mccore.rand.seed.WorldSeed;
import com.seedfinding.mccore.state.Dimension;
import com.seedfinding.mccore.util.pos.BPos;
import com.seedfinding.mccore.version.MCVersion;
import com.seedfinding.mcreversal.TwoChunkCRR;
import feature.SugarCaneFeature;
import feature.WaterfallFeature;
import org.apache.commons.lang3.NotImplementedException;
import terrain.ShatteredSavannahSurfaceGenerator;

import java.util.ArrayList;
import java.util.List;

import static settings.SearchParameters.*;

public class TallSugarcaneFinder extends SeedFinder {
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
        ArrayList<Long> chunk2Seeds = new ArrayList<>();

        new WaterfallChunkFinder(seedMin/10, seedMax/10).run(chunk1Seeds);
        new SugarcaneChunkFinder(seedMin*10, seedMax*10).run(chunk2Seeds);

        //long s1 = chunk1Seeds.size(), s2 = chunk2Seeds.size();
        //System.out.printf("-- candidates: %d x %d = %d\n", s1, s2, s1 * s2);

        // Double pop seed reversal

        for (long c1 : chunk1Seeds) {
            for (long c2 : chunk2Seeds) {
                if ((c1 & 15) != (c2 & 15)) {
                    continue;
                }
                tccrr.getWorldseedFromTwoChunkseeds(c1, c2, 0, 16, MCVersion.v1_16_1).forEach(this::postFilter);
            }
        }
    }

    private void postFilter(TwoChunkCRR.Result res) {
        // post filter checks for A: good terrain, B: good biome

        long popseed = rand.setPopulationSeed(res.structureSeed(), res.blockX(), res.blockZ(), version);
        BPos waterfall = WaterfallFeature.goodWaterfallInChunk(popseed, rand);

        long popseed2 = rand.setPopulationSeed(res.structureSeed(), res.blockX(), res.blockZ() + 16, version);
        BPos sugarcane = SugarCaneFeature.findSugarCaneStack(popseed2, res.blockX() >> 4, (res.blockZ() + 16) >> 4, 8, SUGARCANE_ROOT_Y, rand);

        if (waterfall == null || sugarcane == null) {
            return;
        }

        // terrain at sugarcane x,z has to be exactly y=SUGARCANE_ROOT_Y
        ShatteredSavannahSurfaceGenerator sgen = new ShatteredSavannahSurfaceGenerator(res.structureSeed());
        if (sgen.getHeightOnGround(sugarcane.getX(), sugarcane.getZ()) != SUGARCANE_ROOT_Y) {
            return;
        }
        //System.out.println("-- sugarcane terrain height good " + res);

        // waterfall conditions
        BPos opening = waterfall.add(0, 0, 1);
        BPos[] solidBlocks = new BPos[] {
            waterfall.add(0, 0, -1),
            waterfall.add(0, 1, 0),
            waterfall.add(0, -1, 0),
            waterfall.add(1, 0, 0),
            waterfall.add(-1, 0, 0),
        };
        if (sgen.getBlockAt(opening).get().getId() != Blocks.AIR.getId()) {
            return;
        }
        for (var pos : solidBlocks) {
            if (sgen.getBlockAt(pos).get().getId() == Blocks.AIR.getId()) {
                return;
            }
        }
        System.out.println("-- waterfall good " + res);

        // waterfall reaches all the way down to 1 block below minRootY
        if (sgen.getHeightOnGround(opening.getX(), opening.getZ()) >= SUGARCANE_ROOT_Y) {
            return;
        }

        System.out.println("-- reached world seed check for candidate " + res);
        WorldSeed.getSisterSeeds(res.structureSeed()).asStream().boxed()
                .forEach(worldSeed -> {
                    BiomeSource obs = BiomeSource.of(Dimension.OVERWORLD, MCVersion.v1_16_1, worldSeed);

                    final int qRange = 3;
                    int quartX = (res.blockX() + 8) >> 2;
                    int quartZ = (res.blockZ() + 15) >> 2;
                    for (int dx = -qRange; dx <= qRange; dx++) {
                        for (int dz = -qRange; dz <= qRange; dz++) {
                            if (obs.getBiomeForNoiseGen(quartX + dx, 0, quartZ + dz) != Biomes.SHATTERED_SAVANNA) {
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
        long offset = 100_000_000L * 100;
        long batchSize = 100_000_000L;
        int numBatches = 100;

        for (int b = 0; b < numBatches; b++) {
            System.out.printf("===== batch %d/%d\n", b+1, numBatches);
            long start = b * batchSize + offset;
            long end = (b + 1) * batchSize + offset;
            new TallSugarcaneFinder(start, end).runThreaded(new ArrayList<>(), 8);
        }
    }
}
