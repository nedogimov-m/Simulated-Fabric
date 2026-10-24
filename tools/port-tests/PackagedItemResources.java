import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.simibubi.create.content.equipment.EquipmentItemRenderer;
import dev.simulated_team.simulated.content.items.plunger_launcher.PlungerLauncherItemRenderer;
import dev.simulated_team.simulated.content.physics_staff.PhysicsStaffItemRenderer;
import dev.simulated_team.simulated.index.SimPartialModels;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.fabricmc.fabric.api.client.model.loading.v1.ExtraModelKey;
import net.fabricmc.fabric.api.client.model.loading.v1.FabricModelManager;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.texture.MissingTextureAtlasSprite;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.resources.Identifier;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.properties.Property;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.atomic.AtomicInteger;

/** Actual registered names, baked partials and ordinary native GUI/hand rendering only. */
public final class PackagedItemResources {
    private static final Set<String> ADDONS = Set.of("simulated", "aeronautics", "offroad");
    private static final List<String> TOOLS = List.of("creative_physics_staff", "plunger_launcher");

    public static void runGuiOnly(ClientGameTestContext test, TestSingleplayerContext world, List<String> checks) throws Exception {
        var report = new LinkedHashMap<String, Object>();
        var locales = new LinkedHashMap<String, Object>();
        Path destination = Path.of(System.getProperty("port.smoke.report")).resolveSibling("item-gui.json");
        report.put("scope", "Only the previously unverified EN/RU GUI frames; waits for actual native loading overlay retirement and three extracted item-screen frames. Names, partials and hand scopes are not repeated.");
        report.put("locales", locales);
        try {
            for (String locale : List.of("en_us", "ru_ru")) {
                var reload = new AtomicReference<CompletableFuture<Void>>();
                test.runOnClient(client -> {
                    client.options.languageCode = locale;
                    client.getLanguageManager().setSelected(locale);
                    reload.set(client.reloadResourcePacks());
                });
                test.waitFor(client -> reload.get() != null && reload.get().isDone(), 600);
                test.runOnClient(client -> reload.get().join());
                test.waitFor(client -> client.gui.overlay() == null, 300);
                var screen = new AtomicReference<ItemScreen>();
                var extracted = new AtomicInteger();
                test.runOnClient(client -> {
                    client.options.guiScale().set(2);
                    var itemScreen = new ItemScreen(locale);
                    screen.set(itemScreen);
                    client.setScreenAndShow(itemScreen);
                    ScreenEvents.afterExtract(itemScreen).register((current, graphics, x, y, partial) -> {
                        if (client.gui.overlay() == null && client.gui.screen() == itemScreen) extracted.incrementAndGet();
                    });
                });
                test.waitFor(client -> client.gui.overlay() == null && client.gui.screen() == screen.get() && extracted.get() >= 3, 300);
                var screenshot = test.takeScreenshot("item-gui-only-" + locale);
                locales.put(locale, Map.of("overlayRetired", true, "actualItemScreenFrames", extracted.get(), "screenshot", screenshot.toString()));
                test.runOnClient(client -> client.setScreenAndShow(null));
            }
            checks.add("actual EN/RU native staff/launcher GUI screenshots after loading overlay retired and at least three extracted item-screen frames per locale");
            report.put("status", "passed");
        } catch (Exception | Error failure) {
            report.put("status", "failed"); report.put("error", failure.toString()); throw failure;
        } finally {
            Files.writeString(destination, new GsonBuilder().setPrettyPrinting().create().toJson(report));
        }
    }

