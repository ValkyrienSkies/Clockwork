package org.valkyrienskies.clockwork.content.forces

import io.mockk.every
import io.mockk.mockk
import io.mockk.spyk
import io.mockk.verify
import com.fasterxml.jackson.annotation.JsonAutoDetect
import com.fasterxml.jackson.annotation.PropertyAccessor
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.module.SimpleModule
import net.minecraft.SharedConstants
import net.minecraft.core.BlockPos
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.Bootstrap
import net.minecraft.server.MinecraftServer
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.state.BlockState
import org.joml.Matrix4d
import org.joml.Vector3d
import org.joml.Vector3dc
import org.joml.primitives.AABBi
import org.joml.primitives.AABBic
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.valkyrienskies.clockwork.ClockworkConfig
import org.valkyrienskies.clockwork.content.forces.data.BalloonData
import org.valkyrienskies.clockwork.content.forces.data.BalloonData.EnclosureStatus
import org.valkyrienskies.clockwork.util.BlockUpdateCollector
import org.valkyrienskies.core.api.ships.LoadedServerShip
import org.valkyrienskies.core.api.ships.properties.ShipTransform
import org.valkyrienskies.core.api.world.PhysLevel
import org.valkyrienskies.core.impl.game.ships.PhysShipImpl
import org.valkyrienskies.core.internal.world.VsiServerShipWorld
import org.valkyrienskies.kelvin.api.DuctNetwork
import org.valkyrienskies.kelvin.api.GasType
import org.valkyrienskies.kelvin.impl.registry.GasTypeRegistry
import org.valkyrienskies.mod.common.IShipObjectWorldServerProvider
import org.valkyrienskies.mod.common.util.DimensionIdProvider

class BalloonRegressionTest {
    companion object {
        @BeforeAll @JvmStatic fun bootstrap() {
            SharedConstants.tryDetectVersion()
            Bootstrap.bootStrap()
        }
    }

    private class World(server: Boolean = false) {
        val solids = HashSet<BlockPos>()
        val unloaded = HashSet<BlockPos>()
        var reads = 0
        var loadChecks = 0
        val solid = mockk<BlockState> {
            every { isAir } returns false
            every { isCollisionShapeFullBlock(any(), any()) } returns true
        }
        val air = mockk<BlockState> { every { isAir } returns true }
        val level: Level = if (server) mockk<ServerLevel>(relaxed = true, moreInterfaces = arrayOf(DimensionIdProvider::class)) else mockk<Level>()
        init {
            every { level.isLoaded(any()) } answers { loadChecks++; firstArg<BlockPos>() !in unloaded }
            every { level.getBlockState(any()) } answers {
                reads++
                if (firstArg<BlockPos>() in solids) solid else air
            }
            if (server) every { (level as DimensionIdProvider).dimensionId } returns "audit"
        }
        fun box(minX: Int = 0, maxX: Int = 4, height: Int = 5, floor: Boolean = true) {
            for (x in minX..maxX) for (y in 0..height) for (z in 0..4) {
                if (x == minX || x == maxX || y == height || z == 0 || z == 4 || (floor && y == 0)) solids.add(BlockPos(x,y,z))
            }
        }
    }

    private fun balloon(vararg boxes: AABBic) = BalloonData(arrayListOf(*boxes), hashMapOf(), 0.0, 0.0, false).apply { recalculateVolume() }
    private fun scan(world: World, start: BlockPos = BlockPos(2,5,2), seed: BlockPos = start.below()): List<AABBic> {
        val c = BalloonController()
        val shell = c.scanShell(start, world.level, 10000) ?: return emptyList()
        return c.tryFillBalloonFromShell(shell, seed, world.level)
    }
    private fun volume(regions: List<AABBic>) = BalloonGeometry(regions).cells.size
    private fun gas() = GasType("Air", ResourceLocation("kelvin", "air"), 1.293, 1.716e-5, 1.005, 0.026)

