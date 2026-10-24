import com.google.gson.JsonParser;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.content.kinetics.motor.CreativeMotorBlock;
import com.simibubi.create.content.kinetics.motor.CreativeMotorBlockEntity;
import com.simibubi.create.foundation.utility.LoadedLevelAccess;
import dev.simulated_team.simulated.content.blocks.torsion_spring.TorsionSpringBlock;
import dev.simulated_team.simulated.content.blocks.torsion_spring.TorsionSpringBlockEntity;
import dev.simulated_team.simulated.index.SimBlocks;
import dev.simulated_team.simulated.util.extra_kinetics.ExtraBlockPos;
import dev.simulated_team.simulated.util.extra_kinetics.NativeKineticChannelState;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Bounded real spring turns, redstone lock/release, externally driven output and native restart. */
public final class DirectionTorsionMatrix {
    private static final String GROUP = "torsion";
    private static final int WAIT = 28;
    private static final List<Cell> CELLS = cells();
    private static final Map<Integer, Boolean> PULSES = new java.util.HashMap<>();
    private static int stage;
    private static long changedAt;
    private static boolean complete;
    private static String active = "setup";

    private record Cell(int index, Direction facing, boolean outputDriven, int rpm, boolean powered) {
        BlockPos pos() { return DirectionMatrixSupport.base(GROUP, index); }
        Direction sourceFace() { return outputDriven ? facing : facing.getOpposite(); }
        Direction powerFace() { return facing.getAxis() == Direction.Axis.Y ? Direction.EAST : Direction.UP; }
        BlockPos motor() { return pos().relative(sourceFace()); }
        BlockPos shaft() { return pos().relative(sourceFace().getOpposite()); }
        String id() { return facing.getSerializedName() + "/source-" + (outputDriven ? "stored" : "physical")
                + "/rpm-" + rpm + "/cold-powered-" + powered; }
    }

    private static List<Cell> cells() {
        List<Cell> cells = new ArrayList<>();
        for (Direction facing : Direction.values()) for (boolean output : new boolean[]{false,true})
            for (int rpm : new int[]{64,0,-64}) for (boolean power : new boolean[]{false,true})
                cells.add(new Cell(cells.size(),facing,output,rpm,power));
        return List.copyOf(cells);
    }

