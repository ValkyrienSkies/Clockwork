package org.valkyrienskies.clockwork.content.curiosities.meteor

import net.minecraft.core.BlockPos
import net.minecraft.core.particles.BlockParticleOption
import net.minecraft.core.particles.ParticleTypes
import net.minecraft.core.Vec3i
import net.minecraft.server.level.ServerLevel
import net.minecraft.util.Mth
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.levelgen.Heightmap
import org.joml.RoundingMode
import org.joml.Vector3d
import org.joml.Vector3i
import org.valkyrienskies.clockwork.ClockworkBlocks
import org.valkyrienskies.clockwork.ClockworkMod
import org.valkyrienskies.clockwork.ClockworkPackets
import org.valkyrienskies.clockwork.ClockworkSounds
import org.valkyrienskies.core.api.VsBeta
import org.valkyrienskies.core.api.attachment.getAttachment
import org.valkyrienskies.core.api.events.CollisionEvent
import org.valkyrienskies.core.api.ships.LoadedServerShip
import org.valkyrienskies.core.api.util.GameTickOnly
import org.valkyrienskies.mod.api.dimensionId
import org.valkyrienskies.mod.api.toMinecraft
import org.valkyrienskies.mod.common.assembly.ShipAssembler
import org.valkyrienskies.mod.common.forEach
import org.valkyrienskies.mod.common.shipObjectWorld
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentLinkedQueue
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

@OptIn(VsBeta::class)
object MeteorStorm {
    private data class CollisionShips(val shipIdA: Long, val shipIdB: Long)

    private val collisionsByDimension = ConcurrentHashMap<String, ConcurrentLinkedQueue<CollisionShips>>()
    private val pendingMeteorShips = ConcurrentHashMap<String, ConcurrentHashMap<Long, PendingMeteor>>()
    private val pendingGroundContacts = ConcurrentHashMap<String, ConcurrentHashMap<Long, Boolean>>()

    private data class PendingMeteor(val spawnPos: BlockPos, var touchedGround: Boolean = false)

    fun onCollision(event: CollisionEvent) {
        collisionsByDimension.computeIfAbsent(event.dimensionId) { ConcurrentLinkedQueue() }
            .add(CollisionShips(event.shipIdA, event.shipIdB))
    }

    fun tick(level: ServerLevel) {
        val pendingShips = pendingMeteorShips[level.dimensionId]
        if (pendingShips != null) {
            val iterator = pendingShips.entries.iterator()
            while (iterator.hasNext()) {
                val (shipId, pendingMeteor) = iterator.next()
                val ship = level.shipObjectWorld.loadedShips.getById(shipId) ?: continue
                ship.isStatic = false
                if (pendingMeteor.touchedGround) {
                    markTouchedGround(level, shipId, ship)
                } else {
                    ship.setAttachment(MeteorShipAttachment())
                    syncState(
                        level,
                        shipId,
                        pendingMeteor.spawnPos.x.toDouble(),
                        pendingMeteor.spawnPos.y.toDouble(),
                        pendingMeteor.spawnPos.z.toDouble(),
                        active = true
                    )
                }
                iterator.remove()
            }
            if (pendingShips.isEmpty()) pendingMeteorShips.remove(level.dimensionId, pendingShips)
        }

        val queue = collisionsByDimension[level.dimensionId]
        if (queue != null) {
            while (true) {
                val event = queue.poll() ?: break
                val meteorId = when {
                    level.shipObjectWorld.loadedShips.getById(event.shipIdA)
                        ?.getAttachment<MeteorShipAttachment>() != null -> event.shipIdA
                    level.shipObjectWorld.loadedShips.getById(event.shipIdB)
                        ?.getAttachment<MeteorShipAttachment>() != null -> event.shipIdB
                    pendingShips?.containsKey(event.shipIdA) == true -> event.shipIdA
                    pendingShips?.containsKey(event.shipIdB) == true -> event.shipIdB
                    else -> continue
                }
                val otherId = if (meteorId == event.shipIdA) event.shipIdB else event.shipIdA
                val otherShip = level.shipObjectWorld.loadedShips.getById(otherId)
                val groundId = level.shipObjectWorld.dimensionToGroundBodyIdImmutable[level.dimensionId]
                val touchesGround = otherId == groundId || otherShip == null || otherShip.isStatic
                if (!touchesGround) continue

                val meteorShip = level.shipObjectWorld.loadedShips.getById(meteorId)
                if (meteorShip == null) {
                    val pendingMeteor = pendingShips?.get(meteorId)
                    if (pendingMeteor != null) {
                        pendingMeteor.touchedGround = true
                    } else {
                        queueGroundContact(level, meteorId)
                    }
                    continue
                }

                val attachment = meteorShip.getAttachment<MeteorShipAttachment>()
                if (attachment == null) {
                    queueGroundContact(level, meteorId)
                } else if (!attachment.touchedGround) {
                    markTouchedGround(level, meteorId, meteorShip)
                }
            }
        }

        val groundContacts = pendingGroundContacts[level.dimensionId]
        if (groundContacts != null) {
            val contacts = groundContacts.entries.iterator()
            while (contacts.hasNext()) {
                val contact = contacts.next()
                val ship = level.shipObjectWorld.loadedShips.getById(contact.key)
                val attachment = ship?.getAttachment<MeteorShipAttachment>()
                if (attachment != null) {
                    if (!attachment.touchedGround) {
                        markTouchedGround(level, contact.key, ship)
                    } else if (ship != null) {
                        disassembleMeteor(level, ship)
                    }
                    contacts.remove()
                }
            }
            if (groundContacts.isEmpty()) pendingGroundContacts.remove(level.dimensionId, groundContacts)
        }

        if (level.gameTime % 20L == 0L) {
            for (ship in level.shipObjectWorld.loadedShips) {
                val attachment = ship.getAttachment<MeteorShipAttachment>() ?: continue
                if (attachment.touchedGround) {
                    disassembleMeteor(level, ship)
                    continue
                }
                syncState(
                    level,
                    ship.id,
                    ship.transform.positionInWorld.x(),
                    ship.transform.positionInWorld.y(),
                    ship.transform.positionInWorld.z(),
                    active = !attachment.touchedGround
                )
            }
        }
    }

