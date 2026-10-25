import com.google.gson.GsonBuilder;
import com.mojang.blaze3d.platform.InputConstants;
import com.simibubi.create.content.equipment.goggles.GogglesItem;
import dev.eriksonn.aeronautics.index.*;
import dev.ryanhcode.offroad.index.*;
import dev.ryanhcode.offroad.content.blocks.wheel_mount.WheelMountBlockEntity;
import dev.simulated_team.simulated.index.*;
import dev.simulated_team.simulated.content.items.rope.RopeItem.RopeItem;
import dev.simulated_team.simulated.content.entities.honey_glue.HoneyGlueEntity;
import dev.simulated_team.simulated.content.entities.launched_plunger.LaunchedPlungerEntity;
import dev.simulated_team.simulated.content.blocks.portable_engine.PortableEngineBlockEntity;
import dev.simulated_team.simulated.content.blocks.nameplate.NameplateBlockEntity;
import dev.simulated_team.simulated.content.blocks.nameplate.NameplateScreen;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Predicate;
import net.fabricmc.fabric.api.client.gametest.v1.context.*;
import net.minecraft.core.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.JukeboxBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.*;

/** Real input, packets, finite inventory and native GUIs. No direct writes to the expected outcomes. */
public final class PackagedGameplayTools {
    private static final BlockPos BASE = new BlockPos(40,120,40);
    private static final AABB AREA = new AABB(33,118,33,48,135,48);
    @FunctionalInterface private interface Scenario { void run(ClientGameTestContext t,TestSingleplayerContext w,Map<String,Object> row) throws Exception; }
    public static void run(ClientGameTestContext t,TestSingleplayerContext w,List<String> checks) throws Exception {
        var report=new LinkedHashMap<String,Object>();var rows=new ArrayList<Map<String,Object>>();var failures=new ArrayList<String>();
        report.put("scope","Native keyboard/mouse and server-authoritative survival item use. Commands prepare an isolated platform and finite stock; assertions never supply the expected result to production.");
        report.put("cases",rows);report.put("failures",failures);
        var scenarios=new LinkedHashMap<String,Scenario>();
        scenarios.put("tires",PackagedGameplayTools::tires);scenarios.put("goggles",PackagedGameplayTools::goggles);
        scenarios.put("disc",PackagedGameplayTools::disc);scenarios.put("levitating",PackagedGameplayTools::levitating);
        scenarios.put("launcher",PackagedGameplayTools::launcher);scenarios.put("rope",PackagedGameplayTools::rope);
        scenarios.put("spring",PackagedGameplayTools::spring);scenarios.put("glue",PackagedGameplayTools::glue);
        scenarios.put("guis",PackagedGameplayTools::guis);scenarios.put("engine",PackagedGameplayTools::engine);
        scenarios.put("controls",PackagedGameplayTools::controls);scenarios.put("handle",PackagedGameplayTools::handle);
        scenarios.put("lasers",PackagedGameplayTools::lasers);scenarios.put("burner",PackagedGameplayTools::burner);
        scenarios.put("cannon",PackagedGameplayTools::cannon);
        scenarios.put("navigation",PackagedGameplayTools::navigation);
        scenarios.put("staff",PackagedGameplayTools::staff);
        scenarios.put("diagram",PackagedGameplayTools::diagram);
        scenarios.put("utilities",PackagedGameplayTools::utilities);
        var selected=System.getProperty("port.smoke.auditCase","");
        var path=Path.of(System.getProperty("port.smoke.report")).resolveSibling("gameplay-tools.json");
        try {
            for(var scenario:scenarios.entrySet()){
                if(!selected.isEmpty()&&!Set.of(selected.split(",")).contains(scenario.getKey()))continue;
                var row=new LinkedHashMap<String,Object>();row.put("case",scenario.getKey());rows.add(row);
                try{prepare(t,w);scenario.getValue().run(t,w,row);row.put("status","passed");checks.add("Actual finite-stock native use: "+scenario.getKey());}
                catch(Throwable failure){row.put("status","failed");row.put("error",failure.toString());failures.add(scenario.getKey()+": "+failure);try{row.put("failureScreenshot",t.takeScreenshot("addon-use-failed-"+scenario.getKey()).toString());}catch(Throwable ignored){}}
                finally{t.getInput().releaseMouse(InputConstants.MOUSE_BUTTON_RIGHT);t.getInput().releaseKey(o->o.keyShift);t.getInput().releaseKey(o->o.keyUse);t.runOnClient(c->c.setScreenAndShow(null));Files.writeString(path,new GsonBuilder().setPrettyPrinting().create().toJson(report)+"\n");}
            }
            check(!rows.isEmpty(),"No native tool scenarios selected");report.put("status",failures.isEmpty()?"passed":"failed");check(failures.isEmpty(),"Native item-use failures: "+failures);
        }finally{Files.writeString(path,new GsonBuilder().setPrettyPrinting().create().toJson(report)+"\n");}
    }
    private static void prepare(ClientGameTestContext t,TestSingleplayerContext w){
        t.runOnClient(c->{c.setScreenAndShow(null);c.options.setCameraType(net.minecraft.client.CameraType.FIRST_PERSON);});
        var server=w.getServer();server.runCommand("gamemode survival @a");server.runCommand("time set noon");server.runCommand("weather clear");
        server.runOnServer(s->{var level=s.overworld();for(int x=34;x<=47;x++)for(int z=34;z<=47;z++){level.setBlockAndUpdate(new BlockPos(x,119,z),Blocks.STONE.defaultBlockState());for(int y=120;y<=125;y++)level.setBlockAndUpdate(new BlockPos(x,y,z),Blocks.AIR.defaultBlockState());}
            level.getEntitiesOfClass(ItemEntity.class,AREA).forEach(e->e.discard());level.getEntitiesOfClass(LaunchedPlungerEntity.class,AREA).forEach(e->e.discard());level.getEntitiesOfClass(HoneyGlueEntity.class,AREA).forEach(e->e.discard());player(s).setItemSlot(EquipmentSlot.HEAD,ItemStack.EMPTY);});
        server.runCommand("clear @a");server.runCommand("tp @a 40.5 120 43.5 180 15");t.getInput().pressKey(InputConstants.KEY_1);
        t.waitFor(c->c.player!=null&&c.player.distanceToSqr(40.5,120,43.5)<.1&&c.player.getMainHandItem().isEmpty(),200);t.waitTicks(6);
    }
    private static void supply(ClientGameTestContext t,TestSingleplayerContext w,ItemStack stock){
        t.getInput().pressKey(InputConstants.KEY_1);w.getServer().runCommand("clear @a");w.getServer().runOnServer(s->{player(s).getInventory().setItem(0,stock.copy());player(s).containerMenu.broadcastChanges();});
        t.waitFor(c->ItemStack.matches(c.player.getMainHandItem(),stock),200);t.waitTicks(6);
    }
    private static void fixture(ClientGameTestContext t,TestSingleplayerContext w,BlockPos pos,BlockState state){w.getServer().runOnServer(s->s.overworld().setBlockAndUpdate(pos,state));t.waitFor(c->c.level.getBlockState(pos).is(state.getBlock()),200);}
    private static void click(ClientGameTestContext t,BlockPos pos){
        var aim=t.computeOnClient(c->{var shape=c.level.getBlockState(pos).getShape(c.level,pos);return Vec3.atLowerCornerOf(pos).add(shape.isEmpty()?new Vec3(.5,.5,.5):shape.bounds().getCenter());});
        look(t,aim);t.waitFor(c->c.hitResult instanceof BlockHitResult hit&&hit.getBlockPos().equals(pos),200);t.getInput().pressKey(o->o.keyUse);t.waitTicks(6);
    }
    private static void look(ClientGameTestContext t,Vec3 target){var a=t.computeOnClient(c->{var d=target.subtract(c.player.getEyePosition());return new Vec2((float)Math.toDegrees(Math.atan2(-d.x,d.z)),(float)-Math.toDegrees(Math.atan2(d.y,Math.hypot(d.x,d.z))));});t.getInput().lookAt(a.x,a.y);}
    private static void await(ClientGameTestContext t,TestSingleplayerContext w,Predicate<MinecraftServer> condition,String message){for(int tick=0;tick<200;tick++){if(w.getServer().computeOnServer(condition::test))return;t.waitTicks(1);}throw new AssertionError(message+"; mainHand="+w.getServer().computeOnServer(s->player(s).getMainHandItem().toString()));}
    private static void tires(ClientGameTestContext t,TestSingleplayerContext w,Map<String,Object> row){
        var evidence=new ArrayList<Object>();row.put("tires",evidence);
        for(var item:List.of(OffroadItems.SMALL_TIRE,OffroadItems.TIRE,OffroadItems.LARGE_TIRE,OffroadItems.MONSTROUS_TIRE)){
            fixture(t,w,BASE,OffroadBlocks.WHEEL_MOUNT.defaultBlockState().setValue(HorizontalDirectionalBlock.FACING,Direction.SOUTH));supply(t,w,new ItemStack(item,2));click(t,BASE);
            await(t,w,s->((WheelMountBlockEntity)s.overworld().getBlockEntity(BASE)).getHeldItem().is(item)&&player(s).getInventory().countItem(item)==1,"Tire insertion must consume exactly one real tire");
            t.waitFor(c->c.level.getBlockEntity(BASE)instanceof WheelMountBlockEntity m&&m.getHeldItem().is(item),200);
            t.getInput().pressKey(InputConstants.KEY_2);t.waitFor(c->c.player.getMainHandItem().isEmpty(),200);t.waitTicks(6);click(t,BASE);
            await(t,w,s->((WheelMountBlockEntity)s.overworld().getBlockEntity(BASE)).getHeldItem().isEmpty()&&player(s).getInventory().countItem(item)==2,"Empty-hand tire removal must return the original without duplication");
            evidence.add(Map.of("item",BuiltInRegistries.ITEM.getKey(item).toString(),"insertedAndRemoved",true,"totalTiresAfter",2));
        }row.put("screenshot",t.takeScreenshot("addon-native-tire-insert-remove").toString());
    }
    private static void goggles(ClientGameTestContext t,TestSingleplayerContext w,Map<String,Object> row){
        supply(t,w,new ItemStack(AeroItems.AVIATORS_GOGGLES));t.getInput().lookAt(0,-80);t.getInput().pressKey(o->o.keyUse);
        await(t,w,s->player(s).getItemBySlot(EquipmentSlot.HEAD).is(AeroItems.AVIATORS_GOGGLES)&&player(s).getMainHandItem().isEmpty()&&GogglesItem.isWearingGoggles(player(s)),"Native right-click must equip one aviator goggles and enable Create goggles");
        t.waitFor(c->c.player.getItemBySlot(EquipmentSlot.HEAD).is(AeroItems.AVIATORS_GOGGLES)&&GogglesItem.isWearingGoggles(c.player),200);
        t.runOnClient(c->c.options.setCameraType(net.minecraft.client.CameraType.THIRD_PERSON_FRONT));t.waitTicks(10);row.put("screenshot",t.takeScreenshot("addon-native-aviator-goggles-equipped").toString());row.put("equippedAndCreatePredicate",true);
    }
    private static void disc(ClientGameTestContext t,TestSingleplayerContext w,Map<String,Object> row){
        fixture(t,w,BASE,Blocks.JUKEBOX.defaultBlockState());supply(t,w,new ItemStack(AeroItems.MUSIC_DISC_CLOUD_SKIPPER));click(t,BASE);
        await(t,w,s->((JukeboxBlockEntity)s.overworld().getBlockEntity(BASE)).getTheItem().is(AeroItems.MUSIC_DISC_CLOUD_SKIPPER)&&player(s).getMainHandItem().isEmpty(),"Native jukebox must accept and consume the Cloud Skipper disc");
        row.put("screenshot",t.takeScreenshot("addon-native-cloud-skipper-playing").toString());click(t,BASE);
        await(t,w,s->((JukeboxBlockEntity)s.overworld().getBlockEntity(BASE)).getTheItem().isEmpty()&&s.overworld().getEntitiesOfClass(ItemEntity.class,AREA).stream().filter(e->e.getItem().is(AeroItems.MUSIC_DISC_CLOUD_SKIPPER)).mapToInt(e->e.getItem().getCount()).sum()==1,"Native jukebox must eject exactly one original disc");row.put("playedAndEjectedExactlyOne",true);
    }
    private static void levitating(ClientGameTestContext t,TestSingleplayerContext w,Map<String,Object> row){
        var evidence=new ArrayList<Object>();row.put("items",evidence);
        for(var item:List.of(AeroItems.ENDSTONE_POWDER,AeroBlocks.LEVITITE.asItem(),AeroBlocks.PEARLESCENT_LEVITITE.asItem(),AeroItems.MUSIC_DISC_CLOUD_SKIPPER)){
            supply(t,w,new ItemStack(item,2));t.getInput().lookAt(0,0);t.getInput().pressKey(o->o.keyDrop);var uuid=new AtomicReference<UUID>();
            await(t,w,s->{var a=s.overworld().getEntitiesOfClass(ItemEntity.class,AREA).stream().filter(e->e.getItem().is(item)).toList();if(a.size()!=1)return false;uuid.set(a.getFirst().getUUID());return player(s).getInventory().countItem(item)==1;},"Native drop must create one finite levitating item");
            double y=w.getServer().computeOnServer(s->s.overworld().getEntity(uuid.get()).getY());t.waitTicks(45);
            var after=w.getServer().computeOnServer(s->{var e=(ItemEntity)s.overworld().getEntity(uuid.get());return Map.<String,Object>of("height",e.getY(),"gravity",e.getGravity(),"count",e.getItem().getCount());});
            check(((Number)after.get("gravity")).doubleValue()==0,"Levitating item retained gravity");check(((Number)after.get("height")).doubleValue()>y-.25,"Dropped levitating item fell instead of hovering");evidence.add(Map.of("item",BuiltInRegistries.ITEM.getKey(item).toString(),"startY",y,"after45Ticks",after));w.getServer().runOnServer(s->s.overworld().getEntity(uuid.get()).discard());
        }row.put("screenshot",t.takeScreenshot("addon-native-levitating-item-drop").toString());
    }
    private static void launcher(ClientGameTestContext t,TestSingleplayerContext w,Map<String,Object> row){
        w.getServer().runOnServer(s->{for(int x=37;x<=44;x++)for(int y=120;y<=124;y++)s.overworld().setBlockAndUpdate(new BlockPos(x,y,38),Blocks.STONE.defaultBlockState());});
        supply(t,w,new ItemStack(SimItems.PLUNGER_LAUNCHER));look(t,new Vec3(39.5,121.5,38.5));t.getInput().pressKey(o->o.keyUse);
        await(t,w,s->plungers(s).size()==1&&player(s).getMainHandItem().getDamageValue()==1,"First native launcher shot must create one projectile and cost one durability");t.waitTicks(12);
        look(t,new Vec3(42.5,121.5,38.5));t.getInput().pressKey(o->o.keyUse);
        await(t,w,s->plungers(s).size()==2&&plungers(s).stream().allMatch(e->e.getOther()!=null)&&player(s).getMainHandItem().getDamageValue()==2,"Second native launcher shot must link two originals and cost one durability");
        await(t,w,s->plungers(s).stream().allMatch(LaunchedPlungerEntity::isPlunged),"Native plungers must actually attach to the wall");row.put("screenshot",t.takeScreenshot("addon-native-two-linked-plungers").toString());
        t.waitTicks(20);t.getInput().holdKey(o->o.keyShift);await(t,w,s->player(s).isShiftKeyDown(),"Native launcher clear sneak state");t.getInput().pressKey(o->o.keyUse);
        await(t,w,s->plungers(s).isEmpty()&&player(s).getMainHandItem().getDamageValue()==2,"Sneak-use must clear both without firing or extra durability");row.put("twoShotsLinkedAttachedAndCleared",true);
    }
    private static List<LaunchedPlungerEntity> plungers(MinecraftServer s){return s.overworld().getEntitiesOfClass(LaunchedPlungerEntity.class,AREA);}
    private static void rope(ClientGameTestContext t,TestSingleplayerContext w,Map<String,Object> row){
        var a=BASE.west(2);var b=BASE.east(2);fixture(t,w,a,SimBlocks.ROPE_CONNECTOR.defaultBlockState());fixture(t,w,b,SimBlocks.ROPE_CONNECTOR.defaultBlockState());supply(t,w,new ItemStack(SimItems.ROPE_COUPLING,2));click(t,a);
        await(t,w,s->a.equals(player(s).getMainHandItem().get(SimDataComponents.ROPE_FIRST_CONNECTION))&&player(s).getInventory().countItem(SimItems.ROPE_COUPLING)==2,"First native rope anchor must store selection without consumption");click(t,b);
        await(t,w,s->RopeItem.getRopeHolder(s.overworld(),a).isAttached()&&RopeItem.getRopeHolder(s.overworld(),b).isAttached()&&player(s).getInventory().countItem(SimItems.ROPE_COUPLING)==1&&!player(s).getMainHandItem().has(SimDataComponents.ROPE_FIRST_CONNECTION),"Second rope click must attach both real holders and consume one coupling");
        row.put("screenshot",t.takeScreenshot("addon-native-rope-between-connectors").toString());row.put("finiteCouplingAttachedBothHolders",true);
    }
    private static void spring(ClientGameTestContext t,TestSingleplayerContext w,Map<String,Object> row){
        var a=BASE.west(2);var b=BASE.east(2);fixture(t,w,a,Blocks.STONE.defaultBlockState());fixture(t,w,b,Blocks.STONE.defaultBlockState());supply(t,w,new ItemStack(SimItems.SPRING,2));
        for(var support:List.of(a,b)){look(t,Vec3.atCenterOf(support).add(0,.499,0));t.waitFor(c->c.hitResult instanceof BlockHitResult hit&&hit.getBlockPos().equals(support)&&hit.getDirection()==Direction.UP,200);t.getInput().pressKey(o->o.keyUse);t.waitTicks(8);}
        await(t,w,s->s.overworld().getBlockState(a.above()).is(SimBlocks.SPRING)&&s.overworld().getBlockState(b.above()).is(SimBlocks.SPRING)&&player(s).getInventory().countItem(SimItems.SPRING)==1,"Two native spring clicks must create the pair and consume exactly one spring");row.put("screenshot",t.takeScreenshot("addon-native-spring-two-click-placement").toString());row.put("pairedEndsOneConsumed",true);
    }
    private static void glue(ClientGameTestContext t,TestSingleplayerContext w,Map<String,Object> row){
        var a=BASE.west();var b=BASE.east();fixture(t,w,a,Blocks.STONE.defaultBlockState());fixture(t,w,b,Blocks.STONE.defaultBlockState());supply(t,w,new ItemStack(SimItems.HONEY_GLUE));click(t,a);click(t,b);
        await(t,w,s->s.overworld().getEntitiesOfClass(HoneyGlueEntity.class,AREA).size()==1,"Two ordinary native honey-glue clicks must create the selected region");
        row.put("result",w.getServer().computeOnServer(s->{var e=s.overworld().getEntitiesOfClass(HoneyGlueEntity.class,AREA).getFirst();return Map.of("bounds",e.getBoundingBox().toString(),"durability",player(s).getMainHandItem().getDamageValue());}));row.put("screenshot",t.takeScreenshot("addon-native-honey-glue-created").toString());
    }
    private static void guis(ClientGameTestContext t,TestSingleplayerContext w,Map<String,Object> row){
        var evidence=new ArrayList<Object>();row.put("screens",evidence);
        for(var id:List.of("simulated:altitude_sensor","simulated:modulating_linked_receiver","simulated:linked_typewriter","simulated:white_nameplate")){
            if(!System.getProperty("port.smoke.auditItem","").isEmpty()&&!id.equals(System.getProperty("port.smoke.auditItem")))continue;
            row.put("currentBlock",id);
            w.getServer().runCommand("tp @a 40.5 120 43.5 180 15");t.waitFor(c->c.player.distanceToSqr(40.5,120,43.5)<.1,200);
            var block=BuiltInRegistries.BLOCK.getValue(net.minecraft.resources.Identifier.parse(id));fixture(t,w,BASE.north(),Blocks.STONE.defaultBlockState());
            var state=block.defaultBlockState();if(state.hasProperty(HorizontalDirectionalBlock.FACING))state=state.setValue(HorizontalDirectionalBlock.FACING,Direction.SOUTH);
            if(state.hasProperty(DirectionalBlock.FACING))state=state.setValue(DirectionalBlock.FACING,Direction.SOUTH);
            fixture(t,w,BASE,state);supply(t,w,ItemStack.EMPTY);
            // The front frequency slots are separate native interactions. Open the receiver body from its side.
            if(id.contains("modulating")){w.getServer().runCommand("tp @a 43.5 120 40.5 90 15");t.waitFor(c->c.player.distanceToSqr(43.5,120,40.5)<.1,200);}
            if(id.contains("typewriter")){t.getInput().holdKey(o->o.keyShift);await(t,w,s->player(s).isShiftKeyDown(),"Native typewriter editor requires sneak-use");}
            click(t,BASE);t.getInput().releaseKey(o->o.keyShift);t.waitFor(c->c.gui.screen()!=null,200);t.waitTicks(8);var name=t.computeOnClient(c->c.gui.screen().getClass().getName());
            if(id.contains("modulating")){
                var at=t.computeOnClient(c->{var input=c.gui.screen().children().stream().filter(com.simibubi.create.foundation.gui.widget.ScrollInput.class::isInstance).map(com.simibubi.create.foundation.gui.widget.ScrollInput.class::cast).min(Comparator.comparingInt(e->e.getX())).orElseThrow();return new Vec2((input.getX()+8f)*c.getWindow().getScreenWidth()/c.getWindow().getGuiScaledWidth(),(input.getY()+8f)*c.getWindow().getScreenHeight()/c.getWindow().getGuiScaledHeight());});
                t.getInput().setCursorPos(at.x,at.y);t.getInput().scroll(1);t.waitTicks(4);
            }
            if(id.contains("nameplate")){check(t.computeOnClient(c->c.gui.screen() instanceof NameplateScreen),"Nameplate opened wrong screen");t.getInput().pressKey(InputConstants.KEY_END);for(int i=0;i<40;i++)t.getInput().pressKey(InputConstants.KEY_BACKSPACE);t.getInput().typeChars("Native audit");}
            evidence.add(Map.of("block",id,"screen",name,"screenshot",t.takeScreenshot("addon-native-gui-"+id.substring(id.indexOf(':')+1)).toString()));t.getInput().pressKey(InputConstants.KEY_ESCAPE);t.waitFor(c->c.gui.screen()==null,200);t.waitTicks(6);
            if(id.contains("nameplate"))await(t,w,s->((NameplateBlockEntity)s.overworld().getBlockEntity(BASE)).getName().equals("Native audit"),"Native typed name must reach the server nameplate");
            if(id.contains("modulating"))await(t,w,s->((dev.simulated_team.simulated.content.blocks.redstone.modulating_receiver.ModulatingLinkedReceiverBlockEntity)s.overworld().getBlockEntity(BASE)).minRange==9,"Native receiver scroll editing must commit the changed minimum range to the server");
        }
    }
    private static void engine(ClientGameTestContext t,TestSingleplayerContext w,Map<String,Object> row){
        var block=BuiltInRegistries.BLOCK.getValue(net.minecraft.resources.Identifier.parse("simulated:white_portable_engine"));fixture(t,w,BASE,block.defaultBlockState());supply(t,w,new ItemStack(Items.COAL,3));click(t,BASE);
        await(t,w,s->s.overworld().getBlockEntity(BASE)instanceof PortableEngineBlockEntity e&&e.getCurrentBurnTime()>0&&player(s).getInventory().countItem(Items.COAL)==0,"Native portable engine must accept finite coal and burn");
        var redDye=BuiltInRegistries.ITEM.getValue(net.minecraft.resources.Identifier.parse("minecraft:red_dye"));
        row.put("beforeDye",w.getServer().computeOnServer(s->{var e=(PortableEngineBlockEntity)s.overworld().getBlockEntity(BASE);return Map.of("burnTime",e.getCurrentBurnTime(),"speed",e.getGeneratedSpeed(),"storedCoal",e.inventory.slot.getStack().getCount());}));supply(t,w,new ItemStack(redDye,2));click(t,BASE);
        await(t,w,s->s.overworld().getBlockState(BASE).is(SimBlocks.PORTABLE_ENGINES.get(DyeColor.RED)),"Native engine dye must change the registered color");
        row.put("afterDye",w.getServer().computeOnServer(s->{var e=(PortableEngineBlockEntity)s.overworld().getBlockEntity(BASE);return Map.of("burnTime",e.getCurrentBurnTime(),"storedCoal",e.inventory.slot.getStack().getCount(),"heldDye",player(s).getInventory().countItem(redDye));}));
        check(w.getServer().computeOnServer(s->((PortableEngineBlockEntity)s.overworld().getBlockEntity(BASE)).getCurrentBurnTime()>0),"Native recoloring discarded a running engine's fuel");row.put("screenshot",t.takeScreenshot("addon-native-fuel-and-dye-portable-engine").toString());
    }
    private static void aim(ClientGameTestContext t,BlockPos pos){
        var at=t.computeOnClient(c->{var shape=c.level.getBlockState(pos).getShape(c.level,pos);return Vec3.atLowerCornerOf(pos).add(shape.bounds().getCenter());});
        look(t,at);t.waitFor(c->c.hitResult instanceof BlockHitResult hit&&hit.getBlockPos().equals(pos),200);
    }
    private static void controls(ClientGameTestContext t,TestSingleplayerContext w,Map<String,Object> row){
        t.runOnClient(c->c.mouseHandler.grabMouse());t.getInput().moveCursor(1,1);t.waitTicks(3);
        row.put("nativeMouseGrabbed",t.computeOnClient(c->c.mouseHandler.isMouseGrabbed()));
        fixture(t,w,BASE,SimBlocks.THROTTLE_LEVER.defaultBlockState().setValue(FaceAttachedHorizontalDirectionalBlock.FACE,net.minecraft.world.level.block.state.properties.AttachFace.FLOOR));
        supply(t,w,ItemStack.EMPTY);aim(t,BASE);t.getInput().holdMouse(InputConstants.MOUSE_BUTTON_RIGHT);
        t.waitFor(c->SimClickInteractions.THROTTLE_LEVER_MANAGER.isBlockActive(BASE),200);
        t.getInput().moveCursor(0,-500);t.waitTicks(8);
        await(t,w,s->((dev.simulated_team.simulated.content.blocks.throttle_lever.ThrottleLeverBlockEntity)s.overworld().getBlockEntity(BASE)).getState()>0,"Real native mouse movement while holding the throttle must change the server redstone output");
        row.put("throttle",w.getServer().computeOnServer(s->((dev.simulated_team.simulated.content.blocks.throttle_lever.ThrottleLeverBlockEntity)s.overworld().getBlockEntity(BASE)).getState()));
        row.put("throttleScreenshot",t.takeScreenshot("addon-native-mouse-controlled-throttle").toString());
        t.getInput().releaseMouse(InputConstants.MOUSE_BUTTON_RIGHT);t.waitFor(c->!SimClickInteractions.THROTTLE_LEVER_MANAGER.isActive(),200);
        row.put("phase","steering-wheel-rim");
        var wheelState=SimBlocks.STEERING_WHEEL.defaultBlockState().setValue(HorizontalDirectionalBlock.FACING,Direction.SOUTH);
        fixture(t,w,BASE,wheelState);
        var wheel=SimBlockShapes.STEERING_WHEEL_FLOOR.get(Direction.SOUTH);
        look(t,Vec3.atLowerCornerOf(BASE).add(wheel.bounds().getCenter()).add(.45,0,0));
        t.waitFor(c->dev.simulated_team.simulated.content.blocks.steering_wheel.SteeringWheelBlock.lookingAtWheel(c.player,BASE,1,c.level.getBlockState(BASE)),200);
        t.getInput().holdMouse(InputConstants.MOUSE_BUTTON_RIGHT);
        t.waitFor(c->SimClickInteractions.STEERING_WHEEL_MANAGER.isBlockActive(BASE),200);t.getInput().moveCursor(500,0);t.waitTicks(8);
        t.getInput().releaseMouse(InputConstants.MOUSE_BUTTON_RIGHT);t.waitFor(c->!SimClickInteractions.STEERING_WHEEL_MANAGER.isActive(),200);
        await(t,w,s->Math.abs(((dev.simulated_team.simulated.content.blocks.steering_wheel.SteeringWheelBlockEntity)s.overworld().getBlockEntity(BASE)).targetAngle)>1,"Real held steering-wheel mouse movement and release must commit a server target angle");
        row.put("steeringTargetAngle",w.getServer().computeOnServer(s->((dev.simulated_team.simulated.content.blocks.steering_wheel.SteeringWheelBlockEntity)s.overworld().getBlockEntity(BASE)).targetAngle));
        row.put("steeringScreenshot",t.takeScreenshot("addon-native-mouse-controlled-steering-wheel").toString());
    }
    private static void handle(ClientGameTestContext t,TestSingleplayerContext w,Map<String,Object> row){
        fixture(t,w,BASE.north(),Blocks.STONE.defaultBlockState());
        var state=SimBlocks.IRON_HANDLE.defaultBlockState();if(state.hasProperty(DirectionalBlock.FACING))state=state.setValue(DirectionalBlock.FACING,Direction.SOUTH);
        fixture(t,w,BASE,state);supply(t,w,ItemStack.EMPTY);aim(t,BASE);t.getInput().holdMouse(InputConstants.MOUSE_BUTTON_RIGHT);
        t.waitFor(c->SimClickInteractions.HANDLE_HANDLER.isBlockActive(BASE),200);
        await(t,w,s->((dev.simulated_team.simulated.content.blocks.handle.HandleBlockEntity)s.overworld().getBlockEntity(BASE)).hasPlayer(),"Native handle hold must register the actual player on the server");
        row.put("screenshot",t.takeScreenshot("addon-native-handle-held").toString());t.getInput().releaseMouse(InputConstants.MOUSE_BUTTON_RIGHT);
        await(t,w,s->!((dev.simulated_team.simulated.content.blocks.handle.HandleBlockEntity)s.overworld().getBlockEntity(BASE)).hasPlayer(),"Native handle release must retire the server holder");row.put("heldAndReleasedActualPlayer",true);
    }
    private static BlockState lever(){return Blocks.LEVER.defaultBlockState().setValue(FaceAttachedHorizontalDirectionalBlock.FACE,net.minecraft.world.level.block.state.properties.AttachFace.FLOOR);}
    private static void lasers(ClientGameTestContext t,TestSingleplayerContext w,Map<String,Object> row){
        var pointer=BASE.west(2);var sensor=BASE.east(2);var control=pointer.south();
        fixture(t,w,sensor.east(),Blocks.STONE.defaultBlockState());
        fixture(t,w,pointer,SimBlocks.LASER_POINTER.defaultBlockState().setValue(DirectionalBlock.FACING,Direction.EAST));
        fixture(t,w,sensor,SimBlocks.LASER_SENSOR.defaultBlockState().setValue(HorizontalDirectionalBlock.FACING,Direction.WEST));fixture(t,w,control,lever());supply(t,w,ItemStack.EMPTY);click(t,control);
        await(t,w,s->((dev.simulated_team.simulated.content.blocks.lasers.laser_sensor.LaserSensorBlockEntity)s.overworld().getBlockEntity(sensor)).currentPower>0,"Native powered laser must reach its facing sensor and generate a real server signal");
        row.put("litPower",w.getServer().computeOnServer(s->((dev.simulated_team.simulated.content.blocks.lasers.laser_sensor.LaserSensorBlockEntity)s.overworld().getBlockEntity(sensor)).currentPower));
        row.put("screenshot",t.takeScreenshot("addon-native-laser-sensor-active").toString());fixture(t,w,BASE,Blocks.STONE.defaultBlockState());
        await(t,w,s->((dev.simulated_team.simulated.content.blocks.lasers.laser_sensor.LaserSensorBlockEntity)s.overworld().getBlockEntity(sensor)).currentPower==0,"Physical obstruction must turn the real laser sensor off");
        fixture(t,w,BASE,Blocks.AIR.defaultBlockState());await(t,w,s->((dev.simulated_team.simulated.content.blocks.lasers.laser_sensor.LaserSensorBlockEntity)s.overworld().getBlockEntity(sensor)).currentPower>0,"Removing the laser obstruction must restore the signal");
        click(t,control);await(t,w,s->((dev.simulated_team.simulated.content.blocks.lasers.laser_sensor.LaserSensorBlockEntity)s.overworld().getBlockEntity(sensor)).currentPower==0,"Native lever off must stop the laser signal");row.put("onOccludedRestoredOff",true);
        for(int x=39;x<=47;x++)fixture(t,w,new BlockPos(x,120,40),Blocks.AIR.defaultBlockState());
        fixture(t,w,pointer,SimBlocks.OPTICAL_SENSOR.defaultBlockState().setValue(HorizontalDirectionalBlock.FACING,Direction.EAST));fixture(t,w,sensor,Blocks.STONE.defaultBlockState());
        await(t,w,s->((dev.simulated_team.simulated.content.blocks.lasers.optical_sensor.OpticalSensorBlockEntity)s.overworld().getBlockEntity(pointer)).hasHit(),"Native optical sensor must observe a real stone target");
        fixture(t,w,sensor,Blocks.AIR.defaultBlockState());
        await(t,w,s->!((dev.simulated_team.simulated.content.blocks.lasers.optical_sensor.OpticalSensorBlockEntity)s.overworld().getBlockEntity(pointer)).hasHit(),"Removing the stone target must retire the optical hit");row.put("opticalTargetAddedAndRemoved",true);
    }
    private static void burner(ClientGameTestContext t,TestSingleplayerContext w,Map<String,Object> row){
        fixture(t,w,BASE,AeroBlocks.HOT_AIR_BURNER.defaultBlockState());var control=BASE.south();fixture(t,w,control,lever());supply(t,w,ItemStack.EMPTY);click(t,control);
        await(t,w,s->((dev.eriksonn.aeronautics.content.blocks.hot_air.hot_air_burner.HotAirBurnerBlockEntity)s.overworld().getBlockEntity(BASE)).getGasOutput()>0,"Native lever-powered burner must emit hot air");
        row.put("gasOutput",w.getServer().computeOnServer(s->((dev.eriksonn.aeronautics.content.blocks.hot_air.hot_air_burner.HotAirBurnerBlockEntity)s.overworld().getBlockEntity(BASE)).getGasOutput()));
        row.put("screenshot",t.takeScreenshot("addon-native-adjustable-burner-powered").toString());click(t,control);
        await(t,w,s->((dev.eriksonn.aeronautics.content.blocks.hot_air.hot_air_burner.HotAirBurnerBlockEntity)s.overworld().getBlockEntity(BASE)).getGasOutput()==0,"Native lever off must stop hot-air output");row.put("redstoneOnOff",true);
    }
    private static void cannon(ClientGameTestContext t,TestSingleplayerContext w,Map<String,Object> row){
        var state=AeroBlocks.MOUNTED_POTATO_CANNON.defaultBlockState().setValue(DirectionalBlock.FACING,Direction.EAST);
        var shaft=Direction.fromAxisAndDirection(AeroBlocks.MOUNTED_POTATO_CANNON.getRotationAxis(state),Direction.AxisDirection.NEGATIVE);var motorPos=BASE.relative(shaft);
        row.put("nativeShaftDirection",shaft.toString());fixture(t,w,BASE,state);
        fixture(t,w,motorPos,com.simibubi.create.AllBlocks.CREATIVE_MOTOR.defaultBlockState().setValue(DirectionalBlock.FACING,shaft.getOpposite()));
        w.getServer().runOnServer(s->{var motor=(com.simibubi.create.content.kinetics.motor.CreativeMotorBlockEntity)s.overworld().getBlockEntity(motorPos);motor.generatedSpeed.value=256;motor.updateGeneratedRotation();});
        await(t,w,s->Math.abs(((dev.eriksonn.aeronautics.content.blocks.mounted_potato_cannon.MountedPotatoCannonBlockEntity)s.overworld().getBlockEntity(BASE)).getSpeed())>0,"Real cannon shaft must receive native motor rotation before firing");
        supply(t,w,new ItemStack(Items.POTATO,3));click(t,BASE);
        await(t,w,s->((dev.eriksonn.aeronautics.content.blocks.mounted_potato_cannon.MountedPotatoCannonBlockEntity)s.overworld().getBlockEntity(BASE)).getInventory().slot.getStack().getCount()==3&&player(s).getInventory().countItem(Items.POTATO)==0,"Native cannon loading must transfer exactly three original potatoes");
        var control=BASE.south();fixture(t,w,control,lever());supply(t,w,ItemStack.EMPTY);click(t,control);
        await(t,w,s->!s.overworld().getEntitiesOfClass(com.simibubi.create.content.equipment.potatoCannon.PotatoProjectileEntity.class,AREA.inflate(60)).isEmpty(),"Native kinetic charge and lever must fire a real potato projectile");
        click(t,control);
        row.put("remainingPotatoes",w.getServer().computeOnServer(s->((dev.eriksonn.aeronautics.content.blocks.mounted_potato_cannon.MountedPotatoCannonBlockEntity)s.overworld().getBlockEntity(BASE)).getInventory().slot.getStack().getCount()));
        row.put("screenshot",t.takeScreenshot("addon-native-mounted-potato-cannon-fired").toString());row.put("actualPlayerLoadedAndLeverFired",true);
    }
    private static void navigation(ClientGameTestContext t,TestSingleplayerContext w,Map<String,Object> row){
        fixture(t,w,BASE,SimBlocks.NAVIGATION_TABLE.defaultBlockState().setValue(DirectionalBlock.FACING,Direction.UP));supply(t,w,new ItemStack(Items.COMPASS,2));click(t,BASE);
        await(t,w,s->((dev.simulated_team.simulated.content.blocks.nav_table.NavTableBlockEntity)s.overworld().getBlockEntity(BASE)).getHeldItem().is(Items.COMPASS)&&player(s).getInventory().countItem(Items.COMPASS)==1,"Native navigation-table insertion must retain exactly one original compass");
        await(t,w,s->((dev.simulated_team.simulated.content.blocks.nav_table.NavTableBlockEntity)s.overworld().getBlockEntity(BASE)).getTargetPosition(true)!=null,"Native compass must supply a real navigation target");
        row.put("target",w.getServer().computeOnServer(s->((dev.simulated_team.simulated.content.blocks.nav_table.NavTableBlockEntity)s.overworld().getBlockEntity(BASE)).getTargetPosition(true).toString()));
        row.put("screenshot",t.takeScreenshot("addon-native-navigation-table-compass").toString());t.getInput().pressKey(InputConstants.KEY_2);t.waitFor(c->c.player.getMainHandItem().isEmpty(),200);t.waitTicks(6);click(t,BASE);
        await(t,w,s->((dev.simulated_team.simulated.content.blocks.nav_table.NavTableBlockEntity)s.overworld().getBlockEntity(BASE)).getHeldItem().isEmpty()&&player(s).getInventory().countItem(Items.COMPASS)==2,"Native empty-hand navigation extraction must return both original compasses");row.put("insertTargetExtract",true);
    }
    private record PreparedBody(dev.ryanhcode.sable.sublevel.ServerSubLevel ship,BlockPos local){}
    private static PreparedBody assembleBody(ClientGameTestContext t,TestSingleplayerContext w){
        var plot=new AtomicReference<dev.ryanhcode.sable.sublevel.ServerSubLevel>();var local=new AtomicReference<BlockPos>();
        w.getServer().runOnServer(s->{var level=s.overworld();var cells=new HashSet<BlockPos>(List.of(BASE,BASE.east(),BASE.above()));for(var p:cells)level.setBlockAndUpdate(p,Blocks.OAK_PLANKS.defaultBlockState());
            var ship=dev.ryanhcode.sable.api.SubLevelAssemblyHelper.assembleBlocks(level,BASE,cells,new dev.ryanhcode.sable.companion.math.BoundingBox3i(BASE,BASE.east().above()));check(ship!=null,"Native staff fixture assembly failed");plot.set(ship);local.set(BlockPos.containing(ship.logicalPose().transformPositionInverse(Vec3.atCenterOf(BASE.above()))));});
        var ship=plot.get();
        await(t,w,s->ship.getMassTracker().getMass()>0&&dev.ryanhcode.sable.api.physics.handle.RigidBodyHandle.of(ship)!=null,"Native staff fixture rigid body must initialize");
        t.waitFor(c->dev.ryanhcode.sable.api.sublevel.SubLevelContainer.getContainer(c.level).getSubLevel(ship.getUniqueId())instanceof dev.ryanhcode.sable.sublevel.ClientSubLevel clientShip&&clientShip.isFinalized(),400);
        return new PreparedBody(ship,local.get());
    }
    private static void staff(ClientGameTestContext t,TestSingleplayerContext w,Map<String,Object> row){
        row.put("fixtureScope","A real three-block Sable body is prepared through its native assembly API; all staff lock, unlock, drag and stop actions below use physical player input and real packets. No test pose, force or velocity writes.");
        var body=assembleBody(t,w);var ship=body.ship();row.put("ship",ship.getUniqueId().toString());
        supply(t,w,new ItemStack(SimItems.PHYSICS_STAFF));t.waitTicks(20);
        var target=t.computeOnClient(c->{var sub=dev.ryanhcode.sable.api.sublevel.SubLevelContainer.getContainer(c.level).getSubLevel(ship.getUniqueId());return sub.logicalPose().transformPosition(Vec3.atCenterOf(body.local()));});look(t,target);
        t.waitFor(c->c.hitResult instanceof BlockHitResult hit&&dev.ryanhcode.sable.Sable.HELPER.getContaining(c.level,hit.getBlockPos())!=null&&dev.ryanhcode.sable.Sable.HELPER.getContaining(c.level,hit.getBlockPos()).getUniqueId().equals(ship.getUniqueId()),200);
        t.getInput().pressMouse(InputConstants.MOUSE_BUTTON_LEFT);
        await(t,w,s->dev.simulated_team.simulated.content.physics_staff.PhysicsStaffServerHandler.get(s.overworld()).isLocked(ship),"Native physics-staff attack must lock the actual body");t.waitTicks(25);
        t.getInput().pressMouse(InputConstants.MOUSE_BUTTON_LEFT);
        await(t,w,s->!dev.simulated_team.simulated.content.physics_staff.PhysicsStaffServerHandler.get(s.overworld()).isLocked(ship),"Second native staff attack must unlock the same body");t.waitTicks(25);
        t.getInput().pressMouse(InputConstants.MOUSE_BUTTON_RIGHT);
        await(t,w,PackagedGameplayTools::dragging,"Native staff right-click must start a real server drag session");
        var start=w.getServer().computeOnServer(s->new org.joml.Vector3d(ship.logicalPose().position()));
        t.getInput().lookAt(180,-20);t.waitTicks(30);
        var distance=w.getServer().computeOnServer(s->ship.logicalPose().position().distance(start));row.put("dragDistance",distance);
        check(distance>.1,"Actual staff drag did not move the native body");row.put("screenshot",t.takeScreenshot("addon-native-physics-staff-dragging").toString());
        t.getInput().pressMouse(InputConstants.MOUSE_BUTTON_RIGHT);await(t,w,s->!dragging(s),"Second native staff right-click must retire the server drag session");row.put("lockedUnlockedDraggedAndStopped",true);
    }
    private static void diagram(ClientGameTestContext t,TestSingleplayerContext w,Map<String,Object> row){
        var body=assembleBody(t,w);supply(t,w,new ItemStack(SimItems.CONTRAPTION_DIAGRAM));t.waitTicks(15);
        var at=t.computeOnClient(c->{var sub=dev.ryanhcode.sable.api.sublevel.SubLevelContainer.getContainer(c.level).getSubLevel(body.ship().getUniqueId());return sub.logicalPose().transformPosition(Vec3.atCenterOf(body.local()));});look(t,at);
        t.waitFor(c->c.hitResult instanceof BlockHitResult hit&&dev.ryanhcode.sable.Sable.HELPER.getContaining(c.level,hit.getBlockPos())!=null,200);t.getInput().pressKey(o->o.keyUse);
        var uuid=new AtomicReference<UUID>();
        await(t,w,s->{for(var entity:s.overworld().getAllEntities())if(entity instanceof dev.simulated_team.simulated.content.entities.diagram.DiagramEntity diagram){uuid.set(diagram.getUUID());return player(s).getMainHandItem().isEmpty();}return false;},"Native diagram use must place one original board on the assembled body");
        t.waitFor(c->c.level.entitiesForRendering().iterator().hasNext()&&c.level.getEntitiesOfClass(dev.simulated_team.simulated.content.entities.diagram.DiagramEntity.class,new AABB(body.local()).inflate(5)).stream().anyMatch(e->e.getUUID().equals(uuid.get())),200);
        var target=t.computeOnClient(c->{for(var entity:c.level.entitiesForRendering())if(entity.getUUID().equals(uuid.get()))return dev.ryanhcode.sable.Sable.HELPER.projectOutOfSubLevel(c.level,entity.getBoundingBox().getCenter());throw new AssertionError("Paired diagram missing");});look(t,target);
        t.waitFor(c->c.hitResult instanceof EntityHitResult hit&&hit.getEntity().getUUID().equals(uuid.get()),200);t.getInput().pressKey(o->o.keyUse);
        t.waitFor(c->c.gui.screen()instanceof dev.simulated_team.simulated.content.entities.diagram.screen.DiagramScreen,400);t.waitTicks(15);
        row.put("actualDiagram",uuid.get().toString());row.put("ship",body.ship().getUniqueId().toString());row.put("screenshot",t.takeScreenshot("addon-native-diagram-screen-on-assembled-body").toString());
        t.getInput().pressKey(InputConstants.KEY_ESCAPE);t.waitFor(c->c.gui.screen()==null,200);row.put("nativePlacementAndEntityUseOpenedScreen",true);
    }
    private static boolean dragging(MinecraftServer server){
        try{var handler=dev.simulated_team.simulated.content.physics_staff.PhysicsStaffServerHandler.get(server.overworld());var field=handler.getClass().getDeclaredField("draggingSessions");field.setAccessible(true);return ((Map<?,?>)field.get(handler)).containsKey(player(server).getUUID());}
        catch(ReflectiveOperationException failure){throw new AssertionError(failure);}
    }
    private static void utilities(ClientGameTestContext t,TestSingleplayerContext w,Map<String,Object> row){
        var evidence=new ArrayList<Object>();row.put("redstone",evidence);
        for(var accumulator:new boolean[]{true,false}){
            prepare(t,w);var block=accumulator?SimBlocks.REDSTONE_ACCUMULATOR:SimBlocks.REDSTONE_INDUCTOR;
            fixture(t,w,BASE,block.defaultBlockState().setValue(HorizontalDirectionalBlock.FACING,Direction.SOUTH));var back=BASE.south();fixture(t,w,back,lever());supply(t,w,ItemStack.EMPTY);click(t,back);
            await(t,w,s->storedSignal(s,accumulator)>=3,"Native redstone device must accumulate/ramp its real input over ordinary ticks");click(t,back);
            int charged=w.getServer().computeOnServer(s->storedSignal(s,accumulator));
            if(accumulator){t.waitTicks(25);check(w.getServer().computeOnServer(s->storedSignal(s,true))==charged,"Accumulator must retain its real stored signal after input stops");var side=BASE.east();fixture(t,w,side,lever());click(t,side);await(t,w,s->storedSignal(s,true)==0,"Native side input must discharge the redstone accumulator");click(t,side);}
            else await(t,w,s->storedSignal(s,false)==0,"Native inductor must ramp down after the input stops");
            w.getServer().runCommand("tp @a 43.5 120 40.5 90 20");t.waitFor(c->c.player.distanceToSqr(43.5,120,40.5)<.1,200);click(t,BASE);
            var property=accumulator?dev.simulated_team.simulated.content.blocks.redstone.redstone_accumulator.RedstoneAccumulatorBlock.INVERTED:dev.simulated_team.simulated.content.blocks.redstone.redstone_inductor.RedstoneInductorBlock.INVERTED;
            await(t,w,s->s.overworld().getBlockState(BASE).getValue(property),"Native empty-hand use must invert the redstone device");
            evidence.add(Map.of("block",BuiltInRegistries.BLOCK.getKey(block).toString(),"chargedSignal",charged,"discharged",true,"nativeInversion",true));
        }
        prepare(t,w);fixture(t,w,BASE,SimBlocks.REDSTONE_MAGNET.defaultBlockState().setValue(DirectionalBlock.FACING,Direction.EAST));var control=BASE.south();fixture(t,w,control,lever());supply(t,w,ItemStack.EMPTY);click(t,control);
        await(t,w,s->((dev.simulated_team.simulated.content.blocks.redstone_magnet.RedstoneMagnetBlockEntity)s.overworld().getBlockEntity(BASE)).magnetActive(),"Native lever must activate the redstone magnet");
        row.put("activeMagneticMoment",w.getServer().computeOnServer(s->((dev.simulated_team.simulated.content.blocks.redstone_magnet.RedstoneMagnetBlockEntity)s.overworld().getBlockEntity(BASE)).setMagneticMoment(new org.joml.Vector3d()).length()));
        check(((Number)row.get("activeMagneticMoment")).doubleValue()>0,"Powered native magnet has no magnetic moment");click(t,control);
        await(t,w,s->!((dev.simulated_team.simulated.content.blocks.redstone_magnet.RedstoneMagnetBlockEntity)s.overworld().getBlockEntity(BASE)).magnetActive(),"Native lever off must retire the magnet field");
        prepare(t,w);fixture(t,w,BASE,SimBlocks.DOCKING_CONNECTOR.defaultBlockState().setValue(DirectionalBlock.FACING,Direction.EAST));fixture(t,w,control,lever());supply(t,w,ItemStack.EMPTY);click(t,control);
        await(t,w,s->((dev.simulated_team.simulated.content.blocks.docking_connector.DockingConnectorBlockEntity)s.overworld().getBlockEntity(BASE)).isExtended()&&s.overworld().getBlockState(BASE.east()).is(SimBlocks.PAIRED_DOCKING_CONNECTOR),"Native powered dock must extend its actual paired block");
        row.put("dockScreenshot",t.takeScreenshot("addon-native-dock-extension").toString());click(t,control);
        await(t,w,s->((dev.simulated_team.simulated.content.blocks.docking_connector.DockingConnectorBlockEntity)s.overworld().getBlockEntity(BASE)).isRetracted()&&s.overworld().getBlockState(BASE.east()).isAir(),"Native dock retraction must remove the paired extension");row.put("dockExtendedAndRetracted",true);
        row.put("scope","Native accumulator charge/hold/side-discharge, inductor rise/fall, inversion, magnet field on/off and dock extension/retraction. This case does not certify item/fluid/energy transfer across a docked pair or magnetic attraction between two moving ships.");
    }
    private static int storedSignal(MinecraftServer server,boolean accumulator){
        var level=server.overworld();var state=level.getBlockState(BASE);
        return Arrays.stream(Direction.values()).mapToInt(side->state.getSignal(level,BASE,side)).max().orElse(0);
    }
    private static ServerPlayer player(MinecraftServer s){return s.getPlayerList().getPlayers().getFirst();}
    private static void check(boolean condition,String message){if(!condition)throw new AssertionError(message);}
    private PackagedGameplayTools(){}
}
