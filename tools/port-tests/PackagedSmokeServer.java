import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.loader.api.FabricLoader;
import java.nio.file.Files;
import java.nio.file.Path;

/** Drives a genuine dedicated server; contains no replacement production classes. */
public final class PackagedSmokeServer implements ModInitializer {
    private int ticks;
    private final java.util.List<String> servicesChecks = new java.util.ArrayList<>();
    @Override public void onInitialize() {
        if (FabricLoader.getInstance().getEnvironmentType() != net.fabricmc.api.EnvType.SERVER) return;
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            if (++ticks < 20) return;
            boolean servicesOnly = Boolean.getBoolean("port.smoke.servicesOnly");
            if (!servicesOnly && ticks != 20) return;
            if (!servicesOnly) PackagedActivation.verify();
            try {
                if (server.overworld() == null) throw new AssertionError("Dedicated overworld was not loaded");
                var checks = servicesOnly ? this.servicesChecks : new java.util.ArrayList<String>();
                if (servicesOnly && !(Boolean) Class.forName("PackagedNativeServices").getMethod("runOnServer",
                        net.minecraft.server.MinecraftServer.class, java.util.List.class).invoke(null, server, checks)) return;
                checks.add("production entrypoints and required server mixins");
                checks.add("actual overworld creation and twenty ticks");
                if (Boolean.getBoolean("port.smoke.dyeRecipe"))
                    Class.forName("PackagedDyeRecipe").getMethod("runOnServer", net.minecraft.server.MinecraftServer.class,
                            java.util.List.class).invoke(null, server, checks);
                Files.writeString(Path.of(System.getProperty("port.smoke.report")),
                        new com.google.gson.GsonBuilder().setPrettyPrinting().create().toJson(java.util.Map.of(
                                "status", "passed", "side", "dedicated-server", "ticks", ticks, "checks", checks)));
            } catch (Exception failure) { throw new RuntimeException(failure); }
            server.halt(false);
        });
    }
}