    public static void run(ClientGameTestContext test, TestSingleplayerContext world, List<String> checks) throws Exception {
        var report = new LinkedHashMap<String, Object>();
        var locales = new LinkedHashMap<String, Object>();
        var models = new LinkedHashMap<String, Object>();
        var screenshots = new ArrayList<String>();
        Path destination = Path.of(System.getProperty("port.smoke.report")).resolveSibling("item-resources.json");
        report.put("scope", "Actual registered addon default stacks, native EN/RU resource reload and name components, baked equipment partials/quads/sprites, native GUI and first-person frames. No earlier gameplay scenes or activation batch.");
        report.put("locales", locales); report.put("models", models); report.put("screenshots", screenshots);
        try {
            test.runOnClient(client -> exportBlockRegistry(destination.resolveSibling("block-registry.json")));
            report.put("blockRegistry", destination.resolveSibling("block-registry.json").toString());
            JsonObject expected;
            try (var input = PackagedItemResources.class.getResourceAsStream("/item-resource-expectations.json")) {
                check(input != null, "Missing exact source-derived item name expectations");
                expected = JsonParser.parseReader(new InputStreamReader(input, StandardCharsets.UTF_8)).getAsJsonObject().getAsJsonObject("modules");
            }
            world.getServer().runCommand("gamemode creative @a");
            world.getServer().runCommand("time set noon");
            world.getServer().runCommand("weather clear");
            world.getServer().runCommand("tp @a 0 -59 0 0 0");
            for (String locale : List.of("en_us", "ru_ru")) {
                var reload = new AtomicReference<CompletableFuture<Void>>();
                test.runOnClient(client -> {
                    client.options.languageCode = locale;
                    client.getLanguageManager().setSelected(locale);
                    reload.set(client.reloadResourcePacks());
                });
                test.waitFor(client -> reload.get() != null && reload.get().isDone(), 600);
                test.runOnClient(client -> {
                    reload.get().join();
                    check(client.getLanguageManager().getSelected().equals(locale), "Native selected language did not change to " + locale);
                    locales.put(locale, verifyNames(client, locale, expected));
                    if (locale.equals("en_us")) verifyModels(client, models);
                    client.options.guiScale().set(2);
                    client.setScreenAndShow(new ItemScreen(locale));
                });
                frames(test, 12);
                screenshots.add(test.takeScreenshot("item-resources-gui-" + locale).toString());
                test.runOnClient(client -> client.setScreenAndShow(null));
            }
            var itemCount = ((Map<?, ?>) locales.get("en_us")).get("registeredItems");
            report.put("verifiedRegisteredItems", itemCount);
            checks.add("actual default ItemStacks for all " + itemCount + " registered Simulated/Aeronautics/Offroad items: ITEM_NAME keys, current Language/I18n, direct EN/RU pack entries and getHoverName after native language reload");
            checks.add("actual staff and launcher inherited equipment/SimPartialModels baked geometry: nonempty quads, finite vertices, real expected sprites and no missing-texture quads");
            for (String item : TOOLS) {
                var id = Identifier.fromNamespaceAndPath("simulated", item);
                world.getServer().runCommand("item replace entity @a weapon.mainhand with " + id);
                test.waitFor(client -> client.player != null && client.player.getMainHandItem().is(BuiltInRegistries.ITEM.getValue(id)), 200);
                test.runOnClient(client -> {
                    client.options.setCameraType(CameraType.FIRST_PERSON);
                    client.setCameraEntity(client.player);
                    client.setScreenAndShow(null);
                    var state = new ItemStackRenderState();
                    client.getItemModelResolver().updateForLiving(state, client.player.getMainHandItem(), ItemDisplayContext.FIRST_PERSON_RIGHT_HAND, client.player);
                    check(!state.isEmpty(), "Native first-person resolver produced an empty " + id);
                });
                frames(test, 35);
                screenshots.add(test.takeScreenshot("item-resources-first-person-" + item).toString());
            }
            checks.add("actual 1280x720 GUI EN/RU and first-person staff/launcher frames with real server-equipped items");
            report.put("status", "passed");
        } catch (Exception | Error failure) {
            report.put("status", "failed"); report.put("error", failure.toString()); throw failure;
        } finally {
            Files.writeString(destination, new GsonBuilder().setPrettyPrinting().create().toJson(report));
        }
    }

