package net.kodehawa.mantarobot.utils.data;

import br.com.brjdevs.crossbot.currency.GetMoneyPacket;
import br.com.brjdevs.crossbot.IdentifyPacket;
import br.com.brjdevs.crossbot.currency.SetMoneyPacket;
import br.com.brjdevs.crossbot.currency.UpdateMoneyPacket;
import br.com.brjdevs.network.*;
import net.kodehawa.mantarobot.utils.UnsafeUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.java_websocket.WebSocket;

import java.io.Closeable;
import java.net.ConnectException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.function.BiConsumer;
import java.util.function.Function;

public class CrossBotDataManager implements Closeable {
    private final List<SocketListener> listeners = new CopyOnWriteArrayList<>();
    private final Map<String, Pair<String, Integer>> bots = new ConcurrentHashMap<>();
    private final Connection connection;
    private volatile Object lastServerPacket;
    private volatile int lastSenderId;

    private CrossBotDataManager(String host, int port, String name, String password, List<SocketListener> listenersToAdd) throws URISyntaxException {
        if(host == null) throw new NullPointerException("host");
        if(name == null) throw new NullPointerException("name");
        if(port == 0) throw new IllegalArgumentException("port == 0");
        this.listeners.addAll(listenersToAdd);
        PacketRegistry pr = new PacketRegistry();
        registerPackets(pr);
        Client client;
        connection = client = new Client(new URI("wss://" + host + ":" + port), pr, new SocketListenerAdapter() {
            @Override
            public Object onPacket(Connection connection, int i, Object o) {
                lastServerPacket = o;
                lastSenderId = i;
                for(SocketListener listener : listeners) {
                    Object p = listener.onPacket(connection, i, o);
                    if(p != null) return p;
                }
                return null;
            }

            @Override
            public void onClose(Connection connection, int id, int code, String message) {
                for(SocketListener listener : listeners)
                    listener.onClose(connection, id, code, message);
            }

            @Override
            public void onConnect(Connection connection, int id, WebSocket socket) {
                for(SocketListener listener : listeners)
                    listener.onConnect(connection, id, socket);
            }

            @Override
            public void onError(Connection connection, int id, Exception ex) {
                for(SocketListener listener : listeners)
                    listener.onError(connection, id, ex);
            }
        });
        try {
            if(!client.getPacketClient().connectBlocking()) {
                UnsafeUtils.throwException(new ConnectException("Error connecting to server"));
            }
        } catch(InterruptedException e) {
            throw new InternalError("Error connecting", e);
        }
        client.getPacketClient().waitForValidation();
        client.getPacketClient().sendPacket(new IdentifyPacket(name, password));
    }

    private CrossBotDataManager(int port, String password, List<SocketListener> listenersToAdd) {
        if(port == 0) throw new IllegalArgumentException("port == 0");
        this.listeners.addAll(listenersToAdd);
        PacketRegistry pr = new PacketRegistry();
        registerPackets(pr);
        Server server;
        connection = server = new Server(new InetSocketAddress(port), pr, new SocketListenerAdapter() {
            @Override
            public Object onPacket(Connection connection, int i, Object o) {
                if(o instanceof IdentifyPacket) {
                    IdentifyPacket pkt = (IdentifyPacket)o;
                    if(password != null && !password.equals(pkt.password)) {
                        connection.getSocket(i).close(1403, "Invalid password");
                        return null;
                    }
                    if(bots.values().stream().filter((pair)->pair.getLeft().equals(pkt.name)).count() != 0) {
                        connection.getSocket(i).close(1403, "Client named " + pkt.name + " alredy connected");
                        return null;
                    }
                    bots.put(getAddress(connection.getSocket(i)), new ImmutablePair<>(pkt.name, i));
                    return null;
                }
                for(SocketListener listener : listeners) {
                    Object p = listener.onPacket(connection, i, o);
                    if(p != null) return p;
                }
                return null;
            }

            @Override
            public void onClose(Connection connection, int id, int code, String message) {
                for(SocketListener listener : listeners)
                    listener.onClose(connection, id, code, message);
            }

            @Override
            public void onConnect(Connection connection, int id, WebSocket socket) {
                for(SocketListener listener : listeners)
                    listener.onConnect(connection, id, socket);
            }

            @Override
            public void onError(Connection connection, int id, Exception ex) {
                for(SocketListener listener : listeners)
                    listener.onError(connection, id, ex);
            }

            private String getAddress(WebSocket socket) {
                InetSocketAddress addr = socket.getRemoteSocketAddress();
                if(addr == null) return socket.getLocalSocketAddress().getHostString();
                return addr.getHostString();
            }
        });
        server.start();
    }

    @Override
    public void close() {
        if(connection instanceof Client)
            connection.close(0);
        else {
            for(int i : ((Server)connection).getClients())
                connection.close(i);
        }
    }

    public CrossBotDataManager registerListener(SocketListener listener) {
        listeners.add(listener);
        return this;
    }

    public CrossBotDataManager unregisterListener(SocketListener listener) {
        listeners.remove(listener);
        return this;
    }

    private static void registerPackets(PacketRegistry pr) {
        pr.register(IdentifyPacket.FACTORY);
        pr.register(GetMoneyPacket.FACTORY);
        pr.register(GetMoneyPacket.Response.FACTORY);
        pr.register(SetMoneyPacket.FACTORY);
        pr.register(UpdateMoneyPacket.FACTORY);
    }

    public static class Builder {
        public enum Type {
            CLIENT, SERVER
        }
        private final Type type;
        private final List<SocketListener> listeners = new ArrayList<>();
        private String host;
        private String password;
        private int port;
        private String name;

        public Builder(Type type) {
            this.type = type;
        }

        public Builder host(String host) {
            if(type == Type.SERVER) throw new UnsupportedOperationException("Only client can specify host");
            this.host = host;
            return this;
        }

        public Builder name(String name) {
            if(type == Type.SERVER) throw new UnsupportedOperationException("Only client can specify host");
            this.name = name;
            return this;
        }

        public Builder port(int port) {
            this.port = port;
            return this;
        }

        public Builder addListeners(SocketListener... listeners) {
            this.listeners.addAll(Arrays.asList(listeners));
            return this;
        }

        public Builder password(String password) {
            this.password = password;
            return this;
        }

        public CrossBotDataManager build() {
            switch(type) {
                case CLIENT:
                    try {
                        return new CrossBotDataManager(host, port, name, password, listeners);
                    } catch(URISyntaxException e) {
                        UnsafeUtils.throwException(e);
                    }
                case SERVER:
                    return new CrossBotDataManager(port, password, listeners);
                default:
                    throw new AssertionError();
            }
        }
    }
}