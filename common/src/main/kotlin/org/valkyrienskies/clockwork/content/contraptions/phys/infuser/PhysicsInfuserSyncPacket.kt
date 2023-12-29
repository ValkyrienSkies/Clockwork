package org.valkyrienskies.clockwork.content.contraptions.phys.infuser


import net.minecraft.client.Minecraft
import net.minecraft.core.BlockPos
import net.minecraft.core.NonNullList
import net.minecraft.nbt.CompoundTag
import net.minecraft.network.FriendlyByteBuf
import net.minecraft.world.ContainerHelper
import net.minecraft.world.item.ItemStack
import org.valkyrienskies.clockwork.platform.api.network.ClientNetworkContext
import org.valkyrienskies.clockwork.platform.api.network.S2CCWPacket

class PhysicsInfuserSyncPacket : S2CCWPacket {
    private var inventoryPacket: NonNullList<ItemStack>? = null
    private val pos: BlockPos

    constructor(buf: FriendlyByteBuf) {
        val nbt = buf.readNbt()
        if (nbt != null) {
            inventoryPacket = NonNullList.withSize(1, ItemStack.EMPTY)
            ContainerHelper.loadAllItems(nbt, inventoryPacket)
        } else {
            inventoryPacket = NonNullList.withSize(1, ItemStack.EMPTY)
        }
        pos = buf.readBlockPos()
    }

    constructor(tile: PhysicsInfuserBlockEntity) {
        inventoryPacket = tile.inventory
        pos = tile.getBlockPos()
    }

    override fun write(buffer: FriendlyByteBuf) {
        buffer.writeBlockPos(pos)
        val nbt = CompoundTag()
        ContainerHelper.saveAllItems(nbt, inventoryPacket)
        buffer.writeNbt(nbt)
    }

    override fun handle(context: ClientNetworkContext) {
        context.enqueueWork {
            if (Minecraft.getInstance().level != null && Minecraft.getInstance().level!!.getBlockEntity(
                    pos
                ) is PhysicsInfuserBlockEntity
            ) {
                val ce =
                    Minecraft.getInstance().level!!.getBlockEntity(pos) as PhysicsInfuserBlockEntity?
                if (ce != null) {
                    ce.inventory = inventoryPacket
                }
            }
        }
    }
}