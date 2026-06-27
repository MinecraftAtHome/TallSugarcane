/*
MIT License

Copyright (c) 2026 Kris

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
*/

#ifndef SEEDFINDING_CUH
#define SEEDFINDING_CUH

#include "cuda_runtime.h"
#include "device_launch_parameters.h"

#include <cinttypes>
#include <cstdint>
#include <chrono>
#include <cstdio>

// ------------------------------------------------------------------------------
// General-purpose utilities

__host__ static void gpuAssert(cudaError_t err, const char *file, int line) {
    if (err != cudaSuccess) {
        std::fprintf(stderr, "CUDA error %s:%d — %s\n", file, line, cudaGetErrorString(err));
        std::exit(EXIT_FAILURE);
    }
}
#define CUDA_CHECK(ans) gpuAssert((ans), __FILE__, __LINE__)


static constexpr uint64_t LCG_JAVA_MULTIPLIER = 0x5deece66d;
static constexpr uint32_t LCG_JAVA_ADDEND = 11;
static constexpr uint64_t REGION_SEED_A = 341873128712ULL;
static constexpr uint64_t REGION_SEED_B = 132897987541ULL;

static constexpr uint64_t MASK(uint8_t bits) {
    return (UINT64_C(1) << bits) - 1;
}
static constexpr uint64_t MASK_48 = MASK(48);
static constexpr uint32_t MASK_32 = static_cast<uint32_t>(MASK(32));
static constexpr uint32_t MASK_16 = static_cast<uint32_t>(MASK(16));


struct Timer {
    double full_work_size;
    double current_percent;
    double percent_per_update;
    std::chrono::time_point<std::chrono::steady_clock> time_start;

    __host__ inline Timer(double full_work_size, double percent_per_update = 1.0) {
        this->full_work_size = full_work_size;
        this->percent_per_update = percent_per_update;
        this->current_percent = 0.0;
    }

    __host__ inline void start() {
        time_start = std::chrono::steady_clock::now();
        this->current_percent = 0.0;
    }

    __host__ inline void update_completion(double new_work_done) {
        std::chrono::time_point<std::chrono::steady_clock> time_current = std::chrono::steady_clock::now();
        double new_percent = std::round(new_work_done / full_work_size * 100.0 / percent_per_update) * percent_per_update;

        if (new_percent != current_percent) {
            current_percent = new_percent;
            double seconds_for_current = (time_current - time_start).count() * 1e-9;
            double seconds_for_full = seconds_for_current * (full_work_size / new_work_done);
            double seconds_left = seconds_for_full - seconds_for_current;
            std::fprintf(stderr, "----- %f %% done, ETA: %f seconds\n", current_percent, seconds_left);
        }
    }
};

// --------------------------------------------------------------------------------
// The below implementations of Java Random and Xoroshiro128++
// Are direct ports of Cubitect's rng.h file in the Cubiomes library, available under MIT.
// Link to the repo (valid as of March 2026): https://github.com/Cubitect/cubiomes
// File (valid as of March 2026): https://github.com/Cubitect/cubiomes/blob/master/rng.h
/*
MIT License

Copyright (c) 2020 Cubitect

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
*/ 

// Xoroshiro -------------

struct Xoroshiro {
    uint64_t lo, hi;
};

__host__ __device__ static inline void xSetSeed(Xoroshiro *xr, uint64_t value) {
    const uint64_t XL = 0x9e3779b97f4a7c15ULL;
    const uint64_t XH = 0x6a09e667f3bcc909ULL;
    const uint64_t A = 0xbf58476d1ce4e5b9ULL;
    const uint64_t B = 0x94d049bb133111ebULL;
    uint64_t l = value ^ XH;
    uint64_t h = l + XL;
    l = (l ^ (l >> 30)) * A;
    h = (h ^ (h >> 30)) * A;
    l = (l ^ (l >> 27)) * B;
    h = (h ^ (h >> 27)) * B;
    l = l ^ (l >> 31);
    h = h ^ (h >> 31);
    xr->lo = l;
    xr->hi = h;
}

__host__ __device__ static inline uint64_t rotl64(uint64_t x, uint8_t b) {
    return (x << b) | (x >> (64-b));
}

