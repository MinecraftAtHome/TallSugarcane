import com.seedfinding.mccore.util.pos.BPos;
import com.seedfinding.mccore.util.pos.CPos;
import feature.DirtPatchFeature;
import org.junit.jupiter.api.Test;
import terrain.CubiomesCanyonGenerator;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;


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
    public void testCanyonStart() {
        assertTrue(CubiomesCanyonGenerator.startsAt(123456, -23, -14));
        assertFalse(CubiomesCanyonGenerator.startsAt(123456, -22, -14));
        assertFalse(CubiomesCanyonGenerator.startsAt(123456, -23, -15));
    }
}
