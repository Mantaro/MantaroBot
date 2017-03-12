package net.kodehawa.mantarobot.utils.data;

import br.com.brjdevs.crossbot.currency.GetMoneyPacket;
import br.com.brjdevs.crossbot.currency.IdentifyPacket;
import br.com.brjdevs.crossbot.currency.SetMoneyPacket;
import br.com.brjdevs.crossbot.currency.UpdateMoneyPacket;
import br.com.brjdevs.network.*;
import net.kodehawa.mantarobot.utils.UnsafeUtils;
import org.java_websocket.WebSocket;
import org.json.JSONObject;

import java.io.Closeable;
import java.net.ConnectException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.BiConsumer;
import java.util.function.Function;

public class CrossBotDataManager implements Closeable{
    private final List<SocketListener> listeners = new CopyOnWriteArrayList<>();
    private final Map<String, Integer> bots = new ConcurrentHashMap<>();
    private final Connection connection;
    private volatile Object lastServerPacket;
    private volatile int lastSenderId;

    private CrossBotDataManager(String host, int port, String name, Function<Long, Long> getmoney, BiConsumer<Long, Long> setmoney, List<SocketListener> listenersToAdd) throws URISyntaxException {
        if(getmoney == null) throw new NullPointerException("getmoney");
        if(setmoney == null) throw new NullPointerException("setmoney");
        if(host == null) throw new NullPointerException("host");
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
                if(o instanceof GetMoneyPacket) {
                    long id = ((GetMoneyPacket) o).userid;
                    try {
                        return new GetMoneyPacket.Response(id, getmoney.apply(id));
                    } catch(Throwable t) {
                        return new GetMoneyPacket.Response(id, -1);
                    }
                } else if(o instanceof SetMoneyPacket) {
                    SetMoneyPacket smp = (SetMoneyPacket)o;
                    setmoney.accept(smp.userid, smp.money);
                    return null;
                } else if(o instanceof UpdateMoneyPacket) {
                    UpdateMoneyPacket ump = (UpdateMoneyPacket)o;
                    setmoney.accept(ump.userid, getmoney.apply(ump.userid) + ump.delta);
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
                bots.entrySet().removeIf((entry)->entry.getValue() == id);
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
        client.getPacketClient().sendPacket(new IdentifyPacket(name));
    }

    private CrossBotDataManager(int port, Function<Long, Long> getmoney, BiConsumer<Long, Long> setmoney, List<SocketListener> listenersToAdd) {
        if(getmoney == null) throw new NullPointerException("getmoney");
        if(setmoney == null) throw new NullPointerException("setmoney");
        if(port == 0) throw new IllegalArgumentException("port == 0");
        this.listeners.addAll(listenersToAdd);
        PacketRegistry pr = new PacketRegistry();
        registerPackets(pr);
        Server server;
        connection = server = new Server(new InetSocketAddress(port), pr, new SocketListenerAdapter() {
            @Override
            public Object onPacket(Connection connection, int i, Object o) {
                if(o instanceof GetMoneyPacket) {
                    long id = ((GetMoneyPacket) o).userid;
                    try {
                        return new GetMoneyPacket.Response(id, getmoney.apply(id));
                    } catch(Throwable t) {
                        return new GetMoneyPacket.Response(id, -1);
                    }
                } else if(o instanceof SetMoneyPacket) {
                    SetMoneyPacket smp = (SetMoneyPacket)o;
                    setmoney.accept(smp.userid, smp.money);
                } else if(o instanceof UpdateMoneyPacket) {
                    UpdateMoneyPacket ump = (UpdateMoneyPacket)o;
                    setmoney.accept(ump.userid, getmoney.apply(ump.userid) + ump.delta);
                    return null;
                } else if(o instanceof IdentifyPacket) {
                    bots.put(((IdentifyPacket) o).name, i);
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
        });
        server.start();
    }

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

    public long getMoney(long userid, String bot) {
        if(connection instanceof Client) {
            AbstractClient client = ((Client)connection).getPacketClient();
            client.sendPacket(new GetMoneyPacket(userid));
            GetMoneyPacket.Response resp;
            do {
                resp = client.readPacketBlocking(GetMoneyPacket.Response.class);
            } while(resp.userid != userid);
            return resp.money;
        }
        else {
            if(bot == null)
                throw new IllegalArgumentException("No bot specified");
            Integer id = bots.get(bot);
            if(id == null)
                throw new IllegalArgumentException("No bot identified as " + bot + " connected");
            Server server = (Server)connection;
            server.sendPacket(server.getSocket(id), new GetMoneyPacket(userid));
            GetMoneyPacket.Response resp = null;
            do {
                Object l = lastServerPacket;
                if(lastSenderId == id && l instanceof GetMoneyPacket.Response) {
                    resp = (GetMoneyPacket.Response)l;
                }
            } while(resp == null);
            return resp.money;
        }
    }

    public void setMoney(long userid, long money, String bot) {
        if(connection instanceof Client) {
            AbstractClient client = ((Client)connection).getPacketClient();
            client.sendPacket(new SetMoneyPacket(userid, money));
        }
        else {
            if(bot == null)
                throw new IllegalArgumentException("No bot specified");
            Integer id = bots.get(bot);
            if(id == null)
                throw new IllegalArgumentException("No bot identified as " + bot + " connected");
            Server server = (Server)connection;
            server.sendPacket(server.getSocket(id), new SetMoneyPacket(userid, money));
        }
    }

    public void updateMoney(long userid, long delta, String bot) {
        if(connection instanceof Client) {
            AbstractClient client = ((Client)connection).getPacketClient();
            client.sendPacket(new UpdateMoneyPacket(userid, delta));
        }
        else {
            if(bot == null)
                throw new IllegalArgumentException("No bot specified");
            Integer id = bots.get(bot);
            if(id == null)
                throw new IllegalArgumentException("No bot identified as " + bot + " connected");
            Server server = (Server)connection;
            server.sendPacket(server.getSocket(id), new UpdateMoneyPacket(userid, delta));
        }
    }

    public void sendJSON(JSONObject object, String bot) {
        if(connection instanceof Client) {
            AbstractClient client = ((Client)connection).getPacketClient();
            client.sendPacket(new JSONPacket(object.toString()));
        }
        else {
            if(bot == null)
                throw new IllegalArgumentException("No bot specified");
            Integer id = bots.get(bot);
            if(id == null)
                throw new IllegalArgumentException("No bot identified as " + bot + " connected");
            Server server = (Server)connection;
            server.sendPacket(server.getSocket(id), new JSONPacket(object.toString()));
        }
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
            CLIENT, SERVER;
        }
        private final Type type;
        private final List<SocketListener> listeners = new ArrayList<>();
        private String host;
        private int port;
        private String name;
        private Function<Long, Long> getmoney;
        private BiConsumer<Long, Long> setmoney;

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

        public Builder getMoney(Function<Long, Long> func) {
            this.getmoney = func;
            return this;
        }

        public Builder setMoney(BiConsumer<Long, Long> func) {
            this.setmoney = func;
            return this;
        }

        public Builder addListeners(SocketListener... listeners) {
            this.listeners.addAll(Arrays.asList(listeners));
            return this;
        }

        public CrossBotDataManager build() {
            switch(type) {
                case CLIENT:
                    try {
                        return new CrossBotDataManager(host, port, name, getmoney, setmoney, listeners);
                    } catch(URISyntaxException e) {
                        UnsafeUtils.throwException(e);
                    }
                case SERVER:
                    return new CrossBotDataManager(port, getmoney, setmoney, listeners);
                default:
                    throw new AssertionError();
            }
        }
    }
}