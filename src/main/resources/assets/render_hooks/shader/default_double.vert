#version 120

uniform float u_time;
uniform sampler2D u_textures;
uniform sampler2D u_lightmap;

//varying vec4 light;

void main()
{
    gl_Position = gl_ModelViewProjectionMatrix * gl_Vertex;
    gl_TexCoord[0] = gl_MultiTexCoord0;

	// first is block light, second is sky light
//	light = texture2D(lightMap, vec2((gl_MultiTexCoord1.x + 8.0) / 255.0, (gl_MultiTexCoord1.y + 8.0) / 255.0));
    gl_FrontColor = gl_Color;
}
