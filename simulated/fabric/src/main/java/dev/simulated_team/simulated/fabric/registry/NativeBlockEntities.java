package dev.simulated_team.simulated.fabric.registry;

import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Arrays;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

/** Native BE factories supply the actual registered type to the existing three-argument constructors. */
public final class NativeBlockEntities {
    @FunctionalInterface
    public interface Factory<T extends BlockEntity> {
        T create(BlockEntityType<?> type, BlockPos position, BlockState state);
    }

    public static <T extends BlockEntity> BlockEntityType<T> register(Identifier id, Factory<T> factory, Block... blocks) {
        if (blocks.length == 0) throw new IllegalArgumentException("A block entity type needs valid blocks: " + id);
        final var self = new AtomicReference<BlockEntityType<T>>();
        final BlockEntityType<T> type = FabricBlockEntityTypeBuilder.<T>create((position, state) ->
                factory.create(Objects.requireNonNull(self.get(), "Block entity factory used before its type was registered"), position, state), blocks)
                .canPotentiallyExecuteCommands(false).build();
        self.set(type);
        return Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, id, type);
    }

    public static Block[] blocks(Block[]... groups) {
        return Arrays.stream(groups).flatMap(Arrays::stream).toArray(Block[]::new);
    }

    private NativeBlockEntities() {}
}
