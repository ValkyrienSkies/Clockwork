package org.valkyrienskies.clockwork.forge.gametest

import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.gametest.framework.AfterBatch
import net.minecraft.gametest.framework.GameTest
import net.minecraft.gametest.framework.GameTestHelper
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.InteractionHand
import net.minecraft.world.item.context.UseOnContext
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.phys.BlockHitResult
import net.minecraft.world.phys.Vec3
import net.minecraftforge.gametest.GameTestHolder
import org.joml.Vector3dc
import org.valkyrienskies.clockwork.ClockworkMod
import org.valkyrienskies.core.api.ships.ServerShip
import org.valkyrienskies.mod.common.allShips
import org.valkyrienskies.mod.common.assembly.ShipAssembler


@Suppress("unused")
@GameTestHolder(ClockworkMod.MOD_ID)
class ClockworkForgeGameTests {
    companion object {
        @JvmStatic
        @GameTest(timeoutTicks = 200, setupTicks = 12, batch = "props", template = "")
        // Template Location at 'data/vs_clockwork/structures/clockworkforgegametests.proptestpositive'
        fun propTestPositive(helper: GameTestHelper) {
            generalPropellerTest(helper) { pos ->
                pos.z() < helper.absolutePos(BlockPos(0, 0, -2)).z
            }
        }

        @JvmStatic
        @GameTest(timeoutTicks = 200, setupTicks = 12, batch = "props")
        // Template Location at 'data/vs_clockwork/structures/clockworkforgegametests.proptestnegative'
        fun propTestNegative(helper: GameTestHelper) {
            generalPropellerTest(helper) { pos ->
                pos.z() > helper.absolutePos(BlockPos(0, 0, 4)).z
            }
        }

        @JvmStatic
        @GameTest(timeoutTicks = 200, setupTicks = 12, batch = "props")
        // Template Location at 'data/vs_clockwork/structures/clockworkforgegametests.bladetestnegative'
        fun bladeTestNegative(helper: GameTestHelper) {
            generalPropellerTest(helper) { pos ->
                pos.z() > helper.absolutePos(BlockPos(0, 0, 4)).z
            }
        }

        @JvmStatic
        @GameTest(timeoutTicks = 200, setupTicks = 12, batch = "props")
        // Template Location at 'data/vs_clockwork/structures/clockworkforgegametests.bladetestpositive'
        fun bladeTestPositive(helper: GameTestHelper) {
            generalPropellerTest(helper) { pos ->
                pos.z() < helper.absolutePos(BlockPos(0, 0, -2)).z
            }
        }

        fun generalPropellerTest(helper: GameTestHelper, positionCondition: (Vector3dc) -> Boolean) {
            val from = helper.absolutePos(BlockPos(2, 2, 2))
            val to = helper.absolutePos(BlockPos(4, 4, 5))
            val ship = ShipAssembler.assembleToShip(
                helper.level,
                BlockPos.betweenClosed(from, to).map { it.mutable() }.toSet(),
                1.0
            )

            helper.runAfterDelay(5) {
                val aabb = ship.shipAABB
                helper.assertTrue(aabb != null, "Ship aabb was null")

                val corner = BlockPos(aabb!!.minX(), aabb.minY(), aabb.minZ())
                val bearing = corner.offset(1, 1, 2)

                helper.runAfterDelay(10) {
                    helper.useShipyardBlock(bearing)
                }

                helper.succeedWhen {
                    helper.assertTrue(positionCondition.invoke(ship.transform.positionInWorld), "Ship didn't travel far enough")
                }
            }
        }
    }
}

/**
 * [GameTestHelper.useBlock] assumes local coordinates, which doesn't work with shipyard coordinates.
 * Even if we try to reverse transform the shipyard coordinate to local, precision issues occur.
 * So this method simply bypasses the local coordinate entirely and expects a world coordinate to interact with.
 */
fun GameTestHelper.useShipyardBlock(blockPos: BlockPos) {
    val player = this.makeMockPlayer()
    val blockState: BlockState = this.level.getBlockState(blockPos)
    val result = BlockHitResult(Vec3.atCenterOf(blockPos), Direction.NORTH, blockPos, true)
    val interactionResult = blockState.use(this.level, player, InteractionHand.MAIN_HAND, result)
    if (!interactionResult.consumesAction()) {
        val useOnContext = UseOnContext(player, InteractionHand.MAIN_HAND, result)
        player.getItemInHand(InteractionHand.MAIN_HAND).useOn(useOnContext)
    }
}
