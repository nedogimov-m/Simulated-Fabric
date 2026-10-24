import com.google.gson.GsonBuilder;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/** Separate targeted driver: no activation batch, previous gameplay checks or replacement classes. */
public final class DirectionMatrixServer implements ModInitializer {
    private int ticks;
    private final List<String> checks = new ArrayList<>();
    private boolean finished;
    @Override public void onInitialize() {
        String group = System.getProperty("port.matrix.group");
        String owner = switch (group) { case "gearshift" -> "DirectionGearshiftMatrix"; case "analog" -> "DirectionAnalogMatrix"; case "borehead" -> "DirectionBoreheadMatrix"; case "swivel" -> "DirectionSwivelMatrix"; case "torsion" -> "DirectionTorsionMatrix"; default -> throw new IllegalArgumentException(group); };
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            if (finished || ++ticks < 20) return;
            var report = new LinkedHashMap<String, Object>();
            try {
                if (ticks == 20) server.tickRateManager().setTickRate(Float.parseFloat(System.getProperty("port.matrix.tickRate", "200")));
                boolean done = (boolean) Class.forName(owner).getMethod("tick", net.minecraft.server.MinecraftServer.class, List.class).invoke(null, server, checks);
                if (!done) return;
                report.put("status", "passed");
            } catch (Throwable failure) {
                while (failure instanceof java.lang.reflect.InvocationTargetException reflected && reflected.getCause() != null) failure = reflected.getCause();
                failure.printStackTrace(); DirectionMatrixSupport.fail(group, "run-failure", failure);
                report.put("status", "failed"); report.put("error", failure.toString());
            }
            finished = true;
            report.put("ticks", ticks); report.put("checks", checks); report.put("matrix", DirectionMatrixSupport.summary(group));
            report.put("worldPersistence", "Real dedicated server clean shutdown saves the world; phase restore starts another JVM from those bytes.");
            try { Files.writeString(Path.of(System.getProperty("port.smoke.report")), new GsonBuilder().setPrettyPrinting().create().toJson(report)); }
            catch (Exception failure) { throw new RuntimeException(failure); }
            server.halt(false);
        });
    }
}
