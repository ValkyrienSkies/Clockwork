package org.valkyrienskies.clockwork.content.logistics.solid.delivery.cannon

import net.minecraft.client.Minecraft

import net.minecraft.core.BlockPos
import net.minecraft.network.FriendlyByteBuf
import net.minecraft.world.item.ItemStack
import net.minecraft.world.phys.Vec3
import org.joml.Vector3d
import org.valkyrienskies.clockwork.platform.api.network.ClientNetworkContext
import org.valkyrienskies.clockwork.platform.api.network.S2CCWPacket
import org.valkyrienskies.core.util.readVec3d
import org.valkyrienskies.core.util.readVec3fAsDouble
import org.valkyrienskies.core.util.writeVec3d

class DeliveryCannonSyncPacket : S2CCWPacket {
    private val stack: ItemStack
    private val location: Vec3
    private val progress: Double
    private val xRotation: Double
    private val yRotation: Double
    private val xTargetRotation: Double
    private val yTargetRotation: Double
    private val pos: BlockPos



    constructor(buffer: FriendlyByteBuf) {
        stack = buffer.readItem()
        val temp = buffer.readVec3d()
        location = Vec3(temp.x,temp.y,temp.z)
        progress = buffer.readDouble()
        xRotation = buffer.readDouble()
        yRotation = buffer.readDouble()
        xTargetRotation = buffer.readDouble()
        yTargetRotation = buffer.readDouble()
        pos = buffer.readBlockPos()

    }

    constructor(newStack: ItemStack,newLoc:Vec3,newProg: Double,newxRotation: Double, newyRotation: Double, newPos: BlockPos, newXTargetRotation: Double, newYTargetRotation: Double) {
        stack = newStack
        location = newLoc
        progress = newProg
        xRotation = newxRotation
        yRotation = newyRotation
        xTargetRotation = newXTargetRotation
        yTargetRotation = newYTargetRotation
        pos  = newPos

    }

    override fun handle(context: ClientNetworkContext) {
        context.enqueueWork {

            val be = Minecraft.getInstance().level?.getBlockEntity(pos) as DeliveryCannonBlockEntity
            be.transportStack = stack
            be.realLocation = location
            be.progress = progress
            be.xRotation = xRotation
            be.yRotation = yRotation
            be.xTargetRotation = xTargetRotation
            be.yTargetRotation = yTargetRotation
        }
        context.setPacketHandled(true)
    }

    override fun write(buffer: FriendlyByteBuf) {
        buffer.writeItem(stack)
        buffer.writeVec3d(Vector3d(location.x,location.y,location.z))
        buffer.writeDouble(progress)
        buffer.writeDouble(xRotation)
        buffer.writeDouble(yRotation)
        buffer.writeDouble(xTargetRotation)
        buffer.writeDouble(yTargetRotation)
        buffer.writeBlockPos(pos)

    }


}
