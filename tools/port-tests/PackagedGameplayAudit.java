import com.google.gson.GsonBuilder;
import com.google.gson.JsonParser;
import com.mojang.blaze3d.platform.InputConstants;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.*;
import port.observation.GameplayItems;

/** Production-only native inventory and finite-stock user placement audit. */
public final class PackagedGameplayAudit {
    private static final Set<String> ADDONS = Set.of("simulated", "aeronautics", "offroad");
    private static final BlockPos TARGET = new BlockPos(8, 120, 8);

    public static void run(ClientGameTestContext test, TestSingleplayerContext world, List<String> checks) throws Exception {
        var report = new LinkedHashMap<String, Object>();
        var selection = System.getProperty("port.smoke.auditChecks", "items");
        report.put("scope", "Actual production JARs in a native Vulkan client. Item rows observe real InventoryScreen resolver calls; placement uses physical input and finite survival stock. Setup commands only prepare isolated fixtures.");
        report.put("selection", selection);
        try {
            var items = test.computeOnClient(client -> BuiltInRegistries.ITEM.entrySet().stream()
                    .filter(e -> ADDONS.contains(e.getKey().identifier().getNamespace()))
                    .filter(e -> System.getProperty("port.smoke.auditItem", "").isEmpty()
                            || e.getKey().identifier().toString().equals(System.getProperty("port.smoke.auditItem")))
                    .sorted(Comparator.comparing(e -> e.getKey().identifier().toString())).map(Map.Entry::getValue).toList());
            report.put("registeredItems", items.size());
            check(!items.isEmpty(), "No registered addon items match the selected item filter");
            if (selection.equals("items")) inventory(test, world, items, report, checks);
            else if (selection.equals("placement")) placements(test, world, items, report, checks);
            else if (selection.equals("ponder") || selection.equals("tools")) {
                Class.forName(selection.equals("ponder") ? "PackagedGameplayPonder" : "PackagedGameplayTools").getMethod("run", ClientGameTestContext.class,
                        TestSingleplayerContext.class, List.class).invoke(null, test, world, checks);
            } else throw new IllegalArgumentException("Unknown native audit selection: " + selection);
            report.put("status", "passed");
        } catch (Throwable failure) {
            report.put("status", "failed"); report.put("error", failure.toString()); throw failure;
        } finally {
            GameplayItems.active = Set.of();
            report.put("items", GameplayItems.ROWS.values().stream().sorted(Comparator.comparing(row -> row.id)).toList());
            var reportName = selection.equals("ponder") || selection.equals("tools") ? "gameplay-dispatch-" + selection : "gameplay-" + selection;
            Files.writeString(Path.of(System.getProperty("port.smoke.report")).resolveSibling(reportName + ".json"),
                    new GsonBuilder().setPrettyPrinting().create().toJson(report) + "\n");
        }
    }

