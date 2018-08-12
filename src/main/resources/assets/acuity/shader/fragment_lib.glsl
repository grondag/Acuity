//varying vec3 v_light;

#if LAYER_COUNT > 1
varying vec4 v_color_1;
varying vec2 v_texcoord_1;
#endif

#if LAYER_COUNT > 2
varying vec4 v_color_2;
varying vec2 v_texcoord_2;
#endif

vec4 shadeColor(vec4 lightColor, vec4 fragmentColor)
{
	float glow = fragmentColor.a >= 0.5 ? 1.0 : 0;

	const float SCALE_127_TO_255 = 2.00787401574803;
	float aOut = (fragmentColor.a - glow * 128.0) * SCALE_127_TO_255;
	vec4 colorOut = vec4(fragmentColor.rgb, aOut);
	return glow == 0.0
			? lightColor * colorOut
			: colorOut;
}

vec4 diffuseColor()
{
	// the lightmap texture matrix is scaled to 1/256 and then offset + 8
	// it is also clamped to repeat and has linear min/mag
	vec4 lightCoord0 = gl_TexCoord[1];
	vec4 lightColor = texture2D(u_lightmap, vec2((lightCoord0.x + 8.0) / 255.0, (lightCoord0.y + 8.0) / 255.0));
	lightColor = vec4(lightColor.rgb, 1.0);

	vec4 texCoord0 = gl_TexCoord[0];

#ifdef SOLID
		float non_mipped = bitValue(gl_Color.a, 1);
		vec4 a = texture2D(u_textures, texCoord0.st, non_mipped * -4.0);

		float cutout = bitValue(gl_Color.a, 0);
		if(cutout == 1.0 && a.a < 0.5)
			discard;
#else
		vec4 a = texture2D(u_textures, texCoord0.st);
#endif

	// Note in the solid layer the lower bits of gl_Color.a will be a jumble
	// of control flags but it won't matter because we'll be rendering with alpha off.
	vec4 shade = shadeColor(lightColor, gl_Color);

	a *= shade;


#if LAYER_COUNT > 1
	vec4 b = texture2D(u_textures, v_texcoord_1) * shadeColor(lightColor, v_color_1);
	a = mix(a, b, b.a);
#endif

#if LAYER_COUNT > 2
	vec4 c = texture2D(u_textures, v_texcoord_2) * shadeColor(lightColor, v_color_2);
	a = mix(a, c, c.a);
#endif

	return a;
}

/**
 * Linear fog.  Is really an inverse factor - 0 means full fog.
 */
float linearFogFactor()
{
	float fogFactor = (u_fogAttributes.x - gl_FogFragCoord)/u_fogAttributes.y;
	return clamp( fogFactor, 0.0, 1.0 );
}

/**
 * Exponential fog.  Is really an inverse factor - 0 means full fog.
 */
float expFogFactor()
{
    float fogFactor = 1.0 / exp(gl_FogFragCoord * u_fogAttributes.z);
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
