package lavalink.client.io;

import org.java_websocket.drafts.Draft;
import org.json.JSONObject;

import java.net.URI;
import java.util.Map;
import java.util.Set;

public class LessAnnoyingLavalinkSocket extends LavalinkSocket {
    private static final Set<String> KNOWN_EVENTS = Set.of(
            "TrackEndEvent",
            "TrackExceptionEvent",
            "TrackStuckEvent",
            "WebSocketClosedEvent"
    );

    LessAnnoyingLavalinkSocket(String name, Lavalink<?> lavalink, URI serverUri,
                               Draft protocolDraft, Map<String, String> headers) {
        super(name, lavalink, serverUri, protocolDraft, headers);
    }

    @Override
    public void onMessage(String message) {
        var json = new JSONObject(message);

        if ("event".equals(json.optString("op"))) {
            var name = json.optString("type");

            if (!KNOWN_EVENTS.contains(name)) {
                return;
            }
        }

        super.onMessage(message);
    }
}
