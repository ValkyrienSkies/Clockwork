package org.valkyrienskies.clockwork.forge.gametest

import com.simibubi.create.AllBlocks
import com.simibubi.create.content.schematics.cannon.SchematicannonBlockEntity
import com.simibubi.create.content.schematics.requirement.ItemRequirement
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.gametest.framework.AfterBatch
import net.minecraft.gametest.framework.GameTest
import net.minecraft.gametest.framework.GameTestHelper
import net.minecraft.nbt.CompoundTag
import net.minecraft.nbt.IntTag
import net.minecraft.nbt.ListTag
import net.minecraft.nbt.NbtUtils
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.entity.item.ItemEntity
import net.minecraft.world.phys.AABB
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import net.minecraft.world.level.ChunkPos
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.block.entity.BarrelBlockEntity
import net.minecraftforge.gametest.GameTestHolder
import net.minecraftforge.gametest.PrefixGameTestTemplate
import org.joml.Quaterniond
import org.joml.Vector3d
import org.valkyrienskies.clockwork.ClockworkItems
import org.valkyrienskies.clockwork.ClockworkMod
import org.valkyrienskies.clockwork.content.curiosities.tools.gravitron.GravitronFrozenShip
import org.valkyrienskies.clockwork.content.curiosities.tools.wanderwand.WandAnchor
import org.valkyrienskies.clockwork.content.curiosities.tools.wanderwand.WandLinkSchematics
import org.valkyrienskies.clockwork.content.curiosities.tools.wanderwand.WandLinks
import org.valkyrienskies.clockwork.content.schematics.VdexBlueprintData
import org.valkyrienskies.clockwork.content.schematics.VdexBlueprintLibrary
import org.valkyrienskies.clockwork.content.schematics.VdexCannonAccess
import org.valkyrienskies.clockwork.content.schematics.VdexCannonBuild
import org.valkyrienskies.clockwork.content.schematics.VdexCannonInventory
import org.valkyrienskies.clockwork.content.schematics.VdexConstruction
import org.valkyrienskies.core.internal.joints.VSFixedJoint
import org.valkyrienskies.clockwork.util.gtpa
import org.valkyrienskies.mod.common.schematic.VdexConstraintEntry
import org.valkyrienskies.mod.common.schematic.VdexConstraintMetadata
import org.valkyrienskies.core.internal.joints.VSJointPose
import org.valkyrienskies.mod.common.schematic.VdexIO
import org.valkyrienskies.mod.common.schematic.VdexMetadata
import org.valkyrienskies.mod.common.schematic.VdexShipEntry
import org.valkyrienskies.mod.common.schematic.VdexSocialMetadata
import org.valkyrienskies.mod.common.shipObjectWorld
import java.nio.file.Files
import java.nio.file.Path
import java.util.UUID

