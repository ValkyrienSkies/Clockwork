package org.valkyrienskies.clockwork.content.schematics

import io.mockk.every
import io.mockk.mockk
import net.minecraft.SharedConstants
import net.minecraft.core.BlockPos
import net.minecraft.nbt.CompoundTag
import net.minecraft.nbt.ListTag
import net.minecraft.server.Bootstrap
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import net.minecraft.world.level.block.Blocks
import org.joml.Vector3d
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.valkyrienskies.clockwork.content.contraptions.phys.bearing.PhysBearingBlockEntity
import org.valkyrienskies.clockwork.content.contraptions.phys.linear.PhysicalPistonBlockEntity
import org.valkyrienskies.clockwork.content.logistics.solid.delivery.cannon.DeliveryCannonBlockEntity
import org.valkyrienskies.clockwork.util.ClockworkBlockReferences

class VdexSafeNbtTest {
    companion object {
        @JvmStatic @BeforeAll fun bootstrap() { SharedConstants.tryDetectVersion(); Bootstrap.bootStrap() }
    }

    @Test
    fun `ordinary inventory and unknown block entity data are not copied`() {
        val original = CompoundTag().apply {
            put("Items", ListTag().apply { add(ItemStack(Items.DIAMOND, 64).save(CompoundTag())) })
            put("ModdedHiddenInventory", get("Items")!!.copy())
            putString("LootTable", "minecraft:chests/end_city_treasure")
        }
        assertNull(VdexSafeNbt.block(Blocks.CHEST.defaultBlockState(), BlockPos.ZERO, original))
        assertEquals(1, original.getList("Items", 10).size)
    }

    @Test
    fun `delivery cannon keeps aiming without duplicating loaded or airborne stacks`() {
        val be = mockk<DeliveryCannonBlockEntity>()
        every { be.blockState } returns Blocks.STONE.defaultBlockState()
        val original = CompoundTag().apply {
            putFloat("xRot", 32f); putInt("distance", 20)
            put("CurrentStack", ItemStack(Items.DIAMOND, 64).save(CompoundTag()))
            put("MidAirStack", get("CurrentStack")!!.copy())
            put("FutureInventoryName", get("CurrentStack")!!.copy())
        }
        val safe = CompoundTag()
        VdexSafeNbt.preserveClockwork(be, original, safe)
        assertEquals(setOf("xRot", "distance"), safe.allKeys)
        assertEquals(32f, safe.getFloat("xRot"))
    }

    @Test
    fun `bearing anchors and settings survive without runtime resources`() {
        val be = mockk<PhysBearingBlockEntity>()
        every { be.blockState } returns Blocks.STONE.defaultBlockState()
        val original = CompoundTag().apply {
            putInt("jointId", 12); putLong("partnerShipId", 23)
            putDouble("partnerPosx", 100.5); putDouble("PartnerAnchorx", 101.0)
            putDouble("NeutralRotW", 1.0); putFloat("targetAngle", 37f)
            put("Items", ListTag()); putInt("FuelTicks", 200)
        }
        val safe = CompoundTag()
        VdexSafeNbt.preserveClockwork(be, original, safe)
        assertEquals(12, safe.getInt("jointId"))
        assertEquals(23L, safe.getLong("partnerShipId"))
        assertEquals(101.0, safe.getDouble("PartnerAnchorx"))
        assertEquals(37f, safe.getFloat("targetAngle"))
        assertFalse(safe.contains("Items")); assertFalse(safe.contains("FuelTicks"))
    }

    @Test
    fun `piston connection can still remap after sanitization`() {
        val be = mockk<PhysicalPistonBlockEntity>()
        every { be.blockState } returns Blocks.STONE.defaultBlockState()
        val original = CompoundTag().apply {
            putLong("ShaftOrigin", BlockPos(2, 3, 4).asLong()); putLong("ShaftShip", 7)
            putInt("Head", 4); putInt("Tail", 2); putDouble("LockedOffset", 1.5)
        }
        val safe = CompoundTag()
        VdexSafeNbt.preserveClockwork(be, original, safe)
        ClockworkBlockReferences.paste(safe, mapOf(7L to 8L), mapOf(7L to (Vector3d() to Vector3d(10.0, 0.0, 0.0))), emptyMap())
        assertEquals(8L, safe.getLong("ShaftShip"))
        assertEquals(BlockPos(12, 3, 4), BlockPos.of(safe.getLong("ShaftOrigin")))
        assertEquals(1.5, safe.getDouble("LockedOffset"))
    }

    @Test
    fun `displayed container item loses contents but keeps its display name`() {
        val stack = ItemStack(Items.SHULKER_BOX)
        stack.getOrCreateTagElement("BlockEntityTag").put("Items", ListTag().apply {
            add(ItemStack(Items.DIAMOND, 64).save(CompoundTag()))
        })
        stack.getOrCreateTagElement("display").putString("Name", "\"Toolbox\"")
        val safe = ItemStack.of(VdexSafeNbt.item(stack.save(CompoundTag())))
        assertNull(safe.getTagElement("BlockEntityTag"))
        assertEquals("\"Toolbox\"", safe.getTagElement("display")!!.getString("Name"))
        assertNotNull(stack.getTagElement("BlockEntityTag"))
    }
}
