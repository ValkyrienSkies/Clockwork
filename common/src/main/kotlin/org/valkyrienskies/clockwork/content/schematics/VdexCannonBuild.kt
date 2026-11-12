package org.valkyrienskies.clockwork.content.schematics

import com.simibubi.create.AllSoundEvents
import com.simibubi.create.content.schematics.requirement.ItemRequirement
import com.simibubi.create.content.schematics.SchematicPrinter
import com.simibubi.create.content.schematics.cannon.MaterialChecklist
import com.simibubi.create.content.schematics.cannon.SchematicannonBlockEntity
import net.minecraft.core.BlockPos
import net.minecraft.core.registries.Registries
import net.minecraft.nbt.CompoundTag
import net.minecraft.nbt.ListTag
import net.minecraft.sounds.SoundSource
import org.valkyrienskies.clockwork.ClockworkSounds
import net.minecraft.server.level.TicketType
import net.minecraft.world.level.ChunkPos
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate
import net.minecraft.world.phys.Vec3
import org.joml.Quaterniond
import org.joml.Vector3d
import org.joml.Vector3i
import org.valkyrienskies.clockwork.ClockworkMod
import org.valkyrienskies.clockwork.content.contraptions.phys.hinge.HingeLinks
import org.valkyrienskies.clockwork.content.curiosities.lubricant.LubricantData
import org.valkyrienskies.clockwork.content.curiosities.tools.gravitron.GravitronFrozenShip
import org.valkyrienskies.clockwork.content.curiosities.tools.wanderwand.WandLinkSchematics
import org.valkyrienskies.clockwork.content.curiosities.tools.wanderwand.WandLinks
import org.valkyrienskies.clockwork.content.curiosities.tools.wanderwand.WanderwandServer
import org.valkyrienskies.clockwork.util.ClockworkShipSchematics
import org.valkyrienskies.clockwork.util.gtpa
import org.valkyrienskies.core.api.ships.LoadedServerShip
import org.valkyrienskies.core.api.ships.ServerShip
import org.valkyrienskies.mod.common.assembly.ICopyableBlock
import org.valkyrienskies.mod.common.dimensionId
import org.valkyrienskies.mod.common.safeRenameTo
import org.valkyrienskies.mod.common.shipObjectWorld
import org.valkyrienskies.mod.common.util.floorToInt
import org.valkyrienskies.mod.common.vsCore
import org.valkyrienskies.mod.common.yRange
import java.util.UUID

class VdexCannonBuild(val saved: CompoundTag) {
    private var planCache: VdexBuildPlan? = null
    private var inFlightJoint = false
    private var lastTick = Long.MIN_VALUE
    private var lastError = ""
    val id: String get() = saved.getString("File")
    val cursor: Int get() = saved.getInt("Cursor")
    val position: Vec3 get() = Vec3(saved.getDouble("X"), saved.getDouble("Y"), saved.getDouble("Z"))
    private val rotation get() = Quaterniond().rotationY(Math.toRadians(saved.getFloat("Yaw").toDouble()))
    private fun plan(level: ServerLevel) = planCache ?: VdexBuildPlan(VdexBlueprintLibrary.load(level, id), level).also { planCache = it }
    private fun shipId(index: Int) = saved.getCompound("Ships").getLong(index.toString())
    private fun corner(index: Int) = BlockPos.of(saved.getCompound("Corners").getLong(index.toString()))
    private fun ships(level: ServerLevel): List<ServerShip> = plan(level).blueprint.ships.indices.map { level.shipObjectWorld.allShips.getById(shipId(it)) ?: error("A construction ship is missing") }
    private fun destinations(level: ServerLevel) = plan(level).blueprint.ships.mapIndexed { index, ship -> ship.entry.previousId to (shipId(index) to corner(index)) }.toMap()
    private fun stableId(kind: String, index: Int) = UUID.nameUUIDFromBytes("${saved.getUUID("Job")}:$kind:$index".toByteArray())

