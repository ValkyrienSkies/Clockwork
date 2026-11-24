package org.valkyrienskies.clockwork.content.ponders

import com.simibubi.create.content.kinetics.base.KineticBlockEntity
import com.simibubi.create.content.kinetics.gauge.SpeedGaugeBlockEntity
import com.simibubi.create.foundation.ponder.CreateSceneBuilder
import net.createmod.catnip.math.Pointing
import net.createmod.ponder.api.PonderPalette
import net.createmod.ponder.api.scene.SceneBuilder
import net.createmod.ponder.api.scene.SceneBuildingUtil
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.level.block.Blocks
import org.valkyrienskies.clockwork.ClockworkBlocks
import org.valkyrienskies.clockwork.ClockworkConfig
import org.valkyrienskies.clockwork.ClockworkGasses
import org.valkyrienskies.clockwork.content.logistics.gas.engine.GasEngineBlockEntity
import org.valkyrienskies.clockwork.content.logistics.gas.engine.GasEngineLogic
import org.valkyrienskies.clockwork.content.logistics.gas.engine.SterlingEngineBlockEntity
import org.valkyrienskies.clockwork.content.logistics.gas.generation.creative_generator.CreativeGeneratorBlockEntity
import org.valkyrienskies.clockwork.content.logistics.gas.pump.PumpDuctBlockEntity
import org.valkyrienskies.kelvin.api.GasType
import org.valkyrienskies.kelvin.impl.registry.GasTypeRegistry

object GasMachineryPonders {
    private fun scene(builder: SceneBuilder, id: String, title: String): CreateSceneBuilder {
        val scene = CreateSceneBuilder(builder)
        scene.title(id, title)
        scene.configureBasePlate(0, 0, 5)
        scene.showBasePlate()
        scene.idle(10)
        return scene
    }

    private fun CreateSceneBuilder.caption(
        util: SceneBuildingUtil, pos: BlockPos, text: String,
        color: PonderPalette = PonderPalette.WHITE, gas: GasType? = null
    ) {
        val duration = (46 + 6 * text.count { it == ' ' }).coerceAtLeast(90)
        val anchor = util.vector().topOf(pos)
        if (gas != null) overlay().showControls(anchor, Pointing.RIGHT, duration).withGas(gas)
        overlay().showText(duration).text(text).pointAt(anchor).placeNearTarget().colored(color).attachKeyFrame()
        idle(duration + 15)
    }

    private fun CreateSceneBuilder.supplySteam(pos: BlockPos, grams: Int, temperature: Double) {
        world().modifyBlockEntity(pos, CreativeGeneratorBlockEntity::class.java) {
            it.gasValues[ClockworkGasses.STEAM] = grams
            it.temperature = temperature
        }
    }

    fun gasEngine(builder: SceneBuilder, util: SceneBuildingUtil) {
        val scene = scene(builder, "gas_engine", "Driving Steam Engines with hot gas")
        val engine = util.grid().at(2, 1, 1)
        val piston = engine.south()
        val shaft = piston.south(2)
        val gauge = shaft.east()
        val second = engine.west()

        fun updateDrive() {
            scene.addInstruction { step ->
                val source = step.world.getBlockEntity(engine) as GasEngineBlockEntity
                val efficiency = source.getEngineEfficiency()
                val rpm = if (efficiency > 0) 16f * GasEngineLogic.getSpeedModifier(efficiency) else 0f
                (step.world.getBlockEntity(shaft) as KineticBlockEntity).speed = rpm
                (step.world.getBlockEntity(gauge) as SpeedGaugeBlockEntity).apply {
                    speed = rpm
                    dialTarget = SpeedGaugeBlockEntity.getDialTarget(rpm)
                }
            }
        }

        scene.world().setBlock(second, ClockworkBlocks.DUCT.defaultState, false)
        scene.world().showSection(util.select().fromTo(0, 1, 1, 4, 1, 1), Direction.DOWN)
        scene.supplySteam(engine.east(2), 400, 600.0)
        scene.idle(20)
        scene.caption(util, engine, "Pass hot gas through a Gas Engine by connecting Ducts to both ends")
        scene.world().showSection(util.select().position(piston), Direction.NORTH)
        scene.idle(12)
        scene.caption(util, piston, "Attach a Steam Engine to a free side to put the heat and flow to work")
        scene.world().showSection(util.select().fromTo(shaft, gauge), Direction.DOWN)
        scene.world().modifyBlockEntity(engine, GasEngineBlockEntity::class.java) { it.attachedEngines = 1 }
        scene.idle(12)
        updateDrive()
        scene.effects().rotationDirectionIndicator(shaft)
        scene.caption(util, shaft, "Place a Shaft two blocks out from the Steam Engine, leaving room for its piston", PonderPalette.OUTPUT)
        scene.supplySteam(engine.east(2), 900, 1250.0)
        scene.idle(50)
        updateDrive()
        scene.effects().rotationSpeedIndicator(gauge)
        scene.caption(util, gauge, "Hotter gas and a higher flow rate increase the shaft's RPM and Stress Capacity", PonderPalette.OUTPUT)
        scene.supplySteam(engine.east(2), 0, 1250.0)
        scene.idle(50)
        updateDrive()
        scene.caption(util, engine, "When the gas stops flowing, the engine stops supplying stress capacity")

        scene.world().restoreBlocks(util.select().position(second))
        scene.world().showSection(util.select().position(second.south()), Direction.NORTH)
        scene.world().showSection(util.select().position(shaft.west()), Direction.DOWN)
        scene.world().modifyBlockEntity(second, GasEngineBlockEntity::class.java) { it.attachedEngines = 1 }
        scene.supplySteam(engine.east(2), 900, 1250.0)
        scene.idle(50)
        updateDrive()
        scene.addInstruction { step ->
            val rpm = (step.world.getBlockEntity(shaft) as KineticBlockEntity).speed
            (step.world.getBlockEntity(shaft.west()) as KineticBlockEntity).speed = rpm
        }
        scene.caption(util, second, "Connect Gas Engines in series to drive several pistons with the same stream of gas")
        scene.markAsFinished()
    }

