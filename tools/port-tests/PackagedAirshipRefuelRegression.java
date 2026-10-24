import com.google.gson.GsonBuilder;
import com.simibubi.create.content.processing.burner.BlazeBurnerBlockEntity;
import dev.ryanhcode.sable.api.sublevel.SubLevelContainer;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import dev.simulated_team.simulated.content.blocks.portable_engine.PortableEngineBlockEntity;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
/** Refuel only the delivered ship through real native coal APIs in one server task. */
public final class PackagedAirshipRefuelRegression {
    public static void run(ClientGameTestContext test,TestSingleplayerContext world,List<String> checks)throws Exception{
        var report=new LinkedHashMap<String,Object>();var burners=new ArrayList<Object>();var engines=new ArrayList<Object>();report.put("burners",burners);report.put("engines",engines);
        report.put("scope","Coal only: native committed blaze-burner fuel to10000ticks; native portable-engine inventory top-up to64coal. No commands, controls, player/gamemode/host/name changes, creative cake, infinite fuel, physics writes or flight/UI tests.");
        var path=Path.of(System.getProperty("port.smoke.report")).resolveSibling("airship-refuel.json");
        try{
            world.getServer().runOnServer(s->{
                var all=SubLevelContainer.getContainer(s.overworld()).getAllSubLevels();check(all.size()==1,"Expected exactly one delivered ship");var ship=(ServerSubLevel)all.getFirst();
                report.put("ship",ship.getUniqueId().toString());var before=identity(s);report.put("beforeIdentity",before);
                var heaters=new ArrayList<BlazeBurnerBlockEntity>();var motors=new ArrayList<PortableEngineBlockEntity>();
                for(var chunk:ship.getPlot().getLoadedChunks())for(var be:chunk.getChunk().getBlockEntities().values()){if(be instanceof BlazeBurnerBlockEntity b)heaters.add(b);if(be instanceof PortableEngineBlockEntity e)motors.add(e);}
                check(heaters.size()==20&&motors.size()==2,"Expected20burners+2engines: "+heaters.size()+"/"+motors.size());int minimum=Integer.MAX_VALUE;
                for(var burner:heaters){
                    int initial=burner.getRemainingBurnTime(),coal=0;check(!burner.isCreative(),"Creative fuel already present; coal-only scope cannot replace it");
                    for(int attempt=0;attempt<64&&burner.getRemainingBurnTime()<BlazeBurnerBlockEntity.MAX_HEAT_CAPACITY;attempt++){
                        int previous=burner.getRemainingBurnTime();boolean accepted=burner.tryFuel(new ItemStack(Items.COAL),true,false);int next=burner.getRemainingBurnTime();if(next>previous)coal++;if(!accepted||next<=previous)break;
                    }
                    int after=burner.getRemainingBurnTime();burners.add(Map.of("pos",burner.getBlockPos().toShortString(),"beforeBurnTicks",initial,"afterBurnTicks",after,"coalItemsApplied",coal));minimum=Math.min(minimum,after);check(after==BlazeBurnerBlockEntity.MAX_HEAT_CAPACITY,"Native coal refuel did not reach burner capacity: "+burner.getBlockPos()+"="+after);
                }
                for(var engine:motors){
                    var previous=engine.inventory.getItem(0).copy();check(previous.isEmpty()||previous.is(Items.COAL),"Engine contains another fuel; do not replace user inventory");int count=previous.isEmpty()?0:previous.getCount();var remainder=engine.inventory.insertSlot(new ItemStack(Items.COAL,64),0,false);var after=engine.inventory.getItem(0);int added=64-remainder.getCount();
                    engines.add(Map.of("pos",engine.getBlockPos().toShortString(),"beforeCoalCount",count,"afterCoalCount",after.getCount(),"coalItemsAdded",added,"currentBurnTicks",engine.getCurrentBurnTime()));check(after.is(Items.COAL)&&after.getCount()==64&&after.getCount()-count==added,"Engine native coal top-up failed");
                }
                report.put("minimumBurnTicks",minimum);var after=identity(s);report.put("afterIdentity",after);check(before.equals(after),"Player/host/world identity changed during refuel task");s.saveEverything(false,true,true);
            });
            report.put("status","passed");checks.add("Delivered ship refuel only:20native blaze burners at10000coal burnticks and2portable engines topped to64coal; player/host/name/control state untouched");
        }catch(Throwable failure){report.put("status","failed");report.put("error",failure.toString());throw failure;}
        finally{Files.writeString(path,new GsonBuilder().setPrettyPrinting().create().toJson(report));}
    }
    private static Map<String,Object> identity(MinecraftServer s){var p=s.getPlayerList().getPlayers().getFirst();var pos=p.position();var host=s.getWorldData().getSinglePlayerUUID();return Map.of("hostUUID",host==null?"null":host.toString(),"levelName",s.getWorldData().getLevelName(),"playerPosition",List.of(pos.x,pos.y,pos.z),"playerYaw",p.getYRot(),"playerPitch",p.getXRot(),"creative",p.isCreative(),"spectator",p.isSpectator());}
    private static void check(boolean value,String message){if(!value)throw new AssertionError(message);}
    private PackagedAirshipRefuelRegression(){}
}
