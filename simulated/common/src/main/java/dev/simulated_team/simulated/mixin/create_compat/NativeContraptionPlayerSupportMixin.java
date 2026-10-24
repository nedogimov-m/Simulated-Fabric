package dev.simulated_team.simulated.mixin.create_compat;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.simibubi.create.content.contraptions.AbstractContraptionEntity;
import com.simibubi.create.content.contraptions.ContraptionCollider;
import com.simibubi.create.foundation.collision.CollisionList;
import com.simibubi.create.foundation.collision.ContinuousOBBCollider;
import com.simibubi.create.foundation.collision.Matrix3d;
import com.simibubi.create.foundation.collision.OrientedBB;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

/** Keep genuine server support across Create's end-of-sweep separation epsilon. */
@Mixin(value = ContraptionCollider.class, remap = false)
public abstract class NativeContraptionPlayerSupportMixin {
    @WrapOperation(method = "collideEntities", at = @At(value = "INVOKE", target =
            "Lcom/simibubi/create/foundation/collision/ContinuousOBBCollider;collideMany(Lcom/simibubi/create/foundation/collision/CollisionList;Lcom/simibubi/create/foundation/collision/CollisionList;Lcom/simibubi/create/foundation/collision/OrientedBB;Lnet/minecraft/world/phys/Vec3;FZ)Lcom/simibubi/create/foundation/collision/ContinuousOBBCollider$CollisionResponse;"), require = 1)
    private static ContinuousOBBCollider.CollisionResponse simulated$preserveServerSupport(
            CollisionList shapes, CollisionList dense, OrientedBB box, Vec3 motion,
            float stepHeight, boolean horizontalPass,
            Operation<ContinuousOBBCollider.CollisionResponse> original,
            @Local Entity entity, @Local(argsOnly = true) AbstractContraptionEntity owner) {
        var result = original.call(shapes, dense, box, motion, stepHeight, horizontalPass);
        if (!(entity instanceof ServerPlayer)) return result;
        return simulated$retryStandingSupport(shapes, dense, box, motion,
                owner.getRotationState().asMatrix(), result);
    }

    @Unique
    private static ContinuousOBBCollider.CollisionResponse simulated$retryStandingSupport(
            CollisionList shapes, CollisionList dense, OrientedBB box, Vec3 motion,
            Matrix3d rotation, ContinuousOBBCollider.CollisionResponse original) {
        if (original.surfaceCollision) return original;
        var relativeWorldMotion = rotation.transformTransposed(motion);
        if (relativeWorldMotion.y > 0) return original;

        // Create separates resting contacts by 1e-4m. At TOI==1 its horizontal
        // pass exits before marking support. Query only the fixed contact skin,
        // never a fall trajectory; zero step allowance also excludes wall tops.
        var probeMotion = rotation.transform(new Vec3(0, -2e-4, 0));
        var probe = ContinuousOBBCollider.collideMany(shapes, dense, box, probeMotion, 0, false);
        var worldNormal = rotation.transformTransposed(probe.normal).normalize();
        return probe.surfaceCollision && worldNormal.y > .4 ? probe : original;
    }
}
