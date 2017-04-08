package br.com.brjdevs.crossbot.currency;

import br.com.brjdevs.network.factory.KryoPacketFactory;
import br.com.brjdevs.network.factory.PacketFactory;

public class GetMoneyPacket extends AbstractMoneyPacket {
	public static class Response extends AbstractMoneyPacket {
		public static final PacketFactory<Response> FACTORY = new KryoPacketFactory<>(Response.class, 4);

		public final long money;

		public Response(long userid, long money, long requestId) {
			super(userid, requestId);
			this.money = money;
		}
	}

	public static final PacketFactory<GetMoneyPacket> FACTORY = new KryoPacketFactory<>(GetMoneyPacket.class, 3);

	public GetMoneyPacket(long userid, long requestId) {
		super(userid, requestId);
	}
}
