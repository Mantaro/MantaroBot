package net.kodehawa.mantarobot.commands.custom.kaiperscript.wrapper;

import net.dv8tion.jda.core.entities.ISnowflake;

import java.time.OffsetDateTime;

class SafeJDAObject<T extends ISnowflake> {
    final T object;

    SafeJDAObject(T object) {
        this.object = object;
    }

    public String getId() {
        return object.getId();
    }

    public long getIdLong() {
        return object.getIdLong();
    }

    public OffsetDateTime getCreationTime() {
        return object.getCreationTime();
    }

    @Override
    public String toString() {
        return object.toString();
    }
}
