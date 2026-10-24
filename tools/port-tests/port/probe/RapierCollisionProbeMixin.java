package port.probe;

import java.lang.reflect.InvocationTargetException;
import port.observation.RapierCollisionProbeState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** Test-only transparent tap: the native collision array is returned unchanged. */
@Mixin(targets = "dev.ryanhcode.sable.physics.impl.rapier.RapierPhysicsPipeline", remap = false)
public abstract class RapierCollisionProbeMixin {
    @Redirect(method = "processCollisionEffects", at = @At(value = "INVOKE",
            target = "Ldev/ryanhcode/sable/physics/impl/rapier/Rapier3D;clearCollisions(J)[D"), remap = false)
    private double[] port$observeNativeCollisions(long scene) {
        try {
            double[] collisions = (double[]) RapierCollisionProbeState.NATIVE_CLEAR.invoke(null, scene);
            RapierCollisionProbeState.observe(scene, collisions);
            return collisions;
        } catch (InvocationTargetException failure) {
            Throwable cause = failure.getCause();
            if (cause instanceof RuntimeException runtime) throw runtime;
            if (cause instanceof Error error) throw error;
            throw new RuntimeException(cause);
        } catch (ReflectiveOperationException failure) {
            throw new RuntimeException(failure);
        }
    }

}
