package port.observation;

import java.util.IdentityHashMap;
import java.util.Map;
import net.createmod.ponder.foundation.PonderScene;
import net.createmod.ponder.foundation.ui.PonderViewport;

/** Observes actual extraction, native source submission and native PIP blitting. No synthetic callbacks. */
public final class GameplayPonder {
    private static volatile Sample active;

    public record FrameReceipt(int sceneTime, int simulationTime, int sections, int blockModels,
        int blockEntities, int entities, int sourceItems, int texts, int inputs, int outlines,
        int fluidSections, int breakingSections, int validatedMeshFaces, boolean nativeSubmissionReturned, boolean nativeBlitReturned) {}

    public static final class Sample {
        final PonderScene scene;
        final Map<PonderViewport.State, Observation> observations = new IdentityHashMap<>();
        volatile Throwable failure;
        int validatedMeshFaces;
        Sample(PonderScene scene) { this.scene = scene; }
        public synchronized FrameReceipt completed() {
            for (var entry : observations.entrySet()) {
                var observation = entry.getValue();
                if (observation.submitted && observation.blitted) return receipt(entry.getKey(), observation);
            }
            return null;
        }
        public synchronized PonderViewport.State completedState() {
            for (var entry : observations.entrySet()) {
                var observation = entry.getValue();
                if (observation.submitted && observation.blitted) return entry.getKey();
            }
            return null;
        }
        public Throwable failure() { return failure; }
        private FrameReceipt receipt(PonderViewport.State state, Observation observation) {
            var frame = state.frame();
            return new FrameReceipt(observation.time, observation.simulationTime, frame.sections().size(),
                frame.sections().stream().mapToInt(section -> section.blocks().size()).sum(),
                frame.sections().stream().mapToInt(section -> section.blockEntities().size()).sum(),
                frame.entities().size(), frame.items().size(), frame.texts().size(), frame.inputs().size(),
                frame.outlines().size(), state.fluids().size(), state.breaking().size(), validatedMeshFaces, observation.submitted, observation.blitted);
        }
    }

    private static final class Observation {
        final int time, simulationTime;
        boolean submitted, blitted;
        Observation(PonderScene scene) { time = scene.getCurrentTime(); simulationTime = scene.getWorld().simulationTicks(); }
    }

    public static Sample begin(PonderScene scene) {
        var sample = new Sample(scene); active = sample; return sample;
    }
    public static void clear() { active = null; }

    public static void extracted(PonderScene scene, PonderViewport.State state) {
        var sample = active;
        if (sample == null || sample.scene != scene) return;
        synchronized (sample) { sample.observations.put(state, new Observation(scene)); }
    }

    public static void submitted(PonderViewport.State state) {
        var sample = active;
        if (sample == null) return;
        synchronized (sample) {
            var observation = sample.observations.get(state);
            if (observation == null) return;
            if (!observation.submitted) {
                try {
                    var meshes = java.util.Collections.newSetFromMap(new java.util.IdentityHashMap<net.fabricmc.fabric.api.client.renderer.v1.mesh.Mesh, Boolean>());
                    for (var section : state.frame().sections()) {
                        if (!section.transform().isFinite()) throw new AssertionError("Nonfinite actual Ponder section transform");
                        for (var block : section.blocks()) if (block.mesh != null) meshes.add(block.mesh);
                        for (var owner : section.blockEntities()) if (owner.state() instanceof dev.simulated_team.simulated.fabric.registry.NativePartsRenderer.State parts) {
                            var field = parts.getClass().getDeclaredField("parts"); field.setAccessible(true);
                            for (var value : (java.util.List<?>) field.get(parts)) {
                                var part = (dev.simulated_team.simulated.fabric.registry.NativePartsRenderer.Part) value;
                                if (!part.transform().isFinite()) throw new AssertionError("Nonfinite native addon part transform at " + owner.position());
                                meshes.add(part.mesh());
                            }
                        }
                    }
                    for (var mesh : meshes) validateMesh(sample, mesh);
                } catch (Throwable failure) { sample.failure = failure; return; }
            }
            for (var section : state.frame().sections()) for (var owner : section.blockEntities()) {
                if (owner.failure().hasFailed()) {
                    sample.failure = new AssertionError("Actual native BE submission failed at " + owner.position()
                        + ", state=" + owner.state().getClass().getName());
                    return;
                }
            }
            observation.submitted = true;
        }
    }

    public static void blitted(PonderViewport.State state) {
        var sample = active;
        if (sample == null) return;
        synchronized (sample) {
            var observation = sample.observations.get(state);
            if (observation != null) observation.blitted = true;
        }
    }

    private static void validateMesh(Sample sample, net.fabricmc.fabric.api.client.renderer.v1.mesh.Mesh mesh) {
        mesh.forEach(quad -> {
            sample.validatedMeshFaces++;
            var atlas = net.minecraft.client.Minecraft.getInstance().getAtlasManager().getAtlasOrThrow(quad.atlas().getId());
            var missing = atlas.getSprite(net.minecraft.client.renderer.texture.MissingTextureAtlasSprite.getLocation());
            boolean samplesMissing = true;
            for (int v = 0; v < 4; v++) {
                if (!Float.isFinite(quad.x(v) + quad.y(v) + quad.z(v) + quad.u(v) + quad.v(v)))
                    throw new AssertionError("Nonfinite actual Ponder mesh position/UV");
                samplesMissing &= quad.u(v) >= missing.getU0() && quad.u(v) <= missing.getU1()
                        && quad.v(v) >= missing.getV0() && quad.v(v) <= missing.getV1();
            }
            if (samplesMissing) throw new AssertionError("Actual native Ponder mesh samples missing-texture UVs in " + quad.atlas().getId());
        });
    }

    private GameplayPonder() {}
}
