import com.google.gson.GsonBuilder;
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
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Starts only the original paired main propellers through their native controls. */
public final class PackagedAirshipPropulsionRegression {
    private static final BlockPos CONTROL=new BlockPos(23,24,74);
    private static final List<BlockPos> MAIN_PROPS=List.of(new BlockPos(6,19,78),new BlockPos(34,19,78));

    public static void run(ClientGameTestContext test,TestSingleplayerContext world,List<String> checks) throws Exception {
        var report=new LinkedHashMap<String,Object>();
        var actions=new ArrayList<Object>();
        var samples=new ArrayList<Map<String,Object>>();
        report.put("scope","Original main propulsion only: remove an existing native physics-staff fixation if present, inverted cogwheel-channel throttle native setSignal(15), native empty-hand bearing useItemOn if not assembled; no fuel, boiler, gas, other controls, player teleport, force, pose or RPM writes. Fresh resumed save.");
        report.put("actions",actions);report.put("samples",samples);
        var path=Path.of(System.getProperty("port.smoke.report")).resolveSibling("airship-propulsion.json");
        try {
            var server=world.getServer();
            report.put("before",server.computeOnServer(PackagedAirshipReloadRegression::sample));
            var identityBefore=server.computeOnServer(PackagedAirshipPropulsionRegression::identity);
            report.put("playerIdentityBefore",identityBefore);
            server.runOnServer(s->{
                var all=SubLevelContainer.getContainer(s.overworld()).getAllSubLevels();
                check(all.size()==1,"Expected one ship");
                var ship=(ServerSubLevel)all.getFirst();
                var staff=PhysicsStaffServerHandler.get(s.overworld());
                if(staff.isLocked(ship)){
                    staff.removeLock(ship);
                    check(!staff.isLocked(ship),"Native staff fixation did not release");
                    actions.add(Map.of("action","PhysicsStaffServerHandler.removeLock(ship)","beforeStaffLocked",true,"afterStaffLocked",false));
                }
                BlockPos origin=null;
                for(var chunk:ship.getPlot().getLoadedChunks())for(var be:chunk.getChunk().getBlockEntities().values())
                    if(be instanceof PhysicsAssemblerBlockEntity){origin=be.getBlockPos().subtract(new BlockPos(20,23,71));break;}
                check(origin!=null,"Missing ship assembler");
                var throttle=(ThrottleLeverBlockEntity)s.overworld().getBlockEntity(origin.offset(CONTROL));
                check(throttle!=null&&throttle.getBlockState().getValue(ThrottleLeverBlock.INVERTED),"Expected original inverted main propulsion throttle");
                int oldState=throttle.getState();throttle.setSignal(15);
                check(throttle.getState()==0,"Native inverted throttle did not produce full-drive state0");
                actions.add(Map.of("relative",CONTROL.toShortString(),"action","ThrottleLeverBlockEntity.setSignal(15)","beforeState",oldState,"afterState",throttle.getState(),"channel","create:cogwheel / create:cogwheel"));
                var player=s.getPlayerList().getPlayers().getFirst();
                check(!player.isShiftKeyDown(),"Player is sneaking; preserve player and do not alter shift state");
                for(var relative:MAIN_PROPS){
                    var pos=origin.offset(relative);
                    var bearing=(PropellerBearingBlockEntity)s.overworld().getBlockEntity(pos);
                    check(bearing!=null,"Missing main bearing "+relative);
                    if(!bearing.isRunning()){
                        var savedHand=player.getItemInHand(InteractionHand.MAIN_HAND).copy();
                        try{
                            player.setItemInHand(InteractionHand.MAIN_HAND,ItemStack.EMPTY);
                            var hit=new BlockHitResult(Vec3.atCenterOf(pos),Direction.SOUTH,pos,false);
                            var result=bearing.getBlockState().useItemOn(ItemStack.EMPTY,s.overworld(),player,InteractionHand.MAIN_HAND,hit);
                            actions.add(Map.of("relative",relative.toShortString(),"action","BlockState.useItemOn(empty main hand)","result",result.toString()));
                            check(result.consumesAction(),"Main bearing did not accept native empty-hand assembly "+relative+": "+result);
                        } finally {player.setItemInHand(InteractionHand.MAIN_HAND,savedHand);}
                    }else actions.add(Map.of("relative",relative.toShortString(),"action","already running; native interaction skipped"));
                }
            });
            samples.add(server.computeOnServer(PackagedAirshipReloadRegression::sample));
            long start=((Number)samples.getFirst().get("serverGameTime")).longValue();
            int sustained=0;
            for(int tick=10;tick<=200;tick+=10){
                long target=start+tick;
                server.waitFor(s->s.overworld().getGameTime()>=target,200);
                var sample=server.computeOnServer(PackagedAirshipReloadRegression::sample);samples.add(sample);
                if(mainPropsActive(sample))sustained+=10;else sustained=0;
                report.put("lastSustainedPropulsionTicks",sustained);
                Files.writeString(path,new GsonBuilder().setPrettyPrinting().create().toJson(report));
            }
            var first=(List<?>)samples.getFirst().get("position");var last=(List<?>)samples.getLast().get("position");
            double dx=number(last.get(0))-number(first.get(0)),dy=number(last.get(1))-number(first.get(1)),dz=number(last.get(2))-number(first.get(2));
            double horizontal=Math.hypot(dx,dz);
            report.put("displacement",List.of(dx,dy,dz));report.put("horizontalDisplacement",horizontal);
            check(sustained>=100,"Both original main props did not sustain active nonzero thrust for last100ticks");
            check(horizontal>=3,"Original main propulsion did not move ship horizontally3m: "+horizontal);
            var identityAfter=server.computeOnServer(PackagedAirshipPropulsionRegression::identity);
            report.put("playerIdentityAfter",identityAfter);
            check(identityBefore.equals(identityAfter),"Player/host identity, gamemode or held inventory changed");
            report.put("screenshot",test.takeScreenshot("airship-original-main-propulsion").toString());
            server.runOnServer(s->s.saveEverything(false,true,true));
            report.put("status","passed");
            checks.add("Original paired main propellers: only native throttle and empty-hand assembly; final100ticks both active with nonzero thrust and >=3m horizontal movement, save written");
        } catch(Throwable failure){report.put("status","failed");report.put("error",failure.toString());throw failure;}
        finally{Files.writeString(path,new GsonBuilder().setPrettyPrinting().create().toJson(report));}
    }
    private static boolean mainPropsActive(Map<String,Object> sample){
        int active=0;
        for(var entry:(List<?>)sample.get("propellers")){
            var prop=(Map<?,?>)entry;String relative=String.valueOf(prop.get("relative"));
            if((relative.equals("6, 19, 78")||relative.equals("34, 19, 78"))&&Boolean.TRUE.equals(prop.get("running"))&&Boolean.TRUE.equals(prop.get("active"))&&Math.abs(number(prop.get("thrust")))>1e-6)active++;
        }
        return active==2;
    }
    private static double number(Object value){return ((Number)value).doubleValue();}
    private static Map<String,Object> identity(MinecraftServer server){
        var player=server.getPlayerList().getPlayers().getFirst();var host=server.getWorldData().getSinglePlayerUUID();
        return Map.of("uuid",player.getUUID().toString(),"hostUUID",host==null?"null":host.toString(),"levelName",server.getWorldData().getLevelName(),"creative",player.isCreative(),"spectator",player.isSpectator(),"mainHand",player.getItemInHand(InteractionHand.MAIN_HAND).toString(),"offHand",player.getItemInHand(InteractionHand.OFF_HAND).toString());
    }
    private static void check(boolean condition,String message){if(!condition)throw new AssertionError(message);}
    private PackagedAirshipPropulsionRegression(){}
}
