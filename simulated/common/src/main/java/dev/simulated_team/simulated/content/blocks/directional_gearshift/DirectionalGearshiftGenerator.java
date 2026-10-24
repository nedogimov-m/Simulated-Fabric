package dev.simulated_team.simulated.content.blocks.directional_gearshift;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import dev.simulated_team.simulated.fabric.data.NativeBlockStateJson;

/** Original144 multipart entries, including all original condition multiplicities. */
public final class DirectionalGearshiftGenerator {
    public static JsonObject generate(){
        var parts=new JsonArray();
        for(String face:new String[]{"north","south","west","east","up","down"})for(boolean axis:new boolean[]{true,false})
            for(boolean left:new boolean[]{true,false})for(boolean right:new boolean[]{true,false}){
                boolean horizontal=!face.equals("up")&&!face.equals("down");
                boolean xAxis=face.equals("east")||face.equals("west");
                String orientation=horizontal&&xAxis==axis?"vertical/":"horizontal/";
                int x=face.equals("down")?270:face.equals("up")?90:0;
                int y=horizontal?NativeBlockStateJson.angle(face):axis?0:90;
                String base="directional_gearshift/"+orientation;
                parts.add(NativeBlockStateJson.part(NativeBlockStateJson.model(base+"middle",x,y),"facing",face,"axis_along_first",String.valueOf(axis)));
                parts.add(NativeBlockStateJson.part(NativeBlockStateJson.model(base+"left"+(left?"_powered":""),x,y),"left_powered",String.valueOf(left),"facing",face,"axis_along_first",String.valueOf(axis)));
                parts.add(NativeBlockStateJson.part(NativeBlockStateJson.model(base+"right"+(right?"_powered":""),x,y),"right_powered",String.valueOf(right),"facing",face,"axis_along_first",String.valueOf(axis)));
            }
        var output=new JsonObject();output.add("multipart",parts);return output;
    }
    private DirectionalGearshiftGenerator(){}
}
