package org.valkyrienskies.clockwork.content.logistics.gas.pump

import net.minecraft.core.BlockPos
import net.minecraft.nbt.CompoundTag
import net.minecraft.network.chat.Component
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.Container
import net.minecraft.world.SimpleContainer
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.phys.Vec3
import org.valkyrienskies.clockwork.ClockworkGasses
import org.valkyrienskies.clockwork.ClockworkMod
import org.valkyrienskies.clockwork.content.logistics.gas.GasTransfer
import org.valkyrienskies.kelvin.util.GasPhysics
import org.valkyrienskies.mod.api.dimensionId
import org.valkyrienskies.mod.common.getShipManagingPos
import org.valkyrienskies.mod.common.shipObjectWorld
import org.valkyrienskies.mod.common.toWorldCoordinates
import kotlin.math.min

class OzoniteGeneratorBlockEntity private constructor(
    type: BlockEntityType<*>, pos: BlockPos, state: BlockState, private val lapis: SimpleContainer,
) : PumpDuctBlockEntity(type, pos, state), Container by lapis {

    constructor(type: BlockEntityType<*>, pos: BlockPos, state: BlockState) : this(type, pos, state, SimpleContainer(1))

    private var remainingCharge = 0.0

    init {
        lapis.addListener {
            setChanged()
            sendData()
        }
    }

    override fun setChanged() = super.setChanged()

    override fun canPlaceItem(slot: Int, stack: ItemStack) = slot == 0 && stack.`is`(Items.LAPIS_LAZULI)

    fun loadLapis(stack: ItemStack, creative: Boolean) {
        if (!canPlaceItem(0, stack)) return
        val remaining = lapis.addItem(stack)
        if (!creative) stack.count = remaining.count
    }

    override fun tick() {
        super.tick()
        val world = level as? ServerLevel ?: return
        if (dataToLoad != null || speedFraction <= 0.0) return
        val needsLapis = remainingCharge <= GasTransfer.MIN_MASS
        if (needsLapis && !canPlaceItem(0, lapis.getItem(0))) return

        val intake = blockPos.relative(facing.opposite)
        if (!world.hasChunkAt(intake) || !world.getBlockState(intake).isAir) return
        val intakeCenter = Vec3.atCenterOf(intake)
        val worldIntakeCenter = world.getShipManagingPos(blockPos)?.toWorldCoordinates(intakeCenter) ?: intakeCenter
        val outside = BlockPos.containing(worldIntakeCenter)
        if (outside != intake && (!world.hasChunkAt(outside) || !world.getBlockState(outside).isAir)) return

        val network = ClockworkMod.getKelvin(world)
        val outlet = GasTransfer.port(network, getDuctNodePosition()) ?: return
        if (outlet.pressure >= MAX_OUTLET_PRESSURE) return

        val y = worldIntakeCenter.y
        val atmosphere = world.shipObjectWorld.aerodynamicUtils
        val density = atmosphere.getAirDensityForY(y, world.dimensionId)
        val temperature = atmosphere.getAirTemperatureForY(y, world.dimensionId)
        if (!density.isFinite() || density <= 0.0 || !temperature.isFinite() || temperature <= 0.0) return

        val capacity = if (needsLapis) OZONITE_PER_LAPIS else remainingCharge
        val mass = min(capacity, MAX_MASS_PER_TICK * speedFraction * (density / SEA_LEVEL_DENSITY).coerceAtMost(1.0))
        val products = mapOf(ClockworkGasses.OZONITE to mass)
        val energy = GasPhysics.mixtureCapacity(products) * temperature
        val fraction = outlet.allowedFraction(products, energy, MAX_OUTLET_PRESSURE, outlet.node.maxTemperature)
        val produced = mass * fraction
        if (produced <= GasTransfer.MIN_MASS) return

        remainingCharge = (capacity - produced).coerceAtLeast(0.0)
        if (needsLapis) lapis.removeItem(0, 1)
        GasTransfer.add(network, outlet, products, energy, fraction)
        setChanged()
    }

    override fun write(tag: CompoundTag, clientPacket: Boolean) {
        super.write(tag, clientPacket)
        tag.put("Lapis", lapis.getItem(0).save(CompoundTag()))
        if (!clientPacket) tag.putDouble("RemainingCharge", remainingCharge)
    }

    override fun read(tag: CompoundTag, clientPacket: Boolean) {
        super.read(tag, clientPacket)
        val stack = ItemStack.of(tag.getCompound("Lapis"))
        lapis.setItem(0, if (canPlaceItem(0, stack)) stack else ItemStack.EMPTY)
        if (!clientPacket) {
            remainingCharge = tag.getDouble("RemainingCharge").takeIf { it.isFinite() }
                ?.coerceIn(0.0, OZONITE_PER_LAPIS) ?: 0.0
        }
    }

    override fun addToGoggleTooltip(tooltip: List<Component>?, isPlayerSneaking: Boolean): Boolean {
        (tooltip as? MutableList)?.add(Component.translatable("vs_clockwork.ozonite_generator.lapis", lapis.getItem(0).count))
        return super.addToGoggleTooltip(tooltip, isPlayerSneaking) || tooltip != null
    }

    companion object {
        private const val OZONITE_PER_LAPIS = 0.025
        private const val MAX_MASS_PER_TICK = 0.0008
        private const val MAX_OUTLET_PRESSURE = 1_000_000.0
        private const val SEA_LEVEL_DENSITY = 1.225
    }
}