    fun printer(cannon: SchematicannonBlockEntity): SchematicPrinter = object : SchematicPrinter() {
        override fun isLoaded() = true
        override fun isErrored() = false
        override fun isWorldEmpty() = false
        override fun getAnchor(): BlockPos = BlockPos.containing(position)
        override fun getCurrentTarget(): BlockPos = BlockPos.of(saved.getLong("Target"))
        override fun markAllBlockRequirements(checklist: MaterialChecklist, world: Level, predicate: PlacementPredicate): Int {
            if (world is ServerLevel) plan(world).steps.drop(cursor).forEach { checklist.require(it.requirement) }
            return if (world is ServerLevel) plan(world).steps.size - cursor else 0
        }
        override fun markAllEntityRequirements(checklist: MaterialChecklist) {}
        override fun sendBlockUpdates(level: Level) {}
    }

    fun tick(cannon: SchematicannonBlockEntity) {
        val level = cannon.level as? ServerLevel ?: return
        if (lastTick == level.gameTime) return
        lastTick = level.gameTime
        try {
            val access = cannon as VdexCannonInventory
            val stack = access.blueprint()
            if (stack.item !is VdexBlueprintItem || stack.tag?.getString("VdexFile") != id || cannon.state == SchematicannonBlockEntity.State.STOPPED) {
                cancel(level)
                if (stack.item is VdexBlueprintItem) stack.orCreateTag.putBoolean("VdexStopped", true)
                cannon.statusMsg = "idle"
                cannon.sendUpdate = true; cannon.setChanged()
                (cannon as VdexCannonAccess).vdexBuild = null
                cannon.printer = SchematicPrinter()
                return
            }
            val plan = plan(level)
            cannon.blocksToPlace = plan.steps.size
            cannon.blocksPlaced = cursor
            if (saved.getInt("Flight") > 0) {
                saved.putInt("Flight", saved.getInt("Flight") - 1)
                cannon.setChanged()
                if (saved.getInt("Flight") == 0) deliver(level, cannon, plan.steps[cursor])
                return
            }
            if (saved.getBoolean("Paid")) { deliver(level, cannon, plan.steps[cursor]); return }
            if (cursor >= plan.steps.size) {
                if (!finishMachines(level)) return
                if (!access.canFinishBlueprint()) { cannon.statusMsg = "vdexOutputFull"; cannon.sendUpdate = true; return }
                finish(level)
                check(access.finishBlueprint())
                cannon.state = SchematicannonBlockEntity.State.STOPPED
                cannon.statusMsg = "finished"
                (cannon as VdexCannonAccess).vdexBuild = null
                cannon.printer = SchematicPrinter()
                cannon.sendUpdate = true; cannon.setChanged()
                AllSoundEvents.SCHEMATICANNON_FINISH.playOnServer(level, cannon.blockPos)
                return
            }
            if (cannon.state == SchematicannonBlockEntity.State.PAUSED && cannon.missingItem == null && cannon.remainingFuel > 0) return
            if (cannon.remainingFuel <= 0 && !cannon.hasCreativeCrate) {
                cannon.state = SchematicannonBlockEntity.State.PAUSED
                cannon.statusMsg = "noGunpowder"; cannon.sendUpdate = true
                return
            }
            val step = plan.steps[cursor]
            if ((step.link >= 0 || step.coating) && !finishMachines(level)) return
            val target = worldTarget(level, step)
            if (!level.hasChunkAt(BlockPos.containing(target))) { cannon.positionNotLoaded = true; cannon.statusMsg = "targetNotLoaded"; return }
            cannon.positionNotLoaded = false
            val missing = if (cannon.hasCreativeCrate) null else access.takeMaterials(step.requirement)
            if (missing != null) {
                cannon.missingItem = missing; cannon.state = SchematicannonBlockEntity.State.PAUSED
                cannon.statusMsg = "missingBlock"; cannon.sendUpdate = true
                return
            }
            cannon.missingItem = null; cannon.state = SchematicannonBlockEntity.State.RUNNING
            cannon.statusMsg = if (step.link >= 0) "vdexConnections" else "placing"
            if (!cannon.hasCreativeCrate) cannon.remainingFuel--
            saved.putBoolean("Paid", true)
            saved.putBoolean("Consumed", !cannon.hasCreativeCrate)
            saved.put("PaidItems", ListTag().also { list -> step.requirement.requiredItems.filter { it.usage == ItemRequirement.ItemUseType.CONSUME }.forEach { list.add(it.stack.save(CompoundTag())) } })
            saved.putLong("Target", BlockPos.containing(target).asLong())
            val duration = (8 + VdexBuildEffectPacket.cannonPosition(level, cannon.blockPos).distanceTo(target).toInt() / 8).coerceAtMost(30)
            saved.putInt("Flight", duration)
            cannon.playFiringSound()
            VdexBuildEffectPacket.send(level, cannon.blockPos, target, target,
                step.requirement.requiredItems.firstOrNull()?.stack ?: ItemStack.EMPTY, duration, false)
            cannon.sendUpdate = true; cannon.setChanged()
        } catch (e: Exception) {
            cannon.state = SchematicannonBlockEntity.State.PAUSED
            cannon.statusMsg = "vdexError"; cannon.sendUpdate = true
            if (lastError != e.message) ClockworkMod.LOGGER.warn("VDex construction at {} paused: {}", cannon.blockPos, e.message)
            lastError = e.message.orEmpty()
        }
    }

