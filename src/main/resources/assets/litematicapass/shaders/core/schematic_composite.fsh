#version 330

uniform sampler2D SchematicColor;
uniform sampler2D SchematicDepth;
uniform sampler2D MainDepth;
uniform sampler2D OpaqueDepth;
uniform sampler2D NoHandDepth;
uniform vec2 OutputSize;

layout(location = 0) out vec4 fragColor;

const float BEHIND_TRANSLUCENT = 0.5;

const float TRANSLUCENT_EXISTS = 1.0005;
const float BEHIND = 0.999;

#ifdef STANDARD_DEPTH
float closeness(float depth) { return 1.0 - depth; }
#else
float closeness(float depth) { return depth; }
#endif

ivec2 at(sampler2D s) {
    return ivec2(gl_FragCoord.xy * vec2(textureSize(s, 0)) / OutputSize);
}

void main() {
    vec4 color = texelFetch(SchematicColor, at(SchematicColor), 0);
    if (color.a <= 0.0) {
        discard;
    }

    float opaqueRaw = texelFetch(OpaqueDepth, at(OpaqueDepth), 0).r;

    if (opaqueRaw != texelFetch(NoHandDepth, at(NoHandDepth), 0).r) {
        discard;
    }

    float schematicDepth = closeness(texelFetch(SchematicDepth, at(SchematicDepth), 0).r);
    float mainDepth = closeness(texelFetch(MainDepth, at(MainDepth), 0).r);
    float opaqueDepth = closeness(opaqueRaw);

    bool translucentInFront = mainDepth > opaqueDepth * TRANSLUCENT_EXISTS;
    bool behindTranslucent = translucentInFront && schematicDepth < mainDepth * BEHIND;

    fragColor = behindTranslucent ? color * BEHIND_TRANSLUCENT : color;
}
