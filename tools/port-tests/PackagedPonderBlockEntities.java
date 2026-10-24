import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.content.kinetics.chainDrive.ChainGearshiftBlockEntity;
import com.simibubi.create.content.kinetics.crank.ValveHandleBlockEntity;
import dev.simulated_team.simulated.content.blocks.portable_engine.PortableEngineBlockEntity;
import net.createmod.ponder.api.level.PonderLevel;
import net.createmod.ponder.foundation.registration.PonderSceneRegistry;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.storage.TagValueOutput;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/** Only the newly migrated authored engine BE identities, native placement and backup/restore. */
public final class PackagedPonderBlockEntities {
    public static void run(ClientGameTestContext test, TestSingleplayerContext world, List<String> checks) throws Exception {
        if (Boolean.getBoolean("port.smoke.ponderOwnersOnly")) { runOwnersOnly(test, checks); return; }
        Path reportPath = Path.of(System.getProperty("port.smoke.report")).resolveSibling("ponder-block-entities.json");
        var report = new LinkedHashMap<String, Object>();
        var accepted = new ArrayList<Object>();
        report.put("scope", "Actual engine block entities from the newly migrated authored Ponder templates, native placement and fresh-owner backup/restore. No earlier item names, geometry, hand, statistics, scene-tick or activation replay.");
        report.put("engines", accepted);
        try {
            var grouped = new LinkedHashMap<String, List<JsonObject>>();
            try (var input = PackagedPonderBlockEntities.class.getResourceAsStream("/ponder-engine-expectations.json")) {
                check(input != null, "Missing exact authored Ponder engine expectations");
                var entries = JsonParser.parseReader(new InputStreamReader(input, StandardCharsets.UTF_8)).getAsJsonObject().getAsJsonArray("entries");
                for (var entry : entries) {
                    var value = entry.getAsJsonObject();
                    grouped.computeIfAbsent(value.get("asset").getAsString(), ignored -> new ArrayList<>()).add(value);
                }
                report.put("expectedEngineCount", entries.size());
            }
            test.runOnClient(client -> {
                exportBlockEntityRegistry(reportPath.resolveSibling("block-entities.json"));
                for (var asset : grouped.entrySet()) {
                    String resource = asset.getKey().substring(asset.getKey().indexOf("/assets/") + "/assets/".length());
                    int namespaceEnd = resource.indexOf('/');
                    String path = resource.substring(namespaceEnd + "/ponder/".length());
                    check(path.endsWith(".nbt"), "Unexpected authored Ponder resource path " + resource);
                    var sceneId = Identifier.fromNamespaceAndPath(resource.substring(0, namespaceEnd), path.substring(0, path.length() - 4));
                    var template = PonderSceneRegistry.loadSchematic(sceneId);
                    var ponder = new PonderLevel(BlockPos.ZERO, client.level);
                    check(ponder.place(template, BlockPos.ZERO, new StructurePlaceSettings()), "Native Ponder template placement refused " + sceneId);
                    var original = new LinkedHashMap<BlockPos, PortableEngineBlockEntity>();
                    var evidence = new LinkedHashMap<BlockPos, Map<String, Object>>();
                    for (var expected : asset.getValue()) {
                        var pos = position(expected);
                        var engine = requireEngine(ponder, pos, expected, "initial");
                        original.put(pos, engine);
                        var detail = new LinkedHashMap<String, Object>();
                        detail.put("asset", asset.getKey()); detail.put("scene", sceneId.toString());
                        detail.put("position", List.of(pos.getX(), pos.getY(), pos.getZ()));
                        detail.put("initial", verifyPayload(ponder, engine, expected.getAsJsonObject("payload"), "initial " + sceneId + " " + pos));
                        evidence.put(pos, detail);
                    }
                    ponder.createBackup();
                    for (var engine : original.values()) {
                        engine.setCurrentBurnTime(engine.getCurrentBurnTime() == 0 ? 1 : 0);
                        engine.setSuperHeated(!engine.isSuperHeated());
                        engine.inventory.slot.setStack(ItemStack.EMPTY);
                    }
                    ponder.restore();
                    for (var expected : asset.getValue()) {
                        var pos = position(expected);
                        var engine = requireEngine(ponder, pos, expected, "restored");
                        check(engine != original.get(pos) && original.get(pos).isRemoved(), "Ponder backup reused the old engine owner at " + pos);
                        var detail = evidence.get(pos);
                        detail.put("restored", verifyPayload(ponder, engine, expected.getAsJsonObject("payload"), "restored " + sceneId + " " + pos));
                        detail.put("freshOwner", true);
                        accepted.add(detail);
                    }
                }
            });
            check(accepted.size() == ((Number) report.get("expectedEngineCount")).intValue(), "Not every authored engine was placed and restored");
            report.put("nativeTemplates", grouped.size());
            report.put("status", "passed");
            checks.add("new authored Ponder engine BE boundary: " + accepted.size() + " real portable engine owners in " + grouped.size() + " templates retain burn time, superheat, direction, generated speed and exact inventory through native placement and fresh-owner backup/restore");
        } catch (Exception | Error failure) {
            report.put("status", "failed"); report.put("error", failure.toString()); throw failure;
        } finally {
            Files.writeString(reportPath, new GsonBuilder().setPrettyPrinting().create().toJson(report));
        }
    }

