#version 150
uniform sampler2D Sampler0;
in vec2 uv;
in vec4 tint;
in vec3 normal;
out vec4 fragColor;
void main() {
    vec4 surface = uv.x < 0.0 ? tint : texture(Sampler0, uv) * tint;
    if (surface.a < 0.1) discard;
    vec3 n = normalize(normal);
    float light = 0.42 + 0.58 * max(0.0, dot(n, normalize(vec3(-0.5, 0.8, 0.6))));
    float tone = dot(surface.rgb, vec3(0.299, 0.587, 0.114)) * light;
    fragColor = vec4(n * 0.5 + 0.5, 0.2 + tone * 0.8);
}
