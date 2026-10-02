#version 150

#moj_import <fog.glsl>

uniform sampler2D Sampler0;
uniform float GameTime;
uniform vec4 ColorModulator;
uniform float FogStart;
uniform float FogEnd;
uniform vec4 FogColor;

in float vertexDistance;
in vec4 vertexColor;
in vec4 lightColor;
in vec2 texCoord0;
in float hitDistance;
flat in int effectMode;
out vec4 fragColor;

float noise(vec2 p) {
    return fract(sin(dot(p, vec2(127.1, 311.7))) * 43758.5453);
}

void main() {
    vec4 texel = texture(Sampler0, texCoord0);
    if (texel.a < 0.1 || vertexColor.a < 0.003) discard;
    float time = GameTime * 24000.0;
    vec2 pixel = floor(texCoord0 * 2048.0);
    vec3 rgb;
    float alpha = texel.a * vertexColor.a;
    if (effectMode == 1) {
        // Retain the actual block texture and lighting while draining its color.
        vec3 lit = texel.rgb * vertexColor.rgb * lightColor.rgb;
        float grey = dot(lit, vec3(0.2126, 0.7152, 0.0722));
        float row = noise(vec2(pixel.y, floor(time * 0.35)));
        float fizzle = step(0.975, noise(pixel + floor(time * 0.6)));
        float glitch = step(0.96, row);
        rgb = vec3(grey) * (1.0 - 0.22 * glitch) + vec3(0.12, 0.08, 0.18) * fizzle;
        alpha *= 1.0 - 0.22 * glitch;
    } else {
        // Fine interference patterns shimmer on the faces under the expanding local pulse.
        float phase = hitDistance * 3.8 - time * 0.13 + noise(pixel) * 0.35;
        float interference = pow(0.5 + 0.5 * sin(phase * 6.2831853), 6.0);
        float mote = step(0.985, noise(pixel + floor(time * 0.12)));
        float detail = dot(texel.rgb, vec3(0.2126, 0.7152, 0.0722));
        rgb = vertexColor.rgb * (0.7 + detail * 0.3) + vec3(0.18, 0.1, 0.22) * interference;
        if (effectMode == 2) {
            // Launch carries a wider, brighter impulse than the sustained grab field.
            rgb *= 1.35;
            alpha *= 0.5 + 0.65 * interference + 0.2 * mote;
        } else {
            alpha *= 0.28 + 0.55 * interference + 0.17 * mote;
        }
    }
    fragColor = linear_fog(vec4(rgb, min(alpha, 1.0)) * ColorModulator, vertexDistance, FogStart, FogEnd, FogColor);
}
