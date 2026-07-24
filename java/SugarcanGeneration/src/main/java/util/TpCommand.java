package util;

import com.seedfinding.mccore.util.pos.BPos;
import com.seedfinding.mccore.util.pos.CPos;

public class TpCommand {
    public static String tpCommand(BPos pos) {
        return String.format("/tp %d %d %d", pos.getX(), pos.getY(), pos.getZ());
    }

    public static String tpCommand(CPos pos) {
        return String.format("/tp %d %d %d", pos.getX() * 16, 64, pos.getZ() * 16);
    }
}
