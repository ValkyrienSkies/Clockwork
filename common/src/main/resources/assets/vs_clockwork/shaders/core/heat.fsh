#version 150

#moj_import <fog.glsl>

uniform sampler2D Sampler0;

uniform vec4 ColorModulator;
uniform float FogStart;
uniform float FogEnd;
uniform vec4 FogColor;
uniform float Intensity;

in float vertexDistance;
in vec4 vertexColor;
in vec4 lightMapColor;
in vec4 overlayColor;
in vec2 texCoord0;
in vec4 normal;

out vec4 fragColor;

const vec3 BLACK = vec3(0.0, 0.0, 0.0);
const vec3 RED = vec3(1.0, 0.0, 0.0);
const vec3 ORANGE = vec3(1.0, 0.5, 0.0);

vec3 intensityToColor(float intensity) {
    intensity = clamp(intensity, 0.0, 100.0) / 100.0;

    if (intensity < 0.5) {
        return mix(BLACK, RED, intensity * 2.0);
    } else {
        return mix(RED, ORANGE, (intensity - 0.5) * 2.0);
    }
}

float intensityToMultiplier(float intensity) {
    intensity = clamp(intensity, 0.0, 100.0) / 100.0;

    float power = 2.0;
    float multiplier = mix(1.0, 1.5, pow(intensity, power));

    return multiplier;
}

void main() {
    vec4 color = texture(Sampler0, texCoord0);
    if (color.a < 0.1) {
        discard;
    }
    color *= vertexColor * ColorModulator;
    color.rgb = mix(overlayColor.rgb, color.rgb, overlayColor.a);
    color *= lightMapColor * intensityToMultiplier(Intensity);
    color.rgba += vec4(intensityToColor(Intensity) / 1.2f, 1);
    fragColor = linear_fog(color, vertexDistance, FogStart, FogEnd, FogColor);
}
