package org.valkyrienskies.clockwork.content.ponders

import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.simibubi.create.AllItems
import com.simibubi.create.foundation.ponder.CreateSceneBuilder
import net.createmod.catnip.math.Pointing
import net.createmod.ponder.api.PonderPalette
import net.createmod.ponder.api.scene.SceneBuilder
import net.createmod.ponder.api.scene.SceneBuildingUtil
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import org.valkyrienskies.clockwork.ClockworkGasses
import org.valkyrienskies.clockwork.ClockworkItems
import org.valkyrienskies.clockwork.ClockworkMod
import org.valkyrienskies.clockwork.ClockworkModClient
import org.valkyrienskies.clockwork.ClockworkRecipes
import org.valkyrienskies.clockwork.content.logistics.gas.crafter.GasCrafterBlockEntity
import org.valkyrienskies.clockwork.content.logistics.gas.crafter.GasCraftingRecipe
import org.valkyrienskies.clockwork.content.logistics.gas.duct.DuctBlockEntity
import org.valkyrienskies.clockwork.content.logistics.gas.duct.DuctConnectionType
import org.valkyrienskies.clockwork.content.logistics.gas.duct.DuctEdgeType
import org.valkyrienskies.clockwork.content.logistics.gas.duct.IDuct.Companion.NORTH_CONNECTION
import org.valkyrienskies.clockwork.content.logistics.gas.duct.IDuct.Companion.SOUTH_CONNECTION
import org.valkyrienskies.clockwork.content.logistics.gas.generation.creative_generator.CreativeGeneratorBlockEntity
import org.valkyrienskies.kelvin.api.DuctEdge
import org.valkyrienskies.kelvin.api.edges.FilteredEdge
import org.valkyrienskies.kelvin.api.edges.SmartEdge
import org.valkyrienskies.kelvin.impl.recipe.KelvinGasRecipeSerializer
import org.valkyrienskies.kelvin.impl.registry.GasTypeRegistry
import org.valkyrienskies.kelvin.util.KelvinExtensions.toDuctNodePos

object GasProcessingPonders {
    private fun bundledRecipe(path: String): JsonObject =
        requireNotNull(javaClass.getResourceAsStream("/data/vs_clockwork/$path.json"))
            .bufferedReader().use { JsonParser.parseReader(it).asJsonObject }

    private val bogCombustion by lazy {
        requireNotNull(KelvinGasRecipeSerializer.parse(bundledRecipe("kelvin_reactions/bog_combustion")))
    }

    private val copperOxidation by lazy {
        val id = ClockworkMod.asResource("gas_crafting/copper_oxidation/copper_block")
        ClockworkRecipes.ClockworkRecipeTypes.GAS_CRAFTING.serializer.get()
            .fromJson(id, bundledRecipe("recipes/${id.path}")) as GasCraftingRecipe
    }

    private fun showMixedGasSupply(scene: CreateSceneBuilder, util: SceneBuildingUtil) {
        scene.configureBasePlate(0, 0, 5)
        scene.showBasePlate()
        scene.idle(8)
        scene.world().showSection(util.select().layer(1), Direction.DOWN)
        scene.world().modifyBlockEntity(util.grid().at(3, 1, 2), CreativeGeneratorBlockEntity::class.java) {
            it.gasValues[ClockworkGasses.STEAM] = 130
            it.gasValues[ClockworkGasses.EXHAUST] = 130
            it.temperature = 373.15
        }
        scene.idle(25)
    }

    private fun configureConnection(scene: CreateSceneBuilder, pos: BlockPos, type: DuctEdgeType, configure: (DuctEdge) -> Unit) {
        scene.addInstruction { step ->
            val first = step.world.getBlockEntity(pos) as? DuctBlockEntity ?: return@addInstruction
            val second = step.world.getBlockEntity(pos.south()) as? DuctBlockEntity ?: return@addInstruction
            val from = first.getDuctNodePosition()
            val to = second.getDuctNodePosition()
            first.setEdgeType(Direction.SOUTH, to, type, clientPacket = false)
            second.DIR_TO_CONNECTION_TYPE[Direction.NORTH] = type
            ClockworkMod.getKelvin(step.world).getEdgeBetween(from, to)?.let(configure)
        }
    }