__host__ __device__ static inline uint64_t xNextLong(Xoroshiro *xr) {
    uint64_t l = xr->lo;
    uint64_t h = xr->hi;
    uint64_t n = rotl64(l + h, 17) + l;
    h ^= l;
    xr->lo = rotl64(l, 49) ^ h ^ (h << 21);
    xr->hi = rotl64(h, 28);
    return n;
}

__host__ __device__ static inline int xNextInt(Xoroshiro *xr, uint32_t n) {
    uint64_t r = (xNextLong(xr) & 0xFFFFFFFF) * n;
    if ((uint32_t)r < n)
    {
        while ((uint32_t)r < (~n + 1) % n)
        {
            r = (xNextLong(xr) & 0xFFFFFFFF) * n;
        }
    }
    return r >> 32;
}

__host__ __device__ static inline double xNextDouble(Xoroshiro *xr) {
    return (xNextLong(xr) >> (64-53)) * 1.1102230246251565E-16;
}

__host__ __device__ static inline float xNextFloat(Xoroshiro *xr) {
    return (xNextLong(xr) >> (64-24)) * 5.9604645E-8F;
}

__host__ __device__ static inline void xSkipN(Xoroshiro *xr, int count) {
    while (count --> 0)
        xNextLong(xr);
}

__host__ __device__ static inline uint64_t xNextLongJ(Xoroshiro *xr) {
    int32_t a = xNextLong(xr) >> 32;
    int32_t b = xNextLong(xr) >> 32;
    return ((uint64_t)a << 32) + b;
}

__host__ __device__ static inline int xNextIntJ(Xoroshiro *xr, uint32_t n) {
    int bits, val;
    const int m = n - 1;

    if ((m & n) == 0) {
        uint64_t x = n * (xNextLong(xr) >> 33);
        return (int) ((int64_t) x >> 31);
    }

    do {
        bits = (xNextLong(xr) >> 33);
        val = bits % n;
    }
    while ((int32_t)((uint32_t)bits - val + m) < 0);
    return val;
}

// Java Random ---------

__host__ __device__ static inline void setSeed(uint64_t *seed, uint64_t value) {
    *seed = (value ^ 0x5deece66d) & ((1ULL << 48) - 1);
}

__host__ __device__ static inline int next(uint64_t *seed, const int bits) {
    *seed = (*seed * 0x5deece66d + 0xb) & ((1ULL << 48) - 1);
    return (int) ((int64_t)*seed >> (48 - bits));
}

__host__ __device__ static inline int nextInt(uint64_t *seed, const int n) {
    int bits, val;
    const int m = n - 1;

    if ((m & n) == 0) {
        uint64_t x = n * (uint64_t)next(seed, 31);
        return (int) ((int64_t) x >> 31);
    }

    do {
        bits = next(seed, 31);
        val = bits % n;
    }
    while ((int32_t)((uint32_t)bits - val + m) < 0);
    return val;
}

__host__ __device__ static inline uint64_t nextLong(uint64_t *seed) {
    return ((uint64_t) next(seed, 32) << 32) + next(seed, 32);
}

__host__ __device__ static inline float nextFloat(uint64_t *seed) {
    return next(seed, 24) / (float) (1 << 24);
}

__host__ __device__ static inline double nextDouble(uint64_t *seed) {
    uint64_t x = (uint64_t)next(seed, 26);
    x <<= 27;
    x += next(seed, 27);
    return (int64_t) x / (double) (1ULL << 53);
}


// End of Cubiomes library code
// --------------------------------------------------------------------------------

// Additional Java Random utilities

__host__ __device__ static inline int nextIntFast(uint64_t *seed, const int n) {
    if (((n - 1) & n) == 0) {
        uint64_t x = n * static_cast<uint64_t>(next(seed, 31));
        return static_cast<int>((static_cast<int64_t>(x) >> 31));
    }

    return next(seed, 31) % n;
}

__host__ __device__ static inline int nextIntBoundedFast(uint64_t *seed, const int min, const int max) {
    if (min == max) {
        return min;
    }
    return min + nextIntFast(seed, max - min + 1);
}

template <int n>
__host__ __device__ static inline int nextIntFastTemplate(uint64_t *seed) {
    if constexpr (((n - 1) & n) == 0) {
        uint64_t x = n * static_cast<uint64_t>(next(seed, 31));
        return static_cast<int>((static_cast<int64_t>(x) >> 31));
    }

    return next(seed, 31) % n;
}