    private record OwnerCase(String scene, BlockPos position, String type, Class<? extends KineticBlockEntity> owner, float speed, String authoredField, int authoredValue) { }

    private static void runOwnersOnly(ClientGameTestContext test, List<String> checks) throws Exception {
        Path reportPath = Path.of(System.getProperty("port.smoke.report")).resolveSibling("ponder-owner-types.json");
        var report = new LinkedHashMap<String, Object>();
        var accepted = new ArrayList<Object>(); report.put("owners", accepted);
        report.put("scope", "Only the two newly migrated authored Create block-entity IDs: native placement, valid owner type and fresh-owner backup/restore. Previously accepted 16 portable engines and registry export are not repeated.");
        var cases = List.of(
                new OwnerCase("aeronautics:gyroscopic_propeller_bearing/helicopter", new BlockPos(3, 3, 6), "create:adjustable_chain_gearshift", ChainGearshiftBlockEntity.class, 32, "Signal", 0),
                new OwnerCase("aeronautics:gyroscopic_propeller_bearing/redstone", new BlockPos(4, 2, 3), "create:valve_handle", ValveHandleBlockEntity.class, 0, "InUse", 0));
        try {
            test.runOnClient(client -> {
                for (var expected : cases) {
                    var template = PonderSceneRegistry.loadSchematic(Identifier.parse(expected.scene));
                    var ponder = new PonderLevel(BlockPos.ZERO, client.level);
                    check(ponder.place(template, BlockPos.ZERO, new StructurePlaceSettings()), "Native Ponder placement refused " + expected.scene);
                    var initial = requireOwner(ponder, expected);
                    var detail = new LinkedHashMap<String, Object>(); detail.put("scene", expected.scene);
                    detail.put("position", List.of(expected.position.getX(), expected.position.getY(), expected.position.getZ()));
                    detail.put("initial", ownerPayload(ponder, initial, expected));
                    ponder.createBackup();
                    ponder.removeBlockEntity(expected.position);
                    check(initial.isRemoved(), "Native Ponder removal did not retire the original owner");
                    ponder.restore();
                    var restored = requireOwner(ponder, expected);
                    check(restored != initial && initial.isRemoved(), "Ponder backup reused the original Create owner");
                    detail.put("restored", ownerPayload(ponder, restored, expected)); detail.put("freshOwner", true);
                    accepted.add(detail);
                }
            });
            report.put("status", "passed");
            checks.add("new Ponder owner types: actual adjustable chain gearshift and copper valve handle native placement and fresh-owner backup/restore retain authored type, speed and Signal/InUse payload");
        } catch (Exception | Error failure) {
            report.put("status", "failed"); report.put("error", failure.toString()); throw failure;
        } finally { Files.writeString(reportPath, new GsonBuilder().setPrettyPrinting().create().toJson(report)); }
    }

    private static KineticBlockEntity requireOwner(PonderLevel level, OwnerCase expected) {
        var entity = level.getBlockEntity(expected.position);
        check(expected.owner.isInstance(entity), "Wrong actual Ponder Create owner at " + expected.position + ": " + entity);
        var kinetic = (KineticBlockEntity) entity;
        var type = BuiltInRegistries.BLOCK_ENTITY_TYPE.getKey(kinetic.getType());
        check(type != null && type.toString().equals(expected.type), "Wrong native Ponder BE type " + type + ", expected " + expected.type);
        check(kinetic.getType().isValid(level.getBlockState(expected.position)), "Native Ponder owner does not accept its actual block");
        check(kinetic.getLevel() == level && kinetic.isVirtual() && !kinetic.isRemoved(), "Native Ponder Create owner is not bound to its actual virtual level");
        return kinetic;
    }

