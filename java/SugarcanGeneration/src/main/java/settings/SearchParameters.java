package settings;

public class SearchParameters {
    /*
    15
    Pos{x=15, y=45, z=10}
    185093308538807

    14
    Pos{x=0, y=43, z=13}
    29846539632599
     */

    public static final int DIRT_Y = 43;
    public static final int DIRT_INDENT = 0;
    public static final int SUGARCANE_ROOT_Y = DIRT_Y - DIRT_INDENT;

    // offset from waterfall chunk to sugarcane chunk
    public static final int CHUNK_DX = 1;
    public static final int CHUNK_DZ = 0;
    public static final int BLOCK_DX = CHUNK_DX * 16;
    public static final int BLOCK_DZ = CHUNK_DZ * 16;

    public static final long SUGARCANE_POPSEED = 29846539632599L;

    public static final int SUGARCANE_RELATIVE_X = 0;
    public static final int SUGARCANE_RELATIVE_Z = 13;
    public static final int WATERCOL_RELATIVE_X = (SUGARCANE_RELATIVE_X - CHUNK_DX) & 15;
    public static final int WATERCOL_RELATIVE_Z = (SUGARCANE_RELATIVE_Z - CHUNK_DZ) & 15;

    public static final int WATERFALL_MIN_Y = 52;
    public static final int WATERFALL_MAX_Y = 58;
}
