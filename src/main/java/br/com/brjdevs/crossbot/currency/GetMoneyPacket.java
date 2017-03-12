package br.com.brjdevs.crossbot.currency;

import br.com.brjdevs.network.factory.KryoPacketFactory;
import br.com.brjdevs.network.factory.PacketFactory;

public class GetMoneyPacket extends AbstractMoneyPacket {
    public static final PacketFactory<GetMoneyPacket> FACTORY = new KryoPacketFactory<>(GetMoneyPacket.class, 3);

    public GetMoneyPacket(long userid) {
        super(userid);
    }

    public static class Response extends AbstractMoneyPacket{
        public static final PacketFactory<Response> FACTORY = new KryoPacketFactory<>(Response.class, 4);

        public final long money;

        public Response(long userid, long money) {
            super(userid);
            this.money = money;
        }
    }
}