    private static Map<String, Object> verifyNames(Minecraft client, String locale, JsonObject expected) {
        var dictionaries = new LinkedHashMap<String, JsonObject>();
        for (String namespace : ADDONS) {
            var resource = client.getResourceManager().getResource(Identifier.fromNamespaceAndPath(namespace, "lang/" + locale + ".json")).orElseThrow();
            try (var reader = resource.openAsReader()) { dictionaries.put(namespace, JsonParser.parseReader(reader).getAsJsonObject()); }
            catch (java.io.IOException failure) { throw new RuntimeException(failure); }
        }
        var names = new TreeMap<String, Object>();
        var failures = new ArrayList<String>();
        var expectedIds = new LinkedHashSet<String>();
        expected.entrySet().forEach(module -> module.getValue().getAsJsonObject().getAsJsonObject("items").keySet().forEach(expectedIds::add));
        for (var entry : BuiltInRegistries.ITEM.entrySet()) {
            var id = entry.getKey().identifier();
            if (!ADDONS.contains(id.getNamespace())) continue;
            var stack = entry.getValue().getDefaultInstance();
            var component = stack.get(DataComponents.ITEM_NAME);
            var wanted = expected.getAsJsonObject(id.getNamespace()).getAsJsonObject("items").getAsJsonObject(id.toString());
            if (wanted == null) { failures.add("Registered item missing expectation: " + id); continue; }
            var key = wanted.get("key").getAsString();
            var localized = wanted.get(locale).getAsString();
            if (!(component != null && component.getContents() instanceof TranslatableContents translation && translation.getKey().equals(key)))
                failures.add(id + " ITEM_NAME component key: " + component + ", expected " + key);
            var dictionary = dictionaries.get(id.getNamespace());
            if (!dictionary.has(key) || !dictionary.get(key).getAsString().equals(localized)) failures.add(id + " direct " + locale + " resource key/value mismatch: " + key);
            if (!Language.getInstance().has(key) || !I18n.get(key).equals(localized)) failures.add(id + " active native language resolver mismatch: " + key);
            if (!stack.getHoverName().getString().equals(localized)) failures.add(id + " actual getHoverName: " + stack.getHoverName().getString() + ", expected " + localized);
            names.put(id.toString(), Map.of("key", key, "component", component == null ? "null" : component.getString(), "hoverName", stack.getHoverName().getString()));
        }
        if (!names.keySet().equals(expectedIds)) failures.add("Actual registered set differs from full expected item set");
        check(names.size() == expectedIds.size(), "Expected " + expectedIds.size() + " actual registered addon items, found " + names.size());
        check(failures.isEmpty(), locale + " registered item name failures: " + failures);
        return Map.of("registeredItems", names.size(), "names", names);
    }

    private static void verifyModels(Minecraft client, Map<String, Object> output) {
        for (var entry : SimPartialModels.allModels().entrySet()) {
            String path = entry.getKey().getPath();
            if (path.startsWith("item/creative_physics_staff/") || path.startsWith("item/plunger_launcher/"))
                output.put("partial/" + entry.getKey(), inspect(client, entry.getValue(), expectedSprite(path)));
        }
        inspectEquipment(client, PhysicsStaffItemRenderer.INSTANCE, "creative_physics_staff", output);
        inspectEquipment(client, PlungerLauncherItemRenderer.INSTANCE, "plunger_launcher", output);
        for (String id : TOOLS) {
            var stack = BuiltInRegistries.ITEM.getValue(Identifier.fromNamespaceAndPath("simulated", id)).getDefaultInstance();
            var state = new ItemStackRenderState();
            client.getItemModelResolver().updateForLiving(state, stack, ItemDisplayContext.GUI, client.player);
            check(!state.isEmpty(), "Actual native GUI item resolver returned no layers for " + id);
        }
    }

    @SuppressWarnings("unchecked")
    private static void inspectEquipment(Minecraft client, EquipmentItemRenderer renderer, String item, Map<String, Object> output) {
        try {
            // The renderer's actual registered keys are protected; access only the
            // fixture's evidence boundary, then inspect public native baked data.
            var field = EquipmentItemRenderer.class.getDeclaredField("models"); field.setAccessible(true);
            var keys = (Map<String, ExtraModelKey<BlockStateModel>>) field.get(renderer);
            check(keys.size() == (item.equals("creative_physics_staff") ? 7 : 4), "Unexpected live equipment partial inventory for " + item);
            for (var entry : keys.entrySet()) output.put("equipment/" + item + "/" + entry.getKey(), inspect(client, entry.getValue(), expectedSprite(item)));
        } catch (ReflectiveOperationException failure) { throw new RuntimeException(failure); }
    }

