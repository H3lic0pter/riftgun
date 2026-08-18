#version 150

uniform sampler2D Sampler0;

in vec3 tintColor;
in vec2 uv;
in float rotation;

out vec4 fragColor;

void main() {
    vec2 centered = uv - 0.5;
    // Keep the portal an ellipse: clear the corners so no rotating texture
    // bleeds past the round portal silhouette (mirrors the swirl surface).
    float edgeAlpha = 1.0 - smoothstep(0.475, 0.5, length(centered));
    if (edgeAlpha < 0.001) discard;
    float c = cos(rotation);
    float s = sin(rotation);
    vec2 rotated = vec2(centered.x * c - centered.y * s, centered.x * s + centered.y * c) + 0.5;
    vec4 tex = texture(Sampler0, rotated);
    fragColor = vec4(tex.rgb * tintColor, tex.a * edgeAlpha);
}
