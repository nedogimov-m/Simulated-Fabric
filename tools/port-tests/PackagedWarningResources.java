import com.google.gson.GsonBuilder;
import dev.ryanhcode.offroad.content.blocks.wheel_mount.WheelMountBlockEntity;
import dev.ryanhcode.offroad.content.blocks.wheel_mount.WheelMountInventory;
import dev.ryanhcode.offroad.content.components.TireLike;
import dev.ryanhcode.offroad.fabric.registry.NativeTireModels;
import dev.ryanhcode.offroad.index.OffroadBlocks;
import dev.ryanhcode.offroad.index.OffroadDataComponents;
import dev.simulated_team.simulated.fabric.registry.NativePartsRenderer;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.renderer.texture.MissingTextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;
import net.fabricmc.fabric.api.client.renderer.v1.Renderer;
import net.fabricmc.fabric.api.client.renderer.v1.sprite.SpriteFinder;
import net.minecraft.data.AtlasIds;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

/** Only the changed exact-ID tire bake, server component and native resource reload boundary. */
public final class PackagedWarningResources {
    private static final Identifier CUSTOM = Identifier.parse("port_warning_test:component_tire");
    private static final Identifier CUSTOM_MESH = Identifier.parse("port_warning_test:component_mesh_tire");
    private static final BlockPos MOUNT = new BlockPos(2, 120, 2);
    public static void run(ClientGameTestContext test, TestSingleplayerContext world, List<String> checks) throws Exception {
        if (System.getProperty("port.smoke.warningChecks", "valid").equals("invalid")) { runInvalid(test, checks); return; }
        Map<String,Object> report = new LinkedHashMap<>();
        var path = Path.of(System.getProperty("port.smoke.report")).resolveSibling("warning-resources.json");
        var first = new AtomicReference<BlockStateModel>();
        var firstMesh = new AtomicReference<Object>();
        var reload = new AtomicReference<CompletableFuture<Void>>();
        try {
            world.getServer().runCommand("tp @a 4 123 -6");
            world.getServer().runOnServer(server -> {
                var level = server.overworld(); level.getChunk(MOUNT);
                check(level.setBlock(MOUNT, OffroadBlocks.WHEEL_MOUNT.defaultBlockState(), 3), "Real wheel mount placement failed");
                var entity = (WheelMountBlockEntity) level.getBlockEntity(MOUNT);
                var stack = new ItemStack(Items.COBBLESTONE);
                stack.set(OffroadDataComponents.TIRE, new TireLike(1, CUSTOM_MESH));
                try {
                    var field = WheelMountBlockEntity.class.getDeclaredField("inventory"); field.setAccessible(true);
                    ((WheelMountInventory) field.get(entity)).slot.setStack(stack);
                } catch (ReflectiveOperationException e) { throw new RuntimeException(e); }
                entity.notifyUpdate();
            });
            test.waitFor(client -> client.level.getBlockEntity(MOUNT) instanceof WheelMountBlockEntity entity
                    && entity.getHeldItem().get(OffroadDataComponents.TIRE) != null
                    && entity.getHeldItem().get(OffroadDataComponents.TIRE).model().filter(CUSTOM_MESH::equals).isPresent(), 400);
            checks.add("real server-provided TireLike component received by native wheel block entity with an unregistered third-namespace model ID");
            test.runOnClient(client -> {
                report.put("initial", inspect(client));
                first.set(NativeTireModels.getModel(CUSTOM_MESH)); firstMesh.set(NativeTireModels.extract(CUSTOM_MESH).mesh);
                check(first.get() == NativeTireModels.getModel(CUSTOM_MESH), "Requested model was not cached");
                check(firstMesh.get() == NativeTireModels.extract(CUSTOM_MESH).mesh, "Detached geometry was not cached");
                checks.add("exact-ID native parent/texture resolution, real wheel extraction and cached immutable geometry");
                reload.set(client.reloadResourcePacks());
            });
            test.waitFor(client -> reload.get() != null && reload.get().isDone(), 400);
            test.runOnClient(client -> {
                reload.get().join(); report.put("afterReload", inspect(client));
                check(first.get() != NativeTireModels.getModel(CUSTOM_MESH), "Native reload retained an old lazy model");
                check(firstMesh.get() != NativeTireModels.extract(CUSTOM_MESH).mesh, "Native reload retained an old tire mesh");
                checks.add("actual native resource reload replaces lazy model and mesh while preserving registered tire partials and live wheel rendering");
            });
            report.put("status", "passed");
        } catch (Throwable e) {
            report.put("status", "failed"); report.put("error", e.toString()); throw e;
        } finally { Files.writeString(path, new GsonBuilder().setPrettyPrinting().create().toJson(report)); }
    }
    @SuppressWarnings("unchecked")
    private static Map<String,Object> inspect(Minecraft client) {
        var results = new LinkedHashMap<String,Object>();
        for (var id : List.of(TireLike.ROCKCUTTING_WHEEL.model().orElseThrow(), TireLike.MECHANICAL_ROLLER.model().orElseThrow(), CUSTOM, CUSTOM_MESH)) {
            var model = NativeTireModels.getModel(id); check(model != null, "No actual tire model: " + id);
            if (!id.equals(CUSTOM) && !id.equals(CUSTOM_MESH)) check(model == ((net.fabricmc.fabric.api.client.model.loading.v1.FabricModelManager) client.getModelManager()).getModel(NativeTireModels.get(id)), "Builtin partial was replaced: " + id);
            var parts = new ArrayList<BlockStateModelPart>(); model.collectParts(RandomSource.create(42), parts);
            Set<String> sprites = new TreeSet<>(); int[] quads = {0};
            var independent = Renderer.get().mutableMesh();
            parts.forEach(part -> ((net.fabricmc.fabric.api.client.renderer.v1.model.FabricBlockStateModelPart) part).emitQuads(independent.emitter(), face -> false));
            var finder = ((net.fabricmc.fabric.api.client.renderer.v1.sprite.FabricTextureAtlas) client.getAtlasManager().getAtlasOrThrow(AtlasIds.BLOCKS)).spriteFinder();
            independent.immutableCopy().forEach(quad -> {
                var sprite = finder.find(quad).contents().name();
                check(!sprite.equals(MissingTextureAtlasSprite.getLocation()), "Missing sprite in actual tire " + id);
                sprites.add(sprite.toString()); quads[0]++;
                for (int i = 0; i < 4; i++) {
                    check(Float.isFinite(quad.x(i)) && Float.isFinite(quad.y(i)) && Float.isFinite(quad.z(i)), "Non-finite native geometry: " + id);
                }
            });
            check(quads[0] > 0, "Empty tire geometry: " + id);
            if (id.equals(TireLike.ROCKCUTTING_WHEEL.model().orElseThrow())) check(quads[0] == 64, "Authored rock wheel mesh face count changed");
            if (id.equals(CUSTOM_MESH)) check(quads[0] == 1, "Registered Create mesh loader did not preserve the custom component face");
            var extracted = NativeTireModels.extract(id); AtomicInteger nativeQuads = new AtomicInteger();
            extracted.mesh.forEach(quad -> nativeQuads.incrementAndGet());
            check(nativeQuads.get() == quads[0], "Detached Fabric mesh lost tire quads: " + id);
            if (id.equals(CUSTOM) || id.equals(CUSTOM_MESH)) check(sprites.equals(Set.of("minecraft:block/stone")), "Custom parent/texture override was not resolved: " + sprites);
            results.put(id.toString(), Map.of("quads", quads[0], "nativeMeshQuads", nativeQuads.get(), "sprites", sprites));
        }
        var entity = (WheelMountBlockEntity) client.level.getBlockEntity(MOUNT);
        NativePartsRenderer<WheelMountBlockEntity> renderer = (NativePartsRenderer<WheelMountBlockEntity>) (Object) client.getBlockEntityRenderDispatcher().getRenderer(entity);
        check(renderer != null, "Actual native wheel renderer missing");
        var state = renderer.createRenderState(); renderer.extractRenderState(entity, state, 0, Vec3.ZERO, null);
        try {
            var field = NativePartsRenderer.State.class.getDeclaredField("parts"); field.setAccessible(true);
            var entries = (List<NativePartsRenderer.Part>) field.get(state);
            check(entries.stream().anyMatch(p -> p.mesh() == NativeTireModels.extract(CUSTOM_MESH).mesh), "Real wheel extraction did not use the server component's custom model");
            results.put("nativeWheelParts", entries.size());
        } catch (ReflectiveOperationException e) { throw new RuntimeException(e); }
        return results;
    }
    private static void check(boolean ok, String message) { if (!ok) throw new AssertionError(message); }
    private static void runInvalid(ClientGameTestContext test, List<String> checks) throws Exception {
        var report = new LinkedHashMap<String,Object>();
        var path = Path.of(System.getProperty("port.smoke.report")).resolveSibling("warning-invalid-models.json");
        try {
            test.runOnClient(client -> {
                for (String name : List.of("bad_json", "bad_parent", "cyclic_parent")) {
                    var id = Identifier.parse("port_warning_test:" + name);
                    var model = NativeTireModels.getModel(id);
                    check(model != null && model == NativeTireModels.getModel(id), "Malformed model fallback not cached: " + id);
                    var first = NativeTireModels.extract(id);
                    check(first.mesh != null && first.mesh == NativeTireModels.extract(id).mesh, "Malformed native mesh fallback not cached: " + id);
                    AtomicInteger quads = new AtomicInteger(); first.mesh.forEach(quad -> quads.incrementAndGet());
                    check(quads.get() > 0, "Missing-model fallback is empty: " + id);
                    report.put(name, Map.of("nativeMeshQuads", quads.get(), "cachedModel", true, "cachedMesh", true));
                }
                checks.add("only new malformed JSON, missing-parent and cyclic-parent exact-ID component paths use cached native missing-model geometry without render exceptions");
            });
            report.put("status", "passed");
        } catch (Throwable failure) { report.put("status", "failed"); report.put("error", failure.toString()); throw failure; }
        finally { Files.writeString(path, new GsonBuilder().setPrettyPrinting().create().toJson(report)); }
    }
}
