import com.seedfinding.mccore.rand.ChunkRand;
import com.seedfinding.mccore.util.pos.BPos;
import com.seedfinding.mccore.util.pos.CPos;
import com.seedfinding.mccore.version.MCVersion;
import com.seedfinding.mcseed.lcg.LCG;
import feature.DirtPatchFeature;
import feature.SugarCaneFeature;
import org.junit.jupiter.api.Test;
import terrain.CubiomesCanyonGenerator;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static settings.SearchParameters.SUGARCANE_POPSEED;


public class WorldgenRegressionTests {
    @Test
    public void testDirtPatch() {
        DirtPatchFeature dirt = new DirtPatchFeature();
        List<CPos> chunkPositions = List.of(
            //new CPos(-23, -13),
            new CPos(-23, -14),
            new CPos(-24, -13)
            //new CPos(-24, -14)
        );
        List<BPos> expectedDirt = List.of(
            new BPos(-366, 14, -217),
            new BPos(-379, 59, -204)
        );

        assertEquals(chunkPositions.size(), expectedDirt.size());

        for (int i = 0; i < expectedDirt.size(); i++) {
            var pos = chunkPositions.get(i);
            var expectedDirtPos = expectedDirt.get(i);
            BPos gotPos = dirt.getFirstPos(123456, pos.getX(), pos.getZ());
            assertEquals(gotPos, expectedDirtPos);
        }
    }

    @Test
    public void testDirtPatchCorrectness() {
        // 204913391727623 /tp -29999872 43 -28579971

        long seed = 204913391727623L;
        BPos expectedPos = new BPos(-29999872, 43, -28579971);
        CPos chunk = expectedPos.toChunkPos();
        DirtPatchFeature dirt = new DirtPatchFeature();
        System.out.println(dirt.getFirstPos(seed, chunk.getX(), chunk.getZ()));
        System.out.println(dirt.getFirstPosFromPopulationSeed(SUGARCANE_POPSEED, chunk.getX(), chunk.getZ()));

        for (int dcx = -2; dcx <= 2; dcx++) for (int dcz = -2; dcz <= 2; dcz++) {
            System.out.println(new ChunkRand().setPopulationSeed(
                    seed,
                    (chunk.getX() + dcx) << 4,
                    (chunk.getZ() + dcz) << 4,
                    MCVersion.v1_16_1
            ) + "  at " + dcx + " " + dcz);
        }
    }

    @Test
    public void testCanyonStart() {
        assertTrue(CubiomesCanyonGenerator.startsAt(123456, -23, -14));
        assertFalse(CubiomesCanyonGenerator.startsAt(123456, -22, -14));
        assertFalse(CubiomesCanyonGenerator.startsAt(123456, -23, -15));
    }

    @Test
    public void testSugarcane() {
        // got these using mcp-reborn
        {
            //BlockPos{x=1145, y=122, z=340}
            BPos firstPlacementPos = new BPos(1145, 88, 340);
            CPos sugarcaneChunkPos = firstPlacementPos.toChunkPos();

            ChunkRand rand = new ChunkRand();
            rand.setDecoratorSeed(
                    123456L,
                    sugarcaneChunkPos.getX() << 4,
                    sugarcaneChunkPos.getZ() << 4,
                    SugarCaneFeature.SALT,
                    MCVersion.v1_16_1
            );

            int x = sugarcaneChunkPos.getX() * 16 + rand.nextInt(16);
            int z = sugarcaneChunkPos.getZ() * 16 + rand.nextInt(16);

            assertEquals(x, firstPlacementPos.getX());
            assertEquals(z, firstPlacementPos.getZ());
        }
        {
            //BlockPos{x=8988, y=109, z=8947}
            BPos firstPlacementPos = new BPos(8988, 109, 8947);
            CPos sugarcaneChunkPos = firstPlacementPos.toChunkPos();

            ChunkRand rand = new ChunkRand();
            rand.setDecoratorSeed(
                    123456L,
                    sugarcaneChunkPos.getX() << 4,
                    sugarcaneChunkPos.getZ() << 4,
                    SugarCaneFeature.SALT,
                    MCVersion.v1_16_1
            );

            int x = sugarcaneChunkPos.getX() * 16 + rand.nextInt(16);
            int z = sugarcaneChunkPos.getZ() * 16 + rand.nextInt(16);

            assertEquals(x, firstPlacementPos.getX());
            assertEquals(z, firstPlacementPos.getZ());
        }
    }

    @Test
    public void popseedcheck() {
        BPos pos = new BPos(-29999968, 44, 15287991);
        CPos cpos = pos.toChunkPos();
        long seed = 3344966128489737L;
        System.out.println(new ChunkRand().setPopulationSeed(seed, cpos.getX()<<4, cpos.getZ()<<4, MCVersion.v1_16_1));
    }
}
