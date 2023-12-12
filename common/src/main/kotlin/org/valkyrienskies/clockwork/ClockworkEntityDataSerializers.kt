package org.valkyrienskies.clockwork

import net.minecraft.core.BlockPos
import net.minecraft.network.FriendlyByteBuf
import net.minecraft.network.syncher.EntityDataAccessor
import net.minecraft.network.syncher.EntityDataSerializer
import net.minecraft.network.syncher.EntityDataSerializers
import net.minecraft.network.syncher.SynchedEntityData
import net.minecraft.world.entity.player.Player
import org.joml.primitives.AABBi
import org.joml.primitives.AABBic
import org.valkyrienskies.clockwork.content.curiosities.tools.bluper.SelectedAreaToolkit
import java.util.*

object ClockworkEntityDataSerializers {

    @JvmField
    val AREA_TOOLKIT_SERIALIZER: EntityDataSerializer<SelectedAreaToolkit> =
        object : EntityDataSerializer<SelectedAreaToolkit> {
            override fun write(buf: FriendlyByteBuf, toolkit: SelectedAreaToolkit) {
                var i = 0
                toolkit.selectedAreas.forEach { aabBic ->
                    i++
                    writeAABBi(buf, aabBic)
                }
                buf.writeVarInt(i)
                i = 0

                val aabBicSet = mutableSetOf<AABBic>()
                toolkit.selectionClusters.forEach { set ->
                    set.forEach { aabBic ->
                        i++
                        writeAABBi(buf, aabBic)
                    }
                }
                buf.writeVarInt(i)
                toolkit.selectionClusters.add(aabBicSet)

                i = 0
                val aabBicSet2 = mutableSetOf<AABBic>()
                toolkit.toStopRendering.forEach { set ->
                    set.forEach { aabBic ->
                        i++
                        writeAABBi(buf, aabBic)
                    }
                }
                buf.writeVarInt(i)
                toolkit.toStopRendering.add(aabBicSet2)
            }

            override fun read(buf: FriendlyByteBuf): SelectedAreaToolkit {
                val toolkit = SelectedAreaToolkit()

                val sizeSelectedAreas = buf.readVarInt()
                val sizeSelectionClusters = buf.readVarInt()
                val sizeToStopRendering = buf.readVarInt()
                val aabBicSet = mutableSetOf<AABBic>()
                val aabBicSet2 = mutableSetOf<AABBic>()

                repeat(sizeSelectedAreas) {
                    val aabBi = readAABBi(buf)
                    toolkit.clusterNewArea(aabBi)
                }

                repeat(sizeSelectionClusters) {
                    val aabBi = readAABBi(buf)
                    aabBicSet.add(aabBi)
                }
                toolkit.selectionClusters.add(aabBicSet)

                repeat(sizeToStopRendering) {
                    val aabBi = readAABBi(buf)
                    aabBicSet2.add(aabBi)
                }
                toolkit.toStopRendering.add(aabBicSet2)

                return toolkit
            }

            override fun copy(value: SelectedAreaToolkit): SelectedAreaToolkit {
                return value
            }
        }

    @JvmField
    val AREA_TOOLKIT: EntityDataAccessor<SelectedAreaToolkit> = SynchedEntityData.defineId(
        Player::class.java, AREA_TOOLKIT_SERIALIZER
    )

    @JvmField
    val FIRST_POS: EntityDataAccessor<Optional<BlockPos>> = SynchedEntityData.defineId(
        Player::class.java, EntityDataSerializers.OPTIONAL_BLOCK_POS
    )

    @JvmField
    val SECOND_POS: EntityDataAccessor<Optional<BlockPos>> = SynchedEntityData.defineId(
        Player::class.java, EntityDataSerializers.OPTIONAL_BLOCK_POS
    )

    @JvmStatic
    fun init() {
        EntityDataSerializers.registerSerializer(AREA_TOOLKIT_SERIALIZER)
    }

    fun writeAABBi(buffer: FriendlyByteBuf, aabbic: AABBic) {
        buffer.writeInt(aabbic.minX())
        buffer.writeInt(aabbic.minY())
        buffer.writeInt(aabbic.minZ())
        buffer.writeInt(aabbic.maxX())
        buffer.writeInt(aabbic.maxY())
        buffer.writeInt(aabbic.maxZ())
    }

    fun readAABBi(buffer: FriendlyByteBuf): AABBi {
        val minX = buffer.readInt()
        val minY = buffer.readInt()
        val minZ = buffer.readInt()
        val maxX = buffer.readInt()
        val maxY = buffer.readInt()
        val maxZ = buffer.readInt()

        return AABBi(minX, minY, minZ, maxX, maxY, maxZ)
    }
}