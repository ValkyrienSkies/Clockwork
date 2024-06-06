package org.valkyrienskies.clockwork.integration.cc

import dan200.computercraft.api.peripheral.IComputerAccess

class ComputerAttachmentHandler {
    private val accessList: MutableList<IComputerAccess> = mutableListOf()

    fun attachComputer(access: IComputerAccess) {
        this.accessList.add(access)
    }

    fun detachComputer(access: IComputerAccess) {
        this.accessList.remove(access)
    }

    fun sendEvent(name: String, vararg obj: Any?) {
        this.accessList.forEach {
            it.queueEvent(name, obj)
        }
    }
}