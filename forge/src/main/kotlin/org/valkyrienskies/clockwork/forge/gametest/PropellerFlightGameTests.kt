package org.valkyrienskies.clockwork.forge.gametest

import com.mojang.authlib.GameProfile
import com.simibubi.create.AllBlocks
import com.simibubi.create.content.kinetics.motor.CreativeMotorBlockEntity
import io.netty.channel.embedded.EmbeddedChannel
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.gametest.framework.AfterBatch
import net.minecraft.gametest.framework.BeforeBatch
import net.minecraft.gametest.framework.GameTest
import net.minecraft.gametest.framework.GameTestHelper
import net.minecraft.network.Connection
import net.minecraft.network.ConnectionProtocol
import net.minecraft.network.PacketSendListener
import net.minecraft.network.protocol.Packet
import net.minecraft.network.protocol.PacketFlow
import net.minecraft.network.protocol.handshake.ClientIntentionPacket
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.ChunkPos
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.block.state.properties.BlockStateProperties.FACING
import net.minecraftforge.gametest.GameTestHolder
import net.minecraftforge.gametest.PrefixGameTestTemplate
import net.minecraftforge.network.NetworkHooks
import org.joml.Quaterniond
import org.joml.Vector3d
import org.valkyrienskies.clockwork.ClockworkBlocks
import org.valkyrienskies.clockwork.ClockworkConfig
import org.valkyrienskies.clockwork.ClockworkItems
import org.valkyrienskies.clockwork.ClockworkMod
import org.valkyrienskies.clockwork.content.contraptions.propeller.PropellerBearingBlockEntity
import org.valkyrienskies.clockwork.content.contraptions.propeller.blades.BladeControllerBlockEntity
import org.valkyrienskies.core.api.ships.ServerShip
import org.valkyrienskies.core.impl.config.VSCoreConfig
import org.valkyrienskies.mod.common.assembly.ShipAssembler
import org.valkyrienskies.mod.common.shipObjectWorld
import org.valkyrienskies.mod.common.vsCore
import org.valkyrienskies.mod.common.vsPipeline
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption.APPEND
import java.util.UUID
import kotlin.math.abs

