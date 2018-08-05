#version 120

#define LAYER_COUNT 1

uniform float u_time;
uniform sampler2D u_textures;
uniform sampler2D u_lightmap;
uniform vec3 u_eye_position;
uniform vec3 u_fogColor;
uniform vec3 u_fogAttributes;

//attribute vec4 in_normal_ao;
//attribute vec4 in_lightmaps;

//varying vec3 v_light;

#if LAYER_COUNT > 1
attribute vec4 in_color_1;
attribute vec2 in_uv_1;
varying vec4 v_color_1;
varying vec2 v_texcoord_1;
#endif

#if LAYER_COUNT > 2
attribute vec4 in_color_2;
attribute vec2 in_uv_2;
varying vec4 v_color_2;
varying vec2 v_texcoord_2;
#endif

vec4 shadeVertex(vec4 lightColor, vec4 vertexColor)
{
	float glow = vertexColor.a >= 0.5 ? 1.0 : 0;

	const float SCALE_127_TO_255 = 2.00787401574803;
	float aOut = (vertexColor.a - glow * 128.0) * SCALE_127_TO_255;
	vec4 colorOut = vec4(vertexColor.rgb, aOut);
	return glow == 0.0
			? lightColor * colorOut
			: colorOut;
}

void setupVertex()
{
    gl_Position = gl_ModelViewProjectionMatrix * gl_Vertex;
    vec4 viewCoord = gl_ModelViewMatrix * gl_Vertex;
    gl_ClipVertex = gl_Position;
    gl_FogFragCoord = length(viewCoord.xyz);
    gl_TexCoord[0] = gl_MultiTexCoord0;
    gl_TexCoord[1] = gl_MultiTexCoord1;

    // the lightmap texture matrix is scaled to 1/256 and then offset + 8
    // it is also clamped to repeat and has linear min/mag
    vec4 lightColor = texture2D(u_lightmap, vec2((gl_MultiTexCoord1.x + 8.0) / 255.0, (gl_MultiTexCoord1.y + 8.0) / 255.0));
    lightColor = vec4(lightColor.rgb, 1.0);

    gl_FrontColor = shadeVertex(lightColor, gl_Color);

#if LAYER_COUNT > 1
    v_color_1 = shadeVertex(lightColor, in_color_1); //vec4(in_color_1.rgb * shade, in_color_1.a);
    v_texcoord_1 = in_uv_1;
#endif

#if LAYER_COUNT > 2
    v_color_2 = shadeVertex(lightColor, in_color_2); //vec4(in_color_2.rgb * shade, in_color_2.a);
    v_texcoord_2 = in_uv_2;
#endif
}

