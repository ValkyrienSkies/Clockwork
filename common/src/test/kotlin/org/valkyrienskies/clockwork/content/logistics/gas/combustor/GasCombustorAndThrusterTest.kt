package org.valkyrienskies.clockwork.content.logistics.gas.combustor

import com.google.gson.JsonParser
import com.google.gson.JsonPrimitive
import io.mockk.every
import io.mockk.mockkObject
import io.mockk.unmockkObject
import net.minecraft.core.Direction
import net.minecraft.resources.ResourceLocation
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.valkyrienskies.clockwork.content.physicalities.gas_thruster.GasThrusterDuctNode
import org.valkyrienskies.clockwork.util.DuctNetworkUtils
import org.valkyrienskies.kelvin.api.ConnectionType
import org.valkyrienskies.kelvin.api.DuctNodePos
import org.valkyrienskies.kelvin.api.DuctNodeState
import org.valkyrienskies.kelvin.api.GasType
import org.valkyrienskies.kelvin.api.edges.PipeDuctEdge
import org.valkyrienskies.kelvin.api.nodes.PipeDuctNode
import org.valkyrienskies.kelvin.api.recipe.GasBaseRecipe
import org.valkyrienskies.kelvin.api.recipe.GasReactionRequirement
import org.valkyrienskies.kelvin.api.simulation.NodeMaterialState
import org.valkyrienskies.kelvin.impl.DuctNetworkServer
import org.valkyrienskies.kelvin.impl.recipe.DefaultKelvinRequirements
import org.valkyrienskies.kelvin.impl.recipe.KelvinGasRecipeSerializer
import org.valkyrienskies.kelvin.impl.registry.GasTypeRegistry
import org.valkyrienskies.kelvin.impl.registry.ReactionRequirementRegistry
import org.valkyrienskies.kelvin.impl.solvers.JacobiSeidelSolver
import org.valkyrienskies.kelvin.util.GasPhysics
import org.valkyrienskies.kelvin.util.NodeThermodynamics
import java.nio.file.Files
import java.nio.file.Path
import kotlin.math.abs
import kotlin.math.min

class GasCombustorAndThrusterTest {

