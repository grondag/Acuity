
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
	vec4 shade = shadeColor(v_light, gl_Color);

	a *= shade;


#if LAYER_COUNT > 1
	vec4 b = texture2D(u_textures, v_texcoord_1) * shadeColor(v_light, v_color_1);
	a = mix(a, b, b.a);
#endif

#if LAYER_COUNT > 2
	vec4 c = texture2D(u_textures, v_texcoord_2) * shadeColor(v_light, v_color_2);
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
