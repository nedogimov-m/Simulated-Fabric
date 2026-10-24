package dev.simulated_team.simulated.fabric.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.sublevel.ClientSubLevel;
import dev.ryanhcode.sable.sublevel.render.vanilla.PlotSectionMesh;
import dev.ryanhcode.sable.sublevel.render.vanilla.VanillaChunkedSubLevelRenderData;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.chunk.RenderRegionCache;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.joml.Quaternionf;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;

/** Detached plot/BE/entity content for caller-owned native scene passes. */
public final class NativeSubLevelGroupFrame {
    @FunctionalInterface private interface BlockSubmit {
        void submit(PoseStack pose, SubmitNodeCollector nodes, CameraRenderState camera);
    }
    private record BlockDraw(Matrix4fc transform, BlockSubmit submit) {
        private BlockDraw { transform = new Matrix4f(transform); }
    }
    private final List<PlotSectionMesh.Snapshot> terrain;
    private final List<BlockDraw> blocks;
    private final List<EntityRenderState> entities;
    private final EntityRenderDispatcher entityDispatcher;
    private final Vec3 cameraPosition;
    private NativeSubLevelGroupFrame(List<PlotSectionMesh.Snapshot> terrain, List<BlockDraw> blocks,
                                    List<EntityRenderState> entities, EntityRenderDispatcher dispatcher, Vec3 cameraPosition) {
        this.terrain = List.copyOf(terrain); this.blocks = List.copyOf(blocks); this.entities = List.copyOf(entities);
        this.entityDispatcher = dispatcher; this.cameraPosition = cameraPosition;
    }
    public static NativeSubLevelGroupFrame extract(ClientLevel level, Collection<ClientSubLevel> selected,
                                                    Vec3 cameraPosition, float partial, boolean renderPlayers) {
        final var client = Minecraft.getInstance();
        final var regions = new RenderRegionCache();
        final var updates = client.options.prioritizeChunkUpdates().get();
        final var dispatcher = client.getEntityRenderDispatcher();
        final var terrain = new ArrayList<PlotSectionMesh.Snapshot>();
        final var blocks = new ArrayList<BlockDraw>();
        final var entities = new ArrayList<EntityRenderState>();
        final var visited = Collections.newSetFromMap(new IdentityHashMap<Entity, Boolean>());
        for (final var owner : List.copyOf(selected)) {
            if (owner.isRemoved()) continue;
            final var data = (VanillaChunkedSubLevelRenderData) owner.getRenderData();
            data.compileSections(updates, regions, client.gameRenderer.mainCamera());
            terrain.addAll(data.extract(cameraPosition, partial));
            final var bounds = owner.getPlot().getBoundingBox();
            if (bounds == null) continue;
            final var pose = owner.renderPose(partial);
            for (int z = bounds.minZ() >> 4; z <= bounds.maxZ() >> 4; z++) {
                for (int x = bounds.minX() >> 4; x <= bounds.maxX() >> 4; x++) {
                    for (final var entity : List.copyOf(level.getChunk(x, z).getBlockEntities().values())) {
                        final var p = entity.getBlockPos();
                        if (entity.isRemoved() || p.getX() < bounds.minX() || p.getX() > bounds.maxX()
                                || p.getY() < bounds.minY() || p.getY() > bounds.maxY()
                                || p.getZ() < bounds.minZ() || p.getZ() > bounds.maxZ()) continue;
                        final var renderer = client.getBlockEntityRenderDispatcher().getRenderer(entity);
                        if (renderer == null) continue;
                        final var origin = pose.transformPosition(Vec3.atLowerCornerOf(p)).subtract(cameraPosition);
                        final var transform = new Matrix4f().translation((float) origin.x, (float) origin.y, (float) origin.z)
                                .rotate(new Quaternionf(pose.orientation())).scale((float) pose.scale().x(), (float) pose.scale().y(), (float) pose.scale().z());
                        // Direct renderer extraction as in Create contraptions: parent owns the full plot T/R/S.
                        blocks.add(extractBlock(entity, renderer, partial, pose.transformPositionInverse(cameraPosition), transform));
                    }
                }
            }
            for (final var entity : level.getEntitiesOfClass(Entity.class, bounds.toAABB().inflate(16))) {
                if ((!renderPlayers && entity instanceof Player) || visited.contains(entity)) continue;
                if (Sable.HELPER.getContaining(entity) != owner && Sable.HELPER.getTrackingOrVehicleSubLevel(entity) != owner) continue;
                visited.add(entity);
                // The real dispatcher also applies Sable's plot, riding and eye transform extraction.
                entities.add(dispatcher.extractEntity(entity, level.tickRateManager().isEntityFrozen(entity) ? 1 : partial));
            }
        }
        return new NativeSubLevelGroupFrame(terrain, blocks, entities, dispatcher, cameraPosition);
    }
    private static <T extends BlockEntity, S extends BlockEntityRenderState> BlockDraw extractBlock(
            T entity, BlockEntityRenderer<T, S> renderer, float partial, Vec3 camera, Matrix4fc transform) {
        final S state = renderer.createRenderState();
        renderer.extractRenderState(entity, state, partial, camera, null);
        return new BlockDraw(transform, (pose, nodes, frameCamera) -> renderer.submit(state, pose, nodes, frameCamera));
    }
    public void submitTerrain(PoseStack parent, SubmitNodeCollector nodes) {
        for (final var section : terrain) section.submit(parent, nodes);
    }
    public void submitFeatures(PoseStack parent, SubmitNodeCollector nodes, CameraRenderState camera) {
        for (final var draw : blocks) {
            parent.pushPose();
            try { parent.mulPose(draw.transform()); draw.submit().submit(parent, nodes, camera); }
            finally { parent.popPose(); }
        }
        for (final var entity : entities) entityDispatcher.submit(entity, camera,
                entity.x - cameraPosition.x, entity.y - cameraPosition.y, entity.z - cameraPosition.z, parent, nodes);
    }
    public boolean isEmpty() { return terrain.isEmpty() && blocks.isEmpty() && entities.isEmpty(); }
}
