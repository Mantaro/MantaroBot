/*
 * Copyright (C) 2016 Kodehawa
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
 * along with Mantaro. If not, see http://www.gnu.org/licenses/
 *
 */

package net.kodehawa.mantarobot.utils.cache;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.core.JsonProcessingException;
import net.kodehawa.mantarobot.commands.action.WeebAPIRequester;
import net.kodehawa.mantarobot.data.MantaroData;
import net.kodehawa.mantarobot.utils.data.JsonDataManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Random;
import java.util.concurrent.TimeUnit;

public class ImageCache {
    @JsonIgnore
    private static final Random rand = new Random();
    @JsonIgnore
    private static final WeebAPIRequester weebAPI = new WeebAPIRequester();

    private static final Logger log = LoggerFactory.getLogger(ImageCache.class);
    private final List<ImageCacheType> images = new ArrayList<>();

    public List<ImageCacheType> getImages() {
        return images;
    }

    @JsonIgnore
    public boolean containsImage(String hash) {
        return images.stream()
                .filter(type -> type.id() != null) // How?
                .anyMatch(type -> type.id().equals(hash));
    }

    @JsonIgnore
    public static WeebAPIRequester.WeebAPIObject getImage(String type) throws NoSuchElementException, JsonProcessingException {
        // Having this on the method call itself caused this to fail prematurely.
        WeebAPIRequester.WeebAPIObject result = null;
        try {
            result = weebAPI.getRandomImageByType(type, false, "gif");
        } catch (Exception e) {
            log.debug("Error getting image from WeebAPI, attempting fallback", e);
        }

        if (result != null) {
            try (var jedis = MantaroData.getDefaultJedisPool().getResource()) {
                var cached = jedis.hget("image-cache", type);
                ImageCache cache;
                if (cached == null) {
                    cache = new ImageCache(); // No cache saved?
                } else {
                    cache = JsonDataManager.fromJson(cached, ImageCache.class);
                }

                // How any of this ones can be null is out of my understanding
                // but it happens
                var pass = result.id() != null && result.type() != null && result.url() != null;
                if (pass && !cache.containsImage(result.id())) {
                    cache.getImages().add(new ImageCacheType(result.type(), result.url(), result.id()));
                    jedis.hset("image-cache", type, JsonDataManager.toJson(cache));

                    // Expire the entire cache in 10 days, assuming we have no expiry set.
                    if (jedis.ttl("image-cache") == -1) { // NX option was added in Redis 7, and I spent a solid 20 minutes without realizing this.
                        jedis.expire("image-cache", TimeUnit.DAYS.toSeconds(10));
                    }
                }
            }
        } else { // API is dead, again.
            try (var jedis = MantaroData.getDefaultJedisPool().getResource()) {
                var cache = jedis.hget("image-cache", type);
                if (cache != null) {
                    var cacheData = JsonDataManager.fromJson(cache, ImageCache.class);
                    var images = cacheData.getImages();
                    var res = images.get(rand.nextInt(images.size()));
                    // We probably want to cache the actual image too somewhere? This kinda assumes the API is dead but the CDN isn't, which
                    // isn't always true...
                    // Also, this feels kinda hacky.
                    result = new WeebAPIRequester.WeebAPIObject(
                            res.id(), res.url(), "gif", false, type, Collections.emptyList()
                    );
                } else {
                    throw new NoSuchElementException();
                }
            }
        }

        return result;
    }
}
