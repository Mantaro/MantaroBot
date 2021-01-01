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

import lavalink.client.io.jda.JdaLavalink;
import net.dv8tion.jda.api.JDA;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.java_websocket.drafts.Draft_6455;

import java.lang.reflect.Field;
import java.net.URI;
import java.util.HashMap;
import java.util.function.Function;

public class LessAnnoyingJdaLavalink extends JdaLavalink {
    private static final Field USER_ID_FIELD;

    static {
        try {
            USER_ID_FIELD = Lavalink.class.getDeclaredField("userId");
            USER_ID_FIELD.setAccessible(true);
        } catch (Exception e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    public LessAnnoyingJdaLavalink(String userId, int numShards, Function<Integer, JDA> jdaProvider) {
        super(userId, numShards, jdaProvider);
    }

    @Override
    public void addNode(@NonNull String name, @NonNull URI serverUri, @NonNull String password) {
        String userId;
        try {
            userId = (String)USER_ID_FIELD.get(this);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        if (userId == null) {
            throw new IllegalStateException("We need a userId to connect to Lavalink");
        }

        HashMap<String, String> headers = new HashMap<>();
        headers.put("Authorization", password);
        headers.put("Num-Shards", Integer.toString(numShards));
        headers.put("User-Id", userId);
        headers.put("Client-Name", "Lavalink-Client");

        var socket = new LessAnnoyingLavalinkSocket(name, this, serverUri, new Draft_6455(), headers);
        socket.connect();

        ((Lavalink<?>)this).nodes.add(socket);
    }
}
