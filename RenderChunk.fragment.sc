$input v_color0, v_fog, v_normal, v_tangent, v_bitangent, v_texcoord0, v_lightmapUV, v_worldPos, v_pbrTextureId

#include <bgfx_shader.sh>

Buffer<uint4> pbrInfoArray : register(t3, space0);
SAMPLER2D(s_MatTexture,      0);
SAMPLER2D(s_LightMapTexture, 1);
SAMPLER2D(s_SeasonsTexture,  2);

void main() {
    uint pbrTextureId = uint(v_pbrTextureId);
    vec4 albedo =texture2D(s_MatTexture, v_texcoord0);

    #ifdef ALPHA_TEST
        if (albedo.a < 0.5) {
            discard;
        }
    #endif

    #ifdef SEASONS
        albedo.rgb *=
            mix(vec3(1.0, 1.0, 1.0),
                texture2D(s_SeasonsTexture, v_color0.xy).rgb * 2.0, v_color0.b);
        albedo.rgb *= v_color0.aaa;
    #else
        albedo     *= v_color0;
    #endif

    vec4 vanillaLight = Texture2D(s_LightMapTexture,
    min(BlockSkyAmbientContribution.xy * v_lightmapUV.xy, vec2(1.0, 1.0)));
    float emissive;
    float metallic;
    float roughness;
    if ((pbrInfoArray.Load((pbrTextureId * 16u) + 8u).x & 1u) ==0u)// don't have mer tex
    {
        metallic  = float(pbrInfoArray.Load((pbrTextureId * 16u) + 11u).x);
        emissive  = float(pbrInfoArray.Load((pbrTextureId * 16u) + 10u).x);
        roughness = float(pbrInfoArray.Load((pbrTextureId * 16u) +  9u).x);
    } else {
        vec4 merTex = texture2D(s_MatTexture,
            vec2(mad(v_texcoord0.x,
                    float(pbrInfoArray.Load( pbrTextureId * 16u       ).x),
                    float(pbrInfoArray.Load((pbrTextureId * 16u) +  2u).x)),
                 mad(v_texcoord0.y,
                    float(pbrInfoArray.Load((pbrTextureId * 16u) +  1u).x),
                    float(pbrInfoArray.Load((pbrTextureId * 16u) +  3u).x))));
        metallic  = merTex.x;
        emissive  = merTex.y;
        roughness = merTex.z;
    }
    vec3  GNormal      = normalize(v_normal.xyz);
    float rGNorManhLen = 1.0f / (abs(GNormal.x) + abs(GNormal.y) + abs(GNormal.z));
    float NX           = rGNorManhLen * GNormal.x;
    float NY           = rGNorManhLen * GNormal.y;
    bool  isDownFace   = GNormal.z < 0.0;
    vec3  modelCamPos  = (ViewPositionAndTime.xyz - v_worldPos.xyz);
    float camDis       = length(modelCamPos);
    vec3  viewDir      = normalize(-modelCamPos);

    gl_FragData[0].xyz = sqrt(albedo.xyz);  // Fuck YOU NVIDIA gamma 2.0
    gl_FragData[0].w   = metallic;

    gl_FragData[1].x   =
        isDownFace ? ((1.0f - abs(NY)) * ((NX >= 0.0f) ? 1.0f : (-1.0f))) : NX;
    gl_FragData[1].y   =
        isDownFace ? ((1.0f - abs(NX)) * ((NY >= 0.0f) ? 1.0f : (-1.0f))) : NY;
    gl_FragData[1].zw  = 0.0f;

    gl_FragData[2].xy  = emissive;
    gl_FragData[2].z   = dot(vec3(0.299, 0.587, 0.114), vec3(vanillaLight.xyz));
    gl_FragData[2].w   = roughness;

    gl_FragData[3].x   =
    uintBitsToFloat(((((uint(mad(GNormal.y, 0.5f, 0.5f) * 1023.0f) << 18u) & 268173312u) ^
                       (uint(mad(GNormal.x, 0.5f, 0.5f) * 1023.0f) << 22u)) ^
                      ((uint(mad(GNormal.z, 0.5f, 0.5f) * 1023.0f) << 14u) & 16760832u)) +
                        uint(dot(vec3(GNormal.x,    GNormal.y,    GNormal.z   ),
                                 vec3(v_worldPos.x, v_worldPos.y, v_worldPos.z))));
    gl_FragData[3].yzw = 0.0f;

    gl_FragData[4].xyz = v_worldPos.xyz;
    gl_FragData[4].w   = camDis;

    gl_FragData[5].xyz = v_worldPos.xyz;
    gl_FragData[5].w   = camDis;

    gl_FragData[6].xyz = viewDir;
    gl_FragData[6].w   = float(
        #ifndef SEASONS
        (albedo.a < 0.8) ||
        #endif
        ((metallic == 1.0f) && (roughness < 0.01))); //is specular
}
