import com.google.gson.JsonParser;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.content.contraptions.glue.SuperGlueEntity;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.content.kinetics.motor.CreativeMotorBlock;
import com.simibubi.create.content.kinetics.motor.CreativeMotorBlockEntity;
import com.simibubi.create.foundation.utility.LoadedEntityAccess;
import dev.ryanhcode.sable.api.physics.constraint.RotaryConstraintHandle;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.api.physics.handle.RigidBodyHandle;
import dev.ryanhcode.sable.api.sublevel.SubLevelContainer;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import dev.simulated_team.simulated.content.blocks.swivel_bearing.SwivelBearingBlock;
import dev.simulated_team.simulated.content.blocks.swivel_bearing.SwivelBearingBlockEntity;
import dev.simulated_team.simulated.content.blocks.swivel_bearing.link_block.SwivelBearingPlateBlock;
import dev.simulated_team.simulated.content.blocks.swivel_bearing.link_block.SwivelBearingPlateBlockEntity;
import dev.simulated_team.simulated.index.SimBlocks;
import dev.simulated_team.simulated.util.extra_kinetics.NativeKineticChannelState;
import dev.simulated_team.simulated.util.extra_kinetics.NativeKineticPositions;
import dev.simulated_team.simulated.util.SimAssemblyHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.AABB;
import org.joml.Vector3d;
import java.nio.file.Files;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Real world/plot swivel links, cog mesh routes and Rapier joints; separate native restart. */
public final class DirectionSwivelMatrix {
    private static final String GROUP="swivel";
    private static final List<Rig> RIGS=rigs();
    private static final int[] PRIMARY={64,0,-64},SECONDARY={4,0,-4};
    private static final Map<Integer,Double> ANGLES=new java.util.HashMap<>();
    private static final Field HANDLE=field("handle");
    private static final Field PLATE_PARENT=plateField("parent");
    private static final Map<Integer,Relink> OLD_LINKS=new java.util.HashMap<>();
    private static int stage,combination;
    private static long changedAt;
    private static boolean complete;
    private static String active="setup";
    private record Relink(java.util.UUID id,BlockPos plate,RotaryConstraintHandle handle,ServerSubLevel sub){}

