package org.valkyrienskies.clockwork.util

import net.minecraft.core.BlockPos
import net.minecraft.server.level.ServerLevel
import org.joml.Vector3d
import org.valkyrienskies.core.api.ships.ServerShip
import org.valkyrienskies.kelvin.util.KelvinExtensions.toMinecraft
import org.valkyrienskies.mod.common.assembly.ShipAssembler
import org.valkyrienskies.mod.common.assembly.VSAssemblyEvents
import org.valkyrienskies.mod.common.util.toJOML
import org.valkyrienskies.mod.common.util.toJOMLD
import java.util.concurrent.CompletableFuture

object VS2AssemblyBridge {

    data class AssembleResult(
        val ship: ServerShip,
        val fromCenter: Vector3d,
        val toCenter: Vector3d
    )

    data class MoveResult(
        val wasSuccessful: Boolean,
        val fromCenter: Vector3d,
        val toCenter: Vector3d
    )

    private val shipAssemblerClass = ShipAssembler::class.java
    private val blockTransferClass: Class<*>? by lazy {
        runCatching {
            Class.forName("org.valkyrienskies.mod.common.assembly.BlockTransfer")
        }.getOrNull()
    }

    private val queueAssembleToShipFullMethod by lazy {
        runCatching {
            shipAssemblerClass.getMethod(
                "queueAssembleToShipFull",
                ServerLevel::class.java,
                Set::class.java,
                java.lang.Double.TYPE
            )
        }.getOrNull()
    }

    private val queueTransferBlocksMethod by lazy {
        runCatching {
            shipAssemblerClass.getMethod(
                "queueTransferBlocks",
                ServerLevel::class.java,
                Collection::class.java,
                ServerShip::class.java,
                ServerShip::class.java,
                Vector3d::class.java,
                Vector3d::class.java,
                BlockPos::class.java,
                BlockPos::class.java,
                java.lang.Boolean.TYPE,
                java.lang.Boolean.TYPE
            )
        }.getOrNull()
    }

    private val transferBlocksNowMethod by lazy {
        runCatching {
            shipAssemblerClass.getMethod(
                "transferBlocksNow",
                ServerLevel::class.java,
                Collection::class.java,
                ServerShip::class.java,
                ServerShip::class.java,
                Vector3d::class.java,
                Vector3d::class.java,
                BlockPos::class.java,
                BlockPos::class.java,
                java.lang.Boolean.TYPE,
                java.lang.Boolean.TYPE
            )
        }.getOrNull()
    }

    private fun <T> failedFuture(error: Throwable): CompletableFuture<T> =
        CompletableFuture<T>().also { it.completeExceptionally(error) }

    @Suppress("UNCHECKED_CAST")
    private fun <T> invokeFuture(methodResult: Any?, mapper: (Any) -> T): CompletableFuture<T> {
        val future = methodResult as? CompletableFuture<Any?> ?: return failedFuture(
            IllegalStateException("Unexpected assembly bridge return type: ${methodResult?.javaClass?.name}")
        )
        return future.thenApply { value -> mapper(value ?: error("Assembly bridge completed with null value")) }
    }

    private fun readAssembleResult(value: Any): AssembleResult {
        val type = value.javaClass
        val ship = type.getMethod("getShip").invoke(value) as ServerShip
        val fromCenter = (type.getMethod("getFromCenter").invoke(value) as Vector3d).get(Vector3d())
        val toCenter = (type.getMethod("getToCenter").invoke(value) as Vector3d).get(Vector3d())
        return AssembleResult(ship, fromCenter, toCenter)
    }

    private fun readMoveResult(value: Any): MoveResult {
        val type = value.javaClass
        val wasSuccessful = type.getMethod("getWasSuccessful").invoke(value) as Boolean
        val fromCenter = (type.getMethod("getFromCenter").invoke(value) as Vector3d).get(Vector3d())
        val toCenter = (type.getMethod("getToCenter").invoke(value) as Vector3d).get(Vector3d())
        return MoveResult(wasSuccessful, fromCenter, toCenter)
    }

    fun queueAssembleToShip(level: ServerLevel, blocks: Set<BlockPos>, scale: Double = 1.0): CompletableFuture<ServerShip> =
        queueAssembleToShipFull(level, blocks, scale).thenApply { it.ship }

