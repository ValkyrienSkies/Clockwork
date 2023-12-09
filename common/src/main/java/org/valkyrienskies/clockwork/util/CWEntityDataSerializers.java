package org.valkyrienskies.clockwork.util;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.syncher.EntityDataSerializer;
import net.minecraft.network.syncher.EntityDataSerializers;
import org.joml.primitives.AABBi;
import org.joml.primitives.AABBic;
import org.valkyrienskies.clockwork.content.curiosities.tools.auric.designator.SelectedAreaToolkit;

public class CWEntityDataSerializers {

    public static EntityDataSerializer<SelectedAreaToolkit> AREA_TOOLKIT_SERIALIZER = new EntityDataSerializer<SelectedAreaToolkit>() {
        @Override
        public void write(FriendlyByteBuf buf, SelectedAreaToolkit toolkit) {
            //selectedAreas
            for (AABBic aabBic : toolkit.getSelectedAreas()) {
                writeAABBi(buf, aabBic);
            }
        }

        @Override
        public SelectedAreaToolkit read(FriendlyByteBuf buf) {
            SelectedAreaToolkit toolkit = new SelectedAreaToolkit();
            AABBi aabBi = readAABBi(buf);
            toolkit.clusterNewArea(aabBi);

            return toolkit;
        }

        @Override
        public SelectedAreaToolkit copy(SelectedAreaToolkit value) {
            return value;
        }
    };

    public static void init() {
        EntityDataSerializers.registerSerializer(CWEntityDataSerializers.AREA_TOOLKIT_SERIALIZER);
    }

    static void writeAABBi(FriendlyByteBuf buffer, AABBic aabbic) {
        buffer.writeInt(aabbic.minX());
        buffer.writeInt(aabbic.minY());
        buffer.writeInt(aabbic.minZ());
        buffer.writeInt(aabbic.maxX());
        buffer.writeInt(aabbic.maxY());
        buffer.writeInt(aabbic.maxZ());
    }

    static AABBi readAABBi(FriendlyByteBuf buffer) {
        int minX = buffer.readInt();
        int minY = buffer.readInt();
        int minZ = buffer.readInt();
        int maxX = buffer.readInt();
        int maxY = buffer.readInt();
        int maxZ = buffer.readInt();

        return new AABBi(minX, minY, minZ, maxX, maxY, maxZ);
    }
}
