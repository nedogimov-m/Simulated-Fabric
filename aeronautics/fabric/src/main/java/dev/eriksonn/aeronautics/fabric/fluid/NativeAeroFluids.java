package dev.eriksonn.aeronautics.fabric.fluid;

import dev.eriksonn.aeronautics.index.AeroSoundEvents;
import dev.simulated_team.simulated.fabric.registry.NativeItemCatalog;
import dev.simulated_team.simulated.fabric.registry.NativeRegistrations;
import net.fabricmc.fabric.api.transfer.v1.fluid.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.dispenser.BlockSource;
import net.minecraft.core.dispenser.DefaultDispenseItemBehavior;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockBehaviour;
import java.util.Optional;

/** Registers the original separate fluid block and bucket, outside the normal block-item catalog. */
public final class NativeAeroFluids {
    public static final LevititeBlendFluid.Source SOURCE = Registry.register(BuiltInRegistries.FLUID,
            id("levitite_blend"), new LevititeBlendFluid.Source());
    public static final LevititeBlendFluid.Flowing FLOWING = Registry.register(BuiltInRegistries.FLUID,
            id("flowing_levitite_blend"), new LevititeBlendFluid.Flowing());
    public static final LiquidBlock BLOCK = NativeRegistrations.block(id("levitite_blend"),
            properties -> new LiquidBlock(SOURCE, properties) {},
            BlockBehaviour.Properties.ofFullCopy(Blocks.WATER).strength(100).noLootTable());
    public static final BucketItem BUCKET = NativeRegistrations.item(id("levitite_blend_bucket"),
            properties -> new BucketItem(SOURCE, properties) {
                @Override protected void playEmptySound(LivingEntity entity, LevelAccessor level, BlockPos pos) {
                    level.playSound(entity, pos, AeroSoundEvents.LEVITITE_BLEND_EMPTY.event(), SoundSource.BLOCKS, 1, 1);
                }
            }, new Item.Properties().stacksTo(1).craftRemainder(Items.BUCKET));
    private static boolean registered;

    public static void register() {
        if (registered) return;
        registered = true;
        NativeItemCatalog.add(BUCKET, id("aeronautics"), NativeItemCatalog.Visibility.MAIN);
        final var attributes = new FluidVariantAttributeHandler() {
            @Override public Component getName(FluidVariant fluid) { return Component.translatable("fluid.aeronautics.levitite_blend"); }
            @Override public int getViscosity(FluidVariant fluid, Level level) { return 1500; }
            @Override public Optional<SoundEvent> getFillSound(FluidVariant fluid) { return SOURCE.getPickupSound(); }
            @Override public Optional<SoundEvent> getEmptySound(FluidVariant fluid) { return Optional.of(AeroSoundEvents.LEVITITE_BLEND_EMPTY.event()); }
        };
        FluidVariantAttributes.register(SOURCE, attributes);
        FluidVariantAttributes.register(FLOWING, attributes);
        DispenserBlock.registerBehavior(BUCKET, new DefaultDispenseItemBehavior() {
            @Override protected ItemStack execute(BlockSource source, ItemStack stack) {
                final var pos = source.pos().relative(source.state().getValue(DispenserBlock.FACING));
                if (BUCKET.emptyContents(null, source.level(), pos, null)) {
                    BUCKET.checkExtraContent(null, source.level(), stack, pos);
                    return consumeWithRemainder(source, stack, new ItemStack(Items.BUCKET));
                }
                return super.execute(source, stack);
            }
        });
    }
    private static Identifier id(String path) { return Identifier.fromNamespaceAndPath("aeronautics", path); }
    private NativeAeroFluids() {}
}