    private fun serverShip(w: World, c: BalloonController): LoadedServerShip {
        val sow = mockk<VsiServerShipWorld>(relaxed = true)
        val server = mockk<MinecraftServer>(moreInterfaces = arrayOf(IShipObjectWorldServerProvider::class))
        every { (server as IShipObjectWorldServerProvider).shipObjectWorld } returns sow
        every { (w.level as ServerLevel).server } returns server
        val ship = mockk<LoadedServerShip>(relaxed = true) {
            every { id } returns 1L
            every { chunkClaimDimension } returns "audit"
            every { transform.shipToWorld } returns Matrix4d()
            every { getAttachment(BalloonController::class.java) } returns c
        }
        every { sow.isChunkInShipyard(any(),any(),any()) } returns true
        every { sow.allShips.getByChunkPos(any(),any(),any()) } returns ship
        every { sow.loadedShips.getById(any()) } returns ship
        val atmosphere = sow.aerodynamicUtils
        every { atmosphere.getAirDensityForY(any(),any()) } returns 1.225
        every { atmosphere.getAirPressureForY(any(),any()) } returns 101325.0
        every { atmosphere.getAirTemperatureForY(any(),any()) } returns 288.15
        return ship
    }

    @Test fun `sealed and open bottom volumes do not depend on exterior supports`() {
        for (floor in listOf(true, false)) {
            val w = World().apply { box(floor = floor) }
            val expected = if (floor) 36 else 45
            assertEquals(expected, volume(scan(w)))
            for (y in -5..-1) w.solids.add(BlockPos(0,y,0))
            assertEquals(expected, volume(scan(w)))
        }
    }

    @Test fun `volume and shell budgets reject incomplete scans and accept exact closed volume`() {
        val w = World().apply { box() }
        val config = ClockworkConfig.SERVER.balloons
        val saved = config.hotAirBalloonMaxScanVolume
        try {
            config.hotAirBalloonMaxScanVolume = 10.0
            assertTrue(scan(w).isEmpty())
            config.hotAirBalloonMaxScanVolume = 36.0
            assertEquals(36, volume(scan(w)))
            assertNull(BalloonController().scanShell(BlockPos(2,5,2), w.level, 5))
            assertNotNull(BalloonController().scanShell(BlockPos(2,5,2), w.level, w.solids.size))
        } finally { config.hotAirBalloonMaxScanVolume = saved }
    }

    @Test fun `unknown chunks abort scanning instead of masquerading as holes`() {
        val w = World().apply { box() }
        w.unloaded.add(BlockPos(2,4,2))
        assertTrue(scan(w).isEmpty())
        w.unloaded.clear(); w.unloaded.add(BlockPos(2,5,2))
        assertTrue(scan(w).isEmpty())
    }

    @Test fun `centroid is invariant under partitioning and duplicate boxes`() {
        val b = balloon(AABBi(0,0,0,10,10,10), AABBi(10,0,0,11,1,1), AABBi(0,0,0,10,10,10))
        assertEquals(1001.0, b.currentVolume)
        assertEquals((5000.0 + 10.5) / 1001, b.getCenter().x(), 1e-12)
        assertEquals((5000.0 + 0.5) / 1001, b.getCenter().y(), 1e-12)
    }

    @Test fun `split follows air and conserves mass and energy without duplicating the retained chamber`() {
        val w = World().apply { box(maxX = 6) }
        for (y in 1..4) for (z in 1..3) w.solids.add(BlockPos(3,y,z))
        val b = balloon(AABBi(1,1,1,6,5,4))
        b.gasMasses["kelvin:air"] = 60.0; b.currentEnergy = 6000.0
        val (changed, extra) = b.trySplit(w.level)
        assertTrue(changed); assertEquals(1, extra.size)
        val both = listOf(b) + extra
        assertEquals(48.0, both.sumOf { it.currentVolume })
        assertEquals(60.0, both.sumOf { it.gasMasses.values.sum() }, 1e-12)
        assertEquals(6000.0, both.sumOf { it.currentEnergy }, 1e-12)
        assertFalse(both[0].geometry.cells.any { it in both[1].geometry.cells })
        assertTrue(both.all { it.enclosureStatus == EnclosureStatus.VALID })
    }

