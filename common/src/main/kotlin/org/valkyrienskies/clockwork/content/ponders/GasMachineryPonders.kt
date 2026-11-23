package org.valkyrienskies.clockwork.content.ponders

import com.simibubi.create.content.kinetics.base.KineticBlockEntity
import com.simibubi.create.foundation.ponder.CreateSceneBuilder
import net.createmod.ponder.api.PonderPalette
import net.createmod.ponder.api.scene.SceneBuilder
import net.createmod.ponder.api.scene.SceneBuildingUtil
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.world.level.block.Blocks
import org.valkyrienskies.clockwork.ClockworkConfig
import org.valkyrienskies.clockwork.ClockworkGasses
import org.valkyrienskies.clockwork.content.logistics.gas.engine.GasEngineBlockEntity
import org.valkyrienskies.clockwork.content.logistics.gas.engine.GasEngineLogic
import org.valkyrienskies.clockwork.content.logistics.gas.engine.SterlingEngineBlockEntity
import org.valkyrienskies.clockwork.content.logistics.gas.generation.creative_generator.CreativeGeneratorBlockEntity
import org.valkyrienskies.clockwork.content.logistics.gas.pump.PumpDuctBlockEntity

object GasMachineryPonders {
    private fun scene(builder: SceneBuilder, id: String, title: String): CreateSceneBuilder {
        val scene = CreateSceneBuilder(builder)
        scene.title(id, title)
        scene.configureBasePlate(0, 0, 5)
        scene.showBasePlate()
        scene.idle(10)
        return scene
    }

    private fun CreateSceneBuilder.caption(util: SceneBuildingUtil, pos: BlockPos, text: String, color: PonderPalette = PonderPalette.WHITE) {
        overlay().showText(65).text(text).pointAt(util.vector().topOf(pos)).placeNearTarget().colored(color).attachKeyFrame()
        idle(75)
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
        scene.world().showSection(util.select().fromTo(0, 1, 1, 4, 1, 1), Direction.DOWN)
        scene.supplySteam(engine.east(2), 400, 900.0)
        scene.idle(20)
        scene.caption(util, engine, "Pass hot gas through a Gas Engine by connecting ducts to both ends")
        scene.world().showSection(util.select().position(piston), Direction.NORTH)
        scene.idle(12)
        scene.caption(util, piston, "Attach a Steam Engine to a free side to put the heat and flow to work")
        scene.world().showSection(util.select().fromTo(shaft, shaft.west()), Direction.DOWN)
        scene.world().modifyBlockEntity(engine, GasEngineBlockEntity::class.java) {
            it.attachedEngines = 1
        }
        scene.idle(8)
        scene.world().setKineticSpeed(util.select().fromTo(shaft, shaft.west()), 16f)
        scene.effects().rotationDirectionIndicator(shaft)
        scene.caption(util, shaft, "Place a Shaft two blocks out from the Steam Engine, leaving room for its piston", PonderPalette.OUTPUT)
        scene.supplySteam(engine.east(2), 0, 900.0)
        scene.idle(35)
        scene.world().setKineticSpeed(util.select().fromTo(shaft, shaft.west()), 0f)
        scene.caption(util, engine, "When the gas stops flowing, the engine stops supplying stress capacity")
        scene.supplySteam(engine.east(2), 400, 900.0)
        scene.idle(20)
        scene.world().setKineticSpeed(util.select().fromTo(shaft, shaft.west()), 16f)
        scene.markAsFinished()
    }

    fun sterlingEngine(builder: SceneBuilder, util: SceneBuildingUtil) {
        val scene = scene(builder, "sterling_engine", "Generating rotation with a Sterling Engine")
        val engine = util.grid().at(2, 1, 2)
        val gear = engine.south()
        scene.world().showSection(util.select().fromTo(engine, engine.east(2)), Direction.DOWN)
        scene.idle(15)
        scene.caption(util, engine.east(), "Supply a Sterling Engine with hot gas through either end", PonderPalette.INPUT)
        scene.supplySteam(engine.east(2), 400, 950.0)
        scene.idle(30)
        scene.world().modifyBlockEntity(engine, SterlingEngineBlockEntity::class.java) {
            it.efficiency = GasEngineLogic.calculateTemperatureEfficiency(
                it.level!!, it.getDuctNodePosition(), ClockworkConfig.SERVER.gasEngine.sterlingEngineTemperatureIncrement
            )
            it.speed = it.getGeneratedSpeed()
        }
        scene.effects().rotationDirectionIndicator(engine)
        scene.caption(util, engine, "Heat in the gas turns the engine's built-in cogwheel")
        scene.world().showSection(util.select().fromTo(gear, gear.west()), Direction.DOWN)
        scene.addInstruction { step ->
            val source = step.world.getBlockEntity(engine) as? SterlingEngineBlockEntity ?: return@addInstruction
            for (pos in listOf(gear, gear.west())) {
                (step.world.getBlockEntity(pos) as? KineticBlockEntity)?.speed = -source.speed
            }
        }
        scene.effects().rotationDirectionIndicator(gear)
        scene.caption(util, gear, "Mesh a Cogwheel with the engine to take rotation out of the gas line", PonderPalette.OUTPUT)
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