    private fun ensureShip(level: ServerLevel, cannon: BlockPos, index: Int): ServerShip {
        val stored = shipId(index)
        if (saved.getCompound("Ships").contains(index.toString())) return level.shipObjectWorld.allShips.getById(stored) ?: error("A construction ship is missing")
        val source = plan(level).blueprint.ships[index]
        val point = Vector3d(source.entry.relativePos).rotate(rotation).add(position.x, position.y, position.z)
        val ship = level.shipObjectWorld.createNewShipAtBlock(point.floorToInt(), false, source.entry.scale, level.dimensionId)
        VdexConstruction.reserve(level, ship.id, cannon, source.entry.isStatic)
        ship.isStatic = true
        ship.safeRenameTo(level, source.entry.name)
        ship.unsafeSetTransform(vsCore.newBodyTransform(point, Quaterniond(rotation).mul(source.entry.relativeRot), Vector3d(source.entry.scale), ship.transform.positionInModel))
        val center = ship.chunkClaim.getCenterBlockCoordinates(level.yRange, Vector3i())
        val corner = BlockPos(center.x - source.size.x / 2, center.y - source.size.y / 2, center.z - source.size.z / 2)
        val ids = saved.getCompound("Ships"); ids.putLong(index.toString(), ship.id); saved.put("Ships", ids)
        val corners = saved.getCompound("Corners"); corners.putLong(index.toString(), corner.asLong()); saved.put("Corners", corners)
        return ship
    }

    private fun worldTarget(level: ServerLevel, step: VdexBuildPlan.Step): Vec3 {
        if (step.link >= 0) return links(level)[step.link].a.world(level) ?: position
        val p = step.block?.pos?.let { Vector3d(it.x + .5, it.y + .5, it.z + .5) } ?: Vector3d()
        plan(level).blueprint.ships[step.ship].relativePoint(p).rotate(rotation).add(position.x, position.y, position.z)
        return Vec3(p.x, p.y, p.z)
    }

    private fun links(level: ServerLevel) = WandLinkSchematics.decode(plan(level).blueprint.data.nbtData[WandLinkSchematics.ENTRY] ?: CompoundTag(), destinations(level))
        .mapIndexed { i, link -> link.copy(id = stableId("link", i)).also { it.settings = link.settings; it.appliedLength = link.appliedLength; it.needsReframe = link.needsReframe } }

