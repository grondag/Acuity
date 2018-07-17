#version 120

//uniform float u_time;
uniform sampler2D u_textures;
uniform sampler2D u_lightmap;

vec3 diffuse (vec3 normal)
{
	// same as Forge LightUtil.diffuse()
	float d = min(normal.x * normal.x * 0.6 + normal.y * normal.y * ((3.0 + normal.y) / 4.0) + normal.z * normal.z * 0.8, 1.0);
	return vec3(d, d, d);
}
