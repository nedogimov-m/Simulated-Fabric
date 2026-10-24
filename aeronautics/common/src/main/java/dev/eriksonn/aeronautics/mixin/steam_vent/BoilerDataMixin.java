package dev.eriksonn.aeronautics.mixin.steam_vent;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalIntRef;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.api.stress.BlockStressValues;
import com.simibubi.create.content.equipment.goggles.GoggleTooltip;
import com.simibubi.create.content.fluids.tank.BoilerData;
import com.simibubi.create.content.fluids.tank.FluidTankBlockEntity;
import com.simibubi.create.foundation.utility.CreateLang;
import dev.eriksonn.aeronautics.data.AeroLang;
import dev.eriksonn.aeronautics.index.AeroBlocks;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import java.util.List;

/** Vent consumers share the real native boiler supply, heat, efficiency and saved state. */
@Mixin(BoilerData.class)
public abstract class BoilerDataMixin {
    @Shadow public int attachedEngines;
    @Shadow public int activeHeat;
    @Shadow public float waterSupply;
    @Unique private int aeronautics$attachedVents;
    @Unique private static final float AERONAUTICS_PASSIVE_EFFICIENCY = .125f;

    @Inject(method = "evaluate", at = @At("HEAD"), require = 1)
    private void aeronautics$beginVentCount(FluidTankBlockEntity controller, CallbackInfoReturnable<Boolean> cir,
                                           @Share("prevVents") LocalIntRef previous) {
        previous.set(this.aeronautics$attachedVents); this.aeronautics$attachedVents = 0;
    }
    @ModifyExpressionValue(method = "evaluate", at = @At(value = "INVOKE", ordinal = 1,
            target = "Lcom/simibubi/create/foundation/utility/LoadedLevelAccess;blockState(Lnet/minecraft/world/level/BlockGetter;Lnet/minecraft/core/BlockPos;)Lnet/minecraft/world/level/block/state/BlockState;"), require = 1, expect = 1)
    private BlockState aeronautics$countNativeNeighbor(BlockState state) {
        if (state.is(AeroBlocks.STEAM_VENT)) this.aeronautics$attachedVents++;
        return state;
    }
    @ModifyReturnValue(method = "evaluate", at = @At("RETURN"), require = 1)
    private boolean aeronautics$changedVents(boolean original, @Share("prevVents") LocalIntRef previous) {
        return original || this.aeronautics$attachedVents != previous.get();
    }
    @ModifyReturnValue(method = "isActive", at = @At("RETURN"), require = 1)
    private boolean aeronautics$activeWithVents(boolean original) { return original || this.aeronautics$attachedVents > 0; }
    @ModifyExpressionValue(method = "getEngineEfficiency", at = @At(value = "FIELD",
            target = "Lcom/simibubi/create/content/fluids/tank/BoilerData;attachedEngines:I"), require = 4, expect = 4)
    private int aeronautics$ventEfficiency(int engines) { return engines + this.aeronautics$attachedVents; }

    @Inject(method = "addToGoggleTooltip", at = @At("HEAD"), cancellable = true, require = 1)
    private void aeronautics$nativeVentTooltip(List<Component> tooltip, boolean sneaking, int size,
                                              CallbackInfoReturnable<Boolean> cir) {
        final var boiler = (BoilerData) (Object) this;
        if (!boiler.isActive()) { cir.setReturnValue(false); return; }
        boiler.calcMinMaxForSize(size);
        tooltip.add(GoggleTooltip.translate("boiler.status", boiler.getHeatLevelTextComponent().withStyle(ChatFormatting.GREEN)));
        GoggleTooltip.line(tooltip, boiler.getSizeComponent(true, false), 1);
        GoggleTooltip.line(tooltip, boiler.getWaterComponent(true, false), 1);
        GoggleTooltip.line(tooltip, boiler.getHeatComponent(true, false), 1);
        if (this.attachedEngines + this.aeronautics$attachedVents == 0) { cir.setReturnValue(true); return; }
        tooltip.add(Component.empty());
        if (boiler.getMaxHeatLevelForBoilerSize(size) > 0 && boiler.getMaxHeatLevelForWaterSupply() == 0
                && (boiler.passiveHeat ? 1 : this.activeHeat) > 0) {
            tooltip.add(GoggleTooltip.translate("boiler.water_input_rate").withStyle(ChatFormatting.GRAY));
            GoggleTooltip.line(tooltip, GoggleTooltip.units(this.waterSupply, "millibuckets").withStyle(ChatFormatting.BLUE)
                    .append(Component.literal(" / ").withStyle(ChatFormatting.GRAY))
                    .append(GoggleTooltip.translate("boiler.per_tick", GoggleTooltip.units(10, "millibuckets")).withStyle(ChatFormatting.DARK_GRAY)), 1);
            cir.setReturnValue(true); return;
        }
        final int heat = Math.max(boiler.getActualHeat(size), 1);
        final float passive = boiler.isPassive(size) ? AERONAUTICS_PASSIVE_EFFICIENCY : 1;
        final double capacity = passive * 16 * heat * BlockStressValues.getCapacity(AllBlocks.STEAM_ENGINE);
        tooltip.add(GoggleTooltip.translate("tooltip.capacityProvided").withStyle(ChatFormatting.GRAY));
        CreateLang.number(capacity).translate("generic.unit.stress").style(ChatFormatting.AQUA).forGoggles(tooltip, 1);
        final float efficiency = boiler.getEngineEfficiency(size) / passive;
        final double engineSU = capacity * efficiency * this.attachedEngines / heat;
        final double ventSU = capacity * efficiency * this.aeronautics$attachedVents / heat;
        if (engineSU > 0 || ventSU > 0) AeroLang.translate("tooltip.capacity_used").style(ChatFormatting.GRAY).forGoggles(tooltip);
        if (engineSU > 0) CreateLang.number(engineSU).translate("generic.unit.stress").style(ChatFormatting.AQUA).space()
                .add((this.attachedEngines == 1 ? CreateLang.translate("boiler.via_one_engine")
                        : CreateLang.translate("boiler.via_engines", this.attachedEngines)).style(ChatFormatting.DARK_GRAY)).forGoggles(tooltip, 1);
        if (ventSU > 0) CreateLang.number(ventSU).translate("generic.unit.stress").style(ChatFormatting.AQUA).space()
                .add((this.aeronautics$attachedVents == 1 ? AeroLang.translate("boiler.via_one_vent")
                        : AeroLang.translate("boiler.via_vents", this.aeronautics$attachedVents)).style(ChatFormatting.DARK_GRAY)).forGoggles(tooltip, 1);
        cir.setReturnValue(true);
    }
    @Inject(method = "clear", at = @At("TAIL"), require = 1)
    private void aeronautics$clearVents(CallbackInfo ci) { this.aeronautics$attachedVents = 0; }
    @Inject(method = "save", at = @At("TAIL"), require = 1)
    private void aeronautics$saveVents(ValueOutput output, CallbackInfo ci) { output.putInt("SimVents", this.aeronautics$attachedVents); }
    @Inject(method = "load", at = @At("TAIL"), require = 1)
    private void aeronautics$loadVents(ValueInput input, int size, CallbackInfo ci) { this.aeronautics$attachedVents = input.getIntOr("SimVents", 0); }
}
