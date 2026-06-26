package feature;

public class SugarCaneReverser {
    /*
    feature placement:
    W    x=15
    S    x=0

    repeat 10:
        baseX := nextInt(16)
        baseZ := nextInt(16)
        y := nextInt(terrainHeight * 2)

        repeat 20:
            x := baseX + nextInt(5) - nextInt(5)
            skip(2)
            z := baseZ + nextInt(5) - nextInt(5)

            if correct pos:
                height += [2,4]


     Reversal assumptions:
        - first attempt succeeds (for simplicity and this is still the most lenient case)
        - all attempts at different positions fail (actually quite likely, y diff is 0)
        - all of our good attempts happen exatly at (0,Z)
        - we assume that each good attempt succeeds (56% chance per attempt, which is pretty decent still)
        - we keep track of the current min height and current max height. any value in that range counts
          towards the sugarcane stack height.



     uint64_t rand = state predict nextInt(2*h) == h
     advance(&rand, -2);
     int x = nextIntAt(&rand, 16);
     if (x != 0) return;
     advance(&rand, 2); // skip the z & y states
     advance(&rand, 20 * 6 + 1); // skip all displacement and height choice calls

     int minY = h + 2;
     int maxY = h + 4;

     #pragma unroll
     for (int i = 0; i < 9; i++) {
        int x = nextInt(&rand, 16);
        int z = nextInt(&rand, 16);
        int y = nextInt(2*h);
        if (bad position or bad y) {
            advance(&rand, 20 * 6); // all displacement calls but no height choice
        }
        else {
            advance(&rand, 20 * 6 + 1); // all calls
            minY += 2;
            maxY += 4;
        }
     }

     if (minY >= 8) {
        uint32_t idx = atomicAdd(&resultCount1, 1);
        uint64_t state = reconstruct decorator seed
        results1[idx] = state;
     }
     */
}