    private static String expectedSprite(String path) {
        return path.contains("creative_physics_staff") ? "simulated:block/native_items/zapper_staff" : "simulated:block/native_items/plungerlauncher";
    }

    private static Map<String, Object> inspect(Minecraft client, ExtraModelKey<BlockStateModel> key, String expected) {
        var model = ((FabricModelManager) client.getModelManager()).getModel(key);
        check(model != null, "Actual equipment model did not bake: " + key);
        var parts = new ArrayList<BlockStateModelPart>(); model.collectParts(RandomSource.create(42), parts);
        var quads = new ArrayList<BakedQuad>();
        for (var part : parts) { quads.addAll(part.getQuads(null)); for (var face : Direction.values()) quads.addAll(part.getQuads(face)); }
        check(!quads.isEmpty(), "Actual equipment partial has no baked quads: " + key);
        var sprites = new LinkedHashSet<String>();
        for (var quad : quads) {
            var sprite = quad.materialInfo().sprite().contents().name();
            check(!sprite.equals(MissingTextureAtlasSprite.getLocation()), "Actual equipment partial contains missing-texture geometry: " + key);
            sprites.add(sprite.toString());
            for (int i = 0; i < 4; i++) {
                var point = quad.position(i);
                check(Float.isFinite(point.x()) && Float.isFinite(point.y()) && Float.isFinite(point.z()), "Nonfinite native equipment quad position: " + key);
            }
        }
        check(sprites.contains(expected), "Actual partial uses unexpected baked sprite set " + sprites + ", expected " + expected + ": " + key);
        return Map.of("quads", quads.size(), "sprites", List.copyOf(sprites));
    }

    private static final class ItemScreen extends Screen {
        private final String locale;
        private ItemScreen(String locale) { super(Component.literal("Simulated")); this.locale = locale; }
        @Override public boolean isPauseScreen() { return false; }
        @Override public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partial) {
            graphics.fill(0, 0, width, height, 0xff18232f);
            graphics.centeredText(font, "Simulated · " + locale, width / 2, 24, 0xfff3eee1);
            for (int i = 0; i < TOOLS.size(); i++) {
                int center = width * (i * 2 + 1) / 4, top = height / 2 - 55;
                var stack = BuiltInRegistries.ITEM.getValue(Identifier.fromNamespaceAndPath("simulated", TOOLS.get(i))).getDefaultInstance();
                graphics.fill(center - 76, top - 12, center + 76, top + 116, 0xff2b3e50);
                var pose = graphics.pose(); pose.pushMatrix(); pose.translate(center - 48, top); pose.scale(6, 6);
                graphics.item(stack, 0, 0); pose.popMatrix();
                graphics.centeredText(font, stack.getHoverName().getString(), center, top + 132, 0xfff3eee1);
            }
        }
    }

    private static void frames(ClientGameTestContext test, int count) { for (int tick = 0; tick < count; tick++) test.waitTick(); }
    private static void exportBlockRegistry(Path destination) {
        var blocks = new TreeMap<String, Object>();
        for (var id : BuiltInRegistries.BLOCK.keySet()) {
            var properties = new TreeMap<String, Object>();
            for (var property : BuiltInRegistries.BLOCK.getValue(id).getStateDefinition().getProperties())
                properties.put(property.getName(), propertyValues(property));
            blocks.put(id.toString(), properties);
        }
        try { Files.writeString(destination, new GsonBuilder().setPrettyPrinting().create().toJson(blocks)); }
        catch (java.io.IOException failure) { throw new RuntimeException(failure); }
    }
    private static <T extends Comparable<T>> List<String> propertyValues(Property<T> property) {
        return property.getPossibleValues().stream().map(property::getName).toList();
    }
    private static void check(boolean condition, String message) { if (!condition) throw new AssertionError(message); }
}
