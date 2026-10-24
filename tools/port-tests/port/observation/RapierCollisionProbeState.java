package port.observation;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Bounded read-only test observations; outside the mixin transformation namespace. */
public final class RapierCollisionProbeState {
    public static final Method NATIVE_CLEAR = nativeClear();
    public static volatile long calls;
    public static volatile long totalEvents;
    public static volatile long latestScene;
    public static volatile double[] latest = new double[0];
    public static final List<Map<String,Object>> firstEvents = new ArrayList<>();
    private static final Map<Long,Long> sceneEvents = new LinkedHashMap<>();

    private static Method nativeClear() {
        try {
            Method method = Class.forName("dev.ryanhcode.sable.physics.impl.rapier.Rapier3D")
                    .getDeclaredMethod("clearCollisions", long.class);
            method.setAccessible(true);
            return method;
        } catch (ReflectiveOperationException failure) {
            throw new ExceptionInInitializerError(failure);
        }
    }

    public static synchronized void observe(long scene, double[] collisions) {
        calls++;
        int count = collisions.length / 15;
        totalEvents += count;
        sceneEvents.merge(scene, (long) count, Long::sum);
        if (count == 0) return;
        latestScene = scene;
        latest = Arrays.copyOf(collisions, Math.min(collisions.length, 20 * 15));
        for (int i = 0; i < count && firstEvents.size() < 20; i++) {
            firstEvents.add(Map.of("scene", scene, "call", calls,
                    "event", Arrays.copyOfRange(collisions, i * 15, (i + 1) * 15)));
        }
    }

    public static synchronized Map<String,Object> snapshot() {
        var report = new LinkedHashMap<String,Object>();
        report.put("calls", calls);
        report.put("totalEvents", totalEvents);
        report.put("sceneEvents", new LinkedHashMap<>(sceneEvents));
        report.put("latestScene", latestScene);
        report.put("latest", latest.clone());
        report.put("firstEvents", new ArrayList<>(firstEvents));
        report.put("eventFormat", "15 doubles: bodyA,bodyB,force,normalAxyz,normalBxyz,pointAxyz,pointBxyz");
        return report;
    }

    private RapierCollisionProbeState() {}
}