    companion object {
        private val TEST_DIM = ResourceLocation("minecraft", "overworld")
        private val AIR = GasType("Air", ResourceLocation("kelvin", "air"), 1.293, 1.716e-5, 1.005, 0.026)
        private val EXHAUST = GasType("Exhaust", ResourceLocation("vs_clockwork", "exhaust"), density = 1.98, viscosity = 1.10e-5, specificHeatCapacity = 2.846, thermalConductivity = 0.031)
        private val STEAM = GasType("Steam", ResourceLocation("vs_clockwork", "steam"), density = 0.762, viscosity = 1.223e-5, specificHeatCapacity = 2.2, thermalConductivity = 0.031)
        private val PHLOGISTON = GasType("Phlogiston", ResourceLocation("vs_clockwork", "phlogiston"), density = 3.0, viscosity = 2.0e-5, specificHeatCapacity = 14.30, thermalConductivity = 0.240, sutherlandConstant = 150.0, adiabaticIndex = 1.008)
        private val HELIUM = GasType("Aether", ResourceLocation("vs_clockwork", "aether"), density = 0.166, viscosity = 1.96e-5, specificHeatCapacity = 5.1832, thermalConductivity = 0.151, sutherlandConstant = 79.4, adiabaticIndex = 1.66)
        private val HYDROGEN = GasType("Stellane", ResourceLocation("vs_clockwork", "stellane"), density = 0.08988, viscosity = 0.88e-5, specificHeatCapacity = 14.30, thermalConductivity = 0.18, sutherlandConstant = 72.0, adiabaticIndex = 1.4)
        private val METHANE = GasType("Bog", ResourceLocation("vs_clockwork", "bog"), density = 0.657, viscosity = 1.10e-5, specificHeatCapacity = 2.2, thermalConductivity = 0.031, sutherlandConstant = 90.0, adiabaticIndex = 1.3)
        private val VITRIOL = GasType("Vitriol", ResourceLocation("vs_clockwork", "vitriol"), density = 2.2, viscosity = 2.0e-5, specificHeatCapacity = 8.0, thermalConductivity = 0.16, sutherlandConstant = 68.4, adiabaticIndex = 1.008)
        private val OZONITE = GasType("Ozonite", ResourceLocation("vs_clockwork", "ozonite"), density = 1.15, viscosity = 0.78e-5, specificHeatCapacity = 0.92, thermalConductivity = 0.024, sutherlandConstant = 127.0, adiabaticIndex = 1.4)
        private val gasRegistry = HashMap<ResourceLocation, GasType>()
        private lateinit var REACTIONS: List<GasBaseRecipe>

        @JvmStatic
        @BeforeAll
        fun setup() {
            gasRegistry.clear()
            for (gas in listOf(AIR, EXHAUST, STEAM, PHLOGISTON, HELIUM, HYDROGEN, METHANE, VITRIOL, OZONITE)) {
                gasRegistry[gas.resourceLocation] = gas
            }
            mockkObject(GasTypeRegistry)
            every { GasTypeRegistry.getGasType(any<ResourceLocation>()) } answers { gasRegistry[firstArg<ResourceLocation>()] }
            every { GasTypeRegistry.getGasType(any<String>(), any<String>()) } answers {
                gasRegistry[ResourceLocation(firstArg<String>(), secondArg<String>())]
            }
            every { GasTypeRegistry.getGasTypes() } answers { gasRegistry.values }

            ReactionRequirementRegistry.init()
            REACTIONS = loadBundledReactions().values.toList()
            assertTrue(REACTIONS.size >= 7, "Expected bundled kelvin_reactions to be loaded")
        }

        @JvmStatic
        @AfterAll
        fun tearDown() {
            unmockkObject(GasTypeRegistry)
        }

        private fun reactionDirectory(): Path {
            val candidates = listOf(
                Path.of("src/main/resources/data/vs_clockwork/kelvin_reactions"),
                Path.of("common/src/main/resources/data/vs_clockwork/kelvin_reactions"),
            )
            return candidates.firstOrNull { Files.isDirectory(it) }
                ?: error("Could not locate kelvin_reactions directory from ${Path.of("").toAbsolutePath()}")
        }

        private fun gasCraftingDirectory(): Path {
            val candidates = listOf(
                Path.of("src/main/resources/data/vs_clockwork/recipes/gas_crafting"),
                Path.of("common/src/main/resources/data/vs_clockwork/recipes/gas_crafting"),
            )
            return candidates.firstOrNull { Files.isDirectory(it) }
                ?: error("Could not locate gas_crafting directory from ${Path.of("").toAbsolutePath()}")
        }

        private fun loadBundledReactions(): Map<String, GasBaseRecipe> {
            val dir = reactionDirectory()
            val out = LinkedHashMap<String, GasBaseRecipe>()
            Files.list(dir).use { stream ->
                stream.filter { it.toString().endsWith(".json") }.sorted().forEach { path ->
                    val json = JsonParser.parseString(Files.readString(path)).asJsonObject
                    val recipe = requireNotNull(KelvinGasRecipeSerializer.parse(json)) {
                        "Failed to parse reaction ${path.fileName}"
                    }
                    out[path.fileName.toString()] = recipe
                }
            }
            return out
        }

        private fun checkRequirement(req: GasReactionRequirement, state: DuctNodeState, value: com.google.gson.JsonElement): Boolean =
            when (req.resourceLocation) {
                DefaultKelvinRequirements.minTemperature.resourceLocation -> state.temperature >= value.asDouble
                DefaultKelvinRequirements.maxTemperature.resourceLocation -> state.temperature <= value.asDouble
                DefaultKelvinRequirements.minPressure.resourceLocation -> state.pressure >= value.asDouble
                DefaultKelvinRequirements.maxPressure.resourceLocation -> state.pressure <= value.asDouble
                DefaultKelvinRequirements.inhibitedBy.resourceLocation -> {
                    val gasId = ResourceLocation.of(value.asJsonObject["gas"].asString, ':')
                    val gas = GasTypeRegistry.getGasType(gasId)
                    val ratio = value.asJsonObject["ratio"].asDouble
                    val total = state.totalGasMass
                    total > 0.0 && ((state.gasMasses[gas] ?: 0.0) / total) <= ratio
                }
                else -> error("Unexpected requirement in test: ${req.resourceLocation}")
            }

        private fun runReactions(network: DuctNetworkServer, pos: DuctNodePos, reactions: Collection<GasBaseRecipe> = REACTIONS) {
            val node = network.getNodeAt(pos) ?: return
            val info = network.nodeInfo[pos] ?: return
            if (info.currentGasMasses.isEmpty()) return

            for (reaction in reactions) {
                var maxAmount = Double.MAX_VALUE
                var ok = reaction.gasses.isNotEmpty()
                for ((gas, req) in reaction.gasses) {
                    val avail = info.currentGasMasses.getDouble(gas)
                    if (avail < 0.0001) {
                        ok = false
                        break
                    }
                    maxAmount = min(maxAmount, avail / req)
                }
                if (!ok || maxAmount <= 0.0) continue

                fun holds(amt: Double): Boolean {
                    val masses = HashMap<GasType, Double>(info.currentGasMasses)
                    for ((g, m) in reaction.gasses) masses[g] = (masses[g] ?: 0.0) - amt * m
                    for ((g, m) in reaction.result) masses[g] = (masses[g] ?: 0.0) + amt * m
                    val energy = (info.currentEnergy + amt * reaction.energy).coerceAtLeast(0.001)
                    val vol = node.volume + info.volumeChange
                    val state = NodeThermodynamics.calculateState(node, NodeMaterialState(masses, energy, vol, node.heatCapacity))
                    val nodeState = DuctNodeState(pos, state.temperature, state.pressure, masses, energy, vol)
                    return reaction.requirements.all { (req, value) -> checkRequirement(req, nodeState, value) }
                }

                if (!holds(0.0)) continue
                val amount = if (holds(maxAmount)) {
                    maxAmount
                } else {
                    var lo = 0.0
                    var hi = maxAmount
                    repeat(16) {
                        val mid = 0.5 * (lo + hi)
                        if (holds(mid)) lo = mid else hi = mid
                    }
                    lo
                }
                if (amount <= 0.0) continue
                for ((g, m) in reaction.gasses) network.modGasMass(pos, g, -amount * m)
                for ((g, m) in reaction.result) network.modGasMass(pos, g, amount * m)
                network.modHeatEnergy(pos, reaction.energy * amount)
                NodeThermodynamics.update(node, info)
            }
        }

        private fun seedNode(network: DuctNetworkServer, pos: DuctNodePos, gases: Map<GasType, Double>, tempK: Double) {
            val node = requireNotNull(network.getNodeAt(pos))
            val info = requireNotNull(network.nodeInfo[pos])
            info.currentGasMasses.clear()
            for ((g, m) in gases) {
                if (m > 0.0) info.currentGasMasses.put(g, m)
            }
            info.currentEnergy = GasPhysics.nodeHeatCapacity(gases, node.heatCapacity) * tempK
            NodeThermodynamics.update(node, info)
        }

        private fun massForPressure(gas: GasType, pressurePa: Double, volumeM3: Double, tempK: Double): Double {
            val cv = gas.specificHeatCapacity * 1000.0 / gas.adiabaticIndex
            val r = (gas.adiabaticIndex - 1.0) * cv
            return pressurePa * volumeM3 / (r * tempK)
        }
    }

