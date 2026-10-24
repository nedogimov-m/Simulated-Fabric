package dev.eriksonn.aeronautics.mixin.render.native_capture;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import dev.eriksonn.aeronautics.fabric.registry.NativeLevititeRenderer;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.client.renderer.block.BlockQuadOutput;
import net.minecraft.client.renderer.block.ModelBlockRenderer;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
@Mixin(ModelBlockRenderer.class)
public abstract class NativeLevititeModelCaptureMixin {
    @WrapMethod(method="tesselateBlock")
    private void aeronautics$captureActualQuad(BlockQuadOutput output,float x,float y,float z,BlockAndTintGetter level,BlockPos pos,BlockState state,BlockStateModel model,long seed,Operation<Void> original) {
        final var capture=NativeLevititeRenderer.capture();
        if(capture==null){original.call(output,x,y,z,level,pos,state,model,seed);return;}
        BlockQuadOutput wrapped=(qx,qy,qz,quad,instance)->{output.put(qx,qy,qz,quad,instance);capture.quad(state,qx,qy,qz,quad,instance);};
        original.call(wrapped,x,y,z,level,pos,state,model,seed);
    }
}
