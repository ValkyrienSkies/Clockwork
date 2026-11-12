#version 150
uniform sampler2D Sampler0;
in vec2 uv;
in vec4 tint;
in vec3 normal;
out vec4 fragColor;
void main() {
    vec4 surface = uv.x < 0.0 ? tint : texture(Sampler0, uv) * tint;
    if (surface.a < 0.1) discard;
    float shade = 0.45 + 0.55 * max(0.0, dot(normalize(normal), normalize(vec3(-0.5, 0.8, 0.6))));
    float tone = dot(surface.rgb, vec3(0.299, 0.587, 0.114)) * shade;
    float stripe = abs(fract((gl_FragCoord.x + gl_FragCoord.y * 0.7) / 7.0) - 0.5);
    float pencil = (1.0 - smoothstep(0.05, 0.2, stripe)) * (1.0 - tone);
    fragColor = vec4(mix(vec3(0.20, 0.42, 0.53), vec3(0.66, 0.88, 0.96), tone), 0.22 + pencil * 0.26);
}
