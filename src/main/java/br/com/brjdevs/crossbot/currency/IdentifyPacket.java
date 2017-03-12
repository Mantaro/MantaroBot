package br.com.brjdevs.crossbot.currency;

import br.com.brjdevs.network.factory.KryoPacketFactory;
import br.com.brjdevs.network.factory.PacketFactory;

public class IdentifyPacket {
    public static final PacketFactory<IdentifyPacket> FACTORY = new KryoPacketFactory<>(IdentifyPacket.class, 2);

    public final String name;

    public IdentifyPacket(String name) {
        this.name = name;
    }
}
