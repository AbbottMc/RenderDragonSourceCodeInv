$input a_position, a_texcoord0
$output v_texcoord0, v_sunMoonColor

#include <bgfx_shader.sh>

uniform vec4 SunMoonColor;

void main() {
    v_texcoord0 = a_texcoord0;
    v_sunMoonColor = SunMoonColor;
    gl_Position = u_modelViewProj * vec4(a_position, 1.0);
}