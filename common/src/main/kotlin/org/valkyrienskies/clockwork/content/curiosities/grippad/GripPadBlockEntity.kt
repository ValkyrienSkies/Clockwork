package org.valkyrienskies.clockwork.content.curiosities.grippad

import com.simibubi.create.foundation.blockEntity.SmartBlockEntity
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour
import net.minecraft.core.BlockPos
import net.minecraft.core.particles.BlockParticleOption
import net.minecraft.core.particles.ParticleTypes
import net.minecraft.nbt.CompoundTag
import net.minecraft.server.level.ServerLevel
import net.minecraft.sounds.SoundEvents
import net.minecraft.sounds.SoundSource
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.block.DirectionalBlock
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraft.world.level.block.state.BlockState
import org.joml.Vector3d
import org.valkyrienskies.mod.common.dimensionId
import org.valkyrienskies.mod.common.getShipManagingPos

class GripPadBlockEntity(type: BlockEntityType<*>, pos: BlockPos, state: BlockState) : SmartBlockEntity(type, pos, state) {
    var contact = false
        private set
    var gripping = false
        private set
    var compression = 0f
        private set
    var previousCompression = 0f
        private set
    private var key: GripPadController.Key? = null
    private var lastLanding = Long.MIN_VALUE

    override fun addBehaviours(behaviours: MutableList<BlockEntityBehaviour>) {}

    override fun tick() {
        super.tick()
        previousCompression = compression
        compression += ((if (contact) 1f else 0f) - compression) * 0.4f
        val world = level as? ServerLevel ?: return
        val currentKey = GripPadController.Key(world.dimensionId, blockPos.immutable())
        key = currentKey
        val ship = world.getShipManagingPos(blockPos)
        val facing = blockState.getValue(DirectionalBlock.FACING)
        GripPadController.publish(currentKey, ship?.id, facing, blockState.getValue(GripPadBlock.POWERED))
        val result = GripPadController.feedback(currentKey)
        val touching = result?.contact == true
        val active = result?.gripping == true
        if (contact == touching && gripping == active) return
        if (!contact && touching && (lastLanding == Long.MIN_VALUE || world.gameTime - lastLanding > 10)) {
            lastLanding = world.gameTime
            val point = Vector3d(blockPos.x + 0.5, blockPos.y + 0.5, blockPos.z + 0.5)
            ship?.transform?.shipToWorld?.transformPosition(point)
            world.playSound(null, point.x, point.y, point.z, SoundEvents.SLIME_BLOCK_HIT, SoundSource.BLOCKS, 0.45f, 0.7f)
            val count = if ((result?.impact ?: 0.0) > 1.0) 8 else 3
            world.sendParticles(BlockParticleOption(ParticleTypes.BLOCK, Blocks.STONE.defaultBlockState()),
                point.x, point.y, point.z, count, 0.15, 0.02, 0.15, 0.03)
        }
        contact = touching
        gripping = active
        world.updateNeighbourForOutputSignal(blockPos, blockState.block)
        sendData()
    }

    override fun invalidate() {
        key?.let(GripPadController::remove)
        super.invalidate()
    }

    override fun write(tag: CompoundTag, clientPacket: Boolean) {
        super.write(tag, clientPacket)
        if (clientPacket) { tag.putBoolean("Contact", contact); tag.putBoolean("Gripping", gripping) }
    }

    override fun read(tag: CompoundTag, clientPacket: Boolean) {
        super.read(tag, clientPacket)
        if (clientPacket) { contact = tag.getBoolean("Contact"); gripping = tag.getBoolean("Gripping") }
    }
}
