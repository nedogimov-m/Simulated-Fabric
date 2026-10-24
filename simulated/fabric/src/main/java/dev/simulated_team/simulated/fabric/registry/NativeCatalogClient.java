package dev.simulated_team.simulated.fabric.registry;

import com.simibubi.create.foundation.item.ItemDescription;
import net.fabricmc.fabric.api.client.model.loading.v1.ModelLoadingPlugin;
import net.fabricmc.fabric.api.client.model.loading.v1.wrapper.WrapperBlockStateModel;
import net.fabricmc.fabric.api.client.renderer.v1.mesh.QuadEmitter;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;

import java.util.function.Predicate;

/** Client-only bindings for metadata captured by the real common registration catalog. */
public final class NativeCatalogClient {
    private static boolean registered;

    public static void register() {
        if (registered) return;
        NativeItemCatalog.tooltipKeys().forEach(ItemDescription::useKey);
        registerTooltips();
        ModelLoadingPlugin.register(context -> context.modifyBlockModelAfterBake().register((model, bake) ->
                NativeItemCatalog.isCutout(bake.state().getBlock()) ? new CutoutModel(model) : model));
        registered = true;
    }

    private static void registerTooltips() {
        for (NativeItemCatalog.Entry entry : NativeItemCatalog.entries()) {
            final var item = entry.item();
            if (!java.util.Set.of("simulated", "aeronautics", "offroad").contains(
                    net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(item).getNamespace())) continue;
            // Even CLIENT_STARTED precedes26.3's initial component reload. Resolve rarity
            // from the actual hovered stack, never from a startup-created default instance.
            com.simibubi.create.foundation.item.TooltipModifier.REGISTRY.register(item, context -> {
                final var rarity = context.stack().getRarity();
                var palette = net.createmod.catnip.lang.FontHelper.Palette.STANDARD_CREATE;
                if (rarity == net.minecraft.world.item.Rarity.EPIC) {
                    palette = new net.createmod.catnip.lang.FontHelper.Palette(
                            com.simibubi.create.foundation.item.TooltipHelper.styleFromColor(
                                    dev.simulated_team.simulated.util.SimColors.EPIC_OURPLE),
                            com.simibubi.create.foundation.item.TooltipHelper.styleFromColor(rarity.color()));
                }
                new ItemDescription.Modifier(item, palette).andThen(
                        com.simibubi.create.foundation.item.TooltipModifier.mapNull(
                                com.simibubi.create.foundation.item.KineticStats.create(item))).modify(context);
            });
        }
    }

    private static final class CutoutModel extends WrapperBlockStateModel {
        private CutoutModel(BlockStateModel wrapped) { super(wrapped); }
        private record Key(CutoutModel model, Object geometry) {}
        @Override
        public Object createGeometryKey(BlockAndTintGetter level, BlockPos pos, BlockState state, RandomSource random) {
            final Object geometry = this.wrapped.createGeometryKey(level, pos, state, random);
            return geometry == null ? null : new Key(this, geometry);
        }
        @Override
        public void emitQuads(QuadEmitter out, BlockAndTintGetter level, BlockPos pos, BlockState state,
                              RandomSource random, Predicate<Direction> cull) {
            out.pushTransform(quad -> { quad.chunkLayer(ChunkSectionLayer.CUTOUT); return true; });
            try { this.wrapped.emitQuads(out, level, pos, state, random, cull); }
            finally { out.popTransform(); }
        }
    }

    private NativeCatalogClient() {}
}
