package net.kodehawa.mantarobot.utils.crossbot;

import br.com.brjdevs.crossbot.IdentifyPacket;
import br.com.brjdevs.crossbot.WrappedJSONPacket;
import br.com.brjdevs.crossbot.currency.GetMoneyPacket;
import br.com.brjdevs.crossbot.currency.SetMoneyPacket;
import br.com.brjdevs.crossbot.currency.UpdateMoneyPacket;
import br.com.brjdevs.network.*;
import gnu.trove.map.hash.TLongObjectHashMap;
import lombok.SneakyThrows;
import net.kodehawa.mantarobot.utils.data.DataManager;
import net.kodehawa.mantarobot.utils.data.FunctionalPacketHandler;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.java_websocket.WebSocket;
import org.json.JSONObject;

import java.io.Closeable;
import java.net.ConnectException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

public class CrossBotDataManager implements Closeable, DataManager<Consumer<Object>> {
	public static class Builder {
		public enum Type {
			CLIENT, SERVER
		}

		private final List<SocketListener> listeners = new ArrayList<>();
		private final Type type;
		private boolean async;
		private String host;
		private String name;
		private String password;
		private int poolSize = 10;
		private int port;
		private boolean secure;
		private int sleepTime;

		public Builder(Type type) {
			this.type = type;
		}

		public Builder addListeners(SocketListener... listeners) {
			this.listeners.addAll(Arrays.asList(listeners));
			return this;
		}

		public Builder async(boolean async, int sleepTime) {
			if (sleepTime < 0) throw new IllegalArgumentException("sleepTime < 0");
			this.async = async;
			this.sleepTime = sleepTime;
			return this;
		}

		@SneakyThrows
		public CrossBotDataManager build() {
			switch (type) {
				case CLIENT:
					return new CrossBotDataManager(host, port, poolSize, name, password, secure, async, sleepTime, listeners);
				case SERVER:
					return new CrossBotDataManager(port, poolSize, password, async, sleepTime, listeners);
				default:
					throw new AssertionError();
			}
		}

		public Builder host(String host) {
			if (type == Type.SERVER) throw new UnsupportedOperationException("Only client can specify host");
			this.host = host;
			return this;
		}

		public Builder name(String name) {
			if (type == Type.SERVER) throw new UnsupportedOperationException("Only client can specify host");
			this.name = name;
			return this;
		}

		public Builder password(String password) {
			this.password = password;
			return this;
		}

		public Builder poolSize(int poolSize) {
			this.poolSize = poolSize;
			return this;
		}

		public Builder port(int port) {
			this.port = port;
			return this;
		}

		public Builder secure(boolean secure) {
			if (type == Type.SERVER) throw new UnsupportedOperationException("Only client can specify secure");
			this.secure = secure;
			return this;
		}
	}

	protected final ExecutorService actionsExecutor;
	protected final Map<String, Pair<String, Integer>> bots = new ConcurrentHashMap<>();
	protected final AtomicBoolean closed = new AtomicBoolean(false);
	protected final Connection connection;
	protected final List<SocketListener> listeners = new CopyOnWriteArrayList<>();
	protected final AtomicLong nextRequestId = new AtomicLong(1);
	protected final Consumer<Object> send;
	protected final Queue<Object> sendQueue;
	protected Function<JSONObject, JSONObject> jsonHandler = obj -> new JSONObject().put("error", "unsupported");
	protected TLongObjectHashMap<Object> map = new TLongObjectHashMap<>(23, 15F, 0);

