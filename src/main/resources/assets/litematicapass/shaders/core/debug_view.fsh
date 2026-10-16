#version 330

uniform sampler2D SchematicColor;
uniform sampler2D SchematicDepth;
uniform sampler2D MainDepth;
uniform sampler2D OpaqueDepth;
uniform vec2 OutputSize;

layout(location = 0) out vec4 fragColor;

#ifdef STANDARD_DEPTH
float closeness(float depth) { return 1.0 - depth; }
#else
float closeness(float depth) { return depth; }
#endif

ivec2 at(sampler2D s) {
    return ivec2(gl_FragCoord.xy * vec2(textureSize(s, 0)) / OutputSize);
}

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
