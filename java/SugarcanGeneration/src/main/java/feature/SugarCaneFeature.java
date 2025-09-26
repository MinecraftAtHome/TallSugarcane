package feature;

import com.seedfinding.mccore.rand.ChunkRand;
import com.seedfinding.mccore.util.data.Pair;
import com.seedfinding.mccore.util.pos.BPos;
import com.seedfinding.mccore.version.MCVersion;
import com.seedfinding.mcterrain.terrain.OverworldTerrainGenerator;
import terrain.ShatteredSavannahSurfaceGenerator;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.LongStream;

public class SugarCaneFeature {
    // salt is for the shattered savannah biome
    private static final int SUGAR_CANE = 80_005;

    //patches per chunk is higher for some biomes
    private static final int PACTHES_PER_CHUNK = 10;
    private static final int TRIES_PER_PATCH = 20;


    //checks sugar cane patches of a chunk checking for good x,z overlap and y heights
    public static int calculatePotentialSugarCaneStackHeight(long structureSeed, int chunkX, int chunkZ, ChunkRand rand) {

        rand.setDecoratorSeed(structureSeed, chunkX << 4, chunkZ << 4, SUGAR_CANE, MCVersion.v1_16_1);

        //either terraingen or gamble on what the height is going to be? can check y values for diff heights
        //gamble time
        int startRootY = 63;

        //this will keep track of where the previous sugar cane root was so we can know the allowed heigths of next y
        //if there's first a y63 then a y66 but then a y65 that y65 will not be kept track of currently (can be added later)
        //todo ^^
        int currentRootY = -1;

        for (int patch = 0; patch < PACTHES_PER_CHUNK; ++patch) {
            int x = chunkX * 16 + rand.nextInt(16);
            int z = chunkZ * 16 + rand.nextInt(16);

            int y = rand.nextInt(startRootY*2);

            int yDiff = y - currentRootY;

            rand.advance(TRIES_PER_PATCH*6);  //advance 6 calls for each sugar cane it tries to gen

            //if((first sugar && is height we predicted) or (not first sugar && is 1 to 4 higher than previous y))
            if((currentRootY == -1 && y == startRootY) || (currentRootY !=-1 && yDiff >= 1 && yDiff <= 4)) {
                //todo ^^
                //check if x and z has overlap with all previous x and z's or something
                //also has to be next to a chunkborder

                currentRootY = y;
                rand.advance(1); //advance 1 call, assume only 1 sugar cane gens
            }

        }

        int possibleStackHeight = Math.max(currentRootY - startRootY + 4, 0);

        return possibleStackHeight;
    }


    public static List<Pair<BPos, Integer>> getAllSugarCanePositionsAndLength(long structureSeed, int chunkX, int chunkZ, ChunkRand rand, OverworldTerrainGenerator terrainGen) {
        ArrayList<Pair<BPos, Integer>> sugarCanePositions = new ArrayList<>();

        rand.setDecoratorSeed(structureSeed, chunkX << 4, chunkZ << 4, SUGAR_CANE, MCVersion.v1_16_1);

        for (int patch = 0; patch < PACTHES_PER_CHUNK; ++patch) {
            int x = chunkX * 16 + rand.nextInt(16);
            int z = chunkZ * 16 + rand.nextInt(16);

            //height might be wrong sometimes maybe cause caves?
            int heightOrigin = Math.max(terrainGen.getHeightOnGround(x, z), 63);
            int y = rand.nextInt(heightOrigin*2);

            BPos patchOrigin = new BPos(x, y, z);
            for (int j = 0; j < TRIES_PER_PATCH; ++j) {
                BPos sugarCanePos = patchOrigin.add(rand.nextInt(5) - rand.nextInt(5), rand.nextInt(1) - rand.nextInt(1), rand.nextInt(5) - rand.nextInt(5));


                int height = terrainGen.getHeightOnGround(sugarCanePos.getX(),sugarCanePos.getZ());

                if(height==patchOrigin.getY()) {

                    //collision check with already placed sugar cane (probably not that needed for waterfall strat)
                    boolean alreadyHasSugarCane = false;
                    for (Pair<BPos, Integer> pair : sugarCanePositions) {
                        if (pair.getFirst().equals(sugarCanePos)) {
                            alreadyHasSugarCane = true;
                            break;
                        }
                    }
                    if (alreadyHasSugarCane) {
                        continue;
                    }

                    if(isAty63AndHasWater(sugarCanePos, terrainGen)) {
                        int length = 2 + rand.nextInt(rand.nextInt(3) + 1);
                        sugarCanePositions.add(new Pair<>(sugarCanePos, length));
                    }

                    //cant place on stone check? (lots of stone in shattered savanna but we can just gamble on it being dirt/grass)

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

    private static final long BLOCK = (long) Math.pow(2, 32);
    public static void main(String[] args) {
        LongStream.range(0L, (long) Math.pow(2, 16)).parallel().forEach(i -> {
            ChunkRand rand = new ChunkRand();
            for (long seed = i*BLOCK; seed < (i+1)*BLOCK; seed++) {
                int possibleHeight = calculatePotentialSugarCaneStackHeight(seed, 0, 0, rand);
                if(possibleHeight>10) {
                    System.out.println(possibleHeight);
                }
            }
        });
    }

    public static void main2(String[] args) {
        //(tested in single biome shattered savanna)
        long seed = 6691L;
        //int chunkX = 18;
        //int chunkZ = 34;

        //int chunkX = 21;
        //int chunkZ = 32;

        // grass cancels this??? idk
        //int chunkX = -107;
        //int chunkZ = 115;

        int chunkX = -134;
        int chunkZ = 206;

        ChunkRand rand = new ChunkRand();

        List<Pair<BPos, Integer>> positions = getAllSugarCanePositionsAndLength(seed, chunkX, chunkZ, rand, new ShatteredSavannahSurfaceGenerator(seed));
        System.out.println(positions);
    }

}
