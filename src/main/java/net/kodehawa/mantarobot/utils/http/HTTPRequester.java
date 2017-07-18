package net.kodehawa.mantarobot.utils.http;

import net.kodehawa.mantarobot.commands.currency.RateLimiter;
import okhttp3.OkHttpClient;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;
import java.util.function.Function;

/**
 * Original: https://github.com/natanbc/GabrielBot/tree/master/src/main/java/gabrielbot/utils/http/HTTPRequester.java
 * Licensed under GPLv3.
 * <3
 *
 * @author natanbc
 * @since 04/07/2017
 */
public class HTTPRequester {
    public static final OkHttpClient PARENT = new OkHttpClient();
    public static final HTTPRequester DEFAULT = new HTTPRequester("Default");

    protected final OkHttpClient client;
    protected final String identifier;
    private RateLimiter rateLimiter;

    public HTTPRequester(String identifier) {
        this(identifier, null, null);
    }

    public HTTPRequester(String identifier, RateLimiter rateLimiter) {
        this(identifier, rateLimiter, null);
    }

    public HTTPRequester(String identifier, RateLimiter rateLimiter, Function<OkHttpClient.Builder, OkHttpClient> configurator) {
        OkHttpClient.Builder builder = PARENT.newBuilder();
        if(configurator != null) {
            this.client = Objects.requireNonNull(configurator.apply(builder), "Client");
        } else {
            this.client = builder.build();
        }
        this.identifier = identifier;
        this.rateLimiter = rateLimiter;
    }

    private static byte[] fromBody(ResponseBody body) throws IOException {
        if(body == null) return new byte[0];
        InputStream is = body.byteStream();
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        int r;
        while((r = is.read(buffer)) != -1) {
            baos.write(buffer, 0, r);
        }
        return baos.toByteArray();
    }

    public Request newRequest(String url) {
        return newRequest(url, "no-key");
    }

    public Request newRequest(String url, String rateLimitKey) {
        return new Request(this, url, rateLimitKey);
    }

    protected long processRateLimit(String rateLimitKey) {
        if(rateLimiter == null) return -1;
        if(rateLimiter.process(rateLimitKey)) return -1;
        return rateLimiter.tryAgainIn(rateLimitKey);
    }

    public Response get(Request request) throws RequestingException {
        long l = processRateLimit(request.rateLimitKey);
        if(l != -1 && !onRateLimited(request, l)) throw new RateLimitedException(l);
        try {
            return execute(requestBuilder(request));
        } catch(Exception e) {
            throw new RequestingException(request, identifier, e);
        }
    }

    public Response post(Request request) throws RequestingException {
        long l = processRateLimit(request.rateLimitKey);
        if(l != -1 && !onRateLimited(request, l)) throw new RateLimitedException(l);
        try {
            return execute(requestBuilder(request)
                    .post(RequestBody.create(
                            request.type,
                            request.body
                    ))
            );
        } catch(Exception e) {
            throw new RequestingException(request, identifier, e);
        }
    }

    protected boolean onRateLimited(Request request, long tryAgainIn) {
        try {
            Thread.sleep(tryAgainIn);
        } catch(InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
        return true;
    }

    protected void setRateLimiter(RateLimiter limiter) {
        this.rateLimiter = limiter;
    }

    protected okhttp3.Request.Builder requestBuilder(Request request) {
        okhttp3.Request.Builder builder = new okhttp3.Request.Builder()
                .url(request.url)
                .header("User-Agent", "GabrielBot (Discord Bot)/" + identifier + " HTTP Client");
        request.headers.forEach(builder::addHeader);
        return builder;
    }

    protected Response execute(okhttp3.Request.Builder builder) throws IOException {
        okhttp3.Response res = client.newCall(builder.build()).execute();
        ResponseBody body = res.body();
        Response r = new Response(fromBody(body), res.code(), res.headers().toMultimap());
        res.close();
        return r;
    }
}
