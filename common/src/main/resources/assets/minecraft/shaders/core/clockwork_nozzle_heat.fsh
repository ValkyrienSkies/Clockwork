#version 150

in vec4 heatData;
in vec2 platePixels;
out vec4 fragColor;

const int bayer[16] = int[16](0, 8, 2, 10, 12, 4, 14, 6, 3, 11, 1, 9, 15, 7, 13, 5);

void main() {
    float depth = max(platePixels.y, 0.0);
    float rowScale = heatData.g > 0.5 && depth >= 8.0 ? 8.0 / 5.0 : 1.0;
    float row = floor(8.0 + (depth - 8.0) * rowScale);
    float center = 8.0 + (row + 0.5 - 8.0) / rowScale;
    float reach = 16.0 * (0.12 + heatData.b * 1.05);
    float heat = heatData.r * max(1.0 - center / reach, 0.0);
    ivec2 cell = ivec2(mod(vec2(floor(platePixels.x), row), 4.0));
    float threshold = (float(bayer[cell.y * 4 + cell.x]) + 0.5) / 16.0;
    float level = heat * 5.0;
    float midpoint = mix(0.15, 0.85, threshold);
    float band = (floor(level) + smoothstep(midpoint - 0.15, midpoint + 0.15, fract(level))) / 5.0;
    fragColor = vec4(1.0, 0.08 + 0.77 * band * band, 0.025 + 0.43 * band * band * band, band * heatData.a);
}
