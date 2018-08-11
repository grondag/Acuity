#version 120
#extension GL_EXT_gpu_shader4 : enable
#define LAYER_COUNT 1
#define SOLID // will be TRANSLUCENT when rendering translucent layer

uniform float u_time;
uniform sampler2D u_textures;
uniform sampler2D u_lightmap;
uniform vec3 u_eye_position;
uniform vec3 u_fogColor;
uniform vec3 u_fogAttributes;
uniform mat4 u_modelView;
uniform mat4 u_projection;
uniform mat4 u_modelViewProjection;

vec3 diffuse (vec3 normal)
{
	// same as Forge LightUtil.diffuse()
	float d = min(normal.x * normal.x * 0.6 + normal.y * normal.y * ((3.0 + normal.y) / 4.0) + normal.z * normal.z * 0.8, 1.0);
	return vec3(d, d, d);
}

// from somewhere on the Internet...
float random (vec2 st)
{
    return fract(sin(dot(st.xy,
                         vec2(12.9898,78.233)))*
        43758.5453123);
}

// Ken Perlin's improved smoothstep
float smootherstep(float edge0, float edge1, float x)
{
  // Scale, and clamp x to 0..1 range
  x = clamp((x - edge0) / (edge1 - edge0), 0.0, 1.0);
  // Evaluate polynomial
  return x * x * x * (x * (x * 6 - 15) + 10);
}

// Based in part on 2D Noise by Morgan McGuire @morgan3d
// https://www.shadertoy.com/view/4dS3Wd
float tnoise (in vec2 st, float t)
{
    vec2 i = floor(st);
    vec2 f = fract(st);

    // Compute values for four corners
    float a = random(i);
    float b = random(i + vec2(1.0, 0.0));
    float c = random(i + vec2(0.0, 1.0));
    float d = random(i + vec2(1.0, 1.0));

    a =  0.5 + sin((0.5 + a) * t) * 0.5;
    b =  0.5 + sin((0.5 + b) * t) * 0.5;
    c =  0.5 + sin((0.5 + c) * t) * 0.5;
    d =  0.5 + sin((0.5 + d) * t) * 0.5;

    // Mix 4 corners
    return mix(a, b, f.x) +
            (c - a)* f.y * (1.0 - f.x) +
            (d - b) * f.x * f.y;
}

#if GL_EXT_gpu_shader4 == 0
	const float[8] BITWISE_FLOAT_DIVISORS = float[8](127.5, 63.75, 31.875, 15.9375, 7.96875, 3.984375, 1.9921875, 0.99609375);
	const float[8] BITWISE_INT_DIVISORS = float[8](0.5, 0.25, 0.125, 0.0625, 0.03125, 0.015625, 0.0078125, 0.00390625);
#endif

/**
 * Returns the value (0-1) of the indexed bit (0-7)
 * within a normalized float value that represents a single byte (0-255).
 *
 * GLSL 120 unfortunately lacks bitwise operations
 * so we need to emulate them unless the extension is active.
 */
float bitValue(float byteValue, int bitIndex)
{
#if GL_EXT_gpu_shader4 == 1
	return (int(byteValue * 255.0) >> bitIndex) & 1;
#else
	return floor(fract(byteValue * BITWISE_FLOAT_DIVISORS[bitIndex]) * 2.0);
#endif
}

/**
 * Returns the value (0-1) of the indexed bit (0-7)
 * within the given integer.
 *
 * GLSL 120 unfortunately lacks bitwise operations
 * so we need to emulate them unless the extension is active.
 */
float bitValue(int flags, int bitIndex)
{
#if GL_EXT_gpu_shader4 == 1
	return (flags >> bitIndex) & 1;
#else
	return floor(fract(float(flags) * BITWISE_INT_DIVISORS[bitIndex]) * 2.0);
#endif
}
