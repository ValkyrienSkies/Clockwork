#version 150
layout (vertices = 32) out;


in float vertexDistance[];
in vec4 vertexColor[];
in vec2 texCoord0[];
in TCS_INPUT {
//    vec3 worldPosition;
    vec3 normal;
    vec2 noiseTexCoord0;
    vec2 noiseTexCoord1;
} tcs_in[];

out float VertexDistance[];
out vec4 VertexColor[];
out vec2 TexCoord0[];
out TES_INPUT {
//    vec3 worldPosition;
    vec3 normal;
    vec2 noiseTexCoord0;
    vec2 noiseTexCoord1;
} tes_in[];

void main() {
    gl_out[gl_InvocationID].gl_Position = gl_in[gl_InvocationID].gl_Position;

    VertexDistance[gl_InvocationID] = vertexDistance[gl_InvocationID];
    VertexColor[gl_InvocationID] = vertexColor[gl_InvocationID];
    TexCoord0[gl_InvocationID] = TexCoord0[gl_InvocationID];
//    tes_in[gl_InvocationID].worldPosition = tcs_in[gl_InvocationID].worldPosition;
    tes_in[gl_InvocationID].normal = tcs_in[gl_InvocationID].normal;
    tes_in[gl_InvocationID].noiseTexCoord0 = tcs_in[gl_InvocationID].noiseTexCoord0;
    tes_in[gl_InvocationID].noiseTexCoord1 = tcs_in[gl_InvocationID].noiseTexCoord1;

    if (gl_InvocationID == 0) {
        gl_TessLevelOuter[0] = 2;
        gl_TessLevelOuter[1] = 2;
        gl_TessLevelOuter[2] = 2;
        gl_TessLevelOuter[3] = 2;

        gl_TessLevelInner[0] = 2;
        gl_TessLevelInner[1] = 2;
    }

}
