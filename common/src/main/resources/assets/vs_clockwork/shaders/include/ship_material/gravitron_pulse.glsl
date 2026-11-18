int style = int(mod(params.w, 4.0));
float radius = floor(params.w * 0.25) / 64.0;
float distanceFromHit = length(shipPos - params.xyz);
float width = style == 1 ? 1.5 : (style == 2 ? 0.7 : 1.1);
float crest = 1.0 - smoothstep(0.0, width, abs(distanceFromHit - radius));
if (crest > 0.0) {
    vec3 pixel = floor(shipPos * 16.0);
    float grain = fract(sin(dot(pixel, vec3(12.9898, 78.233, 37.719))) * 43758.5453);
    float shimmer = pow(0.5 + 0.5 * sin(6.28318 * (distanceFromHit * 3.8 - radius * 0.6 + grain * 0.35)), 6.0);
    float detail = dot(color.rgb, vec3(0.2126, 0.7152, 0.0722));
    vec3 light = vec3(0.765, 0.627, 0.890) * (0.7 + 0.3 * detail) + vec3(0.18, 0.1, 0.22) * shimmer;
    float opacity = style == 1 ? 0.55 + 0.45 * shimmer : 0.32 + 0.55 * shimmer;
    color.rgb = mix(color.rgb, light * (style == 1 ? 1.35 : 1.0), opacity * crest * (style == 2 ? 0.24 : 1.0));
}
