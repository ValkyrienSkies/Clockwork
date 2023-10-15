package org.valkyrienskies.clockwork.integration.cc

import dan200.computercraft.api.peripheral.IComputerAccess
import java.util.function.Consumer

class ComputerAttachmentHandler {
    private val accessList: MutableList<IComputerAccess> = ArrayList()
    fun attachComputer(access: IComputerAccess) {
        accessList.add(access)
    }

    fun detachComputer(access: IComputerAccess) {
        accessList.remove(access)
    }

    fun sendEvent(name: String?, obj: Any?) {
        accessList.forEach(
            Consumer { access: IComputerAccess ->
                access.queueEvent(
                    name!!, obj
                )
            }
        )
    }
}
