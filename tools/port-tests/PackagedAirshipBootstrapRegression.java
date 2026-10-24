import com.google.gson.GsonBuilder;
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
public final class PackagedAirshipBootstrapRegression {
    private static final BlockPos ANCHOR=new BlockPos(16,128,16),ASSEMBLER=ANCHOR.offset(20,23,71);
    private static final List<BlockPos> VENTS=List.of(new BlockPos(20,24,48),new BlockPos(20,24,49),new BlockPos(20,24,50),new BlockPos(20,37,52));
    private static final List<BlockPos> BOILERS=List.of(new BlockPos(19,18,48),new BlockPos(19,26,44),new BlockPos(19,31,52));
    private static final List<BlockPos> PUMPS=new ArrayList<>();
    private static ServerSubLevel ship;private static BlockPos offset;private static double startY;
    public static void run(ClientGameTestContext test,TestSingleplayerContext world,List<String> checks)throws Exception{
        var report=new LinkedHashMap<String,Object>();var samples=new ArrayList<Object>();report.put("samples",samples);
        report.put("scope","New actual operating-input test on saved accepted original ship: original8water cells, native coal fuel for2portable engines and20blaze burners, paper/paper RF pair, original controls restored then all5throttles request raw15. No assembly, initial60tick or UI replay. Fresh JVM creates no test joint; this helper installs none. No force, pose, RPM, motor, boiler supply or gas-output writes.");
        var path=Path.of(System.getProperty("port.smoke.report")).resolveSibling("airship-bootstrap.json");
        try{
            var server=world.getServer();var original=Path.of(System.getProperty("port.regression.schematic"));var raw=SchematicFiles.readTag(original);
            server.runCommand("gamemode spectator @a");server.runCommand("time set noon");server.runCommand("weather clear");
            server.runOnServer(s->{
                var level=s.overworld();var all=SubLevelContainer.getContainer(level).getAllSubLevels();check(all.size()==1,"Expected one saved original ship");ship=(ServerSubLevel)all.getFirst();BlockPos movedAssembler=null;
                for(var c:ship.getPlot().getLoadedChunks())for(var be:c.getChunk().getBlockEntities().values())if(be instanceof PhysicsAssemblerBlockEntity a&&a.isPrimaryAssembler()){check(movedAssembler==null,"Multiple primary assemblers");movedAssembler=be.getBlockPos();}
                check(movedAssembler!=null,"Saved primary assembler missing");offset=movedAssembler.subtract(ASSEMBLER);report.put("ship",ship.getUniqueId().toString());report.put("beforeOperatingInputs",sample());
                CompoundTag source;try{source=SchematicFiles.decode(level,raw).save(new CompoundTag());}catch(java.io.IOException e){throw new RuntimeException(e);}var palette=source.getListOrEmpty("palette");
                int waters=0,burners=0,engines=0,vanilla=0,throttles=0;var changes=new ArrayList<Object>();
                for(var e:source.getListOrEmpty("blocks")){
                    var block=(CompoundTag)e;var pos=block.getListOrEmpty("pos");var relative=new BlockPos(pos.getIntOr(0,0),pos.getIntOr(1,0),pos.getIntOr(2,0));var local=local(relative);var expected=NbtUtils.readBlockState(level.registryAccess().lookupOrThrow(Registries.BLOCK),palette.getCompoundOrEmpty(block.getIntOr("state",0)));var be=level.getBlockEntity(local);
                    if(expected.is(AllBlocks.MECHANICAL_PUMP))PUMPS.add(local);
                    if(expected.is(Blocks.WATER)){level.setBlockAndUpdate(local,expected);waters++;changes.add(Map.of("sourceRelative",relative.toShortString(),"input","original water source cell"));}
                    if(expected.is(Blocks.LEVER)){level.setBlockAndUpdate(local,expected);vanilla++;}
                    if(be instanceof ThrottleLeverBlockEntity throttle){int rawState=block.getCompoundOrEmpty("nbt").getIntOr("State",0);boolean inverted=throttle.getBlockState().getValue(ThrottleLeverBlock.INVERTED);throttle.setSignal(inverted?15-rawState:rawState);throttle.setSignal(inverted?0:15);throttles++;changes.add(Map.of("sourceRelative",relative.toShortString(),"originalRawState",rawState,"requestedRawState",15,"inverted",inverted));}
                    if(be instanceof BlazeBurnerBlockEntity burner){check(burner.tryFuel(new ItemStack(Items.COAL),true,false),"Native burner rejected coal: "+relative);burners++;changes.add(Map.of("sourceRelative",relative.toShortString(),"input","one coal through native committed tryFuel","burnTime",burner.getRemainingBurnTime()));}
                    if(be instanceof PortableEngineBlockEntity engine){var remainder=engine.inventory.insertSlot(new ItemStack(Items.COAL,64),0,false);check(remainder.isEmpty(),"Native engine rejected coal: "+relative);engines++;changes.add(Map.of("sourceRelative",relative.toShortString(),"input","64coal through native inventory"));}
                }
                check(waters==8&&burners==20&&engines==2&&vanilla==6&&throttles==5&&PUMPS.size()==9,"Original input/control count differs: "+List.of(waters,burners,engines,vanilla,throttles));report.put("operatingInputs",changes);
                var links=new ArrayList<Object>();for(var relative:List.of(new BlockPos(18,24,71),new BlockPos(19,37,52))){var link=(RedstoneLinkBlockEntity)level.getBlockEntity(local(relative));check(link!=null,"Original redstone link missing");var before=link.getLink().getNetworkKey().toString();link.getLink().setFrequency(true,new ItemStack(Items.PAPER));link.getLink().setFrequency(false,new ItemStack(Items.PAPER));links.add(Map.of("sourceRelative",relative.toShortString(),"before",before,"after",link.getLink().getNetworkKey().toString()));}report.put("actualRfNetworkKeys",links);
                startY=ship.logicalPose().position().y();report.put("startY",startY);report.put("testJointInstalled",false);
            });
            double maxY=startY,minY=startY,maxRecovery=0,maxUpwardSpeed=0,maxGas=0,maxWater=0,maxHeat=0;long time=server.computeOnServer(s->s.overworld().getGameTime());int observed=0;
            for(int tick=1;tick<=600;tick++){
                final long previous=time;server.waitFor(s->s.overworld().getGameTime()>previous,120);time=server.computeOnServer(s->s.overworld().getGameTime());observed++;
                if(tick==1||tick%10==0){var row=server.computeOnServer(s->sample());samples.add(row);double y=((Number)row.get("height")).doubleValue();maxY=Math.max(maxY,y);minY=Math.min(minY,y);maxRecovery=Math.max(maxRecovery,y-minY);maxUpwardSpeed=Math.max(maxUpwardSpeed,((Number)((List<?>)row.get("linearVelocity")).get(1)).doubleValue());maxGas=Math.max(maxGas,((Number)row.get("totalGasOutput")).doubleValue());maxWater=Math.max(maxWater,((Number)row.get("totalWaterSupply")).doubleValue());maxHeat=Math.max(maxHeat,((Number)row.get("totalActiveHeat")).doubleValue());if(maxRecovery>=3&&maxUpwardSpeed>.05&&maxGas>0&&maxWater>0&&maxHeat>0)break;}
            }
            report.put("observedAdvancingServerTicks",observed);report.put("maximumRiseFromStart",maxY-startY);report.put("minimumObservedY",minY);report.put("maximumRecoveryFromMinimum",maxRecovery);report.put("maximumUpwardSpeed",maxUpwardSpeed);report.put("maximumGasOutput",maxGas);report.put("maximumWaterSupply",maxWater);report.put("maximumActiveHeat",maxHeat);report.put("final",server.computeOnServer(s->sample()));
            report.put("screenshot",test.takeScreenshot("original-airship-operating-inputs").toString());
            check(maxGas>0&&maxWater>0&&maxHeat>0,"Native supplied ship did not establish boiler steam; inspect actual resource/kinetic diagnostics");check(maxRecovery>=3&&maxUpwardSpeed>.05,"Native supplied ship did not recover3m from natural low point with positive vertical speed within600observed server ticks");
            report.put("status","passed");checks.add("Original accepted ship, new actual supplied coal+8water+paper channels: native boiler steam and >=3m unconstrained recovery from natural low point with positive vertical speed, no pose/force/RPM/motor writes");server.runOnServer(s->s.saveEverything(false,true,true));
        }catch(Throwable failure){report.put("status","failed");report.put("error",failure.toString());if(ship!=null)try{report.put("failureState",world.getServer().computeOnServer(s->sample()));}catch(Throwable ignored){}throw failure;}
        finally{Files.writeString(path,new GsonBuilder().setPrettyPrinting().create().toJson(report));}
    }
    private static BlockPos local(BlockPos relative){return ANCHOR.offset(relative).offset(offset);}
    private static Map<String,Object> sample(){
        var level=ship.getLevel();var vents=new ArrayList<Object>();var boilers=new ArrayList<Object>();var engines=new ArrayList<Object>();var pumps=new ArrayList<Object>();double gas=0,water=0,heat=0;
        for(var rel:VENTS){var vent=(SteamVentBlockEntity)level.getBlockEntity(local(rel));double output=vent.getGasOutput();gas+=output;vents.add(Map.of("sourceRelative",rel.toShortString(),"signal",vent.signalStrength,"gasOutput",output));}
        for(var rel:BOILERS){var tank=(FluidTankBlockEntity)level.getBlockEntity(local(rel));water+=tank.boiler.waterSupply;heat+=tank.boiler.activeHeat;boilers.add(Map.of("sourceRelative",rel.toShortString(),"waterSupply",tank.boiler.waterSupply,"activeHeat",tank.boiler.activeHeat,"engines",tank.boiler.attachedEngines,"size",tank.getTotalTankSize()));}
        for(var rel:List.of(new BlockPos(19,19,47),new BlockPos(21,19,47))){var e=(PortableEngineBlockEntity)level.getBlockEntity(local(rel));engines.add(Map.of("sourceRelative",rel.toShortString(),"burnTime",e.getCurrentBurnTime(),"generatedSpeed",e.getGeneratedSpeed(),"actualSpeed",e.getSpeed()));}
        for(var pos:PUMPS){var be=level.getBlockEntity(pos);pumps.add(be instanceof KineticBlockEntity k?Map.of("sourceRelative",pos.subtract(offset).subtract(ANCHOR).toShortString(),"rpm",k.getSpeed()):Map.of("plot",pos.toShortString(),"missingPump",true));}
        var p=ship.logicalPose().position();var q=ship.logicalPose().orientation();var h=RigidBodyHandle.of(ship);var linear=h.getLinearVelocity(new Vector3d());var angular=h.getAngularVelocity(new Vector3d());var row=new LinkedHashMap<String,Object>();row.put("serverGameTime",level.getGameTime());row.put("height",p.y());row.put("position",List.of(p.x(),p.y(),p.z()));row.put("orientation",List.of(q.x(),q.y(),q.z(),q.w()));row.put("linearVelocity",List.of(linear.x,linear.y,linear.z));row.put("angularVelocity",List.of(angular.x,angular.y,angular.z));row.put("mass",ship.getMassTracker().getMass());row.put("steamVents",vents);row.put("boilers",boilers);row.put("portableEngines",engines);row.put("mechanicalPumps",pumps);row.put("totalGasOutput",gas);row.put("totalWaterSupply",water);row.put("totalActiveHeat",heat);return row;
    }
    private static void check(boolean value,String message){if(!value)throw new AssertionError(message);}
    private PackagedAirshipBootstrapRegression(){}
}
