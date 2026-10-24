#version 330
#extension GL_ARB_separate_shader_objects : require
layout(location = 0) in vec3 Position;
layout(location = 1) in vec4 Color;
layout(location = 2) in vec2 UV0;
layout(location = 3) in ivec2 UV2;
layout(std140) uniform EndSeaTransforms {
    mat4 ProjMat;
    mat4 ModelViewMat;
    float CameraY;
    float ShadowVolumeSize;
    float StartY;
};
layout(location = 0) out vec2 texCoord;
layout(location = 1) out vec2 shadowTexCoord;
layout(location = 2) out vec4 vertexColor;
layout(location = 3) out float planeY;
void main() {
    gl_Position = ProjMat * ModelViewMat * vec4(Position, 1.0);
    vertexColor = Color;
    texCoord = UV0;
    shadowTexCoord = vec2(UV2.x, 1.0 - UV2.y);
    planeY = Position.y + CameraY;
}
