package br.com.brjdevs.crossbot.currency;

abstract class AbstractMoneyPacket {
	public final long requestId;
	public final long userid;

	AbstractMoneyPacket(long userid, long requestId) {
		this.userid = userid;
		this.requestId = requestId;
	}
}
