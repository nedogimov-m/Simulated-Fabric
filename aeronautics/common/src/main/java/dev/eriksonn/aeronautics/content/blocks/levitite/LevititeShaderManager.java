package dev.eriksonn.aeronautics.content.blocks.levitite;

import dev.eriksonn.aeronautics.fabric.registry.NativeLevititePass;
import dev.eriksonn.aeronautics.index.AeroBlocks;
import dev.ryanhcode.sable.physics.config.block_properties.PhysicsBlockPropertyHelper;
import dev.ryanhcode.sable.physics.config.dimension_physics.DimensionPhysicsData;
import dev.ryanhcode.sable.sublevel.ClientSubLevel;
import dev.ryanhcode.sable.util.SableMathUtils;
import org.joml.Matrix3f;
import org.joml.Vector3d;
import org.joml.Vector3f;
import java.util.HashMap;
import java.util.Map;

/** Per-owner smoothing and immutable extracted native shader inputs. No global mutable shader state. */
public final class LevititeShaderManager {
    private static final double SMOOTHING_SPEED=.5;
    public static final Map<ClientSubLevel,LevititeShaderManager> managers=new HashMap<>();
    private final Vector3d linear=new Vector3d(),lastLinear=new Vector3d(),angular=new Vector3d(),lastAngular=new Vector3d();
    private final Vector3d gravity=new Vector3d();
    public static void tick(){managers.entrySet().removeIf(entry->{if(entry.getKey().isRemoved())return true;entry.getValue().tick(entry.getKey());return false;});}
    public static LevititeShaderManager getInstance(ClientSubLevel owner){return managers.computeIfAbsent(owner,key->new LevititeShaderManager());}
    private void tick(ClientSubLevel owner) {
        lastLinear.set(linear);lastAngular.set(angular);
        var motion=owner.logicalPose().position().sub(owner.lastPose().position(),new Vector3d());
        var pivot=owner.logicalPose().rotationPoint().sub(owner.lastPose().rotationPoint(),new Vector3d());
        owner.logicalPose().orientation().transform(pivot);motion.sub(pivot);
        var rotation=SableMathUtils.getAngularVelocity(owner.lastPose().orientation(),owner.logicalPose().orientation(),new Vector3d());
        linear.lerp(motion,SMOOTHING_SPEED);angular.lerp(rotation,SMOOTHING_SPEED);
        DimensionPhysicsData.getGravity(owner.getLevel(),owner.logicalPose().position(),gravity);
    }
    public NativeLevititePass.Motion extract(ClientSubLevel owner,float partialTick) {
        var pose=owner.renderPose(partialTick);
        var velocity=lastLinear.lerp(linear,partialTick,new Vector3d());
        var rotation=lastAngular.lerp(angular,partialTick,new Vector3d());
        var localGravity=new Vector3d(gravity);
        pose.orientation().transformInverse(velocity);pose.orientation().transformInverse(rotation);pose.orientation().transformInverse(localGravity);
        var position=pose.position();
        return motion(new Vector3f(velocity).mul(20),new Vector3f(rotation).mul(20),
                new Vector3f((float)(position.x()%10000),(float)(position.y()%10000),(float)(position.z()%10000)),
                new Matrix3f().set(pose.orientation()),new Vector3f(localGravity),true,
                (angular.lengthSquared()>1E-6||linear.lengthSquared()>1E-6)&&gravity.lengthSquared()>.001);
    }
    public static NativeLevititePass.Motion world(){return motion(new Vector3f(),new Vector3f(),new Vector3f(),new Matrix3f(),new Vector3f(),false,false);}
    private static NativeLevititePass.Motion motion(Vector3f linear,Vector3f angular,Vector3f position,Matrix3f orientation,Vector3f gravity,boolean sublevel,boolean ghosts) {
        var material=PhysicsBlockPropertyHelper.getFloatingMaterial(AeroBlocks.LEVITITE.defaultBlockState());
        float transition=material==null?1:(float)material.transitionSpeed();
        var slow=gravityMatrix(gravity,material==null?0:(float)material.slowVerticalFriction(),material==null?0:(float)material.slowHorizontalFriction());
        var fast=gravityMatrix(gravity,material==null?0:(float)material.fastVerticalFriction(),material==null?0:(float)material.fastHorizontalFriction());
        return new NativeLevititePass.Motion(linear,angular,position,orientation,gravity.length(),transition,slow,fast,sublevel,ghosts);
    }
    private static Matrix3f gravityMatrix(Vector3f g,float vertical,float horizontal) {
        var target=new Matrix3f();
        if(g.lengthSquared()>.00001f) {
            float scale=(horizontal-vertical)/g.dot(g);
            target.m00=g.x*g.x*scale;target.m01=g.y*g.x*scale;target.m02=g.z*g.x*scale;
            target.m10=g.x*g.y*scale;target.m11=g.y*g.y*scale;target.m12=g.z*g.y*scale;
            target.m20=g.x*g.z*scale;target.m21=g.y*g.z*scale;target.m22=g.z*g.z*scale;
        }
        target.m00-=horizontal;target.m11-=horizontal;target.m22-=horizontal;return target;
    }
    public static void clear(){managers.clear();}
    private LevititeShaderManager(){ }
}
