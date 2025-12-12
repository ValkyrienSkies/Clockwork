package org.valkyrienskies.clockwork.util.blocktype

import net.minecraft.client.Minecraft
import net.minecraft.core.BlockPos
import net.minecraft.core.NonNullList
import net.minecraft.nbt.CompoundTag
import net.minecraft.network.FriendlyByteBuf
import net.minecraft.world.ContainerHelper
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ItemStack
import org.valkyrienskies.clockwork.platform.api.network.ClientNetworkContext
import org.valkyrienskies.clockwork.platform.api.network.S2CCWPacket

class SyncableStoragePacket : S2CCWPacket {
    override var player: Player? = null
    private val pos: BlockPos
    private val inventory: NonNullList<ItemStack>
    private val inventorySize: Int

    constructor(buffer: FriendlyByteBuf) {
        pos = buffer.readBlockPos()
        val nbt = buffer.readNbt()

        if (nbt == null) {
            inventory = NonNullList.withSize(1, ItemStack.EMPTY)
            inventorySize = 1
        } else {
            inventorySize = nbt.getInt("size")
            inventory = NonNullList.withSize(inventorySize, ItemStack.EMPTY)
            ContainerHelper.loadAllItems(nbt, inventory)
        }
    }

    constructor(be: ISyncableStorage) {
        pos = be.getBlockPositionFromISS()
        inventory = be.getStorageInventory()
        inventorySize = be.getStorageInventorySize()
    }

    override fun write(buffer: FriendlyByteBuf) {
        buffer.writeBlockPos(pos)
        val nbt = CompoundTag()
        ContainerHelper.saveAllItems(nbt, inventory)
        nbt.putInt("size", inventorySize)
        buffer.writeNbt(nbt)
    }

    override fun handle(context: ClientNetworkContext) {
        context.enqueueWork {
            if (Minecraft.getInstance().level != null && Minecraft.getInstance().level!!.getBlockEntity(
                    pos
                ) is ISyncableStorage
            ) {
                val ce =
                    Minecraft.getInstance().level!!.getBlockEntity(pos) as ISyncableStorage?
                if (ce != null) {
                    ce.sync(inventory)
                }
            }
        }
        context.setPacketHandled(true)
    }
}