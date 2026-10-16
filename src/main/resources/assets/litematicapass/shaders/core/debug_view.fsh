#version 330

// MODE 对应 DebugView.Mode 的序号：1 代表颜色，2 代表 schematic 深度，3 代表场景深度，4 代表最终深度，5 代表半透明深度
uniform sampler2D SchematicColor;
uniform sampler2D SchematicDepth;
uniform sampler2D MainDepth;
uniform sampler2D OpaqueDepth;
uniform vec2 OutputSize;

layout(location = 0) out vec4 fragColor;

// 1.12 的深度是标准的（近 = 0，远 = 1，定义了 STANDARD_DEPTH）；下面的比较都基于"接近度"：越大越近，远处为 0。
#ifdef STANDARD_DEPTH
float closeness(float depth) { return 1.0 - depth; }
#else
float closeness(float depth) { return depth; }
#endif

ivec2 at(sampler2D s) {
    return ivec2(gl_FragCoord.xy * vec2(textureSize(s, 0)) / OutputSize);
}

/** depth 为原始深度值；显示的是接近度，远处（清空值）为深蓝。 */
vec3 depthColor(float depth) {
    depth = closeness(depth);
    if (depth <= 0.0) {
        return vec3(0.0, 0.0, 0.15);
    }
    return vec3(pow(clamp(depth, 0.0, 1.0), 0.25));
}

void main() {
#if MODE == 1
    vec4 color = texelFetch(SchematicColor, at(SchematicColor), 0);
    fragColor = vec4(color.rgb + vec3(0.15) * (1.0 - color.a), 1.0);
#elif MODE == 2
    fragColor = vec4(depthColor(texelFetch(SchematicDepth, at(SchematicDepth), 0).r), 1.0);
#elif MODE == 3
    fragColor = vec4(depthColor(texelFetch(OpaqueDepth, at(OpaqueDepth), 0).r), 1.0);
#elif MODE == 4
    fragColor = vec4(depthColor(texelFetch(MainDepth, at(MainDepth), 0).r), 1.0);
#else
    float mainDepth = texelFetch(MainDepth, at(MainDepth), 0).r;
    float opaqueDepth = texelFetch(OpaqueDepth, at(OpaqueDepth), 0).r;
    bool translucentInFront = closeness(mainDepth) > closeness(opaqueDepth) * 1.0005;
    fragColor = vec4(translucentInFront ? depthColor(mainDepth) : vec3(0.0, 0.0, 0.15), 1.0);
#endif
}