    public static boolean tick(MinecraftServer server, List<String> checks) {
        if (complete) return true;
        try {
            ServerLevel level = DirectionMatrixSupport.level(server);
            if ("restore".equals(DirectionMatrixSupport.phase())) return restore(level,checks);
            if (stage == 0) {
                for (Cell cell : CELLS) if (!done("snapshot/",cell)) build(level,cell);
                changedAt=level.getGameTime(); stage=1; return false;
            }
            if (stage==1) {
                if (elapsed(level)<2) return false;
                for(Cell cell:CELLS) if(!done("snapshot/",cell)) motor(level,cell,cell.rpm);
                resetPulse(); changedAt=level.getGameTime(); stage=2; return false;
            }
            if (stage==2) {
                observe(level,false);
                if(elapsed(level)<WAIT) return false;
                for(Cell cell:CELLS) if(!done("cold/",cell)) {
                    verifyOwners(level,cell);
                    check(spring(level,cell).getBlockState().getValue(TorsionSpringBlock.POWERED)==cell.powered,cell.id()+": actual cold redstone flag");
                    if(cell.outputDriven) external(level,cell,cell.rpm);
                    else {
                        primary(level,cell,cell.rpm);
                        near(spring(level,cell).getAngle(),Math.signum(cell.rpm)*90,cell.id()+": charged angle");
                        near(spring(level,cell).getExtraKinetics().getSpeed(),0,cell.id()+": finite turn must stop");
                        near(shaft(level,cell).getSpeed(),0,cell.id()+": output must stop after charge");
                        if(cell.rpm!=0) check(PULSES.getOrDefault(cell.index,false),cell.id()+": real winding pulse absent");
                    }
                    record(level,cell,"cold/",checks);
                }
                for(Cell cell:CELLS) if(!done("snapshot/",cell)) motor(level,cell,-cell.rpm);
                resetPulse(); changedAt=level.getGameTime(); stage=3; return false;
            }
            if(stage==3) {
                observe(level,true);
                if(elapsed(level)<WAIT) return false;
                for(Cell cell:CELLS) if(!done("reverse/",cell)) {
                    if(cell.outputDriven) external(level,cell,-cell.rpm);
                    else {
                        primary(level,cell,-cell.rpm);
                        near(spring(level,cell).getAngle(),-Math.signum(cell.rpm)*90,cell.id()+": reversed target angle");
                        near(shaft(level,cell).getSpeed(),0,cell.id()+": reversed finite turn must stop");
                        if(cell.rpm!=0)check(PULSES.getOrDefault(cell.index,false),cell.id()+": real reverse pulse absent");
                    }
                    record(level,cell,"reverse/",checks);
                }
                for(Cell cell:CELLS) if(!cell.outputDriven&&!done("snapshot/",cell)) { power(level,cell,true);motor(level,cell,0); }
                changedAt=level.getGameTime();stage=4;return false;
            }
            if(stage==4) {
                if(elapsed(level)<WAIT)return false;
                for(Cell cell:CELLS) if(!cell.outputDriven&&!done("locked/",cell)) {
                    primary(level,cell,0);near(spring(level,cell).getAngle(),-Math.signum(cell.rpm)*90,cell.id()+": powered hold");
                    near(shaft(level,cell).getSpeed(),0,cell.id()+": locked output");
                    record(level,cell,"locked/",checks);
                }
                for(Cell cell:CELLS) if(!cell.outputDriven&&!done("snapshot/",cell)) power(level,cell,false);
                resetPulse();changedAt=level.getGameTime();stage=5;return false;
            }
            if(stage==5) {
                observe(level,false);
                if(elapsed(level)<WAIT)return false;
                for(Cell cell:CELLS) if(!cell.outputDriven&&!done("released/",cell)) {
                    primary(level,cell,0);near(spring(level,cell).getAngle(),0,cell.id()+": genuine release must unwind");
                    near(shaft(level,cell).getSpeed(),0,cell.id()+": released finite turn must stop");
                    if(cell.rpm!=0)check(PULSES.getOrDefault(cell.index,false),cell.id()+": real unwind pulse absent");
                    record(level,cell,"released/",checks);
                }
                for(Cell cell:CELLS) if(!done("snapshot/",cell)) { power(level,cell,true);motor(level,cell,cell.rpm); }
                changedAt=level.getGameTime();stage=6;return false;
            }
            if(stage==6) {
                if(elapsed(level)<WAIT)return false;
                for(Cell cell:CELLS) if(!cell.outputDriven&&!done("snapshot/",cell)) motor(level,cell,0);
                changedAt=level.getGameTime();stage=7;return false;
            }
            if(elapsed(level)<WAIT)return false;
            for(Cell cell:CELLS)if(!done("snapshot/",cell)) {
                snapshot(level,cell,false);record(level,cell,"snapshot/",checks);
            }
            complete=true;return true;
        }catch(Throwable failure){
            DirectionMatrixSupport.fail(GROUP,active,failure);
            throw failure instanceof RuntimeException exception?exception:new RuntimeException(failure);
        }
    }

    private static boolean restore(ServerLevel level,List<String>checks)throws Exception {
        if(stage==0){for(Cell cell:CELLS)if(!done("restore/",cell))load(level,cell.pos());changedAt=level.getGameTime();stage=1;return false;}
        if(elapsed(level)<WAIT)return false;
        for(Cell cell:CELLS)if(!done("restore/",cell)) {
            active="restore/"+cell.id();snapshot(level,cell,true);record(level,cell,"restore/",checks);
        }
        complete=true;return true;
    }

