package dev.simulated_team.simulated.content.entities.diagram;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.simibubi.create.AllPartialModels;
import com.simibubi.create.foundation.model.PartialModelRenderState;
import dev.simulated_team.simulated.index.SimPartialModels;
import net.fabricmc.fabric.api.client.renderer.v1.Renderer;
import net.fabricmc.fabric.api.client.renderer.v1.mesh.Mesh;
import net.fabricmc.fabric.api.util.TriState;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import org.joml.Matrix4f;

/** Extracts the original board geometry and transform before native render submission. */
public final class DiagramEntityRenderer extends EntityRenderer<DiagramEntity, DiagramEntityRenderer.State> {
    private final Mesh[] sourceModels = new Mesh[3];
    private final Mesh[] unshadedModels = new Mesh[3];

    public static final class State extends EntityRenderState {
        final PartialModelRenderState board = new PartialModelRenderState();
        final Matrix4f transform = new Matrix4f();
    }

    public DiagramEntityRenderer(EntityRendererProvider.Context context) { super(context); }
    @Override public State createRenderState() { return new State(); }

    @Override
    public void extractRenderState(DiagramEntity entity, State state, float partialTick) {
        super.extractRenderState(entity, state, partialTick);
        final int size = entity.getSize() == 3 ? 3 : entity.getSize() == 2 ? 2 : 1;
        AllPartialModels.extract(size == 3 ? SimPartialModels.CONTRAPTION_DIAGRAM_3x3
                : size == 2 ? SimPartialModels.CONTRAPTION_DIAGRAM_2x2 : SimPartialModels.CONTRAPTION_DIAGRAM_1x1, state.board);
        final int index = size - 1;
        if (this.sourceModels[index] != state.board.mesh) {
            final var mesh = Renderer.get().mutableMesh();
            final var emitter = mesh.emitter();
            state.board.mesh.forEach(quad -> emitter.copyFrom(quad).shadeDirectionOverride(Direction.UP)
                    .ambientOcclusion(TriState.FALSE).emissive(false).emit());
            this.sourceModels[index] = state.board.mesh;
            this.unshadedModels[index] = mesh.immutableCopy();
        }
        state.board.mesh = this.unshadedModels[index];
        final var pose = new PoseStack();
        pose.rotateDegrees(Axis.YP, -Mth.lerp(partialTick, entity.yRotO, entity.getYRot()));
        pose.rotateDegrees(Axis.XP, 90.0f + entity.getXRot());
        pose.translate(-0.5, -1 / 32f, -0.5);
        if (size == 2) pose.translate(0.5, 0, -0.5);
        state.transform.set(pose.last().pose());
    }

    @Override
    public void submit(State state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
        pose.pushPose();
        pose.mulPose(state.transform);
        state.board.submit(pose, collector, state.lightCoords, OverlayTexture.NO_OVERLAY, state.outlineColor);
        pose.popPose();
        super.submit(state, pose, collector, camera);
    }
}
