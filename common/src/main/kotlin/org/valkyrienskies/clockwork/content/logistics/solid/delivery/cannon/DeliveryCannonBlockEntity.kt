package org.valkyrienskies.clockwork.content.logistics.solid.delivery.cannon

import com.simibubi.create.content.kinetics.base.KineticBlockEntity
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.core.NonNullList
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraft.world.level.block.state.BlockState
import org.joml.Vector3dc
import org.valkyrienskies.clockwork.ClockworkPackets
import org.valkyrienskies.clockwork.content.logistics.solid.delivery.ActiveChutes
import org.valkyrienskies.clockwork.content.logistics.solid.delivery.chute.DeliveryChuteBlockEntity
import org.valkyrienskies.clockwork.util.blocktype.ISyncableStorage
import org.valkyrienskies.clockwork.util.blocktype.SyncableStoragePacket

class DeliveryCannonBlockEntity