    @Test
    fun `all kelvin reaction recipes and pure gas crafting recipes conserve mass natively in json`() {
        val reactions = loadBundledReactions()
        assertTrue(reactions.isNotEmpty())
        for ((name, recipe) in reactions) {
            val inMass = recipe.gasses.values.sum()
            val outMass = recipe.result.values.sum()
            assertEquals(inMass, outMass, 1e-9, "Reaction $name must conserve mass (in=$inMass, out=$outMass)")
        }

        // Pure gas-to-gas gas_crafting recipes (catalyst item only, no item/fluid outputs) also conserve gas mass.
        val craftingDir = gasCraftingDirectory()
        for (fileName in listOf("methane_reforming.json", "phlogiston.json")) {
            val json = JsonParser.parseString(Files.readString(craftingDir.resolve(fileName))).asJsonObject
            val recipe = requireNotNull(KelvinGasRecipeSerializer.parse(json))
            val inMass = recipe.gasses.values.sum()
            val outMass = recipe.result.values.sum()
            assertEquals(inMass, outMass, 1e-9, "Gas crafting recipe $fileName must conserve gas mass")
        }
    }

    @Test
    fun `spark bubble ignites flammable mixtures while refusing to cheat inert, diluted, or endothermic gas`() {
        val network = DuctNetworkServer(disabled = false)
        val pos = DuctNodePos(0.0, 0.0, 0.0, TEST_DIM)
        network.addNode(pos, CombustorDuctNode(pos, volume = 0.4, maxPressure = 16_375_049.0, maxTemperature = 1478.0))

        // 1) Inert air: cannot be abused as a heater (2 kJ only warms ~0.48 kg air + 44.9 J/K wall by ~5 K)
        seedNode(network, pos, mapOf(AIR to 0.48), 293.15)
        val inert = CombustionSpark.ignite(network, pos, reactions = REACTIONS, requirementCheck = ::checkRequirement)
        assertFalse(inert.ignited, "Pure air must not ignite")
        assertTrue(inert.finalTemperature < 305.0, "Inert air should only warm slightly from 2 kJ spark (got ${inert.finalTemperature} K)")

        // 2) Flammable Bog + Air mixture: ignites cleanly and reaches self-sustaining combustion temperature
        seedNode(network, pos, mapOf(AIR to 0.48, METHANE to 0.015), 293.15)
        val totalMassBefore = network.getGasMassAt(pos).values.sum()
        val bogIgnite = CombustionSpark.ignite(network, pos, reactions = REACTIONS, requirementCheck = ::checkRequirement)
        assertTrue(bogIgnite.ignited, "Bog + Air should ignite")
        assertTrue(bogIgnite.finalTemperature > 900.0, "Ignited Bog + Air should heat past auto-ignition (got ${bogIgnite.finalTemperature} K)")
        assertEquals(totalMassBefore, network.getGasMassAt(pos).values.sum(), 1e-9, "Spark combustion must conserve total mass")

        // 3) Heavily steam-diluted mixture: flame quenches instead of igniting the whole chamber
        seedNode(network, pos, mapOf(STEAM to 0.40, AIR to 0.08, METHANE to 0.005), 293.15)
        val diluted = CombustionSpark.ignite(network, pos, reactions = REACTIONS, requirementCheck = ::checkRequirement)
        assertTrue(diluted.finalTemperature < 350.0, "Heavily diluted mix should quench (got ${diluted.finalTemperature} K)")

        // 4) Stellane + Ozonite (rocket propellant): ignites cleanly with zero air
        seedNode(network, pos, mapOf(HYDROGEN to 0.02, OZONITE to 0.01), 293.15)
        val rocket = CombustionSpark.ignite(network, pos, reactions = REACTIONS, requirementCheck = ::checkRequirement)
        assertTrue(rocket.ignited, "Stellane + Ozonite should ignite")
        assertTrue(rocket.finalTemperature > 800.0, "Ignited Stellane + Ozonite should reach > 800 K (got ${rocket.finalTemperature} K)")

        // 5) Endothermic recipe at 800 K: must immediately self-extinguish without cheating bulk conversion
        val endo = GasBaseRecipe(
            gasses = hashMapOf(AIR to 0.001),
            requirements = hashMapOf(DefaultKelvinRequirements.minTemperature to JsonPrimitive(800.0)),
            energy = -50_000.0,
            result = hashMapOf(STEAM to 0.001),
        )
        seedNode(network, pos, mapOf(AIR to 0.40), 293.15)
        val endoResult = CombustionSpark.ignite(network, pos, reactions = listOf(endo), requirementCheck = ::checkRequirement)
        assertTrue((network.getGasMassAt(pos)[STEAM] ?: 0.0) < 0.001, "Endothermic reaction must not run bulk conversion")
        assertTrue(endoResult.finalTemperature < 310.0)
    }