    @Test fun `removing a shared wall merges only its neighboring balloons once`() {
        val w = World().apply { box(maxX = 8) }
        for (y in 1..4) for (z in 1..3) w.solids.add(BlockPos(4,y,z))
        val c = BalloonController()
        for (x in listOf(1,20,40,5)) {
            val b = balloon(AABBi(x,1,1,x+3,5,4))
            b.gasMasses["kelvin:air"] = 10.0; b.currentEnergy = 1000.0
            c.addBalloon(b)
        }
        val a = c.balloons[1]!!; val d = c.balloons[4]!!
        val opening = BlockPos(4,3,2)
        w.solids.remove(opening)
        repeat(100) { c.onBlockChanged(opening) }
        c.processBlockChanges(w.level)
        val merged = listOf(a,d).single { !it.shouldRemove }
        assertTrue(listOf(a,d).count { it.shouldRemove } == 1)
        assertFalse(c.balloons[2]!!.shouldRemove); assertFalse(c.balloons[3]!!.shouldRemove)
        assertEquals(20.0, merged.gasMasses.values.sum())
        assertEquals(2000.0, merged.currentEnergy)
        assertEquals(73.0, merged.currentVolume)
    }

    @Test fun `repair validation reads only dirty boundary positions and cache invalidates on geometry changes`() {
        val w = World().apply { box() }
        val leak = BlockPos(0,3,2)
        w.solids.remove(leak)
        val b = balloon(AABBi(1,1,1,4,5,4))
        assertEquals(EnclosureStatus.LEAKING, b.validate(w.level))
        val boundary = b.getExternalPositions()
        w.solids.add(leak); b.markBoundaryDirty(leak); w.reads = 0
        assertEquals(EnclosureStatus.VALID, b.validate(w.level))
        assertEquals(1, w.reads)
        assertFalse(b.isLeaking); assertEquals(0, b.missingExternalPositions)
        repeat(1000) { assertSame(boundary, b.getExternalPositions()) }
        assertEquals(1, w.reads)
        b.updateRegionsNoValidation(listOf(AABBi(1,2,1,4,5,4)), w.level)
        assertNotSame(boundary, b.getExternalPositions()); assertTrue(b.shouldValidate)
    }

    @Test fun `unloaded boundary validation preserves committed leaks and retries atomically`() {
        val w = World().apply { box() }
        val leak = BlockPos(0,3,2); w.solids.remove(leak)
        val b = balloon(AABBi(1,1,1,4,5,4))
        b.validate(w.level)
        w.solids.add(leak); w.unloaded.add(leak); b.markBoundaryDirty(leak)
        assertEquals(EnclosureStatus.UNKNOWN, b.validate(w.level))
        assertTrue(b.isLeaking); assertEquals(1, b.missingExternalPositions); assertTrue(b.shouldValidate)
        w.unloaded.clear()
        assertEquals(EnclosureStatus.VALID, b.validate(w.level))
        assertFalse(b.isLeaking); assertEquals(0, b.missingExternalPositions)
    }

    @Suppress("DEPRECATION")
    @Test fun `deserialized balloon rebuilds transient leak state once`() {
        val w = World().apply { box() }
        val restored = BalloonData()
        restored.regions.add(AABBi(1,1,1,4,5,4)); restored.currentVolume = 999.0; restored.isLeaking = true
        assertTrue(restored.shouldValidate)
        assertEquals(EnclosureStatus.VALID, restored.validate(w.level))
        assertEquals(36.0, restored.currentVolume)
        assertFalse(restored.shouldValidate); assertFalse(restored.isLeaking)
    }

    @Test fun `unrelated updates neither scan geometry nor query blocks`() {
        val w = World()
        val c = BalloonController()
        val b = spyk(balloon(AABBi(0,0,0,40,40,40)))
        c.addBalloon(b)
        repeat(10000) { c.onBlockChanged(BlockPos(1000,1000,it)) }
        c.processBlockChanges(w.level)
        assertEquals(0, w.reads)
        verify(exactly = 0) { b.touchesPosition(any()) }
        verify(exactly = 0) { b.getExternalPositions() }
    }

