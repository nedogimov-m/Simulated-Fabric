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
void main() {
    vec4 sampleColor=texture(Sampler0,texCoord);
    if(sampleColor.a==0.0) discard;
    vec3 sourceColor=vertexColor.rgb*0.9+0.05;
    float lightness=sampleColor.r;
    vec3 color=lightness<0.5 ? mix(sourceColor,vec3(0.24,0.184,0.29),(0.5-lightness)*2.0)
                           : mix(sourceColor,vec3(0.96,0.91,1.0),(lightness-0.5)*2.0);
    fragColor=apply_fog(vec4(color,1)*lightColor*ColorModulator,sphericalDistance, cylindricalDistance, FogEnvironmentalStart, FogEnvironmentalEnd, FogRenderDistanceStart, FogRenderDistanceEnd, FogColor);
}
