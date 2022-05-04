Buffer<uint4> pbrInfoArray : register(t3, space0);

SAMPLER2D(s_MatTexture, 0);
SAMPLER2D(s_LightMapTexture, 1);
SAMPLER2D(s_SeasonsTexture, 2);

void main() {
    uint pbrTextureId = uint(v_pbrTextureId);
    vec4 albedo =texture2D(s_MatTexture, v_texcoord0);

#ifdef ALPHA_TEST
    if (albedo.a < 0.5) {
        discard;
    }
#endif

    vec4 vanillaLight = Texture2D(s_LightMapTexture,
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
        vec4 merTex = texture2D(s_MatTexture,
            float2(mad(v_texcoord0.x,
                       float(pbrInfoArray.Load(pbrTextureId * 16u).x),
                       float(pbrInfoArray.Load((pbrTextureId * 16u) + 2u).x)),
                   mad(v_texcoord0.y,
                       float(pbrInfoArray.Load((pbrTextureId * 16u) + 1u).x),
                       float(pbrInfoArray.Load((pbrTextureId * 16u) + 3u).x))));
        metallic = merTex.x;
        emissive = merTex.y;
        roughness = merTex.z;
    }
    vec3 normal = normalize(v_normal.xyz);
    float rNormalManhattanLength = 1.0f / (abs(normal.x) + abs(normal.y) + abs(normal.z));
    float _210 = rNormalManhattanLength * normal.x;
    float _211 = rNormalManhattanLength * normal.y;
    bool isHeightMap = normal.z < 0.0;
    vec3 modelCamPos = (ViewPositionAndTime.xyz - v_worldPos.xyz);
    float camDis = length(modelCamPos);
    vec3 viewDir = normalize(-modelCamPos);

    gl_FragData[0].xyz = sqrt(albedo.xyz * v_color0.xyz);  // Fuck YOU NVIDIA gamma 2.0
    gl_FragData[0].w = metallic;

    gl_FragData[1].x =
        isHeightMap ? (((_210 >= 0.0f) ? 1.0f : (-1.0f)) * (1.0f - abs(_211))) : _210;
    gl_FragData[1].y =
        isHeightMap ? ((1.0f - abs(_210)) * ((_211 >= 0.0f) ? 1.0f : (-1.0f))) : _211;
    gl_FragData[1].zw = 0.0f;

    gl_FragData[2].xy = emissive;
    gl_FragData[2].z = dot(float3(0.299, 0.587, 0.114), float3(vanillaLight.xyz));
    gl_FragData[2].w = roughness;

    gl_FragData[3].x =
        uintBitsToFloat(((((floatBitsToUint(mad(normal.y, 0.5f, 0.5f) * 1023.0f) << 18u) &
                 268173312u) ^
                (floatBitsToUint(mad(normal.x, 0.5f, 0.5f) * 1023.0f) << 22u)) ^
               ((floatBitsToUint(mad(normal.z, 0.5f, 0.5f) * 1023.0f) << 14u) &
                16760832u)) +
              floatBitsToUint(dot(float3(normal.x, normal.y, normal.z),
                           float3(v_worldPos.x, v_worldPos.y, v_worldPos.z))));
    gl_FragData[3].yzw = 0.0f;

    gl_FragData[4].xyz = v_worldPos.xyz;
    gl_FragData[4].w = camDis;

    gl_FragData[5].xyz = v_worldPos.xyz;
    gl_FragData[5].w = camDis;

    gl_FragData[6].xyz = viewDir;
    gl_FragData[6].w = float(((albedo.a * v_color0.a) < 0.8) ||
                          ((metallic == 1.0f) && (roughness < 0.01))); //is specular
}
