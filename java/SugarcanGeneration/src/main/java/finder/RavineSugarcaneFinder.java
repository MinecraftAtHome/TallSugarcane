package finder;

import com.seedfinding.mccore.rand.ChunkRand;
import com.seedfinding.mccore.util.pos.BPos;
import com.seedfinding.mcfeature.structure.Stronghold;
import com.seedfinding.mcseed.lcg.LCG;
import feature.DirtPatchFeature;
import feature.SugarCaneFeature;
import terrain.CubiomesCanyonGenerator;

import java.util.Comparator;
import java.util.stream.Collectors;

public class RavineSugarcaneFinder {
    private static final int DIRT_MIN_Y = 40;
    private static final int DIRT_MAX_Y = 50;
    private static final int INDENT = 2;


    public static void main(String[] args) {
        DirtPatchFeature dirt = new DirtPatchFeature();
        ChunkRand rand = new ChunkRand();
        LCG back1 = LCG.JAVA.combine(-1);

        // state-predict for ravine at 0,0
        long spMax = (1L << 48) / 50;

        seedLoop:
        for (long iseed = 0; iseed < spMax; iseed++) {
            rand.setSeed(iseed, false);
            int ravineX = rand.nextInt(16);
            int ravineY = rand.nextInt(rand.nextInt(40) + 8) + 20;
            int ravineZ = rand.nextInt(16);
            float angle = (float)Math.PI * 2F * rand.nextFloat();
            if (ravineY < 40) {
                continue;
            }

            long internalCarverSeed = back1.nextSeed(iseed);
            long structureSeed = (internalCarverSeed ^ LCG.JAVA.multiplier) - 1;

            BPos dirtPos = dirt.getFirstPos(structureSeed, 0, 0);
            if (dirtPos.getY() < DIRT_MIN_Y || dirtPos.getY() > DIRT_MAX_Y || !atChunkBorder(dirtPos)) {
                continue;
            }

            // is ravine direction vector close to the ravine-dirt vector?
            if (!isRavineFacingDirt(dirtPos, new BPos(ravineX, ravineY, ravineZ), angle)) {
                continue;
            }

            BPos sugarcaneStack = SugarCaneFeature.findSugarCaneStack(
                    structureSeed, 0, 0,
                    8, dirtPos.getY() - INDENT, dirtPos,
                    rand
            );

            if (sugarcaneStack == null) {
                continue;
            }
            //System.out.println("Sugarcane good");

            var sugarcaneColumnAir = CubiomesCanyonGenerator.getCanyonCarvedAir(structureSeed, 0, 0)
                    .stream().filter(pos -> pos.getX() == dirtPos.getX() && pos.getZ() == dirtPos.getZ())
                    .collect(Collectors.toUnmodifiableSet());

            if (!sugarcaneColumnAir.contains(dirtPos.add(0, -INDENT, 0)) || sugarcaneColumnAir.contains(dirtPos.add(0, -INDENT - 1, 0))) {
                System.out.println(sugarcaneColumnAir.contains(dirtPos.add(0, -INDENT, 0)) + " - " + sugarcaneColumnAir.contains(dirtPos.add(0, -INDENT - 1, 0)));
                continue;
            }
            System.out.println("Almost good sugarcane stack & ravine air: " + structureSeed);

            int dx = dirtPos.getX();
            int dz = dirtPos.getZ();
            for (int y = dirtPos.getY() - INDENT; y < 64; y++) {
                if (!sugarcaneColumnAir.contains(new BPos(dx, y, dz))) {
                    System.out.println("failed at y = " + y + ", dirtPosY = " + dirtPos.getY());
                    continue seedLoop;
                }
            }

            System.out.println("Good sugarcane stack & ravine air!");
        }
    }

    private static boolean isRavineFacingDirt(BPos dirtPos, BPos ravinePos, float ravineAngle) {
        BPos a = dirtPos.subtract(ravinePos);
        float v1x = a.getX();
        float v1z = a.getZ();
        float v2x = (float)Math.cos(ravineAngle);
        float v2z = (float)Math.sin(ravineAngle);

        float dot = v1x * v2x + v1z * v2z;
        float lenProd = veclen(v1x, v1z) * veclen(v2x, v2z);
        float angle = (float)Math.acos(dot / lenProd);
        //System.out.println(angle);
        return angle < 0.5F;
    }

    private static float veclen(float v1x, float v1z) {
        return (float)Math.sqrt(v1x * v1x + v1z * v1z);
    }

    private static boolean atChunkBorder(BPos pos) {
        int rx = pos.getX() & 15;
        int rz = pos.getZ() & 15;
        return rx == 0 || rz == 0 || rx == 15 || rz == 15;
    }
}
