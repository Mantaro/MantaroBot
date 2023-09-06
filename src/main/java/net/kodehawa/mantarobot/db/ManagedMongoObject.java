package net.kodehawa.mantarobot.db;

import net.kodehawa.mantarobot.data.MantaroData;
import org.bson.codecs.pojo.annotations.BsonIgnore;

import javax.annotation.Nonnull;

@SuppressWarnings("unused")
public interface ManagedMongoObject {
    @Nonnull
    String getId();

    @BsonIgnore
    @Nonnull
    String getTableName();

    @BsonIgnore
    @Nonnull
    default String getDatabaseId() {
        return getId();
    }

    @BsonIgnore
    default void updateField(String key, Object value) {
        MantaroData.db().updateFieldValue(this, key, value);
    }

    @BsonIgnore
    default void updateAllChanged() {
        throw new UnsupportedOperationException();
    }

    // Need to implement class-by-class...
    @BsonIgnore
    void save();
    @BsonIgnore
    void delete();
}