    private static Map<String, Object> ownerPayload(PonderLevel level, KineticBlockEntity entity, OwnerCase expected) {
        var output = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, level.registryAccess());
        entity.saveWithoutMetadata(output); var saved = output.buildResult();
        check(Float.compare(saved.getFloatOr("Speed", Float.NaN), expected.speed) == 0, "Authored Create owner Speed changed at " + expected.position);
        check(saved.getIntOr(expected.authoredField, -1) == expected.authoredValue, "Authored Create owner " + expected.authoredField + " changed at " + expected.position);
        return Map.of("type", expected.type, "class", entity.getClass().getName(), "speed", expected.speed,
                expected.authoredField, expected.authoredValue, "actualBlock", BuiltInRegistries.BLOCK.getKey(entity.getBlockState().getBlock()).toString());
    }

    private static PortableEngineBlockEntity requireEngine(PonderLevel level, BlockPos position, JsonObject expected, String phase) {
        var entity = level.getBlockEntity(position);
        check(entity instanceof PortableEngineBlockEntity, phase + " did not create actual portable engine at " + position + ": " + entity);
        var engine = (PortableEngineBlockEntity) entity;
        var type = BuiltInRegistries.BLOCK_ENTITY_TYPE.getKey(engine.getType());
        check(type != null && type.toString().equals(expected.get("current_type").getAsString()), phase + " engine has wrong native type at " + position + ": " + type);
        check(engine.getType().isValid(level.getBlockState(position)), phase + " engine type does not accept its actual local block");
        check(engine.getLevel() == level && engine.isVirtual() && !engine.isRemoved(), phase + " engine was not bound as an actual virtual Ponder owner");
        return engine;
    }

    private static Map<String, Object> verifyPayload(PonderLevel level, PortableEngineBlockEntity engine, JsonObject expected, String context) {
        int burn = expected.has("BurnTime") ? expected.get("BurnTime").getAsInt() : 0;
        boolean superheated = expected.has("SuperHeated") && expected.get("SuperHeated").getAsInt() != 0;
        check(engine.getCurrentBurnTime() == burn, context + " authored BurnTime changed: " + engine.getCurrentBurnTime() + " != " + burn);
        check(engine.isSuperHeated() == superheated, context + " authored SuperHeated changed");
        var output = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, level.registryAccess());
        engine.write(output, false);
        var saved = output.buildResult();
        int direction = expected.has("ScrollValue") ? expected.get("ScrollValue").getAsInt() : 0;
        float generated = expected.has("GeneratedSpeed") ? expected.get("GeneratedSpeed").getAsFloat() : 0;
        check(saved.getIntOr("ScrollValue", -1) == direction, context + " authored movement direction changed");
        check(Float.compare(saved.getFloatOr("GeneratedSpeed", Float.NaN), generated) == 0, context + " authored generated speed changed");
        var stack = engine.inventory.slot.getStack();
        String item = "minecraft:air"; int count = 0;
        if (expected.has("Inventory") && expected.getAsJsonObject("Inventory").has("item")) {
            var wanted = expected.getAsJsonObject("Inventory").getAsJsonObject("item");
            item = wanted.get("id").getAsString(); count = wanted.get("count").getAsInt();
        }
        check(stack.getCount() == count && BuiltInRegistries.ITEM.getKey(stack.getItem()).toString().equals(item), context + " authored inventory changed: " + stack + ", expected " + count + " " + item);
        return Map.of("type", BuiltInRegistries.BLOCK_ENTITY_TYPE.getKey(engine.getType()).toString(), "burnTime", burn,
                "superHeated", superheated, "movementDirection", direction, "generatedSpeed", generated,
                "inventoryItem", item, "inventoryCount", count);
    }

    private static BlockPos position(JsonObject expected) {
        var pos = expected.getAsJsonArray("position");
        return new BlockPos(pos.get(0).getAsInt(), pos.get(1).getAsInt(), pos.get(2).getAsInt());
    }

    private static void exportBlockEntityRegistry(Path destination) {
        var types = new TreeMap<String, List<String>>();
        for (var id : BuiltInRegistries.BLOCK_ENTITY_TYPE.keySet()) {
            var type = BuiltInRegistries.BLOCK_ENTITY_TYPE.getValue(id);
            var blocks = new ArrayList<String>();
            for (var block : BuiltInRegistries.BLOCK.keySet()) if (type.isValid(BuiltInRegistries.BLOCK.getValue(block).defaultBlockState())) blocks.add(block.toString());
            blocks.sort(String::compareTo); types.put(id.toString(), blocks);
        }
        try { Files.writeString(destination, new GsonBuilder().setPrettyPrinting().create().toJson(types)); }
        catch (java.io.IOException failure) { throw new RuntimeException(failure); }
    }

    private static void check(boolean condition, String message) { if (!condition) throw new AssertionError(message); }
}
