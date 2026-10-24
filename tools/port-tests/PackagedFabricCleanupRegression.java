import com.google.gson.GsonBuilder;
import dev.ryanhcode.sable.SableServerConfig;
import dev.ryanhcode.sable.api.sublevel.SubLevelContainer;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.util.LinkedHashMap;
import java.util.List;

/** New Fabric-owned configuration lifetime and dependency cleanup, in an isolated world. */
public final class PackagedFabricCleanupRegression {
    public static void run(ClientGameTestContext test,TestSingleplayerContext world,List<String> checks) throws Exception {
        var report=new LinkedHashMap<String,Object>();
        var path=Path.of(System.getProperty("port.smoke.report")).resolveSibling("fabric-cleanup.json");
        try {
            PackagedBundleStartupRegression.run(test,world,checks);
            if(FabricLoader.getInstance().isModLoaded("forgeconfigapiport"))throw new AssertionError("Legacy configuration bridge still loaded");
            try {Class.forName("net.neoforged.neoforge.common.ModConfigSpec");throw new AssertionError("Old configuration API still present");}
            catch(ClassNotFoundException expected) {report.put("legacyApiAbsent",true);}
            var server=world.getServer();
            server.runOnServer(s->{
                var file=SableServerConfig.SPEC.path();
                if(file==null||!file.getParent().getFileName().toString().equals("serverconfig")||!Files.isRegularFile(file))throw new AssertionError("Server configuration was not bound to the actual world");
                if(SableServerConfig.SUB_LEVEL_SUBSTEPS_PER_TICK.getAsInt()!=2)throw new AssertionError("Initial physics setting differs");
                report.put("worldConfig",file.toString());
                try {
                    var text=Files.readString(file);if(!text.contains("sub_level_substeps_per_tick = 2"))throw new AssertionError("Unexpected native file format");
                    Files.writeString(file,text.replace("sub_level_substeps_per_tick = 2","sub_level_substeps_per_tick = 3"));
                    Files.setLastModifiedTime(file,FileTime.fromMillis(System.currentTimeMillis()+2000));
                }catch(java.io.IOException failure){throw new java.io.UncheckedIOException(failure);}
            });
            server.waitFor(s->{
                if(SableServerConfig.SUB_LEVEL_SUBSTEPS_PER_TICK.getAsInt()!=3)return false;
                for(var level:s.getAllLevels())if(SubLevelContainer.getContainer(level).physicsSystem().getConfig().substepsPerTick!=3)return false;
                return true;
            },120);
            report.put("nativeFileHotReloadedSubsteps",3);
            report.put("status","passed");checks.add("Fabric-only Sable configuration binds real world/serverconfig, disk edit updates every native physics pipeline without legacy API");
        }catch(Throwable failure){report.put("status","failed");report.put("error",failure.toString());throw failure;}
        finally{Files.writeString(path,new GsonBuilder().setPrettyPrinting().create().toJson(report));}
    }
    public static void verifyStopped(ClientGameTestContext test,List<String> checks) {
        test.waitFor(client -> SableServerConfig.SPEC.path() == null, 200);
        if (SableServerConfig.SUB_LEVEL_SUBSTEPS_PER_TICK.getAsInt() != 2)
            throw new AssertionError("World configuration leaked past server shutdown");
        checks.add("Fabric server-stop event detaches world file and restores default physics settings");
    }
    private PackagedFabricCleanupRegression(){}
}
