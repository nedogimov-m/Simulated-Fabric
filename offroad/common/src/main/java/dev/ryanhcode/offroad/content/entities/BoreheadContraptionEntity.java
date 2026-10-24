package dev.ryanhcode.offroad.content.entities;

import com.simibubi.create.AllContraptionTypes;
import com.simibubi.create.api.behaviour.movement.MovementBehaviour;
import com.simibubi.create.content.contraptions.Contraption;
import com.simibubi.create.content.contraptions.ControlledContraptionEntity;
import com.simibubi.create.content.contraptions.IControlContraption;
import com.simibubi.create.content.contraptions.StructureTransform;
import com.simibubi.create.content.contraptions.actors.contraptionControls.ContraptionControlsMovement;
import com.simibubi.create.content.contraptions.behaviour.MovementContext;
import dev.ryanhcode.offroad.content.blocks.borehead_bearing.BoreheadBearingBlockEntity;
import dev.ryanhcode.offroad.content.blocks.rock_cutting_wheel.RockCuttingWheelActor;
import dev.ryanhcode.offroad.content.contraptions.borehead_contraption.BoreheadBearingContraption;
import dev.ryanhcode.offroad.index.OffroadContraptionTypes;
import dev.ryanhcode.offroad.index.OffroadEntityTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.phys.Vec3;

import static net.createmod.catnip.math.AngleHelper.angleLerp;

public class BoreheadContraptionEntity extends ControlledContraptionEntity {

    public BoreheadContraptionEntity(final EntityType<? extends BoreheadContraptionEntity> type, final Level world) {
        super(type, world);
    }

    public static BoreheadContraptionEntity create(final Level world, final IControlContraption controller,
                                                   final Contraption contraption) {
        final BoreheadContraptionEntity entity = new BoreheadContraptionEntity(OffroadEntityTypes.BOREHEAD_CONTRAPTION_ENTITY, world);
        entity.setControllerPos(controller.getBlockPosition());
        entity.setContraption(contraption);
        return entity;
    }



    @Override
    protected boolean isActorActive(final MovementContext context, final MovementBehaviour actor) {
        if (!(actor instanceof RockCuttingWheelActor || actor instanceof ContraptionControlsMovement)) {
            context.disabled = true;
            return false;
        }

        return super.isActorActive(context, actor);
    }

    public void setControllerPos(final BlockPos controllerPos) {
        super.setControllerPos(controllerPos);
    }

    public BlockPos getControllerPos() {
        return this.controllerPos;
    }

    public void setContraption(final Contraption contraption) {
        super.setContraption(contraption);
    }

    @Override
    public float getAngle(final float partialTicks) {
        final IControlContraption controller = this.controllerPos != null && this.level().isLoaded(this.controllerPos)
                && this.level().getBlockEntity(this.controllerPos) instanceof IControlContraption found ? found : null;

        // make sure to respect the slowdown controller
        if (controller instanceof final BoreheadBearingBlockEntity be) {
            if (be.isSlowingDown()) {
                return be.getInterpolatedAngle(partialTicks - 1);
            }
        }

        return partialTicks == 1.0F ? this.angle : angleLerp(partialTicks, this.prevAngle, this.angle);
    }

    public float getAngleDelta() {
        return net.minecraft.util.Mth.wrapDegrees(this.angle - this.prevAngle);
    }

    @Override
    protected void readAdditionalSaveData(net.minecraft.world.level.storage.ValueInput input) {
        final var original = input.read("Contraption", CompoundTag.CODEC).orElse(null);
        super.readAdditionalSaveData(input);
        // Old saves used Create's bearing ID. Rebuild through the genuine registered addon factory.
        if (original != null && this.contraption != null && this.contraption.getType() == AllContraptionTypes.BEARING.value()) {
            final var migrated = original.copy();
            migrated.putString("Type", OffroadContraptionTypes.BOREHEAD_CONTRAPTION_TYPE.getId().toString());
            this.setContraption(Contraption.fromNBT(this.level(), migrated, false));
        }
    }

    //make sure no matter what we disassembly in the same position we started
    @Override
    protected StructureTransform disassemblyTransform() {
        final StructureTransform transform = super.disassemblyTransform();

        transform.angle = 0;
        transform.rotation = Rotation.NONE;
        transform.mirror = Mirror.NONE;
        transform.rotationAxis = Direction.Axis.X;

        return transform;
    }

    public BoreheadBearingContraption getContraption() {
        return ((BoreheadBearingContraption) this.contraption);
    }
}
