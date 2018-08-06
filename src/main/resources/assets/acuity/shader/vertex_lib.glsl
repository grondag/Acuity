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

void setupVertex()
{
    gl_Position = gl_ModelViewProjectionMatrix * gl_Vertex;
    vec4 viewCoord = gl_ModelViewMatrix * gl_Vertex;
    gl_ClipVertex = viewCoord;
    gl_FogFragCoord = viewCoord.z; //length(viewCoord.xyz);
    gl_TexCoord[0] = gl_MultiTexCoord0;
    gl_TexCoord[1] = gl_MultiTexCoord1;

    gl_FrontColor = gl_Color;

#if LAYER_COUNT > 1
    v_color_1 = in_color_1; //vec4(in_color_1.rgb * shade, in_color_1.a);
    v_texcoord_1 = in_uv_1;
#endif

#if LAYER_COUNT > 2
    v_color_2 = in_color_2; //vec4(in_color_2.rgb * shade, in_color_2.a);
    v_texcoord_2 = in_uv_2;
#endif
}

