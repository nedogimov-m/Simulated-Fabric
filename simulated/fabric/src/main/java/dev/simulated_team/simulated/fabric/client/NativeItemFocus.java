package dev.simulated_team.simulated.fabric.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.joml.Vector3f;
import org.joml.Vector3fc;
import org.joml.Vector4f;
import java.util.HashMap;
import java.util.Map;

/** Actual hand-pass projection/pose capture. Reprojects a muzzle into world camera space at query time. */
public final class NativeItemFocus {
    private record Frame(Matrix4fc view,Matrix4fc projection) { }
    private record Focus(Vector3fc viewPoint,Matrix4fc projection) { }
    private static final ThreadLocal<Frame> FRAME=new ThreadLocal<>();
    private static final Map<Identifier,Focus> FOCUS=new HashMap<>();
    public static void begin(CameraRenderState camera,Matrix4fc handProjection){FRAME.set(new Frame(new Matrix4f(camera.viewRotationMatrix),new Matrix4f(handProjection)));}
    public static void end(){FRAME.remove();}
    public static void capture(Identifier id,PoseStack pose,Vector3fc local) {
        final var frame=FRAME.get();if(frame==null||id==null)return;
        final var point=pose.last().pose().transformPosition(local,new Vector3f());frame.view.transformPosition(point);
        FOCUS.put(id,new Focus(point,new Matrix4f(frame.projection)));
    }
    public static Vector3f reproject(Vector3fc handView,Matrix4fc handProjection,Matrix4fc worldProjection,Matrix4fc inverseView) {
        final var point=new Vector4f(handView,1);handProjection.transform(point);new Matrix4f(worldProjection).invert().transform(point);
        if(!Float.isFinite(point.w)||Math.abs(point.w)<1E-7f)return new Vector3f();
        final var relative=new Vector3f(point.x/point.w,point.y/point.w,point.z/point.w);inverseView.transformDirection(relative);
        return relative;
    }
    public static Vec3 relative(Identifier id) {
        final var focus=FOCUS.get(id);if(focus==null)return Vec3.ZERO;
        final var camera=Minecraft.getInstance().gameRenderer.mainCamera();final var inverseView=camera.getViewRotationMatrix(new Matrix4f()).invert();
        final var projection=camera.getViewRotationProjectionMatrix(new Matrix4f()).mul(inverseView);
        final var relative=reproject(focus.viewPoint,focus.projection,projection,inverseView);
        return new Vec3(relative.x,relative.y,relative.z);
    }
    public static Vec3 world(Identifier id){return Minecraft.getInstance().gameRenderer.mainCamera().position().add(relative(id));}
    public static void clear(){FOCUS.clear();FRAME.remove();}
    private NativeItemFocus(){ }
}
