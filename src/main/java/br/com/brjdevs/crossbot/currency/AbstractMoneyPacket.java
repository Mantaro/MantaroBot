package br.com.brjdevs.crossbot.currency;

public abstract class AbstractMoneyPacket {
    public final long userid;

    AbstractMoneyPacket(long userid) {
        this.userid = userid;
    }
}
