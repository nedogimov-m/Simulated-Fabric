import com.google.gson.GsonBuilder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;

/** Applies the registered production mixins to their real target classes in one batch. */
public final class PackagedActivation {
    public static void verify() {
        var report = new LinkedHashMap<String,Object>();
        var passed = new ArrayList<String>();
        var failures = new LinkedHashMap<String,String>();
        try (var stream = PackagedActivation.class.getResourceAsStream("/packaged-activation-targets.txt")) {
            if (stream == null) throw new AssertionError("Missing activation target inventory");
            for (String name : new String(stream.readAllBytes(), StandardCharsets.UTF_8).split("\\R")) {
                if (name.isBlank()) continue;
                try {
                    Class.forName(name, false, PackagedActivation.class.getClassLoader());
                    passed.add(name);
                } catch (Throwable failure) {
                    failure.printStackTrace();
                    failures.put(name, failure.toString());
                }
            }
            report.put("status", failures.isEmpty() ? "passed" : "failed");
            report.put("scope", "Class loading applies actual registered production mixins; gameplay behavior is checked separately.");
            report.put("passed", passed);
            report.put("failures", failures);
            Files.writeString(Path.of(System.getProperty("port.smoke.activationReport")), new GsonBuilder().setPrettyPrinting().create().toJson(report));
        } catch (Exception failure) { throw new RuntimeException(failure); }
        if (!failures.isEmpty()) throw new AssertionError("Production Mixin target application failed for " + failures.size() + " classes; see activation.json");
    }
    private PackagedActivation() { }
}
