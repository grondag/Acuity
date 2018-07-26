#version 120

//uniform float u_time;
uniform sampler2D u_textures;
uniform sampler2D u_lightmap;
uniform vec3 u_eye_position;
uniform vec3 u_fogColor;
uniform vec3 u_fogAttributes;

attribute vec4 in_color_0;
attribute vec2 in_uv_0;
attribute vec4 in_normal_ao;
attribute vec4 in_lightmaps;

//varying vec3 v_light;
varying float v_fogDistance;
varying vec4 v_color;
varying vec2 v_texcoord;

void setupVertex()
{
    gl_Position = ftransform(); // gl_ModelViewProjectionMatrix * gl_Vertex;
    vec4 viewCoord = gl_ModelViewMatrix * gl_Vertex;
    v_fogDistance = length(viewCoord.xyz);


    // the lightmap texture matrix is scaled to 1/256 and then offset + 8
    // it is also clamped to repeat and has linear min/mag
    vec2 lightCoord = (in_lightmaps.rg * 0.00367647) + 0.03125;
    vec4 lightColor = texture2D(u_lightmap, lightCoord);
    float ao = in_normal_ao.w / 255.0;
    float diffuse = in_lightmaps.b / 255.0;
    float glow = fract(in_lightmaps.a * 0.5) * 2.0;
    vec3 shade = max(lightColor.rgb * ao * diffuse, vec3(glow));

    v_color = vec4(in_color_0.rgb * shade, in_color_0.a);

    v_texcoord = in_uv_0;
}
