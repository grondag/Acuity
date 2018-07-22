#version 120

//uniform float u_time;
uniform sampler2D u_textures;
uniform sampler2D u_lightmap;

attribute vec3 in_position;
attribute vec4 in_color_0;
attribute vec2 in_uv_0;
attribute vec4 in_normal_ao;
attribute vec4 in_lightmaps;

//varying vec4 light;
varying vec4 v_color;
varying vec2 v_texcoord;
//varying vec3 v_light;

//vec3 diffuse (vec3 normal)
//{
//	// same as Forge LightUtil.diffuse()
//	float d = min(normal.x * normal.x * 0.6 + normal.y * normal.y * ((3.0 + normal.y) / 4.0) + normal.z * normal.z * 0.8, 1.0);
//	return vec3(d, d, d);
//}

void main()
{
    gl_Position = gl_ModelViewProjectionMatrix * vec4(in_position, 1.0);
//    gl_Position = gl_ModelViewProjectionMatrix * gl_Vertex;

    // the lightmap texture matrix is scaled to 1/256 and then offset + 8
    // it is also clamped to repeat and has linear min/mag
    vec2 lightCoord = (in_lightmaps.rg * 0.00367647) + 0.03125;
    vec4 lightColor = texture2D(u_lightmap, lightCoord);
    float ao = in_normal_ao.w / 255.0;
    float diffuse = in_lightmaps.b / 255.0;
    float glow = fract(in_lightmaps.a * 0.5) * 2.0;
    vec3 shade = max(lightColor.rgb * ao * diffuse, vec3(glow));

//    vec3 shade = vec3(1.0);
    v_color = vec4(in_color_0.rgb * shade, in_color_0.a);

//    if(gl_Vertex.w == 1.0)
//    	v_color = vec4(0.0, 1.0, 0.0, 1.0);
    v_texcoord = in_uv_0;
}
