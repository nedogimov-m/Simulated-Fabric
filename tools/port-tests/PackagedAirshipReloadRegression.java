import com.google.gson.GsonBuilder;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.content.fluids.tank.FluidTankBlockEntity;
import com.simibubi.create.content.fluids.FluidTransportBehaviour;
import com.simibubi.create.content.processing.burner.BlazeBurnerBlockEntity;
import dev.eriksonn.aeronautics.content.blocks.hot_air.balloon.map.BalloonMap;
import dev.eriksonn.aeronautics.content.blocks.hot_air.balloon.ServerBalloon;
import dev.eriksonn.aeronautics.content.blocks.hot_air.steam_vent.SteamVentBlockEntity;
import dev.eriksonn.aeronautics.content.blocks.propeller.bearing.propeller_bearing.PropellerBearingBlockEntity;
import dev.ryanhcode.sable.api.physics.handle.RigidBodyHandle;
import dev.ryanhcode.sable.api.sublevel.SubLevelContainer;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import dev.simulated_team.simulated.content.blocks.physics_assembler.PhysicsAssemblerBlockEntity;
import dev.simulated_team.simulated.content.blocks.portable_engine.PortableEngineBlockEntity;
import dev.simulated_team.simulated.content.blocks.throttle_lever.ThrottleLeverBlockEntity;
import dev.simulated_team.simulated.content.blocks.throttle_lever.ThrottleLeverBlock;
import dev.simulated_team.simulated.content.blocks.analog_transmission.AnalogTransmissionBlockEntity;
import dev.simulated_team.simulated.content.physics_staff.PhysicsStaffServerHandler;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.MinecraftServer;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.joml.Vector3d;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Observes a freshly reopened copy; no gameplay, control or physics writes. */
public final class PackagedAirshipReloadRegression {
    public static void run(ClientGameTestContext test, TestSingleplayerContext world, List<String> checks) throws Exception {
        var report = new LinkedHashMap<String,Object>();
        var samples = new ArrayList<Map<String,Object>>();
        report.put("scope", "Reopen observation only; 200 advancing server ticks, no fuel/control/force/pose/RPM writes. This diagnostic PASS does not assert flight.");
        report.put("samples", samples);
        var path = Path.of(System.getProperty("port.smoke.report")).resolveSibling("airship-reload.json");
        try {
            var server = world.getServer();
            report.put("worldObstructions",server.computeOnServer(PackagedAirshipReloadRegression::obstructions));
            samples.add(server.computeOnServer(PackagedAirshipReloadRegression::sample));
            long start = ((Number)samples.getFirst().get("serverGameTime")).longValue();
            for (int tick = 20; tick <= 200; tick += 20) {
                long target = start + tick;
                server.waitFor(s -> s.overworld().getGameTime() >= target, 200);
                samples.add(server.computeOnServer(PackagedAirshipReloadRegression::sample));
                Files.writeString(path, new GsonBuilder().setPrettyPrinting().create().toJson(report));
            }
            var first = (List<?>)samples.getFirst().get("position");
            var last = (List<?>)samples.getLast().get("position");
            var delta = new ArrayList<Double>();
            for (int i=0;i<3;i++) delta.add(((Number)last.get(i)).doubleValue()-((Number)first.get(i)).doubleValue());
            report.put("displacement",delta);
            report.put("screenshot",PackagedAirshipRecoveryRegression.externalScreenshot(test,samples.getLast()));
            report.put("status","passed");
            checks.add("Fresh ship reload observed for200native server ticks without controls/fuel/physics writes; diagnostic only");
        } catch (Throwable failure) {
            report.put("status","failed"); report.put("error",failure.toString()); throw failure;
        } finally {
            Files.writeString(path,new GsonBuilder().setPrettyPrinting().create().toJson(report));
        }
    }
    public static Map<String,Object> sample(MinecraftServer server) {
        var level = server.overworld();
        var container = SubLevelContainer.getContainer(level);
        var ships = container.getAllSubLevels();
        if (ships.size()!=1) throw new AssertionError("Expected one ship, got "+ships.size());
        var ship = (ServerSubLevel)ships.getFirst();
        var handle = RigidBodyHandle.of(ship);
        var row = new LinkedHashMap<String,Object>();
        var pos=ship.logicalPose().position(); var q=ship.logicalPose().orientation();
        row.put("serverGameTime",level.getGameTime()); row.put("ship",ship.getUniqueId().toString());
        row.put("runtimeId",ship.getRuntimeId()); row.put("javaIdentity",System.identityHashCode(ship));
        var player=server.getPlayerList().getPlayers().getFirst();
        row.put("playerPosition",List.of(player.getX(),player.getY(),player.getZ()));
        row.put("position",List.of(pos.x(),pos.y(),pos.z())); row.put("orientation",List.of(q.x(),q.y(),q.z(),q.w()));
        row.put("linearVelocity",vector(handle.getLinearVelocity(new Vector3d())));
        row.put("angularVelocity",vector(handle.getAngularVelocity(new Vector3d())));
        row.put("mass",ship.getMassTracker().getMass()); row.put("physicsPaused",container.physicsSystem().getPaused());
        row.put("lastNetworkedStopped",ship.getLastNetworkedStopped()); row.put("tickFrozen",server.tickRateManager().isFrozen());
        row.put("staffLocked",PhysicsStaffServerHandler.get(level).isLocked(ship));
        var bounds=ship.boundingBox();
        row.put("globalBounds",List.of(bounds.minX(),bounds.minY(),bounds.minZ(),bounds.maxX(),bounds.maxY(),bounds.maxZ()));
        var terrain=new ArrayList<Object>();
        for(double x:new double[]{bounds.minX(),pos.x(),bounds.maxX()}) for(double z:new double[]{bounds.minZ(),pos.z(),bounds.maxZ()}) {
            int bx=(int)Math.floor(x),bz=(int)Math.floor(z);
            terrain.add(Map.of("x",bx,"z",bz,"surfaceY",level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.WORLD_SURFACE,bx,bz)));
        }
        row.put("terrain",terrain);
        var children=new ArrayList<Object>();
        for(var child:ship.getPlot().getContraptions()) children.add(Map.of("position",List.of(child.sable$getPosition().x(),child.sable$getPosition().y(),child.sable$getPosition().z()),"valid",child.sable$isValid(),"collides",child.sable$shouldCollide(),"class",child.getClass().getSimpleName()));
        row.put("kinematicChildren",children);
        var actors=new ArrayList<String>();
        for(var actor:ship.getPlot().getBlockEntityActors()) actors.add(actor.getClass().getSimpleName());
        row.put("actors",actors);
        try {
            var collisions=(Map<?,?>)Class.forName("port.observation.RapierCollisionProbeState").getMethod("snapshot").invoke(null);
            row.put("nativeCollisions",collisions);
            var contacts=new LinkedHashMap<String,Object>();
            for(var event:(List<?>)collisions.get("firstEvents")) {
                double[] data=(double[])((Map<?,?>)event).get("event");
                for(int side=0;side<2;side++) if(data[side]==-1) {
                    int index=side==0?9:12;
                    var contact=BlockPos.containing(data[index],data[index+1],data[index+2]);
                    for(var direction:net.minecraft.core.Direction.values()) {
                        var cell=contact.relative(direction);
                        contacts.put(cell.toShortString(),level.getBlockState(cell).toString());
                    }
                    contacts.put(contact.toShortString(),level.getBlockState(contact).toString());
                }
            }
            row.put("worldContactBlocks",contacts);
        } catch(ClassNotFoundException ignored) {
        } catch(ReflectiveOperationException failure) { throw new RuntimeException(failure); }
        var entities = new ArrayList<BlockEntity>();
        for(var chunk:ship.getPlot().getLoadedChunks()) entities.addAll(chunk.getChunk().getBlockEntities().values());
        BlockPos assembler=null;
        for(var be:entities) if(be instanceof PhysicsAssemblerBlockEntity) {assembler=be.getBlockPos(); break;}
        if(assembler==null) throw new AssertionError("Missing ship assembler");
        var origin=assembler.subtract(new BlockPos(20,23,71));
        var controls=new ArrayList<Object>(); var kinetics=new ArrayList<Object>(); var boilers=new ArrayList<Object>();
        var vents=new ArrayList<Object>(); var engines=new ArrayList<Object>(); var propellers=new ArrayList<Object>();
        int burners=0,minBurn=Integer.MAX_VALUE;
        for(var be:entities) {
            var relative=be.getBlockPos().subtract(origin).toShortString();
            var name=BuiltInRegistries.BLOCK.getKey(be.getBlockState().getBlock()).toString();
            if(be instanceof ThrottleLeverBlockEntity t) controls.add(Map.of("relative",relative,"state",t.getState(),"inverted",t.getBlockState().getValue(ThrottleLeverBlock.INVERTED)));
            if(be instanceof KineticBlockEntity k) {
                if(be instanceof AnalogTransmissionBlockEntity a) kinetics.add(Map.of("relative",relative,"block",name,"rpm",k.getSpeed(),"extraRpm",a.getExtraKinetics().getSpeed(),"modifier",a.getRotationModifier()));
                else if(name.contains("pump")) kinetics.add(Map.of("relative",relative,"block",name,"rpm",k.getSpeed()));
            }
            if(be instanceof FluidTankBlockEntity t && t.isController()) boilers.add(Map.of("relative",relative,"waterSupply",t.boiler.waterSupply,"activeHeat",t.boiler.activeHeat,"engines",t.boiler.attachedEngines,"size",t.getTotalTankSize(),"fluidAmount",t.getTankInventory().getFluidAmount(),"fluid",t.getTankInventory().getFluid().toString()));
            if(be instanceof SteamVentBlockEntity v) vents.add(Map.of("relative",relative,"signal",v.signalStrength,"gasOutput",v.getGasOutput()));
            if(be instanceof PortableEngineBlockEntity e) engines.add(Map.of("relative",relative,"burnTicks",e.getCurrentBurnTime(),"inventoryCount",e.inventory.getItem(0).getCount(),"rpm",e.getSpeed(),"generatedRpm",e.getGeneratedSpeed()));
            if(be instanceof PropellerBearingBlockEntity p) {
                var prop=new LinkedHashMap<String,Object>();
                prop.put("relative",relative); prop.put("facing",p.getBlockDirection().toString());
                prop.put("running",p.isRunning()); prop.put("active",p.isActive()); prop.put("rpm",p.getSpeed());
                prop.put("rotationSpeed",p.getRotationSpeed()); prop.put("thrust",p.getThrust());
                prop.put("hasContraption",p.getMovedContraption()!=null); prop.put("scaledThrust",p.getScaledThrust());
                prop.put("airflowScaling",p.getAirflowScaling()); prop.put("pressure",p.getCurrentAirPressure());
                propellers.add(prop);
            }
            if(be instanceof BlazeBurnerBlockEntity b) {burners++;minBurn=Math.min(minBurn,b.getRemainingBurnTime());}
        }
        row.put("throttles",controls); row.put("kinetics",kinetics); row.put("boilers",boilers);
        row.put("vents",vents); row.put("engines",engines); row.put("propellers",propellers);
        row.put("burners",burners); row.put("minimumBurnTicks",minBurn);
        var balloons=new ArrayList<Object>();
        for(var b:BalloonMap.MAP.get(level).getBalloons()) if(b instanceof ServerBalloon sb) balloons.add(Map.of("filled",sb.getTotalFilledVolume(),"target",sb.getTotalTargetVolume(),"lift",sb.getTotalLift(),"valid",sb.isValid()));
        row.put("balloons",balloons);
        var sources = new ArrayList<Object>();
        for(int[] pair : new int[][]{{17,53},{17,54},{18,53},{18,54},{22,53},{22,54},{23,53},{23,54}}) {
            var relative = new BlockPos(pair[0],26,pair[1]);
            var cell = origin.offset(relative);
            var fluid = level.getFluidState(cell);
            sources.add(Map.of("relative",relative.toShortString(),"state",level.getBlockState(cell).toString(),
                    "fluid",BuiltInRegistries.FLUID.getKey(fluid.getType()).toString(),"source",fluid.isSource(),"loaded",level.isLoaded(cell)));
        }
        row.put("waterSources",sources);
        var waterRoute = new ArrayList<Object>();
        for(var relative : List.of(new BlockPos(17,27,54),new BlockPos(17,28,54),new BlockPos(17,29,54),
                new BlockPos(17,30,54),new BlockPos(17,31,54),new BlockPos(18,31,54),new BlockPos(19,31,54),
                new BlockPos(18,27,54),new BlockPos(18,28,54),new BlockPos(17,28,53),
                new BlockPos(19,26,47),new BlockPos(19,31,46))) {
            var cell=origin.offset(relative); var be=level.getBlockEntity(cell);
            var entry=new LinkedHashMap<String,Object>();
            entry.put("relative",relative.toShortString());entry.put("state",level.getBlockState(cell).toString());
            entry.put("loaded",level.isLoaded(cell));entry.put("blockEntity",be==null?"none":be.getClass().getSimpleName());
            if(be instanceof FluidTankBlockEntity tank) entry.put("controller",tank.getController().subtract(origin).toShortString());
            if(be instanceof FluidTransportBehaviour.Provider provider) {
                var transport=provider.transport();
                if(transport!=null) {
                    entry.put("phase",transport.phase.toString());var connections=new ArrayList<Object>();
                    if(transport.interfaces!=null) for(Direction side:Direction.values()) {
                        var connection=transport.interfaces.get(side);if(connection==null)continue;
                        var data=new LinkedHashMap<String,Object>();data.put("side",side.toString());
                        data.put("inboundPressure",connection.getInboundPressure());data.put("outwardPressure",connection.getOutwardPressure());
                        data.put("hasNetwork",connection.hasNetwork());var flow=transport.getFlow(side);
                        try {
                            var field=com.simibubi.create.content.fluids.PipeConnection.class.getDeclaredField("source");field.setAccessible(true);
                            var source=(java.util.Optional<?>)field.get(connection);
                            data.put("sourceClass",source.map(value->value.getClass().getSimpleName()).orElse("empty"));
                        } catch(ReflectiveOperationException failure) {throw new RuntimeException(failure);}
                        if(flow!=null) {data.put("inbound",flow.inbound);data.put("complete",flow.complete);data.put("progress",flow.progress);data.put("fluid",flow.fluid.toString());}
                        connections.add(data);
                    }
                    entry.put("connections",connections);
                }
            }
            waterRoute.add(entry);
        }
        row.put("upperBoilerWaterRoute",waterRoute);
        return row;
    }
    private static List<Double> vector(Vector3d v) {return List.of(v.x,v.y,v.z);}
    public static Map<String,Object> obstructions(MinecraftServer server) {
        var level=server.overworld(); var ships=SubLevelContainer.getContainer(level).getAllSubLevels();
        if(ships.size()!=1) throw new AssertionError("Expected one ship");
        var bounds=ships.getFirst().boundingBox();
        var blocks=new ArrayList<Object>(); var histogram=new java.util.TreeMap<String,Integer>();
        for(var cell:BlockPos.betweenClosed((int)Math.floor(bounds.minX())-1,(int)Math.floor(bounds.minY())-1,(int)Math.floor(bounds.minZ())-1,
                (int)Math.ceil(bounds.maxX())+1,(int)Math.ceil(bounds.maxY())+1,(int)Math.ceil(bounds.maxZ())+1)) {
            var state=level.getBlockState(cell);
            if(!state.isAir() && !state.getCollisionShape(level,cell).isEmpty()) {
                String id=BuiltInRegistries.BLOCK.getKey(state.getBlock()).toString(); histogram.merge(id,1,Integer::sum);
                if(blocks.size()<1000)blocks.add(Map.of("position",cell.toShortString(),"state",state.toString(),"blockEntity",level.getBlockEntity(cell)!=null));
            }
        }
        return Map.of("solidBlocks",histogram.values().stream().mapToInt(Integer::intValue).sum(),"histogram",histogram,"first1000",blocks);
    }
    private PackagedAirshipReloadRegression() {}
}
