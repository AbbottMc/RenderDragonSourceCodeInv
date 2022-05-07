$input a_color0, a_normal, a_tangent, a_position, a_texcoord0, a_texcoord1, a_pbrTextureId
#ifdef INSTANCING
    $input i_data0, i_data1, i_data2, i_data3
#endif
$output v_color0, v_fog, v_normal, v_tangent, v_bitangent, v_texcoord0, v_lightmapUV, v_worldPos, v_pbrTextureId

#include <bgfx_shader.sh>

uniform vec4 RenderChunkFogAlpha;
uniform vec4 FogAndDistanceControl;
uniform vec4 ViewPositionAndTime;
uniform vec4 FogColor;

void main() {
    mat4 model;
#ifdef INSTANCING
    model = mtxFromCols(i_data0, i_data1, i_data2, i_data3);
#else
    model = u_model[0]; 
#endif
    vec3 worldPos = mul(model, vec4(a_position, 1.0)).xyz;
    vec3 modelCamPos = (ViewPositionAndTime.xyz - worldPos);
    float camDis = length(modelCamPos);
    v_texcoord0 = a_texcoord0;
    v_lightmapUV = a_texcoord1;
    v_color0 = a_color0;
    v_normal = a_normal;
    v_tangent = a_tangent;
    v_bitangent = cross(a_normal.xyz, a_tangent.xyz);
    v_worldPos = worldPos;
    v_pbrTextureId = int(uint(a_pbrTextureId) & 65535u);
    v_fog.xyz = FogColor.xyz;
    v_fog.w =
        clamp(((RenderChunkFogAlpha.x - FogAndDistanceControl.x) +
               (camDis / FogAndDistanceControl.z)) /
                  (FogAndDistanceControl.y - FogAndDistanceControl.x),
              0.0, 1.0);
    gl_Position = mul(u_viewProj, vec4(worldPos, 1.0));
}
