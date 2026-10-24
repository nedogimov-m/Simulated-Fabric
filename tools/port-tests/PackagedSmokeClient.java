import com.google.gson.GsonBuilder;
import com.mojang.blaze3d.systems.RenderSystem;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;

/** Loads the full production jars, a real world, then reloads and disconnects. */
public final class PackagedSmokeClient implements FabricClientGameTest {
    @Override public void runTest(ClientGameTestContext test) {
        var report = new LinkedHashMap<String,Object>();
        var checks = new ArrayList<String>();
        var reload = new AtomicReference<CompletableFuture<Void>>();
        try {
            test.runOnClient(client -> {
                boolean isolated = Boolean.getBoolean("port.smoke.itemResources") || Boolean.getBoolean("port.smoke.reportedCrash") || Boolean.getBoolean("port.smoke.ponderBlockEntities") || Boolean.getBoolean("port.smoke.borehead") || Boolean.getBoolean("port.smoke.warningResources") || System.getProperty("port.smoke.userRegression") != null;
                if (!isolated) {
                    try { Class.forName("PackagedActivation").getMethod("verify").invoke(null); }
                    catch (ReflectiveOperationException failure) { throw new RuntimeException(failure); }
                }
                report.put("backend", RenderSystem.getDevice().getDeviceInfo().backendName());
                if (net.fabricmc.loader.api.FabricLoader.getInstance().isModLoaded("veil")) {
                    try {
                        if (Class.forName("foundry.veil.api.client.render.VeilRenderSystem").getMethod("renderer").invoke(null) == null)
                            throw new AssertionError("Veil production renderer did not initialize");
                    } catch (ReflectiveOperationException failure) { throw new RuntimeException(failure); }
                }
                checks.add(isolated
                        ? "actual production targeted client startup and initial resource loading"
                        : "complete production client entrypoints, required mixins, access wideners and initial resource reload");
            });
            try (var world = System.getProperty("port.smoke.resumeWorld") == null ? test.worldBuilder().create()
                    : new net.fabricmc.fabric.impl.client.gametest.world.TestWorldSaveImpl(test,
                            Path.of(System.getProperty("port.smoke.resumeWorld"))).open()) {
                test.waitFor(client -> client.level != null && client.player != null
                        && client.gameRenderer.gameRenderState().levelRenderState.cameraRenderState.initialized, 400);
                checks.add("actual integrated world, player and extracted/rendered camera");
                if (System.getProperty("port.smoke.userRegression") != null) {
                    var helper = switch (System.getProperty("port.smoke.userRegression")) {
                        case "diagram" -> "PackagedDiagramRegression";
                        case "schematicannon" -> "PackagedSchematicannonRegression";
                        case "schematicannon-interaction" -> "PackagedSchematicannonInteraction";
                        case "physical-walking" -> "PackagedPhysicalWalkingRegression";
                        case "glue" -> "PackagedGlueVisibilityRegression";
                        case "grass" -> "PackagedGrassStandingRegression";
                        case "create-walking" -> "PackagedCreateWalkingRegression";
                        case "create-micro-walking" -> "PackagedCreateMicroWalkingRegression";
                        case "plot-interaction" -> "PackagedPlotInteractionRegression";
                        case "assembler-lifecycle" -> "PackagedAssemblerLifecycleRegression";
                        case "assembler-roundtrip" -> "PackagedAssemblerLifecycleRegression";
                        case "airship-operation" -> "PackagedAirshipOperationRegression";
                        case "airship-bootstrap" -> "PackagedAirshipBootstrapRegression";
                        case "airship-preheat-launch" -> "PackagedAirshipPreheatLaunchRegression";
                        case "airship-refuel" -> "PackagedAirshipRefuelRegression";
                        case "airship-reload" -> "PackagedAirshipReloadRegression";
                        case "airship-propulsion" -> "PackagedAirshipPropulsionRegression";
                        case "plot-water" -> "PackagedPlotWaterRegression";
                        case "create-plot-retain" -> "PackagedCreatePlotRetainRegression";
                        case "bundle-startup" -> "PackagedBundleStartupRegression";
                        case "fabric-cleanup" -> "PackagedFabricCleanupRegression";
                        case "airship-recovery" -> "PackagedAirshipRecoveryRegression";
                        default -> throw new IllegalArgumentException("Unknown targeted regression");
                    };
                    try { Class.forName(helper).getMethod("run", ClientGameTestContext.class,
                            net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext.class,
                            java.util.List.class).invoke(null, test, world, checks); }
                    catch (ReflectiveOperationException failure) { throw new RuntimeException(failure); }
                } else if (Boolean.getBoolean("port.smoke.warningResources")) {
                    try { Class.forName("PackagedWarningResources").getMethod("run", ClientGameTestContext.class,
                            net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext.class,
                            java.util.List.class).invoke(null, test, world, checks); }
                    catch (ReflectiveOperationException failure) { throw new RuntimeException(failure); }
                } else if (Boolean.getBoolean("port.smoke.borehead")) {
                    try { Class.forName("PackagedBorehead").getMethod("run", ClientGameTestContext.class,
                            net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext.class,
                            java.util.List.class).invoke(null, test, world, checks); }
                    catch (ReflectiveOperationException failure) { throw new RuntimeException(failure); }
                } else if (Boolean.getBoolean("port.smoke.ponderBlockEntities")) {
                    try { Class.forName("PackagedPonderBlockEntities").getMethod("run", ClientGameTestContext.class,
                            net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext.class,
                            java.util.List.class).invoke(null, test, world, checks); }
                    catch (ReflectiveOperationException failure) { throw new RuntimeException(failure); }
                } else if (Boolean.getBoolean("port.smoke.itemResources") || Boolean.getBoolean("port.smoke.reportedCrash")) {
                    if (Boolean.getBoolean("port.smoke.itemResources")) {
                        try { Class.forName("PackagedItemResources").getMethod("run", ClientGameTestContext.class,
                            net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext.class,
                            java.util.List.class).invoke(null, test, world, checks); }
                        catch (ReflectiveOperationException failure) { throw new RuntimeException(failure); }
                    }
                    if (Boolean.getBoolean("port.smoke.itemGuiOnly")) {
                        try { Class.forName("PackagedItemResources").getMethod("runGuiOnly", ClientGameTestContext.class,
                            net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext.class,
                            java.util.List.class).invoke(null, test, world, checks); }
                        catch (ReflectiveOperationException failure) { throw new RuntimeException(failure); }
                    }
                    if (!System.getProperty("port.smoke.crashChecks", "all").equals("none")) {
                        try { Class.forName("PackagedCrashRegressions").getMethod("run", ClientGameTestContext.class,
                            net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext.class,
                            java.util.List.class).invoke(null, test, world, checks); }
                        catch (ReflectiveOperationException failure) { throw new RuntimeException(failure); }
                    }
                } else if (Boolean.getBoolean("port.smoke.aeronauticsFlight")) {
                    try { Class.forName("PackagedAeronauticsFlight").getMethod("run", ClientGameTestContext.class,
                            net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext.class,
                            java.util.List.class).invoke(null, test, world, checks); }
                    catch (ReflectiveOperationException failure) { throw new RuntimeException(failure); }
                } else {
                    if (Boolean.getBoolean("port.smoke.handDepth"))
                        test.runOnClient(client -> {
                            try { Class.forName("PackagedHandLightDepth").getMethod("run", net.minecraft.client.Minecraft.class,
                                    java.util.List.class).invoke(null, client, checks); }
                            catch (ReflectiveOperationException failure) { throw new RuntimeException(failure); }
                        });
                    if (Boolean.getBoolean("port.smoke.gustSync")) {
                        try { Class.forName("PackagedGustSync").getMethod("run", ClientGameTestContext.class,
                                net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext.class,
                                java.util.List.class).invoke(null, test, world, checks); }
                        catch (ReflectiveOperationException failure) { throw new RuntimeException(failure); }
                    }
                    if (Boolean.getBoolean("port.smoke.addonScenes")) {
                        try { Class.forName("PackagedAddonScenes").getMethod("run", ClientGameTestContext.class,
                                net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext.class,
                                java.util.List.class).invoke(null, test, world, checks); }
                        catch (ReflectiveOperationException failure) { throw new RuntimeException(failure); }
                    }
                    if (Boolean.getBoolean("port.smoke.endseaVisible")) {
                        try { Class.forName("PackagedEndSeaVisible").getMethod("run", ClientGameTestContext.class,
                                net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext.class,
                                java.util.List.class).invoke(null, test, world, checks); }
                        catch (ReflectiveOperationException failure) { throw new RuntimeException(failure); }
                    }
                }
                // The flight helper waits inside this live world until its control
                // file arrives. Keep-open mode performs no resource reload here.
                if (!Boolean.getBoolean("port.smoke.keepOpen") && !Boolean.getBoolean("port.smoke.itemResources") && !Boolean.getBoolean("port.smoke.reportedCrash") && !Boolean.getBoolean("port.smoke.ponderBlockEntities") && !Boolean.getBoolean("port.smoke.borehead") && !Boolean.getBoolean("port.smoke.warningResources") && System.getProperty("port.smoke.userRegression") == null) {
                    test.runOnClient(client -> reload.set(client.reloadResourcePacks()));
                    test.waitFor(client -> reload.get() != null && reload.get().isDone(), 400);
                    test.runOnClient(client -> reload.get().join());
                    checks.add("actual production resource reload while a world is open");
                }
            }
            test.waitFor(client -> client.level == null, 200);
            checks.add("world disconnect and renderer retirement");
            if ("fabric-cleanup".equals(System.getProperty("port.smoke.userRegression"))) {
                try { Class.forName("PackagedFabricCleanupRegression").getMethod("verifyStopped", ClientGameTestContext.class, java.util.List.class).invoke(null, test, checks); }
                catch (ReflectiveOperationException failure) { throw new RuntimeException(failure); }
            }

            if ("airship-reload".equals(System.getProperty("port.smoke.userRegression"))) {
                test.runOnClient(client -> {
                    if (client.getCameraEntity() != null) throw new AssertionError("Expected retired camera entity");
                    for (String name : new String[]{"SUB_LEVEL_VIEW", "SUB_LEVEL_VIEW_UNLOCKED"}) {
                        client.options.setCameraType(Enum.valueOf(net.minecraft.client.CameraType.class, name));
                        client.gameRenderer.update(client.getDeltaTracker());
                        if (client.options.getCameraType() != net.minecraft.client.CameraType.FIRST_PERSON)
                            throw new AssertionError("Ship camera survived disconnect: " + name);
                    }
                });
                checks.add("both ship camera modes retire with null level/camera during native renderer update");
            }
            report.put("status", "passed");
        } catch (Throwable failure) {
            failure.printStackTrace();
            report.put("status", "failed");
            report.put("error", failure.toString());
            throw new RuntimeException(failure);
        } finally {
            report.put("checks", checks);
            try { Files.writeString(Path.of(System.getProperty("port.smoke.report")), new GsonBuilder().setPrettyPrinting().create().toJson(report)); }
            catch (Exception failure) { throw new RuntimeException(failure); }
        }
    }
}
