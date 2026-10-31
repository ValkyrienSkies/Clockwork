#version 150

#moj_import <fog.glsl>

uniform sampler2D Sampler0;
uniform vec4 ColorModulator;
uniform float FogStart;
uniform float FogEnd;
uniform vec4 FogColor;

in float vertexDistance;
in vec4 vertexColor;
in vec4 lightMapColor;
in vec4 overlayColor;
in vec2 texCoord0;
in vec4 normal;

out vec4 fragColor;

void main() {
    vec2 atlasPixels = texCoord0 * vec2(textureSize(Sampler0, 0));
    vec2 localUv = fract(atlasPixels / 16.0);
    float along = clamp(localUv.y, 0.0, 1.0);
    float across = 1.0 - abs(localUv.x * 2.0 - 1.0);
    float taper = pow(along, 1.4);
    float taperLow = clamp(pow(1 - along, 2) * 4, 0.0, 1.0);
    float softEdge = smoothstep(0.0, 0.82, across);
    float fade = clamp(vertexColor.r, 0.0, 1.0);
    float wisps = 0.82 + 0.18 * sin(along * 36.0 + sin(localUv.x * 8.0) * 2.0);
    float alpha = clamp(texture(Sampler0, texCoord0).a * taper * taperLow * softEdge * wisps * fade * 2.0, 0.0, 1.0);
    if (alpha < 0.01) {
        discard;
    }

    vec3 color = mix(vec3(0.17, 0.025, 0.42), vec3(0.78, 0.48, 1.0), along);
    fragColor = linear_fog(vec4(color * ColorModulator.rgb, alpha * ColorModulator.a), vertexDistance, FogStart, FogEnd, FogColor);
}
