package net.kodehawa.mantarobot.utils.data;

import br.com.brjdevs.network.Connection;
import br.com.brjdevs.network.SocketListener;
import org.java_websocket.WebSocket;

@FunctionalInterface
public interface FunctionalPacketHandler extends SocketListener {
	Object onPacket(Object packet);

	@Override
	default void onClose(Connection connection, int i, int i1, String s) {
	}

	@Override
	default void onConnect(Connection connection, int i, WebSocket webSocket) {
	}

	@Override
	default void onError(Connection connection, int i, Exception e) {
	}

	@Override
	default Object onPacket(Connection conn, int id, Object packet) {
		return onPacket(packet);
	}
}
