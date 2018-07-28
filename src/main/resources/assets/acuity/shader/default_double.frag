#version 120

uniform float u_time;
uniform sampler2D u_textures;
uniform sampler2D u_lightmap;

void main()
{
	vec2 uvTex = vec2(gl_TexCoord[0]);
	vec4 texColor = texture2D(texture, uvTex);
    gl_FragColor = texColor;
}


