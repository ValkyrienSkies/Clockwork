#version 150
in vec3 Position;
in vec4 Color;
in vec2 UV0;
in vec3 Normal;
uniform mat4 ModelViewMat;
uniform mat4 ProjMat;
out vec2 uv;
out vec4 tint;
out vec3 normal;
void main() {
    gl_Position = ProjMat * ModelViewMat * vec4(Position, 1.0);
    normal = normalize(mat3(ModelViewMat) * Normal);
    uv = UV0;
    tint = Color;
}
