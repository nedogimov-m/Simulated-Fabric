package dev.simulated_team.simulated.content.blocks.steering_wheel;

import com.google.gson.JsonObject;
import dev.simulated_team.simulated.fabric.data.NativeBlockStateJson;

/** Regenerates the original16 native blockstate variants. */
public final class SteeringWheelGenerator {
    public static JsonObject generate(){
        var variants=new JsonObject();
        for(String face:new String[]{"east","north","south","west"})for(boolean floor:new boolean[]{false,true})for(boolean water:new boolean[]{false,true})
            variants.add("facing="+face+",on_floor="+floor+",waterlogged="+water,
                    NativeBlockStateJson.model("steering_wheel/"+(floor?"block":"block_up"),0,NativeBlockStateJson.angle(face)+180));
        var output=new JsonObject();output.add("variants",variants);return output;
    }
    private SteeringWheelGenerator(){}
}