    fun queueAssembleToShipFull(level: ServerLevel, blocks: Set<BlockPos>, scale: Double = 1.0): CompletableFuture<AssembleResult> {
        val queueMethod = queueAssembleToShipFullMethod
        if (queueMethod != null) {
            return try {
                invokeFuture(queueMethod.invoke(null, level, blocks, scale), ::readAssembleResult)
            } catch (error: Throwable) {
                failedFuture(error)
            }
        }

        var centerPositions = Vector3d() to Vector3d()
        val event = VSAssemblyEvents.onPasteBeforeBlocksAreLoaded.on {
            centerPositions = it.centerPosition.first.get(Vector3d()) to it.centerPosition.second.get(Vector3d())
        }
        return try {
            val ship = ShipAssembler.assembleToShip(level, blocks, scale)
            event.unregister()
            CompletableFuture.completedFuture(
                AssembleResult(ship, centerPositions.first.get(Vector3d()), centerPositions.second.get(Vector3d()))
            )
        } catch (error: Throwable) {
            event.unregister()
            failedFuture(error)
        }
    }

    fun moveBlocksFromTo(
        level: ServerLevel,
        blocks: Collection<BlockPos>,
        removeOriginal: Boolean,
        originCenter: BlockPos,
        toCenter: BlockPos,
        originShip: ServerShip?,
        toShip: ServerShip?
    ): MoveResult {
        val directMethod = transferBlocksNowMethod
        if (directMethod != null && blockTransferClass != null) {
            val (minB, maxB) = ShipAssembler.findMinAndMax(blocks)
            return try {
                readMoveResult(
                    directMethod.invoke(
                        null,
                        level,
                        buildTransfers(blocks, originCenter, toCenter),
                        originShip,
                        toShip,
                        originCenter.toJOMLD(),
                        toCenter.toJOMLD(),
                        minB,
                        maxB,
                        removeOriginal,
                        true
                    )!!
                )
            } catch (error: Throwable) {
                MoveResult(false, originCenter.toJOMLD(), toCenter.toJOMLD())
            }
        }

        val (minB, maxB) = ShipAssembler.findMinAndMax(blocks)
        val result = ShipAssembler.moveBlocksFromTo(
            level,
            blocks.toSet(),
            originShip,
            toShip,
            minB,
            maxB,
            toCenter.toJOML(),
            removeOriginal
        )
        return MoveResult(result.wasSuccessful, originCenter.toJOMLD(), toCenter.toJOMLD())
    }

    fun queueMoveBlocksFromTo(
        level: ServerLevel,
        blocks: Collection<BlockPos>,
        removeOriginal: Boolean,
        originCenter: BlockPos,
        toCenter: BlockPos,
        originShip: ServerShip?,
        toShip: ServerShip?
    ): CompletableFuture<MoveResult> {
        val queueMethod = queueTransferBlocksMethod
        if (queueMethod != null && blockTransferClass != null) {
            val (minB, maxB) = ShipAssembler.findMinAndMax(blocks)
            return try {
                invokeFuture(
                    queueMethod.invoke(
                        null,
                        level,
                        buildTransfers(blocks, originCenter, toCenter),
                        originShip,
                        toShip,
                        originCenter.toJOMLD(),
                        toCenter.toJOMLD(),
                        minB,
                        maxB,
                        removeOriginal,
                        true
                    ),
                    ::readMoveResult
                )
            } catch (error: Throwable) {
                failedFuture(error)
            }
        }

        return CompletableFuture.completedFuture(
            moveBlocksFromTo(level, blocks, removeOriginal, originCenter, toCenter, originShip, toShip)
        )
    }

    private fun buildTransfers(blocks: Collection<BlockPos>, originCenter: BlockPos, toCenter: BlockPos): List<Any> {
        val constructor = blockTransferClass?.getConstructor(BlockPos::class.java, BlockPos::class.java)
            ?: return emptyList()

        return blocks.map { sourcePos ->
            val relative = sourcePos.subtract(originCenter)
            constructor.newInstance(sourcePos, toCenter.offset(relative))
        }
    }
}
