#version 150
#extension GL_ARB_geometry_shader4 : enable

layout (triangles) in;
layout (triangle_strip, max_vertices = 30) out;

uniform sampler2D NoiseSampler;

uniform mat4 ModelViewMat;
uniform mat4 ProjMat;
uniform mat3 NormalMat;
uniform vec3 ChunkOffset;
uniform vec3 Direction;
uniform float Length;
uniform float Velocity;
uniform float Strength;
uniform float Increment;
uniform float TaperSize;
uniform vec4 LightColor;
uniform vec4 TrailColor;
uniform vec4 TrailColorHot;
uniform vec4 BowShockColor;
uniform float BowShockOffset;
uniform float BowShockColorLerpOffset;

const vec3 gLightDirection = vec3(-.577, -.577, .577);
const float gShadowMapBias = 0.0008f;
const ivec2 gShadowMapSize = ivec2(1920, 1080);
const float gShadowStrength = 1.0;
const float gSunStrength = 0.8f;

const int gSubImageX = 0;
const int gSubImageY = 0;
const int gSubImageChannel = 0;
const int gSubImageXN = 0;
const int gSubImageYN = 0;
const int gSubImageChannelN = 0;
const float gNoiseLerp = 0;

in float vertexDistance[3];
in vec4 vertexColor[3];
in vec2 texCoord0[3];
in GS_INPUT {
    vec3 normal;
    vec3 localNormal;
    vec3 viewDir;
    vec2 noiseTexCoord0;
    vec2 noiseTexCoord1;
} gs_in[3];

out vec4 fVertexColor;

float lerp(float a, float b, float t) {
    return a + t * (b - a);
}

vec2 lerp(vec2 a, vec2 b, float t) {
    return a + t * (b - a);
}

vec3 lerp(vec3 a, vec3 b, float t) {
    return a + t * (b - a);
}

vec4 lerp(vec4 a, vec4 b, float t) {
    return a + t * (b - a);
}

float saturate(float a) {
    return clamp(a, 0.0, 1.0);
}

float getNoiseLength(vec2 tc, vec2 tcN) {
    float lengthVar = texture(NoiseSampler, tc).x;
    float lengthVarNext = texture(NoiseSampler, tcN).x;
    return lerp(lengthVarNext, lengthVar, gNoiseLerp);
}

float getNoiseLength(vec2 tc, int channel) {
    return texture(NoiseSampler, tc)[channel];
}

float fresnel(vec3 normal, vec3 viewDir, float power) {
    return pow((1.0 - saturate(dot(normalize(normal), normalize(viewDir)))), power);
}

void createVertex(vec3 pos, vec4 color) {
    gl_Position.xyz = pos;
    gl_Position.w = 1.0;
    gl_Position = ProjMat * ModelViewMat * gl_Position;
    fVertexColor = color;
    EmitVertex();
}

void createVertex(vec3 pos, vec4 color, float alpha) {
    gl_Position.xyz = pos + ChunkOffset;
    gl_Position.w = 1.0;
    gl_Position = ProjMat * ModelViewMat * gl_Position;
    fVertexColor = color;
    fVertexColor.a = alpha;
    EmitVertex();
}

