package org.valkyrienskies.clockwork.platform.api.network;

public interface C2SCWPacket extends CWPacket {

    void handle(ServerNetworkContext context);

}
