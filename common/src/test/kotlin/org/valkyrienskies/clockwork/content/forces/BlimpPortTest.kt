package org.valkyrienskies.clockwork.content.forces

import com.fasterxml.jackson.annotation.JsonAutoDetect
import com.fasterxml.jackson.annotation.PropertyAccessor
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.module.SimpleModule
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.spyk
import io.mockk.unmockkObject
import net.minecraft.SharedConstants
import net.minecraft.core.BlockPos
import net.minecraft.core.particles.ParticleOptions
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.Bootstrap
import net.minecraft.server.MinecraftServer
import net.minecraft.server.level.ServerLevel
import net.minecraft.util.RandomSource
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.block.state.BlockState
import org.joml.Matrix4d
import org.joml.Quaterniond
import org.joml.primitives.AABBi
import org.joml.primitives.AABBic
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.valkyrienskies.clockwork.ClockworkConfig
import org.valkyrienskies.clockwork.content.forces.BalloonController.SealedScanResult
import org.valkyrienskies.clockwork.content.forces.data.BalloonData
import org.valkyrienskies.clockwork.content.forces.data.BalloonData.EnclosureStatus
import org.valkyrienskies.clockwork.content.logistics.gas.pockets.blimp.BlimpPortBlock
import org.valkyrienskies.core.api.ships.LoadedServerShip
import org.valkyrienskies.core.internal.world.VsiServerShipWorld
import org.valkyrienskies.kelvin.api.ConnectionType
import org.valkyrienskies.kelvin.api.DuctNetwork
import org.valkyrienskies.kelvin.api.DuctNodePos
import org.valkyrienskies.kelvin.api.GasType
import org.valkyrienskies.kelvin.api.edges.PipeDuctEdge
import org.valkyrienskies.kelvin.api.nodes.PipeDuctNode
import org.valkyrienskies.kelvin.api.nodes.ValveDuctNode
import org.valkyrienskies.kelvin.api.recipe.GasBaseRecipe
import org.valkyrienskies.kelvin.impl.DuctNetworkServer
import org.valkyrienskies.kelvin.impl.recipe.KelvinReactionDataLoader
import org.valkyrienskies.kelvin.impl.registry.GasTypeRegistry
import org.valkyrienskies.mod.common.IShipObjectWorldServerProvider
import org.valkyrienskies.mod.common.util.DimensionIdProvider

class BlimpPortTest {
    private val gasRegistry = HashMap<ResourceLocation, GasType>()
    private val testDim = ResourceLocation("minecraft", "overworld")

    private val air = GasType("Air", ResourceLocation("kelvin", "air"), 1.293, 1.716e-5, 1.005, 0.026)
    private val helium = GasType("Helium", ResourceLocation("audit", "helium"), 0.166, 1.96e-5, 5.1832, 0.151, 79.4, 1.66)
    private val fuel = GasType("Stellane", ResourceLocation("audit", "stellane"), 0.0899, 8.8e-6, 14.3, 0.18, 79.4, 1.41)
    private val exhaust = GasType("Exhaust", ResourceLocation("audit", "exhaust"), 1.5, 1.8e-5, 1.0, 0.025)

    @BeforeEach
    fun setUpGasRegistry() {
        mockkObject(GasTypeRegistry)
        gasRegistry.clear()
        gasRegistry[air.resourceLocation] = air
        gasRegistry[helium.resourceLocation] = helium
        gasRegistry[fuel.resourceLocation] = fuel
        gasRegistry[exhaust.resourceLocation] = exhaust
        every { GasTypeRegistry.getGasType(any<ResourceLocation>()) } answers { gasRegistry[firstArg<ResourceLocation>()] }
        every { GasTypeRegistry.getGasType(any<String>(), any<String>()) } answers {
            gasRegistry[ResourceLocation(firstArg<String>(), secondArg<String>())]
        }
        every { GasTypeRegistry.getGasTypes() } answers { gasRegistry.values }
    }

    @AfterEach
    fun restoreGasRegistry() {
        unmockkObject(GasTypeRegistry)
        clearAllMocks()
    }

    companion object {
        @BeforeAll
        @JvmStatic
        fun bootstrap() {
            SharedConstants.tryDetectVersion()
            Bootstrap.bootStrap()
        }
    }

