package dev.simulated_team.simulated.fabric.registry;

import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/** Existing relative From/To lists and native entity Pos retain schematic/save compatibility. */
public final class NativeHoneyGlueBounds {
    public static void write(ValueOutput output,AABB bounds,Vec3 position) {
        final var relative=bounds.move(position.scale(-1));
        output.store("From",Vec3.CODEC,relative.getMinPosition());output.store("To",Vec3.CODEC,relative.getMaxPosition());
    }
    public static AABB read(ValueInput input) {
        final var position=input.read("Pos",Vec3.CODEC).orElse(Vec3.ZERO);
        return new AABB(input.read("From",Vec3.CODEC).orElse(Vec3.ZERO),input.read("To",Vec3.CODEC).orElse(Vec3.ZERO)).move(position);
    }
    private NativeHoneyGlueBounds() { }
}
