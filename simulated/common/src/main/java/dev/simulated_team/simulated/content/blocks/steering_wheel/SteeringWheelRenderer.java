package dev.simulated_team.simulated.content.blocks.steering_wheel;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.simibubi.create.AllPartialModels;
import com.simibubi.create.foundation.model.PartialModelRenderState;
import dev.simulated_team.simulated.fabric.registry.NativePartsRenderer;
import dev.simulated_team.simulated.fabric.registry.NativePartTransforms;
import dev.simulated_team.simulated.index.SimPartialModels;
import net.fabricmc.fabric.api.client.renderer.v1.Renderer;
import net.fabricmc.fabric.api.client.renderer.v1.mesh.Mesh;
import net.fabricmc.fabric.api.client.renderer.v1.sprite.SpriteFinder;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.AtlasIds;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/** Native immutable wheel meshes retain original arbitrary plank textures and both mounting transforms. */
public final class SteeringWheelRenderer extends NativePartsRenderer<SteeringWheelBlockEntity> {
    private final Map<BlockState,Mesh> wheelMeshes=new HashMap<>();
    private Mesh templateMesh;
    public SteeringWheelRenderer(BlockEntityRendererProvider.Context context) { }
    @Override protected void extractParts(SteeringWheelBlockEntity entity,float partial,Parts parts) {
        final var state=entity.getBlockState();final boolean floor=state.getValue(SteeringWheelBlock.ON_FLOOR);
        if(entity.shouldRenderShaft()) {
            final var shaft=rotating(entity,partial,rotationAxis(entity));
            NativePartTransforms.partialFacing(shaft,floor?Direction.DOWN:Direction.UP);
            parts.add(AllPartialModels.SHAFT_HALF,shaft);
        }
        final var wheel=new PoseStack();rotateCentered(wheel,state.getValue(SteeringWheelBlock.FACING).getRotation());
        wheel.translate(0,6.5/16f,(floor?-5:5)/16f);
        rotateCentered(wheel,Axis.YP.rotation(entity.getRenderAngle(partial)));
        final var template=new PartialModelRenderState();AllPartialModels.extract(SimPartialModels.STEERING_WHEEL,template);
        if(template.mesh!=this.templateMesh){wheelMeshes.clear();this.templateMesh=template.mesh;}
        final var model=new PartialModelRenderState();model.mesh=wheelMeshes.computeIfAbsent(entity.material,material->generateMesh(template.mesh,material));
        model.translucent=template.translucent;parts.add(model,wheel);
    }
    private static Mesh generateMesh(Mesh template,BlockState material) {
        final String path=BuiltInRegistries.BLOCK.getKey(material.getBlock()).getPath();
        if(!path.endsWith("_planks")&&!path.contains("wood/planks/"))return template;
        final var client=Minecraft.getInstance();final var atlas=client.getAtlasManager().getAtlasOrThrow(AtlasIds.BLOCKS);
        final var oak=atlas.getSprite(net.minecraft.resources.Identifier.parse("minecraft:block/oak_planks"));
        final var target=spriteOnSide(material,Direction.UP);if(target==null)return template;
        return remap(template,atlas.spriteFinder(),oak,target);
    }
    public static Mesh remap(Mesh template,SpriteFinder finder,TextureAtlasSprite original,TextureAtlasSprite target) {
        final var replacement=Renderer.get().mutableMesh();final var out=replacement.emitter();
        template.forEach(quad->{
            out.copyFrom(quad);
            if(finder.find(quad)==original)for(int vertex=0;vertex<4;vertex++) {
                final float u=(quad.u(vertex)-original.getU0())/(original.getU1()-original.getU0());
                final float v=(quad.v(vertex)-original.getV0())/(original.getV1()-original.getV0());
                out.uv(vertex,target.getU(u),target.getV(v));
                out.animated(target.isAnimated());
            }
            out.emit();
        });
        return replacement.immutableCopy();
    }
    private static TextureAtlasSprite spriteOnSide(BlockState state,Direction side) {
        final var model=Minecraft.getInstance().getModelManager().getBlockStateModelSet().get(state);
        final var parts=new ArrayList<BlockStateModelPart>();model.collectParts(RandomSource.create(42),parts);
        for(var part:parts){var quads=part.getQuads(side);if(!quads.isEmpty())return quads.getFirst().materialInfo().sprite();}
        for(var part:parts)for(var quad:part.getQuads(null))if(quad.direction()==side)return quad.materialInfo().sprite();
        return model.particleMaterial().sprite();
    }
}
