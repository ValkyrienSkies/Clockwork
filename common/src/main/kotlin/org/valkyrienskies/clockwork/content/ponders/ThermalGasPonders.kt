package org.valkyrienskies.clockwork.content.ponders

import com.simibubi.create.AllItems
import com.simibubi.create.content.fluids.tank.FluidTankBlockEntity
import com.simibubi.create.foundation.ponder.CreateSceneBuilder
import dev.architectury.fluid.FluidStack
import net.createmod.catnip.math.Pointing
import net.createmod.ponder.api.PonderPalette
import net.createmod.ponder.api.scene.SceneBuilder
import net.createmod.ponder.api.scene.SceneBuildingUtil
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.core.particles.DustParticleOptions
import net.minecraft.core.particles.ParticleTypes
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.phys.Vec3
import org.joml.Vector3f
import org.valkyrienskies.clockwork.ClockworkBlocks
import org.valkyrienskies.clockwork.ClockworkConfig
import org.valkyrienskies.clockwork.ClockworkGasses
import org.valkyrienskies.clockwork.ClockworkModClient
import org.valkyrienskies.kelvin.api.GasType
import org.valkyrienskies.kelvin.util.KelvinExtensions.toDuctNodePos

object ThermalGasPonders {
    fun steamExtractor(builder: SceneBuilder, util: SceneBuildingUtil) {
        val scene = CreateSceneBuilder(builder)
        scene.title("steam_extractor", "Drawing Steam from a Boiler")
        scene.configureBasePlate(0, 0, 5)
        scene.showBasePlate()
        scene.idle(10)

        val tank = util.grid().at(3, 2, 2)
        val extractor = tank.west()
        val pump = util.grid().at(4, 1, 2)
        scene.world().showSection(util.select().fromTo(tank, util.grid().at(4, 2, 3)), Direction.DOWN)
        scene.idle(15)
        scene.overlay().showControls(util.vector().blockSurface(tank, Direction.WEST), Pointing.RIGHT, 30)
            .rightClick().withItem(ClockworkBlocks.STEAM_GENERATOR.asStack())
        scene.idle(10)
        scene.world().showSection(util.select().position(extractor), Direction.EAST)
        scene.world().modifyBlockEntityNBT(util.select().position(tank), FluidTankBlockEntity::class.java) { tag ->
            tag.getCompound("Boiler").putInt("Engines", 1)
        }
        scene.overlay().showText(115).attachKeyFrame()
            .text("Attach a Steam Extractor to a Fluid Tank to form a Boiler")
            .pointAt(util.vector().topOf(extractor)).placeNearTarget()
        scene.idle(130)

        scene.world().showSection(util.select().fromTo(3, 1, 1, 4, 1, 2), Direction.UP)
        scene.world().setKineticSpeed(util.select().position(pump), 64f)
        scene.world().setKineticSpeed(util.select().position(4, 1, 1), -64f)
        scene.overlay().showText(110).attachKeyFrame()
            .text("Heat the Boiler from below and pump Water into the tank")
            .pointAt(util.vector().topOf(pump)).placeNearTarget()
        scene.idle(120)

        scene.world().showSection(util.select().fromTo(0, 2, 2, 1, 2, 2), Direction.EAST)
        scene.world().modifyBlockEntityNBT(util.select().position(tank), FluidTankBlockEntity::class.java) { tag ->
            val boiler = tag.getCompound("Boiler")
            boiler.putInt("ActiveHeat", 1)
            boiler.putDouble("Supply", FluidStack.bucketAmount() / 100.0)
        }
        scene.idle(15)
        scene.overlay().showControls(util.vector().blockSurface(extractor.west(2), Direction.WEST), Pointing.RIGHT, 115)
            .withGas(ClockworkGasses.STEAM)
        scene.overlay().showText(115).attachKeyFrame()
            .text("Connect a Gas Duct to the outer face to carry Steam away")
            .pointAt(util.vector().blockSurface(extractor, Direction.WEST)).placeNearTarget()
        scene.idle(130)
        scene.markAsFinished()
    }

