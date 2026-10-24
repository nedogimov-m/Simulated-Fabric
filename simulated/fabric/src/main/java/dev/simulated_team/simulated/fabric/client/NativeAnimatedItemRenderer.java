package dev.simulated_team.simulated.fabric.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.content.equipment.EquipmentItemRenderer;
import net.fabricmc.fabric.api.client.model.loading.v1.ModelLoadingPlugin;
import net.fabricmc.fabric.api.client.model.loading.v1.SimpleUnbakedExtraModel;
import net.fabricmc.fabric.api.client.renderer.v1.Renderer;
import net.fabricmc.fabric.api.client.renderer.v1.mesh.Mesh;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.resources.model.geometry.ItemQuads;
import net.minecraft.client.renderer.item.ModelRenderProperties;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.ItemOwner;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector3fc;
import java.util.*;

/** Namespace-aware native equipment registrar using the unchanged Create native extraction/draw API. */
public abstract class NativeAnimatedItemRenderer extends EquipmentItemRenderer {
    public record Effect(Mesh mesh,int light) { }
    public static class AnimatedState extends State {
        public final Map<Draw,Effect> effects=new IdentityHashMap<>();
        public Identifier focusId;
        public Vector3fc focus;
    }
    private final Map<ItemQuads,Map<RenderType,Mesh>> effects=new IdentityHashMap<>();
    private ModelRenderProperties properties;
    protected State extractForOwner(ItemStack stack, ItemDisplayContext display, float partial, float time, ItemOwner owner) {
        return extract(stack, display, partial, time);
    }
    public final void register(Identifier id) {
        ModelLoadingPlugin.register(context->{
            models.forEach((name,key)->context.addModel(key,SimpleUnbakedExtraModel.blockStateModel(Identifier.parse(modelPath(name)))));
            context.modifyItemModelAfterBake().register((original,bake)->{
                if(!bake.itemId().equals(id))return original;
                final var resolved=bake.bakingContext().blockModelBaker().getModel(Identifier.fromNamespaceAndPath(id.getNamespace(),"item/"+id.getPath()));
                final var renderProperties=ModelRenderProperties.fromResolvedModel(bake.bakingContext().blockModelBaker(),resolved,resolved.getTopTextureSlots());
                this.properties=renderProperties;this.effects.clear();
                final var transform=new Matrix4f(bake.transformation());final var identity=new Object();
                return (renderState,stack,resolver,display,level,owner,seed)->{
                    final var client=Minecraft.getInstance();final float partial=client.getDeltaTracker().getGameTimeDeltaPartialTick(false);
                    final float time=level==null?partial:level.getGameTime()+partial;
                    final var state=extractForOwner(stack,display,partial,time,owner);state.display=display;finish(state);
                    renderState.appendModelIdentityElement(identity);renderState.appendModelIdentityElement(state);renderState.setAnimated();
                    final var layer=renderState.newLayer();renderProperties.applyToLayer(layer,display);layer.setLocalTransform(transform);
                    layer.setFoilType(stack.hasFoil()?ItemStackRenderState.FoilType.STANDARD:ItemStackRenderState.FoilType.NONE);
                    layer.setupSpecialModel(this,state);layer.setExtents(()->state.extents);
                };
            });
        });
    }
    @Override public Matrix4f cameraTransform(ItemDisplayContext context) {
        final var pose=new PoseStack();if(properties==null)throw new IllegalStateException("Simulated item properties have not baked");
        properties.transforms().getTransform(context).apply(context.leftHand(),pose.last());return new Matrix4f(pose.last().pose());
    }
    private static void finish(State state) {
        final var min=new Vector3f(Float.POSITIVE_INFINITY);final var max=new Vector3f(Float.NEGATIVE_INFINITY);final var point=new Vector3f();
        for(var draw:state.draws)for(var quad:draw.quads().all())for(int vertex=0;vertex<4;vertex++) {
            draw.transform().transformPosition(quad.position(vertex),point);point.add(.5f,.5f,.5f);min.min(point);max.max(point);
        }
        if(!Float.isFinite(min.x)){min.set(0);max.set(1);}
        state.extents=new Vector3fc[]{new Vector3f(min),new Vector3f(max),new Vector3f(min.x,min.y,max.z),new Vector3f(min.x,max.y,min.z),
                new Vector3f(max.x,min.y,min.z),new Vector3f(max.x,max.y,min.z),new Vector3f(max.x,min.y,max.z),new Vector3f(min.x,max.y,max.z)};
    }
    protected final void glowing(AnimatedState state,String model,PoseStack pose,RenderType type,int light) {
        draw(state,model,pose);final var draw=state.draws.getLast();
        final var mesh=effects.computeIfAbsent(draw.quads(),key->new IdentityHashMap<>()).computeIfAbsent(type,key->{
            final var mutable=Renderer.get().mutableMesh();final var out=mutable.emitter();
            for(var quad:draw.quads().all())out.fromBakedQuad(quad).emissive(true).itemRenderType(type).emit();
            return mutable.immutableCopy();
        });
        state.effects.put(draw,new Effect(mesh,light));
    }
    protected PoseStack poseForDraw(State state, Draw draw, PoseStack parent) {
        final var pose = new PoseStack(); pose.mulPose(parent.last().pose()); pose.mulPose(draw.transform()); return pose;
    }
    @Override public void submit(State state,PoseStack pose,SubmitNodeCollector collector,int light,int overlay,boolean foil,int outline) {
        if(!(state instanceof AnimatedState animated)){super.submit(state,pose,collector,light,overlay,foil,outline);return;}
        pose.pushPose();pose.translate(.5,.5,.5);
        if(state.display.firstPerson()&&animated.focus!=null)NativeItemFocus.capture(animated.focusId,pose,animated.focus);
        for(var draw:state.draws) {
            final var itemPose=poseForDraw(state,draw,pose);final var effect=animated.effects.get(draw);
            if(effect==null)collector.submitItem(itemPose,state.display,light,overlay,outline,new int[0],draw.quads(),foil?ItemStackRenderState.FoilType.STANDARD:ItemStackRenderState.FoilType.NONE);
            else collector.submitItem(itemPose,state.display,effect.light,overlay,outline,new int[0],ItemQuads.EMPTY,effect.mesh,foil?ItemStackRenderState.FoilType.STANDARD:ItemStackRenderState.FoilType.NONE);
        }
        pose.popPose();
    }
}
