package org.valkyrienskies.clockwork.content.logistics.solid.delivery.cannon

import net.minecraft.client.Minecraft

import net.minecraft.core.BlockPos
import net.minecraft.network.FriendlyByteBuf
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ItemStack
import net.minecraft.world.phys.Vec3
import org.joml.Vector3d
import org.valkyrienskies.clockwork.platform.api.network.ClientNetworkContext
import org.valkyrienskies.clockwork.platform.api.network.S2CCWPacket
import org.valkyrienskies.core.util.readVec3d
import org.valkyrienskies.core.util.writeVec3d

class DeliveryCannonSyncPacket : S2CCWPacket {
    override var player: Player? = null
    private val currentStack: ItemStack
    private val transportStack: ItemStack
    private val location: Vec3
    private val maxProgress: Double
    private val xRotation: Double
    private val yRotation: Double
    private val xTargetRotation: Double
    private val yTargetRotation: Double
    private val gunpowderTicks: Int
    private val pos: BlockPos



    constructor(buffer: FriendlyByteBuf) {
        currentStack = buffer.readItem()
        transportStack = buffer.readItem()
        val temp = buffer.readVec3d()
        location = Vec3(temp.x,temp.y,temp.z)
        maxProgress = buffer.readDouble()
        xRotation = buffer.readDouble()
        yRotation = buffer.readDouble()
        xTargetRotation = buffer.readDouble()
        yTargetRotation = buffer.readDouble()
        gunpowderTicks = buffer.readInt()
        pos = buffer.readBlockPos()

    }

    constructor(newCurrentStack: ItemStack, newTransportStack: ItemStack, newLoc:Vec3, newProg: Double, newxRotation: Double, newyRotation: Double, newPos: BlockPos, newXTargetRotation: Double, newYTargetRotation: Double, newGunpowderTicks:Int) {
        currentStack = newCurrentStack
        transportStack = newTransportStack
        location = newLoc
        maxProgress = newProg
        xRotation = newxRotation
        yRotation = newyRotation
        xTargetRotation = newXTargetRotation
        yTargetRotation = newYTargetRotation
        gunpowderTicks = newGunpowderTicks
        pos  = newPos

    }

    override fun handle(context: ClientNetworkContext) {
        context.enqueueWork {

            val be = Minecraft.getInstance().level?.getBlockEntity(pos) as DeliveryCannonBlockEntity? ?: return@enqueueWork
            be.currentStack = currentStack
            be.transportStack = transportStack
            be.realLocation = location
            be.maxProgress = maxProgress
            be.xRotation = xRotation
            be.yRotation = yRotation
            be.xTargetRotation = xTargetRotation
            be.yTargetRotation = yTargetRotation
            be.gunPowderTicks = gunpowderTicks
        }
        context.setPacketHandled(true)
    }

    override fun write(buffer: FriendlyByteBuf) {
        buffer.writeItem(currentStack)
        buffer.writeItem(transportStack)
        buffer.writeVec3d(Vector3d(location.x,location.y,location.z))
        buffer.writeDouble(maxProgress)
        buffer.writeDouble(xRotation)
        buffer.writeDouble(yRotation)
        buffer.writeDouble(xTargetRotation)
        buffer.writeDouble(yTargetRotation)
        buffer.writeInt(gunpowderTicks)
        buffer.writeBlockPos(pos)

    }


}
