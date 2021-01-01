/*
 * Copyright (C) 2016-2021 David Rubio Escares / Kodehawa
 *
 *  Mantaro is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *  Mantaro is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Mantaro. If not, see http://www.gnu.org/licenses/
 */

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
