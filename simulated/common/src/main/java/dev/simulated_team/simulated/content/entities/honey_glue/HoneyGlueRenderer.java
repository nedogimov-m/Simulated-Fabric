package dev.simulated_team.simulated.content.entities.honey_glue;

import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;

public class HoneyGlueRenderer extends EntityRenderer<HoneyGlueEntity, EntityRenderState> {

	public HoneyGlueRenderer(final EntityRendererProvider.Context context) {
		super(context);
	}

	@Override
	public EntityRenderState createRenderState() {
		return new EntityRenderState();
	}

	@Override
	public boolean shouldRender(final HoneyGlueEntity entity, final Frustum frustum, final double x, final double y, final double z, final float partialTick) {
		return false;
	}

}
