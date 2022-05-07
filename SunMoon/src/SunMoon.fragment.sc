$input v_texcoord0, v_sunMoonColor

#include <bgfx_shader.sh>

SAMPLER2D(s_SunMoonTexture, 0);

void main() {
    gl_FragColor = v_sunMoonColor * texture2D(s_SunMoonTexture, v_texcoord0);
}