#version 330
#extension GL_ARB_separate_shader_objects : require
uniform sampler2D DiffuseSampler0;
uniform sampler2D DiffuseDepthSampler;
uniform sampler2D MainSampler;
uniform sampler2D MainDepthSampler;
layout(std140) uniform PostUniforms {
    vec2 InSize;
    vec2 OutSize;
};
layout(location = 0) in vec2 texCoord;
layout(location = 0) out vec4 fragColor;
void main() {
    vec4 baseColor = texture(MainSampler, texCoord);
    // Native depth is reversed: smaller depth is behind the visible world.
    if (texture(DiffuseDepthSampler, texCoord).r < texture(MainDepthSampler, texCoord).r) {
        fragColor = baseColor;
        return;
    }
    vec3 base = baseColor.rgb;
    vec4 blendColor = texture(DiffuseSampler0, texCoord);
    vec3 blend = blendColor.rgb;
    vec3 softLit = mix(
        sqrt(base) * (2.0 * blend - 1.0) + 2.0 * base * (1.0 - blend),
        2.0 * base * blend + base * base * (1.0 - 2.0 * blend),
        step(base, vec3(0.5))
    );
    fragColor = mix(baseColor, vec4(softLit, baseColor.a), blendColor.a);
}
