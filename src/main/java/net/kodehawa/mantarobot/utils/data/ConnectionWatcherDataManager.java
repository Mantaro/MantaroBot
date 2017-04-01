package net.kodehawa.mantarobot.utils.data;

import br.com.brjdevs.network.*;
import net.kodehawa.mantarobot.data.ConnectionWatcherData;
import net.kodehawa.mantarobot.utils.KryoUtils;
import net.kodehawa.mantarobot.utils.UnsafeUtils;
import org.json.JSONArray;
import org.json.JSONObject;

import java.net.URI;
import java.util.Base64;
import java.util.List;

public class ConnectionWatcherDataManager implements DataManager<ConnectionWatcherData> {
    private final Client client;

    public ConnectionWatcherDataManager(int port) {
        try {
            client = new Client(new URI("wss://localhost:" + port), new PacketRegistry(), new SocketListenerAdapter() {
                @Override
                public Object onPacket(Connection connection, int id, Object packet) {
                    return null;
                }
            });
            client.getPacketClient().connectBlocking();
            client.getPacketClient().waitForValidation();
        } catch(Exception e) {
            UnsafeUtils.throwException(e);
            throw new InternalError();
        }
    }

    public void reboot(boolean hardReboot) {
        client.getPacketClient().sendPacket(new JSONPacket("{\"command\":\"cw.reboot(" + hardReboot + ")\""));
        client.close(0);
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

    @Override
    public void save() {
    }

    @Override
    public void close() {
        client.close(0);
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
}
