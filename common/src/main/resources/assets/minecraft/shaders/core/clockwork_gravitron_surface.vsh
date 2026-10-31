#version 150

#moj_import <fog.glsl>

in vec3 Position;
in vec4 Color;
in vec2 UV0;
in ivec2 UV1;
in ivec2 UV2;

uniform mat4 ModelViewMat;
uniform mat4 ProjMat;
uniform int FogShape;
uniform sampler2D Sampler2;

out float vertexDistance;
out vec4 vertexColor;
out vec4 lightColor;
out vec2 texCoord0;
out float hitDistance;
flat out int effectMode;

void main() {
    gl_Position = ProjMat * ModelViewMat * vec4(Position, 1.0);
    vertexDistance = fog_distance(ModelViewMat, Position, FogShape);
    vertexColor = Color;
    lightColor = texelFetch(Sampler2, UV2 / 16, 0);
    texCoord0 = UV0;
    hitDistance = float(UV1.x) / 256.0;
    effectMode = UV1.y;
}
