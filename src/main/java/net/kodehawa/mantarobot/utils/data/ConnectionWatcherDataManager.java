package net.kodehawa.mantarobot.utils.data;

import br.com.brjdevs.network.*;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import net.kodehawa.mantarobot.MantaroBot;
import net.kodehawa.mantarobot.data.ConnectionWatcherData;
import net.kodehawa.mantarobot.shard.MantaroShard;
import net.kodehawa.mantarobot.utils.KryoUtils;
import net.kodehawa.mantarobot.utils.SentryHelper;
import org.json.JSONArray;
import org.json.JSONObject;

import java.net.ConnectException;
import java.net.URI;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;

@Slf4j
public class ConnectionWatcherDataManager implements DataManager<ConnectionWatcherData> {
    public static final int CLOSE_CODE_OK = 1600;

    private final Client client;

    @SneakyThrows
    public ConnectionWatcherDataManager(int port) {
        client = new Client(new URI("wss://localhost:" + port), new PacketRegistry(), new SocketListenerAdapter() {
            @Override
            public void onClose(Connection connection, int id, int code, String message) {
                if(code != CLOSE_CODE_OK) {
                    SentryHelper.captureMessage("Connection within MW closed with unexpected code " + code + ": " + message, this.getClass());
                }
            }

            @Override
            public Object onPacket(Connection connection, int id, Object packet) {
                if(packet instanceof JSONPacket) {
                    JSONObject json = ((JSONPacket) packet).getJSON();
                    if(json.has("action")) {
                        switch(json.getString("action")) {
                            case "shutdown":
                                //TODO re-enable
                                /*MantaroBot.getInstance().getAudioManager().getMusicManagers().forEach((s, musicManager) -> {
									if (musicManager.getTrackScheduler() != null)
										musicManager.getTrackScheduler().stop();
								});*/

                                Arrays.stream(MantaroBot.getInstance().getShardedMantaro().getShards()).forEach(MantaroShard::prepareShutdown);

                                Arrays.stream(MantaroBot.getInstance().getShardedMantaro().getShards()).forEach(mantaroShard -> mantaroShard.getJDA().shutdown());
                                ConnectionWatcherDataManager.this.close();
                                System.exit(0);
                                break;
                            case "test":
                                System.out.println("received command remotely: " + json);
                                break;
                        }
                    }

                }
                return null;
            }
        });

        if(!client.getPacketClient().connectBlocking()) throw new ConnectException("Connection failed");

        client.getPacketClient().waitForValidation();
    }

    @Override
    @SuppressWarnings("unchecked")
    public ConnectionWatcherData get() {
        AbstractClient client = this.client.getPacketClient();
        client.sendPacket(new JSONPacket("{\"command\":\"return cw.getJdaPing()\"}"));
        int ping = client.readPacketBlocking(JSONPacket.class).getJSON().getJSONArray("returns").getInt(0);
        client.sendPacket(new JSONPacket("{\"command\":\"return cw.getReboots()\"}"));
        int reboots = client.readPacketBlocking(JSONPacket.class).getJSON().getJSONArray("returns").getInt(0);
        client.sendPacket(new JSONPacket("{\"command\":\"return cw.getOwners()\"}"));
        String owners = client.readPacketBlocking(JSONPacket.class).getJSON().getJSONArray("returns").getJSONObject(0).getString("data");
        client.sendPacket(new JSONPacket("{\"command\":\"return cw.getJvmArgs()\"}"));
        String jvmargs = client.readPacketBlocking(JSONPacket.class).getJSON().getJSONArray("returns").getJSONObject(0).getString("data");
        return new ConnectionWatcherData(
                (List<String>) KryoUtils.unserialize(Base64.getDecoder().decode(owners)),
                (List<String>) KryoUtils.unserialize(Base64.getDecoder().decode(jvmargs)),
                reboots,
                ping);
    }

    @Override
    public void save() {
    }

    @Override
    public void close() {
        client.getPacketClient().getConnection().close(CLOSE_CODE_OK);
    }

    public Object[] eval(String code) {
        client.getPacketClient().sendPacket(new JSONPacket(new JSONObject().put("command", code).toString()));
        JSONObject response = client.getPacketClient().readPacketBlocking(JSONPacket.class).getJSON();
        if(response.has("error")) {
            throw new RuntimeException(response.getString("error"));
        }
        JSONArray returns = response.getJSONArray("returns");
        Object[] ret = new Object[returns.length()];
        for(int i = 0; i < ret.length; i++) {
            Object o = returns.get(i);
            if(o instanceof JSONObject) {
                o = KryoUtils.unserialize(Base64.getDecoder().decode(((JSONObject) o).getString("data")));
            }
            ret[i] = o;
        }
        return ret;
    }

    public void reboot(boolean hardReboot) {
        client.getPacketClient().sendPacket(new JSONPacket("{\"command\":\"cw.reboot(" + hardReboot + ")\""));
        client.getPacketClient().getConnection().close(CLOSE_CODE_OK);
    }
}
