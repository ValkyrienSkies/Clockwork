package org.valkyrienskies.clockwork.util

import net.minecraft.server.level.ServerLevel
import org.valkyrienskies.core.apigame.joints.VSJoint
import org.valkyrienskies.core.apigame.joints.VSJointAndId
import org.valkyrienskies.mod.common.ValkyrienSkiesMod
import org.valkyrienskies.mod.common.dimensionId
import org.valkyrienskies.mod.common.util.GameTickForceApplier

val ServerLevel.gtfa: GameTickForceApplier get() = ValkyrienSkiesMod.getOrCreateGTFA(this.dimensionId)
fun GameTickForceApplier.updateJoint(id: Int, joint: VSJoint) { this.updateJoint(VSJointAndId(id, joint)) }