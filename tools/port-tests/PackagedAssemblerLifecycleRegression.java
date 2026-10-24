import com.google.gson.GsonBuilder;
import com.simibubi.create.content.contraptions.glue.SuperGlueEntity;
import com.simibubi.create.foundation.utility.LoadedLevelAccess;
import dev.ryanhcode.sable.api.physics.constraint.FreeConstraintHandle;
import dev.ryanhcode.sable.api.sublevel.SubLevelContainer;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import dev.simulated_team.simulated.content.blocks.physics_assembler.PhysicsAssemblerBlockEntity;
import dev.simulated_team.simulated.index.SimBlocks;
import dev.simulated_team.simulated.mixin_interface.assembly_preventer.PrimaryAssemblerExtension;
import dev.simulated_team.simulated.network.packets.AssemblePacket;
import foundry.veil.api.network.VeilPacketManager;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.Container;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.properties.AttachFace;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.AABB;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

/** Only new assembler refusal/unlock and duplicate alignment request boundaries; no schematic printing. */
public final class PackagedAssemblerLifecycleRegression {
    private static final BlockPos REFUSED=new BlockPos(0,128,0),ASSEMBLER=new BlockPos(8,128,8),STONE=ASSEMBLER.below(),BARREL=STONE.east();
    private static final List<BlockPos>SOURCE=List.of(STONE,BARREL,ASSEMBLER);
    private static final Field HOLD=field("holdingLever"),CONSTRAINT=field("alignmentConstraint"),DISASSEMBLING=field("disassembling");
    public static void run(ClientGameTestContext test,TestSingleplayerContext world,List<String>checks)throws Exception{
        if(System.getProperty("port.smoke.userRegression","").equals("assembler-roundtrip")){runRoundtrip(test,world,checks);return;}
        var report=new LinkedHashMap<String,Object>();var server=world.getServer();var initialIds=new AtomicReference<Set<UUID>>();var sub=new AtomicReference<ServerSubLevel>();var clientRefused=new AtomicReference<PhysicsAssemblerBlockEntity>();
        try{
            server.runOnServer(s->{var level=s.overworld();level.setChunkForced(0,0,true);level.getChunk(REFUSED);initialIds.set(SubLevelContainer.getContainer(level).getAllSubLevels().stream().map(v->v.getUniqueId()).collect(Collectors.toSet()));var state=SimBlocks.PHYSICS_ASSEMBLER.defaultBlockState().setValue(BlockStateProperties.ATTACH_FACE,AttachFace.FLOOR).setValue(BlockStateProperties.HORIZONTAL_FACING,Direction.NORTH);level.setBlock(REFUSED.below(),Blocks.AIR.defaultBlockState(),Block.UPDATE_CLIENTS|Block.UPDATE_KNOWN_SHAPE);level.setBlock(REFUSED,state,Block.UPDATE_CLIENTS|Block.UPDATE_KNOWN_SHAPE);level.setBlockAndUpdate(REFUSED.south(3),Blocks.STONE.defaultBlockState());level.setBlockAndUpdate(STONE,Blocks.STONE.defaultBlockState());level.setBlockAndUpdate(BARREL,Blocks.BARREL.defaultBlockState());level.setBlockAndUpdate(ASSEMBLER,state);((Container)level.getBlockEntity(BARREL)).setItem(0,new ItemStack(Items.DIAMOND,7));check(level.addFreshEntity(new SuperGlueEntity(level,new AABB(STONE).expandTowards(1,1,0))),"Genuine fixture glue was refused");});
            server.runCommand("gamemode creative @a");server.runCommand("tp @a 0.5 129 3.5 180 35");
            test.waitFor(c->c.level!=null&&c.level.getBlockEntity(REFUSED)instanceof PhysicsAssemblerBlockEntity&&c.player!=null&&c.player.getY()>128,200);
            server.waitFor(s->pendingTeleport(s.getPlayerList().getPlayers().getFirst().connection)==null,200);
            test.runOnClient(c->{var owner=(PhysicsAssemblerBlockEntity)c.level.getBlockEntity(REFUSED);clientRefused.set(owner);owner.setClientHoldLeverInPlace(true);check(bool(HOLD,owner),"Pending lever fixture did not hold");VeilPacketManager.server().sendPacket(new AssemblePacket(REFUSED));});
            server.waitFor(s->{var owner=(PhysicsAssemblerBlockEntity)s.overworld().getBlockEntity(REFUSED);return owner.getLastAssemblyException()!=null;},100);
            test.waitFor(c->c.level.getBlockEntity(REFUSED)==clientRefused.get()&&!bool(HOLD,clientRefused.get()),100);
            server.runOnServer(s->{var level=s.overworld();var owner=(PhysicsAssemblerBlockEntity)level.getBlockEntity(REFUSED);check(!owner.isPrimaryAssembler(),"Refused assembly retained false primary ownership");check(level.getBlockState(REFUSED.below()).isAir(),"Empty source changed during refusal");check(SubLevelContainer.getContainer(level).getAllSubLevels().stream().map(v->v.getUniqueId()).collect(Collectors.toSet()).equals(initialIds.get()),"Refusal allocated a plot");report.put("refusal",Map.of("actualAssemblePacket",true,"serverException",owner.getLastAssemblyException().toString(),"primaryCleared",true,"sameClientOwnerUnlockedByActualFailurePacket",true,"sublevelsAllocated",0));});
            checks.add("Actual AssemblePacket with an empty assembly target reports failure, clears primary ownership and unlocks the same pending client lever through the real failure packet");
            server.runOnServer(s->{var level=s.overworld();var owner=(PhysicsAssemblerBlockEntity)level.getBlockEntity(ASSEMBLER);owner.assembleOrDisassemble();var created=SubLevelContainer.getContainer(level).getAllSubLevels().stream().filter(v->!initialIds.get().contains(v.getUniqueId())).toList();check(created.size()==1&&created.getFirst()instanceof ServerSubLevel,"One successful source transfer did not allocate exactly one actual plot");sub.set((ServerSubLevel)created.getFirst());checkConservation(level,sub.get());});
            int tick=server.computeOnServer(s->s.getTickCount());server.waitFor(s->s.getTickCount()>=tick+4,100);
            server.runOnServer(s->{var level=s.overworld();var moved=plotPositions(sub.get()).stream().map(level::getBlockEntity).filter(PhysicsAssemblerBlockEntity.class::isInstance).map(PhysicsAssemblerBlockEntity.class::cast).findFirst().orElseThrow();check(moved.isPrimaryAssembler(),"A real successful transfer lost its primary flag");check(moved.getBlockPos().equals(((PrimaryAssemblerExtension)sub.get()).simulated$getPrimaryAssembler()),"A real successful transfer lost its primary owner position");moved.assembleOrDisassemble();var first=(FreeConstraintHandle)get(CONSTRAINT,moved);check(first!=null&&first.isValid()&&bool(DISASSEMBLING,moved),"Initial genuine disassembly request did not create an alignment constraint");moved.assembleOrDisassemble();check(get(CONSTRAINT,moved)==first&&first.isValid(),"Duplicate request replaced its live native alignment handle");try{var stop=PhysicsAssemblerBlockEntity.class.getDeclaredMethod("stopDisassembling");stop.setAccessible(true);stop.invoke(moved);}catch(ReflectiveOperationException failure){throw new RuntimeException(failure);}check(!first.isValid()&&get(CONSTRAINT,moved)==null&&!bool(DISASSEMBLING,moved),"Native cleanup failed to retire the one real constraint");checkConservation(level,sub.get());check(moved.isPrimaryAssembler(),"Alignment cleanup incorrectly changed successful ownership");report.put("successfulTransferAndRepeatedRequest",Map.of("sourceBlocksRemaining",0,"actualPlotBlocks",3,"actualPlotDiamonds",7,"allocatedSublevels",1,"primarySurvivedTransfer",true,"sameAlignmentConstraintOnRepeatedRequest",true,"constraintRetiredByNormalCleanup",true));});
            checks.add("One native assembler transfer conserves three world/plot blocks and seven inventory diamonds; successful primary ownership survives and repeated disassembly requests retain one native alignment constraint");
            report.put("status","passed");report.put("scope","Tiny empty-target refusal and one three-block assembly; no full schematic print, broad rollback claim or complete disassembly");
        }catch(Throwable failure){report.put("status","failed");report.put("error",failure.toString());throw failure;}
        finally{Files.writeString(Path.of(System.getProperty("port.smoke.report")).resolveSibling("assembler-lifecycle.json"),new GsonBuilder().setPrettyPrinting().create().toJson(report));}
    }
    private static void runRoundtrip(ClientGameTestContext test,TestSingleplayerContext world,List<String>checks)throws Exception{
        var report=new LinkedHashMap<String,Object>();var cycles=new ArrayList<Object>();var server=world.getServer();var baseline=new AtomicReference<Set<UUID>>();
        try{
            server.runOnServer(s->{var level=s.overworld();level.setChunkForced(0,0,true);level.getChunk(ASSEMBLER);baseline.set(ids(level));for(int x=5;x<=12;x++)for(int z=5;z<=12;z++)level.setBlockAndUpdate(new BlockPos(x,126,z),Blocks.BEDROCK.defaultBlockState());level.setBlockAndUpdate(ASSEMBLER.south(3),Blocks.BEDROCK.defaultBlockState());level.setBlockAndUpdate(STONE,Blocks.STONE.defaultBlockState());level.setBlockAndUpdate(BARREL,Blocks.BARREL.defaultBlockState());var state=SimBlocks.PHYSICS_ASSEMBLER.defaultBlockState().setValue(BlockStateProperties.ATTACH_FACE,AttachFace.FLOOR).setValue(BlockStateProperties.HORIZONTAL_FACING,Direction.NORTH);level.setBlockAndUpdate(ASSEMBLER,state);((Container)level.getBlockEntity(BARREL)).setItem(0,new ItemStack(Items.DIAMOND,7));check(level.addFreshEntity(new SuperGlueEntity(level,new AABB(STONE).expandTowards(1,1,0))),"Roundtrip fixture glue was refused");});
            server.runCommand("gamemode creative @a");server.runCommand("tp @a 8.5 129 11.5 180 35");
            test.waitFor(c->c.player!=null&&c.player.position().distanceTo(new net.minecraft.world.phys.Vec3(8.5,129,11.5))<1,200);server.waitFor(s->pendingTeleport(s.getPlayerList().getPlayers().getFirst().connection)==null,200);
            for(int cycle=1;cycle<=2;cycle++){
                var row=new LinkedHashMap<String,Object>();row.put("cycle",cycle);
                var source=server.computeOnServer(s->{var cells=worldPayload(s.overworld());check(cells.size()==3,"Before assembly world payload is duplicated/incomplete: "+cells);checkPayload(s.overworld(),cells);check(ids(s.overworld()).equals(baseline.get()),"Previous plot was not retired before next cycle");return cells;});
                BlockPos worldAssembler=server.computeOnServer(s->source.stream().filter(p->s.overworld().getBlockState(p).is(SimBlocks.PHYSICS_ASSEMBLER)).findFirst().orElseThrow());
                test.waitFor(c->c.level.getBlockEntity(worldAssembler)instanceof PhysicsAssemblerBlockEntity,200);
                test.runOnClient(c->VeilPacketManager.server().sendPacket(new AssemblePacket(worldAssembler)));
                server.waitFor(s->{var added=SubLevelContainer.getContainer(s.overworld()).getAllSubLevels().stream().filter(v->!baseline.get().contains(v.getUniqueId())).toList();return added.size()==1;},200);
                var sub=server.computeOnServer(s->{var level=s.overworld();var value=(ServerSubLevel)SubLevelContainer.getContainer(level).getAllSubLevels().stream().filter(v->!baseline.get().contains(v.getUniqueId())).findFirst().orElseThrow();for(var p:source)check(LoadedLevelAccess.blockState(level,p).isAir(),"Assembly left duplicate world source at "+p);check(worldPayload(level).isEmpty(),"Assembly left payload blocks in world");var cells=plotPositions(value);check(cells.size()==3,"Assembly duplicated/lost plot blocks: "+cells);checkPayload(level,cells);return value;});
                var plotCells=server.computeOnServer(s->List.copyOf(plotPositions(sub)));
                var plotAssembler=server.computeOnServer(s->plotCells.stream().filter(p->s.overworld().getBlockState(p).is(SimBlocks.PHYSICS_ASSEMBLER)).findFirst().orElseThrow());
                test.waitFor(c->c.level.getBlockEntity(plotAssembler)instanceof PhysicsAssemblerBlockEntity,300);
                int tick=server.computeOnServer(s->s.getTickCount());server.waitFor(s->s.getTickCount()>=tick+4,100);
                test.runOnClient(c->VeilPacketManager.server().sendPacket(new AssemblePacket(plotAssembler)));
                server.waitFor(s->{var container=SubLevelContainer.getContainer(s.overworld());return (sub.isRemoved()||container.getSubLevel(sub.getUniqueId())==null)&&ids(s.overworld()).equals(baseline.get());},400);
                server.runOnServer(s->{var level=s.overworld();check(ids(level).equals(baseline.get()),"Completed disassembly retained an allocated plot");for(var p:plotCells)check(LoadedLevelAccess.blockState(level,p).isAir(),"Completed disassembly retained a duplicate plot block at "+p);var restored=worldPayload(level);check(restored.size()==3,"Completed disassembly duplicated/lost world blocks: "+restored);checkPayload(level,restored);for(var p:restored)if(level.getBlockEntity(p)instanceof PhysicsAssemblerBlockEntity owner)check(!owner.isPrimaryAssembler(),"Returned world assembler retained a stale primary owner");check(level.getEntitiesOfClass(ItemEntity.class,new AABB(4,123,4,14,133,14)).isEmpty(),"Roundtrip dropped/duplicated world inventory items");for(var p:plotCells)check(level.getEntitiesOfClass(ItemEntity.class,new AABB(p).inflate(2)).isEmpty(),"Roundtrip left dropped/duplicated items at retired plot");row.put("nativeWorldToPlotPacket",true);row.put("nativePlotToWorldPacket",true);row.put("actualWorldBlocksAfter",3);row.put("actualPlotBlocksAfter",0);row.put("actualInventoryDiamondsAfter",7);row.put("droppedInventoryItems",0);row.put("nativePlotRemoved",true);row.put("retiredPlotUuid",sub.getUniqueId().toString());row.put("restoredPositions",restored.stream().map(BlockPos::toShortString).toList());});
                cycles.add(row);
            }
            report.put("status","passed");report.put("cycles",cycles);report.put("scope","Only two completed native three-block/seven-diamond assembly-disassembly roundtrips through actual AssemblePacket and native alignment ticks; original ship printing/refusal/reentry checks not replayed");checks.add("Two completed native assembler packet roundtrips retire each plot and conserve exactly three world/plot blocks and seven inventory diamonds without dropped items or stale primary ownership");
        }catch(Throwable failure){report.put("status","failed");report.put("error",failure.toString());report.put("completedCycles",cycles);throw failure;}
        finally{Files.writeString(Path.of(System.getProperty("port.smoke.report")).resolveSibling("assembler-roundtrip.json"),new GsonBuilder().setPrettyPrinting().create().toJson(report));}
    }
    private static Set<UUID>ids(net.minecraft.server.level.ServerLevel level){return SubLevelContainer.getContainer(level).getAllSubLevels().stream().map(v->v.getUniqueId()).collect(Collectors.toSet());}
    private static List<BlockPos>worldPayload(net.minecraft.server.level.ServerLevel level){var out=new ArrayList<BlockPos>();for(var p:BlockPos.betweenClosed(new BlockPos(4,123,4),new BlockPos(13,132,13))){var state=level.getBlockState(p);if(state.is(Blocks.STONE)||state.is(Blocks.BARREL)||state.is(SimBlocks.PHYSICS_ASSEMBLER))out.add(p.immutable());}return out;}
    private static void checkPayload(net.minecraft.server.level.ServerLevel level,List<BlockPos>cells){long stone=cells.stream().filter(p->level.getBlockState(p).is(Blocks.STONE)).count(),barrels=cells.stream().filter(p->level.getBlockState(p).is(Blocks.BARREL)).count(),assemblers=cells.stream().filter(p->level.getBlockState(p).is(SimBlocks.PHYSICS_ASSEMBLER)).count();check(stone==1&&barrels==1&&assemblers==1,"Payload block identities changed: "+cells);int diamonds=0;for(var p:cells)if(level.getBlockEntity(p)instanceof Container inventory)for(int i=0;i<inventory.getContainerSize();i++)if(inventory.getItem(i).is(Items.DIAMOND))diamonds+=inventory.getItem(i).getCount();check(diamonds==7,"Payload cargo was lost or duplicated: "+diamonds);}
    private static void checkConservation(net.minecraft.server.level.ServerLevel level,ServerSubLevel sub){for(BlockPos p:SOURCE)check(level.getBlockState(p).isAir(),"A transferred source block remains at "+p);var positions=plotPositions(sub);check(positions.size()==3,"Actual plot/source block conservation failed: "+positions);long stone=positions.stream().filter(p->level.getBlockState(p).is(Blocks.STONE)).count(),barrels=positions.stream().filter(p->level.getBlockState(p).is(Blocks.BARREL)).count(),assemblers=positions.stream().filter(p->level.getBlockState(p).is(SimBlocks.PHYSICS_ASSEMBLER)).count();check(stone==1&&barrels==1&&assemblers==1,"Actual plot block identities changed");var inventory=(Container)level.getBlockEntity(positions.stream().filter(p->level.getBlockState(p).is(Blocks.BARREL)).findFirst().orElseThrow());int diamonds=0;for(int i=0;i<inventory.getContainerSize();i++)if(inventory.getItem(i).is(Items.DIAMOND))diamonds+=inventory.getItem(i).getCount();check(diamonds==7,"Transferred inventory duplicated or lost its diamonds: "+diamonds);check(level.getEntitiesOfClass(ItemEntity.class,new AABB(STONE).expandTowards(1,1,0).inflate(2)).isEmpty(),"Transfer dropped source inventory items");}
    private static List<BlockPos>plotPositions(ServerSubLevel sub){var level=sub.getLevel();var out=new ArrayList<BlockPos>();for(var holder:sub.getPlot().getLoadedChunks()){var bounds=holder.getBoundingBox();if(bounds==null)continue;for(int x=bounds.minX();x<=bounds.maxX();x++)for(int y=bounds.minY();y<=bounds.maxY();y++)for(int z=bounds.minZ();z<=bounds.maxZ();z++){var p=new BlockPos(holder.getPos().getMinBlockX()+x,y,holder.getPos().getMinBlockZ()+z);if(!level.getBlockState(p).isAir())out.add(p);}}return out;}
    private static Field field(String name){try{var f=PhysicsAssemblerBlockEntity.class.getDeclaredField(name);f.setAccessible(true);return f;}catch(ReflectiveOperationException failure){throw new ExceptionInInitializerError(failure);}}
    private static Object get(Field f,Object owner){try{return f.get(owner);}catch(IllegalAccessException failure){throw new RuntimeException(failure);}}
    private static boolean bool(Field f,Object owner){return(Boolean)get(f,owner);}
    private static Object pendingTeleport(Object connection){try{var f=connection.getClass().getDeclaredField("awaitingPositionFromClient");f.setAccessible(true);return f.get(connection);}catch(ReflectiveOperationException failure){throw new RuntimeException(failure);}}
    private static void check(boolean value,String message){if(!value)throw new AssertionError(message);}
    private PackagedAssemblerLifecycleRegression(){}
}
