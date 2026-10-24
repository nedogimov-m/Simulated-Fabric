package dev.simulated_team.simulated.content.blocks.lasers;

import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.util.SableDistUtil;
import dev.simulated_team.simulated.fabric.registry.NativeLaserGeometry;
import dev.simulated_team.simulated.fabric.registry.NativePartsRenderer;
import dev.simulated_team.simulated.index.SimRenderTypes;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector4f;

/** Ray results and owner colour are resolved once; native submission receives detached beam vertices. */
public abstract class AbstractLaserRenderer<T extends AbstractLaserBlockEntity> extends NativePartsRenderer<T> {
    public AbstractLaserRenderer(BlockEntityRendererProvider.Context context) { }
    @Override protected void extractParts(T entity,float partial,Parts parts) {
        final var laser=entity.getAllBehaviours().stream().filter(LaserBehaviour.class::isInstance).map(LaserBehaviour.class::cast).findFirst().orElse(null);
        if(laser==null||!laser.shouldCast())return;
        final var colors=getColors(entity,partial);if(colors.w<=0)return;
        final var geometry=NativeLaserGeometry.extract(colors,laser.getRange(),getLaserLength(laser));
        if(!geometry.vertices().isEmpty())parts.addGeometry(SimRenderTypes.laser(),geometry,NativeLaserGeometry.pose(entity.getDirection(),getLaserScale(laser)));
    }
    public abstract Vector4f getColors(T entity,float partial);
    public abstract float getLaserScale(LaserBehaviour laser);
    public float getLaserLength(LaserBehaviour laser) {
        final var hit=getRenderedHitResult(laser);final var virtual=laser.getVirtualHitPos();
        if(hit!=null&&hit.getType()!=HitResult.Type.MISS||!virtual.equals(Vec3.ZERO)) {
            final var target=!virtual.equals(Vec3.ZERO)?virtual:hit.getLocation();
            return (float)Math.sqrt(Sable.HELPER.distanceSquaredWithSubLevels(SableDistUtil.getClientLevel(),laser.getLaserPositions().get().getFirst(),target))-.1f;
        }
        return laser.getRange();
    }
    public HitResult getRenderedHitResult(LaserBehaviour laser){return laser.getClosestHitResult();}
    @Override public boolean shouldRenderOffScreen(){return true;}
    @Override public int getViewDistance(){return 256;}
}
