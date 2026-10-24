package dev.eriksonn.aeronautics.mixin.render.native_capture;
import dev.eriksonn.aeronautics.fabric.registry.NativeLevititeGeometryHolder;
import dev.eriksonn.aeronautics.fabric.registry.NativeLevititeMesh;
import net.minecraft.client.renderer.chunk.CompiledSectionMesh;
import net.minecraft.client.renderer.chunk.SectionCompiler;
import net.minecraft.client.renderer.chunk.TranslucencyPointOfView;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(CompiledSectionMesh.class)
public abstract class NativeLevititeCompiledMeshMixin implements NativeLevititeGeometryHolder {
    @Unique private NativeLevititeMesh aeronautics$geometry;
    @Inject(method="<init>",at=@At("TAIL"),require=1)
    private void aeronautics$inheritCompiledGeometry(TranslucencyPointOfView point,SectionCompiler.Results results,long start,CallbackInfo ci) {
        aeronautics$geometry=((NativeLevititeGeometryHolder)(Object)results).aeronautics$levititeMesh();
    }
    @Override public NativeLevititeMesh aeronautics$levititeMesh(){return aeronautics$geometry;}
    @Override public void aeronautics$levititeMesh(NativeLevititeMesh mesh){aeronautics$geometry=mesh;}
}
