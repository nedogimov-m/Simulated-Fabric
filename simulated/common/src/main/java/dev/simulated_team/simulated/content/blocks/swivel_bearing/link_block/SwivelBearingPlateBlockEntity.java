package dev.simulated_team.simulated.content.blocks.swivel_bearing.link_block;

import com.simibubi.create.content.kinetics.base.IRotate;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.api.block.BlockEntitySubLevelActor;
import dev.ryanhcode.sable.api.physics.handle.RigidBodyHandle;
import dev.ryanhcode.sable.api.sublevel.SubLevelContainer;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import dev.ryanhcode.sable.sublevel.SubLevel;
import dev.simulated_team.simulated.content.blocks.swivel_bearing.SwivelBearingBlockEntity;
import dev.simulated_team.simulated.index.SimBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.UUID;

public class SwivelBearingPlateBlockEntity extends KineticBlockEntity implements BlockEntitySubLevelActor {

    private BlockPos parent;
    private UUID parentSubLevelId;
    private boolean assembling;

    public SwivelBearingPlateBlockEntity(final BlockEntityType<?> typeIn, final BlockPos pos, final BlockState state) {
        super(typeIn, pos, state);
    }

    /**
     * Called before we disasemble/assemble the swivel bearing plate
     */
    public void beforeAssembly() {
        this.assembling = true;
    }

    @Override
    public void remove() {
        // if the block was broken / destroyed, destroy our parent
        if (!this.level.isClientSide() && !this.assembling) {
            this.destroyBearing();
        }

        super.remove();
    }

    private void destroyBearing() {
        if (this.parent != null && this.getLevel().getBlockState(this.parent).is(SimBlocks.SWIVEL_BEARING)) {
            this.getLevel().destroyBlock(this.parent, false);
        }
    }

    public void setParent(final SwivelBearingBlockEntity be) {
        final SubLevel subLevel = Sable.HELPER.getContaining(be);

        this.parent = be.getBlockPos();
        this.parentSubLevelId = subLevel != null ? subLevel.getUniqueId() : null;
    }

    @Override
    public void tick() {
        super.tick();
    }

    @Override
    public float propagateRotationTo(final KineticBlockEntity target, final BlockState stateFrom, final BlockState stateTo, final BlockPos diff, final boolean connectedViaAxes, final boolean connectedViaCogs) {
        return this.isOwningBearing(target) ? 1 : super.propagateRotationTo(target, stateFrom, stateTo, diff, connectedViaAxes, connectedViaCogs);
    }

    @Override
    public boolean isCustomConnection(final KineticBlockEntity other, final BlockState state, final BlockState otherState) {
        return this.isOwningBearing(other);
    }

    private boolean isOwningBearing(final KineticBlockEntity target) {
        return this.parent != null && target instanceof SwivelBearingBlockEntity bearing
                && target == this.level.getBlockEntity(this.parent)
                && this.getBlockPos().equals(bearing.getPlatePos());
    }

    @Override
    public List<BlockPos> addPropagationLocations(final IRotate block, final BlockState state, final List<BlockPos> neighbours) {
        if (this.parent != null) {
            neighbours.add(this.parent);
        }

        return super.addPropagationLocations(block, state, neighbours);
    }

    @Override
    protected void write(final net.minecraft.world.level.storage.ValueOutput compound, final boolean clientPacket) {
        super.write(compound, clientPacket);

        if (this.parent != null) {
            compound.store("ParentPos", dev.simulated_team.simulated.api.SimBlockEntityCodecs.BLOCK_POS, this.parent);
        }

        if (this.parentSubLevelId != null) {
            compound.store("ParentSubLevelId", net.minecraft.core.UUIDUtil.CODEC, this.parentSubLevelId);
        }
    }

    @Override
    protected void read(final net.minecraft.world.level.storage.ValueInput compound, final boolean clientPacket) {
        super.read(compound, clientPacket);

        this.parent = compound.read("ParentPos", dev.simulated_team.simulated.api.SimBlockEntityCodecs.BLOCK_POS)
                .or(() -> compound.read("parent", dev.simulated_team.simulated.api.SimBlockEntityCodecs.BLOCK_POS)).orElse(null);
        this.parentSubLevelId = compound.read("ParentSubLevelId", net.minecraft.core.UUIDUtil.CODEC).orElse(null);
    }

    @Override
    public void sable$physicsTick(final ServerSubLevel subLevel, final RigidBodyHandle handle, final double timeStep) {
        if (this.parent != null) {
            final BlockEntity parentBE = this.level.getBlockEntity(this.parent);

            if (parentBE instanceof final SwivelBearingBlockEntity swivelBearingBlockEntity) {
                swivelBearingBlockEntity.updateServoCoefficients();
            }
        }
    }

    @Override
    public @Nullable Iterable<@NotNull SubLevel> sable$getConnectionDependencies() {
        if (this.parent == null) {
            return null;
        }

        final SubLevelContainer container = SubLevelContainer.getContainer(this.level);

        if (this.parentSubLevelId != null) {
            final SubLevel subLevel = container.getSubLevel(this.parentSubLevelId);

            if (subLevel != null) {
                return List.of(subLevel);
            }
        }

        return null;
    }


    public void setParentAssembleNextTick() {
        final BlockEntity be = this.level.getBlockEntity(this.parent);
        if (be instanceof final SwivelBearingBlockEntity sbe) {
            sbe.assembleNextTick = true;
        }
    }

    public void fixParentLinkingWhenMoved() {
        if (this.level.isClientSide() || this.parent == null) {
            return;
        }

        final BlockEntity be = this.level.getBlockEntity(this.parent);

        if (be instanceof final SwivelBearingBlockEntity sbe) {
            sbe.setPlatePos(this.getBlockPos());

            final ServerSubLevel newSublevel = (ServerSubLevel)Sable.HELPER.getContaining(this);
            if (newSublevel != null) {
                final UUID subLevelID = sbe.getSubLevelID();
                final UUID newID = newSublevel.getUniqueId();

                if (newID != subLevelID) {
                    sbe.setSubLevelID(newSublevel.getUniqueId());
                    sbe.reattachConstraint(newSublevel, true);
                }
            } else {
                sbe.setSubLevelID(null);
                sbe.reattachConstraint(null, true);
            }
        }
    }
}
