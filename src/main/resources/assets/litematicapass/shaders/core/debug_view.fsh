#version 330
#extension GL_ARB_separate_shader_objects : require

// MODE 对应 DebugView.Mode 的序号：1 代表颜色，2 代表 schematic 深度，3 代表场景深度，4 代表主深度，5 代表半透明深度
uniform sampler2D SchematicColor;
uniform sampler2D SchematicDepth;
uniform sampler2D MainDepth;
uniform sampler2D OpaqueDepth;

layout(location = 0) in vec2 texCoord;

layout(location = 0) out vec4 fragColor;

vec3 depthColor(float depth) {
    if (depth <= 0.0) {
        return vec3(0.0, 0.0, 0.15);
    }
    return vec3(pow(clamp(depth, 0.0, 1.0), 0.25));
}

void main() {
    ivec2 pixelCoords = ivec2(gl_FragCoord.xy);

#if MODE == 1
    vec4 color = texelFetch(SchematicColor, pixelCoords, 0);
    fragColor = vec4(color.rgb + vec3(0.15) * (1.0 - color.a), 1.0);
#elif MODE == 2
    fragColor = vec4(depthColor(texelFetch(SchematicDepth, pixelCoords, 0).r), 1.0);
#elif MODE == 3
    fragColor = vec4(depthColor(texelFetch(OpaqueDepth, pixelCoords, 0).r), 1.0);
#elif MODE == 4
    fragColor = vec4(depthColor(texelFetch(MainDepth, pixelCoords, 0).r), 1.0);
#else
    float mainDepth = texelFetch(MainDepth, pixelCoords, 0).r;
    float opaqueDepth = texelFetch(OpaqueDepth, pixelCoords, 0).r;
    bool translucentInFront = mainDepth > opaqueDepth * 1.0005;
    fragColor = vec4(translucentInFront ? depthColor(mainDepth) : vec3(0.0, 0.0, 0.15), 1.0);
#endif
}
