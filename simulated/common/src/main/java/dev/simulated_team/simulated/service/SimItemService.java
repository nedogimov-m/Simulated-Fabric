package dev.simulated_team.simulated.service;

import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;

public interface SimItemService {

    SimItemService INSTANCE = ServiceUtil.load(SimItemService.class);

    static DyeColor getDyeColor(final ItemStack itemStack) {
        return itemStack.get(net.minecraft.core.component.DataComponents.DYE);
    }

    int getBurnTime(final ItemStack stack);
    int getBurnTime(final ItemStack stack, final net.minecraft.world.level.block.entity.BlockEntity owner);

    int getSuperheatedBurnTime(final ItemStack stack);
    int getSuperheatedBurnTime(final ItemStack stack, final net.minecraft.world.level.block.entity.BlockEntity owner);

    default boolean canFuel(final ItemStack stack, final net.minecraft.world.level.block.entity.BlockEntity owner) {
        return owner.getLevel() instanceof net.minecraft.server.level.ServerLevel ? getBurnTime(stack, owner) > 0
                : stack.has(net.minecraft.core.component.DataComponents.COOKING_FUEL);
    }
}
