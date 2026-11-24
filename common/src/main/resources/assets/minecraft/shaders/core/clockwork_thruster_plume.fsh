#version 150

uniform float GameTime;
uniform float GlowPass;
in vec4 plumeColor;
in vec2 faceUV;
flat in vec2 faceSize;
out vec4 fragColor;

float hashCell(vec2 cell) {
    vec3 p = fract(vec3(cell.xyx) * vec3(0.1031, 0.1030, 0.0973));
    p += dot(p, p.yzx + 33.33);
    return fract((p.x + p.y) * p.z);
}

void main() {
    float face = floor(faceUV.x * 0.5);
    float side = mod(face, 4.0);
    vec2 uv = vec2(faceUV.x - 2.0 * face, faceUV.y);
    vec2 cell = min(floor(uv * faceSize), ceil(faceSize) - 1.0);
    float depth = (cell.y + 0.5) / faceSize.y;
    float frame = floor(GameTime * 12000.0);
    float seed = floor(face / 4.0) * 17.0;
    float lane = side >= 2.0 ? ceil(faceSize.x) - 1.0 - cell.x : cell.x;
    lane += mod(side, 2.0) * 7.0 + seed;

    float fuelRow = mod(cell.y - 2.0 * frame, 240.0);
    float drift = floor(3.0 * hashCell(vec2(seed, floor(fuelRow / 6.0)))) - 1.0;
    float pocket = hashCell(vec2(floor((lane + drift) / 2.0), floor(fuelRow / 6.0)));
    float grain = hashCell(vec2(lane + drift, floor(fuelRow / 3.0)));
    float fuel = 0.65 * pocket + 0.35 * grain;
    float cooling = depth + (1.0 - fuel) * (0.12 + 0.31 * depth);
    float heat = clamp(1.0 - cooling + 0.24, 0.0, 1.0);
    float coverageThreshold = 0.15 + 0.23 * hashCell(vec2(lane, floor(fuelRow / 2.0)));
    if (heat < coverageThreshold) discard;

    float band = floor(clamp((heat - 0.14) / 0.86, 0.0, 1.0) * 4.0) * 0.25;
    float layer = mod(floor(face / 4.0), 5.0);
    vec3 flame = mix(plumeColor.rgb, vec3(1.0), 0.12 + 0.10 * band);
    if (GlowPass > 0.5) {
        float emission = band * (0.48 + 0.09 * layer);
        fragColor = vec4(mix(flame, vec3(1.0), 0.42 * band), emission * plumeColor.a);
    } else {
        float core = (0.16 + 0.075 * layer) * band;
        fragColor = vec4(mix(flame, vec3(1.0), core), min(1.0, plumeColor.a * 1.7));
    }
}
