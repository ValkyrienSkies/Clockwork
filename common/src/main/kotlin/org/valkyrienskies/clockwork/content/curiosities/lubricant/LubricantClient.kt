package org.valkyrienskies.clockwork.content.curiosities.lubricant

import com.simibubi.create.AllSpecialTextures
import net.createmod.catnip.outliner.Outliner
import net.minecraft.client.Minecraft
import net.minecraft.client.multiplayer.ClientLevel
import net.minecraft.core.BlockPos
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.InteractionHand
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.BlockHitResult
import net.minecraft.world.phys.HitResult
import org.valkyrienskies.mod.common.getShipManagingPos
import org.valkyrienskies.clockwork.ClockworkPackets

object LubricantClient {
    private const val COLOR = 0x67E4A5
    private val preview = Any()
    private var level: ClientLevel? = null
    private var blocks = longArrayOf()
    private var areas = longArrayOf()
    private var dimension: ResourceLocation? = null
    private val clusters = HashMap<Pair<Long, Long>, List<BlockPos>>()
    private val slots = HashMap<Pair<Long, Long>, Any>()
    private var rebuild = true

    @JvmStatic
    fun holding() = Minecraft.getInstance().player?.mainHandItem?.item is LubricantItem

    @JvmStatic
    fun attack(): Boolean {
        if (!holding()) return false
        ClockworkPackets.sendToServer(LubricantRemovePacket())
        Minecraft.getInstance().player?.swing(InteractionHand.MAIN_HAND)
        return true
    }

    fun accept(dimension: ResourceLocation, blocks: LongArray, areas: LongArray) {
        val current = Minecraft.getInstance().level ?: return
        if (current.dimension().location() != dimension) return
        if (level !== current) clear()
        level = current
        if (this.dimension == dimension && this.blocks.contentEquals(blocks) && this.areas.contentEquals(areas)) return
        this.dimension = dimension
        this.blocks = blocks
        this.areas = areas
        rebuild = true
    }

    fun tick() {
        val mc = Minecraft.getInstance()
        val current = mc.level
        if (level !== current) { clear(); level = current }
        val player = mc.player ?: return
        if (current == null) return
        val stack = player.handSlots.firstOrNull { it.item is LubricantItem } ?: run {
            rebuild = true
            return
        }
        val outliner = Outliner.getInstance()
        if (rebuild || current.gameTime % 40L == 0L) {
            slots.values.forEach(outliner::remove)
            slots.clear()
            clusters.clear()
            clusters.putAll(blocks.indices.groupBy { (current.getShipManagingPos(BlockPos.of(blocks[it]))?.id ?: -1L) to areas[it] }
                .mapValues { (_, indices) -> indices.map { BlockPos.of(blocks[it]) } })
            for ((body, positions) in clusters) {
                val slot = Any()
                slots[body] = slot
                outliner.showCluster(slot, positions).colored(COLOR)
                    .withFaceTexture(AllSpecialTextures.CUTOUT_CHECKERED).lineWidth(1f / 64f)
                    .disableLineNormals()
            }
            rebuild = false
        }
        slots.values.forEach(outliner::keep)
        val hit = mc.hitResult as? BlockHitResult ?: return
        if (hit.type != HitResult.Type.BLOCK) return
        val tag = stack.tag
        val started = tag?.contains("Start") == true && tag.getString("Dimension") == current.dimension().location().toString()
        val from = if (started) BlockPos.of(tag!!.getLong("Start")) else hit.blockPos
        val valid = LubricantItem.validSize(from, hit.blockPos) &&
            (!started || tag!!.getLong("Ship") == (current.getShipManagingPos(hit.blockPos)?.id ?: -1L))
        if (!valid) return
        outliner.showAABB(preview, AABB(from).minmax(AABB(hit.blockPos)))
            .colored(COLOR).withFaceTexture(AllSpecialTextures.CUTOUT_CHECKERED)
            .lineWidth(1f / 32f).disableLineNormals()
    }

    private fun clear() {
        slots.values.forEach { Outliner.getInstance().remove(it) }
        Outliner.getInstance().remove(preview)
        slots.clear()
        clusters.clear()
        blocks = longArrayOf()
        areas = longArrayOf()
        dimension = null
        rebuild = true
    }
}
