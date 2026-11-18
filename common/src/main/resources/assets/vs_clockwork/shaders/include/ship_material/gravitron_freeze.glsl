float distanceFromHit = length(shipPos - params.xyz);
bool settled = params.w < 0.0;
float coverage = settled ? 1.0 : smoothstep(0.0, 1.4, params.w - distanceFromHit);
if (coverage > 0.0) {
    float phase = settled ? -params.w - 1.0 : params.w;
    vec3 pixel = floor(shipPos * 16.0);
    float row = fract(sin(dot(vec2(pixel.y, floor(phase * 0.35)), vec2(127.1, 311.7))) * 43758.5453);
    float spark = fract(sin(dot(pixel + floor(phase * 0.6), vec3(12.9898, 78.233, 37.719))) * 43758.5453);
    float grey = dot(color.rgb, vec3(0.2126, 0.7152, 0.0722));
    vec3 frozen = vec3(grey * (1.0 - 0.22 * step(0.96, row))) + vec3(0.12, 0.08, 0.18) * step(0.975, spark);
    color.rgb = mix(color.rgb, frozen, coverage * 0.88);
}
