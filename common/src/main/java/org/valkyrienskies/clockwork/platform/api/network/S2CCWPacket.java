package org.valkyrienskies.clockwork.platform.api.network;

public interface S2CCWPacket extends CWPacket {

    void handle(ClientNetworkContext context);

}