    @Test fun `enclosure status ordering retains valid and rejects invalid`() {
        assertTrue(EnclosureStatus.VALID.isAtLeast(EnclosureStatus.LEAKING))
        assertFalse(EnclosureStatus.INVALID.isAtLeast(EnclosureStatus.LEAKING))
        assertEquals(EnclosureStatus.INVALID, EnclosureStatus.VALID.weakestOf(EnclosureStatus.INVALID))
    }

    @Test fun `empty physics snapshot applies no forces or damping`() {
        val ship = mockk<PhysShipImpl>(relaxed = true)
        BalloonController().physTick(ship, mockk())
        verify(exactly = 0) { ship.applyWorldTorque(any()) }
        verify(exactly = 0) { ship.applyWorldForce(any(), any()) }
    }

    @Test fun `lift equals displaced mass deficit times gravity independent of density multiplication`() {
        val c = BalloonController()
        c.forcefulBalloons = listOf(BalloonData.PhysBalloonData(Vector3d(),10.0,100.0))
        val transform = mockk<ShipTransform> {
            every { shipToWorld } returns Matrix4d()
            every { positionInShip } returns Vector3d()
        }
        var force = Vector3d()
        val ship = mockk<PhysShipImpl>(relaxed = true) {
            every { this@mockk.transform } returns transform
            every { angularVelocity } returns Vector3d()
            every { velocity } returns Vector3d()
            every { applyWorldForce(any(),any()) } answers { force = Vector3d(firstArg<Vector3dc>()); Unit }
        }
        val level = mockk<PhysLevel> {
            every { dimension } returns "audit"
            every { aerodynamicUtils.getAtmosphereForDimension(any()) } returns Triple(2000.0,62.0,9.81)
        }
        c.physTick(ship,level)
        assertEquals(10.0*9.81*ClockworkConfig.SERVER.balloons.balloonForceMult, force.y, 1e-8)
    }

    @Test fun `gas and energy remain bounded across permeability temperatures leaks and vacuum`() {
        val air = gas()
        for (permeability in listOf(0.0,0.01,1.0)) for (temperature in listOf(100.0,288.15,1500.0))
            for (holes in listOf(0,10,1000)) for (ambientPressure in listOf(0.0,101325.0)) {
                val masses = hashMapOf(air to 33.075)
                var energy = BalloonThermodynamics.capacity(masses)*temperature
                repeat(100) {
                    energy = BalloonThermodynamics.step(masses,energy,27.0,air,ambientPressure,288.15,permeability,1.0,5.0,holes)
                    assertTrue(energy.isFinite() && energy >= 0.0)
                    assertTrue(masses.values.all { it.isFinite() && it >= 0.0 })
                    val capacity = BalloonThermodynamics.capacity(masses)
                    if (capacity > 1e-9) assertTrue(energy / capacity in minOf(temperature,288.15)-1e-8..maxOf(temperature,288.15)+1e-8)
                }
            }
    }

    @Test fun `high permeability vents only to equilibrium rather than producing negative mass`() {
        val air = gas(); val masses = hashMapOf(air to 33.075)
        val energy = BalloonThermodynamics.step(masses,BalloonThermodynamics.capacity(masses)*1500.0,27.0,air,101325.0,288.15,1.0,0.0,5.0,0)
        assertTrue(masses[air]!! in 0.0..33.075)
        val temperature = energy/BalloonThermodynamics.capacity(masses)
        assertEquals(1500.0, temperature, 1e-8)
        assertEquals(101325.0, air.massToMoles(masses[air]!!)*DuctNetwork.idealGasConstant*temperature/27.0, 1e-7)
    }

