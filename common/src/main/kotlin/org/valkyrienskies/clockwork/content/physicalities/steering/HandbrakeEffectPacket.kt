package org.valkyrienskies.clockwork.content.physicalities.steering

import net.createmod.catnip.outliner.Outliner
import net.createmod.catnip.render.BindableTexture
import net.createmod.catnip.theme.Color
import net.minecraft.client.Minecraft
import net.minecraft.core.BlockPos
import net.minecraft.core.particles.ParticleTypes
import net.minecraft.network.FriendlyByteBuf
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.entity.player.Player
import org.valkyrienskies.clockwork.ClockworkPackets
import org.valkyrienskies.clockwork.content.physicalities.wheel.WheelHubBlock
import org.valkyrienskies.clockwork.platform.api.network.ClientNetworkContext
import org.valkyrienskies.clockwork.platform.api.network.S2CCWPacket
import org.valkyrienskies.mod.common.toWorldCoordinates

class HandbrakeEffectPacket(private val target: BlockPos) : S2CCWPacket {
    override var player: Player? = null
    constructor(buffer: FriendlyByteBuf) : this(buffer.readBlockPos())
    override fun write(buffer: FriendlyByteBuf) { buffer.writeBlockPos(target) }
    override fun handle(context: ClientNetworkContext) {
        context.enqueueWork { HandbrakeFlash.show(target) }
        context.setPacketHandled(true)
    }

    companion object {
        internal fun send(level: ServerLevel, target: BlockPos) {
            val point = level.toWorldCoordinates(target.center)
            ClockworkPackets.sendToNear(level, BlockPos.containing(point), 96, HandbrakeEffectPacket(target))
        }
    }
}

private object HandbrakeFlash {
    private val white = BindableTexture { ResourceLocation("minecraft", "textures/block/white_concrete.png") }
    private val red = Color(255, 48, 32, 205)

    fun show(pos: BlockPos) {
        val level = Minecraft.getInstance().level ?: return
        if (!level.hasChunkAt(pos)) return
        val state = level.getBlockState(pos)
        if (!DrivingControlItem.brakeTarget(state.block)) return
        val shape = if (state.block is WheelHubBlock) state.getCollisionShape(level, pos) else state.getShape(level, pos)
        shape.toAabbs().forEachIndexed { index, box ->
            Outliner.getInstance().showAABB(Triple(this, pos, index), box.move(pos).inflate(.003))
                .colored(red).withFaceTexture(white).lineWidth(.008f).disableLineNormals()
        }
        val point = level.toWorldCoordinates(pos.center)
        val random = level.random
        repeat(5) {
            level.addParticle(ParticleTypes.SMOKE, point.x, point.y, point.z,
                (random.nextDouble() - .5) * .4, random.nextDouble() * .2, (random.nextDouble() - .5) * .4)
        }
    }
}
