/*
 * Copyright (C) 2016-2021 David Rubio Escares / Kodehawa
 *
 *  Mantaro is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *  Mantaro is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Mantaro. If not, see http://www.gnu.org/licenses/
 */

package net.kodehawa.mantarobot.core.shard;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.events.ReadyEvent;
import net.dv8tion.jda.api.hooks.EventListener;
import net.kodehawa.mantarobot.core.MantaroEventManager;
import net.kodehawa.mantarobot.core.listeners.entities.CachedMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.time.Month;
import java.time.OffsetDateTime;
import java.util.Objects;
import java.util.Optional;

import static net.kodehawa.mantarobot.data.MantaroData.config;

public class Shard {
    private static final Logger log = LoggerFactory.getLogger(Shard.class);
    private final Cache<Long, Optional<CachedMessage>> messageCache =
            CacheBuilder.newBuilder().concurrencyLevel(5).maximumSize(2500).build();

    private final MantaroEventManager manager = new MantaroEventManager();
    private final int id;
    private final EventListener listener;
    private JDA jda;

    public Shard(int id) {
        this.id = id;
        this.listener = event -> {
            if(event instanceof ReadyEvent) {
                synchronized(this) {
                    jda = event.getJDA();
                    this.changeStatus();
                }
            }
        };
    }

    @CheckReturnValue
    public int getId() {
        return id;
    }

    @Nonnull
    @CheckReturnValue
    public Cache<Long, Optional<CachedMessage>> getMessageCache() {
        return messageCache;
    }

    @Nonnull
    @CheckReturnValue
    public MantaroEventManager getManager() {
        return manager;
    }

    @Nonnull
    @CheckReturnValue
    public EventListener getListener() {
        return listener;
    }

    @Nullable
    @CheckReturnValue
    public JDA getNullableJDA() {
        return jda;
    }

    @Nonnull
    @CheckReturnValue
    public JDA getJDA() {
        return Objects.requireNonNull(jda, "Shard has not been started yet");
    }

    private void changeStatus() {
        //insert $CURRENT_YEAR meme here
        var now = OffsetDateTime.now();
        if (now.getMonth() == Month.DECEMBER && now.getDayOfMonth() == 25) {
            getJDA().getPresence().setActivity(Activity.playing(String.format("/help | %s", "Merry Christmas!")));
            return;
        } else if (now.getMonth() == Month.JANUARY && now.getDayOfMonth() == 1) {
            getJDA().getPresence().setActivity(Activity.playing(String.format("/help | %s", "Happy New Year!")));
            return;
        }

        getJDA().getPresence().setActivity(Activity.playing("/help"));
    }
}
