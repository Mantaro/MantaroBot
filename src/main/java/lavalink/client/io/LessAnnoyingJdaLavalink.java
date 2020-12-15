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
