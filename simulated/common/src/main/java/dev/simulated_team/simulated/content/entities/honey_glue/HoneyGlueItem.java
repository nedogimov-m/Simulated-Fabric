package dev.simulated_team.simulated.content.entities.honey_glue;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Glue entity spawning is done via {@link HoneyGlueClientHandler}
 */
public class HoneyGlueItem extends Item {

    public HoneyGlueItem(final Properties properties) {
        super(properties);
    }

    @Override
    public boolean canDestroyBlock(final net.minecraft.world.item.ItemStack stack,final BlockState pState,final Level pLevel,final BlockPos pPos,final net.minecraft.world.entity.LivingEntity user) {
        return false;
    }
}