	@SneakyThrows
	protected CrossBotDataManager(String host, int port, int poolSize, String name, String password, boolean secure, boolean async, int sleepTime, List<SocketListener> listenersToAdd) throws URISyntaxException {
		if (host == null) throw new NullPointerException("host");
		if (name == null) throw new NullPointerException("name");
		if (port == 0) throw new IllegalArgumentException("port == 0");
		this.listeners.addAll(listenersToAdd);
		PacketRegistry pr = new PacketRegistry();
		registerPackets(pr);
		Client client;
		connection = client = new Client(new URI((secure ? "wss://" : "ws://") + host + ":" + port), pr, new SocketListenerAdapter() {
			@Override
			public void onClose(Connection connection, int id, int code, String message) {
				for (SocketListener listener : listeners)
					listener.onClose(connection, id, code, message);
			}

			@Override
			public Object onPacket(Connection connection, int i, Object o) {
				for (SocketListener listener : listeners) {
					Object p = listener.onPacket(connection, i, o);
					if (p != null) return p;
				}
				return null;
			}

			@Override
			public void onConnect(Connection connection, int id, WebSocket socket) {
				for (SocketListener listener : listeners)
					listener.onConnect(connection, id, socket);
			}

			@Override
			public void onError(Connection connection, int id, Exception ex) {
				for (SocketListener listener : listeners)
					listener.onError(connection, id, ex);
			}
		});
		try {
			if (!client.getPacketClient().connectBlocking()) throw new ConnectException("Error connecting to server");
		} catch (InterruptedException e) {
			throw new InternalError("Error connecting", e);
		}
		client.getPacketClient().waitForValidation();
		client.getPacketClient().sendPacket(new IdentifyPacket(name, password));
		sendQueue = new ConcurrentLinkedQueue<>();
		if (async) {
			Thread t = new Thread(() -> {
				while (true) {
					if (closed.get()) return;
					while (!sendQueue.isEmpty())
						client.getPacketClient().sendPacket(sendQueue.poll());
					try {
						Thread.sleep(sleepTime);
					} catch (InterruptedException e) {
						return;
					}
				}
			}, "CrossBotDataManagerClientSendThread");
			t.setDaemon(true);
			t.setPriority(Thread.MAX_PRIORITY);
			t.start();
		}
		send = async ? sendQueue::add : (o) -> client.getPacketClient().sendPacket(o);
		actionsExecutor = poolSize < 1 ? Executors.newCachedThreadPool() : Executors.newFixedThreadPool(poolSize);
		init();
	}

	protected CrossBotDataManager(int port, int poolSize, String password, boolean async, int sleepTime, List<SocketListener> listenersToAdd) {
		if (port == 0) throw new IllegalArgumentException("port == 0");
		this.listeners.addAll(listenersToAdd);
		PacketRegistry pr = new PacketRegistry();
		registerPackets(pr);
		Server server;
		connection = server = new Server(new InetSocketAddress(port), pr, new SocketListenerAdapter() {
			private String getAddress(WebSocket socket) {
				InetSocketAddress addr = socket.getRemoteSocketAddress();
				if (addr == null) return socket.getLocalSocketAddress().getHostString();
				return addr.getHostString();
			}

			@Override
			public Object onPacket(Connection connection, int i, Object o) {
				if (o instanceof IdentifyPacket) {
					IdentifyPacket pkt = (IdentifyPacket) o;
					if (password != null && !password.equals(pkt.password)) {
						connection.getSocket(i).close(1403, "Invalid password");
						return null;
					}
					if (bots.values().stream().filter(pair -> pair.getLeft().equals(pkt.name)).count() != 0) {
						connection.getSocket(i).close(1403, "Client named " + pkt.name + " already connected");
						return null;
					}
					bots.put(getAddress(connection.getSocket(i)), new ImmutablePair<>(pkt.name, i));
					return null;
				}
				for (SocketListener listener : listeners) {
					Object p = listener.onPacket(connection, i, o);
					if (p != null) return p;
				}
				return null;
			}

			@Override
			public void onClose(Connection connection, int id, int code, String message) {
				for (SocketListener listener : listeners)
					listener.onClose(connection, id, code, message);
			}

			@Override
			public void onConnect(Connection connection, int id, WebSocket socket) {
				for (SocketListener listener : listeners)
					listener.onConnect(connection, id, socket);
			}

			@Override
			public void onError(Connection connection, int id, Exception ex) {
				for (SocketListener listener : listeners)
					listener.onError(connection, id, ex);
			}

		});
		server.start();
		sendQueue = new ConcurrentLinkedQueue<>();
		if (async) {
			Thread t = new Thread(() -> {
				while (true) {
					if (closed.get()) return;
					while (!sendQueue.isEmpty()) {
						Object o = sendQueue.poll();
						server.connections().forEach(socket -> server.sendPacket(socket, o));
					}
					try {
						Thread.sleep(sleepTime);
					} catch (InterruptedException e) {
						return;
					}
				}
			}, "CrossBotDataManagerServerSendThread");
			t.setDaemon(true);
			t.setPriority(Thread.MAX_PRIORITY);
			t.start();
		}
		send = async ? sendQueue::add : (o) -> server.connections().forEach(socket -> server.sendPacket(socket, o));
		actionsExecutor = poolSize < 1 ? Executors.newCachedThreadPool() : Executors.newFixedThreadPool(poolSize);
		init();
	}

