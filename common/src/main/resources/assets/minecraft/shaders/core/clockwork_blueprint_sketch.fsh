#version 150
uniform sampler2D Sampler0;
uniform vec2 SketchSize;
uniform float PixelScale;
in vec2 uv;
out vec4 fragColor;
float noise(vec2 p) {
    return fract(sin(dot(p, vec2(81.37, 173.19))) * 29467.31);
}
vec4 samplePaper(vec2 p) {
    if (any(lessThan(p, vec2(0.0))) || any(greaterThan(p, vec2(1.0)))) return vec4(0.0);
    return texture(Sampler0, p);
}
float coverage(vec4 s) { return step(0.1, s.a); }
float tone(vec4 s) { return mix(1.0, clamp((s.a - 0.2) / 0.8, 0.0, 1.0), coverage(s)); }
float hatch(vec2 p, vec2 direction, float spacing, float weight) {
    float wave = dot(p, direction) + sin(p.x * 0.065 + p.y * 0.043) * 0.4;
    float d = abs(fract(wave / spacing) - 0.5) * spacing;
    float aa = max(0.18, fwidth(wave) * 0.55);
    return 1.0 - smoothstep(weight, weight + aa, d);
}
void main() {
    vec2 px = uv * SketchSize / PixelScale;
    vec2 texel = PixelScale / SketchSize;
    vec2 drift = vec2(sin(px.y * 0.11) + sin(px.y * 0.041), cos(px.x * 0.09)) * 0.23;
    vec2 at = uv + drift * texel;
    vec4 s = samplePaper(at);
    float body = coverage(s);
    float sum = tone(s) * 2.0;
    float edge = 0.0;
    float textureEdge = 0.0;
    vec3 normal = s.rgb * 2.0 - 1.0;
    for (int i = 0; i < 8; ++i) {
        float angle = float(i) * 0.785398163;
        vec2 offset = vec2(cos(angle), sin(angle)) * texel * 0.8;
        vec4 near = samplePaper(at + offset);
        float covered = coverage(near);
        sum += tone(near);
        float bend = smoothstep(0.22, 0.8, length(normal - (near.rgb * 2.0 - 1.0))) * body * covered;
        edge = max(edge, max(abs(body - covered), bend));
        textureEdge = max(textureEdge, abs(tone(s) - tone(near)) * body * covered);
    }
    float dark = 1.0 - sum / 10.0;
    float grain = noise(floor(uv * SketchSize));
    float graphite = smoothstep(0.16, 0.92, dark) * 0.43 * (0.65 + grain * 0.35);
    float strokes = hatch(px, normalize(vec2(1.0, 0.72)), 3.7, 0.22) * smoothstep(0.3, 0.75, dark) * 0.36;
    strokes += hatch(px, normalize(vec2(-0.52, 1.0)), 5.1, 0.18) * smoothstep(0.65, 0.95, dark) * 0.22;
    float pressure = 0.68 + 0.17 * sin(px.x * 0.23 + px.y * 0.15) + grain * 0.15;
    float contour = edge * pressure * 0.85;
    float details = smoothstep(0.13, 0.38, textureEdge) * 0.22 * (0.5 + grain * 0.5);
    float ink = max(contour, (graphite + strokes + details) * body);
    fragColor = vec4(0.17, 0.20, 0.23, clamp(ink, 0.0, 0.92));
}
