package dev.eriksonn.aeronautics.content.particle;

import net.minecraft.client.renderer.state.level.QuadParticleRenderState;
import dev.ryanhcode.sable.api.particle.ParticleSubLevelKickable;
import dev.ryanhcode.sable.mixinterface.particle.ParticleExtension;
import dev.ryanhcode.sable.sublevel.ClientSubLevel;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.*;
import org.joml.Quaterniondc;
import org.joml.Quaternionf;

public class GustParticle extends SingleQuadParticle implements ParticleSubLevelKickable {

	private static final float FPS = 16.0f;
	private static final float FRAMES = 8.0f;

	private final Quaternionf orientation;
	private final Quaternionf renderOrientation = new Quaternionf();
	private final Quaternionf subLevelOrientation = new Quaternionf();

	protected GustParticle(final ClientLevel level, final double x, final double y, final double z, final Quaternionf orientation, final net.minecraft.client.renderer.texture.TextureAtlasSprite sprite) {
		super(level, x, y, z, sprite);
		this.orientation = new Quaternionf(orientation).normalize();
		this.quadSize = 2;
		this.lifetime = (int) (FRAMES / FPS * 20) - 1;
		this.alpha = 0.25f;
	}

	@Override
	public void extract(final QuadParticleRenderState state, final Camera renderInfo, final float partialTicks) {
        this.renderOrientation.set(this.orientation);

		final ParticleExtension extension = (ParticleExtension) this;
		if (extension.sable$getTrackingSubLevel() instanceof final ClientSubLevel subLevel) {
			final Quaterniondc orientation1 = subLevel.renderPose().orientation();
            this.renderOrientation.premul(this.subLevelOrientation.set(orientation1));
		}
		this.extractRotatedQuad(state, renderInfo, this.renderOrientation, partialTicks);
	}

	@Override
    protected void extractRotatedQuad(final QuadParticleRenderState state, final Quaternionf rotation,
                                      final float x, final float y, final float z, final float partialTicks) {
        super.extractRotatedQuad(state, rotation, x, y, z, partialTicks);
        // Reverse the winding with the same UVs at each original corner.
        final Quaternionf back = new Quaternionf(rotation).rotateY((float) Math.PI);
        state.add(this.getLayer(), x, y, z, back.x, back.y, back.z, back.w, this.getQuadSize(partialTicks),
                this.getU1(), this.getU0(), this.getV0(), this.getV1(),
                net.minecraft.util.ARGB.colorFromFloat(this.alpha, this.rCol, this.gCol, this.bCol),
                this.getLightCoords(partialTicks));
    }

	@Override
	protected float getU0() {
		float offset = getFrameOffset(this.getFrame());
		return super.getU0() + offset;
	}

	@Override
	protected float getU1() {
		float offset = getFrameOffset((this.getFrame() + 1));
		return super.getU0() + offset;
	}

	private float getFrameOffset(int frame) {
		float width = this.sprite.getU1() - this.sprite.getU0();
		float frameWidth = width / FRAMES;
		return frameWidth * frame;
	}

	private int getFrame() {
		final float age = this.age / 20f;
		return (int) (age * FPS);
	}


	@Override
	public void tick() {
		super.tick();
	}

	@Override
	public Layer getLayer() {
		return Layer.TRANSLUCENT;
	}

	@Override
	public boolean sable$shouldKickFromTracking() {
		return false;
	}

	@Override
	public boolean sable$shouldCollideWithTrackingSubLevel() {
		return false;
	}

	public static class Factory implements ParticleProvider<GustParticleData> {
		private final SpriteSet spriteSet;

		public Factory(final SpriteSet animatedSprite) {
			this.spriteSet = animatedSprite;
		}

		public Particle createParticle(final GustParticleData data, final ClientLevel worldIn, final double x, final double y, final double z,
									   final double xSpeed, final double ySpeed, final double zSpeed, final net.minecraft.util.RandomSource random) {
			final GustParticle particle = new GustParticle(worldIn, x, y, z, data.orientation(), this.spriteSet.get(random));
			return particle;
		}

	}
}