	@Override
	public void close() {
		closed.set(true);
		if (connection instanceof Client)
			connection.close(0);
		else {
			for (int i : ((Server) connection).getClients())
				connection.close(i);
		}
	}

	@Override
	public Consumer<Object> get() {
		return send;
	}

	@Override
	public void save() {
		if (connection instanceof Client) {
			Client client = (Client) connection;
			while (!sendQueue.isEmpty()) {
				client.getPacketClient().sendPacket(sendQueue.poll());
			}
		} else {
			Server server = (Server) connection;
			while (!sendQueue.isEmpty()) {
				Object o = sendQueue.poll();
				server.connections().forEach(socket -> server.sendPacket(socket, o));
			}
		}
	}

	private void init() {
		registerListener((FunctionalPacketHandler) (o) -> {
			if (o instanceof WrappedJSONPacket) {
				long id = ((WrappedJSONPacket) o).requestId;
				if (((WrappedJSONPacket) o).isResponse) {
					map.put(id, ((WrappedJSONPacket) o).getJSON());
					return null;
				}
				if (jsonHandler == null) return new WrappedJSONPacket("{}", id, true);
				try {
					return new WrappedJSONPacket(jsonHandler.apply(((WrappedJSONPacket) o).getJSON()).toString(), id, true);
				} catch (Throwable t) {
					return new WrappedJSONPacket("{}", id, true);
				}
			}
			return null;
		});
	}

	public boolean isClosed() {
		return closed.get();
	}

	public CrossBotDataManager jsonHandler(Function<JSONObject, JSONObject> jsonHandler) {
		this.jsonHandler = jsonHandler;
		return this;
	}

	public CrossBotDataManager registerListener(SocketListener listener) {
		listeners.add(listener);
		return this;
	}

	protected void registerPackets(PacketRegistry pr) {
		pr.register(IdentifyPacket.FACTORY);
		pr.register(GetMoneyPacket.FACTORY);
		pr.register(GetMoneyPacket.Response.FACTORY);
		pr.register(SetMoneyPacket.FACTORY);
		pr.register(UpdateMoneyPacket.FACTORY);
		pr.register(WrappedJSONPacket.FACTORY);
	}

	public CrossBotAction<JSONObject> sendJson(JSONObject obj) {
		return sendJson(obj, 3000);
	}

	@SuppressWarnings("Convert2Lambda")
	public CrossBotAction<JSONObject> sendJson(JSONObject obj, long responseTimeout) {
		return CrossBotAction.of(actionsExecutor, new Supplier<JSONObject>() {
			@Override
			@SneakyThrows
			public JSONObject get() {
				long timeout = responseTimeout;
				long id = nextRequestId.getAndIncrement();
				send.accept(new WrappedJSONPacket(obj.toString(), id, false));
				while (!map.containsKey(id)) {
					if (timeout-- <= 0)
						throw new TimeoutException("No response received in " + responseTimeout + " ms");
					Thread.sleep(1);
				}
				return (JSONObject) map.remove(id);
			}
		});
	}

	public CrossBotDataManager unregisterListener(SocketListener listener) {
		listeners.remove(listener);
		return this;
	}
}