    @Test fun `rescan keeps an intact chamber and refreshes leaks after a geometry change`() {
        val w = World(true).apply { box() }
        val c = BalloonController(); val ship = serverShip(w,c)
        val b = spyk(balloon(AABBi(1,1,1,4,5,4)))
        every { b.tick(any(),any()) } returns false
        every { b.announceLeak(any(),any()) } returns Unit
        c.addBalloon(b)
        c.gameTick(w.level as ServerLevel,ship)
        val inserted = BlockPos(2,3,2); w.solids.add(inserted); c.onBlockChanged(inserted)
        c.gameTick(w.level,ship)
        assertFalse(b.shouldRemove); assertEquals(35.0,b.currentVolume)
        assertFalse(b.shouldValidate); assertEquals(EnclosureStatus.VALID,b.enclosureStatus)
        val hole = BlockPos(0,3,2); w.solids.remove(hole); c.onBlockChanged(hole)
        c.gameTick(w.level,ship)
        assertFalse(b.shouldRemove); assertEquals(35.0,b.currentVolume)
        assertTrue(b.isLeaking); assertEquals(1,b.missingExternalPositions)
        w.solids.add(hole); repeat(100) { c.onBlockChanged(hole) }; w.reads = 0
        c.gameTick(w.level,ship)
        assertFalse(b.isLeaking); assertEquals(0,b.missingExternalPositions)
        assertEquals(2,w.reads)
        w.reads = 0
        repeat(100) { c.gameTick(w.level,ship) }
        assertEquals(0,w.reads)
    }

    @Test fun `batched split and opening rescan the smaller component too`() {
        val w = World(true).apply { box(maxX = 8); box(minX = -4,maxX = 0) }
        val c = BalloonController(); val ship = serverShip(w,c)
        val b = spyk(balloon(AABBi(1,1,1,8,5,4)))
        every { b.tick(any(),any()) } returns false
        c.addBalloon(b)
        c.gameTick(w.level as ServerLevel,ship)
        for (y in 1..4) for (z in 1..3) {
            val wall = BlockPos(3,y,z)
            w.solids.add(wall); c.onBlockChanged(wall)
        }
        val opening = BlockPos(0,3,2)
        w.solids.remove(opening); c.onBlockChanged(opening)
        c.gameTick(w.level,ship)
        assertEquals(2,c.balloons.size)
        val smallerId = c.getExistingBalloon(BlockPos(1,3,2))
        val smaller = spyk(c.balloons[smallerId]!!)
        every { smaller.tick(any(),any()) } returns false
        c.balloons[smallerId] = smaller
        assertTrue(smaller.shouldReScan)
        c.gameTick(w.level,ship)
        assertFalse(smaller.shouldReScan); assertFalse(smaller.isLeaking)
        assertEquals(61.0,smaller.currentVolume)
        assertEquals(smallerId,c.getExistingBalloon(BlockPos(-2,3,2)))
        assertEquals(48.0,b.currentVolume)
    }

    @Test fun `unloaded shell defers rescan and polls only its missing chunk until it loads`() {
        val w = World(true).apply { box() }
        val c = BalloonController(); val ship = serverShip(w,c)
        val b = spyk(balloon(AABBi(1,1,1,4,5,4)))
        every { b.tick(any(),any()) } returns false
        c.addBalloon(b)
        c.gameTick(w.level as ServerLevel,ship)
        val inserted = BlockPos(2,3,2)
        w.solids.add(inserted); c.onBlockChanged(inserted)
        // Outside the balloon, inside the shell scan.
        w.unloaded.add(BlockPos(-1,0,0))
        c.gameTick(w.level,ship)
        assertTrue(b.shouldReScan); assertTrue(b.shouldValidate)
        assertEquals(EnclosureStatus.UNKNOWN,b.enclosureStatus)
        w.reads = 0; w.loadChecks = 0
        repeat(100) { c.gameTick(w.level,ship) }
        assertTrue(b.shouldReScan); assertEquals(0,w.reads)
        assertTrue(w.loadChecks in 1..5)
        assertTrue(c.forcefulBalloons.isEmpty())
        w.unloaded.clear()
        repeat(21) { c.gameTick(w.level,ship) }
        assertFalse(b.shouldReScan); assertFalse(b.shouldValidate)
        assertEquals(EnclosureStatus.VALID,b.enclosureStatus)
        assertEquals(35.0,b.currentVolume)
    }