    fun sterlingEngine(builder: SceneBuilder, util: SceneBuildingUtil) {
        val scene = scene(builder, "sterling_engine", "Generating rotation with a Sterling Engine")
        val engine = util.grid().at(2, 1, 2)
        val gear = engine.south()
        val second = engine.west()

        fun updateDrive(enginePos: BlockPos, vararg driven: BlockPos) {
            scene.world().modifyBlockEntity(enginePos, SterlingEngineBlockEntity::class.java) { source ->
                val level = source.level ?: return@modifyBlockEntity
                source.efficiency = GasEngineLogic.calculateTemperatureEfficiency(
                    level, source.getDuctNodePosition(), ClockworkConfig.SERVER.gasEngine.sterlingEngineTemperatureIncrement
                )
                source.speed = source.getGeneratedSpeed()
                for (pos in driven) (level.getBlockEntity(pos) as? KineticBlockEntity)?.speed = -source.speed
            }
        }

        scene.world().setBlock(second, Blocks.AIR.defaultBlockState(), false)
        scene.world().showSection(util.select().fromTo(engine, engine.east(2)), Direction.DOWN)
        scene.idle(15)
        scene.caption(util, engine.east(), "Supply a Sterling Engine with hot gas through either end", PonderPalette.INPUT)
        scene.supplySteam(engine.east(2), 400, 600.0)
        scene.idle(35)
        updateDrive(engine)
        scene.effects().rotationDirectionIndicator(engine)
        scene.caption(util, engine, "Heat in the gas turns the engine's built-in cogwheel")
        scene.world().showSection(util.select().fromTo(gear, gear.west()), Direction.DOWN)
        updateDrive(engine, gear, gear.west())
        scene.effects().rotationDirectionIndicator(gear)
        scene.caption(util, gear, "Mesh a Cogwheel with the engine to take rotation out of the gas line", PonderPalette.OUTPUT)
        scene.supplySteam(engine.east(2), 1200, 1250.0)
        scene.idle(50)
        updateDrive(engine, gear, gear.west())
        scene.effects().rotationSpeedIndicator(gear)
        scene.caption(util, gear, "As the gas gets hotter, the engine turns at a higher RPM", PonderPalette.OUTPUT)

        scene.world().restoreBlocks(util.select().position(second))
        scene.world().showSection(util.select().position(second), Direction.EAST)
        scene.world().showSection(util.select().position(second.north()), Direction.SOUTH)
        scene.idle(40)
        updateDrive(second, second.north())
        scene.effects().rotationDirectionIndicator(second.north())
        scene.caption(util, second, "Line up the ports of several Sterling Engines to supply them with hot gas in series")
        scene.markAsFinished()
    }

