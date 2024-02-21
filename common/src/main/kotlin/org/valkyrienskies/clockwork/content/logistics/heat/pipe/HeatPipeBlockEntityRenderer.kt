package org.valkyrienskies.clockwork.content.logistics.heat.pipe

import com.jozufozu.flywheel.core.PartialModel
import com.mojang.blaze3d.vertex.PoseStack
import com.mojang.math.Vector4f
import com.simibubi.create.foundation.blockEntity.renderer.SmartBlockEntityRenderer
import com.simibubi.create.foundation.render.CachedBufferer
import com.simibubi.create.foundation.render.SuperByteBuffer
import com.simibubi.create.foundation.utility.AnimationTickHolder
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.client.renderer.RenderType
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider
import net.minecraft.core.Direction
import org.valkyrienskies.clockwork.ClockworkMod
import org.valkyrienskies.clockwork.ClockworkPartials
import org.valkyrienskies.clockwork.ClockworkRenderTypes
import org.valkyrienskies.clockwork.ClockworkShaders
import org.valkyrienskies.clockwork.util.render.RenderUtil

class HeatPipeBlockEntityRenderer