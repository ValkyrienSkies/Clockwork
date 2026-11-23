package org.valkyrienskies.clockwork.content.logistics.gas.generation.intake

import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour
import net.minecraft.core.BlockPos
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.block.state.properties.BlockStateProperties
import org.joml.Vector3d
import org.valkyrienskies.clockwork.ClockworkMod
import org.valkyrienskies.clockwork.content.logistics.gas.GasTransfer
import org.valkyrienskies.clockwork.content.logistics.gas.IConnectable
import org.valkyrienskies.clockwork.util.kelvin.KNodeBlockEntity
import org.valkyrienskies.kelvin.impl.registry.GasTypeRegistry
import org.valkyrienskies.kelvin.util.GasPhysics
import org.valkyrienskies.mod.api.dimensionId
import org.valkyrienskies.mod.common.getShipObjectManagingPos
import org.valkyrienskies.mod.common.shipObjectWorld
import org.valkyrienskies.mod.common.util.toJOMLD
import kotlin.math.min

class AirIntakeBlockEntity(type: BlockEntityType<*>, pos: BlockPos, state: BlockState) :
    KNodeBlockEntity(type, pos, state), IConnectable {

    override fun addBehaviours(behaviours: MutableList<BlockEntityBehaviour>) {}

    override fun tick() {
        super.tick()
        val world = level as? ServerLevel ?: return
        if (dataToLoad != null) return
        val ship = world.getShipObjectManagingPos(blockPos) ?: return
        val facing = blockState.getValue(BlockStateProperties.FACING)
        val openPos = blockPos.relative(facing)
        if (!world.hasChunkAt(openPos) || !world.getBlockState(openPos).isAir) return

        val transform = ship.transform
        val normal = facing.normal.toJOMLD()
        val mouth = transform.shipToWorld.transformPosition(blockPos.toJOMLD().add(0.5, 0.5, 0.5).fma(0.5, normal))
        val axis = transform.shipToWorldRotation.transform(normal)
        val arm = mouth.sub(transform.positionInWorld, Vector3d())
        val axialSpeed = ship.angularVelocity.cross(arm, Vector3d()).add(ship.velocity).dot(axis)
        if (!axialSpeed.isFinite() || axialSpeed <= 0.0) return

        val outside = transform.shipToWorld.transformPosition(openPos.toJOMLD().add(0.5, 0.5, 0.5))
        val outsidePos = BlockPos.containing(outside.x, outside.y, outside.z)
        if (!world.hasChunkAt(outsidePos) || !world.getBlockState(outsidePos).isAir) return

        val atmosphere = world.shipObjectWorld.aerodynamicUtils
        val dimension = world.dimensionId
        val density = atmosphere.getAirDensityAt(ship, mouth.x, mouth.y, mouth.z, dimension)
        val pressure = atmosphere.getAirPressureAt(ship, mouth.x, mouth.y, mouth.z, dimension)
        val capture = captureAir(axialSpeed, density, pressure) ?: return
        val temperature = atmosphere.getAirTemperatureAt(ship, mouth.x, mouth.y, mouth.z, dimension)
        if (!temperature.isFinite() || temperature <= 0.0) return

        val network = ClockworkMod.getKelvin(world)
        val outlet = GasTransfer.port(network, getDuctNodePosition()) ?: return
        val pressureLimit = min(capture.pressure, outlet.node.maxPressure)
        if (outlet.pressure >= pressureLimit) return
        val air = GasTypeRegistry.getGasType("kelvin", "air") ?: return
        val products = mapOf(air to capture.massPerTick)
        val energy = GasPhysics.mixtureCapacity(products) * temperature
        val fraction = outlet.allowedFraction(products, energy, pressureLimit, outlet.node.maxTemperature)
        if (capture.massPerTick * fraction > GasTransfer.MIN_MASS) {
            GasTransfer.add(network, outlet, products, energy, fraction)
        }
    }
}

private const val CAPTURE_AREA = 0.05
private const val TICKS_PER_SECOND = 20.0

internal data class AirIntakeCapture(val massPerTick: Double, val pressure: Double)

internal fun captureAir(axialSpeed: Double, density: Double, ambientPressure: Double): AirIntakeCapture? {
    if (!axialSpeed.isFinite() || axialSpeed <= 0.0 || !density.isFinite() || density <= 0.0 ||
        !ambientPressure.isFinite() || ambientPressure < 0.0
    ) return null

    val mass = density * axialSpeed * CAPTURE_AREA / TICKS_PER_SECOND
    val pressure = ambientPressure + 0.5 * density * axialSpeed * axialSpeed
    if (!mass.isFinite() || !pressure.isFinite()) return null
    return AirIntakeCapture(mass, pressure)
}
