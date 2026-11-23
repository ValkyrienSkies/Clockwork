package org.valkyrienskies.clockwork.forge.gametest

import com.mojang.authlib.GameProfile
import com.simibubi.create.AllBlocks
import com.simibubi.create.AllItems
import com.simibubi.create.content.equipment.armor.BacktankUtil
import com.simibubi.create.content.kinetics.motor.CreativeMotorBlockEntity
import io.netty.channel.embedded.EmbeddedChannel
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.gametest.framework.AfterBatch
import net.minecraft.gametest.framework.BeforeBatch
import net.minecraft.gametest.framework.GameTest
import net.minecraft.gametest.framework.GameTestHelper
import net.minecraft.network.Connection
import net.minecraft.nbt.CompoundTag
import com.simibubi.create.content.equipment.clipboard.ClipboardCloneable
import net.minecraft.nbt.Tag
import net.minecraft.network.ConnectionProtocol
import net.minecraft.network.FriendlyByteBuf
import net.minecraft.network.PacketSendListener
import net.minecraft.network.protocol.Packet
import net.minecraft.network.protocol.PacketFlow
import net.minecraft.network.protocol.handshake.ClientIntentionPacket
import net.minecraft.network.protocol.game.ClientboundCustomPayloadPacket
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.InteractionHand
import net.minecraft.world.entity.EquipmentSlot
import net.minecraft.world.entity.EntityType
import net.minecraft.world.entity.projectile.Arrow
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.context.UseOnContext
import net.minecraft.world.level.ChunkPos
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.block.SnowLayerBlock
import net.minecraft.world.level.block.state.properties.BlockStateProperties.WATERLOGGED
import net.minecraft.world.level.block.state.properties.BlockStateProperties.FACING
import net.minecraft.world.phys.BlockHitResult
import net.minecraft.world.phys.Vec3
import net.minecraftforge.gametest.GameTestHolder
import net.minecraftforge.gametest.PrefixGameTestTemplate
import net.minecraftforge.network.NetworkHooks
import org.joml.Vector3d
import org.joml.Quaterniond
import org.valkyrienskies.clockwork.ClockworkBlocks
import org.valkyrienskies.clockwork.ClockworkMod
import org.valkyrienskies.clockwork.ClockworkPackets
import org.valkyrienskies.clockwork.content.contraptions.phys.hinge.HingeAssembly
import org.valkyrienskies.clockwork.content.contraptions.phys.hinge.HingeBlock
import org.valkyrienskies.clockwork.content.contraptions.phys.hinge.HingeGeometry
import org.valkyrienskies.clockwork.content.contraptions.phys.hinge.HingeLinks
import org.valkyrienskies.clockwork.content.physicalities.steering.HandbrakeBlock
import org.valkyrienskies.clockwork.content.physicalities.steering.SteeringWheelBlockEntity
import org.valkyrienskies.clockwork.content.physicalities.wheel.WheelContactPacket
import org.valkyrienskies.clockwork.content.physicalities.wheel.WheelHubBlockEntity
import org.valkyrienskies.clockwork.content.physicalities.wheel.WheelHubBlock
import org.valkyrienskies.clockwork.ClockworkItems
import org.valkyrienskies.core.api.bodies.shape.WheelBodyShapeData
import org.valkyrienskies.clockwork.api.wheel.TireTypes
import org.valkyrienskies.clockwork.content.physicalities.wheel.TireType
import org.valkyrienskies.clockwork.content.physicalities.wheel.AirPumpSetting
import org.valkyrienskies.clockwork.content.physicalities.wheel.WheelSize
import org.valkyrienskies.clockwork.util.gtpa
import org.valkyrienskies.core.api.ships.ServerShip
import org.valkyrienskies.clockwork.content.physicalities.wheel.WheelContacts
import org.valkyrienskies.clockwork.content.curiosities.lubricant.LubricantMaterials
import org.valkyrienskies.core.api.physics.blockstates.TireSurfaceSolidState
import org.valkyrienskies.mod.common.BlockStateInfo
import org.valkyrienskies.core.api.physics.TirePatchSnapshot
import org.valkyrienskies.core.impl.config.VSCoreConfig
import org.valkyrienskies.core.internal.joints.VSRevoluteJoint
import org.valkyrienskies.mod.common.assembly.ShipAssembler
import org.valkyrienskies.mod.common.shipObjectWorld
import org.valkyrienskies.mod.common.dimensionId
import org.valkyrienskies.mod.common.vsCore
import java.util.UUID
import java.util.concurrent.atomic.AtomicInteger
import kotlin.math.abs

