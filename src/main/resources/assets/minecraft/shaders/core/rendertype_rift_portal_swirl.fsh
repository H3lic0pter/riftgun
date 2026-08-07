#version 150

in vec3 tintColor;
in vec2 uv;
in float portalPhase;
flat in int horizontalPortal;

out vec4 fragColor;

uniform sampler2D Sampler0;
uniform int AnimationEnabled;
uniform float ElapsedSeconds;
uniform float OuterPeriod;
uniform float InnerPeriod;
uniform float InwardPeriod;
uniform int InwardDirection;

const float TAU = 6.28318530718;

vec2 materialUv(vec2 portalUv) {
    if (horizontalPortal == 0) return portalUv;

    // Alpha bounds measured from the 128x128 source: x=9..118, y=11..114.
    const vec2 alphaCenter = vec2(63.5, 62.5) / 128.0;
    const vec2 alphaSpan = vec2(110.0, 104.0) / 128.0;
    return alphaCenter + (portalUv - 0.5) * alphaSpan;
}

void main() {
    vec2 centered = uv - 0.5;
    float radius = length(centered);
    float circleAlpha = 1.0 - smoothstep(0.485, 0.5, radius);
    if (circleAlpha < 0.01) discard;
    float flowShade = 0.0;

    if (AnimationEnabled != 0 && radius > 0.0001) {
        float angle = atan(centered.y, centered.x);
        float speedBlend = smoothstep(0.08, 0.55, radius);
        float innerSpeed = TAU / max(InnerPeriod, 0.1);
        float outerSpeed = TAU / max(OuterPeriod, 0.1);
        float rotation = ElapsedSeconds * mix(innerSpeed, outerSpeed, speedBlend)
            + portalPhase * TAU;
        angle += rotation;
        centered = vec2(cos(angle), sin(angle)) * radius;

        float inwardDirection = InwardDirection == 0 ? -1.0 : 1.0;
        float inwardPhase = radius * 42.0 + angle * 2.0
            + ElapsedSeconds * TAU / max(InwardPeriod, 0.1) * inwardDirection + portalPhase * TAU;
        float edgeFade = 1.0 - smoothstep(0.38, 0.5, radius);
        flowShade = sin(inwardPhase) * 0.075 * edgeFade;
    }

    vec4 material = texture(Sampler0, materialUv(centered + 0.5));
    if (material.a < 0.01) discard;

    float luminance = dot(material.rgb, vec3(0.2126, 0.7152, 0.0722));
    float shade = mix(0.42, 1.18, luminance) * (1.0 + flowShade);
    fragColor = vec4(clamp(tintColor * shade, 0.0, 1.0), material.a * circleAlpha);
}