    @Test
    fun `combustor reed valves block backflow, respect zero throttle, allow bidirectional exhaust, and insulate inlets`() {
        val network = DuctNetworkServer(disabled = false)
        val solver = JacobiSeidelSolver()
        val fuelPos = DuctNodePos(0.0, 0.0, 1.0, TEST_DIM)      // South = back inlet (facing NORTH)
        val combustorPos = DuctNodePos(0.0, 0.0, 0.0, TEST_DIM)
        val exhaustPos = DuctNodePos(0.0, 0.0, -1.0, TEST_DIM)  // North = front exhaust

        val combustor = CombustorDuctNode(combustorPos, volume = 0.4, maxPressure = 16_375_049.0, maxTemperature = 1478.0).apply {
            facing = Direction.NORTH
            throttle = 0.0
        }
        network.addNode(fuelPos, PipeDuctNode(fuelPos, volume = 0.4, maxPressure = 16_375_049.0, maxTemperature = 1478.0, heatCapacity = 44.9))
        network.addNode(combustorPos, combustor)
        network.addNode(exhaustPos, PipeDuctNode(exhaustPos, volume = 0.4, maxPressure = 16_375_049.0, maxTemperature = 1478.0, heatCapacity = 44.9))

        val inletEdge = PipeDuctEdge(ConnectionType.PIPE, fuelPos, combustorPos, radius = 0.3125, length = 0.375)
        val exhaustEdge = DuctNetworkUtils.configureMetalDuctEdge(PipeDuctEdge(ConnectionType.PIPE, combustorPos, exhaustPos, radius = 0.3125, length = 0.375))
        network.addEdge(fuelPos, combustorPos, inletEdge)
        network.addEdge(combustorPos, exhaustPos, exhaustEdge)
        combustor.insulateInletEdges()

        // 0) Port classification: only rear (SOUTH) is INLET and front (NORTH) is EXHAUST; sides and vertical are BLOCKED
        assertEquals(CombustorDuctNode.PortKind.EXHAUST, combustor.classifyNeighbor(exhaustPos))
        assertEquals(CombustorDuctNode.PortKind.INLET, combustor.classifyNeighbor(fuelPos))
        assertEquals(CombustorDuctNode.PortKind.BLOCKED, combustor.classifyNeighbor(DuctNodePos(1.0, 0.0, 0.0, TEST_DIM)))
        assertEquals(CombustorDuctNode.PortKind.BLOCKED, combustor.classifyNeighbor(DuctNodePos(-1.0, 0.0, 0.0, TEST_DIM)))
        assertEquals(CombustorDuctNode.PortKind.BLOCKED, combustor.classifyNeighbor(DuctNodePos(0.0, 1.0, 0.0, TEST_DIM)))
        assertEquals(CombustorDuctNode.PortKind.BLOCKED, combustor.classifyNeighbor(DuctNodePos(0.0, -1.0, 0.0, TEST_DIM)))

        // 1) Throttle = 0: high-pressure fuel cannot enter; high-pressure exhaust CAN flow backward into combustor!
        seedNode(network, fuelPos, mapOf(METHANE to massForPressure(METHANE, 400_000.0, 0.4, 293.15)), 293.15)
        seedNode(network, combustorPos, emptyMap(), 293.15)
        seedNode(network, exhaustPos, mapOf(AIR to massForPressure(AIR, 101_325.0, 0.4, 293.15)), 293.15)

        repeat(5) { solver.step(network, 10) }
        assertEquals(0.0, network.getGasMassAt(combustorPos)[METHANE] ?: 0.0, 1e-9, "Zero throttle must keep fuel inlet shut")
        assertTrue((network.getGasMassAt(combustorPos)[AIR] ?: 0.0) > 0.1, "Front exhaust port must allow air backflow")

        // 2) High chamber pressure + high temperature: reed valve prevents backflow AND thermal insulation prevents heating fuel pipe
        combustor.throttle = 1.0
        seedNode(network, fuelPos, mapOf(METHANE to massForPressure(METHANE, 200_000.0, 0.4, 293.15)), 293.15)
        seedNode(network, combustorPos, mapOf(EXHAUST to massForPressure(EXHAUST, 500_000.0, 0.4, 1200.0)), 1200.0)
        seedNode(network, exhaustPos, mapOf(AIR to massForPressure(AIR, 101_325.0, 0.4, 293.15)), 293.15)

        repeat(5) { solver.step(network, 10) }
        assertEquals(0.0, network.getGasMassAt(fuelPos)[EXHAUST] ?: 0.0, 1e-9, "Reed valve must block exhaust backflow into fuel pipe")
        assertEquals(293.15, network.getTemperatureAt(fuelPos), 1e-6, "Insulated inlet must not conduct chamber heat into fuel pipe")
        assertTrue((network.getGasMassAt(exhaustPos)[EXHAUST] ?: 0.0) > 0.01, "Hot exhaust must flow out the front port")
    }

