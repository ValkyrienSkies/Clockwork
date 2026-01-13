#version 150

in vec3 Position;
in vec4 Color;
in vec2 UV0;
in ivec2 UV2;
in vec3 Normal;

uniform sampler2D Sampler2;

uniform mat4 ModelViewMat;
uniform mat4 ProjMat;
uniform mat3 NormalMat;
uniform vec3 ChunkOffset;
uniform int FogShape;

out float vertexDistance;
out vec4 vertexColor;
out vec2 texCoord0;

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
}
