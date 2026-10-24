package dev.simulated_team.simulated.fabric.service;

import com.simibubi.create.api.registry.CreateDataMaps;
import dev.simulated_team.simulated.content.blocks.portable_engine.PortableEngineBlockEntity;
import dev.simulated_team.simulated.service.SimItemService;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CookingFuel;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.storage.loot.*;
import net.minecraft.world.level.storage.loot.parameters.*;
import net.minecraft.world.level.storage.loot.providers.number.ints.ResolvableInt;
import net.minecraft.world.phys.Vec3;
import java.util.Optional;

/** Native component fuel evaluation uses the actual server owner, inventory and position. */
public final class FabricSimItemService implements SimItemService {
    @Override public int getBurnTime(ItemStack stack) {
        final CookingFuel fuel = stack.get(DataComponents.COOKING_FUEL);
        if (fuel == null) return 0;
        if (fuel.burnTime() instanceof ResolvableInt.Constant value) return value.value();
        throw new IllegalStateException("Context-dependent cooking fuel requires its real server block entity");
    }
    @Override public int getBurnTime(ItemStack stack, BlockEntity owner) {
        if (stack.isEmpty()) return 0;
        if (!(owner.getLevel() instanceof ServerLevel level)) return getBurnTime(stack);
        final var inventory = owner instanceof PortableEngineBlockEntity engine ? engine.inventory
                : owner instanceof net.minecraft.world.Container container ? container : new SimpleContainer(stack.copyWithCount(1));
        final var parameters = new LootParams.Builder(level).withParameter(LootContextParams.BLOCK_STATE, owner.getBlockState())
                .withParameter(LootContextParams.BLOCK_ENTITY, owner).withParameter(LootContextParams.ORIGIN, Vec3.atCenterOf(owner.getBlockPos()))
                .withParameter(LootContextParams.CONTAINER, inventory).create(LootContextParamSets.CONTAINER_PROCESS);
        return ResolvableInt.getFromItem(stack, DataComponents.COOKING_FUEL, CookingFuel::burnTime,
                new LootContext.Builder(parameters).create(Optional.empty()), 0);
    }
    @Override public int getSuperheatedBurnTime(ItemStack stack) {
        throw new IllegalStateException("Create datapack fuel requires its real server block entity");
    }
    @Override public int getSuperheatedBurnTime(ItemStack stack, BlockEntity owner) {
        if (stack.isEmpty() || !(owner.getLevel() instanceof ServerLevel level)) return 0;
        final var fuel = CreateDataMaps.get(level.getServer(), CreateDataMaps.SUPERHEATED_BLAZE_BURNER_FUELS, stack.getItem());
        return fuel == null ? 0 : fuel.burnTime();
    }
}