    fun radiator(builder: SceneBuilder, util: SceneBuildingUtil) {
        val scene = CreateSceneBuilder(builder)
        scene.title("radiator", "Cooling gas with Radiators")
        scene.configureBasePlate(0, 0, 5)
        scene.showBasePlate()
        scene.idle(10)

        val radiator = util.grid().at(2, 1, 2)
        val second = radiator.west()
        scene.world().showSection(util.select().position(radiator), Direction.DOWN)
        scene.idle(8)
        scene.world().showSection(util.select().fromTo(3, 1, 2, 4, 1, 2), Direction.WEST)
        scene.overlay().showText(100).attachKeyFrame()
            .text("Connect a Gas Duct to either end of a Radiator")
            .pointAt(util.vector().blockSurface(radiator, Direction.EAST)).placeNearTarget()
        scene.idle(115)

        supplyGas(scene, (0..4).map { BlockPos(it, 1, 2) }, ClockworkGasses.EXHAUST, 850.0)
        showTemperature(scene, radiator, 375)
        scene.overlay().showText(130).attachKeyFrame()
            .text("Heat escapes through the fins when the gas inside is hotter than the surrounding air")
            .pointAt(util.vector().topOf(radiator)).placeNearTarget()
        scene.idle(140)

        scene.world().showSection(util.select().position(second), Direction.EAST)
        scene.idle(5)
        scene.world().showSection(util.select().position(0, 1, 2), Direction.EAST)
        showTemperature(scene, second, 230)
        scene.overlay().showText(100).attachKeyFrame()
            .text("Add more Radiators in series to cool the gas faster")
            .pointAt(util.vector().topOf(second)).placeNearTarget()
        scene.idle(115)
        scene.overlay().showControls(util.vector().blockSurface(radiator, Direction.SOUTH), Pointing.DOWN, 45)
            .withItem(AllItems.GOGGLES.asStack())
        scene.overlay().showText(115)
            .text("Engineer's Goggles show the temperature as it falls towards the air temperature")
            .pointAt(util.vector().topOf(radiator)).placeNearTarget()
        scene.idle(130)
        scene.markAsFinished()
    }

    fun heatExchanger(builder: SceneBuilder, util: SceneBuildingUtil) {
        val scene = CreateSceneBuilder(builder)
        scene.title("heat_exchanger", "Transferring heat between gas lines")
        scene.configureBasePlate(0, 0, 5)
        scene.showBasePlate()
        scene.idle(10)

        val hot = util.grid().at(2, 1, 2)
        val cool = hot.south()
        val firstLine = util.select().fromTo(1, 1, 2, 3, 1, 2)
        val secondLine = util.select().fromTo(1, 1, 3, 3, 1, 3)
        scene.world().showSection(firstLine, Direction.DOWN)
        scene.idle(12)
        scene.world().showSection(secondLine, Direction.NORTH)
        scene.overlay().showText(125).attachKeyFrame()
            .text("Place two Heat Exchangers side by side, with their ports facing the same direction")
            .pointAt(util.vector().blockSurface(hot, Direction.SOUTH)).placeNearTarget()
        scene.idle(140)

        scene.overlay().showOutline(PonderPalette.RED, "hotGasLine", firstLine, 95)
        scene.overlay().showOutline(PonderPalette.BLUE, "coolGasLine", secondLine, 95)
        scene.overlay().showText(95).attachKeyFrame()
            .text("Keep the Ducts on each side in separate lines")
            .pointAt(util.vector().blockSurface(hot.west(), Direction.SOUTH)).placeNearTarget()
        scene.idle(110)

        supplyGas(scene, (1..3).map { BlockPos(it, 1, 2) }, ClockworkGasses.EXHAUST, 850.0)
        supplyGas(scene, (1..3).map { BlockPos(it, 1, 3) }, ClockworkGasses.HELIUM, 300.0)
        showTemperature(scene, hot, 260)
        showTemperature(scene, cool, 260)
        scene.overlay().showText(125).attachKeyFrame()
            .text("Heat passes from the hotter gas into the cooler gas through the touching sides")
            .pointAt(util.vector().blockSurface(hot, Direction.SOUTH)).placeNearTarget()
        scene.idle(140)
        scene.overlay().showText(120)
            .text("Their temperatures approach each other while the gases stay in their own lines")
            .pointAt(util.vector().topOf(cool)).placeNearTarget()
        scene.idle(135)
        scene.markAsFinished()
    }