    private static void snapshot(ServerLevel level,Cell cell,boolean restore)throws Exception {
        verifyOwners(level,cell);check(level.getBlockState(cell.pos()).getValue(TorsionSpringBlock.POWERED),cell.id()+": persistent lock flag");
        if(cell.outputDriven)external(level,cell,cell.rpm);
        else {
            primary(level,cell,0);near(spring(level,cell).getAngle(),Math.signum(cell.rpm)*90,cell.id()+": persistent angle");
            near(shaft(level,cell).getSpeed(),0,cell.id()+": persistent locked output");
        }
        if(restore){
            var root=JsonParser.parseString(Files.readString(DirectionMatrixSupport.journalPath(GROUP))).getAsJsonObject();
            var saved=root.getAsJsonObject("cases").getAsJsonObject("initial/snapshot/"+cell.id()).getAsJsonObject("evidence");
            check(saved.get("secondaryNetworkId").getAsLong()==networkId(level,cell),cell.id()+": native saved secondary source ID changed");
            near(spring(level,cell).getAngle(),saved.get("angle").getAsFloat(),cell.id()+": native saved angle payload");
        }
    }

    private static void observe(ServerLevel level,boolean reversed){
        for(Cell cell:CELLS)if(!cell.outputDriven&&cell.rpm!=0&&!done("snapshot/",cell)){
            TorsionSpringBlockEntity spring=spring(level,cell);float speed=spring.getExtraKinetics().getSpeed();
            int expectedSign=(reversed?-1:1)*Integer.signum(cell.rpm);
            if(Math.abs(speed)>1&&Math.signum(speed)==expectedSign){
                near(shaft(level,cell).getSpeed(),speed,cell.id()+": actual pulse must reach real output shaft");
                check(shaft(level,cell).getSource() instanceof ExtraBlockPos,cell.id()+": pulse source must retain stored channel");
                check(shaft(level,cell).getGeneratingSource() instanceof ExtraBlockPos,cell.id()+": pulse generator must retain stored channel");
                PULSES.put(cell.index,true);
            }
        }
    }

