package dev.eriksonn.aeronautics.fabric.registry;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.content.contraptions.render.ContraptionEntityRenderer;
import dev.eriksonn.aeronautics.content.blocks.propeller.bearing.contraption.PropellerBearingContraptionEntity;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import org.joml.Vector3f;

/** Keeps the original gyroscopic 12/16 tilt pivot around the real native contraption renderer. */
public final class NativePropellerContraptionRenderer extends EntityRenderer<PropellerBearingContraptionEntity, ContraptionEntityRenderer.State> {
    private final ContraptionEntityRenderer delegate;
    public NativePropellerContraptionRenderer(EntityRendererProvider.Context context) { super(context);this.delegate=new ContraptionEntityRenderer(context); }
    @Override public ContraptionEntityRenderer.State createRenderState() { return this.delegate.createRenderState(); }
    @Override public void extractRenderState(PropellerBearingContraptionEntity entity,ContraptionEntityRenderer.State state,float partial) {
        this.delegate.extractRenderState(entity,state,partial);
        final var normal=new Vector3f(entity.direction.getStepX(),entity.direction.getStepY(),entity.direction.getStepZ()).mul(12/16f);
        final var shifted=new Vector3f(normal).rotate(entity.getInterpolatedQuat(partial)).sub(normal);
        state.anchorOffset=state.anchorOffset.add(shifted.x,shifted.y,shifted.z);
    }
    @Override public void submit(ContraptionEntityRenderer.State state,PoseStack pose,SubmitNodeCollector collector,CameraRenderState camera) {
        this.delegate.submit(state,pose,collector,camera);
    }
}