    private inner class World(server: Boolean = false) {
        val solids = HashSet<BlockPos>()
        val unloaded = HashSet<BlockPos>()
        val solid: BlockState = mockk {
            every { block } returns Blocks.STONE
            every { isAir } returns false
            every { isCollisionShapeFullBlock(any(), any()) } returns true
        }
        val airState: BlockState = mockk {
            every { block } returns Blocks.AIR
            every { isAir } returns true
            every { isCollisionShapeFullBlock(any(), any()) } returns false
        }
        val level: Level = if (server) {
            mockk<ServerLevel>(relaxed = true, moreInterfaces = arrayOf(DimensionIdProvider::class))
        } else {
            mockk()
        }

        init {
            every { level.dimension() } returns Level.OVERWORLD
            every { level.isLoaded(any()) } answers { firstArg<BlockPos>() !in unloaded }
            every { level.getBlockState(any()) } answers {
                if (firstArg<BlockPos>() in solids) solid else airState
            }
            if (server) {
                val sl = level as ServerLevel
                every { (level as DimensionIdProvider).dimensionId } returns "audit"
                every { sl.getRandom() } returns RandomSource.create(0L)
                every { sl.sendParticles(any<ParticleOptions>(), any(), any(), any(), any(), any(), any(), any(), any()) } returns 0
                every { sl.playSound(any(), any<Double>(), any(), any(), any(), any(), any(), any()) } returns Unit
                every { sl.playSound(any(), any<BlockPos>(), any(), any(), any(), any()) } returns Unit
            }
        }

        fun box(minX: Int = 0, maxX: Int = 4, minY: Int = 0, maxY: Int = 5, minZ: Int = 0, maxZ: Int = 4, floor: Boolean = true) {
            for (x in minX..maxX) for (y in minY..maxY) for (z in minZ..maxZ) {
                if (x == minX || x == maxX || y == maxY || z == minZ || z == maxZ || (floor && y == minY)) {
                    solids.add(BlockPos(x, y, z))
                }
            }
        }
    }

    private fun serverShip(w: World, c: BalloonController): LoadedServerShip {
        val sow = mockk<VsiServerShipWorld>(relaxed = true)
        val server = mockk<MinecraftServer>(moreInterfaces = arrayOf(IShipObjectWorldServerProvider::class))
        every { (server as IShipObjectWorldServerProvider).shipObjectWorld } returns sow
        every { (w.level as ServerLevel).server } returns server
        val ship = mockk<LoadedServerShip>(relaxed = true) {
            every { id } returns 1L
            every { chunkClaimDimension } returns "audit"
            every { transform.shipToWorld } returns Matrix4d()
            every { transform.shipToWorldRotation } returns Quaterniond()
            every { getAttachment(BalloonController::class.java) } returns c
        }
        every { sow.isChunkInShipyard(any(), any(), any()) } returns true
        every { sow.allShips.getByChunkPos(any(), any(), any()) } returns ship
        every { sow.loadedShips.getById(any()) } returns ship
        val atmosphere = sow.aerodynamicUtils
        every { atmosphere.getAirDensityForY(any(), any()) } returns 1.225
        every { atmosphere.getAirPressureForY(any(), any()) } returns 101325.0
        every { atmosphere.getAirTemperatureForY(any(), any()) } returns 288.15
        return ship
    }

    private fun massForPressure(gas: GasType, pressure: Double, volume: Double, temp: Double): Double {
        val moles = pressure * volume / (DuctNetwork.idealGasConstant * temp)
        return gas.molesToMass(moles)
    }

    @Test
    fun `sealed scan requires full 6-sided enclosure and starts empty`() {
        val openWorld = World().apply { box(floor = false) }
        val openController = BalloonController()
        val (openId, openResult) = openController.tryGetOrCreateSealedBalloon(BlockPos(2, 2, 2), openWorld.level)
        assertEquals(-1, openId)
        assertEquals(SealedScanResult.NOT_SEALED, openResult)

        val closedWorld = World().apply { box(floor = true) }
        val closedController = BalloonController()
        val (blockedId, blockedResult) = closedController.tryGetOrCreateSealedBalloon(BlockPos(0, 2, 2), closedWorld.level)
        assertEquals(-1, blockedId)
        assertEquals(SealedScanResult.BLOCKED, blockedResult)

        closedWorld.unloaded.add(BlockPos(2, 1, 2))
        val (unloadedId, unloadedResult) = closedController.tryGetOrCreateSealedBalloon(BlockPos(2, 2, 2), closedWorld.level)
        assertEquals(-1, unloadedId)
        assertEquals(SealedScanResult.UNLOADED, unloadedResult)
        closedWorld.unloaded.clear()

        val (id, result) = closedController.tryGetOrCreateSealedBalloon(BlockPos(2, 2, 2), closedWorld.level)
        assertEquals(SealedScanResult.LINKED, result)
        val balloon = closedController.getBalloonById(id)!!
        assertTrue(balloon.sealed)
        assertEquals(36.0, balloon.currentVolume, 1e-9)
        assertTrue(balloon.gasMasses.isEmpty())
        assertEquals(0.0, balloon.currentEnergy, 1e-9)
        assertEquals(EnclosureStatus.VALID, balloon.enclosureStatus)

        // Removing a floor block must be detected as a leak on a sealed envelope.
        val floorBlock = BlockPos(2, 0, 2)
        closedWorld.solids.remove(floorBlock)
        closedController.onBlockChanged(floorBlock)
        closedController.processBlockChanges(closedWorld.level)
        assertEquals(EnclosureStatus.LEAKING, balloon.validate(closedWorld.level))
        assertEquals(1, balloon.missingExternalPositions)
    }

