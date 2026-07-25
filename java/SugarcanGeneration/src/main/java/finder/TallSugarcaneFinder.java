package finder;

import com.seedfinding.mcbiome.biome.Biomes;
import com.seedfinding.mcbiome.source.BiomeSource;
import com.seedfinding.mccore.rand.seed.WorldSeed;
import com.seedfinding.mccore.state.Dimension;
import com.seedfinding.mccore.util.block.BlockDirection;
import com.seedfinding.mccore.util.pos.BPos;
import com.seedfinding.mccore.util.pos.CPos;
import com.seedfinding.mccore.version.MCVersion;
import com.seedfinding.mcmath.util.Mth;
import com.seedfinding.mcreversal.ChunkRandomReverser;
import com.seedfinding.mcreversal.MultiChunkHelper;
import feature.DirtPatchFeature;
import feature.SugarCaneFeature;
import feature.WaterfallFeature;
import org.apache.commons.lang3.NotImplementedException;
import terrain.CubiomesCanyonGenerator;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static settings.SearchParameters.*;
import static util.TpCommand.tpCommand;

public class TallSugarcaneFinder extends SeedFinder {
    private final MultiChunkHelper tccrr = new MultiChunkHelper();

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
        new WaterfallChunkFinder(seedMin, seedMax).withLowerBits(SUGARCANE_POPSEED).run(chunk1Seeds);
        System.out.printf("-- candidates: %d \n", chunk1Seeds.size());

        // Double pop seed reversal

