package dev.simulated_team.simulated.mixin.rope;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.simulated_team.simulated.content.blocks.rope.strand.client.ZiplineClientManager;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.extract.LevelExtractor;
import net.minecraft.client.renderer.state.level.LevelRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LevelExtractor.class)
public class LevelRendererMixin {

    @Inject(method = "extractBlockOutline", at = @At("HEAD"), cancellable = true)
    private void sable$cancelBlockHitOutline(final Camera camera, final LevelRenderState state, final CallbackInfo ci) {
        if (ZiplineClientManager.hoveringRope != null) {
            state.blockOutlineRenderState = null;
            ci.cancel();
        }
    }
}
