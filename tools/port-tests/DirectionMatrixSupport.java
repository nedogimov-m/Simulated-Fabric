import com.google.gson.*;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;

/** Durable, phase-specific records for real production-world direction cases. */
public final class DirectionMatrixSupport {
    private static final Gson JSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Map<String, JsonObject> JOURNALS = new HashMap<>();
    private static final Path WORK = Path.of(System.getProperty("port.smoke.report")).getParent();
    private static final String PHASE = System.getProperty("port.matrix.phase", "initial");
    private static final String INPUT_HASH = inputHash();
    public static String phase() { return PHASE; }
    public static ServerLevel level(MinecraftServer server) { return server.overworld(); }
    public static BlockPos base(String group, int index) {
        int g = switch (group) { case "gearshift" -> 0; case "analog" -> 1; case "borehead" -> 2; case "swivel" -> 3; case "torsion" -> 4; default -> throw new IllegalArgumentException("Unknown group: " + group); };
        return new BlockPos(16 + g % 2 * 512 + index % 16 * 12,
                -32 + index / 256 * 24, 16 + g / 2 * 512 + index / 16 % 16 * 12);
    }
    public static boolean done(String group, String id) {
        var entry = journal(group).getAsJsonObject("cases").getAsJsonObject(PHASE + "/" + id);
        return entry != null && entry.has("status") && entry.get("status").getAsString().equals("passed");
    }
    public static void pass(String group, String id, Map<String, Object> evidence) {
        if (done(group, id)) return;
        var entry = new JsonObject(); entry.addProperty("status", "passed");
        entry.addProperty("phase", PHASE); entry.addProperty("inputsSha256", INPUT_HASH);
        entry.add("evidence", JSON.toJsonTree(evidence));
        journal(group).getAsJsonObject("cases").add(PHASE + "/" + id, entry); flush(group);
        long count = journal(group).getAsJsonObject("cases").entrySet().stream().filter(e -> e.getValue().getAsJsonObject().get("status").getAsString().equals("passed")).count();
        if (count % 48 == 0) System.out.println("DIRECTION MATRIX " + group + " recorded=" + count + " latest=" + id);
    }
    public static void fail(String group, String id, Throwable failure) {
        var entry = new JsonObject(); entry.addProperty("status", "failed"); entry.addProperty("phase", PHASE);
        entry.addProperty("inputsSha256", INPUT_HASH); entry.addProperty("error", failure.toString());
        journal(group).getAsJsonObject("cases").add(PHASE + "/" + id, entry); flush(group);
    }
    public static void check(boolean condition, String message) { if (!condition) throw new AssertionError(message); }
    public static Path journalPath(String group) { return WORK.resolve("direction-" + group + ".json"); }
    public static Map<String, Object> summary(String group) {
        var cases = journal(group).getAsJsonObject("cases");
        long passed = cases.entrySet().stream().filter(e -> e.getValue().getAsJsonObject().get("status").getAsString().equals("passed")).count();
        return Map.of("group", group, "phase", PHASE, "passedCasesAcrossPhases", passed, "journal", journalPath(group).toString(), "inputsSha256", INPUT_HASH);
    }
    private static JsonObject journal(String group) {
        return JOURNALS.computeIfAbsent(group, key -> {
            try {
                if (Files.exists(journalPath(group))) return JsonParser.parseString(Files.readString(journalPath(group))).getAsJsonObject();
                var result = new JsonObject(); result.addProperty("group", group);
                result.addProperty("scope", "Real registered production blocks, native networks and native saved world across separate dedicated JVM processes. Passed rows retain their original input hashes; they are not silently reattributed to later builds.");
                result.add("cases", new JsonObject()); return result;
            } catch (Exception failure) { throw new RuntimeException(failure); }
        });
    }
    private static void flush(String group) {
        try {
            var target = journalPath(group); var pending = target.resolveSibling(target.getFileName() + ".tmp");
            Files.writeString(pending, JSON.toJson(journal(group)) + "\n");
            Files.move(pending, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (Exception failure) { throw new RuntimeException(failure); }
    }
    private static String inputHash() {
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(WORK.resolve("inputs.json")))); }
        catch (Exception failure) { throw new RuntimeException(failure); }
    }
    private DirectionMatrixSupport() {}
}
