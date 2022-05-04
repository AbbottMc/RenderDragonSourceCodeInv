Buffer<uint4> pbrInfoArray : register(t3, space0);

SAMPLER2D(s_MatTexture, 0);
SAMPLER2D(s_LightMapTexture, 1);
SAMPLER2D(s_SeasonsTexture, 2);

void main() {
    uint pbrTextureId = uint(v_pbrTextureId);
    vec4 albedo = s_MatTexture.Sample(s_MatTextureSampler,
                                      float2(v_texcoord0.x, v_texcoord0.y));

#ifdef ALPHA_TEST
    if (albedo.a < 0.5) {
        discard;
    }
#endif

    vec4 vanillaLight = s_LightMapTexture.Sample(
        s_LightMapTextureSampler,
        min(BlockSkyAmbientContribution.xy * v_lightmapUV.xy, vec2(1.0, 1.0)));
    float emissive;
    float metallic;
    float roughness;
    if ((pbrInfoArray.Load((pbrTextureId * 16u) + 8u).x & 1u) ==
        0u)  // don't have mer tex
    {
        metallic = float(pbrInfoArray.Load((pbrTextureId * 16u) + 11u).x);
        emissive = float(pbrInfoArray.Load((pbrTextureId * 16u) + 10u).x);
        roughness = float(pbrInfoArray.Load((pbrTextureId * 16u) + 9u).x);
    } else {
        vec4 merTex = s_MatTexture.Sample(
            s_MatTextureSampler,
            float2(mad(v_texcoord0.x,
                       float(pbrInfoArray.Load(pbrTextureId * 16u).x),
                       float(pbrInfoArray.Load((pbrTextureId * 16u) + 2u).x)),
                   mad(v_texcoord0.y,
                       float(pbrInfoArray.Load((pbrTextureId * 16u) + 1u).x),
                       float(pbrInfoArray.Load((pbrTextureId * 16u) + 3u).x))));
        // where is gamma  correction ???????
        metallic = merTex.x;
        emissive = merTex.y;
        roughness = merTex.z;
    }
    vec3 normal = normalize(v_normal.xyz);
    float _209 = 1.0f / ((abs(normal.y) + abs(normal.x)) + abs(normal.z));
    float _210 = _209 * normal.x;
    float _211 = _209 * normal.y;
    bool _212 = normal.z < 0.0f;
    vec3 modelCamPos = (ViewPositionAndTime.xyz - v_worldPos.xyz);
    float camDis = length(modelCamPos);
    vec3 viewDir = normalize(-modelCamPos);
    gl_FragData[0].x = sqrt(albedo.xyz * v_color0.xyz);  // Fuck YOU NVIDIA gamma 2.0
    gl_FragData[0].w = metallic;
    gl_FragData[1].x =
        _212 ? (((_210 >= 0.0f) ? 1.0f : (-1.0f)) * (1.0f - abs(_211))) : _210;
    gl_FragData[1].y =
        _212 ? ((1.0f - abs(_210)) * ((_211 >= 0.0f) ? 1.0f : (-1.0f))) : _211;
    gl_FragData[1].zw = 0.0f;
    gl_FragData[2].xy = emissive;
    gl_FragData[2].z = dot(float3(0.299, 0.587, 0.114), float3(vanillaLight.xyz));
    gl_FragData[2].w = roughness;
    gl_FragData[3].x =
        float(((((uint(int(mad(normal.y, 0.5f, 0.5f) * 1023.0f)) << 18u) &
                 268173312u) ^
                (uint(int(mad(normal.x, 0.5f, 0.5f) * 1023.0f)) << 22u)) ^
               ((uint(int(mad(normal.z, 0.5f, 0.5f) * 1023.0f)) << 14u) &
                16760832u)) +
              uint(int(dot(float3(normal.x, normal.y, normal.z),
                           float3(v_worldPos.x, v_worldPos.y, v_worldPos.z)))));
    gl_FragData[3].yzw = 0.0f;
    gl_FragData[4].xyz = v_worldPos.xyz;
    gl_FragData[4].w = camDis;
    gl_FragData[5].xyz = v_worldPos.xyz;
    gl_FragData[5].w = camDis;
    gl_FragData[6].xyz = viewDir;
    gl_FragData[6].w = float(((_96 * v_color0.w) < 0.8) ||
                          ((metallic == 1.0f) && (roughness < 0.01)));

}