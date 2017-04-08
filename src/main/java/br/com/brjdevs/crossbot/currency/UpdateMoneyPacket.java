package br.com.brjdevs.crossbot.currency;

import br.com.brjdevs.network.factory.KryoPacketFactory;
import br.com.brjdevs.network.factory.PacketFactory;

public class UpdateMoneyPacket extends AbstractMoneyPacket {
	public static final PacketFactory<UpdateMoneyPacket> FACTORY = new KryoPacketFactory<>(UpdateMoneyPacket.class, 6);

	public final long delta;

	public UpdateMoneyPacket(long userid, long delta, long requestId) {
		super(userid, requestId);
		this.delta = delta;
	}
}
