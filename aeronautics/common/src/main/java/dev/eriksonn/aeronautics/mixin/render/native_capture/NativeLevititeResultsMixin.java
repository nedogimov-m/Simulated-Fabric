package dev.eriksonn.aeronautics.mixin.render.native_capture;
import dev.eriksonn.aeronautics.fabric.registry.NativeLevititeGeometryHolder;
import dev.eriksonn.aeronautics.fabric.registry.NativeLevititeMesh;
import net.minecraft.client.renderer.chunk.SectionCompiler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
@Mixin(SectionCompiler.Results.class)
public abstract class NativeLevititeResultsMixin implements NativeLevititeGeometryHolder {
    @Unique private NativeLevititeMesh aeronautics$geometry;
    @Override public NativeLevititeMesh aeronautics$levititeMesh(){return aeronautics$geometry;}
    @Override public void aeronautics$levititeMesh(NativeLevititeMesh mesh){aeronautics$geometry=mesh;}
}
