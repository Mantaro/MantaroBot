package net.kodehawa.mantarobot.connectionwatcher;

import br.com.brjdevs.network.Connection;
import br.com.brjdevs.network.JSONPacket;
import br.com.brjdevs.network.SocketListenerAdapter;
import net.sandius.rembulan.Table;
import net.sandius.rembulan.load.LoaderException;
import net.sandius.rembulan.runtime.LuaFunction;
import org.java_websocket.WebSocket;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

public class WebsocketMessageHandler extends SocketListenerAdapter {
    @Override
    public Object onPacket(Connection connection, int id, Object packet) {
        if(packet instanceof JSONPacket) {
            JSONObject json = ((JSONPacket) packet).getJSON();
            String s = json.optString("command");
            if(s == null) return null;
            try {
                Object[] o = LuaEvaluator.eval(s);
                if(o == null) return new JSONPacket(new JSONObject().put("error", "Unknown error").toString(4));
                JSONArray returns = new JSONArray();
                for(Object obj : o) {
                    if(obj instanceof String || obj instanceof Number || obj == null) {
                        returns.put(obj);
                    } else if(obj instanceof Table) {
                        Map<Object, Object> map = new HashMap<>();
                        Table t = (Table)obj;
                        for(Object key = t.initialKey(); key != null; key = t.successorKeyOf(key)) {
                            Object v = t.rawget(key);
                            Object k = key.getClass().getName().equals("net.sandius.rembulan.StringByteString") ? key.toString() : key;
                            if(v instanceof LuaFunction) {
                                map.put(k, "function: 0x" + Integer.toHexString(System.identityHashCode(v)));
                            } if(v instanceof Table) {
                                map.put(k, "table: 0x" + Integer.toHexString(System.identityHashCode(v)));
                            } else {
                                map.put(k, v);
                            }
                        }
                        returns.put(new JSONObject().put("data", Base64.getEncoder().encodeToString(KryoUtils.serialize(map))));
                    } else if(obj instanceof LuaFunction) {
                        returns.put("function: 0x" + Integer.toHexString(System.identityHashCode(obj)));
                    } else {
                        returns.put(new JSONObject().put("data", Base64.getEncoder().encodeToString(KryoUtils.serialize(obj))));
                    }
                }
                return new JSONPacket(new JSONObject().put("returns", returns).toString(4));
            } catch(LoaderException e) {
                String err = e.getLuaStyleErrorMessage();
                return new JSONPacket(new JSONObject().put("error", err).toString(4));
            } catch(LuaEvaluator.RunningException e) {
                String err = e.getTraceback();
                return new JSONPacket(new JSONObject().put("error", err).toString(4));
            }
        }
        return null;
    }

    @Override
    public void onConnect(Connection connection, int id, WebSocket socket) {
        if(socket.getLocalSocketAddress() == null) socket.close(1500, "Not localhost");
    }
}
