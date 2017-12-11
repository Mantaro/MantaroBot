/*
 * Copyright (C) 2016-2017 David Alejandro Rubio Escares / Kodehawa
 *
 * Mantaro is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * Mantaro is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Mantaro.  If not, see http://www.gnu.org/licenses/
 */

package net.kodehawa.mantarobot.utils.data;

import br.com.brjdevs.network.*;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import net.kodehawa.mantarobot.MantaroBot;
import net.kodehawa.mantarobot.core.shard.MantaroShard;
import net.kodehawa.mantarobot.data.ConnectionWatcherData;
import net.kodehawa.mantarobot.utils.SentryHelper;
import org.json.JSONObject;

import java.net.ConnectException;
import java.net.URI;
import java.util.Arrays;

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
                                MantaroBot.getInstance().getAudioManager().getMusicManagers().forEach((s, musicManager) -> {
                                    if(musicManager.getTrackScheduler() != null)
                                        musicManager.getTrackScheduler().stop();
                                });

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
        long ping = client.readPacketBlocking(JSONPacket.class).getJSON().getLong("return");
        client.sendPacket(new JSONPacket("{\"command\":\"return cw.getReboots()\"}"));
        int reboots = client.readPacketBlocking(JSONPacket.class).getJSON().getInt("return");
        client.sendPacket(new JSONPacket("{\"command\":\"return cw.getOwners()\"}"));
        String owners = client.readPacketBlocking(JSONPacket.class).getJSON().getString("return");
        client.sendPacket(new JSONPacket("{\"command\":\"return cw.getJvmArgs()\"}"));
        String jvmargs = client.readPacketBlocking(JSONPacket.class).getJSON().getString("return");
        return new ConnectionWatcherData(
                owners,
                jvmargs,
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

    public String eval(String code) {
        client.getPacketClient().sendPacket(new JSONPacket(new JSONObject().put("command", code).toString()));
        JSONObject response = client.getPacketClient().readPacketBlocking(JSONPacket.class).getJSON();
        if(response.has("error")) {
            throw new RuntimeException(response.getString("error"));
        }
        return response.getString("return");
    }

    public void reboot(boolean hardReboot) {
        client.getPacketClient().sendPacket(new JSONPacket("{\"command\":\"cw.reboot(" + hardReboot + ")\""));
        client.getPacketClient().getConnection().close(CLOSE_CODE_OK);
    }
}