/** Real Forge blocks and native VS physics; the rig is free to translate and rotate in the air. */
@GameTestHolder(ClockworkMod.MOD_ID)
@PrefixGameTestTemplate(false)
class PropellerFlightGameTests {
    companion object {
        private const val BATCH = "propeller_flight"
        private val ships = mutableListOf<ServerShip>()
        private val chunks = mutableSetOf<ChunkPos>()
        private val players = mutableListOf<ServerPlayer>()
        private val channels = mutableListOf<EmbeddedChannel>()
        private var previousSync = false
        private var previousSpinTorque = false
        private var previousBladeWear = false
        private var previousConfig = doubleArrayOf()

        @JvmStatic @BeforeBatch(batch = BATCH)
        fun prepare(level: ServerLevel) {
            previousSync = VSCoreConfig.SERVER.pt.synchronizePhysics
            previousSpinTorque = ClockworkConfig.SERVER.propellerSpinTorqueEnabled
            previousBladeWear = ClockworkConfig.SERVER.bladeControllerUsesDurability
            previousConfig = doubleArrayOf(ClockworkConfig.SERVER.forceMulPerSailInPropeller,
                ClockworkConfig.SERVER.sailPropellerForceMultiplier, ClockworkConfig.SERVER.propellerMaxForce,
                ClockworkConfig.SERVER.propellerMaxTorque)
            VSCoreConfig.SERVER.pt.synchronizePhysics = true
            ClockworkConfig.SERVER.propellerSpinTorqueEnabled = false
            ClockworkConfig.SERVER.bladeControllerUsesDurability = false
            ClockworkConfig.SERVER.forceMulPerSailInPropeller = 240.0
            ClockworkConfig.SERVER.sailPropellerForceMultiplier = 4.0
            ClockworkConfig.SERVER.propellerMaxForce = 0.0
            ClockworkConfig.SERVER.propellerMaxTorque = 0.0
        }

        @JvmStatic @AfterBatch(batch = BATCH)
        fun cleanup(level: ServerLevel) {
            ships.forEach { ShipAssembler.deleteShip(level, it, true, false) }
            chunks.forEach { level.setChunkForced(it.x, it.z, false) }
            players.forEach { level.server.playerList.remove(it) }
            channels.forEach { it.finishAndReleaseAll() }
            ships.clear(); chunks.clear(); players.clear(); channels.clear()
            VSCoreConfig.SERVER.pt.synchronizePhysics = previousSync
            ClockworkConfig.SERVER.propellerSpinTorqueEnabled = previousSpinTorque
            ClockworkConfig.SERVER.bladeControllerUsesDurability = previousBladeWear
            ClockworkConfig.SERVER.forceMulPerSailInPropeller = previousConfig[0]
            ClockworkConfig.SERVER.sailPropellerForceMultiplier = previousConfig[1]
            ClockworkConfig.SERVER.propellerMaxForce = previousConfig[2]
            ClockworkConfig.SERVER.propellerMaxTorque = previousConfig[3]
        }

        @JvmStatic
        @GameTest(template = "clockworkforgegametests.bladetestpositive", batch = BATCH, timeoutTicks = 300)
        fun bladeControllerDoesNotRollAnAirborneShipWhenSpinTorqueIsDisabled(helper: GameTestHelper) {
            val level = helper.level
            helper.assertTrue(!level.server.vsPipeline.isUsingDummyPhysics, "Flight check requires native VS physics")
            ClockworkMod.LOGGER.info("CW369_BACKEND {}", level.server.vsPipeline.getPhysicsBackendType())
            val base = helper.absolutePos(BlockPos(3, 3, 3)).atY(220)
            val observer = observer(level, base)
            val blocks = mutableSetOf<BlockPos>()
            // Match the two transverse inertias so pitching in free fall cannot turn into
            // shaft-axis spin through rigid-body precession. No rotation is constrained.
            for (x in -1..1) for (y in -1..1) for (z in -4..-2) blocks.add(base.offset(x, y, z))
            blocks.forEach { level.setBlockAndUpdate(it, Blocks.IRON_BLOCK.defaultBlockState()) }
            val motorPos = base.north()
            val bladePos = base.south()
            level.setBlockAndUpdate(motorPos, AllBlocks.CREATIVE_MOTOR.get().defaultBlockState().setValue(FACING, Direction.SOUTH))
            level.setBlockAndUpdate(base, ClockworkBlocks.BRASS_PROPELLER_BEARING.get().defaultBlockState().setValue(FACING, Direction.SOUTH))
            level.setBlockAndUpdate(bladePos, ClockworkBlocks.BLADE_CONTROLLER.get().defaultBlockState().setValue(FACING, Direction.SOUTH))
            blocks.addAll(listOf(motorPos, base, bladePos))
            (level.getBlockEntity(motorPos) as CreativeMotorBlockEntity).generatedSpeed.setValue(128)
            (level.getBlockEntity(bladePos) as BladeControllerBlockEntity).apply {
                repeat(4) { slot ->
                    blades[slot] = ItemStack(ClockworkItems.WIDE_PROPELLER_BLADE.get()).apply {
                        orCreateTag.putDouble("BladeLength", 3.5)
                    }
                }
                angleController.setValue(-12)
                setChanged()
            }
            val ship = ShipAssembler.assembleToShip(level, blocks, 1.0)
            ships.add(ship)
            ship.isStatic = true
            val bounds = ship.shipAABB!!
            val bearingPos = BlockPos(bounds.minX() + 1, bounds.minY() + 1, bounds.minZ() + 4)
            for (x in (bounds.minX() shr 4)..(bounds.maxX() shr 4)) {
                for (z in (bounds.minZ() shr 4)..(bounds.maxZ() shr 4)) force(level, ChunkPos(x, z))
            }
            for (x in -2..2) for (z in -8..8) force(level, ChunkPos((base.x shr 4) + x, (base.z shr 4) + z))
            val output = Path.of(System.getProperty("clockwork.flight.output", "flight-369.csv"))
            output.toAbsolutePath().parent?.let(Files::createDirectories)
            Files.writeString(output, "spin_torque_enabled,tick,x,y,z,forward_speed,shaft_spin_rad_s,integrated_roll_deg,rpm\n")

            lateinit var bearing: PropellerBearingBlockEntity
            var phase = -1
            var firstTick = 0L
            var startPosition = Vector3d()
            var peakSpin = 0.0
            var disabledPeakSpin = 0.0
            var peakRoll = 0.0
            var disabledPeakRoll = 0.0
            var roll = 0.0
            fun beginPhase(enabled: Boolean) {
                ClockworkConfig.SERVER.propellerSpinTorqueEnabled = enabled
                level.shipObjectWorld.teleportShip(ship, vsCore.newShipTeleportData(
                    Vector3d(base.x + .5, 220.5, base.z + .5), Quaterniond(), Vector3d(0.0, 0.0, 20.0), Vector3d()))
                ship.isStatic = false
                firstTick = helper.tick + 3
                peakSpin = 0.0
                peakRoll = 0.0
                roll = 0.0
                ClockworkMod.LOGGER.info("CW369_BEGIN enabled={} ship={} mass={} blades={} pitch={} rpm={}",
                    enabled, ship.id, ship.inertiaData.mass, bearing.blades.size, bearing.blades.firstOrNull()?.angle, bearing.speed)
            }
            helper.runAfterDelay(10) {
                bearing = level.getBlockEntity(bearingPos) as PropellerBearingBlockEntity
                bearing.assemble()
            }
            helper.runAfterDelay(70) {
                helper.assertTrue(bearing.running && bearing.active && abs(bearing.currentOmega) > 1.0,
                    "Blade controller did not start: running=${bearing.running}, active=${bearing.active}, rpm=${bearing.speed}")
                helper.assertTrue(bearing.blades.size == 4, "Expected four real item blades")
                helper.assertTrue(level.shipObjectWorld.loadedShips.getById(ship.id) != null, "Ship is not loaded in physics")
                phase = 0
                beginPhase(false)
            }
            helper.onEachTick {
                val position = ship.transform.positionInWorld
                observer.setPos(position.x() + 10.0, position.y() + 5.0, position.z() + 10.0)
                if (phase < 0 || helper.tick < firstTick) return@onEachTick
                val tick = helper.tick - firstTick
                if (tick == 0L) startPosition = Vector3d(position)
                val axis = ship.transform.shipToWorldRotation.transform(Vector3d(0.0, 0.0, 1.0))
                val spin = ship.angularVelocity.dot(axis)
                peakSpin = maxOf(peakSpin, abs(spin))
                roll += spin * .05
                peakRoll = maxOf(peakRoll, abs(roll))
                val forwardSpeed = ship.velocity.dot(axis)
                val enabled = phase == 1
                Files.writeString(output, "$enabled,$tick,${position.x()},${position.y()},${position.z()},$forwardSpeed,$spin,${Math.toDegrees(roll)},${bearing.speed}\n", APPEND)
                helper.assertTrue(ship.velocity.isFinite && ship.angularVelocity.isFinite, "Nonfinite ship motion")
                helper.assertTrue(!ship.isStatic && position.y() > 100.0, "Rig must remain airborne and unconstrained")
                if (tick < 60) return@onEachTick
                val travel = abs(position.z() - startPosition.z())
                ClockworkMod.LOGGER.info("CW369_RESULT enabled={} travel={} peakSpin={} rollDegrees={} speed={} altitude={} rpm={}",
                    enabled, travel, peakSpin, Math.toDegrees(roll), forwardSpeed, position.y(), bearing.speed)
                helper.assertTrue(travel > 10.0, "Rig did not fly far enough: $travel blocks")
                if (enabled) {
                    helper.assertTrue(peakSpin > .1, "Torque-enabled control did not roll: $peakSpin rad/s")
                    // Native airframe aerodynamics still act during the fall. Bound that small
                    // drift instead of requiring the config to suppress every source of rotation.
                    helper.assertTrue(disabledPeakSpin < .05 && Math.toDegrees(disabledPeakRoll) < 1.0,
                        "Disabled blade-controller shaft torque still rolls the ship: $disabledPeakSpin rad/s")
                    helper.succeed()
                } else {
                    disabledPeakSpin = peakSpin
                    disabledPeakRoll = peakRoll
                    phase = 1
                    beginPhase(true)
                }
            }
        }

        private fun force(level: ServerLevel, chunk: ChunkPos) {
            if (level.setChunkForced(chunk.x, chunk.z, true)) chunks.add(chunk)
        }

        private fun observer(level: ServerLevel, base: BlockPos): ServerPlayer {
            val connection = object : Connection(PacketFlow.SERVERBOUND) {
                override fun send(packet: Packet<*>, listener: PacketSendListener?) { listener?.onSuccess() }
            }
            val channel = EmbeddedChannel()
            channel.pipeline().addLast("packet_handler", connection)
            channel.pipeline().fireChannelActive()
            channels.add(channel)
            NetworkHooks.registerServerLoginChannel(connection, ClientIntentionPacket("localhost", 0, ConnectionProtocol.LOGIN))
            val player = ServerPlayer(level.server, level, GameProfile(UUID.randomUUID(), "flight-test"))
            level.server.playerList.placeNewPlayer(connection, player)
            player.setPos(base.x.toDouble(), base.y + 10.0, base.z.toDouble())
            player.setNoGravity(true)
            player.abilities.flying = true
            players.add(player)
            return player
        }
    }
}
