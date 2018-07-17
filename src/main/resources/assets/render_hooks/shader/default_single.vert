#version 120

//uniform float u_time;
uniform sampler2D u_textures;
uniform sampler2D u_lightmap;

attribute vec4 i_normal_ao;
attribute vec4 i_lightlevels;


//varying vec4 light;
varying vec4 v_color;
varying vec2 v_texcoord;
//varying vec3 v_light;

vec3 diffuse (vec3 normal)
{
	// same as Forge LightUtil.diffuse()
	float d = min(normal.x * normal.x * 0.6 + normal.y * normal.y * ((3.0 + normal.y) / 4.0) + normal.z * normal.z * 0.8, 1.0);
	return vec3(d, d, d);
}

void main()
{
    gl_Position = gl_ModelViewProjectionMatrix * gl_Vertex;

    // the lightmap texture matrix is scaled to 1/256 and then offset + 8
    // it is also clamped to repeat and has linear min/mag
    vec2 lightCoord = i_lightlevels.rg * 0.00367647 + 0.03125;
    vec4 light = texture2D(u_lightmap, lightCoord);
    vec3 t =  (i_normal_ao.xyz - 127.0) / 127.0;
    float ao = i_normal_ao.w / 255.0;
    float glow = mod(i_lightlevels.b, 16.0);

    // disable shading if glow >= 1
    vec3 shade = max(vec3(min(1, glow)), diffuse(t));

    // disable ao if actually glowing
    ao = max(min(1, glow - 1), ao);

    // clamp to minimum light from glow
    vec3 adjlight = max(light.rgb, vec3((glow - 1.0) / 14.0));
    v_color = vec4(gl_Color.rgb * adjlight * shade * ao, 1.0);

    v_texcoord = gl_MultiTexCoord0.st;
}
