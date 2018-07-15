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

void main()
{
    gl_Position = gl_ModelViewProjectionMatrix * gl_Vertex;

    // the lightmap texture matrix is scaled to 1/256 and then offset + 8
    // it is also clamped to repeat and has linear min/mag
    vec2 lightCoord = i_lightlevels.ra * 0.00367647 + 0.03125;
    vec4 light = texture2D(u_lightmap, lightCoord);
    v_color = vec4(gl_Color.rgb * light.rgb, 1.0);
    v_texcoord = gl_MultiTexCoord0.st;
}