void main() {

//    for (int i = 0; i < 3; i++) {
//        fVertexColor = vec4(1.0);
//        gl_Position = ProjMat * ModelViewMat * gl_in[i].gl_Position;
//        EmitVertex();
//    }
//
//    EndPrimitive();

    if (Velocity < 1000 /**|| _DisableBowshock > 0*/) return;

    // Initialize iterator variable
    int i = 0;

    // Scale the entry speed
    float clampedEntrySpeed = min(Velocity, 2300.0);
    float entrySpeed = min(Velocity / 4000.0, 0.57);
    float scaledEntrySpeed = lerp(0.0, entrySpeed + 0.55, saturate((entrySpeed - 0.32) * 2.0));

    // Get the occlusion for each vertex
    //    float3 occlusion = float3(
    //        Shadow(vertex[0].airstreamNDC, -0.003, 1),
    //        Shadow(vertex[1].airstreamNDC, -0.003, 1),
    //        Shadow(vertex[2].airstreamNDC, -0.003, 1)
    //    );

    vec3 occlusion = vec3(1.0);

    // Calculate the base effect length
    float baseLength = clampedEntrySpeed * 0.0005;

    // Sample noise
    vec3 noise = vec3(0.0);
    for (i = 0; i < 3; i++) noise[i] = getNoiseLength(gl_in[i].gl_Position.xy , 1) * baseLength * getNoiseLength(gl_in[i].gl_Position.yz, 2)/** * 10.0*/;

    // Calculate the outward effect length
    vec3 effectLength = (baseLength + noise * 0.3) * scaledEntrySpeed * 1.5;
    vec3 middleLength = effectLength * 1.5 /** * 0.54*/;

    // Calculate the forward effect length
    vec3 effectSideLength = (3 + noise) * scaledEntrySpeed * 1.25;
    vec3 middleSideLength = effectSideLength * 0.45;

    // Offset the bowshock away from the ship
    vec3 offset = /**Direction*/(Direction * 0.4 * entrySpeed) * (1.0 + BowShockOffset)/** * _ModelScale.y*/;
    vec3 trailOffset = offset * 1.1;

    // Fresnel effect
    vec3 vertFresnel = vec3(0.0);
    for (i = 0; i < 3; i++) vertFresnel[i] = fresnel(gs_in[i].normal, gs_in[i].viewDir, 1.0) * fresnel(-gs_in[i].normal, gs_in[i].viewDir, 1.0) - (fresnel(gs_in[i].normal, gs_in[i].viewDir, 3.0) - 0.3) - (1.0 - fresnel(gs_in[i].normal, gs_in[i].viewDir, 1.0) - 0.8);

    // Dot product of normal and velocity
    vec3 velDotInv = vec3(0.0);
    for (i = 0; i < 3; i++) velDotInv[i] = -dot(gs_in[i].normal, Direction);

    // Inverted dot product of normal and velocity
    vec3 velDot = velDotInv;

    // Create the "bowl"
    if (occlusion[0] > 0.9 && velDotInv[0] > 0.2)
    {
        for (i = 0; i < 3; i++)
        {
            // bowl fresnel effect
            float fresnelVal = fresnel(gs_in[i].normal, gs_in[i].viewDir, 2.0);
            float fresnelInv = fresnel(-gs_in[i].normal, gs_in[i].viewDir, 2.0);

            // Fresnel value to soften the edges
            float softFresnel = fresnel(gs_in[i].normal, gs_in[i].viewDir, 2.0);

            // bowl opacity
            float alpha = 0.1 * 0.85 /*** fresnelVal */* scaledEntrySpeed/** * (1 - softFresnel)*/ * Strength;

            // add vertex
//            createVertex(gl_in[i].gl_Position.xyz, vec4(1.0));
            createVertex(gl_in[i].gl_Position.xyz - offset, BowShockColor * velDotInv[i] + fresnelVal * 0.6, alpha);
        }

        EndPrimitive();

//        for (int i = 0; i < 3; i++) {
//            createVertex(gl_in[i].gl_Position.xyz, vec4(1.0));
//        }
//
//        EndPrimitive();

    }

    // Scale
//    effectLength *= _ModelScale.y;
//    middleLength *= _ModelScale.y;
//    middleSideLength *= _ModelScale.y;
//    effectSideLength *= _ModelScale.y;

    // make sure these values dont go negative
//    effectLength = abs(effectLength);
//    middleLength = abs(middleLength);
//    middleSideLength = abs(middleSideLength);
//    effectSideLength = abs(effectSideLength);

    // Iterate through every vertex
    for (int i = 0; i < 2/**3*/; i++) {
        if (true /**occlusion[i] > 0.9 *//**&& velDot[i] > -0.4 && velDot[i] < 0.0*//** && pow(vertFresnel[i], 2.0) > 0.2*/) {
            int j = (i + 1) % 3;  // next vertex
            int k = (j + 1) % 3;  // another vertex

            // Get the average edge length
            float edgeLength_j = length(gl_in[i].gl_Position - gl_in[j].gl_Position);
            float edgeLength_k = length(gl_in[i].gl_Position - gl_in[k].gl_Position);
            float edgeLength = ((edgeLength_j + edgeLength_k) / 2.0)/** * (1.0 / _ModelScale.y)*/;
            float edgeMul = clamp(edgeLength / 0.1, 0.1, 1.0);

            // Create the offsets which move the trail a bit inside of the bowl
            vec3 offset_i = vec3(0.0)/**vec3(gl_in[i].gl_Position.x, 0, gl_in[i].gl_Position.z) * 0.0*/;
            vec3 offset_j = vec3(0.0)/**vec3(gl_in[j].gl_Position.x, 0, gl_in[j].gl_Position.z) * 0.0*/;

            // Sample noise
            float vertNoise = 1.0/**getNoiseLength(gl_in[i].gl_Position.xy + gs_in[i].noiseTexCoord0*//** + _Time.x*//**, 0)*/;

            // Create the vector which will be used to widen the trail segments
            vec3 sizeVector = -normalize(cross(Direction, gs_in[i].localNormal));

            // Width
            vec3 side = sizeVector * 1.5;  // vector pointing to the side, which allows for thicker trails
            vec3 middleSide = side * 2.0;
            vec3 endSide = side * 2.0;

            // Scale
//            side *= _ModelScale.x;
//            middleSide *= _ModelScale.x;
//            endSide *= _ModelScale.x;

            // Opacity
            //float alpha = (0.05 + vertNoise * 0.009) * saturate(pow(vertFresnel[i], 1)) * 0.75 * scaledEntrySpeed;
            float alpha = 0.1 * 0.5 * scaledEntrySpeed/** * saturate(pow(vertFresnel[i], 3.0))*/ * Strength;
            float middleAlpha = alpha * 0.6;
            alpha *= edgeMul;
            middleAlpha *= edgeMul;

            // Define the vertex positions
            vec3 offset_b0 = - offset_i + trailOffset - side;
            vec3 offset_b1 = - offset_j + trailOffset + side;

            vec3 offset_m0 = - offset_i + trailOffset - middleSide - middleLength[i] * gs_in[i].localNormal - Direction * middleSideLength[i];
            vec3 offset_m1 = - offset_j + trailOffset + middleSide - middleLength[j] * gs_in[j].localNormal - Direction * middleSideLength[j];

            vec3 offset_t0 = - offset_i + trailOffset - endSide - effectLength[i] * gs_in[i].localNormal - Direction * effectSideLength[i];
            vec3 offset_t1 = - offset_j + trailOffset + endSide - effectLength[j] * gs_in[j].localNormal - Direction * effectSideLength[j];

            // add the vertices to the tri strip
            createVertex(gl_in[i].gl_Position.xyz - offset_b0, BowShockColor, alpha);
            createVertex(gl_in[j].gl_Position.xyz - offset_b1, BowShockColor, alpha);

            createVertex(gl_in[i].gl_Position.xyz - offset_m0, BowShockColor, middleAlpha);
            createVertex(gl_in[j].gl_Position.xyz - offset_m1, BowShockColor, middleAlpha);

            createVertex(gl_in[i].gl_Position.xyz - offset_t0, BowShockColor, 0.0);
            createVertex(gl_in[j].gl_Position.xyz - offset_t1, BowShockColor, 0.0);

            // Restart the strip
            EndPrimitive();
        }

    }

}
