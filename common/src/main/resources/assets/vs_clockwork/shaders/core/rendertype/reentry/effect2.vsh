#version 150

in vec3 Position;
in vec4 Color;
in vec2 UV0;
in ivec2 UV2;
in vec3 Normal;

uniform mat4 ModelViewMat;
uniform mat4 ProjMat;
uniform mat3 NormalMat;
uniform vec3 ChunkOffset;
uniform int FogShape;

uniform sampler2D Sampler2;

uniform vec3 Direction;
uniform float Length;
uniform float Velocity;
uniform float Strength;
uniform float Increment;
uniform float TaperSize;
uniform vec4 TrailColor;
uniform vec4 TrailColorHot;
uniform vec4 BowShockColor;
uniform float BowShockOffset;
uniform float BowShockColorLerpOffset;


const int gSubImageX = 0;
const int gSubImageY = 0;
const int gSubImageChannel = 0;
const int gSubImageXN = 0;
const int gSubImageYN = 0;
const int gSubImageChannelN = 0;
const float gNoiseLerp = 0.0; // must be float literal

out float vertexDistance;
out vec4 vertexColor;
out vec2 texCoord0;

out GS_INPUT {
    vec3 normal;
    vec3 localNormal;
    vec3 viewDir;
    vec2 noiseTexCoord0;
    vec2 noiseTexCoord1;
} gs_in;

float fog_distance(vec3 pos, int shape) {
    if (shape == 0) return length(pos);
    return max(length(vec3(pos.x, 0.0, pos.z)), pos.y);
}

vec2 minecraft_sample_lightmap_coords(ivec2 uv) {
    vec2 fuv = vec2(uv) / 256.0;
    return clamp(fuv, vec2(0.5 / 16.0), vec2(15.5 / 16.0));
}

vec4 minecraft_sample_lightmap(sampler2D lightMap, ivec2 uv) {
    return texture(lightMap, minecraft_sample_lightmap_coords(uv));
}

void main() {
    vec3 pos = Position + ChunkOffset;

    gl_Position = ProjMat * ModelViewMat * vec4(pos, 1.0);

    vertexDistance = fog_distance(pos, FogShape);
    vertexColor = Color * minecraft_sample_lightmap(Sampler2, UV2);
    texCoord0 = UV0;

    gs_in.normal = Normal;
    gs_in.localNormal = Normal;

    gs_in.viewDir = Normal;

    // Keep uniforms alive even if geometry stage is not linked / aggressive optimizer.
    float _u0 = Length + Velocity + Strength + Increment + TaperSize + BowShockOffset + BowShockColorLerpOffset;
    vec3  _u1 = Direction;
    vec4  _u2 = TrailColor + TrailColorHot + BowShockColor;

    // no-op use so compiler can't delete them
    pos += (_u1 * 0.0) + vec3((_u0 + _u2.x) * 0.0);

    float tile = 0.25;
    gs_in.noiseTexCoord0 = vec2(float(gSubImageX) * tile + UV0.x * tile,
    float(gSubImageY) * tile + UV0.y * tile);
    gs_in.noiseTexCoord1 = vec2(float(gSubImageXN) * tile + UV0.x * tile,
    float(gSubImageYN) * tile + UV0.y * tile);
}
