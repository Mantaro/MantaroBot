package net.kodehawa.mantarobot.db;

import com.fasterxml.jackson.annotation.JsonIgnore;

import javax.annotation.Nonnull;

public interface ManagedMongoObject {
    @Nonnull
    String getId();

    @JsonIgnore
    @Nonnull
    String getTableName();

    @JsonIgnore
    @Nonnull
    default String getDatabaseId() {
        return getId();
    }

    // Need to implement class-by-class...
    void save();
    void delete();
}