    private static void inventory(ClientGameTestContext test, TestSingleplayerContext world, List<Item> items,
            Map<String, Object> report, List<String> checks) throws Exception {
        GameplayItems.ROWS.clear();
        test.runOnClient(client -> {
            for (var item : items) {
                var row = new GameplayItems.Row(); row.id = BuiltInRegistries.ITEM.getKey(item).toString();
                row.itemClass = item.getClass().getName();
                if (item instanceof BlockItem block) row.blockClass = block.getBlock().getClass().getName();
                var hooks = Set.of("use", "useOn", "interactLivingEntity", "inventoryTick", "releaseUsing", "finishUsingItem", "hurtEnemy", "onUseTick");
                row.useHooks = Arrays.stream(item.getClass().getMethods()).filter(method -> hooks.contains(method.getName())
                                && method.getDeclaringClass() != Item.class)
                        .map(method -> method.getDeclaringClass().getSimpleName() + "." + method.getName()).distinct().sorted().toList();
                GameplayItems.ROWS.put(item, row);
            }
        });
        world.getServer().runCommand("gamemode survival @a");
        var screenshots = new ArrayList<String>(); report.put("screenshots", screenshots);
        for (var locale : List.of("en_us", "ru_ru")) {
            var reload = new AtomicReference<CompletableFuture<Void>>();
            test.runOnClient(client -> {
                client.options.languageCode = locale; client.getLanguageManager().setSelected(locale);
                reload.set(client.reloadResourcePacks());
            });
            test.waitFor(client -> reload.get() != null && reload.get().isDone(), 600);
            test.runOnClient(client -> { reload.get().join(); names(client, items, locale); });
            test.waitFor(client -> client.gui.overlay() == null, 300);
            test.getInput().pressKey(options -> options.keyInventory);
            test.waitFor(client -> client.gui.screen() instanceof InventoryScreen, 200);
            for (int start = 0; start < items.size(); start += 36) {
                var page = List.copyOf(items.subList(start, Math.min(start + 36, items.size())));
                var counts = new IdentityHashMap<Item, Integer>();
                test.runOnClient(client -> {
                    GameplayItems.active = Set.copyOf(page);
                    page.forEach(item -> counts.put(item, GameplayItems.ROWS.get(item).observations));
                });
                world.getServer().runOnServer(server -> {
                    var player = player(server);
                    for (int slot = 0; slot < 36; slot++) player.getInventory().setItem(slot,
                            slot < page.size() ? new ItemStack(page.get(slot)) : ItemStack.EMPTY);
                    player.containerMenu.broadcastChanges();
                });
                test.waitFor(client -> {
                    for (int slot = 0; slot < page.size(); slot++) if (!client.player.getInventory().getItem(slot).is(page.get(slot))
                            || GameplayItems.ROWS.get(page.get(slot)).observations < counts.get(page.get(slot)) + 3) return false;
                    return true;
                }, 300);
                test.runOnClient(client -> client.gui.toastManager().clear());
                screenshots.add(test.takeScreenshot("addon-inventory-" + locale + "-" + start / 36).toString());
            }
            test.getInput().pressKey(InputConstants.KEY_ESCAPE);
            test.waitFor(client -> client.gui.screen() == null, 200);
        }
        var failures = GameplayItems.ROWS.values().stream().filter(row -> !row.nonempty || row.faces == 0 || !row.issues.isEmpty())
                .map(row -> row.id + ": faces=" + row.faces + ", issues=" + row.issues).toList();
        report.put("failures", failures);
        check(failures.isEmpty(), "Actual visible inventory item failures: " + failures);
        checks.add("All " + items.size() + " registered addon items rendered in real EN/RU survival inventories after two native reloads; actual vanilla/Fabric/equipment layers have finite visible geometry and no missing-texture UVs; screenshots retained");
    }

    private static void names(Minecraft client, List<Item> items, String locale) {
        var dictionaries = new HashMap<String, com.google.gson.JsonObject>();
        for (var namespace : ADDONS) {
            var id = Identifier.fromNamespaceAndPath(namespace, "lang/" + locale + ".json");
            try (var reader = client.getResourceManager().getResource(id).orElseThrow().openAsReader()) {
                dictionaries.put(namespace, JsonParser.parseReader(reader).getAsJsonObject());
            } catch (java.io.IOException failure) { throw new RuntimeException(failure); }
        }
        for (var item : items) {
            var stack = new ItemStack(item); var component = stack.get(DataComponents.ITEM_NAME);
            var row = GameplayItems.ROWS.get(item); var namespace = BuiltInRegistries.ITEM.getKey(item).getNamespace();
            row.names.put(locale, stack.getHoverName().getString());
            if (component == null || !(component.getContents() instanceof TranslatableContents contents))
                row.issues.add("Missing translatable ITEM_NAME in " + locale);
            else if (!dictionaries.get(namespace).has(contents.getKey())) row.issues.add("Missing " + locale + " name: " + contents.getKey());
        }
    }

