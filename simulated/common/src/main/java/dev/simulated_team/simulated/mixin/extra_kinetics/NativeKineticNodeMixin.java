package dev.simulated_team.simulated.mixin.extra_kinetics;

import dev.simulated_team.simulated.util.extra_kinetics.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import org.spongepowered.asm.mixin.*;
import java.util.Objects;

/** The actual native solver record distinguishes physical and secondary channels without moving either owner. */
@Mixin(targets="com.simibubi.create.content.kinetics.KineticNetworks$Node",remap=false)
public abstract class NativeKineticNodeMixin implements NativeKineticNodeView {
    @Shadow @Final private BlockPos pos;
    @Shadow @Final private Direction port;
    @Override public BlockPos simulated$position(){return pos;}
    @Override public Direction simulated$port(){return port;}
    /** @author Simulated @reason Native same-cell kinetic channels have distinct graph identity. */
    @Overwrite public final boolean equals(Object other){
        return other instanceof NativeKineticNodeView node && NativeKineticPositions.same(pos,node.simulated$position()) && port==node.simulated$port();
    }
    /** @author Simulated @reason Match the exact channel-aware graph-node equality. */
    @Overwrite public final int hashCode(){return 31*NativeKineticPositions.hash(pos)+Objects.hashCode(port);}
}
