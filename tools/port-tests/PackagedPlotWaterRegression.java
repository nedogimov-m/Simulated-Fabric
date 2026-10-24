import com.google.gson.GsonBuilder;
import com.simibubi.create.content.fluids.OpenEndedPipe;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.api.SubLevelAssemblyHelper;
import dev.ryanhcode.sable.companion.math.BoundingBox3i;
import dev.simulated_team.simulated.mixin.accessor.NativeFlowingFluidAccessor;
import net.createmod.catnip.math.BlockFace;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidConstants;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.phys.Vec3;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** New renewable-source compatibility boundary only; no previous gameplay scopes. */
public final class PackagedPlotWaterRegression {
    private static final BlockPos SOURCE=new BlockPos(16,120,16);
    public static void run(ClientGameTestContext test,TestSingleplayerContext world,List<String> checks) throws Exception {
        var report=new LinkedHashMap<String,Object>();
        report.put("scope","Native OpenEndedPipe transactions on a newly assembled dry plot: two originally filled2x2water pools sustain8drain cycles each; isolated finite source abort preserves then commit drains. Water is placed once before all assertions, never replenished.");
        var path=Path.of(System.getProperty("port.smoke.report")).resolveSibling("plot-water.json");
        try {
            world.getServer().runOnServer(server->{
                var level=server.overworld();
                var sourceBlocks=new ArrayList<BlockPos>();
                for(int x=0;x<14;x++)for(int z=0;z<4;z++){
                    var floor=SOURCE.offset(x,0,z);level.getChunk(floor);
                    level.setBlockAndUpdate(floor,Blocks.STONE.defaultBlockState());sourceBlocks.add(floor);
                    boolean opening=((x==1||x==2||x==6||x==7)&&(z==1||z==2))||(x==11&&z==1);
                    if(!opening){var wall=floor.above();level.setBlockAndUpdate(wall,Blocks.STONE.defaultBlockState());sourceBlocks.add(wall);}
                }
                var plot=SubLevelAssemblyHelper.assembleBlocks(level,SOURCE,sourceBlocks,new BoundingBox3i(SOURCE,SOURCE.offset(13,1,3)));
                check(plot!=null&&!plot.isRemoved(),"Dry pool platform assembly failed");
                var origin=BlockPos.containing(plot.logicalPose().transformPositionInverse(Vec3.atCenterOf(SOURCE)));
                var pools=new ArrayList<List<BlockPos>>();
                for(int baseX:new int[]{1,6}){
                    var pool=new ArrayList<BlockPos>();
                    for(int x=baseX;x<baseX+2;x++)for(int z=1;z<=2;z++)pool.add(origin.offset(x,1,z));
                    pools.add(pool);
                }
                var finite=origin.offset(11,1,1);
                var allWater=new ArrayList<BlockPos>();pools.forEach(allWater::addAll);allWater.add(finite);
                // One-time fixture setup. No source placement occurs after this loop.
                for(var pos:allWater){check(Sable.HELPER.getContaining(level,pos)==plot,"Water fixture is outside actual plot");level.setBlockAndUpdate(pos,Blocks.WATER.defaultBlockState());}
                report.put("plot",plot.getUniqueId().toString());
                report.put("sourceCells",allWater.stream().map(BlockPos::toShortString).toList());
                report.put("waterSourceConversion",level.getGameRules().get(GameRules.WATER_SOURCE_CONVERSION));
                report.put("beforeDrainDiagnostics",allWater.stream().map(p->diagnostic(level,p)).toList());
                var drainDiagnostics=new ArrayList<Object>();report.put("drainDiagnostics",drainDiagnostics);
                var water=FluidVariant.of(Fluids.WATER);
                var poolPipes=new ArrayList<List<OpenEndedPipe>>();
                for(var pool:pools){var pipes=new ArrayList<OpenEndedPipe>();for(var pos:pool)pipes.add(pipe(level,pos));poolPipes.add(pipes);}
                var cycles=new ArrayList<Object>();long total=0;
                for(int cycle=1;cycle<=8;cycle++){
                    for(int poolIndex=0;poolIndex<pools.size();poolIndex++){
                        for(var pipe:poolPipes.get(poolIndex)){
                            var before=diagnostic(level,pipe.getOutputPos());
                            try(var tx=Transaction.openOuter()){
                                var view=pipe.provideHandler().iterator().next();
                                check(view.getResource().equals(water),"Native open pipe did not expose water");
                                long extracted=view.extract(water,FluidConstants.BUCKET,tx);
                                check(extracted==FluidConstants.BUCKET,"Renewable pool starved at cycle"+cycle+" pool"+poolIndex+": "+extracted);
                                tx.commit();total+=extracted;
                            }
                            if(cycle==1)drainDiagnostics.add(Map.of("pool",poolIndex,"before",before,"after",diagnostic(level,pipe.getOutputPos())));
                        }
                        for(var pos:pools.get(poolIndex))check(level.getFluidState(pos).isSourceOfType(Fluids.WATER),"Renewable source was consumed at"+pos+" cycle"+cycle);
                    }
                    cycles.add(Map.of("cycle",cycle,"renewableSourceCells",8,"extractedBuckets",8));
                }
                report.put("cycles",cycles);report.put("totalExtractedBuckets",total/FluidConstants.BUCKET);
                var finitePipe=pipe(level,finite);
                try(var tx=Transaction.openOuter()){
                    long extracted=finitePipe.provideHandler().iterator().next().extract(water,FluidConstants.BUCKET,tx);
                    check(extracted==FluidConstants.BUCKET,"Finite source simulation failed");
                    // Abort: source and internal buffer must both roll back.
                }
                check(level.getFluidState(finite).isSource(),"Aborted finite drain changed the world");
                try(var tx=Transaction.openOuter()){
                    long extracted=finitePipe.provideHandler().iterator().next().extract(water,FluidConstants.BUCKET,tx);
                    check(extracted==FluidConstants.BUCKET,"Finite source committed drain failed");tx.commit();
                }
                var finiteAfter=level.getBlockState(finite);
                check(!finiteAfter.getFluidState().isSource()&&finiteAfter.hasProperty(LiquidBlock.LEVEL)&&finiteAfter.getValue(LiquidBlock.LEVEL)==14,"Finite source was incorrectly retained: "+finiteAfter);
                try(var tx=Transaction.openOuter()){
                    long second=finitePipe.provideHandler().extract(water,FluidConstants.BUCKET,tx);
                    check(second==0,"Finite pool became an infinite supply: "+second);tx.commit();
                }
                report.put("finite",Map.of("abortPreservedSource",true,"committedBuckets",1,"afterState",finiteAfter.toString(),"nextDrainAmount",0));
                report.put("renewableFinalStates",pools.stream().flatMap(List::stream).map(p->level.getBlockState(p).toString()).toList());
            });
            report.put("status","passed");
            checks.add("New plot water boundary:2native2x2pools survive64bucketdrains across8cycles; finitewater abort/commit/empty verification passed without replenishment");
        } catch(Throwable failure){report.put("status","failed");report.put("error",failure.toString());throw failure;}
        finally{Files.writeString(path,new GsonBuilder().setPrettyPrinting().create().toJson(report));}
    }
    private static OpenEndedPipe pipe(net.minecraft.server.level.ServerLevel level,BlockPos water){var pipe=new OpenEndedPipe(new BlockFace(water.above(),Direction.DOWN));pipe.manageSource(level,null);return pipe;}
    private static Map<String,Object> diagnostic(net.minecraft.server.level.ServerLevel level,BlockPos pos){
        var row=new LinkedHashMap<String,Object>();var state=level.getBlockState(pos);var fluid=state.getFluidState();
        row.put("position",pos.toShortString());row.put("blockState",state.toString());row.put("fluidFromBlock",fluid.toString());row.put("fluidFromLevel",level.getFluidState(pos).toString());
        row.put("floorState",level.getBlockState(pos.below()).toString());row.put("floorSolid",level.getBlockState(pos.below()).isSolid());row.put("canBeReplaced",state.canBeReplaced());
        row.put("plot",String.valueOf(Sable.HELPER.getContaining(level,pos)));
        var neighbors=new ArrayList<Object>();
        for(var direction:Direction.Plane.HORIZONTAL){var neighbor=level.getBlockState(pos.relative(direction));neighbors.add(Map.of("direction",direction.toString(),"state",neighbor.toString(),"source",neighbor.getFluidState().isSource()));}
        row.put("neighbors",neighbors);
        if(fluid.isSource()&&fluid.createLegacyBlock().hasProperty(LiquidBlock.LEVEL)){
            var candidate=fluid.createLegacyBlock().setValue(LiquidBlock.LEVEL,14);row.put("candidate",candidate.toString());
            var type=candidate.getFluidState().getType();row.put("accessorApplied",type instanceof NativeFlowingFluidAccessor);
            if(type instanceof FlowingFluid&&type instanceof NativeFlowingFluidAccessor flowing){var renewed=flowing.simulated$getNewLiquid(level,pos,candidate);row.put("nativeRenewed",renewed.toString());row.put("nativeRenewsOriginal",renewed.equals(fluid));}
        }
        return row;
    }
    private static void check(boolean condition,String message){if(!condition)throw new AssertionError(message);}
    private PackagedPlotWaterRegression(){}
}