@GameTestHolder(ClockworkMod.MOD_ID)
@PrefixGameTestTemplate(false)
class VdexBlueprintGameTests {
    companion object {
        private const val TEMPLATE = "clockworkforgegametests.bladetestpositive"
        private val ships = HashSet<Long>()
        private val chunks = HashSet<ChunkPos>()
        private val files = ArrayList<Path>()

        @JvmStatic @AfterBatch(batch = "vdexBlueprints")
        fun cleanup(level: ServerLevel) {
            ships.addAll(level.shipObjectWorld.allShips.filter { it.slug == "Blueprint test" }.map { it.id })
            WandLinks.get(level).links.values.filter { it.a.shipId in ships || it.b.shipId in ships }.toList().forEach { WandLinks.get(level).remove(level, it) }
            ships.forEach { level.shipObjectWorld.allShips.getById(it)?.let(level.shipObjectWorld::deleteShip) }
            chunks.forEach { level.setChunkForced(it.x, it.z, false) }
            files.forEach(Files::deleteIfExists)
            ships.clear(); chunks.clear(); files.clear()
        }

        private fun force(level: ServerLevel, pos: BlockPos) {
            val chunk = ChunkPos(pos); chunks.add(chunk); level.setChunkForced(chunk.x, chunk.z, true); level.getChunkAt(pos)
        }

        private fun fixture(helper: GameTestHelper, links: Boolean = true, fixedJoint: Boolean = false): Triple<SchematicannonBlockEntity, BarrelBlockEntity, String> {
            val level = helper.level
            val pos = helper.absolutePos(BlockPos(2, 3, 2))
            force(level, pos); force(level, pos.east()); force(level, pos.offset(12, 10, 0))
            level.setBlockAndUpdate(pos, AllBlocks.SCHEMATICANNON.defaultState)
            level.setBlockAndUpdate(pos.east(), Blocks.BARREL.defaultBlockState())
            val cannon = level.getBlockEntity(pos) as SchematicannonBlockEntity
            val barrel = level.getBlockEntity(pos.east()) as BarrelBlockEntity
            fun ints(vararg values: Int) = ListTag().also { list -> values.forEach { list.add(IntTag.valueOf(it)) } }
            val template = CompoundTag().apply {
                put("size", ints(1, 2, 1))
                put("palette", ListTag().apply { add(NbtUtils.writeBlockState(Blocks.STONE.defaultBlockState())); add(NbtUtils.writeBlockState(Blocks.BARREL.defaultBlockState())) })
                put("blocks", ListTag().apply {
                    add(CompoundTag().apply { put("pos", ints(0, 0, 0)); putInt("state", 0) })
                    add(CompoundTag().apply {
                        put("pos", ints(0, 1, 0)); putInt("state", 1)
                        put("nbt", CompoundTag().apply {
                            putString("id", "minecraft:barrel")
                            put("Items", ListTag().apply { add(ItemStack(Items.DIAMOND, 64).save(CompoundTag()).apply { putByte("Slot", 0) }) })
                            putString("CustomName", "{\"text\":\"Saved barrel\"}")
                        })
                    })
                })
                put("entities", ListTag())
            }
            val entries = (0..1).map { i -> VdexShipEntry(name = "Blueprint test", nbtFile = "ship_$i.nbt", previousId = i + 101L,
                previousCenter = Vector3d(.5, 1.0, .5), relativePos = Vector3d(i * 4.0, 0.0, 0.0), isStatic = true) }
            val data = entries.associate { it.nbtFile to template.copy() }.toMutableMap()
            if (links) {
                val link = WandLinks.Link(UUID.randomUUID(), WandAnchor(BlockPos.ZERO, Direction.EAST, 101L),
                    WandAnchor(BlockPos.ZERO, Direction.WEST, 102L), true, 4.0,
                    VSJointPose(Vector3d(1.0, .5, .5), Quaterniond()), VSJointPose(Vector3d(0.0, .5, .5), Quaterniond()), true)
                data[WandLinkSchematics.ENTRY] = CompoundTag().apply {
                    putInt("version", 1)
                    put("links", ListTag().apply { add(link.save()) })
                    put("origins", ListTag().apply { for (entry in entries) add(CompoundTag().apply { putLong("ship", entry.previousId); putLong("pos", BlockPos.ZERO.asLong()) }) })
                }
            }
            val path = VdexBlueprintLibrary.directory(level).resolve("test-${UUID.randomUUID()}.vdex"); files.add(path)
            val joints = if (fixedJoint) listOf(VdexConstraintEntry(VSFixedJoint(101L, VSJointPose(Vector3d(), Quaterniond()),
                102L, VSJointPose(Vector3d(), Quaterniond()), compliance = 0.0),
                VdexConstraintMetadata(0, 1, Vector3d(2.0, 0.0, 0.0), Vector3d(-2.0, 0.0, 0.0)), 43)) else emptyList()
            VdexIO.write(path, VdexMetadata(social = VdexSocialMetadata(name = "Build test"), ships = entries, shipIdToIndex = mapOf(101L to 0, 102L to 1), constraints = joints), emptyList(), data)
            val id = VdexBlueprintLibrary.install(level, Files.readAllBytes(path)); files.add(VdexBlueprintLibrary.path(level, id))
            val paper = ClockworkItems.VDEX_BLUEPRINT.asStack()
            VdexBlueprintLibrary.select(paper, id, "Build test")
            paper.orCreateTag.apply { putBoolean("Deployed", true); putDouble("VdexX", pos.x + 12.5); putDouble("VdexY", pos.y + 10.0); putDouble("VdexZ", pos.z + .5) }
            cannon.inventory.setStackInSlot(0, paper)
            cannon.remainingFuel = 100
            cannon.findInventories()
            return Triple(cannon, barrel, id)
        }

        @JvmStatic @GameTest(template = TEMPLATE, batch = "vdexBlueprints", timeoutTicks = 220)
        fun cannonBuildsBlocksBeforeRopesAndResumesPaidFlight(helper: GameTestHelper) {
            val (cannon, barrel, id) = fixture(helper)
            val access = cannon as VdexCannonAccess
            barrel.setItem(0, ItemStack(Items.STONE, 2)); barrel.setItem(1, ItemStack(Items.BARREL, 2))
            barrel.setItem(2, ClockworkItems.ROPE_KNOT.asStack())
            var build: VdexCannonBuild? = null
            helper.startSequence().thenIdle(3).thenExecute {
                build = access.vdexBuild
                helper.assertTrue(build != null, "Cannon did not accept VDex paper")
                cannon.state = SchematicannonBlockEntity.State.RUNNING
            }.thenWaitUntil {
                helper.assertTrue(access.vdexBuild?.cursor == 1 && access.vdexBuild?.saved?.getBoolean("Paid") == true, "Waiting for paid second shot")
            }.thenExecute {
                val ids = build!!.saved.getCompound("Ships")
                val created = ids.allKeys.mapNotNull { helper.level.shipObjectWorld.loadedShips.getById(ids.getLong(it)) }
                ships.addAll(created.map { it.id })
                helper.assertTrue(created.all { it.isStatic && VdexConstruction.locked(it) && it.getAttachment(GravitronFrozenShip::class.java) != null }, "Construction ship not frozen and protected")
                helper.assertTrue(WandLinks.get(helper.level).links.values.none { it.a.shipId in ships }, "Connection was installed before blocks")
                val saved = cannon.saveWithoutMetadata()
                cannon.load(saved)
                build = (cannon as VdexCannonAccess).vdexBuild
                helper.assertTrue((cannon as VdexCannonAccess).vdexBuild?.cursor == 1, "Build cursor was lost on reload")
            }.thenWaitUntil {
                helper.assertTrue(cannon.statusMsg == "finished", "Build has not finished: ${cannon.statusMsg}")
            }.thenExecute {
                val ids = build!!.saved.getCompound("Ships")
                val created = ids.allKeys.mapNotNull { helper.level.shipObjectWorld.loadedShips.getById(ids.getLong(it)) }
                ships.addAll(created.map { it.id })
                helper.assertTrue(created.size == 2, "Expected two distinct ships")
                helper.assertTrue(created.none { VdexConstruction.locked(it) || it.getAttachment(GravitronFrozenShip::class.java) != null }, "Completed ships remained locked")
                helper.assertTrue(WandLinks.get(helper.level).links.values.count { it.a.shipId in ships && it.b.shipId in ships } == 1, "Expected one paid rope")
                for (ship in created) {
                    val bounds = ship.shipAABB!!
                    val copied = helper.level.getBlockEntity(BlockPos(bounds.minX(), bounds.minY() + 1, bounds.minZ())) as BarrelBlockEntity
                    helper.assertTrue(copied.isEmpty, "Stored inventory was duplicated")
                    helper.assertTrue(copied.name.string == "Saved barrel", "Safe block entity settings were lost")
                }
                helper.assertTrue(barrel.isEmpty, "Materials were not consumed exactly once")
                helper.assertTrue(cannon.inventory.getStackInSlot(1).tag?.getString("VdexFile") == id, "Finished blueprint was not returned")
            }.thenSucceed()
        }

        @JvmStatic @GameTest(template = TEMPLATE, batch = "vdexBlueprints", timeoutTicks = 220)
        fun nativeJointIsRemappedAfterBlocks(helper: GameTestHelper) {
            val (cannon, barrel) = fixture(helper, false, true)
            barrel.setItem(0, ItemStack(Items.STONE, 2)); barrel.setItem(1, ItemStack(Items.BARREL, 2))
            var build: VdexCannonBuild? = null
            helper.startSequence().thenIdle(3).thenExecute {
                build = (cannon as VdexCannonAccess).vdexBuild
                cannon.state = SchematicannonBlockEntity.State.RUNNING
            }.thenWaitUntil { helper.assertTrue(cannon.statusMsg == "finished", "Waiting for native joint build: ${cannon.statusMsg}") }
                .thenExecute {
                    val saved = build!!.saved
                    val ids = saved.getCompound("Ships")
                    for (key in ids.allKeys) ships.add(ids.getLong(key))
                    val joints = saved.getCompound("Joints")
                    helper.assertTrue(joints.allKeys.size == 1, "Native joint was omitted or restored twice")
                    val joint = helper.level.gtpa.getJointById(joints.getInt("43"))
                    helper.assertTrue(joint != null && joint.shipId0 == ids.getLong("0") && joint.shipId1 == ids.getLong("1"), "Native joint still references source ships")
                    helper.level.gtpa.removeJoint(joints.getInt("43"))
                }.thenSucceed()
        }

        @JvmStatic @GameTest(template = TEMPLATE, batch = "vdexBlueprints", timeoutTicks = 100)
        fun missingMaterialsDoNotConsumePartialRequirements(helper: GameTestHelper) {
            val (cannon, barrel) = fixture(helper, false)
            barrel.setItem(0, ItemStack(Items.STONE, 1))
            val requirement = ItemRequirement(ItemRequirement.ItemUseType.CONSUME, ItemStack(Items.STONE, 1))
                .union(ItemRequirement(ItemRequirement.ItemUseType.CONSUME, ItemStack(Items.STONE, 1)))
            helper.assertTrue((cannon as VdexCannonInventory).takeMaterials(requirement) != null, "Duplicate requirements used the same item twice")
            helper.assertTrue(barrel.getItem(0).count == 1, "Failed extraction consumed materials")
            helper.succeed()
        }

        @JvmStatic @GameTest(template = TEMPLATE, batch = "vdexBlueprints", timeoutTicks = 100)
        fun stoppingAPaidFlightRefundsWithoutPlacing(helper: GameTestHelper) {
            val (cannon, barrel) = fixture(helper, false)
            barrel.setItem(0, ItemStack(Items.STONE))
            helper.startSequence().thenIdle(3).thenExecute { cannon.state = SchematicannonBlockEntity.State.RUNNING }
                .thenWaitUntil { helper.assertTrue((cannon as VdexCannonAccess).vdexBuild?.saved?.getBoolean("Paid") == true, "Waiting for paid shot") }
                .thenExecute { cannon.state = SchematicannonBlockEntity.State.STOPPED }
                .thenIdle(2).thenExecute {
                    helper.assertTrue((cannon as VdexCannonAccess).vdexBuild == null, "Cancelled build remained active")
                    val drops = helper.level.getEntitiesOfClass(ItemEntity::class.java, AABB(cannon.blockPos).inflate(2.0))
                    helper.assertTrue(drops.any { it.item.`is`(Items.STONE) }, "Paid shot was not returned")
                }.thenSucceed()
        }
    }
}
