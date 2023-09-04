#version 150

uniform mat4 invViewMat;
uniform mat4 invProjMat;
uniform vec3 pos;
uniform vec3 center;
uniform float radius;
uniform sampler2D depthTex;

in vec2 texCoord0;

out vec4 fragColor;

const float width = 10;
const float sharpness = 10;
const vec4 outerColor = vec4(0.855, 0.8, 1.0, 1.0);
const vec4 midColor = vec4(0.49, 0.4, 0.698, 1.0);
const vec4 innerColor = vec4(0.404,0.098,0.898, 1.0);
const vec4 scanlineColor = vec4(0.78, 0.659, 1.0, 1.0);

float scanlines() {
    return sin(gl_FragCoord.y)*0.5+0.5;
}

vec3 worldpos(float depth) {
    float z = depth * 2.0 - 1.0;
    vec4 clipSpacePosition = vec4(texCoord0 * 2.0 - 1.0, z, 1.0);
    vec4 viewSpacePosition = invProjMat * clipSpacePosition;
    viewSpacePosition /= viewSpacePosition.w;
    vec4 worldSpacePosition = invViewMat * viewSpacePosition;

    return pos + worldSpacePosition.xyz;
}

void main() {
    vec4 color = vec4(0, 0, 0, 0);

    float depth = texture2D(depthTex, texCoord0).r;
    vec3 pos = worldpos(depth);
    float dist = distance(pos, center);

    if (dist < radius && dist > radius - width && depth < 1) {
        float diff = 1.0 - (radius - dist)/width;
        vec4 edge = mix(midColor, outerColor, pow(diff, sharpness));
        color = mix(innerColor, edge, diff) + scanlines()*scanlineColor;
        color *= diff;
    }

    fragColor = color;
}