    @Test
    fun `sealed blimp has zero vacuum lift and scales buoyancy with effective volume capped at hull volume`() {
        val w = World(server = true).apply { box(floor = true) }
        val c = BalloonController()
        val ship = serverShip(w, c)
        val level = w.level as ServerLevel
        val (id, _) = c.tryGetOrCreateSealedBalloon(BlockPos(2, 2, 2), level)
        val b = c.getBalloonById(id)!!

        // 1. Empty blimp: zero vacuum buoyancy.
        val emptyForce = b.makeForceData(level, ship)
        assertEquals(0.0, emptyForce.hotAir, 1e-9)
        assertEquals(0.0, emptyForce.volume, 1e-9)

        // 2. Half-filled cold Helium (0.5 atm, 288.15 K): 50% effective volume and proportional lift.
        val halfMass = massForPressure(helium, 101325.0 * 0.5, 36.0, 288.15)
        b.gasMasses[helium.resourceLocation.toString()] = halfMass
        b.currentEnergy = halfMass * BalloonThermodynamics.specificCapacity(helium) * 288.15
        val halfForce = b.makeForceData(level, ship)
        assertEquals(18.0, halfForce.volume, 1e-6)
        assertEquals(1.225 * 18.0 - halfMass, halfForce.hotAir, 1e-6)
        assertTrue(halfForce.hotAir > 0.0)

        // 3. Full cold Helium (1.0 atm): 100% effective volume, exactly 2x lift of half-filled.
        val fullMass = massForPressure(helium, 101325.0, 36.0, 288.15)
        b.gasMasses[helium.resourceLocation.toString()] = fullMass
        b.currentEnergy = fullMass * BalloonThermodynamics.specificCapacity(helium) * 288.15
        val fullForce = b.makeForceData(level, ship)
        assertEquals(36.0, fullForce.volume, 1e-6)
        assertEquals(halfForce.hotAir * 2.0, fullForce.hotAir, 1e-6)

        // 4. Overpressurized cold Helium (2.0 atm): effective volume capped at 36 m³, lift slightly lower due to extra gas mass.
        val overMass = massForPressure(helium, 101325.0 * 2.0, 36.0, 288.15)
        b.gasMasses[helium.resourceLocation.toString()] = overMass
        b.currentEnergy = overMass * BalloonThermodynamics.specificCapacity(helium) * 288.15
        val overForce = b.makeForceData(level, ship)
        assertEquals(36.0, overForce.volume, 1e-6)
        assertTrue(overForce.hotAir < fullForce.hotAir)

        // 5. Full ambient Air (1.0 atm, 288.15 K): ~0 lift (air density ~1.225).
        val airMass = 1.225 * 36.0
        b.gasMasses.clear()
        b.gasMasses[air.resourceLocation.toString()] = airMass
        b.currentEnergy = airMass * BalloonThermodynamics.specificCapacity(air) * 288.15
        val airForce = b.makeForceData(level, ship)
        assertEquals(0.0, airForce.hotAir, 1e-6)
    }

