#version 330
#extension GL_ARB_separate_shader_objects : require
#include <minecraft:dynamictransforms.glsl>
#include <minecraft:projection.glsl>
#include <minecraft:fog.glsl>
#include <minecraft:light.glsl>
layout(location=0) in vec3 Position;
layout(location=1) in vec4 Color;
layout(location=2) in vec2 UV0;
layout(location=3) in ivec2 UV2;
uniform sampler2D Sampler2;
layout(location=4) in vec3 Normal;
layout(location=0) out float sphericalDistance;
layout(location=1) out float cylindricalDistance;
layout(location=2) out vec2 texCoord;
layout(location=3) out vec4 vertexColor;
layout(location=4) out vec4 lightColor;
layout(location=5) out vec4 stressColor;
void main() {
    gl_Position=ProjMat*ModelViewMat*vec4(Position,1);
    sphericalDistance=fog_spherical_distance(Position); cylindricalDistance=fog_cylindrical_distance(Position);
    texCoord=UV0; vertexColor=minecraft_mix_light(Light0_Direction,Light1_Direction,Normal,vec4(1));
    lightColor=texelFetch(Sampler2,UV2/16,0); stressColor=Color;
}
