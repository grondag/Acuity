#version 120

void main()
{
	vec4 a = diffuseColor_0();
	vec4 b = diffuseColor_1();

	a = mix(a, b, b.a);
	b = diffuseColor_2();

	a = mix(a, b, b.a);

    gl_FragColor = fog(a);
}
