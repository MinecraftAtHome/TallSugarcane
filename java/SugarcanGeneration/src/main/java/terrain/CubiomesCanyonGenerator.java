package terrain;

import com.seedfinding.mccore.rand.ChunkRand;
import com.seedfinding.mccore.util.pos.BPos;
import com.seedfinding.mccore.version.MCVersion;
import dev.xpple.cubiomes.*;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.util.ArrayList;
import java.util.List;


public class CubiomesCanyonGenerator {
    private static ChunkRand rand = new ChunkRand();

    public static List<BPos> getCanyonCarvedAir(long structureSeed, int chunkX, int chunkZ) {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment pos3ListPointer = arena.allocate(Pos3List.layout().byteSize()).reinterpret(Pos3List.layout().byteSize());
            MemorySegment cccPointer = arena.allocate(CanyonCarverConfig.layout().byteSize());
            MemorySegment biomesArray = arena.allocate(Cubiomes.C_INT.byteSize() * 17 * 17);

            Cubiomes.createPos3List(pos3ListPointer, 256);
            Cubiomes.getCanyonCarverConfig(Cubiomes.CANYON_CARVER(), Cubiomes.MC_1_16_1(), cccPointer);

            Cubiomes.carveCanyon(
                    structureSeed,
                    Cubiomes.MC_1_16_1(),
                    chunkX, chunkZ,
                    cccPointer,
                    Cubiomes.CANYON_CARVER(),
                    biomesArray, pos3ListPointer
            );

            ArrayList<BPos> airPoses = new ArrayList<>();

            int posListSize = Pos3List.size(pos3ListPointer);
            MemorySegment innerList = Pos3List.pos3s(pos3ListPointer);
            for (int i = 0; i < posListSize; i++) {
                MemorySegment pos = Pos3.asSlice(innerList, i);
                int x = Pos3.x(pos);
                int y = Pos3.y(pos);
                int z = Pos3.z(pos);
                BPos airPos = new BPos(x, y, z);
                airPoses.add(airPos);
            }

            Cubiomes.freePos3List(pos3ListPointer);
            return airPoses;
        }
        catch (Exception ex) {
            System.err.println("Caught exception: " + ex.getMessage());
        }

        return List.of();
    }

    public static boolean startsAt(long seed, int chunkX, int chunkZ) {
        rand.setCarverSeed(seed + 1, chunkX, chunkZ, MCVersion.v1_16_1);
        return rand.nextFloat() < 0.02F;
    }

    static {
        CubiomesInit.load();
    }
}
