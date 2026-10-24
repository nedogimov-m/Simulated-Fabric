package dev.simulated_team.simulated.ponder.outliners;

import dev.simulated_team.simulated.ponder.records.PonderLineRecord;
import net.createmod.catnip.outliner.LineOutline;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3d;
import java.util.List;

/** Interpolated Ponder line state for the unchanged Create native cuboid-outline extractor. */
public final class LerpedLineOutline extends LineOutline {
    private Vec3 prevStart,prevEnd;
    public LerpedLineOutline(PonderLineRecord initial){prevStart=initial.startPos();prevEnd=initial.endPos();set(prevStart,prevEnd);}
    public LerpedLineOutline(Vec3 initial){prevStart=prevEnd=initial;set(initial,initial);}
    public void update(Vec3 previousStart,Vec3 previousEnd,Vec3 currentStart,Vec3 currentEnd) {
        prevStart=previousStart;prevEnd=previousEnd;set(currentStart,currentEnd);
    }
    @Override public Geometry extract(Vec3 camera,float partial) {
        final var lines=params.getLineWidth()==0?List.<Segment>of():List.of(new Segment(prevStart.lerp(start,partial),prevEnd.lerp(end,partial)));
        final var geometry=geometry(lines,List.of(),List.of());
        return new Geometry(geometry.segments(),geometry.faces(),geometry.items(),geometry.color(),geometry.width(),LightCoordsUtil.FULL_BRIGHT,false);
    }
    public static Vector3d interpolatePoint(Vector3d current,Vector3d target,float partial) {
        return new Vector3d(Mth.lerp(partial,current.x,target.x),Mth.lerp(partial,current.y,target.y),Mth.lerp(partial,current.z,target.z));
    }
}
