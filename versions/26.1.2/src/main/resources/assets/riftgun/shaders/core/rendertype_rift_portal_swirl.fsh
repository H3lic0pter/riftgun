#version 330

in vec2 texCoord;
in vec3 tintColor;
in float rotation;
flat in int mapped;

out vec4 fragColor;

uniform sampler2D Sampler0;

void main() {
    vec2 centered = texCoord - 0.5;
    float c = cos(rotation);
    float s = sin(rotation);
    vec2 rotated = vec2(centered.x * c - centered.y * s, centered.x * s + centered.y * c) + 0.5;

    vec2 sampleUV = rotated;
    if (mapped == 1) {
        // Alpha bounds measured from the 128x128 source: x=9..118, y=11..114.
        const vec2 alphaCenter = vec2(63.5, 62.5) / 128.0;
        const vec2 alphaSpan = vec2(110.0, 104.0) / 128.0;
        sampleUV = alphaCenter + (rotated - 0.5) * alphaSpan;
    }

    vec4 tex = texture(Sampler0, sampleUV);
    if (tex.a < 0.1) discard;
    // Cutout surface: the texture's own circular alpha keeps the disc opaque while the
    // glow variant differs only by the additive pipeline blend, so both share this shader.
    fragColor = vec4(tex.rgb * tintColor, tex.a);
}
