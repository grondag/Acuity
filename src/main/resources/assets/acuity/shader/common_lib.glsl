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
