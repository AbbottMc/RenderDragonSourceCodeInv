$input v_color0, v_starsColor

#include <bgfx_shader.sh>

void main() {
    vec4 starColor;
    starColor.w = v_color0.w;
    starColor.xyz = v_color0.xyz * v_starsColor.xyz * v_color0.w;
    gl_FragColor = starColor;
}