package dev.simulated_team.simulated.content.blocks.handle;

import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.world.entity.player.Player;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class PlayerHoldingHandleRenderer {
    private static final Set<UUID> holdingPlayers = new HashSet<>();

    public static void updatePlayerList(final Collection<UUID> uuids) {
        holdingPlayers.clear();
        holdingPlayers.addAll(uuids);
    }

    public static void afterSetupAnim(final Player player, final HumanoidModel<?> model) {
        if (isHolding(player.getUUID()) && !Minecraft.getInstance().isPaused())
            setHangingPose(model);
    }

    public static boolean isHolding(UUID player) { return holdingPlayers.contains(player); }

    /** Applies only the state already captured for this native avatar frame. */
    public static void setHangingPose(final HumanoidModel<?> model) {
        model.leftArm.zRot = 0.0f;
        model.rightArm.zRot = 0.0f;

        model.leftArm.xRot = (float) Math.toRadians(-80.0f)+ model.head.xRot;
        model.rightArm.xRot = (float) Math.toRadians(-80.0f)+ model.head.xRot;

        model.rightArm.yRot = (float) Math.toRadians(-15);
        model.leftArm.yRot = (float) Math.toRadians(15);
    }
}
