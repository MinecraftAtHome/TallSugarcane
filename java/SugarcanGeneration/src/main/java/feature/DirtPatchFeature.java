package feature;

import com.seedfinding.mccore.rand.ChunkRand;
import com.seedfinding.mccore.util.pos.BPos;
import com.seedfinding.mccore.version.MCVersion;

public class DirtPatchFeature {
    private static final int SALT = 60_000;


    private final ChunkRand rand;

    public DirtPatchFeature() {
        this.rand = new ChunkRand();
    }

    public BPos getFirstPos(long structureSeed, int chunkX, int chunkZ) {
        rand.setDecoratorSeed(structureSeed, chunkX << 4, chunkZ << 4, SALT, MCVersion.v1_16_1);
        return getPos(chunkX, chunkZ);
    }

    public BPos getFirstPosFromPopulationSeed(long populationSeed, int chunkX, int chunkZ) {
        rand.setSeed(populationSeed + SALT);
        return getPos(chunkX, chunkZ);
    }

    private BPos getPos(int chunkX, int chunkZ) {
        int x = chunkX * 16 + rand.nextInt(16);
        int z = chunkZ * 16 + rand.nextInt(16);

        // bottomOffset = topOffset = 0, maximum = 256
        int y = rand.nextInt(256);

        return new BPos(x, y, z);
    }

    public int getSalt() {
        return SALT;
    }
}
