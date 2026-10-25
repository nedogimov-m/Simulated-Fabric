import com.google.gson.GsonBuilder;
import com.mojang.blaze3d.platform.InputConstants;
import java.lang.reflect.Field;
import java.nio.file.*;
import java.util.*;
import net.createmod.ponder.api.registration.StoryBoardEntry;
import net.createmod.ponder.foundation.*;
import net.createmod.ponder.foundation.ui.PonderUI;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import port.observation.GameplayPonder;

/** Runs every distinct installed addon storyboard, observing actual native submission and texture blitting. */
public final class PackagedGameplayPonder {
    private static final Field SCHEDULE = field(PonderScene.class, "activeSchedule");
    private static final Set<String> ADDONS = Set.of("simulated", "aeronautics", "offroad");
    public static void run(ClientGameTestContext test, TestSingleplayerContext world, List<String> checks) throws Exception {
        var report = new LinkedHashMap<String, Object>(); var rows = new ArrayList<Map<String, Object>>();
        var failures = new ArrayList<String>(); report.put("programs", rows); report.put("failures", failures);
        report.put("scope", "All installed addon Ponder bindings enumerated; duplicate subjects sharing the exact callback class and schematic run once. Every selected source program compiles in the real host world and executes to its native finish flag with rendered checkpoints, then replays and seeks through the actual UI. Background animation instructions may remain after the authored finish marker. Native source submission and native PIP texture blit are observed, not simulated.");
        var path = Path.of(System.getProperty("port.smoke.report")).resolveSibling("gameplay-ponder.json");
        try {
            var entries = test.computeOnClient(client -> PonderIndex.getSceneAccess().getRegisteredEntries().stream()
                    .map(Map.Entry::getValue).filter(e -> ADDONS.contains(e.getNamespace())).toList());
            check(!entries.isEmpty(), "Installed addon storyboard registry is empty");
            var groups = new LinkedHashMap<String, List<StoryBoardEntry>>();
            for (var entry : entries) {
                var callback = entry.getBoard().getClass().getName().split("/")[0];
                groups.computeIfAbsent(entry.getSchematicLocation() + "|" + callback, key -> new ArrayList<>()).add(entry);
            }
            report.put("bindings", entries.size()); report.put("distinctPrograms", groups.size());
            int ordinal = 0;
            for (var group : groups.values()) {
                var entry = group.getFirst(); var row = new LinkedHashMap<String, Object>(); rows.add(row);
                var filter = System.getProperty("port.smoke.auditCase", "");
                if (!filter.isEmpty() && !Set.of(filter.split(",")).contains(entry.getSchematicLocation().toString())) { rows.removeLast(); continue; }
                row.put("ordinal", ordinal++); row.put("schematic", entry.getSchematicLocation().toString());
                row.put("subjects", group.stream().map(e -> e.getComponent().toString()).toList());
                var frames = new ArrayList<GameplayPonder.FrameReceipt>(); row.put("frames", frames);
                PonderUI ui = null;
                try {
                    ui = test.computeOnClient(client -> {
                        var scenes = PonderIndex.getSceneAccess().compile(List.of(entry));
                        check(scenes.size() == 1, "One binding must compile to one scene");
                        var result = new PonderUI(scenes, null); client.setScreenAndShow(result);
                        row.put("program", result.getActiveScene().getId().toString());
                        row.put("initialBlocks", result.getActiveScene().getWorld().getBlockMap().size());
                        row.put("totalTime", result.getActiveScene().getTotalTime());
                        return result;
                    });
                    var active = ui; var scene = active.getActiveScene();
                    // First check ordinary UI playback, independently of the accelerated full source walk.
                    var before = test.computeOnClient(client -> scene.getCurrentTime());
                    test.waitTicks(8);
                    row.put("naturalPlaybackStart", before);
                    row.put("naturalPlaybackEnd", test.computeOnClient(client -> scene.getCurrentTime()));
                    capture(test, scene, frames);
                    check(test.computeOnClient(client -> scene.getTotalTime() < 2 || scene.getCurrentTime() > before || scene.isFinished()),
                            "Actual UI does not advance ordinary source playback");
                    test.getInput().pressKey(InputConstants.KEY_SPACE); test.waitTicks(3);
                    var paused = test.computeOnClient(client -> scene.getCurrentTime()); test.waitTicks(3);
                    check(test.computeOnClient(client -> scene.getCurrentTime() == paused), "Native space pause did not retain source time");
                    int ticks = 0; boolean visible = false;
                    while (test.computeOnClient(client -> scheduled(scene) && !scene.isFinished())) {
                        var count = new int[]{0};
                        test.runOnClient(client -> {
                            while (scheduled(scene) && !scene.isFinished() && count[0] < 40) { scene.tick(); count[0]++; }
                        });
                        ticks += count[0];
                        if (ticks > Math.max(1000, scene.getTotalTime() + 400)) {
                            row.put("nativeEndState", test.computeOnClient(client -> Map.of("time", scene.getCurrentTime(), "totalTime", scene.getTotalTime(),
                                    "sourceFinished", scene.isFinished(), "remainingInstructions", remaining(scene))));
                            throw new AssertionError("Source timeline ended but native completion flag never became true");
                        }
                        capture(test, scene, frames);
                        var frame = frames.getLast(); visible |= frame.blockModels() > 0 || frame.blockEntities() > 0 || frame.entities() > 0 || frame.sourceItems() > 0;
                        if (!row.containsKey("screenshot") && visible && frames.size() >= 3)
                            row.put("screenshot", test.takeScreenshot("addon-ponder-" + row.get("ordinal") + "-" + scene.getId().toString().replace(':', '_').replace('/', '_')).toString());
                    }
                    row.put("executedSourceTicks", ticks); row.put("sourceFinished", scene.isFinished()); row.put("backgroundInstructionsAtFinish", test.computeOnClient(client -> remaining(scene)));
                    check(scene.isFinished(), "Scheduler drained without source completion");
                    check(visible, "Storyboard never submitted visible source geometry");
                    test.runOnClient(client -> {
                        var oldOwners = new ArrayList<net.minecraft.world.level.block.entity.BlockEntity>();
                        scene.getWorld().getBlockEntities().forEach(oldOwners::add);
                        active.seekToTime(0); check(scene.getCurrentTime() == 0, "Native backward seek did not rewind");
                        for (var owner : oldOwners) check(owner.isRemoved(), "Replay retained an old scene block entity");
                        active.seekToTime(Math.max(1, scene.getTotalTime() / 2));
                    });
                    capture(test, scene, frames); row.put("replayAndSeek", true); row.put("status", "passed");
                } catch (Throwable failure) {
                    row.put("status", "failed"); row.put("error", failure.toString()); failures.add(entry.getSchematicLocation() + ": " + failure);
                    try { row.put("failureScreenshot", test.takeScreenshot("addon-ponder-failure-" + row.get("ordinal")).toString()); } catch (Throwable ignored) {}
                } finally {
                    var retired = ui;
                    test.runOnClient(client -> {
                        GameplayPonder.clear(); client.setScreenAndShow(null);
                        if (retired != null) {
                            retired.getActiveScene().getWorld().getBlockEntities().forEach(owner -> owner.setRemoved());
                            retired.getActiveScene().getWorld().getEntityList().forEach(entity -> entity.discard());
                        }
                    });
                    Files.writeString(path, new GsonBuilder().setPrettyPrinting().create().toJson(report) + "\n");
                }
            }
            if (System.getProperty("port.smoke.auditCase", "").equals("navigation")) navigation(test, report, checks);
            else check(!rows.isEmpty(), "No installed addon storyboards match the selected filter");
            report.put("status", failures.isEmpty() ? "passed" : "failed");
            check(failures.isEmpty(), "Actual addon Ponder failures: " + failures);
            checks.add("Installed addon Ponder registry: " + entries.size() + " bindings, " + groups.size() + " distinct source programs; " + rows.size() + " selected programs passed ordinary UI playback, authored source completion, native rendered checkpoints, replay, seek and retired owners");
        } finally { Files.writeString(path, new GsonBuilder().setPrettyPrinting().create().toJson(report) + "\n"); }
    }
    private static void capture(ClientGameTestContext test, PonderScene scene, List<GameplayPonder.FrameReceipt> frames) {
        var sample = test.computeOnClient(client -> GameplayPonder.begin(scene));
        test.waitFor(client -> sample.failure() != null || sample.completed() != null, 200);
        if (sample.failure() != null) throw new AssertionError("Native Ponder owner submission failed", sample.failure());
        check(sample.completed() != null, "No actual native source/PIP frame completed"); frames.add(sample.completed());
        test.runOnClient(client -> GameplayPonder.clear());
    }
    private static void navigation(ClientGameTestContext test, Map<String,Object> report, List<String> checks) {
        var rows = new ArrayList<Object>(); report.put("nativeNavigation", rows);
        for (var subject : List.of("aeronautics:gyroscopic_propeller_bearing", "offroad:borehead_bearing")) {
            var ui = test.computeOnClient(client -> { var screen = PonderUI.of(net.minecraft.resources.Identifier.parse(subject)); client.setScreenAndShow(screen); return screen; });
            var first = ui.getActiveScene().getId();
            test.runOnClient(client -> ui.seekToTime(Math.max(0, ui.getActiveScene().getTotalTime() - 2)));
            test.waitFor(client -> ui.getActiveScene().isFinished(), 300); test.waitTicks(70);
            var row = new LinkedHashMap<String,Object>(); row.put("subject", subject); row.put("finished", first.toString());
            row.put("completionScreenshot", test.takeScreenshot("addon-ponder-native-completed-" + first.toString().replace(':','_')).toString());
            var point = test.computeOnClient(client -> new net.minecraft.world.phys.Vec2((ui.width / 2f + 28) * client.getWindow().getScreenWidth() / client.getWindow().getGuiScaledWidth(),
                    (ui.height - 41f) * client.getWindow().getScreenHeight() / client.getWindow().getGuiScaledHeight()));
            test.getInput().setCursorPos(point.x, point.y); test.waitTicks(2); test.getInput().pressMouse(InputConstants.MOUSE_BUTTON_LEFT);
            test.waitFor(client -> !ui.getActiveScene().getId().equals(first), 200); test.waitTicks(30);
            row.put("next", ui.getActiveScene().getId().toString()); row.put("nextScreenshot", test.takeScreenshot("addon-ponder-native-second-scene-" + subject.replace(':','_')).toString());
            if (subject.startsWith("offroad")) {
                var reload = new java.util.concurrent.atomic.AtomicReference<java.util.concurrent.CompletableFuture<Void>>();
                test.runOnClient(client -> reload.set(client.reloadResourcePacks()));
                test.waitFor(client -> reload.get() != null && reload.get().isDone(), 600); test.runOnClient(client -> reload.get().join());
                test.waitFor(client -> client.gui.overlay() == null, 300);
                var frames = new ArrayList<GameplayPonder.FrameReceipt>(); capture(test, ui.getActiveScene(), frames);
                row.put("liveSecondSceneAfterReload", frames); row.put("reloadScreenshot", test.takeScreenshot("addon-ponder-native-second-scene-after-reload").toString());
            }
            rows.add(row); test.runOnClient(client -> client.setScreenAndShow(null));
        }
        checks.add("Real PonderUI.of subject navigation: native timeline finish, completion frames, physical next-button clicks into the second gyro/borehead scenes; actual resource reload while the second Offroad scene stays open and native mesh/PIP receipts after reload");
    }
    private static boolean scheduled(PonderScene scene) {
        try { return !((List<?>) SCHEDULE.get(scene)).isEmpty(); }
        catch (IllegalAccessException failure) { throw new AssertionError(failure); }
    }
    private static List<String> remaining(PonderScene scene) {
        try { return ((List<?>) SCHEDULE.get(scene)).stream().map(value -> value.getClass().getName()).toList(); }
        catch (IllegalAccessException failure) { throw new AssertionError(failure); }
    }
    private static Field field(Class<?> owner, String name) {
        try { var result = owner.getDeclaredField(name); result.setAccessible(true); return result; }
        catch (ReflectiveOperationException failure) { throw new ExceptionInInitializerError(failure); }
    }
    private static void check(boolean value, String message) { if (!value) throw new AssertionError(message); }
    private PackagedGameplayPonder() {}
}
