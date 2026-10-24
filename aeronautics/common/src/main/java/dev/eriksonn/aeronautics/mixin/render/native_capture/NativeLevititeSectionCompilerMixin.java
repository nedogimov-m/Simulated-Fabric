package dev.eriksonn.aeronautics.mixin.render.native_capture;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.mojang.blaze3d.vertex.VertexSorting;
import dev.eriksonn.aeronautics.fabric.registry.NativeLevititeRenderer;
import net.minecraft.client.renderer.SectionBufferBuilderPack;
import net.minecraft.client.renderer.chunk.RenderSectionRegion;
import net.minecraft.client.renderer.chunk.SectionCompiler;
import net.minecraft.core.SectionPos;
import org.spongepowered.asm.mixin.Mixin;
@Mixin(SectionCompiler.class)
public abstract class NativeLevititeSectionCompilerMixin {
    @WrapMethod(method="compile")
    private SectionCompiler.Results aeronautics$captureNativeLevitite(SectionPos section,RenderSectionRegion region,VertexSorting sorting,SectionBufferBuilderPack buffers,Operation<SectionCompiler.Results> original) {
        return NativeLevititeRenderer.compile(section,()->original.call(section,region,sorting,buffers));
    }
}
