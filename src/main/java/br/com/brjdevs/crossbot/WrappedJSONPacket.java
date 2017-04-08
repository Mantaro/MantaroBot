package br.com.brjdevs.crossbot;

import br.com.brjdevs.network.JSONPacket;
import br.com.brjdevs.network.factory.KryoPacketFactory;
import br.com.brjdevs.network.factory.PacketFactory;

public class WrappedJSONPacket extends JSONPacket {
    public static final PacketFactory<WrappedJSONPacket> FACTORY = new KryoPacketFactory<>(WrappedJSONPacket.class, 7);

    public final long requestId;
    public final boolean isResponse;

    public WrappedJSONPacket(String json, long requestId, boolean isResponse) {
        super(json);
        this.requestId = requestId;
        this.isResponse = isResponse;
    }
}
