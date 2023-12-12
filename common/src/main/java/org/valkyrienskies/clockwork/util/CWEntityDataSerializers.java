package org.valkyrienskies.clockwork.util;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.syncher.EntityDataSerializer;
import net.minecraft.network.syncher.EntityDataSerializers;
import org.joml.Vector3ic;
import org.joml.primitives.AABBi;
import org.joml.primitives.AABBic;
import org.valkyrienskies.clockwork.content.curiosities.tools.gravitron.SelectedAreaToolkit;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

public class CWEntityDataSerializers {

    public static EntityDataSerializer<SelectedAreaToolkit> AREA_TOOLKIT_SERIALIZER = new EntityDataSerializer<SelectedAreaToolkit>() {
        @Override
        public void write(FriendlyByteBuf buf, SelectedAreaToolkit toolkit) {
            //selectedAreas
            int i = 0;
            for (AABBic aabBic : toolkit.getSelectedAreas()) {
                i++;
                writeAABBi(buf, aabBic);
            }
            buf.writeVarInt(i);
            i = 0;

            for (Set<AABBic> set : toolkit.getSelectionClusters()) {
                for (AABBic aabBic : set) {
                    i++;
                    writeAABBi(buf, aabBic);
                }
            }
            buf.writeVarInt(i);
            i = 0;
            for (Set<AABBic> set : toolkit.getToStopRendering()) {
                for (AABBic aabBic : set) {
                    i++;
                    writeAABBi(buf, aabBic);
                }
            }
            buf.writeVarInt(i);
        }

        @Override
        public SelectedAreaToolkit read(FriendlyByteBuf buf) {
            SelectedAreaToolkit toolkit = new SelectedAreaToolkit();

            int sizeSelectedAreas = buf.readVarInt();
            int sizeSelectionClusters = buf.readVarInt();
            int sizeToStopRendering = buf.readVarInt();
            Set<AABBic> aabBicSet = new HashSet<>();
            Set<AABBic> aabBicSet2 = new HashSet<>();

            for (int i = 0; i < sizeSelectedAreas; i++) {
                AABBi aabBi = readAABBi(buf);
                toolkit.clusterNewArea(aabBi);
            }

            for (int i = 0; i < sizeSelectionClusters; i++) {
                AABBi aabBi = readAABBi(buf);
                aabBicSet.add(aabBi);
            }
            toolkit.getSelectionClusters().add(aabBicSet);

            for (int i = 0; i < sizeToStopRendering; i++) {
                AABBi aabBi = readAABBi(buf);
                aabBicSet2.add(aabBi);
            }
            toolkit.getToStopRendering().add(aabBicSet2);

            return toolkit;
        }

        @Override
        public SelectedAreaToolkit copy(SelectedAreaToolkit value) {
            return value;
        }
    };

    public static EntityDataSerializer<Optional<Vector3ic>> VEC3I = new EntityDataSerializer<Optional<Vector3ic>>() {

        @Override
        public void write(FriendlyByteBuf buffer, Optional<Vector3ic> value) {
            value.ifPresent(vector3ic -> ClockworkUtils.INSTANCE.writeVector3i(buffer, vector3ic));
        }

        @Override
        public Optional<Vector3ic> read(FriendlyByteBuf buffer) {
            return Optional.of(ClockworkUtils.INSTANCE.readVector3i(buffer));
        }

        @Override
        public Optional<Vector3ic> copy(Optional<Vector3ic> value) {
            return value;
        }
    };

    public static void init() {
        EntityDataSerializers.registerSerializer(CWEntityDataSerializers.AREA_TOOLKIT_SERIALIZER);
        EntityDataSerializers.registerSerializer(CWEntityDataSerializers.VEC3I);
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
