package org.valkyrienskies.clockwork.content.curiosities.tools.gatemakyr;

import com.mojang.blaze3d.vertex.VertexConsumer;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector2f;
import org.joml.Vector3f;
import org.joml.Vector4f;

final class GateSliceVertices implements VertexConsumer {
    private static final class Vertex {
        final Vector3f position = new Vector3f();
        final Vector3f normal = new Vector3f();
        final Vector4f color = new Vector4f(255f);
        final Vector2f uv = new Vector2f();
        final Vector2f overlay = new Vector2f();
        final Vector2f light = new Vector2f();
        float distance;

        void interpolate(Vertex a, Vertex b, float t) {
            a.position.lerp(b.position, t, position);
            a.normal.lerp(b.normal, t, normal);
            a.color.lerp(b.color, t, color);
            a.uv.lerp(b.uv, t, uv);
            a.overlay.lerp(b.overlay, t, overlay);
            a.light.lerp(b.light, t, light);
        }
    }

    private final VertexConsumer out;
    private final Vector4f plane;
    private final Matrix4f transform;
    private final Matrix3f normals;
    private final int size;
    private final boolean projectBack;
    private final Vertex[] face = {new Vertex(), new Vertex(), new Vertex(), new Vertex()};
    private final Vertex[] intersections = {new Vertex(), new Vertex(), new Vertex(), new Vertex()};
    private final Vertex[] front = new Vertex[8], back = new Vertex[8];
    private final Vector3f pos = new Vector3f(), direction = new Vector3f();
    private final Vector4f defaultColor = new Vector4f(255f);
    private int index;

    GateSliceVertices(VertexConsumer out, Vector4f plane, Matrix4f transform, int size, boolean projectBack) {
        this.out = out;
        this.plane = plane;
        this.transform = transform;
        this.normals = new Matrix3f(transform).normal();
        this.size = size;
        this.projectBack = projectBack;
    }

    @Override public VertexConsumer vertex(double x, double y, double z) {
        face[index].position.set(x, y, z);
        face[index].color.set(defaultColor);
        return this;
    }

    @Override public VertexConsumer color(int r, int g, int b, int a) { face[index].color.set(r, g, b, a); return this; }
    @Override public VertexConsumer uv(float u, float v) { face[index].uv.set(u, v); return this; }
    @Override public VertexConsumer overlayCoords(int u, int v) { face[index].overlay.set(u, v); return this; }
    @Override public VertexConsumer uv2(int u, int v) { face[index].light.set(u, v); return this; }
    @Override public VertexConsumer normal(float x, float y, float z) { face[index].normal.set(x, y, z); return this; }
    @Override public void defaultColor(int r, int g, int b, int a) { defaultColor.set(r, g, b, a); }
    @Override public void unsetDefaultColor() { defaultColor.set(255f); }

    @Override public void endVertex() {
        Vertex v = face[index];
        v.distance = plane.x * v.position.x + plane.y * v.position.y + plane.z * v.position.z + plane.w;
        if (++index != size) return;
        index = 0;
        int frontCount = 0, backCount = 0;
        boolean hasBack = false;
        Vertex a = face[size - 1];
        for (int i = 0; i < size; i++) {
            Vertex b = face[i];
            if (a.distance * b.distance < 0f) {
                Vertex cut = intersections[i];
                cut.interpolate(a, b, a.distance / (a.distance - b.distance));
                front[frontCount++] = cut;
                back[backCount++] = cut;
            }
            hasBack |= b.distance < 0f;
            if (b.distance >= 0f) front[frontCount++] = b;
            if (b.distance <= 0f) back[backCount++] = b;
            a = b;
        }
        emit(front, frontCount, false);
        if (projectBack && hasBack) emit(back, backCount, true);
    }

    private void emit(Vertex[] polygon, int count, boolean mapped) {
        if (size == 4 && count == 4) {
            for (int i = 0; i < 4; i++) write(polygon[i], mapped);
            return;
        }
        for (int i = 1; i + 1 < count; i++) {
            write(polygon[0], mapped);
            write(polygon[i], mapped);
            write(polygon[i + 1], mapped);
            if (size == 4) write(polygon[i + 1], mapped);
        }
    }

    private void write(Vertex v, boolean mapped) {
        pos.set(v.position);
        direction.set(v.normal);
        if (mapped) {
            transform.transformPosition(pos);
            normals.transform(direction);
        }
        out.vertex(pos.x, pos.y, pos.z).color(Math.round(v.color.x), Math.round(v.color.y), Math.round(v.color.z), Math.round(v.color.w))
            .uv(v.uv.x, v.uv.y).overlayCoords(Math.round(v.overlay.x), Math.round(v.overlay.y))
            .uv2(Math.round(v.light.x), Math.round(v.light.y)).normal(direction.x, direction.y, direction.z).endVertex();
    }
}
