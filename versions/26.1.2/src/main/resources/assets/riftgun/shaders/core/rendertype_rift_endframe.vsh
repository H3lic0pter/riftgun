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

const float TAU = 6.28318530718;

void main() {
    texCoord = UV0;
    tintColor = Color.rgb;
    // The CPU bakes the per-frame rotation angle (0..TAU) into the lightmap
    // attribute, so this shader needs no per-draw uniforms and the animation
    // settings take effect live.
    rotation = (float(UV2.x & 0xFFFF) / 65535.0) * TAU;
    gl_Position = ProjMat * ModelViewMat * vec4(Position, 1.0);
}