    @Test
    fun `sealed permeation is outward-only and tiny when intact but rapid when punctured`() {
        val w = World(server = true).apply { box(floor = true) }
        val c = BalloonController()
        val ship = serverShip(w, c)
        val level = w.level as ServerLevel
        val (id, _) = c.tryGetOrCreateSealedBalloon(BlockPos(2, 2, 2), level)
        val b = c.getBalloonById(id)!!

        // Empty blimp stays empty (never sucks outside air in).
        repeat(50) { c.gameTick(level, ship, null) }
        assertTrue(b.gasMasses.isEmpty())
        assertEquals(0.0, b.currentEnergy, 1e-9)
        assertNotNull(c.getBalloonById(id))

        // Half-filled Helium loses a tiny amount via passive permeation and never gains air.
        val initialMass = massForPressure(helium, 101325.0 * 0.5, 36.0, 288.15)
        b.gasMasses[helium.resourceLocation.toString()] = initialMass
        b.currentEnergy = initialMass * BalloonThermodynamics.specificCapacity(helium) * 288.15
        repeat(100) { c.gameTick(level, ship, null) }
        val afterPassive = b.gasMasses[helium.resourceLocation.toString()] ?: 0.0
        assertFalse(b.gasMasses.containsKey(air.resourceLocation.toString()))
        assertTrue(afterPassive < initialMass, "Expected tiny outward permeation")
        assertTrue(afterPassive > initialMass * 0.99, "Passive blimp permeation should be very small over 100 ticks")

        // Puncturing the hull vents rapidly and removes the balloon once vented.
        // Avoid ClockworkSounds <clinit> (Architectury @ExpectPlatform) in common unit tests.
        val spied = spyk(b)
        every { spied.announceLeak(any(), any()) } returns Unit
        every { spied.sendLeakParticles(any(), any(), any()) } returns Unit
        spied.timeSinceLeakSound = 1000
        c.balloons[id] = spied

        val holePos = BlockPos(2, 0, 2)
        w.solids.remove(holePos)
        c.onBlockChanged(holePos)
        repeat(200) { c.gameTick(level, ship, null) }
        assertNull(c.getBalloonById(id), "Punctured sealed blimp should vent and be removed")
    }

    @Test
    fun `unsealed enclosed balloon upgrades to sealed preserving gas while open-bottom refuses upgrade`() {
        // Enclosed unsealed balloon upgrades cleanly.
        val closedWorld = World().apply { box(floor = true) }
        val closedController = BalloonController()
        val existing = BalloonData(arrayListOf(AABBi(1, 1, 1, 4, 5, 4)), hashMapOf(), 0.0, 0.0, false, sealed = false)
        existing.recalculateVolume()
        existing.validate(closedWorld.level)
        existing.gasMasses[helium.resourceLocation.toString()] = 4.2
        existing.currentEnergy = 4.2 * BalloonThermodynamics.specificCapacity(helium) * 300.0
        closedController.addBalloon(existing)

        val (upgradedId, upgradeResult) = closedController.tryGetOrCreateSealedBalloon(BlockPos(2, 2, 2), closedWorld.level)
        assertEquals(SealedScanResult.LINKED, upgradeResult)
        val upgraded = closedController.getBalloonById(upgradedId)!!
        assertSame(existing, upgraded)
        assertTrue(upgraded.sealed)
        assertEquals(4.2, upgraded.gasMasses[helium.resourceLocation.toString()]!!, 1e-9)

        // Open-bottom unsealed balloon refuses upgrade.
        val openWorld = World().apply { box(floor = false) }
        val openController = BalloonController()
        val openBalloon = BalloonData(arrayListOf(AABBi(1, 0, 1, 4, 5, 4)), hashMapOf(), 0.0, 0.0, false, sealed = false)
        openBalloon.recalculateVolume()
        openBalloon.validate(openWorld.level)
        openController.addBalloon(openBalloon)

        val (refusedId, refusedResult) = openController.tryGetOrCreateSealedBalloon(BlockPos(2, 2, 2), openWorld.level)
        assertEquals(-1, refusedId)
        assertEquals(SealedScanResult.NOT_SEALED, refusedResult)
        assertFalse(openBalloon.sealed)
    }

