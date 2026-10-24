import com.google.gson.GsonBuilder;
import com.google.gson.JsonParser;
import com.mojang.blaze3d.platform.InputConstants;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllDataComponents;
import com.simibubi.create.AllItems;
import com.simibubi.create.content.schematics.SchematicConfig;
import com.simibubi.create.content.schematics.SchematicFiles;
import com.simibubi.create.content.schematics.SchematicItem;
import com.simibubi.create.content.schematics.SchematicAndQuillItem;
import com.simibubi.create.content.schematics.SchematicStructure;
import com.simibubi.create.content.schematics.cannon.SchematicannonBlockEntity;
import com.simibubi.create.content.schematics.cannon.SchematicannonScreen;
import com.simibubi.create.content.schematics.client.SchematicIconButton;
import com.simibubi.create.content.schematics.requirement.ItemRequirement;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.MouseButtonInfo;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.properties.AttachFace;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/** Actual native Create cannon menu packets and the supplied, unchanged airship structure file. */
public final class PackagedSchematicannonRegression {
    private static final BlockPos CANNON=new BlockPos(0,128,0),ANCHOR=new BlockPos(16,128,16);
    private static final Path REPORT=Path.of(System.getProperty("port.smoke.report")).resolveSibling("schematicannon-regression.json");