    fun filterEdge(builder: SceneBuilder, util: SceneBuildingUtil) {
        val scene = CreateSceneBuilder(builder)
        scene.title("duct_filter", "Separating gases with a Filtered Duct connection")
        showMixedGasSupply(scene, util)
        val duct = util.grid().at(2, 1, 2)
        val joint = util.vector().blockSurface(duct, Direction.SOUTH)
        val outlet = util.vector().blockSurface(util.grid().at(1, 1, 4), Direction.WEST)

        scene.overlay().showText(115)
            .text("Here, Steam and Exhaust share a Duct and leave through two outlets")
            .pointAt(outlet).placeNearTarget()
        scene.idle(130)

        scene.overlay().showControls(joint, Pointing.DOWN, 35).whileSneaking().rightClick().withItem(ClockworkItems.SCREWDRIVER.asStack())
        scene.overlay().showText(120).attachKeyFrame()
            .text("Sneak and right-click a connection with a Screwdriver until the Filtered connection appears")
            .pointAt(joint).placeNearTarget()
        scene.idle(15)
        configureConnection(scene, duct, DuctEdgeType.FILTERED) {}
        scene.idle(120)

        scene.overlay().showControls(joint, Pointing.DOWN, 35).rightClick().withItem(ClockworkItems.SCREWDRIVER.asStack())
        scene.overlay().showText(110).attachKeyFrame()
            .text("Right-click it with the Screwdriver to choose which gases may pass")
            .pointAt(joint).placeNearTarget()
        scene.idle(125)

        configureConnection(scene, duct, DuctEdgeType.FILTERED) {
            (it as FilteredEdge).modFilter(hashSetOf(ClockworkGasses.STEAM), false)
        }
        scene.overlay().showControls(outlet, Pointing.RIGHT, 115).withGas(ClockworkGasses.STEAM)
        scene.overlay().showText(115).attachKeyFrame().colored(PonderPalette.OUTPUT)
            .text("Add Steam to the Allow List and only Steam reaches the outlet")
            .pointAt(outlet).placeNearTarget()
        scene.idle(130)

        configureConnection(scene, duct, DuctEdgeType.FILTERED) {
            (it as FilteredEdge).modFilter(hashSetOf(ClockworkGasses.STEAM), true)
        }
        scene.overlay().showControls(outlet, Pointing.RIGHT, 125).withGas(ClockworkGasses.EXHAUST)
        scene.overlay().showText(125).colored(PonderPalette.OUTPUT)
            .text("Switch to the Deny List to hold back Steam and let the Exhaust through")
            .pointAt(outlet).placeNearTarget()
        scene.idle(140)
    }

    fun smartEdge(builder: SceneBuilder, util: SceneBuildingUtil) {
        val scene = CreateSceneBuilder(builder)
        scene.title("duct_smart", "Controlling flow with a Smart Duct connection")
        showMixedGasSupply(scene, util)
        val duct = util.grid().at(2, 1, 2)
        val joint = util.vector().blockSurface(duct, Direction.SOUTH)
        val receiver = util.vector().topOf(duct.south())
        val outlet = util.vector().blockSurface(util.grid().at(1, 1, 4), Direction.WEST)

        scene.overlay().showControls(joint, Pointing.DOWN, 35).whileSneaking().rightClick().withItem(ClockworkItems.SCREWDRIVER.asStack())
        scene.overlay().showText(120).attachKeyFrame()
            .text("Cycle a Duct connection with a Screwdriver while sneaking to make it Smart")
            .pointAt(joint).placeNearTarget()
        scene.idle(15)
        configureConnection(scene, duct, DuctEdgeType.SMART) {}
        scene.idle(120)

        scene.overlay().showControls(joint, Pointing.DOWN, 35).rightClick().withItem(ClockworkItems.SCREWDRIVER.asStack())
        scene.overlay().showText(130).attachKeyFrame()
            .text("Right-click to set a condition on the receiving side. Try a pressure above 100 kPa")
            .pointAt(receiver).placeNearTarget()
        configureConnection(scene, duct, DuctEdgeType.SMART) {
            (it as SmartEdge).apply {
                filter = SmartEdge.FilterType.PRESSURE
                comparisonValue = 100_000.0
                moreThan = true
            }
        }
        scene.idle(145)

        scene.overlay().showText(120).colored(PonderPalette.RED)
            .text("The outlet keeps this side below the threshold, so the connection stays shut")
            .pointAt(outlet).placeNearTarget()
        scene.idle(135)

        configureConnection(scene, duct, DuctEdgeType.SMART) { (it as SmartEdge).moreThan = false }
        scene.overlay().showText(115).attachKeyFrame().colored(PonderPalette.GREEN)
            .text("Switch to below 100 kPa and the whole mixture can flow again")
            .pointAt(outlet).placeNearTarget()
        scene.idle(130)

        scene.overlay().showText(120)
            .text("Select which gases count towards the pressure, temperature, heat energy or mass measurement")
            .pointAt(joint).placeNearTarget()
        scene.idle(135)
    }