    private static void placements(ClientGameTestContext test, TestSingleplayerContext world, List<Item> items,
            Map<String, Object> report, List<String> checks) {
        var rows = new ArrayList<Object>(); report.put("placements", rows);
        var failures = new ArrayList<String>(); report.put("failures", failures);
        var server = world.getServer();
        server.runCommand("gamemode survival @a"); server.runCommand("time set noon"); server.runCommand("weather clear");
        server.runOnServer(s -> {
            var level = s.overworld();
            for (int x = 2; x <= 14; x++) for (int z = 2; z <= 15; z++)
                level.setBlockAndUpdate(new BlockPos(x, 119, z), Blocks.STONE.defaultBlockState());
        });
        for (var item : items) {
            if (!(item instanceof BlockItem block)) continue;
            var id = BuiltInRegistries.ITEM.getKey(item).toString();
            var placedAt = item instanceof com.simibubi.create.content.contraptions.actors.roller.RollerBlockItem ? TARGET.above() : TARGET;
            boolean wall = id.contains("nameplate");
            server.runCommand("tp @a 8.5 120 11.5 180 25");
            server.runCommand("clear @a");
            server.runOnServer(s -> {
                var level = s.overworld();
                level.setBlockAndUpdate(TARGET, Blocks.AIR.defaultBlockState());
                level.setBlockAndUpdate(TARGET.above(), Blocks.AIR.defaultBlockState());
                level.setBlockAndUpdate(TARGET.north(), wall ? Blocks.STONE.defaultBlockState() : Blocks.AIR.defaultBlockState());
                var player = player(s); player.getInventory().setItem(0, new ItemStack(item, 2)); player.containerMenu.broadcastChanges();
            });
            test.waitFor(client -> client.player != null && client.player.distanceToSqr(8.5, 120, 11.5) < .1
                    && client.player.getMainHandItem().is(item) && client.player.getMainHandItem().getCount() == 2
                    && client.level.getBlockState(TARGET).isAir(), 200);
            var support = wall ? TARGET.north() : TARGET.below(); var face = wall ? Direction.SOUTH : Direction.UP;
            var aim = Vec3.atCenterOf(support).add(Vec3.atLowerCornerOf(face.getUnitVec3i()).scale(.499));
            look(test, aim);
            test.waitFor(client -> client.hitResult instanceof BlockHitResult hit && hit.getBlockPos().equals(support)
                    && hit.getDirection() == face, 200);
            test.getInput().pressKey(options -> options.keyUse);
            // Wait for real server consumption, not client prediction. A refused contextual placement is recorded for investigation.
            boolean consumed = false;
            for (int tick = 0; tick < 30; tick++) {
                if (server.computeOnServer(s -> player(s).getInventory().countItem(item) == 1)) { consumed = true; break; }
                test.waitTicks(1);
            }
            final boolean stockConsumed = consumed;
            var row = server.computeOnServer(s -> {
                var state = s.overworld().getBlockState(placedAt); var be = s.overworld().getBlockEntity(placedAt);
                return Map.<String, Object>of("item", id, "support", wall ? "wall" : "floor", "state", state.toString(),
                        "placedExpectedBlock", state.is(block.getBlock()), "oneConsumed", stockConsumed,
                        "remaining", player(s).getInventory().countItem(item), "blockEntity", be == null ? "none" : be.getClass().getName(),
                        "nativePlacementPosition", placedAt.toShortString());
            });
            rows.add(row);
            if (!Boolean.TRUE.equals(row.get("placedExpectedBlock")) || !stockConsumed) failures.add(id + ": " + row);
            if (Boolean.TRUE.equals(row.get("placedExpectedBlock"))) test.waitFor(client -> client.level.getBlockState(placedAt).is(block.getBlock()), 200);
            test.runOnClient(client -> { if (client.gui.screen() != null) client.setScreenAndShow(null); });
        }
        report.put("screenshot", test.takeScreenshot("addon-finite-stock-placement-end").toString());
        check(failures.isEmpty(), "Native finite-stock placements require investigation: " + failures);
        checks.add(rows.size() + " actual physical addon BlockItem placements in survival; each consumes exactly one of two stock items, creates the registered block/BE and converges to the client");
    }

    static void look(ClientGameTestContext test, Vec3 target) {
        var angles = test.computeOnClient(client -> {
            var delta = target.subtract(client.player.getEyePosition());
            return new Vec2((float) Math.toDegrees(Math.atan2(-delta.x, delta.z)),
                    (float) -Math.toDegrees(Math.atan2(delta.y, Math.hypot(delta.x, delta.z))));
        });
        test.getInput().lookAt(angles.x, angles.y);
    }
    static ServerPlayer player(net.minecraft.server.MinecraftServer server) { return server.getPlayerList().getPlayers().getFirst(); }
    static void check(boolean condition, String message) { if (!condition) throw new AssertionError(message); }
    private PackagedGameplayAudit() {}
}
