package org.valkyrienskies.clockwork.content.logistics.solid.delivery.cannon

import net.minecraft.client.Minecraft

import net.minecraft.core.BlockPos
import net.minecraft.network.FriendlyByteBuf
import net.minecraft.world.item.ItemStack
import org.valkyrienskies.clockwork.platform.api.network.ClientNetworkContext
import org.valkyrienskies.clockwork.platform.api.network.S2CCWPacket

class DeliveryCannonSyncPacket : S2CCWPacket {
    private val stack: ItemStack
    private val location: BlockPos
    private val progress: Double
    private val xRotation: Double
    private val yRotation: Double
    private val shootingTicks: Int
    private val pos: BlockPos


    constructor(buffer: FriendlyByteBuf) {
        stack = buffer.readItem()
        location = buffer.readBlockPos()
        progress = buffer.readDouble()
        xRotation = buffer.readDouble()
        yRotation = buffer.readDouble()
        shootingTicks = buffer.readInt()
        pos = buffer.readBlockPos()
    }

    constructor(newStack: ItemStack,newLoc:BlockPos,newProg: Double,newxRotation: Double, newyRotation: Double, newShootingTicks:Int, newPos: BlockPos) {
        stack = newStack
        location = newLoc
        progress = newProg
        xRotation = newxRotation
        yRotation = newyRotation
        shootingTicks = newShootingTicks
        pos  = newPos
    }

    override fun handle(context: ClientNetworkContext) {
        context.enqueueWork {

            val be = Minecraft.getInstance().level?.getBlockEntity(pos) as DeliveryCannonBlockEntity
            be.transportStack = stack
            be.location = location
            be.progress = progress
            be.xRotation = xRotation
            be.yRotation = yRotation
            be.shootingTicks = shootingTicks
        }
        context.setPacketHandled(true)
    }

    override fun write(buffer: FriendlyByteBuf) {
        buffer.writeItem(stack)
        buffer.writeBlockPos(location)
        buffer.writeDouble(progress)
        buffer.writeDouble(xRotation)
        buffer.writeDouble(yRotation)
        buffer.writeInt(shootingTicks)
        buffer.writeBlockPos(pos)
    }


}
