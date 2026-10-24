package dev.simulated_team.simulated.fabric.data;

import com.google.gson.JsonObject;

/** Native blockstate resource JSON; no Registrate or loader-specific model builder. */
public final class NativeBlockStateJson {
    public static JsonObject model(String path,int x,int y){
        var model=new JsonObject();model.addProperty("model","simulated:block/"+path);
        x=Math.floorMod(x,360);y=Math.floorMod(y,360);
        if(x!=0)model.addProperty("x",x);if(y!=0)model.addProperty("y",y);return model;
    }
    public static JsonObject part(JsonObject model,String... conditions){
        var when=new JsonObject();for(int i=0;i<conditions.length;i+=2)when.addProperty(conditions[i],conditions[i+1]);
        var part=new JsonObject();part.add("apply",model);part.add("when",when);return part;
    }
    public static int angle(String facing){return switch(facing){case "north"->180;case "south"->0;case "west"->90;case "east"->270;default->0;};}
    private NativeBlockStateJson(){}
}