    @Test
    fun `shared virtual BalloonDuctNode pumps and vents cleanly across multiple independent valve ports`() {
        val w = World(server = true).apply { box(floor = true) }
        val c = BalloonController()
        val ship = serverShip(w, c)
        val level = w.level as ServerLevel
        val network = DuctNetworkServer(disabled = false)

        val (id, _) = c.tryGetOrCreateSealedBalloon(BlockPos(2, 2, 2), level)
        val b = c.getBalloonById(id)!!
        val balloonNodePos = b.ensureKelvinNode(network, testDim)!!
        assertTrue(network.getNodeAt(balloonNodePos) is BalloonDuctNode)
        // Virtual node Y is fractional (+0.5) so it never collides with integer block nodes.
        assertEquals(0.5, balloonNodePos.y - kotlin.math.floor(balloonNodePos.y), 1e-9)

        // Port A (open valve) connected to a high-pressure Helium supply tank.
        val supplyPos = DuctNodePos(-2.0, 2.0, 2.0, testDim)
        val portAPos = DuctNodePos(0.0, 2.0, 2.0, testDim)
        val portANode = ValveDuctNode(
            portAPos,
            volume = 0.25,
            maxPressure = 16375049.0,
            maxTemperature = 1478.0,
            heatCapacity = 44.9,
            radius = BlimpPortBlock.MAX_FLOW_RADIUS
        )
        network.addNode(
            supplyPos,
            PipeDuctNode(supplyPos, volume = 50.0, maxPressure = 16375049.0, maxTemperature = 1478.0, heatCapacity = 44.9)
        )
        network.addNode(portAPos, portANode)
        network.addEdge(supplyPos, portAPos, PipeDuctEdge(ConnectionType.PIPE, supplyPos, portAPos, radius = 0.3125, length = 0.5))
        network.addEdge(portAPos, balloonNodePos, PipeDuctEdge(ConnectionType.PIPE, portAPos, balloonNodePos, radius = 0.3125, length = 0.375))
        network.addGasAtTemperature(supplyPos, helium, 12.0, 288.15)

        // Port B (closed valve, radius = 0) connected to an empty sink tank.
        val portBPos = DuctNodePos(4.0, 2.0, 2.0, testDim)
        val sinkPos = DuctNodePos(6.0, 2.0, 2.0, testDim)
        val portBNode = ValveDuctNode(
            portBPos,
            volume = 0.25,
            maxPressure = 16375049.0,
            maxTemperature = 1478.0,
            heatCapacity = 44.9,
            radius = 0.0
        )
        network.addNode(portBPos, portBNode)
        network.addNode(
            sinkPos,
            PipeDuctNode(sinkPos, volume = 50.0, maxPressure = 16375049.0, maxTemperature = 1478.0, heatCapacity = 44.9)
        )
        network.addEdge(balloonNodePos, portBPos, PipeDuctEdge(ConnectionType.PIPE, balloonNodePos, portBPos, radius = 0.3125, length = 0.375))
        network.addEdge(portBPos, sinkPos, PipeDuctEdge(ConnectionType.PIPE, portBPos, sinkPos, radius = 0.3125, length = 0.5))

        // Step Kelvin + BalloonController together: Helium inflates the blimp via Port A while Port B stays sealed.
        repeat(40) {
            network.tick(level, subSteps = 10)
            c.gameTick(level, ship, network)
        }
        val inflatedMass = b.gasMasses[helium.resourceLocation.toString()] ?: 0.0
        assertTrue(inflatedMass > 1.0, "Expected Helium to flow into blimp through open Port A, got $inflatedMass kg")
        assertEquals(0.0, network.getGasMassAt(sinkPos).values.sum(), 1e-6, "Closed Port B must prevent gas from reaching sink")
        assertTrue(c.forcefulBalloons.isNotEmpty() && c.forcefulBalloons[0].hotAir > 0.0, "Inflated Helium blimp must generate lift")

        // Remove Port A: balloon keeps its gas and virtual node intact.
        network.removeEdge(portAPos, balloonNodePos)
        network.removeEdge(supplyPos, portAPos)
        network.removeNode(portAPos)
        val massBeforePortBOpen = b.gasMasses[helium.resourceLocation.toString()] ?: 0.0
        assertTrue(massBeforePortBOpen > 1.0)
        assertNotNull(network.getNodeAt(balloonNodePos))

        // Open Port B: gas vents from the shared virtual node into the sink.
        portBNode.radius = BlimpPortBlock.MAX_FLOW_RADIUS
        repeat(40) {
            network.tick(level, subSteps = 10)
            c.gameTick(level, ship, network)
        }
        val massAfterVent = b.gasMasses[helium.resourceLocation.toString()] ?: 0.0
        assertTrue(massAfterVent < massBeforePortBOpen * 0.8, "Opening Port B should vent blimp gas into sink")
        assertTrue(network.getGasMassAt(sinkPos)[helium] ?: 0.0 > 0.5, "Vented Helium should arrive in sink")
    }

