#version 330
#extension GL_ARB_separate_shader_objects : require
#include <minecraft:dynamictransforms.glsl>
#include <minecraft:projection.glsl>
#include <minecraft:fog.glsl>
layout(location=0) in vec3 Position;
layout(location=1) in vec4 Color;
layout(location=2) in vec2 UV0;
layout(location=3) in ivec2 UV2;
uniform sampler2D Sampler2;
layout(location=4) in vec3 Normal;
layout(std140) uniform SimulatedBlockLighting { float VeilBlockFaceBrightness[6]; };
layout(location=0) out float sphericalDistance;
layout(location=1) out float cylindricalDistance;
layout(location=2) out vec2 texCoord;
layout(location=3) out vec4 vertexColor;
layout(location=4) out vec4 lightColor;
float block_brightness(vec3 n) {
    return pow(clamp(-n.y,0.0,1.0),3.0)*VeilBlockFaceBrightness[0]
         + pow(clamp( n.y,0.0,1.0),3.0)*VeilBlockFaceBrightness[1]
         + pow(clamp(-n.z,0.0,1.0),2.0)*VeilBlockFaceBrightness[2]
         + pow(clamp( n.z,0.0,1.0),2.0)*VeilBlockFaceBrightness[3]
         + pow(clamp(-n.x,0.0,1.0),2.0)*VeilBlockFaceBrightness[4]
         + pow(clamp( n.x,0.0,1.0),2.0)*VeilBlockFaceBrightness[5];
}
void main() {
    gl_Position=ProjMat*ModelViewMat*vec4(Position,1);
    sphericalDistance=fog_spherical_distance(Position); cylindricalDistance=fog_cylindrical_distance(Position);
    texCoord=UV0; vertexColor=Color*block_brightness(Normal); lightColor=texture(Sampler2,clamp(UV2/256.0,vec2(0.5/16.0),vec2(15.5/16.0)));
}
