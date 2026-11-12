#version 150
#moj_import <clockwork_blueprint_reveal.glsl>
uniform vec2 Reveal;
uniform vec2 SketchSize;
uniform float PixelScale;
uniform float WorldPreview;
uniform sampler2D Sampler0;
in vec4 inkColor;
in vec2 sceneUV;
in float sceneDepth;
out vec4 fragColor;
void main() {
    gl_FragDepth = WorldPreview > 0.5 ? sceneDepth : gl_FragCoord.z;
    if (sceneDepth < 0.0 || sceneDepth > 1.0 || sceneDepth > texture(Sampler0, sceneUV).r + 0.00015) discard;
    float grain = fract(sin(dot(floor(gl_FragCoord.xy), vec2(127.1, 311.7))) * 43758.5453);
    fragColor = vec4(inkColor.rgb, inkColor.a * (0.94 + grain * 0.06) * blueprintReveal(sceneUV, SketchSize, PixelScale, Reveal));
}
