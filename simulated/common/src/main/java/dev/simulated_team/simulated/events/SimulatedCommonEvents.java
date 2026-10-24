package dev.simulated_team.simulated.events;

import com.simibubi.create.AllItems;
import dev.ryanhcode.sable.api.sublevel.ServerSubLevelContainer;
import dev.ryanhcode.sable.api.sublevel.SubLevelContainer;
import dev.ryanhcode.sable.index.SableAttributes;
import dev.ryanhcode.sable.platform.SableEventPlatform;
import dev.ryanhcode.sable.sublevel.system.SubLevelPhysicsSystem;
import dev.ryanhcode.sable.sublevel.system.SubLevelTrackingSystem;
import dev.simulated_team.simulated.Simulated;
import dev.simulated_team.simulated.content.blocks.docking_connector.DockingConnectorBlockEntity;
import dev.simulated_team.simulated.content.blocks.handle.ServerHandleHoldingHandler;
import dev.simulated_team.simulated.content.blocks.redstone_magnet.RedstoneMagnetBlockEntity;
import dev.simulated_team.simulated.content.blocks.rope.strand.server.ServerLevelRopeManager;
import dev.simulated_team.simulated.content.blocks.rope.strand.server.ServerRopeTrackingSystem;
import dev.simulated_team.simulated.content.blocks.spring.SpringBlock;
import dev.simulated_team.simulated.content.end_sea.EndSeaPhysicsData;
import dev.simulated_team.simulated.content.entities.diagram.DiagramEntity;
import dev.simulated_team.simulated.content.entities.launched_plunger.LaunchedPlungerServerHandler;
import dev.simulated_team.simulated.content.navigation_targets.lodestone_compass_compatability.LodestoneTrackingMap;
import dev.simulated_team.simulated.content.physics_staff.PhysicsStaffServerHandler;
import dev.simulated_team.simulated.content.physics_staff.PhysicsStaffSubLevelObserver;
import dev.simulated_team.simulated.content.worldgen.SimulatedWorldPreset;
import dev.simulated_team.simulated.index.SimBlocks;
import dev.simulated_team.simulated.index.SimTags;
import dev.simulated_team.simulated.index.SimWorldPresets;
import dev.simulated_team.simulated.mixin_interface.PrimaryLevelDataExtension;
import dev.simulated_team.simulated.index.SimNavigationTargets;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.chunk.ChunkAccess;
import org.jetbrains.annotations.Nullable;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerChunkEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.resource.v1.ResourceLoader;
import net.minecraft.server.packs.PackType;

