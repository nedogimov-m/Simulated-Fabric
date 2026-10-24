package dev.simulated_team.simulated.fabric.registry;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.simibubi.create.AllPartialModels;
import com.simibubi.create.content.kinetics.base.IRotate;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.content.kinetics.simpleRelays.CogWheelBlock;
import com.simibubi.create.foundation.model.PartialModelRenderState;
import net.fabricmc.fabric.api.client.model.loading.v1.ExtraModelKey;
import net.fabricmc.fabric.api.client.renderer.v1.Renderer;
import net.fabricmc.fabric.api.client.renderer.v1.mesh.Mesh;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.joml.Quaternionf;

import java.util.ArrayList;
import java.util.List;

/** Extracts immutable Fabric meshes and copied transforms; submission never reads a block entity or world. */
public abstract class NativePartsRenderer<T extends BlockEntity> implements BlockEntityRenderer<T, NativePartsRenderer.State> {
    public record Part(Mesh mesh, Matrix4fc transform, int light, boolean translucent) {
        public Part { transform = new Matrix4f(transform); }
    }
    public record ItemPart(ItemStackRenderState item, Matrix4fc transform) {
        public ItemPart { transform = new Matrix4f(transform); }
    }
    public record GeometryPart(RenderType type, SubmitNodeCollector.CustomGeometryRenderer geometry, Matrix4fc transform) {
        public GeometryPart { transform = new Matrix4f(transform); }
    }
    public static class State extends BlockEntityRenderState {
        private List<Part> parts = List.of();
        private List<ItemPart> items = List.of();
        private List<GeometryPart> geometry = List.of();
        public static State from(Parts parts) {
            final var state = new State();
            state.lightCoords = parts.light;
            state.parts = List.copyOf(parts.entries);
            state.items = List.copyOf(parts.items);
            state.geometry = List.copyOf(parts.geometry);
            return state;
        }
    }
    public static final class Parts {
        private final List<Part> entries = new ArrayList<>();
        private final List<ItemPart> items = new ArrayList<>();
        private final List<GeometryPart> geometry = new ArrayList<>();
        private final int light;
        public Parts(int light) { this.light = light; }
        public int light() { return this.light; }
        /** Geometry callbacks must retain only detached extraction values, never live entities/worlds. */
        public void addGeometry(RenderType type, SubmitNodeCollector.CustomGeometryRenderer geometry, PoseStack pose) {
            this.geometry.add(new GeometryPart(type, geometry, pose.last().pose()));
        }
        public void addItem(ItemStackRenderState item, PoseStack pose) {
            if (!item.isEmpty()) this.items.add(new ItemPart(item, pose.last().pose()));
        }
        public void add(ExtraModelKey<BlockStateModel> key, PoseStack pose) {
            add(key, pose, -1, null, false, this.light);
        }
        public void add(PartialModelRenderState model, PoseStack pose) {
            add(model, pose, -1, null, false, this.light);
        }
        public void add(ExtraModelKey<BlockStateModel> key, PoseStack pose, int color,
                        ChunkSectionLayer layer, boolean emissive, int light) {
            final var model = new PartialModelRenderState();
            AllPartialModels.extract(key, model);
            add(model, pose, color, layer, emissive, light);
        }
        public void add(BlockState block, PoseStack pose) {
            final var model = new PartialModelRenderState();
            AllPartialModels.extract(block, model);
            add(model, pose, -1, null, false, this.light);
        }
        private void add(PartialModelRenderState model, PoseStack pose, int color,
                         ChunkSectionLayer layer, boolean emissive, int light) {
            Mesh mesh = model.mesh;
            if (color != -1 || layer != null || emissive) {
                final var target = Renderer.get().mutableMesh();
                final var output = target.emitter();
                mesh.forEach(quad -> {
                    output.copyFrom(quad);
                    if (color != -1) output.color(color, color, color, color);
                    if (layer != null) output.chunkLayer(layer);
                    if (emissive) output.emissive(true);
                    output.emit();
                });
                mesh = target.immutableCopy();
            }
            this.entries.add(new Part(mesh, pose.last().pose(), light,
                    layer == null ? model.translucent : layer == ChunkSectionLayer.TRANSLUCENT));
        }
    }

