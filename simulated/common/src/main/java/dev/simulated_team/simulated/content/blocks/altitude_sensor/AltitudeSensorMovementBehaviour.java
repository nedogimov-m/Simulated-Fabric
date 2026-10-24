package dev.simulated_team.simulated.content.blocks.altitude_sensor;

import com.simibubi.create.api.behaviour.movement.MovementBehaviour;
import com.simibubi.create.content.contraptions.behaviour.MovementContext;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.companion.math.JOMLConversion;
import org.apache.commons.lang3.tuple.Pair;

/** Common actor state only; native client extraction lives in AltitudeSensorActorRenderer. */
public class AltitudeSensorMovementBehaviour implements MovementBehaviour {
    @Override public boolean disableBlockEntityRendering() { return true; }
    @Override public void tick(MovementContext context) {
        MovementBehaviour.super.tick(context);
        if (context.position == null) return;
        final float height = (float) Sable.HELPER.projectOutOfSubLevel(context.world, JOMLConversion.toJOML(context.position)).y;
        if (context.temporaryData instanceof Pair<?, ?> heights) context.temporaryData = Pair.of(heights.getRight(), height);
        else context.temporaryData = Pair.of(height, height);
    }
}
