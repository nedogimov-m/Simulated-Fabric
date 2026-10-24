package dev.simulated_team.simulated.content.entities.honey_glue;

import com.simibubi.create.api.schematic.requirement.SpecialEntityItemRequirement;
import com.simibubi.create.content.schematics.requirement.ItemRequirement;
import dev.simulated_team.simulated.Simulated;
import dev.simulated_team.simulated.data.SimLang;
import dev.simulated_team.simulated.index.SimEntityTypes;
import dev.simulated_team.simulated.index.SimItems;
import dev.simulated_team.simulated.network.packets.honey_glue.HoneyGlueSyncBoundsPacket;
import foundry.veil.api.network.VeilPacketManager;
import net.createmod.catnip.data.Iterate;
import net.createmod.catnip.math.VecHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;
import org.joml.Vector3fc;

public class HoneyGlueEntity extends Entity implements SpecialEntityItemRequirement {
    private static final EntityDataAccessor<Vector3fc> BOUNDS_FROM = SynchedEntityData.defineId(HoneyGlueEntity.class, EntityDataSerializers.VECTOR3);
    private static final EntityDataAccessor<Vector3fc> BOUNDS_TO = SynchedEntityData.defineId(HoneyGlueEntity.class, EntityDataSerializers.VECTOR3);
    public BlockPos blockMin = null;
    public BlockPos blockMax = null;

    public static HoneyGlueEntity create(final EntityType<?> entityType, final Level world) {
        return new HoneyGlueEntity(entityType, world);
    }

    public HoneyGlueEntity(final EntityType<?> type, final Level world) {
        super(type, world);
        this.noPhysics = true;
    }

    @Override
    protected void defineSynchedData(final SynchedEntityData.Builder builder) {
        builder.define(BOUNDS_FROM, new Vector3f());
        builder.define(BOUNDS_TO, new Vector3f());
    }

    @Override
    public void onSyncedDataUpdated(final java.util.List<SynchedEntityData.DataValue<?>> values) {
        super.onSyncedDataUpdated(values);
        this.applyTrackedBounds();
    }

    @Override
    protected void reapplyPosition() {
        if (this.entityData == null) super.reapplyPosition(); else this.applyTrackedBounds();
    }

    private void applyTrackedBounds() {
        final var from = this.entityData.get(BOUNDS_FROM);
        final var to = this.entityData.get(BOUNDS_TO);
        final AABB bounds = new AABB(from.x(), from.y(), from.z(), to.x(), to.y(), to.z()).move(this.position());
        this.setBoundingBox(bounds);
        this.blockMin = BlockPos.containing(bounds.getMinPosition());
        this.blockMax = BlockPos.containing(bounds.getMaxPosition());
    }

    public HoneyGlueEntity(final Level world, final AABB boundingBox) {
        this(SimEntityTypes.HONEY_GLUE, world);
        this.setBoundingBox(boundingBox);
        this.resetPositionToBounds();
        this.setBoundsAndSync(boundingBox);
    }

    @Override
    public void tick() {
        this.xRotO = this.getXRot();
        this.yRotO = this.getYRot();
        this.xo = this.getX();
        this.yo = this.getY();
        this.zo = this.getZ();

        if (this.level().isClientSide()) {
            this.updateClientBounds();
        } else {
            if (this.getBoundingBox().getXsize() <0.9f || this.getBoundingBox().getYsize() <0.9f || this.getBoundingBox().getZsize() < 0.9f) {
                Simulated.LOGGER.warn("Removing {} ({}) due to invalid bounds!", this.getUUID(), SimLang.builder().add(this.getName()).string());
                this.discard();
            }
        }
    }

    public void updateClientBounds() {
        if (this.blockMin != null && this.blockMax != null) {
            this.setBoundingBox(new AABB(Vec3.atLowerCornerOf(this.blockMin), Vec3.atLowerCornerOf(this.blockMax)));
        }
    }

    public void resetPositionToBounds() {
        final AABB bb = this.getBoundingBox();
        this.setPosRaw(bb.getCenter().x, bb.minY, bb.getCenter().z);
    }

    public void spawnParticles() {
        final AABB bb = this.getBoundingBox();
        final Vec3 origin = new Vec3(bb.minX, bb.minY, bb.minZ);
        final Vec3 extents = new Vec3(bb.getXsize(), bb.getYsize(), bb.getZsize());

        if (!(this.level() instanceof final ServerLevel serverLevel)) {
            return;
        }

        for (final Direction.Axis axis : Iterate.axes) {
            final Direction.AxisDirection positive = Direction.AxisDirection.POSITIVE;
            final double max = axis.choose(extents.x, extents.y, extents.z);
            final Vec3 normal = Vec3.atLowerCornerOf(Direction.fromAxisAndDirection(axis, positive)
                    .getUnitVec3i());
            for (final Direction.Axis axis2 : Iterate.axes) {
                if (axis2 == axis) {
                    continue;
                }
                final double max2 = axis2.choose(extents.x, extents.y, extents.z);
                final Vec3 normal2 = Vec3.atLowerCornerOf(Direction.fromAxisAndDirection(axis2, positive)
                        .getUnitVec3i());
                for (final Direction.Axis axis3 : Iterate.axes) {
                    if (axis3 == axis2 || axis3 == axis) {
                        continue;
                    }
                    final double max3 = axis3.choose(extents.x, extents.y, extents.z);
                    final Vec3 normal3 = Vec3.atLowerCornerOf(Direction.fromAxisAndDirection(axis3, positive)
                            .getUnitVec3i());

                    for (int i = 0; i <= max * 2; i++) {
                        for (final int o1 : Iterate.zeroAndOne) {
                            for (final int o2 : Iterate.zeroAndOne) {
                                final Vec3 v = origin.add(normal.scale(i / 2f))
                                        .add(normal2.scale(max2 * o1))
                                        .add(normal3.scale(max3 * o2));

                                serverLevel.sendParticles(
                                        new ItemParticleOption(ParticleTypes.ITEM,Blocks.HONEY_BLOCK.asItem()), v.x,
                                        v.y, v.z, 1, 0, 0, 0, 0);

                            }
                        }
                    }
                    break;
                }
                break;
            }
        }
    }