    @OptIn(GameTickOnly::class)
    fun assembleMeteor(level: ServerLevel, at: Vector3i) {
        val ship = level.shipObjectWorld.createNewShipAtBlock(at, false, 1.0, level.dimensionId)
        val center = BlockPos(
            Vec3i(
                ship.kinematics.positionInModel.toMinecraft().x.toInt(),
                ship.kinematics.positionInModel.toMinecraft().y.toInt(),
                ship.kinematics.positionInModel.toMinecraft().z.toInt()
            )
        )
        ship.isStatic = true
        MeteorGenerator.generate(level, center,
            Mth.randomBetween(level.random, 0.035f, 0.045f).toDouble(),
            Mth.randomBetweenInclusive(level.random, 6, 8),
            Mth.randomBetweenInclusive(level.random, 2, 5))

        val loadedShip = level.shipObjectWorld.loadedShips.getById(ship.id)
        if (loadedShip != null) {
            loadedShip.isStatic = false
            loadedShip.setAttachment(MeteorShipAttachment())
            val position = ship.transform.positionInWorld
            syncState(level, ship.id, position.x(), position.y(), position.z(), active = true)
        } else {
            pendingMeteorShips.computeIfAbsent(level.dimensionId) { ConcurrentHashMap() }[ship.id] =
                PendingMeteor(BlockPos(at.x, at.y, at.z))
        }
    }

    private fun syncState(level: ServerLevel, shipId: Long, x: Double, y: Double, z: Double, active: Boolean) {
        ClockworkPackets.sendToNear(
            level,
            BlockPos.containing(x, y, z),
            256,
            MeteorShipSyncPacket(shipId, active)
        )
    }

    private fun queueGroundContact(level: ServerLevel, shipId: Long) {
        pendingGroundContacts.computeIfAbsent(level.dimensionId) { ConcurrentHashMap() }[shipId] = true
    }

    @OptIn(GameTickOnly::class)
    private fun markTouchedGround(
        level: ServerLevel,
        shipId: Long,
        ship: LoadedServerShip
    ) {
        ship.setAttachment(MeteorShipAttachment(touchedGround = true))
        val bounds = ship.worldAABB
        val impactX = bounds.center(Vector3d()).x
        val impactY = bounds.minY() + 0.5
        val impactZ = bounds.center(Vector3d()).z
        playImpactEffect(level, ship, impactX, impactY, impactZ)
        val position = ship.transform.positionInWorld
        syncState(level, shipId, position.x(), position.y(), position.z(), active = false)
        disassembleMeteor(level, ship)
    }

    @OptIn(GameTickOnly::class)
    private fun playImpactEffect(level: ServerLevel, ship: LoadedServerShip, x: Double, y: Double, z: Double) {
        ClockworkSounds.METEOR_IMPACT.play(level, null, x, y, z, 4f, 1f)
        level.sendParticles(ParticleTypes.DRAGON_BREATH, x, y, z, 10, 0.0, 1.0, 0.0, 1.0)

        val extents = ship.worldAABB.extent(Vector3d())
        val spreadX = extents.x.coerceIn(1.5, 5.0)
        val spreadZ = extents.z.coerceIn(1.5, 5.0)
        level.sendParticles(ParticleTypes.LARGE_SMOKE, x, y, z, 180, spreadX, 1.0, spreadZ, 0.6)
        level.sendParticles(ParticleTypes.FLAME, x, y, z, 120, spreadX * 0.7, 0.6, spreadZ * 0.7, 1.2)
        level.sendParticles(
            BlockParticleOption(ParticleTypes.BLOCK, ClockworkBlocks.COBBLED_NYX.defaultState),
            x, y, z, 48, spreadX, 1.2, spreadZ, 0.2
        )
        level.sendParticles(
            BlockParticleOption(ParticleTypes.BLOCK, ClockworkBlocks.CHARGED_NYX.defaultState),
            x, y, z, 12, spreadX * 0.8, 1.0, spreadZ * 0.8, 0.16
        )
    }

