float blueprintReveal(vec2 uv, vec2 size, float scale, vec2 reveal) {
    if (reveal.x <= 0.0 || reveal.y >= 1.0) return 0.0;
    if (reveal.x >= 1.0 && reveal.y <= 0.0) return 1.0;
    vec2 extent = size / max(scale, 1.0);
    vec2 p = vec2(uv.x, 1.0 - uv.y) * extent;
    float tooth = fract(sin(dot(floor(p * 1.8), vec2(127.1, 311.7))) * 43758.5453);
    float diagonal = p.y + p.x * 0.22;
    float band = floor(diagonal / 6.0);
    float across = mix(p.x / extent.x, 1.0 - p.x / extent.x, mod(band, 2.0));
    float pencilOrder = (band + across * 0.86 + fract(diagonal / 6.0) * 0.14) / ceil((extent.y + extent.x * 0.22) / 6.0);
    float jitter = (tooth - 0.5) * 0.008 + sin(p.x * 0.8 + p.y * 1.6) * 0.002;
    float drawn = reveal.x >= 1.0 ? 1.0 : smoothstep(pencilOrder + jitter - 0.012, pencilOrder + jitter + 0.005, reveal.x);
    float rub = p.y - p.x * 0.14 + extent.x * 0.14;
    float row = floor(rub / 22.0);
    float stroke = mix(1.0 - p.x / extent.x, p.x / extent.x, mod(row, 2.0));
    float eraseOrder = (row + stroke * 0.8 + fract(rub / 22.0) * 0.2) / ceil((extent.y + extent.x * 0.14) / 22.0);
    float erased = reveal.y <= 0.0 ? 0.0 : smoothstep(eraseOrder - 0.045 + tooth * 0.035, eraseOrder + 0.02 + tooth * 0.025, reveal.y);
    return drawn * (1.0 - erased);
}
