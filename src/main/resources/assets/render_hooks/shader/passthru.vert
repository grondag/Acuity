#version 120

//uniform float u_time;
uniform sampler2D u_textures;
//uniform sampler2D u_lightmap;

//attribute vec4 i_normal_ao;
//attribute vec4 i_lightlevels;

//varying vec4 light;
varying vec4 v_color;
varying vec2 v_texcoord;

void main()
{
    gl_Position = gl_ModelViewProjectionMatrix * gl_Vertex;
    v_color = gl_Color;
    v_texcoord = gl_MultiTexCoord0.st;

	// first is block light, second is sky light
//	light = texture2D(lightMap, vec2((gl_MultiTexCoord1.x + 8.0) / 255.0, (gl_MultiTexCoord1.y + 8.0) / 255.0));
}
