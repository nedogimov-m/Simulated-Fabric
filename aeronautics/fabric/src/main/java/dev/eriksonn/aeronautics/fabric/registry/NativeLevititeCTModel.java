package dev.eriksonn.aeronautics.fabric.registry;

import com.simibubi.create.foundation.block.connected.CTSpriteShiftEntry;
import com.simibubi.create.foundation.block.connected.OmnidirectionalCT;
import dev.eriksonn.aeronautics.index.AeroSpriteShift;
import net.fabricmc.fabric.api.client.model.loading.v1.ModelLoadingPlugin;
import net.fabricmc.fabric.api.client.model.loading.v1.wrapper.WrapperBlockStateModel;
import net.fabricmc.fabric.api.client.renderer.v1.mesh.QuadEmitter;
import net.fabricmc.fabric.api.client.renderer.v1.sprite.SpriteFinder;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Arrays;
import java.util.List;
import java.util.function.Predicate;

/** The original SimpleCTBehaviour uses the native Create omnidirectional context and 8x8 sheet. */
public final class NativeLevititeCTModel extends WrapperBlockStateModel {
    private final SpriteFinder finder;
    private final TextureAtlasSprite original;
    private final TextureAtlasSprite connected;

    private NativeLevititeCTModel(BlockStateModel wrapped, SpriteFinder finder,
                                  TextureAtlasSprite original, TextureAtlasSprite connected) {
        super(wrapped);
        this.finder = finder;
        this.original = original;
        this.connected = connected;
    }

    public static void register() {
        ModelLoadingPlugin.register(context -> context.modifyBlockModelAfterBake().register((model, bake) -> {
            final CTSpriteShiftEntry descriptor;
            final var id = BuiltInRegistries.BLOCK.getKey(bake.state().getBlock());
            if (!id.getNamespace().equals("aeronautics")) return model;
            if (id.getPath().equals("levitite")) descriptor = AeroSpriteShift.LEVITITE;
            else if (id.getPath().equals("pearlescent_levitite")) descriptor = AeroSpriteShift.PEARLESCENT_LEVITITE;
            else return model;
            final var materials = bake.baker().materials();
            return new NativeLevititeCTModel(model, materials.spriteFinder(net.minecraft.data.AtlasIds.BLOCKS),
                    materials.get(new Material(descriptor.original()), () -> "Levitite source sprite").sprite(),
                    materials.get(new Material(descriptor.connected()), () -> "Levitite connected sheet").sprite());
        }));
    }

    private static List<Integer> tiles(BlockAndTintGetter level, BlockPos pos, BlockState state) {
        return Arrays.stream(Direction.values()).map(face -> OmnidirectionalCT.build(level, pos, state, face).textureIndex()).toList();
    }
    private record GeometryKey(NativeLevititeCTModel owner, Object wrappedKey, List<Integer> tiles) {}

    @Override
    public Object createGeometryKey(BlockAndTintGetter level, BlockPos pos, BlockState state, RandomSource random) {
        final Object key = this.wrapped.createGeometryKey(level, pos, state, random);
        return key == null ? null : new GeometryKey(this, key, tiles(level, pos, state));
    }

    @Override
    public void emitQuads(QuadEmitter out, BlockAndTintGetter level, BlockPos pos, BlockState state,
                           RandomSource random, Predicate<Direction> cullTest) {
        final var tiles = tiles(level, pos, state);
        out.pushTransform(quad -> {
            if (this.finder.find(quad) != this.original) return true;
            final int tile = tiles.get(quad.lightFace().ordinal());
            for (int vertex = 0; vertex < 4; vertex++) {
                final float u = (quad.u(vertex) - this.original.getU0()) / (this.original.getU1() - this.original.getU0());
                final float v = (quad.v(vertex) - this.original.getV0()) / (this.original.getV1() - this.original.getV0());
                quad.uv(vertex, this.connected.getU((u + tile % 8) / 8), this.connected.getV((v + tile / 8) / 8));
            }
            return true;
        });
        try { this.wrapped.emitQuads(out, level, pos, state, random, cullTest); }
        finally { out.popTransform(); }
    }
}