    private record Rig(int index,Direction facing,int route,Direction mesh,int motorSide){
        BlockPos pos(){return DirectionMatrixSupport.base(GROUP,index);}
        String id(){return facing.getSerializedName()+"/"+(route==0?"body-back":route==1?"plate-front":"cog-"+mesh.getSerializedName()+"-motor-"+motorSide);}
        boolean cog(){return route>=2;}
        Direction powerFace(){for(Direction face:Direction.values())if(face.getAxis()!=facing.getAxis()&&face!=mesh)return face;throw new AssertionError();}
        BlockPos cogPos(){return pos().relative(mesh);}
        BlockPos cogMotor(){return cogPos().relative(motorSide>0?facing:facing.getOpposite());}
    }
    private static List<Rig> rigs(){
        List<Rig>out=new ArrayList<>();
        for(Direction facing:Direction.values()){
            out.add(new Rig(out.size(),facing,0,null,0));out.add(new Rig(out.size(),facing,1,null,0));
            int route=2;for(Direction mesh:Direction.values())if(mesh.getAxis()!=facing.getAxis())for(int side:new int[]{-1,1})out.add(new Rig(out.size(),facing,route++,mesh,side));
        }return List.copyOf(out);
    }
    public static boolean tick(MinecraftServer server,List<String>checks){
        if(complete)return true;
        try{
            ServerLevel level=DirectionMatrixSupport.level(server);
            if(Boolean.getBoolean("port.matrix.swivelReuseDiagnostic"))return reuseDiagnostic(level,checks);
            if("restore".equals(DirectionMatrixSupport.phase()))return restore(level,checks);
            if(stage==0){
                for(Rig rig:RIGS){
                    load(level,rig.pos());
                    if(level.getBlockEntity(rig.pos()) instanceof SwivelBearingBlockEntity damaged&&damaged.isAssembled()&&damaged.getPlatePos()==null&&damaged.getSubLevelID()!=null){
                        var old=(ServerSubLevel)SubLevelContainer.getContainer(level).getSubLevel(damaged.getSubLevelID());
                        check(old!=null,"damaged native assembly has no recoverable paid plot");var oldId=old.getUniqueId();var anchor=old.getPlot().getCenterBlock().relative(rig.facing.getOpposite());
                        check(SimAssemblyHelper.disassembleSubLevel(level,old,anchor,rig.pos(),Rotation.NONE,true),"native recovery of paid missing-plate plot refused");
                        check(old.isRemoved()||SubLevelContainer.getContainer(level).getSubLevel(oldId)==null,"native missing-plate recovery left retired plot live");
                        damaged.disassemble();check(!damaged.isAssembled()&&damaged.getPlatePos()==null&&damaged.getSubLevelID()==null,"native recovery left old assembly links");
                        check(level.getBlockState(rig.pos().relative(rig.facing)).is(AllBlocks.SHAFT),"native recovery lost paid rotor shaft");
                        Files.writeString(DirectionMatrixSupport.journalPath(GROUP).resolveSibling("recovered-missing-plate-"+rig.facing.getSerializedName()+"-"+rig.route+".json"),new com.google.gson.GsonBuilder().setPrettyPrinting().create().toJson(Map.of("oldSubLevelUuid",oldId.toString(),"nativePlotRemoved",true,"paidShaftRestored",true,"fixture",coords(rig.pos()))));
                    }
                    if(!(level.getBlockEntity(rig.pos()) instanceof SwivelBearingBlockEntity existing)||!existing.isAssembled()){
                        if(level.getBlockEntity(rig.pos()) instanceof SwivelBearingBlockEntity existing&&!existing.isAssembled()&&level.getBlockState(rig.pos().relative(rig.facing)).is(AllBlocks.SHAFT)){}
                        else build(level,rig);
                    }
                    else if(rig.route==1&&!(level.getBlockEntity(existing.getPlatePos().relative(rig.facing,2)) instanceof CreativeMotorBlockEntity))existing.disassemble();
                }changedAt=level.getGameTime();stage=1;return false;
            }
            if(stage==1){
                if(elapsed(level)<4)return false;
                for(Rig rig:RIGS)if(rig.route==1&&!body(level,rig).isAssembled()){
                    if(!LoadedEntityAccess.isReady(level,rig.pos().relative(rig.facing))){check(elapsed(level)<100,"actual glue entity section never became ready");return false;}
                }
                for(Rig rig:RIGS){
                    active="assembly/"+rig.id();
                    if(!body(level,rig).isAssembled()){
                        if(rig.route==1){
                            BlockPos front=rig.pos().relative(rig.facing),motor=front.relative(rig.facing);
                            check(level.addFreshEntity(new SuperGlueEntity(level,AABB.encapsulatingFullBlocks(front,motor))),active+": actual glue entity rejected");
                            check(SuperGlueEntity.isGlued(level,front,rig.facing,new java.util.HashSet<>()),active+": actual glue not discoverable");
                        }
                        body(level,rig).assemble();
                    }
                }changedAt=level.getGameTime();stage=2;return false;
            }
            if(stage==2){if(elapsed(level)<12)return false;for(Rig rig:RIGS)joint(level,rig);stage=3;}
            if(stage==3){
                if(combination>=18){for(Rig rig:RIGS){motors(level,rig,0,0);power(level,rig,0);}changedAt=level.getGameTime();stage=6;return false;}
                int p=PRIMARY[combination/6],s=SECONDARY[combination/2%3],signal=combination%2*15;
                for(Rig rig:RIGS)if(eligible(rig,s)&&!done(caseId(rig,p,s,signal))){active=caseId(rig,p,s,signal);motors(level,rig,p,s);power(level,rig,signal);}
                changedAt=level.getGameTime();stage=4;return false;
            }
            int p=PRIMARY[Math.min(combination,17)/6],s=SECONDARY[Math.min(combination,17)/2%3],signal=combination%2*15;
            if(stage==4){if(elapsed(level)<8)return false;for(Rig rig:RIGS)if(eligible(rig,s)&&!done(caseId(rig,p,s,signal)))ANGLES.put(rig.index,body(level,rig).getTargetAngleDegrees());changedAt=level.getGameTime();stage=5;return false;}
            if(stage==5){
                if(elapsed(level)<4)return false;
                for(Rig rig:RIGS)if(eligible(rig,s)&&!done(caseId(rig,p,s,signal))){
                    active=caseId(rig,p,s,signal);verify(level,rig,p,s,signal);
                    double expected=rig.facing.getAxisDirection().getStep()*(-s)*.3*elapsed(level);
                    double actual=wrap(body(level,rig).getTargetAngleDegrees()-ANGLES.get(rig.index));
                    check(Math.abs(wrap(actual-expected))<.03,active+": actual servo target delta expected "+expected+", got "+actual);
                    record(level,rig,active,checks,Map.of("primaryRpm",p,"secondaryMotorRpm",s,"signal",signal,"targetDelta",actual));
                }combination++;stage=3;return false;
            }
            if(stage==6){
                if(elapsed(level)<12)return false;
                for(Rig rig:RIGS)if(rig.route==0&&!done("unrelated-plate/"+rig.id())){
                    Direction side=rig.powerFace().getOpposite();BlockPos other=rig.pos().relative(side);
                    level.setBlockAndUpdate(other,SimBlocks.SWIVEL_BEARING_LINK_BLOCK.defaultBlockState().setValue(SwivelBearingPlateBlock.FACING,side));
                    motors(level,rig,64,0);
                }changedAt=level.getGameTime();stage=7;return false;
            }
            if(stage==7){
                if(elapsed(level)<12)return false;
                for(Rig rig:RIGS)if(rig.route==0&&!done("unrelated-plate/"+rig.id())){
                    active="unrelated-plate/"+rig.id();BlockPos other=rig.pos().relative(rig.powerFace().getOpposite());
                    near(((KineticBlockEntity)level.getBlockEntity(other)).getSpeed(),0,active+": another unowned adjacent plate must stay unpowered");
                    verify(level,rig,64,0,0);record(level,rig,active,checks,Map.of("unrelatedPlate",coords(other)));
                    ((SwivelBearingPlateBlockEntity)level.getBlockEntity(other)).setParent(body(level,rig));
                }changedAt=level.getGameTime();stage=8;return false;
            }
            if(stage==8){
                if(elapsed(level)<8)return false;
                for(Rig rig:RIGS)if(rig.route==0&&!done("stale-parent-plate/"+rig.id())){
                    active="stale-parent-plate/"+rig.id();var other=rig.pos().relative(rig.powerFace().getOpposite());
                    near(((KineticBlockEntity)level.getBlockEntity(other)).getSpeed(),0,active+": stale reciprocal ParentPos must not create an unowned kinetic connection");
                    verify(level,rig,64,0,0);record(level,rig,active,checks,Map.of("stalePlate",coords(other)));
                }
                for(Rig rig:RIGS)if(rig.route==0&&!done("stale-parent-driven/"+rig.id())){
                    var side=rig.powerFace().getOpposite();var other=rig.pos().relative(side);
                    if(!(level.getBlockEntity(other)instanceof SwivelBearingPlateBlockEntity))level.setBlockAndUpdate(other,SimBlocks.SWIVEL_BEARING_LINK_BLOCK.defaultBlockState().setValue(SwivelBearingPlateBlock.FACING,side));
                    ((SwivelBearingPlateBlockEntity)level.getBlockEntity(other)).setParent(body(level,rig));
                    placeMotor(level,other.relative(side),side.getOpposite());setMotor(level,other.relative(side),64);motors(level,rig,0,0);
                }changedAt=level.getGameTime();stage=82;return false;
            }
            if(stage==82){
                if(elapsed(level)<12)return false;
                for(Rig rig:RIGS)if(rig.route==0&&!done("stale-parent-driven/"+rig.id())){
                    active="stale-parent-driven/"+rig.id();var side=rig.powerFace().getOpposite();var other=rig.pos().relative(side);
                    verify(level,rig,0,0,0);near(((KineticBlockEntity)level.getBlockEntity(other)).getSpeed(),64,active+": independently driven stale plate must retain own motor RPM");
                    record(level,rig,active,checks,Map.of("stalePlateRpm",64));
                    ((SwivelBearingPlateBlockEntity)level.getBlockEntity(other)).beforeAssembly();
                    level.setBlockAndUpdate(other,Blocks.AIR.defaultBlockState());level.setBlockAndUpdate(other.relative(side),Blocks.AIR.defaultBlockState());motors(level,rig,0,0);
                }changedAt=level.getGameTime();stage=81;return false;
            }
            if(stage==81){
                if(elapsed(level)<8)return false;
                for(Rig rig:RIGS)if(rig.route==0&&!done("relink/"+rig.id())){
                    active="relink/"+rig.id();var owner=body(level,rig);
                    OLD_LINKS.put(rig.index,new Relink(owner.getSubLevelID(),owner.getPlatePos(),(RotaryConstraintHandle)HANDLE.get(owner),(ServerSubLevel)SubLevelContainer.getContainer(level).getSubLevel(owner.getSubLevelID())));
                    owner.disassemble();check(!owner.isAssembled(),active+": genuine disassembly refused");
                    check(level.getBlockState(rig.pos().relative(rig.facing)).is(AllBlocks.SHAFT),active+": paid rotor shaft not restored");
                }changedAt=level.getGameTime();stage=9;return false;
            }
            if(stage==9){if(elapsed(level)<4)return false;for(Rig rig:RIGS)if(rig.route==0&&!done("relink/"+rig.id())){
                var old=OLD_LINKS.get(rig.index);check(!old.handle.isValid(),rig.id()+": old native joint leaked after disassembly");
                check(old.sub.isRemoved()||SubLevelContainer.getContainer(level).getSubLevel(old.id)==null,rig.id()+": old real plot leaked after disassembly");
                check(!level.getBlockState(old.plate).is(SimBlocks.SWIVEL_BEARING_LINK_BLOCK),rig.id()+": old plot plate leaked after disassembly");
                check(body(level,rig).getPlatePos()==null&&body(level,rig).getSubLevelID()==null,rig.id()+": old links not cleared");body(level,rig).assemble();
            }changedAt=level.getGameTime();stage=10;return false;}
            if(stage==10){
                if(elapsed(level)<12)return false;
                for(Rig rig:RIGS)if(rig.route==0&&!done("relink/"+rig.id())){verify(level,rig,0,0,0);ownership(level,rig);var old=OLD_LINKS.get(rig.index);check(!body(level,rig).getSubLevelID().equals(old.id),rig.id()+": reassembly retained retired plot UUID");check(HANDLE.get(body(level,rig))!=old.handle,rig.id()+": reassembly reused retired joint handle");record(level,rig,"relink/"+rig.id(),checks,Map.of("oldSubLevelUuid",old.id.toString(),"oldJointInvalid",!old.handle.isValid()));}
                for(Rig rig:RIGS)motors(level,rig,64,rig.cog()?4:0);changedAt=level.getGameTime();stage=11;return false;
            }
            if(elapsed(level)<12)return false;
            if(stage==11){
                for(Rig rig:RIGS){
                    verify(level,rig,64,rig.cog()?4:0,0);ownership(level,rig);activeNetworks(level,rig);
                    if(!done("ownership/"+rig.id()))record(level,rig,"ownership/"+rig.id(),checks,Map.of());
                    if(!done("driven-snapshot/"+rig.id()))record(level,rig,"driven-snapshot/"+rig.id(),checks,Map.of());
                    if(rig.cog())setMotor(level,rig.cogMotor(),0);
                }changedAt=level.getGameTime();stage=12;return false;
            }
            for(Rig rig:RIGS)if(!done("snapshot/"+rig.id())){verify(level,rig,64,0,0);ownership(level,rig);physicalNetwork(level,rig);record(level,rig,"snapshot/"+rig.id(),checks,Map.of());}
            complete=true;return true;
        }catch(Throwable failure){DirectionMatrixSupport.fail(GROUP,active,failure);throw failure instanceof RuntimeException e?e:new RuntimeException(failure);}
    }
    private static boolean reuseDiagnostic(ServerLevel level,List<String>checks)throws Exception{
        List<Rig>rigs=new ArrayList<>();for(Direction facing:Direction.values())rigs.add(new Rig(100+facing.ordinal(),facing,0,null,0));
        if(stage==0){for(Rig rig:rigs)build(level,rig);changedAt=level.getGameTime();stage=1;return false;}
        if(stage==1){if(elapsed(level)<4)return false;for(Rig rig:rigs)body(level,rig).assemble();changedAt=level.getGameTime();stage=2;return false;}
        if(stage==2){if(elapsed(level)<12)return false;for(Rig rig:rigs){active="reuse-initial/"+rig.id();joint(level,rig);var owner=body(level,rig);OLD_LINKS.put(rig.index,new Relink(owner.getSubLevelID(),owner.getPlatePos(),(RotaryConstraintHandle)HANDLE.get(owner),(ServerSubLevel)SubLevelContainer.getContainer(level).getSubLevel(owner.getSubLevelID())));owner.disassemble();}changedAt=level.getGameTime();stage=3;return false;}
        if(elapsed(level)<4)return false;
        List<Map<String,Object>>observations=new ArrayList<>();
        for(Rig rig:rigs){
            active="reuse-cache/"+rig.id();var old=OLD_LINKS.get(rig.index);level.getBlockState(old.plate);
            var cachedBefore=level.getChunk(old.plate);body(level,rig).assemble();var owner=body(level,rig);
            var sub=(ServerSubLevel)SubLevelContainer.getContainer(level).getSubLevel(owner.getSubLevelID());
            var expectedPlate=sub.getPlot().getCenterBlock().relative(rig.facing.getOpposite());
            var actualChunk=SubLevelContainer.getContainer(level).getChunk(new ChunkPos(expectedPlate.getX()>>4,expectedPlate.getZ()>>4));var returnedChunk=level.getChunk(expectedPlate);
            Map<String,Object>e=new LinkedHashMap<>();e.put("facing",rig.facing.getSerializedName());e.put("oldPlate",coords(old.plate));e.put("newExpectedPlate",coords(expectedPlate));e.put("cachedBeforeClass",cachedBefore.getClass().getName());e.put("returnedChunkClass",returnedChunk.getClass().getName());e.put("actualPlotChunkClass",actualChunk.getClass().getName());e.put("sameChunkIdentity",returnedChunk==actualChunk);e.put("worldPlateState",level.getBlockState(expectedPlate).toString());e.put("actualPlotPlateState",actualChunk.getBlockState(expectedPlate).toString());e.put("recordedPlate",owner.getPlatePos()==null?"null":coords(owner.getPlatePos()));observations.add(e);
            DirectionMatrixSupport.pass(GROUP,active+"/observation",e);checks.add(active+"/observation");
        }
        Files.writeString(DirectionMatrixSupport.journalPath(GROUP).resolveSibling("reuse-cache-observations.json"),new com.google.gson.GsonBuilder().setPrettyPrinting().create().toJson(observations));
        for(var e:observations)check(Boolean.TRUE.equals(e.get("sameChunkIdentity")),"Actual plot allocation returns stale cached chunk: "+e);
        complete=true;return true;
    }
    private static boolean restore(ServerLevel level,List<String>checks)throws Exception{
        if(stage==0){for(Rig rig:RIGS)load(level,rig.pos());changedAt=level.getGameTime();stage=1;return false;}
        if(elapsed(level)<24)return false;
        var saved=JsonParser.parseString(Files.readString(DirectionMatrixSupport.journalPath(GROUP))).getAsJsonObject().getAsJsonObject("cases");
        if(stage==2){
            for(Rig rig:RIGS)if(!done("restore-driven/"+rig.id())){
                active="restore-driven/"+rig.id();verify(level,rig,64,rig.cog()?4:0,0);ownership(level,rig);activeNetworks(level,rig);
                var driven=saved.getAsJsonObject("initial/driven-snapshot/"+rig.id()).getAsJsonObject("evidence");
                check(body(level,rig).network.longValue()==driven.get("physicalNetwork").getAsLong(),active+": genuine physical network ID after native reload");
                if(rig.cog())check(body(level,rig).getExtraKinetics().network.longValue()==driven.get("storedNetwork").getAsLong(),active+": genuine stored network ID after native reload/resume");
                record(level,rig,active,checks,Map.of());
            }complete=true;return true;
        }
        for(Rig rig:RIGS)if(!done("restore/"+rig.id())){
            active="restore/"+rig.id();verify(level,rig,64,0,0);ownership(level,rig);physicalNetwork(level,rig);var evidence=saved.getAsJsonObject("initial/snapshot/"+rig.id()).getAsJsonObject("evidence");
            check(body(level,rig).getSubLevelID().toString().equals(evidence.get("subLevelUuid").getAsString()),active+": real saved plot UUID changed");
            var plate=evidence.getAsJsonArray("platePos");var pos=body(level,rig).getPlatePos();
            check(pos.getX()==plate.get(0).getAsInt()&&pos.getY()==plate.get(1).getAsInt()&&pos.getZ()==plate.get(2).getAsInt(),active+": native saved plate position changed");
            check(channelId(level,rig)==evidence.get("secondaryNetworkId").getAsLong(),active+": native stored channel source ID changed");
            check(body(level,rig).network.longValue()==evidence.get("physicalNetwork").getAsLong(),active+": native physical network identity changed");
            check(Math.abs(wrap(body(level,rig).getTargetAngleDegrees()-evidence.get("targetAngle").getAsDouble()))<.001,active+": native saved stopped servo target was lost");
            record(level,rig,active,checks,Map.of());
        }
        for(Rig rig:RIGS)if(rig.cog())setMotor(level,rig.cogMotor(),4);
        changedAt=level.getGameTime();stage=2;return false;
    }
    private static boolean eligible(Rig rig,int secondary){return rig.cog()||secondary==0;}
    private static String caseId(Rig rig,int p,int s,int power){return "direction/"+rig.id()+"/primary-"+p+"/secondary-"+(rig.cog()?s:0)+"/signal-"+power;}
    private static void build(ServerLevel level,Rig rig){
        active="assembly/"+rig.id();load(level,rig.pos());
        for(BlockPos pos:BlockPos.betweenClosed(rig.pos().offset(-4,-4,-4),rig.pos().offset(4,4,4)))level.setBlockAndUpdate(pos,Blocks.AIR.defaultBlockState());
        level.setBlockAndUpdate(rig.pos(),SimBlocks.SWIVEL_BEARING.defaultBlockState().setValue(SwivelBearingBlock.FACING,rig.facing));
        BlockPos front=rig.pos().relative(rig.facing);shaft(level,front,rig.facing.getAxis());
        if(rig.route==1){
            placeMotor(level,front.relative(rig.facing),rig.facing.getOpposite());shaft(level,rig.pos().relative(rig.facing.getOpposite()),rig.facing.getAxis());
        }else placeMotor(level,rig.pos().relative(rig.facing.getOpposite()),rig.facing);
        if(rig.cog()){level.setBlockAndUpdate(rig.cogPos(),AllBlocks.COGWHEEL.defaultBlockState().setValue(BlockStateProperties.AXIS,rig.facing.getAxis()));placeMotor(level,rig.cogMotor(),rig.motorSide>0?rig.facing.getOpposite():rig.facing);}
    }
    private static void placeMotor(ServerLevel level,BlockPos pos,Direction facing){level.setBlockAndUpdate(pos,AllBlocks.CREATIVE_MOTOR.defaultBlockState().setValue(CreativeMotorBlock.FACING,facing));setMotor(level,pos,0);}
    private static void shaft(ServerLevel level,BlockPos pos,Direction.Axis axis){level.setBlockAndUpdate(pos,AllBlocks.SHAFT.defaultBlockState().setValue(BlockStateProperties.AXIS,axis));}
    private static BlockPos primaryMotor(ServerLevel level,Rig rig){
        if(rig.route!=1)return rig.pos().relative(rig.facing.getOpposite());
        BlockPos plate=body(level,rig).getPlatePos(),pos=plate.relative(rig.facing,2);
        check(level.getBlockEntity(pos) instanceof CreativeMotorBlockEntity,rig.id()+": front motor missing; plate="+plate+" expected="+pos+" actual="+level.getBlockState(pos)+" shaft="+level.getBlockState(plate.relative(rig.facing))+" originalMotor="+level.getBlockState(rig.pos().relative(rig.facing,2)));
        return pos;
    }
    private static void motors(ServerLevel level,Rig rig,int p,int s){setMotor(level,primaryMotor(level,rig),p);if(rig.cog())setMotor(level,rig.cogMotor(),s);}
    private static void setMotor(ServerLevel level,BlockPos pos,int rpm){check(level.getBlockEntity(pos) instanceof CreativeMotorBlockEntity,"real motor missing "+pos);var motor=(CreativeMotorBlockEntity)level.getBlockEntity(pos);motor.generatedSpeed.setValue(rpm*motor.getBlockState().getValue(CreativeMotorBlock.FACING).getAxisDirection().getStep());}
    private static void power(ServerLevel level,Rig rig,int signal){level.setBlockAndUpdate(rig.pos().relative(rig.powerFace()),(signal==0?Blocks.AIR:Blocks.REDSTONE_BLOCK).defaultBlockState());}
    private static void verify(ServerLevel level,Rig rig,int primary,int secondary,int signal)throws Exception{
        joint(level,rig);var owner=body(level,rig);BlockPos plate=owner.getPlatePos();
        near(owner.getSpeed(),primary,active+": actual body RPM");near(((KineticBlockEntity)level.getBlockEntity(plate)).getSpeed(),primary,active+": actual owned plot plate RPM");
        near(((KineticBlockEntity)level.getBlockEntity(plate.relative(rig.facing))).getSpeed(),primary,active+": real plot rotor shaft RPM");
        near(owner.getExtraKinetics().getSpeed(),rig.cog()?-secondary:0,active+": independent actual stored cog RPM");
        check(owner.getBlockState().getValue(SwivelBearingBlock.POWERED)==(signal==0),active+": actual default lock/redstone branch");
        check(!owner.isConflictDestructionPending()&&!owner.getExtraKinetics().isConflictDestructionPending(),active+": native conflict destruction pending");
        if(primary!=0){BlockPos source=primaryMotor(level,rig);check(source.equals(owner.getGeneratingSource()),active+": physical generator provenance");check(source.equals(((KineticBlockEntity)level.getBlockEntity(plate)).getGeneratingSource()),active+": plot plate generator provenance");}
        if(rig.cog()&&secondary!=0)check(rig.cogMotor().equals(owner.getExtraKinetics().getGeneratingSource()),active+": independent cog generator provenance");
    }
    private static void joint(ServerLevel level,Rig rig)throws Exception{
        var owner=body(level,rig);check(owner!=null&&owner.isAssembled()&&owner.getPlatePos()!=null&&owner.getSubLevelID()!=null,active+": "+rig.id()+" genuine assembly/link missing; assembled="+(owner==null?null:owner.isAssembled())+" plate="+(owner==null?null:owner.getPlatePos())+" uuid="+(owner==null?null:owner.getSubLevelID())+" error="+(owner==null?null:owner.getLastAssemblyException())+" front="+level.getBlockState(rig.pos().relative(rig.facing))+" plot="+plotDiagnostic(level,owner));
        check(level.getBlockState(owner.getPlatePos()).is(SimBlocks.SWIVEL_BEARING_LINK_BLOCK),active+": owned plate missing");
        var sub=(ServerSubLevel)SubLevelContainer.getContainer(level).getSubLevel(owner.getSubLevelID());
        check(sub!=null&&!sub.isRemoved()&&sub.logicalPose().position().isFinite()&&sub.logicalPose().orientation().isFinite(),active+": real Sable pose missing/nonfinite");
        var handle=(RotaryConstraintHandle)HANDLE.get(owner);check(handle!=null&&handle.isValid(),active+": actual Rapier rotary joint missing");
        check(RigidBodyHandle.of(sub).getLinearVelocity(new Vector3d()).isFinite(),active+": real Rapier velocity nonfinite");
    }
    private static void ownership(ServerLevel level,Rig rig)throws Exception{
        joint(level,rig);var owner=body(level,rig);var sub=SubLevelContainer.getContainer(level).getSubLevel(owner.getSubLevelID());var pos=owner.getPlatePos();
        check(Sable.HELPER.getContaining(level,pos)==sub,active+": owned plate belongs to another actual plot");
        check(Sable.HELPER.getContaining(level,pos.relative(rig.facing))==sub,active+": paid shaft belongs to another actual plot");
        if(rig.route==1)check(Sable.HELPER.getContaining(level,pos.relative(rig.facing,2))==sub,active+": paid front motor belongs to another actual plot");
        check(PLATE_PARENT.get((SwivelBearingPlateBlockEntity)level.getBlockEntity(pos)) instanceof BlockPos parent&&parent.equals(rig.pos()),active+": reciprocal plate parent mismatch");
    }
    private static String plotDiagnostic(ServerLevel level,SwivelBearingBlockEntity owner){
        if(owner==null||owner.getSubLevelID()==null)return"none";var sub=SubLevelContainer.getContainer(level).getSubLevel(owner.getSubLevelID());
        if(sub==null)return"missing";var plot=sub.getPlot();var p=plot.getCenterBlock();List<String>found=new ArrayList<>();
        for(BlockPos pos:BlockPos.betweenClosed(p.offset(-4,-4,-4),p.offset(4,4,4)))if(!level.getBlockState(pos).isAir())found.add(pos+"="+level.getBlockState(pos)+"/be="+level.getBlockEntity(pos));return p+" "+found;
    }
    private static void activeNetworks(ServerLevel level,Rig rig){
        physicalNetwork(level,rig);var owner=body(level,rig);
        if(rig.cog()){
            var extra=owner.getExtraKinetics();var motor=(KineticBlockEntity)level.getBlockEntity(rig.cogMotor());
            check(extra.network!=null&&extra.network.equals(motor.network)&&!extra.network.equals(owner.network),active+": actual independently sourced network identities collapsed");
            check(NativeKineticPositions.same(motor.getBlockPos(),extra.getGeneratingSource()),active+": stored generator channel identity mismatch");
        }
    }
    private static void physicalNetwork(ServerLevel level,Rig rig){
        var owner=body(level,rig);var source=(KineticBlockEntity)level.getBlockEntity(primaryMotor(level,rig));
        check(owner.network!=null&&owner.network!=0&&owner.network.equals(source.network),active+": actual physical source network mismatch");
        check(NativeKineticPositions.same(source.getBlockPos(),owner.getGeneratingSource()),active+": physical generator channel identity mismatch");
    }
    private static void record(ServerLevel level,Rig rig,String id,List<String>checks,Map<String,Object>extra){
        active=id;var owner=body(level,rig);Map<String,Object>e=new LinkedHashMap<>(extra);e.put("fixture",coords(rig.pos()));e.put("facing",rig.facing.getSerializedName());e.put("route",rig.id());e.put("platePos",coords(owner.getPlatePos()));e.put("subLevelUuid",owner.getSubLevelID().toString());e.put("targetAngle",owner.getTargetAngleDegrees());e.put("bodyRpm",owner.getSpeed());e.put("storedRpm",owner.getExtraKinetics().getSpeed());e.put("secondaryNetworkId",channelId(level,rig));e.put("physicalNetwork",owner.network);e.put("storedNetwork",owner.getExtraKinetics().network);DirectionMatrixSupport.pass(GROUP,id,e);checks.add(GROUP+"/"+id);
    }
    private static long channelId(ServerLevel level,Rig rig){return((NativeKineticChannelState)body(level,rig).getExtraKinetics()).simulated$virtualNetworkId();}
    private static SwivelBearingBlockEntity body(ServerLevel level,Rig rig){return(SwivelBearingBlockEntity)level.getBlockEntity(rig.pos());}
    private static Field field(String name){try{Field f=SwivelBearingBlockEntity.class.getDeclaredField(name);f.setAccessible(true);return f;}catch(Exception e){throw new RuntimeException(e);}}
    private static Field plateField(String name){try{Field f=SwivelBearingPlateBlockEntity.class.getDeclaredField(name);f.setAccessible(true);return f;}catch(Exception e){throw new RuntimeException(e);}}
    private static List<Integer>coords(BlockPos pos){return List.of(pos.getX(),pos.getY(),pos.getZ());}
    private static double wrap(double value){return((value+180)%360+360)%360-180;}
    private static void load(ServerLevel level,BlockPos pos){level.setChunkForced(pos.getX()>>4,pos.getZ()>>4,true);level.getChunk(pos);}
    private static long elapsed(ServerLevel level){return level.getGameTime()-changedAt;}
    private static boolean done(String id){return DirectionMatrixSupport.done(GROUP,id);}
    private static void check(boolean v,String message){DirectionMatrixSupport.check(v,message);}
    private static void near(float actual,float expected,String message){check(Float.isFinite(actual)&&Math.abs(actual-expected)<.02f,message+": expected "+expected+", got "+actual);}
    private DirectionSwivelMatrix(){}
}
