#version 330
#extension GL_ARB_separate_shader_objects : require
#include <minecraft:dynamictransforms.glsl>
#include <minecraft:fog.glsl>
layout(location=0) in float sphericalDistance;
layout(location=1) in float cylindricalDistance;
layout(location=2) in vec2 texCoord;
layout(location=3) in vec4 vertexColor;
layout(location=4) in vec4 lightColor;
uniform sampler2D Sampler0;
layout(location=0) out vec4 fragColor;
layout(location=5) in vec4 stressColor;
void main() {
    vec4 color=texture(Sampler0,texCoord);
    if(color.a<0.1) discard;
    color*=vertexColor*ColorModulator;
    color.rgb=mix(color.rgb,stressColor.rgb,stressColor.a);
    fragColor=apply_fog(color*lightColor,sphericalDistance, cylindricalDistance, FogEnvironmentalStart, FogEnvironmentalEnd, FogRenderDistanceStart, FogRenderDistanceEnd, FogColor);
}