    @Override public State createRenderState() { return new State(); }
    @Override public void extractRenderState(T entity, State state, float partialTick, Vec3 camera,
                                             ModelFeatureRenderer.CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(entity, state, partialTick, camera, breakProgress);
        final var parts = new Parts(state.lightCoords);
        if (entity instanceof KineticBlockEntity kinetic) {
            final var bracket = kinetic.bracketBehaviour().getBracket();
            if (bracket != null) parts.add(bracket, new PoseStack());
        }
        extractParts(entity, partialTick, parts);
        state.parts = List.copyOf(parts.entries);
        state.items = List.copyOf(parts.items);
        state.geometry = List.copyOf(parts.geometry);
    }
    protected abstract void extractParts(T entity, float partialTick, Parts parts);

    @Override public void submit(State state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
        submitParts(state, pose, collector);
    }
    public static void submitParts(State state, PoseStack pose, SubmitNodeCollector collector) {
        for (Part part : state.parts) {
            pose.pushPose();
            pose.mulPose(part.transform());
            final var model = new PartialModelRenderState();
            model.mesh = part.mesh();
            model.translucent = part.translucent();
            model.submit(pose, collector, part.light(), OverlayTexture.NO_OVERLAY, 0);
            if (state.breakProgress != null) {
                // The native collector snapshots this part's transformed pose with its immutable mesh.
                collector.submitBreakingBlockModel(pose, List.of(), part.mesh(), state.breakProgress.progress(), true);
            }
            pose.popPose();
        }
        for (ItemPart item : state.items) {
            pose.pushPose();
            pose.mulPose(item.transform());
            item.item().submit(pose, collector, state.lightCoords, OverlayTexture.NO_OVERLAY, 0);
            pose.popPose();
        }
        for (GeometryPart geometry : state.geometry) {
            pose.pushPose();
            pose.mulPose(geometry.transform());
            collector.submitCustomGeometry(pose, geometry.type(), geometry.geometry());
            pose.popPose();
        }
    }
    public static Direction.Axis rotationAxis(KineticBlockEntity entity) {
        final var state = entity.getBlockState();
        return ((IRotate) state.getBlock()).getRotationAxis(state);
    }
    public static float rotationOffset(KineticBlockEntity entity, Direction.Axis axis) {
        final var pos = entity.getBlockPos();
        final int parity = (axis == Direction.Axis.X ? 0 : pos.getX()) + (axis == Direction.Axis.Y ? 0 : pos.getY())
                + (axis == Direction.Axis.Z ? 0 : pos.getZ());
        return parity % 2 == 0 ? 22.5f : entity.getBlockState().getBlock() instanceof CogWheelBlock cog && cog.isLargeCog() ? 11.25f : 0;
    }
    public static PoseStack rotating(KineticBlockEntity entity, float partial, Direction.Axis axis) {
        final var pose = new PoseStack();
        rotateCentered(pose, axis, entity.getAngle(partial) + rotationOffset(entity, axis));
        return pose;
    }
    public static void rotateCentered(PoseStack pose, Direction.Axis axis, float degrees) {
        rotateCentered(pose, switch (axis) {
            case X -> Axis.XP.rotationDegrees(degrees);
            case Y -> Axis.YP.rotationDegrees(degrees);
            case Z -> Axis.ZP.rotationDegrees(degrees);
        });
    }
    public static void rotateCentered(PoseStack pose, Quaternionf rotation) {
        pose.translate(0.5, 0.5, 0.5);
        pose.rotate(rotation);
        pose.translate(-0.5, -0.5, -0.5);
    }
    public static void faceSouth(PoseStack pose, Direction facing) {
        rotateCentered(pose, new Quaternionf().rotationTo(0, 0, 1, facing.getStepX(), facing.getStepY(), facing.getStepZ()));
    }
}
