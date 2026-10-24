#version 330
#extension GL_ARB_separate_shader_objects : require
#include <aeronautics:levitite_uniforms.glsl>
#include <minecraft:fog.glsl>
uniform sampler2D Sampler0;
uniform sampler2D DiffuseDepthSampler;
layout(location = 0) in float sphericalVertexDistance;
layout(location = 1) in float cylindricalVertexDistance;
layout(location = 2) in vec4 vertexColor;
layout(location = 3) in vec2 texCoord0;
layout(location = 4) in float ghostLayerFullness;
layout(location = 5) in float ghostNoiseMagnitude;
layout(location = 6) in float viewDepth;
layout(location = 0) out vec4 fragColor;
vec3 rgb2hsv(vec3 c) {
    vec4 K = vec4(0.0, -1.0 / 3.0, 2.0 / 3.0, -1.0);
    vec4 p = c.g < c.b ? vec4(c.bg, K.wz) : vec4(c.gb, K.xy);
    vec4 q = c.r < p.x ? vec4(p.xyw, c.r) : vec4(c.r, p.yzx);

    float d = q.x - min(q.w, q.y);
    float e = 1.0e-10;
    return vec3(abs(q.z + (q.w - q.y) / (6.0 * d + e)), d / (q.x + e), q.x);
}

vec3 hsv2rgb(vec3 c) {
    vec4 K = vec4(1.0, 2.0 / 3.0, 1.0 / 3.0, 3.0);
    vec3 p = abs(fract(c.xxx + K.xyz) * 6.0 - K.www);
    return c.z * mix(K.xxx, clamp(p - K.xxx, 0.0, 1.0), c.y);
}
vec3 getGhostColorHsv(vec3 hsv, bool forwards) {
    hsv += forwards ? vec3(-0.45,0,0) : vec3(0.25,0,0);
    hsv.x = mod(hsv.x+1.0,1.0);
    hsv.z = pow(hsv.z,0.8);
    return hsv;
}
void main() {
    vec4 color = texture(Sampler0,texCoord0) * vertexColor;
    vec3 hsv = rgb2hsv(color.rgb);
    if (layerIndex == 0) {
        float s = exp(-ghostNoiseMagnitude);
        hsv.z = pow(hsv.z,s);
        hsv.y = pow(hsv.y,s);
        color.rgb = hsv2rgb(hsv);
    }
    if (onSublevel > 0) {
        float ghostAlpha = ghostLayerFullness * 0.3;
        if (layerIndex == 0) {
            vec3 ghostTotal = hsv2rgb(getGhostColorHsv(hsv,false)) + hsv2rgb(getGhostColorHsv(hsv,true));
            color.rgb -= ghostTotal * ghostAlpha * 0.2;
        } else {
            color.rgb = hsv2rgb(getGhostColorHsv(hsv,layerIndex < 0));
            vec2 screenUv = gl_FragCoord.xy / ScreenSize;
            float sceneDepth = texture(DiffuseDepthSampler,screenUv).r;
            vec4 sceneView = InvProjMat * vec4(screenUv*2.0-1.0,sceneDepth,1.0);
            float sceneDistance = -sceneView.z / sceneView.w;
            float fade = smoothstep(-2.0,4.0,(sceneDistance-viewDepth)*16.0);
            if (fade < 0.001) discard;
            color.a *= ghostAlpha * fade;
        }
    }
    fragColor = apply_fog(color,sphericalVertexDistance,cylindricalVertexDistance,
            FogEnvironmentalStart,FogEnvironmentalEnd,FogRenderDistanceStart,FogRenderDistanceEnd,FogColor);
}