    @Test
    fun `thruster refills air up to ambient when open, stays idle in vacuum or when blocked, and creates Kadenacy suction after blast`() {
        val network = DuctNetworkServer(disabled = false)
        val solver = JacobiSeidelSolver()
        val pos = DuctNodePos(0.0, 0.0, 0.0, TEST_DIM)
        val thruster = GasThrusterDuctNode(pos, volume = 0.5, maxPressure = 16_375_049.0, maxTemperature = 1478.0).apply {
            ambientPressure = 101_325.0
            ambientTemperature = 293.15
            ambientAirGas = AIR
            nozzleOpenToAir = false
            environmentValid = true
        }
        network.addNode(pos, thruster)

        // 1) Blocked nozzle: does NOT suck ambient air
        seedNode(network, pos, emptyMap(), 293.15)
        repeat(10) { solver.step(network, 10) }
        assertEquals(0.0, network.getPressureAt(pos), 1e-6, "Blocked nozzle must not draw ambient air")

        // 2) Open nozzle: slowly fills with ambient air up to ambient pressure without overshooting or producing thrust
        thruster.nozzleOpenToAir = true
        repeat(40) {
            solver.step(network, 10)
            val summary = thruster.drainTickSummary()
            assertEquals(0.0, summary.averageRawThrust, 1e-9, "Refilling air must produce zero thrust")
        }
        assertTrue(network.getPressureAt(pos) in 99_000.0..101_326.0, "Open thruster should fill up to ambient pressure (got ${network.getPressureAt(pos)} Pa)")

        // 3) High-pressure blast builds Kadenacy exhaust inertia and briefly dips pressure below ambient!
        seedNode(network, pos, mapOf(EXHAUST to massForPressure(EXHAUST, 400_000.0, 0.5, 1200.0)), 1200.0)
        var minPressure = Double.POSITIVE_INFINITY
        var peakThrust = 0.0
        repeat(40) {
            solver.step(network, 10)
            val summary = thruster.drainTickSummary()
            if (summary.averageRawThrust > peakThrust) peakThrust = summary.averageRawThrust
            val p = network.getPressureAt(pos)
            if (p < minPressure) minPressure = p
        }
        assertTrue(peakThrust > 100.0, "Hot high-pressure blast must produce thrust")
        assertTrue(minPressure < 92_000.0, "Kadenacy exhaust inertia should dip pressure below ambient (min=$minPressure Pa)")

        // 4) Vacuum: never sucks air or triggers Kadenacy
        thruster.ambientPressure = 0.0
        seedNode(network, pos, emptyMap(), 293.15)
        repeat(10) { solver.step(network, 10) }
        assertEquals(0.0, network.getPressureAt(pos), 1e-6, "Vacuum thruster must stay empty")
    }