    @OptIn(GameTickOnly::class)
    private fun disassembleMeteor(level: ServerLevel, ship: LoadedServerShip) {
        val bounds = ship.shipAABB
        if (bounds == null) {
            ClockworkMod.LOGGER.warn("Meteor ship {} touched the ground without block bounds; removing the empty ship", ship.id)
            if (ShipAssembler.deleteShip(level, ship, deleteBlocks = false, dropBlocks = false) == 0) {
                ClockworkMod.LOGGER.error("Failed to remove empty meteor ship {} after ground contact", ship.id)
            }
            return
        }

        val blocks = mutableSetOf<BlockPos>()
        bounds.forEach { x, y, z ->
            val pos = BlockPos(x, y, z)
            if (!level.getBlockState(pos).isAir) blocks.add(pos)
        }
        if (blocks.isEmpty()) {
            ClockworkMod.LOGGER.warn("Meteor ship {} touched the ground without any blocks; removing the empty ship", ship.id)
            if (ShipAssembler.deleteShip(level, ship, deleteBlocks = false, dropBlocks = false) == 0) {
                ClockworkMod.LOGGER.error("Failed to remove empty meteor ship {} after ground contact", ship.id)
            }
            return
        }

        val (min, max) = ShipAssembler.findMinAndMax(blocks)
        val localCenter = Vector3d(
            (min.x + max.x + 1.0) / 2.0,
            (min.y + max.y + 1.0) / 2.0,
            (min.z + max.z + 1.0) / 2.0
        )
        val impactCenter = ship.transform.shipToWorld.transformPosition(localCenter, Vector3d())
        val worldExtents = ship.worldAABB.extent(Vector3d())
        val worldHalfHeight = worldExtents.y
        val meteorRadius = maxOf(worldExtents.x, worldExtents.z)
        val worldCenter = Vector3d(impactCenter).also { it.y -= worldHalfHeight }
        val destinationCenter = Vector3i(worldCenter, RoundingMode.FLOOR)
        val moveResult = ShipAssembler.moveBlocksFromTo(
            level,
            blocks,
            ship,
            null,
            min,
            max,
            destinationCenter
        )

        if (!moveResult.wasSuccessful) {
            ClockworkMod.LOGGER.error("Failed to disassemble meteor ship {} after ground contact", ship.id)
            return
        }

        if (ShipAssembler.deleteShip(level, ship, deleteBlocks = false, dropBlocks = false) == 0) {
            ClockworkMod.LOGGER.error("Moved blocks from meteor ship {}, but failed to remove the ship", ship.id)
            return
        }

        scatterImpactBlocks(level, impactCenter, meteorRadius)
    }

    private fun scatterImpactBlocks(level: ServerLevel, impactCenter: Vector3d, meteorRadius: Double) {
        val scatterRadius = (meteorRadius * 2.0).coerceIn(4.0, 32.0)
        val attempts = (PI * scatterRadius * scatterRadius * 0.16).toInt().coerceIn(40, 256)
        val placedPositions = HashSet<BlockPos>()
        var placedCount = 0
        val centerX = floor(impactCenter.x).toInt()
        val centerZ = floor(impactCenter.z).toInt()

        repeat(attempts) {
            val angle = level.random.nextDouble() * (2.0 * PI)
            val distance = sqrt(level.random.nextDouble()) * scatterRadius
            val distanceRatio = distance / scatterRadius
            val placementChance = 0.9 * (1.0 - distanceRatio).pow(1.5)
            if (level.random.nextDouble() > placementChance) return@repeat

            val x = centerX + (cos(angle) * distance).roundToInt()
            val z = centerZ + (sin(angle) * distance).roundToInt()
            val surfaceY = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z) - 1
            val pos = BlockPos(x, surfaceY, z)
            if (!placedPositions.add(pos)) return@repeat

            val existingState = level.getBlockState(pos)
            if (!existingState.isSolidRender(level, pos) || existingState.block == Blocks.BEDROCK) {
                return@repeat
            }

            val debrisState = if (placedCount == 0 || (placedCount > 1 && level.random.nextFloat() < 0.35f)) {
                ClockworkBlocks.CHARGED_NYX.defaultState
            } else {
                ClockworkBlocks.COBBLED_NYX.defaultState
            }
            if (level.setBlock(pos, debrisState, 3)) placedCount++
        }
    }
}
