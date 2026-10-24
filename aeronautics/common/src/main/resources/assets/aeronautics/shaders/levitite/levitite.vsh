#version 330
#extension GL_ARB_separate_shader_objects : require
#include <aeronautics:levitite_uniforms.glsl>
#include <aeronautics:levitite_utils.glsl>
#include <minecraft:sample_lightmap.glsl>
#include <minecraft:fog.glsl>
layout(location = 0) in vec3 Position;
layout(location = 1) in vec4 Color;
layout(location = 2) in vec2 UV0;
layout(location = 3) in ivec2 UV2;
layout(location = 4) in vec3 Normal;
uniform sampler2D Sampler2;
uniform sampler2D Noise;
layout(location = 0) out float sphericalVertexDistance;
layout(location = 1) out float cylindricalVertexDistance;
layout(location = 2) out vec4 vertexColor;
layout(location = 3) out vec2 texCoord0;
layout(location = 4) out float ghostLayerFullness;
layout(location = 5) out float ghostNoiseMagnitude;
layout(location = 6) out float viewDepth;
vec3 getNoiseFromPositionAndTime(vec4 p,vec3 noiseScale)
{
    //matrices divisible by 10 to allow position to tile using sufficiently high power of ten wrapping distance
    mat2 R1 = mat2(25,3,2,15)/10;
    mat2 R2 = mat2(15,1,2,-25)/10;

    mat3 M1 = mat3(
    10,2,-1,
    4,10,-5,
    -3,-2,10
    )/10;

    //linearly transform position input to avoid artificial-looking repetition
    mat3 M2 = transpose(M1);
    vec4 p1 = vec4(M1*p.xyz,p.w);
    vec4 p2 = vec4(M2*p.xyz,p.w);

    //each coordinate gets a positive and negative noise slice on the two other coordinates, offset by time
    //noise textures from https://opengameart.org/content/700-noise-textures
    vec3 noiseOutX = texture(Noise,R1*p1.zw).xyz - texture(Noise,R2*p2.yw).xyz;
    vec3 noiseOutY = texture(Noise,R1*p1.xw).xyz - texture(Noise,R2*p2.zw).xyz;
    vec3 noiseOutZ = texture(Noise,R1*p1.yw).xyz - texture(Noise,R2*p2.xw).xyz;
    mat3 mat = mat3(noiseOutX,noiseOutY,noiseOutZ);
    return transpose(mat)*noiseScale;
}

void main() {
    vec3 pos = Position;
    vec3 localPos = Position + SectionOffset;
    if (onSublevel > 0 && layerIndex != 0) {
        vec3 localVelocity = getVelocity(localPos);
        float speed = length(localVelocity);
        vec3 shift = localVelocity * (1.0-exp(-speed/max(materialTransitionSpeed, 0.000001))) / (speed+0.001) * float(layerIndex) * 0.15;
        pos += shift;
        localPos += shift;
    }
    ghostNoiseMagnitude = 0.0;
    vec3 vel = getVelocity(localPos);
    float l = onSublevel > 0 ? length(vel) : 0;
    float falloff = exp(-l/max(materialTransitionSpeed, 0.000001));
    float gravityScale = 1-exp(-gravityStrength/3.0);
    ghostLayerFullness = (1-falloff*falloff) * gravityScale;

    if(layerIndex!=0)
    {
        vec3 p = localPos;
        float t = time/200;
        p/=50;

        //drag vector has low speed component, complement of that, and lastly the transition spike
        vec3 drag = getDragState(vel);
        //these are mapped to the noise textures in the order of mid, low and high frequency
        //such that the noise is fairly mobile at low speeds, frantic at the transition and calm at higher speeds
        vec3 modifier = drag.yxz*vec3(0.4,0.3,0.2);

        vec3 nudge = getNoiseFromPositionAndTime(vec4(p,t),modifier);
        if(l>0.0001) //squish the noise offsets to move more along the direction of motion as the speed increases
            nudge = mix(vel*dot(nudge,vel)/dot(vel,vel),nudge,mix(1.0,0.1,(1-falloff)*(1-falloff)));
        nudge *= ghostLayerFullness;
        pos += nudge;
    }else
    {
        //keep slight noisy shimmer for stationary blocks, otherwise use similar scaling as the noisy vertex offsets
        vec3 drag = onSublevel > 0 ? getDragState(vel) : vec3(1,0,0);
        float t = time/500;
        vec3 localNoise = getNoiseFromPositionAndTime(vec4(localPos/10,t),vec3(1,0,0)*vec3(0.7,1,1));
        vec3 p = getGlobalPosition(localPos);
        vec3 n = getNoiseFromPositionAndTime(vec4(p/10,t),drag.yxz*vec3(0.7,1,1));
        n = mix(localNoise,n,gravityScale);
        ghostNoiseMagnitude = abs(n.x+n.y+n.z)*max(0.3,ghostLayerFullness);
    }


    vec4 viewPosition = ModelViewMat * vec4(pos, 1.0);
    gl_Position = ProjMat * viewPosition;
    // Same clip-space bias magnitude with the native reversed depth direction.
    gl_Position.z += 0.00001;
    viewDepth = -viewPosition.z;
    sphericalVertexDistance = fog_spherical_distance(viewPosition.xyz);
    cylindricalVertexDistance = fog_cylindrical_distance(viewPosition.xyz);
    vertexColor = Color * sample_lightmap(Sampler2, ivec2(vec2(UV2)*vec2(1.0,SkyLightScale)));
    if (DynamicShading > 0.5) {
        vec3 n = currentOrientation * normalize(Normal);
        vec3 negative = pow(max(-n,vec3(0.0)),vec3(3.0));
        vec3 positive = pow(max(n,vec3(0.0)),vec3(3.0));
        vertexColor.rgb *= dot(negative,vec3(LightWest,LightDown,LightNorth)) + dot(positive,vec3(LightEast,LightUp,LightSouth));
    }
    texCoord0 = UV0;
}
