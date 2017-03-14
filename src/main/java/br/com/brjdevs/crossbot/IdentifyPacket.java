package br.com.brjdevs.crossbot;

import br.com.brjdevs.network.factory.KryoPacketFactory;
import br.com.brjdevs.network.factory.PacketFactory;

public class IdentifyPacket {
    public static final PacketFactory<IdentifyPacket> FACTORY = new KryoPacketFactory<>(IdentifyPacket.class, 2);

    public final String name;
    public final String password;

    public IdentifyPacket(String name, String password) {
        this.name = name;
        this.password = password;
    }
}
