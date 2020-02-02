/*
 * Copyright (C) 2016-2020 David Alejandro Rubio Escares / Kodehawa
 *
 *  Mantaro is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * Mantaro is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Mantaro.  If not, see http://www.gnu.org/licenses/
 *
 */

package net.kodehawa.mantarobot.commands.music.utils;

import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.tools.io.MessageInput;
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.BasicAudioPlaylist;
import lavalink.client.io.Lavalink;
import lavalink.client.io.LavalinkSocket;
import net.kodehawa.mantarobot.data.MantaroData;
import net.kodehawa.mantarobot.utils.URLEncoding;
import net.kodehawa.mantarobot.utils.Utils;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.Function;

public class LavalinkTrackLoader {
    public static void load(AudioPlayerManager manager, Lavalink<?> lavalink, String query,
                            AudioLoadResultHandler handler) {
        Iterator<LavalinkSocket> sockets = lavalink.getNodes().iterator();
        if(!sockets.hasNext()) {
            manager.loadItem(query, handler);
            return;
        }
        
        CompletionStage<Runnable> last = tryLoad(manager, sockets.next().getRemoteUri(), query, handler);
        while(sockets.hasNext()) {
            URI uri = sockets.next().getRemoteUri();
            //TODO: java 12 replace this with the line commented below
            var cf = new CompletableFuture<Runnable>();
            last.thenApply(CompletableFuture::completedStage)
                    .exceptionally(e -> tryLoad(manager, uri, query, handler))
                    .thenCompose(Function.identity())
                    .thenAccept(cf::complete)
                    .exceptionally(e -> {
                        cf.completeExceptionally(e);
                        return null;
                    });
            last = cf;
            //last = last.exceptionallyCompose(e -> tryLoad(manager, uri, query, handler));
        }
        last.whenComplete((ok, oof) -> {
            if(oof != null) {
                handler.loadFailed(new FriendlyException("Failed to load", FriendlyException.Severity.SUSPICIOUS, oof));
            } else {
                ok.run();
            }
        });
    }
    
    private static CompletionStage<Runnable> tryLoad(AudioPlayerManager manager, URI node, String query,
                                                     AudioLoadResultHandler handler) {
        CompletableFuture<Runnable> future = new CompletableFuture<>();
        Utils.httpClient.newCall(new Request.Builder()
                                         .url(node.toString() + "/loadtracks?identifier" + URLEncoding.encode(query))
                                         .header("Authorization", MantaroData.config().get().lavalinkPass)
                                         .build()
        ).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                future.completeExceptionally(e);
            }
            
            @Override
            public void onResponse(Call call, Response response) {
                try(Response r = response) {
                    ResponseBody body = r.body();
                    if(body == null) {
                        future.completeExceptionally(new IllegalArgumentException(
                                "Response body was null! Code " + response.code()
                        ));
                        return;
                    }
                    JSONObject json = new JSONObject(new JSONTokener(body.byteStream()));
                    switch(json.getString("loadType")) {
                        case "LOAD_FAILED": {
                            future.completeExceptionally(new IllegalArgumentException(
                                    json.getJSONObject("exception").getString("message")
                            ));
                            break;
                        }
                        case "NO_MATCHES": {
                            future.complete(handler::noMatches);
                            break;
                        }
                        case "TRACK_LOADED": {
                            String track = json.getJSONArray("tracks").getJSONObject(0).getString("track");
                            try {
                                AudioTrack t = decode(manager, track);
                                future.complete(() -> handler.trackLoaded(t));
                            } catch(Exception e) {
                                future.completeExceptionally(e);
                            }
                            break;
                        }
                        case "PLAYLIST_LOADED":
                        case "SEARCH_RESULT": {
                            List<AudioTrack> decoded = new ArrayList<>();
                            JSONArray tracks = json.getJSONArray("tracks");
                            for(int i = 0; i < tracks.length(); i++) {
                                try {
                                    decoded.add(decode(manager, tracks.getJSONObject(i).getString("track")));
                                } catch(Exception e) {
                                    future.completeExceptionally(e);
                                    return;
                                }
                            }
                            int selected = json.getJSONObject("playlistInfo").getInt("selectedTrack");
                            AudioPlaylist playlist = new BasicAudioPlaylist(
                                    json.getJSONObject("playlistInfo").getString("name"),
                                    decoded,
                                    selected >= 0 && selected < decoded.size() ? decoded.get(selected) : null,
                                    json.getString("loadType").equals("SEARCH_RESULT")
                            );
                            future.complete(() -> handler.playlistLoaded(playlist));
                            break;
                        }
                        default: {
                            future.completeExceptionally(new IllegalArgumentException("Unexpected loadType " + json.getString("loadType")));
                            break;
                        }
                    }
                } catch(Exception e) {
                    future.completeExceptionally(e);
                }
            }
        });
        return future;
    }
    
    private static AudioTrack decode(AudioPlayerManager manager, String track) {
        try {
            return manager.decodeTrack(new MessageInput(new ByteArrayInputStream(
                    Base64.getDecoder().decode(track)
            ))).decodedTrack;
        } catch(Exception e) {
            throw new IllegalArgumentException("Failed to decode track", e);
        }
    }
}
