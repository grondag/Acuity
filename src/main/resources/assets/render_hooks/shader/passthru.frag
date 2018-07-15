#version 120

//uniform float u_time;
uniform sampler2D u_textures;
//uniform sampler2D u_lightmap;

varying vec4 v_color;
varying vec2 v_texcoord;

void main()
{
	vec4 texColor = texture2D(u_textures, v_texcoord);
    gl_FragColor = texColor * v_color;
//	gl_FragColor = vec4(1.0, 1.0, 1.0, 1.0);
//	gl_FragColor = gl_FrontColor;
//	gl_FragColor = vertColor;
}


