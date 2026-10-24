import com.google.gson.GsonBuilder;
import com.mojang.blaze3d.platform.InputConstants;
import com.simibubi.create.AllDataComponents;
import com.simibubi.create.AllItems;
import com.simibubi.create.content.schematics.SchematicFiles;
import com.simibubi.create.content.schematics.SchematicItem;
import com.simibubi.create.content.schematics.SchematicPrinter;
import com.simibubi.create.content.schematics.cannon.SchematicannonBlockEntity;
import com.simibubi.create.content.schematics.cannon.SchematicannonMenu;
import com.simibubi.create.content.schematics.cannon.SchematicannonScreen;
import com.simibubi.create.content.schematics.client.SchematicIconButton;
import com.simibubi.create.foundation.utility.BlockHelper;
import dev.simulated_team.simulated.util.hold_interaction.HoldInteractionManager;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.MouseButtonInfo;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Isolates the previously unverified native menu interaction after reopening a real saved world. */
public final class PackagedSchematicannonInteraction {
    private static final BlockPos CANNON=new BlockPos(0,128,0);
    public static void run(ClientGameTestContext test,TestSingleplayerContext world,List<String>checks)throws Exception{
        var result=new LinkedHashMap<String,Object>();var server=world.getServer();
        try{
            server.runOnServer(s->checkSavedPrinterTags(result));
            server.runOnServer(s->{var level=s.overworld();var owner=(SchematicannonBlockEntity)level.getBlockEntity(CANNON);check(owner!=null&&owner.state==SchematicannonBlockEntity.State.STOPPED&&owner.statusMsg.equals("finished")&&owner.flyingBlocks.isEmpty()&&!owner.printer.isLoaded()&&owner.inventory.getItem(0).isEmpty()&&owner.inventory.getItem(1).is(AllItems.EMPTY_SCHEMATIC),"Saved full print lacks native completed/source-to-empty-output contract");check(owner.blocksPlaced==18507,"Completed native launch count changed: "+owner.blocksPlaced);var outcomes=new java.util.ArrayList<Object>();int[][]dirt={{35,136,54},{35,136,75},{35,136,76},{36,136,75},{37,136,74}};for(int[]coordinates:dirt){var pos=new BlockPos(coordinates[0],coordinates[1],coordinates[2]);var actual=level.getBlockState(pos);check(actual.is(Blocks.DIRT)||actual.is(Blocks.GRASS_BLOCK),"Previously printed dirt has an unrelated block at "+pos+": "+actual);outcomes.add(Map.of("position",List.of(coordinates[0],coordinates[1],coordinates[2]),"source","minecraft:dirt","actual",actual.toString(),"reason","Native grass spreading during elapsed world ticks"));}check(BlockHelper.setZeroAge(Blocks.LAVA_CAULDRON.defaultBlockState()).is(Blocks.CAULDRON),"Native cauldron source normalization changed");for(var pos:List.of(new BlockPos(33,156,58),new BlockPos(37,156,57))){var actual=level.getBlockState(pos);check(actual.is(Blocks.CAULDRON),"Native filled-cauldron normalization missing at "+pos+": "+actual);outcomes.add(Map.of("position",List.of(pos.getX(),pos.getY(),pos.getZ()),"source","minecraft:lava_cauldron","actual",actual.toString(),"reason","Actual native BlockHelper.setZeroAge and SchematicPlacement intentionally print an empty cauldron"));}result.put("focusedSevenNativeOutcomes",outcomes);result.put("nativeCompletedPrintAfterReload",Map.of("status",owner.state+"/"+owner.statusMsg,"launched",owner.blocksPlaced,"flyingJobs",owner.flyingBlocks.size(),"sourceEmpty",true,"emptySchematicOutput",true));});
            var original=Path.of(System.getProperty("port.regression.schematic"));Files.createDirectories(SchematicFiles.uploadedRoot().resolve("codex"));Files.copy(original,SchematicFiles.uploadedRoot().resolve("codex/airshipbase.nbt"),java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            server.runOnServer(s->{var level=s.overworld();var owner=(SchematicannonBlockEntity)level.getBlockEntity(CANNON);var blueprint=SchematicItem.create(level,"airshipbase.nbt","codex");blueprint.set(AllDataComponents.SCHEMATIC_DEPLOYED,true);blueprint.set(AllDataComponents.SCHEMATIC_ANCHOR,new BlockPos(16,128,16));owner.inventory.setItem(0,blueprint);});
            server.waitFor(s->{var owner=(SchematicannonBlockEntity)s.overworld().getBlockEntity(CANNON);return owner.printer.isLoaded()&&!owner.printer.isErrored()&&owner.state==SchematicannonBlockEntity.State.PAUSED&&owner.statusMsg.equals("ready");},300);
            result.put("menuFixture","A second genuine original blueprint inserted PAUSED after full completion/output verification; no second print");
            server.runOnServer(s->{var player=s.getPlayerList().getPlayers().getFirst();var level=s.overworld();var value=new LinkedHashMap<String,Object>();var raw=level.getBlockEntity(CANNON);value.put("playerPosition",player.position().toString());value.put("awaitingNativeTeleportAck",pendingPosition(player.connection));value.put("eyePosition",player.getEyePosition().toString());value.put("creative",player.isCreative());value.put("spectator",player.isSpectator());value.put("mayBuild",player.mayBuild());value.put("mayInteract",level.mayInteract(player,CANNON));value.put("inNativeRange",player.isWithinBlockInteractionRange(CANNON,1));value.put("mainHand",player.getMainHandItem().toString());value.put("offHand",player.getOffhandItem().toString());value.put("shift",player.isShiftKeyDown());value.put("blockState",level.getBlockState(CANNON).toString());value.put("blockEntity",raw==null?"null":raw.getClass().getName());value.put("menu",player.containerMenu.getClass().getName());if(raw instanceof SchematicannonBlockEntity owner){value.put("canPlayerUse",owner.canPlayerUse(player));value.put("nativeState",owner.state+"/"+owner.statusMsg);}result.put("serverBeforeInteraction",value);});
            snapshot(test,result,"clientBeforeChunkWait");
            test.waitFor(c->c.level!=null&&c.level.getBlockEntity(CANNON)instanceof SchematicannonBlockEntity,400);
            snapshot(test,result,"clientAfterChunkWait");
            server.waitFor(s->pendingPosition(s.getPlayerList().getPlayers().getFirst().connection).equals("none"),200);
            result.put("nativeTeleportAckReadyBeforeUse",true);
            test.runOnClient(c->{
                var hit=new BlockHitResult(Vec3.atCenterOf(CANNON),Direction.SOUTH,CANNON,false);
                var callback=UseBlockCallback.EVENT.invoker().interact(c.player,c.level,InteractionHand.MAIN_HAND,hit);
                result.put("actualClientUseBlockCallbacks",callback.toString());
                check(callback==InteractionResult.PASS,"An actual addon UseBlock callback intercepted empty-hand cannon use: "+callback);
                var interaction=c.gameMode.useItemOn(c.player,InteractionHand.MAIN_HAND,hit);result.put("actualNativeUseItemOnResult",interaction.toString());
            });
            server.waitFor(s->s.getPlayerList().getPlayers().getFirst().containerMenu instanceof SchematicannonMenu,200);
            test.waitFor(c->c.gui.screen()instanceof SchematicannonScreen,200);
            boolean previousSkip=server.computeOnServer(s->((SchematicannonBlockEntity)s.overworld().getBlockEntity(CANNON)).skipMissing);
            click(test,"create.gui.schematicannon.showOptions");click(test,"create.gui.schematicannon.option.skipMissing");
            server.waitFor(s->((SchematicannonBlockEntity)s.overworld().getBlockEntity(CANNON)).skipMissing!=previousSkip,100);
            test.waitFor(c->c.gui.screen()instanceof SchematicannonScreen screen&&screen.getMenu().owner.skipMissing!=previousSkip,100);
            click(test,"create.gui.schematicannon.option.skipMissing");server.waitFor(s->((SchematicannonBlockEntity)s.overworld().getBlockEntity(CANNON)).skipMissing==previousSkip,100);result.put("actualAfterReloadOptionToggleAndSync",true);
            snapshot(test,result,"clientAfterNativeMenuPacket");
            result.put("screenshot",test.takeScreenshot("schematicannon-native-menu-after-world-reopen").toString());
            result.put("status","passed");checks.add("After reopening the accepted real world, actual addon UseBlock callbacks passed native empty-hand cannon interaction and its server SchematicannonMenu/open-screen packet reached the real client screen");
        }catch(Throwable failure){result.put("status","failed");result.put("error",failure.toString());snapshot(test,result,"clientAtFailure");server.runOnServer(s->{var player=s.getPlayerList().getPlayers().getFirst();result.put("serverMenuAtFailure",player.containerMenu.getClass().getName());var raw=s.overworld().getBlockEntity(CANNON);result.put("serverCanUseAtFailure",raw instanceof SchematicannonBlockEntity owner&&owner.canPlayerUse(player));});throw failure;}
        finally{Files.writeString(Path.of(System.getProperty("port.smoke.report")).resolveSibling("schematicannon-interaction.json"),new GsonBuilder().setPrettyPrinting().create().toJson(result));}
    }
    private static void checkSavedPrinterTags(Map<String,Object>result){
        var empty=new SchematicPrinter();var emptyTag=new CompoundTag();empty.write(emptyTag);empty.fromTag(emptyTag,false);
        try{var resume=SchematicPrinter.class.getDeclaredField("resume");resume.setAccessible(true);check(resume.get(empty)==null,"Native empty saved cursor incorrectly retained a resume that can erase the next deployed anchor");}catch(ReflectiveOperationException failure){throw new RuntimeException(failure);}
        var anchored=new CompoundTag();new SchematicPrinter().write(anchored);anchored.store("Anchor",BlockPos.CODEC,new BlockPos(16,128,16));anchored.store("CurrentPos",BlockPos.CODEC,new BlockPos(23,24,61));anchored.putLong("Cursor",103876);anchored.putString("Fingerprint","3650b930fee14168af37dc4487c1428092f4a39626c4bd073fd09e05963f6f31");var deferred=new ListTag();var member=new CompoundTag();member.store("Pos",BlockPos.CODEC,new BlockPos(20,0,96));deferred.add(member);anchored.put("DeferredBlocks",deferred);
        var active=new SchematicPrinter();active.fromTag(anchored,false);var roundTrip=new CompoundTag();active.write(roundTrip);check(roundTrip.equals(anchored)&&active.getAnchor().equals(new BlockPos(16,128,16))&&active.blockCursor()==103876,"Compatibility changed a genuine anchored native cursor/fingerprint/deferred snapshot");
        result.put("nativeSavedPrinterTagGuards",Map.of("emptyNativeSnapshotDiscarded",true,"anchoredCursorFingerprintDeferredRoundTripUnchanged",true,"scope","Native serialization APIs, using representative anchor/cursor/fingerprint/deferred values from the saved6632-job world; no placement or print jobs"));
    }
    private static void click(ClientGameTestContext test,String key){test.runOnClient(c->{check(c.gui.screen()instanceof SchematicannonScreen,"Actual cannon screen closed before "+key);var screen=(SchematicannonScreen)c.gui.screen();SchematicIconButton target=null;for(var child:screen.children())if(child instanceof SchematicIconButton button&&button.getMessage().getContents()instanceof TranslatableContents contents&&contents.getKey().equals(key)){target=button;break;}check(target!=null&&target.active&&target.visible,"Actual after-reload option unavailable: "+key);var event=new MouseButtonEvent(target.getX()+8,target.getY()+8,new MouseButtonInfo(InputConstants.MOUSE_BUTTON_LEFT,0));check(screen.mouseClicked(event,false),"Actual option did not accept click");screen.mouseReleased(event);});}
    private static void snapshot(ClientGameTestContext test,Map<String,Object>result,String key){test.runOnClient(c->{var value=new LinkedHashMap<String,Object>();value.put("screen",c.gui.screen()==null?"null":c.gui.screen().getClass().getName());value.put("cameraPosition",c.gameRenderer.gameRenderState().levelRenderState.cameraRenderState.pos.toString());value.put("actualRay",ray(c.hitResult));value.put("holdInteractionActive",HoldInteractionManager.isActive());if(c.player!=null){value.put("playerPosition",c.player.position().toString());value.put("rotation",List.of(c.player.getYRot(),c.player.getXRot()));value.put("eyePosition",c.player.getEyePosition().toString());value.put("creative",c.player.isCreative());value.put("mayBuild",c.player.mayBuild());value.put("shift",c.player.isShiftKeyDown());value.put("mainHand",c.player.getMainHandItem().toString());value.put("offHand",c.player.getOffhandItem().toString());value.put("menu",c.player.containerMenu.getClass().getName());value.put("inNativeRange",c.player.isWithinBlockInteractionRange(CANNON,1));}if(c.level!=null){value.put("blockState",c.level.getBlockState(CANNON).toString());var raw=c.level.getBlockEntity(CANNON);value.put("blockEntity",raw==null?"null":raw.getClass().getName());if(raw instanceof SchematicannonBlockEntity owner&&c.player!=null)value.put("canPlayerUse",owner.canPlayerUse(c.player));}result.put(key,value);});}
    private static String pendingPosition(Object connection){try{var field=connection.getClass().getDeclaredField("awaitingPositionFromClient");field.setAccessible(true);var value=field.get(connection);return value==null?"none":value.toString();}catch(ReflectiveOperationException failure){throw new RuntimeException(failure);}}
    private static Object ray(net.minecraft.world.phys.HitResult hit){if(hit==null)return "null";var value=new LinkedHashMap<String,Object>();value.put("type",hit.getType().toString());value.put("location",hit.getLocation().toString());if(hit instanceof BlockHitResult block){value.put("block",block.getBlockPos().toShortString());value.put("face",block.getDirection().toString());}return value;}
    private static void check(boolean value,String message){if(!value)throw new AssertionError(message);}
    private PackagedSchematicannonInteraction(){}
}