    @Override
    protected boolean repositionEntityAfterLoad() {
        return false;
    }

    @Override
    public float rotate(final Rotation transformRotation) {
        final AABB bb = this.getBoundingBox().move(this.position().scale(-1));
        if (transformRotation == Rotation.CLOCKWISE_90 || transformRotation == Rotation.COUNTERCLOCKWISE_90) {
            this.setBoundsAndSync(new AABB(bb.minZ, bb.minY, bb.minX, bb.maxZ, bb.maxY, bb.maxX).move(this.position()));
        }

        return super.rotate(transformRotation);
    }

    @Override
    public void setPos(final double x, final double y, final double z) {
        final AABB bb = this.getBoundingBox();
        this.setPosRaw(x, y, z);
        final Vec3 center = bb.getCenter();
        this.setBoundingBox(bb.move(-center.x, -bb.minY, -center.z)
                .move(x, y, z));
    }

    @Override
    public void addAdditionalSaveData(final net.minecraft.world.level.storage.ValueOutput output) {
        dev.simulated_team.simulated.fabric.registry.NativeHoneyGlueBounds.write(output,this.getBoundingBox(),this.position());
    }

    @Override
    public void readAdditionalSaveData(final net.minecraft.world.level.storage.ValueInput input) {
        final var bounds=dev.simulated_team.simulated.fabric.registry.NativeHoneyGlueBounds.read(input);
        if(this.level().isClientSide())this.setBounds(bounds);else this.setBoundsAndSync(bounds);
    }

    @Override
    public void thunderHit(final ServerLevel serverLevel, final LightningBolt lightningBolt) {
    }

    @Override
    public double getEyeY() {
        return 0.0f;
    }

    public void setBoundsAndSync(final AABB bounds) {
        final Level level = this.level();

        if (level.isClientSide()) {
            return;
        }

        this.setBounds(bounds);
        this.syncBounds(null);
    }

    public void setBoundsAndSync(final AABB bounds, @Nullable final Player player) {
        final Level level = this.level();

        if (level.isClientSide()) {
            return;
        }

        this.setBounds(bounds);
        this.syncBounds(player);
    }

    public void syncBounds(@Nullable final Player player) {
        VeilPacketManager.tracking(this).sendPacket(new HoneyGlueSyncBoundsPacket(this.getBoundingBox(), this.getId(), player != null ? player.getUUID() : null));
    }

    public void setBounds(final AABB bounds) {
        this.setBoundingBox(bounds);
        this.resetPositionToBounds();

        this.blockMin = BlockPos.containing(bounds.getMinPosition());
        this.blockMax = BlockPos.containing(bounds.getMaxPosition());
        if (!this.level().isClientSide()) {
            final var relative = bounds.move(this.position().scale(-1));
            this.entityData.set(BOUNDS_FROM, new Vector3f((float) relative.minX, (float) relative.minY, (float) relative.minZ));
            this.entityData.set(BOUNDS_TO, new Vector3f((float) relative.maxX, (float) relative.maxY, (float) relative.maxZ));
        }
    }

    @Override
    public void move(final MoverType typeIn, final Vec3 pos) {
        if (!this.level().isClientSide() && this.isAlive() && pos.lengthSqr() > 0.0D) {
            this.discard();
        }
    }

    @Override
    public InteractionResult interact(final Player player, final InteractionHand hand,final Vec3 hit) {
        return InteractionResult.PASS;
    }

    @Override
    public void refreshDimensions() {
    }

    @Override
    public boolean hurtServer(final ServerLevel level,final DamageSource source,final float amount) {
        return false;
    }

    @Override public boolean hurtClient(final DamageSource source){return false;}

    @Override
    public ItemRequirement getRequiredItems() {
        return new ItemRequirement(ItemRequirement.ItemUseType.DAMAGE, SimItems.HONEY_GLUE);
    }

    @Override
    public boolean isIgnoringBlockTriggers() {
        return true;
    }

    public boolean contains(final BlockPos pos) {
        return this.getBoundingBox().contains(Vec3.atCenterOf(pos));
    }

    @Override
    public PushReaction getPistonPushReaction() {
        return PushReaction.IGNORE_ENTITY;
    }

    @Override
    public @NotNull EntityDimensions getDimensions(@NotNull final Pose pose) {
        return super.getDimensions(pose).withEyeHeight(0.0F);
    }
}
