package dev.simulated_team.simulated.fabric.service;

import dev.simulated_team.simulated.service.SimFluidService;
import net.fabricmc.fabric.api.transfer.v1.context.ContainerItemContext;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidConstants;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidStorage;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;

public final class FabricSimFluidService implements SimFluidService {
    @Override
    public long mbToLoaderUnits(long mb) {
        return Math.multiplyExact(mb, FluidConstants.BUCKET / 1_000);
    }

    @Override
    public Fluid getFluidInItem(ItemStack stack) {
        if (stack.isEmpty()) {
            return Fluids.EMPTY;
        }
        final var storage = ContainerItemContext.withConstant(stack.copyWithCount(1)).find(FluidStorage.ITEM);
        if (storage != null) {
            for (final var view : storage.nonEmptyViews()) {
                if (view.getAmount() > 0) {
                    return view.getResource().getFluid();
                }
            }
        }
        return Fluids.EMPTY;
    }
}
