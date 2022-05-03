$input v_color0, v_fog, v_texcoord0, v_lightmapUV, v_position

#include <bgfx_shader.sh>

uniform vec4 FogColor;
SAMPLER2D(s_MatTexture, 0);
SAMPLER2D(s_LightMapTexture, 1);
SAMPLER2D(s_SeasonsTexture, 2);

void main() {
    vec4 diffuse;

#if defined(DEPTH_ONLY_OPAQUE) || defined(DEPTH_ONLY)
    diffuse.rgb = vec3(1.0, 1.0, 1.0);
#else
    diffuse = texture2D(s_MatTexture, v_texcoord0);

    #if defined(ALPHA_TEST) || defined(DEPTH_ONLY)
        if (diffuse.a < 0.5) {
            discard;
        }
    #endif

    #if defined(SEASONS) && (defined(OPAQUE) || defined(ALPHA_TEST))
        diffuse.rgb *= mix(vec3(1.0, 1.0, 1.0), texture2D(s_SeasonsTexture, v_color0.xy).rgb * 2.0, v_color0.b);
        diffuse.rgb *= v_color0.aaa;
    #else
        diffuse *= v_color0;
    #endif
#endif

#ifndef TRANSPARENT
    diffuse.a = 1.0;
#endif

    diffuse.rgb *= texture2D(s_LightMapTexture, v_lightmapUV).rgb;

    //https://github.com/OEOTYAN/useless-shaders/blob/master/shaders/glsl/renderchunk.fragment
    vec3 chunkPos = v_position;
    vec3 cp = fract (chunkPos.xyz);
	if
	(
			((chunkPos.x < 0.0625 || chunkPos.x > 15.9375) && (chunkPos.y < 0.0625 || chunkPos.y > 15.9375)) ||
			((chunkPos.x < 0.0625 || chunkPos.x > 15.9375) && (chunkPos.z < 0.0625 || chunkPos.z > 15.9375)) ||
			((chunkPos.y < 0.0625 || chunkPos.y > 15.9375) && (chunkPos.z < 0.0625 || chunkPos.z > 15.9375))
	)
		diffuse.rgb = mix(diffuse.rgb,vec3(0.0f, 0.0f, 1.0f),0.2f);
    else if
	(
		((chunkPos.x < 0.03125 || chunkPos.x > 15.96875) || (chunkPos.z < 0.03125 || chunkPos.z > 15.96875)) &&
		(
			((cp.x < 0.03125 || cp.x > 0.96875) && (cp.y < 0.03125 || cp.y > 0.96875)) ||
			((cp.x < 0.03125 || cp.x > 0.96875) && (cp.z < 0.03125 || cp.z > 0.96875)) ||
			((cp.y < 0.03125 || cp.y > 0.96875) && (cp.z < 0.03125 || cp.z > 0.96875))
		)
	)
		diffuse.rgb = (diffuse.rgb/0.4f)*(vec3(1.0f, 1.0f, 1.0f) - diffuse.rgb);

    diffuse.rgb = mix(diffuse.rgb, v_fog.rgb, v_fog.a);
    gl_FragColor = diffuse;
}
