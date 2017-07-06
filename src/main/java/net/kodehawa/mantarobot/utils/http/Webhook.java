package net.kodehawa.mantarobot.utils.http;

import net.dv8tion.jda.core.MessageBuilder;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.MessageEmbed;
import net.dv8tion.jda.core.entities.impl.MessageEmbedImpl;
import net.dv8tion.jda.core.entities.impl.MessageImpl;
import net.kodehawa.mantarobot.commands.currency.RateLimiter;
import org.json.JSONArray;
import org.json.JSONObject;

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
    private static String API_ENDPOINT;
    public static final Requester REQUESTER = new Requester();

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
        return REQUESTER.post(message);
    }

    public Response post(Message message) throws RequestingException {
        return rawPost(((MessageImpl)message).toJSONObject());
    }

    public Response post(MessageEmbed... embeds) throws RequestingException {
        JSONObject object = new JSONObject();
        JSONArray array = new JSONArray();
        for(MessageEmbed embed : embeds) {
            array.put(((MessageEmbedImpl)embed).toJSONObject());
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

    private static final class Requester extends HTTPRequester {
        Requester() {
            super("WebhookRequester", new RateLimiter(TimeUnit.SECONDS, 5, 5000));
        }

        public Response post(JSONObject message) throws RequestingException {
            return newRequest(API_ENDPOINT, API_ENDPOINT).body(message).header("Content-Type", "application/json").post();
        }
    }
}
