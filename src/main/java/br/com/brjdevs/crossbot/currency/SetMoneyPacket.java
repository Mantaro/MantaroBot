package br.com.brjdevs.crossbot.currency;

import br.com.brjdevs.network.factory.KryoPacketFactory;
import br.com.brjdevs.network.factory.PacketFactory;

public class SetMoneyPacket extends AbstractMoneyPacket {
    public static final PacketFactory<SetMoneyPacket> FACTORY = new KryoPacketFactory<>(SetMoneyPacket.class, 5);

    public final long money;

    public SetMoneyPacket(long userid, long money) {
        super(userid);
        this.money = money;
    }
}
