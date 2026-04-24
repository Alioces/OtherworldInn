#version 150

uniform sampler2D DiffuseSampler;
uniform float FlatLightingStrength;
uniform float Contrast;
uniform float Brightness;
uniform float Saturation;
uniform float Warmth;
uniform float DitherPixelSize;
uniform float DitherStrength;
uniform float Levels;

in vec2 texCoord;

out vec4 fragColor;

float bayer8x8(ivec2 p) {
    int x = p.x & 7;
    int y = p.y & 7;
    int index = x + y * 8;
    const float table[64] = float[](
        0.0, 48.0, 12.0, 60.0, 3.0, 51.0, 15.0, 63.0,
        32.0, 16.0, 44.0, 28.0, 35.0, 19.0, 47.0, 31.0,
        8.0, 56.0, 4.0, 52.0, 11.0, 59.0, 7.0, 55.0,
        40.0, 24.0, 36.0, 20.0, 43.0, 27.0, 39.0, 23.0,
        2.0, 50.0, 14.0, 62.0, 1.0, 49.0, 13.0, 61.0,
        34.0, 18.0, 46.0, 30.0, 33.0, 17.0, 45.0, 29.0,
        10.0, 58.0, 6.0, 54.0, 9.0, 57.0, 5.0, 53.0,
        42.0, 26.0, 38.0, 22.0, 41.0, 25.0, 37.0, 21.0
    );
    return (table[index] + 0.5) / 64.0;
}

vec3 applyMapTone(vec3 color) {
    float luma = dot(color, vec3(0.299, 0.587, 0.114));

    // Keep hue relationships stable first.
    vec3 saturated = mix(vec3(luma), color, clamp(Saturation, 0.0, 1.3));

    // Gentle global white-balance warmth (no split-toning to avoid strange casts).
    vec3 warmthColor = vec3(1.0 + 0.05 * Warmth, 1.0 + 0.02 * Warmth, 1.0 - 0.035 * Warmth);
    vec3 graded = saturated * warmthColor;

    // Slightly lift deep shadows for softer map-illustration tonality.
    float lift = 0.03;
    graded = mix(vec3(luma + lift), graded, 0.92);

    return clamp(graded, 0.0, 1.0);
}

void main() {
    vec4 src = texture(DiffuseSampler, texCoord);
    vec3 toned = applyMapTone(src.rgb);

    float luma = dot(toned, vec3(0.299, 0.587, 0.114));
    float flattened = mix(luma, 0.70, clamp(FlatLightingStrength, 0.0, 1.0));
    flattened = (flattened - 0.5) * Contrast + 0.5;
    flattened = clamp(flattened + Brightness, 0.0, 1.0);

    // Keep hue/saturation while flattening luminance, but clamp ratio to avoid color shift.
    float lumaSafe = max(luma, 0.0001);
    float ratio = clamp(flattened / lumaSafe, 0.70, 1.45);
    vec3 flatColor = clamp(mix(toned, toned * ratio, 0.78), 0.0, 1.0);

    vec2 inSize = vec2(textureSize(DiffuseSampler, 0));
    float ditherPixel = max(DitherPixelSize, 1.0);
    ivec2 p = ivec2(floor((texCoord * inSize) / ditherPixel));
    float threshold = bayer8x8(p);

    // Stronger visible grain: larger dither cells + amplified threshold shift.
    float levels = max(Levels, 2.0);
    float ditherShift = (threshold - 0.5) * (DitherStrength / (levels - 1.0));
    vec3 dithered = clamp(flatColor + vec3(ditherShift), 0.0, 1.0);
    vec3 quantized = floor(dithered * (levels - 1.0) + 0.5) / (levels - 1.0);
    vec3 mapped = clamp(quantized, 0.0, 1.0);

    fragColor = vec4(mapped, src.a);
}
