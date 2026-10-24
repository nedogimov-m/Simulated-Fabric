import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.metadata.ModOrigin;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.core.registries.BuiltInRegistries;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Only the changed bundle discovery/startup boundary; no accepted gameplay replay. */
public final class PackagedBundleStartupRegression {
    public static void run(ClientGameTestContext test, TestSingleplayerContext world, List<String> checks) throws Exception {
        var report=new LinkedHashMap<String,Object>();var components=new ArrayList<Object>();report.put("components",components);
        report.put("scope","Single Aeronautics bundle native Fabric discovery, component entrypoints, resources, registry initialization and real integrated world. No previous flight, walking, controls or directions tests are replayed.");
        var path=Path.of(System.getProperty("port.smoke.report")).resolveSibling("bundle-startup.json");
        try {
            test.runOnClient(client->{
                var loader=FabricLoader.getInstance();
                if(loader.getModContainer("aeronautics_bundle").isEmpty())throw new AssertionError("Bundle was not discovered");
                for(String id:List.of("simulated","aeronautics","offroad")) {
                    var mod=loader.getModContainer(id).orElseThrow();var origin=mod.getOrigin();
                    if(origin.getKind()!=ModOrigin.Kind.NESTED||!origin.getParentModId().equals("aeronautics_bundle"))throw new AssertionError("Expected nested component "+id+": "+origin);
                    long count=BuiltInRegistries.BLOCK.keySet().stream().filter(key->key.getNamespace().equals(id)).count();
                    if(count==0)throw new AssertionError("Component blocks were not registered: "+id);
                    components.add(Map.of("id",id,"version",mod.getMetadata().getVersion().getFriendlyString(),"origin",origin.getKind().toString(),"parent",origin.getParentModId(),"registeredBlocks",count));
                }
            });
            report.put("status","passed");checks.add("One bundle discovers all3nested production components; native entrypoints/resources/blocks initialize and integrated world opens");
        } catch(Throwable failure){report.put("status","failed");report.put("error",failure.toString());throw failure;}
        finally {Files.writeString(path,new GsonBuilder().setPrettyPrinting().create().toJson(report));}
    }
    private PackagedBundleStartupRegression(){}
}
