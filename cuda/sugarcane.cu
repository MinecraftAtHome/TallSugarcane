#include <cstdint>
#include <cstdio>
#include "seedfinding.cuh"

//constexpr uint32_t H = 72;
constexpr uint32_t MIN_HEIGHT = 6;
constexpr uint32_t TARGET_HEIGHT = 12;


constexpr uint32_t MAX_RESULTS_1 = 1024 * 1024;
__device__ uint64_t results1[MAX_RESULTS_1];
__managed__ uint32_t resultCount1 = 0;

constexpr uint32_t MAX_RESULTS_2 = 1024;
__managed__ uint64_t results2[MAX_RESULTS_2];
__managed__ uint32_t heights2[MAX_RESULTS_2];
__managed__ uint32_t resultCount2 = 0;


__global__ void kernel1(uint64_t offset) {
	const uint64_t tid = blockIdx.x * blockDim.x + threadIdx.x + offset;
	const uint64_t upper31 = tid >> 17;
	const uint32_t lower17 = tid & 0x1ffff;
	uint64_t rand = ((upper31 * (2*H) + H) << 17) | lower17; // state predict nextInt(2*h) == h
	rand = javaLcgSkip<-2>(rand);
	int base_x = static_cast<int>(rand >> 44);
	if (base_x != 0) return;
	rand = javaLcgSkip<1>(rand);
	int base_z = static_cast<int>(rand >> 44);
	
	// skip the y state = +1
	// skip all displacement and height choice calls = +121
	rand = javaLcgSkip<122>(rand);

	int minY = H + 2;
	int maxY = H + 4;

	#pragma unroll
	for (int i = 0; i < 9; i++) {
		int x = nextIntFastTemplate<16>(&rand);
		int z = nextIntFastTemplate<16>(&rand);
		int y = nextIntFastTemplate<2*H>(&rand);
		if (x != 0 || z != base_z || y < minY || y > maxY) {
			rand = javaLcgSkip<120>(rand); // all calls without the height roll
		}
		else {
			rand = javaLcgSkip<121>(rand); // all calls
			minY += 2;
			maxY += 4;
		}
		
		if (minY + 2*(8-i) < H + MIN_HEIGHT) {
			return;
		}
	}

	if (minY >= H + MIN_HEIGHT) {
		uint32_t idx = atomicAdd(&resultCount1, 1);
		if (idx >= MAX_RESULTS_1) {
			printf("TOO MANY RESULTS from 1\n");
			return;
		}
		uint64_t state = ((upper31 * (2*H) + H) << 17) | lower17;
		state = javaLcgSkip<-3>(state);
		results1[idx] = state;
	}
}


__global__ void kernel2() {
	uint32_t tid = blockIdx.x * blockDim.x + threadIdx.x;
	if (tid >= resultCount1) {
		return;
	}
	
	uint64_t chunkseedInternal = results1[tid];
	uint64_t rand = chunkseedInternal;
	
	int currentY = H;
	int base_z;
	
	for (int i = 0; i < 10; i++) {
		int x = nextIntFastTemplate<16>(&rand);
		int z = nextIntFastTemplate<16>(&rand);
		if (i == 0) {
			base_z = z;
		}
		
		int y = nextIntFastTemplate<2*H>(&rand);
		if (x != 0 || z != base_z || y != currentY) {
			rand = javaLcgSkip<120>(rand); // all calls without the height roll
		}
		else {
			// actually process this now
			
			for (int j = 0; j < 20; j++) {
				int dx1 = nextIntFastTemplate<5>(&rand);
				int dx2 = nextIntFastTemplate<5>(&rand);
				rand = javaLcgSkip<2>(rand);
				int dz1 = nextIntFastTemplate<5>(&rand);
				int dz2 = nextIntFastTemplate<5>(&rand);
				
				if (dx1 == dx2 && dz1 == dz2) {
					// not displacing the sugarcane
					currentY += 2 + nextIntFastTemplate<3>(&rand);
					
					// consume remaining calls
					j++;
					while (j < 20) {
						rand = javaLcgSkip<6>(rand);
						j++;
					}
					break;
				}					
			}
		}
	}
	
	if (currentY >= H + TARGET_HEIGHT) {
		uint32_t idx = atomicAdd(&resultCount2, 1);
		if (idx >= MAX_RESULTS_2) {
			printf("TOO MANY RESULTS from 2\n");
			return;
		}
		uint64_t state = chunkseedInternal ^ LCG_JAVA_MULTIPLIER;
		results2[idx] = state;
		heights2[idx] = currentY - H;
	}
}


int main() {
	CUDA_CHECK(cudaSetDevice(0));
	
	const uint64_t TOTAL_WORK = ((1ULL << 48) + 2*H - 1) / (2*H);
	const uint64_t WORK_PER_BATCH = 1ULL << 32;
	const uint32_t BATCHES = (TOTAL_WORK + WORK_PER_BATCH - 1) / WORK_PER_BATCH;
	const uint32_t N_THREADS = 256;
	const uint32_t N_BLOCKS = WORK_PER_BATCH / N_THREADS;
	
	Timer t = Timer(BATCHES, 1.0);
	t.start();
	for (uint32_t b = 0; b < BATCHES; b++) {
		resultCount1 = 0;
		kernel1 <<< N_BLOCKS, N_THREADS >>> (WORK_PER_BATCH * b);
		CUDA_CHECK(cudaDeviceSynchronize());
		
		if (resultCount1 > 0) {
			resultCount2 = 0;
			uint32_t N_BLOCKS_2 = (resultCount1 + N_THREADS - 1) / N_THREADS;
			kernel2 <<< N_BLOCKS_2, N_THREADS >>> ();
			CUDA_CHECK(cudaDeviceSynchronize());
		}
		
		for (uint32_t i = 0; i < resultCount2; i++) {
			std::printf("y=%d h=%u %llu\n", H, heights2[i], results2[i]);
		}
		
		t.update_completion(b + 1);
	}
	
	return 0;
}