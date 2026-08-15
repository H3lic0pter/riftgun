#version 330

#moj_import <minecraft:dynamictransforms.glsl>

in vec4 color;
in vec2 uv;
in vec2 size;

out vec4 fragColor;

void main() {
    float safeU = max(abs(uv.x), 0.0001);
    float safeV = max(abs(uv.y), 0.0001);
    float pixelWidth = (1.0 / 16.0) / (1.0 + ((size.x - uv.x) / safeU));
    float pixelHeight = (1.0 / 16.0) / (1.0 + ((size.y - uv.y) / safeV));
    pixelWidth = max(abs(pixelWidth), 0.0001);
    pixelHeight = max(abs(pixelHeight), 0.0001);
    vec2 pixelUv = vec2(
        uv.x - mod(uv.x, pixelWidth) + pixelWidth * 0.5,
        uv.y - mod(uv.y, pixelHeight) + pixelHeight * 0.5
    );
    float x = 2.0 * pixelUv.x - 1.0;
    float y = 2.0 * pixelUv.y - 1.0;
    float alpha = x * x / 3.0 + y * y / 3.0;
    float bands = alpha - mod(alpha, 0.07) + 0.10;
    fragColor = vec4(color.rgb, clamp(bands, 0.08, 0.78) * ColorModulator.a);
}