    fun gasReactions(builder: SceneBuilder, util: SceneBuildingUtil) {
        val scene = CreateSceneBuilder(builder)
        scene.title("gas_reactions", "Reacting gases inside Ducts")
        scene.configureBasePlate(0, 0, 5)
        scene.showBasePlate()
        scene.idle(8)
        val bogSource = util.grid().at(3, 1, 2)
        val airSource = util.grid().at(2, 1, 1)
        val chamber = util.grid().at(2, 1, 2)
        val shutoff = chamber.south()
        val outlet = util.grid().at(1, 1, 4)
        scene.world().showSection(util.select().layer(1).substract(util.select().position(airSource)), Direction.DOWN)
        scene.addInstruction { step ->
            ClockworkModClient.getKelvin(step.world).sceneReactions = listOf(bogCombustion)
        }
        scene.idle(15)
        scene.world().modifyBlockEntity(bogSource, CreativeGeneratorBlockEntity::class.java) {
            it.gasValues[ClockworkGasses.METHANE] = 30
            it.temperature = 300.0
        }
        scene.overlay().showControls(util.vector().topOf(bogSource), Pointing.RIGHT, 100).withGas(ClockworkGasses.METHANE)
        scene.overlay().showText(100).colored(PonderPalette.INPUT)
            .text("Fill a closed Duct with Bog gas from one side...")
            .pointAt(util.vector().topOf(bogSource)).placeNearTarget()
        scene.idle(115)
        scene.world().showSection(util.select().position(airSource), Direction.SOUTH)
        val air = GasTypeRegistry.GAS_TYPES.getValue(ResourceLocation("kelvin", "air"))
        scene.world().modifyBlockEntity(airSource, CreativeGeneratorBlockEntity::class.java) {
            it.gasValues[air] = 60
            it.temperature = 300.0
        }
        scene.overlay().showControls(util.vector().topOf(airSource), Pointing.RIGHT, 115).withGas(air)
        scene.overlay().showText(115).attachKeyFrame().colored(PonderPalette.INPUT)
            .text("...and admit Air from the other. At room temperature they simply mix")
            .pointAt(util.vector().topOf(chamber)).placeNearTarget()
        scene.idle(130)

        scene.world().modifyBlockEntity(bogSource, CreativeGeneratorBlockEntity::class.java) { it.gasValues.clear() }
        scene.world().modifyBlockEntity(airSource, CreativeGeneratorBlockEntity::class.java) { it.gasValues.clear() }
        scene.overlay().showText(140).attachKeyFrame()
            .text("Bring the mixture between 800 and 1200 K and the Bog gas reacts with the Air")
            .pointAt(util.vector().topOf(chamber)).placeNearTarget()
        scene.idle(15)
        scene.addInstruction { step ->
            val network = ClockworkModClient.getKelvin(step.world)
            for (pos in listOf(bogSource, airSource, chamber, shutoff)) {
                val node = pos.toDuctNodePos(step.world.dimension().location())
                network.modTemperature(node, 900.0 - network.getTemperatureAt(node))
            }
        }
        scene.effects().indicateSuccess(chamber)
        scene.idle(140)

        val joint = util.vector().blockSurface(shutoff, Direction.SOUTH)
        scene.overlay().showControls(joint, Pointing.DOWN, 25).rightClick().withItem(AllItems.WRENCH.asStack())
        scene.overlay().showText(120).attachKeyFrame().colored(PonderPalette.OUTPUT)
            .text("Open the outlet to release the Steam and Exhaust produced by the reaction")
            .pointAt(util.vector().blockSurface(outlet, Direction.WEST)).placeNearTarget()
        scene.idle(10)
        scene.world().modifyBlock(shutoff, { it.setValue(SOUTH_CONNECTION, DuctConnectionType.SIDE) }, false)
        scene.world().modifyBlock(shutoff.south(), { it.setValue(NORTH_CONNECTION, DuctConnectionType.SIDE) }, false)
        scene.idle(125)
    }

