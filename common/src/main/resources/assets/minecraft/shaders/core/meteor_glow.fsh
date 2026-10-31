#version 150

uniform sampler2D Sampler0;
uniform vec4 ColorModulator;

in vec4 vertexColor;
in vec2 texCoord0;

out vec4 fragColor;

void main() {
    vec4 texel = texture(Sampler0, texCoord0);
    if (texel.a < 0.1) {
        discard;
    }

    fragColor = vec4(vec3(0.9, 0.72, 1.1), texel.a * vertexColor.a * 0.12 * ColorModulator.a);
}
