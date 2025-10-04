package feature;

import com.seedfinding.mccore.rand.ChunkRand;
import com.seedfinding.mccore.util.block.BlockBox;
import com.seedfinding.mccore.util.data.Pair;
import com.seedfinding.mccore.util.math.DistanceMetric;
import com.seedfinding.mccore.util.pos.BPos;
import com.seedfinding.mccore.version.MCVersion;
import com.seedfinding.mcseed.rand.JRand;
import com.seedfinding.mcterrain.terrain.OverworldTerrainGenerator;
import com.seedfinding.mcterrain.terrain.SurfaceGenerator;
import terrain.ShatteredSavannahSurfaceGenerator;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.LongStream;

public class SugarCaneFeature {
    // salt is for the shattered savannah biome
    private static final int SUGAR_CANE = 80_005;

    //patches per chunk is higher for some biomes
    private static final int PATCHES_PER_CHUNK = 10;
    private static final int TRIES_PER_PATCH = 20;
    private static final int PATCH_SPREAD = 4;

    public static void findSugarCaneStack(long structureSeed, int chunkX, int chunkZ, int minHeight, ChunkRand rand) {

        int startRootY = 64; // we assume terrain height is at startRootY-1 and first sugar cane gens at startRootY

        // patchPosition = (patch index in the chunk, center of its 9x9 area)
        ArrayList<Pair<Integer, BPos>> patchPositions = calculatePotentialSugarCanePacthes(structureSeed, chunkX, chunkZ, startRootY, rand);
        if(patchPositions.isEmpty()) {
            return;
        }

        int maxPatchHeight = patchPositions.get(patchPositions.size()-1).getSecond().getY()+4 - startRootY;
        if(maxPatchHeight<minHeight) {
            return;
        }

        BlockBox overlapBox = findOverlapBox(patchPositions);
        ArrayList<BPos> sugarCaneStacks = new ArrayList<>();
        for (int x = overlapBox.minX; x <= overlapBox.maxX; x++) {
            for (int z = overlapBox.minZ; z <= overlapBox.maxZ; z++) {
                if((x==0 || z==0 || x==15 || z==15) && x>=0 && z>=0 && x<=15 && z<=15) {
                    sugarCaneStacks.add(new BPos(x, startRootY, z));
                }
            }
        }

        rand.setDecoratorSeed(structureSeed, chunkX << 4, chunkZ << 4, SUGAR_CANE, MCVersion.v1_16_1);

        int patchesProcessed = 0;
        for (Pair<Integer, BPos> patchPosition : patchPositions) {
            //advance calls for all previous skipped patches
            rand.advance((3 + TRIES_PER_PATCH * 6) * (patchPosition.getFirst() - patchesProcessed));

            //advance 3 calls for patch x, z, y
            rand.advance(3);

            for (int j = 0; j < TRIES_PER_PATCH; ++j) {
                BPos sugarCane = patchPosition.getSecond().add(
                        rand.nextInt(PATCH_SPREAD + 1) - rand.nextInt(PATCH_SPREAD + 1),
                        rand.nextInt(1) - rand.nextInt(1),
                        rand.nextInt(PATCH_SPREAD + 1) - rand.nextInt(PATCH_SPREAD + 1)
                );

                for (int k = 0; k < sugarCaneStacks.size(); k++) {
                    BPos sugarCaneStack = sugarCaneStacks.get(k);
                    if (sugarCaneStack.equals(sugarCane)) {
                        JRand patchRand = rand.copy();

                        int length = 2 + patchRand.nextInt(patchRand.nextInt(3) + 1);
                        sugarCaneStacks.set(k, sugarCaneStack.add(0, length, 0));
                    }
                }
            }
            rand.advance(2); // 2 calls advance
            patchesProcessed += patchPosition.getFirst() + 1;
        }


        for (BPos sugarCaneStack : sugarCaneStacks) {
            if(sugarCaneStack.getY()-startRootY >= minHeight) {
                SurfaceGenerator sgen = new ShatteredSavannahSurfaceGenerator(structureSeed);
                if(checkTerrainForSugarCaneStack(sugarCaneStack, patchPositions, startRootY, sgen)) {
                    System.out.println(structureSeed);
                    System.out.println(sugarCaneStack);
                    System.out.println(patchPositions);
                    System.out.println();
                }
            }
        }
    }

    private static boolean checkTerrainForSugarCaneStack(BPos sugarCaneStack, ArrayList<Pair<Integer, BPos>> patchPositions, int startRootY, SurfaceGenerator sgen) {
        for (Pair<Integer, BPos> patchPosition : patchPositions) {
            if (sgen.getHeightOnGround(patchPosition.getSecond().getX(), patchPosition.getSecond().getZ()) != startRootY) {
                return false;
            }
        }
        return sgen.getHeightOnGround(sugarCaneStack.getX(), sugarCaneStack.getZ()) == startRootY;
    }

    private static BlockBox findOverlapBox(ArrayList<Pair<Integer, BPos>> patchPositions) {
        BPos first = patchPositions.get(0).getSecond();
        int minX = first.getX() - 4;
        int maxX = first.getX() + 4;
        int minZ = first.getZ() - 4;
        int maxZ = first.getZ() + 4;

        for (int i = 1; i < patchPositions.size(); i++) {
            BPos p = patchPositions.get(i).getSecond();
            minX = Math.max(minX, p.getX() - 4);
            maxX = Math.min(maxX, p.getX() + 4);
            minZ = Math.max(minZ, p.getZ() - 4);
            maxZ = Math.min(maxZ, p.getZ() + 4);
        }

        return new BlockBox(minX, 0, minZ, maxX, 256, maxZ);
    }