template <int min, int max>
__host__ __device__ static inline int nextIntBoundedFastTemplate(uint64_t *seed) {
    if (min == max) {
        return min;
    }
    return min + nextIntFastTemplate<max - min + 1>(seed);
}

// Java Random N-state skip template

constexpr uint64_t COMBINED_RANDOM(uint64_t skip_value, bool multiplier) {
    skip_value &= MASK_48;

    uint64_t im = LCG_JAVA_MULTIPLIER;
    uint64_t ia = LCG_JAVA_ADDEND;
    uint64_t mult = 1;
    uint64_t add = 0;

    while (skip_value) {
        if (skip_value & 1) {
            add = (im * add + ia) & MASK_48;
            mult = (mult * im) & MASK_48;
        }
        
        ia = (im * ia + ia) & MASK_48;
        im = (im * im) & MASK_48;

        skip_value >>= 1;
    }

    // scuffed but works
    return multiplier ? mult : add;
}

template <int64_t SKIP>
__host__ __device__ static inline uint64_t javaLcgSkip(uint64_t rand) {
	constexpr uint64_t uskip = static_cast<uint64_t>(SKIP);
    return (rand * COMBINED_RANDOM(uskip, true) + COMBINED_RANDOM(uskip, false)) & MASK_48;
}

// --------------------------------------------------------------------------------
// The below code for Java Random nextLong() reversal is a direct port of Matthew Bolan's
// algorithm, published by Neil & KaptainWutax in their mc_core_java library, and licensed under MIT.
// Link to the repo (valid as of March 2026): https://github.com/SeedFinding/mc_core_java
// File (valid as of March 2026): https://github.com/SeedFinding/mc_core_java/blob/main/src/main/java/com/seedfinding/mccore/util/math/NextLongReverser.java
/*
The MIT License (MIT)

Copyright (c) 2020 KaptainWutax, Neil

Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
*/

__host__ __device__ static inline int64_t floorDiv(int64_t x, int64_t y) {
	const int64_t q = x / y;
	if ((x ^ y) < 0 && (q * y != x)) {
		return q - 1;
	}
	return q;
}

__host__ __device__ static inline void reverseNextLong(uint64_t nextLongLower48, uint64_t* seedList, int* seedCount) {
    *seedCount = 0;

	int64_t lowerBits = nextLongLower48 & 0xffff'ffffULL;
	int64_t upperBits = nextLongLower48 >> 32;
	//Did the lower bits affect the upper bits
	if ((lowerBits & 0x80000000LL) != 0)
		upperBits += 1; //restoring the initial value of the upper bits

	//TODO I can only guarantee the algorithm's correctness for bitsOfDanger = 0 but believe 1 should still always work, needs to be confirmed!!!

	//The algorithm is meant to have bitsOfDanger = 0, but this runs into overflow issues.
	//By using a different small value, we introduce small numerical error which probably cannot break things
	//while keeping everything in range of a long and avoiding nasty BigDecimal/BigInteger overhead
	int bitsOfDanger = 1;

	int64_t lowMin = lowerBits << 16 - bitsOfDanger;
	int64_t lowMax = ((lowerBits + 1) << 16 - bitsOfDanger) - 1;
	int64_t upperMin = ((upperBits << 16) - 107048004364969LL) >> bitsOfDanger;

	//hardcoded matrix multiplication again
	int64_t m1lv = floorDiv(lowMax * -33441LL + upperMin * 17549LL, 1LL << 31 - bitsOfDanger) + 1; //I cancelled out a common factor of 2 in this line
	int64_t m2lv = floorDiv(lowMin * 46603LL + upperMin * 39761LL, 1LL << 32 - bitsOfDanger) + 1;

	int64_t seed;

	// (0,0) -> 0.6003265380859375
	seed = (-39761LL * m1lv + 35098LL * m2lv);
	if ((46603LL * m1lv + 66882LL * m2lv) + 107048004364969LL >> 16 == upperBits) {
		if (((uint64_t)seed >> 16) == lowerBits)
			seedList[(*seedCount)++] = ((254681119335897ULL * (uint64_t)seed + 120305458776662ULL) & MASK_48); //pull back 2 LCG calls
	}
	//(1,0) -> 0.282440185546875
	seed = (-39761LL * (m1lv + 1) + 35098LL * m2lv);
	if ((46603LL * (m1lv + 1) + 66882LL * m2lv) + 107048004364969LL >> 16 == upperBits) {
		if (((uint64_t)seed >> 16) == lowerBits)
			seedList[(*seedCount)++] = ((254681119335897ULL * (uint64_t)seed + 120305458776662ULL) & MASK_48); //pull back 2 LCG calls
	}
	//(0,1) -> 0.1172332763671875
	seed = (-39761LL * m1lv + 35098LL * (m2lv + 1));
	if ((46603LL * m1lv + 66882LL * (m2lv + 1)) + 107048004364969LL >> 16 == upperBits) {
		if (((uint64_t)seed >> 16) == lowerBits)
			seedList[(*seedCount)++] = ((254681119335897ULL * (uint64_t)seed + 120305458776662ULL) & MASK_48); //pull back 2 LCG calls
	}
	//(1,1) -> 0.0
}

