package dev.simulated_team.simulated.multiloader.tanks;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.DataResult;
import com.mojang.logging.LogUtils;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.Fluids;

/**
 * A loader-independent representation of a fluid
 */
public record CFluidType(Fluid fluid, DataComponentPatch data) {
    public boolean isBlank() {
        return this.fluid == Fluids.EMPTY;
    }

    public static final CFluidType BLANK = new CFluidType(Fluids.EMPTY, DataComponentPatch.EMPTY);

    public CompoundTag write() {
        final CompoundTag tag = new CompoundTag();
        tag.putString("Fluid", BuiltInRegistries.FLUID.getKey(this.fluid).toString());

        final DataResult<Tag> result = DataComponentPatch.CODEC.encodeStart(NbtOps.INSTANCE, this.data);
        if (result.isError()) {
            LogUtils.getLogger().warn(result.error().get().message());
        } else {
            tag.put("data", result.result().get());
        }

        return tag;
    }

    public static CFluidType read(final CompoundTag tag) {
        final Identifier id = Identifier.tryParse(tag.getStringOr("Fluid", ""));
        if (id == null) {
            return BLANK;
        }
        final Fluid fluid = BuiltInRegistries.FLUID.getValue(id);
        if (fluid == Fluids.EMPTY) {
            return BLANK;
        }
        DataComponentPatch data = DataComponentPatch.EMPTY;
        if (tag.contains("data")) {
            final DataResult<Pair<DataComponentPatch, Tag>> result = DataComponentPatch.CODEC.decode(NbtOps.INSTANCE, tag.getCompoundOrEmpty("data"));
            if (result.isError()) {
                LogUtils.getLogger().warn(result.error().get().message());
            } else {
                data = result.result().get().getFirst();
            }
        }

        return new CFluidType(fluid, data);
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }

        if (obj instanceof CFluidType(Fluid fluid1, DataComponentPatch data1)) {
            // both haves tag, or both no haves tag
            return this.fluid.isSame(fluid1) && this.data.equals(data1);
        }
        return false;
    }

    @Override
    public int hashCode() {
        final Fluid canonical = this.fluid instanceof FlowingFluid flowing ? flowing.getSource() : this.fluid;
        return 31 * canonical.hashCode() + this.data.hashCode();
    }
}
