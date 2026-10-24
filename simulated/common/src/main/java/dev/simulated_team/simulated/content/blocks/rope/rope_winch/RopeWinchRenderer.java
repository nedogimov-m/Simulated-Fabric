package dev.simulated_team.simulated.content.blocks.rope.rope_winch;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.AllPartialModels;
import com.simibubi.create.foundation.model.PartialModelRenderState;
import dev.simulated_team.simulated.content.blocks.rope.strand.client.RopeStrandRenderer;
import dev.simulated_team.simulated.fabric.registry.NativePartsRenderer;
import dev.simulated_team.simulated.index.SimPartialModels;
import dev.simulated_team.simulated.index.SimSpriteShifts;
import net.createmod.catnip.math.AngleHelper;
import net.fabricmc.fabric.api.client.renderer.v1.Renderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.data.AtlasIds;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import static com.simibubi.create.content.kinetics.base.DirectionalAxisKineticBlock.AXIS_ALONG_FIRST_COORDINATE;
import static com.simibubi.create.content.kinetics.base.DirectionalKineticBlock.FACING;

public final class RopeWinchRenderer extends NativePartsRenderer<RopeWinchBlockEntity> {
    public RopeWinchRenderer(BlockEntityRendererProvider.Context context) {}
    @Override public boolean shouldRenderOffScreen() { return true; }
    @Override public boolean shouldRender(RopeWinchBlockEntity entity, Vec3 camera) { return true; }
    @Override protected void extractParts(RopeWinchBlockEntity entity, float partial, Parts parts) {
        final var state = entity.getBlockState();
        final var shaft = rotating(entity, partial, rotationAxis(entity));
        transform(shaft, state);
        parts.add(SimPartialModels.ROPE_WINCH_SHAFT, shaft);
        final var holder = entity.getRopeHolder();
        if (holder.isAttached() || entity.isVirtual() && holder.renderAttached) {
            final var coil = new PartialModelRenderState();
            AllPartialModels.extract(SimPartialModels.ROPE_WINCH_ROPE_COIL, coil);
            final var facing = state.getValue(FACING);
            final float speed = facing == Direction.DOWN ? 1 :
                    (facing.getAxisDirection() == Direction.AxisDirection.NEGATIVE) == state.getValue(AXIS_ALONG_FIRST_COORDINATE) ? 1 : -1;
            scrollCoil(coil, entity.clientAngle.getValue(partial), speed);
            final var pose = new PoseStack(); transform(pose, state);
            parts.add(coil, pose);
        }
        RopeStrandRenderer.extract(entity, holder, partial, parts);
    }
    private static void transform(PoseStack pose, BlockState state) {
        final var facing = state.getValue(FACING);
        final boolean first = state.getValue(AXIS_ALONG_FIRST_COORDINATE);
        rotateCentered(pose, Direction.Axis.Z, facing == Direction.UP ? 270 : facing == Direction.DOWN ? 90 : 0);
        rotateCentered(pose, Direction.Axis.Y, AngleHelper.horizontalAngle(facing) + (first || facing.getAxis() != Direction.Axis.Y ? 0 : 90));
        rotateCentered(pose, Direction.Axis.Z, (first ^ facing.getAxis() == Direction.Axis.Z) ? 90 : 0);
    }
    private static void scrollCoil(PartialModelRenderState model, float offset, float speed) {
        if (offset == 0) return;
        final var shift = SimSpriteShifts.ROPE_WINCH_COIL;
        final var atlas = Minecraft.getInstance().getAtlasManager().getAtlasOrThrow(AtlasIds.BLOCKS);
        final var source = atlas.getSprite(shift.original());
        final var target = atlas.getSprite(shift.target());
        final float phase = offset * speed / 2 + 3f / 16;
        final float scroll = -phase - (float) Math.floor(-2 * phase) / 2;
        final var mesh = Renderer.get().mutableMesh();
        final var emitter = mesh.emitter();
        model.mesh.forEach(quad -> {
            emitter.copyFrom(quad);
            for (int i = 0; i < 4; i++) emitter.uv(i, quad.u(i) - source.getU0() + target.getU0(),
                    quad.v(i) - source.getV0() + target.getV0() + scroll * (target.getV1() - target.getV0()));
            emitter.emit();
        });
        model.mesh = mesh.immutableCopy();
    }
}
