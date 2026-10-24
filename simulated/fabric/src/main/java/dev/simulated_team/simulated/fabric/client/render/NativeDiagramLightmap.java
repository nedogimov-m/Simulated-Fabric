package dev.simulated_team.simulated.fabric.client.render;

import org.joml.Vector3f;
import java.nio.ByteBuffer;

/** Original warm diagram block-light/gamma curve, in native RGBA8 texture byte order. */
public final class NativeDiagramLightmap {
    public static ByteBuffer pixels(float multiplier) {
        if(!Float.isFinite(multiplier)||multiplier<0||multiplier>1)throw new IllegalArgumentException("Diagram brightness must be in 0..1");
        final var bytes=ByteBuffer.allocateDirect(16*16*4);
        final var color=new Vector3f();
        for(int sky=0;sky<16;sky++)for(int block=0;block<16;block++) {
            float f=block/15f;
            float brightness=f/(4-3*f)*.6f+.15f;
            float green=brightness*((brightness*.6f+.4f)*.6f+.4f);
            float blue=brightness*(brightness*brightness*.6f+.4f);
            color.set(brightness,green,blue).lerp(new Vector3f(.99f,1.12f,1),.25f);
            clamp(color);
            color.lerp(new Vector3f(notGamma(color.x),notGamma(color.y),notGamma(color.z)),.55f);
            color.lerp(new Vector3f(.75f),.04f);clamp(color);
            color.mul(255f).mul(multiplier);
            bytes.put((byte)(int)color.x).put((byte)(int)color.y).put((byte)(int)color.z).put((byte)255);
        }
        return bytes.flip().asReadOnlyBuffer();
    }
    private static float notGamma(float value){float inverse=1-value;return 1-inverse*inverse*inverse*inverse;}
    private static void clamp(Vector3f value){value.x=Math.clamp(value.x,0,1);value.y=Math.clamp(value.y,0,1);value.z=Math.clamp(value.z,0,1);}
    private NativeDiagramLightmap(){}
}
