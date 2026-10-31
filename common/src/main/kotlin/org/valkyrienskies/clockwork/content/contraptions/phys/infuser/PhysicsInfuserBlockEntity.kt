package org.valkyrienskies.clockwork.content.contraptions.phys.infuser

import com.simibubi.create.foundation.blockEntity.SmartBlockEntity
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.core.NonNullList
import net.minecraft.nbt.CompoundTag
import net.minecraft.nbt.ListTag
import net.minecraft.nbt.Tag
import net.minecraft.network.chat.Component
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.ContainerHelper
import net.minecraft.world.WorldlyContainer
import net.minecraft.world.entity.item.ItemEntity
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.phys.Vec3
import org.valkyrienskies.clockwork.ClockworkConfig
import org.valkyrienskies.clockwork.ClockworkMod
import org.valkyrienskies.clockwork.ClockworkPackets
import org.valkyrienskies.clockwork.ClockworkSounds
import org.valkyrienskies.clockwork.content.curiosities.tools.wanderwand.WanderwandItem
import org.valkyrienskies.clockwork.content.curiosities.tools.wanderwand.WandSelectionMath
import org.valkyrienskies.mod.common.assembly.ShipAssembler
import org.valkyrienskies.mod.common.getLoadedShipManagingPos
import java.util.UUID
import kotlin.math.roundToInt

