#version 120

//uniform float u_time;
uniform sampler2D u_textures;
//uniform sampler2D u_lightmap;

varying vec4 vertColor;

void main()
{
	vec2 uvTex = vec2(gl_TexCoord[0]);
	vec4 texColor = texture2D(u_textures, uvTex);
    gl_FragColor = texColor * vertColor;
//	gl_FragColor = vec4(1.0, 1.0, 1.0, 1.0);
//	gl_FragColor = gl_FrontColor;
//	gl_FragColor = vertColor;
}


