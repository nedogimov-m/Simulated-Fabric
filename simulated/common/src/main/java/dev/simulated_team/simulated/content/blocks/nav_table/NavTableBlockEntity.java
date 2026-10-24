package dev.simulated_team.simulated.content.blocks.nav_table;

import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.sublevel.SubLevel;
import dev.simulated_team.simulated.content.blocks.nav_table.navigation_target.NavigationTarget;
import dev.simulated_team.simulated.data.advancements.SimAdvancements;
import dev.simulated_team.simulated.util.SimMathUtils;
import net.createmod.catnip.animation.LerpedFloat;
import net.createmod.catnip.data.Iterate;
import net.createmod.catnip.math.AngleHelper;
import net.createmod.catnip.math.VecHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.Vec3i;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.Clearable;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaterniond;
import org.joml.Quaternionf;
import org.jetbrains.annotations.Nullable;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public class NavTableBlockEntity extends SmartBlockEntity implements Clearable {

    public SubLevel subLevel;
    public NavTableInventory inventory;

    /**
     * Current target the navTable is pointing towards. Defined By current held item. <br>
     */
    @Nullable
    public Vec3 currentTarget;

    public boolean isPowering;

    private float relativeAngle;
    public final LerpedFloat lerpedAngleDegrees;

    private final Map<Direction, Integer> signalStrengthCache = new EnumMap<>(Direction.class);

    private int ticks = 0;
    private double distanceToTarget;
    private double lastDistanceToTarget;

    public NavTableBlockEntity(final BlockEntityType<?> type, final BlockPos pos, final BlockState state) {
        super(type, pos, state);
        this.inventory = new NavTableInventory(this);

        this.currentTarget = null;

        this.relativeAngle = 0;
        this.lerpedAngleDegrees = LerpedFloat.angular();
    }

    @Override
    public void preRemoveSideEffects(final BlockPos pos, final BlockState state) {
        if (this.level != null && !this.level.isClientSide()) this.dropHeldItem();
        super.preRemoveSideEffects(pos, state);
    }

    @Override
    public void addBehaviours(final List<BlockEntityBehaviour> behaviours) {

    }

    @Override
    public void tick() {
        super.tick();

        if (this.level.isClientSide()) {
            this.lerpedAngleDegrees.tickChaser();
        }

        if (this.level == null || this.isVirtual()) {
            return;
        }

        this.subLevel = Sable.HELPER.getContaining(this);

        // Update the Nav-Table's current target
        if (!this.level.isClientSide()) {
            this.updateTarget();
            this.updateCurrentAngle();

            if (this.getTargetPosition(false) != null) {
                final double dist = this.getProjectedSelfPos().distanceTo(this.getTargetPosition(true));
                if (dist >= 5000) {
                    SimAdvancements.FAR_FROM_HOME.awardToNearby(this.getBlockPos(), this.level, 40, 10);
                }

                this.sendData();
            }
        }

        // Check if Nav table should update surrounding blocks
        if (this.selectivelyUpdateNeighbors()) {
            this.notifyUpdate();
            this.level.updateNeighborsAt(this.worldPosition, this.getBlockState().getBlock());
        }

        final NavigationTarget navigationTarget = this.getNavTableItem();
        if (navigationTarget != null) {
            if (this.ticks > 10) {
                this.ticks = 0;
                this.lastDistanceToTarget = this.distanceToTarget;
                this.distanceToTarget = navigationTarget.distanceToTarget(this);
            }

            this.ticks++;
        }
    }

    /**
     * Update current target position
     */
    private void updateTarget() {
        final NavigationTarget nti = this.getNavTableItem();
        if (nti != null) {
            this.currentTarget = nti.getTarget(this, this.getHeldItem());
        } else {
            this.currentTarget = null;
        }

        this.notifyUpdate();
    }

    /**
     * Get the current angle of this Navigation Table client side.
     *
     * @return the lerped angle
     */
    public float getClientTargetAngle(final float partialTicks) {
        if (this.level.isClientSide()) {
            return -AngleHelper.rad(this.lerpedAngleDegrees.getValue(partialTicks));
        } else {
            return 0;
        }
    }

    /**
     * Primarily used in ponders, force sets the rendering angle to the given angle.
     *
     * @param angle the angle to force set the renderer too.
     */
    public void forceCurrentAngle(final float angle) {
        this.lerpedAngleDegrees.chase(angle, 0.8f, LerpedFloat.Chaser.EXP);
    }

    /**
     * Updates the current angle of this Navigation Table on the server.
     */
    private void updateCurrentAngle() {
        if (this.level.isClientSide() || this.getTargetPosition(false) == null) {
            this.relativeAngle = 0;
            return;
        }

        final Vec3 originPos = this.getProjectedSelfPos();

        final Vec3 difference = this.getTargetPosition(true).subtract(originPos);
        Vec3 directionToTarget = difference.normalize();

        // Gather rotations
        final Quaterniond sublevelRot = this.getSublevelRot();
        final Quaternionf rotation = this.getBlockState().getValue(NavTableBlock.FACING).getRotation();

        // Rotate vector
        directionToTarget = SimMathUtils.rotateQuat(directionToTarget, sublevelRot);
        directionToTarget = SimMathUtils.rotateQuat(directionToTarget, rotation);

        //  Zero y
        directionToTarget = new Vec3(directionToTarget.x, 0, directionToTarget.z);
        this.relativeAngle = (360f + AngleHelper.deg((float) Math.atan2(directionToTarget.z, directionToTarget.x))) % 360f;
    }

    /**
     * Gets the associated redstone strength with the given Direction.
     *
     * @param direction The direction to get the redstone from
     * @return the redstone strength associated with the direction.
     */
    //TODO: make this map-ified
    public int getRedstoneStrength(final Direction direction) {
        // ponder rendering logic where only the visual arrow direction is cared about
        if (this.level.isClientSide() && this.isVirtual()) {
            final Direction facing = this.getBlockState().getValue(NavTableBlock.FACING);
            final Vec3i normal = facing.getUnitVec3i();

            final double andleRad = Math.toRadians(this.lerpedAngleDegrees.getValue());
            Vec3 targetPos = new Vec3(Math.cos(andleRad), 0, Math.sin(andleRad));
            targetPos = NavigationTarget.getPlaneProjectedPos(targetPos, normal);

            final double dot = -targetPos.dot(Vec3.atLowerCornerOf(direction.getUnitVec3i()));
            return (int) (Math.asin(dot) / Math.PI * 30 + 0.5);
        }

        int power = 0;
        final NavigationTarget nti = this.getNavTableItem();
        if (nti != null && this.getTargetPosition(false) != null) {
            power = nti.getRedstoneStrength(this, direction, this.getHeldItem());
        }

        return power;
    }

    /**
     * @return The projected Nav-Table position
     */
    public Vec3 getProjectedSelfPos() {
        Vec3 pos = Vec3.atCenterOf(this.worldPosition);
        if (this.subLevel != null) {
            pos = this.subLevel.logicalPose().transformPosition(pos);
        }

        return pos;
    }

    /**
     * Gets the target position of the currently held item stack in this Navigation Table. <b/>
     * Projects off of any sublevels if applicable.
     *
     * @param project whether the returned target position should be projected
     * @return tha target position
     */
    @Nullable
    public Vec3 getTargetPosition(final boolean project) {
        if (this.currentTarget == null) {
            return null;
        }

        return project ? Sable.HELPER.projectOutOfSubLevel(this.getLevel(), this.currentTarget) : this.currentTarget;
    }

    /**
     * @return Quaternion of the sub-level that the Nav-Table is currently on
     */
    public Quaterniond getSublevelRot() {
        Quaterniond rot = new Quaterniond();
        if (this.subLevel != null) {
            rot = this.subLevel.logicalPose().orientation();
        }

        return rot;
    }

    @Override
    protected void write(final net.minecraft.world.level.storage.ValueOutput tag, final boolean clientPacket) {
        tag.store("CurrentStack", ItemStack.OPTIONAL_CODEC, this.getHeldItem());

        if (this.currentTarget != null) {
            this.writeCurrentTarget(tag);
        }

        tag.putFloat("RelativeAngle", this.relativeAngle);

        super.write(tag, clientPacket);
    }

    @Override
    protected void read(final net.minecraft.world.level.storage.ValueInput tag, final boolean clientPacket) {
        final ItemStack stack = tag.read("CurrentStack", ItemStack.OPTIONAL_CODEC).orElse(ItemStack.EMPTY);
        this.inventory.slot.setStack(stack);

        if (tag.read("CurrentTarget", Vec3.CODEC).isPresent()) {
            this.currentTarget = this.readCurrentTarget(tag);
            this.isPowering = true;
        } else {
            this.currentTarget = null;
            this.isPowering = false;
        }

        this.relativeAngle = tag.getFloatOr("RelativeAngle", 0f);
        if (clientPacket) {
            this.lerpedAngleDegrees.chase(this.relativeAngle, 0.8f, LerpedFloat.Chaser.EXP);
        }

        super.read(tag, clientPacket);
    }

    private void writeCurrentTarget(final net.minecraft.world.level.storage.ValueOutput tag) {
        tag.store("CurrentTarget", Vec3.CODEC, this.currentTarget);
    }

    private Vec3 readCurrentTarget(final net.minecraft.world.level.storage.ValueInput tag) {
        return tag.read("CurrentTarget", Vec3.CODEC).orElse(Vec3.ZERO);
    }

    private boolean selectivelyUpdateNeighbors() {
        if (this.level == null || this.level.isClientSide()) {
            return false;
        }

        final BlockState state = this.getBlockState();
        boolean notifyUpdate = false;
        for (final Direction direction : Iterate.directions) {
            if (direction.getAxis() == state.getValue(NavTableBlock.FACING).getAxis()) {
                continue;
            }

            final int oldStrength = this.signalStrengthCache.computeIfAbsent(direction, d -> -1);
            final int curStrength = this.getRedstoneStrength(direction);

            if (oldStrength == curStrength) {
                continue;
            }

            this.signalStrengthCache.put(direction, curStrength);
            this.level.updateNeighborsAt(this.worldPosition.relative(direction), state.getBlock());
            notifyUpdate = true;
        }

        return notifyUpdate;
    }

    public NavigationTarget getNavTableItem() {
        return NavigationTarget.ofStack(this.getHeldItem());
    }

    public ItemStack getHeldItem() {
        return this.inventory.getItem(0);
    }

    public ItemStack setHeldItem(final ItemStack newStack) {
        final ItemStack oldStack = this.getHeldItem();
        this.inventory.setItem(0, newStack);
        return oldStack;
    }

    public void dropHeldItem() {
        ItemStack heldItem = this.getHeldItem();
        if (heldItem.isEmpty()) {
            return;
        }

        NavigationTarget target = this.getNavTableItem();
        if (target != null) {
            target.onExtract(heldItem, this, null);
        }
        final ItemEntity itementity = new ItemEntity(this.level,
                this.worldPosition.getX() + 0.5, this.worldPosition.getY() + 0.5, this.worldPosition.getZ() + 0.5, heldItem);
        itementity.setDefaultPickUpDelay();
        this.level.addFreshEntity(itementity);
        this.inventory.clearContent();
    }

    public double distanceToTarget() {
        return this.distanceToTarget;
    }

    public double lastDistanceToTarget() {
        return this.lastDistanceToTarget;
    }

    @Override
    public void clearContent() {
        this.inventory.clearContent();
    }

    public float getRelativeAngle() {
        return this.relativeAngle;
    }
}
