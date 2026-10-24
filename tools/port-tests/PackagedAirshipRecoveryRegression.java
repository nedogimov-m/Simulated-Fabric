import com.google.gson.GsonBuilder;
import com.google.gson.JsonParser;
import dev.eriksonn.aeronautics.content.blocks.propeller.bearing.propeller_bearing.PropellerBearingBlockEntity;
import dev.ryanhcode.sable.api.sublevel.SubLevelContainer;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import dev.simulated_team.simulated.content.blocks.physics_assembler.PhysicsAssemblerBlockEntity;
import dev.simulated_team.simulated.content.blocks.throttle_lever.ThrottleLeverBlock;
import dev.simulated_team.simulated.content.blocks.throttle_lever.ThrottleLeverBlockEntity;
import dev.simulated_team.simulated.content.physics_staff.PhysicsStaffServerHandler;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/** Reversible recovery of the exact diagnosed static obstruction and original inputs. */
public final class PackagedAirshipRecoveryRegression {
    private static final List<BlockPos> MAIN_PROPS=List.of(new BlockPos(6,19,78),new BlockPos(34,19,78));
    private static final Map<String,Integer> EXPECTED=Map.of("simulated:white_symmetric_sail",190,"minecraft:dark_oak_planks",60,"minecraft:dark_oak_stairs",28,"minecraft:lantern",11,"minecraft:spruce_log",8);
    public static void run(ClientGameTestContext test,TestSingleplayerContext world,List<String> checks) throws Exception {
        var report=new LinkedHashMap<String,Object>();var actions=new ArrayList<Object>();var samples=new ArrayList<Map<String,Object>>();
        report.put("scope","Recover only the exact297previously diagnosed static obstruction blocks, return all materials into empty onboard chest slots, restore original8water sources once, then native main throttle/bearing controls. No player teleport, force, pose, RPM, gas, boiler or creative-fuel writes.");
        report.put("actions",actions);report.put("samples",samples);
        var path=Path.of(System.getProperty("port.smoke.report")).resolveSibling("airship-recovery.json");
        try {
            var defaultReference=path.getParent().getParent().getParent().resolve("airship-reload-r8-obstructions/client/airship-reload.json");
            var source=Path.of(System.getProperty("port.regression.airshipObstructions",defaultReference.toString()));
            if(!source.isAbsolute())source=Path.of(System.getProperty("user.dir")).resolve(source);
            var reference=JsonParser.parseString(Files.readString(source)).getAsJsonObject().getAsJsonObject("worldObstructions");
            check(reference.get("solidBlocks").getAsInt()==297,"Unexpected obstruction reference count");
            var referenceBlocks=reference.getAsJsonArray("first1000");check(referenceBlocks.size()==297,"Incomplete obstruction reference");
            var expectedStates=new TreeMap<String,String>();
            for(var e:referenceBlocks){var block=e.getAsJsonObject();check(!block.get("blockEntity").getAsBoolean(),"Reference includes block entity");var pos=parsePos(block.get("position").getAsString());check(pos.getX()==35,"Reference includes other x coordinate");check(expectedStates.put(pos.toShortString(),block.get("state").getAsString())==null,"Duplicate obstruction reference");}
            report.put("obstructionReference",source.toString());
            var server=world.getServer();report.put("before",server.computeOnServer(PackagedAirshipReloadRegression::sample));
            var identityBefore=server.computeOnServer(PackagedAirshipRecoveryRegression::identity);report.put("playerIdentityBefore",identityBefore);
            server.runOnServer(s->{
                var level=s.overworld();var all=SubLevelContainer.getContainer(level).getAllSubLevels();check(all.size()==1,"Expected one ship");var ship=(ServerSubLevel)all.getFirst();
                var actual=PackagedAirshipReloadRegression.obstructions(s);report.put("obstructionsBefore",actual);
                check(((Number)actual.get("solidBlocks")).intValue()==297,"Static obstruction count changed; refusing removal");
                var actualStates=new TreeMap<String,String>();for(var item:(List<?>)actual.get("first1000")){var row=(Map<?,?>)item;check(Boolean.FALSE.equals(row.get("blockEntity")),"Current obstruction contains a block entity");actualStates.put(String.valueOf(row.get("position")),String.valueOf(row.get("state")));}
                check(actualStates.equals(expectedStates),"Static obstruction differs from reference; refusing removal");
                var counts=new TreeMap<String,Integer>();var items=new LinkedHashMap<String,net.minecraft.world.item.Item>();
                for(var entry:expectedStates.entrySet()){
                    var pos=parsePos(entry.getKey());var state=level.getBlockState(pos);check(state.toString().equals(entry.getValue())&&level.getBlockEntity(pos)==null,"Obstruction changed at"+pos);
                    var id=BuiltInRegistries.BLOCK.getKey(state.getBlock()).toString();counts.merge(id,1,Integer::sum);items.put(id,state.getBlock().asItem());
                }
                check(counts.equals(EXPECTED),"Obstruction materials differ from diagnosed types: "+counts);
                var materialStacks=new ArrayList<ItemStack>();
                for(var entry:counts.entrySet()){int remaining=entry.getValue();while(remaining>0){var stack=new ItemStack(items.get(entry.getKey()),1);check(!stack.isEmpty(),"Obstruction has no returnable block item "+entry.getKey());int amount=Math.min(remaining,stack.getMaxStackSize());stack.setCount(amount);materialStacks.add(stack);remaining-=amount;}}
                var chests=new ArrayList<ChestBlockEntity>();BlockPos origin=null;
                for(var chunk:ship.getPlot().getLoadedChunks())for(var be:chunk.getChunk().getBlockEntities().values()){if(be instanceof ChestBlockEntity c)chests.add(c);if(be instanceof PhysicsAssemblerBlockEntity)origin=be.getBlockPos().subtract(new BlockPos(20,23,71));}
                check(origin!=null,"Missing primary assembler");chests.sort(Comparator.comparingLong(c->c.getBlockPos().asLong()));
                ChestBlockEntity chest=null;var slots=new ArrayList<Integer>();
                for(var candidate:chests){var empty=new ArrayList<Integer>();for(int i=0;i<candidate.getContainerSize();i++)if(candidate.getItem(i).isEmpty())empty.add(i);if(empty.size()>=materialStacks.size()){chest=candidate;slots=empty;break;}}
                check(chest!=null,"No existing onboard chest with enough empty slots for"+materialStacks.size()+"material stacks");
                var savedInventory=new ArrayList<ItemStack>();for(int i=0;i<chest.getContainerSize();i++)savedInventory.add(chest.getItem(i).copy());
                report.put("materialChest",chest.getBlockPos().toShortString());report.put("chestInventoryBefore",inventory(chest));report.put("removedBlocks",new ArrayList<>(referenceBlocks.asList()));report.put("returnedMaterials",counts);
                var removalOrder=new ArrayList<>(expectedStates.entrySet());
                // Remove attached lanterns before their supports so neighbor updates cannot drop items.
                removalOrder.sort(Comparator.comparingInt((Map.Entry<String,String> e)->e.getValue().contains("minecraft:lantern")?0:e.getValue().contains("symmetric_sail")?1:2));
                for(var entry:removalOrder){var pos=parsePos(entry.getKey());check(level.setBlockAndUpdate(pos,Blocks.AIR.defaultBlockState()),"Could not remove diagnosed obstruction "+pos);check(level.getBlockState(pos).isAir(),"Diagnosed obstruction remains "+pos);}
                for(int i=0;i<materialStacks.size();i++){int slot=slots.get(i);check(chest.getItem(slot).isEmpty(),"Chest slot changed before return");chest.setItem(slot,materialStacks.get(i).copy());}
                chest.setChanged();
                for(int i=0;i<materialStacks.size();i++)check(ItemStack.matches(materialStacks.get(i),chest.getItem(slots.get(i))),"Returned material stack differs in slot"+slots.get(i));
                for(int i=0;i<savedInventory.size();i++)if(!savedInventory.get(i).isEmpty())check(ItemStack.matches(savedInventory.get(i),chest.getItem(i)),"Existing chest inventory changed in slot"+i);
                report.put("chestInventoryAfter",inventory(chest));report.put("materialReturnSlots",slots.subList(0,materialStacks.size()));
                actions.add(Map.of("action","Remove exactly297reference-matchedstaticblocks and return all block materials","materials",counts,"chest",chest.getBlockPos().toShortString(),"stacks",materialStacks.size()));
                var water=new ArrayList<Object>();
                for(int[] pair:new int[][]{{17,53},{17,54},{18,53},{18,54},{22,53},{22,54},{23,53},{23,54}}){var relative=new BlockPos(pair[0],26,pair[1]);var pos=origin.offset(relative);var before=level.getBlockState(pos);check(before.isAir()||before.is(Blocks.WATER),"Original water source cell contains another block"+pos);level.setBlockAndUpdate(pos,Blocks.WATER.defaultBlockState());check(level.getBlockState(pos).getFluidState().isSourceOfType(net.minecraft.world.level.material.Fluids.WATER),"Water source restore failed"+pos);water.add(Map.of("relative",relative.toShortString(),"before",before.toString(),"after",level.getBlockState(pos).toString()));}
                report.put("originalWaterCellsRestoredOnce",water);
                var staff=PhysicsStaffServerHandler.get(level);if(staff.isLocked(ship)){staff.removeLock(ship);actions.add(Map.of("action","Remove native physics-staff fixation"));}
                var throttle=(ThrottleLeverBlockEntity)level.getBlockEntity(origin.offset(23,24,74));check(throttle!=null&&throttle.getBlockState().getValue(ThrottleLeverBlock.INVERTED),"Missing original inverted main throttle");int beforeThrottle=throttle.getState();throttle.setSignal(15);check(throttle.getState()==0,"Main throttle did not produce full drive");actions.add(Map.of("action","Native main throttle.setSignal(15)","relative","23, 24, 74","beforeState",beforeThrottle,"afterState",throttle.getState()));
                var player=s.getPlayerList().getPlayers().getFirst();check(!player.isShiftKeyDown(),"Player is sneaking; do not alter player state");
                for(var relative:MAIN_PROPS){var pos=origin.offset(relative);var bearing=(PropellerBearingBlockEntity)level.getBlockEntity(pos);check(bearing!=null,"Missing main bearing "+relative);if(!bearing.isRunning()){var savedHand=player.getItemInHand(InteractionHand.MAIN_HAND).copy();try{player.setItemInHand(InteractionHand.MAIN_HAND,ItemStack.EMPTY);var hit=new BlockHitResult(Vec3.atCenterOf(pos),Direction.SOUTH,pos,false);var result=bearing.getBlockState().useItemOn(ItemStack.EMPTY,level,player,InteractionHand.MAIN_HAND,hit);check(result.consumesAction(),"Native propeller assembly refused "+relative);actions.add(Map.of("action","Native empty-hand bearing assembly","relative",relative.toShortString(),"result",result.toString()));}finally{player.setItemInHand(InteractionHand.MAIN_HAND,savedHand);}}}
            });
            samples.add(server.computeOnServer(PackagedAirshipReloadRegression::sample));long start=((Number)samples.getFirst().get("serverGameTime")).longValue();int sustained=0;double horizontal=0;
            for(int tick=20;tick<=1000;tick+=20){long target=start+tick;server.waitFor(s->s.overworld().getGameTime()>=target,200);var sample=server.computeOnServer(PackagedAirshipReloadRegression::sample);samples.add(sample);sustained=ready(sample)?sustained+20:0;horizontal=horizontal(samples.getFirst(),sample);report.put("lastSustainedOperatingTicks",sustained);report.put("horizontalDisplacement",horizontal);Files.writeString(path,new GsonBuilder().setPrettyPrinting().create().toJson(report));if(sustained>=100&&horizontal>=3)break;}
            check(sustained>=100,"Both boilers/all4vents/main props did not sustain operation for100ticks");check(horizontal>=3,"Natural horizontal movement below3m: "+horizontal);
            var identityAfter=server.computeOnServer(PackagedAirshipRecoveryRegression::identity);report.put("playerIdentityAfter",identityAfter);check(identityBefore.equals(identityAfter),"Player/host identity, gamemode or held items changed");
            report.put("after",server.computeOnServer(PackagedAirshipReloadRegression::sample));report.put("screenshot",externalScreenshot(test,server.computeOnServer(PackagedAirshipReloadRegression::sample)));server.runOnServer(s->s.saveEverything(false,true,true));report.put("status","passed");checks.add("Actual ship recovered: exact297staticobstructions returned as onboard materials;8originalwatersources restored once; native main propulsion moved>=3m with2boilers/4vents/2props sustained100ticks; no flight force/pose/RPM writes");
        }catch(Throwable failure){report.put("status","failed");report.put("error",failure.toString());throw failure;}
        finally{Files.writeString(path,new GsonBuilder().setPrettyPrinting().create().toJson(report));}
    }
    private static boolean ready(Map<String,Object> sample){
        int boilers=0,vents=0,props=0;
        for(var entry:(List<?>)sample.get("boilers")){var b=(Map<?,?>)entry;if(!"19, 26, 44".equals(b.get("relative"))&&number(b.get("waterSupply"))>0&&number(b.get("activeHeat"))>0)boilers++;}
        for(var entry:(List<?>)sample.get("vents")){var v=(Map<?,?>)entry;if(number(v.get("gasOutput"))>0)vents++;}
        for(var entry:(List<?>)sample.get("propellers")){var p=(Map<?,?>)entry;var r=String.valueOf(p.get("relative"));if((r.equals("6, 19, 78")||r.equals("34, 19, 78"))&&Boolean.TRUE.equals(p.get("active"))&&number(p.get("scaledThrust"))>0)props++;}
        return boilers==2&&vents==4&&props==2;
    }
    private static List<Object> inventory(ChestBlockEntity chest){var list=new ArrayList<Object>();for(int i=0;i<chest.getContainerSize();i++){var stack=chest.getItem(i);list.add(Map.of("slot",i,"item",BuiltInRegistries.ITEM.getKey(stack.getItem()).toString(),"count",stack.getCount(),"components",stack.getComponents().toString()));}return list;}
    public static String externalScreenshot(ClientGameTestContext test,Map<String,Object> sample){
        var position=(List<?>)sample.get("position");double x=number(position.get(0)),y=number(position.get(1)),z=number(position.get(2));
        var restore=test.computeOnClient(client->{
            var before=new CameraRestore(client.getCameraEntity(),client.options.getCameraType());
            var camera=new net.minecraft.world.entity.decoration.ArmorStand(client.level,x+80,y+40,z+60);
            var delta=new Vec3(x,y,z).subtract(camera.getEyePosition());
            camera.setYRot((float)Math.toDegrees(Math.atan2(-delta.x,delta.z)));
            camera.setXRot((float)-Math.toDegrees(Math.atan2(delta.y,Math.hypot(delta.x,delta.z))));
            camera.setYHeadRot(camera.getYRot());camera.setYBodyRot(camera.getYRot());
            camera.yRotO=camera.getYRot();camera.xRotO=camera.getXRot();
            camera.yHeadRotO=camera.getYRot();camera.yBodyRotO=camera.getYRot();
            client.options.setCameraType(net.minecraft.client.CameraType.FIRST_PERSON);client.setCameraEntity(camera);
            return before;
        });
        try{test.waitTicks(2);return test.takeScreenshot("airship-recovered-native-flight").toString();}
        finally{test.runOnClient(client->{client.setCameraEntity(restore.entity());client.options.setCameraType(restore.type());});}
    }
    private record CameraRestore(net.minecraft.world.entity.Entity entity,net.minecraft.client.CameraType type){}
    private static Map<String,Object> identity(MinecraftServer s){var p=s.getPlayerList().getPlayers().getFirst();var host=s.getWorldData().getSinglePlayerUUID();return Map.of("uuid",p.getUUID().toString(),"hostUUID",host==null?"null":host.toString(),"levelName",s.getWorldData().getLevelName(),"creative",p.isCreative(),"spectator",p.isSpectator(),"mainHand",p.getItemInHand(InteractionHand.MAIN_HAND).toString(),"offHand",p.getItemInHand(InteractionHand.OFF_HAND).toString());}
    private static BlockPos parsePos(String text){var p=text.split(",\\s*");check(p.length==3,"Bad reference position "+text);return new BlockPos(Integer.parseInt(p[0]),Integer.parseInt(p[1]),Integer.parseInt(p[2]));}
    private static double horizontal(Map<String,Object> first,Map<String,Object> last){var a=(List<?>)first.get("position");var b=(List<?>)last.get("position");return Math.hypot(number(b.get(0))-number(a.get(0)),number(b.get(2))-number(a.get(2)));}
    private static double number(Object value){return ((Number)value).doubleValue();}
    private static void check(boolean value,String message){if(!value)throw new AssertionError(message);}
    private PackagedAirshipRecoveryRegression(){}
}