    @Test
    fun `kelvin gas reactions execute inside sealed blimp virtual node`() {
        val w = World(server = true).apply { box(floor = true) }
        val c = BalloonController()
        val ship = serverShip(w, c)
        val level = w.level as ServerLevel
        val network = DuctNetworkServer(disabled = false)

        val (id, _) = c.tryGetOrCreateSealedBalloon(BlockPos(2, 2, 2), level)
        val b = c.getBalloonById(id)!!

        // Seed hot fuel + air inside the sealed blimp.
        b.gasMasses[fuel.resourceLocation.toString()] = 1.0
        b.gasMasses[air.resourceLocation.toString()] = 2.0
        b.currentEnergy = BalloonThermodynamics.capacity(b.resolvedMasses()) * 1000.0
        val initialEnergy = b.currentEnergy
        val nodePos = b.ensureKelvinNode(network, testDim)!!

        val combustion = GasBaseRecipe(
            gasses = hashMapOf(fuel to 0.1, air to 0.2),
            result = hashMapOf(exhaust to 0.3),
            requirements = hashMapOf(),
            energy = 500_000.0
        )

        val reactionId = ResourceLocation("audit", "test_combustion")
        val previousReaction = KelvinReactionDataLoader.gas_reactions.put(reactionId, combustion)
        try {
            network.tick(level, subSteps = 1)
            b.pullFromKelvin(network)
        } finally {
            if (previousReaction == null) KelvinReactionDataLoader.gas_reactions.remove(reactionId)
            else KelvinReactionDataLoader.gas_reactions[reactionId] = previousReaction
        }

        assertTrue((b.gasMasses[exhaust.resourceLocation.toString()] ?: 0.0) > 2.5, "Expected combustion product inside blimp")
        assertTrue((b.gasMasses[fuel.resourceLocation.toString()] ?: 0.0) < 0.01, "Expected fuel to be consumed")
        assertTrue(b.currentEnergy > initialEnergy + 1_000_000.0, "Expected exothermic reaction energy in blimp")

        // Verify BalloonController continues ticking cleanly after the reaction.
        c.gameTick(level, ship, network)
        assertTrue(BalloonThermodynamics.isValidSealedState(b.resolvedMasses(), b.currentEnergy))
    }

    @Test
    fun `sealed flag round-trips through Jackson and survives split and merge`() {
        val mapper = ObjectMapper()
            .setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.NONE)
            .setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY)
            .registerModule(SimpleModule().addAbstractTypeMapping(AABBic::class.java, AABBi::class.java))

        val original = BalloonData(arrayListOf(AABBi(1, 1, 1, 4, 5, 4)), hashMapOf(helium.resourceLocation.toString() to 2.5), 1500.0, 36.0, false, sealed = true)
        val restored = mapper.readValue(mapper.writeValueAsString(original), BalloonData::class.java)
        assertTrue(restored.sealed)
        assertEquals(2.5, restored.gasMasses[helium.resourceLocation.toString()]!!, 1e-9)

        // Legacy JSON without "sealed" defaults to false.
        val legacyJson = mapper.writeValueAsString(original).replace("\"sealed\":true,", "").replace(",\"sealed\":true", "")
        val legacyRestored = mapper.readValue(legacyJson, BalloonData::class.java)
        assertFalse(legacyRestored.sealed)

        // Split & merge preserve sealed flag and clean up merged Kelvin virtual node.
        val w = World(server = true).apply { box(minX = 0, maxX = 8, maxY = 5, maxZ = 4, floor = true) }
        val network = DuctNetworkServer(disabled = false)
        val b = BalloonData(arrayListOf(AABBi(1, 1, 1, 8, 5, 4)), hashMapOf(helium.resourceLocation.toString() to 6.0), 4000.0, 84.0, false, sealed = true)
        b.recalculateVolume()
        b.validate(w.level)
        b.ensureKelvinNode(network, testDim)

        // Add a wall down x = 4 to split into two sealed chambers.
        for (y in 1..4) for (z in 1..3) w.solids.add(BlockPos(4, y, z))
        val (changed, additional) = b.trySplit(w.level, network)
        assertTrue(changed)
        assertEquals(1, additional.size)
        val second = additional[0]
        assertTrue(b.sealed)
        assertTrue(second.sealed)
        val secondNodePos = second.ensureKelvinNode(network, testDim)!!
        assertNotNull(network.getNodeAt(secondNodePos))

        // Remove the dividing wall and merge back into one sealed chamber.
        for (y in 1..4) for (z in 1..3) w.solids.remove(BlockPos(4, y, z))
        b.mergeWith(second, w.level, network)
        assertTrue(b.sealed)
        assertNull(second.kelvinNodePos)
        assertNull(network.getNodeAt(secondNodePos), "Merged source balloon's virtual Kelvin node should be removed")
    }
}
