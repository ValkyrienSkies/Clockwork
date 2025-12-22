package org.valkyrienskies.clockwork.util.builder

import com.simibubi.create.api.registrate.CreateRegistrateRegistrationCallback
import com.simibubi.create.foundation.data.CreateBlockEntityBuilder
import com.simibubi.create.foundation.data.CreateRegistrate
import com.tterrag.registrate.builders.BlockEntityBuilder
import com.tterrag.registrate.builders.BuilderCallback
import com.tterrag.registrate.util.nullness.NonNullFunction
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.entity.BlockEntityType
import org.valkyrienskies.clockwork.ClockworkMod

class ClockworkExpandedCreateRegistrate(modid: String = ClockworkMod.MOD_ID) : CreateRegistrate(modid) {
    override fun <T : BlockEntity, P> blockEntity(
        parent: P, name: String,
        factory: BlockEntityBuilder.BlockEntityFactory<T>
    ): CreateBlockEntityBuilder<T, P> {
        return entry<BlockEntityType<*>, BlockEntityType<T>, P, BlockEntityBuilder<T, P>>(
            name,
            NonNullFunction { callback: BuilderCallback ->
                ClockworkBlockEntityBuilder.create<T, P>(
                    this,
                    parent,
                    name,
                    callback,
                    factory
                )
            }) as ClockworkBlockEntityBuilder<T, P>
    }

    companion object {
        fun create(modid: String): CreateRegistrate {
            val registrate = ClockworkExpandedCreateRegistrate(modid)
            // The registrate is registered here instead of in the constructor so that if a subclass
            // overrides the addRegisterCallback to be dependent on some sort of state initialized in the constructor,
            // it won't explode. The consequence is that subclasses must manually provide their registrate to the callback API
            CreateRegistrateRegistrationCallback.provideRegistrate(registrate)
            return registrate
        }
    }
}