    @Test fun `invalid enclosure is removed and unknown enclosure waits without producing forces`() {
        val w = World(true).apply { box() }
        val c = BalloonController(); val ship = serverShip(w,c)
        val b = spyk(balloon(AABBi(1,1,1,4,5,4)))
        every { b.tick(any(),any()) } returns true
        c.addBalloon(b)
        w.unloaded.add(BlockPos(0,3,2))
        c.gameTick(w.level as ServerLevel,ship)
        assertFalse(b.shouldRemove); assertTrue(c.forcefulBalloons.isEmpty())
        verify(exactly = 0) { b.tick(any(),any()) }
        w.unloaded.clear(); w.solids.clear()
        repeat(21) { c.gameTick(w.level,ship) }
        assertTrue(b.shouldRemove); assertTrue(c.balloons.isEmpty()); assertTrue(c.forcefulBalloons.isEmpty())
    }

    @Test fun `block updates do not create controllers and unchanged sealing causes no geometry work`() {
        val w = World(true); val c = spyk(BalloonController()); val ship = serverShip(w,c)
        every { ship.getAttachment(BalloonController::class.java) } returns null
        BlockUpdateCollector.onSetBlock(w.level as ServerLevel,BlockPos.ZERO,w.solid,w.air)
        verify(exactly = 0) { ship.setAttachment(any<BalloonController>()) }
        every { ship.getAttachment(BalloonController::class.java) } returns c
        BlockUpdateCollector.onSetBlock(w.level,BlockPos.ZERO,w.solid,w.solid)
        BlockUpdateCollector.onSetBlock(w.level,BlockPos.ZERO,w.solid,null)
        verify(exactly = 0) { c.onBlockChanged(any()) }
    }

    @Test fun `hot gas at ambient pressure is not mistaken for a cooled balloon`() {
        val w = World(true); val c = BalloonController(); serverShip(w,c)
        val air = gas(); GasTypeRegistry.GAS_TYPES[air.resourceLocation] = air
        val b = balloon(AABBi(0,0,0,3,3,3))
        val mass = air.molesToMass(101325.0*27.0/(DuctNetwork.idealGasConstant*600.0))
        b.gasMasses[air.resourceLocation.toString()] = mass
        b.currentEnergy = mass*BalloonThermodynamics.specificCapacity(air)*600.0
        assertFalse(b.isNearlyAtmospheric(w.level as ServerLevel))
        b.gasMasses[air.resourceLocation.toString()] = 1.225*27
        b.currentEnergy = 1.225*27*BalloonThermodynamics.specificCapacity(air)*288.15
        assertTrue(b.isNearlyAtmospheric(w.level))
    }

    @Test fun `transient geometry and leak flags do not survive a Jackson round trip`() {
        val w = World().apply { box() }
        val b = balloon(AABBi(1,1,1,4,5,4)); b.validate(w.level)
        val mapper = ObjectMapper()
            .setVisibility(PropertyAccessor.ALL,JsonAutoDetect.Visibility.NONE)
            .setVisibility(PropertyAccessor.FIELD,JsonAutoDetect.Visibility.ANY)
            .registerModule(SimpleModule().addAbstractTypeMapping(AABBic::class.java,AABBi::class.java))
        val json = mapper.writeValueAsString(b)
        assertFalse(json.contains("geometryCache")); assertFalse(json.contains("dirtyBoundary"))
        assertFalse(json.contains("shouldValidate")); assertFalse(json.contains("enclosureStatus"))
        val restored = mapper.readValue(json,BalloonData::class.java)
        assertTrue(restored.shouldValidate); assertEquals(EnclosureStatus.UNKNOWN,restored.enclosureStatus)
        assertEquals(EnclosureStatus.VALID,restored.validate(w.level))
        assertEquals(36.0,restored.currentVolume)
    }

    @Test fun `incoming air reaches pressure equilibrium while conserving its thermal energy in a mixture`() {
        val air = gas()
        val helium = GasType("Helium",ResourceLocation("audit","helium"),0.166,1.96e-5,5.1832,0.151,79.4,1.66)
        val masses = hashMapOf(air to 0.1,helium to 0.05)
        val initialEnergy = BalloonThermodynamics.capacity(masses)*100.0
        val energy = BalloonThermodynamics.step(masses,initialEnergy,27.0,air,101325.0,288.15,1.0,0.0,0.0,1000)
        assertEquals(0.05,masses[helium]!!)
        assertEquals(initialEnergy+(masses[air]!!-0.1)*BalloonThermodynamics.specificCapacity(air)*288.15,energy,1e-7)
        val temperature = energy/BalloonThermodynamics.capacity(masses)
        val pressure = masses.entries.sumOf { (gas,mass) -> gas.massToMoles(mass) }*DuctNetwork.idealGasConstant*temperature/27.0
        assertEquals(101325.0,pressure,1e-7)
    }

