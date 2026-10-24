import com.google.gson.GsonBuilder;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.content.contraptions.ControlledContraptionEntity;
import com.simibubi.create.content.contraptions.bearing.BearingBlock;
import com.simibubi.create.content.contraptions.bearing.MechanicalBearingBlockEntity;
import dev.eriksonn.aeronautics.index.AeroBlocks;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.api.SubLevelAssemblyHelper;
import dev.ryanhcode.sable.api.entity.EntitySubLevelUtil;
import dev.ryanhcode.sable.api.physics.constraint.FixedConstraintConfiguration;
import dev.ryanhcode.sable.api.physics.handle.RigidBodyHandle;
import dev.ryanhcode.sable.api.sublevel.SubLevelContainer;
import dev.ryanhcode.sable.companion.math.BoundingBox3i;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaterniond;
import org.joml.Vector3d;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Native bearing reservations must remain in their parent plot through restore. */
public final class PackagedCreatePlotRetainRegression {
    private static ServerSubLevel plot;
    private static final List<BlockPos> sources=List.of(new BlockPos(16,160,16),new BlockPos(16,161,16),
            new BlockPos(20,160,16),new BlockPos(20,161,16),new BlockPos(20,161,17));

    public static void run(ClientGameTestContext test, TestSingleplayerContext world, List<String> checks) throws Exception {
        var report=new LinkedHashMap<String,Object>(); var cases=new ArrayList<Object>(); report.put("cases",cases);
        report.put("scope","Fresh generic Create and Aero propeller bearing entities on one moved/rotated real plot: native reservation retains plot coordinates/controller, two ticking attached owners, native disassembly conserves all eight blocks and creates no world-space remnants. No ship-world mutation.");
        var path=Path.of(System.getProperty("port.smoke.report")).resolveSibling("create-plot-retain.json");
        try {
            var server=world.getServer(); server.runCommand("gamemode creative @a"); server.runCommand("tp @a 20 165 10");
            var mapped=new ArrayList<BlockPos>();
            server.runOnServer(s->{
                var level=s.overworld(); for(var source:sources)level.getChunk(source);
                level.setBlockAndUpdate(sources.get(0),AllBlocks.MECHANICAL_BEARING.defaultBlockState().setValue(BearingBlock.FACING,Direction.UP));
                level.setBlockAndUpdate(sources.get(1),Blocks.STONE.defaultBlockState());
                level.setBlockAndUpdate(sources.get(2),AeroBlocks.PROPELLER_BEARING.defaultBlockState().setValue(BearingBlock.FACING,Direction.UP));
                level.setBlockAndUpdate(sources.get(3),dev.simulated_team.simulated.index.SimBlocks.WHITE_SYMMETRIC_SAIL.defaultBlockState().setValue(dev.simulated_team.simulated.content.blocks.symmetric_sail.SymmetricSailBlock.AXIS,Direction.Axis.Y));
                level.setBlockAndUpdate(sources.get(4),dev.simulated_team.simulated.index.SimBlocks.WHITE_SYMMETRIC_SAIL.defaultBlockState().setValue(dev.simulated_team.simulated.content.blocks.symmetric_sail.SymmetricSailBlock.AXIS,Direction.Axis.Y));
                var connected=new ArrayList<BlockPos>(sources);
                for(int x=17;x<=19;x++){var bridge=new BlockPos(x,160,16);level.setBlockAndUpdate(bridge,Blocks.STONE.defaultBlockState());connected.add(bridge);}
                plot=SubLevelAssemblyHelper.assembleBlocks(level,sources.getFirst(),connected,
                        new BoundingBox3i(sources.get(0),sources.getLast()));
                check(plot!=null&&!plot.isRemoved(),"Failed parent assembly");
                for(var source:sources)mapped.add(BlockPos.containing(plot.logicalPose().transformPositionInverse(Vec3.atCenterOf(source))));
                var rotation=new Quaterniond().rotateY(Math.toRadians(23)).rotateZ(Math.toRadians(7));
                var position=new Vector3d(plot.logicalPose().position()).add(9,5,11);
                RigidBodyHandle.of(plot).teleport(position,rotation);
                var joint=SubLevelContainer.getContainer(level).physicsSystem().getPipeline().addConstraint(null,plot,
                        new FixedConstraintConfiguration(position,new Vector3d(plot.getMassTracker().getCenterOfMass()),rotation));
                check(joint!=null&&joint.isValid(),"Failed fixture fixed joint");
            });
            test.waitTicks(5);
            var owners=new ArrayList<ControlledContraptionEntity>();
            for(int index:new int[]{0,2}) {
                final int target=index;
                server.runOnServer(s->{
                    var level=s.overworld();var bearing=(MechanicalBearingBlockEntity)level.getBlockEntity(mapped.get(target));
                    check(bearing!=null,"Missing plot bearing");bearing.assemble();var owner=bearing.getMovedContraption();
                    check(owner!=null,"Bearing failed to reserve/assemble: "+bearing.getLastAssemblyException());
                    var expected=Vec3.atLowerCornerOf(mapped.get(target).above());
                    var row=new LinkedHashMap<String,Object>();row.put("type",BuiltInRegistries.ENTITY_TYPE.getKey(owner.getType()).toString());
                    row.put("position",owner.position().toString());row.put("expectedPlotPosition",expected.toString());
                    row.put("controller",String.valueOf(owner.getControllerPos()));row.put("kickable",EntitySubLevelUtil.shouldKick(owner));
                    cases.add(row);
                    check(!EntitySubLevelUtil.shouldKick(owner),"Native contraption type missing RETAIN_IN_SUB_LEVEL tag: "+row);
                    check(owner.position().distanceToSqr(expected)<1e-10,"Native reservation was projected into world space: "+row);
                    check(mapped.get(target).equals(owner.getControllerPos()),"Controller left plot coordinates");
                    check(Sable.HELPER.getContaining(owner)==plot,"Owner no longer belongs to plot");
                    check(level.getBlockState(mapped.get(target+1)).isAir(),"Rotor block was not removed into owner");
                    owners.add(owner);
                });
            }
            test.waitTicks(5);
            server.runOnServer(s->{
                var level=s.overworld();
                for(int i=0;i<2;i++){
                    var bearing=(MechanicalBearingBlockEntity)level.getBlockEntity(mapped.get(i*2));var owner=owners.get(i);
                    check(!owner.isRemoved()&&bearing.getMovedContraption()==owner&&bearing.isRunning(),"Native controller lost owner while ticking");
                    check(Sable.HELPER.getContaining(owner)==plot,"Ticking moved owner out of plot");
                    check(bearing.disassemble(),"Native disassembly refused");
                    check(owner.isRemoved(),"Restored owner stayed alive");
                    check(level.getBlockState(mapped.get(i*2+1)).is(i==0?Blocks.STONE:dev.simulated_team.simulated.index.SimBlocks.WHITE_SYMMETRIC_SAIL),"Rotor block was not restored in parent plot");
                }
                for(var source:sources)check(level.getBlockState(source).isAir(),"Original source cell duplicated");
                int plotBlocks=0;
                for(var chunk:plot.getPlot().getLoadedChunks())for(var section:chunk.getChunk().getSections())
                    for(int x=0;x<16;x++)for(int y=0;y<16;y++)for(int z=0;z<16;z++)if(!section.getBlockState(x,y,z).isAir())plotBlocks++;
                check(plotBlocks==8,"Eight-block parent was not conserved: "+plotBlocks);report.put("conservedPlotBlocks",plotBlocks);
                var bounds=plot.boundingBox();int stray=0;
                for(var cell:BlockPos.betweenClosed((int)Math.floor(bounds.minX())-2,(int)Math.floor(bounds.minY())-2,(int)Math.floor(bounds.minZ())-2,
                        (int)Math.ceil(bounds.maxX())+2,(int)Math.ceil(bounds.maxY())+2,(int)Math.ceil(bounds.maxZ())+2))
                    if(!level.getBlockState(cell).isAir())stray++;
                check(stray==0,"Native restoration left world-space blocks: "+stray);report.put("worldSpaceRemnants",stray);
            });
            report.put("status","passed");checks.add("Native generic Create + Aero bearing reservations retain parent plot; attached ticks and actual disassembly conserve8blocks with0world remnants");
        }catch(Throwable failure){report.put("status","failed");report.put("error",failure.toString());throw failure;}
        finally{Files.writeString(path,new GsonBuilder().setPrettyPrinting().create().toJson(report));}
    }
    private static void check(boolean value,String message){if(!value)throw new AssertionError(message);}
    private PackagedCreatePlotRetainRegression(){}
}