@GameTestHolder("clockwork_wheels")
@PrefixGameTestTemplate(false)
class WheelGameTests {
    companion object {
        private const val BATCH = "native_wheels"
        private val blocks = mutableSetOf<BlockPos>()
        private val ships = mutableListOf<ServerShip>()
        private val chunks = mutableSetOf<ChunkPos>()
        private val observers = mutableListOf<Pair<ServerPlayer, EmbeddedChannel>>()
        private var previousSync = false
        @Volatile private var probing = false
        @Volatile private var supportedWheels = emptySet<Long>()

        @JvmStatic @BeforeBatch(batch = BATCH)
        fun prepare(level: ServerLevel) {
            previousSync = VSCoreConfig.SERVER.pt.synchronizePhysics
            VSCoreConfig.SERVER.pt.synchronizePhysics = true
            level.gtpa
            probing = true
            supportedWheels = emptySet()
            vsCore.physTickEvent.on { event, listener ->
                if (event.world.dimension != level.dimensionId) return@on
                if (!probing) {
                    listener.unregister()
                    return@on
                }
                val frame = WheelContacts.lastFrame(event.world.dimension)
                if (frame != null) {
                    supportedWheels = supportedWheels + frame.tires.flatMap { it.patches }
                        .filter { it.events and TirePatchSnapshot.END == 0 && it.normalForce > 1.0 }.map { it.wheelBody }
                }
            }
        }

        @JvmStatic @AfterBatch(batch = BATCH)
        fun cleanup(level: ServerLevel) {
            probing = false
            blocks.forEach { level.removeBlock(it, false) }; blocks.clear()
            ships.forEach { ShipAssembler.deleteShip(level, it, true, false) }; ships.clear()
            chunks.forEach { level.setChunkForced(it.x, it.z, false) }; chunks.clear()
            observers.forEach { (player, channel) -> level.removeTestPlayer(player); channel.finishAndReleaseAll() }
            observers.clear()
            VSCoreConfig.SERVER.pt.synchronizePhysics = previousSync
        }

        @JvmStatic @GameTest(template = "wheel", batch = BATCH, timeoutTicks = 20)
        fun tireSurfacesPreserveBlockMaterials(helper: GameTestHelper) {
            val roadType = BlockStateInfo.get(Blocks.GRAY_CONCRETE.defaultBlockState())!!.second
            val sandType = BlockStateInfo.get(Blocks.SAND.defaultBlockState())!!.second
            val road = vsCore.blockTypes.getState(roadType)!!.solidState as TireSurfaceSolidState
            val sand = vsCore.blockTypes.getState(sandType)!!.solidState as TireSurfaceSolidState
            helper.assertTrue(road.tireSurface!!.rollingResistance < sand.tireSurface!!.rollingResistance,
                "Road and sand did not retain distinct tire materials")
            helper.assertTrue(road.tireSurface!!.maximumSinkage == 0.0 && sand.tireSurface!!.maximumSinkage == .12,
                "Soft-ground profile did not reach the registered solid state")
            val slippery = vsCore.blockTypes.getState(LubricantMaterials.lubricated(sandType))!!.solidState as TireSurfaceSolidState
            helper.assertTrue(slippery.tireSurface == sand.tireSurface && slippery.friction == LubricantMaterials.FRICTION,
                "Lubricant lost the soil profile or failed to reduce friction")
            val waterlogged = Blocks.OAK_SLAB.defaultBlockState().setValue(WATERLOGGED, true)
            val wetSolid = vsCore.blockTypes.getState(BlockStateInfo.get(waterlogged)!!.second)!!.solidState as TireSurfaceSolidState
            helper.assertTrue(wetSolid.tireSurface!!.maximumSinkage == 0.0, "Waterlogged wood became soft mud")
            val snow = Blocks.SNOW.defaultBlockState().setValue(SnowLayerBlock.LAYERS, 2)
            val snowSolid = vsCore.blockTypes.getState(BlockStateInfo.get(snow)!!.second)!!.solidState as TireSurfaceSolidState
            helper.assertTrue(snowSolid.tireSurface!!.maximumSinkage <= .0625, "Thin snow layer allows excessive sinking")
            helper.succeed()
        }

        @JvmStatic @GameTest(template = "wheel", batch = BATCH, timeoutTicks = 300)
        fun axesFiniteTorqueFreewheelAndReload(helper: GameTestHelper) {
            val level = helper.level
            val start = helper.absolutePos(BlockPos(2, 0, 2)).let { BlockPos(it.x, 220, it.z) }
            val player = observe(level, start)
            val hubs = Direction.entries.mapIndexed { i, direction ->
                val pos = start.offset(i * 5, 0, 0)
                force(level, pos)
                blocks.add(pos)
                level.setBlockAndUpdate(pos, ClockworkBlocks.WHEEL_HUB.defaultState.setValue(FACING, direction))
                val hub = level.getBlockEntity(pos) as WheelHubBlockEntity
                hub.install(WheelSize.LARGE, player, type = TireType.RUBBER)
                hub.torque.setValue(1)
                val motorPos = pos.relative(direction.opposite)
                blocks.add(motorPos)
                level.setBlockAndUpdate(motorPos, AllBlocks.CREATIVE_MOTOR.defaultState.setValue(FACING, direction))
                val motor = level.getBlockEntity(motorPos) as CreativeMotorBlockEntity
                motor.generatedSpeed.setValue(64)
                hub
            }
            val ids = mutableListOf<Long>()
            helper.startSequence().thenIdle(4).thenExecute {
                for (hub in hubs) {
                    val body = level.shipObjectWorld.allBodies.getById(hub.bodyId) ?: error("Tire body missing")
                    ids.add(body.id)
                    helper.assertTrue(body.kinematics.position.distance(Vector3d(hub.blockPos.x + .5, hub.blockPos.y + .5, hub.blockPos.z + .5)) < .03,
                        "Wheel is not centered on its mount for ${hub.facing}")
                    val axle = body.kinematics.rotation.transform(Vector3d(1.0, 0.0, 0.0))
                    val expected = Vector3d(hub.facing.stepX.toDouble(), hub.facing.stepY.toDouble(), hub.facing.stepZ.toDouble())
                    helper.assertTrue(axle.dot(expected) > .99, "Wheel axle is misaligned for ${hub.facing}")
                    val omega = body.angularVelocity.length()
                    helper.assertTrue(omega > .05 && omega < 4.5, "Motor bypassed finite torque: omega=$omega")
                    val joint = level.gtpa.getAllJoints().values.filterIsInstance<VSRevoluteJoint>().singleOrNull { it.shipId1 == body.id }
                    helper.assertTrue(joint != null, "Missing or duplicated axle joint for ${hub.facing}")
                    helper.assertTrue(joint!!.driveForceLimit == 1000f, "Incorrect axle torque cap")
                    helper.assertTrue(joint.driveFreeSpin == true, "Powered wheel cannot overrun")
                }
            }.thenIdle(75).thenExecute {
                for (hub in hubs) {
                    val body = level.shipObjectWorld.allBodies.getById(hub.bodyId)!!
                    val expected = Vector3d().setComponent(hub.facing.axis.ordinal, 1.0)
                    helper.assertTrue(abs(body.angularVelocity.dot(expected) - hub.speed * Math.PI / 30) < .6,
                        "Incorrect axle speed for ${hub.facing}: rpm=${hub.speed}, omega=${body.angularVelocity}")
                    hub.setTireSetting(AirPumpSetting.WIDTH, 30)
                    helper.assertTrue(hub.tire!!.widthPercent == 100, "Moving tire allowed a geometry change")
                    (level.getBlockEntity(hub.blockPos.relative(hub.facing.opposite)) as CreativeMotorBlockEntity)
                        .generatedSpeed.setValue(32)
                    hub.setTireSetting(AirPumpSetting.PRESSURE, 80)
                }
            }.thenIdle(20).thenExecute {
                for (hub in hubs) {
                    helper.assertTrue(level.shipObjectWorld.allBodies.getById(hub.bodyId)!!.angularVelocity.length() > 6.0,
                        "Powered overrun braked the tire")
                    (level.getBlockEntity(hub.blockPos.relative(hub.facing.opposite)) as CreativeMotorBlockEntity)
                        .generatedSpeed.setValue(0)
                }
            }.thenIdle(45).thenExecute {
                val clipboard = CompoundTag()
                for (hub in hubs) {
                    helper.assertTrue(level.shipObjectWorld.allBodies.getById(hub.bodyId)!!.angularVelocity.length() < .3,
                        "Zero RPM did not brake the tire")
                    val copying = hub == hubs.first()
                    if (copying) {
                        hub.setTireSetting(AirPumpSetting.WIDTH, 25)
                        hub.setTireSetting(AirPumpSetting.THICKNESS, 25)
                    }
                    for (setting in hub.allBehaviours.filterIsInstance<ClipboardCloneable>()) {
                        val key = setting.clipboardKey
                        if (key != "WheelWidth" && key != "WheelThickness") continue
                        if (copying) {
                            val tag = CompoundTag()
                            helper.assertTrue(setting.writeToClipboard(tag, hub.facing), "Could not copy $key")
                            clipboard.put(key, tag)
                        } else {
                            helper.assertTrue(setting.readFromClipboard(clipboard.getCompound(key), player, hub.facing, false),
                                "Could not paste $key")
                        }
                    }
                }
            }.thenIdle(6).thenExecute {
                for ((i, hub) in hubs.withIndex()) {
                    val body = level.shipObjectWorld.allBodies.getById(hub.bodyId)!!
                    val tire = hub.tire!!
                    helper.assertTrue(hub.bodyId == ids[i], "Tire adjustment replaced its body")
                    helper.assertTrue(tire.widthPercent == 125 && tire.thicknessPercent == 125 && tire.pressureKpa == 400,
                        "Tire settings did not apply")
                    helper.assertTrue(body.collisionShape == tire.shape() && abs(body.inertiaData.mass - tire.mass) < .01,
                        "Tire geometry or inertia did not update")
                    val bounds = body.aabb
                    val width = when (hub.facing.axis) {
                        Direction.Axis.X -> bounds.maxX() - bounds.minX()
                        Direction.Axis.Y -> bounds.maxY() - bounds.minY()
                        Direction.Axis.Z -> bounds.maxZ() - bounds.minZ()
                    }
                    helper.assertTrue(abs(width - tire.width) < .08, "Native tire bounds did not resize: $width")
                    val safe = CompoundTag()
                    hub.writeSafe(safe)
                    helper.assertTrue(safe.getInt("WheelWidth") == 25 && safe.getInt("WheelThickness") == 25 &&
                        safe.getInt("WheelPressure") == 80, "Safe schematic NBT lost tire settings")
                    (level.getBlockEntity(hub.blockPos.relative(hub.facing.opposite)) as CreativeMotorBlockEntity)
                        .generatedSpeed.setValue(64)
                }
            }.thenIdle(75).thenExecute {
                for (hub in hubs) {
                    hub.toggleDrive(player)
                    val tag = hub.saveWithFullMetadata()
                    hub.load(tag)
                    val copy = (hub.blockState.block as WheelHubBlock).onCopy(level, hub.blockPos, hub.blockState, hub, emptyList(), emptyMap())!!
                    helper.assertTrue(!copy.contains("WheelBody") && copy.getString("Wheel") == WheelSize.LARGE.id,
                        "Blueprint retained a live body ID or lost the tire")
                }
            }.thenIdle(10).thenExecute {
                for ((i, hub) in hubs.withIndex()) {
                    helper.assertTrue(hub.bodyId == ids[i], "Reload duplicated the tire")
                    val joints = level.gtpa.getAllJoints().values.filterIsInstance<VSRevoluteJoint>().filter { it.shipId1 == hub.bodyId }
                    helper.assertTrue(joints.size == 1 && joints.single().driveVelocity == null, "Reload duplicated the axle or freewheel still drives")
                    helper.assertTrue(level.shipObjectWorld.allBodies.getById(hub.bodyId)!!.angularVelocity.length() > 4.0,
                        "Freewheeling erased angular momentum")
                    hub.uninstall(player)
                }
            }.thenIdle(4).thenExecute {
                helper.assertTrue(ids.none { level.shipObjectWorld.allBodies.contains(it) }, "Removed tires left physics bodies behind")
            }.thenSucceed()
        }

        @JvmStatic @GameTest(template = "wheel", batch = BATCH, timeoutTicks = 150)
        fun tirePuncturesLeakAndCanBePatched(helper: GameTestHelper) {
            val level = helper.level
            val origin = helper.absolutePos(BlockPos(2, 0, 2)).let { BlockPos(it.x, 206, it.z) }
            val player = observe(level, origin)
            player.abilities.instabuild = false
            fun wheel(pos: BlockPos, size: WheelSize): WheelHubBlockEntity {
                force(level, pos)
                blocks.add(pos)
                level.setBlockAndUpdate(pos, ClockworkBlocks.WHEEL_HUB.defaultState.setValue(FACING, Direction.EAST))
                return (level.getBlockEntity(pos) as WheelHubBlockEntity).also {
                    it.install(size, player, type = TireType.RUBBER)
                }
            }
            val hub = wheel(origin, WheelSize.MEDIUM)
            val trap = wheel(origin.south(6), WheelSize.LARGE)
            val spike = trap.blockPos.below()
            blocks.add(spike)
            blocks.add(spike.below())
            level.setBlockAndUpdate(spike.below(), Blocks.STONE.defaultBlockState())
            level.setBlockAndUpdate(spike, Blocks.POINTED_DRIPSTONE.defaultBlockState())
            val patch = ItemStack(ClockworkItems.TAR.get(), 2)
            var repairedPressure = 0
            helper.startSequence().thenIdle(10).thenExecute {
                val arrow = Arrow(EntityType.ARROW, level)
                arrow.setPos(origin.x - 1.5, origin.y + 1.15, origin.z + .5)
                arrow.setNoGravity(true)
                arrow.setDeltaMovement(3.0, 0.0, 0.0)
                level.addFreshEntity(arrow)
            }.thenIdle(24).thenExecute {
                helper.assertTrue(hub.saveWithFullMetadata().getDouble("TirePunctures") == 1.0, "Arrow missed the sidewall")
                val damage = trap.saveWithFullMetadata().getDouble("TirePunctures")
                helper.assertTrue(damage == 3.0, "One continuous dripstone contact should cause three leaks, got $damage")
                val sites = trap.saveWithFullMetadata().getList("TireHoles", Tag.TAG_COMPOUND.toInt())
                helper.assertTrue(sites.size == 3 && sites.toSet().size == 3, "Dripstone did not leave three distinct holes")
                repairedPressure = hub.tire!!.pressureKpa
                helper.assertTrue(repairedPressure in 1..799, "Puncture did not gradually release pressure")
                hub.removeTire(player)
                val used = player.inventory.items.single { it.item == TireTypes.item(TireType.RUBBER) }
                val shot = used.tag!!.getList("TireHoles", Tag.TAG_COMPOUND.toInt())
                helper.assertTrue(shot.size == 1 && shot.getCompound(0).getDouble("Across") < -.6,
                    "The puncture did not stay on the sidewall the arrow hit")
                hub.fitTire(TireType.RUBBER, player, false, used.tag)
                helper.assertTrue(hub.saveWithFullMetadata().get("TireHoles") == shot, "Refitting the tire moved its puncture")
                helper.assertTrue(hub.tire!!.pressureKpa == repairedPressure && hub.repairTire(patch, player) && patch.count == 1,
                    "Removing and refitting a leaking tire lost its condition")
            }.thenIdle(24).thenExecute {
                helper.assertTrue(hub.tire!!.pressureKpa == repairedPressure && !hub.repairTire(patch, player),
                    "A patch must seal the leak without reinflating the tire")
                helper.assertTrue(!hub.saveWithFullMetadata().contains("TireHoles"), "Patched tire retained its puncture mark")
                val data = trap.saveWithFullMetadata()
                data.putInt("WheelPressure", 1)
                trap.load(data)
            }.thenIdle(24).thenExecute {
                val flat = trap.tire!!
                val nativeShape = level.shipObjectWorld.allBodies.getById(trap.bodyId)?.collisionShape as? WheelBodyShapeData
                helper.assertTrue(flat.pressureKpa == 0 && nativeShape?.radius == flat.radius && flat.radius < flat.size.radius,
                    "Flat tire did not collapse in physics")
                for ((type, expected) in listOf(TireType.FLOAT to 2.0, TireType.GUMMY to .5, TireType.FROST to 0.0, TireType.CARVER to 0.0)) {
                    hub.fitTire(type, player, true)
                    hub.puncture(1.0)
                    helper.assertTrue(hub.saveWithFullMetadata().getDouble("TirePunctures") == expected, "Wrong puncture resistance for ${type.id}")
                }
                val worn = CompoundTag().apply { putDouble("TireWear", .5); putDouble("TirePunctures", 1.0) }
                hub.fitTire(TireType.HONEY, player, true, worn)
                helper.assertTrue(hub.repairTire(patch, player) && hub.saveWithFullMetadata().getDouble("TireWear") == .5,
                    "Tar should seal punctures without replacing tread")
                helper.assertTrue(hub.repairTire(ItemStack(Blocks.HONEY_BLOCK), player) && hub.saveWithFullMetadata().getDouble("TireWear") == .25,
                    "Honey did not restore the tread")
            }.thenSucceed()
        }

        @JvmStatic @GameTest(template = "wheel", batch = BATCH, timeoutTicks = 200)
        fun embeddedInstallationCreatesBody(helper: GameTestHelper) {
            val level = helper.level
            val origin = helper.absolutePos(BlockPos(3, 0, 100)).let { BlockPos(it.x, 220, it.z) }
            val compressed = mutableSetOf<Long>()
            val groundEffects = mutableSetOf<Long>()
            val shipEffects = mutableSetOf<Long>()
            val player = observe(level, origin) { packet ->
                packet.wheels.forEach { wheel -> wheel.effect?.let { effect ->
                    if (effect.blockState == Block.getId(Blocks.STONE.defaultBlockState())) {
                        if (effect.surface == -1L) groundEffects.add(wheel.body) else shipEffects.add(wheel.body)
                    }
                } }
                compressed.addAll(packet.wheels.filter { wheel -> wheel.contacts.any { it.surface == -1L && it.compression > 0 } }.map { it.body })
            }
            player.abilities.instabuild = false
            force(level, origin)
            level.setBlockAndUpdate(origin, ClockworkBlocks.WHEEL_HUB.defaultState.setValue(FACING, Direction.EAST))
            level.setBlockAndUpdate(origin.west(), Blocks.OAK_LOG.defaultBlockState())
            val assembly = ShipAssembler.assembleToShipFull(level, setOf(origin, origin.west()))
            val ship = assembly.ship
            ship.isStatic = true
            ships.add(ship)
            val local = Vector3d(origin.x + .5, origin.y + .5, origin.z + .5)
                .sub(assembly.fromCenter).add(assembly.toCenter)
            val hubPos = BlockPos.containing(local.x, local.y, local.z)
            force(level, hubPos)
            val hub = level.getBlockEntity(hubPos) as WheelHubBlockEntity
            val floor = (-2..3).flatMap { x -> (-2..2).map { z -> origin.offset(x, -1, z) } }.toSet()
            floor.forEach { force(level, it); blocks.add(it); level.setBlockAndUpdate(it, Blocks.STONE.defaultBlockState()) }
            val stack = ItemStack(WheelHubBlock.item(WheelSize.LARGE), 2)
            val tires = ItemStack(TireTypes.item(TireType.RUBBER), 2)
            fun fit(stack: ItemStack): Boolean {
                player.setItemInHand(InteractionHand.MAIN_HAND, stack)
                return stack.useOn(UseOnContext(player, InteractionHand.MAIN_HAND,
                    BlockHitResult(Vec3.atCenterOf(hubPos), Direction.EAST, hubPos, false))).consumesAction()
            }
            helper.startSequence().thenIdle(8).thenExecute {
                helper.assertTrue(fit(stack) && hub.tire == null && fit(tires) && stack.count == 1 && tires.count == 1,
                    "Embedded tire installation failed or consumed the wrong number of items")
            }.thenIdle(4).thenExecute {
                helper.assertTrue(level.shipObjectWorld.allBodies.getById(hub.bodyId) != null,
                    "Embedded tire did not create its native body")
                helper.assertTrue(hub.bodyId in compressed, "Ground tire compression was not sent in world coordinates")
                helper.assertTrue(hub.bodyId in groundEffects, "Ground tire contact did not resolve its surface block")
                hub.setTireSetting(AirPumpSetting.PRESSURE, 167)
                val pump = ItemStack(ClockworkItems.AIR_PUMP.get())
                helper.assertTrue(fit(pump) && hub.tire!!.pressureKpa == 900, "Pump did not round 8.35 bar up to 9 bar")
                helper.assertTrue(pump.damageValue == 1, "Pumping without air should cost one durability")
                val driveBefore = hub.engaged
                val hit = BlockHitResult(Vec3.atCenterOf(hubPos), Direction.EAST, hubPos, false)
                for (pumpHand in InteractionHand.values()) {
                    val emptyHand = if (pumpHand == InteractionHand.MAIN_HAND) InteractionHand.OFF_HAND else InteractionHand.MAIN_HAND
                    player.setItemInHand(pumpHand, pump)
                    player.setItemInHand(emptyHand, ItemStack.EMPTY)
                    hub.blockState.use(level, player, emptyHand, hit)
                    helper.assertTrue(hub.engaged == driveBefore, "Empty hand toggled the drive with a pump in $pumpHand")
                }
                player.setItemInHand(InteractionHand.OFF_HAND, ItemStack.EMPTY)
                val tank = AllItems.COPPER_BACKTANK.asStack()
                val full = BacktankUtil.maxAir(tank).toFloat()
                tank.orCreateTag.putFloat("Air", full)
                player.setItemSlot(EquipmentSlot.CHEST, tank)
                player.isShiftKeyDown = true
                helper.assertTrue(fit(pump) && hub.tire!!.pressureKpa == 800, "Pump did not release a full bar")
                helper.assertTrue(pump.damageValue == 1 && BacktankUtil.getAir(tank) < full,
                    "A charged backtank should pay for the adjustment")
                player.setItemSlot(EquipmentSlot.CHEST, ItemStack.EMPTY)
                player.isShiftKeyDown = false
                val saved = hub.saveWithFullMetadata()
                hub.load(saved)
            }.thenIdle(4).thenExecute {
                helper.assertTrue(level.shipObjectWorld.allBodies.getById(hub.bodyId) != null,
                    "Reloading an embedded tire lost its native body")
                hub.uninstall(null)
                val platform = ShipAssembler.assembleToShipFull(level, floor).ship
                platform.isStatic = true
                ships.add(platform)
                level.shipObjectWorld.teleportShip(ship, vsCore.newShipTeleportData(
                    Vector3d(ship.transform.positionInWorld), Quaterniond().rotationZ(.25), Vector3d(), Vector3d()))
            }.thenIdle(8).thenExecute {
                helper.assertTrue(fit(stack) && fit(tires) && stack.isEmpty && tires.isEmpty,
                    "Tire installation intersecting another ship failed")
            }.thenIdle(8).thenExecute {
                helper.assertTrue(level.shipObjectWorld.allBodies.getById(hub.bodyId) != null,
                    "Tire intersecting another ship did not create its native body")
                helper.assertTrue(hub.bodyId in shipEffects, "Ship tire contact did not resolve its surface block")
                player.isShiftKeyDown = true
                helper.assertTrue(fit(ItemStack(ClockworkItems.SCREWDRIVER.get())) && hub.wheel == WheelSize.LARGE && hub.tire == null,
                    "The first screwdriver use should return only the tire")
                hub.load(hub.saveWithFullMetadata())
            }.thenIdle(4).thenExecute {
                val shape = level.shipObjectWorld.allBodies.getById(hub.bodyId)?.collisionShape as? WheelBodyShapeData
                helper.assertTrue(shape != null && shape.tire == null && shape.radius == WheelSize.LARGE.rimRadius,
                    "Reloading the bare wheel should leave a rigid rim at the correct radius")
                helper.assertTrue(fit(ItemStack(ClockworkItems.SCREWDRIVER.get())) && hub.wheel == null,
                    "The second screwdriver use should remove the rim")
            }.thenSucceed()
        }

        @JvmStatic @GameTest(template = "wheel", batch = BATCH, timeoutTicks = 200)
        fun assembledWheelsCarryAndDriveChassis(helper: GameTestHelper) {
            val level = helper.level
            val center = helper.absolutePos(BlockPos(3, 0, 45)).let { BlockPos(it.x, 220, it.z) }
            val player = observe(level, center)
            for (x in -10..10) for (z in -25..25) {
                val floor = center.offset(x, -2, z)
                force(level, floor)
                blocks.add(floor)
                level.setBlockAndUpdate(floor, Blocks.STONE.defaultBlockState())
            }
            val chassis = mutableSetOf<BlockPos>()
            for (z in listOf(-2, 2)) {
                for (x in -1..1) {
                    val p = center.offset(x, 0, z)
                    chassis.add(p)
                    level.setBlockAndUpdate(p, Blocks.OAK_LOG.defaultBlockState())
                }
                for (direction in listOf(Direction.EAST, Direction.WEST)) {
                    val p = center.offset(direction.stepX * 2, 0, z)
                    chassis.add(p)
                    level.setBlockAndUpdate(p, ClockworkBlocks.WHEEL_HUB.defaultState.setValue(FACING, direction))
                    (level.getBlockEntity(p) as WheelHubBlockEntity).install(WheelSize.LARGE, player, type = TireType.RUBBER)
                }
            }
            for (z in -1..1) {
                val p = center.offset(0, 0, z)
                chassis.add(p)
                level.setBlockAndUpdate(p, Blocks.OAK_LOG.defaultBlockState())
            }
            val wheelIds = mutableSetOf<Long>()
            lateinit var ship: ServerShip
            lateinit var hubs: List<WheelHubBlockEntity>
            lateinit var copiedWheel: CompoundTag
            lateinit var clone: WheelHubBlockEntity
            var initialZ = 0.0
            var firmHeight = 0.0
            helper.startSequence().thenIdle(8).thenExecute {
                chassis.mapNotNull { level.getBlockEntity(it) as? WheelHubBlockEntity }.forEach { wheelIds.add(it.bodyId) }
                copiedWheel = chassis.mapNotNull { level.getBlockEntity(it) as? WheelHubBlockEntity }.first().saveWithFullMetadata()
                val assembly = ShipAssembler.assembleToShipFull(level, chassis)
                ship = assembly.ship
                ships.add(ship)
                hubs = chassis.map { p ->
                    val to = Vector3d(p.x + .5, p.y + .5, p.z + .5).sub(assembly.fromCenter).add(assembly.toCenter)
                    val dest = BlockPos.containing(to.x, to.y, to.z)
                    force(level, dest)
                    level.getBlockEntity(dest) as? WheelHubBlockEntity
                }.filterNotNull()
            }.thenIdle(50).thenExecute {
                helper.assertTrue(hubs.size == 4 && hubs.map { it.bodyId }.toSet() == wheelIds,
                    "Assembly lost or duplicated wheels: before=$wheelIds, after=${hubs.map { it.bodyId }}")
                helper.assertTrue(level.shipObjectWorld.loadedShips.contains(ship.id), "Wheel chassis was not loaded")
                val joints = level.gtpa.getAllJoints().values.filterIsInstance<VSRevoluteJoint>().filter { it.shipId1 in wheelIds }
                helper.assertTrue(joints.size == 4 && joints.all { it.shipId0 == ship.id }, "Wheels were not reattached to chassis: $joints")
                helper.assertTrue(ship.velocity.length() < 1.0 && ship.transform.positionInWorld.y() > center.y - 1.0,
                    "Tires did not support the chassis")
                helper.assertTrue(supportedWheels.containsAll(wheelIds), "Not all wheels developed a loaded tire contact")
                firmHeight = ship.transform.positionInWorld.y()
                hubs.forEach { it.setTireSetting(AirPumpSetting.PRESSURE, 80) }
            }.thenIdle(40).thenExecute {
                helper.assertTrue(ship.transform.positionInWorld.y() < firmHeight - .001,
                    "Lower pressure did not increase loaded tire compression: before=$firmHeight, after=${ship.transform.positionInWorld.y()}")
                helper.assertTrue(hubs.map { it.bodyId }.toSet() == wheelIds, "Pressure adjustment replaced loaded wheels")
                val clonePos = center.east(7)
                blocks.add(clonePos)
                level.setBlockAndUpdate(clonePos, ClockworkBlocks.WHEEL_HUB.defaultState.setValue(FACING, Direction.EAST))
                clone = level.getBlockEntity(clonePos) as WheelHubBlockEntity
                clone.load(copiedWheel)
                for (hub in hubs) {
                    val p = hub.blockPos.relative(hub.facing.opposite)
                    blocks.add(p)
                    level.setBlockAndUpdate(p, AllBlocks.CREATIVE_MOTOR.defaultState.setValue(FACING, hub.facing))
                    (level.getBlockEntity(p) as CreativeMotorBlockEntity).generatedSpeed.setValue(32 * hub.facing.axisDirection.step)
                }
                initialZ = ship.transform.positionInWorld.z()
            }.thenIdle(60).thenExecute {
                helper.assertTrue(abs(ship.transform.positionInWorld.z() - initialZ) > 1.0,
                    "Driven native tires did not move the chassis: position=${ship.kinematics.position}, velocity=${ship.velocity}")
                helper.assertTrue(ship.velocity.length() < 20.0, "Wheel chassis became unstable")
                helper.assertTrue(clone.bodyId >= 0 && clone.bodyId !in wheelIds, "Copied hub stole a moved wheel's body")
                hubs.forEach { it.uninstall(player) }
            }.thenSucceed()
        }

        @JvmStatic @GameTest(template = "wheel", batch = BATCH, timeoutTicks = 260)
        fun largeWheelsClimbSlab(helper: GameTestHelper) = slabClimb(helper, 32, false)

        @JvmStatic @GameTest(template = "wheel", batch = BATCH, timeoutTicks = 260)
        fun largeWheelsClimbSlabAtHighRpm(helper: GameTestHelper) = slabClimb(helper, 256, false)

        @JvmStatic @GameTest(template = "wheel", batch = BATCH, timeoutTicks = 260)
        fun largeWheelsClimbOffsetSlab(helper: GameTestHelper) = slabClimb(helper, 32, true)

        private fun slabClimb(helper: GameTestHelper, rpm: Int, offset: Boolean) {
            val level = helper.level
            val start = helper.absolutePos(BlockPos(3, 0, 180 + rpm + if (offset) 80 else 0))
                .let { BlockPos(it.x, 220, it.z) }
            var markedGround = false
            val player = observe(level, start) { packet ->
                markedGround = markedGround || packet.marks.any { it.surface == -1L &&
                    it.center.distanceSquared(Vector3d(start.x.toDouble(), start.y.toDouble(), start.z.toDouble())) < 400 }
            }
            for (x in -7..7) for (z in -10..35) {
                val floor = start.offset(x, -2, z)
                force(level, floor)
                blocks.add(floor)
                level.setBlockAndUpdate(floor, Blocks.STONE.defaultBlockState())
                if (z >= 5 + if (offset && x < 0) 1 else 0) {
                    blocks.add(floor.above())
                    level.setBlockAndUpdate(floor.above(), Blocks.OAK_SLAB.defaultBlockState())
                }
            }
            val chassis = mutableSetOf<BlockPos>()
            for (z in -2..2) {
                val p = start.offset(0, 1, z)
                chassis.add(p)
                level.setBlockAndUpdate(p, Blocks.OAK_LOG.defaultBlockState())
            }
            val hubPositions = mutableListOf<BlockPos>()
            for (z in listOf(-2, 2)) for (side in listOf(Direction.EAST, Direction.WEST)) {
                val motor = start.offset(side.stepX, 0, z)
                chassis.add(motor)
                level.setBlockAndUpdate(motor, AllBlocks.CREATIVE_MOTOR.defaultState.setValue(FACING, side))
                (level.getBlockEntity(motor) as CreativeMotorBlockEntity).generatedSpeed.setValue(0)
                chassis.add(motor.above())
                level.setBlockAndUpdate(motor.above(), Blocks.OAK_LOG.defaultBlockState())
                val p = motor.relative(side)
                chassis.add(p)
                hubPositions.add(p)
                level.setBlockAndUpdate(p, ClockworkBlocks.WHEEL_HUB.defaultState.setValue(FACING, side))
                val hub = level.getBlockEntity(p) as WheelHubBlockEntity
                helper.assertTrue(hub.install(WheelSize.LARGE, player, type = TireType.RUBBER), "Could not install test tire")
                hub.torque.setValue(64)
            }
            lateinit var ship: ServerShip
            lateinit var hubs: List<WheelHubBlockEntity>
            var initial = Vector3d()
            var climbed = false
            helper.startSequence().thenIdle(8).thenExecute {
                val assembly = ShipAssembler.assembleToShipFull(level, chassis)
                ship = assembly.ship
                ships.add(ship)
                hubs = hubPositions.map { p ->
                    val dest = Vector3d(p.x + .5, p.y + .5, p.z + .5).sub(assembly.fromCenter).add(assembly.toCenter)
                    val local = BlockPos.containing(dest.x, dest.y, dest.z)
                    force(level, local)
                    level.getBlockEntity(local) as WheelHubBlockEntity
                }
            }.thenIdle(30).thenExecute {
                initial = Vector3d(ship.transform.positionInWorld)
                hubs.forEach { hub ->
                    (level.getBlockEntity(hub.blockPos.relative(hub.facing.opposite)) as CreativeMotorBlockEntity)
                        .generatedSpeed.setValue(rpm * hub.facing.axisDirection.step)
                }
            }.thenExecuteFor(160) {
                val travel = Vector3d(ship.transform.positionInWorld).sub(initial)
                if (!climbed && travel.z > 8 && travel.y > .25) {
                    climbed = true
                    hubs.forEach { hub ->
                        (level.getBlockEntity(hub.blockPos.relative(hub.facing.opposite)) as CreativeMotorBlockEntity)
                            .generatedSpeed.setValue(0)
                    }
                }
            }.thenExecute {
                val travel = Vector3d(ship.transform.positionInWorld).sub(initial)
                helper.assertTrue(climbed, "Failed slab climb: rpm=$rpm offset=$offset travel=$travel")
                helper.assertTrue(abs(travel.x) < 2 && ship.velocity.length() < 30, "Slab impact destabilized chassis: $travel")
                if (rpm == 256) helper.assertTrue(markedGround, "Sliding tires did not send ground skidmarks")
                hubs.forEach { it.uninstall(player) }
            }.thenSucceed()
        }

        @JvmStatic @GameTest(template = "wheel", batch = BATCH, timeoutTicks = 140)
        fun linkedHubThrottle(helper: GameTestHelper) {
            val level = helper.level
            val pos = helper.absolutePos(BlockPos(2, 0, 2)).let { BlockPos(it.x, 245, it.z) }
            val wheelPos = pos.south(2)
            val brakePos = pos.east(2)
            var flashes = 0
            val player = observe(level, pos, onBrake = { if (it == pos) flashes++ })
            player.setPos(pos.x + 1.5, pos.y.toDouble(), pos.z + 1.5)
            for (p in listOf(pos, pos.west(), wheelPos, wheelPos.below(), brakePos, brakePos.below())) {
                force(level, p)
                blocks.add(p)
            }
            level.setBlockAndUpdate(pos, ClockworkBlocks.WHEEL_HUB.defaultState.setValue(FACING, Direction.EAST))
            level.setBlockAndUpdate(wheelPos.below(), Blocks.STONE.defaultBlockState())
            val stack = ItemStack(ClockworkBlocks.STEERING_WHEEL.get())
            player.setItemInHand(InteractionHand.MAIN_HAND, stack)
            fun click(p: BlockPos) = player.gameMode.useItemOn(player, level, player.mainHandItem, InteractionHand.MAIN_HAND,
                BlockHitResult(Vec3.atCenterOf(p), Direction.UP, p, false))
            click(pos)
            click(pos)
            helper.assertTrue(stack.tag?.contains("SelectedHubs") != true, "Selected hub did not toggle off")
            click(pos)
            click(wheelPos.below())
            level.setBlockAndUpdate(brakePos.below(), Blocks.STONE.defaultBlockState())
            player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack(ClockworkBlocks.HANDBRAKE.get()))
            click(pos)
            click(wheelPos)
            click(brakePos.below())
            player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY)
            val steering = level.getBlockEntity(wheelPos) as SteeringWheelBlockEntity
            val hub = level.getBlockEntity(pos) as WheelHubBlockEntity
            hub.install(WheelSize.MEDIUM, player, type = TireType.RUBBER)
            level.setBlockAndUpdate(pos.west(), AllBlocks.CREATIVE_MOTOR.defaultState.setValue(FACING, Direction.EAST))
            (level.getBlockEntity(pos.west()) as CreativeMotorBlockEntity).generatedSpeed.setValue(64)
            fun axle() = level.gtpa.getAllJoints().values.filterIsInstance<VSRevoluteJoint>().single { it.shipId1 == hub.bodyId }
            var sawBrake = false
            var movedWheel = wheelPos
            helper.startSequence().thenIdle(8).thenExecute {
                helper.assertTrue(hub.speed == 64f && axle().driveVelocity == null, "Linked hub drove at neutral")
                steering.use(player)
                steering.control(player, 0, 1)
            }.thenIdle(5).thenExecute {
                helper.assertTrue(axle().driveForceLimit!! in 1f..7999f && axle().driveVelocity!!.velocity > 0f,
                    "Forward input did not ramp axle torque")
            }.thenIdle(9).thenExecute {
                helper.assertTrue(axle().driveForceLimit == 8000f && axle().driveFreeSpin == true, "Lost full throttle or overrun")
                steering.control(player, 0, -1)
            }.thenExecuteFor(20) {
                steering.control(player, 0, -1)
                if (axle().driveVelocity?.velocity == 0f) sawBrake = true
            }.thenExecute {
                helper.assertTrue(sawBrake && axle().driveVelocity!!.velocity < 0f && hub.speed == 64f,
                    "Reverse did not brake the rotating wheel before reversing on the same Create network")
                steering.control(player, 0, 0)
            }.thenIdle(8).thenExecute {
                helper.assertTrue(axle().driveVelocity == null, "Releasing throttle did not disengage the drive")
                hub.toggleDrive(player)
                steering.control(player, 0, 1, true)
            }.thenIdle(6).thenExecute {
                helper.assertTrue(level.getBlockState(brakePos).getValue(HandbrakeBlock.PULLED) &&
                    axle().driveVelocity?.velocity == 0f && axle().driveFreeSpin == false && flashes == 1,
                    "Driver handbrake did not lock the freewheeling axle and flash once")
                click(brakePos)
                steering.control(player, 2, 0)
            }.thenIdle(5).thenExecute {
                helper.assertTrue(axle().driveVelocity?.velocity == 0f && flashes == 1, "Dismount released the parking brake or repeated its flash")
                click(brakePos)
            }.thenIdle(4).thenExecute {
                helper.assertTrue(axle().driveVelocity == null && flashes == 1, "Releasing the parking brake failed")
                hub.toggleDrive(player)
                val assembly = ShipAssembler.assembleToShipFull(level, mutableSetOf(wheelPos))
                ships.add(assembly.ship)
                val moved = Vector3d(wheelPos.x + .5, wheelPos.y + .5, wheelPos.z + .5).sub(assembly.fromCenter).add(assembly.toCenter)
                movedWheel = BlockPos.containing(moved.x, moved.y, moved.z)
            }.thenIdle(8).thenExecute {
                helper.assertTrue(level.getBlockEntity(movedWheel) is SteeringWheelBlockEntity && axle().driveVelocity == null,
                    "Moving just the controller during assembly lost its link")
                level.removeBlock(movedWheel, false)
            }.thenIdle(5).thenExecute {
                helper.assertTrue(axle().driveVelocity!!.velocity > 0f && axle().driveForceLimit == 8000f,
                    "Removing the controller did not restore ordinary shaft drive")
            }.thenSucceed()
        }

        @JvmStatic @GameTest(template = "wheel", batch = BATCH, timeoutTicks = 180)
        fun handbrakeHoldsBentHinge(helper: GameTestHelper) {
            val level = helper.level
            val pos = helper.absolutePos(BlockPos(3, 0, 3)).let { BlockPos(it.x, 255, it.z) }
            val brake = pos.east(2)
            val player = observe(level, pos)
            player.setPos(pos.x + .5, pos.y.toDouble(), pos.z + 2.5)
            for (p in listOf(pos, pos.above(), brake, brake.below())) {
                blocks.add(p)
                force(level, p)
            }
            level.setBlockAndUpdate(pos, ClockworkBlocks.HINGE.defaultState.setValue(HingeBlock.FACING, Direction.UP))
            level.setBlockAndUpdate(pos.above(), Blocks.BARREL.defaultBlockState())
            level.setBlockAndUpdate(brake.below(), Blocks.STONE.defaultBlockState())
            player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack(ClockworkBlocks.HANDBRAKE.get()))
            fun click(p: BlockPos) = player.gameMode.useItemOn(player, level, player.mainHandItem, InteractionHand.MAIN_HAND,
                BlockHitResult(Vec3.atCenterOf(p), Direction.UP, p, false))
            click(pos)
            click(brake.below())
            player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY)
            helper.assertTrue(HingeAssembly.assemble(level, pos) == null, "Could not assemble brake test hinge")
            val link = HingeLinks.get(level).at(pos)!!
            ships.add(level.shipObjectWorld.allShips.getById(link.b.shipId)!!)
            force(level, link.b.pos)
            fun angle() = HingeGeometry.angle(HingeLinks.worldRotation(level, link.a), HingeLinks.worldRotation(level, link.b))
            val torque = AtomicInteger(5000)
            var heldAngle = 0.0
            helper.startSequence().thenIdle(20).thenExecute {
                var remaining = 450
                ClockworkMod.physTickOnce(level.dimensionId) { physics, _, again ->
                    if (--remaining > 0 && torque.get() != 0) {
                        physics.getBodyById(link.b.shipId)?.applyWorldTorque(Vector3d(torque.get().toDouble(), 0.0, 0.0))
                        again()
                    }
                }
            }.thenWaitUntil { helper.assertTrue(angle() > 25, "Hinge did not rotate before braking") }
                .thenExecute { heldAngle = angle(); click(brake); torque.set(-5000) }
                .thenIdle(15).thenExecute {
                    helper.assertTrue(link.connected && abs(angle() - heldAngle) < 8, "Brake lost its engagement angle: $heldAngle to ${angle()}")
                    click(brake)
                }.thenWaitUntil { helper.assertTrue(angle() < heldAngle - 15, "Released hinge did not rotate again") }
                .thenExecute { torque.set(0) }.thenSucceed()
        }

        private fun force(level: ServerLevel, pos: BlockPos) {
            val chunk = ChunkPos(pos)
            if (chunks.add(chunk)) level.setChunkForced(chunk.x, chunk.z, true)
        }

        private fun observe(level: ServerLevel, pos: BlockPos, onBrake: (BlockPos) -> Unit = {}, contacts: (WheelContactPacket) -> Unit = {}): ServerPlayer {
            val connection = object : Connection(PacketFlow.SERVERBOUND) {
                override fun send(packet: Packet<*>, listener: PacketSendListener?) {
                    if (packet is ClientboundCustomPayloadPacket && packet.identifier == ClockworkMod.NETWORK_CHANNEL) {
                        val data = FriendlyByteBuf(packet.data.duplicate())
                        when (data.readVarInt()) {
                            ClockworkPackets.WHEEL_CONTACTS.ordinal -> contacts(WheelContactPacket(data))
                            ClockworkPackets.HANDBRAKE_EFFECT.ordinal -> onBrake(data.readBlockPos())
                        }
                    }
                    listener?.onSuccess()
                }
            }
            val channel = EmbeddedChannel()
            channel.pipeline().addLast("packet_handler", connection)
            channel.pipeline().fireChannelActive()
            NetworkHooks.registerServerLoginChannel(connection, ClientIntentionPacket("localhost", 0, ConnectionProtocol.LOGIN))
            val player = ServerPlayer(level.server, level, GameProfile(UUID.randomUUID(), "wheel-test"))
            level.server.playerList.placeNewPlayer(connection, player)
            player.setPos(pos.x.toDouble(), pos.y + 8.0, pos.z.toDouble())
            player.setNoGravity(true)
            player.abilities.flying = true
            player.abilities.instabuild = true
            observers.add(player to channel)
            return player
        }
    }
}
