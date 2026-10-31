package org.valkyrienskies.clockwork.content.contraptions.phys.gimbal.quad_link

import com.simibubi.create.content.redstone.link.RedstoneLinkNetworkHandler
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.nbt.Tag
import net.minecraft.network.FriendlyByteBuf
import net.minecraft.world.item.ItemStack
import org.valkyrienskies.clockwork.LinkedControllerClientHandlerMixinStorage
import org.valkyrienskies.clockwork.LinkedControllerClientHandlerMixinStorage.doNothing
import org.valkyrienskies.clockwork.platform.api.network.C2SCWPacket
import org.valkyrienskies.clockwork.platform.api.network.ServerNetworkContext

class GimbalLinkedControllerBindPacket(
    var button: Int,
    var linkLocation: BlockPos,
    var face: Direction,
    var itemClassName: String
) : C2SCWPacket {
    constructor(buffer: FriendlyByteBuf) : this(
        buffer.readVarInt(),
        buffer.readBlockPos(),
        buffer.readEnum(Direction::class.java),
        buffer.readUtf()
    )

    override fun write(buffer: FriendlyByteBuf) {
        buffer.writeVarInt(button)
        buffer.writeBlockPos(linkLocation)
        buffer.writeEnum(face)
        buffer.writeUtf(itemClassName)
    }

    override fun handle(context: ServerNetworkContext) {
        context.enqueueWork {
            val player = context.sender
            if (player.isSpectator) return@enqueueWork

            var controller = player.mainHandItem
            val itemClass = Class.forName(itemClassName)

            if (!itemClass.isInstance(controller.item)) {
                controller = player.offhandItem
                if (!itemClass.isInstance(controller.item)) return@enqueueWork
            }

            doNothing()
            val getFrequencyItems = itemClass.getDeclaredMethod("getFrequencyItems", ItemStack::class.java)
            val frequencyItems = getFrequencyItems.invoke(null, controller)
            val getStackInSlot = frequencyItems::class.java.getDeclaredMethod(
                "getStackInSlot",
                Integer::class.javaPrimitiveType
            )
            val setStackInSlot = frequencyItems::class.java.getDeclaredMethod(
                "setStackInSlot",
                Integer::class.javaPrimitiveType,
                ItemStack::class.java
            )
            val serializeNbt = frequencyItems::class.java.getDeclaredMethod("serializeNBT")

            val blockState = player.level().getBlockState(linkLocation)
            val type = QuadLinkHandler.getBehaviourType(blockState, face)

            val linkBehaviour = BlockEntityBehaviour.get(player.level(), linkLocation, type) ?: return@enqueueWork

            linkBehaviour.networkKey
                .forEachWithContext { f: RedstoneLinkNetworkHandler.Frequency, first: Boolean ->
                    setStackInSlot.invoke(frequencyItems, button * 2 + (if (first) 0 else 1), f.stack.copy())
                }

            controller.tag!!
                .put("Items", serializeNbt.invoke(frequencyItems) as Tag)
        }
        context.setPacketHandled(true)
    }
}