    fun gasHeater(builder: SceneBuilder, util: SceneBuildingUtil) {
        val scene = CreateSceneBuilder(builder)
        scene.title("gas_heater", "Heating a Basin with a Gas Heater")
        scene.configureBasePlate(0, 0, 5)
        scene.showBasePlate()
        scene.idle(10)

        val heater = util.grid().at(2, 1, 2)
        val second = heater.west()
        val supply = (2..4).map { BlockPos(it, 1, 2) }
        scene.world().setBlock(second, Blocks.AIR.defaultBlockState(), false)
        scene.world().showSection(util.select().position(heater), Direction.DOWN)
        scene.idle(10)
        scene.world().showSection(util.select().fromTo(3, 1, 2, 4, 1, 2), Direction.WEST)
        scene.overlay().showText(120).attachKeyFrame()
            .text("Supply hot gas through either end of a Gas Heater, or from below")
            .pointAt(util.vector().blockSurface(heater, Direction.EAST)).placeNearTarget()
        scene.idle(135)

        val thresholds = ClockworkConfig.SERVER.gasHeater
        supplyGas(scene, supply, ClockworkGasses.EXHAUST, thresholds.heaterKindledTemp + 80.0)
        scene.world().showSection(util.select().position(heater.above()), Direction.DOWN)
        val rim = util.vector().blockSurface(heater, Direction.WEST).add(0.0, 0.4, 0.0)
        scene.effects().emitParticles(rim, scene.effects().simpleParticleEmitter(ParticleTypes.FLAME, Vec3.ZERO), .3f, 55)
        scene.overlay().showText(140).attachKeyFrame()
            .text("A hot Gas Heater can heat a Basin or a Steam Boiler placed directly above it")
            .pointAt(util.vector().blockSurface(heater.above(), Direction.WEST)).placeNearTarget()
        scene.idle(155)

        supplyGas(scene, supply, ClockworkGasses.EXHAUST, thresholds.heaterSeethingTemp + 80.0)
        scene.effects().emitParticles(rim, scene.effects().simpleParticleEmitter(ParticleTypes.SOUL_FIRE_FLAME, Vec3.ZERO), .3f, 60)
        scene.overlay().showText(130).attachKeyFrame()
            .text("Hotter gas can bring the Heater up to Superheated, allowing more recipes in the Basin")
            .pointAt(util.vector().topOf(heater)).placeNearTarget()
        scene.idle(140)
        scene.world().restoreBlocks(util.select().position(second))
        scene.world().showSection(util.select().position(second), Direction.EAST)
        scene.idle(10)
        scene.world().showSection(util.select().position(second.above()), Direction.DOWN)
        supplyGas(scene, listOf(heater.east(2)), ClockworkGasses.EXHAUST, thresholds.heaterSeethingTemp + 80.0, mass = 0.75)
        scene.overlay().showOutline(PonderPalette.INPUT, "heaterSeries", util.select().fromTo(second, heater), 130)
        scene.overlay().showText(130).attachKeyFrame()
            .text("Place Gas Heaters end to end to heat several Basins from the same gas line")
            .pointAt(util.vector().blockSurface(second, Direction.EAST)).placeNearTarget()
        scene.idle(145)
        scene.markAsFinished()
    }

    private fun supplyGas(scene: CreateSceneBuilder, positions: List<BlockPos>, gas: GasType, temperature: Double, mass: Double = 0.025) {
        scene.addInstruction { step ->
            val network = ClockworkModClient.getKelvin(step.world)
            for (pos in positions) {
                val node = pos.toDuctNodePos(step.world.dimension().location())
                network.addGasAtTemperature(node, gas, mass, temperature)
                network.modTemperature(node, temperature - network.getTemperatureAt(node))
            }
        }
    }

    private fun showTemperature(scene: CreateSceneBuilder, pos: BlockPos, duration: Int) {
        scene.effects().emitParticles(Vec3.atCenterOf(pos).add(0.0, .5, 0.0), { world, x, y, z ->
            val node = pos.toDuctNodePos(world.dimension().location())
            val temperature = ClockworkModClient.getKelvin(world).getTemperatureAt(node)
            val heat = ((temperature - 300) / 550).toFloat().coerceIn(0f, 1f)
            val color = Vector3f(.18f, .55f, 1f).lerp(Vector3f(1f, .23f, .07f), heat)
            world.addParticle(DustParticleOptions(color, .85f), x + (world.random.nextDouble() - .5) * .5,
                y, z + (world.random.nextDouble() - .5) * .5, 0.0, .03, 0.0)
        }, .6f, duration)
    }
}
