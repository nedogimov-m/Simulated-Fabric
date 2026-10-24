package dev.simulated_team.simulated.fabric.client.gui;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.world.phys.Vec3;
import java.util.Objects;

/** Detached per-viewport shadow state, including the separate source slide-transition owner. */
public record NativePonderShadowSnapshot(float alpha,Vec3 offset) {
    public static final NativePonderShadowSnapshot DEFAULT=new NativePonderShadowSnapshot(1,Vec3.ZERO);
    public NativePonderShadowSnapshot {Objects.requireNonNull(offset);}
    public void apply(PoseStack pose){pose.translate(offset);}
    public int fade(int color){return (int)((color>>>24)*alpha)<<24|color&0x00ffffff;}
}
