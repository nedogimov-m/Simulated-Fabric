package dev.simulated_team.simulated.api;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import com.simibubi.create.content.contraptions.AssemblyException;

/** Native position storage also accepts the original compound X/Y/Z representation. */
public final class SimBlockEntityCodecs {
    private static final Codec<BlockPos> LEGACY_POSITION = RecordCodecBuilder.create(instance -> instance.group(
            Codec.INT.fieldOf("X").forGetter(BlockPos::getX),
            Codec.INT.fieldOf("Y").forGetter(BlockPos::getY),
            Codec.INT.fieldOf("Z").forGetter(BlockPos::getZ)).apply(instance, BlockPos::new));
    public static final Codec<BlockPos> BLOCK_POS = Codec.withAlternative(BlockPos.CODEC, LEGACY_POSITION);
    /** Older plunger saves used enum names, while native Direction storage uses lowercase names. */
    public static final Codec<Direction> DIRECTION = Codec.withAlternative(Direction.CODEC,
            Codec.STRING.comapFlatMap(name -> {
                try { return com.mojang.serialization.DataResult.success(Direction.valueOf(name)); }
                catch (final IllegalArgumentException failure) {
                    return com.mojang.serialization.DataResult.error(() -> "Unknown legacy direction: " + name);
                }
            }, Direction::name));
    /** Context-aware JSON string encoding keeps Create's original LastException payload intact. */
    private static final com.mojang.serialization.Codec<net.minecraft.network.chat.Component> COMPONENT_JSON = new com.mojang.serialization.Codec<>() {
        @Override public <T> com.mojang.serialization.DataResult<com.mojang.datafixers.util.Pair<net.minecraft.network.chat.Component, T>> decode(
                final com.mojang.serialization.DynamicOps<T> ops, final T input) {
            final com.mojang.serialization.DynamicOps<com.google.gson.JsonElement> json = ops instanceof final net.minecraft.resources.RegistryOps<?> registry
                    ? registry.withParent(com.mojang.serialization.JsonOps.INSTANCE) : com.mojang.serialization.JsonOps.INSTANCE;
            return ops.getStringValue(input).flatMap(text -> {
                try { return net.minecraft.network.chat.ComponentSerialization.CODEC.parse(json, com.google.gson.JsonParser.parseString(text))
                        .map(component -> com.mojang.datafixers.util.Pair.of(component, ops.empty())); }
                catch (final com.google.gson.JsonParseException failure) { return com.mojang.serialization.DataResult.error(failure::getMessage); }
            });
        }
        @Override public <T> com.mojang.serialization.DataResult<T> encode(final net.minecraft.network.chat.Component input,
                final com.mojang.serialization.DynamicOps<T> ops, final T prefix) {
            final com.mojang.serialization.DynamicOps<com.google.gson.JsonElement> json = ops instanceof final net.minecraft.resources.RegistryOps<?> registry
                    ? registry.withParent(com.mojang.serialization.JsonOps.INSTANCE) : com.mojang.serialization.JsonOps.INSTANCE;
            return net.minecraft.network.chat.ComponentSerialization.CODEC.encodeStart(json, input)
                    .flatMap(value -> ops.mergeToPrimitive(prefix, ops.createString(value.toString())));
        }
    };
    public static void writeAssemblyException(final net.minecraft.world.level.storage.ValueOutput output, final AssemblyException exception) {
        if (exception == null) return;
        final var data = output.child("LastException");
        data.store("Component", COMPONENT_JSON, exception.component);
        if (exception.hasPosition()) data.putLong("Position", exception.getPosition().asLong());
        if (exception.canRetry()) data.putBoolean("Retryable", true);
    }
    public static AssemblyException readAssemblyException(final net.minecraft.world.level.storage.ValueInput input) {
        final var tag = new net.minecraft.nbt.CompoundTag();
        input.read("LastException", net.minecraft.nbt.CompoundTag.CODEC).ifPresent(error -> tag.put("LastException", error));
        return AssemblyException.read(tag, input.lookup());
    }


    private SimBlockEntityCodecs() { }
}