    fun gasCrafter(builder: SceneBuilder, util: SceneBuildingUtil) {
        val scene = CreateSceneBuilder(builder)
        scene.title("gas_crafter", "Processing ingredients with a Gas Crafter")
        scene.configureBasePlate(0, 0, 5)
        scene.showBasePlate()
        scene.idle(8)
        val basin = util.grid().at(1, 1, 3)
        val crafter = basin.above()
        val source = util.grid().at(2, 1, 2)
        val basinSide = util.vector().blockSurface(basin, Direction.WEST)
        scene.world().showSection(util.select().position(basin), Direction.DOWN)
        scene.idle(12)
        scene.world().showSection(util.select().position(crafter), Direction.DOWN)
        scene.overlay().showText(90).attachKeyFrame()
            .text("Place a Gas Crafter directly above a Basin")
            .pointAt(util.vector().centerOf(crafter)).placeNearTarget()
        scene.idle(105)

        scene.overlay().showControls(basinSide, Pointing.RIGHT, 35).withItem(ItemStack(Items.COPPER_BLOCK))
        scene.overlay().showText(125).colored(PonderPalette.INPUT)
            .text("Supply the Basin with the recipe's ingredients. Here, a Block of Copper will do")
            .pointAt(basinSide).placeNearTarget()
        scene.world().createItemOnBeltLike(basin, Direction.WEST, ItemStack(Items.COPPER_BLOCK))
        scene.idle(140)

        scene.world().showSection(util.select().fromTo(1, 1, 2, 2, 2, 2), Direction.DOWN)
        scene.world().modifyBlockEntity(source, CreativeGeneratorBlockEntity::class.java) {
            it.gasValues[ClockworkGasses.OZONITE] = 100
            it.temperature = 300.0
        }
        scene.overlay().showControls(util.vector().topOf(crafter.north()), Pointing.RIGHT, 90).withGas(ClockworkGasses.OZONITE)
        scene.overlay().showText(90).attachKeyFrame().colored(PonderPalette.INPUT)
            .text("Feed Ozonite into the Crafter through a Duct")
            .pointAt(util.vector().topOf(crafter.north())).placeNearTarget()
        scene.idle(100)
        scene.world().modifyBlockEntity(crafter, GasCrafterBlockEntity::class.java) {
            val recipe = copperOxidation
            if (it.matchRecipe(recipe)) {
                it.currentRecipe = recipe
                it.processingTicks = recipe.processingDuration
                it.clientProcessingTicks = it.processingTicks.toFloat()
            }
        }
        scene.idle(65)
        scene.effects().indicateSuccess(basin)
        scene.overlay().showControls(basinSide, Pointing.RIGHT, 45).withItem(ItemStack(Items.EXPOSED_COPPER))
        scene.overlay().showText(130).attachKeyFrame().colored(PonderPalette.OUTPUT)
            .text("The Crafter consumes the gas and leaves a block of Exposed Copper in the Basin")
            .pointAt(basinSide).placeNearTarget()
        scene.idle(145)
    }
}
