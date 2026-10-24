import com.google.gson.GsonBuilder;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import dev.simulated_team.simulated.network.packets.AssemblePacket;
import dev.eriksonn.aeronautics.content.blocks.hot_air.balloon.map.BalloonMap;
import dev.eriksonn.aeronautics.content.blocks.hot_air.balloon.ServerBalloon;
import com.simibubi.create.content.schematics.SchematicFiles;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.content.processing.burner.BlazeBurnerBlockEntity;
import com.simibubi.create.content.fluids.tank.FluidTankBlockEntity;
import com.simibubi.create.content.redstone.link.RedstoneLinkBlockEntity;
import dev.eriksonn.aeronautics.content.blocks.hot_air.steam_vent.SteamVentBlockEntity;
import dev.ryanhcode.sable.api.physics.handle.RigidBodyHandle;
import dev.ryanhcode.sable.api.sublevel.SubLevelContainer;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import dev.simulated_team.simulated.content.blocks.physics_assembler.PhysicsAssemblerBlockEntity;
import dev.simulated_team.simulated.content.blocks.portable_engine.PortableEngineBlockEntity;
import dev.simulated_team.simulated.content.blocks.throttle_lever.ThrottleLeverBlockEntity;
import dev.simulated_team.simulated.content.blocks.throttle_lever.ThrottleLeverBlock;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeverBlock;
import org.joml.Vector3d;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
/** New operating-input scope on the accepted original ship; no assembly or UI replay. */
public final class PackagedAirshipPreheatLaunchRegression {
    private static final BlockPos ANCHOR=new BlockPos(16,128,16),ASSEMBLER=ANCHOR.offset(20,23,71);
    private static final List<BlockPos> VENTS=List.of(new BlockPos(20,24,48),new BlockPos(20,24,49),new BlockPos(20,24,50),new BlockPos(20,37,52));
    private static final List<BlockPos> BOILERS=List.of(new BlockPos(19,18,48),new BlockPos(19,26,44),new BlockPos(19,31,52));
    private static final List<BlockPos> PUMPS=new ArrayList<>();
    private static final List<BlockPos> WATER=new ArrayList<>();
    private static net.minecraft.server.level.ServerLevel level;private static ServerSubLevel ship;private static BlockPos offset=BlockPos.ZERO;private static double startY;
    public static void run(ClientGameTestContext test,TestSingleplayerContext world,List<String> checks)throws Exception{
        var report=new LinkedHashMap<String,Object>();var samples=new ArrayList<Object>();report.put("samples",samples);
        report.put("scope","New PREHEAT-THEN-ASSEMBLE workflow on original fully printed static ship: supply coal, original8water and paper pair before actual client AssemblePacket; verify stored balloon gas and sustained four-vent/two-boiler readiness, then >=3m rise ABOVE assembled launch height and sustained operation. No water replacement after transfer, no joint/pose/force/RPM/motor/boiler/gas field writes. Previous cold-start recovery remains separate.");
        var path=Path.of(System.getProperty("port.smoke.report")).resolveSibling("airship-preheat-launch.json");
        try{
            var server=world.getServer();var original=Path.of(System.getProperty("port.regression.schematic"));var raw=SchematicFiles.readTag(original);
            server.runCommand("gamemode creative @a");server.runCommand("time set noon");server.runCommand("weather clear");
            server.runOnServer(s->{
                level=s.overworld();check(SubLevelContainer.getContainer(level).getAllSubLevels().isEmpty(),"Expected original unassembled source world");offset=BlockPos.ZERO;report.put("beforeOperatingInputs",sample());
                CompoundTag source;try{source=SchematicFiles.decode(level,raw).save(new CompoundTag());}catch(java.io.IOException e){throw new RuntimeException(e);}var palette=source.getListOrEmpty("palette");
                int waters=0,burners=0,engines=0,vanilla=0,throttles=0;var changes=new ArrayList<Object>();
                for(var e:source.getListOrEmpty("blocks")){
                    var block=(CompoundTag)e;var pos=block.getListOrEmpty("pos");var relative=new BlockPos(pos.getIntOr(0,0),pos.getIntOr(1,0),pos.getIntOr(2,0));var local=local(relative);var expected=NbtUtils.readBlockState(level.registryAccess().lookupOrThrow(Registries.BLOCK),palette.getCompoundOrEmpty(block.getIntOr("state",0)));var be=level.getBlockEntity(local);
                    if(expected.is(AllBlocks.MECHANICAL_PUMP))PUMPS.add(local);
                    if(expected.is(Blocks.WATER)){level.setBlockAndUpdate(local,expected);WATER.add(relative);waters++;changes.add(Map.of("sourceRelative",relative.toShortString(),"input","original water source cell"));}
                    if(expected.is(Blocks.LEVER)){level.setBlockAndUpdate(local,expected);vanilla++;}
                    if(be instanceof ThrottleLeverBlockEntity throttle){int rawState=block.getCompoundOrEmpty("nbt").getIntOr("State",0);boolean inverted=throttle.getBlockState().getValue(ThrottleLeverBlock.INVERTED);throttle.setSignal(inverted?15-rawState:rawState);throttle.setSignal(inverted?0:15);throttles++;changes.add(Map.of("sourceRelative",relative.toShortString(),"originalRawState",rawState,"requestedRawState",15,"inverted",inverted));}
                    if(be instanceof BlazeBurnerBlockEntity burner){check(burner.tryFuel(new ItemStack(Items.COAL),true,false),"Native burner rejected coal: "+relative);burners++;changes.add(Map.of("sourceRelative",relative.toShortString(),"input","one coal through native committed tryFuel","burnTime",burner.getRemainingBurnTime()));}
                    if(be instanceof PortableEngineBlockEntity engine){var remainder=engine.inventory.insertSlot(new ItemStack(Items.COAL,64),0,false);check(remainder.isEmpty(),"Native engine rejected coal: "+relative);engines++;changes.add(Map.of("sourceRelative",relative.toShortString(),"input","64coal through native inventory"));}
                }
                check(waters==8&&burners==20&&engines==2&&vanilla==6&&throttles==5&&PUMPS.size()==9,"Original input/control count differs: "+List.of(waters,burners,engines,vanilla,throttles));report.put("operatingInputs",changes);
                var links=new ArrayList<Object>();for(var relative:List.of(new BlockPos(18,24,71),new BlockPos(19,37,52))){var link=(RedstoneLinkBlockEntity)level.getBlockEntity(local(relative));check(link!=null,"Original redstone link missing");var before=link.getLink().getNetworkKey().toString();link.getLink().setFrequency(true,new ItemStack(Items.PAPER));link.getLink().setFrequency(false,new ItemStack(Items.PAPER));links.add(Map.of("sourceRelative",relative.toShortString(),"before",before,"after",link.getLink().getNetworkKey().toString()));}report.put("actualRfNetworkKeys",links);
                report.put("waterBeforePreheat",waterCells());report.put("testJointInstalled",false);
            });
            var warm=new ArrayList<Object>();report.put("staticPreheatSamples",warm);long time=server.computeOnServer(s->level.getGameTime());int readyTicks=0,preheatTicks=0;
            for(int tick=1;tick<=600;tick++){final long previous=time;server.waitFor(s->level.getGameTime()>previous,120);time=server.computeOnServer(s->level.getGameTime());var row=server.computeOnServer(s->sample());preheatTicks=tick;if(ready(row)&&((Number)row.get("storedFilledVolume")).doubleValue()>.95*((Number)row.get("storedTargetVolume")).doubleValue()&&((Number)row.get("storedTargetVolume")).doubleValue()>0)readyTicks++;else readyTicks=0;if(tick==1||tick%10==0)warm.add(row);if(readyTicks>=100)break;}
            report.put("staticPreheatTicks",preheatTicks);report.put("waterAfterPreheat",server.computeOnServer(s->waterCells()));report.put("preheated",server.computeOnServer(s->sample()));check(readyTicks>=100,"Static preheat did not maintain both operating boiler supplies and all4vents for100ticks with filled balloons");
            server.runOnServer(s->{var p=s.getPlayerList().getPlayers().getFirst();p.getAbilities().flying=true;p.onUpdateAbilities();});
            server.runCommand("tp @a 37.5 151.5 89.5");server.waitFor(s->acked(s.getPlayerList().getPlayers().getFirst().connection),120);
            test.waitFor(c->c.level.getBlockEntity(ASSEMBLER) instanceof PhysicsAssemblerBlockEntity,200);test.runOnClient(c->ClientPlayNetworking.send(new AssemblePacket(ASSEMBLER)));
            server.waitFor(s->{var all=SubLevelContainer.getContainer(level).getAllSubLevels();if(all.isEmpty())return false;check(all.size()==1,"Expected exactly one newly assembled ship");ship=(ServerSubLevel)all.getFirst();if(offset.equals(BlockPos.ZERO)){BlockPos moved=null;for(var c:ship.getPlot().getLoadedChunks())for(var be:c.getChunk().getBlockEntities().values())if(be instanceof PhysicsAssemblerBlockEntity a&&a.isPrimaryAssembler())moved=be.getBlockPos();check(moved!=null,"Moved primary assembler missing");offset=moved.subtract(ASSEMBLER);for(int i=0;i<PUMPS.size();i++)PUMPS.set(i,PUMPS.get(i).offset(offset));}return true;},400);
            startY=server.computeOnServer(s->ship.logicalPose().position().y());report.put("launchY",startY);report.put("ship",ship.getUniqueId().toString());report.put("waterImmediatelyAfterTransfer",server.computeOnServer(s->waterCells()));
            for(var rel:WATER)check(server.computeOnServer(s->level.getBlockState(local(rel)).is(Blocks.WATER)),"Original water cell lost during actual assembly: "+rel);
            double maxY=startY,minY=startY;int sustained=0,observed=0;time=server.computeOnServer(s->level.getGameTime());
            for(int tick=1;tick<=600;tick++){final long previous=time;server.waitFor(s->level.getGameTime()>previous,120);time=server.computeOnServer(s->level.getGameTime());var row=server.computeOnServer(s->sample());observed=tick;double y=((Number)row.get("height")).doubleValue();maxY=Math.max(maxY,y);minY=Math.min(minY,y);if(ready(row))sustained++;else sustained=0;if(tick==1||tick%10==0)samples.add(row);if(maxY-startY>=3&&sustained>=100)break;}
            report.put("observedFlightServerTicks",observed);report.put("maximumRiseAboveLaunch",maxY-startY);report.put("maximumDropBelowLaunch",startY-minY);report.put("finalSustainedReadyTicks",sustained);report.put("final",server.computeOnServer(s->sample()));report.put("waterAfterFlight",server.computeOnServer(s->waterCells()));
            check(maxY-startY>=3&&sustained>=100,"Preheated launch did not rise3m above launch AND retain both operating boilers/all4vents for final100ticks");
            report.put("screenshot",test.takeScreenshot("original-airship-preheated-launch").toString());
            report.put("status","passed");checks.add("Original static ship preheated before actual client assembly: >=3m above launch height, original water transferred, both operating boiler supplies/all4vents sustained100ticks, no pose/force/RPM/motor writes");server.runOnServer(s->s.saveEverything(false,true,true));
        }catch(Throwable failure){report.put("status","failed");report.put("error",failure.toString());if(ship!=null)try{report.put("failureState",world.getServer().computeOnServer(s->sample()));}catch(Throwable ignored){}throw failure;}
        finally{Files.writeString(path,new GsonBuilder().setPrettyPrinting().create().toJson(report));}
    }
    private static BlockPos local(BlockPos relative){return ANCHOR.offset(relative).offset(offset);}
    private static Map<String,Object> sample(){
        var vents=new ArrayList<Object>();var boilers=new ArrayList<Object>();var engines=new ArrayList<Object>();var pumps=new ArrayList<Object>();double gas=0,water=0,heat=0;
        for(var rel:VENTS){var vent=(SteamVentBlockEntity)level.getBlockEntity(local(rel));double output=vent.getGasOutput();gas+=output;vents.add(Map.of("sourceRelative",rel.toShortString(),"signal",vent.signalStrength,"gasOutput",output));}
        for(var rel:BOILERS){var tank=(FluidTankBlockEntity)level.getBlockEntity(local(rel));water+=tank.boiler.waterSupply;heat+=tank.boiler.activeHeat;boilers.add(Map.of("sourceRelative",rel.toShortString(),"waterSupply",tank.boiler.waterSupply,"activeHeat",tank.boiler.activeHeat,"engines",tank.boiler.attachedEngines,"size",tank.getTotalTankSize()));}
        for(var rel:List.of(new BlockPos(19,19,47),new BlockPos(21,19,47))){var e=(PortableEngineBlockEntity)level.getBlockEntity(local(rel));engines.add(Map.of("sourceRelative",rel.toShortString(),"burnTime",e.getCurrentBurnTime(),"generatedSpeed",e.getGeneratedSpeed(),"actualSpeed",e.getSpeed()));}
        for(var pos:PUMPS){var be=level.getBlockEntity(pos);pumps.add(be instanceof KineticBlockEntity k?Map.of("sourceRelative",pos.subtract(offset).subtract(ANCHOR).toShortString(),"rpm",k.getSpeed()):Map.of("plot",pos.toShortString(),"missingPump",true));}
        double filled=0,target=0,lift=0;int balloonCount=0;for(var balloon:BalloonMap.MAP.get(level).getBalloons())if(balloon instanceof ServerBalloon sb){filled+=sb.getTotalFilledVolume();target+=sb.getTotalTargetVolume();lift+=sb.getTotalLift();balloonCount++;}
        var row=new LinkedHashMap<String,Object>();row.put("serverGameTime",level.getGameTime());row.put("height",ship==null?0:ship.logicalPose().position().y());if(ship!=null){var p=ship.logicalPose().position();var q=ship.logicalPose().orientation();var h=RigidBodyHandle.of(ship);var linear=h.getLinearVelocity(new Vector3d());row.put("position",List.of(p.x(),p.y(),p.z()));row.put("orientation",List.of(q.x(),q.y(),q.z(),q.w()));row.put("linearVelocity",List.of(linear.x,linear.y,linear.z));row.put("mass",ship.getMassTracker().getMass());}row.put("steamVents",vents);row.put("boilers",boilers);row.put("portableEngines",engines);row.put("mechanicalPumps",pumps);row.put("totalGasOutput",gas);row.put("totalWaterSupply",water);row.put("totalActiveHeat",heat);row.put("storedFilledVolume",filled);row.put("storedTargetVolume",target);row.put("storedLift",lift);row.put("balloons",balloonCount);return row;
    }
    private static boolean ready(Map<String,Object> row){for(var v:(List<?>)row.get("steamVents"))if(((Number)((Map<?,?>)v).get("gasOutput")).doubleValue()<=0)return false;for(var b:(List<?>)row.get("boilers")){var value=(Map<?,?>)b;if(!value.get("sourceRelative").equals("19, 26, 44")&&(((Number)value.get("waterSupply")).doubleValue()<=0||((Number)value.get("activeHeat")).doubleValue()<=0))return false;}return true;}
    private static List<Object> waterCells(){var rows=new ArrayList<Object>();for(var rel:WATER)rows.add(Map.of("sourceRelative",rel.toShortString(),"worldSource",level.getBlockState(ANCHOR.offset(rel)).toString(),"mappedPlot",level.getBlockState(local(rel)).toString()));return rows;}
    private static boolean acked(Object connection){try{var f=connection.getClass().getDeclaredField("awaitingPositionFromClient");f.setAccessible(true);return f.get(connection)==null;}catch(ReflectiveOperationException e){throw new RuntimeException(e);}}
    private static void check(boolean value,String message){if(!value)throw new AssertionError(message);}
    private PackagedAirshipPreheatLaunchRegression(){}
}
