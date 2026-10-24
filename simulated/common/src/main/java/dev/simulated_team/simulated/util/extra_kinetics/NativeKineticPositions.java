package dev.simulated_team.simulated.util.extra_kinetics;

import it.unimi.dsi.fastutil.Hash;
import net.minecraft.core.BlockPos;
import java.io.Serializable;
import java.util.Objects;

/** Graph identity only: vanilla positions retain their original coordinates/equality everywhere else. */
public final class NativeKineticPositions {
    public static final Hash.Strategy<Object> STRATEGY = new Strategy();
    private NativeKineticPositions() {}
    public static boolean same(BlockPos left,Object right){
        return right instanceof BlockPos position && left.getX()==position.getX() && left.getY()==position.getY()
                && left.getZ()==position.getZ() && (left instanceof ExtraBlockPos)==(position instanceof ExtraBlockPos);
    }
    public static int hash(BlockPos position){return 31*position.hashCode()+(position instanceof ExtraBlockPos?1:0);}
    private static final class Strategy implements Hash.Strategy<Object>, Serializable {
        @Override public int hashCode(Object value){return value instanceof BlockPos position?hash(position):Objects.hashCode(value);}
        @Override public boolean equals(Object left,Object right){return left instanceof BlockPos position?same(position,right):Objects.equals(left,right);}
    }
}
