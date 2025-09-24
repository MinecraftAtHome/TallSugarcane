package feature;

import com.seedfinding.mccore.rand.ChunkRand;
import com.seedfinding.mccore.util.data.Pair;
import com.seedfinding.mccore.util.pos.BPos;
import com.seedfinding.mccore.version.MCVersion;
import com.seedfinding.mcterrain.terrain.OverworldTerrainGenerator;
import terrain.ShatteredSavannahSurfaceGenerator;

import java.util.ArrayList;
import java.util.List;

public class SugarCaneFeature {
    // salt is for the shattered savannah biome
    private static final int SUGAR_CANE = 80_005;

    //patches per chunk is higher for some biomes
    private static final int PACTHES_PER_CHUNK = 10;
    private static final int TRIES_PER_PATCH = 20;


    public static List<Pair<BPos, Integer>> getAllSugarCanePositionsAndLength(long structureSeed, int chunkX, int chunkZ, ChunkRand rand, OverworldTerrainGenerator terrainGen) {
        ArrayList<Pair<BPos, Integer>> sugarCanePositions = new ArrayList<>();

        rand.setDecoratorSeed(structureSeed, chunkX << 4, chunkZ << 4, SUGAR_CANE, MCVersion.v1_16_1);

        for (int patch = 0; patch < PACTHES_PER_CHUNK; ++patch) {
            int x = chunkX * 16 + rand.nextInt(16);
            int z = chunkZ * 16 + rand.nextInt(16);

            //height might be wrong sometimes maybe cause caves?
            int heightOrigin = Math.max(terrainGen.getHeightOnGround(x, z), 63);
            int y = rand.nextInt(heightOrigin*2);

            System.out.println(x + " " + y + " " + z);

            BPos patchOrigin = new BPos(x, y, z);
            for (int j = 0; j < TRIES_PER_PATCH; ++j) {
                BPos sugarCanePos = patchOrigin.add(rand.nextInt(5) - rand.nextInt(5), rand.nextInt(1) - rand.nextInt(1), rand.nextInt(5) - rand.nextInt(5));


                int height = terrainGen.getHeightOnGround(sugarCanePos.getX(),sugarCanePos.getZ());

                if(height==patchOrigin.getY()) {

                    //code to test, needs other code here for waterfall
                    if(isAty63AndHasWater(sugarCanePos, terrainGen)) {
                        int length = 2 + rand.nextInt(rand.nextInt(3) + 1);
                        sugarCanePositions.add(new Pair<>(sugarCanePos, length));
                    }

                    //needs collision check with already placed sugar cane?

                    //cant place on stone check?

                }

            }
        }

        return sugarCanePositions;
    }

    //this is for sugarcane that grows at y63, to test the code out for now, wont need this for our waterfall
    public static boolean isAty63AndHasWater(BPos sugarCanePos, OverworldTerrainGenerator terrainGen) {
        if(sugarCanePos.getY()!=63) {
            return false;
        }

        if(terrainGen.getHeightOnGround(sugarCanePos.getX()+1,sugarCanePos.getZ())<63) {
            return true;
        }
        if(terrainGen.getHeightOnGround(sugarCanePos.getX(),sugarCanePos.getZ()+1)<63) {
            return true;
        }
        if(terrainGen.getHeightOnGround(sugarCanePos.getX()-1,sugarCanePos.getZ())<63) {
            return true;
        }
        if(terrainGen.getHeightOnGround(sugarCanePos.getX(),sugarCanePos.getZ()-1)<63) {
            return true;
        }
        return false;
    }

    public static void main(String[] args) {
        long seed = 6691L;
        int chunkX = 18;
        int chunkZ = 34;

        //int chunkX = 21;
        //int chunkZ = 32;

        //int chunkX = -44;
        //int chunkZ = 21;

        ChunkRand rand = new ChunkRand();

        List<Pair<BPos, Integer>> positions = getAllSugarCanePositionsAndLength(seed, chunkX, chunkZ, rand, new ShatteredSavannahSurfaceGenerator(seed));
        System.out.println(positions);
    }

}
