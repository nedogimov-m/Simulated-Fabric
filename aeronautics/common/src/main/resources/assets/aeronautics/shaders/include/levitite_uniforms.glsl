#ifndef AERONAUTICS_LEVITITE_UNIFORMS
#define AERONAUTICS_LEVITITE_UNIFORMS
layout(std140) uniform LevititeUniforms {
    mat4 ModelViewMat;
    mat4 ProjMat;
    mat4 InvProjMat;
    vec3 SectionOffset;
    float time;
    vec3 linearVelocity;
    int onSublevel;
    vec3 angularVelocity;
    int layerIndex;
    vec3 sublevelPosition;
    float gravityStrength;
    mat3 currentOrientation;
    float materialTransitionSpeed;
    float SkyLightScale;
    float DynamicShading;
    mat3 materialMatrixSlow;
    mat3 materialMatrixFast;
    vec2 ScreenSize;
    float LightDown;
    float LightUp;
    float LightNorth;
    float LightSouth;
    float LightWest;
    float LightEast;
};
#endif