    private fun deliver(level: ServerLevel, cannon: SchematicannonBlockEntity, step: VdexBuildPlan.Step) {
        if (step.link >= 0) {
            val link = links(level)[step.link]
            val data = WandLinks.get(level)
            if (listOf(link.a, link.b).any { !level.hasChunkAt(it.pos) }) {
                saved.putInt("Flight", 1)
                cannon.positionNotLoaded = true; cannon.statusMsg = "targetNotLoaded"; cannon.sendUpdate = true
                return
            }
            cannon.positionNotLoaded = false
            require(data.links.containsKey(link.id) || data.restore(level, link)) { "Could not attach schematic connection: ${listOf(link.a, link.b).map { "${it.pos}: loaded=${level.hasChunkAt(it.pos)}, state=${level.getBlockState(it.pos)}" }}" }
            WanderwandServer.syncLinks(level)
            val sound = if (link.spring) ClockworkSounds.SPRING_ATTACH_END else ClockworkSounds.ROPE_TIE
            link.a.world(level)?.let { level.playSound(null, it.x, it.y, it.z, sound.mainEvent!!, SoundSource.BLOCKS, 1f, 1f) }
            val a = link.a.world(level) ?: position; val b = link.b.world(level) ?: position
            VdexBuildEffectPacket.send(level, cannon.blockPos, a, b, step.requirement.requiredItems.firstOrNull()?.stack ?: ItemStack.EMPTY, 10, true)
        } else {
            val ship = ensureShip(level, cannon.blockPos, step.ship)
            val origin = corner(step.ship)
            if (step.coating) {
                ClockworkSounds.LUBRICANT_APPLY.playOnServer(level, cannon.blockPos)
            } else if (step.block != null) {
                val block = step.block
                val pos = origin.offset(block.pos)
                level.chunkSource.addRegionTicket(BUILD_TICKET, ChunkPos(pos), 2, cannon.blockPos)
                require(level.setBlock(pos, block.state, Block.UPDATE_CLIENTS or Block.UPDATE_KNOWN_SHAPE) || level.getBlockState(pos) == block.state) { "Could not place schematic block" }
            } else if (step.entity != null) {
                val template = StructureTemplate()
                val source = plan(level).blueprint.data.nbtData[plan(level).blueprint.ships[step.ship].entry.nbtFile]!!.copy()
                source.put("blocks", ListTag())
                source.put("entities", ListTag().also { it.add(step.entity.copy()) })
                template.load(level.holderLookup(Registries.BLOCK), source)
                template.placeInWorld(level, origin, origin, StructurePlaceSettings(), level.random, Block.UPDATE_CLIENTS or Block.UPDATE_KNOWN_SHAPE)
            }
            (ship as? LoadedServerShip)?.let { protect(level, cannon.blockPos, step.ship, it) }
        }
        saved.putBoolean("Paid", false); saved.remove("PaidItems"); saved.putInt("Cursor", cursor + 1)
        cannon.blocksPlaced = cursor; cannon.sendUpdate = true; cannon.setChanged()
    }

    fun protectShips(level: ServerLevel, cannon: BlockPos) {
        if (!saved.contains("Ships")) return
        for (key in saved.getCompound("Ships").allKeys) {
            val ship = level.shipObjectWorld.allShips.getById(shipId(key.toInt())) ?: continue
            if (level.gameTime % 10L == 0L) ship.activeChunksSet.forEach { x, z ->
                level.chunkSource.addRegionTicket(BUILD_TICKET, ChunkPos(x, z), 2, cannon)
            }
            level.shipObjectWorld.loadedShips.getById(ship.id)?.let { protect(level, cannon, key.toInt(), it) }
        }
    }

    private fun protect(level: ServerLevel, cannon: BlockPos, index: Int, ship: LoadedServerShip) {
        if (ship.getAttachment(VdexConstruction::class.java) != null) return
        ship.setAttachment(VdexConstruction().also { it.cannon = cannon.asLong(); it.wasStatic = plan(level).blueprint.ships[index].entry.isStatic })
        ship.isStatic = true
        GravitronFrozenShip.freeze(level, ship, ship.transform.positionInModel)
    }

    private fun finishMachines(level: ServerLevel): Boolean {
        if (saved.getBoolean("Machines")) return true
        val ships = ships(level)
        if (ships.any { level.shipObjectWorld.loadedShips.getById(it.id) == null }) return false
        val data = plan(level).blueprint
        val jointIds = saved.getCompound("Joints")
        for (key in jointIds.allKeys.toList()) if (level.gtpa.getJointById(jointIds.getInt(key)) == null) jointIds.remove(key)
        val joint = data.data.metadata.constraints.firstOrNull { it.previousId != -1 && !jointIds.contains(it.previousId.toString()) }
        if (joint != null) {
            if (!inFlightJoint) {
                inFlightJoint = true
                level.gtpa.addJoint(VdexBuildPhysics.remap(joint, ships), 0) { newId -> level.server.execute {
                    jointIds.putInt(joint.previousId.toString(), newId); saved.put("Joints", jointIds)
                    inFlightJoint = false
                    level.getBlockEntity(BlockPos.of(saved.getLong("Cannon")))?.setChanged()
                } }
            }
            return false
        }
        val ids = data.ships.mapIndexed { i, ship -> ship.entry.previousId to ships[i].id }.toMap()
        val centers = data.ships.mapIndexed { i, ship -> ship.entry.previousId to (Vector3d(ship.entry.previousCenter) to ships[i].shipAABB!!.center(Vector3d())) }.toMap()
        val joints = jointIds.allKeys.associate { it.toInt() to jointIds.getInt(it) }
        for (step in plan(level).blockSteps) {
            val block = step.block!!
            val pos = corner(step.ship).offset(block.pos)
            require(level.getBlockState(pos) == block.state) { "A construction block has changed" }
            val copyable = block.state.block as? ICopyableBlock
            var nbt = block.nbt?.copy()
            if (copyable != null) nbt = copyable.onPaste(level, pos, block.state, ids, centers, joints, nbt) ?: nbt
            if (nbt != null) {
                nbt.putInt("x", pos.x); nbt.putInt("y", pos.y); nbt.putInt("z", pos.z)
                level.getBlockEntity(pos)?.let { it.load(nbt); it.setChanged() }
                level.sendBlockUpdated(pos, block.state, block.state, Block.UPDATE_CLIENTS)
            }
        }
        saved.putBoolean("Machines", true)
        return true
    }

