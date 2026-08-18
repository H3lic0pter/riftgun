#version 150

in vec3 Position;
in vec4 Color;
in vec2 UV0;
in ivec2 UV2;

out vec3 tintColor;
out vec2 uv;
out float rotation;

uniform mat4 ModelViewMat;
uniform mat4 ProjMat;

const float TAU = 6.28318530718;

void main() {
    tintColor = Color.rgb;
    uv = UV0;
    // The CPU bakes the per-frame rotation angle (0..TAU) into the lightmap
    // attribute, so this shader needs no per-draw uniforms and the animation
    // settings take effect live.
    rotation = (float(UV2.x & 65535) / 65535.0) * TAU;
    gl_Position = ProjMat * ModelViewMat * vec4(Position, 1.0);
}