    fun airCompressor(builder: SceneBuilder, util: SceneBuildingUtil) {
        val scene = scene(builder, "air_compressor", "Compressing air into ducts")
        val compressor = util.grid().at(3, 2, 2)
        val intake = compressor.above()
        scene.world().setBlock(intake, Blocks.AIR.defaultBlockState(), false)
        scene.world().showSection(util.select().position(compressor.below()), Direction.UP)
        scene.world().showSection(util.select().position(compressor), Direction.DOWN)
        scene.idle(15)
        scene.world().setKineticSpeed(util.select().fromTo(compressor.below(2), compressor), 96f)
        scene.effects().rotationDirectionIndicator(compressor.below())
        scene.caption(util, compressor.below(), "Drive an Air Compressor from below to draw in the surrounding air", PonderPalette.INPUT)
        scene.world().showSection(util.select().fromTo(compressor.west(), compressor.west(2)), Direction.EAST)
        scene.idle(15)
        scene.caption(util, compressor.west(), "Connect a duct to carry the compressed air away", PonderPalette.OUTPUT)
        scene.world().restoreBlocks(util.select().position(intake))
        scene.world().showSection(util.select().position(intake), Direction.DOWN)
        scene.idle(12)
        scene.caption(util, intake, "Covering the intake stops the compressor, even while its shaft is turning", PonderPalette.RED)
        scene.world().destroyBlock(intake)
        scene.idle(25)
        val air = GasTypeRegistry.GAS_TYPES.getValue(ResourceLocation("kelvin", "air"))
        scene.caption(util, intake, "Altitude and dimension determine the intake temperature and how much of the gas is Air", gas = air)
        scene.caption(util, compressor, "In thin air the Compressor also collects Aether. In the Overworld, this starts around Y = 350", PonderPalette.OUTPUT, ClockworkGasses.HELIUM)
        scene.markAsFinished()
    }

    fun gasValve(builder: SceneBuilder, util: SceneBuildingUtil) {
        val scene = scene(builder, "gas_valve", "Controlling gas flow with a Valve")
        val valve = util.grid().at(2, 1, 2)
        val drive = util.select().fromTo(valve, valve.south(2))
        scene.world().showSection(util.select().fromTo(valve.west(2), valve.east(2)), Direction.DOWN)
        scene.supplySteam(valve.east(2), 200, 373.15)
        scene.idle(15)
        scene.caption(util, valve, "A closed Gas Valve cuts off the flow between its two ducts")
        scene.world().showSection(util.select().fromTo(valve.south(), valve.south(2)), Direction.NORTH)
        scene.world().setKineticSpeed(drive, 32f)
        scene.effects().rotationDirectionIndicator(valve.south())
        scene.idle(25)
        scene.caption(util, valve, "Turn the shaft to open the valve and let gas through", PonderPalette.OUTPUT)
        scene.world().setKineticSpeed(drive, -16f)
        scene.effects().rotationDirectionIndicator(valve.south())
        scene.idle(20)
        scene.world().setKineticSpeed(drive, 0f)
        scene.caption(util, valve, "Reverse the shaft to close the valve. Stop partway to hold a smaller opening")
        scene.world().setKineticSpeed(drive, -32f)
        scene.idle(25)
        scene.world().setKineticSpeed(drive, 0f)
        scene.markAsFinished()
    }

    fun gasPump(builder: SceneBuilder, util: SceneBuildingUtil) {
        val scene = scene(builder, "gas_pump", "Raising gas pressure with a Pump")
        val pump = util.grid().at(2, 1, 2)
        val gear = pump.south()
        val drive = util.select().fromTo(gear, gear.east())
        scene.world().showSection(util.select().fromTo(pump.west(2), pump.east(2)), Direction.DOWN)
        scene.idle(15)
        scene.caption(util, pump, "Place a Gas Pump in a duct with its outlet facing the destination")
        scene.world().showSection(drive, Direction.NORTH)
        scene.world().setKineticSpeed(drive, 16f)
        scene.world().setKineticSpeed(util.select().position(pump), -16f)
        scene.supplySteam(pump.east(2), 200, 373.15)
        scene.world().modifyBlockEntity(pump, PumpDuctBlockEntity::class.java) { it.updatePumpNode() }
        scene.effects().rotationDirectionIndicator(gear)
        scene.caption(util, gear, "Drive the pump's cogwheel to push gas towards its outlet", PonderPalette.INPUT)
        scene.world().setKineticSpeed(drive, 64f)
        scene.world().setKineticSpeed(util.select().position(pump), -64f)
        scene.world().modifyBlockEntity(pump, PumpDuctBlockEntity::class.java) { it.updatePumpNode() }
        scene.effects().rotationSpeedIndicator(gear)
        scene.caption(util, pump.west(), "A faster pump can push against more pressure on the outlet side", PonderPalette.OUTPUT)
        scene.markAsFinished()
    }
}