__host__ __device__ static inline void getNextLongEquivalents(uint64_t nextLongLower48, uint64_t* seedList, int* seedCount) {
    reverseNextLong(nextLongLower48, seedList, seedCount);
    for (int i = 0; i < *seedCount; i++) {
        uint64_t nl = nextLong(&(seedList[i]));
        seedList[i] = nl;
    }
}

// End of mc_core_java library code
// --------------------------------------------------------------------------------

// Minecraft-specific seeding utilities (Xoroshiro)

__host__ __device__ static inline uint64_t xGetDecorationSeed(Xoroshiro* xrand, uint64_t worldseed, int x, int z) {
    xSetSeed(xrand, worldseed);
    uint64_t a = xNextLongJ(xrand) | 1;
    uint64_t b = xNextLongJ(xrand) | 1;
    return a*x + b*z ^ worldseed;
}

__host__ __device__ static inline void xSetDecorationSeed(Xoroshiro* xrand, uint64_t worldseed, int x, int z) {
    xSetSeed(xrand, xGetDecorationSeed(xrand, worldseed, x, z));
}

__host__ __device__ static inline void xSetFeatureSeed(Xoroshiro* xrand, uint64_t worldseed, int x, int z, int salt) {
    xSetSeed(xrand, xGetDecorationSeed(xrand, worldseed, x, z) + salt);
}

__host__ __device__ static inline void xSetFeatureSeed(Xoroshiro* xrand, uint64_t decorationSeed, int salt) {
    xSetSeed(xrand, decorationSeed + salt);
}

// Minecraft-specific seeding utilities (Java Random)

__host__ __device__ static inline uint64_t getDecorationSeed(uint64_t* rand, uint64_t structureSeed, int x, int z) {
    setSeed(rand, structureSeed);
    uint64_t a = nextLong(rand) | 1;
    uint64_t b = nextLong(rand) | 1;
    return (a*x + b*z ^ structureSeed) & MASK_48;
}

__host__ __device__ static inline void setDecorationSeed(uint64_t* rand, uint64_t structureSeed, int x, int z) {
    setSeed(rand, getDecorationSeed(rand, structureSeed, x, z));
}

__host__ __device__ static inline void setFeatureSeed(uint64_t* rand, uint64_t structureSeed, int x, int z, int salt) {
    setSeed(rand, getDecorationSeed(rand, structureSeed, x, z) + salt);
}

__host__ __device__ static inline void setFeatureSeed(uint64_t* rand, uint64_t decorationSeed, int salt) {
    setSeed(rand, decorationSeed + salt);
}

__host__ __device__ static inline void setCarverSeed(uint64_t* rand, uint64_t structureSeed, int chunkX, int chunkZ) {
    setSeed(rand, structureSeed);
    uint64_t a = nextLong(rand);
    uint64_t b = nextLong(rand);
    setSeed(rand, a*chunkX ^ b*chunkZ ^ structureSeed);
}

__host__ __device__ static inline void setRegionSeed(uint64_t* rand, uint64_t structureSeed, int regionX, int regionZ, int structureSalt) {
    setSeed(rand, structureSeed + structureSalt + regionX * REGION_SEED_A + regionZ * REGION_SEED_B);
}

#endif // SEEDFINDING_CUH
