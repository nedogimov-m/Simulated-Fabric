import com.simibubi.create.AllItems;
import com.simibubi.create.foundation.utility.LoadedEntityAccess;
import dev.ryanhcode.sable.api.physics.handle.RigidBodyHandle;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import dev.simulated_team.simulated.content.blocks.portable_engine.PortableEngineBlockEntity;
import dev.simulated_team.simulated.index.SimBlocks;
import dev.simulated_team.simulated.mixin.extra_kinetics.NativeKineticDataAccessor;
import dev.simulated_team.simulated.multiloader.inventory.ItemInfoWrapper;
import dev.simulated_team.simulated.service.*;
import dev.simulated_team.simulated.util.SimAssemblyHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.CookingFuel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.storage.*;
import net.minecraft.world.level.storage.loot.providers.number.ints.ResolvableInt;
import net.minecraft.world.level.storage.loot.providers.number.floats.ResolvableFloat;
import org.joml.Vector3d;
import java.util.List;

/** New services-only actual packaged-world checks; no previous graph/cargo/dye groups. */
public final class PackagedNativeServices {
    private static int phase;
    private static final java.util.Set<String> SELECTED=java.util.Set.of(System.getProperty("port.smoke.servicesChecks","all").split(","));
    private static final boolean FUEL=SELECTED.contains("all")||SELECTED.contains("fuel");
    private static long changedAt;
    private static ServerSubLevel assembled;
    private static final BlockPos ENGINE=new BlockPos(0,120,0),FURNACE=ENGINE.east(4),SOURCE=ENGINE.east(8);

    /** Call each server tick from tick20 until true; usually completes after8 more actual ticks. */
    public static boolean runOnServer(MinecraftServer server,List<String> checks) throws Exception {
        var level=server.overworld();
        if(phase==0){
            level.setChunkForced(0,0,true);level.getChunk(ENGINE);
            if(FUEL){
                level.setBlock(ENGINE,SimBlocks.RED_PORTABLE_ENGINE.defaultBlockState(),3);
                level.setBlock(FURNACE,Blocks.BLAST_FURNACE.defaultBlockState(),3);
            }
            changedAt=level.getGameTime();phase=1;return false;
        }
        if(phase==1){
            if(level.getGameTime()-changedAt<4||!LoadedEntityAccess.isReady(level,ENGINE))return false;
            if(FUEL){
                var engine=(PortableEngineBlockEntity)level.getBlockEntity(ENGINE);var furnace=level.getBlockEntity(FURNACE);
                var fuel=SimItemService.INSTANCE;
                var constant=new ItemStack(Items.COAL);constant.set(DataComponents.COOKING_FUEL,
                        new CookingFuel(new ResolvableInt.Constant(321),new ResolvableFloat.Constant(1)));
                check(fuel.getBurnTime(constant,engine)==321,"Native component constant fuel must retain its actual patched duration");
                var removed=constant.copy();removed.remove(DataComponents.COOKING_FUEL);
                check(!engine.inventory.canInsertItem(ItemInfoWrapper.generateFromStack(removed)),"Native inventory filtering must preserve a removed fuel component patch");
                check(engine.inventory.canInsertItem(ItemInfoWrapper.generateFromStack(constant)),"Native inventory must accept the real patched constant fuel");
                var conditional=new ItemStack(AllItems.BLAZE_CAKE);
                check(conditional.get(DataComponents.COOKING_FUEL).burnTime() instanceof ResolvableInt.Reference,"Actual Create blaze cake must exercise the native conditional registry reference");
                int normal=fuel.getBurnTime(conditional,engine),fast=fuel.getBurnTime(conditional,furnace);
                check(normal>0&&fast>0&&normal!=fast,"Native referenced cooking fuel must resolve against each actual owner block/inventory context");
                check(fuel.getSuperheatedBurnTime(conditional,engine)>0,"Native Create server data map must recognize superheated fuel");
                engine.inventory.slot.setStack(constant.copy());
                var out=TagValueOutput.createWithContext(ProblemReporter.DISCARDING,level.registryAccess());engine.write(out,true);
                var tag=out.buildResult();check(tag.getIntOr("NextFuelBurnTime",-1)==321,"Actual server ValueOutput must publish the evaluated next fuel duration");
                var replica=(PortableEngineBlockEntity)engine.getType().create(ENGINE,engine.getBlockState());replica.setLevel(level);
                ((NativeKineticDataAccessor)replica).simulated$readKinetics(TagValueInput.create(ProblemReporter.DISCARDING,level.registryAccess(),tag),true);
                replica.setLevel(null);
                var next=PortableEngineBlockEntity.class.getDeclaredMethod("getNextBurnTime");next.setAccessible(true);
                check((int)next.invoke(replica)==321,"Decoded real BE uses synchronized duration without evaluating a client-side conditional fuel context");
                checks.add("actual_native_constant_conditional_fuel_owner_inventory_context_component_patch_and_valueio_cache");
            }
            var glue=SimAssemblyService.INSTANCE;
            check(!glue.canStickTo(Blocks.STONE.defaultBlockState(),Blocks.STONE.defaultBlockState())&&glue.canStickTo(Blocks.SLIME_BLOCK.defaultBlockState(),Blocks.STONE.defaultBlockState()),"Actual native adhesion distinguishes ordinary solids from sticky connections");
            check(!glue.canStickTo(Blocks.HONEY_BLOCK.defaultBlockState(),Blocks.SLIME_BLOCK.defaultBlockState())&&!glue.canStickTo(Blocks.SLIME_BLOCK.defaultBlockState(),Blocks.HONEY_BLOCK.defaultBlockState()),"Actual native piston adhesion must reject both honey/slime orders");
            level.setBlock(SOURCE,Blocks.STONE.defaultBlockState(),3);level.setBlock(SOURCE.east(),Blocks.STONE.defaultBlockState(),3);
            level.addFreshEntity(new com.simibubi.create.content.contraptions.glue.SuperGlueEntity(level,new net.minecraft.world.phys.AABB(SOURCE).expandTowards(1,0,0)));
            var result=SimAssemblyHelper.assembleFromSingleBlock(level,SOURCE.below(),SOURCE,true,false);
            check(result!=null&&result.subLevel() instanceof ServerSubLevel,"Actual Sim assembly service must allocate a real Sable sublevel");
            assembled=(ServerSubLevel)result.subLevel();
            check(level.getBlockState(SOURCE).isAir()&&level.getBlockState(SOURCE.east()).isAir(),"Real source blocks must transfer into the allocated plot");
            changedAt=level.getGameTime();phase=2;return false;
        }
        if(level.getGameTime()-changedAt<4)return false;
        check(!assembled.isRemoved()&&assembled.getSelfMassTracker().getMass()>0,"Real transferred plot must retain positive native physics mass");
        var velocity=RigidBodyHandle.of(assembled).getLinearVelocity(new Vector3d());
        check(velocity.isFinite()&&assembled.logicalPose().position().isFinite(),"Actual Rapier body and sublevel pose must remain valid after four physics ticks");
        checks.add("actual_native_adhesion_service_sim_assembly_real_sable_plot_mass_and_four_rapier_ticks");
        return true;
    }
    private static void check(boolean value,String message){if(!value)throw new AssertionError(message);}
}
