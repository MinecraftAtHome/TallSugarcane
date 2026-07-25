package settings;

public class SearchParameters {
    /*
    8
    Pos{x=0, y=45, z=5}
    103619595606068
     */

    public static final int DIRT_Y = 45;
    public static final int DIRT_INDENT = 1;
    public static final int SUGARCANE_ROOT_Y = DIRT_Y - DIRT_INDENT;

    // offset from waterfall chunk to sugarcane chunk
    public static final int CHUNK_DX = 0;
    public static final int CHUNK_DZ = -1;
    public static final int BLOCK_DX = CHUNK_DX * 16;
    public static final int BLOCK_DZ = CHUNK_DZ * 16;

    public static final long SUGARCANE_POPSEED = 206691561575024L;

    public static final int SUGARCANE_RELATIVE_X = 6;
    public static final int SUGARCANE_RELATIVE_Z = 15;
    public static final int WATERCOL_RELATIVE_X = (SUGARCANE_RELATIVE_X - CHUNK_DX) & 15;
    public static final int WATERCOL_RELATIVE_Z = (SUGARCANE_RELATIVE_Z - CHUNK_DZ) & 15;

    public static final int WATERFALL_MIN_Y = 52;
    public static final int WATERFALL_MAX_Y = 58;
}
