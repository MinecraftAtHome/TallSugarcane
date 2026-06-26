package com.seedfinding.mcreversal;

import com.seedfinding.mccore.version.MCVersion;
import com.seedfinding.mcmath.component.vector.QVector;
import com.seedfinding.mcseed.rand.JRand;

import java.util.ArrayList;
import java.util.Random;

// Modified https://github.com/SeedFinding/mc_reversal_java code
/*
The MIT License (MIT)

Copyright (c) 2020 Matthew Bolan, KaptainWutax, Neil

Permission is hereby granted, free of charge, to any person obtaining a copy of this software and
associated documentation files (the "Software"), to deal in the Software without restriction,
including without limitation the rights to use, copy, modify, merge, publish, distribute,
sublicense, and/or sell copies of the Software, and to permit persons to whom the Software
is furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED
TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL
THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF
CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER
DEALINGS IN THE SOFTWARE.
 */

public class TwoChunkCRR {
    private static final long m1 = 25214903917L;
    private static final long m2 = 205749139540585L;
    private static final long addend2 = 277363943098L;
    private static final long m4 = 55986898099985L;
    private static final long addend4 = 49720483695876L;
    private static final int NUM_CHUNKS_IN_WB = 30000000;
    private long k1;
    private long k2;
    private int dx;
    private int dz;
    private final ArrayList<Result> results = new ArrayList<>();
    private final ArrayList<Long> seeds = new ArrayList<>();

    public TwoChunkCRR() {}

    private static long makeMask(int bits) {
        return bits == 64 ? -1L : (1L << bits) - 1L;
    }

    private static long getA(long partialSeed, int bits) {
        long mask = makeMask(bits + 16);
        long mask2 = makeMask(bits);
        return (long)((int)((205749139540585L * ((partialSeed ^ 25214903917L) & mask) + 277363943098L & 281474976710655L) >>> 16) | 1) & mask2;
    }

    private static long getB(long partialSeed, int bits) {
        long mask = makeMask(bits + 16);
        long mask2 = makeMask(bits);
        return (long)((int)((55986898099985L * ((partialSeed ^ 25214903917L) & mask) + 49720483695876L & 281474976710655L) >>> 16) | 1) & mask2;
    }

    private static long getChunkseed13Plus(long seed, int x, int z) {
        Random r = new Random(seed);
        long a = r.nextLong() | 1L;
        long b = r.nextLong() | 1L;
        return ((long)x * a + (long)z * b ^ seed) & 281474976710655L;
    }

    public ArrayList<Result> getWorldseedFromTwoChunkseeds(long chunkseed1, long chunkseed2, int blockDx, int blockDz, MCVersion version) {
        this.results.clear();
        this.dx = blockDx;
        this.dz = blockDz;
        this.k1 = chunkseed1;
        this.k2 = chunkseed2;
        if (version.isOlderThan(MCVersion.v1_12)) {
            throw new UnsupportedOperationException("Cannot do multichunk on versions older than 1.13");
        } else {
            for(long c = this.k1 & 15L; c < 65536L; c += 16L) {
                this.growSolution(c, 16);
            }

            for(long seed : this.seeds) {
                this.results.addAll(findSeedsInWB(chunkseed1, seed));
            }

            return this.results;
        }
    }

    private void growSolution(long c, int bitsOfSeedKnown) {
        if (bitsOfSeedKnown == 48) {
            if (((this.k2 ^ c) - (this.k1 ^ c) & makeMask(48)) == ((getChunkseed13Plus(c, this.dx, this.dz) ^ c) & makeMask(48))) {
                this.seeds.add(c);
            }

        } else {
            int bitsOfVectorKnown = bitsOfSeedKnown - 16;
            if (((this.k2 ^ c) - (this.k1 ^ c) & makeMask(bitsOfVectorKnown + 1)) == ((long)this.dx * getA(c, bitsOfVectorKnown + 1) + (long)this.dz * getB(c, bitsOfVectorKnown + 1) & makeMask(bitsOfVectorKnown + 1))) {
                this.growSolution(c, bitsOfSeedKnown + 1);
            }

            c += 1L << bitsOfSeedKnown;
            if (((this.k2 ^ c) - (this.k1 ^ c) & makeMask(bitsOfVectorKnown + 1)) == ((long)this.dx * getA(c, bitsOfVectorKnown + 1) + (long)this.dz * getB(c, bitsOfVectorKnown + 1) & makeMask(bitsOfVectorKnown + 1))) {
                this.growSolution(c, bitsOfSeedKnown + 1);
            }

        }
    }

    private static ArrayList<Result> findSeedsInWB(long target, long seed) {
        ArrayList<Result> validPositions = new ArrayList<>();
        long goal = (target ^ seed) & makeMask(48);
        JRand r = new JRand(seed);
        Lattice2D lattice = new Lattice2D(r.nextLong() | 1L, r.nextLong() | 1L, 281474976710656L);

        for(QVector v : lattice.findSolutionsInBox(goal, -30000000L, -30000000L, 30000000L, 30000000L)) {
            int x = v.get(0).intValue();
            int z = v.get(1).intValue();
            if (x % 16 == 0 && z % 16 == 0) {
                validPositions.add(new Result(seed, x, z));
            }
        }

        return validPositions;
    }

    public record Result (long structureSeed, int blockX, int blockZ) {}
}
