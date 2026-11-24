#version 150

in vec3 Position;
in vec4 Color;
in vec2 UV0;
in ivec2 UV2;
uniform mat4 ModelViewMat;
uniform mat4 ProjMat;
out vec4 plumeColor;
out vec2 faceUV;
flat out vec2 faceSize;

void main() {
    plumeColor = Color;
    faceUV = UV0;
    faceSize = max(vec2(UV2) / 16.0, vec2(1.0));
    gl_Position = ProjMat * ModelViewMat * vec4(Position, 1.0);
}
