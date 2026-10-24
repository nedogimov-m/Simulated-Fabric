package dev.simulated_team.simulated.content.blocks.redstone.redstone_accumulator;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import dev.simulated_team.simulated.fabric.data.NativeBlockStateJson;

/** Original256 multipart entries, including duplicates from the waterlogged state loop. */
public final class RedstoneAccumulatorBlockStateGen {
    public static JsonObject generate(){
        var parts=new JsonArray();
        for(String face:new String[]{"north","south","west","east"})for(boolean powered:new boolean[]{true,false})
            for(boolean side:new boolean[]{true,false})for(boolean powering:new boolean[]{true,false})for(boolean water:new boolean[]{true,false}){
                int y=NativeBlockStateJson.angle(face)+180;String base="redstone_accumulator/";
                parts.add(NativeBlockStateJson.part(NativeBlockStateJson.model(base+"block_front",0,y),"facing",face));
                parts.add(NativeBlockStateJson.part(NativeBlockStateJson.model(base+"block_back_"+(powered?"on":"off"),0,y),"powered",String.valueOf(powered),"facing",face));
                parts.add(NativeBlockStateJson.part(NativeBlockStateJson.model(base+"block_middle_"+(side?"on":"off"),0,y),"side_powered",String.valueOf(side),"facing",face));
                parts.add(NativeBlockStateJson.part(NativeBlockStateJson.model(base+"torch_"+(powering?"on":"off"),0,y),"powering",String.valueOf(powering),"facing",face));
            }
        var output=new JsonObject();output.add("multipart",parts);return output;
    }
    private RedstoneAccumulatorBlockStateGen(){}
}
