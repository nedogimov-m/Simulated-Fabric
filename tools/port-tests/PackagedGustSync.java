import dev.eriksonn.aeronautics.content.blocks.hot_air.gust.GustEntity;
import dev.eriksonn.aeronautics.index.AeroEntityTypes;
import io.netty.buffer.Unpooled;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.protocol.game.ClientboundSetEntityDataPacket;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.joml.Quaterniond;
import org.joml.Quaterniondc;
import org.joml.Quaternionf;
import org.joml.Quaternionfc;
import org.joml.Vector3d;

import java.util.List;

/** Test-only probe using actual registered Gust entities, native saves and native entity-data packets. */
public final class PackagedGustSync {
    private record Snapshot(int id, List<SynchedEntityData.DataValue<?>> values, Quaterniond expected) { }

    public static void run(ClientGameTestContext test, TestSingleplayerContext world, List<String> checks) {
        try {
            if (!Identifier.fromNamespaceAndPath("aeronautics", "gust").equals(BuiltInRegistries.ENTITY_TYPE.getKey(AeroEntityTypes.GUST))) {
                throw new AssertionError("Gust EntityType is not registered under aeronautics:gust");
            }
            var orientationField = GustEntity.class.getDeclaredField("orientation");
            orientationField.setAccessible(true);
            var save = GustEntity.class.getDeclaredMethod("addAdditionalSaveData", ValueOutput.class);
            save.setAccessible(true);
            var load = GustEntity.class.getDeclaredMethod("readAdditionalSaveData", ValueInput.class);
            load.setAccessible(true);

            // Fabric coordinates the test, server and client threads. Schedule
            // server work from the test thread rather than blocking a render callback.
            Snapshot snapshot = world.getServer().computeOnServer(server -> {
                var level = server.overworld();
                Quaterniond expected = new Quaterniond().rotationXYZ(0.8123456789123, -0.4123456789123, 1.2123456789123).normalize();
                Quaterniond input = new Quaterniond(expected);
                var source = new GustEntity(AeroEntityTypes.GUST, level, input);
                input.identity();
                exact(expected, (Quaterniondc) orientationField.get(source), "constructor copied double precision");
                var output = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, level.registryAccess());
                save.invoke(source, output);
                CompoundTag saved = output.buildResult();
                if (!saved.contains("GustOrientation")) throw new AssertionError("Legacy GustOrientation save is missing");
                var restored = AeroEntityTypes.GUST.create(level, EntitySpawnReason.LOAD);
                if (restored == null) throw new AssertionError("Native Gust server factory returned null");
                load.invoke(restored, TagValueInput.create(ProblemReporter.DISCARDING, level.registryAccess(), saved));
                exact(expected, (Quaterniondc) orientationField.get(restored), "native save/load retained double precision");
                var values = restored.getEntityData().getNonDefaultValues();
                if (values == null || values.stream().noneMatch(value -> value.serializer() == EntityDataSerializers.QUATERNION)) {
                    throw new AssertionError("Restored Gust omitted its native quaternion entity data");
                }
                return new Snapshot(restored.getId(), values, expected);
            });
            checks.add("actual registered Gust server factory, copied double orientation and native ValueOutput/ValueInput persistence");

            test.runOnClient(client -> {
                try {
                    if (client.level == null) throw new AssertionError("Gust sync requires the real integrated world");
                    var counterpart = AeroEntityTypes.GUST.create(client.level, EntitySpawnReason.LOAD);
                    if (counterpart == null) throw new AssertionError("Native Gust client factory returned null");
                    var buffer = new RegistryFriendlyByteBuf(Unpooled.buffer(), client.level.registryAccess());
                    try {
                        ClientboundSetEntityDataPacket.STREAM_CODEC.encode(buffer,
                                new ClientboundSetEntityDataPacket(snapshot.id(), snapshot.values()));
                        var decoded = ClientboundSetEntityDataPacket.STREAM_CODEC.decode(buffer);
                        if (decoded.id() != snapshot.id() || buffer.isReadable()) throw new AssertionError("Native Gust packet boundary changed");
                        if (decoded.packedItems().stream().noneMatch(value -> value.serializer() == EntityDataSerializers.QUATERNION && value.value() instanceof Quaternionfc)) {
                            throw new AssertionError("Native quaternion serializer was not used on the wire");
                        }
                        // This is the native metadata application used by the packet
                        // listener; it invokes the actual Gust client callback.
                        counterpart.getEntityData().assignValues(decoded.packedItems());
                        Quaterniondc actual = (Quaterniondc) orientationField.get(counterpart);
                        Quaterniondc wire = new Quaterniond(new Quaternionf(snapshot.expected()));
                        if (Math.abs(actual.x()-wire.x()) > 1.0e-6 || Math.abs(actual.y()-wire.y()) > 1.0e-6
                                || Math.abs(actual.z()-wire.z()) > 1.0e-6 || Math.abs(actual.w()-wire.w()) > 1.0e-6) {
                            throw new AssertionError("Actual Gust client effects did not receive the native wire orientation: " + actual);
                        }
                        Vector3d direction = actual.transform(new Vector3d(0, 1, 0));
                        if (direction.distance(snapshot.expected().transform(new Vector3d(0, 1, 0))) > 3.0e-6) {
                            throw new AssertionError("Gust particle/impulse direction changed across the native wire");
                        }
                    } finally {
                        buffer.release();
                    }
                } catch (ReflectiveOperationException failure) {
                    throw new RuntimeException("Native Gust client metadata probe failed", failure);
                }
            });
            checks.add("actual Gust native quaternion packet encode/decode and client onSyncedDataUpdated effects direction");
        } catch (Throwable failure) {
            throw new RuntimeException("Packaged Gust sync probe failed", failure);
        }
    }

    private static void exact(Quaterniondc expected, Quaterniondc actual, String check) {
        if (Double.doubleToLongBits(expected.x()) != Double.doubleToLongBits(actual.x())
                || Double.doubleToLongBits(expected.y()) != Double.doubleToLongBits(actual.y())
                || Double.doubleToLongBits(expected.z()) != Double.doubleToLongBits(actual.z())
                || Double.doubleToLongBits(expected.w()) != Double.doubleToLongBits(actual.w())) {
            throw new AssertionError(check + ": " + actual + " != " + expected);
        }
    }
}