    @Test
    fun `coupled combustor and thruster run pulsejet, continuous jet, boosted jet, and vacuum rocket natively in Kelvin`() {
        data class RunStats(
            val avgThrustN: Double,
            val maxThrustN: Double,
            val minThrusterP: Double,
            val endCombustorT: Double,
            val pulses: Int,
        )

        data class SupplySpec(val gas: GasType, val pressurePa: Double, val pipeRadius: Double = 0.3125)

        fun runEngine(
            supplies: List<SupplySpec>,
            ambientP: Double = 101_325.0,
            throttle: Double = 1.0,
            ticks: Int = 240,
        ): RunStats {
            val network = DuctNetworkServer(disabled = false)
            val solver = JacobiSeidelSolver()
            val cPos = DuctNodePos(0.0, 0.0, 0.0, TEST_DIM)
            val tPos = DuctNodePos(0.0, 0.0, -1.0, TEST_DIM)    // North = single front exhaust
            val inPos = DuctNodePos(0.0, 0.0, 1.0, TEST_DIM)    // South = single rear inlet

            val combustor = CombustorDuctNode(cPos, volume = 0.4, maxPressure = 16_375_049.0, maxTemperature = 1478.0).apply {
                facing = Direction.NORTH
                this.throttle = throttle
            }
            val thruster = GasThrusterDuctNode(tPos, volume = 0.5, maxPressure = 16_375_049.0, maxTemperature = 1478.0).apply {
                ambientPressure = ambientP
                ambientTemperature = 293.15
                ambientAirGas = AIR
                nozzleOpenToAir = ambientP > 0.0
                environmentValid = true
            }
            network.addNode(cPos, combustor)
            network.addNode(tPos, thruster)
            network.addEdge(cPos, tPos, DuctNetworkUtils.configureMetalDuctEdge(PipeDuctEdge(ConnectionType.PIPE, cPos, tPos, radius = 0.3125, length = 0.375)))

            // Single rear inlet node (acts as direct supply for 1 gas, or upstream mixing T-junction for 2+ gases)
            val supplyNodes = ArrayList<Pair<DuctNodePos, SupplySpec>>()
            if (supplies.size == 1) {
                network.addNode(inPos, PipeDuctNode(inPos, volume = 10.0, maxPressure = 16_375_049.0, maxTemperature = 1478.0, heatCapacity = 44.9))
                supplyNodes.add(inPos to supplies.single())
            } else {
                network.addNode(inPos, PipeDuctNode(inPos, volume = 0.4, maxPressure = 16_375_049.0, maxTemperature = 1478.0, heatCapacity = 44.9))
                val offsets = listOf(
                    DuctNodePos(0.0, 0.0, 2.0, TEST_DIM),
                    DuctNodePos(-1.0, 0.0, 1.0, TEST_DIM),
                    DuctNodePos(1.0, 0.0, 1.0, TEST_DIM),
                )
                supplies.forEachIndexed { idx, spec ->
                    val sPos = offsets[idx]
                    network.addNode(sPos, PipeDuctNode(sPos, volume = 10.0, maxPressure = 16_375_049.0, maxTemperature = 1478.0, heatCapacity = 44.9))
                    network.addEdge(sPos, inPos, PipeDuctEdge(ConnectionType.PIPE, sPos, inPos, radius = spec.pipeRadius, length = 0.375))
                    supplyNodes.add(sPos to spec)
                }
            }
            val inletEdge = PipeDuctEdge(ConnectionType.PIPE, inPos, cPos, radius = 0.3125, length = 0.375)
            inletEdge.wallThermalConductance = 0.0
            inletEdge.thermalConductivityMultiplier = 0.0
            network.addEdge(inPos, cPos, inletEdge)
            combustor.insulateInletEdges()

            if (ambientP > 0.0) {
                seedNode(network, cPos, mapOf(AIR to massForPressure(AIR, ambientP, 0.4, 293.15)), 293.15)
                seedNode(network, tPos, mapOf(AIR to massForPressure(AIR, ambientP, 0.5, 293.15)), 293.15)
            }

            var sparkCd = 0
            var minP = Double.POSITIVE_INFINITY
            var maxThrust = 0.0
            var sumThrust = 0.0
            var samples = 0
            var pulses = 0
            var high = false

            for (tick in 0 until ticks) {
                // Keep supply reservoirs topped up at their nominal supply pressure
                for ((sPos, spec) in supplyNodes) {
                    seedNode(network, sPos, mapOf(spec.gas to massForPressure(spec.gas, spec.pressurePa, 10.0, 293.15)), 293.15)
                }

                solver.step(network, 10)
                runReactions(network, cPos)
                runReactions(network, tPos)

                if (sparkCd > 0) sparkCd--
                if (throttle > 0.0 && sparkCd == 0 && network.getTemperatureAt(cPos) < 750.0 &&
                    network.getGasMassAt(cPos).values.sum() > 1e-4
                ) {
                    CombustionSpark.ignite(network, cPos, reactions = REACTIONS, requirementCheck = ::checkRequirement)
                    sparkCd = GasCombustorBlockEntity.AUTO_SPARK_COOLDOWN_TICKS
                }

                val summary = thruster.drainTickSummary()
                val thrustN = summary.averageRawThrust * 2000.0
                val tP = network.getPressureAt(tPos)
                if (tP < minP) minP = tP
                if (tick >= 40) {
                    sumThrust += thrustN
                    samples++
                    if (thrustN > maxThrust) maxThrust = thrustN
                }
                if (thrustN > 40_000.0 && !high) {
                    pulses++
                    high = true
                } else if (thrustN < 10_000.0) {
                    high = false
                }
            }

            return RunStats(
                avgThrustN = if (samples > 0) sumThrust / samples else 0.0,
                maxThrustN = maxThrust,
                minThrusterP = minP,
                endCombustorT = network.getTemperatureAt(cPos),
                pulses = pulses,
            )
        }

        // 1) Bog-only Valved Pulsejet (no air pipe!): Kadenacy pulls pressure below ambient and repeats pulses!
        val pulsejet = runEngine(listOf(SupplySpec(METHANE, 250_000.0)), ticks = 280)
        assertTrue(pulsejet.minThrusterP < 95_000.0, "Pulsejet must create sub-ambient Kadenacy suction (minP=${pulsejet.minThrusterP})")
        assertTrue(pulsejet.pulses >= 2, "Pulsejet must fire multiple pulses (pulses=${pulsejet.pulses})")
        assertTrue(pulsejet.avgThrustN > 10_000.0, "Pulsejet must produce meaningful thrust (avg=${pulsejet.avgThrustN})")

        // 2) Bog + Air Continuous Jet via upstream T-junction into the single rear inlet
        val bogJet = runEngine(listOf(SupplySpec(METHANE, 300_000.0), SupplySpec(AIR, 300_000.0)))
        assertTrue(bogJet.avgThrustN > pulsejet.avgThrustN * 2.0, "Continuous jet (${bogJet.avgThrustN}) should beat pulsejet (${pulsejet.avgThrustN})")
        assertTrue(bogJet.endCombustorT > 1000.0, "Continuous Bog+Air jet should stay self-sustaining hot (${bogJet.endCombustorT} K)")

        // 3) Bog + Ozonite + Air Boosted Jet via upstream manifold into the single rear inlet
        val boostedJet = runEngine(listOf(SupplySpec(METHANE, 500_000.0), SupplySpec(AIR, 500_000.0), SupplySpec(OZONITE, 500_000.0)))
        assertTrue(boostedJet.avgThrustN > bogJet.avgThrustN * 1.4, "Ozonite boost (${boostedJet.avgThrustN}) should beat unboosted jet (${bogJet.avgThrustN})")

        // 4) Stellane + Ozonite Vacuum Rocket via valve-metered T-junction (Ozonite is 12.8x denser than Stellane)
        val vacuumRocket = runEngine(
            supplies = listOf(SupplySpec(HYDROGEN, 500_000.0), SupplySpec(OZONITE, 500_000.0, pipeRadius = 0.05)),
            ambientP = 0.0,
        )
        assertTrue(vacuumRocket.avgThrustN > 100_000.0, "Stellane + Ozonite rocket should produce strong vacuum thrust (avg=${vacuumRocket.avgThrustN})")
        assertTrue(vacuumRocket.endCombustorT > 750.0, "Stellane + Ozonite rocket should sustain combustion (${vacuumRocket.endCombustorT} K)")

        // 5) Phlogiston Monopropellant Vacuum Rocket (single supply directly into rear inlet)
        val monopropRocket = runEngine(listOf(SupplySpec(PHLOGISTON, 500_000.0)), ambientP = 0.0)
        assertTrue(monopropRocket.avgThrustN > 100_000.0, "Phlogiston monopropellant should produce strong vacuum thrust (avg=${monopropRocket.avgThrustN})")
        assertTrue(monopropRocket.endCombustorT > 1000.0, "Phlogiston monopropellant should sustain combustion (${monopropRocket.endCombustorT} K)")

        // 6) Plain Air only (no fuel): stays cold, zero heater exploit
        val plainAir = runEngine(listOf(SupplySpec(AIR, 300_000.0)))
        assertTrue(plainAir.endCombustorT < 360.0, "Plain air must not heat up into a free heater (${plainAir.endCombustorT} K)")
    }
}
