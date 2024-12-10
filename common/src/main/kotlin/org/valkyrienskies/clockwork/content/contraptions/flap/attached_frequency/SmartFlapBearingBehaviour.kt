package org.valkyrienskies.clockwork.content.contraptions.flap.attached_frequency

import com.simibubi.create.Create
import com.simibubi.create.content.equipment.clipboard.ClipboardCloneable
import com.simibubi.create.content.redstone.link.IRedstoneLinkable
import com.simibubi.create.content.redstone.link.LinkBehaviour
import com.simibubi.create.content.redstone.link.RedstoneLinkNetworkHandler
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity
import com.simibubi.create.foundation.blockEntity.behaviour.BehaviourType
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour
import com.simibubi.create.foundation.blockEntity.behaviour.ValueBoxTransform
import com.simibubi.create.foundation.utility.Couple
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.nbt.CompoundTag
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.phys.Vec3
import org.apache.commons.lang3.tuple.Pair
import java.util.function.Function
import java.util.function.IntConsumer
import java.util.function.IntSupplier


open class SmartFlapBearingBehaviour(
    be: SmartBlockEntity?,
    slots: Pair<ValueBoxTransform, ValueBoxTransform?>,
    private val signalCallback: IntConsumer,
    front: Boolean
) :
    BlockEntityBehaviour(be), IRedstoneLinkable,
    ClipboardCloneable {

    val front = front

    var frequencyFirst: RedstoneLinkNetworkHandler.Frequency
    var frequencyLast: RedstoneLinkNetworkHandler.Frequency
    var firstSlot: ValueBoxTransform
    var secondSlot: ValueBoxTransform
    var textShift: Vec3

    var newPosition: Boolean = true

    init {
        frequencyFirst = RedstoneLinkNetworkHandler.Frequency.EMPTY
        frequencyLast = RedstoneLinkNetworkHandler.Frequency.EMPTY
        firstSlot = slots.left
        secondSlot = slots.right!!
        textShift = Vec3.ZERO
    }

    fun moveText(shift: Vec3): SmartFlapBearingBehaviour {
        textShift = shift
        return this
    }

    fun copyItemsFrom(behaviour: SmartFlapBearingBehaviour?) {
        if (behaviour == null) return
        frequencyFirst = behaviour.frequencyFirst
        frequencyLast = behaviour.frequencyLast
    }

    override fun isListening(): Boolean {
        return true
    }

    override fun getTransmittedStrength(): Int {
        return 0
    }

    override fun setReceivedStrength(networkPower: Int) {
        if (!newPosition) return
        signalCallback!!.accept(networkPower)
    }

    fun notifySignalChange() {
        Create.REDSTONE_LINK_NETWORK_HANDLER.updateNetworkOf(world, this)
    }

    override fun initialize() {
        super.initialize()
        if (world.isClientSide) return
        handler.addToNetwork(world, this)
        newPosition = true
    }

    override fun getNetworkKey(): Couple<RedstoneLinkNetworkHandler.Frequency> {
        return Couple.create(frequencyFirst, frequencyLast)
    }

    override fun unload() {
        super.unload()
        if (world.isClientSide) return
        handler.removeFromNetwork(world, this)
    }

    override fun isSafeNBT(): Boolean {
        return true
    }

    override fun write(nbt: CompoundTag, clientPacket: Boolean) {
        super.write(nbt, clientPacket)
        nbt.put(
            "FrequencyFirst", frequencyFirst.stack
                .save(CompoundTag())
        )
        nbt.put(
            "FrequencyLast", frequencyLast.stack
                .save(CompoundTag())
        )
        nbt.putLong(
            "LastKnownPosition", blockEntity.blockPos
                .asLong()
        )
    }

    override fun read(nbt: CompoundTag, clientPacket: Boolean) {
        val positionInTag = blockEntity.blockPos
            .asLong()
        val positionKey = nbt.getLong("LastKnownPosition")
        newPosition = positionInTag != positionKey

        super.read(nbt, clientPacket)
        frequencyFirst = RedstoneLinkNetworkHandler.Frequency.of(ItemStack.of(nbt.getCompound("FrequencyFirst")))
        frequencyLast = RedstoneLinkNetworkHandler.Frequency.of(ItemStack.of(nbt.getCompound("FrequencyLast")))
    }

    fun setFrequency(first: Boolean, stack: ItemStack) {
        println("stack ${stack}")
        var stack = stack
        stack = stack.copy()
        stack.count = 1
        val toCompare = if (first) frequencyFirst.stack else frequencyLast.stack
        val changed = !ItemStack.isSame(stack, toCompare) || !ItemStack.tagMatches(stack, toCompare)

        if (changed) handler.removeFromNetwork(world, this)

        if (first) frequencyFirst = RedstoneLinkNetworkHandler.Frequency.of(stack)
        else frequencyLast = RedstoneLinkNetworkHandler.Frequency.of(stack)

        if (!changed) return

        blockEntity.sendData()
        handler.addToNetwork(world, this)
    }

    override fun getType(): BehaviourType<*> {
        return if (front) FRONT_TYPE else BACK_TYPE
    }

    private val handler: RedstoneLinkNetworkHandler
        get() = Create.REDSTONE_LINK_NETWORK_HANDLER

    class SlotPositioning(
        var offsets: Function<BlockState, Pair<Vec3, Vec3>>,
        var rotation: Function<BlockState, Vec3>
    ) {
        var scale: Float = 1f

        fun scale(scale: Float): SmartFlapBearingBehaviour.SlotPositioning {
            this.scale = scale
            return this
        }
    }

    fun testHit(first: Boolean, hit: Vec3): Boolean {
        val state = blockEntity.blockState
        val localHit = hit.subtract(Vec3.atLowerCornerOf(blockEntity.blockPos))
        val slot = (if (first) firstSlot else secondSlot)
        return slot.testHit(state, localHit)
    }

    override fun isAlive(): Boolean {
        val level = world
        val pos = pos
        if (blockEntity.isChunkUnloaded) return false
        if (blockEntity.isRemoved) return false
        if (!level.isLoaded(pos)) return false
        return level.getBlockEntity(pos) === blockEntity
    }

    override fun getLocation(): BlockPos {
        return pos
    }

    override fun getClipboardKey(): String {
        return "Frequencies"
    }

    override fun writeToClipboard(tag: CompoundTag, side: Direction): Boolean {
        tag.put(
            "First", frequencyFirst.stack
                .save(CompoundTag())
        )
        tag.put(
            "Last", frequencyLast.stack
                .save(CompoundTag())
        )
        return true
    }

    override fun readFromClipboard(tag: CompoundTag, player: Player, side: Direction, simulate: Boolean): Boolean {
        if (!tag.contains("First") || !tag.contains("Last")) return false
        if (simulate) return true
        setFrequency(true, ItemStack.of(tag.getCompound("First")))
        setFrequency(false, ItemStack.of(tag.getCompound("Last")))
        return true
    }

    companion object {
        val FRONT_TYPE: BehaviourType<SmartFlapBearingBehaviour> = BehaviourType()
        val BACK_TYPE: BehaviourType<SmartFlapBearingBehaviour> = BehaviourType()


    }
}