    private fun finish(level: ServerLevel) {
        val payload = plan(level).blueprint.data.nbtData[WandLinkSchematics.ENTRY]
        if (payload != null) {
            val dest = destinations(level)
            val hinges = WandLinkSchematics.decodeHinges(payload, dest)
            hinges.forEachIndexed { index, hinge ->
                val restored = hinge.copy(id = stableId("hinge", index))
                val data = HingeLinks.get(level)
                require(data.links.containsKey(restored.id) || data.restore(level, restored)) { "Could not restore schematic hinge" }
            }
            val attachments = payload.getList("attachments", 10).copy() as ListTag
            attachments.filterIsInstance<CompoundTag>().forEach { it.remove("Balloons"); it.remove("Frozen") }
            ClockworkShipSchematics.paste(level, attachments, dest)
            LubricantData.get(level).pasteSchematic(level, payload.getCompound("lubricant"), dest)
        }
        for (ship in ships(level)) level.shipObjectWorld.loadedShips.getById(ship.id)?.let { VdexConstruction.release(level, it) }
        for (step in plan(level).blockSteps) {
            val pos = corner(step.ship).offset(step.block!!.pos)
            level.blockUpdated(pos, step.block.state.block)
            level.updateNeighborsAt(pos, step.block.state.block)
        }
        WanderwandServer.syncLinks(level)
    }

    fun cancel(level: ServerLevel) {
        for (key in saved.getCompound("Ships").allKeys) level.shipObjectWorld.loadedShips.getById(shipId(key.toInt()))?.let { VdexConstruction.release(level, it) }
        if (saved.getBoolean("Paid") && saved.getBoolean("Consumed")) {
            val pos = BlockPos.of(saved.getLong("Cannon"))
            saved.getList("PaidItems", 10).forEach { Block.popResource(level, pos, ItemStack.of(it as CompoundTag)) }
            saved.putBoolean("Paid", false)
        }
    }

    companion object {
        private val BUILD_TICKET = TicketType.create("clockwork_vdex_build", Comparator.comparingLong<BlockPos> { it.asLong() }, 40)

        @JvmStatic
        fun create(cannon: SchematicannonBlockEntity, stack: ItemStack): VdexCannonBuild {
            val tag = stack.tag ?: error("Position the blueprint first")
            require(tag.getBoolean("Deployed")) { "Position the blueprint first" }
            val saved = CompoundTag().apply {
                putString("File", tag.getString("VdexFile")); putUUID("Job", UUID.randomUUID())
                putDouble("X", tag.getDouble("VdexX")); putDouble("Y", tag.getDouble("VdexY")); putDouble("Z", tag.getDouble("VdexZ"))
                putFloat("Yaw", tag.getFloat("VdexYaw")); putLong("Cannon", cannon.blockPos.asLong()); putLong("Target", cannon.blockPos.asLong())
            }
            val build = VdexCannonBuild(saved)
            require(build.position.distanceToSqr(VdexBuildEffectPacket.cannonPosition(cannon.level as ServerLevel, cannon.blockPos)) <= 256.0 * 256.0) { "Blueprint is too far from the schematicannon" }
            build.plan(cannon.level as ServerLevel)
            cannon.state = SchematicannonBlockEntity.State.PAUSED; cannon.statusMsg = "ready"
            return build
        }
    }
}
