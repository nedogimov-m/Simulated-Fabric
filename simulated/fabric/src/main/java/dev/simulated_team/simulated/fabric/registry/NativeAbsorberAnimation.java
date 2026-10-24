package dev.simulated_team.simulated.fabric.registry;

import net.minecraft.util.Mth;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;

/** Source fall/bounce and two mirrored mechanical links, copied before native model submission. */
public final class NativeAbsorberAnimation {
    public record Frame(float compression,float travel,Matrix4fc sponge,Matrix4fc hat,Matrix4fc pivot,Matrix4fc firstArm,Matrix4fc secondArm) {
        public Frame {sponge=new Matrix4f(sponge);hat=new Matrix4f(hat);pivot=new Matrix4f(pivot);firstArm=new Matrix4f(firstArm);secondArm=new Matrix4f(secondArm);}
    }
    public static Frame extract(float timer,float target,float yRotation) {
        float position=timer;
        if(target>.5f) {
            final float fallTime=.3f;
            if(position<fallTime)position=1-position*position/(fallTime*fallTime);
            else {
                position=(position-fallTime)/(1-fallTime);
                float bounce=(float)(Math.exp(-position*4)*Math.sin(position*Math.PI*3));
                final float smoothing=.05f;bounce=(float)Math.sqrt(bounce*bounce+smoothing*smoothing)-smoothing;position=bounce/2;
            }
        } else {position=1-position;position*=Mth.lerp(position,2,1);}
        final float travel=(1+(1-position)*8)/16;
        final var sponge=new Matrix4f().translate(0,.25f,0).scale(1,1-position*8/9,1);
        final var hat=part(yRotation,travel,new Matrix4f());final var pivot=part(yRotation,travel/2,new Matrix4f());
        final float length=13.8f/32,height=(travel/2+.5f/16)/length,width=(float)Math.sqrt(Math.max(0,1-height*height));
        final var linkage=new Matrix4f().m22(width).m21(height).m11(width).m12(-height);
        final var first=part(yRotation,travel/2,linkage);
        linkage.m21(-height).m12(height).m00(.98f);
        return new Frame(position,travel,sponge,hat,pivot,first,part(yRotation,travel/2,linkage));
    }
    private static Matrix4f part(float yRotation,float offset,Matrix4fc rotation) {
        return new Matrix4f().translate(.5f,.25f+offset,.5f).rotateY(yRotation).mul(rotation).translate(-.5f,0,-.5f);
    }
    private NativeAbsorberAnimation() { }
}
