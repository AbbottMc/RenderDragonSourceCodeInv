$input a_color0, a_position
$output v_color0, v_starsColor

#include <bgfx_shader.sh>

uniform vec4 StarsColor;

void main() {
    v_color0 = a_color0;
    v_starsColor = StarsColor;
    gl_Position = u_modelViewProj * vec4(a_position, 1.0);
}