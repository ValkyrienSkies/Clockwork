#version 150
uniform sampler2D Sampler0;
uniform sampler2D Sampler1;
uniform sampler2D Sampler2;
uniform int MaskHand;
uniform vec3 NestedClip;
uniform vec4 Gate;
uniform vec4 Opening;
uniform vec4 ScreenUV;
in vec2 aperture;
out vec4 fragColor;
void main() {
    ivec2 pixel = ivec2(gl_FragCoord.xy);
    if (MaskHand != 0 && texelFetch(Sampler2, pixel, 0).r < texelFetch(Sampler1, pixel, 0).r) discard;
    vec2 uv = gl_FragCoord.xy * ScreenUV.xy + ScreenUV.zw;
    vec2 p = (aperture - 0.5) * Gate.xy;
    vec2 edge = min(aperture - Opening.xy, Opening.zw - aperture) * Gate.xy;
    if (Gate.w > 0.0) {
        if (dot(NestedClip, vec3(aperture, 1.0)) <= 0.0) discard;
        vec2 cell = (floor(p * 16.0) + 0.5) / 16.0;
        float radius = length(2.0 * cell / Gate.xy);
        float swirl = 0.5 + 0.5 * sin(atan(cell.y, cell.x) * 3.0 + radius * 8.0 - Gate.z * 0.08);
        float grain = sin(cell.y * 23.0 + Gate.z * 0.21) * sin(cell.x * 17.0 - Gate.z * 0.14);
        vec3 fill = (vec3(10, 3, 18) + (0.1 + swirl * 0.4 + grain * 0.06) * vec3(45, 13, 78)) / 255.0;
        float rim = 1.0 - smoothstep(0.0, max(0.026, fwidth(p.x) + fwidth(p.y)), min(edge.x, edge.y));
        vec3 glow = mix(vec3(0.68, 0.43, 1.0), vec3(1.0, 0.48, 0.84), Gate.w - 1.0);
        fragColor = vec4(mix(fill, glow, rim), 1.0);
        return;
    }
    vec3 scene = texture(Sampler0, uv).rgb;
    float span = min((Opening.z - Opening.x) * Gate.x, (Opening.w - Opening.y) * Gate.y);
    vec2 distanceToEdges = edge / max(span, 0.001);
    float phase = atan(p.y, p.x) * 3.0 + length(2.0 * (aperture - 0.5)) * 8.0 - Gate.z * 0.08;
    float wave = 0.5 + 0.5 * sin(phase);
    float reach = mix(0.145, 0.215, wave);
    vec2 fade = smoothstep(vec2(0.0), vec2(reach), distanceToEdges);
    float veil = 1.0 - fade.x * fade.y;
    vec3 violet = mix(vec3(0.075, 0.019, 0.14), vec3(0.27, 0.085, 0.37), wave);
    fragColor = vec4(mix(scene, violet, veil * (0.60 + 0.12 * wave)), 1.0);
}
