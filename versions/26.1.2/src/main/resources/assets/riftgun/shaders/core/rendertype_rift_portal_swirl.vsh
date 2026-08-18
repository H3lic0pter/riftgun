#version 330

#moj_import <minecraft:dynamictransforms.glsl>
#moj_import <minecraft:projection.glsl>

in vec3 Position;
in vec4 Color;
in vec2 UV0;
in ivec2 UV2;

out vec2 texCoord;
out vec3 tintColor;
out float rotation;
flat out int mapped;

const float TAU = 6.28318530718;

void main() {
    texCoord = UV0;
    tintColor = Color.rgb;
    // The CPU bakes the per-frame rotation angle (0..TAU) and the alpha-region
    // remap flag into the lightmap attribute every frame, so the surface and glow
    // shaders need no per-draw uniforms and the animation settings take effect live.
    rotation = (float(UV2.x & 0xFFFF) / 65535.0) * TAU;
    mapped = UV2.y;
    gl_Position = ProjMat * ModelViewMat * vec4(Position, 1.0);
}