        int maxCount = 1_000, count = 0;
        for (long c1 : chunk1Seeds) {
            if (count++ > maxCount) {
                break;
            }
            for (var res : tccrr.getWorldseedFromTwoChunkseeds(c1, SUGARCANE_POPSEED, BLOCK_DX, BLOCK_DZ, MCVersion.v1_16_1)) {
                System.out.println("input: " + c1 + " & " + SUGARCANE_POPSEED);
                postFilter(res);
            }
        }
    }

    private void postFilter(MultiChunkHelper.Result res) {
        System.out.println(res.getX() + " " + res.getZ());

        long realPopseed = rand.setPopulationSeed(res.getBitsOfSeed(),
                res.getX() + BLOCK_DX, res.getZ() + BLOCK_DZ, MCVersion.v1_16_1);

        long realC1 = rand.setPopulationSeed(res.getBitsOfSeed(),
                res.getX(), res.getZ(), MCVersion.v1_16_1);

        System.out.println("got: " + realC1 + " & " + realPopseed);

        CPos waterfallChunk = new CPos(res.getX() >> 4, res.getZ() >> 4);
        CPos sugarcaneChunk = new CPos((res.getX() + BLOCK_DX) >> 4, (res.getZ() + BLOCK_DZ) >> 4);
        BPos sugarcaneRoot = sugarcaneChunk.toBlockPos(SUGARCANE_ROOT_Y)
                .add(SUGARCANE_RELATIVE_X, 0, SUGARCANE_RELATIVE_Z);

//        var waterfalls = WaterfallFeature.getAllNearColumn(
//                res.structureSeed(), waterfallChunk.getX(), waterfallChunk.getZ(),
//                res.blockX() + WATERCOL_RELATIVE_X, res.blockZ() + WATERCOL_RELATIVE_Z, rand
//                )
//                .stream().filter(bpos -> WATERFALL_MIN_Y <= bpos.getY() && bpos.getY() <= WATERFALL_MAX_Y)
//                .toList();
//        if (waterfalls.isEmpty()) {
//            return;
//        }
//
//        long popseed2 = rand.setPopulationSeed(res.structureSeed(), sugarcaneChunk.getX() << 4, sugarcaneChunk.getZ() << 4, version);
//        BPos dirt = new DirtPatchFeature().getFirstPos(res.structureSeed(), sugarcaneChunk.getX(), sugarcaneChunk.getZ());
//        BPos sugarcaneRoot = dirt.add(0, -2, 0);
//        BPos sugarcane = SugarCaneFeature.findSugarCaneStack(popseed2, sugarcaneChunk.getX(), sugarcaneChunk.getZ(), SUGARCANE_MIN_HEIGHT, SUGARCANE_ROOT_Y, dirt, rand);
//        if (sugarcane == null) {
//            return;
//        }

        // ravine pre-filter
        int ravines = 0;
        for (int dcx = -2; dcx <= 2; dcx++) for (int dcz = -2; dcz <= 2; dcz++) {
            if (CubiomesCanyonGenerator.startsAt(res.getBitsOfSeed(), sugarcaneChunk.getX() + dcx, sugarcaneChunk.getZ() + dcz)) {
                ravines++;
            }
        }
        if (ravines < 1) {
            return;
        }

        // check ravine stuff:
        // - air column at the sugarcane position
        // - ground right beneath sugarcaneRoot
        // - air column at the water column position, reaching down to 1 block beneath sugarcaneRoot
        // - one of the waterfalls generates thanks to ravine

        var airPosSet = CubiomesCanyonGenerator.getCanyonCarvedAir(res.getBitsOfSeed(), sugarcaneChunk.getX(), sugarcaneChunk.getZ())
                .stream().collect(Collectors.toUnmodifiableSet());
        if (airPosSet.size() < 200) {
            return;
        }
        //System.out.println("-- got a lot of air in " + res);

        if (!airColumnAt(airPosSet, sugarcaneRoot, 64) || airPosSet.contains(sugarcaneRoot.add(0, -1, 0))) {
            return;
        }
        System.out.println("---- air col 1 good for candidate " + res);
        System.out.println(tpCommand(sugarcaneRoot));

        var airPosSet2 = CubiomesCanyonGenerator.getCanyonCarvedAir(res.getBitsOfSeed(), waterfallChunk.getX(), waterfallChunk.getZ())
                .stream().collect(Collectors.toUnmodifiableSet());
        if (!airColumnAt(airPosSet2, sugarcaneRoot.subtract(CHUNK_DX, 1, CHUNK_DZ), WATERFALL_MIN_Y)) {
            return;
        }
        System.out.println("------ air col 2 good for candidate " + res);
//
//        var goodWaterfall = waterfalls.stream().filter(pos -> waterfallCanSpawn(airPosSet2, pos)).findFirst();
//        if (goodWaterfall.isEmpty()) {
//            return;
//        }
        System.out.println("-------- reached world seed check for candidate " + res);
        WorldSeed.getSisterSeeds(res.getBitsOfSeed() & Mth.MASK_48).asStream().boxed()
                .limit(256)
                .forEach(worldSeed -> {
                    BiomeSource obs = BiomeSource.of(Dimension.OVERWORLD, MCVersion.v1_16_1, worldSeed);

                    final int qRange = 3;
                    int quartX = (res.getX() + 8) >> 2;
                    int quartZ = (res.getZ() + 15) >> 2;
                    for (int dx = -qRange; dx <= qRange; dx++) {
                        for (int dz = -qRange; dz <= qRange; dz++) {
                            if (obs.getBiomeForNoiseGen(quartX + dx, 0, quartZ + dz) != Biomes.DESERT) {
                                return;
                            }
                        }
                    }

                    System.out.println(
                            "GOOD SEED! " + worldSeed + "  " + tpCommand(sugarcaneRoot)
                    );
                });
    }

    private static boolean waterfallCanSpawn(Set<BPos> airPosSet, BPos pos) {
        if (airPosSet.contains(pos.add(0, 1, 0)) || airPosSet.contains(pos.add(0, -1, 0))) {
            return false;
        }

        int airCount = 0;
        for (var dir : BlockDirection.getHorizontal()) {
            if (airPosSet.contains(pos.add(new BPos(dir.getVector())))) {
                airCount++;
            }
        }

        return airCount == 1;
    }

    private static boolean airColumnAt(Set<BPos> airPosSet, BPos columnBottom, int maxY) {
        for (int y = columnBottom.getY(); y <= maxY; y++) {
            if (!airPosSet.contains(new BPos(columnBottom.getX(), y, columnBottom.getZ()))) {
                return false;
            }
        }
        return true;
    }

    public static void main(String[] args) {
        long offset = 100_000L * 300;
        long batchSize = 100_000L;
        int numBatches = 100;

        for (int b = 0; b < numBatches; b++) {
            System.out.printf("===== batch %d/%d\n", b+1, numBatches);
            long start = b * batchSize + offset;
            long end = (b + 1) * batchSize + offset;
            new TallSugarcaneFinder(start, end).run(new ArrayList<>()); //.runThreaded(new ArrayList<>(), 1);
        }
    }
}
