#version 150
#moj_import <clockwork_blueprint_reveal.glsl>
uniform vec2 Reveal;
// Adapted from Enderkinesis strike_sketch.fsh (Apache-2.0); see META-INF/licenses/enderkinesis.txt.
// Modified for Clockwork's packed geometry, adaptive tones, and blueprint pencil strokes.
uniform sampler2D Sampler0;
uniform sampler2D Sampler1;
uniform float WorldPreview;
uniform vec2 SketchSize;
uniform float PixelScale;
uniform vec2 ToneRange;
uniform vec4 StrokeStyle;
in vec2 uv;
out vec4 fragColor;

float hash(vec2 p) { return fract(sin(dot(p, vec2(127.1, 311.7))) * 43758.5453); }
float paperNoise(vec2 p) {
    vec2 i = floor(p), f = fract(p);
    f = f * f * (3.0 - 2.0 * f);
    return mix(mix(hash(i), hash(i + vec2(1.0, 0.0)), f.x),
               mix(hash(i + vec2(0.0, 1.0)), hash(i + 1.0), f.x), f.y);
}
vec4 surface(vec2 p) {
    if (any(lessThan(p, vec2(0.0))) || any(greaterThan(p, vec2(1.0)))) return vec4(0.0);
    return texture(Sampler0, p);
}
float coverage(vec4 s) { return step(0.1, s.a); }
float tone(vec4 s) {
    float luminance = (s.a - 0.2) / 0.8;
    return mix(1.0, clamp((luminance - ToneRange.x) / max(0.25, ToneRange.y - ToneRange.x), 0.0, 1.0), coverage(s));
}
float shade(vec2 p, float radius) {
    vec2 stepSize = radius * PixelScale / SketchSize;
    float sum = 0.0;
    for (int y = -1; y <= 1; ++y) for (int x = -1; x <= 1; ++x)
        sum += tone(surface(p + vec2(x, y) * stepSize));
    return sum / 9.0;
}
vec2 wobble(vec2 p, float seed, float amount) {
    vec2 px = p * SketchSize / PixelScale;
    vec2 drift = vec2(sin(px.y * 0.047 + seed) + sin(px.y * 0.113 + seed * 2.1) * 0.35,
                      cos(px.x * 0.053 + seed * 1.7) + sin(px.x * 0.097 + seed) * 0.35);
    return p + drift * amount * PixelScale / SketchSize;
}
vec2 edges(vec2 p, float width) {
    vec4 s = surface(p);
    float body = coverage(s);
    vec3 normal = s.rgb * 2.0 - 1.0;
    vec2 radius = max(0.35, width * StrokeStyle.x * PixelScale) / SketchSize;
    float outer = 0.0, inner = 0.0;
    for (int i = 0; i < 8; ++i) {
        float angle = float(i) * 0.785398163;
        vec4 near = surface(p + vec2(cos(angle), sin(angle)) * radius);
        float covered = coverage(near);
        outer = max(outer, covered * (1.0 - body));
        inner = max(inner, smoothstep(0.25, 0.8, length(normal - (near.rgb * 2.0 - 1.0))) * body * covered);
    }
    return vec2(outer, inner);
}
float sideDark(vec2 p) {
    float darkest = 0.0;
    for (int i = 0; i < 8; ++i) {
        float angle = float(i) * 0.785398163;
        vec4 s = surface(p + vec2(cos(angle), sin(angle)) * 1.8 * PixelScale / SketchSize);
        darkest = max(darkest, (1.0 - tone(s)) * coverage(s));
    }
    return smoothstep(0.12, 0.68, darkest);
}
float detail(vec2 p) {
    vec2 d = PixelScale / SketchSize;
    float a = tone(surface(p + vec2(-d.x, -d.y))), b = tone(surface(p + vec2(0.0, -d.y))), c = tone(surface(p + vec2(d.x, -d.y)));
    float e = tone(surface(p + vec2(-d.x, 0.0))), f = tone(surface(p + vec2(d.x, 0.0)));
    float g = tone(surface(p + vec2(-d.x, d.y))), h = tone(surface(p + vec2(0.0, d.y))), j = tone(surface(p + vec2(d.x, d.y)));
    return length(vec2(c + 2.0 * f + j - a - 2.0 * e - g, g + 2.0 * h + j - a - 2.0 * b - c));
}
float hatch(vec2 p, vec2 direction, float spacing, float weight) {
    float wave = dot(p, direction) + sin(p.x * 0.065 + p.y * 0.043) * 0.4;
    float d = abs(fract(wave / spacing) - 0.5) * spacing;
    float aa = max(0.18, fwidth(wave) * 0.55);
    return 1.0 - smoothstep(weight, weight + aa, d);
}
void main() {
    float worldDepth = WorldPreview > 0.5 ? texture(Sampler1, uv).r : gl_FragCoord.z;
    if (WorldPreview > 0.5 && worldDepth >= 1.0) discard;
    gl_FragDepth = worldDepth;
    vec2 px = uv * SketchSize / PixelScale;
    vec2 at = wobble(uv, 5.0, 0.22);
    vec4 s = surface(at);
    float body = coverage(s);
    float tooth = 0.66 + 0.34 * hash(floor(uv * SketchSize));
    float rubbed = 0.78 + 0.22 * paperNoise(px / vec2(8.0, 3.0));
    float raw = smoothstep(0.08, 0.94, 1.0 - shade(at, 0.45));
    float broad = 1.0 - shade(at, 2.2);
    float family = smoothstep(0.24, 0.43, raw) * 0.42 + smoothstep(0.60, 0.78, raw) * 0.32;
    float salient = smoothstep(0.16, 0.40, abs(tone(s) - (1.0 - broad)));
    vec2 nearEdge = edges(at, 2.0);
    float interest = clamp(nearEdge.y + salient, 0.0, 1.0);
    float smudge = mix(family, raw * 0.78, 0.15 + interest * 0.75) * tooth * rubbed * body;
    smudge = max(smudge, (1.0 - tone(s)) * salient * 0.52 * tooth * body);
    float dust = broad * broad * (1.0 - body) * 0.09 * rubbed * tooth;
    float strokes = hatch(px, normalize(vec2(0.5, 0.866)), 3.1, 0.20) * smoothstep(0.40, 0.80, raw) * 0.28;
    strokes += hatch(px, normalize(vec2(-0.8, 0.6)), 4.8, 0.16) * smoothstep(0.72, 0.98, raw) * 0.16;
    smudge = max(smudge, strokes * body * tooth);

    vec2 atA = wobble(uv, 11.0, 0.48), atB = wobble(uv, 17.0, 0.85);
    float pressureA = sideDark(atA), pressureB = sideDark(atB);
    vec2 edgeA = edges(atA, mix(0.35, 0.82, pressureA));
    vec2 edgeB = edges(atB, 0.40);
    float contourA = max(edgeA.x, edgeA.y * 0.88 * StrokeStyle.y) * mix(0.25, 0.90, pressureA);
    float contourB = max(edgeB.x, edgeB.y * 0.85 * StrokeStyle.y) * mix(0.10, 0.43, pressureB);
    vec2 construction = edges(wobble(uv, 29.0, 1.55), 0.32);
    float pencil = max(contourA, contourB * StrokeStyle.z) * (0.82 + 0.18 * tooth);
    pencil = max(pencil, max(construction.x, construction.y * StrokeStyle.y) * 0.10 * StrokeStyle.z * smoothstep(0.3, 0.65, paperNoise(px / 11.0)));

    float contrast = detail(at);
    float keep = step(0.30, hash(floor(px / 2.8) + 7.0));
    float marks = smoothstep(0.9, 2.0, contrast) * 0.35 * mix(0.15, 1.0, interest);
    marks = max(marks, smoothstep(0.35, 0.95, contrast) * 0.25 * (1.0 - smoothstep(0.35, 0.7, raw)));
    float ink = max(max(smudge, dust) * StrokeStyle.w, max(pencil, marks * keep * body * tooth * StrokeStyle.y));
    fragColor = vec4(0.22, 0.23, 0.25, clamp(ink, 0.0, 0.94) * blueprintReveal(uv, SketchSize, PixelScale, Reveal));
    if (WorldPreview > 0.5) {
        fragColor = vec4(mix(vec3(0.78, 0.90, 0.93), vec3(0.16, 0.23, 0.28), clamp(ink, 0.0, 1.0)),
            clamp(0.28 * body + ink * 0.72, 0.0, 0.94));
    }
}