class PhysicsInfuserBlockEntity(type: BlockEntityType<*>?, pos: BlockPos?, state: BlockState?) :
    SmartBlockEntity(type, pos, state), WorldlyContainer {
    var inventory: NonNullList<ItemStack> = NonNullList.withSize(1, ItemStack.EMPTY)
    var sequence: InfuserSequence? = null
        private set
    var regions: List<InfuserRegion> = emptyList()
        private set
    var selection = CompoundTag()
        private set
    var impacted = false
        private set
    private var completed = true
    private var powered = false
    private var initiator: UUID? = null
    private var planned: List<Set<BlockPos>> = emptyList()
    val assembling get() = sequence != null && !completed

    override fun addBehaviours(behaviours: MutableList<BlockEntityBehaviour>) = Unit
    override fun getSlotsForFace(side: Direction) = intArrayOf()
    override fun canPlaceItemThroughFace(index: Int, stack: ItemStack, direction: Direction?) = false
    override fun canTakeItemThroughFace(index: Int, stack: ItemStack, direction: Direction) = !assembling
    override fun getContainerSize() = 1
    override fun isEmpty() = inventory[0].isEmpty
    override fun getItem(slot: Int): ItemStack = inventory[0]
    override fun removeItem(slot: Int, amount: Int): ItemStack {
        if (assembling) return ItemStack.EMPTY
        return ContainerHelper.removeItem(inventory, slot, amount).also { if (!it.isEmpty) setChanged() }
    }
    override fun removeItemNoUpdate(slot: Int): ItemStack =
        if (assembling) ItemStack.EMPTY else ContainerHelper.takeItem(inventory, slot)
    override fun setItem(slot: Int, stack: ItemStack) {
        if (assembling) return
        inventory[slot] = stack
        setChanged()
    }
    override fun stillValid(player: Player) = level?.getBlockEntity(worldPosition) === this &&
        player.distanceToSqr(Vec3.atCenterOf(worldPosition)) <= 64.0
    override fun clearContent() {
        inventory[0] = ItemStack.EMPTY
        setChanged()
    }
    override fun setChanged() {
        super.setChanged()
        if (level is ServerLevel) {
            sendData()
            ClockworkPackets.sendToNear(level, worldPosition, 128, PhysicsInfuserSyncPacket(this))
        }
    }

    fun redstoneChanged() {
        val world = level as? ServerLevel ?: return
        val signal = world.hasNeighborSignal(worldPosition)
        val rising = signal && !powered
        powered = signal
        super.setChanged()
        if (rising) startAssembly(null)
    }

    fun startAssembly(player: ServerPlayer?): Boolean {
        val world = level as? ServerLevel ?: return false
        if (assembling) return false
        val previous = sequence
        if (previous != null && previous.age(world.gameTime) < previous.settled + 20) return false
        fun fail(key: String): Boolean {
            player?.displayClientMessage(Component.translatable("message.vs_clockwork.infuser.$key"), true)
            return false
        }
        val wand = inventory[0]
        if (wand.item !is WanderwandItem) return false
        val tag = wand.tag ?: return fail("empty")
        if (tag.contains("selectionDimension") && tag.getString("selectionDimension") != world.dimension().location().toString())
            return fail("dimension")
        val selected = tag.getCompound("selectedBlocks")
        val boxes = WanderwandItem.readAABBSetFromNBT(selected)
        if (boxes.isEmpty()) return fail("empty")
        if (boxes.sumOf { WandSelectionMath.volume(it) } > 100000) return fail("large")
        for (box in boxes) {
            if (box.minY() < world.minBuildHeight || box.maxY() > world.maxBuildHeight) return fail("unloaded")
            for (x in (box.minX() shr 4)..((box.maxX() - 1) shr 4))
                for (z in (box.minZ() shr 4)..((box.maxZ() - 1) shr 4))
                    if (!world.hasChunk(x, z)) return fail("unloaded")
        }
        val components = WanderwandItem.findIsolatedComponents(boxes, world)
            .filter { component -> component.isNotEmpty() && component.none {
                it == worldPosition || ClockworkConfig.SERVER.blockBlacklist.contains(world.getBlockState(it).block.descriptionId)
            } }
            .sortedWith(compareBy<Set<BlockPos>> { set -> set.minOf { it.distSqr(worldPosition) } }
                .thenBy { set -> set.minOf { it.asLong() } })
        if (components.isEmpty()) return fail("empty")
        if (components.size > 128) return fail("regions")
        planned = components
        regions = components.map { component -> InfuserRegion(component.minWith(compareBy<BlockPos> {
            it.distSqr(worldPosition)
        }.thenBy { it.asLong() })) }
        selection = selected.copy()
        initiator = player?.uuid
        sequence = InfuserSequence(world.gameTime, player?.let { InfuserProgress.get(world).activate(it.uuid) } ?: false)
        impacted = false
        completed = false
        ClockworkSounds.PHYSICS_INFUSER_INITIALIZE.play(world, null, soundPosition(world), 0.95f, 1f)
        setChanged()
        return true
    }

    fun skipAssembly() {
        val world = level as? ServerLevel ?: return
        if (sequence?.skip(world.gameTime) == true) {
            ClockworkSounds.WAND_WELD.play(world, null, soundPosition(world), 0.8f, 0.8f)
            setChanged()
        }
    }

    override fun tick() {
        super.tick()
        val world = level ?: return
        if (world.isClientSide) {
            InfuserEffects.track(this)
            return
        }
        world as ServerLevel
        val animation = sequence ?: return
        if (completed) return
        val age = animation.age(world.gameTime).toInt()
        val center = Vec3.atCenterOf(worldPosition)
        val soundCenter = soundPosition(world)
        if (age < animation.impact && age % (if (age < 50) 12 else 25) == 0)
            ClockworkSounds.PHYSICS_INFUSER_LIGHTNING.play(world, null, soundCenter, 0.6f, 1f + world.random.nextFloat() * 0.4f)
        if (age == (if (animation.firstUse) 48 else 16))
            ClockworkSounds.WAND_START.play(world, null, soundCenter, 0.8f, 0.85f)
        val connection = regions.indices.firstOrNull {
            kotlin.math.ceil(animation.connection(it, regions.size) + InfuserVisuals.BEAM_FLIGHT).toInt() == age
        }
        if (connection != null && age < animation.impact) {
            ClockworkSounds.WAND_FINISH.play(world, null, soundCenter, 0.85f, 1.0f + connection.toFloat() / regions.size * 0.4f)
            ClockworkSounds.PHYSICS_INFUSER_LIGHTNING.play(world, null, Vec3.atCenterOf(regions[connection].seed), 0.65f, 1.3f)
        }
        if (age == (animation.impact - InfuserVisuals.collapseDuration(animation)).toInt())
            ClockworkSounds.WAND_WELD.play(world, null, soundCenter, 1f, 0.75f)
        if (animation.shouldImpact(world.gameTime, impacted)) {
            impacted = true
            assemble(world)
            ClockworkSounds.PHYSICS_INFUSER_FINISH.play(world, null, soundCenter, 1.25f, 0.9f)
            ClockworkSounds.WAND_FINISH.play(world, null, soundCenter, 0.8f, 0.7f)
            if (regions.any { it.shipId >= 0 }) {
                val player = initiator?.let { world.server.playerList.getPlayer(it) }
                val advancement = world.server.advancements.getAdvancement(ResourceLocation("vs_clockwork", "infuser/awakening"))
                if (player != null && advancement != null) player.advancements.award(advancement, "activate")
            }
            planned = emptyList()
            setChanged()
        }
        if (age >= animation.settled) {
            completed = true
            if (!inventory[0].isEmpty) {
                val item = ItemEntity(world, center.x, center.y + 0.65, center.z, inventory[0])
                inventory[0] = ItemStack.EMPTY
                item.deltaMovement = Vec3(0.0, 0.18, 0.0)
                item.setDefaultPickUpDelay()
                world.addFreshEntity(item)
            }
            setChanged()
        }
    }

    private fun soundPosition(world: ServerLevel): Vec3 {
        val point = org.joml.Vector3d(worldPosition.x + 0.5, worldPosition.y + 0.5, worldPosition.z + 0.5)
        world.getLoadedShipManagingPos(worldPosition)?.shipToWorld?.transformPosition(point)
        return Vec3(point.x, point.y, point.z)
    }

    private fun assemble(world: ServerLevel) {
        for ((index, original) in planned.withIndex()) {
            if (original.any { !world.hasChunkAt(it) }) continue
            val blocks = original.filterTo(hashSetOf()) { !world.getBlockState(it).isAir }
            if (blocks.isEmpty() || blocks.any {
                    ClockworkConfig.SERVER.blockBlacklist.contains(world.getBlockState(it).block.descriptionId)
                }) continue
            try {
                val sourceCenter = org.joml.Vector3d(
                    (blocks.minOf { it.x }.toDouble() + blocks.maxOf { it.x } + 1) / 2,
                    (blocks.minOf { it.y }.toDouble() + blocks.maxOf { it.y } + 1) / 2,
                    (blocks.minOf { it.z }.toDouble() + blocks.maxOf { it.z } + 1) / 2)
                val result = ShipAssembler.assembleToShipFull(world, blocks, 1.0)
                val shift = result.toCenter.sub(sourceCenter, org.joml.Vector3d())
                regions[index].shipId = result.ship.id
                regions[index].offset = BlockPos(shift.x.roundToInt(), shift.y.roundToInt(), shift.z.roundToInt())
            } catch (error: Throwable) {
                // The assembler reports an unsuccessful block transfer with AssertionError.
                if (error !is Exception && error !is AssertionError) throw error
                ClockworkMod.LOGGER.error("Infuser assembly failed at {}", worldPosition, error)
                initiator?.let { world.server.playerList.getPlayer(it) }?.displayClientMessage(
                    Component.translatable("message.vs_clockwork.infuser.failed"), false)
            }
        }
    }

    public override fun write(tag: CompoundTag, clientPacket: Boolean) {
        super.write(tag, clientPacket)
        ContainerHelper.saveAllItems(tag, inventory)
        tag.putBoolean("Powered", powered)
        tag.putBoolean("Impacted", impacted)
        tag.putBoolean("Completed", completed)
        initiator?.let { tag.putUUID("Initiator", it) }
        sequence?.let { animation ->
            tag.put("Sequence", CompoundTag().also {
                it.putLong("Start", animation.startedAt); it.putBoolean("First", animation.firstUse)
                it.putInt("Skip", animation.skippedAt)
            })
        }
        tag.put("Selection", selection)
        tag.put("Regions", ListTag().also { list -> regions.forEach { list.add(it.save()) } })
        if (!clientPacket) tag.put("Planned", ListTag().also { list -> planned.forEach { blocks ->
            list.add(CompoundTag().also { it.putLongArray("Blocks", blocks.map(BlockPos::asLong)) })
        } })
    }

    public override fun read(tag: CompoundTag, clientPacket: Boolean) {
        super.read(tag, clientPacket)
        inventory = NonNullList.withSize(1, ItemStack.EMPTY)
        ContainerHelper.loadAllItems(tag, inventory)
        powered = tag.getBoolean("Powered")
        impacted = tag.getBoolean("Impacted")
        completed = !tag.contains("Sequence") || tag.getBoolean("Completed")
        initiator = if (tag.hasUUID("Initiator")) tag.getUUID("Initiator") else null
        sequence = if (tag.contains("Sequence")) tag.getCompound("Sequence").let {
            InfuserSequence(it.getLong("Start"), it.getBoolean("First"), it.getInt("Skip"))
        } else null
        selection = tag.getCompound("Selection")
        regions = tag.getList("Regions", Tag.TAG_COMPOUND.toInt()).map { InfuserRegion.load(it as CompoundTag) }
        if (!clientPacket) planned = tag.getList("Planned", Tag.TAG_COMPOUND.toInt()).map {
            (it as CompoundTag).getLongArray("Blocks").mapTo(hashSetOf(), BlockPos::of)
        }
    }

    val pulseRange get() = 64.0
    val scanGrowthDuration get() = 2000
    fun computeRadius(start: Long, duration: Float) =
        (64f * ((System.currentTimeMillis() - start) / duration).coerceIn(0f, 1f))
}
