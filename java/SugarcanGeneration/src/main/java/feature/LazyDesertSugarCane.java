package feature;

import com.seedfinding.mccore.rand.ChunkRand;
import com.seedfinding.mccore.util.pos.BPos;
import com.seedfinding.mccore.version.MCVersion;
import com.seedfinding.mcseed.lcg.LCG;

import static settings.SearchParameters.*;

public class LazyDesertSugarCane {
    private static final int N_PATCHES = 60;
    private static final int N_TRIES = 20;
    private static final int SPREAD = 4;

    private final ChunkRand rand = new ChunkRand();
    private final LCG forward2 = LCG.JAVA.combine(2);
    private final LCG forward120 = LCG.JAVA.combine(120);


    public int getStackHeightAt(long seed, BPos rootPos) {
        int cx = rootPos.getX() >> 4, cz = rootPos.getZ() >> 4;
        rand.setDecoratorSeed(seed, cx<<4, cz<<4, SugarCaneFeature.SALT, MCVersion.v1_16_1);

        int currentYTarget = rootPos.getY();
        int currentHeight = 0;

        for (int patch = 0; patch < N_PATCHES; patch++) {
            int centerX = rand.nextInt(16) + (cx << 4);
            int centerZ = rand.nextInt(16) + (cz << 4);
            int y = rand.nextInt(rootPos.getY() * 2);

            //if (y != currentYTarget || Math.abs(centerX - rootPos.getX()) > SPREAD || Math.abs(centerZ - rootPos.getZ()) > SPREAD) {
            if (y != currentYTarget || centerX != rootPos.getX() || centerZ != rootPos.getZ()) {
                rand.advance(forward120);
                continue;
            }

            for (int attempt = 0; attempt < N_TRIES; attempt++) {
                int x = centerX + rand.nextInt(SPREAD + 1) - rand.nextInt(SPREAD + 1);
                rand.advance(forward2); // 2 x nextInt(1) for the y offset
                int z = centerZ + rand.nextInt(SPREAD + 1) - rand.nextInt(SPREAD + 1);

                if (x == rootPos.getX() && y == currentYTarget && z == rootPos.getZ()) {
                    // ColumnPlacer(2,2)
                    int h = 2 + rand.nextInt(rand.nextInt(2 + 1) + 1);
                    currentYTarget += h;
                    currentHeight += h;
                }
            }
        }

        return currentHeight;
    }

    public static void main(String[] args) {
        var cane = new LazyDesertSugarCane();
        ///setblock -29999968 44 15287991 minecraft:dirt
        //x=15, y=45, z=7

        cane.getStackHeightAt(SUGARCANE_POPSEED, new BPos(SUGARCANE_RELATIVE_X, SUGARCANE_ROOT_Y, SUGARCANE_RELATIVE_Z));
    }
}
