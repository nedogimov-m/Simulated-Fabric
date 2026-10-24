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
layout(location=0) out float sphericalDistance;
layout(location=1) out float cylindricalDistance;
layout(location=2) out vec2 texCoord;
layout(location=3) out vec4 vertexColor;
layout(location=4) out vec4 lightColor;
layout(location=5) out float litFraction;
void main() {
    vec4 view=ModelViewMat*vec4(Position,1);gl_Position=ProjMat*view;
    sphericalDistance=length(view.xyz);cylindricalDistance=fog_cylindrical_distance(view.xyz);
    texCoord=UV0;vertexColor=vec4(Color.rgb,1);litFraction=Color.a;
    lightColor=texture(Sampler2,clamp(UV2/256.0,vec2(0.5/16.0),vec2(15.5/16.0)));
}
