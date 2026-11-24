#version 150

in vec3 Position;
in vec4 Color;
in vec2 UV0;
uniform mat4 ModelViewMat;
uniform mat4 ProjMat;
out vec4 heatData;
out vec2 platePixels;

void main() {
    heatData = Color;
    platePixels = UV0;
    gl_Position = ProjMat * ModelViewMat * vec4(Position, 1.0);
}