    public static void run(ClientGameTestContext test,TestSingleplayerContext world,List<String>checks)throws Exception{
        String selectedScope=System.getProperty("port.regression.cannon.mode","prefix");
        if(selectedScope.equals("mini")){runMini(test,world,checks);return;}
        if(selectedScope.equals("full")){runFull(test,world,checks);return;}
        var result=new LinkedHashMap<String,Object>();var completed=new ArrayList<String>();
        var initiallyEmptySourceCells=new java.util.HashSet<BlockPos>();
        var server=world.getServer();var oldConfig=server.computeOnServer(s->SchematicConfig.get());
        float oldRate=server.computeOnServer(s->s.tickRateManager().tickrate());
        try{
            Path original=Path.of(System.getProperty("port.regression.schematic","/Users/spiceberries/Create_26_3/Simulated-Fabric/build/porting/airshipbase/airshipbase.original.nbt"));
            check(Files.isRegularFile(original),"Original user airship schematic is missing");
            String hash=HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(original)));
            Path sourceCheckpoint=REPORT.resolveSibling("schematicannon-source-audit.json");
            var previousSourceAudit=Files.isRegularFile(sourceCheckpoint)?JsonParser.parseString(Files.readString(sourceCheckpoint)).getAsJsonObject():null;
            final boolean reuseSourceAudit=previousSourceAudit!=null&&previousSourceAudit.getAsJsonObject("source").get("sha256").getAsString().equals(hash);
            result.put("source",Map.of("path",original.toString(),"sha256",hash,"unchanged",true));
            Files.createDirectories(SchematicFiles.localRoot());Files.createDirectories(SchematicFiles.uploadedRoot().resolve("codex"));
            Files.copy(original,SchematicFiles.localRoot().resolve("airshipbase.nbt"),StandardCopyOption.REPLACE_EXISTING);
            Files.copy(original,SchematicFiles.uploadedRoot().resolve("codex/airshipbase.nbt"),StandardCopyOption.REPLACE_EXISTING);
            server.runCommand("gamemode creative @a");server.runCommand("tp @a 0.5 129 3.5 180 20");
            server.runOnServer(s->{
                var level=s.overworld();load(level,CANNON);
                for(BlockPos p:BlockPos.betweenClosed(CANNON.offset(-2,-1,-2),CANNON.offset(2,2,2)))level.setBlockAndUpdate(p,Blocks.AIR.defaultBlockState());
                level.setBlockAndUpdate(CANNON,AllBlocks.SCHEMATICANNON.defaultBlockState());
                level.setBlockAndUpdate(CANNON.south(3),Blocks.STONE.defaultBlockState());
                // The real creative crate is an explicit native source feature; no paid/material path is mocked.
                level.setBlockAndUpdate(CANNON.west(),AllBlocks.CREATIVE_CRATE.defaultBlockState());
                var blueprint=SchematicItem.create(level,"airshipbase.nbt","codex");
                blueprint.set(AllDataComponents.SCHEMATIC_DEPLOYED,true);blueprint.set(AllDataComponents.SCHEMATIC_ANCHOR,ANCHOR);
                var size=blueprint.get(AllDataComponents.SCHEMATIC_BOUNDS);
                check(size!=null&&size.getX()>0&&size.getY()>0&&size.getZ()>0,"Actual source schematic has no bounds");
                for(int x=ANCHOR.getX()>>4;x<=(ANCHOR.getX()+size.getX())>>4;x++)for(int z=ANCHOR.getZ()>>4;z<=(ANCHOR.getZ()+size.getZ())>>4;z++){level.setChunkForced(x,z,true);level.getChunk(x,z);}
                cannon(level).inventory.setItem(0,blueprint);
                var player=s.getPlayerList().getPlayers().getFirst();player.setItemInHand(InteractionHand.MAIN_HAND,ItemStack.EMPTY);player.inventoryMenu.broadcastChanges();
                check(player.isCreative()&&cannon(level).canPlayerUse(player),"Real player command fixture is not creative/in cannon interaction range: "+player.position());
                result.put("nativeBounds",List.of(size.getX(),size.getY(),size.getZ()));
            });
            server.waitFor(s->cannon(s.overworld()).printer.isLoaded()||cannon(s.overworld()).statusMsg.startsWith("schematic"),200);
            server.runOnServer(s->{
                var owner=cannon(s.overworld());check(owner.printer.isLoaded(),"Actual user schematic failed native load: "+owner.statusMsg);
                check(!owner.printer.isErrored()&&!owner.printer.isWorldEmpty(),"Actual native printer rejected/emptied user schematic");
                var states=new TreeMap<String,Object>();int invalid=0;var data=owner.printer.getBlockReader();
                for(BlockPos pos:data.positions()){
                    var state=data.getBlockState(pos);
                    if(!state.isAir()&&s.overworld().getBlockState(pos).isAir())initiallyEmptySourceCells.add(pos.immutable());
                    if(reuseSourceAudit)continue;
                    var be=data.getBlockEntity(pos);var req=ItemRequirement.of(state,be);String key=state.toString();
                    if(!states.containsKey(key))states.put(key,Map.of("block",BuiltInRegistries.BLOCK.getKey(state.getBlock()).toString(),"invalid",req.isInvalid(),"empty",req.isEmpty(),"required",req.getRequiredItems().stream().map(v->v.stack.getItem().toString()+"x"+v.stack.getCount()).toList()));
                    if(req.isInvalid()&&!state.isAir())invalid++;
                }
                if(reuseSourceAudit){
                    check(data.positions().size()==previousSourceAudit.get("sourceBlocks").getAsInt()&&data.entities().size()==previousSourceAudit.get("sourceEntities").getAsInt(),"Native source inventory differs from accepted audit");
                    result.put("sourceAuditReused",previousSourceAudit);
                }else{result.put("allNativeSourceStatesAndRequirements",states);result.put("unprintableSourcePositions",invalid);}
                result.put("sourceBlocks",data.positions().size());result.put("sourceEntities",data.entities().size());
                check(owner.state==SchematicannonBlockEntity.State.PAUSED,"Inserted source should be ready, not already printing");
            });
            test.waitFor(c->c.level!=null&&c.level.getBlockEntity(CANNON)instanceof SchematicannonBlockEntity,200);
            test.waitFor(c->c.player!=null&&c.player.position().distanceTo(new Vec3(.5,129,3.5))<1,100);
            test.runOnClient(c->{c.player.setItemInHand(InteractionHand.MAIN_HAND,ItemStack.EMPTY);c.gameMode.useItemOn(c.player,InteractionHand.MAIN_HAND,new BlockHitResult(Vec3.atCenterOf(CANNON),Direction.SOUTH,CANNON,false));});
            test.waitFor(c->c.gui.screen()instanceof SchematicannonScreen,200);
            completed.add("Actual vanilla block-use packet opened native SchematicannonScreen and its real five-slot server menu");
            test.runOnClient(c->{var screen=(SchematicannonScreen)c.gui.screen();check(screen.getMenu().slots.size()==41,"Native cannon/player menu slot count");});
            click(test,"create.gui.schematicannon.showOptions");
            for(int mode=0;mode<4;mode++){
                final int selected=mode;String[]names={"dontReplaceSolid","replaceWithSolid","replaceWithAny","replaceWithEmpty"};
                click(test,"create.gui.schematicannon.option."+names[mode]);server.waitFor(s->cannon(s.overworld()).replaceMode==selected,100);
                test.waitFor(c->c.gui.screen()instanceof SchematicannonScreen screen&&screen.getMenu().owner.replaceMode==selected,100);
            }
            click(test,"create.gui.schematicannon.option.skipMissing");server.waitFor(s->cannon(s.overworld()).skipMissing,100);
            click(test,"create.gui.schematicannon.option.skipMissing");server.waitFor(s->!cannon(s.overworld()).skipMissing,100);
            click(test,"create.gui.schematicannon.option.skipBlockEntities");server.waitFor(s->cannon(s.overworld()).replaceBlockEntities,100);
            click(test,"create.gui.schematicannon.option.skipBlockEntities");server.waitFor(s->!cannon(s.overworld()).replaceBlockEntities,100);
            // Print only solid source content, retaining unrelated existing world block entities.
            click(test,"create.gui.schematicannon.option.replaceWithAny");server.waitFor(s->cannon(s.overworld()).replaceMode==2,100);
            result.put("settingsScreenshot",test.takeScreenshot("schematicannon-real-placement-settings").toString());
            completed.add("Actual native mouse clicks and server menu packets changed all four replacement modes, skip-missing and block-entity options and synchronized the owner back to the screen");
            server.runOnServer(s->{SchematicConfig.installServer(new SchematicConfig.Values(oldConfig.creativePrintIncludesAir(),oldConfig.maxSchematics(),oldConfig.maxTotalSchematicSize(),oldConfig.maxSchematicPacketSize(),oldConfig.schematicIdleTimeout(),1,oldConfig.schematicannonShotsPerGunpowder()));s.tickRateManager().setTickRate(200);});
            click(test,"create.gui.schematicannon.play");server.waitFor(s->cannon(s.overworld()).state==SchematicannonBlockEntity.State.RUNNING,100);
            server.waitFor(s->{var owner=cannon(s.overworld());check(!owner.statusMsg.equals("schematicErrored"),"Actual native print failed while scanning user source");return owner.blocksPlaced>=128;},3000);
            click(test,"create.gui.schematicannon.pause");server.waitFor(s->cannon(s.overworld()).state==SchematicannonBlockEntity.State.PAUSED,100);
            server.waitFor(s->cannon(s.overworld()).flyingBlocks.isEmpty(),300);
            int queued=server.computeOnServer(s->cannon(s.overworld()).blocksPlaced);
            int realPlaced=server.computeOnServer(s->{
                var owner=cannon(s.overworld());int count=0;
                for(BlockPos position:initiallyEmptySourceCells)if(s.overworld().getBlockState(position).getBlock()==owner.printer.getBlockReader().getBlockState(position).getBlock())count++;
                check(count>=100,"Native cannon only counted launches; fewer than100 actual source positions published");
                result.put("pausedNativeStatus",owner.statusMsg);result.put("actualInitiallyEmptyPositionsNowMatchingSourceBlock",count);result.put("nativeQueuedBlockCount",owner.blocksPlaced);result.put("nativeProgress",owner.schematicProgress);return count;
            });
            result.put("pausedScreenshot",test.takeScreenshot("schematicannon-real-airship-print-progress").toString());
            server.runOnServer(s->check(cannon(s.overworld()).blocksPlaced==queued,"Paused native cannon continued queueing"));
            click(test,"create.gui.schematicannon.play");server.waitFor(s->cannon(s.overworld()).blocksPlaced>=queued+32,1000);
            click(test,"create.gui.schematicannon.stop");server.waitFor(s->{var owner=cannon(s.overworld());return owner.state!=SchematicannonBlockEntity.State.RUNNING&&owner.blocksPlaced==0&&owner.printer.blockCursor()==-1;},100);
            server.waitFor(s->cannon(s.overworld()).flyingBlocks.isEmpty(),300);
            server.runOnServer(s->{var owner=cannon(s.overworld());check(owner.statusMsg.equals("idle")||owner.state==SchematicannonBlockEntity.State.PAUSED&&owner.statusMsg.equals("ready"),"Actual stop packet did not reset native print cursor/status: "+owner.state+"/"+owner.statusMsg);check(!owner.inventory.getItem(0).isEmpty(),"Partial print stop consumed original blueprint");result.put("nativeStoppedState",owner.state+"/"+owner.statusMsg);});
            result.put("printedPrefix",Map.of("actualPublishedAtLeast",realPlaced,"sourceFileUnchanged",true,"scope","Native first128+32 launched blocks and at least100 actual non-air source positions; the entire18659-block structure was not claimed printed.","materialMode","Actual adjacent Create creative crate; finite stock payment is outside this scope."));
            completed.add("Actual user airship file loaded, real Schematicannon launched128+32 blocks, at least100 real source positions were published, and native pause/resume/stop packets worked without a perpetual searching loop");
            checks.addAll(completed);result.put("status","passed");
        }catch(Throwable failure){result.put("status","failed");result.put("error",failure.toString());throw failure;}
        finally{
            server.runOnServer(s->{SchematicConfig.installServer(oldConfig);s.tickRateManager().setTickRate(oldRate);});
            result.put("completedChecks",completed);Files.writeString(REPORT,new GsonBuilder().setPrettyPrinting().create().toJson(result));
        }
    }

    /** Isolated three-block source: all target blocks start as air and are placed only by native launches. */
    private static void runMini(ClientGameTestContext test,TestSingleplayerContext world,List<String>checks)throws Exception{
        var result=new LinkedHashMap<String,Object>();var server=world.getServer();
        var oldConfig=server.computeOnServer(s->SchematicConfig.get());float oldRate=server.computeOnServer(s->s.tickRateManager().tickrate());
        var at=new BlockPos(-16,128,0);var anchor=new BlockPos(-32,128,16);
        try{
            server.runOnServer(s->{
                accelerate(s,oldConfig);var level=s.overworld();load(level,at);load(level,anchor);
                level.setBlockAndUpdate(at,AllBlocks.SCHEMATICANNON.defaultBlockState());level.setBlockAndUpdate(at.west(),AllBlocks.CREATIVE_CRATE.defaultBlockState());
                var tank=AllBlocks.FLUID_TANK.defaultBlockState();
                var engine=AllBlocks.STEAM_ENGINE.defaultBlockState().setValue(BlockStateProperties.ATTACH_FACE,AttachFace.FLOOR).setValue(BlockStateProperties.HORIZONTAL_FACING,Direction.NORTH);
                var shaft=AllBlocks.POWERED_SHAFT.defaultBlockState().setValue(BlockStateProperties.AXIS,Direction.Axis.X);
                var requirement=ItemRequirement.of(shaft,null);checkShaftRequirement(requirement);
                var tag=new CompoundTag();tag.put("size",SchematicAndQuillItem.integers(1,4,1));NbtUtils.addCurrentDataVersion(tag);
                var palette=new ListTag();palette.add(NbtUtils.writeBlockState(tank));palette.add(NbtUtils.writeBlockState(engine));palette.add(NbtUtils.writeBlockState(shaft));tag.put("palette",palette);
                var blocks=new ListTag();int[]ys={0,1,3};for(int index=0;index<3;index++){var entry=new CompoundTag();entry.put("pos",SchematicAndQuillItem.integers(0,ys[index],0));entry.putInt("state",index);blocks.add(entry);check(level.getBlockState(anchor.above(ys[index])).isAir(),"Mini target was already occupied");}tag.put("blocks",blocks);tag.put("entities",new ListTag());
                try{SchematicFiles.writeAtomic(SchematicFiles.uploadedRoot().resolve("codex"),"powered-shaft-regression.nbt",tag,true,1000000);}catch(java.io.IOException failure){throw new RuntimeException(failure);}
                var blueprint=SchematicItem.create(level,"powered-shaft-regression.nbt","codex");blueprint.set(AllDataComponents.SCHEMATIC_DEPLOYED,true);blueprint.set(AllDataComponents.SCHEMATIC_ANCHOR,anchor);
                var owner=(SchematicannonBlockEntity)level.getBlockEntity(at);owner.inventory.setItem(0,blueprint);owner.setState(SchematicannonBlockEntity.State.RUNNING);
            });
            server.waitFor(s->{var owner=(SchematicannonBlockEntity)s.overworld().getBlockEntity(at);check(!owner.statusMsg.equals("schematicErrored"),"Mini native printer errored");return finished(owner);},1500);
            server.waitFor(s->s.overworld().getBlockState(anchor.above(3)).is(AllBlocks.POWERED_SHAFT),100);
            server.runOnServer(s->{var level=s.overworld();var owner=(SchematicannonBlockEntity)level.getBlockEntity(at);check(owner.blocksPlaced==3,"Mini did not launch exactly tank, engine, powered shaft: "+owner.blocksPlaced);check(level.getBlockState(anchor).is(AllBlocks.FLUID_TANK)&&level.getBlockState(anchor.above()).is(AllBlocks.STEAM_ENGINE),"Native tank/engine placement failed");check(level.getBlockState(anchor.above(3)).getValue(BlockStateProperties.AXIS)==Direction.Axis.X,"Powered shaft axis changed");result.put("nativeFinalStatus",owner.statusMsg);result.put("nativeLaunchedBlocks",owner.blocksPlaced);result.put("targetStates",List.of(level.getBlockState(anchor).toString(),level.getBlockState(anchor.above()).toString(),level.getBlockState(anchor.above(3)).toString()));});
            // A later independent server observation proves the engine-owned shaft survives scheduled native validation.
            int tick=server.computeOnServer(s->s.getTickCount());server.waitFor(s->s.getTickCount()>=tick+10,100);
            server.runOnServer(s->check(s.overworld().getBlockState(anchor.above(3)).is(AllBlocks.POWERED_SHAFT),"Printed powered shaft reverted after native validation"));
            result.put("materialMode","Real adjacent native creative crate");result.put("targetWasAir",true);result.put("status","passed");
            checks.add("Native Schematicannon printed an initially-air tank, supported steam engine and powered shaft from a real three-block template; shaft survived engine validation and source became an empty schematic only after native jobs settled");
        }catch(Throwable failure){result.put("status","failed");result.put("error",failure.toString());throw failure;}
        finally{server.runOnServer(s->{SchematicConfig.installServer(oldConfig);s.tickRateManager().setTickRate(oldRate);});Files.writeString(REPORT.resolveSibling("schematicannon-mini.json"),new GsonBuilder().setPrettyPrinting().create().toJson(result));}
    }

    /** Continues the accepted prefix world; no GUI settings matrix or source palette audit is repeated. */
    private static void runFull(ClientGameTestContext test,TestSingleplayerContext world,List<String>checks)throws Exception{
        var result=new LinkedHashMap<String,Object>();var server=world.getServer();
        var oldConfig=server.computeOnServer(s->SchematicConfig.get());float oldRate=server.computeOnServer(s->s.tickRateManager().tickrate());
        var source=new java.util.concurrent.atomic.AtomicReference<SchematicStructure>();
        var oldClock=new java.util.concurrent.atomic.AtomicReference<ClientClock>();
        try{
            test.runOnClient(c->{try{
                var timer=c.getDeltaTracker();var field=net.minecraft.client.DeltaTracker.Timer.class.getDeclaredField("msPerTick");field.setAccessible(true);
                oldClock.set(new ClientClock(field.getFloat(timer),c.options.framerateLimit().get(),c.options.inactivityFpsLimit().get(),c.options.pauseOnLostFocus,c.options.enableVsync().get()));
                field.setFloat(timer,5);c.options.framerateLimit().set(240);c.getFramerateLimitTracker().setFramerateLimit(240);c.options.inactivityFpsLimit().set(net.minecraft.client.InactivityFpsLimit.MINIMIZED);c.options.pauseOnLostFocus=false;c.options.enableVsync().set(false);
                check(field.getFloat(timer)==5,"Client test clock did not accept5ms native base tick duration");
                result.put("testClock",Map.of("previousBaseMillisecondsPerTick",oldClock.get().millisecondsPerTick(),"baseMillisecondsPerTick",field.getFloat(timer),"configuredFpsLimit",c.options.framerateLimit().get(),"nativeVsync",c.options.enableVsync().get(),"nativeThrottleReason",c.getFramerateLimitTracker().getThrottleReason().toString(),"scope","Only Fabric client gametest clock; production Create, placements and server tick methods unchanged"));
            }catch(ReflectiveOperationException failure){throw new RuntimeException(failure);}});
            Path original=Path.of(System.getProperty("port.regression.schematic","/Users/spiceberries/Create_26_3/Simulated-Fabric/build/porting/airshipbase/airshipbase.original.nbt"));
            String hash=HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(original)));check(hash.equals("e85f70a6fffc3458ebfad4ee244483b654c12bed4b709e4540ee0575742c6fe6"),"Full print input changed from accepted original");result.put("sourceSha256",hash);
            Files.createDirectories(SchematicFiles.uploadedRoot().resolve("codex"));Files.copy(original,SchematicFiles.uploadedRoot().resolve("codex/airshipbase.nbt"),StandardCopyOption.REPLACE_EXISTING);
            server.runOnServer(s->{var level=s.overworld();load(level,CANNON);var owner=cannon(level);check(owner!=null&&!owner.inventory.getItem(0).isEmpty(),"Resume save lacks original cannon/source");check(owner.inventory.getItem(1).isEmpty(),"Resume source was already completed");if(!owner.printer.isLoaded()||owner.printer.isErrored()){owner.printer.loadSchematic(owner.inventory.getItem(0),level,true);result.put("nativeSourceLoadRetriedWithoutResettingSavedCursor",true);}check(owner.blocksPlaced>=6632,"Saved native6632-job progress was lost: "+owner.blocksPlaced);check(owner.printer.blockCursor()>=103876,"Saved native source cursor103876 was lost: "+owner.printer.blockCursor());result.put("nativeResumedLaunchedCount",owner.blocksPlaced);result.put("nativeResumedCursor",owner.printer.blockCursor());result.put("nativeResumedFlyingJobs",owner.flyingBlocks.size());accelerate(s,oldConfig);for(int x=1;x<=3;x++)for(int z=1;z<=7;z++){level.setChunkForced(x,z,true);level.getChunk(x,z);}});
            server.waitFor(s->cannon(s.overworld()).printer.isLoaded()&&!cannon(s.overworld()).printer.isErrored(),300);
            server.runOnServer(s->{var owner=cannon(s.overworld());source.set(owner.printer.getBlockReader());var data=source.get();check(data.positions().size()==18659&&data.entities().size()==111,"Resumed source inventory differs from accepted original");int powered=0,previouslyPlaced=0;for(BlockPos pos:data.positions()){var state=data.getBlockState(pos);if(state.is(AllBlocks.POWERED_SHAFT)){checkShaftRequirement(ItemRequirement.of(state,data.getBlockEntity(pos)));powered++;}if(!state.isAir()&&s.overworld().getBlockState(pos).getBlock()==state.getBlock())previouslyPlaced++;}check(powered==21,"Expected original21 powered shafts");check(previouslyPlaced>=100,"Accepted placed prefix was not preserved in resumed world");result.put("focusedNewPoweredShaftRequirements",powered);result.put("preservedMatchingPrefixBlocks",previouslyPlaced);result.put("sourceAudit","Reused accepted original18659-block/111-entity inventory; only21 changed powered-shaft material requirements checked");});
            // The real menu/settings/play packet is accepted by the preserved prefix proof.
            // Completion isolates the remaining native print responsibility from client menu loading.
            server.runOnServer(s->cannon(s.overworld()).setState(SchematicannonBlockEntity.State.RUNNING));
            result.put("startMode","Public native setState(RUNNING); actual GUI packets/settings reused from accepted prefix proof");
            long started=System.nanoTime();var lastProgress=new java.util.concurrent.atomic.AtomicInteger(-1);
            server.waitFor(s->{var owner=cannon(s.overworld());check(!owner.statusMsg.equals("schematicErrored"),"Actual full native print failed");if(owner.blocksPlaced/1000!=lastProgress.get()){lastProgress.set(owner.blocksPlaced/1000);System.out.println("[SCHEMATICANNON_FULL] launched="+owner.blocksPlaced+" status="+owner.statusMsg+" stage="+owner.printer.getPrintStage());}return finished(owner);},60000);
            result.put("nativePrintSeconds",(System.nanoTime()-started)/1000000000.0);
            int tick=server.computeOnServer(s->s.getTickCount());server.waitFor(s->s.getTickCount()>=tick+20,200);
            server.runOnServer(s->{var level=s.overworld();var owner=cannon(level);var data=source.get();int matched=0,exact=0,fluid=0,powered=0;var differences=new ArrayList<Object>();var mismatchByBlock=new TreeMap<String,Integer>();
                for(BlockPos pos:data.positions()){var expected=data.getBlockState(pos);var actual=level.getBlockState(pos);if(expected.is(Blocks.WATER)||expected.is(Blocks.LAVA)){fluid++;continue;}if(actual.getBlock()==expected.getBlock()){matched++;if(actual==expected)exact++;}else{String id=BuiltInRegistries.BLOCK.getKey(expected.getBlock()).toString();mismatchByBlock.merge(id,1,Integer::sum);if(differences.size()<1000)differences.add(Map.of("position",List.of(pos.getX(),pos.getY(),pos.getZ()),"source",expected.toString(),"actual",actual.toString()));}if(expected.is(AllBlocks.POWERED_SHAFT)&&actual.is(AllBlocks.POWERED_SHAFT))powered++;}
                var entityCounts=new TreeMap<String,Integer>();for(var entity:level.getAllEntities())if(entity.position().x>=16&&entity.position().x<58&&entity.position().y>=128&&entity.position().y<186&&entity.position().z>=16&&entity.position().z<120)entityCounts.merge(BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()).toString(),1,Integer::sum);
                result.put("nativeFinalStatus",owner.state+"/"+owner.statusMsg);result.put("nativeLaunchedRemainingBlocksAndEntities",owner.blocksPlaced);result.put("matchingSourceBlockTypes",matched);result.put("matchingExactSourceStates",exact);result.put("nativeFluidSourceExclusions",fluid);result.put("poweredShaftsSurviving",powered);result.put("mismatchBySourceBlock",mismatchByBlock);result.put("mismatches",differences);result.put("actualEntitiesWithinSourceBounds",entityCounts);result.put("materialMode","Real adjacent native creative crate; finite-stock payment outside this scope");
                check(fluid==46,"Unexpected native fluid exclusion count");check(powered==21,"Not all original21 powered shafts survived actual native printing");check(mismatchByBlock.isEmpty(),"Native full print completed with source block mismatches: "+mismatchByBlock);
            });
            result.put("screenshot",test.takeScreenshot("schematicannon-full-original-airship-finished").toString());result.put("status","passed");checks.add("Resumed accepted prefix world and completed real native printing of original18659-block airship, preserving all21 powered shafts and consuming source to empty-sheet output;46 native fluid source cells explicitly excluded");
        }catch(Throwable failure){result.put("status","failed");result.put("error",failure.toString());throw failure;}
        finally{if(oldClock.get()!=null)test.runOnClient(c->{try{var settings=oldClock.get();var field=net.minecraft.client.DeltaTracker.Timer.class.getDeclaredField("msPerTick");field.setAccessible(true);field.setFloat(c.getDeltaTracker(),settings.millisecondsPerTick());c.options.framerateLimit().set(settings.fpsLimit());c.getFramerateLimitTracker().setFramerateLimit(settings.fpsLimit());c.options.inactivityFpsLimit().set(settings.inactivity());c.options.pauseOnLostFocus=settings.pauseOnLostFocus();c.options.enableVsync().set(settings.vsync());}catch(ReflectiveOperationException failure){throw new RuntimeException(failure);}});server.runOnServer(s->{SchematicConfig.installServer(oldConfig);s.tickRateManager().setTickRate(oldRate);var owner=cannon(s.overworld());if(owner!=null){result.put("lastNativeState",owner.state+"/"+owner.statusMsg);result.put("lastNativeLaunched",owner.blocksPlaced);result.put("lastNativeFlyingJobs",owner.flyingBlocks.size());}});Files.writeString(REPORT.resolveSibling("schematicannon-full.json"),new GsonBuilder().setPrettyPrinting().create().toJson(result));}
    }

    private record ClientClock(float millisecondsPerTick,int fpsLimit,net.minecraft.client.InactivityFpsLimit inactivity,boolean pauseOnLostFocus,boolean vsync){}
    private static void checkShaftRequirement(ItemRequirement requirement){check(!requirement.isInvalid()&&!requirement.isEmpty()&&requirement.getRequiredItems().size()==1&&requirement.getRequiredItems().getFirst().stack.is(AllBlocks.SHAFT.asItem())&&requirement.getRequiredItems().getFirst().stack.getCount()==1&&requirement.getRequiredItems().getFirst().usage==ItemRequirement.ItemUseType.CONSUME,"Native powered shaft does not require exactly one real consumable shaft");}
    private static boolean finished(SchematicannonBlockEntity owner){return owner.state==SchematicannonBlockEntity.State.STOPPED&&owner.statusMsg.equals("finished")&&owner.flyingBlocks.isEmpty()&&!owner.printer.isLoaded()&&owner.inventory.getItem(0).isEmpty()&&owner.inventory.getItem(1).is(AllItems.EMPTY_SCHEMATIC);}
    private static void accelerate(net.minecraft.server.MinecraftServer server,SchematicConfig.Values config){SchematicConfig.installServer(new SchematicConfig.Values(config.creativePrintIncludesAir(),config.maxSchematics(),config.maxTotalSchematicSize(),config.maxSchematicPacketSize(),config.schematicIdleTimeout(),1,config.schematicannonShotsPerGunpowder()));server.tickRateManager().setTickRate(200);}
    private static void click(ClientGameTestContext test,String key){
        test.runOnClient(c->{
            check(c.gui.screen()instanceof SchematicannonScreen,"Native cannon screen closed before "+key);
            var screen=(SchematicannonScreen)c.gui.screen();SchematicIconButton target=null;
            for(var child:screen.children())if(child instanceof SchematicIconButton button&&button.getMessage().getContents()instanceof TranslatableContents text&&text.getKey().equals(key)){target=button;break;}
            check(target!=null&&target.active&&target.visible,"Actual cannon control unavailable: "+key);
            var event=new MouseButtonEvent(target.getX()+8,target.getY()+8,new MouseButtonInfo(InputConstants.MOUSE_BUTTON_LEFT,0));
            check(screen.mouseClicked(event,false),"Actual native control did not accept mouse click: "+key);screen.mouseReleased(event);
        });
    }
    private static void load(ServerLevel level,BlockPos pos){level.setChunkForced(pos.getX()>>4,pos.getZ()>>4,true);level.getChunk(pos);}
    private static SchematicannonBlockEntity cannon(ServerLevel level){return(SchematicannonBlockEntity)level.getBlockEntity(CANNON);}
    private static void check(boolean value,String message){if(!value)throw new AssertionError(message);}
    private PackagedSchematicannonRegression(){}
}
