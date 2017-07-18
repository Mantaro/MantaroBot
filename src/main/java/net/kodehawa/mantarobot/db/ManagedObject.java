package net.kodehawa.mantarobot.db;

import net.kodehawa.mantarobot.data.MantaroData;

public interface ManagedObject {
    void delete();

    void save();

    default void deleteAsync() {
        MantaroData.queue(this::delete);
    }

    default void saveAsync() {
        MantaroData.queue(this::save);
    }
}
