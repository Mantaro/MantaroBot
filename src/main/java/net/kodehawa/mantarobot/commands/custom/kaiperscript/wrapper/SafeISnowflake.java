package net.kodehawa.mantarobot.commands.custom.kaiperscript.wrapper;

import net.dv8tion.jda.core.entities.ISnowflake;

import java.time.OffsetDateTime;

class SafeISnowflake<T extends ISnowflake> {
    protected final T snowflake;

    SafeISnowflake(T snowflake) {
        this.snowflake = snowflake;
    }

    public String getId() {
        return snowflake.getId();
    }

    public long getIdLong() {
        return snowflake.getIdLong();
    }

    public OffsetDateTime getCreationTime() {
        return snowflake.getCreationTime();
    }

    @Override
    public String toString() {
        return snowflake.toString();
    }
}
