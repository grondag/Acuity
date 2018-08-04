#version 120

#define LAYER_COUNT 1

uniform float u_time;
uniform sampler2D u_textures;
uniform sampler2D u_lightmap;
uniform vec3 u_eye_position;
uniform vec3 u_fogColor;
uniform vec3 u_fogAttributes;

//varying vec3 v_light;
varying float v_fogDistance;

varying vec4 v_color_0;
varying vec2 v_texcoord_0;

#if LAYER_COUNT > 1
varying vec4 v_color_1;
varying vec2 v_texcoord_1;
#endif

#if LAYER_COUNT > 2
varying vec4 v_color_2;
varying vec2 v_texcoord_2;
#endif

varying vec4 v_color_1;
varying vec2 v_texcoord_1;
varying vec4 v_color_2;
varying vec2 v_texcoord_2;

vec4 diffuseColor()
{
	vec4 a = texture2D(u_textures, v_texcoord_0) * v_color_0;

#if LAYER_COUNT > 1
	vec4 b = texture2D(u_textures, v_texcoord_1) * v_color_1;
	a = mix(a, b, b.a);
#endif

#if LAYER_COUNT > 2
	vec4 c = texture2D(u_textures, v_texcoord_2) * v_color_2;
	a = mix(a, c, c.a);
#endif

	return a;
}

vec4 diffuseColor_1()
{
	vec4 texColor = texture2D(u_textures, v_texcoord_1);
	return texColor * v_color_1;
}

vec4 diffuseColor_2()
{
	vec4 texColor = texture2D(u_textures, v_texcoord_2);
	return texColor * v_color_2;
}

/**
 * Linear fog.  Is really an inverse factor - 0 means full fog.
 */
float linearFogFactor()
{
	float fogFactor = (u_fogAttributes.x - v_fogDistance)/u_fogAttributes.y;
	return clamp( fogFactor, 0.0, 1.0 );
}

/**
 * Exponential fog.  Is really an inverse factor - 0 means full fog.
 */
float expFogFactor()
{
    float fogFactor = 1.0 / exp(v_fogDistance * u_fogAttributes.z);
    return clamp( fogFactor, 0.0, 1.0 );
}

/**
 * Returns either linear or exponential fog depending on current uniform value.
 */
float fogFactor()
{
	return u_fogAttributes.z == 0.0 ? linearFogFactor() : expFogFactor();
}

vec4 fog(vec4 diffuseColor)
{
	return mix(vec4(u_fogColor, diffuseColor.a), diffuseColor, fogFactor());
}
