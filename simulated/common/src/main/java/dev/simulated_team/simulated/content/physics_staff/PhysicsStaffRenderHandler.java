package dev.simulated_team.simulated.content.physics_staff;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.api.SubLevelHelper;
import dev.ryanhcode.sable.api.sublevel.SubLevelContainer;
import dev.ryanhcode.sable.mixinterface.clip_overwrite.LevelPoseProviderExtension;
import dev.ryanhcode.sable.sublevel.ClientSubLevel;
import dev.ryanhcode.sable.sublevel.SubLevel;
import dev.simulated_team.simulated.SimulatedClient;
import dev.simulated_team.simulated.index.SimItems;
import dev.simulated_team.simulated.index.SimRenderTypes;
import foundry.veil.api.client.color.Color;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import dev.simulated_team.simulated.fabric.registry.NativeVertexSnapshot;
import foundry.veil.api.event.VeilRenderLevelStageEvent;
import net.createmod.catnip.outliner.Outliner;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4fc;
import org.joml.Vector3dc;

import java.util.List;
import java.util.UUID;

public class PhysicsStaffRenderHandler {

    @Nullable
    private static BlockPos hoverBlockPos = null;

    /**
     * Renders the selection / hovering box for the staff
     */
    public static void renderSelectionBox(final VeilRenderLevelStageEvent.Stage stage, final VeilRenderLevelStageEvent.Context context) {
        if (stage != VeilRenderLevelStageEvent.Stage.COLLECT_SUBMITS) return;
        final Minecraft client = Minecraft.getInstance();
        if (client.player == null || client.level == null) return;
        final PoseStack ps = new PoseStack();
        if (Minecraft.getInstance().gui.hud.isHidden()) {
            return;
        }

        SimulatedClient.PHYSICS_STAFF_CLIENT_HANDLER.onRender(ps, context.submissions(), context.camera().pos, context.camera().cameraEntityPartialTicks);

        final Minecraft minecraft = Minecraft.getInstance();
        final LocalPlayer player = minecraft.player;

        if (!player.getItemInHand(InteractionHand.MAIN_HAND).is(SimItems.PHYSICS_STAFF) &&
                !player.getItemInHand(InteractionHand.OFF_HAND).is(SimItems.PHYSICS_STAFF)) {
            return;
        }

        final Vec3 cameraPos = context.camera().pos;

        final Level level = player.level();
        renderAllLocks(context.submissions(), ps, level, context.camera());

        updateHoverPos(minecraft, player);

        if (hoverBlockPos != null) {
            final Color color = new Color(191.0f / 255.0f, 191.0f / 255.0f, 191.0f / 255.0f, 1.0f);

            Outliner.getInstance().showCluster("physicsStaffSelection", List.of(hoverBlockPos))
                    .colored(color.rgb())
                    .disableLineNormals()
                    .lineWidth(1 / 32f)
                    .withFaceTexture(() -> net.minecraft.resources.Identifier.fromNamespaceAndPath("create", "textures/special/checkerboard.png"));
        }
    }

    /**
     * Updates the hovered block position
     */
    private static void updateHoverPos(final Minecraft minecraft, final LocalPlayer player) {
        final ClientLevel level = minecraft.level;
        final float partialTicks = minecraft.getDeltaTracker().getGameTimeDeltaPartialTick(false);

        hoverBlockPos = null;

        final PhysicsStaffClientHandler.ClientDragSession dragSession = SimulatedClient.PHYSICS_STAFF_CLIENT_HANDLER.getDragSession();

        if (dragSession != null) {
            final Vector3dc localAnchor = dragSession.dragLocalAnchor();
            hoverBlockPos = BlockPos.containing(localAnchor.x(), localAnchor.y(), localAnchor.z());
            return;
        }

        final LevelPoseProviderExtension extension = (LevelPoseProviderExtension) level;
        extension.sable$pushPoseSupplier(x -> ((ClientSubLevel) x).renderPose());
        final HitResult hit;
        try { hit = player.pick(PhysicsStaffItem.RANGE, partialTicks, false); }
        finally { extension.sable$popPoseSupplier(); }

        if (!(hit instanceof final BlockHitResult blockHitResult) || blockHitResult.getType() == HitResult.Type.MISS) {
            return;
        }

        final Vec3 hitLocation = hit.getLocation();

        final SubLevel subLevel = Sable.HELPER.getContaining(level, hitLocation);
        if (subLevel == null) {
            return;
        }

        hoverBlockPos = blockHitResult.getBlockPos();
    }

    /**
     * Renders all the locks our client is aware about
     */
    private static void renderAllLocks(final SubmitNodeCollector nodes, final PoseStack ps, final Level level, final CameraRenderState camera) {
        final Minecraft client = Minecraft.getInstance();
        final List<UUID> locks = SimulatedClient.PHYSICS_STAFF_CLIENT_HANDLER.getLocks(level);
        final SubLevelContainer container = SubLevelContainer.getContainer(level);

        for (final UUID lock : locks) {
            final SubLevel subLevel = container.getSubLevel(lock);

            if (!(subLevel instanceof final ClientSubLevel clientSubLevel)) continue;

            ps.pushPose();
            final Vector3dc renderPos = clientSubLevel.renderPose().position();
            ps.translate(renderPos.x() - camera.pos.x(), renderPos.y() - camera.pos.y(), renderPos.z() - camera.pos.z());
            ps.mulPose(new org.joml.Matrix4f().rotation(camera.orientation));

            final var buffer = new NativeVertexSnapshot.Capture();

            final PoseStack.Pose pose = new PoseStack().last();
            final int color = 0xffffffff;
            buffer.addVertex(pose, 0.0f - 0.5f, 0.0f - 0.5f, 0.0f).setColor(color).setUv(0.0f, 1.0f).setLight(net.minecraft.util.LightCoordsUtil.FULL_BRIGHT);
            buffer.addVertex(pose, 0.0f - 0.5f, 1.0f - 0.5f, 0.0f).setColor(color).setUv(0.0f, 0.0f).setLight(net.minecraft.util.LightCoordsUtil.FULL_BRIGHT);
            buffer.addVertex(pose, 1.0f - 0.5f, 1.0f - 0.5f, 0.0f).setColor(color).setUv(1.0f, 0.0f).setLight(net.minecraft.util.LightCoordsUtil.FULL_BRIGHT);
            buffer.addVertex(pose, 1.0f - 0.5f, 0.0f - 0.5f, 0.0f).setColor(color).setUv(1.0f, 1.0f).setLight(net.minecraft.util.LightCoordsUtil.FULL_BRIGHT);

            nodes.submitCustomGeometry(ps, SimRenderTypes.lock(), buffer.finish());
            ps.popPose();
        }
    }

}