    private static void primary(ServerLevel level,Cell cell,int rpm){
        near(spring(level,cell).getSpeed(),rpm,cell.id()+": actual physical input RPM");
        if(rpm!=0)check(cell.motor().equals(spring(level,cell).getSource())&&spring(level,cell).getKineticSourceFace()==cell.sourceFace(),cell.id()+": physical input provenance");
    }
    private static void external(ServerLevel level,Cell cell,int rpm){
        near(spring(level,cell).getSpeed(),0,cell.id()+": stored source must not drive physical owner");
        near(spring(level,cell).getExtraKinetics().getSpeed(),rpm,cell.id()+": actual externally driven stored owner RPM");
        near(shaft(level,cell).getSpeed(),0,cell.id()+": stored source must not drive opposite shaft");
        if(rpm!=0)check(cell.motor().equals(spring(level,cell).getExtraKinetics().getSource())
                &&spring(level,cell).getExtraKinetics().getKineticSourceFace()==cell.sourceFace(),cell.id()+": stored input provenance");
    }
    private static void verifyOwners(ServerLevel level,Cell cell){
        active="owners/"+cell.id();
        check(level.getBlockState(cell.pos()).is(SimBlocks.TORSION_SPRING),cell.id()+": spring destroyed");
        check(level.getBlockState(cell.motor()).is(AllBlocks.CREATIVE_MOTOR),cell.id()+": motor destroyed");
        check(level.getBlockState(cell.shaft()).is(AllBlocks.SHAFT),cell.id()+": shaft destroyed");
        check(level.getBlockState(cell.pos()).getValue(TorsionSpringBlock.FACING)==cell.facing,cell.id()+": orientation changed");
        var parent=spring(level,cell);var child=parent.getExtraKinetics();
        check(child!=parent&&child.getBlockPos() instanceof ExtraBlockPos,cell.id()+": genuine stored owner missing");
        check(LoadedLevelAccess.blockEntity(level,new ExtraBlockPos(cell.pos()))==child,cell.id()+": actual stored lookup collapsed");
        check(LoadedLevelAccess.blockEntity(level,cell.pos())==parent,cell.id()+": actual physical lookup collapsed");
        check(!parent.isConflictDestructionPending()&&!child.isConflictDestructionPending()&&!shaft(level,cell).isConflictDestructionPending(),cell.id()+": conflict destruction pending");
        check(Float.isFinite(parent.getAngle()),cell.id()+": angle must remain finite");
    }
    private static void record(ServerLevel level,Cell cell,String prefix,List<String>checks){
        active=prefix+cell.id();var parent=spring(level,cell);Map<String,Object>e=new LinkedHashMap<>();
        e.put("fixture",List.of(cell.pos().getX(),cell.pos().getY(),cell.pos().getZ()));e.put("facing",cell.facing.getSerializedName());
        e.put("sourceOwner",cell.outputDriven?"stored":"physical");e.put("sourceFace",cell.sourceFace().getSerializedName());
        e.put("coldRpm",cell.rpm);e.put("coldPower",cell.powered);e.put("actualPowered",parent.getBlockState().getValue(TorsionSpringBlock.POWERED));
        e.put("physicalRpm",parent.getSpeed());e.put("storedRpm",parent.getExtraKinetics().getSpeed());e.put("shaftRpm",shaft(level,cell).getSpeed());
        e.put("angle",parent.getAngle());e.put("observedBoundedPulse",PULSES.getOrDefault(cell.index,false));e.put("secondaryNetworkId",networkId(level,cell));
        DirectionMatrixSupport.pass(GROUP,active,e);checks.add(GROUP+"/"+active);
    }
    private static long networkId(ServerLevel level,Cell cell){return((NativeKineticChannelState)spring(level,cell).getExtraKinetics()).simulated$virtualNetworkId();}
    private static TorsionSpringBlockEntity spring(ServerLevel level,Cell cell){return(TorsionSpringBlockEntity)level.getBlockEntity(cell.pos());}
    private static KineticBlockEntity shaft(ServerLevel level,Cell cell){return(KineticBlockEntity)level.getBlockEntity(cell.shaft());}
    private static void build(ServerLevel level,Cell cell){
        active="cold/"+cell.id();load(level,cell.pos());
        for(Direction face:Direction.values())level.setBlockAndUpdate(cell.pos().relative(face),Blocks.AIR.defaultBlockState());
        level.setBlockAndUpdate(cell.pos(),Blocks.AIR.defaultBlockState());power(level,cell,cell.powered);
        level.setBlockAndUpdate(cell.pos(),SimBlocks.TORSION_SPRING.defaultBlockState().setValue(TorsionSpringBlock.FACING,cell.facing).setValue(TorsionSpringBlock.POWERED,cell.powered));
        level.setBlockAndUpdate(cell.shaft(),AllBlocks.SHAFT.defaultBlockState().setValue(BlockStateProperties.AXIS,cell.facing.getAxis()));
        level.setBlockAndUpdate(cell.motor(),AllBlocks.CREATIVE_MOTOR.defaultBlockState().setValue(CreativeMotorBlock.FACING,cell.sourceFace().getOpposite()));
        // Set the genuine motor control before its first tick; the default 16 RPM must not precharge a zero-input case.
        motor(level,cell,0);
    }
    private static void power(ServerLevel level,Cell cell,boolean on){level.setBlockAndUpdate(cell.pos().relative(cell.powerFace()),(on?Blocks.REDSTONE_BLOCK:Blocks.AIR).defaultBlockState());}
    private static void motor(ServerLevel level,Cell cell,int rpm){((CreativeMotorBlockEntity)level.getBlockEntity(cell.motor())).generatedSpeed.setValue(rpm*cell.sourceFace().getOpposite().getAxisDirection().getStep());}
    private static void load(ServerLevel level,BlockPos pos){level.setChunkForced(pos.getX()>>4,pos.getZ()>>4,true);level.getChunk(pos);}
    private static boolean done(String prefix,Cell cell){return DirectionMatrixSupport.done(GROUP,prefix+cell.id());}
    private static long elapsed(ServerLevel level){return level.getGameTime()-changedAt;}
    private static void resetPulse(){PULSES.clear();}
    private static void check(boolean value,String message){DirectionMatrixSupport.check(value,message);}
    private static void near(float actual,float expected,String message){check(Float.isFinite(actual)&&Math.abs(actual-expected)<0.05f,message+": expected "+expected+", got "+actual);}
    private DirectionTorsionMatrix(){}
}
