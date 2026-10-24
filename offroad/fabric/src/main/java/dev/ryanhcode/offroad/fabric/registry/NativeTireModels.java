package dev.ryanhcode.offroad.fabric.registry;

import com.google.gson.JsonParseException;
import com.simibubi.create.AllPartialModels;
import com.simibubi.create.foundation.model.PartialModelRenderState;
import dev.ryanhcode.offroad.Offroad;
import dev.ryanhcode.offroad.index.OffroadPartialModels;
import foundry.veil.api.client.util.NativeBlockModelBaker;
import net.fabricmc.fabric.api.client.model.loading.v1.ExtraModelKey;
import net.fabricmc.fabric.api.client.model.loading.v1.ModelLoadingPlugin;
import net.fabricmc.fabric.api.client.model.loading.v1.SimpleUnbakedExtraModel;
import net.fabricmc.fabric.api.client.renderer.v1.Renderer;
import net.fabricmc.fabric.api.client.renderer.v1.mesh.Mesh;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.resources.Identifier;
import net.minecraft.util.RandomSource;
import org.joml.Matrix4f;

import java.io.UncheckedIOException;
import java.util.LinkedHashMap;
import java.util.Map;

/** Native model references for the tire component's optional custom block geometry. */
public final class NativeTireModels {
    private static final Map<Identifier, ExtraModelKey<BlockStateModel>> MODELS = new LinkedHashMap<>();
    private static final Map<Identifier, ExtraModelKey<BlockStateModel>> EXTRA = new LinkedHashMap<>();
    private static final Map<Identifier, BlockStateModel> COMPONENT_MODELS = new LinkedHashMap<>();
    private static final Map<Identifier, CachedMesh> MESHES = new LinkedHashMap<>();
    private static Object reloadGeneration;
    private static boolean initialized;
    private record CachedMesh(BlockStateModel model, Mesh mesh) {}
    static {
        MODELS.put(Identifier.fromNamespaceAndPath("offroad", "block/rockcutting_wheel/wheel"), OffroadPartialModels.ROCK_CUTTING_WHEEL_WHEEL);
        MODELS.put(Identifier.fromNamespaceAndPath("create", "block/mechanical_roller/wheel"), AllPartialModels.ROLLER_WHEEL);
    }

    /** Register additional component model references during client initialization, before the model bake. */
    public static synchronized ExtraModelKey<BlockStateModel> register(Identifier id) {
        return MODELS.computeIfAbsent(id, key -> {
            final var nativeKey = ExtraModelKey.<BlockStateModel>create(() -> "Tire model " + key);
            EXTRA.put(key, nativeKey);
            return nativeKey;
        });
    }
    public static synchronized void init(Iterable<Identifier> defaultModels) {
        if (initialized) return;
        defaultModels.forEach(NativeTireModels::register);
        ModelLoadingPlugin.register(context -> {
            final Map<Identifier, ExtraModelKey<BlockStateModel>> extra;
            synchronized (NativeTireModels.class) {
                extra = Map.copyOf(EXTRA);
            }
            extra.forEach((id, key) -> context.addModel(key, SimpleUnbakedExtraModel.blockStateModel(id)));
        });
        initialized = true;
    }
    /** Returns a declared extra-model key; component-only IDs use getModel or extract instead. */
    public static synchronized ExtraModelKey<BlockStateModel> get(Identifier id) {
        final var key = MODELS.get(id);
        if (key == null) throw new IllegalStateException("Tire model has no declared extra-model key; use getModel for component resources: " + id);
        return key;
    }
    /**
     * Resolves only the requested tire resource. A component received from a server after reload
     * can name any concrete block-atlas model, without registering unrelated resource roots.
     */
    public static synchronized BlockStateModel getModel(Identifier id) {
        final var client = Minecraft.getInstance();
        final var manager = client.getModelManager();
        final var generation = manager.getBlockStateModelSet();
        if (reloadGeneration != generation) {
            COMPONENT_MODELS.clear(); MESHES.clear(); reloadGeneration = generation;
        }
        final var key = MODELS.get(id);
        if (key != null) {
            final var declared = manager.getModel(key);
            if (declared != null) return declared;
        }
        return COMPONENT_MODELS.computeIfAbsent(id, model -> {
            try {
                return NativeBlockModelBaker.bakeBlockStateModel(model, client.getResourceManager(), client.getAtlasManager());
            } catch (UncheckedIOException | JsonParseException | IllegalArgumentException failure) {
                // Cache this native fallback like a successful bake, so extraction warns only once
                // for the asset until a reload makes repaired resources available.
                Offroad.LOGGER.warn("Cannot bake tire component model {}; using native missing model: {}", model, failure.toString());
                return manager.getBlockStateModelSet().missingModel();
            }
        });
    }
    /** Detached native Fabric geometry consumed by the wheel renderer's normal extraction path. */
    public static synchronized PartialModelRenderState extract(Identifier id) {
        final var model = getModel(id);
        final var state = new PartialModelRenderState();
        final boolean translucent = model.hasMaterialFlag(BakedQuad.FLAG_TRANSLUCENT);
        final var parts = state.setupModel(new Matrix4f(), translucent);
        model.collectParts(RandomSource.create(42), parts);
        var cached = MESHES.get(id);
        if (cached == null || cached.model != model) {
            final var mesh = Renderer.get().mutableMesh();
            final var emitter = mesh.emitter();
            parts.forEach(part -> part.emitQuads(emitter, direction -> false));
            cached = new CachedMesh(model, mesh.immutableCopy()); MESHES.put(id, cached);
        }
        state.mesh = cached.mesh; state.translucent = translucent;
        return state;
    }
    private NativeTireModels() {}
}
