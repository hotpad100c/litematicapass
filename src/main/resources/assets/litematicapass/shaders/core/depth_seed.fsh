#version 330

uniform sampler2D OpaqueDepth;
uniform sampler2D NoHandDepth;
uniform vec2 OutputSize;

const float PUSH = 0.9995;

#ifdef STANDARD_DEPTH
float closeness(float depth) { return 1.0 - depth; }
float depthOf(float closeness) { return 1.0 - closeness; }
#else
float closeness(float depth) { return depth; }
float depthOf(float closeness) { return closeness; }
#endif

ivec2 at(sampler2D s) {
    return ivec2(gl_FragCoord.xy * vec2(textureSize(s, 0)) / OutputSize);
}

void main() {
    float opaque = texelFetch(OpaqueDepth, at(OpaqueDepth), 0).r;
    float noHand = texelFetch(NoHandDepth, at(NoHandDepth), 0).r;

    if (opaque != noHand) {
        gl_FragDepth = depthOf(1.0);
    } else {
        gl_FragDepth = depthOf(closeness(opaque) * PUSH);
    }
}
