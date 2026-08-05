#version 150

in vec4 color;
in vec2 uv;

out vec4 fragColor;

uniform sampler2D Sampler0;

void main() {
    vec4 material = texture(Sampler0, uv);
    if (material.a < 0.01) discard;

    float luminance = dot(material.rgb, vec3(0.2126, 0.7152, 0.0722));
    float shade = mix(0.42, 1.18, luminance);
    fragColor = vec4(clamp(color.rgb * shade, 0.0, 1.0), material.a * color.a);
}
