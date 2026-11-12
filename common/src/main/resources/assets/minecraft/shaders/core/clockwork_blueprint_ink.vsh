#version 150
in vec3 Position;
in vec4 Color;
in vec2 UV0;
uniform mat4 ModelViewMat;
uniform mat4 ProjMat;
uniform vec4 RenderRect;
out vec4 inkColor;
out vec2 sceneUV;
out float sceneDepth;
void main() {
    vec2 pixel = RenderRect.xy + vec2(Position.x, 1.0 - Position.y) * RenderRect.zw;
    gl_Position = ProjMat * ModelViewMat * vec4(pixel, 0.0, 1.0);
    inkColor = Color;
    sceneUV = UV0;
    sceneDepth = Position.z;
}