    //checks sugar cane patches of a chunk checking for good x,z overlap and y heights
    public static ArrayList<Pair<Integer, BPos>> calculatePotentialSugarCanePacthes(long structureSeed, int chunkX, int chunkZ, int startRootY, ChunkRand rand) {

        ArrayList<Pair<Integer, BPos>> patchPositions = new ArrayList<>();

        rand.setDecoratorSeed(structureSeed, chunkX << 4, chunkZ << 4, SUGAR_CANE, MCVersion.v1_16_1);

        //if there's first a y63 then a y66 but then a y65 that y65 will not be kept track of currently (can be added later)
        //todo ^^

        for (int patch = 0; patch <= PATCHES_PER_CHUNK-1; ++patch) {
            int x = chunkX * 16 + rand.nextInt(16);
            int z = chunkZ * 16 + rand.nextInt(16);

            int y = rand.nextInt(startRootY*2);

            rand.advance(TRIES_PER_PATCH*6);  //advance 6 calls for each sugar cane it tries to gen

            //if((first sugar && is height we predicted) or (not first sugar && is 1 to 4 higher than previous y)) (xd)
            if((patchPositions.isEmpty() && y == startRootY) || (!patchPositions.isEmpty() && y - patchPositions.get(patchPositions.size() - 1).getSecond().getY() >= 1 && y - patchPositions.get(patchPositions.size() - 1).getSecond().getY() <= 4)) {
                //check if patch touches chunkborder
                if(x-PATCH_SPREAD <= 0 || x+PATCH_SPREAD >= 15 || z-PATCH_SPREAD <= 0 || z+PATCH_SPREAD >= 15) {

                    BPos newPos = new BPos(x, y, z);
                    if (patchPositions.isEmpty() || checkPositionsOverlapAtChunkBorder(newPos, patchPositions)) {
                        patchPositions.add(new Pair<>(patch, newPos));
                        rand.advance(2); //assume only 1 sugar cane gens
                    }
                }
            }
        }

        return patchPositions;
    }

    private static boolean checkPositionsOverlapAtChunkBorder(BPos newPos, ArrayList<Pair<Integer, BPos>> patchPositions) {
        for (Pair<Integer, BPos> pair : patchPositions) {
            if (!checkPositionsOverlapAtChunkBorder(newPos, pair.getSecond())) {
                return false;
            }
        }
        return true;
    }

    private static boolean checkPositionsOverlapAtChunkBorder(BPos newPos, BPos oldPos) {
        if(newPos.distanceTo(oldPos, DistanceMetric.CHEBYSHEV)>8) {
            return false;
        }
        if(oldPos.getX()-PATCH_SPREAD <= 0 && newPos.getX()-PATCH_SPREAD <= 0) {
            return true;
        }
        if(oldPos.getZ()-PATCH_SPREAD <= 0 && newPos.getZ()-PATCH_SPREAD <= 0) {
            return true;
        }
        if(oldPos.getX()+PATCH_SPREAD >= 15 && newPos.getX()+PATCH_SPREAD >= 15) {
            return true;
        }
        if(oldPos.getZ()+PATCH_SPREAD >= 15 && newPos.getZ()+PATCH_SPREAD >= 15) {
            return true;
        }
        return false;
    }

    public static List<Pair<BPos, Integer>> getAllSugarCanePositionsAndLength(long structureSeed, int chunkX, int chunkZ, ChunkRand rand, OverworldTerrainGenerator terrainGen) {
        ArrayList<Pair<BPos, Integer>> sugarCanePositions = new ArrayList<>();

        rand.setDecoratorSeed(structureSeed, chunkX << 4, chunkZ << 4, SUGAR_CANE, MCVersion.v1_16_1);

        for (int patch = 0; patch < PATCHES_PER_CHUNK; ++patch) {
            int x = chunkX * 16 + rand.nextInt(16);
            int z = chunkZ * 16 + rand.nextInt(16);

            //height might be wrong sometimes maybe cause caves?
            int heightOrigin = Math.max(terrainGen.getHeightOnGround(x, z), 63);
            int y = rand.nextInt(heightOrigin*2);

            BPos patchOrigin = new BPos(x, y, z);
            for (int j = 0; j < TRIES_PER_PATCH; ++j) {
                BPos sugarCanePos = patchOrigin.add(rand.nextInt(PATCH_SPREAD+1) - rand.nextInt(PATCH_SPREAD+1), rand.nextInt(1) - rand.nextInt(1), rand.nextInt(PATCH_SPREAD+1) - rand.nextInt(PATCH_SPREAD+1));


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

    private static final long BLOCK = (long) Math.pow(2, 16);

    public static void main(String[] args) {
        LongStream.range(0L, (long) Math.pow(2, 16)).parallel().forEach(i -> {
            ChunkRand rand = new ChunkRand();
            for (long seed = i*BLOCK; seed < (i+1)*BLOCK; seed++) {
                findSugarCaneStack(seed, 0, 0, 8, rand);
            }
        });
        //findSugarCaneStack(3147251894L, 0, 0, 8, new ChunkRand());
        //findSugarCaneStack(3948690855L, 0, 0, 8, new ChunkRand());
    }

}
