package dev.ryanhcode.offroad.content.blocks.rock_cutting_wheel;

import dev.ryanhcode.offroad.fabric.registry.NativeRockCuttingWheelGeometry;
import dev.simulated_team.simulated.fabric.registry.NativePartsRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

public class RockCuttingWheelRenderer extends NativePartsRenderer<RockCuttingWheelBlockEntity> {
    public RockCuttingWheelRenderer(BlockEntityRendererProvider.Context context) {}
    @Override protected void extractParts(RockCuttingWheelBlockEntity entity, float partial, Parts parts) {
        final var state = entity.getBlockState();
        NativeRockCuttingWheelGeometry.extract(parts, state.getValue(BlockStateProperties.FACING),
                state.getValue(RockCuttingWheelBlock.AXIS_ALONG_FIRST_COORDINATE),
                entity.isVirtual() ? entity.getAnimatedSpeed(partial) : 0);
    }
}
