package net.kodehawa.mantarobot.utils.http;

import net.dv8tion.jda.core.MessageBuilder;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.MessageEmbed;
import net.dv8tion.jda.core.entities.impl.MessageEmbedImpl;
import net.dv8tion.jda.core.entities.impl.MessageImpl;
import net.kodehawa.mantarobot.commands.currency.RateLimiter;
import org.json.JSONArray;
import org.json.JSONObject;

import java.time.OffsetDateTime;
import java.util.concurrent.TimeUnit;

/**
 * Original: https://github.com/natanbc/GabrielBot/tree/master/src/main/java/gabrielbot/utils/http/Webhook.java
 * Licensed under GPLv3.
 * <3
 * Modified it a lil.
 *
 * @author natanbc
 * @since 04/07/2017
 */
public class Webhook {
    public static final Requester REQUESTER = new Requester();
    private String API_ENDPOINT;
    private String avatarUrl;
    private String username;

    public Webhook(String endPoint) {
        API_ENDPOINT = endPoint;
    }

    public Webhook setUsername(String username) {
        this.username = username;
        return this;
    }

    public Webhook setAvatarUrl(String avatarUrl) {
        this.avatarUrl = avatarUrl;
        return this;
    }

    public Response rawPost(JSONObject message) throws RequestingException {
        if(avatarUrl != null && !message.has("avatar_url")) message.put("avatar_url", avatarUrl);
        if(username != null && !message.has("username")) message.put("username", username);
        return REQUESTER.post(API_ENDPOINT, message);
    }

    public Response post(Message message) throws RequestingException {
        return rawPost(((MessageImpl) message).toJSONObject());
    }

    public Response post(MessageEmbed... embeds) throws RequestingException {
        JSONObject object = new JSONObject();
        JSONArray array = new JSONArray();
        for(MessageEmbed embed : embeds) {
            array.put(((MessageEmbedImpl) embed).toJSONObject());
        }
        object.put("embeds", array);
        return rawPost(object);
    }

    public Response post(String message, boolean tts) throws RequestingException {
        return post(new MessageBuilder().append(message).setTTS(tts).build());
    }

    public Response post(String message) throws RequestingException {
        return post(new MessageBuilder().append(message).build());
    }

    public static final class Requester extends HTTPRequester {
        private boolean configured = false;

        Requester() {
            super("WebhookRequester", new RateLimiter(TimeUnit.MILLISECONDS, 4, 5000), builder ->
                    builder.readTimeout(10, TimeUnit.SECONDS).connectTimeout(10, TimeUnit.SECONDS).writeTimeout(10, TimeUnit.SECONDS).build()
            );
        }

        public Response post(String endpoint, JSONObject message) throws RequestingException {
            Request req = newRequest(endpoint, endpoint.split("/")[5]).body(message).header("Content-Type", "application/json");
            Response res;
            int i = 0;
            do {
                i++;
                res = req.post();
                if(res.code() == 429) {
                    long tryAgainIn = res.asObject().getInt("retry_after");
                    if(!onRateLimited(req, tryAgainIn)) {
                        throw new RateLimitedException(tryAgainIn);
                    }
                }
            } while(res.code() != 204 && i < 4);

            if(!configured && res.code() == 204) {
                int limit = Integer.parseInt(res.headers().get("x-ratelimit-limit").get(0));
                int remaining = Integer.parseInt(res.headers().get("x-ratelimit-remaining").get(0));
                if(remaining == limit - 1) {
                    long time = (Long.parseLong(res.headers().get("x-ratelimit-reset").get(0)) - OffsetDateTime.now().toEpochSecond()) / 10;
                    setRateLimiter(new RateLimiter(TimeUnit.SECONDS, remaining, (int) (time)));
                    configured = true;
                }
            }
            return res;
        }
    }
}
