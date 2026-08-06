#version 150

in vec3 Position;
in vec4 Color;
in vec2 UV0;
in ivec2 UV2;

out vec3 tintColor;
out vec2 uv;
out float portalPhase;
flat out int horizontalPortal;

uniform mat4 ModelViewMat;
uniform mat4 ProjMat;

void main() {
    tintColor = Color.rgb;
    uv = UV0;
    portalPhase = Color.a;
    horizontalPortal = UV2.x;
    gl_Position = ProjMat * ModelViewMat * vec4(Position, 1.0);
}