    @Test fun `invalid thermal states recover before forces without rereading geometry`() {
        val w = World(true).apply { box() }
        val c = BalloonController(); val ship = serverShip(w,c)
        val air = gas()
        val oldAir = GasTypeRegistry.GAS_TYPES.put(air.resourceLocation,air)
        try {
            val b = balloon(AABBi(1,1,1,4,5,4))
            b.validate(w.level); c.addBalloon(b)
            val geometry = b.geometry
            val states = listOf(
                10.0 to Double.NaN,
                10.0 to Double.POSITIVE_INFINITY,
                10.0 to Double.NEGATIVE_INFINITY,
                10.0 to -1.0,
                Double.NaN to 1000000.0,
                Double.POSITIVE_INFINITY to 1000000.0,
                -1.0 to 1000000.0,
                1e308 to 1000000.0,
                1e-9 to Double.MAX_VALUE
            )
            for ((mass,energy) in states) {
                b.gasMasses[air.resourceLocation.toString()] = mass
                b.currentEnergy = energy
                w.reads = 0
                c.gameTick(w.level as ServerLevel,ship)
                assertEquals(1.225*36,b.gasMasses.values.sum(),1e-12)
                assertEquals(288.15,b.currentEnergy/BalloonThermodynamics.capacity(b.resolvedMasses()),1e-9)
                assertTrue(c.forcefulBalloons.isEmpty())
                assertSame(geometry,b.geometry); assertEquals(0,w.reads)
                assertTrue(b.tick(w.level,ship))
                assertTrue(BalloonThermodynamics.isValidState(b.resolvedMasses(),b.currentEnergy))
            }
        } finally {
            if (oldAir == null) GasTypeRegistry.GAS_TYPES.remove(air.resourceLocation)
            else GasTypeRegistry.GAS_TYPES[air.resourceLocation] = oldAir
        }
    }

    @Test fun `invalid gas capacity and invalid simulation results recover in the same tick`() {
        val w = World(true).apply { box() }
        val c = BalloonController(); val ship = serverShip(w,c)
        val air = gas()
        val oldAir = GasTypeRegistry.GAS_TYPES.put(air.resourceLocation,air)
        val badId = ResourceLocation("audit","invalid_thermal_state")
        val oldBad = GasTypeRegistry.GAS_TYPES[badId]
        try {
            val b = balloon(AABBi(1,1,1,4,5,4))
            b.validate(w.level)
            val gases = listOf(
                air.copy(resourceLocation=badId,specificHeatCapacity=Double.POSITIVE_INFINITY),
                air.copy(resourceLocation=badId,specificHeatCapacity=Double.NaN),
                air.copy(resourceLocation=badId,specificHeatCapacity=0.0),
                air.copy(resourceLocation=badId,density=Double.MIN_VALUE)
            )
            for (gas in gases) {
                GasTypeRegistry.GAS_TYPES[badId] = gas
                b.gasMasses.clear(); b.gasMasses[badId.toString()] = 1.0
                b.currentEnergy = 1000000.0
                assertFalse(b.tick(w.level as ServerLevel,ship))
                assertEquals(setOf(air.resourceLocation.toString()),b.gasMasses.keys)
                assertEquals(1.225*36,b.gasMasses.values.sum(),1e-12)
                assertEquals(288.15,b.currentEnergy/BalloonThermodynamics.capacity(b.resolvedMasses()),1e-9)
                assertTrue(b.tick(w.level,ship))
            }
        } finally {
            if (oldAir == null) GasTypeRegistry.GAS_TYPES.remove(air.resourceLocation)
            else GasTypeRegistry.GAS_TYPES[air.resourceLocation] = oldAir
            if (oldBad == null) GasTypeRegistry.GAS_TYPES.remove(badId)
            else GasTypeRegistry.GAS_TYPES[badId] = oldBad
        }
    }

}