import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class SimulatedCommonEvents {
    /**
     * Called at the end of the given ServerLevel's tick
     *
     * @param level The level ticking.
     */
    public static void onServerTickEnd(final ServerLevel level) {
        ServerLevelRopeManager.getOrCreate(level).tickRiders();
        PhysicsStaffServerHandler.get(level).tick();
        RedstoneMagnetBlockEntity.GLOBAL_REDSTONE_MAGNET_MAP.tick(level);
        DockingConnectorBlockEntity.MAGNET_CONTROLLER.tick(level);

	    final LodestoneTrackingMap lodestoneMap = LodestoneTrackingMap.getOrLoad(level);
		if (lodestoneMap != null) {
			lodestoneMap.tick();
		}
    }

    public static void onWorldLoad(final LevelAccessor level) {

    }

    /**
     * Called whenever a block is modified in the given level
     */
    public static void onBlockModifiedEvent(final LevelAccessor level, final BlockPos breakPos) {
    }

    public static void onServerStopped(final MinecraftServer server) {
        ServerHandleHoldingHandler.unload(server);
    }

    public static void onPlayerLoggedIn(final Player player) {
        for (final Map.Entry<Identifier, SimulatedWorldPreset> entry : SimWorldPresets.PRESETS.entrySet()) {
            final ServerLevel level = (ServerLevel) player.level();

            if (player instanceof final ServerPlayer serverPlayer) {
                final Identifier worldPreset = ((PrimaryLevelDataExtension) level.getServer().getWorldData()).getPreset();

                if (entry.getValue().id().equals(worldPreset)) {
                    entry.getValue().onPlayerJoin(level, serverPlayer);
                }
            }
        }

        PhysicsStaffServerHandler.sendAllData(player);
    }

    public static void onChunkLoad(final LevelAccessor level, final ChunkAccess chunk, final boolean newChunk) {
        for (final Map.Entry<Identifier, SimulatedWorldPreset> entry : SimWorldPresets.PRESETS.entrySet()) {
            if(level instanceof final ServerLevel serverLevel) {
                final Identifier worldPreset = ((PrimaryLevelDataExtension) serverLevel.getServer().getWorldData()).getPreset();

                if(entry.getValue().id().equals(worldPreset)) {
                    entry.getValue().onChunkLoad(serverLevel, chunk, newChunk);
                }
            }
        }
    }

    public static void onPhysicsTick(final SubLevelPhysicsSystem physicsSystem, final double timeStep) {
        final ServerLevel level = physicsSystem.getLevel();
        RedstoneMagnetBlockEntity.GLOBAL_REDSTONE_MAGNET_MAP.physicsTick(timeStep, level);
        DockingConnectorBlockEntity.MAGNET_CONTROLLER.physicsTick(timeStep, level);
        EndSeaPhysicsData.physicsTick(timeStep, level);
        ServerLevelRopeManager.getOrCreate(level).physicsTick(physicsSystem, timeStep);

        LaunchedPlungerServerHandler.physicsTickAllPlungers(physicsSystem, timeStep);
        PhysicsStaffServerHandler.get(level).physicsTick(physicsSystem);
    }

    public static @Nullable InteractionResult rightClickBlock(final Level level, final BlockPos pos, final Player player, final ItemStack useStack) {
        if (level.getBlockState(pos).is(SimBlocks.SPRING) && useStack.is(SimTags.Items.SPRING_ADJUSTER)) {
            if (SpringBlock.tryAdjustSpring(level, pos, player)) {
                return InteractionResult.SUCCESS;
            } else {
                return InteractionResult.FAIL;
            }
        }
        return null;
    }

    public static void onPostPhysicsTick(final SubLevelPhysicsSystem physicsSystem, final double timeStep) {
        DiagramEntity.postPhysicsTick(physicsSystem.getLevel());
    }

    private static void onContainerReady(final Level level, final SubLevelContainer subLevelContainer) {
        if (!(subLevelContainer instanceof final ServerSubLevelContainer serverContainer)) {
            return;
        }

        serverContainer.addObserver(new PhysicsStaffSubLevelObserver(serverContainer.getLevel()));
        
        final SubLevelTrackingSystem trackingSystem = serverContainer.trackingSystem();
        trackingSystem.addTrackingPlugin(new ServerRopeTrackingSystem(serverContainer.getLevel()));
    }

    public static void register() {
        SableEventPlatform.INSTANCE.onSubLevelContainerReady(SimulatedCommonEvents::onContainerReady);
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            ServerHandleHoldingHandler.tick(server);
            for (final ServerLevel level : server.getAllLevels()) onServerTickEnd(level);
        });
        ServerLifecycleEvents.SERVER_STOPPED.register(SimulatedCommonEvents::onServerStopped);
        ServerPlayConnectionEvents.JOIN.register((connection, sender, server) -> onPlayerLoggedIn(connection.player));
        ServerChunkEvents.CHUNK_LOAD.register(SimulatedCommonEvents::onChunkLoad);
        ServerLifecycleEvents.SYNC_DATA_PACK_CONTENTS.register((player, joined) -> EndSeaPhysicsData.syncDataPacket(packet -> player.connection.send(packet)));
        ResourceLoader.get(PackType.SERVER_DATA).registerReloadListener(EndSeaPhysicsData.ReloadListener.ID, EndSeaPhysicsData.ReloadListener.INSTANCE);
        CommandRegistrationCallback.EVENT.register((dispatcher, registries, environment) ->
                dev.simulated_team.simulated.command.SimCommand.register(dispatcher, registries));
        UseBlockCallback.EVENT.register((player, level, hand, hit) -> {
            if (player.isSpectator()) return InteractionResult.PASS;
            final var result = rightClickBlock(level, hit.getBlockPos(), player, player.getItemInHand(hand));
            return result == null ? InteractionResult.PASS : result;
        });
    }

    public static void modifyDefaultComponents(final BiConsumer<ItemLike, Consumer<net.minecraft.core.component.DataComponentMap.Builder>> modify) {
        final Identifier basePunchStrengthId = Simulated.path("base_punch_strength");
        final Identifier basePunchCooldownId = Simulated.path("base_punch_cooldown");

        modify.accept(AllItems.EXTENDO_GRIP, builder -> {
            final AttributeModifier strengthModifier = new AttributeModifier(basePunchStrengthId, 10.0f, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
            final AttributeModifier cooldownModifier = new AttributeModifier(basePunchCooldownId, 0.5f, AttributeModifier.Operation.ADD_VALUE);

            builder.set(DataComponents.ATTRIBUTE_MODIFIERS, ItemAttributeModifiers.builder()
                    .add(SableAttributes.PUNCH_STRENGTH, strengthModifier, EquipmentSlotGroup.MAINHAND)
                    .add(SableAttributes.PUNCH_COOLDOWN, cooldownModifier, EquipmentSlotGroup.MAINHAND)
                    .build());
        });

        modify.accept(AllItems.CARDBOARD_SWORD, builder -> {
            final AttributeModifier attributeModifier = new AttributeModifier(basePunchStrengthId, 2.0f, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);

            builder.set(DataComponents.ATTRIBUTE_MODIFIERS, ItemAttributeModifiers.builder()
                    .add(SableAttributes.PUNCH_STRENGTH, attributeModifier, EquipmentSlotGroup.MAINHAND)
                    .build());
        });

        SimNavigationTargets.onAddDefaultComponents(modify);
    }
}
