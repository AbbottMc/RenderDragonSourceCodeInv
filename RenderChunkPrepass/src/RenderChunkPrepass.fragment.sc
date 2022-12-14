$input v_color0, v_fog, v_normal, v_tangent, v_bitangent, v_texcoord0, v_lightmapUV, v_worldPos, v_pbrTextureId
#include <bgfx_shader.sh>

struct PBRTextureData {
    highp float colourToMaterialUvScale0;
    highp float colourToMaterialUvScale1;
    highp float colourToMaterialUvBias0;
    highp float colourToMaterialUvBias1;
    highp float colourToNormalUvScale0;
    highp float colourToNormalUvScale1;
    highp float colourToNormalUvBias0;
    highp float colourToNormalUvBias1;
    int flags;
    highp float uniformRoughness;
    highp float uniformEmissive;
    highp float uniformMetalness;
    highp float maxMipColour;
    highp float maxMipMer;
    highp float maxMipNormal;
    highp float pad;
};

layout(binding = 67, std430) readonly buffer s_PBRData {
    PBRTextureData data[];
} PBRData;

SAMPLER2D(s_MatTexture, 0);
SAMPLER2D(s_LightMapTexture, 1);
SAMPLER2D(s_SeasonsTexture, 2);

void main() {
    int pbrTextureId = v_pbrTextureId;
    vec3 worldPos = v_worldPos.xyz;
    vec4 albedo = texture2D(s_MatTexture, v_texcoord0);

#ifdef ALPHA_TEST
    if (albedo.a < 0.5) {
        discard;
    }
#endif

#ifdef SEASONS
    albedo.rgb *= mix(vec3(1.0, 1.0, 1.0), texture2D(s_SeasonsTexture, v_color0.xy).rgb * 2.0, v_color0.b);
    albedo.rgb *= v_color0.aaa;
#else
    albedo *= v_color0;
#endif

    vec4 vanillaLight =
        Texture2D(s_LightMapTexture, min(BlockSkyAmbientContribution.xy * v_lightmapUV.xy, vec2(1.0, 1.0)));
    float emissive;
    float metallic;
    float roughness;
    if ((PBRData.data[pbrTextureId].flags & 1) == 1) /*have mer texture*/ {
        vec3 merTex =
            texture2D(s_MatTexture, fma(v_texcoord0, vec2(PBRData.data[pbrTextureId].colourToMaterialUvScale0,
                                                          PBRData.data[pbrTextureId].colourToMaterialUvScale1), 
                                                     vec2(PBRData.data[pbrTextureId].colourToMaterialUvBias0, 
                                                          PBRData.data[pbrTextureId].colourToMaterialUvBias1))).xyz;
        metallic = merTex.x;
        emissive = merTex.y;
        roughness = merTex.z;
    } else {
        metallic = PBRData.data[pbrTextureId].uniformMetalness;
        emissive = PBRData.data[pbrTextureId].uniformEmissive;
        roughness = PBRData.data[pbrTextureId].uniformRoughness;
    }
    vec3 GNormal = normalize(v_normal.xyz);
    float rGNormalManhattanLength = 1.0f / (abs(GNormal.x) + abs(GNormal.y) + abs(GNormal.z));
    float NX = rGNormalManhattanLength * GNormal.x;
    float NY = rGNormalManhattanLength * GNormal.y;
    bool isDownFace = GNormal.z < 0.0;
    vec3 modelCamPos = (ViewPositionAndTime.xyz - worldPos);
    float camDis = length(modelCamPos);
    vec3 viewDir = normalize(-modelCamPos);

    ivec3 intGNormal = ivec3(fma(GNormal, vec3(0.5), vec3(0.5)) * 1023.0) & ivec3(1023);

    gl_FragData[0].xyz = sqrt(albedo.xyz);  // FUCK YOU MOJANG gamma 2.0
    gl_FragData[0].w = metallic;

    gl_FragData[1].x = isDownFace ? ((1.0f - abs(NY)) * ((NX >= 0.0f) ? 1.0f : (-1.0f))) : NX;
    gl_FragData[1].y = isDownFace ? ((1.0f - abs(NX)) * ((NY >= 0.0f) ? 1.0f : (-1.0f))) : NY;
    gl_FragData[1].zw = 0.0f;

    gl_FragData[2].xy = emissive;
    gl_FragData[2].z = dot(vec3(0.299, 0.587, 0.114), vec3(vanillaLight.xyz));
    gl_FragData[2].w = roughness;

    gl_FragData[3].x = intBitsToFloat((((0 ^ (intGNormal.x << 22)) ^ (intGNormal.y << 18)) ^ (intGNormal.z << 14)) +
                                      int(dot(GNormal, worldPos)));

    gl_FragData[3].yzw = 0.0f;

    gl_FragData[4].xyz = worldPos;
    gl_FragData[4].w = camDis;

    gl_FragData[5].xyz = worldPos;
    gl_FragData[5].w = camDis;

    gl_FragData[6].xyz = viewDir;
    gl_FragData[6].w = float(
#ifndef SEASONS
        (albedo.a < 0.8) ||
#endif
        ((metallic == 1.0) && (roughness < 0.01)));  // is specular
}
